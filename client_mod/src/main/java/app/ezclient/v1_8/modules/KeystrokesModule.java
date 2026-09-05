package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeystrokesModule extends Module {
    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "Shows WASD, jump, and mouse clicks on HUD", "HUD", true, 4, 38, true);
    }

    @Override
    public int getWidth() {
        return 58;
    }

    @Override
    public int getHeight() {
        return 66;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) return;

        int x = getPosX();
        int y = getPosY();
        int keySize = 18;
        int gap = 2;

        boolean w = client.options.forwardKey.isPressed() || Keyboard.isKeyDown(Keyboard.KEY_W);
        boolean a = client.options.leftKey.isPressed() || Keyboard.isKeyDown(Keyboard.KEY_A);
        boolean s = client.options.backKey.isPressed() || Keyboard.isKeyDown(Keyboard.KEY_S);
        boolean d = client.options.rightKey.isPressed() || Keyboard.isKeyDown(Keyboard.KEY_D);
        boolean lmb = Mouse.isButtonDown(0);
        boolean rmb = Mouse.isButtonDown(1);
        boolean space = client.options.jumpKey.isPressed() || Keyboard.isKeyDown(Keyboard.KEY_SPACE);

        // Row 1: W
        drawKey("W", x + keySize + gap, y, keySize, keySize, w);

        // Row 2: A, S, D
        int row2Y = y + keySize + gap;
        drawKey("A", x, row2Y, keySize, keySize, a);
        drawKey("S", x + keySize + gap, row2Y, keySize, keySize, s);
        drawKey("D", x + (keySize + gap) * 2, row2Y, keySize, keySize, d);

        // Row 3: LMB, RMB
        int row3Y = row2Y + keySize + gap;
        int mouseW = (keySize * 3 + gap * 2 - gap) / 2;
        drawKey("LMB", x, row3Y, mouseW, 14, lmb);
        drawKey("RMB", x + mouseW + gap, row3Y, mouseW, 14, rmb);

        // Row 4: Space
        int row4Y = row3Y + 14 + gap;
        int totalW = keySize * 3 + gap * 2;
        drawKey("—", x, row4Y, totalW, 10, space);
    }

    private void drawKey(String label, int kx, int ky, int kw, int kh, boolean pressed) {
        int activeColor = getTextColor(0xFF55FF55);
        int bg = pressed ? (0x90000000 | (activeColor & 0x00FFFFFF)) : (isShowBackground() ? 0x70000000 : 0x20000000);
        int textCol = pressed ? 0xFF000000 : 0xFFFFFFFF;

        RenderUtils.drawRect(kx, ky, kx + kw, ky + kh, bg);
        int strW = RenderUtils.getStringWidth(label);
        RenderUtils.drawString(label, kx + (kw - strW) / 2.0F, ky + (kh - 8) / 2.0F, textCol, !pressed);
    }
}
