import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from backend.controllers.profile_controller import ProfileController
from backend.models.types import ProfileData


class DummyExtensionScanner:
    _local_pack_entries = ProfileController._local_pack_entries
    _mods_with_local_extensions = ProfileController._mods_with_local_extensions

    def __init__(self):
        self._installed_registry = SimpleNamespace(installed_mods=[])

    @staticmethod
    def _extract_pack_icon(path):
        return ""


class LocalExtensionTests(unittest.TestCase):
    def test_active_profile_resource_and_shader_packs_are_merged(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with patch("backend.models.types.PROFILES_DIR", root):
                profile = ProfileData(id="Profile", name="Profile", minecraft_version="1.21.11")
                (profile.path / "resourcepacks").mkdir(parents=True)
                (profile.path / "shaderpacks").mkdir(parents=True)
                (profile.path / "resourcepacks" / "Fresh Animations.zip").write_bytes(b"pack")
                (profile.path / "shaderpacks" / "Complementary.zip").write_bytes(b"shader")

                entries = DummyExtensionScanner()._mods_with_local_extensions(profile)

        by_name = {entry.name: entry for entry in entries}
        self.assertIn("Fresh Animations", by_name)
        self.assertIn("Complementary", by_name)
        self.assertEqual(by_name["Fresh Animations"].source, "local")
        self.assertIn("Shader", by_name["Complementary"].description)


if __name__ == "__main__":
    unittest.main()
