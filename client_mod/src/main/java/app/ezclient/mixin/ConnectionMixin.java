package app.ezclient.mixin;

import io.netty.channel.Channel;

import net.minecraft.network.Connection;
import net.minecraft.network.UnconfiguredPipelineHandler;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guards against the vanilla race where a keep-alive response is written while
 * the outbound pipeline is still unconfigured (e.g. during a configuration ->
 * play protocol switch). Sending in that state throws EncoderException:
 * "Pipeline has no outbound protocol configured" and disconnects the player.
 *
 * Keep-alives are safe to drop; the server simply resends them.
 */
@Mixin(Connection.class)
abstract class ConnectionMixin {
    @Shadow private Channel channel;

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void ezclient$dropKeepAliveDuringProtocolSwitch(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ServerboundKeepAlivePacket)) return;
        if (channel == null || !channel.isActive()) return;

        var outbound = channel.pipeline().get("encoder");
        if (outbound instanceof UnconfiguredPipelineHandler.Outbound) {
            // Outbound protocol not ready yet -> drop this keep-alive response.
            ci.cancel();
        }
    }
}
