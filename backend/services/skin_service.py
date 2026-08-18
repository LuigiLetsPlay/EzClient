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


def add_skin_to_history(username: str, path: str, preview_url: str = "") -> list[dict]:
    history = get_skin_history()
    # Filter duplicate
    history = [h for h in history if h.get("username", "").lower() != username.lower() and h.get("path") != path]
    history.insert(0, {
        "username": username,
        "path": path,
        "previewUrl": preview_url or f"https://mc-heads.net/avatar/{username}/64"
    })
    history = history[:12]  # keep up to 12 recent skins
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
