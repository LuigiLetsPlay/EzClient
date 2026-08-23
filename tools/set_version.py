"""Set the EzClient release version consistently across launcher and client.

Usage: python tools/set_version.py 1.5.6
Run this before building a release. The script refuses versions that are not
plain semantic versions and reports every source file it changed.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TARGETS = {
    "backend/models/types.py": [(r'APP_VERSION = "[^"]+"', 'APP_VERSION = "{version}"')],
    "backend/services/modrinth.py": [(r'EzClient/\d+\.\d+\.\d+', 'EzClient/{version}')],
    "backend/services/game_bootstrap.py": [(r'EzClient/\d+\.\d+\.\d+', 'EzClient/{version}')],
    "installer/installer_gui.py": [(r'APP_VERSION = "[^"]+"', 'APP_VERSION = "{version}"')],
    "minecraft_wrapper/launcher_wrapper.py": [(r'APP_VERSION = "[^"]+"', 'APP_VERSION = "{version}"')],
    "client_mod/gradle.properties": [(r'mod_version=\d+\.\d+\.\d+', 'mod_version={version}')],
    "client_mod/src/main/resources/fabric.mod.json": [(r'"version": "\d+\.\d+\.\d+"', '"version": "{version}"')],
    "client_mod/src/main/java/app/ezclient/EzClientMod.java": [(r'"EzClient \d+\.\d+\.\d+"', '"EzClient {version}"'), (r'CLIENT_VERSION = "\d+\.\d+\.\d+"', 'CLIENT_VERSION = "{version}"')],
    "client_mod_lite/build.gradle": [(r"version = '\d+\.\d+\.\d+'", "version = '{version}'")],
    "client_mod_lite/src/main/resources/fabric.mod.json": [(r'"version": "\d+\.\d+\.\d+"', '"version": "{version}"')],
    "client_mod_lite/src/main/java/app/ezclient/lite/EzClientLiteMod.java": [(r'"EzClient \d+\.\d+\.\d+ \(Lite\)"', '"EzClient {version} (Lite)"'), (r'CLIENT_VERSION = "\d+\.\d+\.\d+"', 'CLIENT_VERSION = "{version}"')],
    "backend/services/store.py": [(r'version="\d+\.\d+\.\d+"', 'version="{version}"')],
    "build_release.py": [(r'EzClient \d+\.\d+\.\d+', 'EzClient {version}')],
    "build_installer.py": [(r'EzClient \d+\.\d+\.\d+', 'EzClient {version}')],
    "ui/StatusBar.qml": [(r': "\d+\.\d+\.\d+"', ': "{version}"')],
}


def main() -> int:
    if len(sys.argv) != 2 or not re.fullmatch(r"\d+\.\d+\.\d+", sys.argv[1]):
        print("Usage: python tools/set_version.py MAJOR.MINOR.PATCH", file=sys.stderr)
        return 2
    version = sys.argv[1]
    for relative, replacements in TARGETS.items():
        path = ROOT / relative
        content = path.read_text(encoding="utf-8")
        updated = content
        for pattern, replacement in replacements:
            updated, count = re.subn(pattern, replacement.format(version=version), updated)
            if count < 1:
                raise RuntimeError(f"Expected at least one match for {relative}: {pattern} (got {count})")
        path.write_text(updated, encoding="utf-8")
        print(f"updated {relative}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
