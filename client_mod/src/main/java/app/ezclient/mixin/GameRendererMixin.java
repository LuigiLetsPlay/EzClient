package app.ezclient.mixin;

import app.ezclient.EzClientMod;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class GameRendererMixin {
    private float ezclient$animatedFactor = 1.0F;

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void ezclient$applyZoom(float tickDelta, CallbackInfoReturnable<Float> cir) {
        var zoom = app.ezclient.gui.ModuleManager.getInstance().getZoomModule();
        float target = zoom.isEnabled() && EzClientMod.isZooming() ? (float) zoom.getZoomLevel() : 1.0F;
        if (zoom.isSmoothZoom()) {
            ezclient$animatedFactor += (target - ezclient$animatedFactor) * 0.28F;
        } else {
            ezclient$animatedFactor = target;
        }
        if (ezclient$animatedFactor > 1.001F) {
            cir.setReturnValue(cir.getReturnValueF() / ezclient$animatedFactor);
        }
    }
}
