package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;

public class ToggleSprintModule extends Module {
    public ToggleSprintModule() {
        super("sprint", "ToggleSprint", "Automatically sprints when walking forward", "Movement", true, 4, 218, true, true);
    }

    @Override
    public int getWidth() {
        return 115;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options == null) return;

        if (client.options.forwardKey.isPressed() && !client.player.isSneaking() && !client.player.isTouchingWater()) {
            client.player.setSprinting(true);
        }
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.isSprinting()) return;

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

    @Override
    public void renderEditorPreview(float tickDelta) {
        if (!isShowHud()) return;
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
