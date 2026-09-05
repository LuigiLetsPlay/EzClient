#!/usr/bin/env python3
"""
Build the native Inno Setup Windows installer for EzClient.
"""

import sys
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent

ISCC_CANDIDATES = [
    Path.home() / "AppData" / "Local" / "Programs" / "Inno Setup 6" / "ISCC.exe",
    Path(r"C:\Program Files (x86)\Inno Setup 6\ISCC.exe"),
    Path(r"C:\Program Files\Inno Setup 6\ISCC.exe"),
]


def find_iscc() -> Path | None:
    for candidate in ISCC_CANDIDATES:
        if candidate.is_file():
            return candidate

    which = shutil.which("ISCC.exe") or shutil.which("iscc")
    if which:
        return Path(which)

    return None


def build() -> None:
    launcher = ROOT / "dist" / "EzClient.exe"
    if not launcher.is_file():
        raise FileNotFoundError("Build the launcher first: python build_exe.py")

    iss_file = ROOT / "installer" / "EzClient.iss"
    if not iss_file.is_file():
        raise FileNotFoundError(f"Inno Setup script not found: {iss_file}")

    iscc = find_iscc()
    if not iscc:
        raise RuntimeError(
            "Inno Setup Compiler (ISCC.exe) not found.\n"
            "Please install Inno Setup 6 via: winget install JRSoftware.InnoSetup"
        )

    print("==================================================")
    print("      Building EzClient Native Windows Installer  ")
    print("==================================================")
    print(f"[Installer] Using compiler: {iscc}")
    print(f"[Installer] Compiling script: {iss_file.name} ...")

    cmd = [str(iscc), str(iss_file)]
    res = subprocess.run(cmd, cwd=ROOT / "installer", stdin=subprocess.DEVNULL, capture_output=True, text=True)

    if res.returncode != 0:
        print(f"[Installer] Inno Setup compilation failed:\n{res.stdout}\n{res.stderr}")
        sys.exit(res.returncode)

    setup_exe = ROOT / "dist" / "EzClient-Setup.exe"
    if not setup_exe.is_file():
        raise RuntimeError(f"Expected setup output not found: {setup_exe}")

    size_mb = setup_exe.stat().st_size / (1024 * 1024)
    print(f"[Installer] [OK] Successfully built: {setup_exe.name} ({size_mb:.2f} MB)")

    # Sign the installer with Authenticode
    try:
        from tools.sign_tool import sign_binary
        print("[Installer] Signing installer executable with Authenticode...")
        sign_binary(setup_exe, description="EzClient Setup")
    except Exception as e:
        print(f"[Installer] Note: Could not sign installer: {e}")

    print("==================================================")
    print(f" [SUCCESS] EzClient-Setup.exe is ready!")
    print(f" Location: {setup_exe.resolve()}")
    print("==================================================")


if __name__ == "__main__":
    build()
