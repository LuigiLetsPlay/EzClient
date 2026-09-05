package app.ezclient.mixin;
import app.ezclient.gui.*;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SubtitleOverlay.class)
public class SubtitleCustomizerMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void ezclient$subtitles(CallbackInfo ci) {
        if (FeatureModule.get(SoundEnhancerModule.class).isEnabled()) ci.cancel();
    }
}
