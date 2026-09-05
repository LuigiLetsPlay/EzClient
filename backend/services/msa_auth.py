import os
import sys
import json
import time
import urllib.request
import urllib.parse
from pathlib import Path
from dataclasses import dataclass, asdict
from typing import Optional, List
from backend.services.minecraft import minecraft_dir
from backend.models.types import STATE_PATH

CACHE_FILE = STATE_PATH.parent / "auth_cache.json"
ACCOUNTS_FILE = STATE_PATH.parent / "accounts.json"

MICROSOFT_CLIENT_ID = "00000000402b5328"
MICROSOFT_REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf"
MICROSOFT_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL"

MICROSOFT_AUTH_URL = (
    f"https://login.live.com/oauth20_authorize.srf"
    f"?client_id={MICROSOFT_CLIENT_ID}"
    f"&response_type=code"
    f"&redirect_uri={urllib.parse.quote(MICROSOFT_REDIRECT_URI)}"
    f"&scope={urllib.parse.quote(MICROSOFT_SCOPE)}"
    f"&prompt=select_account"
)

@dataclass
class MinecraftSession:
    username: str
    uuid: str
    access_token: str
    user_type: str = "msa"
    skin_url: str = ""
    cape_url: str = ""
    refresh_token: str = ""
    expires_at: float = 0.0
    is_online: bool = False

def _decrypt_dpapi(raw_bytes: bytes) -> Optional[bytes]:
    """Decrypts Windows DPAPI encrypted data (used in launcher_msa_credentials.bin)."""
    if not sys.platform.startswith("win"):
        return None
    try:
        import ctypes
        from ctypes import wintypes

        class DATA_BLOB(ctypes.Structure):
            _fields_ = [
                ("cbData", wintypes.DWORD),
                ("pbData", ctypes.POINTER(ctypes.c_byte))
            ]

        in_blob = DATA_BLOB(len(raw_bytes), (ctypes.c_byte * len(raw_bytes)).from_buffer_copy(raw_bytes))
        out_blob = DATA_BLOB()

        crypt32 = ctypes.windll.crypt32
        if crypt32.CryptUnprotectData(ctypes.byref(in_blob), None, None, None, None, 0, ctypes.byref(out_blob)):
            dec_bytes = bytes((ctypes.c_byte * out_blob.cbData).from_address(ctypes.addressof(out_blob.pbData.contents)))
            ctypes.windll.kernel32.LocalFree(out_blob.pbData)
            return dec_bytes
    except Exception as e:
        print(f"[MsaAuth] DPAPI Decrypt error: {e}")
    return None

def _extract_refresh_token_from_bin(mc_dir: Path) -> Optional[str]:
    """Extracts the Microsoft OAuth refresh token from launcher_msa_credentials.bin."""
    bin_path = mc_dir / "launcher_msa_credentials.bin"
    if not bin_path.exists():
        return None

    try:
        raw = bin_path.read_bytes()
        dec = _decrypt_dpapi(raw)
        if not dec:
            return None

        data = json.loads(dec.decode("utf-8"))
        xuid = data.get("activeUserXuid")
        creds = data.get("credentials", {})
        
        search_dicts = []
        if xuid and xuid in creds:
            search_dicts.append(creds[xuid])
        search_dicts.extend([v for k, v in creds.items() if k != xuid])

        for cred_dict in search_dicts:
            for k, v in cred_dict.items():
                if "Msa" in k:
                    try:
                        sub = json.loads(v) if isinstance(v, str) else v
                        rt = sub.get("refresh_token")
                        if rt:
                            return rt
                    except Exception:
                        pass
    except Exception as e:
        print(f"[MsaAuth] Error parsing credentials.bin: {e}")
    return None

def _exchange_msa_token_to_minecraft(msa_token: str, refresh_token: str = "") -> Optional[MinecraftSession]:
    """Exchanges an MSA token for Xbox Live, XSTS, Minecraft token, and Mojang profile."""
    try:
        # Step 2: Xbox User Authentication
        xbl_req = urllib.request.Request(
            "https://user.auth.xboxlive.com/user/authenticate",
            data=json.dumps({
                "Properties": {
                    "AuthMethod": "RPS",
                    "SiteName": "user.auth.xboxlive.com",
                    "RpsTicket": msa_token
                },
                "RelyingParty": "http://auth.xboxlive.com",
                "TokenType": "JWT"
            }).encode("utf-8"),
            headers={"Content-Type": "application/json", "Accept": "application/json"}
        )
        with urllib.request.urlopen(xbl_req, timeout=12) as resp:
            xbl_data = json.loads(resp.read().decode("utf-8"))
            xbl_token = xbl_data.get("Token")
            xui = xbl_data.get("DisplayClaims", {}).get("xui", [{}])
            uhs = xui[0].get("uhs") if xui else None
            if not xbl_token or not uhs:
                return None

        # Step 3: XSTS Token for Minecraft Services
        xsts_req = urllib.request.Request(
            "https://xsts.auth.xboxlive.com/xsts/authorize",
            data=json.dumps({
                "Properties": {
                    "SandboxId": "RETAIL",
                    "UserTokens": [xbl_token]
                },
                "RelyingParty": "rp://api.minecraftservices.com/",
                "TokenType": "JWT"
            }).encode("utf-8"),
            headers={"Content-Type": "application/json", "Accept": "application/json"}
        )
        with urllib.request.urlopen(xsts_req, timeout=12) as resp:
            xsts_data = json.loads(resp.read().decode("utf-8"))
            xsts_token = xsts_data.get("Token")
            if not xsts_token:
                return None

        # Step 4: Login with Xbox to Mojang Minecraft Services
        mc_req = urllib.request.Request(
            "https://api.minecraftservices.com/authentication/login_with_xbox",
            data=json.dumps({"identityToken": f"XBL3.0 x={uhs};{xsts_token}"}).encode("utf-8"),
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(mc_req, timeout=12) as resp:
            mc_data = json.loads(resp.read().decode("utf-8"))
            mc_token = mc_data.get("access_token")
            expires_in = mc_data.get("expires_in", 86400)
            if not mc_token:
                return None

        # Step 5: Fetch Minecraft Profile & Skin
        prof_req = urllib.request.Request(
            "https://api.minecraftservices.com/minecraft/profile",
            headers={"Authorization": f"Bearer {mc_token}"}
        )
        with urllib.request.urlopen(prof_req, timeout=12) as resp:
            prof_data = json.loads(resp.read().decode("utf-8"))
            name = prof_data.get("name", "Player")
            uuid_val = prof_data.get("id", "")
            skins = prof_data.get("skins", [])
            skin_url = skins[0].get("url", "") if skins else ""
            capes = prof_data.get("capes", [])
            cape_url = next((cape.get("url", "") for cape in capes if cape.get("state", "ACTIVE") == "ACTIVE"), "")

            session = MinecraftSession(
                username=name,
                uuid=uuid_val,
                access_token=mc_token,
                user_type="msa",
                skin_url=skin_url,
                cape_url=cape_url,
                refresh_token=refresh_token,
                expires_at=time.time() + float(expires_in) - 60,
                is_online=True
            )
            # Save to cache
            _save_cached_session(session)
            print(f"[MsaAuth] Successfully authenticated online session for {name} ({uuid_val})")
            return session

    except Exception as e:
        print(f"[MsaAuth] Xbox/Mojang exchange error: {e}")
        return None

def _refresh_minecraft_token(refresh_token: str) -> Optional[MinecraftSession]:
    """Exchanges an MSA refresh token for fresh Xbox Live & Mojang Minecraft access tokens."""
    try:
        token_url = "https://login.live.com/oauth20_token.srf"
        payload = urllib.parse.urlencode({
            "client_id": MICROSOFT_CLIENT_ID,
            "refresh_token": refresh_token,
            "grant_type": "refresh_token",
            "scope": MICROSOFT_SCOPE
        }).encode("utf-8")
        
        req = urllib.request.Request(token_url, data=payload, headers={"Content-Type": "application/x-www-form-urlencoded"})
        with urllib.request.urlopen(req, timeout=12) as resp:
            msa_data = json.loads(resp.read().decode("utf-8"))
            msa_token = msa_data.get("access_token")
            new_refresh = msa_data.get("refresh_token") or refresh_token
            if not msa_token:
                return None

        return _exchange_msa_token_to_minecraft(msa_token, new_refresh)

    except Exception as e:
        print(f"[MsaAuth] Token refresh error: {e}")
    return None

def authenticate_with_authorization_code(code: str) -> Optional[MinecraftSession]:
    """Exchanges a Microsoft OAuth authorization code for Minecraft session tokens."""
    try:
        token_url = "https://login.live.com/oauth20_token.srf"
        payload = urllib.parse.urlencode({
            "client_id": MICROSOFT_CLIENT_ID,
            "code": code,
            "grant_type": "authorization_code",
            "redirect_uri": MICROSOFT_REDIRECT_URI,
            "scope": MICROSOFT_SCOPE
        }).encode("utf-8")

        req = urllib.request.Request(token_url, data=payload, headers={"Content-Type": "application/x-www-form-urlencoded"})
        with urllib.request.urlopen(req, timeout=12) as resp:
            msa_data = json.loads(resp.read().decode("utf-8"))
            msa_token = msa_data.get("access_token")
            refresh_token = msa_data.get("refresh_token", "")
            if not msa_token:
                return None

        return _exchange_msa_token_to_minecraft(msa_token, refresh_token)

    except Exception as e:
        print(f"[MsaAuth] Auth code login error: {e}")
        return None

def _load_cached_session() -> Optional[MinecraftSession]:
    """Loads session from local cache if not expired."""
    if not CACHE_FILE.exists():
        return None
    try:
        data = json.loads(CACHE_FILE.read_text(encoding="utf-8"))
        expires_at = data.get("expires_at", 0)
        # Give 5 minute buffer
        if expires_at > time.time() + 300 and data.get("access_token"):
            return MinecraftSession(**data)
    except Exception:
        pass
    return None

def _save_cached_session(session: MinecraftSession) -> None:
    try:
        CACHE_FILE.parent.mkdir(parents=True, exist_ok=True)
        CACHE_FILE.write_text(json.dumps(asdict(session), indent=2), encoding="utf-8")
        accounts = {}
        if ACCOUNTS_FILE.is_file():
            raw = json.loads(ACCOUNTS_FILE.read_text(encoding="utf-8"))
            accounts = raw if isinstance(raw, dict) else {}
        accounts[session.uuid] = asdict(session)
        ACCOUNTS_FILE.write_text(json.dumps(accounts, indent=2), encoding="utf-8")
    except Exception:
        pass

def list_saved_accounts() -> list[dict]:
    try:
        raw = json.loads(ACCOUNTS_FILE.read_text(encoding="utf-8")) if ACCOUNTS_FILE.is_file() else {}
        if not isinstance(raw, dict):
            raw = {}
        active_uuid = ""
        if CACHE_FILE.is_file():
            cached = json.loads(CACHE_FILE.read_text(encoding="utf-8"))
            active_uuid = str(cached.get("uuid", ""))
            # Transparently migrate the former single-account cache.
            if active_uuid and active_uuid not in raw:
                raw[active_uuid] = cached
                ACCOUNTS_FILE.write_text(json.dumps(raw, indent=2), encoding="utf-8")
        return [
            {
                "uuid": uuid_value,
                "username": str(data.get("username", "Player")),
                "skinUrl": str(data.get("skin_url", "")),
                "capeUrl": str(data.get("cape_url", "")),
                "active": uuid_value == active_uuid,
            }
            for uuid_value, data in raw.items() if isinstance(data, dict)
        ]
    except (OSError, ValueError, TypeError, json.JSONDecodeError):
        return []

def activate_saved_account(uuid_value: str) -> Optional[MinecraftSession]:
    try:
        raw = json.loads(ACCOUNTS_FILE.read_text(encoding="utf-8"))
        data = raw.get(uuid_value) if isinstance(raw, dict) else None
        if not isinstance(data, dict):
            return None
        session = MinecraftSession(**data)
        if session.expires_at <= time.time() + 300 and session.refresh_token:
            refreshed = _refresh_minecraft_token(session.refresh_token)
            if refreshed:
                return refreshed
        CACHE_FILE.write_text(json.dumps(asdict(session), indent=2), encoding="utf-8")
        return session
    except (OSError, ValueError, TypeError, json.JSONDecodeError):
        return None

def logout_account() -> None:
    """Clears cached account session."""
    try:
        if CACHE_FILE.exists():
            CACHE_FILE.unlink()
    except Exception:
        pass

def remove_saved_account(uuid_value: str) -> bool:
    """Removes one saved account and clears the active session when it matches."""
    try:
        raw = json.loads(ACCOUNTS_FILE.read_text(encoding="utf-8")) if ACCOUNTS_FILE.is_file() else {}
        if not isinstance(raw, dict) or uuid_value not in raw:
            return False
        raw.pop(uuid_value, None)
        ACCOUNTS_FILE.write_text(json.dumps(raw, indent=2), encoding="utf-8")
        if CACHE_FILE.is_file():
            active = json.loads(CACHE_FILE.read_text(encoding="utf-8"))
            if str(active.get("uuid", "")) == uuid_value:
                CACHE_FILE.unlink(missing_ok=True)
        return True
    except (OSError, ValueError, TypeError, json.JSONDecodeError):
        return False

def _fallback_offline_account(mc_dir: Path) -> MinecraftSession:
    """Reads basic profile info from launcher_accounts.json for offline fallback."""
    for fname in ["launcher_accounts.json", "launcher_accounts_microsoft_store.json", "launcher_profiles.json"]:
        fpath = mc_dir / fname
        if fpath.exists():
            try:
                data = json.loads(fpath.read_text(encoding="utf-8"))
                active_id = data.get("activeAccountLocalId")
                accounts = data.get("accounts", {})
                if active_id and active_id in accounts:
                    p = accounts[active_id].get("minecraftProfile", {})
                    name = p.get("name")
                    uuid_val = p.get("id") or active_id
                    if name:
                        return MinecraftSession(
                            username=name,
                            uuid=uuid_val,
                            access_token="0",
                            user_type="msa",
                            is_online=False
                        )
                if accounts:
                    acc = next(iter(accounts.values()))
                    p = acc.get("minecraftProfile", {})
                    name = p.get("name") or acc.get("username")
                    if name:
                        return MinecraftSession(
                            username=name,
                            uuid=p.get("id", "00000000000000000000000000000000"),
                            access_token="0",
                            user_type="msa",
                            is_online=False
                        )
            except Exception:
                pass

    return MinecraftSession(
        username=os.environ.get("USERNAME", "Player"),
        uuid="00000000000000000000000000000000",
        access_token="0",
        user_type="msa",
        is_online=False
    )

def get_minecraft_session(force_refresh: bool = False) -> MinecraftSession:
    """
    Main entry point: Resolves a valid Minecraft session.
    1. Checks cached valid token.
    2. Decrypts credentials from .minecraft or stored refresh token and refreshes token with Mojang.
    3. Falls back to offline account info if offline.
    """
    mc_dir = minecraft_dir()

    if not force_refresh:
        cached = _load_cached_session()
        if cached:
            return cached

    # 1. Try refreshing from cached refresh token
    if CACHE_FILE.exists():
        try:
            cached_data = json.loads(CACHE_FILE.read_text(encoding="utf-8"))
            cached_rt = cached_data.get("refresh_token")
            if cached_rt:
                session = _refresh_minecraft_token(cached_rt)
                if session:
                    return session
        except Exception:
            pass

    # 2. Attempt token refresh from launcher credentials in .minecraft
    refresh_token = _extract_refresh_token_from_bin(mc_dir)
    if refresh_token:
        session = _refresh_minecraft_token(refresh_token)
        if session:
            return session

    # Return cached even if slightly old if offline
    if CACHE_FILE.exists():
        try:
            data = json.loads(CACHE_FILE.read_text(encoding="utf-8"))
            if data.get("username"):
                return MinecraftSession(**data)
        except Exception:
            pass

    return _fallback_offline_account(mc_dir)
