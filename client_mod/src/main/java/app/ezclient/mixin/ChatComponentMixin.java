package app.ezclient.mixin;

import app.ezclient.gui.AutoGgModule;
import app.ezclient.gui.ChatCustomizerModule;
import app.ezclient.gui.ModuleManager;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component ezclient$modifyIncomingMessage(Component message) {
        if (message == null) return null;

        AutoGgModule autoGg = ModuleManager.getInstance().getAutoGgModule();
        if (autoGg != null && autoGg.isEnabled()) {
            autoGg.onChatMessage(message.getString());
        }

        ChatCustomizerModule customizer = ModuleManager.getInstance().getChatCustomizerModule();
        if (customizer != null && customizer.isEnabled()) {
            return customizer.appendTimestamp(message);
        }

        return message;
    }

    @ModifyConstant(
            method = {"addMessageToQueue", "addMessageToDisplayQueue"},
            constant = @Constant(intValue = 100)
    )
    private int ezclient$expandChatLimit(int original) {
        ChatCustomizerModule customizer = ModuleManager.getInstance().getChatCustomizerModule();
        if (customizer != null && customizer.isEnabled()) {
            return customizer.getLineLimit();
        }
        return original;
    }

    @Redirect(
            method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;textBackgroundOpacity()Lnet/minecraft/client/OptionInstance;")
    )
    private OptionInstance<Double> ezclient$overrideTextBackgroundOpacity(Options options) {
        ChatCustomizerModule customizer = ModuleManager.getInstance().getChatCustomizerModule();
        if (customizer != null && customizer.isEnabled()) {
            double customVal = customizer.getBackgroundOpacity() / 100.0;
            return new OptionInstance<>("custom_opacity", OptionInstance.noTooltip(), (c, val) -> Component.empty(), OptionInstance.UnitDouble.INSTANCE, customVal, v -> {});
        }
        return options.textBackgroundOpacity();
    }
}
