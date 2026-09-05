"""Automatic installation of the Mojang-required Java runtime."""
from __future__ import annotations

import platform
import os
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path
from typing import Callable

SUPPORTED_JAVA_MAJORS = (8, 16, 17, 21, 25)


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
                parts = value.split(".")
                return int(parts[1] if parts[0] == "1" and len(parts) > 1 else parts[0])
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
                    downloaded_mb = downloaded / 1024 / 1024
                    total_mb = total / 1024 / 1024
                    notify(f"Java {major_version}: {downloaded_mb:.0f}/{total_mb:.0f} MB")

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


def managed_runtime_root(mc_dir: Path, major_version: int) -> Path:
    return mc_dir / "runtime" / f"ezclient-jdk-{major_version}"


def runtime_statuses(mc_dir: Path) -> list[dict]:
    detected: dict[int, Path] = {}
    path_java = shutil.which("javaw") or shutil.which("java")
    candidates = [Path(path_java)] if path_java else []
    if sys.platform.startswith("win"):
        for environment_name in ("ProgramFiles", "ProgramFiles(x86)"):
            base_value = os.environ.get(environment_name)
            if not base_value:
                continue
            base = Path(base_value)
            for vendor in ("Eclipse Adoptium", "Java", "Microsoft", "Zulu"):
                vendor_dir = base / vendor
                if vendor_dir.is_dir():
                    candidates.extend(vendor_dir.glob("*/bin/javaw.exe"))
    for candidate in candidates:
        major = _runtime_major(candidate)
        if major in SUPPORTED_JAVA_MAJORS and major not in detected:
            detected[major] = candidate

    statuses = []
    for major in SUPPORTED_JAVA_MAJORS:
        root = managed_runtime_root(mc_dir, major)
        executable = _java_executable(root) if root.exists() else None
        managed = bool(executable and _runtime_major(executable) == major)
        selected = executable if managed else detected.get(major)
        statuses.append({
            "major": major,
            "installed": bool(selected),
            "managed": managed,
            "path": str(selected or root),
            "label": f"Java {major}",
        })
    return statuses


def delete_managed_java(mc_dir: Path, major_version: int) -> bool:
    if major_version not in SUPPORTED_JAVA_MAJORS:
        raise ValueError(f"Unsupported Java runtime: {major_version}")
    root = managed_runtime_root(mc_dir, major_version).resolve()
    runtime_parent = (mc_dir / "runtime").resolve()
    if root.parent != runtime_parent or root.name != f"ezclient-jdk-{major_version}":
        raise RuntimeError("Unsafe Java runtime path")
    if not root.exists():
        return False
    shutil.rmtree(root)
    return True
