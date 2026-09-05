package app.ezclient.mixin;

import app.ezclient.gui.*;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemPhysicsMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void ezclient$extract(ItemEntity item, ItemEntityRenderState state, float delta, CallbackInfo ci) {
        var module = FeatureModule.get(ItemPhysicsModule.class);
        var motion = item.getDeltaMovement();
        boolean flat = module.text("mode").equals("Flat") && item.onGround();
        float speed = (float)module.number("speed");
        float yaw = flat ? item.getId() * 137.5f : (state.ageInTicks * 6 * speed + item.getId() * 37) % 360;
        float pitch = flat ? 90 : module.flag("physics") ? (float)Math.toDegrees(Math.atan2(motion.y, Math.hypot(motion.x, motion.z))) + state.ageInTicks * 9 * speed : 0;
        var bounds = state.item.getModelBoundingBox();
        // After the X rotation, model Z becomes vertical. Keep even 3D block items above the floor.
        float lift = flat ? (float)Math.max(.025, bounds.maxZ + .015) : .15f;
        ((ItemPhysicsState)state).ezclient$physics(module.active(), pitch, yaw, lift);
    }
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void ezclient$submit(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        ItemPhysicsState physics = (ItemPhysicsState)state;
        if (!physics.ezclient$active() || state.item.isEmpty()) return;
        pose.pushPose();
        try {
            pose.translate(0, physics.ezclient$lift(), 0);
            pose.mulPose(Axis.YP.rotationDegrees(physics.ezclient$yaw()));
            pose.mulPose(Axis.XP.rotationDegrees(physics.ezclient$pitch()));
            ItemEntityRenderer.submitMultipleFromCount(pose, collector, state.lightCoords, state, RandomSource.create(state.seed));
        } finally { pose.popPose(); }
        ci.cancel();
    }
}
