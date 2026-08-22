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
    public void setPosition(int x, int y) { this.x = Math.max(0, x); this.y = Math.max(0, y); ConfigManager.save(); }
    public void setScale(double scale) { this.scale = Math.max(0.5, Math.min(3.0, scale)); ConfigManager.save(); }
    public void setPrefix(String prefix) { this.prefix = prefix == null ? "" : prefix; ConfigManager.save(); }
    public void setSuffix(String suffix) { this.suffix = suffix == null ? "" : suffix; ConfigManager.save(); }
    public void setRainbow(boolean rainbow) { this.rainbow = rainbow; ConfigManager.save(); }
    public void setBackground(boolean background) { this.background = background; ConfigManager.save(); }
    public int color() {
        if (!rainbow) return 0xFFFFFFFF;
        float hue = (System.currentTimeMillis() % 5000L) / 5000.0f;
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 0.9f, 1.0f) & 0xFFFFFF);
    }
}
