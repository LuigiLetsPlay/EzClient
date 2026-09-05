package app.ezclient.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Chat Customizer / Tweaks Module:
 * Enhances Minecraft's native chat with:
 * - Adjustable background opacity (or fully invisible)
 * - Increased chat history up to 10,000 lines
 * - Timestamps ([HH:mm] / [HH:mm:ss])
 * - Click-to-copy chat text
 */
public final class ChatCustomizerModule extends Module {
    public enum TimestampFormat {
        NONE("Off"),
        HH_MM("[HH:mm]"),
        HH_MM_SS("[HH:mm:ss]");

        private final String label;
        TimestampFormat(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static final DateTimeFormatter FMT_HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimestampFormat timestampFormat = TimestampFormat.HH_MM;
    private int backgroundOpacity = 50; // 0 to 100%
    private int lineLimit = 5000; // 100 to 10000
    private boolean copyOnClick = true;

    public ChatCustomizerModule() {
        super("Chat Customizer", "HUD", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/chat.png");
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public TimestampFormat getTimestampFormat() { return timestampFormat; }
    public void setTimestampFormat(TimestampFormat timestampFormat) { this.timestampFormat = timestampFormat; ConfigManager.save(); }

    public int getBackgroundOpacity() { return backgroundOpacity; }
    public void setBackgroundOpacity(int backgroundOpacity) { this.backgroundOpacity = Math.max(0, Math.min(100, backgroundOpacity)); ConfigManager.save(); }

    public int getLineLimit() { return lineLimit; }
    public void setLineLimit(int lineLimit) { this.lineLimit = Math.max(100, Math.min(10000, lineLimit)); ConfigManager.save(); }

    public boolean isCopyOnClick() { return copyOnClick; }
    public void setCopyOnClick(boolean copyOnClick) { this.copyOnClick = copyOnClick; ConfigManager.save(); }

    public Component appendTimestamp(Component original) {
        if (!isEnabled() || timestampFormat == TimestampFormat.NONE) return original;

        LocalTime now = LocalTime.now();
        String stamp = switch (timestampFormat) {
            case HH_MM -> "[" + now.format(FMT_HH_MM) + "] ";
            case HH_MM_SS -> "[" + now.format(FMT_HH_MM_SS) + "] ";
            default -> "";
        };

        return Component.literal("§8" + stamp + "§r").append(original);
    }
}
