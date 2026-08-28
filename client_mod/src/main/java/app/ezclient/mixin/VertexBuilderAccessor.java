package app.ezclient.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the protected vertex builder helper of all feature renderers. */
@Mixin(RenderTypeFeatureRenderer.class)
public interface VertexBuilderAccessor {
    @Invoker("getVertexBuilder")
    VertexConsumer ezclient$getVertexBuilder(RenderType type);
}
