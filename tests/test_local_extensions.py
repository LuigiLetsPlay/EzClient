import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from backend.controllers.profile_controller import ProfileController
from backend.models.types import ProfileData
from backend.services.mod_scanner import InstalledModRegistry


class LocalExtensionTests(unittest.TestCase):
    def test_manually_copied_jar_is_included_in_extension_model(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with patch("backend.models.types.PROFILES_DIR", root):
                profile = ProfileData(id="Profile", name="Profile", minecraft_version="26.2")
                profile.mods_path.mkdir(parents=True)
                (profile.mods_path / "manual.jar").write_bytes(b"not-a-real-zip")
                controller = ProfileController.__new__(ProfileController)
                controller._installed_registry = InstalledModRegistry(profile.mods_path)
                entries = controller._mods_with_local_extensions(profile)

        self.assertEqual(1, len(entries))
        self.assertEqual("manual.jar", entries[0].filename)
        self.assertEqual("local", entries[0].source)

    def test_active_profile_resource_and_shader_packs_are_merged(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with patch("backend.models.types.PROFILES_DIR", root):
                profile = ProfileData(id="Profile", name="Profile", minecraft_version="1.21.11")
                rp_dir = profile.path / "resourcepacks"
                sp_dir = profile.path / "shaderpacks"
                rp_dir.mkdir(parents=True)
                sp_dir.mkdir(parents=True)
                (rp_dir / "Fresh Animations.zip").write_bytes(b"pack")
                (sp_dir / "Complementary.zip").write_bytes(b"shader")

                rp_entries = ProfileController._local_pack_entries(rp_dir, "resourcepacks", "Resource Pack")
                sp_entries = ProfileController._local_pack_entries(sp_dir, "shaderpacks", "Shader Pack")
                entries = rp_entries + sp_entries

        by_name = {entry.name: entry for entry in entries}
        self.assertIn("Fresh Animations", by_name)
        self.assertIn("Complementary", by_name)
        self.assertEqual(by_name["Fresh Animations"].source, "local")
        self.assertIn("Shader", by_name["Complementary"].description)


if __name__ == "__main__":
    unittest.main()
