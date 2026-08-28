package app.ezclient.mixin;

import app.ezclient.gui.ClearGlassModule;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HalfTransparentBlock.class)
public abstract class ClearGlassMixin {
    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void ezclient$skipGlassFaces(BlockState state, BlockState neighborState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (ClearGlassModule.isConnectedRenderingActive() && neighborState.is(state.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}
