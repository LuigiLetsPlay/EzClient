import os
import sys
import subprocess
import shutil
from pathlib import Path

def build_exe():
    root = Path(__file__).resolve().parent
    print("==================================================")
    print("       Building EzClient Standalone Windows .exe  ")
    print("==================================================")

    # 1. Ensure EzClient.jar is up to date
    jar_path = root / "backend" / "assets" / "EzClient.jar"
    if not jar_path.exists() or jar_path.stat().st_size < 1000:
        print("[Build] Compiling EzClient.jar first...")
        build_mod_script = root / "client_mod" / "build_mod.py"
        subprocess.run([sys.executable, str(build_mod_script)], check=True)

    # 2. Prepare PyInstaller command
    icon_path = root / "ui" / "assets" / "icon.ico"
    ui_data = f"{root / 'ui'};ui"
    assets_data = f"{root / 'backend' / 'assets'};backend/assets"

    hidden_imports = [
        "PySide6.QtCore",
        "PySide6.QtGui",
        "PySide6.QtWidgets",
        "PySide6.QtQml",
        "PySide6.QtQuick",
        "PySide6.QtQuickControls2",
        "PySide6.QtNetwork",
        "PySide6.QtSvg",
        "backend.models.types",
        "backend.models.profile_model",
        "backend.models.mod_model",
        "backend.services.store",
        "backend.services.minecraft",
        "backend.services.direct_launch",
        "backend.services.mod_downloader",
        "backend.services.modrinth",
        "backend.services.updater",
        "backend.controllers.profile_controller",
        "backend.controllers.modrinth_controller",
        "backend.controllers.account_controller",
        "backend.controllers.update_controller",
    ]

    cmd = [
        "pyinstaller",
        "--name=EzClient",
        "--onefile",
        "--windowed",
        f"--icon={icon_path}",
        f"--add-data={ui_data}",
        f"--add-data={assets_data}",
        "--clean",
        "--noconfirm",
    ]

    for h in hidden_imports:
        cmd.append(f"--hidden-import={h}")

    cmd.append(str(root / "main.py"))

    print("[Build] Running PyInstaller command:")
    print(" ".join(cmd))
    res = subprocess.run(cmd, cwd=root)

    if res.returncode != 0:
        print(f"[Build] PyInstaller failed with exit code {res.returncode}")
        sys.exit(res.returncode)

    dist_exe = root / "dist" / "EzClient.exe"
    if dist_exe.exists():
        size_mb = dist_exe.stat().st_size / (1024 * 1024)
        print("==================================================")
        print(f" [SUCCESS] EzClient.exe created successfully!")
        print(f" Location: {dist_exe.resolve()}")
        print(f" Size: {size_mb:.2f} MB")
        print("==================================================")
    else:
        print("[Build] Warning: Output exe not found in dist/")

if __name__ == "__main__":
    build_exe()
