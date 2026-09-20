package app.ezclient.gui;

import net.minecraft.resources.Identifier;

public final class BlockOverlayModule extends FeatureModule {
    public record BlockRule(String style, int outlineColor, int fillColor, double fillOpacity) {}
    private final java.util.Map<String, BlockRule> blockRules = new java.util.concurrent.ConcurrentHashMap<>();

    public BlockOverlayModule() {
        super("Block Overlay", false, 10);
        // Category 1: Outline
        flag("Outline", "outline_active", "Outline aktiv", "Schaltet die Blockkontur ein oder aus.", true);
        colorOption("Outline", "outline", "Outline-Farbe", "Farbe der Blockkontur.", "FFFFFFFF");
        option("Outline", "width", "Konturstärke", "Stärke der Blockkontur.", 2.0, 1, 5);
        flag("Outline", "chroma", "Chroma-Kontur", "Animiert die Konturfarbe im Regenbogen-Verlauf.", false);

        // Category 2: Filling
        flag("Filling", "fill_active", "Filling aktiv", "Schaltet die transparente Blockfüllung ein oder aus.", true);
        colorOption("Filling", "fill", "Füllfarbe", "Farbe der transparenten Blockfüllung.", "FFFFFFFF");
        option("Filling", "fillOpacity", "Deckkraft %", "Transparenz der Blockfüllung in Prozent.", 15.0, 0, 100);

        // Category 3: Abbau / Break
        option("Abbau / Break", "break", "Abbauanzeige", "Darstellung während des Blockabbaus.", "Vanilla", 0, 0, "Vanilla", "Hidden", "Tint overlay");
        colorOption("Abbau / Break", "breakColor", "Abbau-Farbe", "Farbe der Abbauüberlagerung.", "40FFAA00");

        // Internal style property kept for compatibility
        option("System", "style", "Style", "Interner Renderstil.", "Both", 0, 0, "Outline", "Fill", "Both", "None");
        syncStyle();
    }

    public void syncStyle() {
        boolean out = flag("outline_active");
        boolean fil = flag("fill_active");
        String effective;
        if (out && fil) effective = "Both";
        else if (out) effective = "Outline";
        else if (fil) effective = "Fill";
        else effective = "None";
        set("style", effective);
    }

    @Override
    public boolean set(Option option, Object value) {
        boolean ok = super.set(option, value);
        if (ok && ("outline_active".equals(option.key()) || "fill_active".equals(option.key()))) {
            syncStyle();
        }
        return ok;
    }

    public boolean isOutlineActive() {
        return flag("outline_active");
    }

    public boolean isFillActive() {
        return flag("fill_active");
    }

    public java.util.Map<String, BlockRule> getBlockRules() { return blockRules; }
    public BlockRule getBlockRule(String blockId) { return blockRules.get(blockId); }
    public void setBlockRule(String blockId, BlockRule rule) { blockRules.put(blockId, rule); ConfigManager.save(); }
    public void removeBlockRule(String blockId) { blockRules.remove(blockId); ConfigManager.save(); }
    public void clearBlockRules() { blockRules.clear(); ConfigManager.save(); }

    @Override
    public com.google.gson.JsonObject saveFeature() {
        syncStyle();
        var json = super.saveFeature();
        var rulesObj = new com.google.gson.JsonObject();
        for (var entry : blockRules.entrySet()) {
            var r = entry.getValue();
            var ro = new com.google.gson.JsonObject();
            ro.addProperty("style", r.style());
            ro.addProperty("outlineColor", r.outlineColor());
            ro.addProperty("fillColor", r.fillColor());
            ro.addProperty("fillOpacity", r.fillOpacity());
            rulesObj.add(entry.getKey(), ro);
        }
        json.add("blockRules", rulesObj);
        return json;
    }

    @Override
    public void loadFeature(com.google.gson.JsonObject json) {
        super.loadFeature(json);
        if (json.has("style") && json.get("style").isJsonPrimitive()) {
            String loadedStyle = json.get("style").getAsString();
            if ("Both".equalsIgnoreCase(loadedStyle)) {
                set("outline_active", true);
                set("fill_active", true);
            } else if ("Outline".equalsIgnoreCase(loadedStyle)) {
                set("outline_active", true);
                set("fill_active", false);
            } else if ("Fill".equalsIgnoreCase(loadedStyle)) {
                set("outline_active", false);
                set("fill_active", true);
            } else if ("None".equalsIgnoreCase(loadedStyle)) {
                set("outline_active", false);
                set("fill_active", false);
            }
        }
        syncStyle();

        blockRules.clear();
        if (json.has("blockRules") && json.get("blockRules").isJsonObject()) {
            var obj = json.getAsJsonObject("blockRules");
            for (String key : obj.keySet()) {
                if (obj.get(key).isJsonObject()) {
                    var ro = obj.getAsJsonObject(key);
                    String style = ro.has("style") ? ro.get("style").getAsString() : "Outline";
                    int outlineColor = ro.has("outlineColor") ? ro.get("outlineColor").getAsInt() : 0xFFFFFFFF;
                    int fillColor = ro.has("fillColor") ? ro.get("fillColor").getAsInt() : 0xFFFFFFFF;
                    double fillOpacity = ro.has("fillOpacity") ? ro.get("fillOpacity").getAsDouble() : 15.0;
                    blockRules.put(key, new BlockRule(style, outlineColor, fillColor, fillOpacity));
                }
            }
        }
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/block_overlay.png");
    }
}
