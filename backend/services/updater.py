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

    # Prefer EzClient_Setup_x64.exe or EzClient-Setup.exe installer asset
    for a in assets:
        name = a.get("name", "")
        if name.endswith(".exe"):
            if "setup" in name.lower() or "installer" in name.lower():
                download_url = a.get("browser_download_url", "")
                asset_name = name
                asset_size = a.get("size", 0)
                break

    # If no explicit setup name found, pick any exe
    if not download_url:
        for a in assets:
            name = a.get("name", "")
            if name.endswith(".exe"):
                download_url = a.get("browser_download_url", "")
                asset_name = name
                asset_size = a.get("size", 0)
                break

    # If no exe asset in release, point to release page
    if not download_url:
        download_url = html_url

    # Ensure saved filename is always distinguishable from running launcher
    target_asset_name = asset_name or "EzClient_Setup_x64.exe"
    if target_asset_name.lower() == "ezclient.exe":
        target_asset_name = "EzClient_Update_Setup.exe"

    return {
        "update_available": is_newer,
        "current_version": current_version,
        "latest_version": tag_name.lstrip("vV"),
        "release_name": release_name,
        "changelog": body.strip() or "Keine Versionshinweise verfügbar.",
        "published_at": published_at,
        "download_url": download_url,
        "asset_name": target_asset_name,
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
    Spawns installer process in update mode and terminates current launcher instance cleanly.
    """
    try:
        current_exe = Path(sys.executable)
        if getattr(sys, "frozen", False):
            install_dir = current_exe.parent
        else:
            install_dir = Path(os.getenv("LOCALAPPDATA", str(Path.home()))) / "Programs" / "EzClient"

        args = [str(installer_path), "--update", f"--dir={install_dir}"]
        creationflags = 0
        if sys.platform == "win32":
            creationflags = subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP
        subprocess.Popen(args, creationflags=creationflags, close_fds=True)

        # Force exit immediately so Windows releases file locks on EzClient.exe
        import os
        os._exit(0)
    except Exception as e:
        print(f"[Updater] Error launching installer: {e}")
