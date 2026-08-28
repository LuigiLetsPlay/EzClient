package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityCapeManager;
import app.ezclient.cosmetics.CommunityPresence;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attaches the EzClient badge glyph cleanly in front of EzClient player name tags.
 * No wide space padding is added, keeping the logo snug and compatible with other mods (like Essential).
 */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ezclient$attachNameTagBadge(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (player == null || state == null) return;

        try {
            state.skin = CommunityCapeManager.replaceCape(state.skin, player.getUUID());

            if (state.nameTag != null && CommunityPresence.isEzClientPlayer(player.getUUID())) {
                if (state.nameTag.getString().indexOf('\uE000') < 0) {
                    MutableComponent badge = Component.literal("\uE000")
                            .withStyle(style -> style.withFont(new FontDescription.Resource(
                                    Identifier.fromNamespaceAndPath("ezclient", "default"))));
                    state.nameTag = Component.empty().append(badge).append(state.nameTag);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
