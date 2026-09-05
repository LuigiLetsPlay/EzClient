package app.ezclient.mixin;
import app.ezclient.gui.*;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SoundEngine.class)
public class SoundVolumeMixin {
    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void ezclient$volume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Math.min(1f, cir.getReturnValue() * FeatureModule.get(SoundEnhancerModule.class).volume(sound.getIdentifier().toString())));
    }
}
