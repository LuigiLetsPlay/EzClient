package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * High-performance, Badlion-style Keystrokes overlay with WASD, Mouse, Space,
 * Sneak, Sprint, smooth key release fading, custom box colors, and CPS integration.
 */
public final class KeystrokesModule extends HudModule {
    public enum LayoutPreset {
        WASD,
        WASD_MOUSE,
        WASD_MOUSE_SPACE,
        WASD_MOUSE_SPACE_CPS,
        FULL
    }

    public enum SpaceStyle {
        LINE,
        BLOCK,
        TEXT
    }

    private LayoutPreset layoutPreset = LayoutPreset.WASD_MOUSE_SPACE_CPS;
    private SpaceStyle spaceStyle = SpaceStyle.LINE;
    private int fadeTimeMs = 150;
    private int normalBoxColor = 0xA8111419;
    private int pressedBoxColor = 0x70FFFFFF;
    private int keyTextColor = 0xFFFFFFFF;
    private int pressedTextColor = 0xFF000000;
    private boolean showMouseCps = true;

    // Fade tracking: release timestamp in ms
    private static long wReleaseTime = 0, aReleaseTime = 0, sReleaseTime = 0, dReleaseTime = 0;
    private static long spaceReleaseTime = 0, lmbReleaseTime = 0, rmbReleaseTime = 0;
    private static long sneakReleaseTime = 0, sprintReleaseTime = 0;

    private static boolean wasW = false, wasA = false, wasS = false, wasD = false;
    private static boolean wasSpace = false, wasLmb = false, wasRmb = false;
    private static boolean wasSneak = false, wasSprint = false;

    private static final int CLICK_BUFFER_SIZE = 128;
    private static final long[] leftClicks = new long[CLICK_BUFFER_SIZE];
    private static final long[] rightClicks = new long[CLICK_BUFFER_SIZE];
    private static int leftWriteIndex, rightWriteIndex, leftClickCount, rightClickCount;
    private long currentRenderTime;

    public KeystrokesModule() {
        super("Keystrokes", "HUD", false, 6, 86, "", "");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/keystrokes.png");
    }

    public static void updateClicks(Minecraft client) {
        if (client == null || client.options == null) return;
        long now = System.currentTimeMillis();

        boolean left = client.options.keyAttack.isDown();
        if (left && !wasLmb) {
            leftClicks[leftWriteIndex] = now;
            leftWriteIndex = (leftWriteIndex + 1) % CLICK_BUFFER_SIZE;
            leftClickCount = Math.min(CLICK_BUFFER_SIZE, leftClickCount + 1);
        }
        if (!left && wasLmb) lmbReleaseTime = now;
        wasLmb = left;

        boolean right = client.options.keyUse.isDown();
        if (right && !wasRmb) {
            rightClicks[rightWriteIndex] = now;
            rightWriteIndex = (rightWriteIndex + 1) % CLICK_BUFFER_SIZE;
            rightClickCount = Math.min(CLICK_BUFFER_SIZE, rightClickCount + 1);
        }
        if (!right && wasRmb) rmbReleaseTime = now;
        wasRmb = right;

        boolean w = client.options.keyUp.isDown();
        if (!w && wasW) wReleaseTime = now;
        wasW = w;

        boolean a = client.options.keyLeft.isDown();
        if (!a && wasA) aReleaseTime = now;
        wasA = a;

        boolean s = client.options.keyDown.isDown();
        if (!s && wasS) sReleaseTime = now;
        wasS = s;

        boolean d = client.options.keyRight.isDown();
        if (!d && wasD) dReleaseTime = now;
        wasD = d;

        boolean space = client.options.keyJump.isDown();
        if (!space && wasSpace) spaceReleaseTime = now;
        wasSpace = space;

        boolean sneak = client.options.keyShift.isDown();
        if (!sneak && wasSneak) sneakReleaseTime = now;
        wasSneak = sneak;

        boolean sprint = client.options.keySprint.isDown();
        if (!sprint && wasSprint) sprintReleaseTime = now;
        wasSprint = sprint;

        leftClickCount = pruneClicks(leftClicks, leftWriteIndex, leftClickCount, now);
        rightClickCount = pruneClicks(rightClicks, rightWriteIndex, rightClickCount, now);
    }

    private static int pruneClicks(long[] clicks, int writeIndex, int count, long now) {
        while (count > 0) {
            int oldest = (writeIndex - count + CLICK_BUFFER_SIZE) % CLICK_BUFFER_SIZE;
            if (now - clicks[oldest] <= 1000L) break;
            count--;
        }
        return count;
    }

    public static int getLeftCps() { return leftClickCount; }
    public static int getRightCps() { return rightClickCount; }

    public LayoutPreset getLayoutPreset() { return layoutPreset; }
    public void setLayoutPreset(LayoutPreset layoutPreset) { this.layoutPreset = layoutPreset; ConfigManager.save(); }

    public SpaceStyle getSpaceStyle() { return spaceStyle; }
    public void setSpaceStyle(SpaceStyle spaceStyle) { this.spaceStyle = spaceStyle; ConfigManager.save(); }

    public int getFadeTimeMs() { return fadeTimeMs; }
    public void setFadeTimeMs(int fadeTimeMs) { this.fadeTimeMs = Math.max(0, Math.min(500, fadeTimeMs)); ConfigManager.save(); }

    public int getNormalBoxColor() { return normalBoxColor; }
    public void setNormalBoxColor(int normalBoxColor) { this.normalBoxColor = normalBoxColor; ConfigManager.save(); }

    public int getPressedBoxColor() { return pressedBoxColor; }
    public void setPressedBoxColor(int pressedBoxColor) { this.pressedBoxColor = pressedBoxColor; ConfigManager.save(); }

    public int getKeyTextColor() { return keyTextColor; }
    public void setKeyTextColor(int keyTextColor) { this.keyTextColor = keyTextColor; ConfigManager.save(); }

    public int getPressedTextColor() { return pressedTextColor; }
    public void setPressedTextColor(int pressedTextColor) { this.pressedTextColor = pressedTextColor; ConfigManager.save(); }

    public boolean isShowMouseCps() { return showMouseCps; }
    public void setShowMouseCps(boolean showMouseCps) { this.showMouseCps = showMouseCps; ConfigManager.save(); }

    // Legacy getters/setters for compatibility
    public boolean isBoxLayout() { return layoutPreset != null; }
    public void setBoxLayout(boolean box) {}
    public boolean isShowSpace() { return layoutPreset == LayoutPreset.WASD_MOUSE_SPACE || layoutPreset == LayoutPreset.WASD_MOUSE_SPACE_CPS || layoutPreset == LayoutPreset.FULL; }
    public void setShowSpace(boolean show) {}
    public boolean isSpaceIsLine() { return spaceStyle == SpaceStyle.LINE; }
    public void setSpaceIsLine(boolean line) { this.spaceStyle = line ? SpaceStyle.LINE : SpaceStyle.TEXT; }
    public boolean isShowMouse() { return layoutPreset != LayoutPreset.WASD; }
    public void setShowMouse(boolean show) {}
    public boolean isShowCps() { return showMouseCps; }
    public void setShowCps(boolean show) { this.showMouseCps = show; }

    @Override
    public int getWidth(Minecraft client) {
        return 64;
    }

    @Override
    public int getHeight(Minecraft client) {
        int h = 42; // WASD
        if (layoutPreset == LayoutPreset.WASD) return h;
        h += (showMouseCps ? 26 : 20); // Mouse buttons
        if (layoutPreset == LayoutPreset.WASD_MOUSE) return h;
        h += 15; // Space bar
        if (layoutPreset == LayoutPreset.FULL) {
            h += 28; // Sneak and Sprint bars
        }
        return h;
    }

    @Override
    protected String value(Minecraft client) {
        return "Keystrokes";
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        currentRenderTime = renderFrameTimeMillis();
        float scale = (float) getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(scale, scale);

        boolean wDown = client.options != null && client.options.keyUp.isDown();
        boolean aDown = client.options != null && client.options.keyLeft.isDown();
        boolean sDown = client.options != null && client.options.keyDown.isDown();
        boolean dDown = client.options != null && client.options.keyRight.isDown();

        drawKey(graphics, client, "W", 22, 0, 20, 20, wDown, wReleaseTime);
        drawKey(graphics, client, "A", 0, 22, 20, 20, aDown, aReleaseTime);
        drawKey(graphics, client, "S", 22, 22, 20, 20, sDown, sReleaseTime);
        drawKey(graphics, client, "D", 44, 22, 20, 20, dDown, dReleaseTime);

        int currentY = 44;

        if (layoutPreset != LayoutPreset.WASD) {
            boolean lmbDown = client.options != null && client.options.keyAttack.isDown();
            boolean rmbDown = client.options != null && client.options.keyUse.isDown();
            int mh = (showMouseCps || layoutPreset == LayoutPreset.WASD_MOUSE_SPACE_CPS || layoutPreset == LayoutPreset.FULL) ? 24 : 18;
            drawMouseKey(graphics, client, "LMB", getLeftCps(), 0, currentY, 31, mh, lmbDown, lmbReleaseTime);
            drawMouseKey(graphics, client, "RMB", getRightCps(), 33, currentY, 31, mh, rmbDown, rmbReleaseTime);
            currentY += mh + 2;
        }

        if (layoutPreset == LayoutPreset.WASD_MOUSE_SPACE || layoutPreset == LayoutPreset.WASD_MOUSE_SPACE_CPS || layoutPreset == LayoutPreset.FULL) {
            boolean spaceDown = client.options != null && client.options.keyJump.isDown();
            drawSpaceKey(graphics, client, 0, currentY, 64, 13, spaceDown, spaceReleaseTime);
            currentY += 15;
        }

        if (layoutPreset == LayoutPreset.FULL) {
            boolean sneakDown = client.options != null && client.options.keyShift.isDown();
            boolean sprintDown = client.options != null && client.options.keySprint.isDown();
            drawBarKey(graphics, client, "SNEAK", 0, currentY, 64, 12, sneakDown, sneakReleaseTime);
            currentY += 14;
            drawBarKey(graphics, client, "SPRINT", 0, currentY, 64, 12, sprintDown, sprintReleaseTime);
        }

        graphics.pose().popMatrix();
    }

    private float getPressFactor(boolean pressed, long releaseTime) {
        if (pressed) return 1.0f;
        if (fadeTimeMs <= 0) return 0.0f;
        long elapsed = currentRenderTime - releaseTime;
        if (elapsed >= fadeTimeMs) return 0.0f;
        return 1.0f - ((float) elapsed / fadeTimeMs);
    }

    private void drawKey(GuiGraphicsExtractor g, Minecraft client, String text, int x, int y, int w, int h, boolean pressed, long releaseTime) {
        float factor = getPressFactor(pressed, releaseTime);
        int bg = interpolateColor(normalBoxColor, pressedBoxColor, factor);
        int txt = interpolateColor(keyTextColor, pressedTextColor, factor);
        if (getColorMode() == ColorMode.RAINBOW) txt = color(x * 50L);

        renderKeyBox(g, x, y, w, h, bg);
        g.centeredText(client.font, net.minecraft.network.chat.Component.literal(text), x + w / 2, y + (h - 8) / 2, txt);
    }

    private void drawSpaceKey(GuiGraphicsExtractor g, Minecraft client, int x, int y, int w, int h, boolean pressed, long releaseTime) {
        float factor = getPressFactor(pressed, releaseTime);
        int bg = interpolateColor(normalBoxColor, pressedBoxColor, factor);
        int col = interpolateColor(keyTextColor, pressedTextColor, factor);
        if (getColorMode() == ColorMode.RAINBOW) col = color(100L);

        renderKeyBox(g, x, y, w, h, bg);

        if (spaceStyle == SpaceStyle.LINE) {
            int lineW = 32;
            int lx = x + (w - lineW) / 2;
            int ly = y + (h - 2) / 2;
            g.fill(lx, ly, lx + lineW, ly + 2, col);
        } else if (spaceStyle == SpaceStyle.BLOCK) {
            int fillW = w - 8;
            int lx = x + 4;
            int ly = y + 3;
            g.fill(lx, ly, lx + fillW, ly + h - 6, col);
        } else {
            g.centeredText(client.font, net.minecraft.network.chat.Component.literal("[SPACE]"), x + w / 2, y + (h - 8) / 2, col);
        }
    }

    private void drawMouseKey(GuiGraphicsExtractor g, Minecraft client, String label, int cps, int x, int y, int w, int h, boolean pressed, long releaseTime) {
        float factor = getPressFactor(pressed, releaseTime);
        int bg = interpolateColor(normalBoxColor, pressedBoxColor, factor);
        int txt = interpolateColor(keyTextColor, pressedTextColor, factor);
        if (getColorMode() == ColorMode.RAINBOW) txt = color(x * 50L);

        renderKeyBox(g, x, y, w, h, bg);

        boolean showCpsText = showMouseCps || layoutPreset == LayoutPreset.WASD_MOUSE_SPACE_CPS || layoutPreset == LayoutPreset.FULL;
        if (showCpsText) {
            g.centeredText(client.font, net.minecraft.network.chat.Component.literal(label), x + w / 2, y + 3, txt);
            String cpsStr = cps + " CPS";
            g.centeredText(client.font, net.minecraft.network.chat.Component.literal(cpsStr), x + w / 2, y + 13, factor > 0.5f ? 0xFF000000 : 0xFFAAAAAA);
        } else {
            g.centeredText(client.font, net.minecraft.network.chat.Component.literal(label), x + w / 2, y + (h - 8) / 2, txt);
        }
    }

    private void drawBarKey(GuiGraphicsExtractor g, Minecraft client, String label, int x, int y, int w, int h, boolean pressed, long releaseTime) {
        float factor = getPressFactor(pressed, releaseTime);
        int bg = interpolateColor(normalBoxColor, pressedBoxColor, factor);
        int txt = interpolateColor(keyTextColor, pressedTextColor, factor);
        if (getColorMode() == ColorMode.RAINBOW) txt = color(y * 40L);

        renderKeyBox(g, x, y, w, h, bg);
        g.centeredText(client.font, net.minecraft.network.chat.Component.literal(label), x + w / 2, y + (h - 8) / 2, txt);
    }

    private void renderKeyBox(GuiGraphicsExtractor g, int x, int y, int w, int h, int bg) {
        if (hasBackground() && bg != 0) {
            if (getCornerRadius() > 0) {
                renderRoundedBox(g, x, y, w, h, getCornerRadius(), bg);
            } else {
                g.fill(x, y, x + w, y + h, bg);
            }
        }
        if (hasBorder()) {
            int bCol = isRainbowBorder() || getColorMode() == ColorMode.RAINBOW ? color() : getBorderColor();
            for (int i = 0; i < getBorderWidth(); i++) {
                g.outline(x + i, y + i, Math.max(1, w - i * 2), Math.max(1, h - i * 2), bCol);
            }
        }
    }
}
