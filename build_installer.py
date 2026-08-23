"""Build the standalone EzClient 1.6.5 Windows setup executable."""
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent


def build() -> None:
    launcher = ROOT / "dist" / "EzClient.exe"
    if not launcher.exists():
        raise FileNotFoundError("Build the launcher first: python build_exe.py")

    cmd = [
        sys.executable, "-m", "PyInstaller",
        "--name=EzClient-Setup",
        "--onefile", "--windowed", "--clean", "--noconfirm",
        f"--icon={ROOT / 'ui' / 'assets' / 'icon.ico'}",
        f"--add-data={launcher};.",
        f"--add-data={ROOT / 'ui' / 'assets'};ui/assets",
        str(ROOT / "installer" / "installer_gui.py"),
    ]
    subprocess.run(cmd, cwd=ROOT, check=True)


if __name__ == "__main__":
    build()
