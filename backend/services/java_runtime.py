"""Automatic installation of the Mojang-required Java runtime."""
from __future__ import annotations

import platform
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path
from typing import Callable


def _machine() -> str:
    machine = platform.machine().lower()
    if machine in {"amd64", "x86_64"}:
        return "x64"
    if machine in {"arm64", "aarch64"}:
        return "aarch64"
    if machine in {"x86", "i386", "i686"}:
        return "x86"
    raise RuntimeError(f"Nicht unterstützte CPU-Architektur: {platform.machine()}")


def _java_executable(runtime_dir: Path) -> Path | None:
    name = "javaw.exe" if sys.platform.startswith("win") else "java"
    matches = list(runtime_dir.rglob(name))
    return matches[0] if matches else None


def _runtime_major(java_exe: Path) -> int | None:
    try:
        result = subprocess.run(
            [str(java_exe), "-XshowSettings:properties", "-version"],
            capture_output=True,
            text=True,
            timeout=15,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
        for line in (result.stdout + result.stderr).splitlines():
            if line.strip().startswith("java.version"):
                value = line.split("=", 1)[1].strip()
                return int(value.split(".", 1)[0])
    except (OSError, subprocess.SubprocessError, ValueError):
        pass
    return None


def install_required_java(
    mc_dir: Path,
    major_version: int,
    notify: Callable[[str], None],
) -> Path:
    """Install Temurin JDK ``major_version`` below the Minecraft directory."""
    runtime_root = mc_dir / "runtime" / f"ezclient-jdk-{major_version}"
    existing = _java_executable(runtime_root)
    if existing and _runtime_major(existing) == major_version:
        return existing

    if sys.platform.startswith("win"):
        system = "windows"
    elif sys.platform == "darwin":
        system = "mac"
    elif sys.platform.startswith("linux"):
        system = "linux"
    else:
        raise RuntimeError(f"Kein automatisches Java-Installationspaket für {sys.platform}.")

    url = (
        "https://api.adoptium.net/v3/binary/latest/"
        f"{major_version}/ga/{system}/{_machine()}/jdk/hotspot/normal/eclipse"
    )
    runtime_root.parent.mkdir(parents=True, exist_ok=True)
    staging_parent = Path(tempfile.mkdtemp(prefix=f"ezclient-jdk-{major_version}-", dir=runtime_root.parent))
    archive_path = staging_parent / "jdk.zip" if system == "windows" else staging_parent / "jdk.tar.gz"
    notify(f"Lade Java {major_version} automatisch herunter…")
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "EzClient/1.6.5"})
        with urllib.request.urlopen(request, timeout=60) as response, archive_path.open("wb") as output:
            total = int(response.headers.get("Content-Length") or 0)
            downloaded = 0
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
                downloaded += len(chunk)
                if total and downloaded % (20 * 1024 * 1024) < 1024 * 1024:
                    notify(f"Java {major_version}: {downloaded / 1024 / 1024:.0f} MB")

        notify(f"Entpacke Java {major_version}…")
        if archive_path.suffix == ".zip":
            with zipfile.ZipFile(archive_path) as archive:
                archive.extractall(staging_parent)
        else:
            subprocess.run(["tar", "-xzf", str(archive_path), "-C", str(staging_parent)], check=True)

        source = None
        for item in staging_parent.iterdir():
            if _java_executable(item):
                source = item
                break
        if not source:
            raise RuntimeError("Das heruntergeladene Java-Archiv ist ungültig.")
        if runtime_root.exists():
            shutil.rmtree(runtime_root)
        source.replace(runtime_root)
    finally:
        shutil.rmtree(staging_parent, ignore_errors=True)

    java_exe = _java_executable(runtime_root)
    if not java_exe or _runtime_major(java_exe) != major_version:
        raise RuntimeError(f"Java {major_version} konnte nicht installiert werden.")
    notify(f"Java {major_version} wurde installiert.")
    return java_exe
