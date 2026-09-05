package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.shared.ClickRateTracker;
import app.ezclient.v1_16_v1_20.gui.RenderUtils;

public class CpsModule extends Module {
    private static final ClickRateTracker leftClicks = new ClickRateTracker(128);
    private static final ClickRateTracker rightClicks = new ClickRateTracker(128);

    public CpsModule() {
        super("cps", "CPS Counter", "Shows left and right clicks per second", "HUD", true, 4, 20, true);
    }

    public static synchronized void recordLeftClick() {
        leftClicks.record(System.currentTimeMillis());
    }

    public static synchronized void recordRightClick() {
        rightClicks.record(System.currentTimeMillis());
    }

    private static int getCps(ClickRateTracker clicks) {
        return clicks.count(System.currentTimeMillis());
    }

    @Override
    public int getWidth() {
        int l = getCps(leftClicks);
        int r = getCps(rightClicks);
        return RenderUtils.getStringWidth("CPS: " + l + " | " + r) + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        int l = getCps(leftClicks);
        int r = getCps(rightClicks);

        String text = "CPS: " + l + " | " + r;
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFF55FFFF);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        String text = "CPS: 12 | 14";
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFF55FFFF);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
