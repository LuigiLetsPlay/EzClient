package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;

public class DayCounterModule extends Module {
    public DayCounterModule() {
        super("days", "Day Counter", "Displays the current in-game day of the world", "HUD", false, 4, 336, true);
    }

    private static long getWorldTime(Object world) {
        if (world == null) return 0L;
        try {
            for (Method m : world.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("time") && m.getParameterTypes().length == 0 && (m.getReturnType() == long.class || m.getReturnType() == Long.class)) {
                    return ((Number) m.invoke(world)).longValue();
                }
            }
        } catch (Throwable ignored) {}
        try {
            Method m = world.getClass().getMethod("getLevelProperties");
            Object props = m.invoke(world);
            if (props != null) {
                for (Method pm : props.getClass().getMethods()) {
                    if (pm.getName().toLowerCase().contains("time") && pm.getParameterTypes().length == 0 && (pm.getReturnType() == long.class || pm.getReturnType() == Long.class)) {
                        return ((Number) pm.invoke(props)).longValue();
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 0L;
    }

    @Override
    public int getWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        long day = client.world != null ? getWorldTime(client.world) / 24000L : 1L;
        String text = "Day: " + day;
        return RenderUtils.getStringWidth(text) + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        long day = getWorldTime(client.world) / 24000L;
        String text = "Day: " + day;
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFFFF55);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        String text = "Day: 42";
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFFFF55);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
