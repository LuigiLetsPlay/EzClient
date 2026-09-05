package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;

public class ToggleSprintModule extends Module {
    public ToggleSprintModule() {
        super("togglesprint", "Toggle Sprint", "Automatically keeps your player sprinting", "Movement", true, 4, 324, true, true);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.options != null) {
            //? if <=1.19.4 {
            /*boolean forward = client.options.keyForward.isPressed();
            *///?} else {
            boolean forward = client.options.forwardKey.isPressed();
            //?}
            if (forward && !client.player.isSneaking()) {
                client.player.setSprinting(true);
            }
        }
    }

    @Override
    public int getWidth() {
        return RenderUtils.getStringWidth("[Sprinting (Toggled)]") + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (client.player.isSprinting()) {
            String text = "[Sprinting (Toggled)]";
            int textWidth = RenderUtils.getStringWidth(text);
            int x = getPosX();
            int y = getPosY();
            int color = getTextColor(0xFF55FF55);

            if (isShowBackground()) {
                RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
            }
            RenderUtils.drawString(text, x + 1, y, color, true);
        }
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        String text = "[Sprinting (Toggled)]";
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFF55FF55);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
