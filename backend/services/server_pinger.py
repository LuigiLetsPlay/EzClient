"""
EzClient Minecraft Server Pinger
Implements native Minecraft Server List Ping (SLP) protocol over TCP.
Retrieves online player count, max players, player sample names, favicon, and MOTD.
"""

import time
import json
import socket
import struct
import subprocess
import re
import threading
from typing import Dict, Any, Optional, Tuple, List
from backend.services.server_nbt import clean_minecraft_formatting

_STATUS_CACHE: Dict[str, Tuple[float, Dict[str, Any]]] = {}
_CACHE_LOCK = threading.Lock()


def encode_varint(val: int) -> bytes:
    """Encodes an integer into Minecraft VarInt wire format."""
    res = bytearray()
    while True:
        b = val & 0x7F
        val >>= 7
        if val:
            res.append(b | 0x80)
        else:
            res.append(b)
            break
    return bytes(res)


def read_varint(sock: socket.socket) -> int:
    """Reads a Minecraft VarInt from a socket."""
    val = 0
    for i in range(5):
        b = sock.recv(1)
        if not b:
            raise EOFError("Socket closed while reading VarInt")
        byte = b[0]
        val |= (byte & 0x7F) << (7 * i)
        if not (byte & 0x80):
            return val
    raise ValueError("VarInt too big")


def resolve_minecraft_srv(address: str, default_port: int = 25565) -> Tuple[str, int]:
    """
    Resolves host and port for a Minecraft address.
    If no port is given, queries DNS SRV record (_minecraft._tcp.<domain>) when on Windows.
    """
    address = address.strip()
    if ":" in address:
        parts = address.split(":", 1)
        host = parts[0].strip()
        try:
            port = int(parts[1].strip())
            return host, port
        except ValueError:
            return host, default_port

    host = address
    # If host is an IP address or localhost, skip SRV lookup
    if re.match(r"^\d+\.\d+\.\d+\.\d+$", host) or host.lower() in ("localhost", "127.0.0.1"):
        return host, default_port

    # Query SRV via Windows nslookup without flashing console windows
    try:
        cmd = ["nslookup", "-type=SRV", f"_minecraft._tcp.{host}"]
        creationflags = 0
        startupinfo = None
        if hasattr(subprocess, "CREATE_NO_WINDOW"):
            creationflags |= subprocess.CREATE_NO_WINDOW
        if hasattr(subprocess, "STARTF_USESHOWWINDOW"):
            startupinfo = subprocess.STARTUPINFO()
            startupinfo.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            startupinfo.wShowWindow = 0
        out = subprocess.check_output(
            cmd,
            text=True,
            timeout=1.5,
            stderr=subprocess.DEVNULL,
            creationflags=creationflags,
            startupinfo=startupinfo,
        )
        target = re.search(r"svr hostname\s*=\s*([^\s\r\n]+)", out, re.IGNORECASE)
        port_m = re.search(r"port\s*=\s*(\d+)", out, re.IGNORECASE)
        if target and port_m:
            resolved_host = target.group(1).rstrip(".")
            resolved_port = int(port_m.group(1))
            return resolved_host, resolved_port
    except Exception:
        pass

    return host, default_port


def extract_chat_component_text(component: Any) -> str:
    """Recursively extracts plain text from a Minecraft chat component."""
    if component is None:
        return ""
    if isinstance(component, str):
        return component
    if isinstance(component, (int, float, bool)):
        return str(component)
    if isinstance(component, list):
        return "".join(extract_chat_component_text(x) for x in component)
    if isinstance(component, dict):
        parts = []
        if "text" in component:
            parts.append(str(component["text"]))
        if "translate" in component:
            with_parts = component.get("with", [])
            if isinstance(with_parts, list):
                parts.append(" ".join(extract_chat_component_text(x) for x in with_parts))
            else:
                parts.append(str(component["translate"]))
        if "extra" in component and isinstance(component["extra"], list):
            for extra in component["extra"]:
                parts.append(extract_chat_component_text(extra))
        return "".join(parts)
    return ""


def ping_minecraft_server(address: str, timeout: float = 2.5) -> Dict[str, Any]:
    """
    Pings a Minecraft server and returns status information.
    """
    if not address or not address.strip():
        return {
            "online": False,
            "players_online": 0,
            "players_max": 0,
            "player_sample": [],
            "favicon": "",
            "motd": "",
            "latency_ms": -1,
            "error": "Empty address",
        }

    clean_addr = address.strip()
    if ":" in clean_addr:
        orig_host = clean_addr.split(":", 1)[0].strip()
    else:
        orig_host = clean_addr

    host, port = resolve_minecraft_srv(clean_addr)
    handshake_host = orig_host or host
    t0 = time.perf_counter()

    try:
        s = socket.create_connection((host, port), timeout=timeout)
        try:
            s.settimeout(timeout)

            # 1. Handshake packet (ID 0x00, protocol 767 for modern 1.21.x, next state 1 = status)
            host_bytes = handshake_host.encode("utf-8")
            packet = bytearray()
            packet += b"\x00"  # Packet ID
            packet += encode_varint(767)  # Protocol version
            packet += encode_varint(len(host_bytes)) + host_bytes
            packet += struct.pack(">H", port)
            packet += encode_varint(1)  # Next state: 1 (status)

            s.sendall(encode_varint(len(packet)) + packet)

            # 2. Status request packet (ID 0x00)
            req = b"\x00"
            s.sendall(encode_varint(len(req)) + req)

            # 3. Status response
            _resp_len = read_varint(s)
            resp_id = read_varint(s)
            if resp_id != 0x00:
                raise ValueError(f"Unexpected packet ID {resp_id}")

            str_len = read_varint(s)
            data = bytearray()
            while len(data) < str_len:
                chunk = s.recv(min(4096, str_len - len(data)))
                if not chunk:
                    break
                data += chunk

            raw_json = json.loads(data.decode("utf-8", errors="replace"))
        finally:
            s.close()

        latency = int((time.perf_counter() - t0) * 1000)

        # Parse player info
        players_dict = raw_json.get("players", {})
        if not isinstance(players_dict, dict):
            players_dict = {}

        try:
            players_online = int(players_dict.get("online", 0))
        except (TypeError, ValueError):
            players_online = 0

        try:
            players_max = int(players_dict.get("max", 0))
        except (TypeError, ValueError):
            players_max = 0

        raw_sample = players_dict.get("sample", [])
        clean_sample: List[str] = []
        if isinstance(raw_sample, list):
            for p in raw_sample:
                if isinstance(p, dict):
                    name_raw = p.get("name")
                    if name_raw:
                        cleaned_name = clean_minecraft_formatting(extract_chat_component_text(name_raw))
                        if cleaned_name:
                            clean_sample.append(cleaned_name)
                elif isinstance(p, str):
                    cleaned_name = clean_minecraft_formatting(p)
                    if cleaned_name:
                        clean_sample.append(cleaned_name)

        # Parse favicon
        favicon = raw_json.get("favicon", "")
        if favicon and not str(favicon).startswith("data:image/png;base64,"):
            favicon = f"data:image/png;base64,{favicon}"

        # Parse description / motd
        desc = raw_json.get("description", "")
        motd = clean_minecraft_formatting(extract_chat_component_text(desc))

        return {
            "online": True,
            "players_online": players_online,
            "players_max": players_max,
            "player_sample": clean_sample[:15],  # Cap sample to 15 entries
            "favicon": favicon,
            "motd": motd,
            "latency_ms": latency,
            "error": None,
        }

    except Exception as exc:
        return {
            "online": False,
            "players_online": 0,
            "players_max": 0,
            "player_sample": [],
            "favicon": "",
            "motd": "",
            "latency_ms": -1,
            "error": str(exc),
        }


def get_cached_server_status(address: str, max_age: float = 60.0) -> Optional[Dict[str, Any]]:
    """Returns cached status if available and younger than max_age seconds."""
    key = address.strip().lower()
    with _CACHE_LOCK:
        entry = _STATUS_CACHE.get(key)
        if entry:
            ts, data = entry
            if time.time() - ts <= max_age:
                return data
    return None


def set_cached_server_status(address: str, data: Dict[str, Any]) -> None:
    """Stores server status in cache with current timestamp."""
    key = address.strip().lower()
    with _CACHE_LOCK:
        _STATUS_CACHE[key] = (time.time(), data)


def clear_server_status_cache(address: Optional[str] = None) -> None:
    """Clears cache for all servers or a specific address."""
    with _CACHE_LOCK:
        if address:
            _STATUS_CACHE.pop(address.strip().lower(), None)
        else:
            _STATUS_CACHE.clear()
