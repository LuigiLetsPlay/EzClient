from __future__ import annotations

import io
import tempfile
import unittest
import uuid
import zlib
import struct
from types import SimpleNamespace
from unittest.mock import patch

from server import create_app


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
        self.app = create_app({
            "TESTING": True,
            "CAPE_DATA_DIR": self.temp.name,
            "TOKEN_VERIFIER": lambda token: {"id": PLAYER_UUID.replace("-", ""), "name": "TestPlayer"}
            if token == "valid-session" else (_ for _ in ()).throw(PermissionError("Ungültige Session.")),
        })
        self.client = self.app.test_client()

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_authenticated_png_upload_and_legacy_lookup(self) -> None:
        response = self.client.post(
            "/upload_cape",
            data={
                "owner": "TestPlayer",
                "owner_uuid": PLAYER_UUID.replace("-", ""),
                "title": "Release Cape",
                "cape": (io.BytesIO(cape_png()), "cape.png"),
            },
            headers={"Authorization": "Bearer valid-session"},
            content_type="multipart/form-data",
        )
        self.assertEqual(200, response.status_code, response.get_data(as_text=True))
        payload = response.get_json()
        self.assertEqual(PLAYER_UUID, payload["owner_uuid"])
        self.assertTrue(payload["token"])
        legacy = self.client.get(f"/get_cape/{PLAYER_UUID}")
        modern = self.client.get(f"/api/capes/{payload['id']}/image")
        try:
            self.assertEqual(200, legacy.status_code)
            self.assertEqual(200, modern.status_code)
        finally:
            legacy.close()
            modern.close()

    def test_rejects_bad_player_data_and_missing_auth(self) -> None:
        bad_uuid = self.client.post(
            "/upload_cape",
            data={"owner": "TestPlayer", "owner_uuid": "not-a-uuid", "title": "Bad Cape", "cape": (io.BytesIO(cape_png()), "cape.png")},
            content_type="multipart/form-data",
        )
        self.assertEqual(400, bad_uuid.status_code)
        no_auth = self.client.post(
            "/api/capes",
            data={"owner": "TestPlayer", "owner_uuid": PLAYER_UUID, "title": "No Auth", "cape": (io.BytesIO(cape_png()), "cape.png")},
            content_type="multipart/form-data",
        )
        self.assertEqual(401, no_auth.status_code)

    def test_ownership_token_can_replace_cape(self) -> None:
        first = self.client.post(
            "/api/capes",
            data={"owner": "TestPlayer", "owner_uuid": PLAYER_UUID, "title": "First Cape", "cape": (io.BytesIO(cape_png()), "cape.png")},
            headers={"Authorization": "Bearer valid-session"},
            content_type="multipart/form-data",
        ).get_json()
        second = self.client.post(
            "/api/capes",
            data={"owner": "TestPlayer", "owner_uuid": PLAYER_UUID, "title": "Second Cape", "token": first["token"], "cape": (io.BytesIO(cape_png()), "cape.png")},
            headers={"X-EzClient-Cape-Token": first["token"]},
            content_type="multipart/form-data",
        )
        self.assertEqual(200, second.status_code, second.get_data(as_text=True))

    def test_gif_upload_generates_bounded_animation(self) -> None:
        from PIL import Image

        data = io.BytesIO()
        frames = [Image.new("RGBA", (20, 32), color) for color in ("red", "green")]
        frames[0].save(data, format="GIF", save_all=True, append_images=frames[1:], duration=80, loop=0)
        data.seek(0)
        response = self.client.post(
            "/api/capes",
            data={"owner": "TestPlayer", "owner_uuid": PLAYER_UUID, "title": "Animated Cape", "cape": (data, "cape.gif")},
            headers={"Authorization": "Bearer valid-session"},
            content_type="multipart/form-data",
        )
        self.assertEqual(200, response.status_code, response.get_data(as_text=True))
        self.assertTrue(response.get_json()["animated"])

    def test_mp4_upload_uses_bounded_media_pipeline(self) -> None:
        def fake_conversion(_source, output_dir, _options):
            output_dir.mkdir()
            Image.new("RGBA", (256, 128), "blue").save(output_dir / "framesheet.png")
            return SimpleNamespace(frame_count=1, fps=12, duration=1 / 12)

        from PIL import Image
        with patch("server.generate_frame_sheet", side_effect=fake_conversion):
            response = self.client.post(
                "/api/capes",
                data={"owner": "TestPlayer", "owner_uuid": PLAYER_UUID, "title": "Video Cape", "cape": (io.BytesIO(b"mock-mp4"), "cape.mp4")},
                headers={"Authorization": "Bearer valid-session"},
                content_type="multipart/form-data",
            )
        self.assertEqual(200, response.status_code, response.get_data(as_text=True))
        self.assertTrue(response.get_json()["animated"])


if __name__ == "__main__":
    unittest.main()
