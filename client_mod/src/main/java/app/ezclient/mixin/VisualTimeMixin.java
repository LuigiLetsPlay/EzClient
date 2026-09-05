package app.ezclient.mixin;
import app.ezclient.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnvironmentAttributeSystem.class)
public class VisualTimeMixin {
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void ezclient$time(EnvironmentAttribute<?> attribute, Vec3 position, SpatialAttributeInterpolator interpolator, CallbackInfoReturnable<Object> cir) {
        if (attribute != EnvironmentAttributes.SUN_ANGLE && attribute != EnvironmentAttributes.MOON_ANGLE && attribute != EnvironmentAttributes.STAR_ANGLE
            && attribute != EnvironmentAttributes.STAR_BRIGHTNESS && attribute != EnvironmentAttributes.SKY_LIGHT_FACTOR
            && attribute != EnvironmentAttributes.SKY_COLOR && attribute != EnvironmentAttributes.SUNRISE_SUNSET_COLOR) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.environmentAttributes() != (Object)this) return;
        var module = FeatureModule.get(TimeWeatherModule.class);
        if (!module.customTime()) return;
        double fraction = (module.visualTicks() / 24000.0 + .75) % 1;
        double angle = fraction + (1 - (Math.cos(fraction * Math.PI) + 1) / 2 - fraction) / 3;
        float daylight = (float)Math.max(0, Math.min(1, Math.cos(angle * Math.PI * 2) * 2 + .5));
        if (attribute == EnvironmentAttributes.SUN_ANGLE || attribute == EnvironmentAttributes.MOON_ANGLE || attribute == EnvironmentAttributes.STAR_ANGLE) cir.setReturnValue((float)(angle * 360));
        else if (attribute == EnvironmentAttributes.STAR_BRIGHTNESS) cir.setReturnValue((1 - daylight) * .5f);
        else if (attribute == EnvironmentAttributes.SKY_LIGHT_FACTOR) cir.setReturnValue(daylight);
        else if (attribute == EnvironmentAttributes.SKY_COLOR) cir.setReturnValue(HudModule.interpolateColor(0xff080b18, 0xff78a7ff, daylight));
        else if (attribute == EnvironmentAttributes.SUNRISE_SUNSET_COLOR) cir.setReturnValue(daylight > .05 && daylight < .95 ? 0x88ff8844 : 0);
    }
}
