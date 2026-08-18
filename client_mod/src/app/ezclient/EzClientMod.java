package app.ezclient;

import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

/**
 * EzClient Core Mod
 * - Dynamic Window Title ("EzClient") & Custom Window Icon
 * - Skip Narrator Notification & Accessibility Prompt
 * - First-Launch Performance & PvP Optimization (Fast Graphics, 8 Chunks, No Shadows/Clouds, Biome Blend 0, Unlimited FPS)
 */
public class EzClientMod implements ClientModInitializer {
    public static final String CLIENT_TITLE = "EzClient";
    public static final String CLIENT_VERSION = "1.0.0";
    private static volatile boolean running = true;
    private static Path ezClientDataDir = null;

    public static Path getEzClientDataDir() {
        if (ezClientDataDir == null) {
            String appdata = System.getenv("APPDATA");
            if (appdata != null && !appdata.trim().isEmpty()) {
                ezClientDataDir = Paths.get(appdata, ".ezclient");
            } else {
                ezClientDataDir = Paths.get(System.getProperty("user.home"), ".ezclient");
            }
            try {
                Files.createDirectories(ezClientDataDir);
                Files.createDirectories(ezClientDataDir.resolve("config"));
                Files.createDirectories(ezClientDataDir.resolve("logs"));
                Files.createDirectories(ezClientDataDir.resolve("cosmetics"));
                Files.createDirectories(ezClientDataDir.resolve("screenshots"));
                Files.createDirectories(ezClientDataDir.resolve("stats"));
            } catch (Exception e) {
                System.err.println("[EzClient] Could not create .ezclient directory structure: " + e.getMessage());
            }
        }
        return ezClientDataDir;
    }

    public static void log(String message) {
        String formatted = String.format("[%tF %<tT] [EzClient] %s", new Date(), message);
        System.out.println(formatted);
        try {
            Path logFile = getEzClientDataDir().resolve("logs").resolve("ezclient_client.log");
            Files.write(logFile, Collections.singletonList(formatted), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onInitializeClient() {
        Path dataDir = getEzClientDataDir();
        log("========================================");
        log("EzClient Core Mod v" + CLIENT_VERSION + " initializing...");
        log("AppData Data Directory: " + dataDir.toAbsolutePath());
        log("========================================");

        // 1. Sync & update persistent client config in .ezclient/config/client_settings.json
        syncGlobalClientConfig();

        // 2. Apply optimized PvP & Sodium settings on first launch
        applyOptimizedSettings();

        // 3. Start Window Title & Icon Watcher Daemon
        startWindowDaemon();

        // 4. Start Accessibility / Narrator Dismissal Daemon
        startNarratorDismissDaemon();

        log("EzClient Core Mod initialized successfully!");
    }

    /**
     * Reads and updates global client configuration stored in %APPDATA%/.ezclient/config/client_settings.json
     */
    private void syncGlobalClientConfig() {
        try {
            Path configDir = getEzClientDataDir().resolve("config");
            Files.createDirectories(configDir);
            Path configFile = configDir.resolve("client_settings.json");

            int launchCount = 1;
            if (Files.exists(configFile)) {
                try {
                    String content = new String(Files.readAllBytes(configFile), "UTF-8");
                    int idx = content.indexOf("\"launchCount\":");
                    if (idx != -1) {
                        String sub = content.substring(idx + 14).trim();
                        int endIdx = -1;
                        for (int i = 0; i < sub.length(); i++) {
                            char c = sub.charAt(i);
                            if (c == ',' || c == '}' || c == '\n' || c == '\r') {
                                endIdx = i;
                                break;
                            }
                        }
                        if (endIdx != -1) {
                            launchCount = Integer.parseInt(sub.substring(0, endIdx).trim()) + 1;
                        }
                    }
                } catch (Exception ignored) {}
            }

            String json = "{\n" +
                    "  \"clientName\": \"" + CLIENT_TITLE + "\",\n" +
                    "  \"clientVersion\": \"" + CLIENT_VERSION + "\",\n" +
                    "  \"windowTitle\": \"" + CLIENT_TITLE + "\",\n" +
                    "  \"customWindowIcon\": true,\n" +
                    "  \"narratorBypass\": true,\n" +
                    "  \"fastPvPPresets\": true,\n" +
                    "  \"launchCount\": " + launchCount + ",\n" +
                    "  \"lastLaunch\": \"" + new Date().toString() + "\",\n" +
                    "  \"dataDirectory\": \"" + getEzClientDataDir().toAbsolutePath().toString().replace("\\", "\\\\") + "\"\n" +
                    "}\n";

            Files.write(configFile, Collections.singletonList(json), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log("Global client configuration synced with " + configFile.toAbsolutePath());
        } catch (Exception e) {
            log("Warning: Could not sync global client config: " + e.getMessage());
        }
    }

    /**
     * Applies high-performance settings for standard game options and Sodium config.
     */
    private void applyOptimizedSettings() {
        try {
            Path runDir = Paths.get(".").toAbsolutePath().normalize();
            Path configDir = runDir.resolve("config");
            Files.createDirectories(configDir);

            Path markerFile = configDir.resolve("ezclient_initialized.json");

            if (!Files.exists(markerFile)) {
                System.out.println("[EzClient] First launch detected! Applying optimized PvP/Performance settings...");

                // 1. Optimize options.txt
                Path optionsFile = runDir.resolve("options.txt");
                Map<String, String> options = new LinkedHashMap<>();

                if (Files.exists(optionsFile)) {
                    List<String> lines = Files.readAllLines(optionsFile);
                    for (String line : lines) {
                        int colon = line.indexOf(':');
                        if (colon > 0) {
                            options.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                        }
                    }
                }

                // PvP & Performance Settings as specified
                options.put("graphicsMode", "0");               // Fast (0=fast, 1=fancy, 2=fabulous)
                options.put("renderDistance", "8");             // 8 Chunks
                options.put("simulationDistance", "5");         // 5 Chunks
                options.put("entityShadows", "false");          // OFF
                options.put("clouds", "false");                 // OFF
                options.put("cloudStatus", "false");            // OFF
                options.put("particles", "2");                  // Minimal (2=minimal, 1=decreased, 0=all)
                options.put("biomeBlendRadius", "0");           // 0 (OFF)
                options.put("maxFps", "260");                   // Max framerate / Unlimited
                options.put("enableVsync", "false");            // VSync OFF
                options.put("onboardAccessibility", "false");   // Skip accessibility screen
                options.put("narrator", "0");                   // Narrator OFF (0)
                options.put("skipRealmsNotifications", "true"); // Skip notifications
                options.put("gamma", "1.0");                    // Brightness 100%
                options.put("smoothLighting", "false");         // Smooth Lighting OFF

                List<String> outLines = new ArrayList<>();
                for (Map.Entry<String, String> entry : options.entrySet()) {
                    outLines.add(entry.getKey() + ":" + entry.getValue());
                }
                Files.write(optionsFile, outLines);

                // 2. Optimize config/sodium-options.json
                Path sodiumFile = configDir.resolve("sodium-options.json");
                if (!Files.exists(sodiumFile)) {
                    String sodiumJson = "{\n" +
                            "  \"quality\": {\n" +
                            "    \"graphics_quality\": \"DEFAULT\",\n" +
                            "    \"weather_quality\": \"FAST\",\n" +
                            "    \"leaves_quality\": \"FAST\",\n" +
                            "    \"cloud_quality\": \"OFF\",\n" +
                            "    \"particles_quality\": \"MINIMAL\",\n" +
                            "    \"smooth_lighting\": \"OFF\",\n" +
                            "    \"biome_blend\": 0,\n" +
                            "    \"entity_shadows\": false,\n" +
                            "    \"vignette\": false\n" +
                            "  },\n" +
                            "  \"performance\": {\n" +
                            "    \"chunk_builder_threads\": 0,\n" +
                            "    \"always_defer_chunk_updates\": true,\n" +
                            "    \"use_compact_vertex_format\": true,\n" +
                            "    \"animate_only_visible_textures\": true\n" +
                            "  },\n" +
                            "  \"advanced\": {\n" +
                            "    \"use_early_z\": true\n" +
                            "  },\n" +
                            "  \"notifications\": {\n" +
                            "    \"hide_donation_prompts\": true\n" +
                            "  }\n" +
                            "}";
                    Files.write(sodiumFile, Collections.singletonList(sodiumJson));
                }

                // 3. Write initialization marker
                Files.write(markerFile, Collections.singletonList("{\"initialized\": true, \"version\": \"" + CLIENT_VERSION + "\"}"));
                System.out.println("[EzClient] Optimized settings successfully applied!");
            }
        } catch (Exception e) {
            System.err.println("[EzClient] Warning: Could not pre-apply settings: " + e.getMessage());
        }
    }

    /**
     * Starts a background thread that sets and maintains the window title to "EzClient"
     * and applies the custom EzClient icon via GLFW safely after Minecraft initializes.
     */
    private void startWindowDaemon() {
        Thread thread = new Thread(() -> {
            try {
                // Allow Minecraft to complete GLFW initialization first
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                return;
            }

            long lastWindow = 0L;
            int attempts = 0;
            boolean iconApplied = false;

            while (running && attempts < 1000) {
                try {
                    Thread.sleep(500);
                    attempts++;

                    long window = 0L;
                    try {
                        Class<?> mcClass = Class.forName("net.minecraft.client.MinecraftClient");
                        Object instance = mcClass.getMethod("getInstance").invoke(null);
                        if (instance != null) {
                            for (java.lang.reflect.Method m : mcClass.getMethods()) {
                                if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().equals("Window")) {
                                    Object winObj = m.invoke(instance);
                                    if (winObj != null) {
                                        for (java.lang.reflect.Method wm : winObj.getClass().getMethods()) {
                                            if (wm.getName().equals("getHandle") && wm.getParameterCount() == 0) {
                                                Object handle = wm.invoke(winObj);
                                                if (handle instanceof Long) {
                                                    window = (Long) handle;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}

                    if (window == 0L) {
                        continue;
                    }

                    // Enforce clean "EzClient" title
                    GLFW.glfwSetWindowTitle(window, CLIENT_TITLE);

                    // Apply custom icon once window is available
                    if (!iconApplied || window != lastWindow) {
                        lastWindow = window;
                        applyWindowIcon(window);
                        iconApplied = true;
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {
                }
            }
        }, "EzClient-WindowDaemon");

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Creates and loads a vibrant green EzClient icon buffer into GLFW.
     */
    private void applyWindowIcon(long window) {
        try {
            // Create a 16x16 and 32x32 procedurally generated EzClient icon in RGBA format
            int[] sizes = {16, 32};
            GLFWImage.Buffer imageBuffer = GLFWImage.malloc(sizes.length);

            for (int s = 0; s < sizes.length; s++) {
                int size = sizes[s];
                ByteBuffer pixels = MemoryUtil.memAlloc(size * size * 4);

                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        // Rounded background rectangle with vibrant emerald green (#24D677)
                        boolean isEdge = (x == 0 || x == size - 1 || y == 0 || y == size - 1);
                        boolean isCorner = (x <= 1 && y <= 1) || (x >= size - 2 && y <= 1) ||
                                           (x <= 1 && y >= size - 2) || (x >= size - 2 && y >= size - 2);

                        if (isCorner) {
                            pixels.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                        } else {
                            // Emerald Green #24D677
                            pixels.put((byte) 0x24); // R
                            pixels.put((byte) 0xD6); // G
                            pixels.put((byte) 0x77); // B
                            pixels.put((byte) 0xFF); // A
                        }
                    }
                }
                pixels.flip();

                imageBuffer.position(s);
                imageBuffer.width(size);
                imageBuffer.height(size);
                imageBuffer.pixels(pixels);
            }
            imageBuffer.position(0);

            GLFW.glfwSetWindowIcon(window, imageBuffer);
            imageBuffer.free();
            System.out.println("[EzClient] Custom EzClient window icon applied to GLFW window!");
        } catch (Throwable t) {
            // Fallback gracefully if native icon allocation differs
            System.out.println("[EzClient] Note: Native icon set fallback handled: " + t.getMessage());
        }
    }

    /**
     * Watches for and dismisses narrator / accessibility prompts automatically.
     */
    private void startNarratorDismissDaemon() {
        Thread thread = new Thread(() -> {
            int checks = 0;
            while (running && checks < 300) {
                try {
                    Thread.sleep(250);
                    checks++;

                    // Use reflection to safely detect and dismiss AccessibilityOnboardingScreen or Narrator
                    // without hard version binding
                    try {
                        Class<?> mcClass = Class.forName("net.minecraft.client.MinecraftClient");
                        Object instance = mcClass.getMethod("getInstance").invoke(null);
                        if (instance != null) {
                            // Check current screen
                            java.lang.reflect.Field screenField = null;
                            for (java.lang.reflect.Field f : mcClass.getDeclaredFields()) {
                                if (f.getType().getName().contains("Screen")) {
                                    screenField = f;
                                    break;
                                }
                            }
                            if (screenField != null) {
                                screenField.setAccessible(true);
                                Object currentScreen = screenField.get(instance);
                                if (currentScreen != null) {
                                    String screenName = currentScreen.getClass().getSimpleName();
                                    if (screenName.contains("Accessibility") || screenName.contains("Narrator") || screenName.contains("Onboarding")) {
                                        System.out.println("[EzClient] Suppressing " + screenName + " -> Navigating to TitleScreen...");
                                        
                                        // Set screen to TitleScreen or null to bypass
                                        Class<?> titleScreenClass = Class.forName("net.minecraft.client.gui.screen.TitleScreen");
                                        Object titleScreen = titleScreenClass.getConstructor().newInstance();
                                        
                                        for (java.lang.reflect.Method m : mcClass.getMethods()) {
                                            if (m.getParameterCount() == 1 && m.getParameterTypes()[0].getName().contains("Screen")) {
                                                m.invoke(instance, titleScreen);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                        // Safe reflection pass
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
        }, "EzClient-NarratorDismissDaemon");

        thread.setDaemon(true);
        thread.start();
    }
}
