package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared, fully user-configurable state for every EzClient HUD module. */
public abstract class HudModule extends Module {
    public enum ColorMode {
        SOLID,
        WAVE,
        RAINBOW
    }

    private int x;
    private int y;
    private double scale = 1.0;
    private String prefix;
    private String suffix;
    private ColorMode colorMode = ColorMode.SOLID;
    private boolean background = true;
    private int textColor = 0xFFFFFFFF;     // Color 1 (Solid / Wave Primary)
    private int waveColor2 = 0xFF22C96E;    // Color 2 (Wave Secondary)
    private int backgroundColor = 0xA8111419;
    private int borderColor = 0xFF35414D;
    private boolean border = false;

    // ── Badlion Systemwide Standard Styling ──
    private boolean textShadow = true;
    private boolean customFont = false;
    private int cornerRadius = 4;
    private int borderWidth = 1;
    private float rainbowSpeed = 1.0f;
    private float rainbowSaturation = 0.85f;
    private boolean rainbowBorder = false;

    protected HudModule(String name, String category, boolean enabled, int x, int y, String prefix, String suffix) {
        super(name, category, enabled);
        this.x = x;
        this.y = y;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    protected abstract String value(Minecraft client);
    public String displayText(Minecraft client) { return prefix + value(client) + suffix; }

    public int getWidth(Minecraft client) {
        if (client == null || client.font == null) return 40;
        return client.font.width(displayText(client)) + 8;
    }

    public int getHeight(Minecraft client) {
        return 14;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public double getScale() { return scale; }
    public String getPrefix() { return prefix; }
    public String getSuffix() { return suffix; }

    public ColorMode getColorMode() { return colorMode; }
    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode == null ? ColorMode.SOLID : colorMode;
        ConfigManager.save();
    }

    public boolean isRainbow() { return colorMode == ColorMode.RAINBOW; }
    public void setRainbow(boolean rainbow) {
        setColorMode(rainbow ? ColorMode.RAINBOW : ColorMode.SOLID);
    }

    public boolean hasBackground() { return background; }
    public int getTextColor() { return textColor; }
    public int getWaveColor2() { return waveColor2; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
    public boolean hasBorder() { return border; }

    public boolean isTextShadow() { return textShadow; }
    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; ConfigManager.save(); }

    public boolean isCustomFont() { return customFont; }
    public void setCustomFont(boolean customFont) { this.customFont = customFont; ConfigManager.save(); }

    public int getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(int cornerRadius) { this.cornerRadius = Math.max(0, Math.min(10, cornerRadius)); ConfigManager.save(); }

    public int getBorderWidth() { return borderWidth; }
    public void setBorderWidth(int borderWidth) { this.borderWidth = Math.max(1, Math.min(3, borderWidth)); ConfigManager.save(); }

    public float getRainbowSpeed() { return rainbowSpeed; }
    public void setRainbowSpeed(float rainbowSpeed) { this.rainbowSpeed = Math.max(0.2f, Math.min(5.0f, rainbowSpeed)); ConfigManager.save(); }

    public float getRainbowSaturation() { return rainbowSaturation; }
    public void setRainbowSaturation(float rainbowSaturation) { this.rainbowSaturation = Math.max(0.0f, Math.min(1.0f, rainbowSaturation)); ConfigManager.save(); }

    public boolean isRainbowBorder() { return rainbowBorder; }
    public void setRainbowBorder(boolean rainbowBorder) { this.rainbowBorder = rainbowBorder; ConfigManager.save(); }

    public void setPosition(int x, int y) { this.x = Math.max(0, x); this.y = Math.max(0, y); ConfigManager.save(); }
    public void setScale(double scale) { this.scale = Math.max(0.5, Math.min(2.5, scale)); ConfigManager.save(); }
    public void setPrefix(String prefix) { this.prefix = prefix == null ? "" : prefix; ConfigManager.save(); }
    public void setSuffix(String suffix) { this.suffix = suffix == null ? "" : suffix; ConfigManager.save(); }
    public void setBackground(boolean background) { this.background = background; ConfigManager.save(); }
    public void setTextColor(int color) { this.textColor = color; ConfigManager.save(); }
    public void setWaveColor2(int color) { this.waveColor2 = color; ConfigManager.save(); }
    public void setBackgroundColor(int color) { this.backgroundColor = color; ConfigManager.save(); }
    public void setBorderColor(int color) { this.borderColor = color; ConfigManager.save(); }
    public void setBorder(boolean border) { this.border = border; ConfigManager.save(); }

    public int color() {
        return color(0);
    }

    public int color(long offsetMs) {
        if (colorMode == ColorMode.RAINBOW) {
            long period = (long) (4000L / Math.max(0.1f, rainbowSpeed));
            float hue = ((System.currentTimeMillis() + offsetMs) % period) / (float) period;
            return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, rainbowSaturation, 1.0f) & 0xFFFFFF);
        } else if (colorMode == ColorMode.WAVE) {
            double time = ((System.currentTimeMillis() + offsetMs) % 3000L) / 3000.0;
            float factor = (float) ((Math.sin(time * Math.PI * 2.0) + 1.0) / 2.0);
            return interpolateColor(textColor, waveColor2, factor);
        }
        return textColor;
    }

    public int currentBorderColor() {
        if (rainbowBorder || colorMode == ColorMode.RAINBOW) {
            return color();
        }
        return borderColor;
    }

    public void renderBackgroundAndBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        if (background) {
            if (cornerRadius > 0) {
                renderRoundedBox(graphics, x, y, w, h, cornerRadius, backgroundColor);
            } else {
                graphics.fill(x, y, x + w, y + h, backgroundColor);
            }
        }
        if (border) {
            int bCol = currentBorderColor();
            for (int i = 0; i < borderWidth; i++) {
                graphics.outline(x + i, y + i, Math.max(1, w - i * 2), Math.max(1, h - i * 2), bCol);
            }
        }
    }

    public static void renderRoundedBox(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
        if (radius <= 0) {
            graphics.fill(x, y, x + w, y + h, color);
            return;
        }
        int r = Math.min(radius, Math.min(w / 2, h / 2));
        // Center rectangle
        graphics.fill(x + r, y, x + w - r, y + h, color);
        // Left & right side strips
        graphics.fill(x, y + r, x + r, y + h - r, color);
        graphics.fill(x + w - r, y + r, x + w, y + h - r, color);
        // Corner approximation steps
        for (int i = 1; i < r; i++) {
            int cx = (int) Math.round(Math.sqrt(r * r - (r - i) * (r - i)));
            int span = r - cx;
            // Top-left
            graphics.fill(x + span, y + i, x + r, y + i + 1, color);
            // Top-right
            graphics.fill(x + w - r, y + i, x + w - span, y + i + 1, color);
            // Bottom-left
            graphics.fill(x + span, y + h - i - 1, x + r, y + h - i, color);
            // Bottom-right
            graphics.fill(x + w - r, y + h - i - 1, x + w - span, y + h - i, color);
        }
    }

    public void resetSettings() {
        this.colorMode = ColorMode.SOLID;
        this.textColor = 0xFFFFFFFF;
        this.waveColor2 = 0xFF22C96E;
        this.background = true;
        this.border = false;
        this.backgroundColor = 0xA8111419;
        this.borderColor = 0xFF35414D;
        this.textShadow = true;
        this.customFont = false;
        this.cornerRadius = 4;
        this.borderWidth = 1;
        this.rainbowSpeed = 1.0f;
        this.rainbowSaturation = 0.85f;
        this.rainbowBorder = false;

        if (this instanceof FpsModule fps) {
            fps.setPrefix("FPS: ");
            fps.setFormatOption(FpsModule.FormatOption.LABEL_VALUE);
            fps.setUpdateIntervalMs(0);
            fps.setColorCoding(false);
            fps.setShowMinMax(false);
        } else if (this instanceof PingModule ping) {
            ping.setPrefix("Ping: ");
        } else if (this instanceof CoordinatesModule coords) {
            coords.setLayoutMode(CoordinatesModule.LayoutMode.MULTI_LINE);
            coords.setDecimalPrecision(0);
            coords.setShowBiome(false);
            coords.setShowDirection(true);
            coords.setShowNether(false);
        } else if (this instanceof KeystrokesModule ks) {
            ks.setLayoutPreset(KeystrokesModule.LayoutPreset.WASD_MOUSE_SPACE_CPS);
            ks.setSpaceStyle(KeystrokesModule.SpaceStyle.LINE);
            ks.setFadeTimeMs(150);
            ks.setNormalBoxColor(0xA8111419);
            ks.setPressedBoxColor(0x70FFFFFF);
            ks.setKeyTextColor(0xFFFFFFFF);
            ks.setPressedTextColor(0xFFFFFFFF);
        } else if (this instanceof ArmorStatusModule armor) {
            armor.setHorizontal(false);
            armor.setDurabilityMode(ArmorStatusModule.DurabilityMode.PERCENT);
            armor.setColorTiers(true);
            armor.setDamageWarning(true);
            armor.setShowItemCount(true);
        } else if (this instanceof PotionEffectModule potion) {
            potion.setDisplayStyle(PotionEffectModule.DisplayStyle.DETAILED);
            potion.setVertical(true);
            potion.setBlinkWarningSeconds(5);
            potion.setUseCustomColors(true);
        } else if (this instanceof CpsModule cps) {
            cps.setDisplayMode(CpsModule.DisplayMode.COMBINED);
            cps.setDynamicColor(true);
            cps.setShowHistoryGraph(false);
        } else if (this instanceof CrosshairModule crosshair) {
            crosshair.setCrosshairType(CrosshairModule.CrosshairType.CLASSIC_CROSS);
            crosshair.setGap(3);
            crosshair.setSize(5);
            crosshair.setVerticalSize(5);
            crosshair.setThickness(1);
            crosshair.setDotSize(2);
            crosshair.setOpacity(100);
            crosshair.setShowDot(false);
            crosshair.setShowOutline(true);
            crosshair.setDynamicSpread(false);
            crosshair.setMovementSpread(true);
            crosshair.setJumpSpread(true);
            crosshair.setCooldownSpread(true);
            crosshair.setTargetMode(CrosshairModule.TargetMode.ALL);
            crosshair.setTargetEntityColor(0xFFFF3333);
            crosshair.setTargetPlayerColor(0xFF38BDF8);
            crosshair.setTargetHostileColor(0xFFFF3333);
            crosshair.setTargetNeutralColor(0xFFFFB020);
            crosshair.setTargetBlockColor(0xFFFACC15);
            crosshair.setTargetEntityScale(1.05f);
            crosshair.setTargetPlayerScale(1.15f);
            crosshair.setTargetHostileScale(1.20f);
            crosshair.setTargetNeutralScale(1.10f);
        }
        ConfigManager.save();
    }

    public static int interpolateColor(int c1, int c2, float factor) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
