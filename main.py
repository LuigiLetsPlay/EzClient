import sys
import os
import ctypes
from pathlib import Path

# PyInstaller bootloader splash support
try:
    import pyi_splash
except ImportError:
    pyi_splash = None

from PySide6.QtWidgets import QApplication
from PySide6.QtGui import QIcon, QPixmap, QPainter, QColor, QFont
from PySide6.QtCore import QUrl, Qt, QTimer, QSize

from backend.models.types import APP_VERSION
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


def force_window_to_front(hwnd: int) -> None:
    """Bypasses Windows foreground lock policy and brings window to absolute top."""
    if sys.platform != "win32" or not hwnd:
        return
    try:
        user32 = ctypes.windll.user32
        kernel32 = ctypes.windll.kernel32
        
        user32.ShowWindow(hwnd, 9)  # SW_RESTORE
        
        fg_hwnd = user32.GetForegroundWindow()
        fg_thread = user32.GetWindowThreadProcessId(fg_hwnd, None)
        curr_thread = kernel32.GetCurrentThreadId()
        
        if fg_thread != curr_thread and fg_thread != 0:
            user32.AttachThreadInput(curr_thread, fg_thread, True)
            user32.BringWindowToTop(hwnd)
            user32.SetForegroundWindow(hwnd)
            user32.SetFocus(hwnd)
            user32.AttachThreadInput(curr_thread, fg_thread, False)
        else:
            user32.BringWindowToTop(hwnd)
            user32.SetForegroundWindow(hwnd)
            user32.SetFocus(hwnd)
    except Exception as e:
        print(f"[FocusHelper] {e}")


def main() -> None:
    # ── 1. Single Instance Protection ──
    if sys.platform == "win32":
        try:
            ctypes.windll.shell32.SetCurrentProcessExplicitAppUserModelID("EzClient")
            mutex = ctypes.windll.kernel32.CreateMutexW(None, False, "EzClientSingleInstanceMutex_v1")
            if ctypes.windll.kernel32.GetLastError() == 183:  # ERROR_ALREADY_EXISTS
                hwnd = ctypes.windll.user32.FindWindowW(None, "EzClient")
                if hwnd:
                    force_window_to_front(hwnd)
                if pyi_splash:
                    pyi_splash.close()
                sys.exit(0)
        except Exception:
            pass

    # ── 2. Create Application & Show Splash Screen Immediately (<20ms) ──
    app = QApplication(sys.argv)
    app.setApplicationName("EzClient")
    app.setApplicationDisplayName("EzClient Launcher")
    app.setOrganizationName("EzClient")
    app.setOrganizationDomain("ezclient.app")

    # Set default UI font
    app.setFont(QFont("Segoe UI", 10))

    # Show instant splash screen
    splash = EzSplashScreen(APP_VERSION)
    splash.show()
    app.processEvents()

    # Close PyInstaller bootloader splash if it was active
    if pyi_splash:
        try:
            pyi_splash.close()
        except Exception:
            pass

    # ── 3. Initialize Render Engines & Style (20%) ──
    splash.setMessage("Initialisiere Grafik- & Render-Engine…", 20)
    app.processEvents()

    from PySide6.QtQuickControls2 import QQuickStyle
    QQuickStyle.setStyle("Basic")
    try:
        import PySide6.QtWebEngineQuick
        PySide6.QtWebEngineQuick.QtWebEngineQuick.initialize()
    except Exception as e:
        print(f"[WebEngineQuick] Init warning: {e}")

    # Set app window icon
    app_icon = create_app_icon()
    app.setWindowIcon(app_icon)

    # ── 4. Load Custom Fonts & Assets (40%) ──
    splash.setMessage("Lade Minecraft Schriften & Assets…", 40)
    app.processEvents()

    from PySide6.QtGui import QFontDatabase
    fonts_dir = get_app_root() / "ui" / "fonts"
    if fonts_dir.exists():
        for fname in ("Minecraft-Bold.ttf", "MinecraftDefault-Bold.ttf", "MinecraftDefault-Regular.ttf"):
            fpath = fonts_dir / fname
            if fpath.exists():
                QFontDatabase.addApplicationFont(str(fpath))

    # ── 5. Initialize Backend Services & Controllers (60%) ──
    splash.setMessage("Lade Profile & Einstellungen…", 60)
    app.processEvents()

    from backend.services.store import ProfileStore
    from backend.models.profile_model import ProfileModel
    from backend.models.mod_model import ModModel
    from backend.controllers.profile_controller import ProfileController
    from backend.controllers.modrinth_controller import ModrinthController
    from backend.controllers.account_controller import AccountController
    from backend.controllers.update_controller import UpdateController

    store = ProfileStore()
    profile_model = ProfileModel()
    mod_model = ModModel()
    profile_controller = ProfileController(store, profile_model, mod_model)
    modrinth_controller = ModrinthController(profile_controller=profile_controller)
    account_controller = AccountController()
    update_controller = UpdateController()

    # ── 6. Setup QML Application Engine (80%) ──
    splash.setMessage("Initialisiere Benutzeroberfläche…", 80)
    app.processEvents()

    from PySide6.QtQml import QQmlApplicationEngine
    engine = QQmlApplicationEngine()
    qml_dir = get_app_root() / "ui"
    engine.addImportPath(str(qml_dir))

    # Expose to QML
    engine.rootContext().setContextProperty("profileController", profile_controller)
    engine.rootContext().setContextProperty("modrinthController", modrinth_controller)
    engine.rootContext().setContextProperty("accountController", account_controller)
    engine.rootContext().setContextProperty("updateController", update_controller)

    # ── 7. Load App.qml (95%) ──
    splash.setMessage("Lade Launcher-Komponenten…", 95)
    app.processEvents()

    qml_file = qml_dir / "App.qml"
    engine.load(QUrl.fromLocalFile(str(qml_file)))

    if not engine.rootObjects():
        splash.close()
        sys.exit(-1)

    window = engine.rootObjects()[0]
    try:
        window.setMinimumSize(QSize(1040, 680))
    except Exception:
        pass

    # ── 8. Show Main Window & Smooth Splash Transition ──
    splash.setMessage("Bereit!", 100)
    app.processEvents()

    window.show()
    window.raise_()
    window.requestActivate()
    splash.close()

    # ── 9. Deferred Non-Critical Setup (System Tray & Discord RPC) ──
    def setup_deferred_services():
        try:
            from PySide6.QtWidgets import QSystemTrayIcon, QMenu
            tray_icon = QSystemTrayIcon(app_icon, app)
            tray_icon.setToolTip("EzClient Launcher")

            tray_menu = QMenu()
            act_restore = tray_menu.addAction("EzClient anzeigen")
            act_play = tray_menu.addAction("Minecraft spielen")
            tray_menu.addSeparator()
            act_quit = tray_menu.addAction("Beenden")

            def restore_window():
                window.show()
                window.showNormal()
                window.raise_()
                window.requestActivate()
                if sys.platform == "win32":
                    try:
                        hwnd = int(window.winId())
                        force_window_to_front(hwnd)
                    except Exception:
                        pass

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

            # Store reference on app to prevent garbage collection
            app._tray_icon = tray_icon
        except Exception as e:
            print(f"[Tray] Init error: {e}")

        # Initialize Discord RPC in background
        try:
            from backend.services import discord_service
            discord_service.init_rpc()
        except Exception as e:
            print(f"[Main] Discord RPC init failed: {e}")

    # Run deferred setup after main window is rendered and active
    QTimer.singleShot(250, setup_deferred_services)

    sys.exit(app.exec())


if __name__ == "__main__":
    main()

