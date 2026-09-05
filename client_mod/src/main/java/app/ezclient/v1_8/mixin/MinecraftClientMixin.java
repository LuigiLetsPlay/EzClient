package app.ezclient.v1_8.mixin;

import app.ezclient.v1_8.EzClientMod_1_8;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        EzClientMod_1_8.onTick();
    }
}
