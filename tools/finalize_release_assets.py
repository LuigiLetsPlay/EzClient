"""Verify and refresh external runtime assets after the final mod/UI checks."""
import hashlib
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from backend.models.types import APP_VERSION

def main():
    distribution = ROOT / "dist/EzClient"
    runtime = distribution / "_internal"
    if not (distribution / "EzClient.exe").is_file() or not runtime.is_dir():
        raise RuntimeError("Build the launcher before finalizing external assets")
    records = []
    for version in ("26.1", "26.1.1", "26.2"):
        source = ROOT / f"backend/assets/EzClient-{APP_VERSION}+{version}.jar"
        with zipfile.ZipFile(source) as jar:
            metadata = json.loads(jar.read("fabric.mod.json"))
            assert metadata["version"] == APP_VERSION
            assert metadata["depends"]["minecraft"] in (version, "~" + version, "=" + version)
            assert json.loads(jar.read("assets/ezclient/lang/en_us.json"))["ezclient.ui." + hashlib.sha1("• Standard".encode()).hexdigest()[:16]] == "• Default"
        destination = runtime / "backend/assets" / source.name
        shutil.copy2(source, destination)
        records.append({"file": source.name, "minecraft": version, "sha256": hashlib.sha256(destination.read_bytes()).hexdigest()})
    expected = {row["file"] for row in records}
    actual = {p.name for p in (runtime / "backend/assets").glob("*.jar")}
    if actual != expected:
        raise RuntimeError(f"Unexpected bundled JARs: {actual - expected}")
    for source in (ROOT / "ui").rglob("*"):
        if source.is_file() and source.suffix in (".qml", ".js"):
            target = runtime / "ui" / source.relative_to(ROOT / "ui")
            if not target.is_file():
                raise RuntimeError(f"Missing bundled UI asset: {target}")
            shutil.copy2(source, target)
    archive = shutil.make_archive(str(ROOT / f"dist/EzClient-v{APP_VERSION}-Windows-Portable"), "zip", root_dir=ROOT / "dist", base_dir="EzClient")
    records.append({"file": Path(archive).name, "sha256": hashlib.sha256(Path(archive).read_bytes()).hexdigest()})
    out = ROOT / "build/release-qa/artifacts.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(records, indent=2), encoding="utf-8")
    print("Final runtime assets and portable archive verified.")

if __name__ == "__main__":
    main()
