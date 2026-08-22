package app.ezclient.gui;

import net.minecraft.client.Minecraft;

/** Compact server address and latency widget; server favicon rendering is handled by the HUD renderer. */
public final class ServerInfoModule extends HudModule {
    public ServerInfoModule() { super("Server Info", "HUD", false, 6, 54, "Server: ", ""); }
    @Override protected String value(Minecraft client) {
        if (client.getCurrentServer() == null) return "Singleplayer";
        String address = client.getCurrentServer().ip;
        if (client.player != null && client.getConnection() != null) {
            var info = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (info != null) address += " (" + info.getLatency() + "ms)";
        }
        return address;
    }
}
