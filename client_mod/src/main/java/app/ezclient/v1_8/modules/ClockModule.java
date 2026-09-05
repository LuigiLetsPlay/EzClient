package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class ClockModule extends Module {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ClockModule() {
        super("clock", "Clock", "Shows your local time", "HUD", false, 4, 214, true);
    }

    private String text() { return "Time: " + LocalTime.now().format(FORMAT); }
    @Override public int getWidth() { return RenderUtils.getStringWidth(text()) + 6; }
    @Override public int getHeight() { return 14; }
    @Override public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        String text = text(); int x = getPosX(), y = getPosY();
        if (isShowBackground()) RenderUtils.drawRect(x - 2, y - 2, x + RenderUtils.getStringWidth(text) + 4, y + 10, 0x80000000);
        RenderUtils.drawString(text, x + 1, y, getTextColor(0xFFFFFFFF), true);
    }
}
