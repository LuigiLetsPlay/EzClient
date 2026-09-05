package app.ezclient.v1_8;

import app.ezclient.v1_8.modules.CrosshairModule;
import app.ezclient.v1_8.modules.ModuleManager;
import net.minecraft.client.MinecraftClient;

/** HUD overlay for Legacy Minecraft (1.8.9) using OpenGL 1.1 pipeline. */
public final class HudRenderer {
    private HudRenderer() {}

    public static int getScaledWidth(MinecraftClient client) {
        if (client.currentScreen != null) return client.currentScreen.width;
        int scale = client.options.guiScale;
        if (scale == 0) scale = 1000;
        int scaleFactor = 1;
        while (scaleFactor < scale && client.width / (scaleFactor + 1) >= 320 && client.height / (scaleFactor + 1) >= 240) {
            scaleFactor++;
        }
        return (int) Math.ceil((double) client.width / (double) scaleFactor);
    }

    public static int getScaledHeight(MinecraftClient client) {
        if (client.currentScreen != null) return client.currentScreen.height;
        int scale = client.options.guiScale;
        if (scale == 0) scale = 1000;
        int scaleFactor = 1;
        while (scaleFactor < scale && client.width / (scaleFactor + 1) >= 320 && client.height / (scaleFactor + 1) >= 240) {
            scaleFactor++;
        }
        return (int) Math.ceil((double) client.height / (double) scaleFactor);
    }

    public static void render(float tickDelta) {
        // Evaluate key input every rendered frame
        EzClientMod_1_8.checkKeyInput();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options == null || client.options.debugEnabled) return;

        // Custom crosshair in first person view
        CrosshairModule crosshair = ModuleManager.getInstance().getCrosshairModule();
        if (crosshair.isEnabled() && client.options.perspective == 0 && client.currentScreen == null) {
            int sw = getScaledWidth(client);
            int sh = getScaledHeight(client);
            crosshair.renderCrosshair(sw, sh);
        }

        // Active modules HUD elements
        ModuleManager.getInstance().renderHud(tickDelta);
    }

    public static void render() {
        render(0.0F);
    }
}
