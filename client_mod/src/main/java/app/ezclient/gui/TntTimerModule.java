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
public final class TntTimerModule extends Module {
    private int precision = 2; // 1 or 2 decimals
    private boolean colorShift = true;
    private boolean renderThroughWalls = true;

    public TntTimerModule() {
        super("TNT Timer", "Render", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/tnt.png");
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public int getPrecision() { return precision; }
    public void setPrecision(int precision) { this.precision = Math.max(1, Math.min(2, precision)); ConfigManager.save(); }

    public boolean isColorShift() { return colorShift; }
    public void setColorShift(boolean colorShift) { this.colorShift = colorShift; ConfigManager.save(); }

    public boolean isRenderThroughWalls() { return renderThroughWalls; }
    public void setRenderThroughWalls(boolean renderThroughWalls) { this.renderThroughWalls = renderThroughWalls; ConfigManager.save(); }

    public Component getFormattedTimer(PrimedTnt entity) {
        int fuse = entity.getFuse();
        float seconds = Math.max(0.0f, (float) fuse / 20.0f);

        String fmt = "%." + precision + "fs";
        String timeStr = String.format(Locale.ROOT, fmt, seconds);

        String color = "§f";
        if (colorShift) {
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
