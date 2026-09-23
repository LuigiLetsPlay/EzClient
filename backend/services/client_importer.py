"""Read-only discovery of profiles from locally installed third-party launchers."""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any

from backend.services.norisk_importer import discover_norisk_profiles


def _json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
        return value if isinstance(value, dict) else {}
    except (OSError, ValueError):
        return {}


def _loader(value: str) -> str:
    lowered = value.lower()
    if "neoforge" in lowered:
        return "NeoForge"
    if "forge" in lowered:
        return "Forge"
    if "quilt" in lowered:
        return "Quilt"
    if "fabric" in lowered:
        return "Fabric"
    return "Vanilla"


def _entry(client: str, icon: str, source: Path, name: str, version: str,
           loader: str, raw: dict[str, Any] | None = None) -> dict[str, Any] | None:
    if not source.is_dir() or not version:
        return None
    mods = source / "mods"
    mod_count = len(list(mods.glob("*.jar"))) + len(list(mods.glob("*.jar.disabled"))) if mods.is_dir() else 0
    return {
        "id": f"{client.lower()}::{source}",
        "name": name or source.name,
        "version": version,
        "loader": loader or "Vanilla",
        "ramMb": 4096,
        "modCount": mod_count,
        "path": str(source),
        "raw": raw or {},
        "sourceClient": client,
        "sourceIcon": icon,
        "hasXaeroWaypoints": False,
        "canConvertXaeroWaypoints": False,
        "xaeroWaypointCount": 0,
    }


def _discover_modrinth(appdata: Path) -> list[dict[str, Any]]:
    root = appdata / "com.modrinth.theseus" / "profiles"
    result = []
    if not root.is_dir():
        return result
    for folder in root.iterdir():
        data = _json(folder / "profile.json")
        version = str(data.get("game_version") or data.get("gameVersion") or "")
        item = _entry("Modrinth", "modrinth.svg", folder,
                      str(data.get("name") or folder.name), version,
                      _loader(str(data.get("loader") or "vanilla")))
        if item:
            result.append(item)
    return result


def _discover_curseforge(home: Path, appdata: Path) -> list[dict[str, Any]]:
    roots = [home / "curseforge" / "minecraft" / "Instances",
             appdata / "CurseForge" / "minecraft" / "Instances"]
    result = []
    seen: set[str] = set()
    for root in roots:
        if not root.is_dir():
            continue
        for folder in root.iterdir():
            data = _json(folder / "minecraftinstance.json")
            version = str(data.get("gameVersion") or data.get("minecraftVersion") or "")
            base_loader = data.get("baseModLoader")
            loader_text = str(base_loader.get("name") or base_loader.get("type") or "") if isinstance(base_loader, dict) else str(base_loader or "")
            key = str(folder.resolve()).casefold()
            if key in seen:
                continue
            item = _entry("CurseForge", "curseforge.svg", folder,
                          str(data.get("name") or folder.name), version,
                          _loader(loader_text))
            if item:
                result.append(item)
                seen.add(key)
    return result


def _cfg(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    try:
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                result[key.strip()] = value.strip()
    except OSError:
        pass
    return result


def _discover_prism(appdata: Path) -> list[dict[str, Any]]:
    result = []
    for client, root in (("Prism Launcher", appdata / "PrismLauncher" / "instances"),
                         ("MultiMC", appdata / "MultiMC" / "instances")):
        if not root.is_dir():
            continue
        for folder in root.iterdir():
            cfg = _cfg(folder / "instance.cfg")
            pack = _json(folder / "mmc-pack.json")
            components = pack.get("components") if isinstance(pack.get("components"), list) else []
            version = str(cfg.get("IntendedVersion") or "")
            loader_text = ""
            for component in components:
                if not isinstance(component, dict):
                    continue
                uid = str(component.get("uid") or "")
                if uid == "net.minecraft":
                    version = str(component.get("version") or version)
                elif any(key in uid.lower() for key in ("fabric", "forge", "quilt")):
                    loader_text = uid
            source = folder / ".minecraft" if (folder / ".minecraft").is_dir() else folder
            item = _entry(client, "extension-grid.svg", source,
                          cfg.get("name") or folder.name, version,
                          _loader(loader_text))
            if item:
                result.append(item)
    return result


def discover_client_profiles() -> list[dict[str, Any]]:
    """Return importable profiles without changing any third-party files."""
    appdata = Path(os.environ.get("APPDATA") or (Path.home() / "AppData" / "Roaming"))
    home = Path.home()
    result: list[dict[str, Any]] = []
    for item in discover_norisk_profiles():
        item = dict(item)
        item.update(sourceClient="NoRiskClient", sourceIcon="client-norisk.svg")
        result.append(item)
    result.extend(_discover_modrinth(appdata))
    result.extend(_discover_curseforge(home, appdata))
    result.extend(_discover_prism(appdata))
    return sorted(result, key=lambda item: (str(item.get("sourceClient", "")), str(item.get("name", "")).casefold()))
