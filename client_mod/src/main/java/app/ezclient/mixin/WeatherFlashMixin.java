package app.ezclient.mixin;
import app.ezclient.gui.*;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ClientLevel.class)
public class WeatherFlashMixin {
    @Inject(method = "getSkyFlashTime", at = @At("HEAD"), cancellable = true)
    private void ezclient$flash(CallbackInfoReturnable<Integer> cir) {
        var module = FeatureModule.get(TimeWeatherModule.class);
        if (module.isEnabled() && module.flag("removeFlash")) cir.setReturnValue(0);
    }
}
