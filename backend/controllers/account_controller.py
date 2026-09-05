import re
import json
import time
import threading
import urllib.parse
import urllib.request
import webbrowser
import shutil
import struct
import tempfile
from pathlib import Path
from backend.services import cape_community, cape_media
from PySide6.QtCore import QObject, Signal, Slot, Property, QUrl, Qt, QByteArray, QBuffer, QIODevice
from PySide6.QtGui import QImage, QPainter, QTransform
from PySide6.QtWidgets import QDialog, QVBoxLayout

from backend.services.msa_auth import (
    get_minecraft_session,
    authenticate_with_authorization_code,
    logout_account,
    list_saved_accounts,
    activate_saved_account,
    remove_saved_account,
    MinecraftSession,
    MICROSOFT_AUTH_URL
)


from backend.services.skin_service import upload_skin_file, reset_skin_to_default
from backend.models.types import DATA_DIR
from PySide6.QtWidgets import QDialog, QVBoxLayout, QFileDialog


def _strip_png_metadata(raw: bytes) -> bytes:
    """Keep only the strict PNG chunks accepted by the cape API."""
    if not raw.startswith(cape_community.PNG_SIGNATURE):
        return raw
    try:
        pos = len(cape_community.PNG_SIGNATURE)
        clean = [cape_community.PNG_SIGNATURE]
        while pos < len(raw):
            size = struct.unpack(">I", raw[pos:pos + 4])[0]
            end = pos + 12 + size
            if end > len(raw):
                return raw
            kind = raw[pos + 4:pos + 8]
            if kind in (b"IHDR", b"IDAT", b"IEND"):
                clean.append(raw[pos:end])
            pos = end
            if kind == b"IEND":
                break
        return b"".join(clean)
    except (IndexError, struct.error):
        return raw


def _bake_editor_cape(image: QImage, fit_mode: str = "Cover") -> QImage:
    """Convert the portrait editor canvas to a vanilla cape texture with Elytra wings."""
    portrait = image
    aspect = Qt.IgnoreAspectRatio if fit_mode == "Stretch" else Qt.KeepAspectRatioByExpanding
    scaled = portrait.scaled(10, 16, aspect, Qt.SmoothTransformation)
    left = max(0, (scaled.width() - 10) // 2)
    top = max(0, (scaled.height() - 16) // 2)
    visible = scaled.copy(left, top, 10, 16)
    result = QImage(64, 32, QImage.Format_RGBA8888)
    result.fill(Qt.transparent)
    painter = QPainter(result)
    # 1. Cape back face: (1, 1, 10, 16)
    painter.drawImage(1, 1, visible)
    # Cape inner face: (12, 1, 10, 16)
    painter.drawImage(12, 1, visible)
    # Cape top/bottom/sides borders
    painter.drawImage(1, 0, visible.scaled(10, 1))
    painter.drawImage(0, 1, visible.scaled(1, 16))
    painter.drawImage(11, 1, visible.scaled(1, 16))

    # 2. Elytra wings: texOffs(22, 0)
    elytra_scaled = portrait.scaled(10, 20, aspect, Qt.SmoothTransformation)
    e_left = max(0, (elytra_scaled.width() - 10) // 2)
    e_top = max(0, (elytra_scaled.height() - 20) // 2)
    elytra_visible = elytra_scaled.copy(e_left, e_top, 10, 20)
    # Outer wing: (24, 2, 10, 20)
    painter.drawImage(24, 2, elytra_visible)
    # Inner wing: (36, 2, 10, 20)
    painter.drawImage(36, 2, elytra_visible)
    # Wing borders & caps: (22, 0, 24, 22)
    painter.drawImage(22, 2, elytra_visible.scaled(2, 20))
    painter.drawImage(34, 2, elytra_visible.scaled(2, 20))
    painter.drawImage(24, 0, elytra_visible.scaled(20, 2))
    painter.end()
    return result


def _write_hd_cape_preview(image: QImage, editor: bool = False, fit_mode: str = "Cover") -> bool:
    """Write a high-resolution preview texture for the launcher's 3D viewer."""
    rgba = image.convertToFormat(QImage.Format_RGBA8888)
    if not editor and abs((rgba.width() / max(1, rgba.height())) - 2.0) < 0.02:
        preview = rgba
    else:
        portrait = rgba
        aspect = Qt.IgnoreAspectRatio if fit_mode == "Stretch" else Qt.KeepAspectRatioByExpanding
        scaled = portrait.scaled(200, 320, aspect, Qt.SmoothTransformation)
        left = max(0, (scaled.width() - 200) // 2)
        top = max(0, (scaled.height() - 320) // 2)
        portrait_visible = scaled.copy(left, top, 200, 320)
        preview = QImage(1280, 640, QImage.Format_RGBA8888)
        preview.fill(Qt.transparent)
        painter = QPainter(preview)
        # Cape visible face: (20, 20, 200, 320)
        painter.drawImage(20, 20, portrait_visible)
        # Cape inner face: (240, 20, 200, 320)
        painter.drawImage(240, 20, portrait_visible)
        # Elytra wings at 20x scale: (480, 40, 200, 400) and (720, 40, 200, 400)
        elytra_scaled = portrait.scaled(200, 400, aspect, Qt.SmoothTransformation)
        el_left = max(0, (elytra_scaled.width() - 200) // 2)
        el_top = max(0, (elytra_scaled.height() - 400) // 2)
        elytra_vis = elytra_scaled.copy(el_left, el_top, 200, 400)
        painter.drawImage(480, 40, elytra_vis)
        painter.drawImage(720, 40, elytra_vis)
        painter.drawImage(440, 40, elytra_vis.scaled(40, 400))
        painter.drawImage(680, 40, elytra_vis.scaled(40, 400))
        painter.drawImage(480, 0, elytra_vis.scaled(400, 40))
        painter.end()
    encoded = QByteArray()
    buffer = QBuffer(encoded)
    if not buffer.open(QIODevice.WriteOnly) or not preview.save(buffer, "PNG"):
        return False
    target = Path(DATA_DIR) / "cosmetics" / "active_cape_preview.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    try:
        target.write_bytes(_strip_png_metadata(bytes(encoded)))
    except OSError:
        return False
    return True


def _write_hd_upload_atlas(image: QImage, fit_mode: str = "Cover") -> bool:
    """Write a sharp 1024x512 atlas for the community upload.

    The server accepts this size and stores it unchanged.  It uses the same
    normalized vanilla cape UVs as the 64x32 game texture, so the visible back
    face receives 160x256 pixels instead of only 10x16.
    """
    portrait = image.convertToFormat(QImage.Format_RGBA8888)
    aspect = Qt.IgnoreAspectRatio if fit_mode == "Stretch" else Qt.KeepAspectRatioByExpanding
    scaled = portrait.scaled(160, 256, aspect, Qt.SmoothTransformation)
    left = max(0, (scaled.width() - 160) // 2)
    top = max(0, (scaled.height() - 256) // 2)
    visible = scaled.copy(left, top, 160, 256)
    atlas = QImage(1024, 512, QImage.Format_RGBA8888)
    atlas.fill(Qt.transparent)
    painter = QPainter(atlas)
    painter.drawImage(16, 16, visible)
    painter.end()
    encoded = QByteArray()
    buffer = QBuffer(encoded)
    if not buffer.open(QIODevice.WriteOnly) or not atlas.save(buffer, "PNG"):
        return False
    target = Path(DATA_DIR) / "cosmetics" / "pending_cape_upload.png"
    target.parent.mkdir(parents=True, exist_ok=True)
    try:
        target.write_bytes(_strip_png_metadata(bytes(encoded)))
    except OSError:
        return False
    return True


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
    capeMediaPrepared = Signal(str, int, float)
    capePreviewPrepared = Signal(str, int)
    capeAnimationPrepared = Signal(str, int, int, int, int, int, bool)

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
        self._active_community_cape_url = ""
        self._cape_preview_lock = threading.Lock()
        self._cape_preview_pending: tuple[int, str, str] | None = None
        self._cape_preview_worker_running = False
        self._cape_preview_revision = 0
        try:
            marker = Path(DATA_DIR) / "cosmetics" / "active_community_cape.txt"
            if marker.is_file():
                self._active_community_cape_url = marker.read_text(encoding="utf-8").strip()
        except OSError:
            pass

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

    @Property(bool, notify=accountChanged)
    def hasAccount(self) -> bool:
        return bool(self._uuid and self._username and self._username != "Player")

    @Property("QVariantList", notify=accountChanged)
    def accounts(self) -> list[dict]:
        accounts = list_saved_accounts()
        for account in accounts:
            account["avatarUrl"] = f"https://minotar.net/helm/{account['username']}/64.png"
        return accounts

    @Slot(str, result=bool)
    def switchAccount(self, uuid_value: str) -> bool:
        session = activate_saved_account(uuid_value)
        if not session:
            self.loginStatusChanged.emit("Account konnte nicht gewechselt werden.", True)
            return False
        self._username = session.username
        self._uuid = session.uuid
        self._skin_url = session.skin_url
        self._active_custom_path = ""
        self._active_custom_name = ""
        self._active_custom_avatar = ""
        self._active_custom_body = ""
        self._is_online = session.is_online
        self._account_type = "Microsoft Account (Online Verifiziert)" if session.is_online else "Microsoft Account (Lokal)"
        self._skin_version = int(time.time())
        self.accountChanged.emit()
        self.loginStatusChanged.emit(f"Gewechselt zu {session.username}.", False)
        return True

    @Slot(str, result=bool)
    def removeAccount(self, uuid_value: str) -> bool:
        was_active = uuid_value == self._uuid
        if not remove_saved_account(uuid_value):
            return False
        if was_active:
            remaining = list_saved_accounts()
            if remaining:
                return self.switchAccount(str(remaining[0].get("uuid", "")))
            self._username = "Player"
            self._uuid = ""
            self._skin_url = ""
            self._is_online = False
            self._account_type = "Offline"
        self.accountChanged.emit()
        return True

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
        """Local EzClient cape, falling back to the active Mojang cape."""
        import base64
        cape = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
        if not cape.exists():
            session = get_minecraft_session()
            return session.cape_url if session and session.is_online else ""
        try:
            return f"data:image/png;base64,{base64.b64encode(cape.read_bytes()).decode('ascii')}"
        except OSError:
            session = get_minecraft_session()
            return session.cape_url if session and session.is_online else ""

    @Property(str, notify=accountChanged)
    def capePreviewTextureUrl(self) -> str:
        """High-resolution local cape used only by the launcher's 3D preview."""
        import base64
        cape = Path(DATA_DIR) / "cosmetics" / "active_cape_preview.png"
        if not cape.exists():
            return self.capeTextureUrl
        try:
            return f"data:image/png;base64,{base64.b64encode(cape.read_bytes()).decode('ascii')}"
        except OSError:
            return ""

    @Property("QVariantMap", notify=accountChanged)
    def capeAnimationInfo(self) -> dict:
        """Animation metadata shared by every launcher 3D cape preview."""
        directory = Path(DATA_DIR) / "cosmetics" / "active_cape_animation"
        manifest_path = directory / "animation.json"
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            sheet = directory / str(manifest.get("sheet") or "framesheet.png")
            if not sheet.is_file():
                return {}
            return {
                "sheetUrl": QUrl.fromLocalFile(str(sheet)).toString() + f"?v={sheet.stat().st_mtime_ns}",
                "frameCount": max(1, int(manifest.get("frame_count", 1))),
                "fps": max(1, int(manifest.get("fps", 12))),
                "columns": max(1, int(manifest.get("columns", 1))),
                "frameWidth": max(1, int(manifest.get("frame_width", 256))),
                "frameHeight": max(1, int(manifest.get("frame_height", 128))),
                "pingPong": bool(manifest.get("ping_pong", False)),
            }
        except (OSError, ValueError, TypeError, json.JSONDecodeError):
            return {}

    @Property("QVariantList", notify=capeCommunityChanged)
    def communityCapes(self) -> list[dict]:
        return self._community_capes

    @Property(str, notify=capeCommunityChanged)
    def capeCommunityStatus(self) -> str:
        return self._cape_community_status

    @Property(str, notify=capeCommunityChanged)
    def activeCommunityCapeUrl(self) -> str:
        return self._active_community_cape_url

    @Slot()
    def refreshCapeCommunity(self) -> None:
        self._cape_community_status = "Lade Community-Capes …"
        self.capeCommunityChanged.emit()

        def worker() -> None:
            try:
                capes = cape_community.list_capes()
                for cape in capes:
                    cape["imageUrl"] = cape_community.cape_image_url(cape)
                    cape["isAnimated"] = bool(cape.get("is_animated") or cape.get("animation_url"))
                    cape["animationUrl"] = str(cape.get("animation_url") or "")
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
    @Slot(str, str, result=bool)
    def activateCommunityCape(self, image_url: str, animation_url: str = "") -> bool:
        """Download a chosen community cape and make it the local active cape."""
        if not image_url.startswith(("http://", "https://")):
            self.capeCommunityStatusChanged.emit("Ungültige Cape-Adresse.", True)
            return False
        self.capeCommunityStatusChanged.emit("Cape wird synchronisiert …", False)

        def worker() -> None:
            try:
                from backend.models.types import APP_VERSION
                request = urllib.request.Request(image_url, headers={"User-Agent": f"EzClient/{APP_VERSION}"})
                with urllib.request.urlopen(request, timeout=15) as response:
                    raw = response.read(2 * 1024 * 1024 + 1)
                if not cape_community.is_safe_cape_png(raw):
                    raise ValueError("Ungültiges Cape-Bild")
                target = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(raw)
                (target.parent / "active_community_cape.txt").write_text(image_url, encoding="utf-8")
                self._active_community_cape_url = image_url
                community_image = QImage()
                if community_image.loadFromData(raw, "PNG"):
                    _write_hd_cape_preview(community_image, editor=False)

                anim_dir = Path(DATA_DIR) / "cosmetics" / "active_cape_animation"
                if anim_dir.exists():
                    shutil.rmtree(anim_dir)
                if animation_url:
                    animation_request = urllib.request.Request(
                        animation_url, headers={"User-Agent": f"EzClient/{APP_VERSION}"}
                    )
                    with urllib.request.urlopen(animation_request, timeout=20) as response:
                        animation_raw = response.read(8 * 1024 * 1024 + 1)
                    if len(animation_raw) > 8 * 1024 * 1024 or not animation_raw.startswith((b"GIF87a", b"GIF89a")):
                        raise ValueError("Ungültige Cape-Animation")
                    with tempfile.TemporaryDirectory(prefix="ezclient-cape-") as temporary:
                        source = Path(temporary) / "community.gif"
                        source.write_bytes(animation_raw)
                        info = cape_media.probe_media(source)
                        fps = max(1, min(20, round(info.source_fps or 12)))
                        cape_media.generate_frame_sheet(
                            source, anim_dir,
                            cape_media.AnimationOptions(0.0, min(10.0, info.duration), fps, False, None),
                        )

                self.accountChanged.emit()
                self.capeCommunityChanged.emit()
                self.capeCommunityStatusChanged.emit("Cape aktiviert und synchronisiert.", False)
            except Exception as exc:
                self.capeCommunityStatusChanged.emit(f"Cape konnte nicht aktiviert werden: {exc}", True)

        threading.Thread(target=worker, name="EzClient-CapeActivate", daemon=True).start()
        return True

    @Slot(str, result=bool)
    def publishCape(self, title: str) -> bool:
        if not self.isOnline or not self.uuid or self.uuid == "00000000000000000000000000000000":
            self.capeCommunityStatusChanged.emit(
                "Community-Capes erfordern einen verifizierten Microsoft-Account, um Identitätsdiebstahl zu verhindern.", True
            )
            return False

        try:
            clean_title = cape_community.validate_cape_title(title)
        except ValueError as exc:
            self.capeCommunityStatusChanged.emit(str(exc), True)
            return False

        cape = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
        upload_atlas = Path(DATA_DIR) / "cosmetics" / "active_cape_upload.png"
        if upload_atlas.is_file():
            try:
                if cape_community.is_safe_cape_png(upload_atlas.read_bytes()):
                    cape = upload_atlas
            except OSError:
                pass
        if not cape.exists():
            self.capeCommunityStatusChanged.emit("Wähle zuerst ein Cape als PNG aus.", True)
            return False

        anim_gif_bytes: bytes | None = None
        anim_gif_path = Path(DATA_DIR) / "cosmetics" / "active_cape_animation" / "preview.gif"
        if anim_gif_path.is_file():
            try:
                anim_gif_bytes = anim_gif_path.read_bytes()
            except OSError:
                anim_gif_bytes = None

        def worker() -> None:
            try:
                session = get_minecraft_session()
                if not session or not session.is_online or not session.access_token:
                    raise ValueError("Die Minecraft-Sitzung ist abgelaufen. Bitte melde dich erneut an.")
                canonical_uuid = cape_community.normalize_player_uuid(session.uuid)

                tokens_file = Path(DATA_DIR) / "cosmetics" / "cape_tokens.json"
                tokens = {}
                if tokens_file.is_file():
                    try:
                        tokens = json.loads(tokens_file.read_text(encoding="utf-8"))
                    except Exception:
                        tokens = {}

                clean_uuid = canonical_uuid.replace("-", "")
                current_token = tokens.get(clean_uuid, "")

                res = cape_community.upload_cape(
                    cape,
                    session.username,
                    canonical_uuid,
                    clean_title,
                    token=current_token,
                    access_token=session.access_token,
                    anim_gif=anim_gif_bytes,
                )
                if isinstance(res, dict) and res.get("token"):
                    tokens[clean_uuid] = res["token"]
                    tokens_file.parent.mkdir(parents=True, exist_ok=True)
                    tokens_file.write_text(json.dumps(tokens, indent=2), encoding="utf-8")

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

    @Slot(result=str)
    def pickCapeFile(self) -> str:
        file_path, _ = QFileDialog.getOpenFileName(
            None, "Cape-Bild auswählen", "", "Bilder (*.png *.jpg *.jpeg *.webp);;Alle Dateien (*)"
        )
        if not file_path:
            return ""
        image = QImage(file_path)
        if image.isNull() or image.width() < 1 or image.height() < 1 or image.width() > 4096 or image.height() > 4096:
            self.skinUploadStatusChanged.emit("Das Bild kann nicht als Cape verarbeitet werden.", True)
            return ""
        rgba = image.convertToFormat(QImage.Format_RGBA8888)
        cape = _bake_editor_cape(rgba)
        _write_hd_cape_preview(rgba, editor=True)
        target = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        if not cape.save(str(target), "PNG"):
            self.skinUploadStatusChanged.emit("Cape konnte nicht gespeichert werden.", True)
            return ""
        target.write_bytes(_strip_png_metadata(target.read_bytes()))
        if not cape_community.is_safe_cape_png(target.read_bytes()):
            target.unlink(missing_ok=True)
            self.skinUploadStatusChanged.emit("Cape konnte nicht sicher gespeichert werden.", True)
            return ""
        self.accountChanged.emit()
        self.skinUploadStatusChanged.emit("Cape gespeichert und in der Vorschau aktiviert.", False)
        return self.capePreviewTextureUrl

    @Slot(result=str)
    def pickCapeImage(self) -> str:
        """Choose a still or animated cape source; the editor decides how to process it."""
        file_path, _ = QFileDialog.getOpenFileName(
            None,
            "Cape-Datei auswählen",
            "",
            "Cape-Medien (*.png *.jpg *.jpeg *.webp *.gif *.mp4 *.webm);;Alle Dateien (*)",
        )
        return QUrl.fromLocalFile(file_path).toString() if file_path else ""

    @Slot(str, result="QVariantMap")
    def probeCapeMedia(self, source_url: str) -> dict:
        try:
            source = QUrl(source_url).toLocalFile() if source_url.startswith("file:") else source_url
            info = cape_media.probe_media(source)
            thumb_url = QUrl.fromLocalFile(info.thumbnail_path).toString() if info.thumbnail_path else ""
            return {
                "ok": True,
                "duration": info.duration,
                "width": info.width,
                "height": info.height,
                "sourceFps": info.source_fps,
                "thumbnailUrl": thumb_url,
            }
        except Exception as exc:
            self.skinUploadStatusChanged.emit(f"Animation konnte nicht gelesen werden: {exc}", True)
            return {"ok": False}

    @Slot(str, float, float, int, bool, result=bool)
    @Slot(str, float, float, int, bool, str, result=bool)
    def prepareAnimatedCape(self, source_url: str, start: float, end: float, fps: int, ping_pong: bool, crop_part: str = "") -> bool:
        """Convert media off the UI thread and expose its first frame as safe fallback."""
        source = QUrl(source_url).toLocalFile() if source_url.startswith("file:") else source_url
        if not source:
            return False

        crop_box: tuple[float, float, float, float] | None = None
        if crop_part:
            raw_crop = crop_part.replace("Crop|", "").strip()
            parts = [float(p.strip()) for p in raw_crop.split(",") if p.strip()]
            if len(parts) == 4 and all(0.0 <= p <= 1.0 for p in parts) and parts[2] > 0 and parts[3] > 0:
                crop_box = (parts[0], parts[1], parts[2], parts[3])

        def worker() -> None:
            try:
                cosmetics = (Path(DATA_DIR) / "cosmetics").resolve()
                target = (cosmetics / "pending_cape_animation").resolve()
                if target.parent != cosmetics:
                    raise ValueError("Ungültiger Animationspfad.")
                if target.exists():
                    shutil.rmtree(target)

                manifest = cape_media.generate_frame_sheet(
                    source,
                    target,
                    cape_media.AnimationOptions(start, end, fps, ping_pong, crop_box),
                )
                from PIL import Image
                sheet_path = target / manifest.sheet
                with Image.open(sheet_path) as sheet:
                    first_frame = sheet.convert("RGBA").crop((0, 0, manifest.frame_width, manifest.frame_height))
                    fallback = cosmetics / "pending_cape.png"
                    first_frame.save(fallback, "PNG", optimize=True)
                fallback.write_bytes(_strip_png_metadata(fallback.read_bytes()))
                if not cape_community.is_safe_cape_png(fallback.read_bytes()):
                    raise ValueError("Das Animations-Fallback entspricht nicht dem Cape-Format.")
                shutil.copyfile(fallback, cosmetics / "pending_cape_preview.png")
                shutil.copyfile(fallback, cosmetics / "pending_cape_upload.png")
                preview_url = QUrl.fromLocalFile(str(cosmetics / "pending_cape_preview.png")).toString()
                sheet_url = QUrl.fromLocalFile(str(sheet_path)).toString()
                self.capeMediaPrepared.emit(preview_url, manifest.frame_count, manifest.duration)
                self.capeAnimationPrepared.emit(
                    sheet_url,
                    manifest.frame_count,
                    manifest.fps,
                    manifest.columns,
                    manifest.frame_width,
                    manifest.frame_height,
                    manifest.ping_pong,
                )
                self.skinUploadStatusChanged.emit(
                    f"Animation bereit: {manifest.frame_count} Frames bei {manifest.fps} FPS.", False
                )
            except Exception as exc:
                self.skinUploadStatusChanged.emit(f"Animation konnte nicht erstellt werden: {exc}", True)

        threading.Thread(target=worker, name="EzClient-CapeMedia", daemon=True).start()
        return True

    @Slot(str, str, result=str)
    def prepareCapeImage(self, source_url: str, fit_mode: str) -> str:
        """Format a selected image into pending game/preview files."""
        try:
            source = source_url
            if source.startswith("file:///"):
                source = QUrl(source).toLocalFile()
            image = QImage(source)
            if image.isNull() or image.width() < 1 or image.height() < 1 or image.width() > 4096 or image.height() > 4096:
                raise ValueError("Das Bild kann nicht verarbeitet werden.")
            _, _, crop_part = fit_mode.partition("|")
            # The UI always provides a cape-shaped 10:16 selection. Transfer
            # that exact selection to the cape face without a second fit mode.
            mode = "Stretch"
            rgba = image.convertToFormat(QImage.Format_RGBA8888)
            if crop_part:
                try:
                    cx, cy, cw, ch = (max(0.0, min(1.0, float(v))) for v in crop_part.split(","))
                    x = round(cx * rgba.width())
                    y = round(cy * rgba.height())
                    w = max(1, min(rgba.width() - x, round(cw * rgba.width())))
                    h = max(1, min(rgba.height() - y, round(ch * rgba.height())))
                    cropped = rgba.copy(x, y, w, h)
                    if not cropped.isNull():
                        rgba = cropped
                except ValueError:
                    pass
            cape = _bake_editor_cape(rgba, mode)
            if not _write_hd_cape_preview(rgba, editor=True, fit_mode=mode):
                raise ValueError("Die Cape-Vorschau konnte nicht erzeugt werden.")
            if not _write_hd_upload_atlas(rgba, mode):
                raise ValueError("Das hochauflösende Cape konnte nicht erzeugt werden.")

            cosmetics = Path(DATA_DIR) / "cosmetics"
            cosmetics.mkdir(parents=True, exist_ok=True)
            encoded = QByteArray()
            buffer = QBuffer(encoded)
            if not buffer.open(QIODevice.WriteOnly) or not cape.save(buffer, "PNG"):
                raise ValueError("Das Cape konnte nicht formatiert werden.")
            (cosmetics / "pending_cape.png").write_bytes(_strip_png_metadata(bytes(encoded)))
            # Keep the currently equipped cape untouched until the user confirms.
            (cosmetics / "active_cape_preview.png").replace(cosmetics / "pending_cape_preview.png")
            self.skinUploadStatusChanged.emit("Vorschau bereit. Du kannst jetzt hochladen.", False)
            import base64
            preview_raw = (cosmetics / "pending_cape_preview.png").read_bytes()
            return "data:image/png;base64," + base64.b64encode(preview_raw).decode("ascii")
        except (ValueError, OSError) as exc:
            self.skinUploadStatusChanged.emit(str(exc), True)
            return ""

    @Slot(str, str, result=int)
    def requestCapePreview(self, source_url: str, fit_mode: str) -> int:
        """Coalesce live crop requests and render only the latest state off the UI thread."""
        with self._cape_preview_lock:
            self._cape_preview_revision += 1
            revision = self._cape_preview_revision
            self._cape_preview_pending = (revision, source_url, fit_mode)
            if self._cape_preview_worker_running:
                return revision
            self._cape_preview_worker_running = True

        def worker() -> None:
            completed_revision = revision
            completed_preview = ""
            while True:
                with self._cape_preview_lock:
                    job = self._cape_preview_pending
                    self._cape_preview_pending = None
                if job is None:
                    with self._cape_preview_lock:
                        # A request may have arrived between the previous check
                        # and acquiring the lock again.
                        if self._cape_preview_pending is not None:
                            continue
                        self._cape_preview_worker_running = False
                    self.capePreviewPrepared.emit(completed_preview, completed_revision)
                    return

                completed_revision, source, mode = job
                completed_preview = self.prepareCapeImage(source, mode)

                with self._cape_preview_lock:
                    if self._cape_preview_pending is None:
                        self._cape_preview_worker_running = False
                        should_finish = True
                    else:
                        should_finish = False
                if should_finish:
                    self.capePreviewPrepared.emit(completed_preview, completed_revision)
                    return

        threading.Thread(target=worker, name="EzClient-CapePreview", daemon=True).start()
        return revision

    @Slot()
    def cancelPendingCape(self) -> None:
        cosmetics = Path(DATA_DIR) / "cosmetics"
        (cosmetics / "pending_cape.png").unlink(missing_ok=True)
        (cosmetics / "pending_cape_preview.png").unlink(missing_ok=True)
        (cosmetics / "pending_cape_upload.png").unlink(missing_ok=True)
        pending_animation = (cosmetics / "pending_cape_animation").resolve()
        if pending_animation.parent == cosmetics.resolve() and pending_animation.exists():
            shutil.rmtree(pending_animation)
        self.skinUploadStatusChanged.emit("Auswahl verworfen.", False)

    @Slot(str, result=bool)
    def confirmPendingCape(self, title: str) -> bool:
        """Promote the previewed image and publish it to the community."""
        cosmetics = Path(DATA_DIR) / "cosmetics"
        pending = cosmetics / "pending_cape.png"
        target = cosmetics / "active_cape.png"
        if not pending.is_file():
            self.skinUploadStatusChanged.emit("Kein neues Cape ausgewählt.", True)
            return False
        pending.replace(target)
        pending_preview = cosmetics / "pending_cape_preview.png"
        if pending_preview.exists():
            pending_preview.replace(cosmetics / "active_cape_preview.png")
        pending_upload = cosmetics / "pending_cape_upload.png"
        if pending_upload.exists():
            pending_upload.replace(cosmetics / "active_cape_upload.png")
        pending_animation = (cosmetics / "pending_cape_animation").resolve()
        active_animation = (cosmetics / "active_cape_animation").resolve()
        if pending_animation.parent == cosmetics.resolve() and pending_animation.exists():
            if active_animation.exists():
                shutil.rmtree(active_animation)
            pending_animation.replace(active_animation)
        elif active_animation.exists():
            shutil.rmtree(active_animation)
        (cosmetics / "active_community_cape.txt").unlink(missing_ok=True)
        self._active_community_cape_url = ""
        self.accountChanged.emit()
        self.capeCommunityChanged.emit()
        return self.publishCape(title)

    @Slot(result=bool)
    def resetCustomCape(self) -> bool:
        """Remove only EzClient's local override so Minecraft can use Mojang's cape."""
        try:
            cosmetics = Path(DATA_DIR) / "cosmetics"
            for name in (
                "active_cape.png", "active_cape_preview.png", "active_cape_upload.png",
                "active_community_cape.txt",
            ):
                (cosmetics / name).unlink(missing_ok=True)
            animation = (cosmetics / "active_cape_animation").resolve()
            if animation.parent == cosmetics.resolve() and animation.exists():
                shutil.rmtree(animation)
            self._active_community_cape_url = ""
            self.accountChanged.emit()
            self.capeCommunityChanged.emit()
            self.skinUploadStatusChanged.emit("EzClient-Cape entfernt – Mojang-Cape wird wieder verwendet.", False)
            session = get_minecraft_session()
            if session and session.is_online and session.uuid:
                tokens_file = cosmetics / "cape_tokens.json"
                try:
                    tokens = json.loads(tokens_file.read_text(encoding="utf-8")) if tokens_file.is_file() else {}
                except (OSError, json.JSONDecodeError):
                    tokens = {}
                token = str(tokens.get(session.uuid.replace("-", ""), ""))
                if token:
                    threading.Thread(
                        target=self._deactivate_server_cape,
                        args=(session.uuid, token, session.access_token),
                        name="EzClient-CapeReset",
                        daemon=True,
                    ).start()
            return True
        except OSError as exc:
            self.skinUploadStatusChanged.emit(f"Cape konnte nicht zurückgesetzt werden: {exc}", True)
            return False

    def _deactivate_server_cape(self, owner_uuid: str, token: str, access_token: str) -> None:
        try:
            cape_community.deactivate_cape(owner_uuid, token, access_token)
            self.capeCommunityStatusChanged.emit("Cape-Synchronisierung zurückgesetzt.", False)
        except Exception as exc:
            self.capeCommunityStatusChanged.emit(f"Server-Cape konnte nicht zurückgesetzt werden: {exc}", True)

    @Slot(result=bool)
    def exportCapeFile(self) -> bool:
        cape = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
        if not cape.is_file():
            self.skinUploadStatusChanged.emit("Kein aktives EzClient-Cape zum Exportieren vorhanden.", True)
            return False
        target, _ = QFileDialog.getSaveFileName(None, "Cape exportieren", "EzClient-Cape.png", "PNG Bild (*.png)")
        if not target:
            return False
        try:
            shutil.copy2(cape, target)
            self.skinUploadStatusChanged.emit("Cape als PNG exportiert.", False)
            return True
        except OSError as exc:
            self.skinUploadStatusChanged.emit(f"Cape-Export fehlgeschlagen: {exc}", True)
            return False

    @Slot(str, result=bool)
    def saveCapeDataUrl(self, data_url: str) -> bool:
        """Persist a Cape Editor canvas only after the same strict PNG validation."""
        try:
            import base64
            prefix = "data:image/png;base64,"
            if not data_url.startswith(prefix):
                raise ValueError("Ungültiges Cape-Format")
            raw = base64.b64decode(data_url[len(prefix):], validate=True)
            image = QImage()
            if not image.loadFromData(raw, "PNG") or image.isNull():
                raise ValueError("Das Editor-Bild konnte nicht gelesen werden")
            rgba = image.convertToFormat(QImage.Format_RGBA8888)
            normalized = _bake_editor_cape(rgba)
            _write_hd_cape_preview(rgba, editor=True)
            encoded = QByteArray()
            buffer = QBuffer(encoded)
            if not buffer.open(QIODevice.WriteOnly) or not normalized.save(buffer, "PNG"):
                raise ValueError("Das Cape konnte nicht als PNG kodiert werden")
            clean = _strip_png_metadata(bytes(encoded))
            if not cape_community.is_safe_cape_png(clean):
                raise ValueError("Das normalisierte Cape entspricht nicht dem sicheren Format")
            target = Path(DATA_DIR) / "cosmetics" / "active_cape.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(clean)
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
