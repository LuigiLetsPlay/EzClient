import os
import sys
import shutil
import subprocess
import winreg
from pathlib import Path
from PySide6.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QLabel,
    QPushButton, QProgressBar, QCheckBox, QLineEdit, QFileDialog, QFrame
)
from PySide6.QtCore import Qt, QThread, Signal
from PySide6.QtGui import QFont, QIcon, QPixmap, QColor

APP_NAME = "EzClient"
APP_VERSION = "2.0.1"
GITHUB_REPO = "LuigiLetsPlay/EzClient"
DEFAULT_INSTALL_DIR = Path(os.getenv("LOCALAPPDATA", str(Path.home()))) / "Programs" / "EzClient"


def get_resource_path(relative_path: str) -> Path:
    """Get absolute path to bundled resource."""
    if hasattr(sys, "_MEIPASS"):
        return Path(sys._MEIPASS) / relative_path
    return Path(__file__).resolve().parent.parent / relative_path


def run_silent(cmd: list[str]) -> subprocess.CompletedProcess:
    """Runs a subprocess command in the background with zero visible window or console flash."""
    creationflags = 0
    startupinfo = None
    if sys.platform == "win32":
        creationflags = subprocess.CREATE_NO_WINDOW
        startupinfo = subprocess.STARTUPINFO()
        startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
        startupinfo.wShowWindow = 0  # SW_HIDE

    return subprocess.run(
        cmd,
        capture_output=True,
        creationflags=creationflags,
        startupinfo=startupinfo,
        text=True
    )


def create_windows_shortcut(target: Path, shortcut_path: Path, description: str = "EzClient Minecraft Client") -> None:
    """Creates a Windows .lnk shortcut cleanly without suspicious shell invocations."""
    shortcut_path.parent.mkdir(parents=True, exist_ok=True)
    vbs_content = f'''Set ws = CreateObject("WScript.Shell")
Set s = ws.CreateShortcut("{shortcut_path}")
s.TargetPath = "{target}"
s.WorkingDirectory = "{target.parent}"
s.Description = "{description}"
s.IconLocation = "{target},0"
s.Save
'''
    temp_vbs = Path(os.environ.get("TEMP", ".")) / f"ez_sc_{os.getpid()}.vbs"
    try:
        temp_vbs.write_text(vbs_content, encoding="utf-8")
        subprocess.run(["wscript.exe", str(temp_vbs)], creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0)
    except Exception as e:
        print(f"[Installer] Shortcut creation note: {e}")
    finally:
        if temp_vbs.exists():
            try:
                temp_vbs.unlink()
            except Exception:
                pass


def register_uninstall(install_dir: Path, exe_path: Path) -> None:
    """Registers the application in Windows Add/Remove Programs (HKCU)."""
    try:
        key_path = r"Software\Microsoft\Windows\CurrentVersion\Uninstall\EzClient"
        with winreg.CreateKey(winreg.HKEY_CURRENT_USER, key_path) as key:
            winreg.SetValueEx(key, "DisplayName", 0, winreg.REG_SZ, f"EzClient (v{APP_VERSION})")
            winreg.SetValueEx(key, "DisplayVersion", 0, winreg.REG_SZ, APP_VERSION)
            winreg.SetValueEx(key, "Publisher", 0, winreg.REG_SZ, "Luigi / EzClient")
            winreg.SetValueEx(key, "DisplayIcon", 0, winreg.REG_SZ, f"{exe_path},0")
            winreg.SetValueEx(key, "InstallLocation", 0, winreg.REG_SZ, str(install_dir))
            cmd_uninstall = f'cmd.exe /c "rmdir /s /q \\"{install_dir}\\" & reg delete \\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\EzClient\\" /f"'
            winreg.SetValueEx(key, "UninstallString", 0, winreg.REG_SZ, cmd_uninstall)
            winreg.SetValueEx(key, "NoModify", 0, winreg.REG_DWORD, 1)
            winreg.SetValueEx(key, "NoRepair", 0, winreg.REG_DWORD, 1)
    except Exception as e:
        print(f"[Installer] Registry error: {e}")


def kill_other_ezclient_instances() -> None:
    """Terminates other running EzClient instances cleanly using psutil without PowerShell."""
    my_pid = os.getpid()
    try:
        import psutil
        for proc in psutil.process_iter(['pid', 'name']):
            try:
                pname = proc.info.get('name') or ''
                if 'ezclient' in pname.lower() and proc.info.get('pid') != my_pid:
                    proc.kill()
            except (psutil.NoSuchProcess, psutil.AccessDenied, Exception):
                pass
    except Exception:
        pass


class InstallWorker(QThread):
    progress = Signal(int, str)
    finished = Signal(bool, str)

    def __init__(self, target_dir: Path, create_desktop: bool, create_startmenu: bool):
        super().__init__()
        self.target_dir = target_dir
        self.create_desktop = create_desktop
        self.create_startmenu = create_startmenu

    def run(self):
        try:
            # 1. Terminate any running EzClient instances silently to release file locks
            self.progress.emit(10, "Schließe laufende EzClient-Instanzen…")
            kill_other_ezclient_instances()
            import time
            time.sleep(0.5)

            self.progress.emit(25, "Erstelle Installationsverzeichnis…")
            self.target_dir.mkdir(parents=True, exist_ok=True)

            self.progress.emit(45, "Installiere EzClient.exe…")
            bundled_exe = get_resource_path("EzClient.exe")
            target_exe = self.target_dir / "EzClient.exe"

            # Copy with retry loop in case Windows needs a moment to release file handles
            copied = False
            last_err = None
            for attempt in range(6):
                try:
                    if bundled_exe.exists():
                        shutil.copy2(bundled_exe, target_exe)
                    else:
                        dist_exe = Path(__file__).resolve().parent.parent / "dist" / "EzClient.exe"
                        if dist_exe.exists():
                            shutil.copy2(dist_exe, target_exe)
                    copied = True
                    break
                except Exception as ex:
                    last_err = ex
                    kill_other_ezclient_instances()
                    time.sleep(0.4)

            if not copied and last_err:
                raise last_err

            self.progress.emit(70, "Richte Verknüpfungen ein…")
            if self.create_desktop:
                desktop = Path(os.getenv("USERPROFILE", "")) / "Desktop"
                if desktop.exists():
                    create_windows_shortcut(target_exe, desktop / "EzClient.lnk")

            if self.create_startmenu:
                start_menu = Path(os.getenv("APPDATA", "")) / "Microsoft" / "Windows" / "Start Menu" / "Programs"
                if start_menu.exists():
                    create_windows_shortcut(target_exe, start_menu / "EzClient.lnk")

            self.progress.emit(90, "Registriere App in Windows…")
            register_uninstall(self.target_dir, target_exe)

            self.progress.emit(100, "Erfolgreich aktualisiert / installiert!")
            self.finished.emit(True, str(target_exe))
        except Exception as e:
            self.finished.emit(False, str(e))


class InstallerWindow(QWidget):
    def __init__(self):
        super().__init__()
        
        # Check if launched in update mode (--update / -u)
        self.is_update = False
        target_dir_arg = None
        for arg in sys.argv[1:]:
            if arg in ("--update", "-u"):
                self.is_update = True
            elif arg.startswith("--dir="):
                target_dir_arg = arg.split("=", 1)[1]

        # Check existing installation in registry if not specified
        if not target_dir_arg and self.is_update:
            try:
                with winreg.OpenKey(winreg.HKEY_CURRENT_USER, r"Software\Microsoft\Windows\CurrentVersion\Uninstall\EzClient") as key:
                    target_dir_arg, _ = winreg.QueryValueEx(key, "InstallLocation")
            except Exception:
                pass

        self.initial_dir = Path(target_dir_arg) if target_dir_arg else DEFAULT_INSTALL_DIR

        self.setWindowTitle("EzClient Aktualisierung" if self.is_update else "EzClient Setup")
        self.setFixedSize(520, 320 if self.is_update else 380)
        self.setStyleSheet("""
            QWidget {
                background-color: #0d0e11;
                color: #e6e8eb;
                font-family: 'Segoe UI', Arial, sans-serif;
            }
            QLabel {
                font-size: 13px;
            }
            QPushButton {
                background-color: #24d677;
                color: #06150c;
                font-weight: bold;
                font-size: 13px;
                border-radius: 6px;
                padding: 8px 18px;
                border: none;
            }
            QPushButton:hover {
                background-color: #4eed98;
            }
            QPushButton:pressed {
                background-color: #1cb563;
            }
            QPushButton#secondary {
                background-color: #17191e;
                color: #8b929e;
                border: 1px solid #262930;
            }
            QPushButton#secondary:hover {
                background-color: #21242c;
                color: #e6e8eb;
            }
            QLineEdit {
                background-color: #121418;
                border: 1px solid #262930;
                border-radius: 6px;
                padding: 6px 10px;
                color: #e6e8eb;
            }
            QCheckBox {
                font-size: 12px;
                color: #8b929e;
                spacing: 8px;
            }
            QCheckBox:hover {
                color: #e6e8eb;
            }
            QCheckBox::indicator {
                width: 16px;
                height: 16px;
                border-radius: 4px;
                border: 1px solid #262930;
                background-color: #121418;
            }
            QCheckBox::indicator:checked {
                background-color: #24d677;
                border-color: #24d677;
            }
            QProgressBar {
                background-color: #121418;
                border: 1px solid #262930;
                border-radius: 4px;
                height: 8px;
                text-align: center;
            }
            QProgressBar::chunk {
                background-color: #24d677;
                border-radius: 4px;
            }
        """)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(24, 24, 24, 24)
        layout.setSpacing(14)

        # Header
        header = QHBoxLayout()
        header.setSpacing(12)
        logo_label = QLabel("⚡")
        logo_label.setStyleSheet("font-size: 28px; color: #24d677;")
        header.addWidget(logo_label)

        title_col = QVBoxLayout()
        title_col.setSpacing(2)
        title_text = "EzClient Aktualisierung" if self.is_update else "EzClient Installation"
        title = QLabel(title_text)
        title.setStyleSheet("font-size: 18px; font-weight: bold; color: #ffffff;")
        sub_text = f"Version {APP_VERSION} wird installiert…" if self.is_update else f"Version {APP_VERSION} · High-Performance Minecraft Client"
        subtitle = QLabel(sub_text)
        subtitle.setStyleSheet("font-size: 11px; color: #8b929e;")
        title_col.addWidget(title)
        title_col.addWidget(subtitle)
        header.addLayout(title_col)
        header.addStretch()
        layout.addLayout(header)

        sep = QFrame()
        sep.setFrameShape(QFrame.HLine)
        sep.setStyleSheet("color: #1f2228;")
        layout.addWidget(sep)

        # In update mode, show simple directory info instead of interactive browser
        self.dir_input = QLineEdit(str(self.initial_dir))
        if self.is_update:
            info_label = QLabel(f"Aktualisiere EzClient in: <b>{self.initial_dir}</b>")
            info_label.setStyleSheet("font-size: 12px; color: #8b929e;")
            layout.addWidget(info_label)
        else:
            dir_label = QLabel("Installationsordner:")
            dir_label.setStyleSheet("font-weight: bold; font-size: 12px;")
            layout.addWidget(dir_label)

            dir_row = QHBoxLayout()
            dir_row.setSpacing(8)
            self.browse_btn = QPushButton("Durchsuchen…")
            self.browse_btn.setObjectName("secondary")
            self.browse_btn.clicked.connect(self.browse_dir)
            dir_row.addWidget(self.dir_input)
            dir_row.addWidget(self.browse_btn)
            layout.addLayout(dir_row)

        # Checkboxes (only shown in fresh install mode)
        self.chk_desktop = QCheckBox("Desktop-Verknüpfung erstellen")
        self.chk_desktop.setChecked(True)
        self.chk_startmenu = QCheckBox("Startmenü-Eintrag erstellen")
        self.chk_startmenu.setChecked(True)
        self.chk_launch = QCheckBox("EzClient nach Abschluss starten")
        self.chk_launch.setChecked(True)

        if not self.is_update:
            layout.addWidget(self.chk_desktop)
            layout.addWidget(self.chk_startmenu)
            layout.addWidget(self.chk_launch)

        # Progress
        self.progress_bar = QProgressBar()
        self.progress_bar.setValue(0)
        self.progress_bar.setVisible(self.is_update)
        self.status_label = QLabel("Bereite Update vor…" if self.is_update else "")
        self.status_label.setStyleSheet("font-size: 11px; color: #8b929e;")
        self.status_label.setVisible(self.is_update)
        layout.addWidget(self.status_label)
        layout.addWidget(self.progress_bar)

        layout.addStretch()

        # Action Buttons
        btn_row = QHBoxLayout()
        btn_row.addStretch()
        self.cancel_btn = QPushButton("Abbrechen")
        self.cancel_btn.setObjectName("secondary")
        self.cancel_btn.clicked.connect(self.close)
        self.install_btn = QPushButton("Aktualisieren" if self.is_update else "Installieren")
        self.install_btn.clicked.connect(self.start_install)
        btn_row.addWidget(self.cancel_btn)
        btn_row.addWidget(self.install_btn)
        layout.addLayout(btn_row)

        self.installed_exe = None

        if self.is_update:
            # Auto-start update immediately
            from PySide6.QtCore import QTimer
            QTimer.singleShot(400, self.start_install)

    def browse_dir(self):
        folder = QFileDialog.getExistingDirectory(self, "Installationsordner wählen", self.dir_input.text())
        if folder:
            self.dir_input.setText(str(Path(folder) / "EzClient"))

    def start_install(self):
        target_dir = Path(self.dir_input.text().strip())
        # Lock options & hide install button while in progress
        self.install_btn.setVisible(False)
        self.cancel_btn.setVisible(True)
        self.cancel_btn.setEnabled(True)
        self.dir_input.setEnabled(False)
        if hasattr(self, 'browse_btn'):
            self.browse_btn.setEnabled(False)
        self.chk_desktop.setEnabled(False)
        self.chk_startmenu.setEnabled(False)
        self.chk_launch.setEnabled(False)
        self.progress_bar.setVisible(True)
        self.status_label.setVisible(True)

        self.worker = InstallWorker(target_dir, self.chk_desktop.isChecked(), self.chk_startmenu.isChecked())
        self.worker.progress.connect(self.on_progress)
        self.worker.finished.connect(self.on_finished)
        self.worker.start()

    def on_progress(self, val: int, text: str):
        self.progress_bar.setValue(val)
        self.status_label.setText(text)

    def on_finished(self, success: bool, msg: str):
        if success:
            self.installed_exe = Path(msg)
            self.status_label.setText("✓ EzClient wurde erfolgreich aktualisiert!" if self.is_update else "✓ EzClient wurde erfolgreich installiert!")
            self.status_label.setStyleSheet("font-size: 12px; color: #24d677; font-weight: bold;")
            self.cancel_btn.setVisible(False)
            self.install_btn.setVisible(True)
            self.install_btn.setText("Fertigstellen")
            self.install_btn.setEnabled(True)
            self.install_btn.clicked.disconnect()
            self.install_btn.clicked.connect(self.finish_and_exit)
            if self.is_update:
                from PySide6.QtCore import QTimer
                QTimer.singleShot(800, self.finish_and_exit)
        else:
            self.status_label.setText(f"Fehler: {msg}")
            self.status_label.setStyleSheet("font-size: 11px; color: #ff5555;")
            self.cancel_btn.setText("Schließen")
            self.cancel_btn.setEnabled(True)

    def finish_and_exit(self):
        if self.chk_launch.isChecked() and self.installed_exe and self.installed_exe.exists():
            creationflags = 0
            if sys.platform == "win32":
                creationflags = subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP
            subprocess.Popen([str(self.installed_exe)], creationflags=creationflags, close_fds=True)
        self.close()


def main():
    app = QApplication(sys.argv)
    icon_path = get_resource_path("ui/assets/icon.ico")
    if icon_path.exists():
        app.setWindowIcon(QIcon(str(icon_path)))
    win = InstallerWindow()
    win.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
