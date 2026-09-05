package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule extends Module {
    public PingModule() {
        super("ping", "Ping Display", "Shows your server latency in milliseconds", "HUD", true, 4, 200, true);
    }

    @Override
    public int getWidth() {
        String text = "Ping: 24ms";
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
        int ping = 0;
        if (client.player != null && client.getNetworkHandler() != null) {
            try {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) {
                    ping = entry.getLatency();
                }
            } catch (Throwable ignored) {}
        }

        String text = "Ping: " + ping + "ms";
        int textWidth = RenderUtils.getStringWidth(text);
        int x = getPosX();
        int y = getPosY();
        int color = getTextColor(0xFF55FFFF);

        if (isShowBackground()) {
            RenderUtils.drawRect(x - 2, y - 2, x + textWidth + 4, y + 10, 0x80000000);
        }
        RenderUtils.drawString(text, x + 1, y, color, true);
    }
}
