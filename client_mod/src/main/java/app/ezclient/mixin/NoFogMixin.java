package app.ezclient.mixin;

import app.ezclient.gui.FeatureModule;
import app.ezclient.gui.NoFogModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
abstract class NoFogMixin {
    @ModifyReturnValue(method = "setupFog", at = @At("RETURN"))
    private FogData ezclient$removeFog(FogData fog, Camera camera, int renderDistance, DeltaTracker delta, float darken, ClientLevel level) {
        NoFogModule module = FeatureModule.get(NoFogModule.class);
        if (!module.isEnabled()) return fog;
        FogType fluid = camera.getFluidInCamera();
        boolean all = module.text("mode").equals("All fog");
        boolean environmentAllowed = fluid == FogType.NONE || fluid == FogType.ATMOSPHERIC
                || (fluid == FogType.WATER && module.flag("water"))
                || (fluid == FogType.LAVA && module.flag("lava"))
                || (fluid == FogType.POWDER_SNOW && module.flag("powderSnow"));
        float end = Math.max(renderDistance * 16.0f, 16.0f) * (float)module.number("endMultiplier");
        fog.renderDistanceStart = end * (float)(module.number("start") / 100.0);
        fog.renderDistanceEnd = end;
        fog.skyEnd = Math.max(fog.skyEnd, end);
        fog.cloudEnd = Math.max(fog.cloudEnd, end);
        if (all && environmentAllowed) {
            fog.environmentalStart = end;
            fog.environmentalEnd = end;
        }
        return fog;
    }
}
