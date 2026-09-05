package app.ezclient.test;

import app.ezclient.gui.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

/** Real world/render smoke test in an isolated, automatically deleted test save. */
public class ModulePackSmokeTest implements FabricClientGameTest {
    private static void set(FeatureModule module, String key, Object value) {
        var option = module.options().stream().filter(o -> o.key().equals(key)).findFirst().orElseThrow();
        if (!module.set(option, value)) throw new AssertionError("Rejected valid setting " + key);
    }
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            context.runOnClient(mc -> {
                for (var module : ModuleManager.getInstance().getModules()) {
                    module.setEnabled(module instanceof FeatureModule);
                    if (module instanceof FeatureModule feature) feature.setCustomFont(true);
                }
                FeatureModule.get(BossBarModule.class).setPosition(180, 12);
                var waypoints = FeatureModule.get(WaypointsModule.class);
                waypoints.put(new WaypointsModule.Waypoint("smoke", "Render Test", mc.player.getX() + 4, mc.player.getY(), mc.player.getZ() + 5,
                    0xff22ff88, "*", WaypointsModule.world(mc), mc.level.dimension().toString(), false));
                set(waypoints, "marker", "Both");
                var time = FeatureModule.get(TimeWeatherModule.class); set(time, "time", "Sunset");
                var box = FeatureModule.get(BlockOverlayModule.class); set(box, "style", "Both");
                var particles = FeatureModule.get(ParticleCustomizerModule.class); set(particles, "tint", true); set(particles, "multiplier", 3.5);
                if (particles.set(particles.options().getFirst(), Double.NaN)) throw new AssertionError("NaN accepted");
                ConfigManager.save(); ConfigManager.load();
                if (particles.number("multiplier") != 3.5) throw new AssertionError("Settings round trip failed");
            });
            var server = world.getServer();
            server.runCommand("execute at @p run summon minecraft:cow ~2 ~ ~4");
            server.runCommand("execute at @p run summon minecraft:item ~1 ~1 ~3 {Item:{id:\"minecraft:diamond\",count:1}}");
            server.runCommand("execute at @p run setblock ~ ~ ~3 minecraft:stone");
            server.runCommand("bossbar add ezclient:smoke \"EzClient Render Test\"");
            server.runCommand("bossbar set ezclient:smoke value 78");
            server.runCommand("bossbar set ezclient:smoke players @a");
            server.runCommand("scoreboard objectives add bedwars dummy \"BED WARS\"");
            server.runCommand("scoreboard objectives setdisplay sidebar bedwars");
            server.runOnServer(s -> {
                var board = s.getScoreboard(); var objective = board.getObjective("bedwars");
                var timer = board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("timer"), objective);
                timer.set(2); timer.display(net.minecraft.network.chat.Component.literal("Diamond II: 12s"));
                var bed = board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("red"), objective);
                bed.set(1); bed.display(net.minecraft.network.chat.Component.literal("Red: ✔"));
            });
            server.runCommand("give @p minecraft:iron_ingot 32");
            server.runCommand("give @p minecraft:gold_ingot 8");
            world.getConnection().waitForClientboundPackets();
            context.getInput().lookAt(context.computeOnClient(mc -> BlockPos.containing(mc.player.position().add(0, 0, 3))));
            context.waitTicks(20);
            context.runOnClient(mc -> {
                var rows = FeatureModule.get(BedwarsModule.class).lines(mc, false);
                if (rows.stream().noneMatch(s -> s.contains("Diamond II")) || rows.stream().noneMatch(s -> s.contains("Red:"))) throw new AssertionError("Scoreboard overlay missing");
                var particles = FeatureModule.get(ParticleCustomizerModule.class);
                set(particles, "multiplier", 0);
                if (mc.particleEngine.createParticle(net.minecraft.core.particles.ParticleTypes.CRIT, mc.player.getX(), mc.player.getY(), mc.player.getZ(), 0, 0, 0) != null) throw new AssertionError("Zero particle multiplier failed");
                set(particles, "multiplier", 3.5);
                var weather = FeatureModule.get(TimeWeatherModule.class); set(weather, "weather", "Rain");
                if (mc.level.getRainLevel(1) != 1) throw new AssertionError("Rain override failed");
                set(weather, "weather", "Clear");
                if (mc.level.getRainLevel(1) != 0) throw new AssertionError("Clear override failed");
            });
            server.runCommand("execute at @p run playsound minecraft:entity.tnt.primed master @a ~2 ~ ~ 1");
            context.waitTicks(5);
            context.runOnClient(mc -> {
                if (FeatureModule.get(SoundEnhancerModule.class).lines(mc, false).stream().noneMatch(s -> s.startsWith("!"))) throw new AssertionError("Highlighted sound subtitle missing");
            });
            context.waitTicks(80); // Let recipe/advancement notifications leave the screenshot.
            context.takeScreenshot("module-pack-world");
            context.getInput().pressKey(GLFW.GLFW_KEY_F5);
            context.waitTicks(5);
            context.takeScreenshot("module-pack-nameplate");
            context.getInput().pressKey(GLFW.GLFW_KEY_F1);
            context.waitTicks(5);
            context.takeScreenshot("module-pack-f1");
            context.getInput().pressKey(GLFW.GLFW_KEY_F1);
            context.getInput().pressKey(GLFW.GLFW_KEY_F3);
            context.waitTicks(5);
            context.takeScreenshot("module-pack-f3");
            context.getInput().pressKey(GLFW.GLFW_KEY_F3);
            context.setScreen(() -> new FeatureSettingsScreen(null, FeatureModule.get(HitboxModule.class)));
            context.waitTicks(3); context.takeScreenshot("module-pack-settings");
            context.setScreen(() -> new FeatureStyleScreen(null, FeatureModule.get(BossBarModule.class)));
            context.waitTicks(3); context.takeScreenshot("module-pack-style");
            context.setScreen(() -> new WaypointScreen(null, FeatureModule.get(WaypointsModule.class)));
            context.waitTicks(3); context.takeScreenshot("module-pack-waypoints");
            context.setScreen(() -> null);
            server.runCommand("kill @p");
            context.waitFor(mc -> mc.player != null && mc.player.isDeadOrDying());
            context.waitTicks(2);
            context.runOnClient(mc -> {
                if (FeatureModule.get(WaypointsModule.class).points().stream().noneMatch(WaypointsModule.Waypoint::death)) throw new AssertionError("Death waypoint missing");
            });
        }
    }
}
