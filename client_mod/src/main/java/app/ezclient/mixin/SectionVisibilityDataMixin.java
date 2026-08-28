package app.ezclient.mixin;

import app.ezclient.performance.visibility.EzVisibilityEngine;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Builds the solidity mask during Minecraft's existing compiler pass, with no second chunk scan. */
@Mixin(SectionCompiler.class)
public abstract class SectionVisibilityDataMixin {
    @Inject(method = "compile", at = @At("HEAD"))
    private void ezclient$beginOccluderCapture(SectionPos section, RenderSectionRegion region,
                                            VertexSorting sorting, SectionBufferBuilderPack buffers,
                                            CallbackInfoReturnable<SectionCompiler.Results> cir) {
        ClientLevel sourceLevel = ((RenderSectionRegionAccessor) (Object) region).ezclient$getLevel();
        EzVisibilityEngine.INSTANCE.beginSectionCapture(sourceLevel, section);
    }

    @Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState ezclient$captureCompiledBlock(RenderSectionRegion region, BlockPos pos) {
        BlockState state = region.getBlockState(pos);
        EzVisibilityEngine.INSTANCE.captureCompiledBlock(pos, state);
        return state;
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private void ezclient$finishOccluderCapture(SectionPos section, RenderSectionRegion region,
                                               VertexSorting sorting, SectionBufferBuilderPack buffers,
                                               CallbackInfoReturnable<SectionCompiler.Results> cir) {
        EzVisibilityEngine.INSTANCE.endSectionCapture();
    }
}
