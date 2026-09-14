from pathlib import Path
import tempfile
import unittest

from PIL import Image
from PySide6.QtGui import QColor, QImage

from backend.controllers.account_controller import _face_atlas
from backend.services.cape_media import AnimationOptions, generate_frame_sheet


class CapeEditorMediaTests(unittest.TestCase):
    def test_gif_sampling_respects_frame_durations_and_trim(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            tmp_path = Path(td)
            source = tmp_path / "uneven.gif"
            colors = [(255, 0, 0), (0, 255, 0), (0, 0, 255)]
            images = [Image.new("RGB", (10, 16), color) for color in colors]
            images[0].save(source, save_all=True, append_images=images[1:],
                           duration=[100, 700, 200], loop=0, disposal=2)

            manifest = generate_frame_sheet(source, tmp_path / "cape", AnimationOptions(0.2, 0.9, 10))
            self.assertEqual(manifest.frame_count, 7)
            with Image.open(tmp_path / "cape" / manifest.sheet) as sheet:
                pixels = [sheet.getpixel(((i % manifest.columns) * 256 + 20,
                                          (i // manifest.columns) * 128 + 20))[:3]
                          for i in range(manifest.frame_count)]
            self.assertEqual(pixels[:6], [(0, 255, 0)] * 6)
            self.assertEqual(pixels[-1], (0, 0, 255))

    def test_animated_cape_keeps_independent_sides(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            tmp_path = Path(td)
            source = tmp_path / "back.gif"
            Image.new("RGB", (10, 16), "red").save(source, save_all=True,
                                                     append_images=[Image.new("RGB", (10, 16), "blue")],
                                                     duration=[100, 100], loop=0)
            front = tmp_path / "front.png"
            left = tmp_path / "left.png"
            right = tmp_path / "right.png"
            top = tmp_path / "top.png"
            bottom = tmp_path / "bottom.png"
            Image.new("RGB", (10, 16), "green").save(front)
            Image.new("RGB", (10, 16), "yellow").save(left)
            Image.new("RGB", (10, 16), "magenta").save(right)
            Image.new("RGB", (10, 1), "cyan").save(top)
            Image.new("RGB", (10, 1), "white").save(bottom)
            sides = {name: {"source": str(path), "crop": [0, 0, 1, 1]}
                     for name, path in (("front", front), ("left", left), ("right", right),
                                        ("top", top), ("bottom", bottom))}
            manifest = generate_frame_sheet(source, tmp_path / "cape", AnimationOptions(0, 0.2, 10, face_sources=sides))
            with Image.open(tmp_path / "cape" / manifest.sheet) as sheet:
                for frame in range(manifest.frame_count):
                    ox = (frame % manifest.columns) * 256
                    oy = (frame // manifest.columns) * 128
                    self.assertEqual(sheet.getpixel((ox + 60, oy + 20))[:3], (0, 128, 0))
                    # left is at x=0..4, right is at x=44..48
                    self.assertEqual(sheet.getpixel((ox + 1, oy + 20))[:3], (255, 255, 0))
                    self.assertEqual(sheet.getpixel((ox + 45, oy + 20))[:3], (255, 0, 255))
                    # Top at x=4..44, y=0..4, Bottom at x=44..84, y=0..4
                    self.assertEqual(sheet.getpixel((ox + 10, oy + 1))[:3], (0, 255, 255))
                    self.assertEqual(sheet.getpixel((ox + 50, oy + 1))[:3], (255, 255, 255))

    def test_static_cape_places_all_six_faces_in_vanilla_uv(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            tmp_path = Path(td)
            faces = {}
            for name, color in (("back", "red"), ("front", "green"),
                                ("left", "yellow"), ("right", "magenta"),
                                ("top", "cyan"), ("bottom", "white")):
                path = tmp_path / f"{name}.png"
                Image.new("RGB", (10, 16) if name not in ("top", "bottom") else (10, 1), color).save(path)
                faces[name] = {"source": str(path), "crop": [0, 0, 1, 1]}
            atlas = _face_atlas(faces, 64)
            self.assertEqual(atlas.size(), QImage(64, 32, QImage.Format_RGBA8888).size())
            self.assertEqual(atlas.pixelColor(5, 5), QColor("red"))
            self.assertEqual(atlas.pixelColor(15, 5), QColor("green"))
            # left is at x=0, right is at x=11
            self.assertEqual(atlas.pixelColor(0, 5), QColor("yellow"))   # left
            self.assertEqual(atlas.pixelColor(11, 5), QColor("magenta"))  # right
            # Top at (1, 0, 10, 1), bottom at (11, 0, 10, 1)
            self.assertEqual(atlas.pixelColor(5, 0), QColor("cyan"))     # top
            self.assertEqual(atlas.pixelColor(15, 0), QColor("white"))   # bottom

    def test_static_cape_with_base64_data_urls(self) -> None:
        import base64, io
        # Create a red PNG in memory and encode as base64 data URL
        buf = io.BytesIO()
        Image.new("RGB", (10, 16), "red").save(buf, "PNG")
        data_url = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")

        buf_top = io.BytesIO()
        Image.new("RGB", (10, 1), "blue").save(buf_top, "PNG")
        data_url_top = "data:image/png;base64," + base64.b64encode(buf_top.getvalue()).decode("ascii")

        faces = {
            "back": {"source": data_url, "crop": [0, 0, 1, 1]},
            "top": {"source": data_url_top, "crop": [0, 0, 1, 1]},
        }
        atlas = _face_atlas(faces, 64)
        self.assertEqual(atlas.pixelColor(5, 5), QColor("red"))
        self.assertEqual(atlas.pixelColor(5, 0), QColor("blue"))

    def test_painting_single_side_does_not_bleed_to_other_faces(self) -> None:
        import base64, io
        # Create a 32x320 image where ONLY the top half (y=0..160) is painted red
        img = Image.new("RGBA", (32, 320), (0, 0, 0, 0))
        for y in range(160):
            for x in range(32):
                img.putpixel((x, y), (255, 0, 0, 255))
        buf = io.BytesIO()
        img.save(buf, "PNG")
        data_url = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")

        faces = {
            "right": {"source": data_url, "crop": [0, 0, 1, 1]},
        }
        atlas = _face_atlas(faces, 64)
        # right is at x=11: top half (y=3) should be red, bottom half (y=12) should be transparent
        self.assertEqual(atlas.pixelColor(11, 3), QColor("red"))
        self.assertEqual(atlas.pixelColor(11, 12).alpha(), 0)
        # back (5, 5) and left (0, 5) MUST NOT BE PAINTED (alpha == 0)
        self.assertEqual(atlas.pixelColor(5, 5).alpha(), 0)
        self.assertEqual(atlas.pixelColor(0, 5).alpha(), 0)

    def test_back_does_not_bleed_to_other_faces(self) -> None:
        import base64, io
        buf = io.BytesIO()
        Image.new("RGB", (10, 16), "blue").save(buf, "PNG")
        data_url = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")

        faces = {
            "back": {"source": data_url, "crop": [0, 0, 1, 1]},
        }
        atlas = _face_atlas(faces, 64)
        # Back face at (5, 5) MUST BE BLUE
        self.assertEqual(atlas.pixelColor(5, 5), QColor("blue"))
        # Front (15, 5), left (0, 5), right (11, 5), top (5, 0), bottom (15, 0) MUST BE TRANSPARENT (alpha == 0)
        self.assertEqual(atlas.pixelColor(15, 5).alpha(), 0)
        self.assertEqual(atlas.pixelColor(0, 5).alpha(), 0)
        self.assertEqual(atlas.pixelColor(11, 5).alpha(), 0)
        self.assertEqual(atlas.pixelColor(5, 0).alpha(), 0)
        self.assertEqual(atlas.pixelColor(15, 0).alpha(), 0)


if __name__ == "__main__":
    unittest.main()

