package app.ezclient.mixin;

import app.ezclient.cosmetics.CommunityPresence;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the EzClient mark on the far left in the tab list as well. */
@Mixin(PlayerTabOverlay.class)
abstract class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void ezclient$addTabMark(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        if (name == null || !CommunityPresence.isEzClientPlayer(info.getProfile().id())) return;
        if (name.getString().startsWith("[EZ] ")) return;
        cir.setReturnValue(Component.literal("[EZ] ").withStyle(ChatFormatting.GREEN).append(name));
    }
}
