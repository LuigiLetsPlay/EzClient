package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;

/**
 * Ping & Server Info Display HUD Module:
 * Displays player network latency with configurable layout presets, server IP info,
 * online player count, update interval throttling, and latency color alerts.
 */
public final class PingModule extends HudModule {
    public enum DisplayLayout {
        LABEL_MS("Ping: %dms"),
        VALUE_ONLY("%dms"),
        SERVER_AND_PING("%s | %dms");

        private final String template;
        DisplayLayout(String template) { this.template = template; }
        public String getTemplate() { return template; }
    }

    private DisplayLayout displayLayout = DisplayLayout.LABEL_MS;
    private int updateIntervalSeconds = 1; // 1 to 10s
    private boolean pingAlert = true; // Yellow >100ms, Red >200ms
    private boolean showPlayerCount = false;

    private int cachedPing = 0;
    private long lastFetchTime = 0L;

    public PingModule() {
        super("Ping", "HUD", true, 6, 22, "", "");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ping.png");
    }

    public DisplayLayout getDisplayLayout() { return displayLayout; }
    public void setDisplayLayout(DisplayLayout displayLayout) { this.displayLayout = displayLayout; ConfigManager.save(); }

    public int getUpdateIntervalSeconds() { return updateIntervalSeconds; }
    public void setUpdateIntervalSeconds(int updateIntervalSeconds) { this.updateIntervalSeconds = Math.max(1, Math.min(10, updateIntervalSeconds)); ConfigManager.save(); }

    public boolean isPingAlert() { return pingAlert; }
    public void setPingAlert(boolean pingAlert) { this.pingAlert = pingAlert; ConfigManager.save(); }

    public boolean isShowPlayerCount() { return showPlayerCount; }
    public void setShowPlayerCount(boolean showPlayerCount) { this.showPlayerCount = showPlayerCount; ConfigManager.save(); }

    @Override
    protected String value(Minecraft client) {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime >= (updateIntervalSeconds * 1000L)) {
            lastFetchTime = now;
            if (client.player != null && client.getConnection() != null) {
                PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
                cachedPing = info == null ? 0 : Math.max(0, info.getLatency());
            } else {
                cachedPing = 0;
            }
        }

        String colorCode = "";
        if (pingAlert) {
            if (cachedPing > 200) colorCode = "§c";
            else if (cachedPing > 100) colorCode = "§e";
            else colorCode = "§a";
        }

        String baseText = switch (displayLayout) {
            case LABEL_MS -> "Ping: " + colorCode + cachedPing + "§rms";
            case VALUE_ONLY -> colorCode + cachedPing + "§rms";
            case SERVER_AND_PING -> {
                String serverName = "Singleplayer";
                ServerData server = client.getCurrentServer();
                if (server != null) {
                    serverName = server.name.isEmpty() ? server.ip : server.name;
                }
                yield serverName + " | " + colorCode + cachedPing + "§rms";
            }
        };

        if (showPlayerCount && client.getConnection() != null) {
            int count = client.getConnection().getOnlinePlayers().size();
            baseText += " §7(" + count + ")";
        }

        return baseText;
    }

    @Override
    public String displayText(Minecraft client) {
        return value(client);
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        if (editor) {
            int p = 24;
            String colorCode = pingAlert ? "§a" : "";
            String base = switch (displayLayout) {
                case LABEL_MS -> "Ping: " + colorCode + p + "§rms";
                case VALUE_ONLY -> colorCode + p + "§rms";
                case SERVER_AND_PING -> "EzServer | " + colorCode + p + "§rms";
            };
            if (showPlayerCount) base += " §7(42)";
            return base;
        }
        return displayText(client);
    }
}
