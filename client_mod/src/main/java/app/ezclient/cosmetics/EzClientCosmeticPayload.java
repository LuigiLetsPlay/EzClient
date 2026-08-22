package app.ezclient.cosmetics;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Small, server-relayed presence payload. Cape bytes are deliberately not sent here. */
public record EzClientCosmeticPayload(String playerId, String capeDigest) implements CustomPacketPayload {
    public static final Type<EzClientCosmeticPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("ezclient", "cosmetics"));
    public static final StreamCodec<ByteBuf, EzClientCosmeticPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EzClientCosmeticPayload::playerId,
            ByteBufCodecs.STRING_UTF8, EzClientCosmeticPayload::capeDigest,
            EzClientCosmeticPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
