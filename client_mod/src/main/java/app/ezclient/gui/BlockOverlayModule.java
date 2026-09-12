package app.ezclient.gui;

import net.minecraft.resources.Identifier;

public final class BlockOverlayModule extends FeatureModule {
    public record BlockRule(String style, int outlineColor, int fillColor, double fillOpacity) {}
    private final java.util.Map<String, BlockRule> blockRules = new java.util.concurrent.ConcurrentHashMap<>();

    public BlockOverlayModule() {
        super("Block Overlay", false, 10);
        option("Darstellung", "style", "Render style", "Wählt Kontur, Füllung oder beides.", "Outline", 0, 0, "Outline", "Fill", "Both", "None");
        option("Darstellung", "width", "Line width", "Stärke der Blockkontur.", 2.0, 1, 5); colorOption("Farbe", "outline", "Outline", "Farbe der Blockkontur.", "FFFFFFFF");
        flag("Farbe", "chroma", "Chroma outline", "Animiert die Konturfarbe.", false); colorOption("Farbe", "fill", "Fill", "Farbe der transparenten Füllung.", "FFFFFFFF");
        option("Farbe", "fillOpacity", "Fill opacity %", "Transparenz der Blockfüllung.", 15.0, 0, 100);
        option("Erweitert", "break", "Breaking cracks", "Steuert die Abbauanzeige.", "Vanilla", 0, 0, "Vanilla", "Hidden", "Tint overlay");
        colorOption("Erweitert", "breakColor", "Breaking overlay", "Farbe der Abbauüberlagerung.", "40FFAA00");
    }

    public java.util.Map<String, BlockRule> getBlockRules() { return blockRules; }
    public BlockRule getBlockRule(String blockId) { return blockRules.get(blockId); }
    public void setBlockRule(String blockId, BlockRule rule) { blockRules.put(blockId, rule); ConfigManager.save(); }
    public void removeBlockRule(String blockId) { blockRules.remove(blockId); ConfigManager.save(); }
    public void clearBlockRules() { blockRules.clear(); ConfigManager.save(); }

    @Override
    public com.google.gson.JsonObject saveFeature() {
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
