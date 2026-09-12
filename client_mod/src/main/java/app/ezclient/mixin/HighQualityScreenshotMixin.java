package app.ezclient.mixin;

import app.ezclient.gui.FeatureModule;
import app.ezclient.gui.HighQualityScreenshotModule;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screenshot.class)
abstract class HighQualityScreenshotMixin {
    @Inject(method = "grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private static void ezclient$highQuality(File gameDirectory, RenderTarget target, Consumer<Component> callback, CallbackInfo ci) {
        HighQualityScreenshotModule module = FeatureModule.get(HighQualityScreenshotModule.class);
        if (!module.isEnabled()) return;
        module.capture(gameDirectory, target, callback);
        ci.cancel();
    }
}
