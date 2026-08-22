package app.ezclient.mixin;

import app.ezclient.EzClientMod;
import app.ezclient.gui.ConfigManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void ezclient$adjustZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        var zoom = app.ezclient.gui.ModuleManager.getInstance().getZoomModule();
        if (zoom.isEnabled() && EzClientMod.isZooming() && vertical != 0.0) {
            zoom.setZoomLevel(zoom.getZoomLevel() + vertical * zoom.getScrollSensitivity());
            ConfigManager.save();
            ci.cancel();
        }
    }
}
