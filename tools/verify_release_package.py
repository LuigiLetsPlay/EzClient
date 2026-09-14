"""Verify shipped runtime assets against source and record release checksums."""
import hashlib
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from backend.models.types import APP_VERSION

def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()

def main():
    runtime = ROOT / "dist/EzClient/_internal"
    portable = ROOT / f"dist/EzClient-v{APP_VERSION}-Windows-Portable.zip"
    with zipfile.ZipFile(portable) as archive:
        for version in ("26.1", "26.1.1", "26.2"):
            filename = f"EzClient-{APP_VERSION}+{version}.jar"
            source = ROOT / "backend/assets" / filename
            assert digest(source) == digest(runtime / "backend/assets" / filename)
            assert digest(source) == hashlib.sha256(archive.read("EzClient/_internal/backend/assets/" + filename)).hexdigest()
        for source in (ROOT / "ui").rglob("*"):
            if source.is_file() and source.suffix in (".qml", ".js"):
                relative = source.relative_to(ROOT / "ui")
                assert digest(source) == digest(runtime / "ui" / relative), relative
                assert digest(source) == hashlib.sha256(archive.read("EzClient/_internal/ui/" + relative.as_posix())).hexdigest(), relative
    files = [ROOT / "dist/EzClient/EzClient.exe", ROOT / "dist/EzClient-Setup.exe", portable]
    results = [{"file": str(path.relative_to(ROOT)), "bytes": path.stat().st_size, "sha256": digest(path)} for path in files]
    (ROOT / "build/release-qa/package-checksums.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
    print("Source, bundled runtime and portable archive match. Release checksums recorded.")

if __name__ == "__main__":
    main()
