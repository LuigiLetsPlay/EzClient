package app.ezclient.v1_16_v1_20;

import app.ezclient.v1_16_v1_20.gui.EzHubScreen;
import app.ezclient.v1_16_v1_20.modules.ComboModule;
import app.ezclient.v1_16_v1_20.modules.CpsModule;
import app.ezclient.v1_16_v1_20.modules.ModuleManager;
import app.ezclient.v1_16_v1_20.modules.ReachModule;
import app.ezclient.shared.EzClientPaths;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

/** Full Modern Compatibility bootstrap for 1.16.5–1.20.1. */
public final class EzClientMod_1_16_1_20 implements ClientModInitializer {
    public static final String CLIENT_VERSION = "2.0.0";
    private static boolean lastRShiftState = false;
    private static boolean lastLeftMouseState = false;
    private static boolean lastRightMouseState = false;

    @Override
    public void onInitializeClient() {
        EzClientPaths.dataDirectory();
        ModuleManager.getInstance().loadConfig();
        System.out.println("[EzClient] Compatibility client " + CLIENT_VERSION + " initialized with Modern Full suite");
    }

    public static void onTick() {
        ModuleManager.getInstance().onTick();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            app.ezclient.v1_16_v1_20.cosmetics.CapeManager.onTick(client);
        }
    }

    public static float getZoomFovFactor() {
        return ModuleManager.getInstance().getZoomModule().getFovFactor();
    }

    public static void openScreen(MinecraftClient client, Screen screen) {
        if (client == null) return;
        //? if <=1.19.4 {
        /*client.openScreen(screen);
        *///?} else {
        client.setScreen(screen);
        //?}
    }

    public static void checkKeyInput() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        long window = client.getWindow().getHandle();

        // 1. Right Shift -> Open Modern Dashboard
        boolean currentRShift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (currentRShift && !lastRShiftState) {
            if (client.currentScreen == null) {
                openScreen(client, new EzHubScreen(null));
            } else if (client.currentScreen instanceof EzHubScreen) {
                openScreen(client, null);
            }
        }
        lastRShiftState = currentRShift;

        // 2. C key -> OptiFine-Style Zoom
        boolean cPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_C) == GLFW.GLFW_PRESS;
        ModuleManager.getInstance().getZoomModule().setZooming(cPressed && client.currentScreen == null);

        // 3. Mouse Clicks -> CPS Counter
        if (client.currentScreen == null) {
            boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (leftDown && !lastLeftMouseState) {
                CpsModule.recordLeftClick();
                // Check entity hit for Combo & Reach
                if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                    Entity target = ((EntityHitResult) client.crosshairTarget).getEntity();
                    if (target instanceof LivingEntity && client.player != null) {
                        ComboModule.onPlayerHit();
                        double dist = client.player.getPos().distanceTo(target.getPos());
                        ReachModule.recordReach(dist);
                    }
                }
            }
            lastLeftMouseState = leftDown;

            boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (rightDown && !lastRightMouseState) {
                CpsModule.recordRightClick();
            }
            lastRightMouseState = rightDown;
        }
    }
}
