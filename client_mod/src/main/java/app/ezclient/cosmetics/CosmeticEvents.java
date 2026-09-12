package app.ezclient.cosmetics;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import java.net.URI;
import java.net.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/** Server-sent events use their own connection, never the Minecraft tick thread. */
public final class CosmeticEvents {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private static final AtomicLong GENERATION = new AtomicLong();
    private static volatile InputStream stream;
    private CosmeticEvents() {}
    public static void connect() {
        disconnect();
        long generation = GENERATION.get();
        Thread worker = new Thread(() -> listen(generation), "EzClient-CosmeticEvents");
        worker.setDaemon(true);
        worker.start();
    }
    public static void disconnect() {
        GENERATION.incrementAndGet();
        InputStream old = stream;
        stream = null;
        if (old != null) try { old.close(); } catch (IOException ignored) {}
    }
    private static void listen(long generation) {
        while (GENERATION.get() == generation) {
            try {
                var request = HttpRequest.newBuilder(URI.create(CommunityPresence.getApiUrl() + "/events"))
                    .timeout(Duration.ofSeconds(25)).header("Accept", "text/event-stream").GET().build();
                var response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream input = response.body()) {
                    if (response.statusCode() != 200) throw new IOException("Event stream unavailable");
                    if (GENERATION.get() != generation) return;
                    stream = input;
                    var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                    String line;
                    while (GENERATION.get() == generation && (line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ") || line.length() > 4096) continue;
                        String json = line.substring(6);
                        var event = JsonParser.parseString(json).getAsJsonObject();
                        String type = event.get("type").getAsString();
                        if ("presence".equals(type)) CommunityPresence.updateFromPresenceResponse(json, System.currentTimeMillis());
                        else if ("cape".equals(type)) {
                            String playerId = event.has("player_uuid") ? event.get("player_uuid").getAsString() : "";
                            Minecraft.getInstance().execute(() -> {
                                if (GENERATION.get() != generation) return;
                                try {
                                    CommunityCapeManager.refreshPlayer(java.util.UUID.fromString(playerId));
                                } catch (IllegalArgumentException ignored) {
                                    CommunityCapeManager.refreshNow();
                                }
                            });
                        } else if ("resync".equals(type)) {
                            Minecraft.getInstance().execute(() -> {
                                if (GENERATION.get() == generation) CommunityCapeManager.refreshNow();
                            });
                        }
                    }
                }
            } catch (Exception ignored) {
                // Reconnect also requests a complete snapshot, covering missed events.
            }
            if (GENERATION.get() != generation) return;
            try { Thread.sleep(2000); } catch (InterruptedException ex) { return; }
        }
    }
}
