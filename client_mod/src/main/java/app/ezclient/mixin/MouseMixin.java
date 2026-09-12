package app.ezclient.mixin;

import app.ezclient.EzClientMod;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void ezclient$freelook(LocalPlayer player, double x, double y) {
        var freelook = app.ezclient.gui.FeatureModule.get(app.ezclient.gui.FreelookModule.class);
        if (freelook.isActive()) freelook.turn(x, y); else player.turn(x, y);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void ezclient$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (app.ezclient.gui.BlockSelectionOverlay.isActive() && vertical != 0.0) {
            app.ezclient.gui.BlockSelectionOverlay.onMouseScrolled(vertical);
            ci.cancel();
            return;
        }
        var zoom = app.ezclient.gui.ModuleManager.getInstance().getZoomModule();
        if (zoom.isEnabled() && EzClientMod.isZooming() && vertical != 0.0) {
            zoom.adjustScrollZoom(vertical * zoom.getScrollSensitivity());
            ci.cancel();
        }
    }
}
