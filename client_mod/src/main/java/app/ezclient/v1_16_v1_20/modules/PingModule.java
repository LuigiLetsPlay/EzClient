package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

public class PingModule extends Module {
    public PingModule() {
        super("ping", "Ping Display", "Shows your current network latency (ms)", "HUD", true, 4, 180, true);
    }

    private static int getPing(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) return 0;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    @Override
    public int getWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        int ping = getPing(client);
        return RenderUtils.getStringWidth("Ping: " + ping + "ms") + 6;
    }

    @Override
    public int getHeight() {
        return 14;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int ping = getPing(client);

        String text = "Ping: " + ping + "ms";
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
        String text = "Ping: 24ms";
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
