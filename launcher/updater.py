"""GitHub-Release-Updater."""

import os
import subprocess
import tempfile
from pathlib import Path
import requests
from . import version


def check_for_update():
    """
    Pruefe ob eine neuere Version auf GitHub verfuegbar ist.

    Gibt (newer_version, download_url) zurueck oder (None, None).
    """
    try:
        url = f"https://api.github.com/repos/{version.GITHUB_REPO}/releases/latest"
        resp = requests.get(url, timeout=5)
        resp.raise_for_status()
        data = resp.json()

        latest_tag = data["tag_name"].lstrip("v")

        if latest_tag > version.__version__:
            # Finde das EzClient.exe-Asset
            for asset in data.get("assets", []):
                if asset["name"] == "EzClient.exe":
                    return (latest_tag, asset["browser_download_url"])

    except Exception:
        pass  # Fehler still schweigen — nie den Start blockieren

    return (None, None)


def download_and_apply_update(download_url):
    """
    Lade die neue Version herunter und ersetze die aktuelle Exe.

    Das geschieht ueber ein kleines Batch-Skript, das nach dem Launcher-Beenden ausgefuehrt wird.
    """
    try:
        temp_dir = Path(tempfile.gettempdir())
        new_exe = temp_dir / "EzClient_new.exe"

        # Download
        resp = requests.get(download_url, timeout=30)
        resp.raise_for_status()

        with open(new_exe, "wb") as f:
            f.write(resp.content)

        # Erstelle ein Batch-Script, das den alten Launcher ersetzt
        launcher_exe = Path(__file__).parent.parent / "EzClient.exe"
        if not launcher_exe.exists():
            launcher_exe = Path(os.environ.get("LOCALAPPDATA", "")) / "EzClient" / "EzClient.exe"

        bat_file = temp_dir / "update_ezclient.bat"
        bat_content = f"""@echo off
timeout /t 2 /nobreak
del "{launcher_exe}"
move "{new_exe}" "{launcher_exe}"
start "" "{launcher_exe}"
"""
        with open(bat_file, "w") as f:
            f.write(bat_content)

        # Batch-Script nach dem Launcher-Beenden starten
        subprocess.Popen(
            ["cmd", "/c", f"start /wait cmd /c {bat_file}"],
            shell=False,
        )

    except Exception:
        pass
