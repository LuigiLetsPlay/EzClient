package app.ezclient.mixin;

import net.minecraft.client.ClientBrandRetriever;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reports the client brand as "EzClient" to servers instead of "fabric". */
@Mixin(ClientBrandRetriever.class)
abstract class ClientBrandRetrieverMixin {
    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true)
    private static void ezclient$brand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("EzClient");
    }
}
