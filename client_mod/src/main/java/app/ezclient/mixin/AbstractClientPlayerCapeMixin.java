package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityCapeManager;
import app.ezclient.cosmetics.ActiveSkinManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces only the cape texture; body, model and Mojang fallback remain untouched. */
@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerCapeMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void ezclient$communityCape(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        PlayerSkin local = ActiveSkinManager.replaceLocalSkin(cir.getReturnValue(), self.getUUID());
        cir.setReturnValue(CommunityCapeManager.replaceCape(local, self.getUUID()));
    }
}
