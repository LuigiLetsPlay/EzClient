package app.ezclient.gui;

import net.minecraft.resources.Identifier;

public final class BossBarModule extends FeatureModule {
    public BossBarModule() {
        super("Boss Bar Customizer", true, 12);
        flag("hide", "Hide all boss bars", false); option("filter", "Hide names containing", "", 0, 0);
        option("style", "Style", "Vanilla", 0, 0, "Vanilla", "Minimal", "Text");
        option("health", "Health display", "Percent", 0, 0, "Percent", "HP", "Hidden");
        option("maxHp", "Known max HP (0 = unknown)", 0.0, 0, 10000);
        flag("override", "Override bar color", false); colorOption("bar", "Bar color", "FFAA55FF"); flag("chroma", "Rainbow bar", false);
    }
    public String health(float progress) {
        if (text("health").equals("Hidden")) return "";
        if (text("health").equals("HP")) return number("maxHp") > 0
            ? String.format(java.util.Locale.ROOT, " %.0f / %.0f HP", progress * number("maxHp"), number("maxHp")) : " HP: unavailable";
        return " " + Math.round(progress * 100) + "%";
    }
    @Override public int getWidth(net.minecraft.client.Minecraft mc, boolean editor) { return 190; }
    @Override public int getHeight(net.minecraft.client.Minecraft mc) { return 28; }
    @Override public void renderFeature(net.minecraft.client.gui.GuiGraphicsExtractor g, net.minecraft.client.Minecraft mc, boolean editor) {
        if (editor) super.renderFeature(g, mc, true); // Live bars are drawn from BossHealthOverlay's synchronized events.
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/boss_bar.png");
    }
}
