package app.ezclient.gui;

/** Toggle for the compact multiplayer latency HUD indicator. */
public final class PingModule extends HudModule {
    public PingModule() {
        super("Ping", "Utils", true, 6, 22, "Ping: ", " ms");
    }
    @Override protected String value(net.minecraft.client.Minecraft client) {
        if (client.player == null || client.getConnection() == null) return "0";
        var info = client.getConnection().getPlayerInfo(client.player.getUUID());
        return Integer.toString(info == null ? 0 : info.getLatency());
    }
}
