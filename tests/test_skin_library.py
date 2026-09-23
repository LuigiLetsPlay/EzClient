import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from PIL import Image

from backend.services.skin_service import (
    delete_saved_skin_from_library,
    get_saved_skins,
    save_skin_to_library,
)


class SkinLibraryTests(unittest.TestCase):
    def test_saved_skin_keeps_full_immutable_texture_and_model(self):
        with tempfile.TemporaryDirectory() as tmp:
            data_dir = Path(tmp) / "data"
            source = Path(tmp) / "account.png"
            Image.new("RGBA", (64, 64), (10, 20, 30, 255)).save(source)

            with patch("backend.models.types.DATA_DIR", data_dir):
                saved = save_skin_to_library("Main", str(source), "https://example.test/head.png", "slim")
                entry = saved[0]
                snapshot = Path(entry["path"])

                self.assertTrue(snapshot.is_file())
                self.assertNotEqual(source, snapshot)
                self.assertEqual(source.read_bytes(), snapshot.read_bytes())
                self.assertEqual("slim", entry["model"])
                self.assertTrue(entry["previewUrl"].startswith("file:///"))

                Image.new("RGBA", (64, 64), (200, 0, 0, 255)).save(source)
                self.assertNotEqual(source.read_bytes(), snapshot.read_bytes())

                delete_saved_skin_from_library(entry["id"])
                self.assertEqual([], get_saved_skins())
                self.assertFalse(snapshot.exists())


if __name__ == "__main__":
    unittest.main()
