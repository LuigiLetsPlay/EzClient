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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sends bounded heartbeat even in main menus; never scans or uploads a server player list. */
public final class CommunityPresence {
    private static final String DEFAULT_API = "http://5.175.192.90:18765/api";
    private static final long INTERVAL_MS = 30_000L;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "EzClient-Presence"); t.setDaemon(true); return t; });
    private static final Map<UUID, SeenClient> ONLINE = new ConcurrentHashMap<>();
    private static final Pattern PRESENCE_ENTRY = Pattern.compile(
            "\\{[^{}]*\\\"uuid\\\"\\s*:\\s*\\\"([a-fA-F0-9-]{36})\\\"[^{}]*"
                    + "\\\"client\\\"\\s*:\\s*\\\"([a-zA-Z0-9_-]{1,24})\\\"[^{}]*}");
    private static final Pattern UUID_ONLY = Pattern.compile(
            "\\\"([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})\\\"");
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
        ONLINE.put(playerId, new SeenClient(ClientType.EZCLIENT, now));

        String name = username != null && !username.isBlank() ? username : "Spieler";
        String body = "{\"player_uuid\":\"" + playerId + "\",\"username\":\"" + name
                + "\",\"client\":\"ezclient\"}";
        String endpoint = getApiUrl() + "/presence";

        WORKER.execute(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "EzClient/2.0.1")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HTTP.send(req, CosmeticHttp.text());
            } catch (Exception ignored) {
                // Heartbeat is decorative and must not fail loudly if presence server is unreachable
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
                        .timeout(Duration.ofSeconds(5)).GET().build(), CosmeticHttp.text()).body();
                updateFromPresenceResponse(body, System.currentTimeMillis());
            } catch (Exception ignored) {
                // Presence is decorative and must never affect rendering.
            }
        });
    }

    static void updateFromPresenceResponse(String body, long now) {
        if (body == null || body.length() > 1_000_000) return;
        Matcher entries = PRESENCE_ENTRY.matcher(body);
        boolean structured = false;
        while (entries.find()) {
            ClientType type = ClientType.fromWire(entries.group(2));
            if (type == ClientType.NONE) continue;
            ONLINE.put(UUID.fromString(entries.group(1)), new SeenClient(type, now));
            structured = true;
        }
        // Compatibility with presence servers from before the client field existed.
        if (!structured) {
            Matcher uuids = UUID_ONLY.matcher(body);
            while (uuids.find()) {
                ONLINE.put(UUID.fromString(uuids.group(1)), new SeenClient(ClientType.EZCLIENT, now));
            }
        }
    }

    public static ClientType clientForPlayer(UUID playerId) {
        return clientForPlayer(playerId, null);
    }

    public static void clearOnline() {
        ONLINE.clear();
    }

    public static ClientType clientForPlayer(UUID playerId, String username) {
        if (playerId == null) return ClientType.NONE;
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null && playerId.equals(client.player.getUUID())) return ClientType.EZCLIENT;
        SeenClient seen = ONLINE.get(playerId);
        if (seen != null) {
            if (System.currentTimeMillis() - seen.timestamp() < 95_000L) {
                return seen.type();
            }
            ONLINE.remove(playerId, seen);
        }
        return ThirdPartyPresence.getOrQueryClient(playerId, username, false, Double.MAX_VALUE);
    }

    public static ClientType clientForPlayerNearby(UUID playerId, String username, double distanceSq) {
        if (playerId == null) return ClientType.NONE;
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null && playerId.equals(client.player.getUUID())) return ClientType.EZCLIENT;
        SeenClient seen = ONLINE.get(playerId);
        if (seen != null) {
            if (System.currentTimeMillis() - seen.timestamp() < 95_000L) {
                return seen.type();
            }
            ONLINE.remove(playerId, seen);
        }
        return ThirdPartyPresence.getOrQueryClient(playerId, username, true, distanceSq);
    }

    public static boolean isEzClientPlayer(UUID playerId) {
        return clientForPlayer(playerId) == ClientType.EZCLIENT;
    }

    public enum ClientType {
        NONE("", '\0'),
        EZCLIENT("ezclient", '\uE000'),
        NORISK("norisk", '\uE001'),
        LABYMOD("labymod", '\uE002'),
        LUNAR("lunar", '\uE003'),
        BADLION("badlion", '\uE004');

        private final String wireName;
        private final char glyph;

        ClientType(String wireName, char glyph) {
            this.wireName = wireName;
            this.glyph = glyph;
        }

        public char glyph() { return glyph; }
        public String wireName() { return wireName; }

        static ClientType fromWire(String value) {
            if (value == null) return NONE;
            for (ClientType type : values()) {
                if (type.wireName.equalsIgnoreCase(value)) return type;
            }
            return NONE;
        }
    }

    private record SeenClient(ClientType type, long timestamp) {}
}
