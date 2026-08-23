package app.ezclient.lite;

import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

/**
 * EzClient Lite Core Mod
 * - Dynamic Window Title ("EzClient") & Custom Window Icon
 * - Skip Narrator Notification & Accessibility Prompt
 * - Does NOT include Zoom or MainMenu branding because it avoids Mixins and Minecraft class dependencies.
 */
public class EzClientLiteMod implements ClientModInitializer {
public static final String CLIENT_VERSION = "1.5.4";
public static final String CLIENT_TITLE = "EzClient 1.5.4 (Lite)";
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
                System.err.println("[EzClient-Lite] Could not create .ezclient directory structure: " + e.getMessage());
            }
        }
        return ezClientDataDir;
    }

    public static void log(String message) {
        String formatted = String.format("[%tF %<tT] [EzClient-Lite] %s", new Date(), message);
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

        // 1. Sync & update persistent client config
        syncGlobalClientConfig();

        // 2. Apply optimized PvP & Sodium settings on first launch
        applyOptimizedSettings();

        // 3. Start Window Title & Icon Watcher Daemon
        startWindowDaemon();

        // 4. Start Accessibility / Narrator Dismissal Daemon
        startNarratorDismissDaemon();

        log("EzClient Core Mod initialized successfully!");
    }

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

    private void applyOptimizedSettings() {
        try {
            Path runDir = Paths.get(".").toAbsolutePath().normalize();
            Path configDir = runDir.resolve("config");
            Files.createDirectories(configDir);

            Path markerFile = configDir.resolve("ezclient_initialized.json");

            if (!Files.exists(markerFile)) {
                System.out.println("[EzClient-Lite] First launch detected! Applying optimized PvP/Performance settings...");

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

                options.put("graphicsMode", "0");
                options.put("renderDistance", "8");
                options.put("simulationDistance", "5");
                options.put("entityShadows", "false");
                options.put("clouds", "false");
                options.put("cloudStatus", "false");
                options.put("particles", "2");
                options.put("biomeBlendRadius", "0");
                options.put("maxFps", "260");
                options.put("enableVsync", "false");
                options.put("onboardAccessibility", "false");
                options.put("narrator", "0");
                options.put("skipRealmsNotifications", "true");
                options.put("gamma", "1.0");
                options.put("smoothLighting", "false");

                List<String> outLines = new ArrayList<>();
                for (Map.Entry<String, String> entry : options.entrySet()) {
                    outLines.add(entry.getKey() + ":" + entry.getValue());
                }
                Files.write(optionsFile, outLines);

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

                Files.write(markerFile, Collections.singletonList("{\"initialized\": true, \"version\": \"" + CLIENT_VERSION + "\"}"));
                System.out.println("[EzClient-Lite] Optimized settings successfully applied!");
            }
        } catch (Exception e) {
            System.err.println("[EzClient-Lite] Warning: Could not pre-apply settings: " + e.getMessage());
        }
    }

    private void startWindowDaemon() {
        Thread thread = new Thread(() -> {
            long lastWindow = 0L;
            int tickCounter = 0;

            while (running) {
                try {
                    Thread.sleep(50);
                    tickCounter++;

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

                    GLFW.glfwSetWindowTitle(window, CLIENT_TITLE);

                    if (window != lastWindow || tickCounter % 40 == 0) {
                        lastWindow = window;
                        applyWindowIcon(window);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {
                }
            }
        }, "EzClient-Lite-WindowDaemon");

        thread.setDaemon(true);
        thread.start();
    }

    private void applyWindowIcon(long window) {
        try {
            java.io.InputStream is = EzClientLiteMod.class.getResourceAsStream("/assets/ezclient/icon.png");
            if (is == null) {
                return;
            }
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);
            is.close();

            int width = img.getWidth();
            int height = img.getHeight();
            int[] pixelsRaw = img.getRGB(0, 0, width, height, null, 0, width);
            ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);

            for (int i = 0; i < pixelsRaw.length; i++) {
                int pixel = pixelsRaw[i];
                pixels.put((byte) ((pixel >> 16) & 0xFF));
                pixels.put((byte) ((pixel >> 8) & 0xFF));
                pixels.put((byte) (pixel & 0xFF));
                pixels.put((byte) ((pixel >> 24) & 0xFF));
            }
            pixels.flip();

            GLFWImage.Buffer imageBuffer = GLFWImage.malloc(1);
            imageBuffer.position(0);
            imageBuffer.width(width);
            imageBuffer.height(height);
            imageBuffer.pixels(pixels);

            GLFW.glfwSetWindowIcon(window, imageBuffer);
            imageBuffer.free();
            MemoryUtil.memFree(pixels);
        } catch (Throwable t) {}
    }

    private void startNarratorDismissDaemon() {
        Thread thread = new Thread(() -> {
            int checks = 0;
            while (running && checks < 300) {
                try {
                    Thread.sleep(250);
                    checks++;

                    try {
                        Class<?> mcClass = Class.forName("net.minecraft.client.MinecraftClient");
                        Object instance = mcClass.getMethod("getInstance").invoke(null);
                        if (instance != null) {
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
                                        System.out.println("[EzClient-Lite] Suppressing " + screenName + " -> Navigating to TitleScreen...");
                                        
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
                    } catch (Throwable ignored) {}
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
        }, "EzClient-Lite-NarratorDismissDaemon");

        thread.setDaemon(true);
        thread.start();
    }
}
