package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Live JVM memory indicator with configurable format presets,
 * declarative FeatureModule options, dynamic warning colors, and unified color picker support.
 */
public final class MemoryModule extends FeatureModule {
    public enum MemoryFormat {
        USED_MAX("Used/Max"),
        PERCENTAGE("Percent"),
        USED_ONLY("Used"),
        FREE("Free");

        private final String label;
        MemoryFormat(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public MemoryModule() {
        super("Memory", true, 128);
        option("Darstellung", "format", "Format", "Darstellungsformat für den belegten Arbeitsspeicher.",
                "Used/Max", 0, 0, "Used/Max", "Percent", "Used", "Free");
        option("Darstellung", "prefix", "Präfix", "Text vor dem angezeigten Speicherwert.", "RAM: ", 0, 0);
        flag("Darstellung", "colorWarning", "Dynamische Warnfarbe", "Färbt die Anzeige bei hoher Speicherauslastung orange bzw. rot ein.", true);
        colorOption("Farben", "textColor", "Textfarbe", "Farbe des Anzeigetextes.", "FFFFFFFF");
        flag("Farben", "chroma", "Chroma-Effekt", "Animiert den Text im flüssigen Regenbogen-Verlauf.", false);
    }



    @Override
    public String getDescription() {
        return "Zeigt die aktuelle Speicherbelegung des Minecraft-Clients im HUD an.";
    }

    public MemoryFormat getMemoryFormat() {
        String fmt = text("format");
        for (MemoryFormat f : MemoryFormat.values()) {
            if (f.getLabel().equalsIgnoreCase(fmt) || f.name().equalsIgnoreCase(fmt)) return f;
        }
        return MemoryFormat.USED_MAX;
    }

    public void setMemoryFormat(MemoryFormat memoryFormat) {
        set("format", memoryFormat != null ? memoryFormat.getLabel() : "Used/Max");
    }

    public boolean isShowPrefix() {
        return !text("prefix").isEmpty();
    }

    public void setShowPrefix(boolean showPrefix) {
        set("prefix", showPrefix ? "RAM: " : "");
    }

    @Override
    public int color() {
        return color(0);
    }

    @Override
    public int color(long offsetMs) {
        if (flag("chroma")) {
            return tint("textColor", true);
        }
        if (flag("colorWarning")) {
            Runtime runtime = Runtime.getRuntime();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            long used = (total - free) / (1024L * 1024L);
            long max = runtime.maxMemory() / (1024L * 1024L);
            double ratio = max > 0 ? (double) used / (double) max : 0;
            if (ratio > 0.85) return 0xFFFF4444; // Warning red
            if (ratio > 0.70) return 0xFFFFAA00; // Caution orange
        }
        return tint("textColor", false);
    }

    @Override
    protected String value(Minecraft client) {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = (total - free) / (1024L * 1024L);
        long max = runtime.maxMemory() / (1024L * 1024L);

        return switch (getMemoryFormat()) {
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
    public List<String> lines(Minecraft client, boolean editor) {
        String pfx = text("prefix");
        if (editor) {
            String val = switch (getMemoryFormat()) {
                case USED_MAX -> "1450 / 4096 MB";
                case PERCENTAGE -> "35%";
                case USED_ONLY -> "1450 MB";
                case FREE -> "2646 MB";
            };
            return List.of(pfx + val);
        }
        return List.of(pfx + value(client));
    }

    @Override
    public String displayText(Minecraft client) {
        return displayText(client, false);
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        List<String> rows = lines(client, editor);
        return rows.isEmpty() ? "" : rows.get(0);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/memory.png");
    }
}

