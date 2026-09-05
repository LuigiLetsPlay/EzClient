package app.ezclient.mixin;

import app.ezclient.gui.ModuleManager;
import app.ezclient.gui.TntTimerModule;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void ezclient$getTntTimerNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof PrimedTnt tnt) {
            TntTimerModule module = ModuleManager.getInstance().getTntTimerModule();
            if (module != null && module.isEnabled()) {
                cir.setReturnValue(module.getFormattedTimer(tnt));
            }
        }
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void ezclient$shouldShowTntTimerName(T entity, double distanceSq, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PrimedTnt) {
            TntTimerModule module = ModuleManager.getInstance().getTntTimerModule();
            if (module != null && module.isEnabled()) {
                cir.setReturnValue(distanceSq <= 4096.0);
            }
        }
    }
}
