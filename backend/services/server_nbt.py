"""
EzClient Server NBT Reader
Parses Minecraft servers.dat to extract recent server names, IPs, and base64 icons.
"""

import io
import re
import gzip
import struct
import time
from pathlib import Path
from typing import List, Dict, Any, Optional, Tuple

COLOR_CODE_PATTERN = re.compile(r"§[0-9a-fk-orA-FK-OR]")
_PLAYED_SERVERS_CACHE: Dict[str, Tuple[float, float, List[Dict[str, str]]]] = {}


def clean_minecraft_formatting(text: str) -> str:
    if not text:
        return ""
    return COLOR_CODE_PATTERN.sub("", text).strip()


def _read_tag(bio: io.BytesIO, tag_type: int) -> Any:
    if tag_type == 0:
        return None
    elif tag_type == 1:
        return bio.read(1)
    elif tag_type == 2:
        return bio.read(2)
    elif tag_type == 3:
        return bio.read(4)
    elif tag_type == 4:
        return bio.read(8)
    elif tag_type == 5:
        return bio.read(4)
    elif tag_type == 6:
        return bio.read(8)
    elif tag_type == 7:
        length = struct.unpack(">i", bio.read(4))[0]
        return bio.read(length)
    elif tag_type == 8:
        length = struct.unpack(">h", bio.read(2))[0]
        return bio.read(length).decode("utf-8", errors="replace")
    elif tag_type == 9:
        elem_type = bio.read(1)[0]
        count = struct.unpack(">i", bio.read(4))[0]
        items = [_read_tag(bio, elem_type) for _ in range(count)]
        return (elem_type, items)
    elif tag_type == 10:
        comp = {}
        while True:
            tt_b = bio.read(1)
            if not tt_b or tt_b[0] == 0:
                break
            tt = tt_b[0]
            nl = struct.unpack(">h", bio.read(2))[0]
            name = bio.read(nl).decode("utf-8", errors="replace")
            comp[name] = (tt, _read_tag(bio, tt))
        return comp
    elif tag_type == 11:
        length = struct.unpack(">i", bio.read(4))[0]
        return bio.read(length * 4)
    elif tag_type == 12:
        length = struct.unpack(">i", bio.read(4))[0]
        return bio.read(length * 8)
    raise ValueError(f"Unknown tag type {tag_type}")


AD_OR_DUMMY_DOMAINS = {
    "advert.norisk.space",
    "norisk.space",
    "heroisland.net",
    "localhost",
    "127.0.0.1",
    "0.0.0.0",
}

AD_NAME_PATTERNS = {
    "ad",
    "advert",
    "norisk.host",
    "norisk host",
    "norisk client",
    "minecraft server",
}


def is_ad_or_dummy_server(ip: str, name: str = "") -> bool:
    """Checks if a server address or name is a dummy/ad/promotional server (e.g. injected by NoRiskClient)."""
    ip_clean = (ip or "").strip().lower()
    name_clean = (name or "").strip().lower()

    if not ip_clean:
        return True

    host = ip_clean.split(":")[0].strip()

    for ad_dom in AD_OR_DUMMY_DOMAINS:
        if host == ad_dom or host.endswith("." + ad_dom):
            return True

    if "advert" in host or "norisk" in host:
        return True

    if name_clean in AD_NAME_PATTERNS:
        return True

    return False


def parse_servers_dat(file_path: Path) -> List[Dict[str, str]]:
    """
    Parses a servers.dat file and returns a list of server dictionaries:
    [{"name": "...", "ip": "...", "icon": "data:image/png;base64,..."}]
    """
    if not file_path.is_file():
        return []

    try:
        data = file_path.read_bytes()
        if not data:
            return []

        is_gzip = data[:2] == b"\x1f\x8b"
        raw = gzip.decompress(data) if is_gzip else data
        bio = io.BytesIO(raw)

        root_type = bio.read(1)[0]
        if root_type != 10:  # TAG_Compound
            return []

        nl = struct.unpack(">h", bio.read(2))[0]
        _ = bio.read(nl)  # root name
        root_val = _read_tag(bio, root_type)

        if not isinstance(root_val, dict) or "servers" not in root_val:
            return []

        _, server_list = root_val["servers"][1]
        results = []

        for item in server_list:
            if not isinstance(item, dict):
                continue

            name_raw = item.get("name", (8, ""))[1] if "name" in item else ""
            ip = item.get("ip", (8, ""))[1] if "ip" in item else ""
            icon = item.get("icon", (8, ""))[1] if "icon" in item else ""

            if not ip:
                continue

            # Format icon as data URL if present
            icon_url = ""
            if icon:
                icon_str = str(icon).strip()
                if icon_str.startswith("data:image/png;base64,"):
                    icon_url = icon_str
                elif icon_str:
                    icon_url = f"data:image/png;base64,{icon_str}"

            clean_name = clean_minecraft_formatting(str(name_raw)) or ip

            results.append({
                "name": clean_name,
                "ip": str(ip).strip(),
                "icon": icon_url
            })

        return results
    except Exception as exc:
        print(f"[ServerNbt] Error parsing {file_path}: {exc}")
        return []


def get_actually_played_servers(
    profile_path: Path,
    limit: int = 3,
    launcher_history: Optional[List[str]] = None
) -> List[Dict[str, str]]:
    """
    Extracts servers where the player ACTUALLY played.
    Scans Minecraft's connection logs (latest.log and recent *.log.gz) and launcher history,
    filters out ad/dummy servers, and matches against servers.dat to preserve custom names and icons.
    Returns at most `limit` items (default 3), strictly ordered by recency.
    """
    cache_key = f"{profile_path}:{limit}:{tuple(launcher_history or [])}"
    now = time.time()
    latest_log_path = profile_path / "logs" / "latest.log"
    latest_mtime = latest_log_path.stat().st_mtime if latest_log_path.is_file() else 0.0

    if cache_key in _PLAYED_SERVERS_CACHE:
        cached_time, cached_mtime, cached_res = _PLAYED_SERVERS_CACHE[cache_key]
        if now - cached_time < 5.0 or (latest_mtime > 0 and cached_mtime == latest_mtime):
            return [dict(x) for x in cached_res]

    played_ips: List[str] = []
    seen_ips = set()

    # 1. Launcher instant-play history (most recent first)
    if launcher_history:
        for raw_ip in launcher_history:
            clean_ip = str(raw_ip).strip()
            if not clean_ip or clean_ip.lower() in seen_ips:
                continue
            if is_ad_or_dummy_server(clean_ip):
                continue
            seen_ips.add(clean_ip.lower())
            played_ips.append(clean_ip)
            if len(played_ips) >= limit:
                break

    # 2. Extract connections from Minecraft logs
    if len(played_ips) < limit:
        logs_dir = profile_path / "logs"
        if logs_dir.is_dir():
            candidate_files = []
            latest_log = logs_dir / "latest.log"
            if latest_log.is_file():
                candidate_files.append(latest_log)

            # Up to 10 most recent compressed logs by mtime
            try:
                gz_logs = sorted(
                    logs_dir.glob("*.log.gz"),
                    key=lambda x: x.stat().st_mtime,
                    reverse=True
                )[:10]
                candidate_files.extend(gz_logs)
            except Exception:
                pass

            # Mojang ConnectScreen log pattern across versions: "Connecting to <host>, <port>"
            conn_re = re.compile(r"Connecting to\s+([a-zA-Z0-9\.\-_]+),\s*(\d+)", re.IGNORECASE)

            for log_file in candidate_files:
                try:
                    if log_file.name.endswith(".gz"):
                        raw_bytes = log_file.read_bytes()
                        content = gzip.decompress(raw_bytes).decode("utf-8", errors="ignore")
                    else:
                        with open(log_file, "r", encoding="utf-8", errors="ignore") as f:
                            content = f.read()

                    lines = content.splitlines()
                    # Reverse line iteration so most recent connection in each file is found first
                    for line in reversed(lines):
                        m = conn_re.search(line)
                        if not m:
                            continue
                        host = m.group(1).strip()
                        port = m.group(2).strip()
                        full_ip = f"{host}:{port}" if (port and port != "25565") else host

                        if is_ad_or_dummy_server(full_ip):
                            continue

                        if full_ip.lower() not in seen_ips:
                            seen_ips.add(full_ip.lower())
                            played_ips.append(full_ip)
                            if len(played_ips) >= limit:
                                break
                    if len(played_ips) >= limit:
                        break
                except Exception:
                    pass

    if not played_ips:
        _PLAYED_SERVERS_CACHE[cache_key] = (now, latest_mtime, [])
        return []

    # 3. Match against servers.dat to get custom names and icons
    servers_dat_file = profile_path / "servers.dat"
    dat_entries = parse_servers_dat(servers_dat_file)
    dat_lookup = {}
    for entry in dat_entries:
        s_ip = entry.get("ip", "").strip()
        if not s_ip or is_ad_or_dummy_server(s_ip, entry.get("name", "")):
            continue
        dat_lookup[s_ip.lower()] = entry
        host_only = s_ip.split(":")[0].lower()
        if host_only not in dat_lookup:
            dat_lookup[host_only] = entry

    results = []
    for ip in played_ips[:limit]:
        ip_lower = ip.lower()
        host_lower = ip.split(":")[0].lower()

        matched = dat_lookup.get(ip_lower) or dat_lookup.get(host_lower)
        if matched:
            name = matched.get("name") or ip
            icon = matched.get("icon") or ""
        else:
            name = ip
            icon = ""

        results.append({
            "name": name,
            "ip": ip,
            "icon": icon
        })

    _PLAYED_SERVERS_CACHE[cache_key] = (now, latest_mtime, [dict(x) for x in results])
    return results


def get_recent_servers(
    profile_path: Path,
    limit: int = 3,
    launcher_history: Optional[List[str]] = None
) -> List[Dict[str, str]]:
    """Returns the top `limit` actually played servers from logs and history, falling back to clean servers.dat entries."""
    played = get_actually_played_servers(profile_path, limit=limit, launcher_history=launcher_history)
    if played:
        return played[:limit]
    return []
