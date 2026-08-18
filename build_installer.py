import os
import sys
import subprocess
from pathlib import Path

def build_installer():
    root = Path(__file__).resolve().parent
    print("==================================================")
    print("       Building EzClient-Setup.exe (Installer)    ")
    print("==================================================")

    # 1. Ensure EzClient.exe is already built
    exe_path = root / "dist" / "EzClient.exe"
    if not exe_path.exists() or exe_path.stat().st_size < 1000000:
        print("[Installer Build] Compiling EzClient.exe first...")
        build_exe_script = root / "build_exe.py"
        subprocess.run([sys.executable, str(build_exe_script)], check=True)

    # 2. Build Setup Executable
    icon_path = root / "ui" / "assets" / "icon.ico"
    exe_data = f"{exe_path};."
    icon_data = f"{icon_path};ui/assets"

    cmd = [
        "pyinstaller",
        "--name=EzClient-Setup",
        "--onefile",
        "--windowed",
        f"--icon={icon_path}",
        f"--add-data={exe_data}",
        f"--add-data={icon_data}",
        "--clean",
        "--noconfirm",
        "--hidden-import=PySide6.QtCore",
        "--hidden-import=PySide6.QtGui",
        "--hidden-import=PySide6.QtWidgets",
        str(root / "installer" / "installer_gui.py")
    ]

    print("[Installer Build] Running PyInstaller for Setup...")
    res = subprocess.run(cmd, cwd=str(root))
    if res.returncode == 0:
        setup_path = root / "dist" / "EzClient-Setup.exe"
        if setup_path.exists():
            size_mb = round(setup_path.stat().st_size / (1024 * 1024), 2)
            print("==================================================")
            print(f" [SUCCESS] EzClient-Setup.exe created successfully!")
            print(f" Location: {setup_path}")
            print(f" Size: {size_mb} MB")
            print("==================================================")
    else:
        print("[Installer Build] Failed with return code:", res.returncode)

if __name__ == "__main__":
    build_installer()
