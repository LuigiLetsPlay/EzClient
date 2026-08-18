import re
import threading
import urllib.parse
import webbrowser
from PySide6.QtCore import QObject, Signal, Slot, Property, QUrl, Qt
from PySide6.QtWidgets import QDialog, QVBoxLayout
from PySide6.QtWebEngineWidgets import QWebEngineView

from backend.services.msa_auth import (
    get_minecraft_session,
    authenticate_with_authorization_code,
    logout_account,
    MinecraftSession,
    MICROSOFT_AUTH_URL
)


class MicrosoftLoginDialog(QDialog):
    """Native Microsoft Login Dialog with embedded WebEngine."""
    codeReceived = Signal(str)

    def __init__(self, parent=None):
        super().__init__(parent)
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
    Manages the Minecraft account session and direct Microsoft Login.
    """
    accountChanged = Signal()
    loginStatusChanged = Signal(str, bool)
    loginSuccess = Signal(str)

    def __init__(self, parent=None):
        super().__init__(parent)
        self._username = "Player"
        self._account_type = "Microsoft Account"
        self._uuid = ""
        self._skin_url = ""
        self._is_online = False
        self._is_logging_in = False
        self._login_status = ""
        self._login_dialog = None
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
        if self._username:
            return f"https://mc-heads.net/avatar/{self._username}/32"
        return ""

    @Property(str, notify=accountChanged)
    def bustUrl(self) -> str:
        if self._username:
            return f"https://mc-heads.net/bust/{self._username}/160"
        return ""

    @Property(str, notify=accountChanged)
    def bodyUrl(self) -> str:
        if self._username:
            return f"https://mc-heads.net/body/{self._username}/360"
        return ""

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

    @Slot()
    def refresh(self) -> None:
        self._load_account(force_refresh=True)
