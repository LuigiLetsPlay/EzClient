package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Scoreboard Customizer Module:
 * Full customization over Minecraft's server sidebar scoreboard:
 * - Position & Scale
 * - Toggle removal of red numbers on the right
 * - Transparent / Invisible / Custom background styles
 * - Text drop shadow toggle
 * - Server IP / Advertisement footer hiding
 */
public final class ScoreboardModule extends Module {
    public enum BackgroundStyle {
        VANILLA("Vanilla"),
        INVISIBLE("Invisible"),
        TRANSLUCENT("Clean Dark"),
        CUSTOM("Custom RGBA");

        private final String label;
        BackgroundStyle(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static final Pattern SERVER_IP_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://)?(?:[a-zA-Z0-9-]+\\.)+(?:com|net|org|de|io|me|eu|gg|to|xyz|tv)\\b"
    );

    private int posX = -1;
    private int posY = -1;
    private double scale = 1.0;
    private boolean removeRedNumbers = true;
    private BackgroundStyle backgroundStyle = BackgroundStyle.TRANSLUCENT;
    private int customBackgroundColor = 0x60000000;
    private boolean textShadow = true;
    private boolean hideServerIpFooter = true;

    public ScoreboardModule() {
        super("Scoreboard", "HUD", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/coordinates.png");
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; ConfigManager.save(); }

    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; ConfigManager.save(); }

    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = Math.max(0.5, Math.min(2.0, scale)); ConfigManager.save(); }

    public boolean isRemoveRedNumbers() { return removeRedNumbers; }
    public void setRemoveRedNumbers(boolean removeRedNumbers) { this.removeRedNumbers = removeRedNumbers; ConfigManager.save(); }

    public BackgroundStyle getBackgroundStyle() { return backgroundStyle; }
    public void setBackgroundStyle(BackgroundStyle backgroundStyle) { this.backgroundStyle = backgroundStyle; ConfigManager.save(); }

    public int getCustomBackgroundColor() { return customBackgroundColor; }
    public void setCustomBackgroundColor(int customBackgroundColor) { this.customBackgroundColor = customBackgroundColor; ConfigManager.save(); }

    public boolean isTextShadow() { return textShadow; }
    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; ConfigManager.save(); }

    public boolean isHideServerIpFooter() { return hideServerIpFooter; }
    public void setHideServerIpFooter(boolean hideServerIpFooter) { this.hideServerIpFooter = hideServerIpFooter; ConfigManager.save(); }

    public void renderCustomScoreboard(GuiGraphicsExtractor graphics, Minecraft client, Objective objective) {
        if (objective == null || client.font == null) return;

        Scoreboard scoreboard = objective.getScoreboard();
        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective);
        if (scores.isEmpty()) return;

        List<PlayerScoreEntry> filtered = new ArrayList<>();
        for (PlayerScoreEntry entry : scores) {
            if (entry.isHidden()) continue;
            String name = entry.owner();
            if (hideServerIpFooter && (SERVER_IP_PATTERN.matcher(name).find() || name.contains("www.") || name.contains(".net") || name.contains(".com"))) {
                continue;
            }
            filtered.add(entry);
        }

        if (filtered.isEmpty()) return;

        // Limit to max 15 lines like vanilla
        if (filtered.size() > 15) {
            filtered = filtered.subList(filtered.size() - 15, filtered.size());
        }

        Component title = objective.getDisplayName();
        int maxW = client.font.width(title);

        record EntryData(Component nameComp, String scoreStr, int nameW, int scoreW) {}
        List<EntryData> entries = new ArrayList<>();

        for (PlayerScoreEntry entry : filtered) {
            Component nameComp = entry.display() != null ? entry.display() : PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
            String scoreStr = String.valueOf(entry.value());
            int nW = client.font.width(nameComp);
            int sW = removeRedNumbers ? 0 : client.font.width(scoreStr) + 6;
            maxW = Math.max(maxW, nW + sW);
            entries.add(new EntryData(nameComp, scoreStr, nW, sW));
        }

        int padding = 4;
        int boxW = maxW + padding * 2;
        int lineH = 9;
        int totalH = (entries.size() + 1) * lineH + padding * 2;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        int drawX = (posX == -1) ? (screenW - boxW - 4) : posX;
        int drawY = (posY == -1) ? Math.max(10, (screenH - totalH) / 2) : posY;

        graphics.pose().pushMatrix();
        graphics.pose().translate(drawX, drawY);
        graphics.pose().scale((float) scale, (float) scale);

        // Background
        int bg = switch (backgroundStyle) {
            case VANILLA -> 0x60000000;
            case INVISIBLE -> 0x00000000;
            case TRANSLUCENT -> 0x4010141D;
            case CUSTOM -> customBackgroundColor;
        };

        if ((bg & 0xFF000000) != 0) {
            EzUi.roundedRect(graphics, 0, 0, boxW, totalH, 3, bg);
        }

        // Title
        int titleX = (boxW - client.font.width(title)) / 2;
        graphics.text(client.font, title, titleX, padding, 0xFFFFFFFF, textShadow);

        // Separator line
        if (backgroundStyle != BackgroundStyle.INVISIBLE) {
            graphics.fill(padding, padding + lineH - 1, boxW - padding, padding + lineH, 0x30FFFFFF);
        }

        // Lines (drawn from top to bottom)
        int curY = padding + lineH + 2;
        for (int i = entries.size() - 1; i >= 0; i--) {
            EntryData data = entries.get(i);
            graphics.text(client.font, data.nameComp, padding, curY, 0xFFFFFFFF, textShadow);
            if (!removeRedNumbers) {
                int scoreX = boxW - padding - client.font.width(data.scoreStr);
                graphics.text(client.font, data.scoreStr, scoreX, curY, 0xFFFF4444, textShadow);
            }
            curY += lineH;
        }

        graphics.pose().popMatrix();
    }
}
