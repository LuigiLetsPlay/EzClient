package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FpsModule extends Module {
    private static Field currentFpsField = null;
    private static Method getFpsMethod = null;
    private static boolean reflectionInit = false;

    public FpsModule() {
        super("fps", "FPS Display", "Shows your current frames per second", "HUD", true, 4, 4, true);
    }

    private static int getCurrentFps(MinecraftClient client) {
        if (!reflectionInit) {
            try {
                for (Field f : MinecraftClient.class.getDeclaredFields()) {
                    if (f.getType() == int.class && f.getName().toLowerCase().contains("fps")) {
                        currentFpsField = f;
                        currentFpsField.setAccessible(true);
                        break;
                    }
                }
                for (Method m : MinecraftClient.class.getDeclaredMethods()) {
                    if (m.getReturnType() == int.class && m.getParameterTypes().length == 0 && m.getName().toLowerCase().contains("fps")) {
                        getFpsMethod = m;
                        getFpsMethod.setAccessible(true);
                        break;
                    }
                }
            } catch (Throwable ignored) {}
            reflectionInit = true;
        }

        if (currentFpsField != null) {
            try {
                return currentFpsField.getInt(client);
            } catch (Throwable ignored) {}
        }
        if (getFpsMethod != null) {
            try {
                return ((Number) getFpsMethod.invoke(client)).intValue();
            } catch (Throwable ignored) {}
        }
        return 60;
    }

    @Override
    public int getWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        int fps = getCurrentFps(client);
        return RenderUtils.getStringWidth("FPS: " + fps) + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int fps = getCurrentFps(client);

        String text = "FPS: " + fps;
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFF55FF55);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        String text = "FPS: 240";
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFF55FF55);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
