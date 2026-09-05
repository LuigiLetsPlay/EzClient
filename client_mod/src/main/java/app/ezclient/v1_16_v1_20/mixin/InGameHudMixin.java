package app.ezclient.v1_16_v1_20.mixin;

import app.ezclient.v1_16_v1_20.HudRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    //? if <=1.19.4 {
    /*@Inject(method = "render", at = @At("TAIL"))
    private void onRender(net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        HudRenderer.render(tickDelta);
    }
    *///?} else if <1.21 {
    /*@Inject(method = "render", at = @At("TAIL"))
    private void onRender(net.minecraft.client.gui.DrawContext context, float tickDelta, CallbackInfo ci) {
        HudRenderer.render(tickDelta);
    }
    *///?} else {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(net.minecraft.client.gui.DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
        HudRenderer.render(0.0F);
    }
    //?}
}
