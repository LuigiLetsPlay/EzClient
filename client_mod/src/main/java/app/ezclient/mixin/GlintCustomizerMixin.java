package app.ezclient.mixin;

import app.ezclient.gui.FeatureModule;
import app.ezclient.gui.GlintCustomizerModule;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFeatureRenderer.class)
abstract class GlintCustomizerMixin {
    @Shadow @Final private QuadInstance quadInstance;

    @Inject(method = "prepareFoilSubmit", at = @At("HEAD"), require = 0)
    private void ezclient$colorGlint26_2(CallbackInfo ci) {
        GlintCustomizerModule module = FeatureModule.get(GlintCustomizerModule.class);
        if (module.isEnabled() && module.flag("items")) quadInstance.setColor(module.glintColor());
    }

    @Inject(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), require = 0)
    private void ezclient$colorGlint26_1(CallbackInfo ci) {
        GlintCustomizerModule module = FeatureModule.get(GlintCustomizerModule.class);
        if (module.isEnabled() && module.flag("items")) quadInstance.setColor(module.glintColor());
    }
}
