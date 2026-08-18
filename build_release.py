"""
EzClient v2.0.0 Release Builder
Packages EzClient into a standalone Windows executable.
"""
import sys
import subprocess
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def build():
    print("==========================================")
    print("   Building EzClient v2.0.0 Release...    ")
    print("==========================================")

    ico_path = ROOT / "ui" / "assets" / "icon.ico"
    ui_data = f"{ROOT / 'ui'};ui"
    backend_data = f"{ROOT / 'backend'};backend"

    cmd = [
        sys.executable, "-m", "PyInstaller",
        "--name=EzClient",
        "--onedir",
        "--windowed",
        "--noconfirm",
        "--clean",
        f"--icon={ico_path}",
        f"--add-data={ui_data}",
        f"--add-data={backend_data}",
        str(ROOT / "main.py")
    ]

    print(f"Running: {' '.join(cmd)}")
    res = subprocess.run(cmd, cwd=str(ROOT))

    if res.returncode == 0:
        print("\n==========================================")
        print(" [SUCCESS] Release build finished!")
        print(f" Executable: {ROOT / 'dist' / 'EzClient' / 'EzClient.exe'}")
        print("==========================================")
    else:
        print("\n[ERROR] Build failed with exit code:", res.returncode)

if __name__ == "__main__":
    build()
