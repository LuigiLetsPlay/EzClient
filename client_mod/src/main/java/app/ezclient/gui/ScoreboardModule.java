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
    /** Ready-to-use layouts keep the first-run experience approachable. */
    public enum Preset {
        CLEAN("Clean"),
        COMPETITIVE("Competitive"),
        MINIMAL("Minimal"),
        VANILLA("Vanilla"),
        CUSTOM("Custom");

        private final String label;
        Preset(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** Controls the automatic screen anchor; manually positioned boards stay untouched. */
    public enum HorizontalAlignment {
        LEFT("Links"),
        CENTER("Mitte"),
        RIGHT("Rechts");

        private final String label;
        HorizontalAlignment(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

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
    private Preset preset = Preset.CLEAN;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.RIGHT;
    private boolean removeRedNumbers = true;
    private BackgroundStyle backgroundStyle = BackgroundStyle.TRANSLUCENT;
    private int customBackgroundColor = 0x60000000;
    private int backgroundAlpha = 100;
    private boolean textShadow = true;
    private boolean hideServerIpFooter = true;
    private boolean showTitle = true;
    private boolean showSeparator = true;
    private int padding = 4;
    private int extraLineSpacing = 0;
    private int maxEntries = 15;
    private int textColor = 0xFFFFFFFF;
    private int titleColor = 0xFFFFFFFF;
    private int separatorColor = 0x30FFFFFF;
    private boolean applyingPreset;

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
    public void setPosX(int posX) { this.posX = posX; changed(); }

    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; changed(); }

    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = Math.max(0.5, Math.min(2.0, scale)); changed(); }

    public Preset getPreset() { return preset; }
    public void setPreset(Preset preset) {
        this.preset = preset == null ? Preset.CUSTOM : preset;
        if (this.preset == Preset.CUSTOM) {
            ConfigManager.save();
            return;
        }
        applyingPreset = true;
        switch (this.preset) {
            case CLEAN -> applyPreset(BackgroundStyle.TRANSLUCENT, true, true, true, true, true, 4, 0, 15,
                    HorizontalAlignment.RIGHT, 0xFFFFFFFF, 0xFFFFFFFF, 0x30FFFFFF, 100);
            case COMPETITIVE -> applyPreset(BackgroundStyle.TRANSLUCENT, true, true, true, true, true, 5, 1, 15,
                    HorizontalAlignment.RIGHT, 0xFFFFFFFF, 0xFFFACC15, 0x5543DD8C, 100);
            case MINIMAL -> applyPreset(BackgroundStyle.INVISIBLE, true, false, true, false, false, 1, 0, 12,
                    HorizontalAlignment.RIGHT, 0xFFFFFFFF, 0xFFFFFFFF, 0x00000000, 100);
            case VANILLA -> applyPreset(BackgroundStyle.VANILLA, false, true, false, true, true, 4, 0, 15,
                    HorizontalAlignment.RIGHT, 0xFFFFFFFF, 0xFFFFFFFF, 0x30FFFFFF, 100);
            case CUSTOM -> { }
        }
        applyingPreset = false;
        ConfigManager.save();
    }

    private void applyPreset(BackgroundStyle style, boolean hideNumbers, boolean shadow, boolean hideFooter,
                             boolean title, boolean separator, int padding, int spacing, int entries,
                             HorizontalAlignment alignment, int text, int titleTint, int separatorTint, int alpha) {
        backgroundStyle = style;
        removeRedNumbers = hideNumbers;
        textShadow = shadow;
        hideServerIpFooter = hideFooter;
        showTitle = title;
        showSeparator = separator;
        this.padding = padding;
        extraLineSpacing = spacing;
        maxEntries = entries;
        horizontalAlignment = alignment;
        textColor = text;
        titleColor = titleTint;
        separatorColor = separatorTint;
        backgroundAlpha = alpha;
    }

    public HorizontalAlignment getHorizontalAlignment() { return horizontalAlignment; }
    public void setHorizontalAlignment(HorizontalAlignment value) {
        horizontalAlignment = value == null ? HorizontalAlignment.RIGHT : value;
        changed();
    }

    public boolean isRemoveRedNumbers() { return removeRedNumbers; }
    public void setRemoveRedNumbers(boolean removeRedNumbers) { this.removeRedNumbers = removeRedNumbers; changed(); }

    public BackgroundStyle getBackgroundStyle() { return backgroundStyle; }
    public void setBackgroundStyle(BackgroundStyle backgroundStyle) { this.backgroundStyle = backgroundStyle == null ? BackgroundStyle.TRANSLUCENT : backgroundStyle; changed(); }

    public int getCustomBackgroundColor() { return customBackgroundColor; }
    public void setCustomBackgroundColor(int customBackgroundColor) { this.customBackgroundColor = customBackgroundColor; changed(); }

    public int getBackgroundAlpha() { return backgroundAlpha; }
    public void setBackgroundAlpha(int backgroundAlpha) { this.backgroundAlpha = Math.max(0, Math.min(100, backgroundAlpha)); changed(); }

    public boolean isTextShadow() { return textShadow; }
    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; changed(); }

    public boolean isHideServerIpFooter() { return hideServerIpFooter; }
    public void setHideServerIpFooter(boolean hideServerIpFooter) { this.hideServerIpFooter = hideServerIpFooter; changed(); }

    public boolean isShowTitle() { return showTitle; }
    public void setShowTitle(boolean showTitle) { this.showTitle = showTitle; changed(); }

    public boolean isShowSeparator() { return showSeparator; }
    public void setShowSeparator(boolean showSeparator) { this.showSeparator = showSeparator; changed(); }

    public int getPadding() { return padding; }
    public void setPadding(int padding) { this.padding = Math.max(0, Math.min(12, padding)); changed(); }

    public int getExtraLineSpacing() { return extraLineSpacing; }
    public void setExtraLineSpacing(int extraLineSpacing) { this.extraLineSpacing = Math.max(0, Math.min(6, extraLineSpacing)); changed(); }

    public int getMaxEntries() { return maxEntries; }
    public void setMaxEntries(int maxEntries) { this.maxEntries = Math.max(1, Math.min(15, maxEntries)); changed(); }

    public int getTextColor() { return textColor; }
    public void setTextColor(int textColor) { this.textColor = textColor; changed(); }

    public int getTitleColor() { return titleColor; }
    public void setTitleColor(int titleColor) { this.titleColor = titleColor; changed(); }

    public int getSeparatorColor() { return separatorColor; }
    public void setSeparatorColor(int separatorColor) { this.separatorColor = separatorColor; changed(); }

    /** Central wording for the Settings HUD and hover tooltips. */
    public String getSettingDescription(String key) {
        return switch (key) {
            case "preset" -> "Wendet ein abgestimmtes Layout an. Eigene Änderungen wechseln zu Custom.";
            case "alignment" -> "Legt fest, an welcher Bildschirmkante ein nicht manuell positioniertes Scoreboard sitzt.";
            case "background" -> "Wählt den Hintergrundstil des Server-Scoreboards.";
            case "backgroundAlpha" -> "Steuert, wie transparent der Scoreboard-Hintergrund dargestellt wird.";
            case "numbers" -> "Blendet die roten Punktzahlen rechts neben den Einträgen aus oder ein.";
            case "title" -> "Zeigt oder versteckt die Überschrift des aktuellen Scoreboard-Ziels.";
            case "separator" -> "Zeigt eine dezente Trennlinie unter der Überschrift.";
            case "padding" -> "Verändert den Innenabstand zwischen Text und Hintergrund.";
            case "spacing" -> "Vergrößert den vertikalen Abstand zwischen zwei Scoreboard-Zeilen.";
            case "entries" -> "Begrenzt die Zahl der sichtbaren Einträge; niedrigere Werte halten das HUD kompakt.";
            case "text" -> "Legt die Standardfarbe für nicht bereits vom Server formatierte Texte fest.";
            case "shadow" -> "Fügt Textschatten für bessere Lesbarkeit auf hellen Hintergründen hinzu.";
            case "footer" -> "Versteckt typische Serveradressen und Werbung in der Scoreboard-Liste.";
            default -> "Passt das Erscheinungsbild des Scoreboards live an.";
        };
    }

    /** Restores only this module; no other HUD layout or module is changed. */
    public void resetSettings() {
        posX = -1;
        posY = -1;
        scale = 1.0;
        preset = Preset.CLEAN;
        applyingPreset = true;
        applyPreset(BackgroundStyle.TRANSLUCENT, true, true, true, true, true, 4, 0, 15,
                HorizontalAlignment.RIGHT, 0xFFFFFFFF, 0xFFFFFFFF, 0x30FFFFFF, 100);
        customBackgroundColor = 0x60000000;
        applyingPreset = false;
        ConfigManager.save();
    }

    private void changed() {
        // Loading a saved preset applies its stored fine-tuning immediately afterwards;
        // those values must not turn the selected preset into Custom during startup.
        if (!applyingPreset && !ConfigManager.isLoading()) preset = Preset.CUSTOM;
        ConfigManager.save();
    }

    /** Computes the final background once per board render and preserves custom RGBA colors. */
    public int getRenderedBackgroundColor() {
        int color = switch (backgroundStyle) {
            case VANILLA -> 0x60000000;
            case INVISIBLE -> 0x00000000;
            case TRANSLUCENT -> 0x4010141D;
            case CUSTOM -> customBackgroundColor;
        };
        int alpha = ((color >>> 24) * backgroundAlpha + 50) / 100;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

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

        // Limit early to keep busy server boards inexpensive to render.
        if (filtered.size() > maxEntries) {
            filtered = filtered.subList(filtered.size() - maxEntries, filtered.size());
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

        int boxW = maxW + padding * 2;
        int lineH = 9 + extraLineSpacing;
        int totalH = (entries.size() + (showTitle ? 1 : 0)) * lineH + padding * 2
                + (showTitle && showSeparator ? 2 : 0);

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        int scaledWidth = (int) Math.ceil(boxW * scale);
        int drawX = posX;
        if (drawX == -1) {
            drawX = switch (horizontalAlignment) {
                case LEFT -> 4;
                case CENTER -> Math.max(0, (screenW - scaledWidth) / 2);
                case RIGHT -> Math.max(0, screenW - scaledWidth - 4);
            };
        }
        int drawY = (posY == -1) ? Math.max(10, (screenH - totalH) / 2) : posY;

        graphics.pose().pushMatrix();
        graphics.pose().translate(drawX, drawY);
        graphics.pose().scale((float) scale, (float) scale);

        // Background
        int bg = getRenderedBackgroundColor();

        if ((bg & 0xFF000000) != 0) {
            EzUi.roundedRect(graphics, 0, 0, boxW, totalH, 1, bg);
        }

        int curY = padding;
        if (showTitle) {
            int titleX = (boxW - client.font.width(title)) / 2;
            graphics.text(client.font, title, titleX, curY, titleColor, textShadow);
            curY += lineH;
            if (showSeparator && (separatorColor >>> 24) != 0) {
                graphics.fill(padding, curY, boxW - padding, curY + 1, separatorColor);
                curY += 2;
            }
        }

        // Lines (drawn from top to bottom)
        for (int i = entries.size() - 1; i >= 0; i--) {
            EntryData data = entries.get(i);
            graphics.text(client.font, data.nameComp, padding, curY, textColor, textShadow);
            if (!removeRedNumbers) {
                int scoreX = boxW - padding - client.font.width(data.scoreStr);
                graphics.text(client.font, data.scoreStr, scoreX, curY, 0xFFFF4444, textShadow);
            }
            curY += lineH;
        }

        graphics.pose().popMatrix();
    }
}
