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

            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();
            int x = width / 2;
            int y = height / 2;

            module.renderCrosshair(graphics, client, x, y, false);
            ci.cancel();
        }
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
