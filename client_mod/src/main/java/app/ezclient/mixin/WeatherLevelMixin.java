package app.ezclient.mixin;
import app.ezclient.gui.*;
import net.minecraft.world.level.Level;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
public class WeatherLevelMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void ezclient$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Object)this instanceof ClientLevel)) return;
        var module = FeatureModule.get(TimeWeatherModule.class);
        if (module.isEnabled() && !module.text("weather").equals("Server")) cir.setReturnValue(module.text("weather").equals("Clear") ? 0f : 1f);
    }
    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void ezclient$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (!((Object)this instanceof ClientLevel)) return;
        var module = FeatureModule.get(TimeWeatherModule.class);
        if (module.isEnabled() && !module.text("weather").equals("Server")) cir.setReturnValue(module.text("weather").equals("Thunder") ? 1f : 0f);
    }
}
