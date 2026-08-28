package app.ezclient.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.joml.Matrix4f;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draws a high-resolution EzClient logo left of the complete rendered name
 * tag text. Because it is placed left of the whole text, it never overlaps
 * badges that other mods (e.g. Essential) prepend to the same name.
 */
@Mixin(NameTagFeatureRenderer.class)
abstract class EzNameTagLogoRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("EzClient");
    private static final Identifier LOGO =
            Identifier.fromNamespaceAndPath("ezclient", "textures/font/badge_hq.png");
    private static boolean warned;

    @Inject(method = "buildGroup", at = @At("TAIL"))
    private void ezclient$drawLogo(FeatureFrameContext context,
                                   List<NameTagFeatureRenderer.Submit> submits,
                                   CallbackInfo ci) {
        if (submits == null) return;

        for (NameTagFeatureRenderer.Submit submit : submits) {
            Component text = submit.text();
            if (text == null || text.getString().indexOf('\uE000') < 0) continue;

            try {
                float size = 9.0F;                 // matches name tag text height
                float gap = 1.5F;
                float x0 = submit.x() - gap - size;
                float y0 = submit.y() - 0.5F;

                Matrix4f matrix = new Matrix4f(submit.pose());
                VertexConsumer vc = ((VertexBuilderAccessor) (Object) this)
                        .ezclient$getVertexBuilder(RenderTypes.text(LOGO));

                vc.addVertex(matrix, x0, y0 + size, 0).setColor(255, 255, 255, 255)
                        .setUv(0F, 1F).setUv2(240, 240).setNormal(0F, 0F, 1F);
                vc.addVertex(matrix, x0 + size, y0 + size, 0).setColor(255, 255, 255, 255)
                        .setUv(1F, 1F).setUv2(240, 240).setNormal(0F, 0F, 1F);
                vc.addVertex(matrix, x0 + size, y0, 0).setColor(255, 255, 255, 255)
                        .setUv(1F, 0F).setUv2(240, 240).setNormal(0F, 0F, 1F);
                vc.addVertex(matrix, x0, y0, 0).setColor(255, 255, 255, 255)
                        .setUv(0F, 0F).setUv2(240, 240).setNormal(0F, 0F, 1F);
            } catch (Throwable t) {
                if (!warned) {
                    warned = true;
                    LOGGER.error("EzClient logo overlay failed", t);
                }
            }
        }
    }
}
