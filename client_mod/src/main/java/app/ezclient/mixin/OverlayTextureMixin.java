package app.ezclient.mixin;

import app.ezclient.gui.DamageTintModule;
import app.ezclient.gui.ModuleManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(OverlayTexture.class)
public class OverlayTextureMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = -1291911168))
    private int ezclient$customHitTint(int original) {
        DamageTintModule mod = ModuleManager.getInstance().getDamageTintModule();
        if (mod != null && mod.isEnabled()) {
            return mod.getTint(null, false);
        }
        return original;
    }
}
