package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * Real-time Clicks Per Second (CPS) counter for LMB & RMB with rolling 1000ms window,
 * customizable display formats, dynamic color thresholds, and an optional click history graph.
 */
public final class CpsModule extends HudModule {
    public enum DisplayMode {
        LMB_ONLY,
        RMB_ONLY,
        COMBINED
    }

    private DisplayMode displayMode = DisplayMode.COMBINED;
    private boolean dynamicColor = true;
    private boolean showHistoryGraph = false;
    private int highlightThresholdMid = 12;
    private int highlightThresholdHigh = 18;

    // History ring buffer for sparkline graph (last 16 samples)
    private static final int HISTORY_SIZE = 16;
    private static final int[] leftHistory = new int[HISTORY_SIZE];
    private static final int[] rightHistory = new int[HISTORY_SIZE];
    private static int historyIndex = 0;
    private static long lastSampleTime = 0;

    public CpsModule() {
        super("CPS", "HUD", false, 6, 22, "CPS: ", "");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/cps.png");
    }

    public DisplayMode getDisplayMode() { return displayMode; }
    public void setDisplayMode(DisplayMode displayMode) { this.displayMode = displayMode; ConfigManager.save(); }

    public boolean isDynamicColor() { return dynamicColor; }
    public void setDynamicColor(boolean dynamicColor) { this.dynamicColor = dynamicColor; ConfigManager.save(); }

    public boolean isShowHistoryGraph() { return showHistoryGraph; }
    public void setShowHistoryGraph(boolean showHistoryGraph) { this.showHistoryGraph = showHistoryGraph; ConfigManager.save(); }

    public int getHighlightThresholdMid() { return highlightThresholdMid; }
    public void setHighlightThresholdMid(int val) { this.highlightThresholdMid = Math.max(1, Math.min(30, val)); ConfigManager.save(); }

    public int getHighlightThresholdHigh() { return highlightThresholdHigh; }
    public void setHighlightThresholdHigh(int val) { this.highlightThresholdHigh = Math.max(1, Math.min(40, val)); ConfigManager.save(); }

    public static void recordSample() {
        long now = System.currentTimeMillis();
        if (now - lastSampleTime >= 100) { // 10Hz sampling
            lastSampleTime = now;
            historyIndex = (historyIndex + 1) % HISTORY_SIZE;
            leftHistory[historyIndex] = KeystrokesModule.getLeftCps();
            rightHistory[historyIndex] = KeystrokesModule.getRightCps();
        }
    }

    @Override
    public int getWidth(Minecraft client) {
        if (client == null || client.font == null) return 48;
        int textW = client.font.width(displayText(client)) + 8;
        return showHistoryGraph ? Math.max(textW, 56) : textW;
    }

    @Override
    public int getHeight(Minecraft client) {
        return showHistoryGraph ? 24 : 14;
    }

    @Override
    protected String value(Minecraft client) {
        int l = KeystrokesModule.getLeftCps();
        int r = KeystrokesModule.getRightCps();
        return switch (displayMode) {
            case LMB_ONLY -> l + " CPS";
            case RMB_ONLY -> r + " CPS";
            case COMBINED -> l + " | " + r;
        };
    }

    public int getCpsColor(int cps) {
        if (!dynamicColor) return color();
        if (cps >= highlightThresholdHigh) {
            return 0xFFFF3333; // Red highlight
        } else if (cps >= highlightThresholdMid) {
            return 0xFFFFAA00; // Yellow/Orange highlight
        }
        return color();
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        recordSample();
        float scale = (float) getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(scale, scale);

        String text = displayText(client);
        if (editor) {
            text = switch (displayMode) {
                case LMB_ONLY -> getPrefix() + "14 CPS" + getSuffix();
                case RMB_ONLY -> getPrefix() + "10 CPS" + getSuffix();
                case COMBINED -> getPrefix() + "14 | 10" + getSuffix();
            };
        }

        int textW = (client != null && client.font != null) ? client.font.width(text) + 8 : 48;
        int totalW = showHistoryGraph ? Math.max(textW, 56) : textW;
        int totalH = getHeight(client);

        renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);

        int textColor = color();
        int textY = showHistoryGraph ? 2 : 3;
        graphics.text(client.font, text, 4, textY, textColor);

        if (showHistoryGraph) {
            int graphX = 4;
            int graphY = totalH - 8;
            int graphW = totalW - 8;
            int barW = Math.max(1, graphW / HISTORY_SIZE);

            for (int i = 0; i < HISTORY_SIZE; i++) {
                int idx = (historyIndex + 1 + i) % HISTORY_SIZE;
                int val = displayMode == DisplayMode.RMB_ONLY ? rightHistory[idx] : leftHistory[idx];
                if (editor) val = (i % 5) * 4;
                int barH = Math.min(6, Math.max(1, val * 6 / 20));
                int bx = graphX + i * barW;
                int by = graphY + (6 - barH);
                int bCol = getCpsColor(val);
                graphics.fill(bx, by, bx + barW - 1, graphY + 6, (bCol & 0x00FFFFFF) | 0x90000000);
            }
        }

        graphics.pose().popMatrix();
    }
}
