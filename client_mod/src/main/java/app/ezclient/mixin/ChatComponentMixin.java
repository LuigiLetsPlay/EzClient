package app.ezclient.mixin;

import app.ezclient.gui.AutoGgModule;
import app.ezclient.gui.ChatCustomizerModule;
import app.ezclient.gui.ModuleManager;
import app.ezclient.gui.XaeroWaypointShare;
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

        Component decorated = XaeroWaypointShare.decorate(message);
        ChatCustomizerModule customizer = ModuleManager.getInstance().getChatCustomizerModule();
        if (customizer != null && customizer.isEnabled()) {
            return customizer.appendTimestamp(decorated);
        }

        return decorated;
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

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "isChatFocused",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ezclient$forceFocusedForDummy(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (ChatCustomizerModule.isDummyChat((ChatComponent)(Object)this)) {
            cir.setReturnValue(true);
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;", shift = At.Shift.AFTER)
    )
    private void ezclient$offsetChatPosition(net.minecraft.client.gui.GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, int tickCount, int mouseX, int mouseY, ChatComponent.DisplayMode displayMode, boolean isRestricted, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (ChatCustomizerModule.isDummyChat((ChatComponent)(Object)this)) {
            return;
        }
        ChatCustomizerModule customizer = ModuleManager.getInstance().getChatCustomizerModule();
        if (customizer != null && customizer.isEnabled()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getWindow() != null) {
                int defY = mc.getWindow().getGuiScaledHeight() - 40 - customizer.getHeight(mc);
                float scale = (float) customizer.getScale();
                graphics.pose().translate(customizer.getX(), customizer.getY());
                if (scale != 1.0f) {
                    graphics.pose().scale(scale, scale);
                }
                graphics.pose().translate(-4, -defY);
            }
        }
    }

    @ModifyVariable(
            method = "captureClickableText(Lnet/minecraft/client/gui/ActiveTextCollector;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private int ezclient$offsetClickX(int mouseX) {
        ChatCustomizerModule customizer = ModuleManager.getInstance().getChatCustomizerModule();
        if (customizer != null && customizer.isEnabled()) {
            double scale = customizer.getScale();
            if (scale <= 0.0) scale = 1.0;
            return (int) Math.round(4.0 + (mouseX - customizer.getX()) / scale);
        }
        return mouseX;
    }

    @ModifyVariable(
            method = "captureClickableText(Lnet/minecraft/client/gui/ActiveTextCollector;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At("HEAD"),
            ordinal = 1,
            argsOnly = true
    )
    private int ezclient$offsetClickY(int mouseY) {
        ChatCustomizerModule customizer = ModuleManager.getInstance().getChatCustomizerModule();
        if (customizer != null && customizer.isEnabled()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.getWindow() != null) {
                int defY = mc.getWindow().getGuiScaledHeight() - 40 - customizer.getHeight(mc);
                double scale = customizer.getScale();
                if (scale <= 0.0) scale = 1.0;
                return (int) Math.round(defY + (mouseY - customizer.getY()) / scale);
            }
        }
        return mouseY;
    }
}
