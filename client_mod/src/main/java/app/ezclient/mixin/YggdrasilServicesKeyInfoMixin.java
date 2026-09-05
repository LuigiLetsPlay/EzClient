package app.ezclient.mixin;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures validateProperty safely skips verification if a property has an empty signature,
 * preventing SignatureException stack traces in the client logs.
 */
@Mixin(value = YggdrasilServicesKeyInfo.class, remap = false)
abstract class YggdrasilServicesKeyInfoMixin {
    @Inject(method = "validateProperty", at = @At("HEAD"), cancellable = true)
    private void ezclient$skipEmptySignature(Property property, CallbackInfoReturnable<Boolean> cir) {
        if (property == null || property.signature() == null || property.signature().isBlank()) {
            cir.setReturnValue(false);
        }
    }
}
