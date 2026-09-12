package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;

public final class HitboxModule extends FeatureModule {
    public record EntityRule(boolean enabled, int color, float width) {}
    private final java.util.Map<String, EntityRule> entityRules = new java.util.LinkedHashMap<>();
    public EntityRule rule(String id) { return entityRules.get(id); }
    public void setRule(String id, EntityRule rule) {
        if (rule == null) entityRules.remove(id);
        else entityRules.put(id, new EntityRule(rule.enabled(), rule.color(), Math.max(1, Math.min(3, rule.width()))));
        ConfigManager.save();
    }
    private EntityRule rule(Entity entity) {
        return entityRules.get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
    public int colorFor(Entity entity) {
        if (net.minecraft.client.Minecraft.getInstance().crosshairPickEntity == entity) return 0xffff0000;
        EntityRule rule = rule(entity);
        return rule == null ? tint("box", flag("chroma")) : rule.color();
    }
    public float widthFor(Entity entity) {
        EntityRule rule = rule(entity);
        return rule == null ? (float)number("width") : rule.width();
    }
    @Override public com.google.gson.JsonObject saveFeature() {
        var json = super.saveFeature();
        json.add("entityTypes", new com.google.gson.Gson().toJsonTree(entityRules));
        return json;
    }
    @Override public void loadFeature(com.google.gson.JsonObject json) {
        entityRules.clear();
        if (json.has("entityTypes") && json.get("entityTypes").isJsonObject()) {
            for (var entry : json.getAsJsonObject("entityTypes").entrySet()) {
                try {
                    var rule = new com.google.gson.Gson().fromJson(entry.getValue(), EntityRule.class);
                    if (rule != null && Float.isFinite(rule.width()))
                        entityRules.put(entry.getKey(), new EntityRule(rule.enabled(), rule.color(), Math.max(1, Math.min(3, rule.width()))));
                } catch (RuntimeException ignored) {}
            }
        }
        super.loadFeature(json);
    }
    public HitboxModule() {
        super("Hitbox Visualizer", false, 10);
        flag("Entity-Typen", "players", "Players", "Zeigt Hitboxen von Spielern.", true); flag("Entity-Typen", "hostile", "Hostile mobs", "Zeigt feindliche Kreaturen.", true);
        flag("Entity-Typen", "animals", "Passive animals", "Zeigt passive Kreaturen.", true); flag("Entity-Typen", "projectiles", "Projectiles", "Zeigt Projektile.", true); flag("Entity-Typen", "items", "Dropped items", "Zeigt fallengelassene Gegenstände.", false);
        colorOption("Farbe", "box", "Hitbox color", "Grundfarbe der Kontur.", "FFFFFFFF"); option("Darstellung", "width", "Line width", "Stärke der Hitbox-Kontur.", 1.0, 1, 3);
        flag("Farbe", "chroma", "Chroma hitboxes", "Animiert die Konturfarbe.", false); flag("Darstellung", "eyes", "Eye height", "Zeigt die Augenhöhe.", true); flag("Darstellung", "look", "Look vector", "Zeigt die Blickrichtung.", true);
        colorOption("Farbe", "eyeColor", "Eye color", "Farbe der Augenhöhen-Markierung.", "FFFF3333"); colorOption("Farbe", "lookColor", "Look color", "Farbe des Blickvektors.", "FF3377FF");
        flag("Darstellung", "fill", "Fill", "Füllt die Hitbox transparent aus.", false); colorOption("Farbe", "fillColor", "Fill color", "Farbe und Transparenz der Füllung.", "26FFFFFF");
        flag("Erweitert", "debugOnly", "Only with F3+B", "Zeigt Hitboxen nur im Minecraft-Debugmodus.", false);
    }
    public boolean accepts(Entity e) {
        EntityRule rule = rule(e);
        if (rule != null) return rule.enabled();
        return e instanceof Player ? flag("players") : e instanceof Enemy ? flag("hostile") : e instanceof Animal ? flag("animals")
            : e instanceof Projectile ? flag("projectiles") : e instanceof ItemEntity && flag("items");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/hitbox.png");
    }
}
