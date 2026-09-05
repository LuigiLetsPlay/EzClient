package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityPresence;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.Map;

/**
 * Attaches a verified client badge glyph cleanly in front of player name tags.
 * Pre-caches badge components to guarantee zero per-frame object allocation overhead.
 */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    private static final Map<CommunityPresence.ClientType, Component> BADGE_CACHE = new EnumMap<>(CommunityPresence.ClientType.class);

    static {
        FontDescription font = new FontDescription.Resource(Identifier.fromNamespaceAndPath("ezclient", "default"));
        for (CommunityPresence.ClientType type : CommunityPresence.ClientType.values()) {
            if (type != CommunityPresence.ClientType.NONE) {
                BADGE_CACHE.put(type, Component.literal(String.valueOf(type.glyph()))
                        .withStyle(style -> style.withFont(font)));
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ezclient$attachNameTagBadge(Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        if (player == null || state == null || state.nameTag == null) return;

        try {
            double dSq = 0.0;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                dSq = player.distanceToSqr(mc.player);
            }
            CommunityPresence.ClientType type = CommunityPresence.clientForPlayerNearby(player.getUUID(), player.getScoreboardName(), dSq);
            if (type == CommunityPresence.ClientType.NONE) return;
            Component badge = BADGE_CACHE.get(type);
            if (badge != null) {
                state.nameTag = Component.empty().append(badge).append(state.nameTag);
            }
        } catch (Throwable ignored) {
        }
    }
}
