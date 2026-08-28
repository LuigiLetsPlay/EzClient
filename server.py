#!/usr/bin/env python3
"""Self-hosted EzClient community service (no pip packages needed).
Includes strict Mojang Online UUID verification, Account Ownership Tokens,
and a modern Web Admin Dashboard with authentic 10:16 vertical cape previews,
interactive zoom lightbox modal, online player tracking, and direct cape deletion.

Run:  python3 server.py
Public API:  http://YOUR_SERVER_IP:18765/api/capes
Admin Panel: http://YOUR_SERVER_IP:18766/
"""
from __future__ import annotations

import base64
import hmac
import html
import json
import os
import re
import secrets
import struct
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zlib
from datetime import UTC, datetime
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse


HOST = "0.0.0.0"
ADMIN_HOST = "0.0.0.0"
PORT = 18765
ADMIN_PORT = 18766
MAX_CAPE_BYTES = 2 * 1024 * 1024
MAX_REQUEST_BYTES = 2 * 1024 * 1024
PRESENCE_TTL_SECONDS = 90
ROOT = Path(__file__).resolve().parent / "cape_community_data"
IMAGE_DIR = ROOT / "images"
DATABASE = ROOT / "capes.json"
REPORT_DATABASE = ROOT / "reports.json"
TOKENS_DATABASE = ROOT / "tokens.json"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"

RATE_LIMITS: dict[str, list[float]] = {}
PRESENCE: dict[str, tuple[float, str]] = {}  # player_uuid -> (timestamp, username)
MOJANG_VERIFIED_CACHE: dict[str, tuple[str, float]] = {}
STATE_LOCK = threading.Lock()
REPORTS_LOCK = threading.Lock()
TOKENS_LOCK = threading.Lock()


def allow_request(ip: str, limit: int, window_seconds: int) -> bool:
    """Small in-memory sliding-window limiter; bounded and dependency-free."""
    now = time.monotonic()
    with STATE_LOCK:
        recent = [stamp for stamp in RATE_LIMITS.get(ip, []) if stamp > now - window_seconds]
        if len(recent) >= limit:
            RATE_LIMITS[ip] = recent
            return False
        recent.append(now)
        RATE_LIMITS[ip] = recent
        if len(RATE_LIMITS) > 10_000:
            for key in list(RATE_LIMITS):
                if not RATE_LIMITS[key] or RATE_LIMITS[key][-1] < now - 3600:
                    RATE_LIMITS.pop(key, None)
        return True


def online_players(requested: set[str] | None = None) -> list[dict]:
    now = time.monotonic()
    with STATE_LOCK:
        for player_id, data in list(PRESENCE.items()):
            seen = data[0] if isinstance(data, tuple) else data
            if seen < now - PRESENCE_TTL_SECONDS:
                PRESENCE.pop(player_id, None)

        result = []
        for player_id, data in PRESENCE.items():
            if requested is not None and player_id not in requested:
                continue
            name = data[1] if isinstance(data, tuple) else "Spieler"
            result.append({"uuid": player_id, "username": name})
    return result


def is_safe_cape_png(raw: bytes) -> bool:
    """Strictly accept only simple, fully decodable RGB/RGBA Minecraft cape PNGs."""
    if not raw.startswith(PNG_SIGNATURE) or len(raw) > MAX_CAPE_BYTES:
        return False
    position, width, height, color_type = len(PNG_SIGNATURE), 0, 0, -1
    idat = bytearray()
    seen_ihdr = seen_iend = False
    try:
        while position < len(raw):
            if position + 12 > len(raw):
                return False
            length = struct.unpack(">I", raw[position:position + 4])[0]
            if length > MAX_CAPE_BYTES or position + 12 + length > len(raw):
                return False
            kind = raw[position + 4:position + 8]
            data = raw[position + 8:position + 8 + length]
            crc = struct.unpack(">I", raw[position + 8 + length:position + 12 + length])[0]
            if zlib.crc32(kind + data) & 0xffffffff != crc:
                return False
            position += 12 + length
            if kind == b"IHDR" and not seen_ihdr and length == 13:
                width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", data)
                if (width, height) not in {(64, 32), (128, 64), (256, 128), (512, 256), (1024, 512)} or bit_depth != 8 or color_type not in (2, 6) or compression or filtering or interlace:
                    return False
                seen_ihdr = True
            elif kind == b"IDAT" and seen_ihdr and not seen_iend:
                idat.extend(data)
            elif kind == b"IEND" and length == 0:
                seen_iend = True
                break
            else:
                return False
        if not seen_ihdr or not seen_iend or position != len(raw) or not idat:
            return False
        channels = 4 if color_type == 6 else 3
        expected = height * (1 + width * channels)
        decoded = zlib.decompress(bytes(idat))
        return len(decoded) == expected and all(decoded[row * (1 + width * channels)] <= 4 for row in range(height))
    except (ValueError, struct.error, zlib.error):
        return False


def load_capes() -> list[dict]:
    try:
        data = json.loads(DATABASE.read_text(encoding="utf-8"))
        return data if isinstance(data, list) else []
    except (FileNotFoundError, json.JSONDecodeError):
        return []


def save_capes(capes: list[dict]) -> None:
    ROOT.mkdir(parents=True, exist_ok=True)
    temporary = DATABASE.with_suffix(".tmp")
    temporary.write_text(json.dumps(capes, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(DATABASE)


def load_reports() -> list[dict]:
    try:
        data = json.loads(REPORT_DATABASE.read_text(encoding="utf-8"))
        return data if isinstance(data, list) else []
    except (FileNotFoundError, json.JSONDecodeError):
        return []


def save_reports(reports: list[dict]) -> None:
    ROOT.mkdir(parents=True, exist_ok=True)
    temporary = REPORT_DATABASE.with_suffix(".tmp")
    temporary.write_text(json.dumps(reports, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(REPORT_DATABASE)


def load_tokens() -> dict[str, str]:
    try:
        data = json.loads(TOKENS_DATABASE.read_text(encoding="utf-8"))
        return data if isinstance(data, dict) else {}
    except (FileNotFoundError, json.JSONDecodeError):
        return {}


def save_tokens(tokens: dict[str, str]) -> None:
    ROOT.mkdir(parents=True, exist_ok=True)
    temporary = TOKENS_DATABASE.with_suffix(".tmp")
    temporary.write_text(json.dumps(tokens, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(TOKENS_DATABASE)


def clean_text(value: str, maximum: int) -> str:
    value = re.sub(r"[\x00-\x1f<>]", "", value or "").strip()
    return value[:maximum]


def normalize_player_uuid(value: str) -> str:
    """Accept Mojang's 32-char id and canonical UUIDs, return canonical form."""
    try:
        parsed = uuid.UUID(str(value).strip())
    except (AttributeError, TypeError, ValueError) as exc:
        raise ValueError("Ungültige Spielerdaten.") from exc
    if parsed.int == 0 or parsed.version == 3:
        raise ValueError("Ungültige Spielerdaten.")
    return str(parsed)


def validate_cape_title(value: str) -> str:
    title = " ".join(str(value or "").split())
    if not 3 <= len(title) <= 48:
        raise ValueError("Der Cape-Name muss zwischen 3 und 48 Zeichen lang sein.")
    if any(ord(char) < 32 or char in "<>" for char in title):
        raise ValueError("Der Cape-Name enthält ungültige Zeichen.")
    return title


def bearer_token(headers) -> str:
    value = str(headers.get("Authorization", "")).strip()
    scheme, separator, token = value.partition(" ")
    if not separator or scheme.lower() != "bearer" or not token or any(char in token for char in "\r\n"):
        return ""
    return token.strip()


def verify_minecraft_access_token(access_token: str, username: str, player_uuid: str) -> bool:
    """Resolve the token at Minecraft Services and bind it to this upload."""
    if not access_token:
        return False
    request = urllib.request.Request(
        "https://api.minecraftservices.com/minecraft/profile",
        headers={
            "Authorization": f"Bearer {access_token}",
            "Accept": "application/json",
            "User-Agent": "EzClient-Community-Server/1.7",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=8) as response:
            profile = json.loads(response.read().decode("utf-8"))
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        print(f"[Auth] Minecraft access-token verification failed: {type(exc).__name__}")
        return False

    if not isinstance(profile, dict):
        return False

    token_uuid = str(profile.get("id", "")).replace("-", "").lower()
    expected_uuid = player_uuid.replace("-", "").lower()
    token_name = str(profile.get("name", ""))
    return hmac.compare_digest(token_uuid, expected_uuid) and token_name.casefold() == username.casefold()


def verify_mojang_identity(username: str, player_uuid: str) -> bool:
    clean_uuid = player_uuid.replace("-", "").lower()
    if clean_uuid == "00000000000000000000000000000000" or len(clean_uuid) != 32:
        return False

    try:
        parsed_uuid = uuid.UUID(player_uuid)
        if parsed_uuid.version == 3:
            return False
    except Exception:
        return False

    now = time.monotonic()
    with STATE_LOCK:
        if clean_uuid in MOJANG_VERIFIED_CACHE:
            cached_name, cached_time = MOJANG_VERIFIED_CACHE[clean_uuid]
            if now - cached_time < 86400 and cached_name.lower() == username.lower():
                return True

    try:
        url = f"https://sessionserver.mojang.com/session/minecraft/profile/{clean_uuid}"
        req = urllib.request.Request(url, headers={"User-Agent": "EzClient-Community-Server/1.6"})
        with urllib.request.urlopen(req, timeout=6) as response:
            if response.status == 200:
                data = json.loads(response.read().decode("utf-8"))
                real_name = data.get("name", "")
                if real_name.lower() == username.lower():
                    with STATE_LOCK:
                        MOJANG_VERIFIED_CACHE[clean_uuid] = (real_name, now)
                    return True
    except Exception as exc:
        print(f"[Auth] Mojang session verification warning for {username} ({player_uuid}): {exc}")

    try:
        url = f"https://api.mojang.com/users/profiles/minecraft/{urllib.parse.quote(username)}"
        req = urllib.request.Request(url, headers={"User-Agent": "EzClient-Community-Server/1.6"})
        with urllib.request.urlopen(req, timeout=6) as response:
            if response.status == 200:
                data = json.loads(response.read().decode("utf-8"))
                real_uuid = data.get("id", "").lower()
                if real_uuid == clean_uuid:
                    with STATE_LOCK:
                        MOJANG_VERIFIED_CACHE[clean_uuid] = (username, now)
                    return True
    except Exception as exc:
        print(f"[Auth] Mojang profile verification warning for {username}: {exc}")

    return False


def verify_account_ownership(owner_uuid: str, provided_token: str) -> tuple[bool, str]:
    with TOKENS_LOCK:
        tokens = load_tokens()
        clean_uuid = owner_uuid.replace("-", "").lower()
        if clean_uuid not in tokens:
            new_token = secrets.token_hex(24)
            tokens[clean_uuid] = new_token
            save_tokens(tokens)
            return True, new_token

        stored_token = tokens[clean_uuid]
        if provided_token and hmac.compare_digest(provided_token, stored_token):
            return True, stored_token

        return False, ""


def active_capes(player_ids: set[str] | None = None) -> list[dict]:
    newest: dict[str, dict] = {}
    for cape in load_capes():
        owner_uuid = str(cape.get("owner_uuid", "")).lower()
        if not re.fullmatch(r"[a-f0-9-]{36}", owner_uuid):
            continue
        if player_ids is not None and owner_uuid not in player_ids:
            continue
        if owner_uuid not in newest or str(cape.get("created_at", "")) > str(newest[owner_uuid].get("created_at", "")):
            newest[owner_uuid] = cape
    return [cape for cape in newest.values() if cape.get("active", True) is not False]


def parse_multipart(content_type: str, body: bytes) -> tuple[dict[str, str], bytes, bytes]:
    match = re.search(r"boundary=([^;]+)", content_type)
    if not match:
        raise ValueError("multipart boundary fehlt")
    boundary = b"--" + match.group(1).strip('"').encode()
    fields: dict[str, str] = {}
    cape = b""
    anim_gif = b""
    for section in body.split(boundary):
        section = section.strip(b"\r\n")
        if not section or section == b"--":
            continue
        header_blob, separator, value = section.partition(b"\r\n\r\n")
        if not separator:
            continue
        headers = header_blob.decode("utf-8", "replace")
        name = re.search(r'name="([^\"]+)"', headers)
        if not name:
            continue
        value = value.rstrip(b"\r\n")
        if name.group(1) == "cape":
            cape = value
        elif name.group(1) == "anim_gif":
            anim_gif = value
        else:
            fields[name.group(1)] = value.decode("utf-8", "replace")
    return fields, cape, anim_gif


class CapeHandler(BaseHTTPRequestHandler):
    server_version = "EzClientCapeCommunity/1.6.0"

    def log_message(self, fmt: str, *args) -> None:
        print(f"[{self.log_date_time_string()}] {self.address_string()} {fmt % args}")

    def send_json(self, status: int, payload: dict) -> None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self) -> None:
        path = urlparse(self.path).path.rstrip("/")
        if not allow_request(self.client_address[0], 900, 60):
            self.send_json(HTTPStatus.TOO_MANY_REQUESTS, {"error": "Zu viele Anfragen"})
            return
        if path == "/api/presence":
            requested = {value for value in urlparse(self.path).query.removeprefix("players=").split(",")
                         if re.fullmatch(r"[a-f0-9-]{36}", value)}
            self.send_json(HTTPStatus.OK, {
                "players": [p["uuid"] for p in online_players(requested or None)],
                "ttl": PRESENCE_TTL_SECONDS
            })
            return
        if path == "/api/capes/active":
            requested = {value for value in urlparse(self.path).query.removeprefix("players=").split(",")
                         if re.fullmatch(r"[a-f0-9-]{36}", value)}
            host = self.headers.get("Host", f"127.0.0.1:{PORT}")
            capes = []
            for item in active_capes(requested or None):
                copy = dict(item)
                cape_id = item["id"]
                copy["image_url"] = f"http://{host}/api/capes/{cape_id}/image"
                has_gif = (IMAGE_DIR / f"{cape_id}.gif").is_file() or bool(item.get("is_animated"))
                copy["is_animated"] = has_gif
                if has_gif:
                    copy["animation_url"] = f"http://{host}/api/capes/{cape_id}/animation"
                capes.append(copy)
            self.send_json(HTTPStatus.OK, {"capes": capes})
            return
        if path == "/api/capes":
            host = self.headers.get("Host", f"127.0.0.1:{PORT}")
            capes = []
            for item in reversed(load_capes()):
                copy = dict(item)
                cape_id = item["id"]
                copy["image_url"] = f"http://{host}/api/capes/{cape_id}/image"
                has_gif = (IMAGE_DIR / f"{cape_id}.gif").is_file() or bool(item.get("is_animated"))
                copy["is_animated"] = has_gif
                if has_gif:
                    copy["animation_url"] = f"http://{host}/api/capes/{cape_id}/animation"
                capes.append(copy)
            self.send_json(HTTPStatus.OK, {"capes": capes})
            return

        anim_match = re.fullmatch(r"/api/capes/([a-f0-9-]{36})/(?:animation|gif)", path)
        if anim_match:
            gif_file = IMAGE_DIR / f"{anim_match.group(1)}.gif"
            if gif_file.is_file():
                raw = gif_file.read_bytes()
                self.send_response(HTTPStatus.OK)
                self.send_header("Content-Type", "image/gif")
                self.send_header("Content-Length", str(len(raw)))
                self.send_header("Cache-Control", "public, max-age=3600")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                self.wfile.write(raw)
                return

        match = re.fullmatch(r"/api/capes/([a-f0-9-]{36})/image", path)
        if match:
            image = IMAGE_DIR / f"{match.group(1)}.png"
            if not image.is_file():
                self.send_json(HTTPStatus.NOT_FOUND, {"error": "Cape nicht gefunden"})
                return
            raw = image.read_bytes()
            self.send_response(HTTPStatus.OK)
            self.send_header("Content-Type", "image/png")
            self.send_header("Content-Length", str(len(raw)))
            self.send_header("Cache-Control", "public, max-age=3600")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(raw)
            return
        self.send_json(HTTPStatus.NOT_FOUND, {"error": "Route nicht gefunden"})

    def do_POST(self) -> None:
        path = urlparse(self.path).path.rstrip("/")
        if path == "/api/presence":
            self.update_presence()
            return
        if path == "/api/capes/deactivate":
            self.deactivate_cape()
            return
        if path == "/api/reports":
            self.create_report()
            return
        if path != "/api/capes":
            self.send_json(HTTPStatus.NOT_FOUND, {"error": "Route nicht gefunden"})
            return
        if not allow_request(self.client_address[0], 24, 60):
            self.send_json(HTTPStatus.TOO_MANY_REQUESTS, {"error": "Upload-Limit erreicht. Bitte kurz warten."})
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_CAPE_BYTES + 8 * 1024 * 1024 + 16_384:
            self.send_json(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, {"error": "Cape ist zu groß"})
            return
        try:
            fields, cape, anim_gif = parse_multipart(self.headers.get("Content-Type", ""), self.rfile.read(length))
            if not is_safe_cape_png(cape):
                raise ValueError("Nur geprüfte Cape-PNGs (64×32 bis 1024×512) bis 2 MB sind erlaubt")

            owner = clean_text(fields.get("owner", ""), 32)
            owner_uuid = normalize_player_uuid(fields.get("owner_uuid", ""))
            token = clean_text(fields.get("token", "") or self.headers.get("X-EzClient-Cape-Token", ""), 64)
            title = validate_cape_title(fields.get("title", ""))

            if not owner:
                self.send_json(HTTPStatus.BAD_REQUEST, {"error": "Ungültige Spielerdaten."})
                return

            provided_bearer = bearer_token(self.headers)
            identity_valid = (
                verify_minecraft_access_token(provided_bearer, owner, owner_uuid)
                if provided_bearer
                else verify_mojang_identity(owner, owner_uuid)
            )
            if not identity_valid:
                self.send_json(HTTPStatus.FORBIDDEN, {
                    "error": "Die Minecraft-Sitzung ist ungültig oder abgelaufen. Bitte melde dich erneut an."
                })
                return

            allowed, current_token = verify_account_ownership(owner_uuid, token)
            if not allowed:
                self.send_json(HTTPStatus.FORBIDDEN, {
                    "error": "Dieses Spieler-Profil ist geschützt. Das Cape kann nur vom ursprünglichen Besitzer geändert werden."
                })
                return

            cape_id = str(uuid.uuid4())
            IMAGE_DIR.mkdir(parents=True, exist_ok=True)
            (IMAGE_DIR / f"{cape_id}.png").write_bytes(cape)
            is_animated = False
            if anim_gif and len(anim_gif) > 0 and len(anim_gif) <= 8 * 1024 * 1024:
                (IMAGE_DIR / f"{cape_id}.gif").write_bytes(anim_gif)
                is_animated = True

            capes = load_capes()
            for existing in capes:
                if str(existing.get("owner_uuid", "")).lower() == owner_uuid.lower():
                    existing["active"] = False
            capes.append({
                "id": cape_id,
                "title": title,
                "owner": owner,
                "owner_uuid": owner_uuid,
                "created_at": datetime.now(UTC).isoformat(),
                "is_animated": is_animated,
                "active": True,
            })
            save_capes(capes)

            self.send_json(HTTPStatus.CREATED, {
                "id": cape_id,
                "title": title,
                "owner": owner,
                "owner_uuid": owner_uuid,
                "token": current_token,
                "is_animated": is_animated,
            })
        except ValueError as exc:
            self.send_json(HTTPStatus.BAD_REQUEST, {"error": str(exc)})
        except Exception as exc:
            print(f"Upload failed: {exc}")
            self.send_json(HTTPStatus.INTERNAL_SERVER_ERROR, {"error": "Upload fehlgeschlagen"})

    def deactivate_cape(self) -> None:
        if not allow_request(self.client_address[0], 30, 60):
            self.send_json(HTTPStatus.TOO_MANY_REQUESTS, {"error": "Zu viele Anfragen"})
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > 4096:
            self.send_json(HTTPStatus.BAD_REQUEST, {"error": "Ungültige Anfrage"})
            return
        try:
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            owner_uuid = normalize_player_uuid(payload.get("owner_uuid", ""))
            provided = clean_text(payload.get("token", "") or self.headers.get("X-EzClient-Cape-Token", ""), 64)
            stored = str(load_tokens().get(owner_uuid.replace("-", "").lower(), ""))
            if not stored or not provided or not hmac.compare_digest(stored, provided):
                self.send_json(HTTPStatus.FORBIDDEN, {"error": "Cape-Besitz konnte nicht bestätigt werden."})
                return
            capes = load_capes()
            changed = False
            for cape in capes:
                if str(cape.get("owner_uuid", "")).lower() == owner_uuid.lower() and cape.get("active", True):
                    cape["active"] = False
                    changed = True
            if changed:
                save_capes(capes)
            self.send_json(HTTPStatus.OK, {"active": False})
        except (ValueError, json.JSONDecodeError) as exc:
            self.send_json(HTTPStatus.BAD_REQUEST, {"error": str(exc)})

    def update_presence(self) -> None:
        if not allow_request(self.client_address[0], 90, 60):
            self.send_json(HTTPStatus.TOO_MANY_REQUESTS, {"error": "Zu viele Anfragen"})
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > 512:
            self.send_json(HTTPStatus.BAD_REQUEST, {"error": "Ungültige Präsenz"})
            return
        try:
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            player_id = clean_text(str(payload.get("player_uuid", "")), 36).lower()
            username = clean_text(str(payload.get("username", "")), 32) or "Spieler"
            if not re.fullmatch(r"[a-f0-9-]{36}", player_id):
                raise ValueError
            with STATE_LOCK:
                PRESENCE[player_id] = (time.monotonic(), username)
            self.send_json(HTTPStatus.OK, {"ok": True, "expires_in": PRESENCE_TTL_SECONDS})
        except (ValueError, json.JSONDecodeError):
            self.send_json(HTTPStatus.BAD_REQUEST, {"error": "Ungültige Präsenz"})

    def create_report(self) -> None:
        if not allow_request(self.client_address[0], 20, 3600):
            self.send_json(HTTPStatus.TOO_MANY_REQUESTS, {"error": "Report-Limit erreicht"})
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > 4096:
            self.send_json(HTTPStatus.BAD_REQUEST, {"error": "Ungültige Meldung"})
            return
        try:
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            cape_id = clean_text(str(payload.get("cape_id", "")), 36)
            reason = clean_text(str(payload.get("reason", "")), 500)
            reporter = clean_text(str(payload.get("reporter", "")), 32) or "Anonym"
            if not re.fullmatch(r"[a-f0-9-]{36}", cape_id) or not reason:
                raise ValueError("Cape und Grund sind erforderlich")
            with REPORTS_LOCK:
                reports = load_reports()
                reports.append({
                    "id": str(uuid.uuid4()),
                    "cape_id": cape_id,
                    "reason": reason,
                    "reporter": reporter,
                    "created_at": datetime.now(UTC).isoformat(),
                    "status": "open"
                })
                save_reports(reports)
            self.send_json(HTTPStatus.CREATED, {"ok": True})
        except (ValueError, json.JSONDecodeError) as exc:
            self.send_json(HTTPStatus.BAD_REQUEST, {"error": str(exc)})


class AdminHandler(BaseHTTPRequestHandler):
    """Modern password-protected Web Admin Dashboard with vertical cape previews and zoom modal."""

    def authorised(self) -> bool:
        expected = os.environ.get("EZCLIENT_ADMIN_PASSWORD", "")
        auth = self.headers.get("Authorization", "")
        if not expected or not auth.startswith("Basic "):
            return False
        try:
            supplied = base64.b64decode(auth[6:]).decode("utf-8").split(":", 1)[1]
            return hmac.compare_digest(supplied, expected)
        except Exception:
            return False

    def challenge(self) -> None:
        self.send_response(HTTPStatus.UNAUTHORIZED)
        self.send_header("WWW-Authenticate", 'Basic realm="EzClient Admin Dashboard"')
        self.end_headers()

    def do_GET(self) -> None:
        if not self.authorised():
            self.challenge()
            return

        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        query = parse_qs(parsed.query)
        tab = query.get("tab", ["capes"])[0]
        search = query.get("q", [""])[0].strip().lower()

        capes = load_capes()
        reports = load_reports()
        online = online_players()

        capes_by_id = {c["id"]: c for c in capes}
        host_ip = self.headers.get("Host", "").split(":")[0] or "127.0.0.1"

        open_reports_count = sum(1 for r in reports if r.get("status") == "open")

        html_out = f"""<!doctype html>
<html lang="de">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>EzClient Admin Dashboard</title>
<style>
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #0B0E14; color: #E2E8F0; padding: 24px; }}
.container {{ max-width: 1200px; margin: 0 auto; }}
.header {{ display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #1E293B; }}
.logo {{ display: flex; align-items: center; gap: 12px; font-size: 20px; font-weight: 700; color: #43DD8C; }}
.stats {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }}
.stat-card {{ background: #131A26; border: 1px solid #1E293B; border-radius: 10px; padding: 16px; }}
.stat-title {{ font-size: 13px; color: #94A3B8; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 6px; }}
.stat-val {{ font-size: 26px; font-weight: 700; color: #F8FAFC; }}
.tabs {{ display: flex; gap: 8px; margin-bottom: 20px; border-bottom: 1px solid #1E293B; padding-bottom: 8px; }}
.tab {{ padding: 8px 16px; border-radius: 6px; text-decoration: none; font-weight: 600; font-size: 14px; color: #94A3B8; background: transparent; transition: all 0.15s ease; }}
.tab:hover {{ background: #1E293B; color: #F8FAFC; }}
.tab.active {{ background: #22C96E; color: #0B0E14; }}
.badge {{ background: #EF4444; color: #FFF; font-size: 11px; padding: 2px 7px; border-radius: 12px; margin-left: 6px; }}
.search-bar {{ margin-bottom: 16px; display: flex; gap: 8px; }}
.search-input {{ flex: 1; max-width: 360px; padding: 9px 14px; background: #131A26; border: 1px solid #1E293B; border-radius: 6px; color: #FFF; font-size: 14px; }}
.search-btn {{ padding: 9px 16px; background: #334155; border: 0; border-radius: 6px; color: #FFF; font-weight: 600; cursor: pointer; }}

/* Card Grid with Vertical Cape Aspect Ratio (10:16) */
.card-grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }}
.cape-card {{ background: #131A26; border: 1px solid #1E293B; border-radius: 10px; padding: 14px; display: flex; gap: 14px; align-items: center; transition: transform 0.15s ease, border-color 0.15s ease; }}
.cape-card:hover {{ border-color: #334155; transform: translateY(-2px); }}

/* Authentic Minecraft Vertical Portrait Cape Container */
.cape-portrait-box {{
    width: 80px;
    height: 128px;
    position: relative;
    overflow: hidden;
    background: #080B10;
    border-radius: 6px;
    border: 1px solid #1E293B;
    flex-shrink: 0;
    cursor: pointer;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.4);
    transition: transform 0.15s ease, border-color 0.15s ease;
}}
.cape-portrait-box:hover {{
    transform: scale(1.05);
    border-color: #43DD8C;
}}
.cape-portrait-img {{
    position: absolute;
    width: 640%;
    height: 200%;
    left: -10%;
    top: -6.25%;
    image-rendering: pixelated;
    image-rendering: -moz-crisp-edges;
    image-rendering: crisp-edges;
}}

.report-cape-box {{
    width: 45px;
    height: 72px;
    position: relative;
    overflow: hidden;
    background: #080B10;
    border-radius: 4px;
    border: 1px solid #1E293B;
    cursor: pointer;
    transition: border-color 0.15s ease;
}}
.report-cape-box:hover {{
    border-color: #43DD8C;
}}
.report-cape-box .cape-portrait-img {{
    position: absolute;
    width: 640%;
    height: 200%;
    left: -10%;
    top: -6.25%;
    image-rendering: pixelated;
}}

.cape-info {{ flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px; font-size: 13px; }}
.cape-title {{ font-weight: 700; color: #F8FAFC; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }}
.cape-owner {{ color: #43DD8C; }}
.cape-uuid {{ color: #64748B; font-family: monospace; font-size: 10.5px; word-break: break-all; }}
.cape-date {{ color: #64748B; font-size: 11px; }}
.btn-row {{ display: flex; gap: 6px; margin-top: 6px; }}
.btn {{ flex: 1; padding: 6px 10px; border: 0; border-radius: 6px; font-size: 12.5px; font-weight: 600; cursor: pointer; text-align: center; text-decoration: none; display: inline-flex; align-items: center; justify-content: center; }}
.btn-danger {{ background: #EF4444; color: #FFF; }}
.btn-danger:hover {{ background: #DC2626; }}
.btn-success {{ background: #22C96E; color: #0B0E14; }}
.btn-success:hover {{ background: #16A34A; }}
.btn-secondary {{ background: #334155; color: #F8FAFC; }}
.btn-secondary:hover {{ background: #475569; }}

table {{ width: 100%; border-collapse: collapse; background: #131A26; border: 1px solid #1E293B; border-radius: 8px; overflow: hidden; font-size: 14px; }}
th, td {{ padding: 12px 16px; border-bottom: 1px solid #1E293B; text-align: left; vertical-align: middle; }}
th {{ background: #0F172A; color: #94A3B8; font-weight: 600; text-transform: uppercase; font-size: 12px; letter-spacing: 0.5px; }}
.status-open {{ color: #EF4444; font-weight: 700; }}
.status-resolved {{ color: #22C96E; font-weight: 600; }}
.status-removed {{ color: #94A3B8; font-style: italic; }}
.empty-msg {{ padding: 48px; text-align: center; color: #64748B; font-size: 16px; }}

/* Zoom Lightbox Modal */
.modal-overlay {{
    display: none;
    position: fixed;
    top: 0; left: 0; width: 100%; height: 100%;
    background: rgba(0, 0, 0, 0.85);
    backdrop-filter: blur(4px);
    z-index: 1000;
    align-items: center;
    justify-content: center;
    padding: 20px;
}}
.modal-overlay.open {{ display: flex; }}
.modal-card {{
    background: #131A26;
    border: 1px solid #1E293B;
    border-radius: 12px;
    padding: 24px;
    max-width: 500px;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    position: relative;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.6);
}}
.modal-close {{
    position: absolute;
    top: 12px; right: 14px;
    background: #1E293B;
    color: #FFF;
    border: 0;
    border-radius: 6px;
    width: 28px; height: 28px;
    font-size: 16px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
}}
.modal-cape-large {{
    width: 160px;
    height: 256px;
    position: relative;
    overflow: hidden;
    background: #080B10;
    border-radius: 8px;
    border: 2px solid #43DD8C;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
}}
.modal-cape-large .cape-portrait-img {{
    position: absolute;
    width: 640%;
    height: 200%;
    left: -10%;
    top: -6.25%;
    image-rendering: pixelated;
}}
.modal-raw-img {{
    max-width: 240px;
    border-radius: 4px;
    border: 1px solid #1E293B;
    image-rendering: pixelated;
    background: #080B10;
    padding: 4px;
}}
</style>
</head>
<body>
<div class="container">
    <div class="header">
        <div class="logo">⚡ EzClient Cape & Community Admin</div>
        <div style="font-size:13px;color:#94A3B8;">Server Port: {PORT} (API) • {ADMIN_PORT} (Admin)</div>
    </div>

    <div class="stats">
        <div class="stat-card">
            <div class="stat-title">Veröffentlichte Capes</div>
            <div class="stat-val">{len(capes)}</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">Offene Reports</div>
            <div class="stat-val" style="color: {'#EF4444' if open_reports_count > 0 else '#22C96E'};">{open_reports_count}</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">Aktive Spieler (Online)</div>
            <div class="stat-val" style="color:#00D2FF;">{len(online)}</div>
        </div>
    </div>

    <div class="tabs">
        <a href="/?tab=capes" class="tab {'active' if tab == 'capes' else ''}">🎨 Alle Capes ({len(capes)})</a>
        <a href="/?tab=reports" class="tab {'active' if tab == 'reports' else ''}">🚨 Reports {f'<span class="badge">{open_reports_count}</span>' if open_reports_count > 0 else ''}</a>
        <a href="/?tab=presence" class="tab {'active' if tab == 'presence' else ''}">👥 Online Spieler ({len(online)})</a>
    </div>
"""

        if tab == "capes":
            filtered_capes = []
            for c in reversed(capes):
                if search:
                    t_match = search in c.get("title", "").lower()
                    o_match = search in c.get("owner", "").lower()
                    u_match = search in c.get("owner_uuid", "").lower()
                    if not (t_match or o_match or u_match):
                        continue
                filtered_capes.append(c)

            html_out += f"""
    <form class="search-bar" method="get">
        <input type="hidden" name="tab" value="capes">
        <input type="text" name="q" class="search-input" placeholder="Suche nach Titel, Spieler oder UUID..." value="{html.escape(search)}">
        <button type="submit" class="search-btn">Suchen</button>
        {'<a href="/?tab=capes" class="btn btn-secondary" style="max-width:80px;">Reset</a>' if search else ''}
    </form>
"""
            if not filtered_capes:
                html_out += '<div class="empty-msg">Keine Capes gefunden.</div>'
            else:
                html_out += '<div class="card-grid">'
                for c in filtered_capes:
                    cape_id = c["id"]
                    has_gif = (IMAGE_DIR / f"{cape_id}.gif").is_file() or bool(c.get("is_animated"))
                    display_url = f"http://{host_ip}:{PORT}/api/capes/{cape_id}/animation" if has_gif else f"http://{host_ip}:{PORT}/api/capes/{cape_id}/image"
                    raw_url = f"http://{host_ip}:{PORT}/api/capes/{cape_id}/image"
                    created = c.get("created_at", "")[:19].replace("T", " ")
                    title_esc = html.escape(c.get('title', 'Ohne Titel'))
                    owner_esc = html.escape(c.get('owner', 'Unbekannt'))
                    badge_html = '<span style="background:#22C96E;color:#0B0E14;font-size:10.5px;font-weight:700;padding:2px 6px;border-radius:4px;margin-left:6px;">🎬 ANIMIERT</span>' if has_gif else ''
                    portrait_img_html = f'<img src="{display_url}" alt="Cape" style="width:100%;height:100%;object-fit:cover;image-rendering:pixelated;">' if has_gif else f'<img src="{display_url}" alt="Cape" class="cape-portrait-img">'
                    html_out += f"""
        <div class="cape-card">
            <div class="cape-portrait-box" title="Klicken für Großansicht" onclick="openModal('{display_url}', '{raw_url}', '{title_esc}', '{owner_esc}', {str(has_gif).lower()})">
                {portrait_img_html}
            </div>
            <div class="cape-info">
                <div class="cape-title" title="{title_esc}">{title_esc}{badge_html}</div>
                <div class="cape-owner">👤 {owner_esc}</div>
                <div class="cape-uuid">{html.escape(c.get('owner_uuid', ''))}</div>
                <div class="cape-date">📅 {html.escape(created)}</div>
                <div class="btn-row">
                    <button type="button" class="btn btn-secondary" onclick="openModal('{display_url}', '{raw_url}', '{title_esc}', '{owner_esc}', {str(has_gif).lower()})">🔍 Groß</button>
                    <form method="post" action="/capes/{cape_id}/delete" onsubmit="return confirm('Möchtest du dieses Cape wirklich sofort löschen?');" style="flex:1;">
                        <button type="submit" class="btn btn-danger" style="width:100%;">Löschen</button>
                    </form>
                </div>
            </div>
        </div>
"""
                html_out += '</div>'

        elif tab == "reports":
            if not reports:
                html_out += '<div class="empty-msg">Keine Meldungen vorhanden.</div>'
            else:
                html_out += """
    <table>
        <thead>
            <tr>
                <th>Status</th>
                <th>Cape (10:16)</th>
                <th>Titel & Besitzer</th>
                <th>Meldegrund</th>
                <th>Gemeldet von</th>
                <th>Datum</th>
                <th>Aktionen</th>
            </tr>
        </thead>
        <tbody>
"""
                for r in reversed(reports):
                    cape = capes_by_id.get(r.get("cape_id", ""), {})
                    cape_id = r.get("cape_id", "")
                    has_gif = (IMAGE_DIR / f"{cape_id}.gif").is_file() or bool(cape.get("is_animated"))
                    display_url = f"http://{host_ip}:{PORT}/api/capes/{cape_id}/animation" if has_gif else f"http://{host_ip}:{PORT}/api/capes/{cape_id}/image"
                    raw_url = f"http://{host_ip}:{PORT}/api/capes/{cape_id}/image"
                    status = r.get("status", "open")
                    status_class = f"status-{status}"
                    status_label = "🚨 Offen" if status == "open" else ("✅ Erledigt" if status == "resolved" else "🗑️ Cape Gelöscht")
                    created = r.get("created_at", "")[:19].replace("T", " ")
                    title_esc = html.escape(cape.get('title', 'Gelöschtes Cape'))
                    owner_esc = html.escape(cape.get('owner', ''))
                    badge_html = ' <span style="background:#22C96E;color:#0B0E14;font-size:10px;font-weight:700;padding:1px 5px;border-radius:4px;">🎬 ANIMIERT</span>' if has_gif else ''
                    report_img_html = f'<img src="{display_url}" style="width:100%;height:100%;object-fit:cover;image-rendering:pixelated;" onerror="this.alt=\'Gelöscht\';this.src=\'\';">' if has_gif else f'<img src="{display_url}" class="cape-portrait-img" onerror="this.alt=\'Gelöscht\';this.src=\'\';">'

                    action_buttons = ""
                    if status == "open":
                        action_buttons = f"""
                        <form method="post" action="/reports/{r['id']}/resolve" style="display:inline-block;margin-bottom:4px;">
                            <button type="submit" class="btn btn-success" style="padding:5px 9px;">✓ Erledigt</button>
                        </form>
                        <form method="post" action="/reports/{r['id']}/remove-cape" onsubmit="return confirm('Cape löschen und Report schließen?');" style="display:inline-block;">
                            <button type="submit" class="btn btn-danger" style="padding:5px 9px;">🗑️ Cape Löschen</button>
                        </form>
                        """
                    else:
                        action_buttons = f"""
                        <form method="post" action="/reports/{r['id']}/dismiss" onsubmit="return confirm('Report-Eintrag entfernen?');" style="display:inline-block;">
                            <button type="submit" class="btn btn-secondary" style="padding:4px 8px;font-size:12px;">✕ Entfernen</button>
                        </form>
                        """

                    html_out += f"""
            <tr>
                <td><span class="{status_class}">{status_label}</span></td>
                <td>
                    <div class="report-cape-box" title="Klicken für Großansicht" onclick="openModal('{display_url}', '{raw_url}', '{title_esc}', '{owner_esc}', {str(has_gif).lower()})">
                        {report_img_html}
                    </div>
                </td>
                <td>
                    <div style="font-weight:700;">{title_esc}{badge_html}</div>
                    <div style="font-size:12px;color:#43DD8C;">{owner_esc}</div>
                </td>
                <td style="max-width:280px;color:#F1F5F9;">{html.escape(r.get('reason', ''))}</td>
                <td style="color:#94A3B8;">{html.escape(r.get('reporter', 'Anonym'))}</td>
                <td style="color:#64748B;font-size:12px;">{html.escape(created)}</td>
                <td>{action_buttons}</td>
            </tr>
"""
                html_out += """
        </tbody>
    </table>
"""

        elif tab == "presence":
            if not online:
                html_out += '<div class="empty-msg">Aktuell sind keine Spieler im EzClient online.</div>'
            else:
                html_out += """
    <table>
        <thead>
            <tr>
                <th>Spielername</th>
                <th>UUID</th>
                <th>Status</th>
            </tr>
        </thead>
        <tbody>
"""
                for p in online:
                    html_out += f"""
            <tr>
                <td style="font-weight:700;color:#F8FAFC;">👤 {html.escape(p['username'])}</td>
                <td style="font-family:monospace;color:#43DD8C;">{html.escape(p['uuid'])}</td>
                <td><span style="color:#22C96E;font-weight:700;">● Online</span></td>
            </tr>
"""
                html_out += """
        </tbody>
    </table>
"""

        # Modal Lightbox HTML & JS
        html_out += """
</div>

<!-- Zoom Lightbox Modal -->
<div class="modal-overlay" id="capeModal" onclick="closeModal(event)">
    <div class="modal-card" onclick="event.stopPropagation()">
        <button class="modal-close" onclick="closeModalDirect()">✕</button>
        <h3 id="modalTitle" style="color:#F8FAFC;font-size:18px;">Cape Vorschau</h3>
        <p id="modalOwner" style="color:#43DD8C;font-size:14px;margin-top:-10px;">👤 Spieler</p>
        
        <div style="display:flex;gap:20px;align-items:center;margin-top:10px;">
            <div style="text-align:center;">
                <div style="font-size:11px;color:#94A3B8;margin-bottom:6px;text-transform:uppercase;">Ingame (10:16)</div>
                <div class="modal-cape-large" id="modalCapeLarge">
                    <img id="modalCapeImg" src="" class="cape-portrait-img">
                </div>
            </div>
            <div style="text-align:center;">
                <div style="font-size:11px;color:#94A3B8;margin-bottom:6px;text-transform:uppercase;">Roh-Atlas (2:1)</div>
                <img id="modalRawImg" src="" class="modal-raw-img">
            </div>
        </div>

        <div style="display:flex;gap:10px;width:100%;margin-top:14px;">
            <a id="modalDirectLink" href="#" target="_blank" class="btn btn-secondary">Datei Öffnen</a>
            <a id="modalDownloadLink" href="#" download class="btn btn-success">Herunterladen</a>
        </div>
    </div>
</div>

<script>
function openModal(displayUrl, rawUrl, title, owner, isAnim) {
    document.getElementById('modalTitle').innerHTML = (title || 'Cape Vorschau') + (isAnim ? ' <span style="background:#22C96E;color:#0B0E14;font-size:11px;font-weight:700;padding:2px 6px;border-radius:4px;">🎬 ANIMIERT</span>' : '');
    document.getElementById('modalOwner').textContent = '👤 ' + (owner || 'Unbekannt');
    const capeImg = document.getElementById('modalCapeImg');
    if (isAnim) {
        capeImg.className = '';
        capeImg.style.width = '100%';
        capeImg.style.height = '100%';
        capeImg.style.objectFit = 'cover';
        capeImg.style.imageRendering = 'pixelated';
        capeImg.src = displayUrl;
    } else {
        capeImg.className = 'cape-portrait-img';
        capeImg.style.width = '640%';
        capeImg.style.height = '200%';
        capeImg.style.left = '-10%';
        capeImg.style.top = '-6.25%';
        capeImg.src = rawUrl;
    }
    document.getElementById('modalRawImg').src = rawUrl;
    document.getElementById('modalDirectLink').href = displayUrl;
    document.getElementById('modalDownloadLink').href = displayUrl;
    document.getElementById('capeModal').classList.add('open');
}
function closeModalDirect() {
    document.getElementById('capeModal').classList.remove('open');
}
function closeModal(e) {
    if (e.target.id === 'capeModal') {
        closeModalDirect();
    }
}
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') closeModalDirect();
});
</script>
</body>
</html>
"""
        raw = html_out.encode("utf-8")
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_POST(self) -> None:
        if not self.authorised():
            self.challenge()
            return

        parsed_path = urlparse(self.path).path

        # 1. Direct Cape Delete: /capes/<id>/delete
        match_cape_del = re.fullmatch(r"/capes/([a-f0-9-]{36})/delete", parsed_path)
        if match_cape_del:
            cape_id = match_cape_del.group(1)
            (IMAGE_DIR / f"{cape_id}.png").unlink(missing_ok=True)
            (IMAGE_DIR / f"{cape_id}.gif").unlink(missing_ok=True)
            save_capes([c for c in load_capes() if c.get("id") != cape_id])
            self.send_response(HTTPStatus.SEE_OTHER)
            self.send_header("Location", "/?tab=capes")
            self.end_headers()
            return

        # 2. Resolve Report: /reports/<id>/resolve
        match_resolve = re.fullmatch(r"/reports/([a-f0-9-]{36})/resolve", parsed_path)
        if match_resolve:
            rep_id = match_resolve.group(1)
            with REPORTS_LOCK:
                reports = load_reports()
                for r in reports:
                    if r.get("id") == rep_id:
                        r["status"] = "resolved"
                save_reports(reports)
            self.send_response(HTTPStatus.SEE_OTHER)
            self.send_header("Location", "/?tab=reports")
            self.end_headers()
            return

        # 3. Dismiss Report: /reports/<id>/dismiss
        match_dismiss = re.fullmatch(r"/reports/([a-f0-9-]{36})/dismiss", parsed_path)
        if match_dismiss:
            rep_id = match_dismiss.group(1)
            with REPORTS_LOCK:
                save_reports([r for r in load_reports() if r.get("id") != rep_id])
            self.send_response(HTTPStatus.SEE_OTHER)
            self.send_header("Location", "/?tab=reports")
            self.end_headers()
            return

        # 4. Remove Cape from Report: /reports/<id>/remove-cape
        match_remove_cape = re.fullmatch(r"/reports/([a-f0-9-]{36})/remove-cape", parsed_path)
        if match_remove_cape:
            rep_id = match_remove_cape.group(1)
            cape_to_del = None
            with REPORTS_LOCK:
                reports = load_reports()
                for r in reports:
                    if r.get("id") == rep_id:
                        cape_to_del = r.get("cape_id")
                        r["status"] = "removed"
                save_reports(reports)
            if cape_to_del:
                (IMAGE_DIR / f"{cape_to_del}.png").unlink(missing_ok=True)
                (IMAGE_DIR / f"{cape_to_del}.gif").unlink(missing_ok=True)
                save_capes([c for c in load_capes() if c.get("id") != cape_to_del])
            self.send_response(HTTPStatus.SEE_OTHER)
            self.send_header("Location", "/?tab=reports")
            self.end_headers()
            return

        self.send_response(HTTPStatus.NOT_FOUND)
        self.end_headers()

        # 2. Report Actions: /reports/<id>/(resolve|remove-cape|dismiss)
        match_report = re.fullmatch(r"/reports/([a-f0-9-]{36})/(resolve|remove-cape|dismiss)", parsed_path)
        if match_report:
            report_id, action = match_report.groups()
            reports = load_reports()
            if action == "dismiss":
                save_reports([r for r in reports if r.get("id") != report_id])
            else:
                report = next((r for r in reports if r.get("id") == report_id), None)
                if report:
                    report["status"] = "resolved" if action == "resolve" else "cape_removed"
                    if action == "remove-cape":
                        (IMAGE_DIR / f"{report['cape_id']}.png").unlink(missing_ok=True)
                        save_capes([c for c in load_capes() if c.get("id") != report["cape_id"]])
                    save_reports(reports)

            self.send_response(HTTPStatus.SEE_OTHER)
            self.send_header("Location", "/?tab=reports")
            self.end_headers()
            return

        self.send_error(HTTPStatus.NOT_FOUND)


if __name__ == "__main__":
    ROOT.mkdir(parents=True, exist_ok=True)
    if not os.environ.get("EZCLIENT_ADMIN_PASSWORD"):
        raise SystemExit("Setze EZCLIENT_ADMIN_PASSWORD vor dem Start.")
    print(f"EzClient Cape Community API: http://{HOST}:{PORT}/api/capes")
    print(f"EzClient Admin Dashboard:     http://{ADMIN_HOST}:{ADMIN_PORT}/")
    admin = ThreadingHTTPServer((ADMIN_HOST, ADMIN_PORT), AdminHandler)
    threading.Thread(target=admin.serve_forever, daemon=True).start()
    ThreadingHTTPServer((HOST, PORT), CapeHandler).serve_forever()
