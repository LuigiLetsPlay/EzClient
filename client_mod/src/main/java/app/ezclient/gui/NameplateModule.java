package app.ezclient.gui;

import net.minecraft.resources.Identifier;

public final class NameplateModule extends FeatureModule {
    public NameplateModule() {
        super("Nameplate Levelhead", false, 10);
        flag("Allgemein", "own", "Own name in third person", "Zeigt den eigenen Namen in der dritten Person.", true);
        option("Darstellung", "health", "Health display", "Legt die Gesundheitsanzeige fest.", "HP", 0, 0, "HP", "Hearts", "Hidden");
        flag("Darstellung", "nameBackground", "Name background", "Hintergrund hinter Namensschildern.", true); flag("Darstellung", "nameShadow", "Name shadow", "Schatten für bessere Lesbarkeit.", true);
        option("Allgemein", "prefix", "Local clan / level prefix", "Text vor dem lokalen Spielernamen.", "", 0, 0);
        option("Freunde", "friends", "Friends (comma-separated names)", "Kommagetrennte Liste lokaler Freundesnamen.", "", 0, 0);
        colorOption("Freunde", "friendColor", "Friend accent", "Akzentfarbe für Freunde.", "FF22C96E");
    }
    public boolean friend(String name) {
        for (String value : text("friends").split(",")) if (value.trim().equalsIgnoreCase(name)) return true;
        return false;
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/nameplate.png");
    }
}
