package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityCapeManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Final render-state hook: this is the texture CapeLayer actually uses. */
@Mixin(CapeLayer.class)
abstract class CapeLayerMixin {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At("HEAD"))
    private void ezclient$useCommunityCape(PoseStack pose, SubmitNodeCollector nodes, int light, AvatarRenderState state, float x, float y, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) return;
        if (Minecraft.getInstance().level.getEntity(state.id) instanceof AbstractClientPlayer player)
            state.skin = CommunityCapeManager.replaceCape(state.skin, player.getUUID());
    }
}
