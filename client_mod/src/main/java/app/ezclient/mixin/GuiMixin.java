package app.ezclient.mixin;

import app.ezclient.gui.CrosshairModule;
import app.ezclient.gui.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
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
}
