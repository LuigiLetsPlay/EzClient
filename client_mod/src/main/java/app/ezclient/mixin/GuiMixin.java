package app.ezclient.mixin;

import app.ezclient.gui.CrosshairModule;
import app.ezclient.gui.ModuleManager;
import net.minecraft.client.Minecraft;
//? if >=26.2 {
import net.minecraft.client.gui.Hud;
//?} else {
/*import net.minecraft.client.gui.Gui;
*///?}
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
@Mixin(Hud.class)
//?} else {
/*@Mixin(Gui.class)
*///?}
public class GuiMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void onExtractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker tickDelta, CallbackInfo ci) {
        CrosshairModule module = ModuleManager.getInstance().getCrosshairModule();
        if (module != null && module.isEnabled()) {
            Minecraft client = Minecraft.getInstance();

            // Auto-hide rules
            if (module.isHideInThirdPerson() && !client.options.getCameraType().isFirstPerson()) {
                ci.cancel();
                return;
            }
            if (module.isHideInF3() && client.getDebugOverlay().showDebugScreen()) {
                ci.cancel();
                return;
            }
            if (module.isHideOnBowZoom() && client.player != null && client.player.isUsingItem() && client.player.getUseItem().is(net.minecraft.world.item.Items.BOW)) {
                ci.cancel();
                return;
            }

            float x = client.getWindow().getWidth() / (2.0f * client.getWindow().getGuiScale());
            float y = client.getWindow().getHeight() / (2.0f * client.getWindow().getGuiScale());
            if (module.isCenterOnMonitor()) {
                float[] monPos = ezclient$getMonitorCenterGuiPos(client);
                if (monPos != null) {
                    x = monPos[0];
                    y = monPos[1];
                }
            }

            module.renderCrosshair(graphics, client, x, y, false);
            ci.cancel();
        }
    }

    private static float[] ezclient$getMonitorCenterGuiPos(Minecraft client) {
        if (client.getWindow() == null) return null;
        long window = client.getWindow().handle();
        int[] winX = new int[1], winY = new int[1];
        int[] winW = new int[1], winH = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetWindowPos(window, winX, winY);
        org.lwjgl.glfw.GLFW.glfwGetWindowSize(window, winW, winH);

        org.lwjgl.PointerBuffer monitors = org.lwjgl.glfw.GLFW.glfwGetMonitors();
        long bestMonitor = 0;
        int bestMonX = 0, bestMonY = 0, bestMonW = 0, bestMonH = 0;
        int maxOverlap = -1;

        if (monitors != null) {
            int[] monX = new int[1], monY = new int[1];
            for (int i = 0; i < monitors.limit(); i++) {
                long monitor = monitors.get(i);
                org.lwjgl.glfw.GLFW.glfwGetMonitorPos(monitor, monX, monY);
                org.lwjgl.glfw.GLFWVidMode mode = org.lwjgl.glfw.GLFW.glfwGetVideoMode(monitor);
                if (mode == null) continue;
                int mx = monX[0], my = monY[0], mw = mode.width(), mh = mode.height();

                int overlapX = Math.max(0, Math.min(winX[0] + winW[0], mx + mw) - Math.max(winX[0], mx));
                int overlapY = Math.max(0, Math.min(winY[0] + winH[0], my + mh) - Math.max(winY[0], my));
                int overlap = overlapX * overlapY;
                if (overlap > maxOverlap) {
                    maxOverlap = overlap;
                    bestMonitor = monitor;
                    bestMonX = mx;
                    bestMonY = my;
                    bestMonW = mw;
                    bestMonH = mh;
                }
            }
        }

        if (bestMonitor != 0 && winW[0] > 0 && winH[0] > 0) {
            double monCenterX = bestMonX + bestMonW / 2.0;
            double monCenterY = bestMonY + bestMonH / 2.0;
            double relX = monCenterX - winX[0];
            double relY = monCenterY - winY[0];
            float guiScaledW = client.getWindow().getGuiScaledWidth();
            float guiScaledH = client.getWindow().getGuiScaledHeight();
            return new float[]{ (float)(relX * (guiScaledW / winW[0])), (float)(relY * (guiScaledH / winH[0])) };
        }
        return null;
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void ezclient$hideVanillaEffects(GuiGraphicsExtractor graphics, DeltaTracker tickDelta, CallbackInfo ci) {
        var potion = ModuleManager.getInstance().getPotionEffectModule();
        if (potion != null && potion.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void ezclient$customScoreboardSidebar(GuiGraphicsExtractor graphics, DeltaTracker tickDelta, CallbackInfo ci) {
        var scoreboardModule = ModuleManager.getInstance().getScoreboardModule();
        if (scoreboardModule != null && scoreboardModule.isEnabled()) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                var scoreboard = client.level.getScoreboard();
                var objective = scoreboard.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR);
                if (objective != null) {
                    scoreboardModule.renderCustomScoreboard(graphics, client, objective);
                    ci.cancel();
                }
            }
        }
    }
}
