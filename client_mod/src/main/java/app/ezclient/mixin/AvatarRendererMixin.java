package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityPresence;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Avatar;

import org.joml.Matrix4f;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the real EzClient logo left of a visible player name tag. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    private static final Identifier ICON =
            Identifier.fromNamespaceAndPath("ezclient", "textures/gui/sprites/title/icon.png");

    // Render states only carry the numeric entity id; remember which ids are
    // EzClient players during state extraction.
    private static final Map<Integer, Boolean> EZ_IDS = new ConcurrentHashMap<>();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ezclient$rememberEzPlayer(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        EZ_IDS.put(state.id, CommunityPresence.isEzClientPlayer(player.getUUID()));
    }

    @Inject(method = "submitNameDisplay", at = @At("TAIL"))
    private void ezclient$nameIcon(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector,
                                   CameraRenderState camera, CallbackInfo ci) {
        if (state == null || state.nameTag == null) return;
        if (!Boolean.TRUE.equals(EZ_IDS.get(state.id))) return;

        // The logo must never break player rendering (skins/names). If anything
        // goes wrong while drawing it, silently skip this frame's icon.
        try {
            Font font = Minecraft.getInstance().font;
            float nameWidth = font.width(state.nameTag);
            float iconSize = 8.0F;
            float gap = 2.5F;

            pose.pushPose();
            // Name tags are centered; move to the left edge of the text and up
            // a little so the icon sits on the same baseline.
            pose.translate(-(nameWidth / 2.0F + gap + iconSize), -6.5F, 0.04F);

            collector.submitCustomGeometry(pose, RenderTypes.text(ICON), (poseStackPose, vertexConsumer) -> {
                try {
                    Matrix4f matrix = poseStackPose.pose();
                    vertexConsumer.addVertex(matrix, 0, iconSize, 0).setColor(255, 255, 255, 255)
                            .setUv(0F, 1F).setUv2(240, 240).setNormal(poseStackPose, 0F, 0F, 1F);
                    vertexConsumer.addVertex(matrix, iconSize, iconSize, 0).setColor(255, 255, 255, 255)
                            .setUv(1F, 1F).setUv2(240, 240).setNormal(poseStackPose, 0F, 0F, 1F);
                    vertexConsumer.addVertex(matrix, iconSize, 0, 0).setColor(255, 255, 255, 255)
                            .setUv(1F, 0F).setUv2(240, 240).setNormal(poseStackPose, 0F, 0F, 1F);
                    vertexConsumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255)
                            .setUv(0F, 0F).setUv2(240, 240).setNormal(poseStackPose, 0F, 0F, 1F);
                } catch (Throwable ignored) { }
            });

            pose.popPose();
        } catch (Throwable ignored) {
            try { pose.popPose(); } catch (Throwable ignoredToo) { }
        }
    }
}
