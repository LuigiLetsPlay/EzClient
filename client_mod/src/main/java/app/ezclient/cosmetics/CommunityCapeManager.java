package app.ezclient.cosmetics;

import app.ezclient.EzClientMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Bounded, client-only community cape cache. Never blocks rendering or sends cape bytes through a game server. */
public final class CommunityCapeManager {
    private static final String API = "http://5.175.192.90:18765/api";
    private static final long REFRESH_MS = 30_000L;
    private static final double RANGE_SQ = 96 * 96;
    private static final Map<UUID, Identifier> CAPES = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "EzClient-Capes"); t.setDaemon(true); return t; });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private static final Pattern CAPE = Pattern.compile("\\\"owner_uuid\\\"\\s*:\\s*\\\"([a-fA-F0-9-]{36})\\\"[^}]*?\\\"image_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static volatile long nextRefresh;

    private CommunityCapeManager() {}
    public static Identifier cape(UUID player) { return CAPES.get(player); }
    public static PlayerSkin replaceCape(PlayerSkin original, UUID player) {
        Identifier texture = cape(player);
        if (texture == null) return original;
        ClientAsset.Texture asset = new ClientAsset.Texture() {
            @Override public Identifier id() { return texture; }
            @Override public Identifier texturePath() { return texture; }
        };
        return new PlayerSkin(original.body(), asset, original.elytra(), original.model(), original.secure());
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        loadLocal(client.player.getUUID());
        long now = System.currentTimeMillis();
        if (now < nextRefresh) return;
        nextRefresh = now + REFRESH_MS;
        List<UUID> nearby = new ArrayList<>();
        client.level.players().forEach(p -> { if (p.distanceToSqr(client.player) <= RANGE_SQ) nearby.add(p.getUUID()); });
        if (!nearby.isEmpty()) {
            refreshNearby(nearby);
            CommunityPresence.refreshNearby(nearby);
        }
    }

    private static void loadLocal(UUID uuid) {
        if (CAPES.containsKey(uuid) || !LOADING.add(uuid)) return;
        Path file = EzClientMod.getEzClientDataDir().resolve("cosmetics").resolve("active_cape.png");
        if (!Files.isRegularFile(file)) { LOADING.remove(uuid); return; }
        WORKER.execute(() -> { try {
            EzClientMod.log("Cape: loading active local cape.");
            install(uuid, Files.readAllBytes(file));
        } catch (Exception ex) { EzClientMod.log("Cape: local cape could not be loaded: " + ex.getClass().getSimpleName()); } finally { LOADING.remove(uuid); } });
    }
    private static void refreshNearby(List<UUID> ids) {
        String query = String.join(",", ids.stream().map(UUID::toString).toList());
        WORKER.execute(() -> { try {
            String body = HTTP.send(HttpRequest.newBuilder(URI.create(API + "/capes/active?players=" + query)).timeout(Duration.ofSeconds(6)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            Matcher m = CAPE.matcher(body);
            while (m.find()) { UUID id = UUID.fromString(m.group(1)); if (CAPES.containsKey(id) || !LOADING.add(id)) continue; download(id, m.group(2)); }
        } catch (Exception ex) { EzClientMod.log("Cape: community sync unavailable (" + ex.getClass().getSimpleName() + ")."); } });
    }
    private static void download(UUID id, String url) { try {
        byte[] bytes = HTTP.send(HttpRequest.newBuilder(URI.create(url.replace("\\/", "/"))).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
        if (bytes.length <= 2 * 1024 * 1024) install(id, bytes);
    } catch (Exception ex) { EzClientMod.log("Cape: download failed for " + id + " (" + ex.getClass().getSimpleName() + ")."); } finally { LOADING.remove(id); } }
    private static void install(UUID id, byte[] bytes) throws Exception {
        NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
        if (image.getWidth() > 1024 || image.getHeight() > 512) { image.close(); return; }
        // The launcher stores a normal vanilla cape texture whose visible back
        // face contains the portrait artwork. Preserve every UV coordinate and
        // only normalize older/larger source images to the expected atlas size.
        NativeImage capeTexture = bakeCapeTexture(image);
        image.close();
        Minecraft.getInstance().execute(() -> {
            Identifier texture = Identifier.fromNamespaceAndPath("ezclient", "cape/" + id.toString().replace("-", ""));
            Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "ezclient-cape", capeTexture));
            CAPES.put(id, texture);
            EzClientMod.log("Cape: cached custom cape for " + id + ".");
        });
    }

    /** Normalizes the launcher/community texture to Minecraft's cape atlas. */
    private static NativeImage bakeCapeTexture(NativeImage design) {
        NativeImage result = new NativeImage(64, 64, true);
        int designWidth = design.getWidth();
        int designHeight = design.getHeight();
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                int sourceX = Math.min(designWidth - 1, x * designWidth / 64);
                int sourceY = Math.min(designHeight - 1, y * designHeight / 32);
                result.setPixel(x, y, design.getPixel(sourceX, sourceY));
            }
        }
        return result;
    }
}
