from __future__ import annotations

import io
import json
import os
import struct
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
import zlib
from http.server import ThreadingHTTPServer
from pathlib import Path
from PIL import Image

import server
from backend.services import cape_community, cape_media


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


class CapeDeleteServerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.orig_root = server.ROOT
        self.orig_img = server.IMAGE_DIR
        self.orig_db = server.DATABASE
        self.orig_rep = server.REPORT_DATABASE
        self.orig_tok = server.TOKENS_DATABASE

        server.ROOT = Path(self.temp.name)
        server.IMAGE_DIR = server.ROOT / "images"
        server.IMAGE_DIR.mkdir(parents=True, exist_ok=True)
        server.DATABASE = server.ROOT / "capes.json"
        server.REPORT_DATABASE = server.ROOT / "reports.json"
        server.TOKENS_DATABASE = server.ROOT / "tokens.json"

        self.httpd = ThreadingHTTPServer(("127.0.0.1", 0), server.CapeHandler)
        self.port = self.httpd.server_port
        self.thread = threading.Thread(target=self.httpd.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self) -> None:
        self.httpd.shutdown()
        self.httpd.server_close()
        server.ROOT = self.orig_root
        server.IMAGE_DIR = self.orig_img
        server.DATABASE = self.orig_db
        server.REPORT_DATABASE = self.orig_rep
        server.TOKENS_DATABASE = self.orig_tok
        self.temp.cleanup()

    def test_delete_cape_success(self) -> None:
        cape_id = "11111111-2222-3333-4444-555555555555"
        token = "secret-token-12345678901234567890"
        clean_uuid = PLAYER_UUID.replace("-", "").lower()
        server.save_tokens({clean_uuid: token})
        server.save_capes([{
            "id": cape_id,
            "title": "My Cape",
            "owner": "Player1",
            "owner_uuid": PLAYER_UUID,
            "active": True
        }])
        png_path = server.IMAGE_DIR / f"{cape_id}.png"
        gif_path = server.IMAGE_DIR / f"{cape_id}.gif"
        png_path.write_bytes(b"png-data")
        gif_path.write_bytes(b"gif-data")

        req_data = json.dumps({"owner_uuid": PLAYER_UUID, "token": token}).encode("utf-8")
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.port}/api/capes/{cape_id}/delete",
            data=req_data,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req) as resp:
            self.assertEqual(200, resp.status)
            body = json.loads(resp.read().decode())
            self.assertTrue(body.get("ok"))

        self.assertEqual([], server.load_capes())
        self.assertFalse(png_path.exists())
        self.assertFalse(gif_path.exists())

    def test_delete_cape_forbidden_wrong_user(self) -> None:
        cape_id = "11111111-2222-3333-4444-555555555555"
        token_victim = "victim-token-12345678901234567890"
        token_attacker = "attacker-token-12345678901234567890"
        attacker_uuid = "99999999-9999-9999-9999-999999999999"

        server.save_tokens({
            PLAYER_UUID.replace("-", "").lower(): token_victim,
            attacker_uuid.replace("-", "").lower(): token_attacker,
        })
        server.save_capes([{
            "id": cape_id,
            "title": "Victim Cape",
            "owner": "Victim",
            "owner_uuid": PLAYER_UUID,
            "active": True
        }])
        png_path = server.IMAGE_DIR / f"{cape_id}.png"
        png_path.write_bytes(b"png-data")

        # Attacker tries to delete Victim's cape
        req_data = json.dumps({"owner_uuid": attacker_uuid, "token": token_attacker}).encode("utf-8")
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.port}/api/capes/{cape_id}/delete",
            data=req_data,
            headers={"Content-Type": "application/json"}
        )
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            urllib.request.urlopen(req)
        self.assertEqual(403, ctx.exception.code)

        # Victim's cape and image must still be untouched!
        self.assertEqual(1, len(server.load_capes()))
        self.assertTrue(png_path.exists())

    def test_delete_cape_forbidden_invalid_token(self) -> None:
        cape_id = "11111111-2222-3333-4444-555555555555"
        real_token = "real-token-12345678901234567890"
        server.save_tokens({PLAYER_UUID.replace("-", "").lower(): real_token})
        server.save_capes([{
            "id": cape_id,
            "title": "My Cape",
            "owner": "Player1",
            "owner_uuid": PLAYER_UUID,
            "active": True
        }])

        req_data = json.dumps({"owner_uuid": PLAYER_UUID, "token": "wrong-token-abc"}).encode("utf-8")
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.port}/api/capes/{cape_id}/delete",
            data=req_data,
            headers={"Content-Type": "application/json"}
        )
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            urllib.request.urlopen(req)
        self.assertEqual(403, ctx.exception.code)
        self.assertEqual(1, len(server.load_capes()))

    def test_delete_cape_not_found(self) -> None:
        req_data = json.dumps({"owner_uuid": PLAYER_UUID, "token": "any"}).encode("utf-8")
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.port}/api/capes/22222222-3333-4444-5555-666666666666/delete",
            data=req_data,
            headers={"Content-Type": "application/json"}
        )
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            urllib.request.urlopen(req)
        self.assertEqual(404, ctx.exception.code)

    def test_cape_community_service_delete(self) -> None:
        cape_id = "11111111-2222-3333-4444-555555555555"
        token = "token-service-test-12345"
        server.save_tokens({PLAYER_UUID.replace("-", "").lower(): token})
        server.save_capes([{
            "id": cape_id,
            "title": "My Cape",
            "owner": "Player1",
            "owner_uuid": PLAYER_UUID,
            "active": True
        }])

        os.environ["EZCLIENT_CAPE_API"] = f"http://127.0.0.1:{self.port}/api"
        try:
            res = cape_community.delete_cape(
                cape_id=cape_id,
                owner="Player1",
                owner_uuid=PLAYER_UUID,
                token=token
            )
            self.assertTrue(res.get("ok"))
            self.assertEqual([], server.load_capes())
        finally:
            os.environ.pop("EZCLIENT_CAPE_API", None)


if __name__ == "__main__":
    unittest.main()
