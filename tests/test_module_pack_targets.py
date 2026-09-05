"""Validate required injection points against real 26.x Minecraft bytecode."""
import pathlib
import re
import shutil
import subprocess
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
TARGETS = {
    "HitboxVisualizerMixin": ("net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer", ["emitGizmos"]),
    "ItemPhysicsMixin": ("net.minecraft.client.renderer.entity.ItemEntityRenderer", []),
    "BossBarCustomizerMixin": ("net.minecraft.client.gui.components.BossHealthOverlay", ["extractRenderState", "extractBar"]),
    "WeatherLevelMixin": ("net.minecraft.world.level.Level", ["getRainLevel", "getThunderLevel"]),
    "WeatherParticlesMixin": ("net.minecraft.client.renderer.WeatherEffectRenderer", ["extractRenderState"]),
    "WeatherFlashMixin": ("net.minecraft.client.multiplayer.ClientLevel", ["getSkyFlashTime"]),
    "VisualTimeMixin": ("net.minecraft.world.attribute.EnvironmentAttributeSystem", ["getValue"]),
    "SoundVolumeMixin": ("net.minecraft.client.sounds.SoundEngine", []),
    "ParticleCustomizerMixin": ("net.minecraft.client.particle.ParticleEngine", ["createParticle"]),
    "ParticleAlphaAccessor": ("net.minecraft.client.particle.SingleQuadParticle", ["setAlpha"]),
    "NameplateCustomizerMixin": ("net.minecraft.client.renderer.entity.player.AvatarRenderer", []),
}


class ModuleTargetTests(unittest.TestCase):
    @unittest.skipUnless(shutil.which("javap"), "JDK required")
    def test_injection_descriptors_on_each_maintained_version(self):
        cache = ROOT / "client_mod/.gradle/loom-cache/minecraftMaven/net/minecraft"
        jars = {v: list(cache.glob(f"*/{v}/*.jar")) for v in ("26.1", "26.1.1", "26.2")}
        if not all(jars.values()):
            self.skipTest("Build the three maintained versions first")
        for version, paths in jars.items():
            targets = dict(TARGETS)
            targets["BlockOverlayExtractionMixin"] = (
                "net.minecraft.client.renderer.extract.LevelExtractor" if version == "26.2" else "net.minecraft.client.renderer.LevelRenderer",
                ["extractBlockOutline", "extractBlockDestroyAnimation"],
            )
            for mixin, (target, names) in targets.items():
                with self.subTest(version=version, mixin=mixin):
                    result = subprocess.run(["javap", "-p", "-s", "-classpath", str(paths[0]), target], capture_output=True, text=True, check=True)
                    output = result.stdout
                    source = (ROOT / f"client_mod/src/main/java/app/ezclient/mixin/{mixin}.java").read_text(encoding="utf-8")
                    for selector in re.findall(r'method\s*=\s*"([^"]+)"', source):
                        if mixin == "BlockOverlayExtractionMixin" and selector == ("finalizeGizmoCollection" if version == "26.2" else "extractGizmos"):
                            continue
                        if "(" in selector:
                            name, descriptor = selector.split("(", 1)
                            self.assertRegex(output, rf"\b{re.escape(name)}\([^\n]*\);\s+descriptor: {re.escape('(' + descriptor)}")
                        else:
                            self.assertRegex(output, rf"\b{re.escape(selector)}\(")
                    for name in names:
                        self.assertRegex(output, rf"\b{re.escape(name)}\(")


if __name__ == "__main__":
    unittest.main()
