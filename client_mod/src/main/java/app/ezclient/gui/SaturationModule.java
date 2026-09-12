package app.ezclient.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Precise hunger saturation readout with optional warning threshold. */
public final class SaturationModule extends FeatureModule {
    public SaturationModule() {
        super("Saturation", true, 244);
        option("Darstellung", "mode", "Display", "Zeigt den Wert als Zahl, Balken oder beides.", "Value + bar", 0, 0,
                "Value", "Bar", "Value + bar");
        option("Darstellung", "decimals", "Decimals", "Nachkommastellen des Sättigungswerts.", 1.0, 0, 2);
        flag("Darstellung", "showHunger", "Show hunger", "Zeigt zusätzlich die Hungerpunkte.", true);
        option("Warnung", "warning", "Low threshold", "Unterhalb dieses Werts wird die Warnfarbe verwendet.", 3.0, 0, 20);
        colorOption("Farben", "normalColor", "Saturation color", "Normale Farbe von Text und Balken.", "FF22C96E");
        colorOption("Farben", "warningColor", "Warning color", "Farbe bei niedriger Sättigung.", "FFFF4D4D");
        colorOption("Farben", "emptyColor", "Empty bar color", "Hintergrundfarbe des Balkens.", "6635414D");
    }

    private float saturation(Minecraft mc) {
        return mc.player == null ? 0.0f : mc.player.getFoodData().getSaturationLevel();
    }

    @Override public List<String> lines(Minecraft mc, boolean editor) {
        float value = editor ? 7.5f : saturation(mc);
        int decimals = (int) number("decimals");
        String formatted = String.format(java.util.Locale.ROOT, "%1$." + decimals + "f", value);
        String hunger = flag("showHunger") && mc.player != null ? " · Hunger " + mc.player.getFoodData().getFoodLevel() : "";
        String title = app.ezclient.util.EzI18n.get("ezclient.hud.saturation");
        return text("mode").equals("Bar") ? List.of(title + hunger) : List.of(title + " " + formatted + hunger);
    }

    @Override public void renderFeature(net.minecraft.client.gui.GuiGraphicsExtractor g, Minecraft mc, boolean editor) {
        if (!hasHud() || (!editor && (EzScreenBridge.hudHidden(mc) || mc.getDebugOverlay().showDebugScreen()))) return;
        float saturation = editor ? 7.5f : saturation(mc);
        String label = lines(mc, editor).getFirst();
        int textWidth = mc.font.width(styledText(label));
        boolean bar = !text("mode").equals("Value");
        int width = Math.max(textWidth + CONTENT_PADDING_X * 2, bar ? 112 : 0);
        int height = bar ? 26 : 9 + CONTENT_PADDING_Y * 2;
        int active = saturation <= number("warning") ? tint("warningColor", false) : tint("normalColor", false);
        g.pose().pushMatrix();
        g.pose().translate(getX(), getY());
        g.pose().scale((float)getScale(), (float)getScale());
        renderBackgroundAndBorder(g, 0, 0, width, height);
        g.text(mc.font, styledText(label), CONTENT_PADDING_X, CONTENT_PADDING_Y, active, isTextShadow());
        if (bar) {
            int barWidth = width - CONTENT_PADDING_X * 2;
            g.fill(CONTENT_PADDING_X, 18, CONTENT_PADDING_X + barWidth, 21, tint("emptyColor", false));
            g.fill(CONTENT_PADDING_X, 18, CONTENT_PADDING_X + Math.round(barWidth * Math.max(0, Math.min(20, saturation)) / 20.0f), 21, active);
        }
        g.pose().popMatrix();
    }

    @Override public int getWidth(Minecraft mc) { return getWidth(mc, false); }
    @Override public int getWidth(Minecraft mc, boolean editor) {
        if (mc == null || mc.font == null) return text("mode").equals("Value") ? 64 : 112;
        int textWidth = mc.font.width(styledText(lines(mc, editor).getFirst())) + CONTENT_PADDING_X * 2;
        return text("mode").equals("Value") ? textWidth : Math.max(textWidth, 112);
    }
    @Override public int getHeight(Minecraft mc) { return text("mode").equals("Value") ? 9 + CONTENT_PADDING_Y * 2 : 26; }
    @Override public int getHeight(Minecraft mc, boolean editor) { return getHeight(mc); }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/saturation.png"); }
}
