"""Read and import locally installed NoRiskClient V3 profiles."""

from __future__ import annotations

import json
import os
import shutil
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path
from typing import Any, Callable

from backend.models.types import ModData, ProfileData
from backend.services.curseforge import _make_request as curseforge_make_request
from backend.services.mod_downloader import download_file
from backend.services.mod_scanner import extract_jar_metadata
from backend.services.modrinth import USER_AGENT


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

        result.append({
            "id": p_id or relative,
            "name": str(raw.get("name") or relative),
            "version": str(raw.get("game_version") or ""),
            "loader": str(raw.get("loader") or "Fabric").title(),
            "loaderVersion": str(raw.get("loader_version") or ""),
            "ramMb": int(memory.get("max") or 4096),
            "modCount": max(len(mods), extra_count),
            "path": str(source),
            "norisk_root": str(base),
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


def _copy_tree(source: Path, target: Path) -> None:
    if source.is_dir():
        target.mkdir(parents=True, exist_ok=True)
        for item in source.iterdir():
            # Skip NoRisk internal folders and transient library caches
            if item.name.startswith("nrc-") or item.name.lower() in (
                "noriskclient", "noriskclientlauncher", "logs", "crash-reports", ".fabric",
                "libraries", "loader", "image-cache", "screenshot-cache", "cosmetic-cache"
            ):
                continue
            dest_item = target / item.name
            if item.is_dir():
                shutil.copytree(
                    item, dest_item, dirs_exist_ok=True,
                    ignore=shutil.ignore_patterns("libraries", "loader", "cache", "*cache*")
                )
            elif item.is_file():
                shutil.copy2(item, dest_item)


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


def import_norisk_files(
    discovered: dict[str, Any],
    profile: ProfileData,
    progress: Callable[[float, str], None] | None = None,
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
        provider = "curseforge" if ("curse" in src_type or proj_id.isdigit()) else ("modrinth" if "modrinth" in src_type else "local")
        enabled = bool(raw_mod.get("enabled", True))

        candidate = _find_candidate_file(fn, mod_cache_dir, source)

        # Download if missing and URL is available
        if candidate is None and dl_url and fn:
            target_candidate = mods_dest / fn
            if download_file(dl_url, target_candidate):
                candidate = target_candidate

        if candidate and candidate.is_file():
            clean_name = candidate.name
            handled_filenames.add(clean_name.lower())

            if clean_name.lower().endswith(".jar"):
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
            if jar.name.lower() not in handled_filenames:
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
        _copy_tree(source / folder, destination / folder)

    # 4. Copy standard config files (options.txt, servers.dat, etc.)
    for filename in _COPY_FILES:
        candidate = source / filename
        if candidate.is_file():
            destination.mkdir(parents=True, exist_ok=True)
            shutil.copy2(candidate, destination / filename)

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
