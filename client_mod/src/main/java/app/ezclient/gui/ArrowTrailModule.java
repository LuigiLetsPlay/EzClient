package app.ezclient.gui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/** Spawns a client-only particle ribbon behind arrows. */
public final class ArrowTrailModule extends FeatureModule {
    private final Map<Integer, Long> lastParticle = new HashMap<>();
    private final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();

    public ArrowTrailModule() {
        super("Arrow Trail", false, 0);
        option("Partikel", "particle", "Particle", "Partikeltyp der Spur.", "Flame", 0, 0,
                "Flame", "Soul fire", "Crit", "Enchant", "End rod", "Happy villager", "Cloud", "Smoke", "Portal");
        option("Partikel", "density", "Density", "Partikel pro Pfeil und Tick.", 2.0, 1, 8);
        option("Partikel", "interval", "Interval ticks", "Abstand zwischen zwei Partikelpaketen.", 1.0, 1, 10);
        option("Bewegung", "spread", "Spread", "Zufällige Streuung der Spur.", 0.02, 0, 0.3);
        option("Bewegung", "speed", "Particle speed", "Geschwindigkeit der Partikel.", 0.0, 0, 0.25);
        flag("Filter", "ownOnly", "Own arrows only", "Zeigt nur vom lokalen Spieler geschossene Pfeile.", false);
        option("Filter", "range", "Maximum range", "Maximale Entfernung für Spuren.", 96.0, 8, 256);
    }

    @Override public void onTick() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null || mc.level.getGameTime() % (long)number("interval") != 0) return;
        long tick = mc.level.getGameTime();
        double maxSq = number("range") * number("range");
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AbstractArrow arrow) || arrow.distanceToSqr(mc.player) > maxSq || arrow.getDeltaMovement().lengthSqr() < 0.0001) continue;
            if (flag("ownOnly") && arrow.getOwner() != mc.player) continue;
            if (lastParticle.getOrDefault(arrow.getId(), -1L) == tick) continue;
            lastParticle.put(arrow.getId(), tick);
            ParticleOptions type = particle();
            for (int i = 0; i < (int)number("density"); i++) {
                double spread = number("spread"), speed = number("speed");
                double ox = (random.nextDouble() - .5) * spread;
                double oy = (random.nextDouble() - .5) * spread;
                double oz = (random.nextDouble() - .5) * spread;
                mc.level.addParticle(type, arrow.getX() + ox, arrow.getY() + arrow.getBbHeight() * .5 + oy, arrow.getZ() + oz,
                        -arrow.getDeltaMovement().x * speed, -arrow.getDeltaMovement().y * speed, -arrow.getDeltaMovement().z * speed);
            }
        }
        if (lastParticle.size() > 512) lastParticle.entrySet().removeIf(e -> tick - e.getValue() > 200);
    }

    private ParticleOptions particle() {
        return switch (text("particle")) {
            case "Soul fire" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "Crit" -> ParticleTypes.CRIT;
            case "Enchant" -> ParticleTypes.ENCHANT;
            case "End rod" -> ParticleTypes.END_ROD;
            case "Happy villager" -> ParticleTypes.HAPPY_VILLAGER;
            case "Cloud" -> ParticleTypes.CLOUD;
            case "Smoke" -> ParticleTypes.SMOKE;
            case "Portal" -> ParticleTypes.PORTAL;
            default -> ParticleTypes.FLAME;
        };
    }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/arrow_trail.png"); }
}
