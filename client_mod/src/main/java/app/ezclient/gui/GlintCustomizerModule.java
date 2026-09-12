package app.ezclient.gui;

import net.minecraft.resources.Identifier;

/** Settings consumed by the item foil rendering hook. */
public final class GlintCustomizerModule extends FeatureModule {
    public GlintCustomizerModule() {
        super("Glint Customizer", false, 0);
        colorOption("Farben", "color", "Glint color", "Farbe des Verzauberungsschimmers.", "FF9B5CFF");
        flag("Farben", "rainbow", "Rainbow", "Animiert den Schimmer durch das Farbspektrum.", false);
        option("Farben", "alpha", "Opacity", "Deckkraft des Schimmers.", 1.0, 0.05, 1.0);
        option("Animation", "speed", "Animation speed", "Geschwindigkeit des Regenbogenverlaufs.", 1.0, 0.1, 5.0);
        flag("Darstellung", "items", "Items", "Passt den Schimmer von Items an.", true);
        flag("Darstellung", "armor", "Armor", "Passt den Schimmer von Rüstung an.", true);
    }
    public int glintColor() {
        int base = tint("color", flag("rainbow"));
        int alpha = Math.max(1, Math.min(255, (int)Math.round(number("alpha") * 255)));
        return (alpha << 24) | (base & 0xffffff);
    }
    @Override public float getRainbowSpeed() { return (float)number("speed"); }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/glint.png"); }
}
