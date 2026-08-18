import sys
import os
from pathlib import Path
from typing import Any
import urllib.request
import urllib.parse
import json

MOJANG_SKIN_URL = "https://api.minecraftservices.com/minecraft/profile/skins"

def upload_skin_file(access_token: str, file_path: str | Path, variant: str = "classic") -> tuple[bool, str]:
    """
    Uploads a PNG skin file to Mojang's skin servers using the player's Bearer access token.
    variant: 'classic' (4px arm) or 'slim' (3px arm, Alex model)
    """
    if not access_token:
        return False, "Kein gültiger Microsoft-Sitzungstoken vorhanden."

    path = Path(file_path)
    if not path.exists() or not path.is_file():
        return False, f"Skin-Datei existiert nicht: {file_path}"

    try:
        skin_bytes = path.read_bytes()
        variant_val = "slim" if str(variant).lower() == "slim" else "classic"

        # Build multipart/form-data payload
        boundary = "----EzClientSkinBoundary" + os.urandom(8).hex()
        body = bytearray()

        # 1. Variant field
        body.extend(f"--{boundary}\r\n".encode("utf-8"))
        body.extend(f'Content-Disposition: form-data; name="variant"\r\n\r\n'.encode("utf-8"))
        body.extend(f"{variant_val}\r\n".encode("utf-8"))

        # 2. File field
        body.extend(f"--{boundary}\r\n".encode("utf-8"))
        body.extend(f'Content-Disposition: form-data; name="file"; filename="{path.name}"\r\n'.encode("utf-8"))
        body.extend(b"Content-Type: image/png\r\n\r\n")
        body.extend(skin_bytes)
        body.extend(b"\r\n")

        # End boundary
        body.extend(f"--{boundary}--\r\n".encode("utf-8"))

        req = urllib.request.Request(
            MOJANG_SKIN_URL,
            data=bytes(body),
            headers={
                "Authorization": f"Bearer {access_token}",
                "Content-Type": f"multipart/form-data; boundary={boundary}",
                "User-Agent": "EzClient/1.0.3"
            },
            method="POST"
        )

        with urllib.request.urlopen(req, timeout=15) as resp:
            status = resp.getcode()
            if status in (200, 204):
                return True, "Skin erfolgreich bei Mojang hochgeladen!"
            
        return True, "Skin erfolgreich aktualisiert!"

    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="replace")
        try:
            err_json = json.loads(err_msg)
            msg = err_json.get("errorMessage", err_msg)
        except Exception:
            msg = err_msg
        return False, f"Mojang-Fehler ({e.code}): {msg}"
    except Exception as e:
        return False, f"Fehler beim Skin-Upload: {e}"

def reset_skin_to_default(access_token: str) -> tuple[bool, str]:
    """Resets player skin to Mojang default."""
    if not access_token:
        return False, "Kein Zugriffstoken vorhanden."
    try:
        req = urllib.request.Request(
            MOJANG_SKIN_URL,
            headers={
                "Authorization": f"Bearer {access_token}",
                "User-Agent": "EzClient/1.0.8"
            },
            method="DELETE"
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            return True, "Skin auf Standard zurückgesetzt."
    except Exception as e:
        return False, f"Fehler beim Zurücksetzen: {e}"


def get_skins_dir() -> Path:
    from backend.models.types import DATA_DIR
    p = DATA_DIR / "skins"
    p.mkdir(parents=True, exist_ok=True)
    return p


def get_skin_history() -> list[dict]:
    hist_file = get_skins_dir() / "history.json"
    if hist_file.exists():
        try:
            return json.loads(hist_file.read_text("utf-8"))
        except Exception:
            return []
    return []


def generate_skin_renders(skin_path: str | Path) -> tuple[str, str]:
    """
    Renders both a full-body assembled character preview and a 64x64 head avatar from a raw skin texture PNG.
    Returns (body_preview_path, avatar_preview_path).
    """
    path = Path(skin_path)
    if not path.exists() or not path.is_file():
        return "", ""

    try:
        from PySide6.QtGui import QImage, QPainter
        from PySide6.QtCore import Qt, QRect

        src = QImage(str(path))
        if src.isNull():
            return "", ""

        is_64x64 = src.height() >= 64
        previews_dir = get_skins_dir() / "renders"
        previews_dir.mkdir(parents=True, exist_ok=True)
        body_out = previews_dir / f"{path.stem}_body.png"
        avatar_out = previews_dir / f"{path.stem}_avatar.png"

        # 1. Render 64x64 Head Avatar
        head_img = QImage(64, 64, QImage.Format_ARGB32_Premultiplied)
        head_img.fill(Qt.transparent)
        hp = QPainter(head_img)
        hp.setRenderHint(QPainter.SmoothPixmapTransform, False)
        # Head Base (8, 8, 8, 8) -> (0, 0, 64, 64)
        hp.drawImage(QRect(0, 0, 64, 64), src, QRect(8, 8, 8, 8))
        # Head Hat Overlay (40, 8, 8, 8)
        hp.drawImage(QRect(0, 0, 64, 64), src, QRect(40, 8, 8, 8))
        hp.end()
        head_img.save(str(avatar_out), "PNG")

        # 2. Render Assembled Front Full-Body (Width: 240, Height: 360)
        body_img = QImage(240, 360, QImage.Format_ARGB32_Premultiplied)
        body_img.fill(Qt.transparent)
        bp = QPainter(body_img)
        bp.setRenderHint(QPainter.SmoothPixmapTransform, False)

        ox, oy, S = 40, 20, 10
        # Head Base (8, 8, 8, 8)
        bp.drawImage(QRect(ox + 4 * S, oy, 8 * S, 8 * S), src, QRect(8, 8, 8, 8))
        # Head Hat (40, 8, 8, 8)
        bp.drawImage(QRect(ox + 4 * S, oy, 8 * S, 8 * S), src, QRect(40, 8, 8, 8))

        # Torso Base (20, 20, 8, 12)
        bp.drawImage(QRect(ox + 4 * S, oy + 8 * S, 8 * S, 12 * S), src, QRect(20, 20, 8, 12))
        if is_64x64:
            # Torso Jacket (20, 36, 8, 12)
            bp.drawImage(QRect(ox + 4 * S, oy + 8 * S, 8 * S, 12 * S), src, QRect(20, 36, 8, 12))

        # Right Arm Base (44, 20, 4, 12)
        bp.drawImage(QRect(ox, oy + 8 * S, 4 * S, 12 * S), src, QRect(44, 20, 4, 12))
        if is_64x64:
            # Right Arm Sleeve (44, 36, 4, 12)
            bp.drawImage(QRect(ox, oy + 8 * S, 4 * S, 12 * S), src, QRect(44, 36, 4, 12))

        # Left Arm Base (36, 52, 4, 12 if 64x64 else mirror)
        if is_64x64:
            bp.drawImage(QRect(ox + 12 * S, oy + 8 * S, 4 * S, 12 * S), src, QRect(36, 52, 4, 12))
            bp.drawImage(QRect(ox + 12 * S, oy + 8 * S, 4 * S, 12 * S), src, QRect(52, 52, 4, 12))
        else:
            bp.drawImage(QRect(ox + 12 * S, oy + 8 * S, 4 * S, 12 * S), src, QRect(44, 20, 4, 12))

        # Right Leg Base (4, 20, 4, 12)
        bp.drawImage(QRect(ox + 4 * S, oy + 20 * S, 4 * S, 12 * S), src, QRect(4, 20, 4, 12))
        if is_64x64:
            bp.drawImage(QRect(ox + 4 * S, oy + 20 * S, 4 * S, 12 * S), src, QRect(4, 36, 4, 12))

        # Left Leg Base (20, 52, 4, 12 if 64x64 else mirror)
        if is_64x64:
            bp.drawImage(QRect(ox + 8 * S, oy + 20 * S, 4 * S, 12 * S), src, QRect(20, 52, 4, 12))
            bp.drawImage(QRect(ox + 8 * S, oy + 20 * S, 4 * S, 12 * S), src, QRect(4, 52, 4, 12))
        else:
            bp.drawImage(QRect(ox + 8 * S, oy + 20 * S, 4 * S, 12 * S), src, QRect(4, 20, 4, 12))

        bp.end()
        body_img.save(str(body_out), "PNG")

        return str(body_out), str(avatar_out)
    except Exception as e:
        print(f"[SkinRenderer] Error rendering skin: {e}")
        return "", ""


def add_skin_to_history(username: str, path: str, preview_url: str = "") -> list[dict]:
    history = get_skin_history()
    u_clean = (username or "").strip().lower()
    p_clean = str(path or "").strip().lower()

    def is_dup(item: dict) -> bool:
        item_u = str(item.get("username", "")).strip().lower()
        item_p = str(item.get("path", "")).strip().lower()
        if u_clean and item_u and item_u == u_clean:
            return True
        if p_clean and item_p and item_p == p_clean:
            return True
        return False

    # If preview_url is not set and path is a local file, generate rendered preview
    if (not preview_url or "mc-heads.net" in preview_url) and path and Path(path).exists():
        body_p, av_p = generate_skin_renders(path)
        if av_p:
            preview_url = "file:///" + str(Path(av_p)).replace("\\", "/")

    history = [h for h in history if not is_dup(h)]
    history.insert(0, {
        "username": (username or "Custom Skin").strip(),
        "path": path or "",
        "previewUrl": preview_url or f"https://mc-heads.net/avatar/{username}/64"
    })
    history = history[:16]  # keep up to 16 recent skins
    try:
        (get_skins_dir() / "history.json").write_text(json.dumps(history, indent=2), "utf-8")
    except Exception:
        pass
    return history


def fetch_skin_by_username(username: str) -> tuple[bool, str, str]:
    """
    Downloads full skin texture PNG for any given Minecraft player username.
    Returns (success, local_path, preview_url)
    """
    name = username.strip()
    if not name:
        return False, "", "Kein Spielername angegeben."

    target_png = get_skins_dir() / f"{name}.png"
    urls = [
        f"https://minotar.net/skin/{name}",
        f"https://mc-heads.net/download/{name}",
        f"https://crafatar.com/skins/{name}"
    ]

    for url in urls:
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "EzClient/1.0.8"})
            with urllib.request.urlopen(req, timeout=8) as resp:
                if resp.getcode() == 200:
                    content = resp.read()
                    if len(content) > 500:
                        target_png.write_bytes(content)
                        preview_url = f"https://mc-heads.net/body/{name}/360"
                        add_skin_to_history(name, str(target_png), preview_url)
                        return True, str(target_png), preview_url
        except Exception:
            continue

    return False, "", f"Skin für '{name}' konnte nicht gefunden werden."
