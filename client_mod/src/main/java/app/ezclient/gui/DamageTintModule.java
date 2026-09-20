package app.ezclient.gui;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Damage Tint / Hit Color Module:
 * Customizes entity hurt flash colors away from vanilla red to custom RGBA hues or chroma effects.
 * Implemented as a declarative FeatureModule with universal color field and per-entity rules.
 */
public final class DamageTintModule extends FeatureModule {
    public enum TargetScope {
        ALL_ENTITIES("All Entities"),
        PLAYERS_ONLY("Players Only"),
        SELF_ONLY("Self Only"),
        NONE("None");

        private final String label;
        TargetScope(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final Map<String, Integer> entityRules = new ConcurrentHashMap<>();

    public DamageTintModule() {
        super("Damage Tint", false, 0);
        colorOption("Schadensfarbe / Flash", "color", "Schadensfarbe", "Farbe und Transparenz des Schadensblitzes.", "B4FF2255");
        flag("Schadensfarbe / Flash", "chroma", "Chroma-Modus", "Animiert die Schadensfarbe im Regenbogen-Verlauf.", false);
        option("Schadensfarbe / Flash", "flashDuration", "Flash-Dauer", "Multiplikator für die Dauer des Schadensblitzes.", 1.0, 0.5, 2.0);
        option("Zielbereich", "scope", "Ziele", "Wählt die Ziele für den Schadenstreffer-Effekt.",
                "All Entities", 0, 0, "All Entities", "Players Only", "Self Only", "None");
    }

    @Override
    public boolean hasPreview() {
        return false;
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/armor_status.png");
    }

    @Override
    public String getDescription() {
        return "Färbt den Schadenstreffer-Effekt für ausgewählte Ziele mit eigener Farbe, Transparenz oder Chroma ein.";
    }

    @Override
    protected void onToggle() {
        applyToTexture(Minecraft.getInstance());
    }

    public TargetScope getTargetScope() {
        String s = text("scope");
        try {
            for (TargetScope scope : TargetScope.values()) {
                if (scope.getLabel().equalsIgnoreCase(s) || scope.name().equalsIgnoreCase(s)) return scope;
            }
        } catch (Exception ignored) {}
        return TargetScope.ALL_ENTITIES;
    }

    public void setTargetScope(TargetScope targetScope) {
        set("scope", targetScope.getLabel());
        applyToTexture(Minecraft.getInstance());
    }

    public int getCustomColor() {
        return tint("color", false) & 0x00FFFFFF;
    }

    public void setCustomColor(int color) {
        int alpha = getCustomAlpha();
        int full = ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
        set("color", String.format("%08X", full));
        applyToTexture(Minecraft.getInstance());
    }

    public int getCustomAlpha() {
        return (tint("color", false) >>> 24) & 0xFF;
    }

    public void setCustomAlpha(int alpha) {
        int rgb = getCustomColor();
        int full = ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (rgb & 0x00FFFFFF);
        set("color", String.format("%08X", full));
        applyToTexture(Minecraft.getInstance());
    }

    public boolean isChromaMode() {
        return flag("chroma");
    }

    public void setChromaMode(boolean chroma) {
        set("chroma", chroma);
        applyToTexture(Minecraft.getInstance());
    }

    public float getFlashDurationMultiplier() {
        return (float) number("flashDuration");
    }

    public void setFlashDurationMultiplier(float multiplier) {
        set("flashDuration", (double) Math.max(0.5f, Math.min(2.0f, multiplier)));
    }

    public Map<String, Integer> getEntityRules() { return entityRules; }
    public Integer getEntityRule(String entityId) { return entityRules.get(entityId); }
    public void setEntityRule(String entityId, int color) {
        entityRules.put(entityId, color);
        ConfigManager.save();
        applyToTexture(Minecraft.getInstance());
    }
    public void removeEntityRule(String entityId) {
        entityRules.remove(entityId);
        ConfigManager.save();
        applyToTexture(Minecraft.getInstance());
    }
    public void clearEntityRules() {
        entityRules.clear();
        ConfigManager.save();
        applyToTexture(Minecraft.getInstance());
    }

    @Override
    public JsonObject saveFeature() {
        JsonObject json = super.saveFeature();
        JsonObject rulesObj = new JsonObject();
        for (Map.Entry<String, Integer> entry : entityRules.entrySet()) {
            rulesObj.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("entityRules", rulesObj);
        return json;
    }

    @Override
    public void loadFeature(JsonObject json) {
        super.loadFeature(json);
        entityRules.clear();
        if (json.has("entityRules") && json.get("entityRules").isJsonObject()) {
            JsonObject obj = json.getAsJsonObject("entityRules");
            for (String key : obj.keySet()) {
                try {
                    entityRules.put(key, obj.get(key).getAsInt());
                } catch (Exception ignored) {}
            }
        }
        applyToTexture(Minecraft.getInstance());
    }

    public void applyToTexture(Minecraft client) {
        if (client == null || client.gameRenderer == null) return;
        try {
            OverlayTexture overlay = client.gameRenderer.overlayTexture();
            if (overlay == null) return;
            DynamicTexture tex = ((app.ezclient.mixin.OverlayTextureAccessor) overlay).ezclient$getTexture();
            if (tex != null && tex.getPixels() != null) {
                var img = tex.getPixels();
                int tint = getTint(null, false);
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 16; x++) {
                        img.setPixel(x, y, tint);
                    }
                }
                tex.upload();
            }
        } catch (Throwable ignored) {}
    }

    public int getTint(Entity entity, boolean isSelf) {
        if (!isEnabled()) return 0xB3FF0000;

        if (entity != null) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            if (entityRules.containsKey(id)) {
                return entityRules.get(id);
            }
        }

        TargetScope targetScope = getTargetScope();
        if (targetScope == TargetScope.NONE) {
            return 0xB3FF0000;
        }
        if (targetScope == TargetScope.PLAYERS_ONLY && !(entity instanceof Player)) {
            return 0xB3FF0000;
        }
        if (targetScope == TargetScope.SELF_ONLY && !isSelf) {
            return 0xB3FF0000;
        }

        int alpha = getCustomAlpha();
        if (isChromaMode()) {
            float hue = (float) ((System.currentTimeMillis() % 2000L) / 2000.0);
            int rgb = Color.HSBtoRGB(hue, 0.9f, 1.0f) & 0x00FFFFFF;
            return (alpha << 24) | rgb;
        }

        return (alpha << 24) | getCustomColor();
    }
}
