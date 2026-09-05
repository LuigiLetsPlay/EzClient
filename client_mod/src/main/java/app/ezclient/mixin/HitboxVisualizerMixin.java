package app.ezclient.mixin;

import app.ezclient.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public class HitboxVisualizerMixin {
    @Inject(method = "emitGizmos", at = @At("HEAD"), cancellable = true)
    private void ezclient$hitboxes(CallbackInfo ci) {
        var module = FeatureModule.get(HitboxModule.class);
        if (!module.isEnabled()) return;
        if (module.flag("debugOnly")) WorldVisuals.hitboxes(Minecraft.getInstance(), module);
        ci.cancel();
    }
}
