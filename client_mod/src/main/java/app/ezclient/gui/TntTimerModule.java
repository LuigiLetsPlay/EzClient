package app.ezclient.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.Locale;

/**
 * TNT Timer & Explosion Indicator Module:
 * Renders high-visibility floating 3D countdown timers above primed TNT
 * with dynamic color shifting from safe green to urgent pulsing red.
 */
public final class TntTimerModule extends FeatureModule {
    public TntTimerModule() {
        super("TNT Timer", false, 0);

        option("Anzeige", "precision", "Dezimalstellen", "Genauigkeit der Zeitanzeige (1 oder 2 Dezimalstellen).", "2", 0, 0, "1", "2");
        flag("Effekte", "colorShift", "Farb-Shift", "Wechselt die Textfarbe von Grün über Gelb zu Rot je nach verbleibender Zeit.", true);
        flag("Sichtbarkeit", "renderThroughWalls", "Durch Wände sehen", "Zeigt den Countdown auch an, wenn TNT hinter Blöcken verdeckt ist.", true);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/tnt.png");
    }

    @Override
    public String getDescription() {
        return "Zeigt eine gut lesbare Restzeit über gezündetem TNT mit optionaler dynamischer Warnfarbe.";
    }

    public int getPrecision() {
        try {
            return Integer.parseInt(text("precision"));
        } catch (Exception e) {
            return 2;
        }
    }

    public void setPrecision(int precision) {
        set("precision", String.valueOf(Math.max(1, Math.min(2, precision))));
        ConfigManager.save();
    }

    public boolean isColorShift() { return flag("colorShift"); }
    public void setColorShift(boolean colorShift) {
        set("colorShift", colorShift);
        ConfigManager.save();
    }

    public boolean isRenderThroughWalls() { return flag("renderThroughWalls"); }
    public void setRenderThroughWalls(boolean renderThroughWalls) {
        set("renderThroughWalls", renderThroughWalls);
        ConfigManager.save();
    }

    public Component getFormattedTimer(PrimedTnt entity) {
        int fuse = entity.getFuse();
        float seconds = Math.max(0.0f, (float) fuse / 20.0f);

        String fmt = "%." + getPrecision() + "fs";
        String timeStr = String.format(Locale.ROOT, fmt, seconds);

        String color = "§f";
        if (isColorShift()) {
            if (seconds > 3.0f) {
                color = "§a"; // Green
            } else if (seconds > 1.5f) {
                color = "§e"; // Yellow
            } else {
                long now = System.currentTimeMillis();
                boolean pulse = (now / 200) % 2 == 0;
                color = pulse ? "§c§l" : "§4§l"; // Pulsing flashing Red
            }
        }

        return Component.literal(color + timeStr);
    }
}
