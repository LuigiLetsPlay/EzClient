package app.ezclient.v1_16_v1_20;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;
import app.ezclient.v1_16_v1_20.modules.CrosshairModule;
import app.ezclient.v1_16_v1_20.modules.ModuleManager;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;

public final class HudRenderer {
    private static Field debugField = null;
    private static boolean debugInit = false;

    private HudRenderer() {}

    public static int getScaledWidth(MinecraftClient client) {
        if (client.getWindow() != null) return client.getWindow().getScaledWidth();
        return 800;
    }

    public static int getScaledHeight(MinecraftClient client) {
        if (client.getWindow() != null) return client.getWindow().getScaledHeight();
        return 600;
    }

    private static boolean isDebugEnabled(MinecraftClient client) {
        if (client.options == null) return false;
        if (!debugInit) {
            try {
                for (Field f : client.options.getClass().getDeclaredFields()) {
                    if (f.getName().toLowerCase().contains("debug")) {
                        debugField = f;
                        debugField.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}
            debugInit = true;
        }
        if (debugField != null) {
            try {
                return debugField.getBoolean(client.options);
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static void render(float tickDelta) {
        EzClientMod_1_16_1_20.checkKeyInput();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options == null || isDebugEnabled(client)) return;

        // Custom crosshair in first person view
        CrosshairModule crosshair = ModuleManager.getInstance().getCrosshairModule();
        if (crosshair.isEnabled() && client.options.getPerspective().isFirstPerson() && client.currentScreen == null) {
            int sw = getScaledWidth(client);
            int sh = getScaledHeight(client);
            crosshair.renderCrosshair(sw, sh);
        }

        // Active HUD elements
        ModuleManager.getInstance().renderHud(tickDelta);
    }

    public static void render() {
        render(0.0F);
    }
}
