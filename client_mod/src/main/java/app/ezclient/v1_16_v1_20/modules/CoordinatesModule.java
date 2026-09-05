package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;

public class CoordinatesModule extends Module {
    public CoordinatesModule() {
        super("coords", "Coordinates", "Displays your XYZ coordinates and facing direction", "HUD", true, 4, 100, true);
    }

    private static String getFacing(float yaw) {
        yaw = yaw % 360.0F;
        if (yaw < 0.0F) yaw += 360.0F;
        if (yaw >= 315.0F || yaw < 45.0F) return "S (+Z)";
        if (yaw >= 45.0F && yaw < 135.0F) return "W (-X)";
        if (yaw >= 135.0F && yaw < 225.0F) return "N (-Z)";
        return "E (+X)";
    }

    private static float getPlayerYaw(MinecraftClient client) {
        if (client.player == null) return 0.0F;
        //? if <=1.19.4 {
        /*return client.player.yaw;
        *///?} else {
        return client.player.getYaw();
        //?}
    }

    @Override
    public int getWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 110;
        int x = (int) Math.floor(client.player.getX());
        int y = (int) Math.floor(client.player.getY());
        int z = (int) Math.floor(client.player.getZ());
        String text = "XYZ: " + x + ", " + y + ", " + z + " (" + getFacing(getPlayerYaw(client)) + ")";
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
        if (client.player == null) return;

        int px = (int) Math.floor(client.player.getX());
        int py = (int) Math.floor(client.player.getY());
        int pz = (int) Math.floor(client.player.getZ());
        String text = "XYZ: " + px + ", " + py + ", " + pz + " (" + getFacing(getPlayerYaw(client)) + ")";

        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFFAA00);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        String text = "XYZ: 100, 64, -250 (S (+Z))";
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFFAA00);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
