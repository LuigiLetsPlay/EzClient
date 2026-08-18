import os
import sys
import shutil
import subprocess
from pathlib import Path

def build_installer():
    root = Path(__file__).resolve().parent
    print("==================================================")
    print("       Building EzClient_Setup_x64.exe (Installer)")
    print("==================================================")

    # 1. Compile fresh EzClient.exe
    build_exe_script = root / "build_exe.py"
    subprocess.run([sys.executable, str(build_exe_script)], check=True)

    exe_path = root / "dist" / "EzClient.exe"
    exe_x64 = root / "dist" / "EzClient_x64.exe"
    if exe_path.exists():
        shutil.copy2(exe_path, exe_x64)

    # 2. Build Setup Executable
    icon_path = root / "ui" / "assets" / "icon.ico"
    exe_data = f"{exe_path};."
    icon_data = f"{icon_path};ui/assets"

    cmd = [
        "pyinstaller",
        "--name=EzClient_Setup_x64",
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
        "--exclude-module=PySide6.QtWebEngineWidgets",
        "--exclude-module=PySide6.QtWebEngineCore",
        "--exclude-module=PySide6.QtQuick",
        "--exclude-module=PySide6.QtQml",
        "--exclude-module=PySide6.QtOpenGL",
        "--exclude-module=tkinter",
        "--exclude-module=unittest",
        str(root / "installer" / "installer_gui.py")
    ]

    print("[Installer Build] Running PyInstaller for Setup...")
    res = subprocess.run(cmd, cwd=str(root))
    if res.returncode == 0:
        setup_x64 = root / "dist" / "EzClient_Setup_x64.exe"
        setup_std = root / "dist" / "EzClient-Setup.exe"
        if setup_x64.exists():
            shutil.copy2(setup_x64, setup_std)
            size_mb = round(setup_x64.stat().st_size / (1024 * 1024), 2)
            print("==================================================")
            print(f" [SUCCESS] EzClient_Setup_x64.exe created successfully!")
            print(f" Location: {setup_x64}")
            print(f" Size: {size_mb} MB")
            print("==================================================")
    else:
        print("[Installer Build] Failed with return code:", res.returncode)

if __name__ == "__main__":
    build_installer()
