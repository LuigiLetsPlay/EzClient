package app.ezclient.v1_16_v1_20.cosmetics;

import app.ezclient.shared.EzClientPaths;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CapeManager {
    private static final Map<UUID, Identifier> CAPES = new ConcurrentHashMap<UUID, Identifier>();
    private static long nextLocalScan = 0L;
    private static long localFingerprint = -1L;

    private CapeManager() {}

    public static Identifier getCape(UUID uuid) {
        return CAPES.get(uuid);
    }

    public static void onTick(MinecraftClient client) {
        if (client == null || client.player == null) return;
        long now = System.currentTimeMillis();
        if (now >= nextLocalScan) {
            nextLocalScan = now + 1000L;
            loadLocalCape(client.player.getUuid());
        }
    }

    private static void loadLocalCape(UUID playerUuid) {
        try {
            File capeDir = new File(EzClientPaths.dataDirectory().toFile(), "capes");
            if (!capeDir.exists()) return;
            File capeFile = new File(capeDir, "active_cape.png");
            if (!capeFile.exists() || !capeFile.isFile()) return;

            long lastMod = capeFile.lastModified();
            if (lastMod == localFingerprint && CAPES.containsKey(playerUuid)) return;
            localFingerprint = lastMod;

            FileInputStream in = new FileInputStream(capeFile);
            NativeImage img = NativeImage.read(in);
            in.close();
            if (img == null) return;

            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            //? if <=1.20.4 {
            Identifier id = new Identifier("ezclient", "cape_" + playerUuid.toString().replace("-", ""));
            //?} else {
            /*Identifier id = Identifier.of("ezclient", "cape_" + playerUuid.toString().replace("-", ""));
            *///?}
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            CAPES.put(playerUuid, id);
        } catch (Throwable t) {
            // Ignore
        }
    }
}
