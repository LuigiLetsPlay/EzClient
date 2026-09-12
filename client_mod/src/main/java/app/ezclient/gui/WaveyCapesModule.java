package app.ezclient.gui;

import net.minecraft.resources.Identifier;

/** Cape motion tuning applied to extracted avatar render state. */
public final class WaveyCapesModule extends FeatureModule {
    public WaveyCapesModule() {
        super("Wavey Capes", false, 0);
        flag("Darstellung", "all_players", "Alle Spieler", "Animiert auf diesem Client jedes sichtbare Cape, auch von NoRisk- und Vanilla-Spielern.", true);
        option("Physik", "style", "Style", "Bewegungsprofil des Capes.", "Natural", 0, 0, "Subtle", "Natural", "Windy");
        option("Physik", "strength", "Movement strength", "Stärke der Lauf- und Flugbewegung.", 1.0, 0.1, 2.5);
        option("Physik", "damping", "Damping", "Dämpft abrupte Cape-Bewegungen.", 0.75, 0, 1);
        flag("Wind", "wind", "Ambient wind", "Bewegt Capes auch im Stand sanft.", true);
        option("Wind", "amplitude", "Wind amplitude", "Stärke der Windwelle.", 6.0, 0, 20);
        option("Wind", "speed", "Wind speed", "Geschwindigkeit der Windwelle.", 1.0, 0.1, 4);
        flag("Darstellung", "elytra", "Disable with elytra", "Deaktiviert Zusatzphysik beim Tragen einer Elytra.", true);
    }
    public float styleMultiplier() { return switch (text("style")) { case "Subtle" -> .55f; case "Windy" -> 1.55f; default -> 1.0f; }; }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/wavey_capes.png"); }
}
