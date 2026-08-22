package app.ezclient.gui;

import net.minecraft.client.Minecraft;

/** Combined precision FPS/Ping monitor with a low-FPS warning color. */
public final class PerformanceModule extends HudModule {
    public PerformanceModule() { super("Performance", "HUD", false, 6, 118, "FPS: ", ""); }
    @Override protected String value(Minecraft client) {
        int ping = 0;
        if (client.player != null && client.getConnection() != null) {
            var info = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (info != null) ping = info.getLatency();
        }
        return client.getFps() + " | PING: " + ping + "ms";
    }
}
