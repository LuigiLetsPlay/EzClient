package app.ezclient.mixin;

import app.ezclient.gui.ConfigManager;
import app.ezclient.gui.EzUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Interactive resize handle positioned right at the top-right corner of the active chat box.
 * Dragging it live-scales Minecraft's native chat scale (fonts, line wrap, size).
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Unique private boolean ezIsDraggingChatHandle = false;
    @Unique private double ezStartScale = 1.0;
    @Unique private double ezStartX = 0, ezStartY = 0;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private int getChatTopRightX(Minecraft mc) {
        double scale = mc.options.chatScale().get();
        int chatBoxW = (int) (ChatComponent.getWidth(mc.options.chatWidth().get()) * scale);
        return 4 + chatBoxW;
    }

    @Unique
    private int getChatTopRightY(Minecraft mc) {
        double scale = mc.options.chatScale().get();
        int chatBoxH = (int) (ChatComponent.getHeight(mc.options.chatHeightFocused().get()) * scale);
        return (height - 40) - chatBoxH;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ezRenderAndHandleChatResize(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int handleX = getChatTopRightX(mc) - 4;
        int handleY = getChatTopRightY(mc) - 4;

        // Handle Active Dragging fallback
        if (ezIsDraggingChatHandle) {
            if (mc.mouseHandler != null && mc.mouseHandler.isLeftPressed()) {
                double deltaDist = ((mouseX - ezStartX) - (mouseY - ezStartY)) / 2.0;
                double newScale = Math.max(0.2, Math.min(1.0, ezStartScale + deltaDist / 120.0));
                newScale = Math.round(newScale * 20.0) / 20.0;
                if (Math.abs(newScale - mc.options.chatScale().get()) > 0.01) {
                    mc.options.chatScale().set(newScale);
                    if (mc.gui != null && mc.gui.hud != null) {
                        mc.gui.hud.getChat().rescaleChat();
                    }
                    ConfigManager.chatScale = newScale;
                }
            } else {
                ezIsDraggingChatHandle = false;
                mc.options.save();
                ConfigManager.save();
            }
        }

        boolean hovered = mouseX >= handleX - 4 && mouseX <= handleX + 16 && mouseY >= handleY - 4 && mouseY <= handleY + 16;
        EzUi.roundedRect(graphics, handleX, handleY, 12, 12, 3, hovered || ezIsDraggingChatHandle ? 0xFF22C96E : 0xC01A2433);
        graphics.outline(handleX, handleY, 12, 12, hovered || ezIsDraggingChatHandle ? 0xFFFFFFFF : 0xFF35414D);
        
        // Clean white corner handle lines (triangle/corner grip)
        graphics.fill(handleX + 3, handleY + 3, handleX + 9, handleY + 4, 0xFFFFFFFF);
        graphics.fill(handleX + 8, handleY + 3, handleX + 9, handleY + 9, 0xFFFFFFFF);
        graphics.fill(handleX + 5, handleY + 7, handleX + 7, handleY + 8, 0xFFFFFFFF);

        if (hovered || ezIsDraggingChatHandle) {
            String scaleStr = String.format(java.util.Locale.ROOT, "%d%% (R-Klick Reset)", (int) (mc.options.chatScale().get() * 100));
            EzUi.roundedRect(graphics, handleX + 16, handleY - 2, 90, 14, 3, 0xF0121722);
            graphics.centeredText(font, Component.literal(scaleStr), handleX + 61, handleY + 1, 0xFF43DD8C);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ezHandleChatHandleClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        int handleX = getChatTopRightX(mc) - 4;
        int handleY = getChatTopRightY(mc) - 4;

        if (event.x() >= handleX - 4 && event.x() <= handleX + 16 && event.y() >= handleY - 4 && event.y() <= handleY + 16) {
            if (event.button() == 1) {
                // Right Click -> Reset to default 100%
                mc.options.chatScale().set(1.0);
                if (mc.gui != null && mc.gui.hud != null) {
                    mc.gui.hud.getChat().rescaleChat();
                }
                ConfigManager.chatScale = 1.0;
                mc.options.save();
                ConfigManager.save();
                cir.setReturnValue(true);
            } else if (event.button() == 0) {
                ezIsDraggingChatHandle = true;
                ezStartScale = mc.options.chatScale().get();
                ezStartX = event.x();
                ezStartY = event.y();
                cir.setReturnValue(true);
            }
        }
    }
}
