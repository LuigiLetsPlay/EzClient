package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Advanced FPS indicator with configurable smoothing intervals,
 * flexible format options, dynamic color coding, and min/max 1% lows.
 */
public final class FpsModule extends HudModule {
    public enum FormatOption {
        LABEL_VALUE, // "FPS: 240"
        VALUE_LABEL, // "240 FPS"
        MINIMAL      // "240"
    }

    private FormatOption formatOption = FormatOption.LABEL_VALUE;
    private int updateIntervalMs = 0; // 0 = realtime, 250, 500, 1000
    private boolean colorCoding = false;
    private boolean showMinMax = false;

    private int smoothedFps = 60;
    private int minFps = 60;
    private int maxFps = 60;
    private long lastUpdate = 0;
    private int fpsAccumulator = 0;
    private int sampleCount = 0;

    public FpsModule() {
        super("FPS", "HUD", true, 6, 6, "FPS: ", "");
    }

    @Override
    public String getDescription() {
        return "Zeigt die aktuelle Bildrate mit optionaler Glättung und Leistungsstatistik an.";
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/fps.png");
    }

    public FormatOption getFormatOption() { return formatOption; }
    public void setFormatOption(FormatOption formatOption) { this.formatOption = formatOption; ConfigManager.save(); }

    public int getUpdateIntervalMs() { return updateIntervalMs; }
    public void setUpdateIntervalMs(int updateIntervalMs) { this.updateIntervalMs = updateIntervalMs; ConfigManager.save(); }

    public boolean isColorCoding() { return colorCoding; }
    public void setColorCoding(boolean colorCoding) { this.colorCoding = colorCoding; ConfigManager.save(); }

    public boolean isShowMinMax() { return showMinMax; }
    public void setShowMinMax(boolean showMinMax) { this.showMinMax = showMinMax; ConfigManager.save(); }

    private void sampleFps(Minecraft client) {
        if (client == null) return;
        int currentFps = client.getFps();
        long now = System.currentTimeMillis();

        if (updateIntervalMs <= 0) {
            smoothedFps = currentFps;
            if (currentFps < minFps || minFps == 0) minFps = currentFps;
            if (currentFps > maxFps) maxFps = currentFps;
        } else {
            fpsAccumulator += currentFps;
            sampleCount++;
            if (now - lastUpdate >= updateIntervalMs) {
                smoothedFps = sampleCount > 0 ? fpsAccumulator / sampleCount : currentFps;
                minFps = Math.min(minFps, currentFps);
                maxFps = Math.max(maxFps, currentFps);
                fpsAccumulator = 0;
                sampleCount = 0;
                lastUpdate = now;
            }
        }
    }

    public int getFpsColor(int fps) {
        if (!colorCoding) return color();
        if (fps >= 120) return 0xFF55FF55; // Bright Green
        if (fps >= 60) return 0xFFFFFF55;  // Yellow
        if (fps >= 30) return 0xFFFFAA00;  // Orange
        return 0xFFFF5555;                 // Red
    }

    @Override
    public int getWidth(Minecraft client) {
        if (client == null || client.font == null) return 40;
        int baseW = client.font.width(displayText(client)) + CONTENT_PADDING_X * 2;
        if (showMinMax) {
            String minMaxStr = "Min: " + minFps + " Max: " + maxFps;
            int mmW = (int) (client.font.width(minMaxStr) * 0.7f) + CONTENT_PADDING_X * 2;
            return Math.max(baseW, mmW);
        }
        return baseW;
    }

    @Override
    public int getWidth(Minecraft client, boolean editor) {
        if (!editor) return getWidth(client);
        if (client == null || client.font == null) return 40;
        int textW = client.font.width(textForRender(true)) + CONTENT_PADDING_X * 2;
        if (!showMinMax) return textW;
        int minMaxW = (int) (client.font.width(minMaxText(true)) * 0.7f) + CONTENT_PADDING_X * 2;
        return Math.max(textW, minMaxW);
    }

    @Override
    public int getHeight(Minecraft client) {
        return showMinMax ? 26 : 9 + CONTENT_PADDING_Y * 2;
    }

    @Override
    protected String value(Minecraft client) {
        sampleFps(client);
        return switch (formatOption) {
            case LABEL_VALUE -> Integer.toString(smoothedFps);
            case VALUE_LABEL -> smoothedFps + " FPS";
            case MINIMAL -> Integer.toString(smoothedFps);
        };
    }

    @Override
    public String displayText(Minecraft client) {
        sampleFps(client);
        return currentDisplayText();
    }

    private String currentDisplayText() {
        return switch (formatOption) {
            case LABEL_VALUE -> getPrefix() + smoothedFps + getSuffix();
            case VALUE_LABEL -> smoothedFps + " FPS" + getSuffix();
            case MINIMAL -> Integer.toString(smoothedFps);
        };
    }

    private String textForRender(boolean editor) {
        if (!editor) return currentDisplayText();
        return switch (formatOption) {
            case LABEL_VALUE -> getPrefix() + "240" + getSuffix();
            case VALUE_LABEL -> "240 FPS" + getSuffix();
            case MINIMAL -> "240";
        };
    }

    private String minMaxText(boolean editor) {
        return "Min: " + (editor ? 180 : minFps) + "  Max: " + (editor ? 290 : maxFps);
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        sampleFps(client);
        float scale = (float) getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(scale, scale);

        String text = textForRender(editor);

        int textW = (client != null && client.font != null) ? client.font.width(text) + CONTENT_PADDING_X * 2 : 40;
        int totalW = textW;
        if (showMinMax && client != null && client.font != null) {
            String mm = minMaxText(editor);
            int mmW = (int) (client.font.width(mm) * 0.7f) + CONTENT_PADDING_X * 2;
            totalW = Math.max(textW, mmW);
        }
        int totalH = getHeight(client);

        renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);

        int textColor = color();
        graphics.text(client.font, text, CONTENT_PADDING_X, CONTENT_PADDING_Y, textColor);

        if (showMinMax) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(CONTENT_PADDING_X, 15);
            graphics.pose().scale(0.7f, 0.7f);
            String mm = minMaxText(editor);
            graphics.text(client.font, mm, 0, 0, 0xFFAAAAAA);
            graphics.pose().popMatrix();
        }

        graphics.pose().popMatrix();
    }
}
