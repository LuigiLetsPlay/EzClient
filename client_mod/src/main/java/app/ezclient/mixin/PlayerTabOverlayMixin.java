package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityPresence;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shows a verified client badge in front of names; vanilla and unknown clients stay unchanged.
 * Pre-caches badges to eliminate per-frame allocations in tab overlays.
 */
@Mixin(net.minecraft.client.gui.components.PlayerTabOverlay.class)
abstract class PlayerTabOverlayMixin {
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

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void ezclient$badgeInTab(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        try {
            if (info == null || info.getProfile() == null) return;
            CommunityPresence.ClientType type = CommunityPresence.clientForPlayer(info.getProfile().id(), info.getProfile().name());
            if (type == CommunityPresence.ClientType.NONE) return;
            Component current = cir.getReturnValue();
            if (current == null) return;
            Component badge = BADGE_CACHE.get(type);
            if (badge != null) {
                cir.setReturnValue(Component.empty().append(badge).append(current));
            }
        } catch (Throwable ignored) {
        }
    }
}
