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

    # Rebuild and package only the actively maintained, exact 26.x artifacts.
    print("[Build] Compiling actively maintained EzClient 26.x JARs first...")
    build_mod_script = root / "client_mod" / "build_mod.py"
    subprocess.run([sys.executable, str(build_mod_script)], check=True)

    # 2. Prepare PyInstaller command
    icon_path = root / "ui" / "assets" / "icon.ico"
    ui_data = f"{root / 'ui'};ui"
    from backend.models.types import APP_VERSION
    release_assets = [root / "backend" / "assets" / f"EzClient-{APP_VERSION}+{version}.jar"
                      for version in ("26.1", "26.1.1", "26.2")]
    for asset in release_assets:
        if not asset.is_file():
            raise FileNotFoundError(f"Required exact release artifact missing: {asset}")

    hidden_imports = [
        "PySide6.QtCore",
        "PySide6.QtGui",
        "PySide6.QtWidgets",
        "PySide6.QtQml",
        "PySide6.QtQuick",
        "PySide6.QtQuickControls2",
        "PySide6.QtNetwork",
        "PySide6.QtSvg",
        "PySide6.QtWebEngineQuick",
        "PySide6.QtWebEngineCore",
        "PySide6.QtWebChannel",
        "PySide6.QtMultimedia",
        "PySide6.QtOpenGL",
        "PySide6.QtOpenGLWidgets",
        "backend.models.types",
        "backend.models.profile_model",
        "backend.models.mod_model",
        "backend.services.store",
        "backend.services.minecraft",
        "backend.services.direct_launch",
        "backend.services.mod_downloader",
        "backend.services.curseforge",
        "backend.services.mod_scanner",
        "backend.services.skin_service",
        "backend.services.live_log_service",
        "backend.services.updater",
        "backend.controllers.profile_controller",
        "backend.controllers.modrinth_controller",
        "backend.controllers.account_controller",
        "backend.controllers.update_controller",
        "backend.ui_splash",
        "shiboken6",
        "psutil",
    ]

    # We use our own QWidget-based splash screen in main.py instead.

    cmd = [
        "pyinstaller",
        "--name=EzClient",
        "--onedir",
        "--windowed",
        "--noupx",
        f"--icon={icon_path}",
        f"--version-file={root / 'file_version_info.txt'}",
        f"--add-data={ui_data}",
        "--clean",
        "--noconfirm",
    ]
    cmd.extend(f"--add-data={asset};backend/assets" for asset in release_assets)

    for h in hidden_imports:
        cmd.append(f"--hidden-import={h}")

    cmd.append(str(root / "main.py"))

    print("[Build] Running PyInstaller command:")
    print(" ".join(cmd))
    res = subprocess.run(cmd, cwd=root)

    if res.returncode != 0:
        print(f"[Build] PyInstaller failed with exit code {res.returncode}")
        sys.exit(res.returncode)

    dist_dir = root / "dist" / "EzClient"
    dist_exe = dist_dir / "EzClient.exe"
    runtime_dir = dist_dir / "_internal"

    # Ensure shiboken6 and C++ runtime DLLs are mirrored into _internal and PySide6
    # so the Windows dynamic loader finds them without depending on system PATH
    shiboken_src = runtime_dir / "shiboken6"
    pyside_target = runtime_dir / "PySide6"
    if shiboken_src.is_dir():
        for dll in shiboken_src.glob("*.dll"):
            shutil.copy2(dll, runtime_dir / dll.name)
            if pyside_target.is_dir():
                shutil.copy2(dll, pyside_target / dll.name)

    if dist_exe.exists():
        try:
            from tools.sign_tool import sign_binary
            print("[Build] Signing EzClient.exe with Authenticode...")
            sign_binary(dist_exe, description="EzClient")
        except Exception as e:
            print(f"[Build] Note: Could not sign EzClient.exe: {e}")

        # Create portable ZIP archive
        from backend.models.types import APP_VERSION
        portable_zip_name = f"EzClient-v{APP_VERSION}-Windows-Portable"
        print(f"[Build] Creating portable ZIP archive: {portable_zip_name}.zip ...")
        zip_path = shutil.make_archive(
            str(root / "dist" / portable_zip_name),
            "zip",
            root_dir=root / "dist",
            base_dir="EzClient"
        )
        zip_size_mb = Path(zip_path).stat().st_size / (1024 * 1024)

        print("==================================================")
        print(f" [SUCCESS] EzClient production build ready!")
        print(f" Folder:   {dist_dir.resolve()}")
        print(f" Launcher: {dist_exe.resolve()}")
        print(f" Portable: {Path(zip_path).name} ({zip_size_mb:.2f} MB)")
        print("==================================================")
    else:
        print("[Build] Warning: Output exe not found in dist/EzClient/")

if __name__ == "__main__":
    build_exe()
