"""Exact loader/version matching shared by preparation and launch."""
import json
from pathlib import Path


def forge_coordinate(minecraft_version: str, requested: str) -> str:
    requested = requested.strip()
    installed_prefix = f"{minecraft_version}-forge-"
    if requested.startswith(installed_prefix):
        return f"{minecraft_version}-{requested[len(installed_prefix):]}"
    if requested.startswith(f"{minecraft_version}-"):
        return requested
    return f"{minecraft_version}-{requested}"


def installed_forge_metadata(mc_dir: Path, minecraft_version: str, requested: str = "") -> list[Path]:
    """Support modern and legacy Forge IDs without falling back to another game/build."""
    coordinate = forge_coordinate(minecraft_version, requested) if requested else ""
    matches = []
    for path in (mc_dir / "versions").glob("*/*.json"):
        if "forge" not in path.parent.name.lower():
            continue
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        if data.get("inheritsFrom") != minecraft_version:
            continue
        libraries = [str(lib.get("name", "")) for lib in data.get("libraries", [])]
        if coordinate and not any(name.startswith(f"net.minecraftforge:forge:{coordinate}")
                                  and name.split(":")[2] == coordinate for name in libraries):
            expected = coordinate.replace(f"{minecraft_version}-", f"{minecraft_version}-forge-", 1)
            if data.get("id", path.stem) != expected:
                continue
        matches.append(path)
    return sorted(matches, key=lambda path: path.stat().st_mtime, reverse=True)
