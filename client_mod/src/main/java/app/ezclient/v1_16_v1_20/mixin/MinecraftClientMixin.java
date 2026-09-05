package app.ezclient.v1_16_v1_20.mixin;

import app.ezclient.v1_16_v1_20.EzClientMod_1_16_1_20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
    "net.minecraft.client.MinecraftClient",
    "net.minecraft.client.Minecraft"
})
public class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        EzClientMod_1_16_1_20.onTick();
    }
}
