import hashlib
import json
import os
import shutil
import tempfile
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path, PurePosixPath
from typing import Callable, Any

from backend.models.types import ProfileData
from backend.services.modrinth import ModrinthService, USER_AGENT, select_preferred_version
from backend.services.curseforge import CurseForgeService


class ModpackInstallError(RuntimeError):
    pass


def _safe_target(root: Path, relative: str) -> Path:
    pure = PurePosixPath(relative.replace("\\", "/"))
    if pure.is_absolute() or ".." in pure.parts:
        raise ModpackInstallError(f"Unsicherer Pfad im Modpack: {relative}")
    target = (root / Path(*pure.parts)).resolve()
    if root.resolve() not in target.parents and target != root.resolve():
        raise ModpackInstallError(f"Pfad verlässt das Profil: {relative}")
    return target


def _download(urls: list[str], destination: Path, hashes: dict[str, str]) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    error: Exception | None = None
    for url in urls:
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=45) as response, temporary.open("wb") as output:
                shutil.copyfileobj(response, output, length=1024 * 256)
            for algorithm in ("sha512", "sha1"):
                expected = str(hashes.get(algorithm, "")).lower()
                if expected:
                    digest = hashlib.new(algorithm, temporary.read_bytes()).hexdigest().lower()
                    if digest != expected:
                        raise ModpackInstallError(f"Hash-Prüfung fehlgeschlagen: {destination.name}")
                    break
            os.replace(temporary, destination)
            return
        except Exception as exc:
            error = exc
            temporary.unlink(missing_ok=True)
    raise ModpackInstallError(f"Download fehlgeschlagen ({destination.name}): {error}")


def _extract_overrides(archive: zipfile.ZipFile, prefix: str, profile_root: Path) -> None:
    normalized = prefix.rstrip("/") + "/"
    for info in archive.infolist():
        if not info.filename.startswith(normalized) or info.is_dir():
            continue
        relative = info.filename[len(normalized):]
        target = _safe_target(profile_root, relative)
        target.parent.mkdir(parents=True, exist_ok=True)
        with archive.open(info) as source, target.open("wb") as output:
            shutil.copyfileobj(source, output)


def install_modrinth_modpack(
    project_id: str,
    profile: ProfileData,
    progress: Callable[[float, str], None] | None = None,
    service: ModrinthService | None = None,
) -> dict[str, Any]:
    """Install a Modrinth .mrpack into an already-created isolated profile."""
    svc = service or ModrinthService()
    versions = svc.get_project_versions(project_id)
    selected = select_preferred_version(versions)
    if not selected:
        raise ModpackInstallError("Für dieses Modpack wurde keine installierbare Version gefunden.")
    files = selected.get("files", [])
    pack_file = next((item for item in files if str(item.get("filename", "")).endswith(".mrpack")), None)
    if pack_file is None:
        pack_file = next((item for item in files if item.get("primary")), files[0] if files else None)
    if not pack_file or not pack_file.get("url"):
        raise ModpackInstallError("Die Modpack-Datei besitzt keinen Download-Link.")

    if progress:
        progress(0.03, "Lade Modpack-Manifest…")
    with tempfile.TemporaryDirectory(prefix="ezclient-modpack-") as temporary_dir:
        archive_path = Path(temporary_dir) / "pack.mrpack"
        _download([pack_file["url"]], archive_path, pack_file.get("hashes", {}))
        with zipfile.ZipFile(archive_path) as archive:
            try:
                index = json.loads(archive.read("modrinth.index.json").decode("utf-8"))
            except (KeyError, json.JSONDecodeError, UnicodeDecodeError) as exc:
                raise ModpackInstallError("Ungültiges Modrinth-Modpack: Index fehlt oder ist beschädigt.") from exc

            dependencies = index.get("dependencies", {})
            minecraft_version = str(dependencies.get("minecraft", ""))
            if not minecraft_version:
                raise ModpackInstallError("Das Modpack nennt keine Minecraft-Version.")
            loader = "Vanilla"
            for dependency, label in (
                ("fabric-loader", "Fabric"), ("quilt-loader", "Quilt"),
                ("neoforge", "NeoForge"), ("forge", "Forge"),
            ):
                if dependencies.get(dependency):
                    loader = label
                    break
            profile.minecraft_version = minecraft_version
            profile.loader = loader

            entries = []
            for entry in index.get("files", []):
                env = entry.get("env", {})
                if str(env.get("client", "required")).lower() == "unsupported":
                    continue
                if not entry.get("downloads") or not entry.get("path"):
                    continue
                entries.append(entry)

            total = max(1, len(entries))
            completed = 0
            with ThreadPoolExecutor(max_workers=6) as executor:
                futures = {
                    executor.submit(
                        _download,
                        list(entry.get("downloads", [])),
                        _safe_target(profile.path, str(entry["path"])),
                        dict(entry.get("hashes", {})),
                    ): entry for entry in entries
                }
                for future in as_completed(futures):
                    future.result()
                    completed += 1
                    if progress:
                        progress(0.08 + (completed / total) * 0.82, f"Installiere Dateien… {completed}/{total}")

            _extract_overrides(archive, "overrides", profile.path)
            _extract_overrides(archive, "client-overrides", profile.path)

    if progress:
        progress(1.0, "Modpack ist spielbereit")
    return {
        "name": index.get("name", profile.name),
        "version": index.get("versionId", selected.get("version_number", "")),
        "minecraft_version": profile.minecraft_version,
        "loader": profile.loader,
        "files": len(entries),
    }


def install_curseforge_modpack(
    project_id: str,
    profile: ProfileData,
    progress: Callable[[float, str], None] | None = None,
    service: CurseForgeService | None = None,
) -> dict[str, Any]:
    """Install a CurseForge manifest pack into an isolated profile."""
    svc = service or CurseForgeService()
    versions = svc.get_project_versions(project_id, mc_version=None, loader=None)
    if not versions:
        raise ModpackInstallError("Für dieses CurseForge-Modpack wurde keine Datei gefunden.")
    pack_file = versions[0].get("files", [{}])[0]
    if not pack_file.get("url"):
        raise ModpackInstallError("Die CurseForge-Modpack-Datei besitzt keinen Download-Link.")
    if progress:
        progress(0.03, "Lade CurseForge-Manifest …")
    with tempfile.TemporaryDirectory(prefix="ezclient-cfpack-") as temporary_dir:
        archive_path = Path(temporary_dir) / "pack.zip"
        _download([str(pack_file["url"])], archive_path, {})
        with zipfile.ZipFile(archive_path) as archive:
            try:
                manifest = json.loads(archive.read("manifest.json").decode("utf-8"))
            except (KeyError, json.JSONDecodeError, UnicodeDecodeError) as exc:
                raise ModpackInstallError("Ungültiges CurseForge-Modpack: manifest.json fehlt oder ist beschädigt.") from exc
            minecraft = manifest.get("minecraft") if isinstance(manifest.get("minecraft"), dict) else {}
            profile.minecraft_version = str(minecraft.get("version") or profile.minecraft_version)
            loaders = minecraft.get("modLoaders") if isinstance(minecraft.get("modLoaders"), list) else []
            loader_id = str(loaders[0].get("id") or "") if loaders and isinstance(loaders[0], dict) else ""
            profile.loader = "NeoForge" if loader_id.startswith("neoforge-") else ("Forge" if loader_id.startswith("forge-") else ("Fabric" if loader_id.startswith("fabric-") else "Vanilla"))
            entries = [entry for entry in manifest.get("files", []) if isinstance(entry, dict) and entry.get("required", True)]
            resolved: list[dict[str, Any]] = []
            for index, entry in enumerate(entries):
                item = svc.get_file(entry.get("projectID"), entry.get("fileID"))
                if not item.get("url"):
                    raise ModpackInstallError(f"CurseForge-Datei {entry.get('fileID')} konnte nicht aufgelöst werden.")
                resolved.append(item)
                if progress and index % 10 == 0:
                    progress(0.05 + (index / max(1, len(entries))) * 0.18, "Löse Modpack-Dateien auf …")
            total = max(1, len(resolved))
            with ThreadPoolExecutor(max_workers=6) as executor:
                futures = {
                    executor.submit(_download, [item["url"]], profile.mods_path / item["filename"], {}): item
                    for item in resolved
                }
                completed = 0
                for future in as_completed(futures):
                    future.result()
                    completed += 1
                    if progress:
                        progress(0.24 + (completed / total) * 0.68, f"Installiere Dateien … {completed}/{total}")
            _extract_overrides(archive, str(manifest.get("overrides") or "overrides"), profile.path)
    if progress:
        progress(1.0, "CurseForge-Modpack ist spielbereit")
    return {"name": manifest.get("name", profile.name), "version": manifest.get("version", ""), "minecraft_version": profile.minecraft_version, "loader": profile.loader, "files": len(resolved)}
