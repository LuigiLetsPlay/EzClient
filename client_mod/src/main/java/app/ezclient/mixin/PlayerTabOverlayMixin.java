package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityPresence;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shows the EzClient badge glyph in front of EzClient names in the tab list cleanly and snugly. */
@Mixin(net.minecraft.client.gui.components.PlayerTabOverlay.class)
abstract class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void ezclient$badgeInTab(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        try {
            if (info == null || info.getProfile() == null) return;
            if (!CommunityPresence.isEzClientPlayer(info.getProfile().id())) return;
            Component current = cir.getReturnValue();
            if (current == null || current.getString().indexOf('\uE000') >= 0) return;
            MutableComponent badge = Component.literal("\uE000")
                    .withStyle(style -> style.withFont(new FontDescription.Resource(
                            Identifier.fromNamespaceAndPath("ezclient", "default"))));
            cir.setReturnValue(Component.empty().append(badge).append(current));
        } catch (Throwable ignored) {
        }
    }
}
