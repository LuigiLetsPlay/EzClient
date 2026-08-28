"""First-run downloader for the official Minecraft files used by EzClient."""
import hashlib
import json
import os
import threading
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Callable

from backend.models.types import ProfileData

VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader"
ASSET_BASE = "https://resources.download.minecraft.net"

# Parallel download settings
_LIBRARY_WORKERS = 12
_ASSET_WORKERS = 24


def _json(url: str) -> dict:
    request = urllib.request.Request(url, headers={"User-Agent": "EzClient/1.8.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def _download(url: str, target: Path, sha1: str = "", expected_size: int = 0) -> bool:
    """Download one file and report whether a network download was necessary."""
    if target.is_file():
        file_size = target.stat().st_size
        if file_size > 0:
            # Fast path: if size matches and we have a hash, skip the expensive
            # SHA1 read for files that are almost certainly correct.
            if expected_size > 0 and file_size == expected_size:
                return False
            if not sha1 or hashlib.sha1(target.read_bytes()).hexdigest() == sha1:
                return False
    target.parent.mkdir(parents=True, exist_ok=True)
    temporary = target.with_suffix(target.suffix + ".part")
    request = urllib.request.Request(url, headers={"User-Agent": "EzClient/1.8.0"})
    with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as output:
        while chunk := response.read(1024 * 256):
            output.write(chunk)
    if sha1 and hashlib.sha1(temporary.read_bytes()).hexdigest() != sha1:
        temporary.unlink(missing_ok=True)
        raise RuntimeError(f"Prüfsumme stimmt nicht: {target.name}")
    os.replace(temporary, target)
    return True


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
    classifier = f"-{parts[3]}" if len(parts) > 3 else ""
    path = f"{group.replace('.', '/')}/{name}/{version}/{name}-{version}{classifier}.jar"
    return {"url": f"{base_url}/{path}", "path": path, "sha1": library.get("sha1", "")}


def _libraries_are_ready(mc_dir: Path, libraries: list[dict]) -> bool:
    for library in libraries:
        artifact = _library_artifact(library)
        if artifact and not (mc_dir / "libraries" / artifact["path"]).is_file():
            return False
    return True


def _download_libraries(
    mc_dir: Path, libraries: list[dict], notify: Callable[[str], None], label: str
) -> None:
    artifacts = [_library_artifact(library) for library in libraries]
    artifacts = [item for item in artifacts if item]
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
            try:
                future.result()
            except Exception as exc:
                # Log but don't abort - retry missing assets on next launch
                print(f"[GameBootstrap] Asset download failed: {exc}")

    elapsed = time.monotonic() - start_time
    notify(
        f"Minecraft-Assets fertig: {progress['downloaded']} neu geladen "
        f"({progress['bytes'] / (1024 * 1024):.1f} MB in {elapsed:.1f}s)"
    )


def ensure_game_ready(profile: ProfileData, mc_dir: Path, notify: Callable[[str], None]) -> None:
    """Download Mojang/Fabric files once; no official launcher is involved."""
    version = profile.minecraft_version
    vanilla_dir = mc_dir / "versions" / version
    vanilla_json = vanilla_dir / f"{version}.json"
    has_vanilla = vanilla_json.exists() and (vanilla_dir / f"{version}.jar").exists()
    vanilla_data = json.loads(vanilla_json.read_text(encoding="utf-8")) if has_vanilla else {}
    vanilla_libraries_ready = has_vanilla and _libraries_are_ready(mc_dir, vanilla_data.get("libraries", []))
    loader_files = list((mc_dir / "versions").glob(f"fabric-loader-*-{version}/*.json"))
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
        profile.loader.lower() != "fabric" or (has_loader and loader_libraries_ready)
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
        notify("Installiere Fabric-Komponenten…")
        loaders = _json(f"{FABRIC_META}/{version}")
        loader = next((item for item in loaders if item.get("loader", {}).get("stable")), loaders[0] if loaders else None)
        if not loader:
            raise RuntimeError(f"Kein Fabric-Loader für Minecraft {version} verfügbar.")
        loader_version = loader["loader"]["version"]
        fabric = _json(f"{FABRIC_META}/{version}/{loader_version}/profile/json")
        fabric_id = fabric.get("id", f"fabric-loader-{loader_version}-{version}")
        fabric_dir = mc_dir / "versions" / fabric_id
        fabric_dir.mkdir(parents=True, exist_ok=True)
        (fabric_dir / f"{fabric_id}.json").write_text(json.dumps(fabric, indent=2), encoding="utf-8")
        _download_libraries(mc_dir, fabric.get("libraries", []), notify, "Fabric-Bibliotheken")
    notify("Minecraft-Dateien sind vorbereitet.")
