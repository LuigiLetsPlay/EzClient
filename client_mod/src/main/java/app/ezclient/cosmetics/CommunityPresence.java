package app.ezclient.cosmetics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sends bounded heartbeat even in main menus; never scans or uploads a server player list. */
public final class CommunityPresence {
    private static final String DEFAULT_API = "http://5.175.192.90:18765/api";
    private static final long INTERVAL_MS = 30_000L;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "EzClient-Presence"); t.setDaemon(true); return t; });
    private static final Map<UUID, Long> ONLINE = new ConcurrentHashMap<>();
    private static volatile long nextHeartbeat = 0L;

    private CommunityPresence() {}

    public static String getApiUrl() {
        String env = System.getenv("EZCLIENT_CAPE_API");
        if (env != null && !env.isBlank()) return env.replaceAll("/+$", "");
        String prop = System.getProperty("ezclient.api");
        if (prop != null && !prop.isBlank()) return prop.replaceAll("/+$", "");
        return DEFAULT_API;
    }

    public static void heartbeat(UUID playerId, String username) {
        if (playerId == null) return;
        long now = System.currentTimeMillis();
        if (now < nextHeartbeat) return;
        nextHeartbeat = now + INTERVAL_MS;
        ONLINE.put(playerId, now);

        String name = username != null && !username.isBlank() ? username : "Spieler";
        String body = "{\"player_uuid\":\"" + playerId + "\",\"username\":\"" + name + "\"}";
        String endpoint = getApiUrl() + "/presence";

        WORKER.execute(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "EzClient/1.8.2")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("[EzClient-Presence] Heartbeat error to " + endpoint + ": " + e.getMessage());
            }
        });
    }

    public static void heartbeat(UUID playerId) {
        heartbeat(playerId, "Spieler");
    }

    /** Refreshes only the UUIDs around the local player; it never scans a server. */
    public static void refreshNearby(Collection<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) return;
        String query = String.join(",", playerIds.stream().map(UUID::toString).toList());
        String endpoint = getApiUrl() + "/presence?players=" + query;
        WORKER.execute(() -> {
            try {
                String body = HTTP.send(HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
                long now = System.currentTimeMillis();
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")
                        .matcher(body);
                while (matcher.find()) ONLINE.put(UUID.fromString(matcher.group()), now);
            } catch (Exception ignored) {
                // Presence is decorative and must never affect rendering.
            }
        });
    }

    public static boolean isEzClientPlayer(UUID playerId) {
        if (playerId == null) return false;
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null && playerId.equals(client.player.getUUID())) return true;
        Long seen = ONLINE.get(playerId);
        return seen != null && System.currentTimeMillis() - seen < 95_000L;
    }
}
