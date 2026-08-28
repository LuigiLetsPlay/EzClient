from __future__ import annotations

import io
import struct
import tempfile
import unittest
import zlib
from pathlib import Path
from PIL import Image

import server
from backend.services import cape_media


PLAYER_UUID = "12345678-1234-5678-9234-567812345678"


def cape_png(width: int = 64, height: int = 32) -> bytes:
    signature = b"\x89PNG\r\n\x1a\n"

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    pixels = b"".join(b"\x00" + b"\x22\xC9\x6E\xFF" * width for _ in range(height))
    return signature + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(pixels)) + chunk(b"IEND", b"")


class CapeServerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.orig_root = server.ROOT
        self.orig_img = server.IMAGE_DIR
        self.orig_db = server.DATABASE
        self.orig_rep = server.REPORT_DATABASE
        self.orig_tok = server.TOKENS_DATABASE

        server.ROOT = Path(self.temp.name)
        server.IMAGE_DIR = server.ROOT / "images"
        server.DATABASE = server.ROOT / "capes.json"
        server.REPORT_DATABASE = server.ROOT / "reports.json"
        server.TOKENS_DATABASE = server.ROOT / "tokens.json"

    def tearDown(self) -> None:
        server.ROOT = self.orig_root
        server.IMAGE_DIR = self.orig_img
        server.DATABASE = self.orig_db
        server.REPORT_DATABASE = self.orig_rep
        server.TOKENS_DATABASE = self.orig_tok
        self.temp.cleanup()

    def test_safe_cape_png_validation(self) -> None:
        valid_png = cape_png(64, 32)
        self.assertTrue(server.is_safe_cape_png(valid_png))
        self.assertFalse(server.is_safe_cape_png(b"not-a-png"))
        self.assertFalse(server.is_safe_cape_png(cape_png(100, 100)))

    def test_parse_multipart_with_anim_gif(self) -> None:
        boundary = "----TestBoundary"
        content_type = f"multipart/form-data; boundary={boundary}"
        body = (
            f"--{boundary}\r\n"
            'Content-Disposition: form-data; name="title"\r\n\r\n'
            "Test Cape\r\n"
            f"--{boundary}\r\n"
            'Content-Disposition: form-data; name="cape"; filename="cape.png"\r\n'
            "Content-Type: image/png\r\n\r\n"
            "fake-cape-bytes\r\n"
            f"--{boundary}\r\n"
            'Content-Disposition: form-data; name="anim_gif"; filename="anim.gif"\r\n'
            "Content-Type: image/gif\r\n\r\n"
            "fake-gif-bytes\r\n"
            f"--{boundary}--\r\n"
        ).encode("utf-8")

        fields, cape, anim_gif = server.parse_multipart(content_type, body)
        self.assertEqual("Test Cape", fields.get("title"))
        self.assertEqual(b"fake-cape-bytes", cape)
        self.assertEqual(b"fake-gif-bytes", anim_gif)

    def test_gif_conversion_generates_preview_gif(self) -> None:
        gif_io = io.BytesIO()
        frames = [Image.new("RGBA", (64, 64), color) for color in ("red", "green", "blue")]
        frames[0].save(gif_io, format="GIF", save_all=True, append_images=frames[1:], duration=100, loop=0)
        gif_bytes = gif_io.getvalue()

        src_file = Path(self.temp.name) / "test.gif"
        src_file.write_bytes(gif_bytes)
        target_dir = Path(self.temp.name) / "out_anim"

        manifest = cape_media.generate_frame_sheet(
            src_file,
            target_dir,
            cape_media.AnimationOptions(start=0.0, end=1.0, fps=10, crop_box=(0.1, 0.1, 0.8, 0.8))
        )
        self.assertTrue((target_dir / "framesheet.png").is_file())
        self.assertTrue((target_dir / "preview.gif").is_file())
        self.assertTrue((target_dir / "animation.json").is_file())
        self.assertGreater(manifest.frame_count, 0)

    def test_inactive_latest_cape_disables_server_override(self) -> None:
        server.save_capes([
            {"id": "old", "owner_uuid": PLAYER_UUID, "created_at": "2026-01-01T00:00:00Z", "active": True},
            {"id": "new", "owner_uuid": PLAYER_UUID, "created_at": "2026-02-01T00:00:00Z", "active": False},
        ])
        self.assertEqual([], server.active_capes({PLAYER_UUID}))

    def test_active_latest_cape_is_advertised(self) -> None:
        server.save_capes([
            {"id": "old", "owner_uuid": PLAYER_UUID, "created_at": "2026-01-01T00:00:00Z", "active": False},
            {"id": "new", "owner_uuid": PLAYER_UUID, "created_at": "2026-02-01T00:00:00Z", "active": True},
        ])
        self.assertEqual("new", server.active_capes({PLAYER_UUID})[0]["id"])


if __name__ == "__main__":
    unittest.main()
