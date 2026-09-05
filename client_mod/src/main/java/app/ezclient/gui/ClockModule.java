package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Configurable local clock with multiple 12h/24h and seconds format presets. */
public final class ClockModule extends HudModule {
    public enum ClockFormat {
        H24("24h", "HH:mm"),
        H24_SEC("24h+s", "HH:mm:ss"),
        H12("12h", "h:mm a"),
        H12_SEC("12h+s", "h:mm:ss a");

        private final String label;
        private final DateTimeFormatter formatter;

        ClockFormat(String label, String pattern) {
            this.label = label;
            this.formatter = DateTimeFormatter.ofPattern(pattern);
        }

        public String getLabel() { return label; }
        public String format(LocalTime time) { return time.format(formatter); }
    }

    private ClockFormat clockFormat = ClockFormat.H24;
    private boolean showPrefix = true;

    public ClockModule() {
        super("Clock", "HUD", false, 6, 112, "Time: ", "");
    }

    public ClockFormat getClockFormat() { return clockFormat; }
    public void setClockFormat(ClockFormat clockFormat) {
        this.clockFormat = clockFormat;
        ConfigManager.save();
    }

    public boolean isShowPrefix() { return showPrefix; }
    public void setShowPrefix(boolean showPrefix) {
        this.showPrefix = showPrefix;
        setPrefix(showPrefix ? "Time: " : "");
        ConfigManager.save();
    }

    @Override
    protected String value(Minecraft client) {
        return clockFormat.format(LocalTime.now());
    }

    @Override
    public String displayText(Minecraft client) {
        return (showPrefix ? "Time: " : "") + value(client);
    }
}
