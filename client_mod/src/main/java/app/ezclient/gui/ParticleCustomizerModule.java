package app.ezclient.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleCustomizerModule extends FeatureModule {
    private final Set<String> disabledParticles = ConcurrentHashMap.newKeySet();

    public ParticleCustomizerModule() {
        super("Particle Customizer", false, 10);
        flag("Allgemein", "allParticles", "Alle Partikel", "Master-Schalter für sämtliche Minecraft-Partikel.", true);
        option("Allgemein", "multiplier", "Crit multiplier", "Multipliziert kritische Trefferpartikel.", 1.0, 0, 5);
        flag("Kampf", "alwaysCrit", "Always crit particles", "Erzeugt bei Treffern kritische Partikel.", false);
        flag("Kampf", "alwaysSharpness", "Always sharpness particles", "Erzeugt Verzauberungs-Partikel bei Treffern.", false);
        flag("Sichtbarkeit", "smoke", "Smoke", "Blendet Rauchpartikel ein oder aus.", true);
        flag("Sichtbarkeit", "explosion", "Explosions", "Blendet Explosionspartikel ein oder aus.", true);
        flag("Sichtbarkeit", "ownPotion", "Own potion swirls", "Zeigt eigene Effektpartikel.", true);
        flag("Farbe", "tint", "Custom particle tint", "Aktiviert eine eigene Partikelfarbe.", false);
        colorOption("Farbe", "color", "Particle color", "Farbe für getönte Partikel.", "FFA855F7");
    }

    public boolean isParticleEnabled(Identifier id) {
        if (id == null) return true;
        return !disabledParticles.contains(id.toString());
    }

    public void setParticleEnabled(Identifier id, boolean enabled) {
        if (id == null) return;
        if (enabled) {
            disabledParticles.remove(id.toString());
        } else {
            disabledParticles.add(id.toString());
        }
        ConfigManager.save();
    }

    public void enableAllParticles() {
        disabledParticles.clear();
        ConfigManager.save();
    }

    public void disableAllParticles() {
        BuiltInRegistries.PARTICLE_TYPE.keySet().forEach(id -> disabledParticles.add(id.toString()));
        ConfigManager.save();
    }

    public int getDisabledCount() {
        return disabledParticles.size();
    }

    public boolean allows(ParticleOptions options) {
        if (!flag("allParticles")) return false;
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (id == null) return true;
        return !disabledParticles.contains(id.toString());
    }

    public static String displayName(Identifier id) {
        if (id == null) return "";
        StringBuilder label = new StringBuilder();
        for (String word : id.getPath().split("[_/]+")) {
            if (word.isBlank()) continue;
            if (!label.isEmpty()) label.append(' ');
            label.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) label.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        if (!"minecraft".equals(id.getNamespace())) label.append(" (").append(id.getNamespace()).append(')');
        return label.toString();
    }

    @Override
    public JsonObject saveFeature() {
        JsonObject json = super.saveFeature();
        if (!disabledParticles.isEmpty()) {
            JsonArray arr = new JsonArray();
            disabledParticles.stream().sorted().forEach(arr::add);
            json.add("disabled_particles", arr);
        }
        return json;
    }

    @Override
    public void loadFeature(JsonObject json) {
        super.loadFeature(json);
        disabledParticles.clear();
        if (json.has("disabled_particles") && json.get("disabled_particles").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("disabled_particles")) {
                if (el.isJsonPrimitive()) {
                    disabledParticles.add(el.getAsString());
                }
            }
        }
    }

    public void attack(Entity target) {
        if (!isEnabled()) return;
        var engine = Minecraft.getInstance().particleEngine;
        if (flag("alwaysCrit")) engine.createTrackingEmitter(target, ParticleTypes.CRIT);
        if (flag("alwaysSharpness")) engine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/particle.png");
    }
}
