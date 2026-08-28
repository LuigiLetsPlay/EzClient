package app.ezclient.mixin;

import app.ezclient.performance.visibility.EzVisibilityEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps compiled section masks correct after client-side block updates. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelVisibilityMixin {
    @Inject(method = "setBlock", at = @At("RETURN"))
    private void ezclient$updateOccluder(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            EzVisibilityEngine.INSTANCE.updateBlock((ClientLevel) (Object) this, pos, state);
        }
    }
}
