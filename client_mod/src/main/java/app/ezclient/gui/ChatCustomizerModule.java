package app.ezclient.gui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Chat Customizer / Tweaks Module:
 * Enhances Minecraft's native chat with:
 * - Adjustable background opacity (or fully invisible)
 * - Increased chat history up to 10,000 lines
 * - Timestamps ([HH:mm] / [HH:mm:ss])
 * - Click-to-copy chat text
 * - Moveable HUD position & realistic dummy preview
 */
public final class ChatCustomizerModule extends HudModule {
    public enum TimestampFormat {
        NONE("Off"),
        HH_MM("[HH:mm]"),
        HH_MM_SS("[HH:mm:ss]");

        private final String label;
        TimestampFormat(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static final DateTimeFormatter FMT_HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimestampFormat timestampFormat = TimestampFormat.HH_MM;
    private int backgroundOpacity = 50; // 0 to 100%
    private int lineLimit = 5000; // 100 to 10000
    private boolean copyOnClick = true;

    public ChatCustomizerModule() {
        super("Chat Customizer", "HUD", false, 4, 140, "", "");
        setBackground(false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/chat.png");
    }

    @Override
    public String getDescription() {
        return "Erweitert den Chat mit Zeitstempeln, anpassbarer Hintergrund-Deckkraft und Klick-zum-Kopieren.";
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    protected String value(Minecraft client) {
        return "";
    }

    @Override
    public int getWidth(Minecraft client, boolean editor) {
        if (client != null && client.options != null && client.options.chatWidth() != null) {
            return net.minecraft.client.gui.components.ChatComponent.getWidth(client.options.chatWidth().get()) + 8;
        }
        return 328;
    }

    @Override
    public int getWidth(Minecraft client) {
        return getWidth(client, false);
    }

    @Override
    public int getHeight(Minecraft client, boolean editor) {
        if (client == null || client.options == null) return 36;
        double spacing = client.options.chatLineSpacing().get();
        int lineHeight = (int) Math.round(9.0 * (spacing + 1.0));
        return 4 * lineHeight;
    }

    @Override
    public int getHeight(Minecraft client) {
        return getHeight(client, false);
    }

    public TimestampFormat getTimestampFormat() { return timestampFormat; }
    public void setTimestampFormat(TimestampFormat timestampFormat) {
        this.timestampFormat = timestampFormat;
        if (dummyChat != null) populateDummyMessages(dummyChat);
        ConfigManager.save();
    }

    public int getBackgroundOpacity() { return backgroundOpacity; }
    public void setBackgroundOpacity(int backgroundOpacity) { this.backgroundOpacity = Math.max(0, Math.min(100, backgroundOpacity)); ConfigManager.save(); }

    public int getLineLimit() { return lineLimit; }
    public void setLineLimit(int lineLimit) { this.lineLimit = Math.max(100, Math.min(10000, lineLimit)); ConfigManager.save(); }

    public boolean isCopyOnClick() { return copyOnClick; }
    public void setCopyOnClick(boolean copyOnClick) { this.copyOnClick = copyOnClick; ConfigManager.save(); }

    public Component appendTimestamp(Component original) {
        if (!isEnabled() || timestampFormat == TimestampFormat.NONE) return original;

        LocalTime now = LocalTime.now();
        String stamp = switch (timestampFormat) {
            case HH_MM -> "[" + now.format(FMT_HH_MM) + "] ";
            case HH_MM_SS -> "[" + now.format(FMT_HH_MM_SS) + "] ";
            default -> "";
        };

        return Component.literal("§8" + stamp + "§r").append(original);
    }

    private static net.minecraft.client.gui.components.ChatComponent dummyChat;

    public static net.minecraft.client.gui.components.ChatComponent getDummyChat(Minecraft mc) {
        if (dummyChat == null && mc != null) {
            dummyChat = new net.minecraft.client.gui.components.ChatComponent(mc);
            populateDummyMessages(dummyChat);
        }
        return dummyChat;
    }

    public static boolean isDummyChat(net.minecraft.client.gui.components.ChatComponent chat) {
        return dummyChat != null && dummyChat == chat;
    }

    public static void populateDummyMessages(net.minecraft.client.gui.components.ChatComponent chat) {
        chat.clearMessages(false);
        chat.addClientSystemMessage(Component.literal("§6[Server] §eDas Spiel beginnt in 10 Sekunden."));
        chat.addClientSystemMessage(Component.literal("§f<§aPlayer1§f> gg everyone!"));
        chat.addClientSystemMessage(Component.literal("§f<§bLu1giLP§f> viel Erfolg allen!"));
        chat.addClientSystemMessage(Component.literal("§f<§cSteve§f> GL HF!"));
    }

    public static void renderDummyChat(GuiGraphicsExtractor g, Minecraft mc, ChatCustomizerModule chat, int x, int y, int width, double scale) {
        if (mc == null) return;
        var dummy = getDummyChat(mc);
        if (dummy == null) return;

        int guiH = g.guiHeight();
        double spacing = (mc.options != null && mc.options.chatLineSpacing() != null) ? mc.options.chatLineSpacing().get() : 0.0;
        int lineHeight = (int) Math.round(9.0 * (spacing + 1.0));
        int totalH = 4 * lineHeight;
        int defY = guiH - 40 - totalH;

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        if (scale != 1.0) {
            g.pose().scale((float) scale, (float) scale);
        }
        g.pose().translate(-4, -defY);

        int tickCount = mc.player != null ? mc.player.tickCount : 0;
        dummy.extractRenderState(g, mc.font, tickCount, 0, 0, net.minecraft.client.gui.components.ChatComponent.DisplayMode.BACKGROUND, false);
        dummy.extractRenderState(g, mc.font, tickCount, 0, 0, net.minecraft.client.gui.components.ChatComponent.DisplayMode.FOREGROUND, false);

        g.pose().popMatrix();
    }

    public void renderCustom(GuiGraphicsExtractor g, Minecraft mc, boolean editor) {
        if (editor) {
            renderDummyChat(g, mc, this, getX(), getY(), getWidth(mc, true), getScale());
        }
    }
}
