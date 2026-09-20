package app.ezclient.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.Objects;

/**
 * Ping & Server Info Display HUD Module:
 * Displays player network latency with configurable layout presets, server IP info,
 * online player count, server icon, update interval throttling, and latency color alerts.
 */
public final class PingModule extends HudModule {
    public enum DisplayLayout {
        LABEL_MS("Ping: %dms"),
        VALUE_ONLY("%dms"),
        SERVER_AND_PING("%s | %dms");

        private final String template;
        DisplayLayout(String template) { this.template = template; }
        public String getTemplate() { return template; }
    }

    private static final Identifier DEFAULT_SERVER_ICON = Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ezclient.png");
    private static FaviconTexture serverFavicon = null;
    private static byte[] lastFaviconBytes = null;
    private static String lastServerKey = null;

    private DisplayLayout displayLayout = DisplayLayout.LABEL_MS;
    private int updateIntervalSeconds = 1; // 1 to 10s
    private boolean pingAlert = true; // Yellow >100ms, Red >200ms
    private boolean showPlayerCount = false;
    private boolean showServerIcon = true;

    private int cachedPing = 0;
    private long lastFetchTime = 0L;

    public PingModule() {
        super("Ping", "HUD", true, 6, 22, "", "");
    }

    @Override
    public String getDescription() {
        return "Zeigt die Serverlatenz mit optionalem Server- und Spielerstatus an.";
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ping.png");
    }

    public DisplayLayout getDisplayLayout() { return displayLayout; }
    public void setDisplayLayout(DisplayLayout displayLayout) { this.displayLayout = displayLayout; ConfigManager.save(); }

    public int getUpdateIntervalSeconds() { return updateIntervalSeconds; }
    public void setUpdateIntervalSeconds(int updateIntervalSeconds) { this.updateIntervalSeconds = Math.max(1, Math.min(10, updateIntervalSeconds)); ConfigManager.save(); }

    public boolean isPingAlert() { return pingAlert; }
    public void setPingAlert(boolean pingAlert) { this.pingAlert = pingAlert; ConfigManager.save(); }

    public boolean isShowPlayerCount() { return showPlayerCount; }
    public void setShowPlayerCount(boolean showPlayerCount) { this.showPlayerCount = showPlayerCount; ConfigManager.save(); }

    public boolean isShowServerIcon() { return showServerIcon; }
    public void setShowServerIcon(boolean showServerIcon) { this.showServerIcon = showServerIcon; ConfigManager.save(); }

    private Identifier resolveServerIcon(Minecraft client) {
        if (!showServerIcon || client == null) return null;
        ServerData current = client.getCurrentServer();
        if (current != null) {
            byte[] bytes = current.getIconBytes();
            if (bytes == null) {
                try {
                    ServerList list = new ServerList(client);
                    list.load();
                    for (int i = 0; i < list.size(); i++) {
                        ServerData entry = list.get(i);
                        if (entry.ip.equalsIgnoreCase(current.ip) && entry.getIconBytes() != null) {
                            bytes = entry.getIconBytes();
                            current.setIconBytes(bytes);
                            break;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            if (bytes != null && bytes.length > 0) {
                if (serverFavicon == null || !Objects.equals(lastServerKey, current.ip) || !Arrays.equals(lastFaviconBytes, bytes)) {
                    try {
                        if (serverFavicon != null && !serverFavicon.isClosed()) {
                            serverFavicon.close();
                        }
                        serverFavicon = FaviconTexture.forServer(client.getTextureManager(), current.ip);
                        serverFavicon.upload(NativeImage.read(bytes));
                        lastFaviconBytes = bytes;
                        lastServerKey = current.ip;
                    } catch (Throwable ignored) {}
                }
                if (serverFavicon != null && !serverFavicon.isClosed()) {
                    return serverFavicon.textureLocation();
                }
            }
            return DEFAULT_SERVER_ICON;
        } else if (client.getSingleplayerServer() != null) {
            try {
                String levelName = client.getSingleplayerServer().getWorldData().getLevelName();
                if (serverFavicon == null || !Objects.equals(lastServerKey, "sp:" + levelName)) {
                    if (serverFavicon != null && !serverFavicon.isClosed()) {
                        serverFavicon.close();
                    }
                    serverFavicon = FaviconTexture.forWorld(client.getTextureManager(), levelName);
                    lastServerKey = "sp:" + levelName;
                }
                if (serverFavicon != null && !serverFavicon.isClosed()) {
                    return serverFavicon.textureLocation();
                }
            } catch (Throwable ignored) {}
            return DEFAULT_SERVER_ICON;
        }
        return DEFAULT_SERVER_ICON;
    }

    @Override
    protected String value(Minecraft client) {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime >= (updateIntervalSeconds * 1000L)) {
            lastFetchTime = now;
            if (client.player != null && client.getConnection() != null) {
                PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
                cachedPing = info == null ? 0 : Math.max(0, info.getLatency());
            } else {
                cachedPing = 0;
            }
        }

        String colorCode = "";
        if (pingAlert) {
            if (cachedPing > 200) colorCode = "§c";
            else if (cachedPing > 100) colorCode = "§e";
            else colorCode = "§a";
        }

        String baseText = switch (displayLayout) {
            case LABEL_MS -> "Ping: " + colorCode + cachedPing + "§rms";
            case VALUE_ONLY -> colorCode + cachedPing + "§rms";
            case SERVER_AND_PING -> {
                String serverName = "Singleplayer";
                ServerData server = client.getCurrentServer();
                if (server != null) {
                    serverName = server.name.isEmpty() ? server.ip : server.name;
                }
                yield serverName + " | " + colorCode + cachedPing + "§rms";
            }
        };

        if (showPlayerCount && client.getConnection() != null) {
            int count = client.getConnection().getOnlinePlayers().size();
            baseText += " §7(" + count + ")";
        }

        return baseText;
    }

    @Override
    public String displayText(Minecraft client) {
        return value(client);
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        if (!editor || (client != null && client.player != null)) {
            return displayText(client);
        }
        int p = 24;
        String colorCode = pingAlert ? "§a" : "";
        String base = switch (displayLayout) {
            case LABEL_MS -> "Ping: " + colorCode + p + "§rms";
            case VALUE_ONLY -> colorCode + p + "§rms";
            case SERVER_AND_PING -> "EzServer | " + colorCode + p + "§rms";
        };
        if (showPlayerCount) base += " §7(42)";
        return base;
    }

    @Override
    public int getWidth(Minecraft client) {
        return getWidth(client, false);
    }

    @Override
    public int getWidth(Minecraft client, boolean editor) {
        if (client == null || client.font == null) return 40;
        int pad = (hasBackground() || hasBorder()) ? CONTENT_PADDING_X : 2;
        int iconOffset = showServerIcon ? (14 + 4) : 0;
        return client.font.width(displayText(client, editor)) + iconOffset + pad * 2;
    }

    @Override
    public int getHeight(Minecraft client) {
        return getHeight(client, false);
    }

    @Override
    public int getHeight(Minecraft client, boolean editor) {
        int pad = (hasBackground() || hasBorder()) ? CONTENT_PADDING_Y : 1;
        return (showServerIcon ? 14 : 9) + pad * 2;
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        int totalW = getWidth(client, editor);
        int totalH = getHeight(client, editor);
        float scale = (float) getScale();
        int renderX = getRenderX(client, totalW, editor);
        int renderY = getRenderY(client, totalH, editor);

        graphics.pose().pushMatrix();
        graphics.pose().translate(renderX, renderY);
        graphics.pose().scale(scale, scale);

        renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);

        int padX = (hasBackground() || hasBorder()) ? CONTENT_PADDING_X : 2;
        int padY = (hasBackground() || hasBorder()) ? CONTENT_PADDING_Y : 1;

        Identifier icon = showServerIcon ? resolveServerIcon(client) : null;
        int iconSize = 14;
        int iconSpacing = 4;
        int textStartX = padX;

        if (icon != null) {
            int iconY = padY + (totalH - padY * 2 - iconSize) / 2;
            try {
                graphics.blit(RenderPipelines.GUI_TEXTURED, icon, padX, iconY, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
            } catch (Throwable ignored) {
                ModuleIconRenderer.drawTexture(graphics, icon, padX, iconY, iconSize);
            }
            textStartX = padX + iconSize + iconSpacing;
        }

        String text = displayText(client, editor);
        int textW = client.font.width(styledText(text));
        int availableW = totalW - padX - textStartX;
        int textX = textStartX + Math.max(0, (availableW - textW) / 2);
        int textY = padY + (totalH - padY * 2 - 9) / 2;

        graphics.text(client.font, styledText(text), textX, textY, color(), isTextShadow());

        graphics.pose().popMatrix();
    }
}
