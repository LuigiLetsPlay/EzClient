#!/usr/bin/env python3
"""
EzClient Code Signing Utility
Signs EzClient binaries with Authenticode digital signature using signtool.exe.
"""

import sys
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PFX_PATH = ROOT / "tools" / "EzClient_CodeSign.pfx"
CER_PATH = ROOT / "tools" / "EzClient_CodeSign.cer"
PFX_PASSWORD = "EzClient2026"

SIGNTOOL_CANDIDATES = [
    Path(r"C:\Program Files (x86)\Windows Kits\10\bin\10.0.26100.0\x64\signtool.exe"),
    Path(r"C:\Program Files (x86)\Windows Kits\10\bin\10.0.22621.0\x64\signtool.exe"),
    Path(r"C:\Program Files (x86)\Windows Kits\10\bin\10.0.19041.0\x64\signtool.exe"),
    Path(r"C:\Program Files (x86)\Windows Kits\10\bin\x64\signtool.exe"),
]


def find_signtool() -> Path | None:
    for candidate in SIGNTOOL_CANDIDATES:
        if candidate.is_file():
            return candidate

    # Search dynamically in Windows Kits
    base = Path(r"C:\Program Files (x86)\Windows Kits\10\bin")
    if base.is_dir():
        for st in base.glob("*/x64/signtool.exe"):
            if st.is_file():
                return st

    import shutil
    which = shutil.which("signtool.exe")
    if which:
        return Path(which)

    return None


def ensure_certificate() -> bool:
    if PFX_PATH.is_file() and CER_PATH.is_file():
        return True

    script = ROOT / "tools" / "create_cert.ps1"
    if not script.is_file():
        print(f"[SignTool] Error: {script} not found.")
        return False

    print("[SignTool] Certificate missing, generating new self-signed certificate...")
    res = subprocess.run(
        ["powershell", "-InputFormat", "None", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(script)],
        cwd=ROOT,
        stdin=subprocess.DEVNULL,
        capture_output=True,
        text=True
    )
    if res.returncode != 0:
        print(f"[SignTool] Certificate generation failed:\n{res.stderr}")
        return False
    return PFX_PATH.is_file()


def sign_binary(target_path: Path, description: str | None = None, timestamp: bool = True) -> bool:
    signtool = find_signtool()
    if not signtool:
        print("[SignTool] Warning: signtool.exe not found. Binary left unsigned.")
        return False

    if not ensure_certificate():
        print("[SignTool] Warning: Certificate not available. Binary left unsigned.")
        return False

    if not target_path.is_file():
        print(f"[SignTool] File not found: {target_path}")
        return False

    if description is None:
        description = "EzClient Setup" if "setup" in target_path.name.lower() else "EzClient"

    print(f"[SignTool] Signing {target_path.name} as '{description}' with Authenticode (SHA256)...")

    # Build signtool command
    cmd = [
        str(signtool),
        "sign",
        "/f", str(PFX_PATH),
        "/p", PFX_PASSWORD,
        "/fd", "sha256",
        "/d", description,
        "/du", "https://github.com/LuigiLetsPlay/EzClient",
    ]

    # Add timestamp if requested
    if timestamp:
        # Try timestamp servers: DigiCert RFC 3161
        cmd.extend(["/tr", "http://timestamp.digicert.com", "/td", "sha256"])

    cmd.append(str(target_path))

    res = subprocess.run(cmd, cwd=ROOT, stdin=subprocess.DEVNULL, capture_output=True, text=True)
    if res.returncode == 0:
        print(f"[SignTool] [OK] Successfully signed {target_path.name}")
        return True

    # If failed with timestamp (e.g. network timeout), retry without timestamp
    if timestamp:
        print("[SignTool] Timestamp failed, retrying without timestamp...")
        return sign_binary(target_path, description=description, timestamp=False)

    print(f"[SignTool] Signing failed for {target_path.name}:\n{res.stderr or res.stdout}")
    return False


def main():
    targets = sys.argv[1:]
    if not targets:
        default_targets = [
            ROOT / "dist" / "EzClient.exe",
            ROOT / "dist" / "EzClient-Setup.exe",
        ]
        targets = [str(p) for p in default_targets if p.is_file()]

    if not targets:
        print("[SignTool] No binaries found to sign in dist/")
        return

    success_all = True
    for target in targets:
        path = Path(target)
        if not sign_binary(path):
            success_all = False

    if not success_all:
        sys.exit(1)


if __name__ == "__main__":
    main()
