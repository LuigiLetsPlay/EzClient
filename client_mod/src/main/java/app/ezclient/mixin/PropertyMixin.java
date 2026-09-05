package app.ezclient.mixin;

import com.mojang.authlib.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents SignatureException crashes and error log spam on servers like Hypixel
 * that send empty signature strings ("") on NPC skins and skull properties.
 */
@Mixin(value = Property.class, remap = false)
abstract class PropertyMixin {
    @Shadow public abstract String signature();

    @Inject(method = "hasSignature", at = @At("HEAD"), cancellable = true)
    private void ezclient$ignoreEmptySignature(CallbackInfoReturnable<Boolean> cir) {
        String sig = signature();
        if (sig == null || sig.isBlank()) {
            cir.setReturnValue(false);
        }
    }
}
