from pathlib import Path

import server


def test_presence_exposes_verified_client_type(monkeypatch):
    monkeypatch.setattr(server.time, "monotonic", lambda: 100.0)
    server.PRESENCE.clear()
    server.PRESENCE["12345678-1234-4234-8234-123456789abc"] = (99.0, "Nora", "norisk")

    assert server.online_players() == [{
        "uuid": "12345678-1234-4234-8234-123456789abc",
        "username": "Nora",
        "client": "norisk",
    }]


def test_presence_removes_expired_entries(monkeypatch):
    monkeypatch.setattr(server.time, "monotonic", lambda: 500.0)
    server.PRESENCE.clear()
    server.PRESENCE["12345678-1234-4234-8234-123456789abc"] = (1.0, "Old", "lunar")

    assert server.online_players() == []
    assert server.PRESENCE == {}


def test_launcher_has_no_client_detection_badge():
    root = Path(__file__).resolve().parents[1]
    controller = (root / "backend/controllers/account_controller.py").read_text(encoding="utf-8")
    top_bar = (root / "ui/TopBar.qml").read_text(encoding="utf-8")

    assert "_detect_client_badge" not in controller
    assert "clientBadge" not in controller
    assert "clientBadge" not in top_bar
    assert "showClientBadge" not in top_bar


def test_ingame_badges_cover_supported_clients():
    root = Path(__file__).resolve().parents[1]
    presence = (root / "client_mod/src/main/java/app/ezclient/cosmetics/CommunityPresence.java").read_text()
    font = (root / "client_mod/src/main/resources/assets/ezclient/font/default.json").read_text()

    for client, glyph in {
        "NORISK": "E001",
        "LABYMOD": "E002",
        "LUNAR": "E003",
        "BADLION": "E004",
    }.items():
        assert client in presence
        assert f"\\u{glyph}" in font
