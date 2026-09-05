import json
from pathlib import Path

from backend.models.types import ProfileData
from backend.services.norisk_importer import discover_norisk_profiles, import_norisk_files


def test_discovers_only_profiles_with_real_directories(tmp_path: Path):
    root = tmp_path / "NoRiskClientV3"
    real = root / "data" / "profiles" / "My Profile"
    real.mkdir(parents=True)
    (root / "profiles.json").write_text(json.dumps({"profiles": [
        {"id": "real", "name": "My Profile", "path": "My Profile", "game_version": "1.20.1", "loader": "fabric", "settings": {"memory": {"max": 6144}}, "mods": [{"display_name": "Sodium"}]},
        {"id": "ghost", "name": "Ghost", "path": "missing", "game_version": "1.21.1"},
    ]}), encoding="utf-8")

    profiles = discover_norisk_profiles(root)

    assert len(profiles) == 1
    assert profiles[0]["name"] == "My Profile"
    assert profiles[0]["loader"] == "Fabric"
    assert profiles[0]["ramMb"] == 6144


def test_import_copies_portable_content_but_not_norisk_internals(tmp_path: Path, monkeypatch):
    monkeypatch.setattr("backend.services.norisk_importer._enrich_mod_metadata", lambda profile, progress=None: None)
    source = tmp_path / "source"
    destination = tmp_path / "destination"
    for folder, filename in (("mods", "sodium.jar"), ("custom_mods", "local.jar"), ("config", "sodium.json"), ("shaderpacks", "shader.zip"), ("saves", "level.dat"), ("NoRiskClient", "private.bin"), ("logs", "latest.log")):
        target = source / folder / filename
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text("data", encoding="utf-8")
    (source / "options.txt").write_text("maxFps:120", encoding="utf-8")
    profile = ProfileData(id="imported", name="Imported", minecraft_version="1.20.1", loader="Fabric")
    monkeypatch.setattr(type(profile), "path", property(lambda self: destination))
    discovered = {
        "path": str(source), "ramMb": 7168,
        "raw": {"settings": {"custom_jvm_args": ["-XX:+UseG1GC"]}, "mods": [{"display_name": "Sodium", "version": "1.0", "source": {"type": "modrinth", "project_id": "AANobbMI", "version_id": "v1", "file_name": "sodium.jar"}}]},
    }

    import_norisk_files(discovered, profile)

    assert (destination / "mods" / "sodium.jar").is_file()
    assert (destination / "mods" / "local.jar").is_file()
    assert (destination / "config" / "sodium.json").is_file()
    assert (destination / "shaderpacks" / "shader.zip").is_file()
    assert (destination / "saves" / "level.dat").is_file()
    assert (destination / "options.txt").is_file()
    assert (destination / "NoRiskClient").exists() is False
    assert (destination / "logs").exists() is False
    assert profile.ram_mb == 7168
    assert profile.jvm_args == "-XX:+UseG1GC"
    assert profile.mods[0].project_id == "AANobbMI"


def test_import_copies_mods_from_mod_cache(tmp_path: Path, monkeypatch):
    monkeypatch.setattr("backend.services.norisk_importer._enrich_mod_metadata", lambda profile, progress=None, **kwargs: None)
    norisk_root = tmp_path / "NoRiskClientV3"
    mod_cache = norisk_root / "meta" / "mod_cache"
    mod_cache.mkdir(parents=True)
    
    # Place mod jar in global mod_cache
    (mod_cache / "xaeroworldmap-fabric-26.2-1.44.2.jar").write_bytes(b"PK fake jar content")
    
    source = norisk_root / "data" / "profiles" / "Crack Attack 2"
    source.mkdir(parents=True)
    # Profile mods dir only contains internal empty nrc folder
    (source / "mods" / "nrc-26.2-fabric").mkdir(parents=True)

    destination = tmp_path / "destination"
    profile = ProfileData(id="crack_imported", name="Crack Attack 2", minecraft_version="26.2", loader="Fabric")
    monkeypatch.setattr(type(profile), "path", property(lambda self: destination))

    discovered = {
        "path": str(source),
        "norisk_root": str(norisk_root),
        "ramMb": 8192,
        "raw": {
            "mods": [{
                "display_name": "Xaero's World Map",
                "version": "1.44.2",
                "source": {
                    "type": "modrinth",
                    "project_id": "NcUtCpym",
                    "version_id": "NzjI8AbM",
                    "file_name": "xaeroworldmap-fabric-26.2-1.44.2.jar"
                }
            }]
        }
    }

    import_norisk_files(discovered, profile)

    # Mod must be copied from mod_cache into destination mods/
    assert (destination / "mods" / "xaeroworldmap-fabric-26.2-1.44.2.jar").is_file()
    # Internal nrc folder must NOT be in destination mods/
    assert not (destination / "mods" / "nrc-26.2-fabric").exists()
    assert len(profile.mods) == 1
    assert profile.mods[0].filename == "xaeroworldmap-fabric-26.2-1.44.2.jar"
    assert profile.mods[0].name == "Xaero's World Map"


def test_import_classifies_shaders_and_resourcepacks(tmp_path: Path, monkeypatch):
    monkeypatch.setattr("backend.services.norisk_importer._enrich_mod_metadata", lambda profile, progress=None, **kwargs: None)
    norisk_root = tmp_path / "NoRiskClientV3"
    mod_cache = norisk_root / "meta" / "mod_cache"
    mod_cache.mkdir(parents=True)

    (mod_cache / "Bliss_v2.1.2.zip").write_bytes(b"PK fake shader zip")
    (mod_cache / "Faithful_pack.zip").write_bytes(b"PK fake resource pack")
    (mod_cache / "regular-mod.jar").write_bytes(b"PK fake mod jar")

    source = norisk_root / "data" / "profiles" / "Packs Profile"
    source.mkdir(parents=True)

    destination = tmp_path / "destination"
    profile = ProfileData(id="packs_test", name="Packs Test", minecraft_version="1.20.1", loader="Fabric")
    monkeypatch.setattr(type(profile), "path", property(lambda self: destination))

    discovered = {
        "path": str(source),
        "norisk_root": str(norisk_root),
        "raw": {
            "mods": [
                {
                    "display_name": "Bliss Shaders",
                    "source": {"type": "curse_forge", "project_id": "610844", "file_name": "Bliss_v2.1.2.zip"}
                },
                {
                    "display_name": "Faithful 32x",
                    "source": {"type": "modrinth", "project_id": "faithful", "file_name": "Faithful_pack.zip"}
                },
                {
                    "display_name": "Regular Mod",
                    "source": {"type": "modrinth", "project_id": "regmod", "file_name": "regular-mod.jar"}
                }
            ]
        }
    }

    import_norisk_files(discovered, profile)

    # Shaders must go to shaderpacks
    assert (destination / "shaderpacks" / "Bliss_v2.1.2.zip").is_file()
    # Resource packs must go to resourcepacks
    assert (destination / "resourcepacks" / "Faithful_pack.zip").is_file()
    # Regular mod must go to mods
    assert (destination / "mods" / "regular-mod.jar").is_file()
    # profile.mods should ONLY contain the jar mod, NOT the shader or resource pack
    assert len(profile.mods) == 1
    assert profile.mods[0].filename == "regular-mod.jar"


def test_batch_enrich_mod_metadata(tmp_path: Path, monkeypatch):
    from backend.models.types import ModData
    from backend.services.norisk_importer import _enrich_mod_metadata

    destination = tmp_path / "destination"
    profile = ProfileData(id="meta_test", name="Meta Test", minecraft_version="1.20.1", loader="Fabric")
    monkeypatch.setattr(type(profile), "path", property(lambda self: destination))

    # Mock Modrinth batch
    monkeypatch.setattr(
        "backend.services.norisk_importer._fetch_modrinth_batch",
        lambda ids: {"sdQwPACz": {"id": "sdQwPACz", "slug": "dreamshift", "title": "Dreamshift", "description": "Mod description", "icon_url": "https://icon.png"}}
    )
    # Mock CurseForge batch
    monkeypatch.setattr(
        "backend.services.norisk_importer._fetch_curseforge_batch",
        lambda ids: {"416089": {"id": 416089, "slug": "simple-voice-chat", "name": "Simple Voice Chat", "summary": "Voice chat", "logo": {"thumbnailUrl": "https://cf.png"}}}
    )

    profile.mods = [
        ModData(
            project_id="sdQwPACz", slug="sdQwPACz", name="dreamshift", version_id="v1",
            version="0.1.3", filename="dreamshift-0.1.3.jar", source="modrinth"
        ),
        ModData(
            project_id="416089", slug="416089", name="voicechat", version_id="v2",
            version="2.6.20", filename="voicechat.jar", source="curseforge"
        ),
    ]

    _enrich_mod_metadata(profile)

    assert profile.mods[0].slug == "dreamshift"
    assert profile.mods[0].name == "Dreamshift"
    assert profile.mods[0].icon_url == "https://icon.png"

    assert profile.mods[1].slug == "simple-voice-chat"
    assert profile.mods[1].name == "Simple Voice Chat"
    assert profile.mods[1].icon_url == "https://cf.png"


