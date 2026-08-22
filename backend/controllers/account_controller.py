import re
import time
import threading
import urllib.parse
import urllib.request
import webbrowser
import shutil
from pathlib import Path
from backend.services import cape_community
from PySide6.QtCore import QObject, Signal, Slot, Property, QUrl, Qt
from PySide6.QtWidgets import QDialog, QVBoxLayout

from backend.services.msa_auth import (
    get_minecraft_session,
    authenticate_with_authorization_code,
    logout_account,
    MinecraftSession,
    MICROSOFT_AUTH_URL
)


from backend.services.skin_service import upload_skin_file, reset_skin_to_default
from backend.models.types import DATA_DIR
from PySide6.QtWidgets import QDialog, QVBoxLayout, QFileDialog


class MicrosoftLoginDialog(QDialog):
    """Native Microsoft Login Dialog with embedded WebEngine."""
    codeReceived = Signal(str)

    def __init__(self, parent=None):
        super().__init__(parent)
        from PySide6.QtWebEngineWidgets import QWebEngineView
        self.setWindowTitle("Microsoft Anmeldung · EzClient")
        self.resize(520, 680)
        self.setWindowFlags(self.windowFlags() & ~Qt.WindowContextHelpButtonHint)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        self.view = QWebEngineView(self)
        layout.addWidget(self.view)

        self.view.urlChanged.connect(self._on_url_changed)
        self.view.load(QUrl(MICROSOFT_AUTH_URL))

    def _on_url_changed(self, url: QUrl) -> None:
        url_str = url.toString()
        if "oauth20_desktop.srf" in url_str:
            parsed = urllib.parse.urlparse(url_str)
            params = urllib.parse.parse_qs(parsed.query)
            if "code" in params:
                code = params["code"][0]
                self.codeReceived.emit(code)
                self.accept()
            elif "error" in params:
                print(f"[MicrosoftLogin] Login error from URL: {params.get('error_description', ['Unknown error'])[0]}")
                self.reject()


class AccountController(QObject):
    """
    Manages the Minecraft account session, direct Microsoft Login, and Skin customization.
    """
    accountChanged = Signal()
    loginStatusChanged = Signal(str, bool)
    loginSuccess = Signal(str)
    skinUploadStatusChanged = Signal(str, bool)
    capeCommunityChanged = Signal()
    capeCommunityStatusChanged = Signal(str, bool)

    def __init__(self, parent=None):
        super().__init__(parent)
        self._username = "Player"
        self._account_type = "Microsoft Account"
        self._uuid = ""
        self._skin_url = ""
        self._skin_version = int(time.time())
        self._is_online = False
        self._is_logging_in = False
        self._login_status = ""
        self._login_dialog = None
        self._active_custom_name = ""
        self._active_custom_path = ""
        self._active_custom_body = ""
        self._active_custom_avatar = ""
        self._community_capes: list[dict] = []
        self._cape_community_status = "Lade Community-Capes …"

        try:
            from backend.services.skin_service import get_active_skin
            act = get_active_skin()
            if act:
                self._active_custom_name = act.get("name", "")
                self._active_custom_path = act.get("path", "")
                self._active_custom_body = act.get("bodyUrl", "")
                self._active_custom_avatar = act.get("avatarUrl", "")
        except Exception:
            pass

        self._load_account()
        self.refreshCapeCommunity()

    def _load_account(self, force_refresh: bool = False) -> None:
        """Read active session from .minecraft or stored cache."""
        def worker():
            try:
                session: MinecraftSession = get_minecraft_session(force_refresh=force_refresh)
                self._username = session.username
                self._uuid = session.uuid
                self._skin_url = session.skin_url
                self._is_online = session.is_online
                self._skin_version = int(time.time())
                self._account_type = "Microsoft Account (Online Verifiziert)" if session.is_online else "Microsoft Account (Lokal)"
            except Exception as e:
                print(f"[AccountController] Error loading account: {e}")
            finally:
                self.accountChanged.emit()

        threading.Thread(target=worker, daemon=True).start()

    @Property(str, notify=accountChanged)
    def username(self) -> str:
        return self._username or "Player"

    @Property(str, notify=accountChanged)
    def accountType(self) -> str:
        return self._account_type

    @Property(str, notify=accountChanged)
    def uuid(self) -> str:
        return self._uuid

    @Property(bool, notify=accountChanged)
    def isOnline(self) -> bool:
        return self._is_online

    @Property(bool, notify=loginStatusChanged)
    def isLoggingIn(self) -> bool:
        return self._is_logging_in

    @Property(str, notify=loginStatusChanged)
    def loginStatus(self) -> str:
        return self._login_status

    @Property(str, constant=True)
    def microsoftAuthUrl(self) -> str:
        return MICROSOFT_AUTH_URL

    @Property(str, notify=accountChanged)
    def avatarUrl(self) -> str:
        from backend.services.skin_service import extract_head_avatar_data_uri, get_skins_dir

        # 1. Real-time head avatar extraction from active custom skin file
        if self._active_custom_path and Path(self._active_custom_path).exists():
            uri = extract_head_avatar_data_uri(self._active_custom_path)
            if uri:
                return uri

        # 2. Check local skins directory for current user
        if self._username:
            user_png = get_skins_dir() / f"{self._username}.png"
            if user_png.exists():
                uri = extract_head_avatar_data_uri(user_png)
                if uri:
                    return uri

        # 3. If stored active avatar is a valid data URI
        if self._active_custom_avatar and self._active_custom_avatar.startswith("data:image"):
            return self._active_custom_avatar

        # 4. Fallback online Mojang head avatar by username / skin version
        if self._username:
            return f"https://minotar.net/helm/{self._username}/64.png"
        return ""

    @Property(str, notify=accountChanged)
    def bustUrl(self) -> str:
        if self._active_custom_body:
            return self._active_custom_body
        if self._username:
            return f"https://minotar.net/armor/bust/{self._username}/160.png"
        return ""

    @Property(str, notify=accountChanged)
    def bodyUrl(self) -> str:
        if self._active_custom_body:
            return self._active_custom_body
        if self._username:
            return f"https://minotar.net/armor/body/{self._username}/360.png"
        return ""

    @Property(str, notify=accountChanged)
    def activeSkinName(self) -> str:
        return self._active_custom_name or self._username or "Player"

    @Property(str, notify=accountChanged)
    def activeSkinPath(self) -> str:
        return self._active_custom_path or ""

    @Property(str, notify=accountChanged)
    def skinTextureUrl(self) -> str:
        import base64
        # 1. Custom active skin file
        if self._active_custom_path and Path(self._active_custom_path).exists():
            try:
                raw = Path(self._active_custom_path).read_bytes()
                return f"data:image/png;base64,{base64.b64encode(raw).decode('ascii')}"
            except Exception:
                pass

        # 2. Check local skins directory for current user
        from backend.services.skin_service import get_skins_dir
        if self._username:
            user_png = get_skins_dir() / f"{self._username}.png"
            if user_png.exists():
                try:
                    raw = user_png.read_bytes()
                    return f"data:image/png;base64,{base64.b64encode(raw).decode('ascii')}"
                except Exception:
                    pass

        # 3. Online session skin
        if self._skin_url:
            return self._skin_url

        # 4. Fallback online lookup by username
        if self._username and self._username.lower() != "player":
            return f"https://minotar.net/skin/{self._username}"

        return ""

    @Property(str, notify=accountChanged)
    def capeTextureUrl(self) -> str:
        """Persistent local EzClient cape used by the launcher preview."""
        import base64
        cape = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
        if not cape.exists():
            return ""

    @Property("QVariantList", notify=capeCommunityChanged)
    def communityCapes(self) -> list[dict]:
        return self._community_capes

    @Property(str, notify=capeCommunityChanged)
    def capeCommunityStatus(self) -> str:
        return self._cape_community_status

    @Slot()
    def refreshCapeCommunity(self) -> None:
        self._cape_community_status = "Lade Community-Capes …"
        self.capeCommunityChanged.emit()

        def worker() -> None:
            try:
                capes = cape_community.list_capes()
                for cape in capes:
                    cape["imageUrl"] = cape_community.cape_image_url(cape)
                self._community_capes = capes
                self._cape_community_status = f"{len(capes)} Community-Capes"
                self.capeCommunityStatusChanged.emit(self._cape_community_status, False)
            except Exception:
                self._community_capes = []
                self._cape_community_status = "Community-Server nicht erreichbar"
                self.capeCommunityStatusChanged.emit(
                    "Cape-Community ist gerade nicht erreichbar. Bitte später erneut versuchen.", True
                )
            finally:
                self.capeCommunityChanged.emit()

        threading.Thread(target=worker, daemon=True).start()

    @Slot(str, result=bool)
    def activateCommunityCape(self, image_url: str) -> bool:
        """Download a chosen community cape and make it the local active cape."""
        try:
            request = urllib.request.Request(image_url, headers={"User-Agent": "EzClient/1.5.1"})
            with urllib.request.urlopen(request, timeout=15) as response:
                raw = response.read(512 * 1024 + 1)
            if not cape_community.is_safe_cape_png(raw):
                raise ValueError("Ungültiges Cape-Bild")
            target = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(raw)
            self.accountChanged.emit()
            self.capeCommunityStatusChanged.emit("Cape aktiviert.", False)
            return True
        except Exception as exc:
            self.capeCommunityStatusChanged.emit(f"Cape konnte nicht aktiviert werden: {exc}", True)
            return False

    @Slot(str, result=bool)
    def publishCape(self, title: str) -> bool:
        cape = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
        if not cape.exists():
            self.capeCommunityStatusChanged.emit("Wähle zuerst ein Cape als PNG aus.", True)
            return False

        def worker() -> None:
            try:
                cape_community.upload_cape(cape, self.username, self.uuid, title.strip())
                self.capeCommunityStatusChanged.emit("Cape wurde in der Community veröffentlicht.", False)
                self.refreshCapeCommunity()
            except Exception as exc:
                self.capeCommunityStatusChanged.emit(f"Upload fehlgeschlagen: {exc}", True)

        threading.Thread(target=worker, daemon=True).start()
        return True

    @Slot(str, str, result=bool)
    def reportCape(self, cape_id: str, reason: str) -> bool:
        if not cape_id or not reason.strip():
            return False
        def worker() -> None:
            try:
                cape_community.report_cape(cape_id, reason.strip(), self.username)
                self.capeCommunityStatusChanged.emit("Danke, die Meldung wurde an das Team gesendet.", False)
            except Exception as exc:
                self.capeCommunityStatusChanged.emit(f"Meldung fehlgeschlagen: {exc}", True)
        threading.Thread(target=worker, daemon=True).start()
        return True
        try:
            return f"data:image/png;base64,{base64.b64encode(cape.read_bytes()).decode('ascii')}"
        except Exception:
            return ""

    @Slot(result=str)
    def pickCapeFile(self) -> str:
        file_path, _ = QFileDialog.getOpenFileName(
            None, "EzClient Cape (.png) auswählen", "", "PNG Cape (*.png);;Alle Dateien (*.*)"
        )
        if not file_path:
            return ""
        try:
            if not cape_community.is_safe_cape_png(Path(file_path).read_bytes()):
                self.skinUploadStatusChanged.emit("Ungültiges Cape: erlaubt sind nur geprüfte PNG-Capes bis 256×128.", True)
                return ""
        except OSError:
            self.skinUploadStatusChanged.emit("Cape-Datei konnte nicht gelesen werden.", True)
            return ""
        cape_dir = Path(DATA_DIR) / "cosmetics"
        cape_dir.mkdir(parents=True, exist_ok=True)
        target = cape_dir / "active_cape.png"
        shutil.copy2(file_path, target)
        self.accountChanged.emit()
        self.skinUploadStatusChanged.emit("Cape gespeichert und in der Vorschau aktiviert.", False)
        return self.capeTextureUrl

    @Slot(str, result=bool)
    def saveCapeDataUrl(self, data_url: str) -> bool:
        """Persist a Cape Editor canvas only after the same strict PNG validation."""
        try:
            import base64
            prefix = "data:image/png;base64,"
            if not data_url.startswith(prefix):
                raise ValueError("Ungültiges Cape-Format")
            raw = base64.b64decode(data_url[len(prefix):], validate=True)
            if not cape_community.is_safe_cape_png(raw):
                raise ValueError("Das Editor-Cape entspricht nicht dem sicheren Cape-Format")
            target = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(raw)
            self.accountChanged.emit()
            self.skinUploadStatusChanged.emit("Cape aus dem Editor gespeichert.", False)
            return True
        except (ValueError, OSError) as exc:
            self.skinUploadStatusChanged.emit(f"Cape konnte nicht gespeichert werden: {exc}", True)
            return False

    @Slot(str, result=str)
    def getSkinTextureUrl(self, path_or_username: str) -> str:
        val = (path_or_username or "").strip()
        if not val:
            return self.skinTextureUrl

        clean_path = val
        if clean_path.startswith("file:///"):
            clean_path = clean_path[8:]
        elif clean_path.startswith("file://"):
            clean_path = clean_path[7:]

        import base64
        p = Path(clean_path)
        if p.exists() and p.is_file():
            try:
                raw = p.read_bytes()
                return f"data:image/png;base64,{base64.b64encode(raw).decode('ascii')}"
            except Exception:
                pass

        from backend.services.skin_service import get_skins_dir
        user_png = get_skins_dir() / f"{val}.png"
        if user_png.exists():
            try:
                raw = user_png.read_bytes()
                return f"data:image/png;base64,{base64.b64encode(raw).decode('ascii')}"
            except Exception:
                pass

        if val.startswith("http://") or val.startswith("https://") or val.startswith("data:"):
            return val

        return f"https://minotar.net/skin/{val}"

    @Slot()
    def openLoginDialog(self) -> None:
        """Opens the embedded native Microsoft login window."""
        try:
            self._login_dialog = MicrosoftLoginDialog()
            self._login_dialog.codeReceived.connect(self.submitAuthorizationCode)
            self._login_dialog.show()
            self._login_dialog.raise_()
            self._login_dialog.activateWindow()
        except Exception as e:
            print(f"[AccountController] Failed to open login dialog: {e}, opening browser instead.")
            self.startBrowserLogin()

    @Slot()
    def startBrowserLogin(self) -> None:
        """Opens Microsoft Login in the system default browser."""
        webbrowser.open(MICROSOFT_AUTH_URL)

    @Slot(str)
    def submitAuthorizationCode(self, code_or_url: str) -> None:
        """Exchanges an authorization code or full redirect URL for a Minecraft session."""
        val = code_or_url.strip()
        code = val
        if "code=" in val:
            match = re.search(r"code=([^&]+)", val)
            if match:
                code = match.group(1)

        if not code:
            self.loginStatusChanged.emit("Ungültiger Autorisierungs-Code.", True)
            return

        self._is_logging_in = True
        self._login_status = "Authentifiziere mit Xbox Live & Mojang…"
        self.loginStatusChanged.emit(self._login_status, False)

        def worker():
            try:
                session = authenticate_with_authorization_code(code)
                if session:
                    self._username = session.username
                    self._uuid = session.uuid
                    self._skin_url = session.skin_url
                    self._is_online = session.is_online
                    self._account_type = "Microsoft Account (Online Verifiziert)"
                    self._is_logging_in = False
                    
                    if session.skin_url:
                        from backend.services.skin_service import get_skins_dir
                        import urllib.request
                        user_png = get_skins_dir() / f"{session.username}.png"
                        try:
                            urllib.request.urlretrieve(session.skin_url, str(user_png))
                        except Exception as e:
                            print(f"[AccountController] Could not download skin texture: {e}")
                    self._login_status = f"Erfolgreich eingeloggt als {session.username}!"
                    self.loginStatusChanged.emit(self._login_status, False)
                    self.accountChanged.emit()
                    self.loginSuccess.emit(session.username)
                else:
                    self._is_logging_in = False
                    self._login_status = "Login fehlgeschlagen. Bitte erneut versuchen."
                    self.loginStatusChanged.emit(self._login_status, True)
            except Exception as exc:
                self._is_logging_in = False
                self._login_status = f"Fehler: {exc}"
                self.loginStatusChanged.emit(self._login_status, True)

        threading.Thread(target=worker, daemon=True).start()

    @Slot()
    def logout(self) -> None:
        """Logs out the active account."""
        logout_account()
        self._username = "Player"
        self._uuid = ""
        self._skin_url = ""
        self._is_online = False
        self._account_type = "Offline"
        self.accountChanged.emit()

    skinFetched = Signal(str, str)  # local_path, preview_url
    skinHistoryChanged = Signal()
    savedSkinsChanged = Signal()

    @Property("QVariantList", notify=skinHistoryChanged)
    def skinHistory(self) -> list[dict]:
        from backend.services.skin_service import get_skin_history
        return get_skin_history()

    @Property("QVariantList", notify=savedSkinsChanged)
    def savedSkins(self) -> list[dict]:
        from backend.services.skin_service import get_saved_skins
        return get_saved_skins()

    @Slot(str, str)
    def saveCurrentSkin(self, name: str, path: str = "") -> None:
        """Saves a custom skin with a custom name to the persistent library."""
        target_path = path or self._active_custom_path
        target_name = (name or "").strip() or self._active_custom_name or self._username or "Mein Skin"
        avatar_to_save = self._active_custom_avatar or self.avatarUrl
        from backend.services.skin_service import save_skin_to_library
        save_skin_to_library(target_name, target_path, avatar_to_save)
        self.savedSkinsChanged.emit()
        self.skinUploadStatusChanged.emit(f"Skin '{target_name}' in Bibliothek gespeichert!", False)

    @Slot(str)
    def deleteSavedSkin(self, skin_id_or_name: str) -> None:
        """Removes a skin from the persistent library."""
        from backend.services.skin_service import delete_saved_skin_from_library
        delete_saved_skin_from_library(skin_id_or_name)
        self.savedSkinsChanged.emit()
        self.skinUploadStatusChanged.emit("Skin aus Bibliothek gelöscht.", False)

    @Slot(str)
    def fetchSkinByUsername(self, username: str) -> None:
        """Fetches skin PNG texture and 3D preview for any player username without applying it."""
        name = (username or "").strip()
        if not name:
            self.skinUploadStatusChanged.emit("Bitte gib einen Spielernamen ein.", True)
            return

        self.skinUploadStatusChanged.emit(f"Lade Skin-Vorschau für '{name}'…", False)

        def worker():
            from backend.services.skin_service import fetch_skin_by_username, generate_skin_renders
            ok, path, preview = fetch_skin_by_username(name)
            if ok:
                body_p, av_p = generate_skin_renders(path)
                body_url = ("file:///" + str(Path(body_p)).replace("\\", "/")) if body_p else preview
                self.skinFetched.emit(path, body_url)
                self.skinUploadStatusChanged.emit(f"Vorschau von '{name}' geladen. Klicke auf 'Skin anwenden'.", False)
            else:
                self.skinUploadStatusChanged.emit(preview, True)

        threading.Thread(target=worker, daemon=True).start()

    @Slot()
    def refresh(self) -> None:
        self._load_account(force_refresh=True)

    @Slot(result=str)
    def pickSkinFile(self) -> str:
        """Opens file dialog for selecting a Minecraft Skin PNG for preview."""
        file_path, _ = QFileDialog.getOpenFileName(
            None,
            "Minecraft Skin (.png) auswählen",
            "",
            "Minecraft Skin (*.png);;Alle Dateien (*.*)"
        )
        if file_path:
            from backend.services.skin_service import generate_skin_renders
            body_p, av_p = generate_skin_renders(file_path)
            body_url = ("file:///" + str(Path(body_p)).replace("\\", "/")) if body_p else ""
            self.skinFetched.emit(file_path, body_url)
            self.skinUploadStatusChanged.emit(f"Datei '{Path(file_path).name}' in Vorschau geladen. Klicke auf 'Skin anwenden'.", False)
        return file_path or ""

    @Slot(str, str, str)
    def applySkin(self, file_path_or_user: str, variant: str = "classic", custom_name: str = "") -> None:
        """Applies and activates a skin (locally and to Mojang if online)."""
        self._apply_and_upload_skin(file_path_or_user, variant, custom_name)

    @Slot(str, str)
    def uploadSkin(self, file_path_or_user: str, variant: str = "classic") -> None:
        """Uploads and activates a skin."""
        self._apply_and_upload_skin(file_path_or_user, variant, "")

    def _apply_and_upload_skin(self, file_path_or_user: str, variant: str = "classic", custom_name: str = "") -> None:
        target = (file_path_or_user or "").strip()
        if not target:
            self.skinUploadStatusChanged.emit("Keine Skin-Datei ausgewählt.", True)
            return

        from backend.services.skin_service import generate_skin_renders, add_skin_to_history, set_active_skin, get_skins_dir

        clean_path = target
        if clean_path.startswith("file:///"):
            clean_path = clean_path[8:]
        elif clean_path.startswith("file://"):
            clean_path = clean_path[7:]

        p = Path(clean_path)
        if not p.exists() or not p.is_file():
            user_p = get_skins_dir() / f"{target}.png"
            if user_p.exists():
                p = user_p
                clean_path = str(p)

        body_p, av_p = generate_skin_renders(clean_path) if (p.exists() and p.is_file()) else ("", "")
        clean_name = (custom_name or "").strip() or (p.stem.replace("_", " ").replace("-", " ").title() if p.exists() else target)
        av_url = ("file:///" + str(Path(av_p)).replace("\\", "/")) if av_p else f"https://mc-heads.net/avatar/{clean_name}/64"
        body_url = ("file:///" + str(Path(body_p)).replace("\\", "/")) if body_p else ""

        self._active_custom_body = body_url
        self._active_custom_avatar = av_url
        self._active_custom_name = clean_name
        self._active_custom_path = str(clean_path) if (p.exists() and p.is_file()) else ""
        set_active_skin(clean_name, self._active_custom_path, body_url, av_url)
        add_skin_to_history(clean_name, self._active_custom_path, av_url)
        self._skin_version = int(time.time())
        self.accountChanged.emit()
        self.skinHistoryChanged.emit()
        self.skinUploadStatusChanged.emit("Skin erfolgreich ausgewählt & aktiviert!", False)

        def worker():
            try:
                session = get_minecraft_session()
                token = session.access_token if session else ""
                if not token:
                    self.skinUploadStatusChanged.emit("Skin lokal ausgewählt (Offline/Lokaler Modus).", False)
                    return

                if p.exists() and p.is_file():
                    ok, msg = upload_skin_file(token, clean_path, variant)
                    self.skinUploadStatusChanged.emit(msg, not ok)
                    if ok:
                        self._load_account(force_refresh=True)
            except Exception as e:
                self.skinUploadStatusChanged.emit(f"Fehler beim Mojang-Upload: {e}", True)

        threading.Thread(target=worker, daemon=True).start()

    @Slot()
    def resetSkin(self) -> None:
        """Resets active skin to default."""
        self._active_custom_body = ""
        self._active_custom_avatar = ""
        self._active_custom_name = ""
        self._active_custom_path = ""
        from backend.services.skin_service import set_active_skin
        set_active_skin("", "", "", "")
        self._skin_version = int(time.time())
        self.accountChanged.emit()
        self.skinUploadStatusChanged.emit("Skin wird zurückgesetzt…", False)

        def worker():
            try:
                session = get_minecraft_session()
                token = session.access_token if session else ""
                if not token:
                    self.skinUploadStatusChanged.emit("Standard-Skin aktiv.", False)
                    return

                ok, msg = reset_skin_to_default(token)
                self.skinUploadStatusChanged.emit(msg, not ok)
                if ok:
                    self._load_account(force_refresh=True)
            except Exception as e:
                self.skinUploadStatusChanged.emit(f"Fehler: {e}", True)

        threading.Thread(target=worker, daemon=True).start()
