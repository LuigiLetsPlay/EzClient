package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class CoordinatesModule extends Module {
    public CoordinatesModule() {
        super("coords", "Coordinates", "Displays XYZ coordinates and facing direction", "HUD", true, 4, 110, true);
    }

    @Override
    public int getWidth() {
        return 115;
    }

    @Override
    public int getHeight() {
        return 24;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int xPos = (int) Math.floor(client.player.x);
        int yPos = (int) Math.floor(client.player.y);
        int zPos = (int) Math.floor(client.player.z);

        int facing = MathHelper.floor((double) (client.player.yaw * 4.0F / 360.0F) + 0.5D) & 3;
        String dir = "South (+Z)";
        if (facing == 1) dir = "West (-X)";
        else if (facing == 2) dir = "North (-Z)";
        else if (facing == 3) dir = "East (+X)";

        String text1 = "XYZ: " + xPos + " / " + yPos + " / " + zPos;
        String text2 = "Facing: " + dir;

        int w1 = RenderUtils.getStringWidth(text1);
        int w2 = RenderUtils.getStringWidth(text2);
        int maxW = Math.max(w1, w2);

        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFFFFFFFF);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + maxW + 4, y + 20, 0x80000000);
        }
        RenderUtils.drawString(text1, x + 1, y, color, true);
        RenderUtils.drawString(text2, x + 1, y + 10, 0xFFAAAAAA, true);
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            renderHud(tickDelta);
        } else {
            String text1 = "XYZ: 100 / 64 / -200";
            String text2 = "Facing: North (-Z)";
            int w1 = RenderUtils.getStringWidth(text1);
            int w2 = RenderUtils.getStringWidth(text2);
            int maxW = Math.max(w1, w2);

            int x = getPosX();
            int y = getPosY();
            int color = getTextColor(0xFFFFFFFF);

            if (isShowBackground()) {
                RenderUtils.drawRect(x - 2, y - 2, x + maxW + 4, y + 20, 0x80000000);
            }
            RenderUtils.drawString(text1, x + 1, y, color, true);
            RenderUtils.drawString(text2, x + 1, y + 10, 0xFFAAAAAA, true);
        }
    }
}
