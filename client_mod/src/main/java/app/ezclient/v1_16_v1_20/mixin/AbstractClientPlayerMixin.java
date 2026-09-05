package app.ezclient.v1_16_v1_20.mixin;

import app.ezclient.v1_16_v1_20.cosmetics.CapeManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getCapeTexture", at = @At("HEAD"), cancellable = true)
    private void onGetCapeTexture(CallbackInfoReturnable<Identifier> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        Identifier custom = CapeManager.getCape(player.getUuid());
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
