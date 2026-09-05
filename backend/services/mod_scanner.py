import json
import zipfile
import hashlib
from pathlib import Path
from typing import Any
import re

import struct

def curseforge_murmur2(data: bytes) -> int:
    """
    Computes the CurseForge Murmur2 (seed 1) hash over file bytes,
    skipping whitespace bytes (0x9, 0xa, 0xd, 0x20) as per CurseForge specification.
    Optimized for high-speed C-level execution.
    """
    filtered = data.translate(None, b"\t\r\n ")
    length = len(filtered)
    if length == 0:
        return 0

    m = 0x5bd1e995
    r = 24
    h = 1 ^ length

    full_chunks = length // 4
    if full_chunks > 0:
        chunk_bytes = filtered[:full_chunks * 4]
        for (k,) in struct.iter_unpack("<I", chunk_bytes):
            k = (k * m) & 0xFFFFFFFF
            k ^= (k >> r)
            k = (k * m) & 0xFFFFFFFF

            h = (h * m) & 0xFFFFFFFF
            h ^= k

    rem = length % 4
    offset = full_chunks * 4
    if rem == 3:
        h ^= filtered[offset + 2] << 16
        h ^= filtered[offset + 1] << 8
        h ^= filtered[offset]
        h = (h * m) & 0xFFFFFFFF
    elif rem == 2:
        h ^= filtered[offset + 1] << 8
        h ^= filtered[offset]
        h = (h * m) & 0xFFFFFFFF
    elif rem == 1:
        h ^= filtered[offset]
        h = (h * m) & 0xFFFFFFFF

    h ^= (h >> 13)
    h = (h * m) & 0xFFFFFFFF
    h ^= (h >> 15)
    return h & 0xFFFFFFFF


_CACHE_FILE = "ezclient_scanner_cache.json"

def extract_jar_metadata(jar_path: Path, cache_data: dict[str, Any] = None) -> dict[str, Any]:
    """
    Inspects an installed mod JAR file archive and extracts metadata.
    Uses an aggressive JSON cache based on file size + mtime to achieve 0ms load times.
    """
    if cache_data is None:
        cache_data = {}

    meta: dict[str, Any] = {
        "filename": jar_path.name,
        "path": str(jar_path),
        "mod_id": "",
        "name": jar_path.stem,
        "version": "",
        "description": "",
        "authors": "",
        "dependencies": [],
        "optional_dependencies": [],
        "sha1": "",
        "murmur2": 0,
        "loader": "unknown"
    }

    if not jar_path.exists() or not jar_path.is_file():
        return meta

    try:
        stat = jar_path.stat()
        cache_key = f"{stat.st_size}_{stat.st_mtime}"
                
        # Check Cache Hit
        if jar_path.name in cache_data:
            cached_entry = cache_data[jar_path.name]
            if cached_entry.get("_cache_key") == cache_key:
                return cached_entry

        # Compute file hashes (cap murmur2 at 16MB to avoid CPU lockup on giant mod jars like Dreamshift 364MB)
        if stat.st_size <= 16 * 1024 * 1024:
            raw_bytes = jar_path.read_bytes()
            meta["sha1"] = hashlib.sha1(raw_bytes).hexdigest()
            meta["murmur2"] = curseforge_murmur2(raw_bytes)
        else:
            h = hashlib.sha1()
            with jar_path.open("rb") as f:
                while chunk := f.read(1024 * 1024):
                    h.update(chunk)
            meta["sha1"] = h.hexdigest()
            meta["murmur2"] = 0

        icon_field = None
        # Inspect ZIP archive contents
        with zipfile.ZipFile(jar_path, "r") as z:
            namelist = set(z.namelist())

            # A. Fabric: fabric.mod.json
            if "fabric.mod.json" in namelist:
                f_data = json.loads(z.read("fabric.mod.json").decode("utf-8", errors="ignore"))
                meta["loader"] = "fabric"
                meta["mod_id"] = str(f_data.get("id", "")).strip()
                meta["name"] = str(f_data.get("name", meta["mod_id"] or jar_path.stem)).strip()
                meta["version"] = str(f_data.get("version", "")).strip()
                meta["description"] = str(f_data.get("description", "")).strip()
                icon_field = f_data.get("icon")
                authors = f_data.get("authors", [])
                meta["authors"] = authors[0] if isinstance(authors, list) and authors else str(authors)
                
                reqs = f_data.get("depends", {})
                if isinstance(reqs, dict):
                    for d in reqs.keys():
                        d_clean = str(d).strip().lower()
                        if d_clean and d_clean not in ("minecraft", "fabricloader", "java"):
                            meta["dependencies"].append(d_clean)
            
            # B. Quilt: quilt.mod.json
            elif "quilt.mod.json" in namelist:
                q_data = json.loads(z.read("quilt.mod.json").decode("utf-8", errors="ignore"))
                qlt = q_data.get("quilt_loader", {})
                meta["loader"] = "quilt"
                meta["mod_id"] = str(qlt.get("id", "")).strip()
                meta["name"] = str(qlt.get("name", meta["mod_id"] or jar_path.stem)).strip()
                meta["version"] = str(qlt.get("version", "")).strip()
                icon_field = qlt.get("icon")
                
                dep_list = qlt.get("depends", [])
                if isinstance(dep_list, list):
                    for dep in dep_list:
                        if isinstance(dep, dict) and "id" in dep:
                            d_clean = str(dep["id"]).strip().lower()
                            if d_clean not in ("minecraft", "quilt_loader", "fabricloader", "java"):
                                meta["dependencies"].append(d_clean)
                        elif isinstance(dep, str):
                            d_clean = dep.strip().lower()
                            if d_clean not in ("minecraft", "quilt_loader", "fabricloader", "java"):
                                meta["dependencies"].append(d_clean)

            # C. Forge / NeoForge: META-INF/mods.toml
            elif "META-INF/mods.toml" in namelist:
                toml_str = z.read("META-INF/mods.toml").decode("utf-8", errors="ignore")
                meta["loader"] = "forge"
                mod_id_m = re.search(r'modId\s*=\s*["\']([^"\']+)["\']', toml_str)
                if mod_id_m: meta["mod_id"] = mod_id_m.group(1).strip()
                display_name_m = re.search(r'displayName\s*=\s*["\']([^"\']+)["\']', toml_str)
                if display_name_m: meta["name"] = display_name_m.group(1).strip()
                version_m = re.search(r'version\s*=\s*["\']([^"\']+)["\']', toml_str)
                if version_m: meta["version"] = version_m.group(1).strip()

            # D. Legacy Forge: mcmod.info
            elif "mcmod.info" in namelist:
                info_str = z.read("mcmod.info").decode("utf-8", errors="ignore").strip()
                info_data = json.loads(info_str)
                if isinstance(info_data, list) and info_data: info_data = info_data[0]
                if isinstance(info_data, dict):
                    meta["loader"] = "forge_legacy"
                    meta["mod_id"] = str(info_data.get("modid", "")).strip()
                    meta["name"] = str(info_data.get("name", meta["mod_id"] or jar_path.stem)).strip()

            # Extract in-jar icon if present
            meta["icon_url"] = _extract_jar_icon(jar_path, z, icon_field)

    except PermissionError:
        # File is locked by a running Minecraft instance; use cached metadata if available
        if jar_path.name in cache_data:
            return cache_data[jar_path.name]
        if "ezclient" in jar_path.name.lower():
            meta["mod_id"] = "ezclient"
            meta["name"] = "EzClient Core"
            meta["loader"] = "fabric"
            meta["icon_url"] = "assets/logo.png"
    except Exception as e:
        if jar_path.name in cache_data:
            return cache_data[jar_path.name]
        print(f"[ModScanner] Could not open jar {jar_path.name}: {e}")

    # Fallback to file name stem
    if not meta["mod_id"]:
        meta["mod_id"] = jar_path.stem.lower()
        if "ezclient" in jar_path.name.lower():
            meta["mod_id"] = "ezclient"
            meta["name"] = "EzClient Core"
            meta["loader"] = "fabric"
            meta["icon_url"] = "assets/logo.png"

    # Update cache dict in memory
    meta["_cache_key"] = cache_key if 'cache_key' in locals() else ""
    cache_data[jar_path.name] = meta

    return meta


def _extract_jar_icon(jar_path: Path, z: zipfile.ZipFile, icon_field: Any) -> str:
    """Extracts mod icon PNG from a jar archive into cache and returns file:/// URL."""
    try:
        import os
        icon_path = ""
        if isinstance(icon_field, str) and icon_field:
            icon_path = icon_field.strip()
        elif isinstance(icon_field, dict) and icon_field:
            for k in ("512", "256", "128", "64", "32", "16"):
                if k in icon_field:
                    icon_path = str(icon_field[k]).strip()
                    break
            if not icon_path:
                icon_path = str(next(iter(icon_field.values()), "")).strip()

        namelist = set(z.namelist())
        candidates = []
        if icon_path:
            candidates.append(icon_path)
            if icon_path.startswith("/"):
                candidates.append(icon_path[1:])
            if not icon_path.startswith("assets/"):
                candidates.append(f"assets/{icon_path}")

        # Fallback candidate search inside jar
        for name in namelist:
            nl = name.lower()
            if (nl.endswith("/icon.png") or nl == "icon.png" or nl.endswith("/logo.png") or nl == "logo.png" or nl.endswith("/mod_icon.png")) and not nl.startswith("meta-inf/"):
                candidates.append(name)

        for cand in candidates:
            if cand in namelist:
                raw = z.read(cand)
                if raw and len(raw) > 32:
                    h = hashlib.md5(raw).hexdigest()
                    appdata = os.environ.get("APPDATA", "")
                    cache_dir = Path(appdata) / ".ezclient" / "cache" / "mod_icons" if appdata else Path.home() / ".ezclient" / "cache" / "mod_icons"
                    cache_dir.mkdir(parents=True, exist_ok=True)
                    out_path = cache_dir / f"{h}.png"
                    if not out_path.exists():
                        out_path.write_bytes(raw)
                    return out_path.as_uri()
    except Exception:
        pass
    return ""


class InstalledModRegistry:
    """
    Central in-memory index of all installed mods for the active profile,
    providing instant O(1) duplicate checks and full dependency tracking.
    """

    def __init__(self, mods_dir: Path | None = None):
        self.mods_dir = mods_dir
        self.installed_mods: list[dict[str, Any]] = []
        self.canonical_ids: set[str] = set()
        self.slugs: set[str] = set()
        self.names_normalized: set[str] = set()
        self.filenames: set[str] = set()
        self.sha1_map: dict[str, dict[str, Any]] = {}
        self.murmur2_map: dict[int, dict[str, Any]] = {}
        self.dependencies_map: dict[str, list[str]] = {}
        self.reverse_dependencies_map: dict[str, list[str]] = {}

        if mods_dir:
            self.scan_directory(mods_dir)

    def scan_directory(self, mods_dir: Path, known_profile_mods: list[Any] | None = None) -> None:
        """Scans the /mods folder and builds the complete indexed registry."""
        self.mods_dir = mods_dir
        self.installed_mods.clear()
        self.canonical_ids.clear()
        self.slugs.clear()
        self.names_normalized.clear()
        self.filenames.clear()
        self.sha1_map.clear()
        self.murmur2_map.clear()
        self.dependencies_map.clear()
        self.reverse_dependencies_map.clear()

        # Build mapping from known ProfileStore mods if available
        known_map: dict[str, Any] = {}
        if known_profile_mods:
            for pm in known_profile_mods:
                fn = getattr(pm, "filename", "") or ""
                slug = getattr(pm, "slug", "") or ""
                pid = getattr(pm, "project_id", "") or ""
                if fn:
                    known_map[fn.lower()] = pm
                if slug:
                    known_map[slug.lower()] = pm
                if pid:
                    known_map[pid.lower()] = pm

        if not mods_dir.exists() or not mods_dir.is_dir():
            return

        cache_path = mods_dir / _CACHE_FILE
        cache_data = {}
        if cache_path.exists():
            try:
                cache_data = json.loads(cache_path.read_text("utf-8"))
            except Exception:
                pass

        for jar_file in sorted(mods_dir.glob("*.jar")) + sorted(mods_dir.glob("*.jar.disabled")):
            meta = extract_jar_metadata(jar_file, cache_data)
            fn_l = jar_file.name.lower()
            clean_fn = fn_l.replace(".disabled", "")

            # Check if linked to known ProfileData mod
            pm = known_map.get(fn_l) or known_map.get(clean_fn) or known_map.get(meta["mod_id"].lower())
            if pm:
                if getattr(pm, "slug", ""):
                    meta["slug"] = getattr(pm, "slug").lower()
                if getattr(pm, "project_id", ""):
                    meta["project_id"] = getattr(pm, "project_id")
                if getattr(pm, "name", "") and not meta.get("name"):
                    meta["name"] = getattr(pm, "name")

            mod_id = (meta.get("mod_id") or jar_file.stem).lower()
            name_norm = re.sub(r"[^a-z0-9]+", "", (meta.get("name") or "").lower())
            slug = (meta.get("slug") or mod_id).lower()

            self.canonical_ids.add(mod_id)
            self.slugs.add(slug)
            if name_norm:
                self.names_normalized.add(name_norm)
            self.filenames.add(clean_fn)
            self.filenames.add(fn_l)

            if meta.get("sha1"):
                self.sha1_map[meta["sha1"]] = meta
            if meta.get("murmur2"):
                self.murmur2_map[meta["murmur2"]] = meta

            # Store dependencies
            deps = meta.get("dependencies", [])
            self.dependencies_map[mod_id] = deps
            self.dependencies_map[slug] = deps

            self.installed_mods.append(meta)

        # Build reverse dependencies mapping
        for m in self.installed_mods:
            mod_display_name = m.get("name") or m.get("mod_id") or m.get("filename")
            for dep_id in m.get("dependencies", []):
                dep_clean = dep_id.lower()
                if dep_clean not in self.reverse_dependencies_map:
                    self.reverse_dependencies_map[dep_clean] = []
                if mod_display_name not in self.reverse_dependencies_map[dep_clean]:
                    self.reverse_dependencies_map[dep_clean].append(mod_display_name)

        if 'cache_data' in locals() and 'cache_path' in locals():
            try:
                cache_path.write_text(json.dumps(cache_data), "utf-8")
            except Exception:
                pass

    def is_installed(
        self,
        project_id: str = "",
        slug: str = "",
        name: str = "",
        filename: str = "",
        sha1: str = "",
        murmur2: int = 0
    ) -> bool:
        """
        Cross-platform check: returns True if the mod is installed on the active profile,
        matching by Mod ID, Slug, Name, SHA1 or Murmur2 hash.
        """
        if sha1 and sha1 in self.sha1_map:
            return True
        if murmur2 and murmur2 in self.murmur2_map:
            return True

        # Normalized identifiers
        targets = set()
        for val in (project_id, slug):
            if val:
                s = str(val).strip().lower()
                targets.add(s)
                # handle variations with hyphens/underscores
                targets.add(s.replace("-", "").replace("_", ""))

        for t in targets:
            if t in self.canonical_ids or t in self.slugs:
                return True

        if filename:
            fn_clean = str(filename).strip().lower().replace(".disabled", "")
            if fn_clean in self.filenames:
                return True

        if name:
            n_norm = re.sub(r"[^a-z0-9]+", "", str(name).lower())
            if n_norm and n_norm in self.names_normalized:
                return True

        return False

    def get_dependent_mods(self, mod_id_or_slug_or_name: str) -> list[str]:
        """
        Returns list of display names of other currently installed mods that require this mod.
        """
        target = str(mod_id_or_slug_or_name or "").strip().lower()
        if not target:
            return []

        # Check direct match in reverse dependencies
        matched_dependents = set()

        # Keys to check
        candidates = {target, target.replace("-", ""), target.replace("_", "")}
        # Try to find corresponding canonical mod_id from installed_mods
        for m in self.installed_mods:
            m_slug = (m.get("slug") or "").lower()
            m_id = (m.get("mod_id") or "").lower()
            m_name = re.sub(r"[^a-z0-9]+", "", (m.get("name") or "").lower())
            target_norm = re.sub(r"[^a-z0-9]+", "", target)

            if target in (m_slug, m_id) or (target_norm and target_norm == m_name):
                candidates.add(m_id)
                candidates.add(m_slug)

        for cand in candidates:
            if cand in self.reverse_dependencies_map:
                for dep_name in self.reverse_dependencies_map[cand]:
                    matched_dependents.add(dep_name)

        # Do not include the mod itself in its own dependent list
        for m in self.installed_mods:
            m_display = m.get("name") or m.get("mod_id")
            if (m.get("mod_id", "").lower() in candidates or m.get("slug", "").lower() in candidates) and m_display in matched_dependents:
                matched_dependents.remove(m_display)

        return sorted(list(matched_dependents))
