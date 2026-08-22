import os
import sys
import shutil
import subprocess
from pathlib import Path

def build_ezclient_jar() -> Path:
    project_root = Path(__file__).resolve().parent.parent
    client_mod_dir = project_root / "client_mod"
    assets_out = project_root / "backend" / "assets"
    assets_out.mkdir(parents=True, exist_ok=True)
    out_jar = assets_out / "EzClient.jar"

    print("[EzClient Builder] Building EzClient.jar using Gradle...")

    gradle_wrapper = client_mod_dir / ("gradlew.bat" if os.name == "nt" else "gradlew")
    res = subprocess.run([str(gradle_wrapper), "build"], cwd=str(client_mod_dir), capture_output=True, text=True)
    
    if res.returncode != 0:
        print(f"[EzClient Builder] Gradle build failed:\n{res.stdout}\n{res.stderr}")
        raise RuntimeError("Gradle compilation failed")

    # The Lite module is declared by the repository root settings file, so it
    # must be invoked from there rather than from client_mod's standalone build.
    lite_res = subprocess.run([str(gradle_wrapper), ":client_mod_lite:build"], cwd=str(project_root), capture_output=True, text=True)
    if lite_res.returncode != 0:
        print(f"[EzClient Builder] Lite build failed:\n{lite_res.stdout}\n{lite_res.stderr}")
        raise RuntimeError("Lite mod compilation failed")

    # Minecraft 26.2 ships unobfuscated names. Loom's normal production JAR is
    # therefore the directly loadable artifact; source JARs are excluded.
    libs_dir = client_mod_dir / "build" / "libs"
    main_jars = sorted(
        jar for jar in libs_dir.glob("EzClient-*.jar")
        if not jar.name.endswith("-sources.jar")
    )
    
    if not main_jars:
        raise FileNotFoundError(f"No built jar found in {libs_dir}")
        
    # Select the newest build; lexicographic ordering can otherwise embed an
    # older release (for example 1.5.0 before 1.5.1).
    main_jar = max(main_jars, key=lambda jar: jar.stat().st_mtime)
    shutil.copy2(main_jar, out_jar)

    lite_dir = project_root / "client_mod_lite" / "build" / "libs"
    lite_jars = sorted(jar for jar in lite_dir.glob("EzClient-Lite-*.jar") if not jar.name.endswith("-sources.jar"))
    if lite_jars:
        shutil.copy2(max(lite_jars, key=lambda jar: jar.stat().st_mtime), assets_out / "EzClient-Lite.jar")

    print(f"[EzClient Builder] Successfully created: {out_jar} ({out_jar.stat().st_size} bytes)")
    return out_jar

if __name__ == "__main__":
    build_ezclient_jar()
