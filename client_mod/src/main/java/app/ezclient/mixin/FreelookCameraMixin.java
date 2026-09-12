package app.ezclient.mixin;

import app.ezclient.gui.FeatureModule;
import app.ezclient.gui.FreelookModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
abstract class FreelookCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
    private void ezclient$freelookRotation(Camera camera, float yaw, float pitch) {
        FreelookModule module = FeatureModule.get(FreelookModule.class);
        if (module.isActive()) {
            setRotation(module.cameraYaw(), module.cameraPitch());
        } else {
            setRotation(yaw, pitch);
        }
    }
}
