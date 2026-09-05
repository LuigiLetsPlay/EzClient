package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;

public class ReachModule extends Module {
    private static double lastReach = 3.00;
    private static long lastReachTime = 0L;

    public ReachModule() {
        super("reach", "Reach Display", "Shows the reach distance of your attack hits", "PvP", true, 4, 288, true);
    }

    public static void recordReach(double distance) {
        lastReach = distance;
        lastReachTime = System.currentTimeMillis();
    }

    @Override
    public int getWidth() {
        String text = String.format("Reach: %.2fm", lastReach);
        return RenderUtils.getStringWidth(text) + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;

        String text = String.format("Reach: %.2fm", lastReach);
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFFAA00);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
