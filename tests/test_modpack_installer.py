import hashlib
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from backend.models.types import ProfileData
from backend.services.modpack_installer import ModpackInstallError, install_curseforge_modpack, install_modrinth_modpack


class FakeModrinth:
    def __init__(self, archive: Path):
        self.archive = archive

    def get_project_versions(self, project_id):
        return [{
            "version_number": "1.0.0",
            "version_type": "release",
            "files": [{"primary": True, "filename": "pack.mrpack", "url": self.archive.as_uri(), "hashes": {}}],
        }]


class FakeCurseForge:
    def __init__(self, archive: Path, mod_file: Path):
        self.archive = archive
        self.mod_file = mod_file

    def get_project_versions(self, project_id, mc_version=None, loader=None):
        return [{"files": [{"url": self.archive.as_uri(), "filename": "pack.zip"}]}]

    def get_file(self, project_id, file_id):
        return {"url": self.mod_file.as_uri(), "filename": "example.jar"}


class ModpackInstallerTests(unittest.TestCase):
    def test_installs_files_overrides_and_loader_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            payload = root / "example.jar"
            payload.write_bytes(b"a valid mod payload")
            archive = root / "example.mrpack"
            index = {
                "formatVersion": 1,
                "game": "minecraft",
                "name": "Example Pack",
                "versionId": "1.0.0",
                "dependencies": {"minecraft": "1.21.11", "fabric-loader": "0.18.4"},
                "files": [{
                    "path": "mods/example.jar",
                    "hashes": {"sha1": hashlib.sha1(payload.read_bytes()).hexdigest()},
                    "downloads": [payload.as_uri()],
                    "fileSize": payload.stat().st_size,
                }],
            }
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("modrinth.index.json", json.dumps(index))
                output.writestr("overrides/config/example.json", "{}")

            with patch("backend.models.types.PROFILES_DIR", root / "profiles"):
                profile = ProfileData(id="Example Pack", name="Example Pack", minecraft_version="placeholder")
                result = install_modrinth_modpack("example", profile, service=FakeModrinth(archive))

                self.assertEqual(profile.minecraft_version, "1.21.11")
                self.assertEqual(profile.loader, "Fabric")
                self.assertEqual((profile.path / "mods" / "example.jar").read_bytes(), payload.read_bytes())
                self.assertEqual((profile.path / "config" / "example.json").read_text(), "{}")
                self.assertEqual(result["files"], 1)

    def test_rejects_paths_outside_profile(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            archive = root / "unsafe.mrpack"
            index = {
                "formatVersion": 1,
                "game": "minecraft",
                "name": "Unsafe",
                "versionId": "1",
                "dependencies": {"minecraft": "1.21.11"},
                "files": [{"path": "../escape.jar", "hashes": {}, "downloads": [archive.as_uri()]}],
            }
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("modrinth.index.json", json.dumps(index))

            with patch("backend.models.types.PROFILES_DIR", root / "profiles"):
                profile = ProfileData(id="Unsafe", name="Unsafe", minecraft_version="placeholder")
                with self.assertRaises(ModpackInstallError):
                    install_modrinth_modpack("unsafe", profile, service=FakeModrinth(archive))

    def test_installs_curseforge_manifest_modpack(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            payload = root / "example.jar"
            payload.write_bytes(b"curseforge mod")
            archive = root / "pack.zip"
            manifest = {
                "name": "Curse Pack", "version": "2.0",
                "minecraft": {"version": "1.20.1", "modLoaders": [{"id": "forge-47.3.0", "primary": True}]},
                "files": [{"projectID": 123, "fileID": 456, "required": True}],
                "overrides": "overrides",
            }
            with zipfile.ZipFile(archive, "w") as output:
                output.writestr("manifest.json", json.dumps(manifest))
                output.writestr("overrides/config/example.toml", "enabled=true")
            with patch("backend.models.types.PROFILES_DIR", root / "profiles"):
                profile = ProfileData(id="Curse Pack", name="Curse Pack", minecraft_version="placeholder")
                result = install_curseforge_modpack("pack", profile, service=FakeCurseForge(archive, payload))
                self.assertEqual(profile.minecraft_version, "1.20.1")
                self.assertEqual(profile.loader, "Forge")
                self.assertEqual((profile.mods_path / "example.jar").read_bytes(), payload.read_bytes())
                self.assertEqual((profile.path / "config" / "example.toml").read_text(), "enabled=true")
                self.assertEqual(result["files"], 1)


if __name__ == "__main__":
    unittest.main()
