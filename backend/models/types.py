from dataclasses import dataclass, field, asdict
from datetime import datetime, timezone
from pathlib import Path
import os
import sys

APP_NAME = "EzClient"
APP_VERSION = "1.5.4"
GITHUB_REPO = "LuigiLetsPlay/EzClient"

def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")

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

@dataclass
class ModData:
    project_id: str
    slug: str
    name: str
    version_id: str
    version: str
    filename: str
    enabled: bool = True
    recommended: bool = False
    essential: bool = False
    icon_url: str = ""
    author: str = ""
    description: str = ""
    source: str = "modrinth"

@dataclass
class ProfileData:
    id: str
    name: str
    minecraft_version: str
    optimize: bool = True
    loader: str = "Fabric"
    ram_mb: int = 4096
    jvm_args: str = "-XX:+UseG1GC -Dsun.rmi.dgc.server.gcInterval=2147483646 -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M"
    created: str = field(default_factory=now_iso)
    last_played: str = ""
    mods: list[ModData] = field(default_factory=list)
    integrated_mods: list[str] = field(default_factory=list)

    @property
    def path(self) -> Path:
        return PROFILES_DIR / self.id

    @property
    def mods_path(self) -> Path:
        return self.path / "mods"
