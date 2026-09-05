package app.ezclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Lets Minecraft's 26.2 feature renderer merge compatible render-state groups. */
@Mixin(targets = "net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group")
public abstract class RenderBatchGroupMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), name = "canReorder", argsOnly = true)
    private static boolean ezclient$allowCompatibleStateReordering(boolean canReorder) {
        return true;
    }
}
