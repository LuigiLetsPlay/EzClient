import os
import sys
import subprocess
import shutil
import tempfile
from pathlib import Path
from typing import Callable, Any
from backend.models.types import ProfileData, now_iso
from backend.services.store import read_json, write_json

MINECRAFT_INSTALLER_URL = "https://launcher.mojang.com/download/MinecraftInstaller.msi"
FABRIC_META_URL = "https://meta.fabricmc.net/v2/versions/installer"
FABRIC_FALLBACK_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar"

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

def launch_minecraft_official() -> None:
    installed, path, is_store = detect_launcher()
    if path and path.exists():
        cmd = ["open", str(path)] if sys.platform == "darwin" else [str(path)]
        subprocess.Popen(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    elif sys.platform.startswith("win"):
        os.startfile("minecraft://")
    else:
        raise RuntimeError("Official Minecraft launcher not found.")
