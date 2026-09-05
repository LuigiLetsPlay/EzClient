package app.ezclient.mixin;

import app.ezclient.gui.ComboCounterModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleHurtAnimation", at = @At("HEAD"))
    private void ezclient$onHurtAnimation(ClientboundHurtAnimationPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && packet.id() == client.player.getId()) {
            ComboCounterModule.onPlayerHurt();
        }
    }
}
