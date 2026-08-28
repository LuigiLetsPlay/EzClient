package app.ezclient.mixin;

import app.ezclient.performance.visibility.EzVisibilityEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.2 equivalent of the former WorldRenderer.renderEntity HEAD hook. */
@Mixin(LevelExtractor.class)
public abstract class LevelVisibilityMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void ezclient$beginOcclusionFrame(DeltaTracker deltaTracker, Camera camera, float tickDelta,
                                               CallbackInfo ci) {
        EzVisibilityEngine.INSTANCE.beginFrame(camera.position());
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void ezclient$finishOcclusionFrame(DeltaTracker deltaTracker, Camera camera, float tickDelta,
                                                CallbackInfo ci) {
        EzVisibilityEngine.INSTANCE.endFrame();
    }

    @Inject(method = "isEntityVisible", at = @At("RETURN"), cancellable = true)
    private void ezclient$cullBeforeStateExtraction(Entity entity, Frustum frustum,
                                                     double cameraX, double cameraY, double cameraZ,
                                                     CallbackInfoReturnable<Boolean> cir) {
        // Vanilla has already performed its AABB frustum and section-visibility-graph checks.
        if (cir.getReturnValueZ() && !EzVisibilityEngine.INSTANCE.shouldRender(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void ezclient$clearOcclusionCache(ClientLevel level, CallbackInfo ci) {
        EzVisibilityEngine.INSTANCE.setLevel(level);
    }
}
