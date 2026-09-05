package app.ezclient.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Transparency;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents IllegalArgumentException when server resource packs (e.g. HugoSMP)
 * specify UV rectangles out of sprite bounds during item model baking.
 */
@Mixin(NativeImage.class)
abstract class NativeImageMixin {
    @Shadow public abstract int getWidth();
    @Shadow public abstract int getHeight();

    @Inject(method = "computeTransparency(IIII)Lcom/mojang/blaze3d/platform/Transparency;", at = @At("HEAD"), cancellable = true)
    private void ezclient$clampOutOfBoundsTransparency(int x0, int y0, int x1, int y1, CallbackInfoReturnable<Transparency> cir) {
        int w = getWidth();
        int h = getHeight();
        if (x0 < 0 || y0 < 0 || x1 > w || y1 > h) {
            int clampedX0 = Math.max(0, Math.min(x0, w));
            int clampedY0 = Math.max(0, Math.min(y0, h));
            int clampedX1 = Math.max(clampedX0, Math.min(x1, w));
            int clampedY1 = Math.max(clampedY0, Math.min(y1, h));
            if (clampedX1 <= clampedX0 || clampedY1 <= clampedY0) {
                cir.setReturnValue(Transparency.NONE);
                return;
            }
            NativeImage self = (NativeImage) (Object) this;
            cir.setReturnValue(self.computeTransparency(clampedX0, clampedY0, clampedX1, clampedY1));
        }
    }
}
