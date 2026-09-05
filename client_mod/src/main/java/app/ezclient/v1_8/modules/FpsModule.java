package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;

public class FpsModule extends Module {
    public FpsModule() {
        super("fps", "FPS Display", "Displays your current frames per second", "HUD", true, 4, 4, true);
    }

    @Override
    public int getWidth() {
        int fps = MinecraftClient.getCurrentFps();
        String text = "FPS: " + fps;
        return RenderUtils.getStringWidth(text) + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        int fps = MinecraftClient.getCurrentFps();
        String text = "FPS: " + fps;
        
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
