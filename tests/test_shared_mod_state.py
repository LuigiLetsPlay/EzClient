import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SHARED = ROOT / "client_mod" / "src" / "main" / "java" / "app" / "ezclient" / "shared"


class SharedModStateTests(unittest.TestCase):
    def test_zoom_and_click_state_are_version_independent(self):
        javac = shutil.which("javac")
        java = shutil.which("java")
        if not javac or not java:
            self.skipTest("JDK is not available")

        harness = """
import app.ezclient.shared.ClickRateTracker;
import app.ezclient.shared.ZoomState;

public final class SharedStateHarness {
    public static void main(String[] args) {
        ZoomState zoom = new ZoomState(4.0, 1.5, 15.0);
        zoom.beginZoom();
        zoom.adjust(8.0);
        if (zoom.getActiveZoom() != 12.0) throw new AssertionError("scroll adjustment failed");
        zoom.beginZoom();
        if (zoom.getActiveZoom() != 4.0) throw new AssertionError("new C press did not reset zoom");
        if (Math.abs(zoom.getFovFactor(true) - 0.25F) > 0.0001F) throw new AssertionError("FOV differs");

        ClickRateTracker clicks = new ClickRateTracker(4);
        clicks.record(1000L);
        clicks.record(1500L);
        if (clicks.count(1999L) != 2) throw new AssertionError("recent clicks missing");
        if (clicks.count(2501L) != 0) throw new AssertionError("old clicks not pruned");
    }
}
"""
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            harness_path = tmp_path / "SharedStateHarness.java"
            harness_path.write_text(harness, encoding="utf-8")
            subprocess.run(
                [javac, "-d", str(tmp_path), str(SHARED / "ZoomState.java"),
                 str(SHARED / "ClickRateTracker.java"), str(harness_path)],
                check=True,
                capture_output=True,
                text=True,
            )
            subprocess.run(
                [java, "-cp", str(tmp_path), "SharedStateHarness"],
                check=True,
                capture_output=True,
                text=True,
            )


if __name__ == "__main__":
    unittest.main()
