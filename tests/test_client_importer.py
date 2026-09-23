import json
from pathlib import Path
from unittest.mock import patch

from backend.services.client_importer import discover_client_profiles


def test_discovers_modrinth_and_curseforge_profiles(tmp_path: Path):
    appdata = tmp_path / "AppData" / "Roaming"
    home = tmp_path / "User"

    modrinth = appdata / "com.modrinth.theseus" / "profiles" / "speedrun"
    (modrinth / "mods").mkdir(parents=True)
    (modrinth / "mods" / "sodium.jar").write_bytes(b"jar")
    (modrinth / "profile.json").write_text(json.dumps({
        "name": "Speedrun", "game_version": "26.2", "loader": "fabric"
    }), encoding="utf-8")

    curse = home / "curseforge" / "minecraft" / "Instances" / "Adventure"
    (curse / "mods").mkdir(parents=True)
    (curse / "minecraftinstance.json").write_text(json.dumps({
        "name": "Adventure", "gameVersion": "1.20.1",
        "baseModLoader": {"name": "forge-47.4.0"},
    }), encoding="utf-8")

    with patch.dict("os.environ", {"APPDATA": str(appdata)}), \
            patch("backend.services.client_importer.Path.home", return_value=home), \
            patch("backend.services.client_importer.discover_norisk_profiles", return_value=[]):
        profiles = discover_client_profiles()

    by_client = {item["sourceClient"]: item for item in profiles}
    assert by_client["Modrinth"]["name"] == "Speedrun"
    assert by_client["Modrinth"]["loader"] == "Fabric"
    assert by_client["Modrinth"]["modCount"] == 1
    assert by_client["CurseForge"]["name"] == "Adventure"
    assert by_client["CurseForge"]["loader"] == "Forge"


def test_discovers_prism_game_directory(tmp_path: Path):
    appdata = tmp_path / "Roaming"
    instance = appdata / "PrismLauncher" / "instances" / "Building"
    game = instance / ".minecraft"
    game.mkdir(parents=True)
    (instance / "instance.cfg").write_text("name=Building World\n", encoding="utf-8")
    (instance / "mmc-pack.json").write_text(json.dumps({"components": [
        {"uid": "net.minecraft", "version": "26.1.1"},
        {"uid": "net.fabricmc.fabric-loader", "version": "0.19.5"},
    ]}), encoding="utf-8")

    with patch.dict("os.environ", {"APPDATA": str(appdata)}), \
            patch("backend.services.client_importer.discover_norisk_profiles", return_value=[]):
        profiles = discover_client_profiles()

    prism = next(item for item in profiles if item["sourceClient"] == "Prism Launcher")
    assert prism["path"] == str(game)
    assert prism["version"] == "26.1.1"
    assert prism["loader"] == "Fabric"
