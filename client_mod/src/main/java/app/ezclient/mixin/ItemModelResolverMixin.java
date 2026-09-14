package app.ezclient.mixin;

import app.ezclient.gui.ItemModelRenderState;
import app.ezclient.gui.ItemModelModule;
import app.ezclient.gui.FeatureModule;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ItemOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {
    @Inject(method = "updateForTopItem", at = @At("TAIL"))
    private void ezclient$rememberItem(ItemStackRenderState state, ItemStack stack,
            ItemDisplayContext context, Level level, ItemOwner owner, int seed, CallbackInfo ci) {
        String id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ((ItemModelRenderState) state).ezclient$setItemId(id);
        // GUI items are cached by model identity. Include the live transform so slider edits
        // invalidate the atlas slot rather than reusing an old rendered icon.
        if (context == ItemDisplayContext.GUI && id != null
                && state instanceof TrackingItemStackRenderState tracked) {
            ItemModelModule.Transform transform = FeatureModule.get(ItemModelModule.class)
                    .renderingTransform(id, context);
            if (!transform.isIdentity()) tracked.appendModelIdentityElement(transform);
        }
    }
}
