package app.ezclient.mixin;

import app.ezclient.gui.EzScreenBridge;
import app.ezclient.gui.WaypointScreen;
import app.ezclient.gui.XaeroWaypointShare;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Handles EzClient's local chat actions; the former chat resize grip is intentionally removed. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void ezclient$openSharedWaypoint(Style style, boolean insertionClick, CallbackInfoReturnable<Boolean> cir) {
        if (style == null || !(style.getClickEvent() instanceof ClickEvent.RunCommand command)) return;
        XaeroWaypointShare share = XaeroWaypointShare.fromCommand(command.command());
        if (share == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            cir.setReturnValue(true);
            return;
        }
        EzScreenBridge.set(mc, WaypointScreen.fromShared((ChatScreen) (Object) this, share));
        cir.setReturnValue(true);
    }
}
