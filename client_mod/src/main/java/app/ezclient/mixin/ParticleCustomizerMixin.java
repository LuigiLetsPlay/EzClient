package app.ezclient.mixin;
import app.ezclient.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ParticleEngine.class)
public abstract class ParticleCustomizerMixin {
    @Unique private boolean ezclient$duplicating;
    @Unique private long ezclient$window;
    @Unique private int ezclient$extras;
    @Shadow public abstract Particle createParticle(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz);
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void ezclient$filter(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        var m = FeatureModule.get(ParticleCustomizerModule.class); var weather = FeatureModule.get(TimeWeatherModule.class);
        if (!m.isEnabled() && !weather.isEnabled()) return;
        String id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType()).getPath();
        if (weather.isEnabled() && !weather.flag("precipitation") && (id.equals("rain") || id.equals("splash"))) { cir.setReturnValue(null); return; }
        if (!m.isEnabled() || ezclient$duplicating) return;
        var player = Minecraft.getInstance().player;
        boolean own = !m.flag("ownPotion") && id.contains("effect") && player != null && player.getBoundingBox().inflate(.25).contains(x, y, z);
        boolean crit = options.getType() == ParticleTypes.CRIT || options.getType() == ParticleTypes.ENCHANTED_HIT;
        if ((!m.flag("smoke") && id.contains("smoke")) || (!m.flag("explosion") && id.contains("explosion")) || own
            || (crit && m.number("multiplier") < 1 && Math.random() >= m.number("multiplier"))) cir.setReturnValue(null);
    }
    @Inject(method = "createParticle", at = @At("RETURN"))
    private void ezclient$multiply(ParticleOptions options, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        var m = FeatureModule.get(ParticleCustomizerModule.class);
        if (!m.isEnabled() || cir.getReturnValue() == null) return;
        if (m.flag("tint") && cir.getReturnValue() instanceof SingleQuadParticle particle) {
            int color = m.tint("color", false);
            particle.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
            ((ParticleAlphaAccessor)particle).ezclient$alpha((color >>> 24) / 255f);
        }
        boolean crit = options.getType() == ParticleTypes.CRIT || options.getType() == ParticleTypes.ENCHANTED_HIT;
        if (!crit || ezclient$duplicating || m.number("multiplier") <= 1) return;
        long now = System.nanoTime() / 50_000_000L;
        if (now != ezclient$window) { ezclient$window = now; ezclient$extras = 0; }
        double extra = m.number("multiplier") - 1;
        int copies = (int)extra + (Math.random() < extra % 1 ? 1 : 0);
        ezclient$duplicating = true;
        try { for (int i = 0; i < copies && ezclient$extras < 200; i++, ezclient$extras++)
            createParticle(options, x + (Math.random() - .5) * .15, y + (Math.random() - .5) * .15, z + (Math.random() - .5) * .15, vx, vy, vz);
        } finally { ezclient$duplicating = false; }
    }
}
