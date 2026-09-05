package app.ezclient.mixin;

import app.ezclient.gui.FovChangerModule;
import app.ezclient.gui.ModuleManager;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void ezclient$modifyFov(boolean firstPerson, float partialTick, CallbackInfoReturnable<Float> cir) {
        FovChangerModule mod = ModuleManager.getInstance().getFovChangerModule();
        if (mod != null && mod.isEnabled()) {
            float vanilla = cir.getReturnValue();
            cir.setReturnValue(mod.modifyFov((AbstractClientPlayer) (Object) this, vanilla, partialTick));
        }
    }
}
