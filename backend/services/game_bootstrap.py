"""First-run downloader for the official Minecraft files used by EzClient."""
import hashlib
import json
import os
import platform
import re
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Callable

from backend.models.types import ProfileData

VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader"
LEGACY_FABRIC_META = "https://meta.legacyfabric.net/v2/versions/loader"
ASSET_BASE = "https://resources.download.minecraft.net"

# Parallel download settings
_LIBRARY_WORKERS = 12
_ASSET_WORKERS = 24
_DOWNLOAD_LOCKS: dict[str, threading.Lock] = {}
_DOWNLOAD_LOCKS_GUARD = threading.Lock()


def _json(url: str) -> dict:
    request = urllib.request.Request(url, headers={"User-Agent": "EzClient/2.2.1"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def _download(url: str, target: Path, sha1: str = "", expected_size: int = 0) -> bool:
    """Download one file and report whether a network download was necessary."""
    # ``Path.resolve()`` can return a slightly different spelling on Windows
    # before and after the file exists. A normalized absolute path stays stable.
    lock_key = os.path.normcase(os.path.abspath(target))
    with _DOWNLOAD_LOCKS_GUARD:
        target_lock = _DOWNLOAD_LOCKS.setdefault(lock_key, threading.Lock())
    with target_lock:
        return _download_locked(url, target, sha1, expected_size)


def _download_locked(url: str, target: Path, sha1: str = "", expected_size: int = 0) -> bool:
    if target.is_file():
        file_size = target.stat().st_size
        if file_size > 0:
            size_matches = not expected_size or file_size == expected_size
            if size_matches and (not sha1 or hashlib.sha1(target.read_bytes()).hexdigest() == sha1):
                return False
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_name(
        f"{target.name}.{os.getpid()}.{threading.get_ident()}.part"
    )
    request = urllib.request.Request(url, headers={"User-Agent": "EzClient/2.2.1"})
    try:
        with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as output:
            while chunk := response.read(1024 * 256):
                output.write(chunk)
        if sha1 and hashlib.sha1(temporary.read_bytes()).hexdigest() != sha1:
            raise RuntimeError(f"Prüfsumme stimmt nicht: {target.name}")
        if expected_size and temporary.stat().st_size != expected_size:
            raise RuntimeError(f"Dateigröße stimmt nicht: {target.name}")
        os.replace(temporary, target)
        return True
    finally:
        temporary.unlink(missing_ok=True)


def _library_artifact(library: dict) -> dict:
    """Normalize Mojang's and Fabric's two library metadata formats."""
    artifact = library.get("downloads", {}).get("artifact", {})
    if artifact.get("url") and artifact.get("path"):
        return artifact

    parts = str(library.get("name", "")).split(":")
    base_url = str(library.get("url", "")).rstrip("/")
    if len(parts) < 3 or not base_url:
        return {}
    group, name, version = parts[:3]
    classifier_name = parts[3] if len(parts) > 3 else ""
    natives = library.get("natives", {})
    if natives:
        system = {"Windows": "windows", "Darwin": "osx", "Linux": "linux"}.get(
            platform.system(), platform.system().lower()
        )
        classifier_name = str(natives.get(system, classifier_name)).replace(
            "${arch}", "64" if platform.machine().endswith("64") else "32"
        )
    classifier = f"-{classifier_name}" if classifier_name else ""
    path = f"{group.replace('.', '/')}/{name}/{version}/{name}-{version}{classifier}.jar"
    return {"url": f"{base_url}/{path}", "path": path, "sha1": library.get("sha1", "")}


def _native_artifact(library: dict) -> dict:
    system = {"Windows": "windows", "Darwin": "osx", "Linux": "linux"}.get(platform.system(), "")
    classifier = str(library.get("natives", {}).get(system, "")).replace("${arch}", "64" if platform.machine().endswith("64") else "32")
    if not classifier:
        return {}
    artifact = library.get("downloads", {}).get("classifiers", {}).get(classifier, {})
    return artifact or (_library_artifact(library) if not library.get("downloads", {}).get("artifact") else {})


def _library_artifacts(libraries: list[dict]) -> list[dict]:
    from backend.services.direct_launch import is_rule_allowed
    artifacts = {}
    for library in libraries:
        if not is_rule_allowed(library):
            continue
        for artifact in (_library_artifact(library), _native_artifact(library)):
            if artifact and artifact.get("path") and artifact.get("url"):
                artifacts[artifact["path"]] = artifact
    return list(artifacts.values())


def _libraries_are_ready(mc_dir: Path, libraries: list[dict]) -> bool:
    for artifact in _library_artifacts(libraries):
        path = mc_dir / "libraries" / artifact["path"]
        if not path.is_file() or path.stat().st_size == 0:
            return False
    return True


def _download_libraries(
    mc_dir: Path, libraries: list[dict], notify: Callable[[str], None], label: str
) -> None:
    artifacts = _library_artifacts(libraries)
    total = len(artifacts)
    if total == 0:
        return

    lock = threading.Lock()
    progress = {"checked": 0, "downloaded": 0}

    def _do_download(artifact: dict) -> bool:
        result = _download(
            artifact["url"],
            mc_dir / "libraries" / artifact["path"],
            artifact.get("sha1", ""),
            int(artifact.get("size", 0)),
        )
        with lock:
            progress["checked"] += 1
            if result:
                progress["downloaded"] += 1
            checked = progress["checked"]
            downloaded = progress["downloaded"]
        if checked == total or checked % 15 == 0:
            notify(f"{label}: {checked}/{total} geprüft · {downloaded} neu geladen")
        return result

    with ThreadPoolExecutor(max_workers=min(_LIBRARY_WORKERS, total)) as pool:
        futures = [pool.submit(_do_download, a) for a in artifacts]
        for future in as_completed(futures):
            future.result()  # Propagate exceptions

    notify(f"{label}: {total}/{total} abgeschlossen · {progress['downloaded']} neu geladen")


def _assets_are_ready(mc_dir: Path, vanilla_json: Path) -> bool:
    """Require the asset index and every indexed asset before allowing a launch."""
    try:
        version_data = json.loads(vanilla_json.read_text(encoding="utf-8"))
        asset_id = version_data.get("assetIndex", {}).get("id")
        if not asset_id:
            return False
        index_path = mc_dir / "assets" / "indexes" / f"{asset_id}.json"
        if not index_path.is_file():
            return False
        objects = json.loads(index_path.read_text(encoding="utf-8")).get("objects", {})
        return all(
            (mc_dir / "assets" / "objects" / item["hash"][:2] / item["hash"]).is_file()
            for item in objects.values()
            if item.get("hash")
        )
    except (OSError, json.JSONDecodeError):
        return False


def _download_assets_parallel(
    mc_dir: Path, asset_list: list[dict], notify: Callable[[str], None]
) -> None:
    """Download all Minecraft assets using a parallel thread pool."""
    # Logical asset names can share one content hash. Download each physical
    # object once so parallel workers cannot race on the same destination.
    asset_list = list(
        {item.get("hash", ""): item for item in asset_list if item.get("hash")}.values()
    )
    total = len(asset_list)
    if total == 0:
        return

    total_mb = sum(int(item.get("size", 0)) for item in asset_list) / (1024 * 1024)
    notify(f"Minecraft-Assets: {total} Dateien werden geprüft (ca. {total_mb:.0f} MB).")

    lock = threading.Lock()
    progress = {"checked": 0, "downloaded": 0, "bytes": 0}
    start_time = time.monotonic()

    def _do_asset(asset: dict) -> bool:
        digest = asset.get("hash", "")
        size = int(asset.get("size", 0))
        result = _download(
            f"{ASSET_BASE}/{digest[:2]}/{digest}",
            mc_dir / "assets" / "objects" / digest[:2] / digest,
            digest,
            size,
        )
        with lock:
            progress["checked"] += 1
            if result:
                progress["downloaded"] += 1
                progress["bytes"] += size
            checked = progress["checked"]
            downloaded = progress["downloaded"]
            dl_bytes = progress["bytes"]

        # Report progress every 50 files or at the end
        if checked == total or checked % 50 == 0:
            percent = (checked * 100) // total
            elapsed = time.monotonic() - start_time
            speed = (dl_bytes / (1024 * 1024)) / max(0.1, elapsed)
            notify(
                f"Minecraft-Assets: {checked}/{total} ({percent}%) · "
                f"{downloaded} neu ({dl_bytes / (1024 * 1024):.1f} MB, {speed:.1f} MB/s)"
            )
        return result

    with ThreadPoolExecutor(max_workers=min(_ASSET_WORKERS, total)) as pool:
        futures = [pool.submit(_do_asset, a) for a in asset_list]
        for future in as_completed(futures):
            future.result()

    elapsed = time.monotonic() - start_time
    notify(
        f"Minecraft-Assets fertig: {progress['downloaded']} neu geladen "
        f"({progress['bytes'] / (1024 * 1024):.1f} MB in {elapsed:.1f}s)"
    )


def _parse_loader_version(version_str: str) -> tuple[int, ...]:
    match = re.search(r"fabric-loader-([0-9.]+)", version_str) or re.search(r"([0-9.]+)", version_str)
    if match:
        try:
            return tuple(int(x) for x in match.group(1).split(".") if x.isdigit())
        except ValueError:
            pass
    return (0,)


def ensure_game_ready(profile: ProfileData, mc_dir: Path, notify: Callable[[str], None]) -> None:
    """Download Mojang/Fabric files once; no official launcher is involved."""
    version = profile.minecraft_version
    vanilla_dir = mc_dir / "versions" / version
    vanilla_json = vanilla_dir / f"{version}.json"
    has_vanilla = vanilla_json.exists() and (vanilla_dir / f"{version}.jar").exists()
    try:
        vanilla_data = json.loads(vanilla_json.read_text(encoding="utf-8")) if has_vanilla else {}
    except (OSError, ValueError):
        has_vanilla = False
        vanilla_data = {}
    vanilla_libraries_ready = has_vanilla and _libraries_are_ready(mc_dir, vanilla_data.get("libraries", []))
    loader_name = profile.loader.lower()
    if loader_name == "fabric":
        all_loader_files = list((mc_dir / "versions").glob(f"fabric-loader-*-{version}/*.json"))
        all_loader_files.sort(key=lambda p: _parse_loader_version(p.parent.name), reverse=True)
        requested = str(getattr(profile, "loader_version", "") or "").strip()
        if requested:
            req_tuple = _parse_loader_version(requested)
            loader_files = [p for p in all_loader_files if (
                _parse_loader_version(p.parent.name) == req_tuple if profile.profile_type == "raw"
                else _parse_loader_version(p.parent.name) >= req_tuple)]
        elif version.startswith("26."):
            # 26.x modern Fabric mods (like Kotlin 1.14.1+) require at least Fabric Loader 0.19.5
            loader_files = [p for p in all_loader_files if _parse_loader_version(p.parent.name) >= (0, 19, 5)]
        else:
            loader_files = all_loader_files
    elif loader_name == "forge":
        from backend.services.loader_versions import installed_forge_metadata
        requested = str(getattr(profile, "loader_version", "") or "").strip()
        loader_files = installed_forge_metadata(mc_dir, version, requested)
    else:
        loader_files = []
    has_loader = bool(loader_files)
    loader_libraries_ready = False
    if has_loader:
        try:
            loader_data = json.loads(loader_files[0].read_text(encoding="utf-8"))
            loader_libraries_ready = _libraries_are_ready(mc_dir, loader_data.get("libraries", []))
        except (OSError, json.JSONDecodeError):
            pass
    assets_ready = has_vanilla and _assets_are_ready(mc_dir, vanilla_json)
    if has_vanilla and vanilla_libraries_ready and assets_ready and (
        loader_name not in ("fabric", "forge") or (has_loader and loader_libraries_ready)
    ):
        return

    notify(f"Lade Minecraft {version} direkt von Mojang herunter…")
    manifest = _json(VERSION_MANIFEST)
    entry = next((item for item in manifest.get("versions", []) if item.get("id") == version), None)
    if not entry:
        raise RuntimeError(f"Minecraft-Version {version} wurde bei Mojang nicht gefunden.")
    vanilla = _json(entry["url"])
    vanilla_dir.mkdir(parents=True, exist_ok=True)
    vanilla_json.write_text(json.dumps(vanilla, indent=2), encoding="utf-8")
    client = vanilla.get("downloads", {}).get("client", {})
    client_downloaded = _download(client["url"], vanilla_dir / f"{version}.jar", client.get("sha1", ""))
    notify("Minecraft-Client heruntergeladen." if client_downloaded else "Minecraft-Client bereits vorhanden.")
    notify("Lade Minecraft-Bibliotheken parallel herunter…")
    _download_libraries(mc_dir, vanilla.get("libraries", []), notify, "Minecraft-Bibliotheken")

    assets = vanilla.get("assetIndex", {})
    if assets.get("url"):
        notify("Lade Minecraft-Assets parallel herunter…")
        index_path = mc_dir / "assets" / "indexes" / f"{assets['id']}.json"
        _download(assets["url"], index_path, assets.get("sha1", ""))
        asset_list = [
            item for item in json.loads(index_path.read_text(encoding="utf-8")).get("objects", {}).values()
            if item.get("hash")
        ]
        _download_assets_parallel(mc_dir, asset_list, notify)

    if profile.loader.lower() == "fabric":
        try:
            version_parts = tuple(int(part) for part in version.split("."))
        except ValueError:
            version_parts = (999,)
        legacy_fabric = (1, 3) <= version_parts <= (1, 13, 2)
        fabric_meta = LEGACY_FABRIC_META if legacy_fabric else FABRIC_META
        notify("Installiere Legacy-Fabric-Komponenten…" if legacy_fabric else "Installiere Fabric-Komponenten…")
        loaders = _json(f"{fabric_meta}/{version}")
        requested = str(getattr(profile, "loader_version", "") or "").strip()
        if profile.profile_type == "raw" and requested:
            loader = next((item for item in loaders if item.get("loader", {}).get("version") == requested), None)
        else:
            loader = next((item for item in loaders if item.get("loader", {}).get("stable")), loaders[0] if loaders else None)
        if not loader:
            raise RuntimeError(f"Kein Fabric-Loader für Minecraft {version} verfügbar.")
        loader_version = loader["loader"]["version"]
        fabric = _json(f"{fabric_meta}/{version}/{loader_version}/profile/json")
        fabric_id = fabric.get("id", f"fabric-loader-{loader_version}-{version}")
        fabric_dir = mc_dir / "versions" / fabric_id
        fabric_dir.mkdir(parents=True, exist_ok=True)
        (fabric_dir / f"{fabric_id}.json").write_text(json.dumps(fabric, indent=2), encoding="utf-8")
        _download_libraries(mc_dir, fabric.get("libraries", []), notify, "Fabric-Bibliotheken")
        profile.loader_version = loader_version
    elif profile.loader.lower() == "forge":
        notify("Installiere Forge und benötigte Bibliotheken …")
        try:
            import minecraft_launcher_lib
            from backend.services.java_runtime import install_required_java
            from backend.services.minecraft_versions import required_java

            requested = str(getattr(profile, "loader_version", "") or "").strip()
            from backend.services.loader_versions import forge_coordinate
            forge_version = forge_coordinate(version, requested) if requested else minecraft_launcher_lib.forge.find_forge_version(version)
            if not forge_version:
                raise RuntimeError(f"Für Minecraft {version} ist keine Forge-Version verfügbar.")
            java_bin = install_required_java(mc_dir, required_java(version), notify)
            minecraft_launcher_lib.forge.install_forge_version(
                forge_version,
                str(mc_dir),
                callback={"setStatus": notify, "setProgress": lambda _value: None, "setMax": lambda _value: None},
                java=str(java_bin),
            )
            profile.loader_version = forge_version
        except ImportError as exc:
            raise RuntimeError("Forge-Unterstützung fehlt. Bitte minecraft-launcher-lib installieren.") from exc
    notify("Minecraft-Dateien sind vorbereitet.")
