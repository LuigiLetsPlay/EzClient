package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityCapeManager;
import app.ezclient.gui.WaveyCapeModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies community textures and the optional articulated model at the cape layer. */
@Mixin(CapeLayer.class)
abstract class CapeLayerMixin {
    @Shadow @Final @Mutable private HumanoidModel<AvatarRenderState> model;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ezclient$installWaveyModel(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
            EntityModelSet models, EquipmentAssetManager equipment, CallbackInfo ci) {
        this.model = new WaveyCapeModel();
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At("HEAD"))
    private void ezclient$useCommunityCape(PoseStack pose, SubmitNodeCollector nodes, int light, AvatarRenderState state, float x, float y, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) return;
        if (Minecraft.getInstance().level.getEntity(state.id) instanceof AbstractClientPlayer player) {
            state.skin = CommunityCapeManager.replaceCape(state.skin, player.getUUID());
        }
    }
}
