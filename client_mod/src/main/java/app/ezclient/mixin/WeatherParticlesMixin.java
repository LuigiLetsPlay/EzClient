package app.ezclient.mixin;
import app.ezclient.gui.*;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WeatherEffectRenderer.class)
public class WeatherParticlesMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void ezclient$precipitation(net.minecraft.client.multiplayer.ClientLevel level, float delta, net.minecraft.world.phys.Vec3 position,
            net.minecraft.client.renderer.state.level.WeatherRenderState state, CallbackInfo ci) {
        var module = FeatureModule.get(TimeWeatherModule.class);
        if (module.isEnabled() && !module.flag("precipitation")) { state.reset(); ci.cancel(); }
    }
}
