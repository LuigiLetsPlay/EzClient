package app.ezclient.mixin;

import app.ezclient.gui.EzHubScreen;
import app.ezclient.gui.EzScreenBridge;
import app.ezclient.gui.EzButton;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds EzClient to Minecraft 26.2's dynamically centered title icon row. */
@Mixin(value = TitleScreen.class, priority = 900)
public abstract class TitleScreenMixin {
    @Shadow
    protected abstract int getHorizontalPosition(int currentButton, int numberOfButtons, int buttonWidth);

    @Definition(id = "numberOfButtons", local = @Local(type = int.class, name = "numberOfButtons"))
    @Expression("numberOfButtons = ?")
    @Inject(method = "init", at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER))
    private void ezclient$includeIconInLayout(
            CallbackInfo ci,
            @Local(name = "numberOfButtons") LocalIntRef numberOfButtons
    ) {
        numberOfButtons.set(numberOfButtons.get() + 1);
    }

    @WrapOperation(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/TitleScreen;getHorizontalPosition(III)I"
            )
    )
    private int ezclient$useExpandedIconCount(
            TitleScreen instance,
            int currentButton,
            int ignoredButtonCount,
            int buttonWidth,
            Operation<Integer> original,
            @Local(name = "numberOfButtons") int numberOfButtons
    ) {
        return original.call(instance, currentButton, numberOfButtons, buttonWidth);
    }

    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/SpriteIconButton;setPosition(II)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void ezclient$addCenteredIcon(
            CallbackInfo ci,
            @Local(name = "currentButton") LocalIntRef currentButton,
            @Local(name = "topPos") int topPos,
            @Local(name = "numberOfButtons") int numberOfButtons
    ) {
        currentButton.set(currentButton.get() + 1);

        Screen titleScreen = (TitleScreen) (Object) this;
        Component tooltip = app.ezclient.util.EzI18n.comp("ezclient.title.tooltip", "EzClient Modules");
        net.minecraft.resources.Identifier icon = net.minecraft.resources.Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ezclient.png");
        EzButton button = new EzButton(
                0, 0, 20, 20, Component.empty(), icon, true,
                ignored -> EzScreenBridge.set(Minecraft.getInstance(), new EzHubScreen(titleScreen))
        );

        button.setPosition(
                getHorizontalPosition(currentButton.get(), numberOfButtons, 20),
                topPos
        );
        Screens.getWidgets(titleScreen).add(button);
    }
}
