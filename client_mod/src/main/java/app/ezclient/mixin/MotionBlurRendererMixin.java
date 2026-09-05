package app.ezclient.mixin;

import app.ezclient.gui.ModuleManager;
import app.ezclient.gui.MotionBlurModule;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MotionBlurRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private CrossFrameResourcePool resourcePool;

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void ezclient$applyMotionBlur(DeltaTracker deltaTracker, CallbackInfo ci) {
        MotionBlurModule motionBlur = ModuleManager.getInstance().getMotionBlurModule();
        //? if >=26.2 {
        boolean cameraMoved = motionBlur != null && motionBlur.shouldRenderMotionBlur(
                ((GameRenderer) (Object) this).mainCamera());
        //?} else {
        /*boolean cameraMoved = motionBlur != null && motionBlur.shouldRenderMotionBlur(
                ((GameRenderer) (Object) this).getMainCamera());
        *///?}
        if (cameraMoved) {
            PostChain chain = minecraft.getShaderManager().getPostChain(
                    motionBlur.getPostChainId(), LevelTargetBundle.MAIN_TARGETS);
            if (chain != null) {
                //? if >=26.2 {
                chain.process(((GameRenderer) (Object) this).mainRenderTarget(), resourcePool);
                //?} else {
                /*chain.process(minecraft.getMainRenderTarget(), resourcePool);
                *///?}
            }
        }
    }
}
