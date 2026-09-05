package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;

public final class MemoryModule extends Module {
    public MemoryModule() { super("memory", "Memory", "Shows used and available JVM memory", "Performance", false, 4, 230, true); }
    private String text() { Runtime r = Runtime.getRuntime(); return "RAM: " + ((r.totalMemory() - r.freeMemory()) / 1048576L) + " / " + (r.maxMemory() / 1048576L) + " MB"; }
    @Override public int getWidth() { return RenderUtils.getStringWidth(text()) + 6; }
    @Override public int getHeight() { return 14; }
    @Override public void renderHud(float tickDelta) { render(); }
    @Override public void renderEditorPreview(float tickDelta) { render(); }
    private void render() {
        String text = text(); int x = getPosX(), y = getPosY();
        if (isShowBackground()) RenderUtils.drawRect(x - 2, y - 2, x + RenderUtils.getStringWidth(text) + 4, y + 10, 0x80000000);
        RenderUtils.drawString(text, x + 1, y, getTextColor(0xFFFFFFFF), true);
    }
}
