import tempfile
import unittest
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from unittest.mock import patch

from backend.services.game_bootstrap import _download, _library_artifact
from backend.services.direct_launch import _java_major, maven_module_key


class GameBootstrapTests(unittest.TestCase):
    def test_java_8_legacy_version_string_is_detected_as_major_8(self):
        completed = type("Completed", (), {
            "stdout": "",
            "stderr": "    java.version = 1.8.0_504\n",
        })()
        with patch("backend.services.direct_launch.subprocess.run", return_value=completed):
            self.assertEqual(8, _java_major("java"))

    def test_maven_conflicts_ignore_version_and_classifier(self):
        self.assertEqual(
            maven_module_key("org.lwjgl.lwjgl:lwjgl:2.9.4+legacyfabric.17"),
            maven_module_key("org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209"),
        )

    def test_legacy_fabric_native_library_uses_windows_classifier(self):
        with patch("backend.services.game_bootstrap.platform.system", return_value="Windows"):
            artifact = _library_artifact(
                {
                    "name": "org.lwjgl.lwjgl:lwjgl-platform:2.9.4+legacyfabric.17",
                    "url": "https://maven.legacyfabric.net/",
                    "natives": {"windows": "natives-windows"},
                }
            )

        self.assertTrue(
            artifact["path"].endswith(
                "lwjgl-platform-2.9.4+legacyfabric.17-natives-windows.jar"
            )
        )
        self.assertTrue(artifact["url"].endswith(artifact["path"]))

    def test_parallel_downloads_to_same_target_do_not_share_part_file(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.bin"
            source.write_bytes(b"EzClient parallel download test")
            target = root / "downloads" / "target.bin"

            with ThreadPoolExecutor(max_workers=8) as pool:
                results = list(pool.map(lambda _: _download(source.as_uri(), target), range(16)))

            self.assertEqual(source.read_bytes(), target.read_bytes())
            self.assertEqual(1, results.count(True))
            self.assertFalse(list(target.parent.glob("*.part")))


if __name__ == "__main__":
    unittest.main()
