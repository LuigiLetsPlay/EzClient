import os
import os
import sys
import shutil
import threading
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
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

    if profile.profile_type == "ezclient":
        from backend.services.store import preseed_optimized_profile_settings
        preseed_optimized_profile_settings(profile.path)

    active_mods = set()
    active_shaders = set()
    active_resourcepacks = set()
    shaderpacks_dir = profile.path / "shaderpacks"
    resourcepacks_dir = profile.path / "resourcepacks"
    shaderpacks_dir.mkdir(parents=True, exist_ok=True)
    resourcepacks_dir.mkdir(parents=True, exist_ok=True)
    pending_dep_pids: set[str] = set()
    owned_ids = {
        str(value).lower() for value in (profile.managed_core_mods + profile.user_mods)
    }
    owned_filenames = {
        (m.filename or "").lower().removesuffix(".disabled")
        for m in profile.mods
        if (m.slug or m.project_id or "").lower() in owned_ids and m.filename
    }

    for m in list(profile.mods):
        slug_or_id = m.slug or m.project_id or m.name
        is_essential = getattr(m, 'essential', False) or (m.slug and m.slug.lower() in profile.managed_core_mods)
        enabled = m.enabled or is_essential

        base_name = m.filename if m.filename else f"{m.slug or 'mod'}.jar"
        if base_name.endswith(".zip"):
            desc = (m.description or "").lower()
            name = (m.name or "").lower()
            slug = (m.slug or "").lower()
            if "shader" in desc or "shader" in name or "shader" in slug:
                target_dir = shaderpacks_dir
                active_set = active_shaders
            else:
                target_dir = resourcepacks_dir
                active_set = active_resourcepacks
        else:
            if not base_name.endswith(".jar"):
                base_name += ".jar"
            target_dir = mods_dir
            active_set = active_mods

        target_jar = target_dir / base_name
        disabled_jar = target_dir / (base_name + ".disabled")

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
            active_set.add(target_jar.name)
            continue

        # A "Latest" request deliberately replaces an already-downloaded JAR.
        requested_version = (getattr(m, "version", "") or "").strip()
        if target_jar.exists() and target_jar.stat().st_size > 1024:
            # Keep the known-good file active unless a replacement completes.
            active_set.add(target_jar.name)
        if requested_version.lower() != "latest" and target_jar.exists() and target_jar.stat().st_size > 1024:
            continue

        # Check cache
        cached = MODS_CACHE_DIR / base_name
        if requested_version.lower() != "latest" and cached.exists() and cached.stat().st_size > 1024:
            try:
                shutil.copy2(cached, target_jar)
                active_set.add(target_jar.name)
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
                    dest = target_dir / real_filename
                    if download_file(primary["url"], dest, use_cache=requested_version.lower() != "latest"):
                        m.filename = real_filename
                        m.version = best_ver.get("version_number", m.version)
                        active_set.add(real_filename)
                        if real_filename != target_jar.name:
                            active_set.discard(target_jar.name)

                # Collect dependencies
                for dep in best_ver.get("dependencies", []):
                    if dep.get("dependency_type") == "required":
                        pid = dep.get("project_id")
                        if pid:
                            if profile.profile_type == "ezclient":
                                from backend.services.profile_migration import _legacy_id
                                if _legacy_id(pid):
                                    continue
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
        if profile.profile_type == "ezclient":
            from backend.services.profile_migration import _legacy_id
            if _legacy_id(dep_id):
                continue

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
                if profile.profile_type == "ezclient":
                    from backend.services.profile_migration import _legacy_id
                    if _legacy_id(fn):
                        continue
                dest = mods_dir / fn
                active_mods.add(fn)
                if not dest.exists() or dest.stat().st_size <= 1024:
                    if status_callback:
                        status_callback(f"Lade Abhängigkeit {fn} herunter…")
                    download_file(primary["url"], dest)

            # Check secondary dependencies
            for sub_dep in best_ver.get("dependencies", []):
                if sub_dep.get("dependency_type") == "required":
                    sub_pid = sub_dep.get("project_id")
                    if sub_pid and sub_pid not in resolved_deps:
                        if profile.profile_type == "ezclient":
                            from backend.services.profile_migration import _legacy_id
                            if _legacy_id(sub_pid):
                                continue
                        pending_dep_pids.add(sub_pid)

        except Exception as e:
            print(f"[ModDownloader] Error downloading dependency {dep_id}: {e}")

    # Remove only inactive files explicitly owned by this profile manifest.
    # Unknown JARs are user content and must never be treated as orphans.
    try:
        for file in mods_dir.glob("*.jar"):
            if file.name not in active_mods and file.name.lower() in owned_filenames:
                try: file.unlink()
                except Exception: pass
        for file in shaderpacks_dir.glob("*.zip"):
            if file.name not in active_shaders and file.name.lower() in owned_filenames:
                try: file.unlink()
                except Exception: pass
        for file in resourcepacks_dir.glob("*.zip"):
            if file.name not in active_resourcepacks and file.name.lower() in owned_filenames:
                try: file.unlink()
                except Exception: pass
    except Exception:
        pass


def provision_profile_mods_parallel(
    profile: ProfileData,
    service: ModrinthService | None = None,
    progress_callback: Callable[[int, int, ModData, str], None] | None = None,
    max_workers: int = 4,
) -> dict[str, str]:
    """Resolve and download independent profile mods concurrently.

    Required transitive dependencies remain the responsibility of the final
    serialized ``sync_profile_mods`` pass. Failures are returned per slug and
    never abort unrelated downloads.
    """
    if not profile or not profile.mods:
        return {}
    svc = service or ModrinthService()
    failures: dict[str, str] = {}
    total = len(profile.mods)

    def install(mod: ModData) -> str:
        target_filename = mod.filename or f"{mod.slug or 'mod'}.jar"
        is_ezclient = (mod.slug or "").lower() in {"ezclient", "ezclient-core"}
        if is_ezclient:
            candidates = [
                Path(sys._MEIPASS) / "backend" / "assets" / target_filename if hasattr(sys, "_MEIPASS") else None,
                Path(__file__).resolve().parent.parent / "assets" / target_filename,
                Path(sys.executable).parent / "backend" / "assets" / target_filename,
                Path.cwd() / "backend" / "assets" / target_filename,
            ]
            source = next((item for item in candidates if item and item.is_file() and item.stat().st_size > 1024), None)
            if source is None:
                raise FileNotFoundError(f"Bundled asset {target_filename} not found")
            profile.mods_path.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, profile.mods_path / target_filename)
            return target_filename

        project = mod.slug or mod.project_id
        versions = svc.get_project_versions(project, mc_version=profile.minecraft_version, loader=profile.loader)
        if not versions:
            versions = svc.get_project_versions(project, loader=profile.loader)
        if not versions:
            raise LookupError(f"No compatible {profile.loader} build for Minecraft {profile.minecraft_version}")
        selected = select_preferred_version(versions)
        if not selected:
            raise LookupError("No stable or beta release found")
        files = selected.get("files", [])
        primary = next((item for item in files if item.get("primary")), files[0] if files else None)
        if not primary or not primary.get("url"):
            raise LookupError("Release contains no downloadable file")
        filename = primary.get("filename") or target_filename
        if not download_file(primary["url"], profile.mods_path / filename):
            raise OSError("Download failed or timed out")
        mod.filename = filename
        mod.version = selected.get("version_number", mod.version)
        mod.version_id = selected.get("id", mod.version_id)
        return filename

    workers = max(1, min(max_workers, total))
    with ThreadPoolExecutor(max_workers=workers, thread_name_prefix="EzClient Mod Download") as pool:
        futures = {pool.submit(install, mod): mod for mod in list(profile.mods)}
        completed = 0
        for future in as_completed(futures):
            mod = futures[future]
            completed += 1
            try:
                filename = future.result()
                status = f"✓ {mod.name} ({filename})"
            except Exception as exc:
                failures[mod.slug or mod.project_id] = str(exc)
                status = f"⚠ {mod.name}: {exc}"
            if progress_callback:
                progress_callback(completed, total, mod, status)
    return failures


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
