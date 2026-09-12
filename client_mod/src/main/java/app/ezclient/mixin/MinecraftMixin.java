package app.ezclient.mixin;

import app.ezclient.EzClientMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "createTitle", at = @At("HEAD"), cancellable = true)
    private void ezclient$customWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(EzClientMod.CLIENT_TITLE);
    }

    @Inject(method = "pauseGame", at = @At("HEAD"), cancellable = true)
    private void ezclient$onPauseGame(boolean pause, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (app.ezclient.gui.BlockSelectionOverlay.isActive()) {
            app.ezclient.gui.BlockSelectionOverlay.cancel((Minecraft) (Object) this);
            ci.cancel();
        }
    }
}
