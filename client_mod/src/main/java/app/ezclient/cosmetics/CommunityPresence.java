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

/** Sends one bounded heartbeat; it never scans or uploads a server player list. */
public final class CommunityPresence {
    private static final String API = "http://5.175.192.90:18765/api";
    private static final long INTERVAL_MS = 45_000L;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "EzClient-Presence"); t.setDaemon(true); return t; });
    private static final Map<UUID, Long> ONLINE = new ConcurrentHashMap<>();
    private static volatile long nextHeartbeat;

    private CommunityPresence() {}

    public static void heartbeat(UUID playerId) {
        // Local clients are known immediately, including in singleplayer.
        ONLINE.put(playerId, System.currentTimeMillis());
        long now = System.currentTimeMillis();
        if (now < nextHeartbeat) return;
        nextHeartbeat = now + INTERVAL_MS;
        String body = "{\"player_uuid\":\"" + playerId + "\"}";
        WORKER.execute(() -> {
            try {
                HTTP.send(HttpRequest.newBuilder(URI.create(API + "/presence"))
                        .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) { }
        });
    }

    /** Refreshes only the UUIDs around the local player; it never scans a server. */
    public static void refreshNearby(Collection<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) return;
        String query = String.join(",", playerIds.stream().map(UUID::toString).toList());
        WORKER.execute(() -> {
            try {
                String body = HTTP.send(HttpRequest.newBuilder(URI.create(API + "/presence?players=" + query))
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
