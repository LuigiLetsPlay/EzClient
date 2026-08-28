package app.ezclient.mixin;

import app.ezclient.performance.culling.OcclusionCullingManager;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Builds the immutable solidity mask from the same safe snapshot used for chunk compilation. */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerOcclusionMixin {
    @Inject(method = "compile", at = @At("HEAD"))
    private void ezclient$captureOccluders(SectionPos section, RenderSectionRegion region,
                                            VertexSorting sorting, SectionBufferBuilderPack buffers,
                                            CallbackInfoReturnable<SectionCompiler.Results> cir) {
        ClientLevel sourceLevel = ((RenderSectionRegionAccessor) (Object) region).ezclient$getLevel();
        OcclusionCullingManager.INSTANCE.captureSection(sourceLevel, section, region);
    }
}
