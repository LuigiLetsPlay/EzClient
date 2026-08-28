#!/usr/bin/env python3
"""
EzClient Version Updater
Updates version strings across the entire EzClient project (Client Mod, Lite Mod, Backend, Launcher, UI, Installer, Build Scripts).

Usage:
    python update_version.py <new_version>
    python update_version.py <old_version> <new_version>
Example:
    python update_version.py 1.8.0
"""

import sys
import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent

RELATIVE_FILES = [
    # Java Client Mod
    "client_mod/src/main/java/app/ezclient/EzClientMod.java",
    "client_mod/src/main/java/app/ezclient/gui/EzHubScreen.java",
    "client_mod/src/main/java/app/ezclient/gui/HudEditorScreen.java",
    "client_mod/src/main/java/app/ezclient/cosmetics/CommunityPresence.java",
    "client_mod/src/main/resources/fabric.mod.json",
    "client_mod/gradle.properties",

    # Java Lite Mod
    "client_mod_lite/src/main/java/app/ezclient/lite/EzClientLiteMod.java",
    "client_mod_lite/src/main/resources/fabric.mod.json",
    "client_mod_lite/build.gradle",

    # Python Backend & Wrapper
    "backend/models/types.py",
    "backend/controllers/profile_controller.py",
    "backend/services/store.py",
    "backend/services/modrinth.py",
    "backend/services/game_bootstrap.py",
    "minecraft_wrapper/launcher_wrapper.py",

    # Installer & Build Scripts
    "installer/installer_gui.py",
    "build_installer.py",
    "build_release.py",

    # UI (QML)
    "ui/StatusBar.qml",
    "ui/HomePage.qml",
    "ui/TopBar.qml",
]


def detect_current_version() -> str:
    types_file = REPO_ROOT / "backend" / "models" / "types.py"
    if types_file.exists():
        text = types_file.read_text(encoding="utf-8")
        match = re.search(r'APP_VERSION\s*=\s*"([^"]+)"', text)
        if match:
            return match.group(1)

    mod_file = REPO_ROOT / "client_mod" / "src" / "main" / "java" / "app" / "ezclient" / "EzClientMod.java"
    if mod_file.exists():
        text = mod_file.read_text(encoding="utf-8")
        match = re.search(r'CLIENT_VERSION\s*=\s*"([^"]+)"', text)
        if match:
            return match.group(1)

    return "1.7.1"


def update_version(old_ver: str, new_ver: str) -> None:
    print(f"[EzClient Version Updater] Updating version: {old_ver} -> {new_ver}\n")
    updated_count = 0

    for rel_path in RELATIVE_FILES:
        target_path = REPO_ROOT / rel_path
        if not target_path.exists():
            print(f"  [MISSING] {rel_path}")
            continue

        try:
            content = target_path.read_text(encoding="utf-8")
            if old_ver in content:
                new_content = content.replace(old_ver, new_ver)
                target_path.write_text(new_content, encoding="utf-8")
                print(f"  [UPDATED] {rel_path}")
                updated_count += 1
            else:
                # Check if it already has the new version
                if new_ver in content:
                    print(f"  [OK]      {rel_path} (already {new_ver})")
                else:
                    print(f"  [SKIPPED] {rel_path} (pattern not found)")
        except Exception as e:
            print(f"  [ERROR]   {rel_path}: {e}")

    print(f"\nCompleted: {updated_count} files updated to version {new_ver}.")


def main():
    if len(sys.argv) == 2:
        new_ver = sys.argv[1].strip()
        old_ver = detect_current_version()
    elif len(sys.argv) >= 3:
        old_ver = sys.argv[1].strip()
        new_ver = sys.argv[2].strip()
    else:
        print("Usage: python update_version.py <new_version>")
        sys.exit(1)

    if old_ver == new_ver:
        print(f"Current version is already {new_ver}.")
        return

    update_version(old_ver, new_ver)


if __name__ == "__main__":
    main()
