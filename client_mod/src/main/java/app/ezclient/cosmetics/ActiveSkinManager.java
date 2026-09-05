package app.ezclient.cosmetics;

import app.ezclient.EzClientMod;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Polls the launcher's active skin file and hot-swaps the local player texture. */
public final class ActiveSkinManager {
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("ezclient", "skin/active_local");
    private static volatile ClientAsset.Texture activeTexture;
    private static volatile UUID localPlayer;
    private static volatile long fingerprint = Long.MIN_VALUE;
    private static long nextScan;
    private static DynamicTexture dynamicTexture;
    private record CachedSkin(ClientAsset.Texture body, PlayerSkin source, PlayerSkin result) {}
    private static volatile CachedSkin cachedSkin;
    private static final java.util.concurrent.ExecutorService WORKER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "EzClient-SkinLoader"); thread.setDaemon(true); return thread;
    });
    private static final java.util.concurrent.atomic.AtomicBoolean SCANNING = new java.util.concurrent.atomic.AtomicBoolean();

    private ActiveSkinManager() {}

    public static void tick(Minecraft client) {
        if (client.player == null || System.currentTimeMillis() < nextScan) return;
        nextScan = System.currentTimeMillis() + 2000L;
        localPlayer = client.player.getUUID();
        if (!SCANNING.compareAndSet(false, true)) return;
        WORKER.execute(() -> scan(client));
    }

    private static void scan(Minecraft client) {
        Path descriptor = EzClientMod.getEzClientDataDir().resolve("skins").resolve("active_skin.json");
        try {
            long descriptorStamp = Files.isRegularFile(descriptor) ? Files.getLastModifiedTime(descriptor).toMillis() : 0L;
            String skinPath = "";
            if (descriptorStamp != 0L) {
                JsonObject json = JsonParser.parseString(Files.readString(descriptor)).getAsJsonObject();
                if (json.has("path")) skinPath = json.get("path").getAsString();
            }
            Path file = skinPath.isBlank() ? null : Path.of(skinPath);
            long fileStamp = file != null && Files.isRegularFile(file)
                    ? Files.getLastModifiedTime(file).toMillis() ^ Files.size(file) : 0L;
            long nextFingerprint = descriptorStamp ^ Long.rotateLeft(fileStamp, 19);
            if (nextFingerprint == fingerprint) return;
            fingerprint = nextFingerprint;
            if (file == null || !Files.isRegularFile(file)) {
                clear(client);
                return;
            }
            if (Files.size(file) > 1024 * 1024) return;
            byte[] bytes = Files.readAllBytes(file);
            client.execute(() -> install(client, bytes));
        } catch (Exception ex) {
            EzClientMod.log("Skin hot reload failed: " + ex.getClass().getSimpleName());
        } finally {
            SCANNING.set(false);
        }
    }

    public static PlayerSkin replaceLocalSkin(PlayerSkin original, UUID player) {
        ClientAsset.Texture body = activeTexture;
        if (body == null || localPlayer == null || !localPlayer.equals(player)) return original;
        CachedSkin cached = cachedSkin;
        if (cached != null && cached.body() == body && (cached.source() == original || cached.source().equals(original))) return cached.result();
        PlayerSkin result = new PlayerSkin(body, original.cape(), original.elytra(), original.model(), original.secure());
        cachedSkin = new CachedSkin(body, original, result);
        return result;
    }

    private static void install(Minecraft client, byte[] bytes) {
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            if (!((image.getWidth() == 64 && image.getHeight() == 64)
                    || (image.getWidth() == 64 && image.getHeight() == 32))) {
                image.close();
                return;
            }
            DynamicTexture texture = new DynamicTexture(() -> "ezclient-active-skin", image);
            client.getTextureManager().register(TEXTURE_ID, texture);
            dynamicTexture = texture;
            activeTexture = new SkinAsset(TEXTURE_ID);
            EzClientMod.log("Skin hot reloaded without reconnecting.");
        } catch (Exception ex) {
            EzClientMod.log("Skin hot reload decode failed: " + ex.getClass().getSimpleName());
        }
    }

    private static void clear(Minecraft client) {
        client.execute(() -> {
            activeTexture = null;
            cachedSkin = null;
            dynamicTexture = null;
            client.getTextureManager().release(TEXTURE_ID);
        });
    }

    private record SkinAsset(Identifier id) implements ClientAsset.Texture {
        @Override public Identifier texturePath() { return id; }
    }
}
