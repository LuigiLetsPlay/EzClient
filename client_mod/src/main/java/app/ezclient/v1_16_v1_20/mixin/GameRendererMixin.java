package app.ezclient.v1_16_v1_20.mixin;

import app.ezclient.v1_16_v1_20.EzClientMod_1_16_1_20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
    "net.minecraft.client.render.GameRenderer",
    "net.minecraft.client.renderer.GameRenderer"
})
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(CallbackInfoReturnable<Double> cir) {
        float factor = EzClientMod_1_16_1_20.getZoomFovFactor();
        if (factor < 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * factor);
        }
    }
}
