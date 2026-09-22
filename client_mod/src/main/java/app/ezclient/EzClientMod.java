package app.ezclient;

import net.fabricmc.api.ClientModInitializer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import app.ezclient.gui.ConfigManager;
import app.ezclient.gui.EzClientScreen;
import app.ezclient.gui.EzHubScreen;
import app.ezclient.gui.EzKeyBindings;
import app.ezclient.gui.EzScreenBridge;
import app.ezclient.gui.HudEditorScreen;
import app.ezclient.gui.HudRenderer;
import app.ezclient.gui.ModuleManager;
import app.ezclient.cosmetics.CommunityPresence;
import app.ezclient.cosmetics.CommunityCapeManager;
import app.ezclient.render.ConnectedGlassModel;
import app.ezclient.render.GlowingOreModel;
import net.minecraft.network.chat.Component;

/**
 * EzClient Core Mod
 * - Dynamic Window Title ("EzClient") & Custom Window Icon
 * - Skip Narrator Notification & Accessibility Prompt
 * - First-Launch Performance & PvP Optimization (Fast Graphics, 8 Chunks, No Shadows/Clouds, Biome Blend 0, 120 FPS default)
 */
public class EzClientMod implements ClientModInitializer {
    public static final String CLIENT_VERSION = "2.2.2";
    public static final String CLIENT_TITLE = "EzClient 2.2.2";
    private static volatile boolean running = true;
    private static Path ezClientDataDir = null;

    private static boolean isZooming = false;
    private static boolean lastGuiKeyState = false;
    private static boolean iconApplied = false;
    private static final Map<String, Boolean> moduleKeyStates = new HashMap<>();

    public static boolean isZooming() {
        return isZooming;
    }

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
        ConnectedGlassModel.register();
        GlowingOreModel.register();
        registerBuiltinResourcePacks();
        app.ezclient.gui.ItemIconHelper.ensureComponentsBound();
        Path dataDir = getEzClientDataDir();
        log("========================================");
        log("EzClient Core Mod v" + CLIENT_VERSION + " initializing...");
        log("AppData Data Directory: " + dataDir.toAbsolutePath());
        log("========================================");

        ConfigManager.load();
        EzKeyBindings.init();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client ->
            client.getSoundManager().addListener(app.ezclient.gui.FeatureModule.get(app.ezclient.gui.SoundEnhancerModule.class)));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EzKeyBindings.syncFromControls(client);
            if (client.getWindow() != null) {
                int hubKey = EzKeyBindings.getKeyCode(EzKeyBindings.KEY_HUB);
                if (hubKey <= 0) hubKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
                boolean isGuiKeyDown = InputConstants.isKeyDown(client.getWindow(), hubKey);
                if (isGuiKeyDown && !lastGuiKeyState) {
                    if (EzScreenBridge.current(client) instanceof EzHubScreen || EzScreenBridge.current(client) instanceof HudEditorScreen) {
                        EzScreenBridge.set(client, null);
                    } else if (EzScreenBridge.current(client) == null && client.level != null && client.player != null) {
                        EzScreenBridge.set(client, new EzHubScreen(null));
                    }
                }
                lastGuiKeyState = isGuiKeyDown;
            }

            if (EzKeyBindings.KEY_HUD_EDITOR != null) {
                while (EzKeyBindings.KEY_HUD_EDITOR.consumeClick()) {
                    if (EzScreenBridge.current(client) instanceof HudEditorScreen) {
                        EzScreenBridge.set(client, null);
                    } else if (EzScreenBridge.current(client) == null && client.level != null && client.player != null) {
                        EzScreenBridge.set(client, new HudEditorScreen(null));
                    }
                }
            }

            if (EzKeyBindings.KEY_COPY_COORDINATES != null) {
                while (EzKeyBindings.KEY_COPY_COORDINATES.consumeClick()) {
                    if (client.player != null) {
                        String text = String.format(Locale.ROOT, "X: %.1f Y: %.1f Z: %.1f", client.player.getX(), client.player.getY(), client.player.getZ());
                        client.keyboardHandler.setClipboard(text);
                        client.player.sendOverlayMessage(Component.literal("§a[EzClient] Koordinaten kopiert: " + text));
                    }
                }
            }

            if (EzKeyBindings.KEY_FULLBRIGHT != null) {
                while (EzKeyBindings.KEY_FULLBRIGHT.consumeClick()) {
                    var fullbright = ModuleManager.getInstance().getFullbrightModule();
                    if (fullbright != null) {
                        fullbright.toggle();
                        ConfigManager.save();
                        if (client.player != null) {
                            client.player.sendOverlayMessage(Component.literal("§e[EzClient] Fullbright: " + (fullbright.isEnabled() ? "§aAktiviert" : "§cDeaktiviert")));
                        }
                    }
                }
            }

            if (EzKeyBindings.KEY_WAYPOINT_MANAGER != null) {
                while (EzKeyBindings.KEY_WAYPOINT_MANAGER.consumeClick()) {
                    var waypoints = app.ezclient.gui.FeatureModule.get(app.ezclient.gui.WaypointsModule.class);
                    if (waypoints != null && client.player != null) {
                        if (EzScreenBridge.current(client) instanceof app.ezclient.gui.WaypointScreen) {
                            EzScreenBridge.set(client, null);
                        } else if (EzScreenBridge.current(client) == null) {
                            if (app.ezclient.gui.BlockSelectionOverlay.isActive()) {
                                app.ezclient.gui.BlockSelectionOverlay.cancel(client);
                            }
                            EzScreenBridge.set(client, new app.ezclient.gui.WaypointScreen(null, waypoints));
                        }
                    }
                }
            }

            if (EzKeyBindings.KEY_QUICK_WAYPOINT != null) {
                while (EzKeyBindings.KEY_QUICK_WAYPOINT.consumeClick()) {
                    var waypoints = app.ezclient.gui.FeatureModule.get(app.ezclient.gui.WaypointsModule.class);
                    if (waypoints != null && client.player != null && EzScreenBridge.current(client) == null && !app.ezclient.gui.BlockSelectionOverlay.isActive()) {
                        waypoints.quick(client);
                    }
                }
            }

            if (EzScreenBridge.current(client) == null && client.player != null && client.getWindow() != null) {
                for (app.ezclient.gui.Module m : ModuleManager.getInstance().getModules()) {
                    int k = m.getKeyBind();
                    if (k != -1 && !(m instanceof app.ezclient.gui.ZoomModule) && !(m instanceof app.ezclient.gui.FreelookModule) && !(m instanceof app.ezclient.gui.WaypointsModule) && !(m instanceof app.ezclient.gui.CoordinatesModule) && !(m instanceof app.ezclient.gui.FullbrightModule)) {
                        boolean down = isKeyOrMouseDown(client.getWindow(), k);
                        boolean wasDown = moduleKeyStates.getOrDefault(m.getName(), false);
                        if (down && !wasDown) {
                            m.toggle();
                            ConfigManager.save();
                            client.player.sendOverlayMessage(Component.literal("§e[EzClient] " + m.getDisplayName() + ": " + (m.isEnabled() ? "§aAktiviert" : "§cDeaktiviert")));
                        }
                        moduleKeyStates.put(m.getName(), down);
                    }
                }
            }
            boolean currentlyZooming = ModuleManager.getInstance().getZoomModule().isEnabled() && 
                                       ModuleManager.getInstance().getZoomModule().getKeyBind() != -1 && 
                                       client.getWindow() != null && 
                                       isKeyOrMouseDown(client.getWindow(), ModuleManager.getInstance().getZoomModule().getKeyBind());
            if (currentlyZooming && !isZooming) {
                ModuleManager.getInstance().getZoomModule().resetToDefault();
            }
            isZooming = currentlyZooming;
            
            // Enforce clean "EzClient" title & icon once upon window initialization, and bring Minecraft to focus
            if (!iconApplied && client.getWindow() != null) {
                long window = client.getWindow().handle();
                applyEarlyWindowProperties(window);
                try {
                    GLFW.glfwFocusWindow(window);
                    GLFW.glfwRequestWindowAttention(window);
                } catch (Throwable ignored) {}
                iconApplied = true;
            }
            
            app.ezclient.gui.KeystrokesModule.updateClicks(client);
            app.ezclient.gui.BlockSelectionOverlay.onClientTick(client);
            
            for (app.ezclient.gui.Module m : ModuleManager.getInstance().getModules()) {
                m.onTick();
            }

            try {
                if (client.getUser() != null && client.getUser().getProfileId() != null) {
                    CommunityPresence.heartbeat(client.getUser().getProfileId(), client.getUser().getName());
                } else if (client.player != null) {
                    CommunityPresence.heartbeat(client.player.getUUID(), client.player.getScoreboardName());
                }
            } catch (Throwable ignored) {}

            if (client.level == null || client.player == null) {
                app.ezclient.cosmetics.ThirdPartyPresence.clearPending();
            } else {
                CommunityCapeManager.tick(client);
                app.ezclient.cosmetics.ActiveSkinManager.tick(client);
            }
        });

        try {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
                app.ezclient.cosmetics.ThirdPartyPresence.clearPending();
                app.ezclient.cosmetics.CommunityPresence.clearOnline();
                CommunityCapeManager.clearSession();
            });
        } catch (Throwable ignored) {}

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("ezclient", "performance_hud"), (graphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;
            app.ezclient.gui.HudModule.beginRenderFrame(System.currentTimeMillis());
            for (var hud : ModuleManager.getInstance().getHudModules()) HudRenderer.draw(graphics, hud, false);
            app.ezclient.gui.WorldVisuals.renderWaypointsHud(graphics, client);
            app.ezclient.gui.BlockSelectionOverlay.renderHud(graphics, client);
        });

        // 1. Sync & update persistent client config in .ezclient/config/client_settings.json
        syncGlobalClientConfig();

        // 2. Apply optimized PvP & Sodium settings on first launch
        applyOptimizedSettings();

        // Window Daemon removed due to GLFW thread safety issues (moved to tick event)
        // No runtime screen suppression: third-party UI screens must never be
        // replaced while the player is in a world. Accessibility defaults are
        // configured through options.txt only.

        log("EzClient Core Mod initialized successfully!");
    }
    
    private static int tickCounter = 0;

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
                options.put("graphicsMode", "1");               // Fancy (1=fancy, transparent leaves)
                options.put("renderDistance", "8");             // 8 Chunks
                options.put("simulationDistance", "5");         // 5 Chunks
                options.put("entityShadows", "false");          // OFF
                options.put("clouds", "false");                 // OFF
                options.put("cloudStatus", "false");            // OFF
                options.put("particles", "2");                  // Minimal (2=minimal, 1=decreased, 0=all)
                options.put("biomeBlendRadius", "0");           // 0 (OFF)
                options.put("maxFps", "120");                   // Leave CPU/GPU headroom
                options.put("enableVsync", "false");            // VSync OFF
                options.put("onboardAccessibility", "false");   // Skip accessibility screen
                options.put("narrator", "0");                   // Narrator OFF (0)
                options.put("skipRealmsNotifications", "true"); // Skip notifications
                options.put("gamma", "1.0");                    // Brightness 100%
                options.put("smoothLighting", "false");         // Smooth Lighting OFF
                options.put("soundCategory_music", "0.05");     // 5% Music Volume

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
                            "    \"leaves_quality\": \"FANCY\",\n" +
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

    // startWindowDaemon removed

    public static void applyEarlyWindowProperties(long window) {
        if (window == 0L) return;
        try {
            GLFW.glfwSetWindowTitle(window, CLIENT_TITLE);
        } catch (Throwable ignored) {}
        applyWindowIcon(window);
    }

    /**
     * Creates and loads multi-resolution EzClient icons (16, 32, 48, 64, 128, 256) into GLFW.
     */
    public static void applyWindowIcon(long window) {
        if (window == 0L) return;
        try {
            java.io.InputStream is = EzClientMod.class.getResourceAsStream("/assets/ezclient/icon.png");
            if (is == null) {
                System.out.println("[EzClient] Could not find /assets/ezclient/icon.png in jar!");
                return;
            }
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);
            is.close();

            int[] targetSizes = new int[]{16, 32, 48, 64, 128, 256};
            GLFWImage.Buffer imageBuffer = GLFWImage.malloc(targetSizes.length);
            java.util.List<ByteBuffer> allocatedBuffers = new java.util.ArrayList<>();

            for (int idx = 0; idx < targetSizes.length; idx++) {
                int size = targetSizes[idx];
                java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(img, 0, 0, size, size, null);
                g.dispose();

                int[] pixelsRaw = scaled.getRGB(0, 0, size, size, null, 0, size);
                ByteBuffer pixels = MemoryUtil.memAlloc(size * size * 4);

                for (int p : pixelsRaw) {
                    pixels.put((byte) ((p >> 16) & 0xFF)); // R
                    pixels.put((byte) ((p >> 8) & 0xFF));  // G
                    pixels.put((byte) (p & 0xFF));         // B
                    pixels.put((byte) ((p >> 24) & 0xFF)); // A
                }
                pixels.flip();
                allocatedBuffers.add(pixels);

                imageBuffer.position(idx);
                imageBuffer.width(size);
                imageBuffer.height(size);
                imageBuffer.pixels(pixels);
            }
            imageBuffer.position(0);

            GLFW.glfwSetWindowIcon(window, imageBuffer);
            imageBuffer.free();
            for (ByteBuffer bb : allocatedBuffers) {
                MemoryUtil.memFree(bb);
            }
            System.out.println("[EzClient] Custom multi-resolution EzClient window icons loaded immediately!");
        } catch (Throwable t) {
            System.out.println("[EzClient] Note: Native icon set fallback handled: " + t.getMessage());
        }
    }

    private static void registerBuiltinResourcePacks() {
        try {
            net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("ezclient").ifPresent(container -> {
                net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
                    Identifier.fromNamespaceAndPath("ezclient", "glowing_ores"),
                    container,
                    net.minecraft.network.chat.Component.literal("EzClient Glowing Ores (OptiFine/Continuity)"),
                    net.fabricmc.fabric.api.resource.ResourcePackActivationType.NORMAL
                );
            });
        } catch (Throwable t) {
            log("Note: Built-in resource pack registration: " + t.getMessage());
        }
    }

    public static boolean isKeyOrMouseDown(com.mojang.blaze3d.platform.Window window, int code) {
        if (window == null || code == -1) return false;
        if (code <= -100) {
            int button = -code - 100;
            return GLFW.glfwGetMouseButton(window.handle(), button) == GLFW.GLFW_PRESS;
        }
        return InputConstants.isKeyDown(window, code);
    }
}
