package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;

public class ComboModule extends Module {
    private static int comboCount = 0;
    private static long lastHitTime = 0L;

    public ComboModule() {
        super("combo", "Combo Counter", "Displays your current consecutive PvP hits", "PvP", true, 4, 270, true);
    }

    public static void onPlayerHit() {
        long now = System.currentTimeMillis();
        if (now - lastHitTime <= 2000L) {
            comboCount++;
        } else {
            comboCount = 1;
        }
        lastHitTime = now;
    }

    public static void onPlayerDamaged() {
        comboCount = 0;
    }

    @Override
    public int getWidth() {
        String text = "Combo: " + comboCount;
        return RenderUtils.getStringWidth(text) + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;

        if (System.currentTimeMillis() - lastHitTime > 2000L) {
            comboCount = 0;
        }

        String text = "Combo: " + comboCount;
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFF5555);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        String text = "Combo: 4";
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFF5555);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
