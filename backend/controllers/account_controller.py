import re
import time
import threading
import urllib.parse
import webbrowser
from pathlib import Path
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
        if self._active_custom_avatar:
            return self._active_custom_avatar
        if self._username:
            return f"https://mc-heads.net/avatar/{self._username}/64?t={self._skin_version}"
        return ""

    @Property(str, notify=accountChanged)
    def bustUrl(self) -> str:
        if self._active_custom_body:
            return self._active_custom_body
        if self._username:
            return f"https://mc-heads.net/bust/{self._username}/160?t={self._skin_version}"
        return ""

    @Property(str, notify=accountChanged)
    def bodyUrl(self) -> str:
        if self._active_custom_body:
            return self._active_custom_body
        if self._username:
            return f"https://mc-heads.net/body/{self._username}/360?t={self._skin_version}"
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
        target_name = (name or "").strip() or self._active_custom_name or "Mein Skin"
        from backend.services.skin_service import save_skin_to_library
        save_skin_to_library(target_name, target_path, self._active_custom_avatar)
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
