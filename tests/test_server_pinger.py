import unittest
import io
import json
import socket
from unittest.mock import patch, MagicMock
from backend.services.server_pinger import (
    encode_varint,
    read_varint,
    resolve_minecraft_srv,
    ping_minecraft_server,
    get_cached_server_status,
    set_cached_server_status,
    clear_server_status_cache
)


class TestServerPinger(unittest.TestCase):
    def setUp(self):
        clear_server_status_cache()

    def tearDown(self):
        clear_server_status_cache()

    def test_encode_and_read_varint(self):
        for val in [0, 1, 127, 128, 255, 2097151, 767]:
            encoded = encode_varint(val)
            mock_sock = MagicMock()
            mock_sock.recv.side_effect = [bytes([b]) for b in encoded]
            decoded = read_varint(mock_sock)
            self.assertEqual(val, decoded)

    def test_resolve_srv_explicit_port(self):
        host, port = resolve_minecraft_srv("play.myserver.net:25570")
        self.assertEqual(host, "play.myserver.net")
        self.assertEqual(port, 25570)

    def test_resolve_srv_ip_address(self):
        host, port = resolve_minecraft_srv("192.168.1.100")
        self.assertEqual(host, "192.168.1.100")
        self.assertEqual(port, 25565)

    def test_cache_operations(self):
        self.assertIsNone(get_cached_server_status("myserver.net"))
        dummy_data = {"online": True, "players_online": 42, "players_max": 100}
        set_cached_server_status("myserver.net", dummy_data)
        cached = get_cached_server_status("myserver.net")
        self.assertIsNotNone(cached)
        self.assertEqual(cached["players_online"], 42)

        # Case-insensitive cache lookup
        cached_caps = get_cached_server_status("MYSERVER.NET")
        self.assertIsNotNone(cached_caps)
        self.assertEqual(cached_caps["players_online"], 42)

        clear_server_status_cache("myserver.net")
        self.assertIsNone(get_cached_server_status("myserver.net"))

    def test_ping_empty_address(self):
        res = ping_minecraft_server("")
        self.assertFalse(res["online"])
        self.assertEqual(res["players_online"], 0)

    @patch("socket.create_connection")
    def test_ping_mocked_server(self, mock_create):
        fake_response = {
            "version": {"name": "1.21", "protocol": 767},
            "players": {
                "max": 50,
                "online": 5,
                "sample": [{"name": "§aPlayerOne", "id": "uuid-1"}, {"name": "PlayerTwo", "id": "uuid-2"}]
            },
            "description": "§6Welcome to EzServer",
            "favicon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg=="
        }
        json_bytes = json.dumps(fake_response).encode("utf-8")

        mock_socket = MagicMock()
        # Mocking incoming packet: packet_len, packet_id (0), string_len, json_data
        encoded_json_len = encode_varint(len(json_bytes))
        resp_data = b"\x00" + encoded_json_len + json_bytes
        encoded_resp_len = encode_varint(len(resp_data))

        # Stream of bytes for read_varint calls and recv calls
        varint_bytes = list(encoded_resp_len) + [0] + list(encoded_json_len)
        varint_idx = [0]

        def fake_recv(n):
            if varint_idx[0] < len(varint_bytes):
                b = bytes([varint_bytes[varint_idx[0]]])
                varint_idx[0] += 1
                return b
            # If past header, return json bytes chunks
            return json_bytes

        mock_socket.recv.side_effect = fake_recv
        mock_create.return_value = mock_socket

        res = ping_minecraft_server("test.server.net:25565", timeout=1.0)
        self.assertTrue(res["online"])
        self.assertEqual(res["players_online"], 5)
        self.assertEqual(res["players_max"], 50)
        self.assertEqual(res["player_sample"], ["PlayerOne", "PlayerTwo"])
        self.assertEqual(res["motd"], "Welcome to EzServer")
        self.assertTrue(res["favicon"].startswith("data:image/png;base64,"))

    def test_extract_chat_component_text(self):
        from backend.services.server_pinger import extract_chat_component_text
        # Simple string
        self.assertEqual(extract_chat_component_text("Hello World"), "Hello World")
        # Dict with extra
        comp = {
            "text": "Welcome to ",
            "extra": [
                {"text": "EzNetwork", "color": "gold"},
                {"text": " [1.21.x]", "color": "gray"}
            ]
        }
        self.assertEqual(extract_chat_component_text(comp), "Welcome to EzNetwork [1.21.x]")
        # Nested list
        nested = [
            {"text": "Line 1\n"},
            {"extra": [{"text": "Line 2"}]}
        ]
        self.assertEqual(extract_chat_component_text(nested), "Line 1\nLine 2")

    @patch("socket.create_connection")
    def test_ping_complex_motd_and_sample(self, mock_create):
        fake_response = {
            "players": {
                "max": "100",
                "online": "12",
                "sample": [
                    {"name": {"text": "§bVIP_", "extra": [{"text": "Steve"}]}, "id": "1"}
                ]
            },
            "description": {
                "text": "§aEzServer ",
                "extra": [
                    {"text": "§eSurvival ", "bold": True},
                    {"text": "§7| §cPvP"}
                ]
            }
        }
        json_bytes = json.dumps(fake_response).encode("utf-8")
        mock_socket = MagicMock()
        encoded_json_len = encode_varint(len(json_bytes))
        resp_data = b"\x00" + encoded_json_len + json_bytes
        encoded_resp_len = encode_varint(len(resp_data))
        varint_bytes = list(encoded_resp_len) + [0] + list(encoded_json_len)
        varint_idx = [0]

        def fake_recv(n):
            if varint_idx[0] < len(varint_bytes):
                b = bytes([varint_bytes[varint_idx[0]]])
                varint_idx[0] += 1
                return b
            return json_bytes

        mock_socket.recv.side_effect = fake_recv
        mock_create.return_value = mock_socket

        res = ping_minecraft_server("play.hypixel.net", timeout=1.0)
        self.assertTrue(res["online"])
        self.assertEqual(res["players_online"], 12)
        self.assertEqual(res["players_max"], 100)
        self.assertEqual(res["motd"], "EzServer Survival | PvP")
        self.assertEqual(res["player_sample"], ["VIP_Steve"])


if __name__ == "__main__":
    unittest.main()

