package app.ezclient.mixin;

import app.ezclient.gui.*;
//? if >=26.2 {
import net.minecraft.client.renderer.extract.LevelExtractor;
//?} else {
/*import net.minecraft.client.renderer.LevelRenderer;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.2 {
@Mixin(LevelExtractor.class)
//?} else {
/*@Mixin(LevelRenderer.class)
*///?}
public class BlockOverlayExtractionMixin {
    //? if >=26.2 {
    @Inject(method = "extractGizmos", at = @At("HEAD"))
    //?} else {
    /*@Inject(method = "finalizeGizmoCollection", at = @At("HEAD"))
    *///?}
    private void ezclient$worldVisuals(CallbackInfo ci) {
        WorldVisuals.extract(net.minecraft.client.Minecraft.getInstance());
    }
    @Inject(method = "extractBlockOutline", at = @At("HEAD"), cancellable = true)
    private void ezclient$outline(CallbackInfo ci) {
        if (FeatureModule.get(BlockOverlayModule.class).isEnabled()) ci.cancel();
    }
    @Inject(method = "extractBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
    private void ezclient$cracks(CallbackInfo ci) {
        var module = FeatureModule.get(BlockOverlayModule.class);
        if (module.isEnabled() && !module.text("break").equals("Vanilla")) ci.cancel();
    }
}
