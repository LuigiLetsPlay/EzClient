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
                "User-Agent": "EzClient/1.0.3"
            },
            method="DELETE"
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            return True, "Skin auf Standard zurückgesetzt."
    except Exception as e:
        return False, f"Fehler beim Zurücksetzen: {e}"
