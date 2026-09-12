package app.ezclient.cosmetics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

/**
 * Robust, non-blocking detector for third-party clients (NoRisk, LabyMod) and external cape loader.
 * Uses direct upstream CDN endpoints, enforces negative-result caching, integrates a circuit breaker
 * for unstable hosts, and respects visual distance gating to eliminate network lag and ping spikes.
 */
public final class ThirdPartyPresence {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1500)).build();
    private static final ConcurrentHashMap<UUID, CachedResult> CACHE = new ConcurrentHashMap<>();

    private static final long CACHE_DURATION_MS = 5 * 60_000L;
    private static final long NEGATIVE_CACHE_MS = 2 * 60_000L;
    private static final long ERROR_RETRY_MS = 30_000L;
    private static final String NORISK_CAPE_API = "https://api.norisk.gg/api/v1/cosmetics/user/";
    private static final String NORISK_CAPE_CDN = "https://cdn.norisk.gg/capes/prod/";
    private static final double MAX_CAPE_FETCH_DIST_SQ = 96.0 * 96.0;

    // Circuit breaker state per host
    private static final ConcurrentHashMap<String, Long> HOST_BACKOFF = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> HOST_FAILURES = new ConcurrentHashMap<>();
    private static final long CIRCUIT_BREAKER_TRIP_MS = 60_000L;

    public record QueuedTarget(UUID playerId, String username, boolean highPriority, double distanceSq, long queuedAt)
            implements Comparable<QueuedTarget> {
        @Override
        public int compareTo(QueuedTarget o) {
            if (this.highPriority != o.highPriority) {
                return this.highPriority ? -1 : 1;
            }
            if (this.highPriority) {
                return Double.compare(this.distanceSq, o.distanceSq);
            }
            return Long.compare(this.queuedAt, o.queuedAt);
        }
    }

    private static final CosmeticWorkQueue<UUID, QueuedTarget> QUEUE =
            new CosmeticWorkQueue<>(64, QueuedTarget::playerId);

    static {
        Thread worker = new Thread(ThirdPartyPresence::workerLoop, "EzClient-CosmeticsWorker");
        worker.setDaemon(true);
        worker.start();
    }

    private ThirdPartyPresence() {}

    public static boolean isCached(UUID playerId) {
        if (playerId == null) return true;
        CachedResult cached = CACHE.get(playerId);
        return cached != null && (System.currentTimeMillis() - cached.timestamp < cached.durationMs);
    }

    public static void clearPending() {
        QUEUE.clearQueued();
    }

    static void evictCape(UUID id) { CACHE.remove(id); }

    public static CommunityPresence.ClientType getOrQueryClient(UUID playerId) {
        return getOrQueryClient(playerId, resolveUsername(playerId), false, Double.MAX_VALUE);
    }

    public static CommunityPresence.ClientType getOrQueryClient(UUID playerId, String username) {
        return getOrQueryClient(playerId, username, false, Double.MAX_VALUE);
    }

    public static CommunityPresence.ClientType getOrQueryClient(UUID playerId, String username, boolean highPriority, double distanceSq) {
        if (playerId == null) return CommunityPresence.ClientType.NONE;
        
        CachedResult cached = CACHE.get(playerId);
        if (cached != null) {
            if (System.currentTimeMillis() - cached.timestamp < cached.durationMs) {
                return cached.type;
            }
        }

        enqueue(playerId, username, highPriority, distanceSq);
        return cached != null ? cached.type : CommunityPresence.ClientType.NONE;
    }

    public static void enqueue(UUID playerId, String username, boolean highPriority, double distanceSq) {
        if (playerId == null) return;
        if (QUEUE.isPending(playerId)) return;
        long now = System.currentTimeMillis();
        CachedResult cached = CACHE.get(playerId);
        // Fix: Negative cache hits must return immediately!
        if (cached != null && (now - cached.timestamp < cached.durationMs)) {
            return;
        }

        String name = (username != null && !username.isBlank()) ? username : resolveUsername(playerId);
        QUEUE.offer(new QueuedTarget(playerId, name, highPriority, distanceSq, now));
    }

    private static void workerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            QueuedTarget target = null;
            try {
                target = QUEUE.take();
                CachedResult cached = CACHE.get(target.playerId());
                if (cached == null || System.currentTimeMillis() - cached.timestamp >= cached.durationMs) {
                    queryApis(target.playerId(), target.username(), target.distanceSq());
                    // Each player can require several requests; pace the entire lookup.
                    Thread.sleep(750L);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            } finally {
                if (target != null) QUEUE.complete(target);
            }
        }
    }

    private static String resolveUsername(UUID playerId) {
        try {
            var mc = Minecraft.getInstance();
            if (mc.player != null && playerId.equals(mc.player.getUUID())) {
                return mc.player.getScoreboardName();
            }
            var conn = mc.getConnection();
            if (conn != null) {
                var info = conn.getPlayerInfo(playerId);
                if (info != null && info.getProfile() != null && info.getProfile().name() != null) {
                    return info.getProfile().name();
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void queryApis(UUID playerId, String username, double distanceSq) {
        String uuidDashed = playerId.toString();
        String uuidNoDashes = uuidDashed.replace("-", "");
        boolean capeLoaded = false;
        CommunityPresence.ClientType detectedType = CommunityPresence.ClientType.NONE;

        // Capes should only be downloaded for players within render/visual range or local player
        boolean shouldFetchCapes = distanceSq <= MAX_CAPE_FETCH_DIST_SQ;

        // 1. NoRisk's public cape endpoint returns a hash, not JSON or an image URL.
        // HTTP 200 also confirms a NoRisk account when no cape is selected.
        NoRiskCape noRisk = fetchNoRiskCape(uuidDashed);
        if (noRisk.userFound()) {
            detectedType = CommunityPresence.ClientType.NORISK;
            if (shouldFetchCapes && noRisk.capeHash() != null) {
                byte[] cape = fetchImage(NORISK_CAPE_CDN + noRisk.capeHash() + ".png");
                if (cape != null) {
                    CommunityCapeManager.installExternalCape(playerId, cape);
                    capeLoaded = true;
                }
            }
        }

        // 2. LabyMod cape fallback, only when NoRisk has no selected/available cape.
        if (shouldFetchCapes && !capeLoaded) {
            byte[] labyCape = fetchImage("https://dl.labymod.net/capes/" + uuidDashed);
            if (labyCape != null) {
                if (detectedType == CommunityPresence.ClientType.NONE) detectedType = CommunityPresence.ClientType.LABYMOD;
                CommunityCapeManager.installExternalCape(playerId, labyCape);
                capeLoaded = true;
            }
        }

        // 3. Fallback Capes: OptiFine and MinecraftCapes direct upstream (only if nearby)
        if (shouldFetchCapes && !capeLoaded) {
            // OptiFine (direct from s.optifine.net, ~300ms)
            if (username != null && !username.isBlank()) {
                byte[] optifine = fetchImage("http://s.optifine.net/capes/" + username + ".png");
                if (optifine != null) {
                    CommunityCapeManager.installExternalCape(playerId, optifine);
                    capeLoaded = true;
                }
            }
            // MinecraftCapes (direct JSON API, ~60ms)
            if (!capeLoaded) {
                JsonObject mcCapes = fetchEndpoint("https://api.minecraftcapes.net/profile/" + uuidNoDashes);
                if (mcCapes != null && mcCapes.has("cape_url") && !mcCapes.get("cape_url").isJsonNull()) {
                    String capeUrl = mcCapes.get("cape_url").getAsString();
                    if (!capeUrl.isBlank()) {
                        byte[] capeBytes = fetchImage(capeUrl);
                        if (capeBytes != null) {
                            CommunityCapeManager.installExternalCape(playerId, capeBytes);
                            capeLoaded = true;
                        }
                    }
                }
            }
        }

        // Confirmed misses are short-lived; a failed upstream request must be retried soon.
        long duration = detectedType != CommunityPresence.ClientType.NONE ? CACHE_DURATION_MS
                : noRisk.unavailable() ? ERROR_RETRY_MS : NEGATIVE_CACHE_MS;
        if (noRisk.capeHash() != null && !capeLoaded) duration = ERROR_RETRY_MS;
        if (CACHE.size() >= 2048) {
            long cutoff = System.currentTimeMillis() - CACHE_DURATION_MS;
            CACHE.entrySet().removeIf(entry -> entry.getValue().timestamp < cutoff);
            if (CACHE.size() >= 2048) CACHE.remove(CACHE.keys().nextElement());
        }
        CACHE.put(playerId, new CachedResult(detectedType, System.currentTimeMillis(), duration));
    }

    private static NoRiskCape fetchNoRiskCape(String uuid) {
        String url = NORISK_CAPE_API + uuid + "/cape";
        if (!isHostAvailable(url)) return NoRiskCape.failed();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, CosmeticHttp.text());
            if (resp.statusCode() == 404) {
                recordHostSuccess(url);
                return new NoRiskCape(false, false, null);
            }
            if (resp.statusCode() == 200) {
                recordHostSuccess(url);
                String hash = resp.body().trim();
                return new NoRiskCape(true, false,
                        hash.matches("[a-fA-F0-9]{32,64}") ? hash : null);
            }
            if (resp.statusCode() == 429 || resp.statusCode() >= 500) recordHostFailure(url);
        } catch (Exception e) {
            recordHostFailure(url);
        }
        return NoRiskCape.failed();
    }

    private record NoRiskCape(boolean userFound, boolean unavailable, String capeHash) {
        private static NoRiskCape failed() { return new NoRiskCape(false, true, null); }
    }

    private static boolean isHostAvailable(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return true;
            Long backoff = HOST_BACKOFF.get(host);
            if (backoff != null) {
                if (System.currentTimeMillis() < backoff) {
                    return false; // Circuit is OPEN
                }
                HOST_BACKOFF.remove(host); // Half-open / retry
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void recordHostFailure(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return;
            AtomicInteger failures = HOST_FAILURES.computeIfAbsent(host, h -> new AtomicInteger(0));
            if (failures.incrementAndGet() >= 2) {
                HOST_BACKOFF.put(host, System.currentTimeMillis() + CIRCUIT_BREAKER_TRIP_MS);
            }
        } catch (Exception ignored) {}
    }

    private static void recordHostSuccess(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return;
            HOST_FAILURES.remove(host);
            HOST_BACKOFF.remove(host);
        } catch (Exception ignored) {}
    }

    private static JsonObject fetchEndpoint(String url) {
        if (!isHostAvailable(url)) return null;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(Duration.ofMillis(1500))
                    .GET()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, CosmeticHttp.text());
            if (resp.statusCode() == 429) {
                recordHostFailure(url);
                return null;
            }
            if (resp.statusCode() == 200) {
                recordHostSuccess(url);
                String body = resp.body().trim();
                if (body.startsWith("{")) {
                    return JsonParser.parseString(body).getAsJsonObject();
                }
            } else if (resp.statusCode() >= 500) {
                recordHostFailure(url);
            }
            return null;
        } catch (Exception e) {
            recordHostFailure(url);
            return null;
        }
    }

    private static byte[] fetchImage(String url) {
        if (!isHostAvailable(url)) return null;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url.replace("\\/", "/")))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(Duration.ofMillis(2000))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = HTTP.send(req, CosmeticHttp.bytes(8 * 1024 * 1024));
            if (resp.statusCode() == 429) {
                recordHostFailure(url);
                return null;
            }
            if (resp.statusCode() == 200) {
                byte[] b = resp.body();
                // Minecraft cape dimensions or minimum valid PNG size check
                if (b != null && b.length >= 8 && b.length <= 2 * 1024 * 1024) {
                    recordHostSuccess(url);
                    return b;
                }
            } else if (resp.statusCode() >= 500) {
                recordHostFailure(url);
            }
        } catch (Exception e) {
            recordHostFailure(url);
        }
        return null;
    }

    private static class CachedResult {
        final CommunityPresence.ClientType type;
        final long timestamp;
        final long durationMs;
        
        CachedResult(CommunityPresence.ClientType type, long timestamp, long durationMs) {
            this.type = type;
            this.timestamp = timestamp;
            this.durationMs = durationMs;
        }
    }
}
