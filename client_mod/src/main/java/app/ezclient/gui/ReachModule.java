package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * Reach Display HUD Module:
 * Measures and displays exact attack reach distance (in blocks) with customizable precision,
 * distance color coding, and configurable fade-out duration.
 */
public final class ReachModule extends HudModule {
    public enum DisplayFormat {
        REACH_PREFIX("Reach: "),
        SIMPLE_M("m"),
        BLOCKS("b");

        private final String label;
        DisplayFormat(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static double lastReach = 0.0;
    private static long lastReachTime = 0L;

    private DisplayFormat displayFormat = DisplayFormat.REACH_PREFIX;
    private int precision = 2; // 1 to 3
    private int fadeOutDurationMs = 1500; // 500 to 3000 ms
    private boolean raytracePrecision = true;
    private boolean colorCoding = true;

    public ReachModule() {
        super("Reach Display", "Combat", false, 6, 144, "", "");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/crosshair.png");
    }

    public static void recordReach(double dist) {
        lastReach = dist;
        lastReachTime = System.currentTimeMillis();
    }

    public static double getLastReach() { return lastReach; }
    public static long getLastReachTime() { return lastReachTime; }

    public DisplayFormat getDisplayFormat() { return displayFormat; }
    public void setDisplayFormat(DisplayFormat displayFormat) { this.displayFormat = displayFormat; ConfigManager.save(); }

    public int getPrecision() { return precision; }
    public void setPrecision(int precision) { this.precision = Math.max(1, Math.min(3, precision)); ConfigManager.save(); }

    public int getFadeOutDurationMs() { return fadeOutDurationMs; }
    public void setFadeOutDurationMs(int fadeOutDurationMs) { this.fadeOutDurationMs = Math.max(500, Math.min(3000, fadeOutDurationMs)); ConfigManager.save(); }

    public boolean isRaytracePrecision() { return raytracePrecision; }
    public void setRaytracePrecision(boolean raytracePrecision) { this.raytracePrecision = raytracePrecision; ConfigManager.save(); }

    public boolean isColorCoding() { return colorCoding; }
    public void setColorCoding(boolean colorCoding) { this.colorCoding = colorCoding; ConfigManager.save(); }

    @Override
    protected String value(Minecraft client) {
        long elapsed = System.currentTimeMillis() - lastReachTime;
        double reach = (elapsed <= fadeOutDurationMs && lastReach > 0) ? lastReach : 0.0;
        if (reach <= 0.0) {
            return switch (displayFormat) {
                case REACH_PREFIX -> "Reach: ---";
                case SIMPLE_M -> "---m";
                case BLOCKS -> "---b";
            };
        }

        String fmt = "%." + precision + "f";
        String formatted = String.format(Locale.ROOT, fmt, reach);
        String colorCode = "";
        if (colorCoding) {
            if (reach < 2.50) colorCode = "§a";
            else if (reach < 2.85) colorCode = "§e";
            else colorCode = "§c";
        }

        return switch (displayFormat) {
            case REACH_PREFIX -> "Reach: " + colorCode + formatted;
            case SIMPLE_M -> colorCode + formatted + "§rm";
            case BLOCKS -> colorCode + formatted + "§rb";
        };
    }

    @Override
    public String displayText(Minecraft client) {
        return value(client);
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        if (editor) {
            String fmt = "%." + precision + "f";
            String formatted = String.format(Locale.ROOT, fmt, 2.94);
            String colorCode = colorCoding ? "§e" : "";
            return switch (displayFormat) {
                case REACH_PREFIX -> "Reach: " + colorCode + formatted;
                case SIMPLE_M -> colorCode + formatted + "§rm";
                case BLOCKS -> colorCode + formatted + "§rb";
            };
        }
        return displayText(client);
    }
}
