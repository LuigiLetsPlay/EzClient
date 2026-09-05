package app.ezclient.v1_8.modules;

import app.ezclient.shared.ClickRateTracker;
import app.ezclient.v1_8.gui.RenderUtils;
import org.lwjgl.input.Mouse;

public class CpsModule extends Module {
    private static final ClickRateTracker leftClicks = new ClickRateTracker(128);
    private static final ClickRateTracker rightClicks = new ClickRateTracker(128);
    private static boolean lastLeftDown = false;
    private static boolean lastRightDown = false;

    public CpsModule() {
        super("cps", "CPS Counter", "Shows left and right clicks per second", "HUD", true, 4, 20, true);
    }

    @Override
    public int getWidth() {
        String text = "CPS: " + getLeftCps() + " | " + getRightCps();
        return RenderUtils.getStringWidth(text) + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    public static void updateClicks() {
        boolean leftDown = Mouse.isButtonDown(0);
        boolean rightDown = Mouse.isButtonDown(1);
        long now = System.currentTimeMillis();

        if (leftDown && !lastLeftDown) {
            leftClicks.record(now);
        }
        if (rightDown && !lastRightDown) {
            rightClicks.record(now);
        }

        lastLeftDown = leftDown;
        lastRightDown = rightDown;

        leftClicks.count(now);
        rightClicks.count(now);
    }

    public static int getLeftCps() {
        return leftClicks.count(System.currentTimeMillis());
    }

    public static int getRightCps() {
        return rightClicks.count(System.currentTimeMillis());
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        String text = "CPS: " + getLeftCps() + " | " + getRightCps();
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFFFFFF);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
