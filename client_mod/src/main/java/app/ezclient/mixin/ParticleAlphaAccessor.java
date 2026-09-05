package app.ezclient.mixin;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(SingleQuadParticle.class)
public interface ParticleAlphaAccessor {
    @Invoker("setAlpha") void ezclient$alpha(float alpha);
}
