package app.ezclient.mixin;

import app.ezclient.gui.EzAccountScreen;
import app.ezclient.gui.EzButton;
import app.ezclient.gui.EzScreenBridge;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Places the signature EzClient Accounts button in the top right corner
 * of the multiplayer servers screen.
 */
@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private EzButton ezclient$accountsButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void ezclient$addAccountsButton(CallbackInfo ci) {
        int btnW = 76;
        int btnH = 20;
        int x = 8;
        int y = 8;
        Identifier icon = Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ezclient.png");
        ezclient$accountsButton = new EzButton(
                x, y, btnW, btnH,
                Component.literal("Accounts"),
                icon,
                true,
                btn -> EzScreenBridge.set(this.minecraft, new EzAccountScreen(this))
        );
        this.addRenderableWidget(ezclient$accountsButton);
    }

    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void ezclient$updateAccountsButtonPos(CallbackInfo ci) {
        if (ezclient$accountsButton != null) {
            ezclient$accountsButton.setPosition(8, 8);
        }
    }
}
