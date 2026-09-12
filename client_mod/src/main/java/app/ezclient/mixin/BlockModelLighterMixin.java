package app.ezclient.mixin;

import app.ezclient.gui.GlowingOresModule;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelLighter.class)
public abstract class BlockModelLighterMixin {
    private static final int FULL_BRIGHT = 0x00F000F0;

    @Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
    private void ezclient$getOreLightCoords(BlockState state, BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (state != null && GlowingOresModule.isEmissive(state.getBlock())) {
            cir.setReturnValue(FULL_BRIGHT);
            return;
        }
        if (GlowingOresModule.isDynamicLightActive()) {
            int boost = GlowingOresModule.getNeighborEmissiveLight(level, pos);
            if (boost > 0) {
                cir.setReturnValue((boost << 4) | 0x00F00000);
            }
        }
    }

    @Inject(method = "prepareQuadAmbientOcclusion", at = @At("TAIL"))
    private void ezclient$prepareQuadAoEmissive(BlockAndTintGetter level, BlockState state, BlockPos pos, BakedQuad quad, QuadInstance quadInstance, CallbackInfo ci) {
        if (state != null && quadInstance != null && GlowingOresModule.isEmissive(state.getBlock())) {
            quadInstance.setLightCoords(FULL_BRIGHT);
            quadInstance.setColor(0xFFFFFFFF);
        }
    }

    @Inject(method = "prepareQuadFlat", at = @At("TAIL"))
    private void ezclient$prepareQuadFlatEmissive(BlockAndTintGetter level, BlockState state, BlockPos pos, int defaultLightCoords, BakedQuad quad, QuadInstance quadInstance, CallbackInfo ci) {
        if (state != null && quadInstance != null && GlowingOresModule.isEmissive(state.getBlock())) {
            quadInstance.setLightCoords(FULL_BRIGHT);
            quadInstance.setColor(0xFFFFFFFF);
        }
    }
}
