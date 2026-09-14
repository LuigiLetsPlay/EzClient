import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
for version in ("26.1", "26.1.1", "26.2"):
    jars = list((ROOT / "client_mod/.gradle/loom-cache/minecraftMaven/net/minecraft").glob(f"*/{version}/*.jar"))
    with zipfile.ZipFile(jars[0]) as jar:
        print(version, json.loads(jar.read("version.json"))["pack_version"])
    with zipfile.ZipFile(ROOT / f"backend/assets/EzClient-2.2.0+{version}.jar") as jar:
        data = json.loads(jar.read("fabric.mod.json"))
        print("EzClient", data["version"], data["depends"])
