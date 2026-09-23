"""Build the complete EzClient 2.2.3 Windows release."""
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent


def build() -> None:
    for script in ("build_exe.py", "build_installer.py"):
        print(f"[Release] Running {script} ...")
        subprocess.run([sys.executable, str(ROOT / script)], cwd=ROOT, stdin=subprocess.DEVNULL, check=True)

    launcher = ROOT / "dist" / "EzClient" / "EzClient.exe"
    if not launcher.is_file():
        launcher = ROOT / "dist" / "EzClient.exe"
    setup = ROOT / "dist" / "EzClient-Setup.exe"

    for artifact in (launcher, setup):
        if not artifact.is_file() or artifact.stat().st_size == 0:
            raise RuntimeError(f"Missing release artifact: {artifact}")
        print(f"[Release] Ready: {artifact} ({artifact.stat().st_size / 1024 / 1024:.1f} MB)")


if __name__ == "__main__":
    build()
