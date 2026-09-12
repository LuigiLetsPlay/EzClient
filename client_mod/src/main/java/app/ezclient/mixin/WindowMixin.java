package app.ezclient.mixin;

import app.ezclient.EzClientMod;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Shadow
    public abstract long handle();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ezclient$earlyWindowInit(CallbackInfo ci) {
        long h = this.handle();
        if (h != 0L) {
            EzClientMod.applyEarlyWindowProperties(h);
        }
    }

    @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true, require = 0)
    private void ezclient$preventIconOverride(CallbackInfo ci) {
        long h = this.handle();
        if (h != 0L) {
            EzClientMod.applyEarlyWindowProperties(h);
        }
        ci.cancel();
    }
}
