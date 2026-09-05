package app.ezclient.detector;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side detector. Vanilla/unknown players are deliberately not reported.
 * Detection uses the brand and channels sent to the server plus NoRisk's official
 * server API when that plugin is installed.
 */
public final class EzClientDetectorPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, ClientType> detected = new ConcurrentHashMap<>();
    private HttpClient http;
    private String endpoint;
    private String token;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        endpoint = getConfig().getString("presence-api", "http://127.0.0.1:18765/api/presence");
        token = getConfig().getString("detector-token", "");
        if (token.isBlank() || token.equals("CHANGE_ME")) {
            getLogger().severe("Set detector-token in config.yml; detector is disabled until it is configured.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
        Bukkit.getPluginManager().registerEvents(this, this);
        long period = Math.max(10, getConfig().getLong("refresh-seconds", 30)) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, this::refreshAll, 20L, period);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> inspect(event.getPlayer()), 40L);
    }

    @EventHandler
    public void onChannel(PlayerRegisterChannelEvent event) {
        merge(event.getPlayer(), ClientType.fromSignal(event.getChannel()));
    }

    private void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) inspect(player);
    }

    private void inspect(Player player) {
        ClientType type = detectNoRisk(player.getUniqueId());
        if (type == ClientType.UNKNOWN) {
            type = ClientType.fromSignal(player.getClientBrandName());
        }
        for (String channel : player.getListeningPluginChannels()) {
            type = ClientType.prefer(type, ClientType.fromSignal(channel));
        }
        merge(player, type);
        ClientType current = detected.get(player.getUniqueId());
        if (current != null) report(player, current);
    }

    private ClientType detectNoRisk(UUID playerId) {
        try {
            Class<?> paper = Class.forName("gg.norisk.paper.Paper");
            Object core = paper.getMethod("getCoreApi").invoke(null);
            if (core != null && Boolean.TRUE.equals(core.getClass().getMethod("isNrcPlayer", UUID.class).invoke(core, playerId))) {
                return ClientType.NORISK;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // The optional official NoRisk server API is not installed.
        }
        return ClientType.UNKNOWN;
    }

    private void merge(Player player, ClientType candidate) {
        if (candidate == ClientType.UNKNOWN) return;
        ClientType chosen = detected.merge(player.getUniqueId(), candidate, ClientType::prefer);
        report(player, chosen);
    }

    private void report(Player player, ClientType type) {
        String safeName = player.getName().replace("\\", "").replace("\"", "");
        String body = "{\"player_uuid\":\"" + player.getUniqueId() + "\",\"username\":\""
                + safeName + "\",\"client\":\"" + type.wire + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("X-EzClient-Detector-Token", token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        http.sendAsync(request, HttpResponse.BodyHandlers.discarding()).exceptionally(error -> {
            getLogger().fine("Presence report failed: " + error.getMessage());
            return null;
        });
    }

    enum ClientType {
        UNKNOWN("", 0), BADLION("badlion", 1), LUNAR("lunar", 2), LABYMOD("labymod", 3), NORISK("norisk", 4);
        final String wire;
        final int priority;
        ClientType(String wire, int priority) { this.wire = wire; this.priority = priority; }

        static ClientType prefer(ClientType left, ClientType right) {
            return left.priority >= right.priority ? left : right;
        }

        static ClientType fromSignal(String raw) {
            String value = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
            if (value.contains("norisk") || value.startsWith("nrc:")) return NORISK;
            if (value.contains("labymod") || value.startsWith("lmc")) return LABYMOD;
            if (value.contains("lunar") || value.startsWith("apollo:")) return LUNAR;
            if (value.contains("badlion") || value.startsWith("blc:")) return BADLION;
            return UNKNOWN;
        }
    }
}
