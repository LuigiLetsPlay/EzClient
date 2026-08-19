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
                "User-Agent": "EzClient/1.1.9"
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
                "User-Agent": "EzClient/1.1.9"
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
    Renders both a pixel-perfect assembled Minecraft character body preview and a 64x64 head avatar from a raw skin texture PNG.
    Returns (body_preview_path, avatar_preview_path).
    """
    path = Path(skin_path)
    if not path.exists() or not path.is_file():
        return "", ""

    try:
        from PIL import Image, ImageEnhance

        skin = Image.open(str(path)).convert("RGBA")
        is_64x64 = skin.height >= 64

        previews_dir = get_skins_dir() / "renders"
        previews_dir.mkdir(parents=True, exist_ok=True)
        body_out = previews_dir / f"{path.stem}_body.png"
        avatar_out = previews_dir / f"{path.stem}_avatar.png"

        # 1. Render 64x64 Head Avatar
        av_img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        head_base = skin.crop((8, 8, 16, 16)).resize((64, 64), Image.Resampling.NEAREST)
        head_hat = skin.crop((40, 8, 48, 16)).resize((64, 64), Image.Resampling.NEAREST)
        av_img.alpha_composite(head_base, (0, 0))
        av_img.alpha_composite(head_hat, (0, 0))
        av_img.save(str(avatar_out), "PNG")

        # 2. Render Pixel-Perfect 3D-Stance Minecraft Character (280 x 420)
        canvas = Image.new("RGBA", (280, 420), (0, 0, 0, 0))
        S = 8.5

        def shade(img, factor):
            r, g, b, a = img.split()
            rgb = Image.merge("RGB", (r, g, b))
            shaded_rgb = ImageEnhance.Brightness(rgb).enhance(factor)
            sr, sg, sb = shaded_rgb.split()
            return Image.merge("RGBA", (sr, sg, sb, a))

        def draw_part(src_box, px, py, pw, ph, brightness=1.0, flip=False):
            part = skin.crop(src_box)
            if flip:
                part = part.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
            if brightness < 0.99:
                part = shade(part, brightness)
            scaled = part.resize((int(pw * S), int(ph * S)), Image.Resampling.NEAREST)
            canvas.alpha_composite(scaled, (int(px), int(py)))

        cx = 95
        cy = 35

        # 1. Left Arm (Behind)
        if is_64x64:
            draw_part((36, 52, 40, 64), cx + 76, cy + 76, 4, 12, 0.78)
            draw_part((52, 52, 56, 64), cx + 76, cy + 76, 4, 12, 0.78)
        else:
            draw_part((44, 20, 48, 32), cx + 76, cy + 76, 4, 12, 0.78, flip=True)

        # 2. Left Leg (Behind)
        if is_64x64:
            draw_part((20, 52, 24, 64), cx + 40, cy + 178, 4, 12, 0.82)
            draw_part((4, 52, 8, 64), cx + 40, cy + 178, 4, 12, 0.82)
        else:
            draw_part((4, 20, 8, 32), cx + 40, cy + 178, 4, 12, 0.82, flip=True)

        # 3. Right Leg (Front)
        draw_part((4, 20, 8, 32), cx + 8, cy + 178, 4, 12, 0.96)
        if is_64x64:
            draw_part((4, 36, 8, 48), cx + 8, cy + 178, 4, 12, 0.96)

        # 4. Torso (Center)
        draw_part((20, 20, 28, 32), cx + 8, cy + 76, 8, 12, 0.96)
        if is_64x64:
            draw_part((20, 36, 28, 48), cx + 8, cy + 76, 8, 12, 0.96)

        # 5. Right Arm (Front)
        draw_part((44, 20, 48, 32), cx - 26, cy + 76, 4, 12, 0.98)
        if is_64x64:
            draw_part((44, 36, 48, 48), cx - 26, cy + 76, 4, 12, 0.98)

        # 6. Head + Hat (Top)
        draw_part((8, 8, 16, 16), cx + 8, cy, 8, 8, 1.0)
        draw_part((40, 8, 48, 16), cx + 8, cy, 8, 8, 1.0)

        canvas.save(str(body_out), "PNG")
        return str(body_out), str(avatar_out)
    except Exception as e:
        print(f"[SkinRenderer] Error rendering skin: {e}")
        return "", ""


def extract_head_avatar_data_uri(skin_source: str | Path | bytes) -> str:
    """
    Extracts the 64x64 Minecraft head avatar (with outer hat layer) from skin bytes or file path
    and returns a base64 data URI string `data:image/png;base64,...`.
    Fast, in-memory, 100% in sync with whatever skin texture is active.
    """
    try:
        from PIL import Image
        import io
        import base64

        if isinstance(skin_source, bytes):
            skin = Image.open(io.BytesIO(skin_source)).convert("RGBA")
        elif isinstance(skin_source, (str, Path)):
            p = Path(skin_source)
            if not p.exists() or not p.is_file():
                return ""
            skin = Image.open(str(p)).convert("RGBA")
        else:
            return ""

        av_img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        head_base = skin.crop((8, 8, 16, 16)).resize((64, 64), Image.Resampling.NEAREST)
        head_hat = skin.crop((40, 8, 48, 16)).resize((64, 64), Image.Resampling.NEAREST)
        av_img.alpha_composite(head_base, (0, 0))
        av_img.alpha_composite(head_hat, (0, 0))

        buf = io.BytesIO()
        av_img.save(buf, format="PNG")
        b64 = base64.b64encode(buf.getvalue()).decode("ascii")
        return f"data:image/png;base64,{b64}"
    except Exception as e:
        print(f"[SkinRenderer] Error extracting head avatar data URI: {e}")
        return ""


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
                        return True, str(target_png), preview_url
        except Exception:
            continue

    return False, "", f"Skin für '{name}' konnte nicht gefunden werden."
