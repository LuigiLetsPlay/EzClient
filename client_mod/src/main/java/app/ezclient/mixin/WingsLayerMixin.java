package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityCapeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the custom community cape texture to Elytra wings as well. */
@Mixin(WingsLayer.class)
abstract class WingsLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"))
    private void ezclient$useCommunityElytra(PoseStack pose, SubmitNodeCollector nodes, int light, HumanoidRenderState state, float x, float y, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatarState && Minecraft.getInstance().level != null) {
            if (Minecraft.getInstance().level.getEntity(avatarState.id) instanceof AbstractClientPlayer player) {
                avatarState.skin = CommunityCapeManager.replaceCape(avatarState.skin, player.getUUID());
            }
        }
    }
}
