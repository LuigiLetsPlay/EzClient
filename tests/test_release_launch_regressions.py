import hashlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from backend.models.types import ProfileData
from backend.services.direct_launch import find_version_meta
from backend.services.game_bootstrap import _download, _download_assets_parallel, _library_artifacts, ensure_game_ready
from backend.services.loader_versions import forge_coordinate, installed_forge_metadata


class ReleaseLaunchTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.addCleanup(self.temp.cleanup)

    def metadata(self, name, data):
        path = self.root / "versions" / name / f"{name}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data), encoding="utf-8")
        return path

    def test_fabric_never_launches_another_minecraft_version(self):
        self.metadata("fabric-loader-0.19.5-26.2", {"inheritsFrom": "26.2"})
        self.assertEqual({}, find_version_meta(self.root, "26.1", "Fabric")[1])

    def test_fabric_skips_corrupt_and_wrong_base_metadata(self):
        self.metadata("fabric-loader-0.19.6-26.1", {"inheritsFrom": "26.2"})
        good = self.metadata("fabric-loader-0.19.5-26.1", {"inheritsFrom": "26.1"})
        self.assertEqual(good, find_version_meta(self.root, "26.1")[0])

    def test_fabric_modpack_pin_is_exact(self):
        self.metadata("fabric-loader-0.19.6-26.1", {"inheritsFrom": "26.1"})
        old = self.metadata("fabric-loader-0.19.5-26.1", {"inheritsFrom": "26.1"})
        self.assertEqual(old, find_version_meta(self.root, "26.1", "Fabric", "0.19.5")[0])
        self.assertIsNone(find_version_meta(self.root, "26.1", "Fabric", "0.19.4")[0])

    def test_forge_coordinate_normalizes_all_saved_forms(self):
        for requested in ("47.4.0", "1.20.1-47.4.0", "1.20.1-forge-47.4.0"):
            self.assertEqual("1.20.1-47.4.0", forge_coordinate("1.20.1", requested))

    def test_legacy_forge_id_resolves_by_exact_library_and_base(self):
        coordinate = "1.8.9-11.15.1.2318-1.8.9"
        path = self.metadata("1.8.9-forge1.8.9-11.15.1.2318-1.8.9", {
            "inheritsFrom": "1.8.9", "libraries": [{"name": f"net.minecraftforge:forge:{coordinate}"}]})
        self.assertEqual([path], installed_forge_metadata(self.root, "1.8.9", coordinate))
        self.assertEqual([], installed_forge_metadata(self.root, "1.8.9", "11.15.1.9999"))
        self.assertEqual([], installed_forge_metadata(self.root, "1.12.2"))

    def test_normal_forge_profile_can_install_without_modpack_pin(self):
        profile = ProfileData("forge", "Forge", "1.20.1", loader="Forge", profile_type="raw")
        with patch("backend.services.game_bootstrap._json", side_effect=[
            {"versions": [{"id": "1.20.1", "url": "manifest"}]}, {"downloads": {"client": {"url": "client"}}}
        ]), patch("backend.services.game_bootstrap._download"), \
                patch("minecraft_launcher_lib.forge.find_forge_version", return_value="1.20.1-47.4.0"), \
                patch("minecraft_launcher_lib.forge.install_forge_version") as install, \
                patch("backend.services.java_runtime.install_required_java", return_value=Path("java.exe")):
            ensure_game_ready(profile, self.root, lambda _: None)
        self.assertEqual("1.20.1-47.4.0", install.call_args.args[0])
        self.assertEqual("1.20.1-47.4.0", profile.loader_version)

    def test_mojang_library_download_includes_java_and_native_artifacts(self):
        library = {"name": "org.lwjgl:lwjgl:2.9.4", "natives": {"windows": "natives-windows"},
                   "downloads": {"artifact": {"path": "lwjgl.jar", "url": "java"},
                                 "classifiers": {"natives-windows": {"path": "lwjgl-native.jar", "url": "native"}}}}
        with patch("platform.system", return_value="Windows"):
            self.assertEqual({"lwjgl.jar", "lwjgl-native.jar"}, {a["path"] for a in _library_artifacts([library])})

    def test_same_size_corrupt_download_is_replaced(self):
        target = self.root / "download.jar"
        target.write_bytes(b"bad!")
        with patch("urllib.request.urlopen", return_value=io.BytesIO(b"good")):
            self.assertTrue(_download("https://example.test/file", target, hashlib.sha1(b"good").hexdigest(), 4))
        self.assertEqual(b"good", target.read_bytes())

    def test_failed_asset_download_aborts_preparation(self):
        with patch("backend.services.game_bootstrap._download", side_effect=OSError("network failure")):
            with self.assertRaises(OSError):
                _download_assets_parallel(self.root, [{"hash": "a" * 40, "size": 4}], lambda _: None)


if __name__ == "__main__":
    unittest.main()
