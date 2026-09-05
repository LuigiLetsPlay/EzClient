package app.ezclient.v1_8.mixin;

import app.ezclient.v1_8.EzClientMod_1_8;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(float tickDelta, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float factor = EzClientMod_1_8.getZoomFovFactor();
        if (factor < 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * factor);
        }
    }
}
