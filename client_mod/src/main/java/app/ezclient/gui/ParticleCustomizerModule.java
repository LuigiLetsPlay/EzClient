package app.ezclient.gui;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;

public final class ParticleCustomizerModule extends FeatureModule {
    public ParticleCustomizerModule() {
        super("Particle Customizer", false, 10);
        option("multiplier", "Crit multiplier", 1.0, 0, 5);
        flag("alwaysCrit", "Always crit particles", false); flag("alwaysSharpness", "Always sharpness particles", false);
        flag("smoke", "Smoke", true); flag("explosion", "Explosions", true); flag("ownPotion", "Own potion swirls", true);
        flag("tint", "Custom particle tint", false); colorOption("color", "Particle color", "FFA855F7");
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
