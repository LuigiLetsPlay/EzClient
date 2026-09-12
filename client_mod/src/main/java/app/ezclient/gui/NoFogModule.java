package app.ezclient.gui;

import net.minecraft.resources.Identifier;

/** Client-side fog distance override. */
public final class NoFogModule extends FeatureModule {
    public NoFogModule() {
        super("No Fog", false, 0);
        option("Allgemein", "mode", "Mode", "Entfernt allen Nebel oder nur den Distanznebel.", "Distance only", 0, 0,
                "Distance only", "All fog");
        flag("Umgebung", "water", "Remove water fog", "Entfernt Unterwassernebel im Modus All fog.", true);
        flag("Umgebung", "lava", "Remove lava fog", "Entfernt Lavanebel im Modus All fog.", false);
        flag("Umgebung", "powderSnow", "Remove powder snow fog", "Entfernt Pulverschnee-Nebel im Modus All fog.", true);
        option("Erweitert", "start", "Fog start percent", "Startpunkt des verbleibenden Distanznebels.", 100.0, 0, 100);
        option("Erweitert", "endMultiplier", "End distance multiplier", "Multipliziert die normale Sichtweite.", 8.0, 1, 32);
    }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/no_fog.png"); }
}
