"""Supported Minecraft release catalog and runtime compatibility matrix."""
from __future__ import annotations

from backend.models.types import APP_VERSION

FROZEN_EZCLIENT_VERSION = "2.0.0"
FROZEN_LAST_MINECRAFT = (1, 21, 99)
FROZEN_EZCLIENT_TARGETS = frozenset()

RELEASE_FAMILIES: tuple[tuple[str, tuple[str, ...]], ...] = (
    ("26", ("26.2", "26.1.1", "26.1")),
    ("1.21", ("1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21")),
    ("1.20", ("1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", "1.20")),
    ("1.19", ("1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19")),
    ("1.18", ("1.18.2", "1.18.1", "1.18")),
    ("1.17", ("1.17.1", "1.17")),
    ("1.16", ("1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1", "1.16")),
    ("1.15", ("1.15.2", "1.15.1", "1.15")),
    ("1.14", ("1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14")),
    ("1.13", ("1.13.2", "1.13.1", "1.13")),
    ("1.12", ("1.12.2", "1.12.1", "1.12")),
    ("1.11", ("1.11.2", "1.11.1", "1.11")),
    ("1.10", ("1.10.2", "1.10.1", "1.10")),
    ("1.9", ("1.9.4", "1.9.3", "1.9.2", "1.9.1", "1.9")),
    ("1.8", ("1.8.9", "1.8.8", "1.8.7", "1.8.6", "1.8.5", "1.8.4", "1.8.3", "1.8.2", "1.8.1", "1.8")),
)


def version_tuple(version: str) -> tuple[int, ...]:
    try:
        return tuple(int(part) for part in version.split("."))
    except ValueError:
        return (0,)


def required_java(version: str) -> int:
    parsed = version_tuple(version)
    if parsed[0] >= 26:
        return 25
    if parsed >= (1, 20, 5):
        return 21
    if parsed >= (1, 18):
        return 17
    if parsed >= (1, 17):
        return 16
    return 8


def asset_name(version: str) -> str:
    product_version = FROZEN_EZCLIENT_VERSION if is_frozen_ezclient_version(version) else APP_VERSION
    return f"EzClient-{product_version}+{version}.jar"


def is_frozen_ezclient_version(version: str) -> bool:
    return False


def catalog(asset_exists) -> list[dict]:
    result = []
    for family, releases in RELEASE_FAMILIES:
        items = []
        for release in releases:
            filename = asset_name(release)
            has_ezclient = bool(release.startswith("26.") and asset_exists(filename))
            items.append({
                "version": release,
                "java": required_java(release),
                "hasFabric": version_tuple(release) >= (1, 3),
                "asset": filename,
                "hasEzClient": has_ezclient,
                "isFrozen": False,
                "supportStatus": "current" if has_ezclient else "none",
                "supportLabel": "EzClient Compatible" if has_ezclient else "",
            })
        result.append({"family": family, "latest": releases[0], "releases": items})
    return result
