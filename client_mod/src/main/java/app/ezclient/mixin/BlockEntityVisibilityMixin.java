package app.ezclient.mixin;

import app.ezclient.performance.visibility.EzVisibilityEngine;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cancels before a block entity renderer allocates or populates its render state. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityVisibilityMixin {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void ezclient$cullBlockEntity(
            E blockEntity, float tickDelta, ModelFeatureRenderer.CrumblingOverlay breakProgress,
            boolean globallyRendered, CallbackInfoReturnable<S> cir) {
        if (!EzVisibilityEngine.INSTANCE.shouldRender(blockEntity, globallyRendered)) {
            cir.setReturnValue(null);
        }
    }
}
