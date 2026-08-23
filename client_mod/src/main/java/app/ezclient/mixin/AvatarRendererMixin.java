package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityPresence;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the EzClient mark as the left-most part of a visible player name. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ezclient$addNameMark(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (state.nameTag == null || !CommunityPresence.isEzClientPlayer(player.getUUID())) return;
        if (state.nameTag.getString().startsWith("◆ ")) return;
        state.nameTag = Component.literal("◆ ").withStyle(ChatFormatting.LIGHT_PURPLE).append(state.nameTag);
    }
}
