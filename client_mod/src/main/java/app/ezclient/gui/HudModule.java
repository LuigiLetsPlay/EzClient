package app.ezclient.gui;

import net.minecraft.client.Minecraft;

/** Shared, fully user-configurable state for every EzClient HUD module. */
public abstract class HudModule extends Module {
    private int x;
    private int y;
    private double scale = 1.0;
    private String prefix;
    private String suffix;
    private boolean rainbow;
    private boolean background = true;
    private int textColor = 0xFFFFFFFF;
    private int backgroundColor = 0xA8111419;
    private int borderColor = 0xFF35414D;
    private boolean border = false;

    protected HudModule(String name, String category, boolean enabled, int x, int y, String prefix, String suffix) {
        super(name, category, enabled);
        this.x = x;
        this.y = y;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    protected abstract String value(Minecraft client);
    public String displayText(Minecraft client) { return prefix + value(client) + suffix; }
    public int getX() { return x; }
    public int getY() { return y; }
    public double getScale() { return scale; }
    public String getPrefix() { return prefix; }
    public String getSuffix() { return suffix; }
    public boolean isRainbow() { return rainbow; }
    public boolean hasBackground() { return background; }
    public int getTextColor() { return textColor; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
    public boolean hasBorder() { return border; }
    public void setPosition(int x, int y) { this.x = Math.max(0, x); this.y = Math.max(0, y); ConfigManager.save(); }
    public void setScale(double scale) { this.scale = Math.max(0.5, Math.min(3.0, scale)); ConfigManager.save(); }
    public void setPrefix(String prefix) { this.prefix = prefix == null ? "" : prefix; ConfigManager.save(); }
    public void setSuffix(String suffix) { this.suffix = suffix == null ? "" : suffix; ConfigManager.save(); }
    public void setRainbow(boolean rainbow) { this.rainbow = rainbow; ConfigManager.save(); }
    public void setBackground(boolean background) { this.background = background; ConfigManager.save(); }
    public void setTextColor(int color) { this.textColor = color; ConfigManager.save(); }
    public void setBackgroundColor(int color) { this.backgroundColor = color; ConfigManager.save(); }
    public void setBorderColor(int color) { this.borderColor = color; ConfigManager.save(); }
    public void setBorder(boolean border) { this.border = border; ConfigManager.save(); }
    public int color() {
        if (!rainbow) return textColor;
        float hue = (System.currentTimeMillis() % 5000L) / 5000.0f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.9f, 1.0f) & 0xFFFFFF);
    }
}
