package app.ezclient.mixin;

import app.ezclient.gui.FeatureModule;
import app.ezclient.gui.ItemModelModule;
import app.ezclient.gui.ItemModelRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
public class ItemModelRenderStateMixin implements ItemModelRenderState {
    @Shadow ItemDisplayContext displayContext;
    @Unique private String ezclient$itemId;
    @Unique private boolean ezclient$posePushed;

    @Override public void ezclient$setItemId(String id) { ezclient$itemId = id; }

    @Inject(method = "clear", at = @At("HEAD"))
    private void ezclient$clearItemId(CallbackInfo ci) { ezclient$itemId = null; }

    @Inject(method = "submit", at = @At("HEAD"))
    private void ezclient$transform(PoseStack pose, SubmitNodeCollector collector,
            int light, int overlay, int outline, CallbackInfo ci) {
        ItemModelModule.Transform transform = FeatureModule.get(ItemModelModule.class)
                .renderingTransform(ezclient$itemId, displayContext);
        ezclient$posePushed = !transform.isIdentity();
        if (ezclient$posePushed) {
            pose.pushPose();
            transform.apply(pose);
        }
    }

    @Inject(method = "submit", at = @At("TAIL"))
    private void ezclient$restore(PoseStack pose, SubmitNodeCollector collector,
            int light, int overlay, int outline, CallbackInfo ci) {
        if (ezclient$posePushed) {
            pose.popPose();
            ezclient$posePushed = false;
        }
    }
}
