"""Build the standalone EzClient 1.8.2 Windows setup executable."""
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent


def build() -> None:
    launcher = ROOT / "dist" / "EzClient.exe"
    if not launcher.exists():
        raise FileNotFoundError("Build the launcher first: python build_exe.py")

    hidden_imports = [
        "PySide6.QtCore",
        "PySide6.QtGui",
        "PySide6.QtWidgets",
        "shiboken6",
        "psutil",
    ]

    cmd = [
        sys.executable, "-m", "PyInstaller",
        "--name=EzClient-Setup",
        "--onefile", "--windowed", "--clean", "--noconfirm",
        "--noupx",
        f"--icon={ROOT / 'ui' / 'assets' / 'icon.ico'}",
        f"--add-data={launcher};.",
        f"--add-data={ROOT / 'ui' / 'assets'};ui/assets",
    ]

    for h in hidden_imports:
        cmd.append(f"--hidden-import={h}")

    cmd.append(str(ROOT / "installer" / "installer_gui.py"))
    subprocess.run(cmd, cwd=ROOT, check=True)


if __name__ == "__main__":
    build()
