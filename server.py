"""Hardened cape community service for EzClient.

The application exposes the current ``/api/capes`` contract and the legacy
``/upload_cape`` / ``/get_cape/<uuid>`` routes.  Production uploads require a
Minecraft bearer token for the first upload.  The returned ownership token can
be used for later replacements without persisting the Minecraft access token.
"""
from __future__ import annotations

import hashlib
import hmac
import json
import os
import secrets
import shutil
import tempfile
import urllib.error
import urllib.request
import uuid
from pathlib import Path
from typing import Any, Callable

from flask import Flask, abort, jsonify, request, send_file
from PIL import Image, ImageSequence
from werkzeug.exceptions import RequestEntityTooLarge
from werkzeug.utils import secure_filename

from backend.services.cape_community import is_safe_cape_png, normalize_player_uuid, validate_cape_title
from backend.services.cape_media import (
    ATLAS_SIZE,
    MAX_FRAMES,
    MAX_SOURCE_BYTES,
    AnimationOptions,
    generate_frame_sheet,
)

MAX_PNG_BYTES = 2 * 1024 * 1024
ALLOWED_MEDIA = {".png", ".gif", ".mp4", ".webm"}
TOKEN_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"


def _json_write_atomic(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f".{path.name}.{secrets.token_hex(6)}.tmp")
    try:
        temporary.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def _read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
        return value if isinstance(value, dict) else {}
    except (OSError, UnicodeDecodeError, json.JSONDecodeError):
        return {}


def _minecraft_profile(access_token: str) -> dict[str, Any]:
    req = urllib.request.Request(
        TOKEN_PROFILE_URL,
        headers={"Authorization": f"Bearer {access_token}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=8) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        raise PermissionError("Minecraft-Sitzung konnte nicht verifiziert werden.") from exc
    if not isinstance(payload, dict) or not payload.get("id"):
        raise PermissionError("Minecraft-Sitzung enthält keine gültigen Spielerdaten.")
    return payload


def _normalized_profile_uuid(profile: dict[str, Any]) -> str:
    raw = str(profile.get("id") or profile.get("uuid") or "").strip()
    return normalize_player_uuid(raw)


def _fit_face(image: Image.Image) -> Image.Image:
    source = image.convert("RGBA")
    target_ratio = 10 / 16
    source_ratio = source.width / max(1, source.height)
    if source_ratio > target_ratio:
        width = max(1, round(source.height * target_ratio))
        left = (source.width - width) // 2
        source = source.crop((left, 0, left + width, source.height))
    else:
        height = max(1, round(source.width / target_ratio))
        top = (source.height - height) // 2
        source = source.crop((0, top, source.width, top + height))
    face = source.resize((40, 64), Image.Resampling.LANCZOS)
    atlas = Image.new("RGBA", ATLAS_SIZE, (0, 0, 0, 0))
    atlas.paste(face, (4, 4), face)
    return atlas


def _convert_gif(source: Path, animation_dir: Path) -> dict[str, Any]:
    animation_dir.mkdir()
    with Image.open(source) as gif:
        frames: list[Image.Image] = []
        durations: list[int] = []
        for frame in ImageSequence.Iterator(gif):
            if len(frames) >= MAX_FRAMES:
                raise ValueError(f"Animierte Capes dürfen höchstens {MAX_FRAMES} Frames enthalten.")
            frames.append(_fit_face(frame))
            durations.append(max(20, int(frame.info.get("duration", 100))))
    if not frames:
        raise ValueError("Das GIF enthält keine lesbaren Frames.")
    columns = min(16, len(frames))
    rows = (len(frames) + columns - 1) // columns
    sheet = Image.new("RGBA", (columns * ATLAS_SIZE[0], rows * ATLAS_SIZE[1]), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        sheet.paste(frame, ((index % columns) * ATLAS_SIZE[0], (index // columns) * ATLAS_SIZE[1]))
    sheet.save(animation_dir / "framesheet.png", "PNG", optimize=True)
    average_ms = sum(durations) / len(durations)
    manifest = {
        "version": 1,
        "sheet": "framesheet.png",
        "frame_count": len(frames),
        "fps": max(1, min(20, round(1000 / average_ms))),
        "ping_pong": False,
        "frame_width": ATLAS_SIZE[0],
        "frame_height": ATLAS_SIZE[1],
        "columns": columns,
        "duration": sum(durations) / 1000,
    }
    _json_write_atomic(animation_dir / "animation.json", manifest)
    frames[0].save(animation_dir.parent / "preview.png", "PNG", optimize=True)
    return manifest


def create_app(config: dict[str, Any] | None = None) -> Flask:
    app = Flask(__name__)
    app.config.update(
        CAPE_DATA_DIR=os.environ.get("EZCLIENT_CAPE_DATA", str(Path.cwd() / "cape_data")),
        MAX_CONTENT_LENGTH=MAX_SOURCE_BYTES + 1024 * 1024,
        TOKEN_VERIFIER=_minecraft_profile,
    )
    if config:
        app.config.update(config)

    root = Path(app.config["CAPE_DATA_DIR"]).resolve()
    capes_dir = root / "capes"
    players_dir = root / "players"
    capes_dir.mkdir(parents=True, exist_ok=True)
    players_dir.mkdir(parents=True, exist_ok=True)

    @app.errorhandler(RequestEntityTooLarge)
    def too_large(_error: RequestEntityTooLarge):
        return jsonify(error="Die Upload-Datei überschreitet das 64-MB-Limit."), 413

    @app.errorhandler(400)
    def bad_request(error):
        return jsonify(error=getattr(error, "description", "Ungültige Anfrage.")), 400

    @app.errorhandler(401)
    def unauthorized(error):
        return jsonify(error=getattr(error, "description", "Authentifizierung fehlgeschlagen.")), 401

    def authenticate(player_uuid: str, owner: str) -> tuple[dict[str, Any], str | None]:
        player_path = players_dir / f"{player_uuid.replace('-', '')}.json"
        player = _read_json(player_path)
        authorization = request.headers.get("Authorization", "")
        bearer = authorization[7:].strip() if authorization.lower().startswith("bearer ") else ""
        supplied_owner_token = request.headers.get("X-EzClient-Cape-Token") or request.form.get("token", "")

        if bearer:
            verifier: Callable[[str], dict[str, Any]] = app.config["TOKEN_VERIFIER"]
            try:
                profile = verifier(bearer)
                verified_uuid = _normalized_profile_uuid(profile)
            except (PermissionError, ValueError, TypeError) as exc:
                abort(401, description=str(exc))
            if not hmac.compare_digest(verified_uuid, player_uuid):
                abort(401, description="Die Session gehört nicht zur angegebenen Spieler-UUID.")
            verified_name = str(profile.get("name") or "").strip()
            if verified_name and owner and verified_name.casefold() != owner.casefold():
                abort(401, description="Der Spielername stimmt nicht mit der Minecraft-Session überein.")
            return player, None

        stored_hash = str(player.get("ownership_token_hash") or "")
        supplied_hash = hashlib.sha256(supplied_owner_token.encode("utf-8")).hexdigest()
        if not stored_hash or not supplied_owner_token or not hmac.compare_digest(stored_hash, supplied_hash):
            abort(401, description="Eine gültige Minecraft-Session oder ein Cape-Besitzertoken ist erforderlich.")
        return player, supplied_owner_token

    @app.get("/health")
    def health():
        return jsonify(status="ok"), 200

    @app.get("/api/capes")
    def list_capes():
        result = []
        for metadata_path in capes_dir.glob("*/metadata.json"):
            metadata = _read_json(metadata_path)
            if metadata:
                result.append(metadata)
        result.sort(key=lambda item: str(item.get("created_at") or ""), reverse=True)
        return jsonify(capes=result), 200

    @app.get("/api/capes/<cape_id>/image")
    def cape_image(cape_id: str):
        try:
            safe_id = str(uuid.UUID(cape_id))
        except ValueError:
            abort(404)
        preview = capes_dir / safe_id / "preview.png"
        if not preview.is_file():
            abort(404)
        return send_file(preview, mimetype="image/png", conditional=True)

    @app.get("/get_cape/<player_uuid>")
    def get_cape(player_uuid: str):
        try:
            canonical = normalize_player_uuid(player_uuid)
        except ValueError:
            abort(400, description="Ungültige Spieler-UUID.")
        player = _read_json(players_dir / f"{canonical.replace('-', '')}.json")
        cape_id = str(player.get("active_cape_id") or "")
        preview = capes_dir / cape_id / "preview.png"
        if not cape_id or not preview.is_file():
            abort(404)
        return send_file(preview, mimetype="image/png", conditional=True)

    @app.post("/api/capes")
    @app.post("/upload_cape")
    def upload_cape():
        owner = " ".join(str(request.form.get("owner") or request.headers.get("X-Player-Name") or "").split())
        raw_uuid = request.form.get("owner_uuid") or request.headers.get("X-Player-UUID") or request.headers.get("X-Minecraft-UUID")
        try:
            player_uuid = normalize_player_uuid(raw_uuid or "")
        except ValueError:
            abort(400, description="Ungültige Spielerdaten: Spieler-UUID fehlt oder ist ungültig.")
        if not 1 <= len(owner) <= 16 or not all(char.isalnum() or char == "_" for char in owner):
            abort(400, description="Ungültige Spielerdaten: Spielername fehlt oder ist ungültig.")
        try:
            title = validate_cape_title(request.form.get("title") or "")
        except ValueError as exc:
            abort(400, description=str(exc))

        player, existing_token = authenticate(player_uuid, owner)
        upload = request.files.get("cape") or request.files.get("file")
        if upload is None or not upload.filename:
            abort(400, description="Es wurde keine Cape-Datei übertragen.")
        filename = secure_filename(upload.filename)
        extension = Path(filename).suffix.lower()
        if extension not in ALLOWED_MEDIA:
            abort(400, description="Unterstützt werden PNG, GIF, MP4 und WebM.")

        cape_id = str(uuid.uuid4())
        capes_dir.mkdir(parents=True, exist_ok=True)
        temporary = Path(tempfile.mkdtemp(prefix=".cape-upload-", dir=capes_dir))
        final = capes_dir / cape_id
        try:
            source = temporary / f"source{extension}"
            upload.save(source)
            size = source.stat().st_size
            if size <= 0 or size > (MAX_PNG_BYTES if extension == ".png" else MAX_SOURCE_BYTES):
                abort(400, description="Die Cape-Datei ist leer oder überschreitet das Größenlimit.")

            animated = extension != ".png"
            manifest: dict[str, Any] | None = None
            if extension == ".png":
                raw = source.read_bytes()
                if not is_safe_cape_png(raw):
                    abort(400, description="Das PNG ist beschädigt oder besitzt kein unterstütztes Cape-Format.")
                shutil.copy2(source, temporary / "preview.png")
            elif extension == ".gif":
                manifest = _convert_gif(source, temporary / "animation")
            else:
                info_dir = temporary / "animation"
                manifest_obj = generate_frame_sheet(source, info_dir, AnimationOptions(end=10.0, fps=12))
                manifest = vars(manifest_obj)
                with Image.open(info_dir / "framesheet.png") as sheet:
                    sheet.crop((0, 0, ATLAS_SIZE[0], ATLAS_SIZE[1])).save(temporary / "preview.png", "PNG", optimize=True)

            import datetime as _datetime
            created_at = _datetime.datetime.now(_datetime.timezone.utc).isoformat()
            metadata = {
                "id": cape_id,
                "owner": owner,
                "owner_uuid": player_uuid,
                "title": title,
                "animated": animated,
                "animation": manifest,
                "image_url": f"/api/capes/{cape_id}/image",
                "created_at": created_at,
            }
            _json_write_atomic(temporary / "metadata.json", metadata)
            temporary.replace(final)

            ownership_token = existing_token or secrets.token_urlsafe(32)
            player.update({
                "uuid": player_uuid,
                "name": owner,
                "active_cape_id": cape_id,
                "ownership_token_hash": hashlib.sha256(ownership_token.encode("utf-8")).hexdigest(),
            })
            _json_write_atomic(players_dir / f"{player_uuid.replace('-', '')}.json", player)
            response = dict(metadata)
            response["token"] = ownership_token
            return jsonify(response), 200
        finally:
            if temporary.exists():
                shutil.rmtree(temporary, ignore_errors=True)

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host=os.environ.get("EZCLIENT_CAPE_HOST", "127.0.0.1"), port=int(os.environ.get("EZCLIENT_CAPE_PORT", "18765")))
