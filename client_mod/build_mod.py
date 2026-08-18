import os
import sys
import shutil
import tempfile
import subprocess
from pathlib import Path

def build_ezclient_jar() -> Path:
    project_root = Path(__file__).resolve().parent.parent
    client_mod_dir = project_root / "client_mod"
    assets_out = project_root / "backend" / "assets"
    assets_out.mkdir(parents=True, exist_ok=True)
    out_jar = assets_out / "EzClient.jar"

    print("[EzClient Builder] Building EzClient.jar...")

    # Find classpath jars
    mc_libs = Path(os.path.expandvars(r"%APPDATA%\.minecraft\libraries"))
    cp_jars = []
    if mc_libs.exists():
        for p in mc_libs.rglob("*.jar"):
            if any(k in p.name.lower() for k in ["fabric-loader", "lwjgl-3.3", "lwjgl-3.4", "lwjgl-glfw", "sponge-mixin"]):
                cp_jars.append(str(p))

    cp_str = os.pathsep.join(cp_jars)

    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)
        classes_dir = tmp_path / "classes"
        classes_dir.mkdir(parents=True, exist_ok=True)

        java_src = client_mod_dir / "src" / "app" / "ezclient" / "EzClientMod.java"
        if not java_src.exists():
            raise FileNotFoundError(f"Source file not found: {java_src}")

        # Compile Java
        cmd = [
            "javac",
            "--release", "17",
            "-encoding", "UTF-8",
            "-cp", cp_str,
            "-d", str(classes_dir),
            str(java_src)
        ]
        print(f"[EzClient Builder] Running javac...")
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode != 0:
            print(f"[EzClient Builder] Javac failed: {res.stderr}")
            raise RuntimeError(f"Javac compilation failed: {res.stderr}")

        # Copy fabric.mod.json
        fabric_json = client_mod_dir / "fabric.mod.json"
        shutil.copy2(fabric_json, classes_dir / "fabric.mod.json")

        # Copy assets/ezclient/icon.png
        mod_assets = classes_dir / "assets" / "ezclient"
        mod_assets.mkdir(parents=True, exist_ok=True)
        logo_src = project_root / "ui" / "assets" / "logo.png"
        if logo_src.exists():
            shutil.copy2(logo_src, mod_assets / "icon.png")

        # Package jar using jar tool or zipfile
        import zipfile
        if out_jar.exists():
            out_jar.unlink()

        with zipfile.ZipFile(out_jar, "w", compression=zipfile.ZIP_DEFLATED) as zf:
            for root, dirs, files in os.walk(classes_dir):
                for f in files:
                    full_p = Path(root) / f
                    arc_name = full_p.relative_to(classes_dir).as_posix()
                    zf.write(full_p, arc_name)

    print(f"[EzClient Builder] Successfully created: {out_jar} ({out_jar.stat().st_size} bytes)")
    return out_jar

if __name__ == "__main__":
    build_ezclient_jar()
