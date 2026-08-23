import os
import os
import sys
import subprocess
import shutil
import tempfile
from pathlib import Path
from typing import Callable, Any
from urllib.request import urlopen
from backend.models.types import ProfileData, now_iso
from backend.services.store import read_json, write_json

MINECRAFT_INSTALLER_URL = "https://launcher.mojang.com/download/MinecraftInstaller.msi"
FABRIC_META_URL = "https://meta.fabricmc.net/v2/versions/installer"
FABRIC_FALLBACK_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar"
_launcher_install_process: subprocess.Popen | None = None

def minecraft_dir() -> Path:
    if sys.platform.startswith("win") and os.environ.get("APPDATA"):
        return Path(os.environ["APPDATA"]) / ".minecraft"
    if sys.platform == "darwin":
        return Path.home() / "Library" / "Application Support" / "minecraft"
    return Path.home() / ".minecraft"

def launcher_candidates() -> list[Path]:
    if not sys.platform.startswith("win"):
        return [Path("/Applications/Minecraft Launcher.app")] if sys.platform == "darwin" else [Path("/usr/bin/minecraft-launcher")]
    local = os.environ.get("LOCALAPPDATA", "")
    program = os.environ.get("PROGRAMFILES", "")
    program_x86 = os.environ.get("PROGRAMFILES(X86)", "")
    return [
        Path(local) / "Programs/Minecraft Launcher/MinecraftLauncher.exe",
        Path(local) / "Minecraft Launcher/MinecraftLauncher.exe",
        Path(program) / "Minecraft Launcher/MinecraftLauncher.exe",
        Path(program_x86) / "Minecraft Launcher/MinecraftLauncher.exe",
    ]

def detect_launcher() -> tuple[bool, Path | None, bool]:
    for path in launcher_candidates():
        if path.exists():
            return True, path, False
    if sys.platform.startswith("win"):
        package = Path(os.environ.get("LOCALAPPDATA", "")) / "Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe"
        if package.exists():
            return True, None, True
    return False, None, False

def java_path() -> str:
    if os.environ.get("JAVA_HOME"):
        cand = Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if sys.platform.startswith("win") else "java")
        if cand.exists():
            return str(cand)
    return shutil.which("java") or ""

def fabric_version(mc_version: str) -> str:
    versions = minecraft_dir() / "versions"
    candidates = [
        path for path in versions.glob(f"fabric-loader-*-{mc_version}") if path.is_dir()
    ] if versions.exists() else []
    if not candidates:
        return f"fabric-loader-0.16.10-{mc_version}"
    return max(candidates, key=lambda p: p.stat().st_mtime).name

def patch_profile_file(path: Path, profile: ProfileData, version_id: str) -> None:
    data = read_json(path, {})
    if not isinstance(data, dict):
        data = {}
    profiles = data.get("profiles") if isinstance(data.get("profiles"), dict) else {}
    profiles["EzClient"] = {
        "name": f"EzClient - {profile.name}", "type": "custom", "created": profile.created,
        "lastUsed": now_iso(), "icon": "Grass", "lastVersionId": version_id,
        "gameDir": str(profile.path),
    }
    data["profiles"] = profiles
    data["selectedProfile"] = "EzClient"
    data["selectedUser"] = "EzClient"
    settings = data.get("settings") if isinstance(data.get("settings"), dict) else {}
    settings["showModded"] = True
    data["settings"] = settings
    write_json(path, data)

def patch_launcher_profile(profile: ProfileData) -> None:
    from backend.services.store import preseed_optimized_profile_settings
    preseed_optimized_profile_settings(profile.path)
    version_id = fabric_version(profile.minecraft_version)
    patch_profile_file(minecraft_dir() / "launcher_profiles.json", profile, version_id)
    store_file = minecraft_dir() / "launcher_profiles_microsoft_store.json"
    if store_file.exists():
        patch_profile_file(store_file, profile, version_id)

def _launcher_installer_path() -> Path:
    """Stable cache location for the official launcher bootstrapper."""
    return Path(tempfile.gettempdir()) / "EzClient" / "MinecraftInstaller.msi"


def _download_official_launcher(status_callback: Callable[[str], None] | None = None) -> Path:
    """Download the official launcher MSI without ever invoking ``minecraft:``.

    ``minecraft:`` makes Windows show an unrelated Store protocol dialog when
    the launcher is missing.  Keeping the first-install flow here also means a
    fresh PC behaves predictably from EzClient's Play button.
    """
    installer = _launcher_installer_path()
    if installer.exists() and installer.stat().st_size > 1_000_000:
        return installer

    installer.parent.mkdir(parents=True, exist_ok=True)
    temporary = installer.with_suffix(".download")
    if status_callback:
        status_callback("Offizieller Minecraft Launcher wird vorbereitet…")
    try:
        with urlopen(MINECRAFT_INSTALLER_URL, timeout=30) as response, temporary.open("wb") as output:
            while chunk := response.read(1024 * 256):
                output.write(chunk)
        if temporary.stat().st_size <= 1_000_000:
            raise RuntimeError("Die Installationsdatei ist unvollständig.")
        temporary.replace(installer)
        return installer
    except Exception:
        temporary.unlink(missing_ok=True)
        raise


def launcher_install_exit_code() -> int | None:
    """Return the background MSI exit code once it has finished."""
    return _launcher_install_process.poll() if _launcher_install_process else None


def launch_minecraft_official(status_callback: Callable[[str], None] | None = None) -> bool:
    """Start the official launcher, or bootstrap it on a fresh Windows PC.

    Returns ``True`` when the launcher was started immediately and ``False``
    when its background installation has just been started.
    """
    installed, path, is_store = detect_launcher()
    if path and path.exists():
        cmd = ["open", str(path)] if sys.platform == "darwin" else [str(path)]
        subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return True
    if is_store and sys.platform.startswith("win"):
        # Launch the installed Store package directly.  This does not rely on
        # the minecraft: protocol and therefore cannot trigger the Store popup.
        subprocess.Popen(
            ["explorer.exe", "shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        return True
    elif sys.platform.startswith("win"):
        global _launcher_install_process
        installer = _download_official_launcher(status_callback)
        if status_callback:
            status_callback("Minecraft Launcher wird einmalig installiert…")
        # Passive keeps the installer non-interactive while still showing a
        # small progress window. /quiet hid failed first installs completely.
        _launcher_install_process = subprocess.Popen(
            ["msiexec.exe", "/i", str(installer), "/passive", "/norestart"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
        return False
    else:
        raise RuntimeError("Official Minecraft launcher not found.")
