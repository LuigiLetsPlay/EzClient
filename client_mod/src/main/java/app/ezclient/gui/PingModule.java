package app.ezclient.gui;

import net.minecraft.resources.Identifier;

/** Toggle for the compact multiplayer latency HUD indicator. */
public final class PingModule extends HudModule {
    public PingModule() {
        super("Ping", "HUD", true, 6, 22, "Ping: ", " ms");
    }
    @Override
    public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ping.png"); }


    @Override
    protected String value(net.minecraft.client.Minecraft client) {
        if (client.player == null || client.getConnection() == null) return "0";
        var info = client.getConnection().getPlayerInfo(client.player.getUUID());
        return Integer.toString(info == null ? 0 : info.getLatency());
    }
}
