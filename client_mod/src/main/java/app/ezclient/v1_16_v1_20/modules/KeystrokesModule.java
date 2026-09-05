package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;

public class KeystrokesModule extends Module {
    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "Displays WASD and jump keys on your HUD", "HUD", true, 4, 38, true);
    }

    @Override
    public int getWidth() {
        return 58;
    }

    @Override
    public int getHeight() {
        return 58;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) return;

        int x = getPosX();
        int y = getPosY();

        //? if <=1.19.4 {
        /*boolean w = client.options.keyForward.isPressed();
        boolean s = client.options.keyBack.isPressed();
        boolean a = client.options.keyLeft.isPressed();
        boolean d = client.options.keyRight.isPressed();
        boolean jump = client.options.keyJump.isPressed();
        *///?} else {
        boolean w = client.options.forwardKey.isPressed();
        boolean s = client.options.backKey.isPressed();
        boolean a = client.options.leftKey.isPressed();
        boolean d = client.options.rightKey.isPressed();
        boolean jump = client.options.jumpKey.isPressed();
        //?}

        renderKey("W", x + 20, y, 18, 18, w);
        renderKey("A", x, y + 20, 18, 18, a);
        renderKey("S", x + 20, y + 20, 18, 18, s);
        renderKey("D", x + 40, y + 20, 18, 18, d);
        renderKey("----", x, y + 40, 58, 14, jump);
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        int x = getPosX();
        int y = getPosY();
        renderKey("W", x + 20, y, 18, 18, true);
        renderKey("A", x, y + 20, 18, 18, false);
        renderKey("S", x + 20, y + 20, 18, 18, false);
        renderKey("D", x + 40, y + 20, 18, 18, false);
        renderKey("----", x, y + 40, 58, 14, false);
    }

    private void renderKey(String label, int x, int y, int w, int h, boolean pressed) {
        int fill = pressed ? 0x9055FF55 : (isShowBackground() ? 0x80000000 : 0x20000000);
        int border = pressed ? 0xFF55FF55 : (isShowBackground() ? 0x60FFFFFF : 0x30FFFFFF);
        int textCol = pressed ? 0xFF000000 : getTextColor(0xFFFFFFFF);

        RenderUtils.drawBorderedRect(x, y, x + w, y + h, 1.0F, fill, border);
        int strW = RenderUtils.getStringWidth(label);
        RenderUtils.drawString(label, x + (w - strW) / 2.0F, y + (h - 8) / 2.0F, textCol, !pressed);
    }
}
