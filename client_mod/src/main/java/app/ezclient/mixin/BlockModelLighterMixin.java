package app.ezclient.mixin;

import app.ezclient.gui.GlowingOresModule;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelLighter.class)
public abstract class BlockModelLighterMixin {
    @Inject(method = "getLightCoords", at = @At("RETURN"), cancellable = true)
    private void ezclient$getOreLightCoords(BlockState state, BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (GlowingOresModule.isDynamicLightActive()) {
            int boost = GlowingOresModule.getNeighborEmissiveLight(level, pos);
            if (boost > 0) {
                int current = cir.getReturnValue();
                int currentBlock = current >> 4 & 0xF;
                if (boost > currentBlock) {
                    cir.setReturnValue((current & 0x00F00000) | boost << 4);
                }
            }
        }
    }
}
