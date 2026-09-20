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
public final class ScoreboardModule extends FeatureModule {
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

    private boolean applyingPreset = false;

    public ScoreboardModule() {
        super("Scoreboard", false, 0);

        option("Vorlagen & Ausrichtung", "preset", "Layout-Vorlage", "Wähle ein vorkonfiguriertes Scoreboard-Design.", "Clean", 0, 0, "Clean", "Competitive", "Minimal", "Vanilla", "Custom");
        option("Vorlagen & Ausrichtung", "horizontalAlignment", "Ausrichtung", "Horizontale Positionierung der Anzeigetafel auf dem Bildschirm.", "Rechts", 0, 0, "Links", "Mitte", "Rechts");

        flag("Anzeige-Elemente", "removeRedNumbers", "Rote Zahlen verbergen", "Entfernt die roten Zahlenwerte auf der rechten Seite des Scoreboards.", true);
        flag("Anzeige-Elemente", "hideServerIpFooter", "Server-IP verbergen", "Blendet Fußzeilen mit Server-IPs und Webseiten aus.", true);
        flag("Anzeige-Elemente", "showTitle", "Titel anzeigen", "Blendet die oberste Titelzeile des Scoreboards ein oder aus.", true);
        flag("Anzeige-Elemente", "showSeparator", "Trennlinie anzeigen", "Zeigt eine optische Trennlinie unterhalb des Titels.", true);
        flag("Anzeige-Elemente", "textShadow", "Textschatten", "Fügt allen Texten einen dezenten Schatteneffekt hinzu.", true);

        option("Layout & Abstände", "maxEntries", "Maximale Einträge", "Maximale Anzahl an sichtbaren Zeilen auf der Anzeigetafel.", 15.0, 5.0, 15.0);
        option("Layout & Abstände", "padding", "Randabstand (Padding)", "Innerer Abstand zwischen Text und Rahmen des Scoreboards.", 4.0, 0.0, 12.0);
        option("Layout & Abstände", "extraLineSpacing", "Zeilenabstand", "Zusätzlicher vertikaler Abstand zwischen den Textzeilen.", 0.0, 0.0, 6.0);

        option("Farben & Hintergrund", "backgroundStyle", "Hintergrund-Stil", "Wählt den Darstellungsstil des Hintergrundkastens.", "Clean Dark", 0, 0, "Vanilla", "Invisible", "Clean Dark", "Custom RGBA");
        option("Farben & Hintergrund", "backgroundAlpha", "Hintergrund-Deckkraft (%)", "Transparenz des Scoreboard-Hintergrunds.", 100.0, 0.0, 100.0);
        colorOption("Farben & Hintergrund", "textColor", "Textfarbe", "Farbe des normalen Scoreboard-Inhalts.", "#FFFFFFFF");
        colorOption("Farben & Hintergrund", "titleColor", "Titelfarbe", "Farbe der Überschrift des Scoreboards.", "#FFFFFFFF");
        colorOption("Farben & Hintergrund", "separatorColor", "Trennlinienfarbe", "Farbe der Trennlinie unter dem Titel.", "#30FFFFFF");
        colorOption("Farben & Hintergrund", "customBackgroundColor", "Eigene Hintergrundfarbe", "Hintergrundfarbe im Modus 'Custom RGBA'.", "#60000000");
    }

    @Override
    public boolean set(Option option, Object value) {
        boolean ok = super.set(option, value);
        if (ok && !applyingPreset) {
            if ("preset".equals(option.key())) {
                onPresetChanged(text("preset"));
            } else if (!ConfigManager.isLoading()) {
                set("preset", "Custom");
            }
        }
        return ok;
    }

    private void onPresetChanged(String presetName) {
        Preset p = Preset.CUSTOM;
        for (Preset pr : Preset.values()) {
            if (pr.getLabel().equalsIgnoreCase(presetName)) { p = pr; break; }
        }
        applyingPreset = true;
        switch (p) {
            case CLEAN -> applyPresetValues("Clean Dark", true, true, true, true, true, 4.0, 0.0, 15.0,
                    "Rechts", "#FFFFFFFF", "#FFFFFFFF", "#30FFFFFF", 100.0);
            case COMPETITIVE -> applyPresetValues("Clean Dark", true, true, true, true, true, 5.0, 1.0, 15.0,
                    "Rechts", "#FFFFFFFF", "#FFFACC15", "#5543DD8C", 100.0);
            case MINIMAL -> applyPresetValues("Invisible", true, false, true, false, false, 1.0, 0.0, 12.0,
                    "Rechts", "#FFFFFFFF", "#FFFFFFFF", "#00000000", 100.0);
            case VANILLA -> applyPresetValues("Vanilla", false, true, false, true, true, 4.0, 0.0, 15.0,
                    "Rechts", "#FFFFFFFF", "#FFFFFFFF", "#30FFFFFF", 100.0);
            case CUSTOM -> { }
        }
        applyingPreset = false;
        ConfigManager.save();
    }

    private void applyPresetValues(String style, boolean hideNumbers, boolean shadow, boolean hideFooter,
                                   boolean title, boolean separator, double padding, double spacing, double entries,
                                   String alignment, String text, String titleTint, String separatorTint, double alpha) {
        set("backgroundStyle", style);
        set("removeRedNumbers", hideNumbers);
        set("textShadow", shadow);
        set("hideServerIpFooter", hideFooter);
        set("showTitle", title);
        set("showSeparator", separator);
        set("padding", padding);
        set("extraLineSpacing", spacing);
        set("maxEntries", entries);
        set("horizontalAlignment", alignment);
        set("textColor", text);
        set("titleColor", titleTint);
        set("separatorColor", separatorTint);
        set("backgroundAlpha", alpha);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/coordinates.png");
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public int getPosX() { return getX(); }
    public void setPosX(int posX) { setPosition(posX, getY()); ConfigManager.save(); }

    public int getPosY() { return getY(); }
    public void setPosY(int posY) { setPosition(getX(), posY); ConfigManager.save(); }

    public Preset getPreset() {
        String p = text("preset");
        for (Preset pr : Preset.values()) {
            if (pr.getLabel().equalsIgnoreCase(p)) return pr;
        }
        return Preset.CUSTOM;
    }
    public void setPreset(Preset preset) {
        set("preset", preset == null ? "Custom" : preset.getLabel());
        ConfigManager.save();
    }

    public HorizontalAlignment getHorizontalAlignment() {
        String a = text("horizontalAlignment");
        for (HorizontalAlignment ha : HorizontalAlignment.values()) {
            if (ha.getLabel().equalsIgnoreCase(a)) return ha;
        }
        return HorizontalAlignment.RIGHT;
    }
    public void setHorizontalAlignment(HorizontalAlignment value) {
        set("horizontalAlignment", value == null ? "Rechts" : value.getLabel());
        ConfigManager.save();
    }

    public boolean isRemoveRedNumbers() { return flag("removeRedNumbers"); }
    public void setRemoveRedNumbers(boolean removeRedNumbers) {
        set("removeRedNumbers", removeRedNumbers);
        ConfigManager.save();
    }

    public BackgroundStyle getBackgroundStyle() {
        String b = text("backgroundStyle");
        for (BackgroundStyle bs : BackgroundStyle.values()) {
            if (bs.getLabel().equalsIgnoreCase(b)) return bs;
        }
        return BackgroundStyle.TRANSLUCENT;
    }
    public void setBackgroundStyle(BackgroundStyle backgroundStyle) {
        set("backgroundStyle", backgroundStyle == null ? "Clean Dark" : backgroundStyle.getLabel());
        ConfigManager.save();
    }

    public int getCustomBackgroundColor() { return tint("customBackgroundColor", false); }
    public void setCustomBackgroundColor(int customBackgroundColor) {
        set("customBackgroundColor", String.format("#%08X", customBackgroundColor));
        ConfigManager.save();
    }

    public int getBackgroundAlpha() { return (int) Math.round(number("backgroundAlpha")); }
    public void setBackgroundAlpha(int backgroundAlpha) {
        set("backgroundAlpha", (double) Math.max(0, Math.min(100, backgroundAlpha)));
        ConfigManager.save();
    }

    public boolean isTextShadow() { return flag("textShadow"); }
    public void setTextShadow(boolean textShadow) {
        set("textShadow", textShadow);
        ConfigManager.save();
    }

    public boolean isHideServerIpFooter() { return flag("hideServerIpFooter"); }
    public void setHideServerIpFooter(boolean hideServerIpFooter) {
        set("hideServerIpFooter", hideServerIpFooter);
        ConfigManager.save();
    }

    public boolean isShowTitle() { return flag("showTitle"); }
    public void setShowTitle(boolean showTitle) {
        set("showTitle", showTitle);
        ConfigManager.save();
    }

    public boolean isShowSeparator() { return flag("showSeparator"); }
    public void setShowSeparator(boolean showSeparator) {
        set("showSeparator", showSeparator);
        ConfigManager.save();
    }

    public int getPadding() { return (int) Math.round(number("padding")); }
    public void setPadding(int padding) {
        set("padding", (double) Math.max(0, Math.min(12, padding)));
        ConfigManager.save();
    }

    public int getExtraLineSpacing() { return (int) Math.round(number("extraLineSpacing")); }
    public void setExtraLineSpacing(int extraLineSpacing) {
        set("extraLineSpacing", (double) Math.max(0, Math.min(6, extraLineSpacing)));
        ConfigManager.save();
    }

    public int getMaxEntries() { return (int) Math.round(number("maxEntries")); }
    public void setMaxEntries(int maxEntries) {
        set("maxEntries", (double) Math.max(1, Math.min(15, maxEntries)));
        ConfigManager.save();
    }

    public int getTextColor() { return tint("textColor", false); }
    public void setTextColor(int textColor) {
        set("textColor", String.format("#%08X", textColor));
        ConfigManager.save();
    }

    public int getTitleColor() { return tint("titleColor", false); }
    public void setTitleColor(int titleColor) {
        set("titleColor", String.format("#%08X", titleColor));
        ConfigManager.save();
    }

    public int getSeparatorColor() { return tint("separatorColor", false); }
    public void setSeparatorColor(int separatorColor) {
        set("separatorColor", String.format("#%08X", separatorColor));
        ConfigManager.save();
    }

    @Override
    public void resetSettings() {
        super.resetSettings();
        setPosX(-1);
        setPosY(-1);
        setScale(1.0);
        onPresetChanged("Clean");
    }

    /** Computes the final background once per board render and preserves custom RGBA colors. */
    public int getRenderedBackgroundColor() {
        int color = switch (getBackgroundStyle()) {
            case VANILLA -> 0x60000000;
            case INVISIBLE -> 0x00000000;
            case TRANSLUCENT -> 0x4010141D;
            case CUSTOM -> getCustomBackgroundColor();
        };
        int alpha = ((color >>> 24) * getBackgroundAlpha() + 50) / 100;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void renderCustomScoreboard(GuiGraphicsExtractor graphics, Minecraft client, Objective objective) {
        if (objective == null || client.font == null) return;

        Scoreboard scoreboard = objective.getScoreboard();
        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective);
        if (scores.isEmpty()) return;

        List<PlayerScoreEntry> filtered = new ArrayList<>();
        boolean hideFooter = isHideServerIpFooter();
        for (PlayerScoreEntry entry : scores) {
            if (entry.isHidden()) continue;
            String name = entry.owner();
            if (hideFooter && (SERVER_IP_PATTERN.matcher(name).find() || name.contains("www.") || name.contains(".net") || name.contains(".com"))) {
                continue;
            }
            filtered.add(entry);
        }

        if (filtered.isEmpty()) return;

        // Limit early to keep busy server boards inexpensive to render.
        int maxE = getMaxEntries();
        if (filtered.size() > maxE) {
            filtered = filtered.subList(filtered.size() - maxE, filtered.size());
        }

        Component title = objective.getDisplayName();
        int maxW = client.font.width(title);

        record EntryData(Component nameComp, String scoreStr, int nameW, int scoreW) {}
        List<EntryData> entries = new ArrayList<>();

        boolean hideRed = isRemoveRedNumbers();
        for (PlayerScoreEntry entry : filtered) {
            Component nameComp = entry.display() != null ? entry.display() : PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
            String scoreStr = String.valueOf(entry.value());
            int nW = client.font.width(nameComp);
            int sW = hideRed ? 0 : client.font.width(scoreStr) + 6;
            maxW = Math.max(maxW, nW + sW);
            entries.add(new EntryData(nameComp, scoreStr, nW, sW));
        }

        int pad = getPadding();
        int lineH = 9 + getExtraLineSpacing();
        boolean hasTitle = isShowTitle();
        boolean hasSeparator = isShowSeparator();
        int totalH = (entries.size() + (hasTitle ? 1 : 0)) * lineH + pad * 2
                + (hasTitle && hasSeparator ? 2 : 0);
        int boxW = maxW + pad * 2;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        double currentScale = getScale();
        int scaledWidth = (int) Math.ceil(boxW * currentScale);
        int drawX = getPosX();
        if (drawX == -1) {
            drawX = switch (getHorizontalAlignment()) {
                case LEFT -> 4;
                case CENTER -> Math.max(0, (screenW - scaledWidth) / 2);
                case RIGHT -> Math.max(0, screenW - scaledWidth - 4);
            };
        }
        int drawY = (getPosY() == -1) ? Math.max(10, (screenH - totalH) / 2) : getPosY();

        graphics.pose().pushMatrix();
        graphics.pose().translate(drawX, drawY);
        graphics.pose().scale((float) currentScale, (float) currentScale);

        // Background
        int bg = getRenderedBackgroundColor();

        if ((bg & 0xFF000000) != 0) {
            EzUi.roundedRect(graphics, 0, 0, boxW, totalH, 1, bg);
        }

        int curY = pad;
        boolean shadow = isTextShadow();
        if (hasTitle) {
            int titleX = (boxW - client.font.width(title)) / 2;
            graphics.text(client.font, title, titleX, curY, getTitleColor(), shadow);
            curY += lineH;
            int sepCol = getSeparatorColor();
            if (hasSeparator && (sepCol >>> 24) != 0) {
                graphics.fill(pad, curY, boxW - pad, curY + 1, sepCol);
                curY += 2;
            }
        }

        // Lines (drawn from top to bottom)
        int txtCol = getTextColor();
        for (int i = entries.size() - 1; i >= 0; i--) {
            EntryData data = entries.get(i);
            graphics.text(client.font, data.nameComp, pad, curY, txtCol, shadow);
            if (!hideRed) {
                int scoreX = boxW - pad - client.font.width(data.scoreStr);
                graphics.text(client.font, data.scoreStr, scoreX, curY, 0xFFFF4444, shadow);
            }
            curY += lineH;
        }

        graphics.pose().popMatrix();
    }
}

