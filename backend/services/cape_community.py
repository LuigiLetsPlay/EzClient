"""Client for the self-hosted EzClient cape community API."""
from __future__ import annotations

import json
import os
import urllib.error
import urllib.parse
import urllib.request
import struct
import uuid
import zlib
from pathlib import Path
from backend.models.types import APP_VERSION


DEFAULT_API_URL = "http://5.175.192.90:18765/api"
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MAX_CAPE_BYTES = 2 * 1024 * 1024


def normalize_player_uuid(value: str) -> str:
    """Return the canonical UUID form used by the community API."""
    try:
        return str(uuid.UUID(str(value).strip()))
    except (AttributeError, TypeError, ValueError) as exc:
        raise ValueError("Die Spieler-UUID ist ungültig. Bitte melde dich erneut an.") from exc


def validate_cape_title(value: str) -> str:
    """Validate user-facing cape metadata without silently truncating it."""
    title = " ".join(str(value or "").split())
    if not 3 <= len(title) <= 48:
        raise ValueError("Der Cape-Name muss zwischen 3 und 48 Zeichen lang sein.")
    if any(ord(char) < 32 or char in "<>" for char in title):
        raise ValueError("Der Cape-Name enthält ungültige Zeichen.")
    return title


def is_safe_cape_png(raw: bytes) -> bool:
    """Mirror the server validation before QML ever receives a community image."""
    if not raw.startswith(PNG_SIGNATURE) or len(raw) > 2 * 1024 * 1024:
        return False
    pos, width, height, color_type = len(PNG_SIGNATURE), 0, 0, -1
    data_stream, ihdr, iend = bytearray(), False, False
    try:
        while pos < len(raw):
            if pos + 12 > len(raw): return False
            size = struct.unpack(">I", raw[pos:pos + 4])[0]
            if size > 2 * 1024 * 1024 or pos + 12 + size > len(raw): return False
            kind, data = raw[pos + 4:pos + 8], raw[pos + 8:pos + 8 + size]
            crc = struct.unpack(">I", raw[pos + 8 + size:pos + 12 + size])[0]
            if zlib.crc32(kind + data) & 0xffffffff != crc: return False
            pos += 12 + size
            if kind == b"IHDR" and not ihdr and size == 13:
                width, height, depth, color_type, compression, filtering, interlace = struct.unpack(">IIBBBBB", data)
                if (width, height) not in {(64, 32), (128, 64), (256, 128), (512, 256), (1024, 512)} or depth != 8 or color_type not in (2, 6) or compression or filtering or interlace: return False
                ihdr = True
            elif kind == b"IDAT" and ihdr and not iend: data_stream.extend(data)
            elif kind == b"IEND" and size == 0: iend = True; break
            else: return False
        channels = 4 if color_type == 6 else 3
        decoded = zlib.decompress(bytes(data_stream))
        row = 1 + width * channels
        return ihdr and iend and pos == len(raw) and len(decoded) == height * row and all(decoded[y * row] <= 4 for y in range(height))
    except (ValueError, struct.error, zlib.error):
        return False


def _base_url() -> str:
    return os.environ.get("EZCLIENT_CAPE_API", DEFAULT_API_URL).rstrip("/")


def _upload_url() -> str:
    return f"{_base_url()}/capes"


def list_capes() -> list[dict]:
    request = urllib.request.Request(
        f"{_base_url()}/capes", headers={"Accept": "application/json", "User-Agent": f"EzClient/{APP_VERSION}"}
    )
    with urllib.request.urlopen(request, timeout=8) as response:
        payload = json.loads(response.read().decode("utf-8"))
    capes = payload.get("capes", payload) if isinstance(payload, (dict, list)) else []
    if not isinstance(capes, list):
        return []
    return [item for item in capes if isinstance(item, dict)]


def upload_cape(
    path: Path,
    owner: str,
    owner_uuid: str,
    title: str,
    token: str = "",
    access_token: str = "",
    anim_gif: bytes | None = None,
) -> dict:
    """Upload one PNG cape (and optional animated GIF) using a small multipart request with ownership token support."""
    image = path.read_bytes()
    if len(image) > MAX_CAPE_BYTES:
        raise ValueError("Das Cape darf maximal 2 MB groß sein.")
    if not is_safe_cape_png(image):
        raise ValueError("Bitte wähle ein gültiges Cape-PNG (64×32 bis 1024×512, maximal 2 MB).")

    if not access_token or any(char in access_token for char in "\r\n"):
        raise ValueError("Die Minecraft-Sitzung ist abgelaufen. Bitte melde dich erneut an.")

    canonical_uuid = normalize_player_uuid(owner_uuid)
    clean_title = validate_cape_title(title)
    boundary = "----EzClientCapeBoundary"
    fields = {
        "owner": owner,
        "owner_uuid": canonical_uuid,
        "title": clean_title,
        "token": token or ""
    }
    parts: list[bytes] = []
    for key, value in fields.items():
        parts.extend([
            f"--{boundary}\r\n".encode(),
            f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode(),
            str(value).encode("utf-8"), b"\r\n",
        ])
    parts.extend([
        f"--{boundary}\r\n".encode(),
        b'Content-Disposition: form-data; name="cape"; filename="cape.png"\r\n',
        b"Content-Type: image/png\r\n\r\n", image, b"\r\n",
    ])
    if anim_gif and len(anim_gif) > 0 and len(anim_gif) <= 8 * 1024 * 1024:
        parts.extend([
            f"--{boundary}\r\n".encode(),
            b'Content-Disposition: form-data; name="anim_gif"; filename="anim.gif"\r\n',
            b"Content-Type: image/gif\r\n\r\n", anim_gif, b"\r\n",
        ])
    parts.append(f"--{boundary}--\r\n".encode())
    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Accept": "application/json",
        "User-Agent": f"EzClient/{APP_VERSION}",
    }
    # Never expose the Minecraft access token over the legacy HTTP endpoint.
    # HTTPS deployments receive the stronger bearer-token verification; HTTP
    # keeps the UUID/name + persistent cape ownership-token compatibility path.
    if urllib.parse.urlparse(_base_url()).scheme == "https":
        headers["Authorization"] = f"Bearer {access_token}"
    if token:
        headers["X-EzClient-Cape-Token"] = token

    request = urllib.request.Request(
        _upload_url(),
        data=b"".join(parts),
        method="POST",
        headers=headers,
    )
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            payload = json.loads(response.read().decode("utf-8"))
        return payload if isinstance(payload, dict) else {}
    except urllib.error.HTTPError as exc:
        try:
            err_data = json.loads(exc.read().decode("utf-8"))
            err_msg = err_data.get("error") or str(exc)
        except Exception:
            err_msg = str(exc)
        raise ValueError(err_msg)


def cape_image_url(cape: dict) -> str:
    value = str(cape.get("image_url") or cape.get("imageUrl") or "")
    if value.startswith(("http://", "https://")):
        return value
    cape_id = str(cape.get("id") or cape.get("slug") or "")
    return f"{_base_url()}/capes/{urllib.parse.quote(cape_id)}/image" if cape_id else ""


def deactivate_cape(owner_uuid: str, token: str, access_token: str = "") -> None:
    """Stop advertising an EzClient cape for this player without deleting uploads."""
    canonical_uuid = normalize_player_uuid(owner_uuid)
    data = json.dumps({"owner_uuid": canonical_uuid, "token": token or ""}).encode("utf-8")
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json",
        "User-Agent": f"EzClient/{APP_VERSION}",
    }
    if urllib.parse.urlparse(_base_url()).scheme == "https" and access_token:
        headers["Authorization"] = f"Bearer {access_token}"
    if token:
        headers["X-EzClient-Cape-Token"] = token
    request = urllib.request.Request(
        f"{_base_url()}/capes/deactivate", data=data, method="POST", headers=headers
    )
    try:
        with urllib.request.urlopen(request, timeout=10):
            pass
    except urllib.error.HTTPError as exc:
        try:
            message = json.loads(exc.read().decode("utf-8")).get("error") or str(exc)
        except Exception:
            message = str(exc)
        raise ValueError(message)


def report_cape(cape_id: str, reason: str, reporter: str) -> None:
    data = json.dumps({"cape_id": cape_id, "reason": reason, "reporter": reporter}).encode("utf-8")
    request = urllib.request.Request(
        f"{_base_url()}/reports", data=data, method="POST",
        headers={"Content-Type": "application/json", "Accept": "application/json", "User-Agent": f"EzClient/{APP_VERSION}"},
    )
    with urllib.request.urlopen(request, timeout=10):
        pass
