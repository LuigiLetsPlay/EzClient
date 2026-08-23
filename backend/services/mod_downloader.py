import os
import os
import sys
import shutil
import threading
import urllib.request
from pathlib import Path
from typing import Callable, Any
from backend.models.types import ProfileData, ModData, CACHE_DIR, DATA_DIR
from backend.services.modrinth import ModrinthService, USER_AGENT, select_preferred_version

MODS_CACHE_DIR = CACHE_DIR / "mods"
MODS_CACHE_DIR.mkdir(parents=True, exist_ok=True)
_PROFILE_SYNC_GUARD = threading.Lock()
_PROFILE_SYNC_LOCKS: dict[str, threading.Lock] = {}


def _profile_sync_lock(profile: ProfileData) -> threading.Lock:
    """Return one lock per profile so background actions never race on JARs."""
    key = str(profile.path.resolve())
    with _PROFILE_SYNC_GUARD:
        return _PROFILE_SYNC_LOCKS.setdefault(key, threading.Lock())

def download_file(url: str, dest_path: Path, use_cache: bool = True) -> bool:
    """Download a file with caching."""
    dest_path.parent.mkdir(parents=True, exist_ok=True)
    cache_file = MODS_CACHE_DIR / dest_path.name
    
    if use_cache and cache_file.exists() and cache_file.stat().st_size > 1024:
        try:
            shutil.copy2(cache_file, dest_path)
            return True
        except Exception:
            pass

    temporary = dest_path.with_suffix(dest_path.suffix + ".part")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=30) as resp, temporary.open("wb") as output:
            while chunk := resp.read(1024 * 256):
                output.write(chunk)
        # An atomic replace prevents Minecraft and other queued actions from
        # ever seeing a half-downloaded JAR.
        os.replace(temporary, dest_path)
        if use_cache:
            shutil.copy2(dest_path, cache_file)
        return True
    except Exception as e:
        temporary.unlink(missing_ok=True)
        print(f"[ModDownloader] Failed to download {url}: {e}")
        return False

def _sync_profile_mods(profile: ProfileData, service: ModrinthService | None = None, status_callback: Callable[[str], None] | None = None) -> None:
    """Synchronize all enabled mod jar files into profile.mods_path including all required dependencies."""
    if not profile:
        return
    
    mods_dir = profile.mods_path
    mods_dir.mkdir(parents=True, exist_ok=True)
    svc = service or ModrinthService()

    # Preseed PvP & Sodium settings
    from backend.services.store import preseed_optimized_profile_settings
    preseed_optimized_profile_settings(profile.path)

    active_filenames = set()
    pending_dep_pids: set[str] = set()

    for m in list(profile.mods):
        slug_or_id = m.slug or m.project_id or m.name
        is_essential = getattr(m, 'essential', False) or (m.slug and m.slug.lower() in ("fabric-api", "ezclient"))
        enabled = m.enabled or is_essential

        base_name = m.filename if m.filename.endswith(".jar") else f"{m.slug or 'mod'}.jar"
        target_jar = mods_dir / base_name
        disabled_jar = mods_dir / (base_name + ".disabled")

        if not enabled:
            if target_jar.exists():
                try:
                    target_jar.replace(disabled_jar)
                except Exception:
                    pass
            continue

        if disabled_jar.exists() and not target_jar.exists():
            try:
                disabled_jar.replace(target_jar)
            except Exception:
                pass

        target_filename = m.filename if m.filename else "EzClient.jar"
        is_ezclient = (m.slug and m.slug.lower() in ("ezclient", "ezclient-core")) or (m.filename and m.filename.lower() in ("ezclient.jar", "ezclient-lite.jar")) or ("ezclient" in m.name.lower())
        
        if is_ezclient:
            candidates = [
                Path(sys._MEIPASS) / "backend" / "assets" / target_filename if hasattr(sys, "_MEIPASS") else None,
                Path(__file__).resolve().parent.parent / "assets" / target_filename,
                Path(sys._MEIPASS) / "assets" / target_filename if hasattr(sys, "_MEIPASS") else None,
                Path(__file__).resolve().parent / "backend" / "assets" / target_filename,
                Path(sys.executable).parent / "backend" / "assets" / target_filename,
                Path.cwd() / "backend" / "assets" / target_filename,
                Path.cwd() / "assets" / target_filename,
            ]
            for c in candidates:
                if c and c.exists() and c.is_file() and c.stat().st_size > 100:
                    try:
                        shutil.copy2(c, target_jar)
                        break
                    except Exception as ex:
                        print(f"[ModDownloader] Error copying {target_filename}: {ex}")
            active_filenames.add(target_jar.name)
            continue

        # A "Latest" request deliberately replaces an already-downloaded JAR.
        requested_version = (getattr(m, "version", "") or "").strip()
        if requested_version.lower() != "latest" and target_jar.exists() and target_jar.stat().st_size > 1024:
            active_filenames.add(target_jar.name)
            continue

        # Check cache
        cached = MODS_CACHE_DIR / base_name
        if requested_version.lower() != "latest" and cached.exists() and cached.stat().st_size > 1024:
            try:
                shutil.copy2(cached, target_jar)
                active_filenames.add(target_jar.name)
                continue
            except Exception:
                pass

        if status_callback:
            status_callback(f"Lade {m.name} herunter…")

        try:
            versions = []
            if getattr(m, 'source', '') == 'curseforge':
                from backend.services.curseforge import CurseForgeService
                cf_svc = CurseForgeService()
                versions = cf_svc.get_project_versions(slug_or_id, mc_version=profile.minecraft_version, loader=profile.loader)
            if not versions:
                versions = svc.get_project_versions(slug_or_id, mc_version=profile.minecraft_version, loader=profile.loader)
            if not versions:
                versions = svc.get_project_versions(slug_or_id, loader=profile.loader)
            if not versions:
                versions = svc.get_project_versions(slug_or_id)
            if not versions:
                from backend.services.curseforge import CurseForgeService
                cf_svc = CurseForgeService()
                versions = cf_svc.get_project_versions(slug_or_id, mc_version=profile.minecraft_version, loader=profile.loader)
            
            if versions:
                best_ver = next(
                    (v for v in versions if requested_version and requested_version.lower() != "latest"
                     and v.get("version_number") == requested_version),
                    None,
                )
                if best_ver is None:
                    best_ver = select_preferred_version(versions)
                if best_ver is None:
                    continue
                files = best_ver.get("files", [])
                primary = next((f for f in files if f.get("primary")), files[0] if files else None)
                if primary and primary.get("url"):
                    real_filename = primary.get("filename", base_name)
                    dest = mods_dir / real_filename
                    if download_file(primary["url"], dest, use_cache=requested_version.lower() != "latest"):
                        m.filename = real_filename
                        m.version = best_ver.get("version_number", m.version)
                        active_filenames.add(real_filename)

                # Collect dependencies
                for dep in best_ver.get("dependencies", []):
                    if dep.get("dependency_type") == "required":
                        pid = dep.get("project_id")
                        if pid:
                            pending_dep_pids.add(pid)

        except Exception as exc:
            print(f"[ModDownloader] Error resolving mod {m.name}: {exc}")

    # Recursive resolution for missing dependencies
    existing_slugs = {m.slug.lower() for m in profile.mods if m.slug} | {m.project_id.lower() for m in profile.mods if m.project_id}
    resolved_deps: set[str] = set()

    while pending_dep_pids:
        dep_id = pending_dep_pids.pop()
        if dep_id in resolved_deps:
            continue
        resolved_deps.add(dep_id)

        try:
            versions = svc.get_project_versions(dep_id, mc_version=profile.minecraft_version, loader=profile.loader)
            if not versions:
                versions = svc.get_project_versions(dep_id, loader=profile.loader)
            if not versions:
                continue

            best_ver = select_preferred_version(versions)
            if best_ver is None:
                continue
            files = best_ver.get("files", [])
            primary = next((f for f in files if f.get("primary")), files[0] if files else None)
            if primary and primary.get("url"):
                fn = primary.get("filename", f"{dep_id}.jar")
                dest = mods_dir / fn
                active_filenames.add(fn)
                if not dest.exists() or dest.stat().st_size <= 1024:
                    if status_callback:
                        status_callback(f"Lade Abhängigkeit {fn} herunter…")
                    download_file(primary["url"], dest)

            # Check secondary dependencies
            for sub_dep in best_ver.get("dependencies", []):
                if sub_dep.get("dependency_type") == "required":
                    sub_pid = sub_dep.get("project_id")
                    if sub_pid and sub_pid not in resolved_deps:
                        pending_dep_pids.add(sub_pid)

        except Exception as e:
            print(f"[ModDownloader] Error downloading dependency {dep_id}: {e}")

    # Remove orphaned jars in profile.mods_path not in active_filenames
    try:
        for file in mods_dir.glob("*.jar"):
            if file.name not in active_filenames and not file.name.endswith(".disabled"):
                try:
                    file.unlink()
                except Exception:
                    pass
    except Exception:
        pass


def sync_profile_mods(profile: ProfileData, service: ModrinthService | None = None, status_callback: Callable[[str], None] | None = None) -> None:
    """Thread-safe synchronisation entry point used by install/update/delete.

    Each UI action already starts a worker.  Serialising work per profile
    avoids overlapping scans, duplicated downloads, and file-lock retries that
    previously made the launcher appear frozen during rapid clicks.
    """
    if not profile:
        return
    with _profile_sync_lock(profile):
        _sync_profile_mods(profile, service=service, status_callback=status_callback)
