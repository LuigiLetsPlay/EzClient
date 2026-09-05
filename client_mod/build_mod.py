import os
import sys
import argparse
import shutil
import subprocess
from pathlib import Path

FROZEN_EZCLIENT_VERSION = "2.0.0"
FROZEN_TARGETS: tuple[str, ...] = ()
CURRENT_TARGETS = ("26.1", "26.1.1", "26.2")


def artifact_version(target: str) -> str:
    """26.x follows the app project version."""
    return project_version()


def build_targets(include_frozen: bool = False, frozen_only: bool = False) -> tuple[str, ...]:
    if frozen_only:
        return FROZEN_TARGETS
    return CURRENT_TARGETS


def build_ezclient_jar(include_frozen: bool = False, frozen_only: bool = False) -> Path:
    project_root = Path(__file__).resolve().parent.parent
    client_mod_dir = project_root / "client_mod"
    assets_out = project_root / "backend" / "assets"
    assets_out.mkdir(parents=True, exist_ok=True)
    print("[EzClient Builder] Building versioned EzClient JARs using Gradle...")

    gradle_wrapper = client_mod_dir / ("gradlew.bat" if os.name == "nt" else "gradlew")
    # Development and release builds touch only the actively maintained 26.x family.
    targets = build_targets(include_frozen, frozen_only)
    outputs: dict[str, Path] = {}
    for target in targets:
        unobfuscated = target.startswith("26.")
        target_version = artifact_version(target)
        res = subprocess.run(
            [
                str(gradle_wrapper),
                f"-Ploomx.unobfuscated={str(unobfuscated).lower()}",
                f"-Pmod_version={target_version}",
                f":{target}:build",
            ],
            cwd=str(client_mod_dir),
            capture_output=True,
            text=True,
        )
        if res.returncode != 0:
            print(f"[EzClient Builder] Gradle build for {target} failed:\n{res.stdout}\n{res.stderr}")
            raise RuntimeError(f"Gradle compilation failed for Minecraft {target}")
        libs_dir = client_mod_dir / "versions" / target / "build" / "libs"
        main_jars = [
            jar for jar in libs_dir.glob("EzClient-*.jar")
            if not jar.name.endswith("-sources.jar") and "-dev" not in jar.name
        ]
        if not main_jars:
            raise FileNotFoundError(f"No built jar found in {libs_dir}")
        main_jar = max(main_jars, key=lambda jar: jar.stat().st_mtime)
        output = assets_out / f"EzClient-{target_version}+{target}.jar"
        shutil.copy2(main_jar, output)
        outputs[target] = output
        print(f"[EzClient Builder] Created: {output.name} ({output.stat().st_size} bytes)")

    return outputs.get("26.2", outputs[targets[-1]])


def project_version() -> str:
    properties = Path(__file__).resolve().parent / "gradle.properties"
    for line in properties.read_text(encoding="utf-8").splitlines():
        if line.startswith("mod_version="):
            return line.split("=", 1)[1].strip()
    raise RuntimeError("mod_version is missing from client_mod/gradle.properties")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Build EzClient Minecraft JARs")
    args = parser.parse_args()
    build_ezclient_jar()
