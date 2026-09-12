package app.ezclient.gui;

import net.minecraft.resources.Identifier;

/** Inventory hover previews rendered by AbstractContainerScreenMixin. */
public final class ShulkerPreviewModule extends FeatureModule {
    public ShulkerPreviewModule() {
        super("Shulker Preview", false, 0);
        flag("Shulker", "shulker", "Shulker preview", "Zeigt den Inhalt von Shulker-Kisten beim Darüberfahren.", true);
        flag("Shulker", "counts", "Item counts", "Zeigt Stackgrößen im Raster.", true);
        flag("Shulker", "emptySlots", "Empty slots", "Zeigt auch leere Slots.", true);
        option("Shulker", "scale", "Preview scale", "Größe der Shulker-Vorschau.", 1.0, 0.6, 1.8);
        flag("Map", "map", "Map preview", "Zeigt Karten beim Darüberfahren groß an.", true);
        option("Map", "mapSize", "Map size", "Kantenlänge der Karten-Vorschau.", 128.0, 64, 256);
        flag("Map", "decorations", "Map decorations", "Zeigt Markierungen und Spieler auf Karten.", true);
        colorOption("Farben", "background", "Background", "Hintergrundfarbe der Vorschau.", "E6111419");
        colorOption("Farben", "border", "Border", "Rahmenfarbe der Vorschau.", "FF35414D");
        colorOption("Farben", "title", "Title", "Farbe des Vorschautitels.", "FFFFFFFF");
    }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/shulker_preview.png"); }
}
