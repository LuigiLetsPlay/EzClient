"""Minecraft-Installation und -Start mit minecraft-launcher-lib."""

import subprocess
from pathlib import Path
from minecraft_launcher_lib.utils import get_version_list
from minecraft_launcher_lib.install import install_minecraft_version
from minecraft_launcher_lib.command import get_minecraft_command


MC_DIR = Path.home() / "AppData" / "Roaming" / ".minecraft"


def list_versions():
    """Gebe eine Liste von Minecraft-Release-Versionen zurueck."""
    all_versions = get_version_list()
    releases = [v for v in all_versions if v.get("type") == "release"]
    # Neueste ~25 Versionen
    return sorted(releases[-25:], key=lambda x: x["releaseTime"], reverse=True)


def install(version_name, progress_callback):
    """
    Installiere eine Minecraft-Version.

    progress_callback({status, progress, max}) wird regelmaessig aufgerufen.
    """

    def callback(v):
        """minecraft-launcher-lib ruft das mit einer Zahl auf."""
        progress_callback(v)

    install_minecraft_version(
        version_id=version_name,
        minecraft_directory=str(MC_DIR),
        callback=callback,
    )


def launch(version_name, profile):
    """
    Starte Minecraft mit dem gegebenen Profil.

    profile: {name, uuid, access_token} vom Login
    """
    command = get_minecraft_command(
        version=version_name,
        minecraft_directory=str(MC_DIR),
        username=profile["name"],
        uuid=profile["uuid"],
        access_token=profile["access_token"],
    )

    # Starte Minecraft als separaten Prozess
    subprocess.Popen(command)
