"""Run launcher tests without touching the user's installed launcher data."""
import os
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
sys.path.insert(0, str(ROOT))
os.environ["APPDATA"] = tempfile.mkdtemp(prefix="ezclient-release-tests-")
os.environ["QT_QPA_PLATFORM"] = "offscreen"

if __name__ == "__main__":
    import pytest
    raise SystemExit(pytest.main(["tests", "-q", *sys.argv[1:]]))
