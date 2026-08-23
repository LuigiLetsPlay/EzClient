from __future__ import annotations

import io
import json
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile
import threading
import time
import urllib.parse
import urllib.request
import uuid
import webbrowser
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from tkinter import Menu, messagebox, ttk
from typing import Any, Callable

try:
    import customtkinter as ctk
except ImportError as exc:
    raise SystemExit(
        "CustomTkinter fehlt. Installiere die Abhaengigkeiten mit: "
        "python -m pip install -r requirements.txt"
    ) from exc

try:
    from PIL import Image, ImageDraw, ImageFont, ImageTk
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False


APP_NAME = "EzClient"
APP_VERSION = "1.6.2"
MINECRAFT_INSTALLER_URL = "https://launcher.mojang.com/download/MinecraftInstaller.msi"
FABRIC_META_URL = "https://meta.fabricmc.net/v2/versions/installer"
FABRIC_FALLBACK_URL = (
    "https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/"
    "fabric-installer-1.0.1.jar"
)
MODRINTH_API = "https://api.modrinth.com/v2"
USER_AGENT = f"EzClient/{APP_VERSION} (desktop launcher)"

# ==========================================
# High-End Desktop Game Launcher Palette
# ==========================================
BG = "#0D0F12"              # Root App Background
TITLEBAR_BG = "#0A0C0E"     # Integrated Titlebar
SIDEBAR = "#111419"         # Sidebar Background
SURFACE = "#15191F"         # Surface Level 1 (Panels/Bars)
SURFACE_2 = "#1B2028"       # Surface Level 2 (Inputs/Rows/Elevated)
SURFACE_3 = "#232934"       # Surface Level 3 (Hover/Active Items)
BORDER = "#252B35"          # Subtle 1px Desktop Borders
BORDER_LIGHT = "#323B4A"    # Focused/Selected Borders

TEXT = "#F2F4F7"            # Primary Text
TEXT_SECONDARY = "#C5CBD3"  # Secondary Text
MUTED = "#8E97A6"           # Muted/Label Text
SUBTLE = "#5C6472"          # Subtle Details

ACCENT = "#10B981"          # Mint/Green Accent (Play, Active, Progress)
ACCENT_HOVER = "#059669"
ACCENT_DARK = "#064E3B"
ACCENT_LIGHT = "#34D399"

CYAN = "#06B6D4"            # Links / Tech Accent
CYAN_DARK = "#164E63"
PURPLE = "#8B5CF6"          # Magic / Tags
DANGER = "#F43F5E"          # Error / Delete
DANGER_HOVER = "#E11D48"
DANGER_DARK = "#4C0519"
WARNING = "#F59E0B"         # Warning / Beta
WARNING_DARK = "#78350F"

FALLBACK_VERSIONS = [
    "26.2", "26.1", "1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5",
    "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21"
]

RECOMMENDED_MODS = [
    ("fabric-api", "Fabric API"),
    ("sodium", "Sodium"),
    ("sodium-extra", "Sodium Extra"),
    ("reeses-sodium-options", "Reese's Sodium Options"),
    ("lithium", "Lithium"),
    ("ferrite-core", "FerriteCore"),
    ("immediatelyfast", "ImmediatelyFast"),
    ("entityculling", "Entity Culling"),
    ("moreculling", "More Culling"),
    ("dynamic-fps", "Dynamic FPS"),
]


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def format_number(num: int | float) -> str:
    if num >= 1_000_000_000:
        return f"{num / 1_000_000_000:.1f}B"
    if num >= 1_000_000:
        return f"{num / 1_000_000:.1f}M"
    if num >= 1_000:
        return f"{num / 1_000:.1f}k"
    return str(int(num))


def format_bytes(size: int) -> str:
    if size < 1024:
        return f"{size} B"
    if size < 1024 * 1024:
        return f"{size / 1024:.1f} KB"
    return f"{size / (1024 * 1024):.2f} MB"


def format_date(iso_str: str) -> str:
    if not iso_str:
        return "Unknown"
    try:
        clean = iso_str.replace("Z", "+00:00")
        dt = datetime.fromisoformat(clean)
        return dt.strftime("%b %d, %Y")
    except Exception:
        return iso_str[:10] if len(iso_str) >= 10 else iso_str


def minecraft_dir() -> Path:
    if sys.platform.startswith("win") and os.environ.get("APPDATA"):
        return Path(os.environ["APPDATA"]) / ".minecraft"
    if sys.platform == "darwin":
        return Path.home() / "Library" / "Application Support" / "minecraft"
    return Path.home() / ".minecraft"


def data_dir() -> Path:
    if sys.platform.startswith("win"):
        root = os.environ.get("APPDATA") or os.environ.get("LOCALAPPDATA")
        if root:
            return Path(root) / ".ezclient"
    return Path.home() / ".ezclient"


DATA_DIR = data_dir()
PROFILES_DIR = DATA_DIR / "profiles"
CACHE_DIR = DATA_DIR / "cache"
ICON_CACHE_DIR = CACHE_DIR / "icons"
STATE_PATH = DATA_DIR / "state.json"


def ensure(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


ensure(DATA_DIR)
ensure(PROFILES_DIR)
ensure(ICON_CACHE_DIR)


def read_json(path: Path, default: Any) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return default


def write_json(path: Path, value: Any) -> None:
    ensure(path.parent)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(path)


def get_json(url: str, timeout: int = 25) -> Any:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def download(url: str, target: Path, timeout: int = 90) -> None:
    ensure(target.parent)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    partial = target.with_suffix(target.suffix + ".part")
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response, partial.open("wb") as output:
            shutil.copyfileobj(response, output)
        partial.replace(target)
    except Exception:
        partial.unlink(missing_ok=True)
        raise


# ==========================================
# Tooltip Helper
# ==========================================
class ToolTip:
    def __init__(self, widget: Any, text: str) -> None:
        self.widget = widget
        self.text = text
        self.tipwindow: Any = None
        self.widget.bind("<Enter>", self.show_tip)
        self.widget.bind("<Leave>", self.hide_tip)

    def show_tip(self, _event: Any = None) -> None:
        if self.tipwindow or not self.text:
            return
        x = self.widget.winfo_rootx() + 10
        y = self.widget.winfo_rooty() + self.widget.winfo_height() + 6
        self.tipwindow = tw = ctk.CTkToplevel(self.widget)
        tw.wm_overrideredirect(True)
        tw.wm_geometry(f"+{x}+{y}")
        tw.configure(fg_color=SURFACE_3)
        label = ctk.CTkLabel(tw, text=self.text, font=("Segoe UI", 9, "bold"), text_color=TEXT,
                             fg_color=SURFACE_3, corner_radius=4, padx=8, pady=4)
        label.pack()

    def hide_tip(self, _event: Any = None) -> None:
        if self.tipwindow:
            self.tipwindow.destroy()
            self.tipwindow = None


# ==========================================
# Icon & Image Management with Async Cache
# ==========================================
class IconManager:
    _instance: IconManager | None = None

    def __init__(self) -> None:
        self.memory_cache: dict[str, ctk.CTkImage] = {}
        self.download_queue: set[str] = set()
        self.lock = threading.Lock()

    @classmethod
    def get(cls) -> IconManager:
        if cls._instance is None:
            cls._instance = IconManager()
        return cls._instance

    def _url_to_path(self, url: str) -> Path:
        filename = re.sub(r"[^a-zA-Z0-9_-]", "_", url)[-64:] + ".png"
        return ICON_CACHE_DIR / filename

    def make_fallback(self, text: str, size: tuple[int, int], bg_color: str = SURFACE_3, fg_color: str = ACCENT_LIGHT) -> ctk.CTkImage:
        cache_key = f"fallback_{text[:2].upper()}_{size[0]}x{size[1]}_{bg_color}"
        if cache_key in self.memory_cache:
            return self.memory_cache[cache_key]

        w, h = size
        scale = 2
        img = Image.new("RGBA", (w * scale, h * scale), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)

        radius = int(min(w, h) * scale * 0.18)
        draw.rounded_rectangle([0, 0, w * scale - 1, h * scale - 1], radius=radius, fill=bg_color)

        display_text = (text[:2] if len(text) >= 2 else (text[:1] if text else "M")).upper()
        try:
            font_size = int(h * scale * 0.44)
            font = ImageFont.truetype("segoeui.ttf", font_size)
        except Exception:
            font = ImageFont.load_default()

        bbox = draw.textbbox((0, 0), display_text, font=font)
        tw = bbox[2] - bbox[0]
        th = bbox[3] - bbox[1]
        tx = (w * scale - tw) / 2 - bbox[0]
        ty = (h * scale - th) / 2 - bbox[1]
        draw.text((tx, ty), display_text, font=font, fill=fg_color)

        ctk_img = ctk.CTkImage(light_image=img, dark_image=img, size=size)
        self.memory_cache[cache_key] = ctk_img
        return ctk_img

    def load_icon_async(self, url: str | None, size: tuple[int, int],
                        callback: Callable[[ctk.CTkImage], None],
                        fallback_text: str = "") -> ctk.CTkImage:
        if not url:
            return self.make_fallback(fallback_text, size)

        cache_key = f"{url}_{size[0]}x{size[1]}"
        with self.lock:
            if cache_key in self.memory_cache:
                return self.memory_cache[cache_key]

        local_path = self._url_to_path(url)
        if local_path.exists() and local_path.stat().st_size > 0:
            try:
                pil_img = Image.open(local_path).convert("RGBA")
                w, h = size
                scale = 2
                pil_resized = pil_img.resize((w * scale, h * scale), Image.Resampling.LANCZOS)
                
                mask = Image.new("L", (w * scale, h * scale), 0)
                mask_draw = ImageDraw.Draw(mask)
                mask_draw.rounded_rectangle([0, 0, w * scale - 1, h * scale - 1],
                                            radius=int(min(w, h) * scale * 0.18), fill=255)
                
                rounded = Image.new("RGBA", (w * scale, h * scale), (0, 0, 0, 0))
                rounded.paste(pil_resized, (0, 0), mask)

                ctk_img = ctk.CTkImage(light_image=rounded, dark_image=rounded, size=size)
                with self.lock:
                    self.memory_cache[cache_key] = ctk_img
                return ctk_img
            except Exception:
                local_path.unlink(missing_ok=True)

        fallback_img = self.make_fallback(fallback_text, size)
        with self.lock:
            if url in self.download_queue:
                return fallback_img
            self.download_queue.add(url)

        def worker() -> None:
            try:
                req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
                with urllib.request.urlopen(req, timeout=12) as resp:
                    raw_data = resp.read()
                
                pil_raw = Image.open(io.BytesIO(raw_data)).convert("RGBA")
                pil_raw.save(local_path, "PNG")

                w, h = size
                scale = 2
                pil_resized = pil_raw.resize((w * scale, h * scale), Image.Resampling.LANCZOS)
                
                mask = Image.new("L", (w * scale, h * scale), 0)
                mask_draw = ImageDraw.Draw(mask)
                mask_draw.rounded_rectangle([0, 0, w * scale - 1, h * scale - 1],
                                            radius=int(min(w, h) * scale * 0.18), fill=255)
                
                rounded = Image.new("RGBA", (w * scale, h * scale), (0, 0, 0, 0))
                rounded.paste(pil_resized, (0, 0), mask)

                ctk_img = ctk.CTkImage(light_image=rounded, dark_image=rounded, size=size)
                with self.lock:
                    self.memory_cache[cache_key] = ctk_img
                    self.download_queue.discard(url)

                callback(ctk_img)
            except Exception:
                with self.lock:
                    self.download_queue.discard(url)

        threading.Thread(target=worker, daemon=True).start()
        return fallback_img


@dataclass
class LauncherInfo:
    installed: bool
    path: Path | None
    store_app: bool = False


def launcher_candidates() -> list[Path]:
    if not sys.platform.startswith("win"):
        return [Path("/Applications/Minecraft Launcher.app")] if sys.platform == "darwin" else [Path("/usr/bin/minecraft-launcher")]
    local = os.environ.get("LOCALAPPDATA", "")
    program = os.environ.get("PROGRAMFILES", "")
    program_x86 = os.environ.get("PROGRAMFILES(X86)", "")
    values = [
        Path(local) / "Programs/Minecraft Launcher/MinecraftLauncher.exe",
        Path(local) / "Minecraft Launcher/MinecraftLauncher.exe",
        Path(program) / "Minecraft Launcher/MinecraftLauncher.exe",
        Path(program_x86) / "Minecraft Launcher/MinecraftLauncher.exe",
    ]
    return [path for path in values if str(path.parent) != "."]


def detect_launcher() -> LauncherInfo:
    for path in launcher_candidates():
        if path.exists():
            return LauncherInfo(True, path)
    if sys.platform.startswith("win"):
        package = Path(os.environ.get("LOCALAPPDATA", "")) / "Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe"
        if package.exists():
            return LauncherInfo(True, None, True)
    return LauncherInfo(False, None)


def install_launcher(status: Callable[[str], None]) -> LauncherInfo:
    if not sys.platform.startswith("win"):
        raise RuntimeError("Die automatische Launcher-Installation wird nur unter Windows unterstuetzt.")
    installer = Path(tempfile.gettempdir()) / "ezclient" / "MinecraftInstaller.msi"
    status("Downloading the official Minecraft Launcher ...")
    download(MINECRAFT_INSTALLER_URL, installer)
    status("Installing the official Minecraft Launcher ...")
    subprocess.run(["msiexec", "/i", str(installer), "/passive", "/norestart"], check=True)
    result = detect_launcher()
    if not result.installed:
        raise RuntimeError("Der Minecraft Launcher wurde nach der Installation nicht gefunden.")
    return result


def launch_launcher(info: LauncherInfo) -> None:
    if info.path:
        command = ["open", str(info.path)] if sys.platform == "darwin" else [str(info.path)]
        subprocess.Popen(command, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    elif sys.platform.startswith("win"):
        raise RuntimeError("Minecraft Launcher nicht gefunden. Bitte installiere den offiziellen Minecraft Launcher oder nutze den Direktstart mit Java.")
    else:
        raise RuntimeError("Der offizielle Minecraft Launcher konnte nicht gestartet werden.")


def java_path() -> str:
    if os.environ.get("JAVA_HOME"):
        candidate = Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if sys.platform.startswith("win") else "java")
        if candidate.exists():
            return str(candidate)
    return shutil.which("java") or ""


def fabric_installer_url() -> str:
    try:
        installers = get_json(FABRIC_META_URL)
        stable = next((item for item in installers if item.get("stable") and item.get("url")), None)
        selected = stable or installers[0]
        if str(selected.get("url", "")).endswith(".jar"):
            return selected["url"]
    except Exception:
        pass
    return FABRIC_FALLBACK_URL


def fabric_version(mc_version: str) -> str:
    versions = minecraft_dir() / "versions"
    candidates = [
        path for path in versions.glob(f"fabric-loader-*-{mc_version}") if path.is_dir()
    ] if versions.exists() else []
    if not candidates:
        raise RuntimeError(f"Fabric fuer Minecraft {mc_version} wurde nicht gefunden.")
    return max(candidates, key=lambda path: path.stat().st_mtime).name


def ensure_fabric(mc_version: str, status: Callable[[str], None]) -> str:
    try:
        return fabric_version(mc_version)
    except RuntimeError:
        pass
    java = java_path()
    if not java:
        raise RuntimeError("Java wurde nicht gefunden. Aktuelle Minecraft-Versionen benoetigen Java 21.")
    jar = Path(tempfile.gettempdir()) / "ezclient" / "fabric-installer.jar"
    status("Downloading Fabric Installer via Meta API ...")
    download(fabric_installer_url(), jar)
    status(f"Installing Fabric for Minecraft {mc_version} ...")
    result = subprocess.run(
        [java, "-jar", str(jar), "client", "-dir", str(minecraft_dir()), "-mcversion", mc_version, "-noprofile"],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
    )
    if result.returncode:
        raise RuntimeError(f"Fabric Installer failed:\n{result.stdout[-1200:]}")
    return fabric_version(mc_version)


@dataclass
class Mod:
    project_id: str
    slug: str
    name: str
    version_id: str
    version: str
    filename: str
    enabled: bool = True
    recommended: bool = False
    icon_url: str = ""
    author: str = ""
    description: str = ""


@dataclass
class Profile:
    id: str
    name: str
    minecraft_version: str
    optimize: bool = True
    loader: str = "Fabric"
    created: str = field(default_factory=now_iso)
    last_played: str = ""
    mods: list[Mod] = field(default_factory=list)

    @property
    def path(self) -> Path:
        return PROFILES_DIR / self.id

    @property
    def mods_path(self) -> Path:
        return self.path / "mods"


class Store:
    def __init__(self) -> None:
        ensure(PROFILES_DIR)
        data = read_json(STATE_PATH, {})
        self.settings = {"close_on_launch": False, "check_updates": True, "last_profile": ""}
        self.settings.update(data.get("settings", {}))
        self.profiles: list[Profile] = []
        for raw in data.get("profiles", []):
            mods = []
            for mod_data in raw.pop("mods", []):
                valid_fields = {k: v for k, v in mod_data.items() if k in Mod.__annotations__}
                mods.append(Mod(**valid_fields))
            profile_fields = {k: v for k, v in raw.items() if k in Profile.__annotations__}
            self.profiles.append(Profile(**profile_fields, mods=mods))

    def save(self) -> None:
        write_json(STATE_PATH, {"settings": self.settings, "profiles": [asdict(profile) for profile in self.profiles]})

    def create(self, name: str, version: str, optimize: bool) -> Profile:
        slug = re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-") or "profile"
        profile = Profile(f"{slug}-{uuid.uuid4().hex[:8]}", name, version, optimize)
        ensure(profile.mods_path)
        ensure(profile.path / "config")
        self.profiles.insert(0, profile)
        self.settings["last_profile"] = profile.id
        self.save()
        return profile

    def last(self) -> Profile | None:
        selected = next((p for p in self.profiles if p.id == self.settings.get("last_profile")), None)
        return selected or (self.profiles[0] if self.profiles else None)

    def delete(self, profile: Profile) -> None:
        self.profiles = [item for item in self.profiles if item.id != profile.id]
        self.settings["last_profile"] = self.profiles[0].id if self.profiles else ""
        self.save()


def patch_profile_file(path: Path, profile: Profile, version_id: str) -> None:
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
    if path.exists():
        try:
            shutil.copy2(path, path.with_suffix(".json.bak"))
        except OSError:
            pass
    write_json(path, data)


def patch_launcher_profile(profile: Profile) -> None:
    version_id = fabric_version(profile.minecraft_version)
    patch_profile_file(minecraft_dir() / "launcher_profiles.json", profile, version_id)
    store_file = minecraft_dir() / "launcher_profiles_microsoft_store.json"
    if store_file.exists():
        patch_profile_file(store_file, profile, version_id)
    settings_file = minecraft_dir() / "launcher_settings.json"
    if settings_file.exists():
        settings = read_json(settings_file, {})
        if isinstance(settings, dict):
            settings["selectedProfile"] = "EzClient"
            write_json(settings_file, settings)


# ==========================================
# Modrinth Client
# ==========================================
class Modrinth:
    def versions(self) -> list[str]:
        try:
            values = [item["version"] for item in get_json(f"{MODRINTH_API}/tag/game_version")
                      if item.get("version_type") == "release" and
                      (item.get("version", "").startswith("1.21") or
                       item.get("version", "").startswith("1.20") or
                       item.get("version", "").startswith("1.19") or
                       item.get("version", "").startswith("1.18") or
                       re.fullmatch(r"2[2-9]\.\d+(\.\d+)?", item.get("version", "")))]
            return list(dict.fromkeys(values + FALLBACK_VERSIONS))
        except Exception:
            return FALLBACK_VERSIONS

    def search(self, query: str, version: str | None = None, category: str = "All",
               sort: str = "relevance", loader: str = "fabric", offset: int = 0, limit: int = 20) -> dict[str, Any]:
        facets = [["project_type:mod"]]
        if loader and loader.lower() != "all":
            facets.append([f"categories:{loader.lower()}"])
        if version and version.lower() != "all":
            facets.append([f"versions:{version}"])
        if category not in ("Featured", "All", "all"):
            facets.append([f"categories:{category.lower()}"])

        params = urllib.parse.urlencode({
            "query": query,
            "facets": json.dumps(facets),
            "index": sort,
            "offset": offset,
            "limit": limit
        })
        data = get_json(f"{MODRINTH_API}/search?{params}")
        return {
            "hits": data.get("hits", []),
            "total_hits": data.get("total_hits", len(data.get("hits", []))),
            "offset": offset,
            "limit": limit
        }

    def project_versions(self, project: str, mc_version: str | None = None,
                         loader: str | None = "fabric") -> list[dict[str, Any]]:
        query_params: dict[str, Any] = {}
        if loader and loader.lower() != "all":
            query_params["loaders"] = json.dumps([loader.lower()])
        if mc_version and mc_version.lower() != "all":
            query_params["game_versions"] = json.dumps([mc_version])
        
        qs = f"?{urllib.parse.urlencode(query_params)}" if query_params else ""
        return get_json(f"{MODRINTH_API}/project/{urllib.parse.quote(project)}/version{qs}")

    def compatible(self, project: str, mc_version: str, loader: str = "fabric") -> list[dict[str, Any]]:
        return self.project_versions(project, mc_version=mc_version, loader=loader)

    def project(self, project: str) -> dict[str, Any]:
        return get_json(f"{MODRINTH_API}/project/{urllib.parse.quote(project)}")

    def install(self, profile: Profile, project_id: str, version_id: str | None = None,
                recommended: bool = False, visited: set[str] | None = None) -> Mod:
        existing = next((mod for mod in profile.mods if mod.project_id == project_id or mod.slug == project_id), None)
        if existing:
            return existing
        visited = visited or set()
        if project_id in visited:
            raise RuntimeError("Circular Modrinth dependency detected.")
        visited.add(project_id)

        metadata = self.project(project_id)
        proj_id = metadata.get("id", project_id)
        proj_slug = metadata.get("slug", project_id)
        proj_title = metadata.get("title", project_id)
        proj_icon = metadata.get("icon_url", "")
        proj_desc = metadata.get("description", "")
        proj_author = metadata.get("author", "") or metadata.get("organization", "")

        if version_id:
            version_data = get_json(f"{MODRINTH_API}/version/{urllib.parse.quote(version_id)}")
        else:
            versions = self.compatible(proj_id, profile.minecraft_version)
            if not versions:
                raise RuntimeError(f"No compatible Fabric release found for Minecraft {profile.minecraft_version}.")
            version_data = versions[0]

        for dependency in version_data.get("dependencies", []):
            if dependency.get("dependency_type") == "required" and dependency.get("project_id"):
                try:
                    self.install(profile, dependency["project_id"], None, False, visited)
                except Exception:
                    pass

        files = version_data.get("files", [])
        file_obj = next((item for item in files if item.get("primary")), files[0] if files else None)
        if not file_obj:
            raise RuntimeError("Modrinth returned no downloadable file for this version.")

        target = profile.mods_path / file_obj["filename"]
        download(file_obj["url"], target)

        mod = Mod(
            project_id=proj_id,
            slug=proj_slug,
            name=proj_title,
            version_id=version_data["id"],
            version=version_data.get("version_number", "Unknown"),
            filename=target.name,
            enabled=True,
            recommended=recommended,
            icon_url=proj_icon,
            author=proj_author,
            description=proj_desc
        )
        profile.mods.append(mod)
        return mod

    def update_for(self, profile: Profile, mod: Mod) -> dict[str, Any] | None:
        versions = self.compatible(mod.project_id, profile.minecraft_version)
        return versions[0] if versions and versions[0].get("id") != mod.version_id else None


# ==========================================
# Main Desktop Game Launcher (EzClient)
# ==========================================
class EzClient(ctk.CTk):
    def __init__(self) -> None:
        super().__init__()
        ctk.set_appearance_mode("dark")
        self.title("EzClient")
        self.geometry("1240x780")
        self.minsize(1040, 660)
        self.configure(fg_color=BG)

        self.store = Store()
        self.modrinth = Modrinth()
        self.icons = IconManager.get()
        self.current = self.store.last()
        self.nav_buttons: dict[str, ctk.CTkButton] = {}
        self.content_area: ctk.CTkFrame | None = None
        self.busy = False

        # Build genuine desktop structure
        self._build_titlebar()
        self._build_layout()
        self._build_statusbar()
        self._bind_shortcuts()

        self.home()

    # ----------------------------------------------------
    # Window Chrome & TitleBar
    # ----------------------------------------------------
    def _build_titlebar(self) -> None:
        self.titlebar = ctk.CTkFrame(self, height=36, corner_radius=0, fg_color=TITLEBAR_BG,
                                     border_color=BORDER, border_width=0)
        self.titlebar.pack(fill="x", side="top")
        self.titlebar.pack_propagate(False)

        # Left branding
        left = ctk.CTkFrame(self.titlebar, fg_color="transparent")
        left.pack(side="left", padx=12)

        icon_lbl = ctk.CTkLabel(left, text="⚡", font=("Segoe UI", 12, "bold"), text_color=ACCENT_LIGHT)
        icon_lbl.pack(side="left", padx=(0, 6))

        title_lbl = ctk.CTkLabel(left, text="EzClient", font=("Segoe UI", 11, "bold"), text_color=TEXT)
        title_lbl.pack(side="left")

        self.title_breadcrumb = ctk.CTkLabel(left, text="", font=("Segoe UI", 11), text_color=MUTED)
        self.title_breadcrumb.pack(side="left", padx=6)

        # Drag handler on titlebar
        self._drag_x = 0
        self._drag_y = 0
        for w in (self.titlebar, left, icon_lbl, title_lbl, self.title_breadcrumb):
            w.bind("<Button-1>", self._start_drag)
            w.bind("<B1-Motion>", self._do_drag)

    def _start_drag(self, event: Any) -> None:
        self._drag_x = event.x_root - self.winfo_x()
        self._drag_y = event.y_root - self.winfo_y()

    def _do_drag(self, event: Any) -> None:
        self.geometry(f"+{event.x_root - self._drag_x}+{event.y_root - self._drag_y}")

    def set_breadcrumb(self, text: str) -> None:
        self.title_breadcrumb.configure(text=f"/  {text}" if text else "")

    # ----------------------------------------------------
    # Desktop Layout: Sidebar + Main Workspace
    # ----------------------------------------------------
    def _build_layout(self) -> None:
        self.workspace = ctk.CTkFrame(self, corner_radius=0, fg_color=BG)
        self.workspace.pack(fill="both", expand=True)

        # Desktop Sidebar (~215px)
        self.sidebar = ctk.CTkFrame(self.workspace, width=215, corner_radius=0, fg_color=SIDEBAR,
                                    border_color=BORDER, border_width=0)
        self.sidebar.pack(side="left", fill="y")
        self.sidebar.pack_propagate(False)

        # Subtle 1px right border for desktop separation
        ctk.CTkFrame(self.sidebar, width=1, corner_radius=0, fg_color=BORDER).pack(side="right", fill="y")

        # Sidebar content
        side_content = ctk.CTkFrame(self.sidebar, fg_color="transparent")
        side_content.pack(fill="both", expand=True, padx=10, pady=12)

        # Category: EZCLIENT
        self._sidebar_header(side_content, "EZCLIENT")
        self._sidebar_btn(side_content, "Home", "⌂", self.home)

        # Category: LIBRARY
        self._sidebar_header(side_content, "LIBRARY", pady=(14, 4))
        self._sidebar_btn(side_content, "Profiles", "▣", self.profiles)
        self._sidebar_btn(side_content, "Mods", "◇", self.mods)

        # Category: SYSTEM
        self._sidebar_header(side_content, "SYSTEM", pady=(14, 4))
        self._sidebar_btn(side_content, "Settings", "⚙", self.settings)

        # Account bar at bottom of sidebar (compact ~38px row)
        account_bar = ctk.CTkFrame(side_content, height=42, corner_radius=6, fg_color=SURFACE,
                                   border_color=BORDER, border_width=1)
        account_bar.pack(side="bottom", fill="x", pady=(10, 0))
        account_bar.pack_propagate(False)

        avatar = ctk.CTkLabel(account_bar, text="MC", width=26, height=26, corner_radius=13,
                              fg_color=ACCENT_DARK, text_color=ACCENT_LIGHT, font=("Segoe UI", 9, "bold"))
        avatar.pack(side="left", padx=8, pady=8)

        acct_info = ctk.CTkFrame(account_bar, fg_color="transparent")
        acct_info.pack(side="left", fill="both", expand=True, pady=6)
        ctk.CTkLabel(acct_info, text="Steve", text_color=TEXT, font=("Segoe UI", 11, "bold")).pack(anchor="w")
        ctk.CTkLabel(acct_info, text="Microsoft Account", text_color=SUBTLE, font=("Segoe UI", 8)).pack(anchor="w")

        # Main Content Canvas
        self.main_view = ctk.CTkFrame(self.workspace, corner_radius=0, fg_color=BG)
        self.main_view.pack(side="right", fill="both", expand=True)

    def _sidebar_header(self, parent: Any, title: str, pady: tuple[int, int] = (6, 4)) -> None:
        ctk.CTkLabel(parent, text=title, font=("Segoe UI", 9, "bold"), text_color=SUBTLE, anchor="w").pack(fill="x", padx=8, pady=pady)

    def _sidebar_btn(self, parent: Any, name: str, symbol: str, command: Callable[[], None]) -> None:
        btn = ctk.CTkButton(
            parent, text=f"  {symbol}   {name}", command=command, anchor="w",
            height=32, corner_radius=5, fg_color="transparent", hover_color=SURFACE_2,
            text_color=MUTED, font=("Segoe UI", 11, "bold")
        )
        btn.pack(fill="x", pady=1)
        self.nav_buttons[name] = btn

    # ----------------------------------------------------
    # Desktop Status Bar (~26px)
    # ----------------------------------------------------
    def _build_statusbar(self) -> None:
        self.statusbar = ctk.CTkFrame(self, height=26, corner_radius=0, fg_color=SURFACE,
                                      border_color=BORDER, border_width=1)
        self.statusbar.pack(fill="x", side="bottom")
        self.statusbar.pack_propagate(False)

        # Status text left
        self.status_left = ctk.CTkLabel(
            self.statusbar, text="● Online  ·  Modrinth API Connected", text_color=ACCENT_LIGHT,
            font=("Segoe UI", 9, "bold")
        )
        self.status_left.pack(side="left", padx=12)

        info = detect_launcher()
        mc_status_text = "● Official Launcher Ready" if info.installed else "● Official Launcher Not Detected"
        mc_status_color = ACCENT if info.installed else WARNING
        self.status_mc = ctk.CTkLabel(
            self.statusbar, text=mc_status_text, text_color=mc_status_color,
            font=("Segoe UI", 9)
        )
        self.status_mc.pack(side="left", padx=8)

        # Right side info
        ctk.CTkLabel(self.statusbar, text=f"EzClient {APP_VERSION}", text_color=SUBTLE,
                     font=("Segoe UI", 9)).pack(side="right", padx=12)

    def _bind_shortcuts(self) -> None:
        self.bind("<Control-k>", lambda _e: self.mods())
        self.bind("<Control-n>", lambda _e: self.create_profile())
        self.bind("<Control-comma>", lambda _e: self.settings())
        self.bind("<Control-r>", lambda _e: self._refresh_current())

    def _refresh_current(self) -> None:
        if self.current:
            self.profile_detail(self.current)
        else:
            self.home()

    def page(self, active: str) -> ctk.CTkFrame:
        for name, button in self.nav_buttons.items():
            is_active = (name == active or (active == "Mods" and name == "Mods"))
            button.configure(
                fg_color=SURFACE_3 if is_active else "transparent",
                text_color=TEXT if is_active else MUTED,
                border_color=ACCENT if is_active else SIDEBAR,
                border_width=1 if is_active else 0
            )
        if self.content_area:
            self.content_area.destroy()
        self.content_area = ctk.CTkFrame(self.main_view, corner_radius=0, fg_color=BG)
        self.content_area.pack(fill="both", expand=True)
        return self.content_area

    def button(self, master: Any, text: str, command: Callable[[], None], primary: bool = False,
               danger: bool = False, cyan: bool = False, width: int = 90, height: int = 32) -> ctk.CTkButton:
        if primary:
            color, hover, text_color = ACCENT, ACCENT_HOVER, BG
        elif danger:
            color, hover, text_color = DANGER_DARK, DANGER_HOVER, DANGER
        elif cyan:
            color, hover, text_color = CYAN_DARK, CYAN, CYAN
        else:
            color, hover, text_color = SURFACE_2, SURFACE_3, TEXT

        return ctk.CTkButton(
            master, text=text, command=command, width=width, height=height, corner_radius=6,
            fg_color=color, hover_color=hover, text_color=text_color,
            font=("Segoe UI", 11, "bold")
        )

    def panel(self, master: Any, **kwargs: Any) -> ctk.CTkFrame:
        return ctk.CTkFrame(master, fg_color=SURFACE, border_color=BORDER, border_width=1,
                            corner_radius=6, **kwargs)

    # ----------------------------------------------------
    # HOME VIEW
    # ----------------------------------------------------
    def home(self) -> None:
        self.set_breadcrumb("Home")
        page = self.page("Home")

        # Top Toolbar
        toolbar = ctk.CTkFrame(page, height=44, corner_radius=0, fg_color=SURFACE, border_color=BORDER, border_width=1)
        toolbar.pack(fill="x")
        toolbar.pack_propagate(False)

        ctk.CTkLabel(toolbar, text="Game Library", font=("Segoe UI", 14, "bold"), text_color=TEXT).pack(side="left", padx=16)
        self.button(toolbar, "+ New Profile", self.create_profile, primary=True, width=110, height=28).pack(side="right", padx=12)

        body = ctk.CTkScrollableFrame(page, fg_color="transparent")
        body.pack(fill="both", expand=True, padx=20, pady=16)

        profile = self.store.last()
        if not profile:
            empty = self.panel(body)
            empty.pack(fill="x", pady=10)
            ctk.CTkLabel(empty, text="No Minecraft profiles created yet.", text_color=TEXT, font=("Segoe UI", 14, "bold")).pack(anchor="w", padx=18, pady=(18, 4))
            ctk.CTkLabel(empty, text="Create a Fabric profile with verified performance mods in 1 click.", text_color=MUTED, font=("Segoe UI", 11)).pack(anchor="w", padx=18)
            self.button(empty, "+ Create First Profile", self.create_profile, True, width=160, height=34).pack(anchor="w", padx=18, pady=16)
            return

        # Recently Played Hero Area (Game Launcher look)
        ctk.CTkLabel(body, text="RECENTLY PLAYED", font=("Segoe UI", 10, "bold"), text_color=SUBTLE).pack(anchor="w", pady=(0, 6))

        hero = self.panel(body, height=125)
        hero.pack(fill="x")
        hero.pack_propagate(False)

        # Left accent line
        ctk.CTkFrame(hero, width=4, corner_radius=2, fg_color=ACCENT).pack(side="left", fill="y")

        hero_body = ctk.CTkFrame(hero, fg_color="transparent")
        hero_body.pack(side="left", fill="both", expand=True, padx=16, pady=14)

        icon_lbl = ctk.CTkLabel(hero_body, text="", width=44, height=44)
        icon_lbl.pack(side="left", padx=(0, 14))
        icon_img = self.icons.make_fallback(profile.name, (44, 44), bg_color=SURFACE_2, fg_color=ACCENT_LIGHT)
        icon_lbl.configure(image=icon_img)

        info = ctk.CTkFrame(hero_body, fg_color="transparent")
        info.pack(side="left", fill="both", expand=True)

        ctk.CTkLabel(info, text=profile.name, text_color=TEXT, font=("Segoe UI", 18, "bold")).pack(anchor="w")
        
        meta_str = f"Minecraft {profile.minecraft_version}  •  Fabric Loader  •  {len(profile.mods)} Mods  •  ● Ready to play"
        ctk.CTkLabel(info, text=meta_str, text_color=MUTED, font=("Segoe UI", 10)).pack(anchor="w", pady=(2, 0))

        # Right Action cluster
        hero_right = ctk.CTkFrame(hero, fg_color="transparent")
        hero_right.pack(side="right", padx=18, pady=16)

        play_btn = ctk.CTkButton(
            hero_right, text="▶   PLAY", command=lambda: self.play(profile), width=130, height=38,
            corner_radius=6, fg_color=ACCENT, hover_color=ACCENT_HOVER, text_color=BG,
            font=("Segoe UI", 12, "bold")
        )
        play_btn.pack(side="right")

        manage_btn = self.button(hero_right, "Manage", lambda: self.profile_detail(profile), width=80, height=36)
        manage_btn.pack(side="right", padx=(0, 8))

        # Compact Quick Tools Strip
        tools_strip = ctk.CTkFrame(body, fg_color="transparent")
        tools_strip.pack(fill="x", pady=14)

        mod_box = self.panel(tools_strip, height=54)
        mod_box.pack(side="left", fill="both", expand=True, padx=(0, 8))
        mod_box.pack_propagate(False)

        ctk.CTkLabel(mod_box, text="🔍  Modrinth Package Manager", font=("Segoe UI", 11, "bold"), text_color=TEXT).pack(side="left", padx=14)
        self.button(mod_box, "Explore Mods", lambda: self.mods(profile), cyan=True, width=105, height=28).pack(side="right", padx=10)

        up_box = self.panel(tools_strip, height=54)
        up_box.pack(side="left", fill="both", expand=True)
        up_box.pack_propagate(False)

        ctk.CTkLabel(up_box, text="🔄  Mod Updates", font=("Segoe UI", 11, "bold"), text_color=TEXT).pack(side="left", padx=14)
        self.button(up_box, "Check Updates", lambda: self.check_updates(profile), width=105, height=28).pack(side="right", padx=10)

        # Recent Profiles List Table
        ctk.CTkLabel(body, text="ALL PROFILES", font=("Segoe UI", 10, "bold"), text_color=SUBTLE).pack(anchor="w", pady=(8, 6))

        table = self.panel(body)
        table.pack(fill="x")

        # Table Header
        th = ctk.CTkFrame(table, height=28, fg_color=SURFACE_2, corner_radius=0)
        th.pack(fill="x")
        th.pack_propagate(False)
        ctk.CTkLabel(th, text="NAME", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, width=220, anchor="w").pack(side="left", padx=14)
        ctk.CTkLabel(th, text="MINECRAFT", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, width=120, anchor="w").pack(side="left")
        ctk.CTkLabel(th, text="LOADER", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, width=100, anchor="w").pack(side="left")
        ctk.CTkLabel(th, text="MODS", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, width=100, anchor="w").pack(side="left")
        ctk.CTkLabel(th, text="ACTIONS", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, anchor="e").pack(side="right", padx=14)

        for p in self.store.profiles:
            row = ctk.CTkFrame(table, height=44, fg_color="transparent", corner_radius=0)
            row.pack(fill="x")
            row.pack_propagate(False)

            # Left Name & icon
            left_p = ctk.CTkFrame(row, fg_color="transparent", width=220)
            left_p.pack(side="left", padx=14)
            left_p.pack_propagate(False)

            p_icon = ctk.CTkLabel(left_p, text="", width=24, height=24)
            p_icon.pack(side="left", padx=(0, 8))
            p_icon.configure(image=self.icons.make_fallback(p.name, (24, 24), bg_color=SURFACE_3, fg_color=ACCENT_LIGHT))

            ctk.CTkLabel(left_p, text=p.name, font=("Segoe UI", 11, "bold"), text_color=TEXT, anchor="w").pack(side="left")

            ctk.CTkLabel(row, text=p.minecraft_version, font=("Segoe UI", 10), text_color=CYAN, width=120, anchor="w").pack(side="left")
            ctk.CTkLabel(row, text="Fabric", font=("Segoe UI", 10), text_color=TEXT_SECONDARY, width=100, anchor="w").pack(side="left")
            ctk.CTkLabel(row, text=f"{len(p.mods)} mods", font=("Segoe UI", 10), text_color=MUTED, width=100, anchor="w").pack(side="left")

            # Actions right
            act = ctk.CTkFrame(row, fg_color="transparent")
            act.pack(side="right", padx=12)

            self.button(act, "Play", lambda prof=p: self.play(prof), primary=True, width=64, height=26).pack(side="right")
            self.button(act, "Manage", lambda prof=p: self.profile_detail(prof), width=68, height=26).pack(side="right", padx=6)

            # Separator line
            ctk.CTkFrame(table, height=1, fg_color=BORDER).pack(fill="x")

    # ----------------------------------------------------
    # PROFILES LIST VIEW
    # ----------------------------------------------------
    def profiles(self) -> None:
        self.set_breadcrumb("Profiles")
        page = self.page("Profiles")

        toolbar = ctk.CTkFrame(page, height=44, corner_radius=0, fg_color=SURFACE, border_color=BORDER, border_width=1)
        toolbar.pack(fill="x")
        toolbar.pack_propagate(False)

        ctk.CTkLabel(toolbar, text=f"Profiles ({len(self.store.profiles)})", font=("Segoe UI", 13, "bold"), text_color=TEXT).pack(side="left", padx=16)
        self.button(toolbar, "+ Create Profile", self.create_profile, primary=True, width=120, height=28).pack(side="right", padx=12)

        body = ctk.CTkScrollableFrame(page, fg_color="transparent")
        body.pack(fill="both", expand=True, padx=20, pady=16)

        for p in self.store.profiles:
            item = self.panel(body, height=64)
            item.pack(fill="x", pady=3)
            item.pack_propagate(False)

            # Left Profile Icon
            icon_lbl = ctk.CTkLabel(item, text="", width=38, height=38)
            icon_lbl.pack(side="left", padx=14)
            icon_lbl.configure(image=self.icons.make_fallback(p.name, (38, 38), bg_color=SURFACE_2, fg_color=ACCENT_LIGHT))

            # Details
            info = ctk.CTkFrame(item, fg_color="transparent")
            info.pack(side="left", fill="both", expand=True, pady=10)

            title_row = ctk.CTkFrame(info, fg_color="transparent")
            title_row.pack(anchor="w")
            ctk.CTkLabel(title_row, text=p.name, font=("Segoe UI", 13, "bold"), text_color=TEXT).pack(side="left")
            
            mc_chip = ctk.CTkLabel(title_row, text=f"MC {p.minecraft_version}", fg_color=SURFACE_2, text_color=CYAN,
                                  corner_radius=4, font=("Segoe UI", 9, "bold"), width=70, height=18)
            mc_chip.pack(side="left", padx=8)

            meta_str = f"Fabric Loader  •  {len(p.mods)} installed mods"
            ctk.CTkLabel(info, text=meta_str, font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w", pady=(1, 0))

            # Right actions
            actions = ctk.CTkFrame(item, fg_color="transparent")
            actions.pack(side="right", padx=14)

            self.button(actions, "▶ Play", lambda prof=p: self.play(prof), primary=True, width=78, height=30).pack(side="right")
            self.button(actions, "Manage", lambda prof=p: self.profile_detail(prof), width=74, height=30).pack(side="right", padx=6)
            self.button(actions, "⋯", lambda prof=p: self.profile_menu(prof), width=32, height=30).pack(side="right")

    def profile_menu(self, profile: Profile) -> None:
        menu = Menu(self, tearoff=False, bg=SURFACE_2, fg=TEXT, activebackground=SURFACE_3,
                    activeforeground=TEXT, relief="flat")
        menu.add_command(label="Manage Profile", command=lambda: self.profile_detail(profile, "Overview"))
        menu.add_command(label="Browse Mods", command=lambda: self.mods(profile))
        menu.add_command(label="Duplicate Profile", command=lambda: self.duplicate(profile))
        menu.add_command(label="Open Profile Directory", command=lambda: self.open_folder(profile.path))
        menu.add_command(label="Open mods/ Folder", command=lambda: self.open_folder(profile.mods_path))
        menu.add_separator()
        menu.add_command(label="Delete Profile", command=lambda: self.delete_profile(profile))
        menu.tk_popup(self.winfo_pointerx(), self.winfo_pointery())

    def duplicate(self, profile: Profile) -> None:
        copy = self.store.create(f"{profile.name} Copy", profile.minecraft_version, profile.optimize)
        for mod in profile.mods:
            source = profile.mods_path / mod.filename
            if source.exists():
                shutil.copy2(source, copy.mods_path / mod.filename)
            copy.mods.append(Mod(**asdict(mod)))
        self.store.save()
        self.profiles()

    def delete_profile(self, profile: Profile) -> None:
        if messagebox.askyesno(APP_NAME, f"Delete profile '{profile.name}'?\n\nIts files on disk will be preserved."):
            self.store.delete(profile)
            self.profiles()

    def create_profile(self) -> None:
        CreateFlow(self)

    # ----------------------------------------------------
    # PROFILE DETAIL VIEW (Game Launcher Hero + Dense Panels)
    # ----------------------------------------------------
    def profile_detail(self, profile: Profile, tab: str = "Overview") -> None:
        self.current = profile
        self.set_breadcrumb(f"Profiles / {profile.name}")
        page = self.page("Profiles")

        # Top Game Launcher Hero Header (~85px)
        header = ctk.CTkFrame(page, height=88, corner_radius=0, fg_color=SURFACE,
                              border_color=BORDER, border_width=1)
        header.pack(fill="x")
        header.pack_propagate(False)

        header_inner = ctk.CTkFrame(header, fg_color="transparent")
        header_inner.pack(fill="both", expand=True, padx=20, pady=12)

        # Back link & Icon
        icon_lbl = ctk.CTkLabel(header_inner, text="", width=46, height=46)
        icon_lbl.pack(side="left", padx=(0, 14))
        icon_lbl.configure(image=self.icons.make_fallback(profile.name, (46, 46), bg_color=SURFACE_2, fg_color=ACCENT_LIGHT))

        info = ctk.CTkFrame(header_inner, fg_color="transparent")
        info.pack(side="left", fill="both", expand=True)

        back_btn = ctk.CTkButton(
            info, text="← Profiles", command=self.profiles, width=60, height=16,
            fg_color="transparent", hover_color=SURFACE_2, text_color=MUTED,
            font=("Segoe UI", 9, "bold"), anchor="w"
        )
        back_btn.pack(anchor="w")

        ctk.CTkLabel(info, text=profile.name, font=("Segoe UI", 16, "bold"), text_color=TEXT).pack(anchor="w")

        meta_line = f"Minecraft {profile.minecraft_version}  •  Fabric Loader  •  {len(profile.mods)} Mods  •  ● Ready to play"
        ctk.CTkLabel(info, text=meta_line, font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w")

        # Right Action Cluster
        actions = ctk.CTkFrame(header_inner, fg_color="transparent")
        actions.pack(side="right")

        play_btn = ctk.CTkButton(
            actions, text="▶   PLAY", command=lambda: self.play(profile), width=130, height=36,
            corner_radius=6, fg_color=ACCENT, hover_color=ACCENT_HOVER, text_color=BG,
            font=("Segoe UI", 12, "bold")
        )
        play_btn.pack(side="right")

        menu_btn = self.button(actions, "⋯", lambda: self.profile_menu(profile), width=34, height=36)
        menu_btn.pack(side="right", padx=6)

        # Compact Tab Strip (~34px)
        tab_strip = ctk.CTkFrame(page, height=36, corner_radius=0, fg_color=TITLEBAR_BG,
                                 border_color=BORDER, border_width=1)
        tab_strip.pack(fill="x")
        tab_strip.pack_propagate(False)

        tab_box = ctk.CTkFrame(tab_strip, fg_color="transparent")
        tab_box.pack(side="left", padx=16)

        for t_name in ["Overview", f"Mods ({len(profile.mods)})", "Settings"]:
            is_active = (t_name.startswith(tab) or (tab == "Mods" and t_name.startswith("Mods")))
            target = "Mods" if t_name.startswith("Mods") else t_name
            btn = ctk.CTkButton(
                tab_box, text=t_name, width=80, height=34, corner_radius=0,
                fg_color="transparent", hover_color=SURFACE,
                text_color=TEXT if is_active else MUTED,
                border_color=ACCENT if is_active else TITLEBAR_BG,
                border_width=2 if is_active else 0,
                font=("Segoe UI", 11, "bold" if is_active else "normal"),
                command=lambda val=target: self.profile_detail(profile, val)
            )
            btn.pack(side="left", padx=4)

        body = ctk.CTkScrollableFrame(page, fg_color="transparent")
        body.pack(fill="both", expand=True, padx=20, pady=16)

        if tab == "Overview":
            self.overview_tab(body, profile)
        elif tab == "Mods":
            self.installed_tab(body, profile)
        else:
            self.profile_settings_tab(body, profile)

    def overview_tab(self, body: Any, profile: Profile) -> None:
        # GENERAL INFO PANEL
        ctk.CTkLabel(body, text="GENERAL", font=("Segoe UI", 10, "bold"), text_color=SUBTLE).pack(anchor="w", pady=(0, 4))
        
        gen_panel = self.panel(body)
        gen_panel.pack(fill="x", pady=(0, 14))

        gen_rows = [
            ("Minecraft Version", profile.minecraft_version, CYAN),
            ("Mod Loader", "Fabric", ACCENT),
            ("Installed Mods", f"{len(profile.mods)} mods", TEXT),
            ("Java Runtime", java_path() or "Java 21 (System)", TEXT_SECONDARY),
        ]

        for label, val, color in gen_rows:
            r = ctk.CTkFrame(gen_panel, height=32, fg_color="transparent")
            r.pack(fill="x", padx=16, pady=2)
            ctk.CTkLabel(r, text=label, font=("Segoe UI", 10), text_color=MUTED).pack(side="left")
            ctk.CTkLabel(r, text=val, font=("Segoe UI", 10, "bold"), text_color=color).pack(side="right")
            ctk.CTkFrame(gen_panel, height=1, fg_color=BORDER).pack(fill="x", padx=12)

        dir_row = ctk.CTkFrame(gen_panel, height=36, fg_color="transparent")
        dir_row.pack(fill="x", padx=16, pady=4)
        ctk.CTkLabel(dir_row, text="Game Directory", font=("Segoe UI", 10), text_color=MUTED).pack(side="left")
        self.button(dir_row, "Open Folder →", lambda: self.open_folder(profile.path), width=105, height=24).pack(side="right")

        # PERFORMANCE MODS PANEL
        ctk.CTkLabel(body, text="PERFORMANCE MODS", font=("Segoe UI", 10, "bold"), text_color=SUBTLE).pack(anchor="w", pady=(0, 4))

        perf_panel = self.panel(body)
        perf_panel.pack(fill="x", pady=(0, 14))

        perf_mods = [m for m in profile.mods if any(rec[0] in m.slug or rec[0] in m.project_id for rec in RECOMMENDED_MODS)]
        
        if perf_mods:
            grid = ctk.CTkFrame(perf_panel, fg_color="transparent")
            grid.pack(fill="x", padx=16, pady=10)
            grid.grid_columnconfigure((0, 1), weight=1)

            for idx, m in enumerate(perf_mods[:6]):
                row_idx, col_idx = divmod(idx, 2)
                item = ctk.CTkFrame(grid, fg_color="transparent")
                item.grid(row=row_idx, column=col_idx, sticky="w", padx=4, pady=3)
                ctk.CTkLabel(item, text="✓", font=("Segoe UI", 11, "bold"), text_color=ACCENT).pack(side="left", padx=(0, 6))
                ctk.CTkLabel(item, text=m.name, font=("Segoe UI", 10, "bold"), text_color=TEXT).pack(side="left")
                ctk.CTkLabel(item, text=f"({m.version})", font=("Segoe UI", 9), text_color=MUTED).pack(side="left", padx=4)

            footer_row = ctk.CTkFrame(perf_panel, fg_color="transparent")
            footer_row.pack(fill="x", padx=16, pady=(4, 10))
            link_btn = ctk.CTkButton(
                footer_row, text=f"View all {len(profile.mods)} installed mods →",
                command=lambda: self.profile_detail(profile, "Mods"),
                fg_color="transparent", hover_color=SURFACE_2, text_color=CYAN,
                font=("Segoe UI", 10, "bold"), anchor="w"
            )
            link_btn.pack(side="left")
        else:
            ctk.CTkLabel(perf_panel, text="No performance mods installed. Optimize your profile via Modrinth.",
                         font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w", padx=16, pady=12)
            self.button(perf_panel, "+ Install Recommended Stack", lambda: self.mods(profile), primary=True, width=180, height=28).pack(anchor="w", padx=16, pady=(0, 12))

        # PROFILE HEALTH DIAGNOSTICS
        ctk.CTkLabel(body, text="PROFILE HEALTH", font=("Segoe UI", 10, "bold"), text_color=SUBTLE).pack(anchor="w", pady=(0, 4))

        health_panel = self.panel(body)
        health_panel.pack(fill="x")

        health_inner = ctk.CTkFrame(health_panel, fg_color="transparent")
        health_inner.pack(fill="x", padx=16, pady=12)

        ctk.CTkLabel(health_inner, text="●  Everything looks good", font=("Segoe UI", 11, "bold"), text_color=ACCENT_LIGHT).pack(anchor="w")
        ctk.CTkLabel(health_inner, text=f"All {len(profile.mods)} mods are verified for Minecraft {profile.minecraft_version} on Fabric.",
                     font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w", pady=(2, 8))

        self.button(health_inner, "Check for Updates", lambda: self.check_updates(profile), width=130, height=26).pack(anchor="w")

    # ----------------------------------------------------
    # INSTALLED MODS TAB (Dense Desktop List)
    # ----------------------------------------------------
    def installed_tab(self, body: Any, profile: Profile) -> None:
        # Search & Filter Toolbar
        toolbar = self.panel(body, height=44)
        toolbar.pack(fill="x", pady=(0, 10))
        toolbar.pack_propagate(False)

        search_var = ctk.StringVar()
        search_entry = ctk.CTkEntry(
            toolbar, textvariable=search_var, height=30, corner_radius=5,
            fg_color=SURFACE_2, border_color=BORDER, placeholder_text="Search installed mods..."
        )
        search_entry.pack(side="left", padx=10, fill="x", expand=True)

        self.button(toolbar, "+ Browse Modrinth", lambda: self.mods(profile), primary=True, width=130, height=28).pack(side="right", padx=10)
        self.button(toolbar, "🔄 Check Updates", lambda: self.check_updates(profile), width=115, height=28).pack(side="right")
        self.button(toolbar, "📁 Open Folder", lambda: self.open_folder(profile.mods_path), width=105, height=28).pack(side="right", padx=6)

        if not profile.mods:
            empty = self.panel(body)
            empty.pack(fill="x", pady=10)
            ctk.CTkLabel(empty, text="No mods installed yet.", font=("Segoe UI", 13, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(16, 4))
            ctk.CTkLabel(empty, text="Explore Modrinth to install compatible Fabric mods.", font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w", padx=16)
            self.button(empty, "Browse Modrinth", lambda: self.mods(profile), primary=True, width=130, height=28).pack(anchor="w", padx=16, pady=14)
            return

        # Dense Table List Container
        list_panel = self.panel(body)
        list_panel.pack(fill="x")

        # Table header
        th = ctk.CTkFrame(list_panel, height=26, fg_color=SURFACE_2, corner_radius=0)
        th.pack(fill="x")
        th.pack_propagate(False)

        ctk.CTkLabel(th, text="MOD NAME", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, width=280, anchor="w").pack(side="left", padx=14)
        ctk.CTkLabel(th, text="VERSION", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, width=120, anchor="w").pack(side="left")
        ctk.CTkLabel(th, text="FILENAME", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, width=180, anchor="w").pack(side="left")
        ctk.CTkLabel(th, text="STATUS", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, width=90, anchor="w").pack(side="left")
        ctk.CTkLabel(th, text="ACTIONS", font=("Segoe UI", 8, "bold"), text_color=SUBTLE, anchor="e").pack(side="right", padx=14)

        for mod in profile.mods:
            row = ctk.CTkFrame(list_panel, height=44, fg_color="transparent", corner_radius=0)
            row.pack(fill="x")
            row.pack_propagate(False)

            # Left Mod Icon + Name
            left_col = ctk.CTkFrame(row, fg_color="transparent", width=280)
            left_col.pack(side="left", padx=14)
            left_col.pack_propagate(False)

            icon_lbl = ctk.CTkLabel(left_col, text="", width=26, height=26)
            icon_lbl.pack(side="left", padx=(0, 8))
            icon_img = self.icons.load_icon_async(
                mod.icon_url, (26, 26),
                lambda img, lbl=icon_lbl: self.after(0, lambda: lbl.configure(image=img) if lbl.winfo_exists() else None),
                fallback_text=mod.name
            )
            icon_lbl.configure(image=icon_img)

            ctk.CTkLabel(left_col, text=mod.name, font=("Segoe UI", 11, "bold"), text_color=TEXT, anchor="w").pack(side="left")

            # Version pill
            v_chip = ctk.CTkLabel(row, text=mod.version, fg_color=SURFACE_2, text_color=TEXT_SECONDARY,
                                  corner_radius=4, font=("Segoe UI", 9), width=110, height=20)
            v_chip.pack(side="left", padx=(0, 10))

            # Filename
            ctk.CTkLabel(row, text=mod.filename, font=("Segoe UI", 9), text_color=MUTED, width=180, anchor="w").pack(side="left")

            # Toggle enable/disable
            status_text = "Enabled" if mod.enabled else "Disabled"
            status_color = ACCENT if mod.enabled else MUTED
            toggle_btn = ctk.CTkButton(
                row, text=status_text, width=68, height=22, corner_radius=4,
                fg_color=SURFACE_2, hover_color=SURFACE_3, text_color=status_color,
                font=("Segoe UI", 9, "bold"),
                command=lambda m=mod: self.toggle_mod(profile, m)
            )
            toggle_btn.pack(side="left", padx=(0, 10))

            # Action menu button & Right click context menu
            act_btn = self.button(row, "⋯", lambda m=mod: self.mod_context_menu(profile, m), width=28, height=26)
            act_btn.pack(side="right", padx=12)

            def make_right_click(m_target: Mod) -> Callable[[Any], None]:
                return lambda _e: self.mod_context_menu(profile, m_target)

            for w in (row, left_col, icon_lbl):
                w.bind("<Button-3>", make_right_click(mod))

            ctk.CTkFrame(list_panel, height=1, fg_color=BORDER).pack(fill="x")

    def mod_context_menu(self, profile: Profile, mod: Mod) -> None:
        menu = Menu(self, tearoff=False, bg=SURFACE_2, fg=TEXT, activebackground=SURFACE_3,
                    activeforeground=TEXT, relief="flat")
        
        slug = mod.slug or mod.project_id
        menu.add_command(label="View on Modrinth ↗", command=lambda: webbrowser.open(f"https://modrinth.com/mod/{slug}"))
        menu.add_command(label="Toggle Enabled", command=lambda: self.toggle_mod(profile, mod))
        menu.add_command(label="Open mods/ Folder", command=lambda: self.open_folder(profile.mods_path))
        menu.add_separator()
        menu.add_command(label="Remove Mod", command=lambda: self.remove_mod(profile, mod))
        menu.tk_popup(self.winfo_pointerx(), self.winfo_pointery())

    def profile_settings_tab(self, body: Any, profile: Profile) -> None:
        panel = self.panel(body)
        panel.pack(fill="x")

        ctk.CTkLabel(panel, text="Profile Configuration", font=("Segoe UI", 13, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(16, 12))

        name = ctk.StringVar(value=profile.name)
        ctk.CTkLabel(panel, text="Profile Name", font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w", padx=16)
        ctk.CTkEntry(panel, textvariable=name, height=34, corner_radius=5, fg_color=SURFACE_2,
                     border_color=BORDER).pack(fill="x", padx=16, pady=(4, 14))

        ctk.CTkLabel(panel, text=f"Target Version: Minecraft {profile.minecraft_version} (Fabric)",
                     font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w", padx=16)

        def save() -> None:
            if not name.get().strip():
                messagebox.showerror(APP_NAME, "Profile name cannot be empty.")
                return
            profile.name = name.get().strip()
            self.store.save()
            self.profile_detail(profile, "Settings")

        actions = ctk.CTkFrame(panel, fg_color="transparent")
        actions.pack(fill="x", padx=16, pady=16)
        self.button(actions, "Save Changes", save, True, width=120, height=30).pack(side="left")
        self.button(actions, "Delete Profile", lambda: self.delete_profile(profile), danger=True, width=110, height=30).pack(side="right")

    # ----------------------------------------------------
    # MODRINTH PACKAGE MANAGER (Desktop Split View)
    # ----------------------------------------------------
    def mods(self, profile: Profile | None = None) -> None:
        self.set_breadcrumb("Modrinth Explorer")
        page = self.page("Mods")
        profile = profile or self.current or self.store.last()
        self.current = profile

        if not profile:
            card = self.panel(page)
            card.pack(fill="x", padx=20, pady=20)
            ctk.CTkLabel(card, text="Please create a profile first", text_color=TEXT, font=("Segoe UI", 14, "bold")).pack(anchor="w", padx=16, pady=(16, 4))
            self.button(card, "Create Profile", self.create_profile, True, width=130).pack(anchor="w", padx=16, pady=(8, 16))
            return

        ModrinthSplitView(self, page, profile).pack(fill="both", expand=True)

    # ----------------------------------------------------
    # SETTINGS VIEW
    # ----------------------------------------------------
    def settings(self) -> None:
        self.set_breadcrumb("Settings")
        page = self.page("Settings")

        toolbar = ctk.CTkFrame(page, height=44, corner_radius=0, fg_color=SURFACE, border_color=BORDER, border_width=1)
        toolbar.pack(fill="x")
        toolbar.pack_propagate(False)

        ctk.CTkLabel(toolbar, text="Launcher Settings", font=("Segoe UI", 13, "bold"), text_color=TEXT).pack(side="left", padx=16)

        body = ctk.CTkScrollableFrame(page, fg_color="transparent")
        body.pack(fill="both", expand=True, padx=20, pady=16)

        # General Options
        general = self.panel(body)
        general.pack(fill="x", pady=(0, 14))
        ctk.CTkLabel(general, text="General Preferences", font=("Segoe UI", 12, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(14, 8))

        close = ctk.BooleanVar(value=self.store.settings["close_on_launch"])
        updates = ctk.BooleanVar(value=self.store.settings["check_updates"])
        self._setting_row(general, "Close EzClient after game launch", "Minimize CPU and memory usage during gameplay.", close)
        self._setting_row(general, "Check for mod updates automatically", "Query Modrinth for compatible Fabric updates on launch.", updates)

        # Storage & Paths
        storage = self.panel(body)
        storage.pack(fill="x", pady=(0, 14))
        ctk.CTkLabel(storage, text="Storage & Directories", font=("Segoe UI", 12, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(14, 8))
        self._path_row(storage, ".minecraft directory", minecraft_dir())
        self._path_row(storage, "EzClient profiles", PROFILES_DIR)

        # Official Launcher
        launcher = self.panel(body)
        launcher.pack(fill="x")
        info = detect_launcher()
        ctk.CTkLabel(launcher, text="Official Minecraft Launcher Integration", font=("Segoe UI", 12, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(14, 4))
        status_str = "Installed & Verified" if info.installed else "Not detected"
        status_col = ACCENT if info.installed else WARNING
        ctk.CTkLabel(launcher, text=status_str, font=("Segoe UI", 10, "bold"), text_color=status_col).pack(anchor="w", padx=16)

        self.button(launcher, "Test Launch Official Launcher", lambda: self.open_launcher(), width=200, height=28).pack(anchor="w", padx=16, pady=12)

        def save() -> None:
            self.store.settings["close_on_launch"] = close.get()
            self.store.settings["check_updates"] = updates.get()
            self.store.save()
            messagebox.showinfo(APP_NAME, "Settings saved.")

        self.button(body, "Save Settings", save, True, width=130, height=32).pack(anchor="e", pady=16)

    def _setting_row(self, master: Any, title: str, subtitle: str, variable: ctk.BooleanVar) -> None:
        r = ctk.CTkFrame(master, fg_color="transparent")
        r.pack(fill="x", padx=16, pady=6)
        lbls = ctk.CTkFrame(r, fg_color="transparent")
        lbls.pack(side="left", fill="x", expand=True)
        ctk.CTkLabel(lbls, text=title, font=("Segoe UI", 10, "bold"), text_color=TEXT).pack(anchor="w")
        ctk.CTkLabel(lbls, text=subtitle, font=("Segoe UI", 9), text_color=MUTED).pack(anchor="w")
        ctk.CTkSwitch(r, text="", variable=variable, progress_color=ACCENT).pack(side="right")

    def _path_row(self, master: Any, title: str, path: Path) -> None:
        r = ctk.CTkFrame(master, fg_color="transparent")
        r.pack(fill="x", padx=16, pady=6)
        lbls = ctk.CTkFrame(r, fg_color="transparent")
        lbls.pack(side="left", fill="x", expand=True)
        ctk.CTkLabel(lbls, text=title, font=("Segoe UI", 9), text_color=MUTED).pack(anchor="w")
        ctk.CTkLabel(lbls, text=str(path), font=("Segoe UI", 9, "bold"), text_color=TEXT_SECONDARY).pack(anchor="w")
        self.button(r, "Open", lambda: self.open_folder(path), width=70, height=24).pack(side="right")

    # ----------------------------------------------------
    # LAUNCHER EXECUTION & ACTIONS
    # ----------------------------------------------------
    def play(self, profile: Profile) -> None:
        if self.busy:
            return
        self.busy = True
        dialog = ProgressWindow(self, f"Launching {profile.name}", ["Official launcher", "Fabric preparation", "Configuring profile", "Starting game"])

        def work() -> None:
            try:
                dialog.active(0, "Verifying official launcher ...")
                info = detect_launcher()
                if not info.installed:
                    info = install_launcher(dialog.status)
                dialog.done(0)

                dialog.active(1, f"Preparing Fabric for Minecraft {profile.minecraft_version} ...")
                ensure_fabric(profile.minecraft_version, dialog.status)
                dialog.done(1)

                dialog.active(2, "Configuring EzClient profile ...")
                patch_launcher_profile(profile)
                dialog.done(2)

                dialog.active(3, "Opening official Minecraft Launcher ...")
                launch_launcher(info)
                dialog.done(3)

                profile.last_played = now_iso()
                self.store.settings["last_profile"] = profile.id
                self.store.save()
                self.after(0, lambda: self.play_done(dialog))
            except Exception as exc:
                self.after(0, lambda: self.background_error(dialog, exc))

        threading.Thread(target=work, daemon=True).start()

    def play_done(self, dialog: ProgressWindow) -> None:
        self.busy = False
        dialog.finish("Minecraft is ready to play!")
        if self.store.settings["close_on_launch"]:
            self.after(1000, self.destroy)

    def background_error(self, dialog: Any, exc: Exception) -> None:
        self.busy = False
        if dialog.winfo_exists():
            dialog.destroy()
        messagebox.showerror(APP_NAME, str(exc))

    def install_mod(self, profile: Profile, project: str, version_id: str | None = None,
                    done: Callable[[], None] | None = None) -> None:
        if any(mod.project_id == project or mod.slug == project for mod in profile.mods) and not version_id:
            messagebox.showinfo(APP_NAME, "This mod is already installed in this profile.")
            return

        dialog = ProgressWindow(self, "Installing Mod", ["Compatibility check", "Dependencies", "Download & verify"])

        def work() -> None:
            try:
                dialog.active(0, "Checking Fabric compatibility ...")
                dialog.done(0)

                dialog.active(1, "Resolving dependencies ...")
                mod = self.modrinth.install(profile, project, version_id=version_id)
                dialog.done(1)

                dialog.active(2, f"Installing {mod.name} ...")
                self.store.save()
                dialog.done(2)

                def complete() -> None:
                    dialog.finish(f"{mod.name} installed successfully!")
                    if done:
                        self.after(700, done)

                self.after(0, complete)
            except Exception as exc:
                self.after(0, lambda: self.background_error(dialog, exc))

        threading.Thread(target=work, daemon=True).start()

    def toggle_mod(self, profile: Profile, mod: Mod) -> None:
        source = profile.mods_path / mod.filename
        target = source.with_name(source.name + ".disabled") if mod.enabled else Path(str(source).removesuffix(".disabled"))
        try:
            if source.exists():
                source.rename(target)
            mod.filename, mod.enabled = target.name, not mod.enabled
            self.store.save()
            self.profile_detail(profile, "Mods")
        except OSError as exc:
            messagebox.showerror(APP_NAME, str(exc))

    def remove_mod(self, profile: Profile, mod: Mod) -> None:
        if messagebox.askyesno(APP_NAME, f"Remove mod '{mod.name}'?"):
            (profile.mods_path / mod.filename).unlink(missing_ok=True)
            profile.mods.remove(mod)
            self.store.save()
            self.profile_detail(profile, "Mods")

    def check_updates(self, profile: Profile) -> None:
        if not profile.mods:
            messagebox.showinfo(APP_NAME, "No installed mods to check.")
            return

        dialog = ProgressWindow(self, "Checking Mod Updates", [mod.name for mod in profile.mods])

        def work() -> None:
            found: list[tuple[Mod, dict[str, Any]]] = []
            try:
                for index, mod in enumerate(profile.mods):
                    dialog.active(index, f"Checking {mod.name} ...")
                    update = self.modrinth.update_for(profile, mod)
                    if update:
                        found.append((mod, update))
                    dialog.done(index)
                self.after(0, lambda: self.updates_ready(dialog, profile, found))
            except Exception as exc:
                self.after(0, lambda: self.background_error(dialog, exc))

        threading.Thread(target=work, daemon=True).start()

    def updates_ready(self, dialog: ProgressWindow, profile: Profile,
                      updates: list[tuple[Mod, dict[str, Any]]]) -> None:
        dialog.destroy()
        if not updates:
            messagebox.showinfo(APP_NAME, "All installed mods are currently up to date.")
        else:
            UpdateWindow(self, profile, updates)

    def open_launcher(self) -> None:
        try:
            info = detect_launcher()
            if not info.installed:
                messagebox.showinfo(APP_NAME, "The official launcher will be downloaded automatically when you start playing.")
            else:
                launch_launcher(info)
        except Exception as exc:
            messagebox.showerror(APP_NAME, str(exc))

    @staticmethod
    def open_folder(path: Path) -> None:
        ensure(path)
        if sys.platform.startswith("win"):
            os.startfile(path)  # type: ignore[attr-defined]
        elif sys.platform == "darwin":
            subprocess.Popen(["open", str(path)])
        else:
            subprocess.Popen(["xdg-open", str(path)])


# ==========================================
# Modrinth Package Manager (Desktop Split View)
# ==========================================
class ModrinthSplitView(ctk.CTkFrame):
    def __init__(self, app: EzClient, master: Any, profile: Profile) -> None:
        super().__init__(master, fg_color="transparent")
        self.app, self.profile = app, profile
        self.query = ctk.StringVar()
        self.category = "All"
        self.sort = ctk.StringVar(value="relevance")
        self.offset = 0
        self.limit = 20
        self.total_hits = 0
        self.current_hits: list[dict[str, Any]] = []
        self.selected_hit: dict[str, Any] | None = None

        self.build_ui()
        self.search(reset_offset=True)

    def build_ui(self) -> None:
        # Top Desktop Toolbar
        toolbar = ctk.CTkFrame(self, height=44, corner_radius=0, fg_color=SURFACE, border_color=BORDER, border_width=1)
        toolbar.pack(fill="x")
        toolbar.pack_propagate(False)

        # Profile picker
        ctk.CTkLabel(toolbar, text="Profile:", font=("Segoe UI", 9, "bold"), text_color=MUTED).pack(side="left", padx=(12, 6))
        p_menu = ctk.CTkOptionMenu(
            toolbar, values=[p.name for p in self.app.store.profiles], width=140, height=28,
            corner_radius=4, fg_color=SURFACE_2, button_color=SURFACE_3, font=("Segoe UI", 9),
            command=self._on_profile_change
        )
        p_menu.set(self.profile.name)
        p_menu.pack(side="left", padx=(0, 10))

        # Search Entry
        search_entry = ctk.CTkEntry(
            toolbar, textvariable=self.query, height=30, corner_radius=5,
            fg_color=SURFACE_2, border_color=BORDER, placeholder_text="Search Modrinth (e.g. sodium, iris, voice chat)..."
        )
        search_entry.pack(side="left", fill="x", expand=True, padx=(0, 8))
        search_entry.bind("<Return>", lambda _e: self.search(reset_offset=True))

        self.app.button(toolbar, "Search", lambda: self.search(reset_offset=True), primary=True, width=75, height=28).pack(side="left", padx=(0, 10))

        # Category pills in toolbar
        for name in ["All", "Optimization", "Utility", "Tech", "Magic"]:
            btn = ctk.CTkButton(
                toolbar, text=name, width=60, height=24, corner_radius=4,
                fg_color=ACCENT_DARK if name == self.category else SURFACE_2,
                hover_color=SURFACE_3, text_color=TEXT if name == self.category else MUTED,
                font=("Segoe UI", 9, "bold"),
                command=lambda val=name: self.choose_category(val)
            )
            btn.pack(side="left", padx=2)

        # Sort menu right
        ctk.CTkOptionMenu(
            toolbar, variable=self.sort, values=["relevance", "downloads", "follows", "newest", "updated"],
            width=110, height=26, corner_radius=4, fg_color=SURFACE_2, button_color=SURFACE_3,
            font=("Segoe UI", 9), command=lambda _v: self.search(reset_offset=True)
        ).pack(side="right", padx=12)

        # Split View Body (Left: Mod List ~66%, Right: Desktop Inspector ~34%)
        split_body = ctk.CTkFrame(self, fg_color="transparent")
        split_body.pack(fill="both", expand=True)

        # Left list panel
        self.left_pane = ctk.CTkScrollableFrame(split_body, fg_color="transparent")
        self.left_pane.pack(side="left", fill="both", expand=True, padx=(12, 6), pady=10)

        # Right Inspector panel (fixed ~340px)
        self.right_pane = ctk.CTkFrame(split_body, width=340, corner_radius=6, fg_color=SURFACE,
                                       border_color=BORDER, border_width=1)
        self.right_pane.pack(side="right", fill="y", padx=(6, 12), pady=10)
        self.right_pane.pack_propagate(False)

        self.render_inspector_placeholder()

    def _on_profile_change(self, name: str) -> None:
        target = next((p for p in self.app.store.profiles if p.name == name), None)
        if target:
            self.profile = target
            self.app.current = target
            self.search(reset_offset=True)

    def choose_category(self, category: str) -> None:
        self.category = category
        self.search(reset_offset=True)

    def search(self, reset_offset: bool = True) -> None:
        if reset_offset:
            self.offset = 0
            self.current_hits = []
            for child in self.left_pane.winfo_children():
                child.destroy()
            ctk.CTkLabel(self.left_pane, text="Searching Modrinth ...", text_color=MUTED, font=("Segoe UI", 11)).pack(anchor="w", padx=12, pady=16)

        def work() -> None:
            try:
                res = self.app.modrinth.search(
                    query=self.query.get().strip(),
                    version=self.profile.minecraft_version,
                    category=self.category,
                    sort=self.sort.get(),
                    loader="fabric",
                    offset=self.offset,
                    limit=self.limit
                )
                self.after(0, lambda: self.render_results(res, append=not reset_offset))
            except Exception as exc:
                self.after(0, lambda: self.render_error(exc))

        threading.Thread(target=work, daemon=True).start()

    def render_results(self, response: dict[str, Any], append: bool = False) -> None:
        if not append:
            for child in self.left_pane.winfo_children():
                child.destroy()

        hits = response.get("hits", [])
        self.total_hits = response.get("total_hits", 0)
        self.current_hits.extend(hits)

        if not self.current_hits:
            card = self.app.panel(self.left_pane)
            card.pack(fill="x", pady=10)
            ctk.CTkLabel(card, text="No compatible Fabric mods found.", font=("Segoe UI", 12, "bold"), text_color=TEXT).pack(anchor="w", padx=14, pady=(14, 4))
            ctk.CTkLabel(card, text="Try adjusting your search query or category filter.", font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w", padx=14, pady=(0, 14))
            return

        for hit in hits:
            self.render_row(hit)

        if len(self.current_hits) < self.total_hits:
            more_bar = ctk.CTkFrame(self.left_pane, fg_color="transparent")
            more_bar.pack(fill="x", pady=12)
            btn = self.app.button(
                more_bar, f"Load More ({len(self.current_hits)} of {self.total_hits:,})",
                self.load_more, width=220, height=30
            )
            btn.pack(anchor="center")

        # Auto-select first item if none selected
        if not self.selected_hit and self.current_hits:
            self.select_mod(self.current_hits[0])

    def load_more(self) -> None:
        self.offset += self.limit
        self.search(reset_offset=False)

    def render_row(self, hit: dict[str, Any]) -> None:
        is_selected = (self.selected_hit and self.selected_hit.get("project_id") == hit.get("project_id"))
        row = ctk.CTkFrame(
            self.left_pane, height=48, corner_radius=5,
            fg_color=SURFACE_3 if is_selected else SURFACE,
            border_color=BORDER_LIGHT if is_selected else BORDER,
            border_width=1, cursor="hand2"
        )
        row.pack(fill="x", pady=2)
        row.pack_propagate(False)

        # Mod Icon (32x32)
        icon_lbl = ctk.CTkLabel(row, text="", width=32, height=32)
        icon_lbl.pack(side="left", padx=10)
        icon_img = self.app.icons.load_icon_async(
            hit.get("icon_url"), (32, 32),
            lambda img, lbl=icon_lbl: self.after(0, lambda: lbl.configure(image=img) if lbl.winfo_exists() else None),
            fallback_text=hit.get("title", "M")
        )
        icon_lbl.configure(image=icon_img)

        # Info
        info = ctk.CTkFrame(row, fg_color="transparent")
        info.pack(side="left", fill="both", expand=True, pady=6)

        title_row = ctk.CTkFrame(info, fg_color="transparent")
        title_row.pack(anchor="w")
        ctk.CTkLabel(title_row, text=hit.get("title", "Unknown"), font=("Segoe UI", 11, "bold"), text_color=TEXT).pack(side="left")
        ctk.CTkLabel(title_row, text=f"by {hit.get('author', '')}", font=("Segoe UI", 9), text_color=MUTED).pack(side="left", padx=6)

        desc = hit.get("description", "")
        ctk.CTkLabel(info, text=desc[:85] + ("..." if len(desc) > 85 else ""),
                     font=("Segoe UI", 9), text_color=SUBTLE).pack(anchor="w")

        # Stats right
        right_box = ctk.CTkFrame(row, fg_color="transparent")
        right_box.pack(side="right", padx=10)

        dls = format_number(hit.get("downloads", 0))
        ctk.CTkLabel(right_box, text=f"⬇ {dls}", font=("Segoe UI", 9, "bold"), text_color=CYAN).pack(side="left", padx=6)

        proj_id = str(hit.get("project_id") or hit.get("slug"))
        installed = any(m.project_id == proj_id or m.slug == hit.get("slug") for m in self.profile.mods)

        if installed:
            ctk.CTkLabel(right_box, text="✓ Installed", font=("Segoe UI", 9, "bold"), text_color=ACCENT, width=68).pack(side="right")
        else:
            self.app.button(
                right_box, "+ Install",
                lambda p=proj_id: self.app.install_mod(self.profile, p, done=lambda: self.search(reset_offset=False)),
                primary=True, width=64, height=24
            ).pack(side="right")

        def on_click(_event: Any = None) -> None:
            self.select_mod(hit)

        for w in (row, icon_lbl, info, title_row):
            w.bind("<Button-1>", on_click)

    def select_mod(self, hit: dict[str, Any]) -> None:
        self.selected_hit = hit
        self.render_inspector(hit)

    def render_inspector_placeholder(self) -> None:
        for child in self.right_pane.winfo_children():
            child.destroy()
        ctk.CTkLabel(self.right_pane, text="Select a mod to inspect details", font=("Segoe UI", 11), text_color=MUTED).pack(expand=True)

    def render_inspector(self, hit: dict[str, Any]) -> None:
        for child in self.right_pane.winfo_children():
            child.destroy()

        scroll = ctk.CTkScrollableFrame(self.right_pane, fg_color="transparent")
        scroll.pack(fill="both", expand=True, padx=14, pady=12)

        # Header with icon
        icon_lbl = ctk.CTkLabel(scroll, text="", width=48, height=48)
        icon_lbl.pack(anchor="w")
        icon_img = self.app.icons.load_icon_async(
            hit.get("icon_url"), (48, 48),
            lambda img: self.after(0, lambda: icon_lbl.configure(image=img) if icon_lbl.winfo_exists() else None),
            fallback_text=hit.get("title", "M")
        )
        icon_lbl.configure(image=icon_img)

        ctk.CTkLabel(scroll, text=hit.get("title", "Mod Details"), font=("Segoe UI", 14, "bold"), text_color=TEXT).pack(anchor="w", pady=(6, 2))
        ctk.CTkLabel(scroll, text=f"by {hit.get('author', 'Unknown')}", font=("Segoe UI", 10), text_color=MUTED).pack(anchor="w")

        # Action Buttons
        proj_id = str(hit.get("project_id") or hit.get("slug"))
        installed = any(m.project_id == proj_id or m.slug == hit.get("slug") for m in self.profile.mods)

        btn_row = ctk.CTkFrame(scroll, fg_color="transparent")
        btn_row.pack(fill="x", pady=10)

        if installed:
            ctk.CTkLabel(btn_row, text="✓ Installed in profile", font=("Segoe UI", 10, "bold"), text_color=ACCENT).pack(side="left")
        else:
            self.app.button(
                btn_row, "⚡ Install Latest",
                lambda: self.app.install_mod(self.profile, proj_id, done=lambda: self.search(reset_offset=False)),
                primary=True, width=130, height=30
            ).pack(side="left")

        slug = hit.get("slug") or proj_id
        self.app.button(btn_row, "Modrinth ↗", lambda: webbrowser.open(f"https://modrinth.com/mod/{slug}"), cyan=True, width=95, height=30).pack(side="right")

        # Description
        ctk.CTkLabel(scroll, text="DESCRIPTION", font=("Segoe UI", 8, "bold"), text_color=SUBTLE).pack(anchor="w", pady=(8, 2))
        desc = hit.get("description", "No description provided.")
        ctk.CTkLabel(scroll, text=desc, font=("Segoe UI", 9), text_color=TEXT_SECONDARY, wraplength=280, justify="left").pack(anchor="w")

        # Metadata table
        ctk.CTkLabel(scroll, text="SPECIFICATIONS", font=("Segoe UI", 8, "bold"), text_color=SUBTLE).pack(anchor="w", pady=(12, 4))
        
        spec_box = ctk.CTkFrame(scroll, fg_color=SURFACE_2, corner_radius=5)
        spec_box.pack(fill="x", pady=4)

        specs = [
            ("Downloads", format_number(hit.get("downloads", 0))),
            ("Followers", format_number(hit.get("follows", 0))),
            ("Target MC", self.profile.minecraft_version),
            ("Client Side", str(hit.get("client_side", "required")).title()),
            ("License", str(hit.get("license", "Unknown"))),
        ]

        for label, val in specs:
            r = ctk.CTkFrame(spec_box, fg_color="transparent")
            r.pack(fill="x", padx=10, pady=3)
            ctk.CTkLabel(r, text=label, font=("Segoe UI", 8), text_color=MUTED).pack(side="left")
            ctk.CTkLabel(r, text=val, font=("Segoe UI", 8, "bold"), text_color=TEXT).pack(side="right")

        # Versions Inspector
        ctk.CTkLabel(scroll, text="AVAILABLE RELEASES", font=("Segoe UI", 8, "bold"), text_color=SUBTLE).pack(anchor="w", pady=(12, 4))
        self.versions_container = ctk.CTkFrame(scroll, fg_color="transparent")
        self.versions_container.pack(fill="x")
        
        ctk.CTkLabel(self.versions_container, text="Loading releases ...", font=("Segoe UI", 9), text_color=MUTED).pack(anchor="w")

        def load_vers() -> None:
            try:
                v_list = self.app.modrinth.project_versions(proj_id, mc_version=self.profile.minecraft_version, loader="fabric")
                self.after(0, lambda: self.render_inspector_versions(v_list, proj_id))
            except Exception:
                pass

        threading.Thread(target=load_vers, daemon=True).start()

    def render_inspector_versions(self, versions: list[dict[str, Any]], proj_id: str) -> None:
        for child in self.versions_container.winfo_children():
            child.destroy()

        if not versions:
            ctk.CTkLabel(self.versions_container, text="No specific version files found.", font=("Segoe UI", 9), text_color=MUTED).pack(anchor="w")
            return

        for v in versions[:4]:
            v_box = ctk.CTkFrame(self.versions_container, fg_color=SURFACE_2, corner_radius=4)
            v_box.pack(fill="x", pady=2)

            top = ctk.CTkFrame(v_box, fg_color="transparent")
            top.pack(fill="x", padx=8, pady=4)

            v_num = v.get("version_number", "v1.0")
            ctk.CTkLabel(top, text=f"v{v_num}", font=("Segoe UI", 9, "bold"), text_color=TEXT).pack(side="left")

            v_id = v.get("id")
            installed_v = any(m.version_id == v_id for m in self.profile.mods)

            if installed_v:
                ctk.CTkLabel(top, text="Installed", font=("Segoe UI", 8, "bold"), text_color=ACCENT).pack(side="right")
            else:
                self.app.button(
                    top, "Install",
                    lambda vid=v_id: self.app.install_mod(self.profile, proj_id, version_id=vid, done=lambda: self.search(reset_offset=False)),
                    primary=True, width=54, height=20
                ).pack(side="right")

    def render_error(self, exc: Exception) -> None:
        for child in self.left_pane.winfo_children():
            child.destroy()
        card = self.app.panel(self.left_pane)
        card.pack(fill="x", pady=10)
        ctk.CTkLabel(card, text="Modrinth query failed", font=("Segoe UI", 12, "bold"), text_color=DANGER).pack(anchor="w", padx=14, pady=(14, 4))
        ctk.CTkLabel(card, text=str(exc), font=("Segoe UI", 9), text_color=MUTED).pack(anchor="w", padx=14)
        self.app.button(card, "Retry", lambda: self.search(reset_offset=True), width=80, height=26).pack(anchor="w", padx=14, pady=12)


# ==========================================
# Desktop Progress & Installer Dialog
# ==========================================
class ProgressWindow(ctk.CTkToplevel):
    def __init__(self, master: "EzClient", title: str, steps: list[str]) -> None:
        super().__init__(master)
        self.steps = steps
        self.rows: list[ctk.CTkLabel] = []
        self.title(title)
        self.geometry("480x420")
        self.resizable(False, False)
        self.configure(fg_color=BG)
        self.transient(master)
        self.grab_set()

        card = ctk.CTkFrame(self, fg_color=SURFACE, border_color=BORDER, border_width=1, corner_radius=6)
        card.pack(fill="both", expand=True, padx=16, pady=16)

        ctk.CTkLabel(card, text=title, font=("Segoe UI", 14, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(16, 2))
        self.message = ctk.CTkLabel(card, text="Initializing installer...", font=("Segoe UI", 10), text_color=MUTED)
        self.message.pack(anchor="w", padx=16)

        self.progress = ctk.CTkProgressBar(card, progress_color=ACCENT, fg_color=SURFACE_2, height=4, corner_radius=2)
        self.progress.pack(fill="x", padx=16, pady=(12, 12))
        self.progress.set(0)

        panel = ctk.CTkScrollableFrame(card, fg_color=SURFACE_2, corner_radius=4)
        panel.pack(fill="both", expand=True, padx=16, pady=(0, 16))
        for step in steps:
            row = ctk.CTkLabel(panel, text=f"○  {step}", font=("Segoe UI", 10), text_color=MUTED, anchor="w")
            row.pack(fill="x", padx=8, pady=3)
            self.rows.append(row)

    def status(self, text: str) -> None:
        self.after(0, lambda: self.message.configure(text=text))

    def active(self, index: int, text: str) -> None:
        def update() -> None:
            self.message.configure(text=text)
            if index < len(self.rows):
                self.rows[index].configure(text=f"●  {self.steps[index]}", text_color=ACCENT_LIGHT)
        self.after(0, update)

    def done(self, index: int) -> None:
        def update() -> None:
            if index < len(self.rows):
                self.rows[index].configure(text=f"✓  {self.steps[index]}", text_color=ACCENT)
            self.progress.set((index + 1) / max(1, len(self.rows)))
        self.after(0, update)

    def finish(self, text: str) -> None:
        self.message.configure(text=text, text_color=ACCENT)
        self.after(700, self.destroy)


# ==========================================
# Profile Creation Desktop Wizard
# ==========================================
class CreateFlow(ctk.CTkToplevel):
    def __init__(self, app: EzClient) -> None:
        super().__init__(app)
        self.app = app
        self.step = 1
        self.name = ctk.StringVar()
        self.version = ctk.StringVar(value="1.21.8")
        self.optimize = ctk.BooleanVar(value=True)
        self.profile: Profile | None = None

        self.title("New Profile Wizard - EzClient")
        self.geometry("580x480")
        self.resizable(False, False)
        self.configure(fg_color=BG)
        self.transient(app)
        self.grab_set()

        self.render()
        threading.Thread(target=self.load_versions, daemon=True).start()

    def render(self) -> None:
        for child in self.winfo_children():
            child.destroy()

        header = ctk.CTkFrame(self, height=44, corner_radius=0, fg_color=SURFACE, border_color=BORDER, border_width=1)
        header.pack(fill="x")
        header.pack_propagate(False)

        ctk.CTkLabel(header, text="Create Minecraft Profile", font=("Segoe UI", 12, "bold"), text_color=TEXT).pack(side="left", padx=16)

        steps = ["1. Details", "2. Stack", "3. Setup"]
        for idx, lbl in enumerate(steps, 1):
            is_active = (idx <= self.step)
            ctk.CTkLabel(header, text=lbl, font=("Segoe UI", 9, "bold"),
                         text_color=ACCENT if is_active else SUBTLE).pack(side="right", padx=10)

        self.body = ctk.CTkFrame(self, fg_color="transparent")
        self.body.pack(fill="both", expand=True, padx=24, pady=16)

        if self.step == 1:
            self.step_details()
        elif self.step == 2:
            self.step_stack()
        else:
            self.step_installer()

    def step_details(self) -> None:
        panel = self.app.panel(self.body)
        panel.pack(fill="both", expand=True)

        ctk.CTkLabel(panel, text="Profile Identity", font=("Segoe UI", 12, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(16, 4))
        ctk.CTkLabel(panel, text="Choose a profile name and target Minecraft release.", font=("Segoe UI", 9), text_color=MUTED).pack(anchor="w", padx=16, pady=(0, 12))

        ctk.CTkLabel(panel, text="Profile Name", font=("Segoe UI", 9, "bold"), text_color=MUTED).pack(anchor="w", padx=16)
        entry = ctk.CTkEntry(panel, textvariable=self.name, height=34, corner_radius=5, fg_color=SURFACE_2, border_color=BORDER, placeholder_text="e.g. Survival 1.21")
        entry.pack(fill="x", padx=16, pady=(4, 14))

        ctk.CTkLabel(panel, text="Minecraft Release", font=("Segoe UI", 9, "bold"), text_color=MUTED).pack(anchor="w", padx=16)
        self.v_menu = ctk.CTkOptionMenu(panel, variable=self.version, values=FALLBACK_VERSIONS, height=34, corner_radius=5, fg_color=SURFACE_2, button_color=SURFACE_3)
        self.v_menu.pack(fill="x", padx=16, pady=(4, 16))

        footer = ctk.CTkFrame(self.body, fg_color="transparent")
        footer.pack(fill="x", pady=(12, 0))
        self.app.button(footer, "Cancel", self.destroy, width=80, height=30).pack(side="left")
        self.app.button(footer, "Continue →", self.next_step, primary=True, width=110, height=30).pack(side="right")
        entry.focus_set()

    def load_versions(self) -> None:
        versions = self.app.modrinth.versions()
        self.after(0, lambda: self.v_menu.configure(values=versions) if hasattr(self, "v_menu") and self.v_menu.winfo_exists() else None)

    def next_step(self) -> None:
        if not self.name.get().strip():
            messagebox.showerror(APP_NAME, "Please specify a valid profile name.", parent=self)
            return
        self.step = 2
        self.render()

    def step_stack(self) -> None:
        panel = self.app.panel(self.body)
        panel.pack(fill="both", expand=True)

        ctk.CTkLabel(panel, text="Optimization Stack", font=("Segoe UI", 12, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(16, 4))
        ctk.CTkLabel(panel, text="Select your initial mod configuration.", font=("Segoe UI", 9), text_color=MUTED).pack(anchor="w", padx=16, pady=(0, 12))

        # Option 1: Performance Stack
        self._radio_opt(panel, True, "⚡ Recommended Performance Stack", "Pre-configures Sodium, Lithium, FerriteCore and Entity Culling for optimal FPS.")
        # Option 2: Clean Fabric
        self._radio_opt(panel, False, "Clean Fabric Loader", "Pure Fabric installation without pre-bundled performance mods.")

        footer = ctk.CTkFrame(self.body, fg_color="transparent")
        footer.pack(fill="x", pady=(12, 0))
        self.app.button(footer, "← Back", lambda: self.go(1), width=80, height=30).pack(side="left")
        self.app.button(footer, "Create Profile →", self.start_install, primary=True, width=130, height=30).pack(side="right")

    def _radio_opt(self, parent: Any, val: bool, title: str, desc: str) -> None:
        is_sel = (self.optimize.get() == val)
        card = ctk.CTkFrame(parent, fg_color=SURFACE_3 if is_sel else SURFACE_2,
                            border_color=ACCENT if is_sel else BORDER, border_width=1, corner_radius=5, cursor="hand2")
        card.pack(fill="x", padx=16, pady=4)

        top = ctk.CTkFrame(card, fg_color="transparent")
        top.pack(fill="x", padx=12, pady=(10, 2))
        ctk.CTkLabel(top, text=title, font=("Segoe UI", 10, "bold"), text_color=TEXT).pack(side="left")
        
        ctk.CTkLabel(card, text=desc, font=("Segoe UI", 8), text_color=MUTED, wraplength=480, justify="left").pack(anchor="w", padx=12, pady=(0, 10))

        def choose(_e: Any = None) -> None:
            self.optimize.set(val)
            self.render()

        for w in (card, top):
            w.bind("<Button-1>", choose)

    def go(self, step: int) -> None:
        self.step = step
        self.render()

    def start_install(self) -> None:
        self.profile = self.app.store.create(self.name.get().strip(), self.version.get(), self.optimize.get())
        self.step = 3
        self.render()
        threading.Thread(target=self.run_install, daemon=True).start()

    def step_installer(self) -> None:
        panel = self.app.panel(self.body)
        panel.pack(fill="both", expand=True)

        ctk.CTkLabel(panel, text=f"Configuring \"{self.profile.name}\"", font=("Segoe UI", 12, "bold"), text_color=TEXT).pack(anchor="w", padx=16, pady=(16, 2))
        self.inst_status = ctk.CTkLabel(panel, text="Checking launcher ...", font=("Segoe UI", 9), text_color=MUTED)
        self.inst_status.pack(anchor="w", padx=16)

        self.inst_progress = ctk.CTkProgressBar(panel, progress_color=ACCENT, fg_color=SURFACE_2, height=4, corner_radius=2)
        self.inst_progress.pack(fill="x", padx=16, pady=(10, 10))
        self.inst_progress.set(0)

        self.inst_log = ctk.CTkScrollableFrame(panel, fg_color=SURFACE_2, corner_radius=4, height=180)
        self.inst_log.pack(fill="both", expand=True, padx=16, pady=(0, 16))

    def log_step(self, text: str, progress: float | None = None) -> None:
        def update() -> None:
            self.inst_status.configure(text=text)
            if progress is not None:
                self.inst_progress.set(progress)
            ctk.CTkLabel(self.inst_log, text=f"✓  {text}", font=("Segoe UI", 9), text_color=ACCENT_LIGHT).pack(anchor="w", padx=8, pady=2)
        self.after(0, update)

    def run_install(self) -> None:
        try:
            info = detect_launcher()
            if not info.installed:
                info = install_launcher(lambda t: self.after(0, lambda: self.inst_status.configure(text=t)))
            self.log_step("Official Minecraft Launcher verified", 0.1)

            ensure_fabric(self.profile.minecraft_version, lambda t: self.after(0, lambda: self.inst_status.configure(text=t)))
            self.log_step("Fabric Loader installed", 0.25)

            if self.profile.optimize:
                for idx, (slug, name) in enumerate(RECOMMENDED_MODS, 1):
                    self.after(0, lambda val=name: self.inst_status.configure(text=f"Installing {val} ..."))
                    try:
                        mod = self.app.modrinth.install(self.profile, slug, recommended=True)
                        mod.recommended = True
                        self.app.store.save()
                        self.log_step(f"Installed {name}", 0.25 + (idx / len(RECOMMENDED_MODS)) * 0.7)
                    except Exception as exc:
                        self.log_step(f"Skipped {name}: {exc}", 0.25 + (idx / len(RECOMMENDED_MODS)) * 0.7)

            patch_launcher_profile(self.profile)
            self.app.store.save()
            self.after(500, self.finish)
        except Exception as exc:
            self.after(0, lambda: self.failed(exc))

    def finish(self) -> None:
        self.destroy()
        self.app.profile_detail(self.profile)

    def failed(self, exc: Exception) -> None:
        messagebox.showerror(APP_NAME, str(exc), parent=self)
        self.destroy()
        self.app.profiles()


# ==========================================
# Mod Updates Window
# ==========================================
class UpdateWindow(ctk.CTkToplevel):
    def __init__(self, app: EzClient, profile: Profile,
                 updates: list[tuple[Mod, dict[str, Any]]]) -> None:
        super().__init__(app)
        self.app, self.profile, self.updates = app, profile, updates
        self.title("Mod Updates - EzClient")
        self.geometry("520x440")
        self.resizable(False, False)
        self.configure(fg_color=BG)
        self.transient(app)
        self.grab_set()

        ctk.CTkLabel(self, text=f"Mod Updates ({len(updates)} available)", font=("Segoe UI", 13, "bold"), text_color=TEXT).pack(anchor="w", padx=20, pady=(18, 2))
        ctk.CTkLabel(self, text=f"Newer compatible releases for Minecraft {profile.minecraft_version} on Fabric.", font=("Segoe UI", 9), text_color=MUTED).pack(anchor="w", padx=20)

        body = ctk.CTkScrollableFrame(self, fg_color="transparent")
        body.pack(fill="both", expand=True, padx=16, pady=12)

        for mod, version in updates:
            row = app.panel(body, height=48)
            row.pack(fill="x", pady=2)
            row.pack_propagate(False)

            info = ctk.CTkFrame(row, fg_color="transparent")
            info.pack(side="left", padx=12, pady=6)
            ctk.CTkLabel(info, text=mod.name, font=("Segoe UI", 10, "bold"), text_color=TEXT).pack(anchor="w")
            
            v_str = f"{mod.version}  ➜  {version.get('version_number', 'new')}"
            ctk.CTkLabel(info, text=v_str, font=("Segoe UI", 8), text_color=ACCENT_LIGHT).pack(anchor="w")

        footer = ctk.CTkFrame(self, fg_color="transparent")
        footer.pack(fill="x", padx=20, pady=(0, 18))
        app.button(footer, "Update All Mods", self.update_all, primary=True, width=130, height=30).pack(side="right")
        app.button(footer, "Later", self.destroy, width=75, height=30).pack(side="right", padx=8)

    def update_all(self) -> None:
        self.destroy()
        dialog = ProgressWindow(self.app, "Updating Mods", [mod.name for mod, _ in self.updates])

        def work() -> None:
            try:
                for index, (mod, _version) in enumerate(self.updates):
                    dialog.active(index, f"Updating {mod.name} ...")
                    (self.profile.mods_path / mod.filename).unlink(missing_ok=True)
                    self.profile.mods.remove(mod)
                    self.app.modrinth.install(self.profile, mod.project_id, recommended=mod.recommended)
                    dialog.done(index)
                self.app.store.save()
                self.app.after(0, lambda: (dialog.finish("All mod updates installed!"),
                                           self.app.profile_detail(self.profile, "Mods")))
            except Exception as exc:
                self.app.after(0, lambda: self.app.background_error(dialog, exc))

        threading.Thread(target=work, daemon=True).start()


# ==========================================
# Main Entry Point
# ==========================================
def main() -> None:
    EzClient().mainloop()


if __name__ == "__main__":
    main()
