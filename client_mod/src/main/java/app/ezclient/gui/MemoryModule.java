package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Live JVM memory indicator with configurable format presets. */
public final class MemoryModule extends HudModule {
    public enum MemoryFormat {
        USED_MAX("Used/Max"),
        PERCENTAGE("Percent"),
        USED_ONLY("Used"),
        FREE("Free");

        private final String label;
        MemoryFormat(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private MemoryFormat memoryFormat = MemoryFormat.USED_MAX;
    private boolean showPrefix = true;

    public MemoryModule() {
        super("Memory", "Performance", false, 6, 128, "RAM: ", "");
    }

    public MemoryFormat getMemoryFormat() { return memoryFormat; }
    public void setMemoryFormat(MemoryFormat memoryFormat) {
        this.memoryFormat = memoryFormat;
        ConfigManager.save();
    }

    public boolean isShowPrefix() { return showPrefix; }
    public void setShowPrefix(boolean showPrefix) {
        this.showPrefix = showPrefix;
        setPrefix(showPrefix ? "RAM: " : "");
        ConfigManager.save();
    }

    @Override
    protected String value(Minecraft client) {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = (total - free) / (1024L * 1024L);
        long max = runtime.maxMemory() / (1024L * 1024L);

        return switch (memoryFormat) {
            case USED_MAX -> used + " / " + max + " MB";
            case PERCENTAGE -> {
                int pct = max > 0 ? (int) ((used * 100) / max) : 0;
                yield pct + "%";
            }
            case USED_ONLY -> used + " MB";
            case FREE -> {
                long freeMb = (max - used);
                yield freeMb + " MB";
            }
        };
    }

    @Override
    public String displayText(Minecraft client) {
        return (showPrefix ? "RAM: " : "") + value(client);
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        if (editor) {
            String val = switch (memoryFormat) {
                case USED_MAX -> "1450 / 4096 MB";
                case PERCENTAGE -> "35%";
                case USED_ONLY -> "1450 MB";
                case FREE -> "2646 MB";
            };
            return (showPrefix ? "RAM: " : "") + val;
        }
        return displayText(client);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/memory.png");
    }
}
