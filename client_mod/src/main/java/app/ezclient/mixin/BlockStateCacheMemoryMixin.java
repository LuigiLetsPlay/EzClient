package app.ezclient.mixin;

import app.ezclient.performance.memory.BooleanArrayPool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Deduplicates equal immutable direction flags stored by every cached block state. */
@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
public abstract class BlockStateCacheMemoryMixin {
    @Shadow @Final @Mutable private boolean[] faceSturdy;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ezclient$canonicalizeDirectionFlags(CallbackInfo ci) {
        faceSturdy = BooleanArrayPool.intern(faceSturdy);
    }
}
