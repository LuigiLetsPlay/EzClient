package app.ezclient.mixin;
import app.ezclient.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value = AvatarRenderer.class, priority = 900)
public class NameplateCustomizerMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void ezclient$own(Avatar player, double distance, CallbackInfoReturnable<Boolean> cir) {
        var m = FeatureModule.get(NameplateModule.class); var mc = Minecraft.getInstance();
        if (m.isEnabled() && m.flag("own") && player == mc.player && !mc.options.getCameraType().isFirstPerson() && !player.isInvisible() && !EzScreenBridge.hudHidden(mc)) cir.setReturnValue(true);
    }
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void ezclient$decorate(Avatar player, AvatarRenderState state, float delta, CallbackInfo ci) {
        var m = FeatureModule.get(NameplateModule.class);
        if (!m.isEnabled() || state.nameTag == null) return;
        var name = state.nameTag.copy();
        if (m.isCustomFont()) name.withStyle(style -> style.withFont(new net.minecraft.network.chat.FontDescription.Resource(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("ezclient", "smooth"))));
        if (m.friend(player.getScoreboardName())) name.withStyle(style -> style.withColor(m.tint("friendColor", false) & 0xffffff));
        if (!m.text("health").equals("Hidden")) name.append(Component.literal(String.format(java.util.Locale.ROOT, " %.1f%s",
            m.text("health").equals("Hearts") ? player.getHealth() / 2 : player.getHealth(), m.text("health").equals("Hearts") ? " ♥" : " HP")));
        state.nameTag = name;
        if (player == Minecraft.getInstance().player && !m.text("prefix").isBlank()) state.scoreText = m.styledText(m.text("prefix"));
    }
    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void ezclient$style(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        var m = FeatureModule.get(NameplateModule.class);
        if (!m.isEnabled()) return;
        ci.cancel();
        if (EzScreenBridge.hudHidden(Minecraft.getInstance()) || state.nameTag == null || state.nameTagAttachment == null) return;
        pose.pushPose();
        try {
            var p = state.nameTagAttachment;
            pose.translate(p.x, p.y + .5, p.z); pose.mulPose(camera.orientation);
            float scale = .025f * (float)m.getScale(); pose.scale(scale, -scale, scale);
            Font font = Minecraft.getInstance().font;
            int bg = m.flag("nameBackground") ? m.getBackgroundColor() : 0;
            collector.submitText(pose, -font.width(state.nameTag) / 2f, 0, state.nameTag.getVisualOrderText(), m.flag("nameShadow"), Font.DisplayMode.NORMAL, state.lightCoords, m.color(), bg, 0);
            if (state.scoreText != null) collector.submitText(pose, -font.width(state.scoreText) / 2f, -12, state.scoreText.getVisualOrderText(), m.flag("nameShadow"), Font.DisplayMode.NORMAL, state.lightCoords, m.color(), bg, 0);
        } finally { pose.popPose(); }
    }
}
