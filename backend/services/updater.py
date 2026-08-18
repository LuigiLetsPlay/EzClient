import os
import sys
import json
import re
import urllib.request
import urllib.error
import subprocess
import shutil
import tempfile
from pathlib import Path
from typing import Callable, Any
from backend.models.types import APP_VERSION, GITHUB_REPO, CACHE_DIR

UPDATES_DIR = CACHE_DIR / "updates"
UPDATES_DIR.mkdir(parents=True, exist_ok=True)

USER_AGENT = f"EzClient-Updater/{APP_VERSION}"


def parse_version_tuple(v_str: str) -> tuple[int, ...]:
    """Parse version string like 'v1.0.1' or '1.2.0' into numeric tuple (1, 0, 1)."""
    clean = re.sub(r"^[vV]", "", v_str.strip())
    parts = []
    for p in clean.split("."):
        try:
            parts.append(int(re.match(r"^\d+", p).group()))
        except Exception:
            parts.append(0)
    while len(parts) < 3:
        parts.append(0)
    return tuple(parts)


def check_for_updates(current_version: str = APP_VERSION, repo: str = GITHUB_REPO) -> dict[str, Any] | None:
    """
    Checks GitHub Releases API for latest release.
    Returns dictionary with update details if a newer version is available, else None.
    """
    url = f"https://api.github.com/repos/{repo}/releases/latest"
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "application/vnd.github.v3+json",
        }
    )

    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        if e.code == 404:
            # No releases published yet on repository
            return None
        print(f"[Updater] GitHub HTTP error: {e.code}")
        return None
    except Exception as e:
        print(f"[Updater] Error checking updates: {e}")
        return None

    tag_name = data.get("tag_name", "")
    release_name = data.get("name", tag_name)
    body = data.get("body", "")
    published_at = data.get("published_at", "")
    html_url = data.get("html_url", f"https://github.com/{repo}/releases")

    remote_ver = parse_version_tuple(tag_name or release_name)
    current_ver = parse_version_tuple(current_version)

    is_newer = remote_ver > current_ver

    # Find downloadable asset (.exe installer or binary)
    assets = data.get("assets", [])
    download_url = ""
    asset_name = ""
    asset_size = 0

    # Prefer EzClient-Setup.exe or EzClient.exe
    for a in assets:
        name = a.get("name", "")
        if name.endswith(".exe"):
            if "setup" in name.lower() or "installer" in name.lower():
                download_url = a.get("browser_download_url", "")
                asset_name = name
                asset_size = a.get("size", 0)
                break
            elif not download_url:
                download_url = a.get("browser_download_url", "")
                asset_name = name
                asset_size = a.get("size", 0)

    # If no exe asset in release, point to release page
    if not download_url:
        download_url = html_url

    return {
        "update_available": is_newer,
        "current_version": current_version,
        "latest_version": tag_name.lstrip("vV"),
        "release_name": release_name,
        "changelog": body.strip() or "Keine Versionshinweise verfügbar.",
        "published_at": published_at,
        "download_url": download_url,
        "asset_name": asset_name or f"EzClient-{tag_name}.exe",
        "asset_size_mb": round(asset_size / (1024 * 1024), 2) if asset_size else 0.0,
        "html_url": html_url,
    }


def download_update_file(download_url: str, target_filename: str, progress_callback: Callable[[float, str], None] | None = None) -> Path | None:
    """
    Downloads update binary with live progress reporting.
    Returns path to downloaded file.
    """
    if not download_url or not download_url.startswith("http"):
        return None

    dest_file = UPDATES_DIR / target_filename
    temp_file = dest_file.with_suffix(".tmp")

    try:
        req = urllib.request.Request(download_url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(req, timeout=30) as resp:
            total_bytes = int(resp.headers.get("Content-Length", 0))
            downloaded = 0
            chunk_size = 64 * 1024  # 64 KB chunks

            with open(temp_file, "wb") as f:
                while True:
                    chunk = resp.read(chunk_size)
                    if not chunk:
                        break
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_bytes > 0 and progress_callback:
                        pct = downloaded / total_bytes
                        mb_done = downloaded / (1024 * 1024)
                        mb_total = total_bytes / (1024 * 1024)
                        progress_callback(pct, f"{mb_done:.1f} MB / {mb_total:.1f} MB")

        if temp_file.exists():
            if dest_file.exists():
                dest_file.unlink(missing_ok=True)
            temp_file.replace(dest_file)
            if progress_callback:
                progress_callback(1.0, "Download abgeschlossen!")
            return dest_file
    except Exception as e:
        print(f"[Updater] Download error: {e}")
        if temp_file.exists():
            temp_file.unlink(missing_ok=True)
    return None


def run_installer_and_exit(installer_path: Path) -> None:
    """
    Spawns installer process and terminates current launcher instance cleanly.
    """
    try:
        if sys.platform == "win32":
            subprocess.Popen([str(installer_path)], shell=True)
        else:
            subprocess.Popen([str(installer_path)])
        sys.exit(0)
    except Exception as e:
        print(f"[Updater] Error launching installer: {e}")
