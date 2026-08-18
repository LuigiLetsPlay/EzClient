import sys
import os
import ctypes
from pathlib import Path
from PySide6.QtWidgets import QApplication, QSystemTrayIcon, QMenu
from PySide6.QtGui import QIcon, QPixmap, QPainter, QColor, QFont, QFontDatabase
from PySide6.QtQuickControls2 import QQuickStyle
from PySide6.QtQml import QQmlApplicationEngine
from PySide6.QtCore import QUrl, Qt

from backend.models.types import APP_VERSION
from backend.services.store import ProfileStore
from backend.models.profile_model import ProfileModel
from backend.models.mod_model import ModModel
from backend.controllers.profile_controller import ProfileController
from backend.controllers.modrinth_controller import ModrinthController
from backend.controllers.account_controller import AccountController
from backend.controllers.update_controller import UpdateController
from backend.ui_splash import EzSplashScreen


def get_app_root() -> Path:
    if getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS"):
        return Path(sys._MEIPASS)
    return Path(__file__).resolve().parent


def create_app_icon() -> QIcon:
    """Create a crisp desktop app icon for Windows taskbar & titlebar."""
    logo_path = get_app_root() / "ui" / "assets" / "logo.png"
    if logo_path.exists():
        return QIcon(str(logo_path))

    pix = QPixmap(64, 64)
    pix.fill(Qt.transparent)
    painter = QPainter(pix)
    painter.setRenderHint(QPainter.Antialiasing)
    painter.setBrush(QColor("#24D677"))
    painter.setPen(Qt.NoPen)
    painter.drawRoundedRect(4, 4, 56, 56, 16, 16)

    painter.setPen(QColor("#000000"))
    font = QFont("Segoe UI", 28, QFont.Bold)
    painter.setFont(font)
    painter.drawText(pix.rect(), Qt.AlignCenter, "E")
    painter.end()
    return QIcon(pix)


def main() -> None:
    # Ensure Windows single-instance check
    if sys.platform == "win32":
        try:
            ctypes.windll.shell32.SetCurrentProcessExplicitAppUserModelID("EzClient")
            mutex = ctypes.windll.kernel32.CreateMutexW(None, False, "EzClientSingleInstanceMutex_v1")
            if ctypes.windll.kernel32.GetLastError() == 183:  # ERROR_ALREADY_EXISTS
                hwnd = ctypes.windll.user32.FindWindowW(None, "EzClient")
                if hwnd:
                    ctypes.windll.user32.ShowWindow(hwnd, 9)  # SW_RESTORE
                    ctypes.windll.user32.SetForegroundWindow(hwnd)
                sys.exit(0)
        except Exception:
            pass

    QQuickStyle.setStyle("Basic")
    app = QApplication(sys.argv)
    app.setApplicationName("EzClient")
    app.setApplicationDisplayName("EzClient Launcher")
    app.setOrganizationName("EzClient")
    app.setOrganizationDomain("ezclient.app")
    app_icon = create_app_icon()
    app.setWindowIcon(app_icon)

    # ── Instant Splash Screen (Shows within 50ms) ──
    splash = EzSplashScreen(APP_VERSION)
    splash.show()
    app.processEvents()

    # Load essential authentic Minecraft fonts fast
    splash.setMessage("Lade Minecraft Schriften & Assets…", 25)
    app.processEvents()

    fonts_dir = get_app_root() / "ui" / "fonts"
    if fonts_dir.exists():
        for fname in ("Minecraft-Bold.ttf", "MinecraftDefault-Bold.ttf", "MinecraftDefault-Regular.ttf"):
            fpath = fonts_dir / fname
            if fpath.exists():
                QFontDatabase.addApplicationFont(str(fpath))

    # Set crisp Segoe UI as global application font
    app.setFont(QFont("Segoe UI", 10))

    # Backend initialization
    splash.setMessage("Lade Profile & Einstellungen…", 50)
    app.processEvents()

    store = ProfileStore()
    profile_model = ProfileModel()
    mod_model = ModModel()
    profile_controller = ProfileController(store, profile_model, mod_model)
    modrinth_controller = ModrinthController()
    account_controller = AccountController()
    update_controller = UpdateController()

    # QML Engine
    splash.setMessage("Initialisiere Benutzeroberfläche…", 75)
    app.processEvents()

    engine = QQmlApplicationEngine()
    qml_dir = get_app_root() / "ui"
    engine.addImportPath(str(qml_dir))

    # Expose to QML
    engine.rootContext().setContextProperty("profileController", profile_controller)
    engine.rootContext().setContextProperty("modrinthController", modrinth_controller)
    engine.rootContext().setContextProperty("accountController", account_controller)
    engine.rootContext().setContextProperty("updateController", update_controller)

    qml_file = qml_dir / "App.qml"
    engine.load(QUrl.fromLocalFile(str(qml_file)))

    if not engine.rootObjects():
        splash.close()
        sys.exit(-1)

    window = engine.rootObjects()[0]
    from PySide6.QtCore import QSize
    try:
        window.setMinimumSize(QSize(1040, 680))
    except Exception:
        pass

    splash.setMessage("Bereit!", 100)
    app.processEvents()
    splash.close()
    window.show()
    window.raise_()
    window.requestActivate()

    # ── System Tray Icon Setup ──
    tray_icon = QSystemTrayIcon(app_icon, app)
    tray_icon.setToolTip("EzClient Launcher")

    tray_menu = QMenu()
    act_restore = tray_menu.addAction("EzClient anzeigen")
    act_play = tray_menu.addAction("Minecraft spielen")
    tray_menu.addSeparator()
    act_quit = tray_menu.addAction("Beenden")

    def restore_window():
        window.showNormal()
        window.raise_()
        window.requestActivate()

    def hide_window_to_tray():
        window.hide()

    act_restore.triggered.connect(restore_window)
    act_play.triggered.connect(profile_controller.launchActiveProfile)
    act_quit.triggered.connect(app.quit)

    def on_tray_activated(reason):
        if reason in (QSystemTrayIcon.Trigger, QSystemTrayIcon.DoubleClick):
            restore_window()

    tray_icon.activated.connect(on_tray_activated)
    tray_icon.setContextMenu(tray_menu)
    tray_icon.show()

    # Connect launcher lifecycle to system tray & window
    profile_controller.hideToTrayRequested.connect(hide_window_to_tray)
    profile_controller.restoreFromTrayRequested.connect(restore_window)

    sys.exit(app.exec())


if __name__ == "__main__":
    main()
