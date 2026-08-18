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


def get_active_skin() -> dict:
    f = get_skins_dir() / "active_skin.json"
    if f.exists():
        try:
            return json.loads(f.read_text("utf-8"))
        except Exception:
            return {}
    return {}


def set_active_skin(name: str, path: str, body_url: str, avatar_url: str) -> dict:
    data = {
        "name": name,
        "path": path,
        "bodyUrl": body_url,
        "avatarUrl": avatar_url
    }
    try:
        (get_skins_dir() / "active_skin.json").write_text(json.dumps(data, indent=2), "utf-8")
    except Exception:
        pass
    return data


def generate_skin_renders(skin_path: str | Path) -> tuple[str, str]:
    """
    Renders both a full 3D isometric character body preview and a 64x64 head avatar from a raw skin texture PNG.
    Returns (body_preview_path, avatar_preview_path).
    """
    path = Path(skin_path)
    if not path.exists() or not path.is_file():
        return "", ""

    try:
        from PySide6.QtGui import QImage, QPainter, QPolygonF, QTransform, QColor
        from PySide6.QtCore import Qt, QPointF, QRect
        import math

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

        # 2. Render Full 3D Isometric Character (Width: 280, Height: 420)
        body_img = QImage(280, 420, QImage.Format_ARGB32_Premultiplied)
        body_img.fill(Qt.transparent)
        bp = QPainter(body_img)
        bp.setRenderHint(QPainter.Antialiasing, True)
        bp.setRenderHint(QPainter.SmoothPixmapTransform, False)

        def draw_textured_quad(src_img, src_rect, p0, p1, p2, p3, brightness=1.0):
            if src_rect.width() <= 0 or src_rect.height() <= 0:
                return
            cropped = src_img.copy(src_rect)
            w, h = src_rect.width(), src_rect.height()
            quad_src = QPolygonF([QPointF(0, 0), QPointF(w, 0), QPointF(w, h), QPointF(0, h)])
            quad_dst = QPolygonF([p0, p1, p2, p3])

            t = QTransform()
            if QTransform.quadToQuad(quad_src, quad_dst, t):
                bp.save()
                bp.setTransform(t, True)
                bp.drawImage(0, 0, cropped)
                if brightness < 0.99:
                    bp.setCompositionMode(QPainter.CompositionMode_Darken)
                    bp.fillRect(0, 0, w, h, QColor(0, 0, 0, int((1.0 - brightness) * 255)))
                bp.restore()

        def draw_3d_box(src_img, top_r, front_r, side_r, top_p, front_p, side_p, b_top=1.0, b_front=0.92, b_side=0.72):
            if top_r:
                draw_textured_quad(src_img, top_r, top_p[0], top_p[1], top_p[2], top_p[3], b_top)
            if front_r:
                draw_textured_quad(src_img, front_r, front_p[0], front_p[1], front_p[2], front_p[3], b_front)
            if side_r:
                draw_textured_quad(src_img, side_r, side_p[0], side_p[1], side_p[2], side_p[3], b_side)

        S = 8.5
        ang = math.radians(24.0)
        ux, uy = math.cos(ang) * S, math.sin(ang) * S
        vx, vy = -math.cos(ang) * S * 0.72, math.sin(ang) * S * 0.72

        def make_box_points(bx, by, w, h, d, z_off=0):
            p0 = QPointF(bx + z_off * vx, by + z_off * vy)
            p1 = QPointF(p0.x() + w * ux, p0.y() + w * uy)
            p2 = QPointF(p1.x() + d * vx, p1.y() + d * vy)
            p3 = QPointF(p0.x() + d * vx, p0.y() + d * vy)
            f0 = p0
            f1 = p1
            f2 = QPointF(f1.x(), f1.y() + h * S * 1.25)
            f3 = QPointF(f0.x(), f0.y() + h * S * 1.25)
            s0 = p3
            s1 = p0
            s2 = f3
            s3 = QPointF(s0.x(), s0.y() + h * S * 1.25)
            return (p0, p1, p2, p3), (f0, f1, f2, f3), (s0, s1, s2, s3)

        cx, cy = 135.0, 115.0

        # Torso & Jacket
        t_top, t_front, t_side = make_box_points(cx, cy, 8, 12, 4)
        draw_3d_box(src, QRect(20, 16, 8, 4), QRect(20, 20, 8, 12), QRect(16, 20, 4, 12), t_top, t_front, t_side)
        if is_64x64:
            draw_3d_box(src, QRect(20, 32, 8, 4), QRect(20, 36, 8, 12), QRect(16, 36, 4, 12), t_top, t_front, t_side)

        # Right Arm (Front)
        r_top, r_front, r_side = make_box_points(cx - 4 * ux - 0.5 * vx, cy - 4 * uy - 0.5 * vy + 2, 4, 12, 4)
        draw_3d_box(src, QRect(44, 16, 4, 4), QRect(44, 20, 4, 12), QRect(40, 20, 4, 12), r_top, r_front, r_side)
        if is_64x64:
            draw_3d_box(src, QRect(44, 32, 4, 4), QRect(44, 36, 4, 12), QRect(40, 36, 4, 12), r_top, r_front, r_side)

        # Left Arm (Back)
        l_top, l_front, l_side = make_box_points(cx + 8 * ux, cy + 8 * uy + 2, 4, 12, 4)
        if is_64x64:
            draw_3d_box(src, QRect(36, 48, 4, 4), QRect(36, 52, 4, 12), QRect(32, 52, 4, 12), l_top, l_front, l_side)
            draw_3d_box(src, QRect(52, 48, 4, 4), QRect(52, 52, 4, 12), QRect(48, 52, 4, 12), l_top, l_front, l_side)
        else:
            draw_3d_box(src, QRect(44, 16, 4, 4), QRect(44, 20, 4, 12), QRect(40, 20, 4, 12), l_top, l_front, l_side)

        # Right Leg
        rl_top, rl_front, rl_side = make_box_points(cx + 0.5 * ux, cy + 12 * S * 1.25, 4, 12, 4)
        draw_3d_box(src, None, QRect(4, 20, 4, 12), QRect(0, 20, 4, 12), rl_top, rl_front, rl_side)
        if is_64x64:
            draw_3d_box(src, None, QRect(4, 36, 4, 12), QRect(0, 36, 4, 12), rl_top, rl_front, rl_side)

        # Left Leg
        ll_top, ll_front, ll_side = make_box_points(cx + 4.5 * ux, cy + 4 * uy + 12 * S * 1.25, 4, 12, 4)
        if is_64x64:
            draw_3d_box(src, None, QRect(20, 52, 4, 12), QRect(16, 52, 4, 12), ll_top, ll_front, ll_side)
            draw_3d_box(src, None, QRect(4, 52, 4, 12), QRect(0, 52, 4, 12), ll_top, ll_front, ll_side)
        else:
            draw_3d_box(src, None, QRect(4, 20, 4, 12), QRect(0, 20, 4, 12), ll_top, ll_front, ll_side)

        # Head + Hat (3D isometric box)
        hx, hy = cx + 0.5 * ux + 0.5 * vx, cy - 8 * S * 1.25 + 14
        h_top, h_front, h_side = make_box_points(hx, hy, 8, 8, 8)
        draw_3d_box(src, QRect(8, 0, 8, 8), QRect(8, 8, 8, 8), QRect(0, 8, 8, 8), h_top, h_front, h_side)
        draw_3d_box(src, QRect(40, 0, 8, 8), QRect(40, 8, 8, 8), QRect(32, 8, 8, 8), h_top, h_front, h_side)

        bp.end()
        body_img.save(str(body_out), "PNG")

        return str(body_out), str(avatar_out)
    except Exception as e:
        print(f"[SkinRenderer] Error rendering 3D skin: {e}")
        return "", ""


def get_saved_skins() -> list[dict]:
    saved_file = get_skins_dir() / "saved_skins.json"
    if saved_file.exists():
        try:
            return json.loads(saved_file.read_text("utf-8"))
        except Exception:
            return []
    return []


def save_skin_to_library(name: str, path: str, preview_url: str = "") -> list[dict]:
    skins = get_saved_skins()
    clean_name = (name or "").strip()
    if not clean_name:
        clean_name = "Mein Skin"

    # If preview_url is not set and path is a local file, generate rendered preview
    if not preview_url and path and Path(path).exists():
        body_p, av_p = generate_skin_renders(path)
        if av_p:
            preview_url = "file:///" + str(Path(av_p)).replace("\\", "/")

    skin_id = f"skin_{int(time.time())}_{len(skins)}"
    # Deduplicate by name
    skins = [s for s in skins if s.get("name", "").lower() != clean_name.lower()]
    skins.insert(0, {
        "id": skin_id,
        "name": clean_name,
        "path": path or "",
        "previewUrl": preview_url or f"https://mc-heads.net/avatar/{clean_name}/64",
        "savedAt": int(time.time())
    })
    try:
        (get_skins_dir() / "saved_skins.json").write_text(json.dumps(skins, indent=2), "utf-8")
    except Exception:
        pass
    return skins


def delete_saved_skin_from_library(skin_id_or_name: str) -> list[dict]:
    skins = get_saved_skins()
    target = (skin_id_or_name or "").strip().lower()
    skins = [s for s in skins if s.get("id", "").lower() != target and s.get("name", "").lower() != target]
    try:
        (get_skins_dir() / "saved_skins.json").write_text(json.dumps(skins, indent=2), "utf-8")
    except Exception:
        pass
    return skins


def get_skin_history() -> list[dict]:
    hist_file = get_skins_dir() / "history.json"
    if hist_file.exists():
        try:
            return json.loads(hist_file.read_text("utf-8"))
        except Exception:
            return []
    return []


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
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }

    # 1. Primary: playerdb.co API (High-speed official texture lookup)
    try:
        req = urllib.request.Request(f"https://playerdb.co/api/player/minecraft/{name}", headers=headers)
        with urllib.request.urlopen(req, timeout=6) as resp:
            if resp.getcode() == 200:
                data = json.loads(resp.read().decode("utf-8", errors="replace"))
                skin_tex_url = data.get("data", {}).get("player", {}).get("skin_texture")
                if skin_tex_url:
                    req_tex = urllib.request.Request(skin_tex_url, headers=headers)
                    with urllib.request.urlopen(req_tex, timeout=6) as r_tex:
                        content = r_tex.read()
                        if len(content) > 300:
                            target_png.write_bytes(content)
                            body_p, av_p = generate_skin_renders(target_png)
                            preview_url = ("file:///" + str(Path(body_p)).replace("\\", "/")) if body_p else f"https://mc-heads.net/body/{name}/360"
                            add_skin_to_history(name, str(target_png), preview_url)
                            return True, str(target_png), preview_url
    except Exception:
        pass

    # 2. Fallbacks: Minotar, mc-heads, Crafatar
    urls = [
        f"https://minotar.net/skin/{name}",
        f"https://mc-heads.net/download/{name}",
        f"https://crafatar.com/skins/{name}"
    ]

    for url in urls:
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=6) as resp:
                if resp.getcode() == 200:
                    content = resp.read()
                    if len(content) > 300:
                        target_png.write_bytes(content)
                        body_p, av_p = generate_skin_renders(target_png)
                        preview_url = ("file:///" + str(Path(body_p)).replace("\\", "/")) if body_p else f"https://mc-heads.net/body/{name}/360"
                        add_skin_to_history(name, str(target_png), preview_url)
                        return True, str(target_png), preview_url
        except Exception:
            continue

    return False, "", f"Skin für '{name}' konnte nicht gefunden werden."
