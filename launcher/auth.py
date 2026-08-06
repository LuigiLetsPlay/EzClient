"""Microsoft-Login - Offline Dummy fuer Entwicklung."""

import json
from pathlib import Path


AUTH_CACHE_DIR = Path.home() / "AppData" / "Roaming" / "EzClient"
AUTH_CACHE_FILE = AUTH_CACHE_DIR / "auth.json"


def login(on_code_callback):
    """
    DEVELOPMENT: Dummy-Login, speichert einen Test-Spieler.

    Fuer Production: Nutze Microsoft-Auth hier.
    """
    profile = {
        "name": "TestSpieler",
        "uuid": "00000000-0000-0000-0000-000000000000",
        "access_token": "test_token_12345",
        "refresh_token": "test_refresh_token",
    }

    _save_cache(profile)
    return profile


def load_cached_profile():
    """Lade gecachtes Profil."""
    if not AUTH_CACHE_FILE.exists():
        return None
    try:
        with open(AUTH_CACHE_FILE) as f:
            return json.load(f)
    except (json.JSONDecodeError, IOError):
        return None


def _save_cache(profile):
    """Speichere Profil im Cache."""
    AUTH_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    with open(AUTH_CACHE_FILE, "w") as f:
        json.dump(profile, f)


def logout():
    """Loesche Profil-Cache."""
    if AUTH_CACHE_FILE.exists():
        AUTH_CACHE_FILE.unlink()


def _xbox_authenticate(access_token):
    """Authentifiziere gegen Xbox Live."""
    url = "https://user.auth.xboxlive.com/user/authenticate"
    headers = {"Content-Type": "application/json"}
    payload = {
        "Properties": {
            "AuthMethod": "RPS",
            "SiteName": "user.auth.xboxlive.com",
            "RpsTicket": f"d={access_token}",
        },
        "RelyingParty": "http://auth.xboxlive.com",
        "TokenType": "JWT",
    }
    resp = requests.post(url, json=payload, headers=headers)
    resp.raise_for_status()
    return resp.json()["Token"]


def _xsts_authorize(xbl_token):
    """Hole XSTS-Token fuer Minecraft."""
    url = "https://xsts.auth.xboxlive.com/xsts/authorize"
    headers = {"Content-Type": "application/json"}
    payload = {
        "Properties": {
            "SandboxId": "RETAIL",
            "UserTokens": [xbl_token],
        },
        "RelyingParty": "rp://api.minecraftservices.com/",
        "TokenType": "JWT",
    }
    resp = requests.post(url, json=payload, headers=headers)

    if resp.status_code != 200:
        data = resp.json()
        xerr = data.get("XErr")
        if xerr == "2148916238":  # "Account doesn't have an Xbox"
            raise ValueError("Xbox-Konto erforderlich: Erstelle ein Xbox-Profil unter xbox.com")
        elif xerr == "2148916233":  # "Country not supported"
            raise ValueError("Regionseinschraenkung: Minecraft in diesem Land nicht verfuegbar.")
        else:
            raise ValueError(f"XSTS-Fehler {xerr}: {data.get('Message')}")

    return resp.json()["Token"]


def _minecraft_login(xsts_token):
    """Hole Minecraft-Session-Token und Profil."""
    url = "https://api.minecraftservices.com/authentication/login_with_xbox"
    headers = {"Content-Type": "application/json"}
    payload = {"identityToken": f"XBL3.0 x={xsts_token}"}

    resp = requests.post(url, json=payload, headers=headers)

    if resp.status_code == 403:
        raise ValueError(
            "Client-ID nicht freigegeben fuer Minecraft API. "
            "Beantrage Zugriff unter aka.ms/mce-reviewappid"
        )
    resp.raise_for_status()
    return resp.json()


def _get_minecraft_profile(access_token):
    """Hole Spieler-Profil (Name, UUID)."""
    url = "https://api.minecraftservices.com/minecraft/profile"
    headers = {"Authorization": f"Bearer {access_token}"}

    resp = requests.get(url, headers=headers)

    if resp.status_code == 404:
        raise ValueError("Minecraft nicht gekauft. Kaufe Minecraft Java Edition unter minecraft.net")
    resp.raise_for_status()

    data = resp.json()
    return {
        "name": data.get("name"),
        "uuid": data.get("id"),
        "access_token": access_token,
    }


def login(on_code_callback):
    """
    Starte Device-Code-Login-Flow.

    on_code_callback(user_code, verification_uri) wird aufgerufen mit dem Code,
    den der Spieler eingeben muss, und der URL zum Bestaetigen.

    Gibt {name, uuid, access_token, refresh_token} zurueck.
    """
    device_resp = _get_device_code()
    user_code = device_resp["user_code"]
    verification_uri = device_resp["verification_uri"]
    device_code = device_resp["device_code"]

    on_code_callback(user_code, verification_uri)

    token_resp = _get_token(device_code)
    ms_access_token = token_resp["access_token"]
    refresh_token = token_resp.get("refresh_token", "")

    xbl_token = _xbox_authenticate(ms_access_token)
    xsts_token = _xsts_authorize(xbl_token)
    mc_login = _minecraft_login(xsts_token)

    profile = _get_minecraft_profile(mc_login["access_token"])
    profile["refresh_token"] = refresh_token

    _save_cache(profile)
    return profile


def load_cached_profile():
    """Lade gecachtes Profil, oder None wenn nicht vorhanden/abgelaufen."""
    if not AUTH_CACHE_FILE.exists():
        return None

    try:
        with open(AUTH_CACHE_FILE) as f:
            return json.load(f)
    except (json.JSONDecodeError, IOError):
        return None


def _save_cache(profile):
    """Speichere Profil im Cache."""
    AUTH_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    with open(AUTH_CACHE_FILE, "w") as f:
        json.dump(profile, f)


def logout():
    """Loesche gecachtes Profil."""
    if AUTH_CACHE_FILE.exists():
        AUTH_CACHE_FILE.unlink()
