package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;

public class CrosshairModule extends Module {
    private int crosshairSize = 4;
    private int crosshairGap = 2;
    private int crosshairThickness = 1;
    private boolean drawDot = false;

    public CrosshairModule() {
        super("crosshair", "Custom Crosshair", "Customizable PvP crosshair with styles and colors", "Render", false, 0, 0, false);
    }

    public int getCrosshairSize() { return crosshairSize; }
    public void setCrosshairSize(int crosshairSize) { this.crosshairSize = crosshairSize; }

    public int getCrosshairGap() { return crosshairGap; }
    public void setCrosshairGap(int crosshairGap) { this.crosshairGap = crosshairGap; }

    public boolean isDrawDot() { return drawDot; }
    public void setDrawDot(boolean drawDot) { this.drawDot = drawDot; }

    public void renderCrosshair(int screenWidth, int screenHeight) {
        if (!isEnabled()) return;

        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        int color = getTextColor(0xFF55FF55);
        int border = 0xAA000000;

        int s = crosshairSize;
        int g = crosshairGap;
        int t = crosshairThickness;

        // Top line
        RenderUtils.drawBorderedRect(cx - t, cy - g - s, cx + t + 1, cy - g, 0.5F, color, border);
        // Bottom line
        RenderUtils.drawBorderedRect(cx - t, cy + g + 1, cx + t + 1, cy + g + s + 1, 0.5F, color, border);
        // Left line
        RenderUtils.drawBorderedRect(cx - g - s, cy - t, cx - g, cy + t + 1, 0.5F, color, border);
        // Right line
        RenderUtils.drawBorderedRect(cx + g + 1, cy - t, cx + g + s + 1, cy + t + 1, 0.5F, color, border);

        // Center dot
        if (drawDot) {
            RenderUtils.drawBorderedRect(cx - t, cy - t, cx + t + 1, cy + t + 1, 0.5F, color, border);
        }
    }
}
