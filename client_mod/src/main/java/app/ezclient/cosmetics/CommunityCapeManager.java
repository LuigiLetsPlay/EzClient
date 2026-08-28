package app.ezclient.cosmetics;

import app.ezclient.EzClientMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.ByteArrayInputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
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
    private static final Map<UUID, CachedSkin> SKINS = new ConcurrentHashMap<>();
    private static final Map<UUID, AnimatedCape> ANIMATED_CAPES = new ConcurrentHashMap<>();
    private static final Map<UUID, String> REMOTE_KEYS = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "EzClient-Capes"); t.setDaemon(true); return t; });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private static volatile long nextRefresh;
    private static volatile long nextLocalScan;
    private static volatile long localFingerprint = Long.MIN_VALUE;
    private static volatile UUID localPlayer;

    private record AnimatedCape(
        DynamicTexture texture,
        NativeImage framesheet,
        int frameCount,
        int fps,
        int columns,
        int frameWidth,
        int frameHeight,
        int[] lastFrame
    ) {}

    private record CapeAsset(Identifier id) implements ClientAsset.Texture {
        @Override public Identifier texturePath() { return id; }
    }

    private record CachedSkin(Identifier texture, PlayerSkin source, PlayerSkin result) {}

    private CommunityCapeManager() {}
    public static Identifier cape(UUID player) { return CAPES.get(player); }
    public static PlayerSkin replaceCape(PlayerSkin original, UUID player) {
        Identifier texture = cape(player);
        if (texture == null) return original;
        if (original.cape() != null && texture.equals(original.cape().id())
                && original.elytra() != null && texture.equals(original.elytra().id())) {
            return original;
        }
        CachedSkin cached = SKINS.get(player);
        if (cached != null && texture.equals(cached.texture())
                && (cached.source() == original || cached.source().equals(original))) {
            return cached.result();
        }
        ClientAsset.Texture asset = new CapeAsset(texture);
        PlayerSkin result = new PlayerSkin(original.body(), asset, asset, original.model(), original.secure());
        SKINS.put(player, new CachedSkin(texture, original, result));
        return result;
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (System.currentTimeMillis() >= nextLocalScan) {
            nextLocalScan = System.currentTimeMillis() + 500L;
            loadLocal(client.player.getUUID());
        }

        // Update animated capes
        if (!ANIMATED_CAPES.isEmpty()) {
            long now = System.currentTimeMillis();
            for (AnimatedCape anim : ANIMATED_CAPES.values()) {
                if (anim.frameCount <= 1 || anim.fps <= 0) continue;
                int currentFrame = (int) ((now * anim.fps / 1000L) % anim.frameCount);
                if (currentFrame != anim.lastFrame[0]) {
                    anim.lastFrame[0] = currentFrame;
                    updateAnimatedFrame(anim, currentFrame);
                }
            }
        }

        long now = System.currentTimeMillis();
        if (now < nextRefresh) return;
        nextRefresh = now + REFRESH_MS;
        List<UUID> nearby = new ArrayList<>();
        client.level.players().forEach(p -> {
            if (nearby.size() < 32 && !p.getUUID().equals(client.player.getUUID())
                    && p.distanceToSqr(client.player) <= RANGE_SQ) nearby.add(p.getUUID());
        });
        if (!nearby.isEmpty()) {
            refreshNearby(nearby);
            CommunityPresence.refreshNearby(nearby);
        }
    }

    private static void updateAnimatedFrame(AnimatedCape anim, int frameIdx) {
        int col = frameIdx % anim.columns;
        int row = frameIdx / anim.columns;
        int srcX = col * anim.frameWidth;
        int srcY = row * anim.frameHeight;
        NativeImage target = anim.texture.getPixels();
        if (target == null || anim.framesheet == null) return;

        int fw = anim.frameWidth;
        int fh = anim.frameHeight;
        for (int y = 0; y < fh && (srcY + y) < anim.framesheet.getHeight() && y < target.getHeight(); y++) {
            for (int x = 0; x < fw && (srcX + x) < anim.framesheet.getWidth() && x < target.getWidth(); x++) {
                target.setPixel(x, y, anim.framesheet.getPixel(srcX + x, srcY + y));
            }
        }
        populateElytraAndMakeOpaque(target);
        anim.texture.upload();
    }

    private static void loadLocal(UUID uuid) {
        Path cosmetics = EzClientMod.getEzClientDataDir().resolve("cosmetics");
        Path animDir = cosmetics.resolve("active_cape_animation");
        Path animJson = animDir.resolve("animation.json");
        Path animSheet = animDir.resolve("framesheet.png");
        Path file = cosmetics.resolve("active_cape.png");
        long fingerprint = fileStamp(file) ^ Long.rotateLeft(fileStamp(animJson), 17) ^ Long.rotateLeft(fileStamp(animSheet), 33);
        if (uuid.equals(localPlayer) && fingerprint == localFingerprint) return;
        localPlayer = uuid;
        localFingerprint = fingerprint;
        clearCape(uuid);
        if (fingerprint == 0L || !LOADING.add(uuid)) return;

        WORKER.execute(() -> {
            try {
                if (Files.isRegularFile(animJson) && Files.isRegularFile(animSheet)) {
                    String json = Files.readString(animJson);
                    int frameCount = extractInt(json, "frame_count", 1);
                    int fps = extractInt(json, "fps", 12);
                    int columns = extractInt(json, "columns", frameCount);
                    int frameWidth = extractInt(json, "frame_width", 256);
                    int frameHeight = extractInt(json, "frame_height", 128);

                    NativeImage sheet = NativeImage.read(new ByteArrayInputStream(Files.readAllBytes(animSheet)));
                    NativeImage frameImg = new NativeImage(frameWidth, frameHeight, true);
                    for (int y = 0; y < frameHeight && y < sheet.getHeight(); y++) {
                        for (int x = 0; x < frameWidth && x < sheet.getWidth(); x++) {
                            frameImg.setPixel(x, y, sheet.getPixel(x, y));
                        }
                    }
                    populateElytraAndMakeOpaque(frameImg);

                    Minecraft.getInstance().execute(() -> {
                        Identifier textureId = Identifier.fromNamespaceAndPath("ezclient", "cape/" + uuid.toString().replace("-", ""));
                        DynamicTexture dynTex = new DynamicTexture(() -> "ezclient-cape-anim", frameImg);
                        Minecraft.getInstance().getTextureManager().register(textureId, dynTex);
                        publishCape(uuid, textureId);
                        ANIMATED_CAPES.put(uuid, new AnimatedCape(dynTex, sheet, frameCount, fps, columns, frameWidth, frameHeight, new int[]{-1}));
                        EzClientMod.log("Cape: initialized animated cape for " + uuid + " (" + frameCount + " frames).");
                    });
                    return;
                }

                if (Files.isRegularFile(file)) {
                    EzClientMod.log("Cape: loading active local cape.");
                    install(uuid, Files.readAllBytes(file));
                }
            } catch (Exception ex) {
                EzClientMod.log("Cape: local cape could not be loaded: " + ex.getClass().getSimpleName());
            } finally {
                LOADING.remove(uuid);
            }
        });
    }

    private static long fileStamp(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.getLastModifiedTime(file).toMillis() ^ Files.size(file) : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static int extractInt(String json, String key, int def) {
        try {
            Matcher m = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*([0-9]+)").matcher(json);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return def;
    }

    private static void refreshNearby(List<UUID> ids) {
        String query = String.join(",", ids.stream().map(UUID::toString).toList());
        WORKER.execute(() -> { try {
            String body = HTTP.send(HttpRequest.newBuilder(URI.create(API + "/capes/active?players=" + query)).timeout(Duration.ofSeconds(6)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            Set<UUID> active = new HashSet<>();
            JsonObject payload = JsonParser.parseString(body).getAsJsonObject();
            for (JsonElement element : payload.getAsJsonArray("capes")) {
                JsonObject cape = element.getAsJsonObject();
                UUID id = UUID.fromString(cape.get("owner_uuid").getAsString());
                String imageUrl = cape.get("image_url").getAsString().replace("\\/", "/");
                String animationUrl = cape.has("animation_url") ? cape.get("animation_url").getAsString().replace("\\/", "/") : "";
                String key = animationUrl.isBlank() ? imageUrl : animationUrl;
                active.add(id);
                if (key.equals(REMOTE_KEYS.get(id)) || !LOADING.add(id)) continue;
                REMOTE_KEYS.put(id, key);
                if (!animationUrl.isBlank()) downloadAnimated(id, animationUrl);
                else download(id, imageUrl);
            }
            for (UUID id : ids) {
                if (!active.contains(id) && REMOTE_KEYS.remove(id) != null) clearCape(id);
            }
        } catch (Exception ex) { EzClientMod.log("Cape: community sync unavailable (" + ex.getClass().getSimpleName() + ")."); } });
    }

    private static void download(UUID id, String url) { try {
        byte[] bytes = HTTP.send(HttpRequest.newBuilder(URI.create(url.replace("\\/", "/"))).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
        if (bytes.length <= 2 * 1024 * 1024) install(id, bytes);
    } catch (Exception ex) { REMOTE_KEYS.remove(id); EzClientMod.log("Cape: download failed for " + id + " (" + ex.getClass().getSimpleName() + ")."); } finally { LOADING.remove(id); } }

    private static void downloadAnimated(UUID id, String url) {
        try {
            byte[] bytes = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofByteArray()).body();
            if (bytes.length <= 8 * 1024 * 1024) installAnimatedGif(id, bytes);
        } catch (Exception ex) {
            REMOTE_KEYS.remove(id);
            EzClientMod.log("Cape: animation download failed for " + id + " (" + ex.getClass().getSimpleName() + ").");
        } finally {
            LOADING.remove(id);
        }
    }

    private static void installAnimatedGif(UUID id, byte[] bytes) throws Exception {
        List<NativeImage> frames = new ArrayList<>();
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) throw new IllegalArgumentException("GIF decoder unavailable");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, false, false);
                int count = Math.min(120, reader.getNumImages(true));
                for (int index = 0; index < count; index++) {
                    BufferedImage frame = reader.read(index);
                    if (frame.getWidth() > 1024 || frame.getHeight() > 1024) throw new IllegalArgumentException("GIF frame too large");
                    frames.add(bakeCapeFace(frame));
                }
            } finally {
                reader.dispose();
            }
        }
        if (frames.isEmpty()) return;
        int columns = Math.min(16, frames.size());
        int rows = (frames.size() + columns - 1) / columns;
        NativeImage sheet = new NativeImage(columns * 256, rows * 128, true);
        for (int i = 0; i < frames.size(); i++) {
            NativeImage frame = frames.get(i);
            int ox = (i % columns) * 256;
            int oy = (i / columns) * 128;
            for (int y = 0; y < 128; y++) for (int x = 0; x < 256; x++) sheet.setPixel(ox + x, oy + y, frame.getPixel(x, y));
            frame.close();
        }
        NativeImage first = new NativeImage(256, 128, true);
        for (int y = 0; y < 128; y++) for (int x = 0; x < 256; x++) first.setPixel(x, y, sheet.getPixel(x, y));
        Minecraft.getInstance().execute(() -> {
            Identifier textureId = Identifier.fromNamespaceAndPath("ezclient", "cape/" + id.toString().replace("-", ""));
            DynamicTexture texture = new DynamicTexture(() -> "ezclient-cape-anim", first);
            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            publishCape(id, textureId);
            ANIMATED_CAPES.put(id, new AnimatedCape(texture, sheet, frames.size(), 12, columns, 256, 128, new int[]{-1}));
        });
    }

    private static NativeImage bakeCapeFace(BufferedImage source) {
        NativeImage atlas = new NativeImage(256, 128, true);
        double sourceRatio = source.getWidth() / (double) Math.max(1, source.getHeight());
        double targetRatio = 10.0 / 16.0;
        int sx = 0, sy = 0, sw = source.getWidth(), sh = source.getHeight();
        if (sourceRatio > targetRatio) { sw = Math.max(1, (int) Math.round(sh * targetRatio)); sx = (source.getWidth() - sw) / 2; }
        else { sh = Math.max(1, (int) Math.round(sw / targetRatio)); sy = (source.getHeight() - sh) / 2; }
        blitScaled(source, atlas, sx, sy, sw, sh, 4, 4, 40, 64);
        blitScaled(source, atlas, sx, sy, sw, sh, 48, 4, 40, 64);
        blitScaled(source, atlas, sx, sy, sw, sh, 4, 0, 40, 4);
        blitScaled(source, atlas, sx, sy, sw, sh, 0, 4, 4, 64);
        blitScaled(source, atlas, sx, sy, sw, sh, 44, 4, 4, 64);
        blitScaled(source, atlas, sx, sy, sw, sh, 96, 8, 40, 80);
        blitScaled(source, atlas, sx, sy, sw, sh, 144, 8, 40, 80);
        blitScaled(source, atlas, sx, sy, sw, sh, 88, 8, 8, 80);
        blitScaled(source, atlas, sx, sy, sw, sh, 136, 8, 8, 80);
        blitScaled(source, atlas, sx, sy, sw, sh, 96, 0, 80, 8);
        return atlas;
    }

    private static void blitScaled(BufferedImage source, NativeImage target, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
        for (int y = 0; y < dh; y++) for (int x = 0; x < dw; x++) {
            int argb = source.getRGB(sx + Math.min(sw - 1, x * sw / dw), sy + Math.min(sh - 1, y * sh / dh));
            int abgr = (argb & 0xFF00FF00) | ((argb & 0x00FF0000) >>> 16) | ((argb & 0x000000FF) << 16);
            target.setPixel(dx + x, dy + y, abgr);
        }
    }

    private static void clearCape(UUID id) {
        CAPES.remove(id);
        SKINS.remove(id);
        AnimatedCape old = ANIMATED_CAPES.remove(id);
        if (old != null) {
            Minecraft.getInstance().execute(() -> {
                try { old.framesheet.close(); } catch (Exception ignored) {}
                try { old.texture.close(); } catch (Exception ignored) {}
            });
        }
    }

    private static void install(UUID id, byte[] bytes) throws Exception {
        AnimatedCape oldAnimation = ANIMATED_CAPES.remove(id);
        if (oldAnimation != null) {
            try { oldAnimation.framesheet.close(); } catch (Exception ignored) {}
            try { oldAnimation.texture.close(); } catch (Exception ignored) {}
        }
        NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
        if (image.getWidth() > 1024 || image.getHeight() > 512) { image.close(); return; }
        int w = image.getWidth();
        int h = image.getHeight();
        NativeImage capeTexture;
        if (w == h * 2 && (w & (w - 1)) == 0 && w >= 64) {
            capeTexture = image;
        } else {
            capeTexture = bakeCapeTexture(image);
            image.close();
        }
        populateElytraAndMakeOpaque(capeTexture);
        Minecraft.getInstance().execute(() -> {
            Identifier texture = Identifier.fromNamespaceAndPath("ezclient", "cape/" + id.toString().replace("-", ""));
            Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(() -> "ezclient-cape", capeTexture));
            publishCape(id, texture);
            EzClientMod.log("Cape: cached custom cape for " + id + ".");
        });
    }

    private static void publishCape(UUID player, Identifier texture) {
        CAPES.put(player, texture);
        SKINS.remove(player);
    }

    /** Normalizes the launcher/community texture to Minecraft's cape atlas. */
    private static NativeImage bakeCapeTexture(NativeImage design) {
        NativeImage result = new NativeImage(64, 32, true);
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

    /** Ensures the visible cape face is opaque and the Elytra wings UV region is populated. */
    private static void populateElytraAndMakeOpaque(NativeImage texture) {
        int scale = Math.max(1, texture.getWidth() / 64);
        int x0 = 1 * scale;
        int y0 = 1 * scale;
        int x1 = 11 * scale;
        int y1 = 17 * scale;

        // 1. Visible cape face opaque
        for (int y = y0; y < y1 && y < texture.getHeight(); y++) {
            for (int x = x0; x < x1 && x < texture.getWidth(); x++) {
                int color = texture.getPixel(x, y);
                texture.setPixel(x, y, color | 0xFF000000);
            }
        }

        // 2. Populate Elytra wing region if empty (X: 22..46, Y: 0..22 in 64x32 grid)
        int elytraX0 = 22 * scale;
        int elytraY0 = 0;
        int elytraX1 = 46 * scale;
        int elytraY1 = 22 * scale;
        boolean elytraEmpty = true;
        for (int y = elytraY0; y < elytraY1 && y < texture.getHeight(); y += scale) {
            for (int x = elytraX0; x < elytraX1 && x < texture.getWidth(); x += scale) {
                if (((texture.getPixel(x, y) >> 24) & 0xFF) > 10) {
                    elytraEmpty = false;
                    break;
                }
            }
            if (!elytraEmpty) break;
        }

        if (elytraEmpty) {
            for (int ey = 0; ey < 20 * scale && (2 * scale + ey) < texture.getHeight(); ey++) {
                int sy = y0 + (ey * (y1 - y0)) / (20 * scale);
                for (int ex = 0; ex < 10 * scale && (24 * scale + ex) < texture.getWidth(); ex++) {
                    int sx = x0 + (ex * (x1 - x0)) / (10 * scale);
                    int color = texture.getPixel(sx, sy) | 0xFF000000;
                    // Outer wing: 24..34, 2..22
                    texture.setPixel(24 * scale + ex, 2 * scale + ey, color);
                    // Inner wing: 36..46, 2..22
                    if (36 * scale + ex < texture.getWidth()) {
                        texture.setPixel(36 * scale + ex, 2 * scale + ey, color);
                    }
                }
            }
            // Wing caps & side borders
            for (int ey = 0; ey < 20 * scale && (2 * scale + ey) < texture.getHeight(); ey++) {
                int color = texture.getPixel(24 * scale, 2 * scale + ey) | 0xFF000000;
                for (int dx = 0; dx < 2 * scale; dx++) {
                    texture.setPixel(22 * scale + dx, 2 * scale + ey, color);
                    texture.setPixel(34 * scale + dx, 2 * scale + ey, color);
                }
            }
            for (int ex = 0; ex < 20 * scale && (24 * scale + ex) < texture.getWidth(); ex++) {
                int color = texture.getPixel(24 * scale + (ex % (10 * scale)), 2 * scale) | 0xFF000000;
                for (int dy = 0; dy < 2 * scale; dy++) {
                    texture.setPixel(24 * scale + ex, dy, color);
                }
            }
        }
    }
}
