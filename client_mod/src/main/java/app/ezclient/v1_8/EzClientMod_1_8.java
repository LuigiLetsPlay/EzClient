package app.ezclient.v1_8;

import app.ezclient.v1_8.gui.EzHubScreen;
import app.ezclient.v1_8.modules.CpsModule;
import app.ezclient.v1_8.modules.ModuleManager;
import app.ezclient.v1_8.modules.ZoomModule;
import app.ezclient.shared.EzClientPaths;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Java 8 / Legacy Fabric bootstrap for Minecraft 1.8.9. */
public final class EzClientMod_1_8 implements ClientModInitializer {
    public static final String CLIENT_VERSION = "2.0.0";
    public static final String CLIENT_TITLE = "EzClient 2.0.0";

    private static boolean lastGuiKeyState = false;

    @Override
    public void onInitializeClient() {
        EzClientPaths.dataDirectory();
        System.out.println("[EzClient] Legacy client " + CLIENT_VERSION + " initializing...");

        ModuleManager.getInstance().loadConfig();
        applyOptimizedSettings();

        System.out.println("[EzClient] Legacy client " + CLIENT_VERSION + " initialized successfully!");
    }

    public static void checkKeyInput() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        boolean isGuiKeyDown = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (isGuiKeyDown && !lastGuiKeyState) {
            if (client.currentScreen == null && client.player != null) {
                System.out.println("[EzClient] Opening Legacy Dashboard via Right Shift");
                client.setScreen(new EzHubScreen(null));
            }
        }
        lastGuiKeyState = isGuiKeyDown;
    }

    public static void onTick() {
        // 1. Update CPS tracker
        CpsModule.updateClicks();

        // 2. Right Shift -> Open Dashboard if in-game
        checkKeyInput();

        // 3. Zoom mouse wheel adjustments
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
            boolean canZoom = client.currentScreen == null
                    && zoom.getKeyBind() > 0
                    && Keyboard.isKeyDown(zoom.getKeyBind());
            zoom.updateZoomPressed(canZoom);
            if (zoom.isZoomPressed()) {
                int dWheel = org.lwjgl.input.Mouse.getDWheel();
                if (dWheel > 0) {
                    zoom.zoomIn();
                } else if (dWheel < 0) {
                    zoom.zoomOut();
                }
            }
        }

        // 4. Tick active modules & cape manager
        ModuleManager.getInstance().onTick();
        app.ezclient.v1_8.cosmetics.CapeManager.onTick(client);
    }

    public static float getZoomFovFactor() {
        return ModuleManager.getInstance().getZoomModule().getFovFactor();
    }

    private static void applyOptimizedSettings() {
        try {
            File runDir = new File(".");
            File markerFile = new File(runDir, "ezclient_legacy_initialized.json");
            if (markerFile.exists()) return;

            File optionsFile = new File(runDir, "options.txt");
            Map<String, String> options = new LinkedHashMap<String, String>();

            if (optionsFile.exists()) {
                List<String> lines = Files.readAllLines(optionsFile.toPath());
                for (String line : lines) {
                    int colon = line.indexOf(':');
                    if (colon > 0) {
                        options.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                    }
                }
            }

            options.put("graphicsMode", "1");
            options.put("renderDistance", "8");
            options.put("entityShadows", "false");
            options.put("clouds", "false");
            options.put("particles", "2");
            options.put("maxFps", "260");
            options.put("enableVsync", "false");
            options.put("gamma", "1.0");
            options.put("smoothLighting", "false");

            List<String> outLines = new ArrayList<String>();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                outLines.add(entry.getKey() + ":" + entry.getValue());
            }
            Files.write(optionsFile.toPath(), outLines);

            FileWriter fw = new FileWriter(markerFile);
            fw.write("{\"initialized\": true, \"version\": \"" + CLIENT_VERSION + "\"}");
            fw.close();
            System.out.println("[EzClient] Applied optimized settings for Legacy 1.8.9");
        } catch (Throwable t) {
            System.err.println("[EzClient] Warning: Could not pre-apply legacy settings: " + t.getMessage());
        }
    }
}
