"""Read and import locally installed NoRiskClient V3 profiles."""

from __future__ import annotations

import hashlib
import json
import os
import shutil
import urllib.parse
import urllib.request
import uuid
import zipfile
from pathlib import Path
from typing import Any, Callable

from backend.models.types import ModData, ProfileData
from backend.services.curseforge import _make_request as curseforge_make_request
from backend.services.mod_downloader import download_file
from backend.services.mod_scanner import extract_jar_metadata
from backend.services.modrinth import USER_AGENT, get_json


def default_norisk_root() -> Path:
    candidates: list[Path] = []
    appdata = os.environ.get("APPDATA")
    if appdata:
        candidates.append(Path(appdata) / "norisk" / "NoRiskClientV3")
    candidates.extend([
        Path.home() / "AppData" / "Roaming" / "norisk" / "NoRiskClientV3",
        Path.home() / ".config" / "norisk" / "NoRiskClientV3",
        Path.home() / ".norisk" / "NoRiskClientV3",
    ])
    for candidate in candidates:
        if candidate.is_dir():
            return candidate
    return candidates[0] if candidates else Path.home() / ".norisk" / "NoRiskClientV3"


def discover_norisk_profiles(root: Path | None = None) -> list[dict[str, Any]]:
    base = Path(root) if root else default_norisk_root()
    manifest = base / "profiles.json"
    if not manifest.is_file():
        return []
    try:
        payload = json.loads(manifest.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return []
    raw_profiles = payload if isinstance(payload, list) else payload.get("profiles", [])
    result: list[dict[str, Any]] = []
    seen: set[str] = set()
    for raw in raw_profiles if isinstance(raw_profiles, list) else []:
        if not isinstance(raw, dict):
            continue
        p_id = str(raw.get("id") or "").strip()
        relative = str(raw.get("path") or p_id).strip()
        source = (base / "data" / "profiles" / relative).resolve()
        if not relative or not source.is_dir():
            continue
        key = str(source).casefold()
        if key in seen:
            continue
        seen.add(key)
        settings = raw.get("settings") if isinstance(raw.get("settings"), dict) else {}
        memory = settings.get("memory") if isinstance(settings.get("memory"), dict) else {}
        mods = [m for m in raw.get("mods", []) if isinstance(m, dict)]

        # Count extra custom jars if present on disk
        extra_count = 0
        for folder in ("mods", "custom_mods"):
            dir_path = source / folder
            if dir_path.is_dir():
                for jar in dir_path.glob("*.jar"):
                    if not jar.name.startswith("nrc-") and not jar.name.lower().startswith("norisk"):
                        extra_count += 1

        xaero_waypoints = discover_xaero_waypoints(source)
        game_version = str(raw.get("game_version") or "")
        result.append({
            "id": p_id or relative,
            "name": str(raw.get("name") or relative),
            "version": game_version,
            "loader": str(raw.get("loader") or "Fabric").title(),
            "loaderVersion": str(raw.get("loader_version") or ""),
            "ramMb": int(memory.get("max") or 4096),
            "modCount": max(len(mods), extra_count),
            "path": str(source),
            "norisk_root": str(base),
            "hasXaeroWaypoints": bool(xaero_waypoints),
            "canConvertXaeroWaypoints": bool(xaero_waypoints) and game_version in {"26.1", "26.1.1", "26.2"},
            "xaeroWaypointCount": len(xaero_waypoints),
            "raw": raw,
        })
    return result


_COPY_DIRS = (
    "config", "resourcepacks", "shaderpacks", "saves", "screenshots",
    "schematics", "xaero", "XaeroWaypoints", "XaeroWorldMap", "essential",
)
_COPY_FILES = (
    "options.txt", "optionsof.txt", "optionsshaders.txt", "servers.dat",
    "servers.dat_old", "servers.essential.dat", "iris.properties",
)


def _copy_tree(source: Path, target: Path, skip_names: set[str] | None = None) -> None:
    if source.is_dir():
        skipped = {name.casefold() for name in (skip_names or set())}
        target.mkdir(parents=True, exist_ok=True)
        for item in source.iterdir():
            # Skip NoRisk internal folders and transient library caches
            skip_xaero = "xaero" in skipped and item.name.casefold().startswith("xaero")
            if skip_xaero or item.name.casefold() in skipped or item.name.startswith("nrc-") or item.name.lower() in (
                "noriskclient", "noriskclientlauncher", "logs", "crash-reports", ".fabric",
                "libraries", "loader", "image-cache", "screenshot-cache", "cosmetic-cache"
            ):
                continue
            dest_item = target / item.name
            if item.is_dir():
                shutil.copytree(
                    item, dest_item, dirs_exist_ok=True,
                    ignore=shutil.ignore_patterns("libraries", "loader", "cache", "*cache*", "xaero*", "Xaero*")
                    if "xaero" in skipped else shutil.ignore_patterns("libraries", "loader", "cache", "*cache*", *(skip_names or ()))
                )
            elif item.is_file():
                shutil.copy2(item, dest_item)


_XAERO_COLORS = (
    0xFF202020, 0xFF3546B8, 0xFF2E9E55, 0xFF2FA7A0,
    0xFFB83A3A, 0xFF9B4DB8, 0xFFE39A32, 0xFFAAAAAA,
    0xFF555555, 0xFF4D6FFF, 0xFF45D66B, 0xFF46D9D0,
    0xFFFF5555, 0xFFFF63D8, 0xFFFFD84A, 0xFFFFFFFF,
)


def _clean_servers_nbt(data: bytes) -> bytes:
    import io
    import struct
    import gzip

    is_gzip = data[:2] == b"\x1f\x8b"
    raw = gzip.decompress(data) if is_gzip else data
    bio = io.BytesIO(raw)

    def read_tag(bio, tag_type):
        if tag_type == 0:
            return None
        elif tag_type == 1:
            return bio.read(1)
        elif tag_type == 2:
            return bio.read(2)
        elif tag_type == 3:
            return bio.read(4)
        elif tag_type == 4:
            return bio.read(8)
        elif tag_type == 5:
            return bio.read(4)
        elif tag_type == 6:
            return bio.read(8)
        elif tag_type == 7:
            length = struct.unpack(">i", bio.read(4))[0]
            return struct.pack(">i", length) + bio.read(length)
        elif tag_type == 8:
            length = struct.unpack(">h", bio.read(2))[0]
            return bio.read(length).decode("utf-8", errors="replace")
        elif tag_type == 9:
            elem_type = bio.read(1)[0]
            count = struct.unpack(">i", bio.read(4))[0]
            items = [read_tag(bio, elem_type) for _ in range(count)]
            return (elem_type, items)
        elif tag_type == 10:
            comp = {}
            while True:
                tt_b = bio.read(1)
                if not tt_b or tt_b[0] == 0:
                    break
                tt = tt_b[0]
                nl = struct.unpack(">h", bio.read(2))[0]
                name = bio.read(nl).decode("utf-8", errors="replace")
                comp[name] = (tt, read_tag(bio, tt))
            return comp
        elif tag_type == 11:
            length = struct.unpack(">i", bio.read(4))[0]
            return struct.pack(">i", length) + bio.read(length * 4)
        elif tag_type == 12:
            length = struct.unpack(">i", bio.read(4))[0]
            return struct.pack(">i", length) + bio.read(length * 8)
        raise ValueError(f"Unknown tag type {tag_type}")

    def write_tag(bio, tag_type, val):
        if tag_type in (1, 2, 3, 4, 5, 6, 7, 11, 12):
            bio.write(val)
        elif tag_type == 8:
            raw_str = val.encode("utf-8")
            bio.write(struct.pack(">h", len(raw_str)))
            bio.write(raw_str)
        elif tag_type == 9:
            elem_type, items = val
            bio.write(bytes([elem_type]))
            bio.write(struct.pack(">i", len(items)))
            for it in items:
                write_tag(bio, elem_type, it)
        elif tag_type == 10:
            for name, (tt, v) in val.items():
                bio.write(bytes([tt]))
                nb = name.encode("utf-8")
                bio.write(struct.pack(">h", len(nb)))
                bio.write(nb)
                write_tag(bio, tt, v)
            bio.write(b"\x00")

    root_type = bio.read(1)[0]
    nl = struct.unpack(">h", bio.read(2))[0]
    root_name = bio.read(nl).decode("utf-8", errors="replace")
    root_val = read_tag(bio, root_type)

    if isinstance(root_val, dict) and "servers" in root_val:
        elem_type, server_list = root_val["servers"][1]
        filtered = []
        for s in server_list:
            if not isinstance(s, dict):
                filtered.append(s)
                continue
            s_name = str(s.get("name", (8, ""))[1])
            s_ip = str(s.get("ip", (8, ""))[1])
            low_name = s_name.lower()
            low_ip = s_ip.lower()
            if any(ad in low_name or ad in low_ip for ad in ("norisk", "advert")):
                continue
            filtered.append(s)
        root_val["servers"] = (9, (elem_type, filtered))

    out = io.BytesIO()
    out.write(bytes([root_type]))
    nb = root_name.encode("utf-8")
    out.write(struct.pack(">h", len(nb)))
    out.write(nb)
    write_tag(out, root_type, root_val)
    res = out.getvalue()
    return gzip.compress(res) if is_gzip else res


def _copy_cleaned_servers_dat(source: Path, destination: Path) -> None:
    try:
        data = source.read_bytes()
        cleaned = _clean_servers_nbt(data)
        destination.write_bytes(cleaned)
    except Exception:
        shutil.copy2(source, destination)



def _xaero_context(path: Path, source: Path) -> tuple[str, str]:
    parts = [part for part in path.relative_to(source).parts]
    server = next((part[len("Multiplayer_"):] for part in parts if part.startswith("Multiplayer_")), "")
    world = f"server:{server.lower()}" if server else "default"
    dimension = "minecraft:overworld"
    for part in parts:
        lowered = part.lower()
        if lowered in ("dim%-1", "dim-1", "the_nether"):
            dimension = "minecraft:the_nether"
        elif lowered in ("dim%1", "dim1", "the_end"):
            dimension = "minecraft:the_end"
    return world, dimension


def _signed_argb(color: int) -> int:
    value = 0xFF000000 | (int(color) & 0xFFFFFF)
    return value - 0x100000000 if value >= 0x80000000 else value


def _discover_norisk_waypoint_json(source: Path) -> list[dict[str, Any]]:
    """Read NoRisk's current Xaero-compatible JSON waypoint store."""
    root = source / "NoRiskClient" / "waypoints"
    if not root.is_dir():
        return []
    found: list[dict[str, Any]] = []
    for path in root.rglob("*.json"):
        if path.name.casefold() == "world_config.json":
            continue
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        sets = payload.get("sets") if isinstance(payload, dict) else None
        if not isinstance(sets, dict):
            continue
        world, _ = _xaero_context(path, source)
        dimension_name = path.stem.casefold().replace("$", ":")
        dimension = {
            "overworld": "minecraft:overworld",
            "the_nether": "minecraft:the_nether",
            "the_end": "minecraft:the_end",
        }.get(dimension_name, dimension_name if ":" in dimension_name else f"minecraft:{dimension_name}")
        for waypoint_set in sets.values():
            waypoints = waypoint_set.get("waypoints") if isinstance(waypoint_set, dict) else None
            if not isinstance(waypoints, list):
                continue
            for waypoint in waypoints:
                if not isinstance(waypoint, dict):
                    continue
                try:
                    name = str(waypoint.get("name") or "Waypoint").strip()
                    x, y, z = float(waypoint["x"]), float(waypoint["y"]), float(waypoint["z"])
                    if not name or len(name) > 80:
                        continue
                except (KeyError, TypeError, ValueError):
                    continue
                found.append({
                    "id": str(waypoint.get("id") or uuid.uuid4()), "name": name,
                    "x": x, "y": y, "z": z,
                    "color": _signed_argb(int(waypoint.get("color") or 0x22C96E)),
                    "icon": "Flag", "world": world, "dimension": dimension,
                    "visible": not bool(waypoint.get("disabled", False)),
                    "folderId": "general", "blocks": [],
                })
    return found


def discover_xaero_waypoints(source: Path) -> list[dict[str, Any]]:
    """Read classic Xaero and current NoRisk/Xaero waypoints without modifying the source."""
    source = Path(source)
    roots = [source / "xaero" / "minimap", source / "XaeroWaypoints"]
    found: list[dict[str, Any]] = _discover_norisk_waypoint_json(source)
    seen: set[tuple[str, str, float, float, float]] = set()
    for waypoint in found:
        seen.add((waypoint["world"], waypoint["dimension"], waypoint["x"], waypoint["y"], waypoint["z"]))
    for root in roots:
        if not root.is_dir():
            continue
        for path in root.rglob("*.txt"):
            world, dimension = _xaero_context(path, source)
            try:
                lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
            except OSError:
                continue
            for line in lines:
                if not line.startswith("waypoint:"):
                    continue
                fields = line.split(":")
                if len(fields) < 8:
                    continue
                try:
                    name = fields[1].strip() or "Waypoint"
                    x, y, z = float(fields[3]), float(fields[4]), float(fields[5])
                    color_index = int(fields[6]) % len(_XAERO_COLORS)
                except (TypeError, ValueError):
                    continue
                key = (world, dimension, x, y, z)
                if key in seen:
                    continue
                seen.add(key)
                found.append({
                    "id": str(uuid.uuid4()), "name": name, "x": x, "y": y, "z": z,
                    "color": _signed_argb(_XAERO_COLORS[color_index]), "icon": "Flag", "world": world,
                    "dimension": dimension, "visible": True, "folderId": "general", "blocks": [],
                })
    return found


def _is_xaero_mod(*values: object) -> bool:
    return "xaero" in " ".join(str(value or "") for value in values).casefold()


def _write_ezclient_waypoints(destination: Path, waypoints: list[dict[str, Any]]) -> None:
    if not waypoints:
        return
    config_path = destination / "config" / "ezclient.json"
    try:
        config = json.loads(config_path.read_text(encoding="utf-8")) if config_path.is_file() else {}
    except (OSError, ValueError):
        config = {}
    if not isinstance(config, dict):
        config = {}
    feature = config.setdefault("feature_Waypoints", {})
    if not isinstance(feature, dict):
        feature = {}
        config["feature_Waypoints"] = feature
    existing = feature.get("waypoints") if isinstance(feature.get("waypoints"), list) else []
    existing_keys = {(p.get("world"), p.get("dimension"), p.get("x"), p.get("y"), p.get("z")) for p in existing if isinstance(p, dict)}
    feature["waypoints"] = existing + [p for p in waypoints if (p["world"], p["dimension"], p["x"], p["y"], p["z"]) not in existing_keys]
    feature.setdefault("folders", [{"id": "general", "name": "GENERAL", "visible": True, "collapsed": False}])
    feature["enabled"] = True
    config_path.parent.mkdir(parents=True, exist_ok=True)
    config_path.write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")


def _classify_zip_pack(zip_path: Path, display_name: str = "") -> str:
    """Classify a zip archive as 'shaderpack' or 'resourcepack'."""
    try:
        with zipfile.ZipFile(zip_path, "r") as z:
            names = set(z.namelist())
            # Resource packs contain pack.mcmeta or assets/
            if any("pack.mcmeta" in n for n in names) or any(n.startswith("assets/") or "/assets/" in n for n in names):
                return "resourcepack"
            # Iris / Optifine shaderpacks have shaders/ at root or shaders.properties
            if any(n.startswith("shaders/") or n.endswith(".fsh") or n.endswith(".vsh") or n == "shaders.properties" for n in names):
                return "shaderpack"
    except Exception:
        pass
    text = (zip_path.name + " " + display_name).lower()
    if "shader" in text:
        return "shaderpack"
    return "resourcepack"


def _fetch_modrinth_batch(project_ids: list[str]) -> dict[str, dict[str, Any]]:
    """Batch fetch multiple Modrinth projects in a single HTTP call."""
    if not project_ids:
        return {}
    results: dict[str, dict[str, Any]] = {}
    unique_ids = list(dict.fromkeys(pid for pid in project_ids if pid))
    for i in range(0, len(unique_ids), 50):
        chunk = unique_ids[i:i + 50]
        try:
            param = urllib.parse.quote(json.dumps(chunk))
            url = f"https://api.modrinth.com/v2/projects?ids={param}"
            req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Accept": "application/json"})
            with urllib.request.urlopen(req, timeout=8) as resp:
                if resp.getcode() == 200:
                    data = json.loads(resp.read().decode("utf-8", errors="replace"))
                    if isinstance(data, list):
                        for p in data:
                            p_id = p.get("id")
                            p_slug = p.get("slug")
                            if p_id:
                                results[p_id] = p
                            if p_slug:
                                results[p_slug.lower()] = p
        except Exception as exc:
            print(f"[NoRiskImporter] Modrinth batch lookup error: {exc}")
    return results


def _fetch_curseforge_batch(mod_ids: list[int | str]) -> dict[str, dict[str, Any]]:
    """Batch fetch multiple CurseForge mods in a single HTTP call."""
    clean_ids: list[int] = []
    for mid in mod_ids:
        try:
            clean_ids.append(int(mid))
        except (ValueError, TypeError):
            pass
    if not clean_ids:
        return {}
    results: dict[str, dict[str, Any]] = {}
    unique_ids = list(dict.fromkeys(clean_ids))
    for i in range(0, len(unique_ids), 50):
        chunk = unique_ids[i:i + 50]
        try:
            resp = curseforge_make_request("/mods", post_data={"modIds": chunk})
            if resp and "data" in resp and isinstance(resp["data"], list):
                for item in resp["data"]:
                    m_id = str(item.get("id"))
                    m_slug = item.get("slug", "")
                    results[m_id] = item
                    if m_slug:
                        results[m_slug.lower()] = item
        except Exception as exc:
            print(f"[NoRiskImporter] CurseForge batch lookup error: {exc}")
    return results


def _find_candidate_file(filename: str, mod_cache_dir: Path, source_dir: Path) -> Path | None:
    """Find a mod file in NoRisk mod_cache or the profile folders."""
    if not filename:
        return None
    # 1. Direct match in meta/mod_cache
    if (mod_cache_dir / filename).is_file():
        return mod_cache_dir / filename
    # 2. Match in profile mods / custom_mods
    if (source_dir / "mods" / filename).is_file():
        return source_dir / "mods" / filename
    if (source_dir / "custom_mods" / filename).is_file():
        return source_dir / "custom_mods" / filename
    # 3. Case-insensitive match in mod_cache
    if mod_cache_dir.is_dir():
        fn_lower = filename.lower()
        for candidate in mod_cache_dir.iterdir():
            if candidate.is_file() and candidate.name.lower() == fn_lower:
                return candidate
    return None


def _enrich_mod_metadata(
    profile: ProfileData,
    progress: Callable[[float, str], None] | None = None,
    raw_mods: list[dict[str, Any]] | None = None,
    *args: Any,
    **kwargs: Any,
) -> None:
    """Enrich mod metadata using local jar extraction and fast batch API calls."""
    if not profile.mods:
        return

    # Build index of raw NoRisk mod entries
    raw_by_filename: dict[str, dict[str, Any]] = {}
    raw_by_id: dict[str, dict[str, Any]] = {}
    for rm in raw_mods or []:
        if not isinstance(rm, dict):
            continue
        src = rm.get("source") if isinstance(rm.get("source"), dict) else {}
        fn = str(src.get("file_name") or src.get("filename") or rm.get("file_name") or "").strip().lower()
        if fn:
            raw_by_filename[fn] = rm
        pid = str(src.get("project_id") or src.get("projectId") or rm.get("id") or "").strip()
        if pid:
            raw_by_id[pid] = rm

    # Collect provider IDs for batch fetching
    modrinth_ids: list[str] = []
    curseforge_ids: list[int | str] = []

    for mod in profile.mods:
        fn_l = (mod.filename or "").lower()
        raw_info = raw_by_filename.get(fn_l) or raw_by_id.get(mod.project_id)
        if raw_info:
            src = raw_info.get("source") if isinstance(raw_info.get("source"), dict) else {}
            src_type = str(src.get("type") or "").lower()
            pid = str(src.get("project_id") or src.get("projectId") or raw_info.get("id") or "").strip()
            if pid:
                if "curse" in src_type or pid.isdigit():
                    curseforge_ids.append(pid)
                    mod.source = "curseforge"
                else:
                    modrinth_ids.append(pid)
                    mod.source = "modrinth"
        elif mod.source == "curseforge" and mod.project_id:
            curseforge_ids.append(mod.project_id)
        elif mod.source == "modrinth" and mod.project_id:
            modrinth_ids.append(mod.project_id)

    if progress:
        progress(0.75, "Rufe Mod-Metadaten gebündelt ab …")

    # Fast batch network queries
    modrinth_map = _fetch_modrinth_batch(modrinth_ids)
    curseforge_map = _fetch_curseforge_batch(curseforge_ids)

    # Local jar fallback cache
    scanner_cache: dict[str, Any] = {}

    for mod in profile.mods:
        jar = profile.mods_path / (mod.filename or "")
        local = extract_jar_metadata(jar, scanner_cache) if jar.is_file() else {}

        # Resolve Modrinth
        if mod.source == "modrinth":
            details = modrinth_map.get(mod.project_id) or modrinth_map.get((mod.slug or "").lower()) or {}
            if details:
                mod.slug = str(details.get("slug") or mod.slug)
                mod.name = str(details.get("title") or mod.name)
                mod.description = str(details.get("description") or mod.description)
                mod.icon_url = str(details.get("icon_url") or mod.icon_url)

        # Resolve CurseForge
        elif mod.source == "curseforge":
            details = curseforge_map.get(str(mod.project_id)) or curseforge_map.get((mod.slug or "").lower()) or {}
            if details:
                mod.slug = str(details.get("slug") or mod.slug)
                mod.name = str(details.get("name") or mod.name)
                mod.description = str(details.get("summary") or mod.description)
                logo = details.get("logo") if isinstance(details.get("logo"), dict) else {}
                icon = logo.get("thumbnailUrl") or logo.get("url") or ""
                if icon:
                    mod.icon_url = str(icon)
                authors = details.get("authors")
                if isinstance(authors, list) and authors:
                    mod.author = authors[0].get("name", mod.author)

        # Apply local JAR metadata fallbacks
        if local:
            if not mod.slug or mod.slug == mod.project_id:
                mod.slug = str(local.get("mod_id") or mod.slug)
            if not mod.name or mod.name == mod.filename or mod.name == "Lokale Mod":
                mod.name = str(local.get("name") or mod.name)
            if local.get("version") and (not mod.version or mod.version == "Unbekannt" or " " in mod.version):
                mod.version = str(local.get("version"))
            if not mod.author or mod.author == "Unbekannt":
                mod.author = str(local.get("authors") or mod.author)
            if not mod.description or mod.description == "Aus NoRiskClient importiert":
                mod.description = str(local.get("description") or mod.description)
            if not mod.icon_url and local.get("icon_url"):
                mod.icon_url = str(local.get("icon_url"))

    if progress:
        progress(0.90, "Metadaten erfolgreich zugewiesen.")


def _matches_hashes(path: Path, hashes: dict[str, str]) -> bool:
    if not path.is_file():
        return False
    for algorithm, expected in hashes.items():
        if algorithm not in ("sha1", "sha256", "sha512", "md5"):
            continue
        if hashlib.new(algorithm, path.read_bytes()).hexdigest().lower() != expected.lower():
            return False
    return True


def _resolve_exact_file(provider: str, project: str, version: str, filename: str,
                        hashes: dict[str, str]) -> dict:
    if provider == "modrinth":
        if version and version.lower() != "latest":
            data = get_json("https://api.modrinth.com/v2/version/" + urllib.parse.quote(version, safe=""))
        elif hashes.get("sha512") or hashes.get("sha1"):
            algorithm = "sha512" if hashes.get("sha512") else "sha1"
            data = get_json("https://api.modrinth.com/v2/version_file/" + hashes[algorithm] + "?algorithm=" + algorithm)
        else:
            raise ValueError("NoRisk-Mod benötigt eine exakte Versions-ID oder einen Dateihash: " + filename)
        files = data.get("files", [])
        if hashes:
            files = [f for f in files if all(f.get("hashes", {}).get(k, v).lower() == v.lower() for k, v in hashes.items())]
        match = next((f for f in files if f.get("filename") == filename), None) if filename else next((f for f in files if f.get("primary")), files[0] if files else None)
        if not match:
            raise ValueError("Datei fehlt in der festgelegten Version: " + filename)
        return dict(match, version_id=data["id"])
    if provider == "curseforge" and project and version and version.lower() != "latest":
        data = curseforge_make_request(f"/mods/{int(project)}/files/{int(version)}").get("data", {})
        return {"filename": data["fileName"], "url": data.get("downloadUrl"),
                "version_id": str(data["id"]),
                "hashes": { {1: "sha1", 2: "md5"}[h["algo"]]: h["value"]
                           for h in data.get("hashes", []) if h.get("algo") in (1, 2)}}
    raise ValueError("Keine exakte Download-Quelle für " + filename)


def import_norisk_files(
    discovered: dict[str, Any],
    profile: ProfileData,
    progress: Callable[[float, str], None] | None = None,
    convert_xaero_waypoints: bool = False,
) -> None:
    """Copy portable player content, mods, shaderpacks, and metadata into EzClient."""
    source = Path(str(discovered["path"]))
    norisk_root = Path(str(discovered.get("norisk_root") or default_norisk_root()))
    mod_cache_dir = norisk_root / "meta" / "mod_cache"

    destination = profile.path
    mods_dest = profile.mods_path
    shaders_dest = destination / "shaderpacks"
    rp_dest = destination / "resourcepacks"

    mods_dest.mkdir(parents=True, exist_ok=True)

    if progress:
        progress(0.10, "Kopiere NoRisk-Dateien …")

    raw = discovered.get("raw") if isinstance(discovered.get("raw"), dict) else {}
    raw_mods = raw.get("mods") if isinstance(raw.get("mods"), list) else []

    imported_mods: list[ModData] = []
    handled_filenames: set[str] = set()
    handled_projects: set[tuple[str, str]] = set()
    handled_mod_ids: set[str] = set()
    scanner_cache: dict = {}
    xaero_waypoints = discover_xaero_waypoints(source) if convert_xaero_waypoints else []

    def duplicate_jar(path: Path) -> bool:
        mod_id = str(extract_jar_metadata(path, scanner_cache).get("mod_id") or "").lower()
        if mod_id and mod_id in handled_mod_ids:
            return True
        if mod_id:
            handled_mod_ids.add(mod_id)
        return False

    # 1. Process mods listed in the profile manifest
    for raw_mod in raw_mods:
        if not isinstance(raw_mod, dict):
            continue
        src = raw_mod.get("source") if isinstance(raw_mod.get("source"), dict) else {}
        fn = str(src.get("file_name") or src.get("filename") or raw_mod.get("file_name") or "").strip()
        disp_name = str(raw_mod.get("display_name") or raw_mod.get("name") or fn or "Mod").strip()
        dl_url = str(src.get("download_url") or "").strip()
        proj_id = str(src.get("project_id") or src.get("projectId") or raw_mod.get("id") or "").strip()
        version_id = str(src.get("version_id") or src.get("file_id") or "").strip()
        raw_version = str(raw_mod.get("version") or "Unbekannt").strip()
        src_type = str(src.get("type") or "local").lower()
        if convert_xaero_waypoints and _is_xaero_mod(fn, disp_name, raw_mod.get("id"), src.get("project_id")):
            continue
        provider = "curseforge" if ("curse" in src_type or proj_id.isdigit()) else ("modrinth" if "modrinth" in src_type else "local")
        enabled = bool(raw_mod.get("enabled", True))

        if fn and (Path(fn).name != fn or "\\" in fn):
            raise ValueError("Ungültiger Dateiname: " + fn)
        key = (provider, proj_id.lower())
        if (fn and fn.lower() in handled_filenames) or (proj_id and key in handled_projects):
            continue
        hashes = dict(src.get("hashes") or {})
        for algorithm in ("sha1", "sha256", "sha512", "md5"):
            if src.get(algorithm):
                hashes[algorithm] = str(src[algorithm])
        candidate = _find_candidate_file(fn, mod_cache_dir, source)
        if candidate and not _matches_hashes(candidate, hashes):
            candidate = None
        if candidate is None:
            if not dl_url or (version_id and version_id.lower() != "latest"):
                exact = _resolve_exact_file(provider, proj_id, version_id, fn, hashes)
                fn = exact["filename"]
                dl_url = exact.get("url") or ""
                version_id = exact["version_id"]
                hashes = {**exact.get("hashes", {}), **hashes}
            if not fn or Path(fn).name != fn or "\\" in fn or not dl_url:
                raise ValueError("Exakte Mod-Datei nicht verfügbar: " + disp_name)
            target_candidate = mods_dest / fn
            if not _matches_hashes(target_candidate, hashes):
                if not download_file(dl_url, target_candidate, use_cache=False):
                    raise RuntimeError("Download fehlgeschlagen: " + disp_name)
            if not _matches_hashes(target_candidate, hashes):
                target_candidate.unlink(missing_ok=True)
                raise ValueError("Dateihash stimmt nicht überein: " + fn)
            candidate = target_candidate

        if candidate and candidate.is_file():
            clean_name = candidate.name
            handled_filenames.add(clean_name.lower())
            if proj_id:
                handled_projects.add(key)

            if clean_name.lower().endswith(".jar"):
                if duplicate_jar(candidate):
                    if candidate.parent == mods_dest:
                        candidate.unlink()
                    continue
                target_jar = mods_dest / clean_name
                if not target_jar.exists() or target_jar.resolve() != candidate.resolve():
                    shutil.copy2(candidate, target_jar)
                imported_mods.append(ModData(
                    project_id=proj_id,
                    slug=proj_id or clean_name[:-4].lower(),
                    name=disp_name,
                    version_id=version_id,
                    version=raw_version,
                    filename=clean_name,
                    enabled=enabled,
                    source=provider,
                    pinned=True,
                    download_url=dl_url,
                    hashes=hashes or {"sha256": hashlib.sha256(candidate.read_bytes()).hexdigest()},
                    description="Aus NoRiskClient importiert",
                ))

            elif clean_name.lower().endswith(".zip"):
                pack_type = _classify_zip_pack(candidate, disp_name)
                if pack_type == "shaderpack":
                    shaders_dest.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(candidate, shaders_dest / clean_name)
                else:
                    rp_dest.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(candidate, rp_dest / clean_name)

    # 2. Copy extra custom jars from profile's mods and custom_mods folders
    for folder in ("custom_mods", "mods"):
        dir_path = source / folder
        if not dir_path.is_dir():
            continue
        for jar in dir_path.glob("*.jar"):
            if jar.name.startswith("nrc-") or jar.name.lower().startswith("norisk"):
                continue
            if convert_xaero_waypoints and _is_xaero_mod(jar.name):
                continue
            if jar.name.lower() not in handled_filenames and not duplicate_jar(jar):
                handled_filenames.add(jar.name.lower())
                target_jar = mods_dest / jar.name
                if not target_jar.exists() or target_jar.resolve() != jar.resolve():
                    shutil.copy2(jar, target_jar)
                imported_mods.append(ModData(
                    project_id=jar.stem.lower(),
                    slug=jar.stem.lower(),
                    name=jar.stem,
                    version_id="local",
                    version="Lokal",
                    filename=jar.name,
                    enabled=True,
                    source="local",
                    description="Lokale Mod",
                ))

    if progress:
        progress(0.40, "Kopiere Konfigurationen & Spielstände …")

    # 3. Copy player content folders (config, saves, screenshots, xaero, etc.)
    for folder in _COPY_DIRS:
        if convert_xaero_waypoints and folder.casefold() in {"xaero", "xaerowaypoints", "xaeroworldmap"}:
            continue
        _copy_tree(source / folder, destination / folder, {"xaero"} if convert_xaero_waypoints else None)

    # 4. Copy standard config files (options.txt, servers.dat, etc.)
    for filename in _COPY_FILES:
        candidate = source / filename
        if candidate.is_file():
            destination.mkdir(parents=True, exist_ok=True)
            if filename.lower().startswith("servers.dat"):
                _copy_cleaned_servers_dat(candidate, destination / filename)
            else:
                shutil.copy2(candidate, destination / filename)

    if convert_xaero_waypoints:
        _write_ezclient_waypoints(destination, xaero_waypoints)

    (destination / ".norisk-import").write_text("Preserve imported configuration", encoding="utf-8")

    # 5. Apply JVM and memory settings
    profile.mods = imported_mods
    profile.user_mods = [m.slug for m in profile.mods if m.slug]
    profile.ram_mb = int(discovered.get("ramMb") or 4096)
    settings = raw.get("settings") if isinstance(raw.get("settings"), dict) else {}
    args = settings.get("custom_jvm_args")
    if isinstance(args, list):
        profile.jvm_args = " ".join(str(value) for value in args)
    elif isinstance(args, str):
        profile.jvm_args = args

    # 6. Enrich metadata
    try:
        _enrich_mod_metadata(profile, progress=progress, raw_mods=raw_mods)
    except TypeError:
        _enrich_mod_metadata(profile, progress=progress)

    if progress:
        progress(1.0, "Profil erfolgreich vorbereitet.")
