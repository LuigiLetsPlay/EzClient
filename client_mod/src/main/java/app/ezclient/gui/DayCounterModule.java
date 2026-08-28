package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;

public final class DayCounterModule extends HudModule {
    private boolean showPlaytime = true;
    private boolean showDay = true;

    public DayCounterModule() {
        super("Day Counter", "HUD", false, 6, 120, "Day ", "");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/daycounter.png");
    }

    public boolean isShowPlaytime() { return showPlaytime; }
    public void setShowPlaytime(boolean showPlaytime) { this.showPlaytime = showPlaytime; ConfigManager.save(); }

    public boolean isShowDay() { return showDay; }
    public void setShowDay(boolean showDay) { this.showDay = showDay; ConfigManager.save(); }

    @Override
    protected String value(Minecraft client) {
        if (client == null || client.level == null) return "1";
        
        long day = client.level.getOverworldClockTime() / 24000L;
        if (day < 0) day = 0;

        StringBuilder sb = new StringBuilder();
        if (showDay) {
            sb.append(day);
        }

        if (showPlaytime) {
            long playTicks = 0;
            if (client.player != null && client.player.getStats() != null) {
                try {
                    playTicks = client.player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
                } catch (Throwable ignored) {}
            }
            long hours = playTicks / 72000L;
            long minutes = (playTicks % 72000L) / 1200L;

            if (sb.length() > 0) {
                sb.append(" (").append(hours).append("h ").append(minutes).append("m)");
            } else {
                sb.append(hours).append("h ").append(minutes).append("m");
            }
        }

        return sb.length() > 0 ? sb.toString() : String.valueOf(day);
    }
}
