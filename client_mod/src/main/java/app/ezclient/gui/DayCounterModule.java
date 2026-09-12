package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;

public final class DayCounterModule extends HudModule {
    private boolean showPlaytime = true;
    private boolean showDay = true;
    private boolean startAtDayOne = true;

    private long sessionTrackedSeconds = 0;
    private long totalTrackedSeconds = 0;
    private long lastTickTimeMs = 0;
    private boolean requestedStats = false;
    private int ticks = 0;

    public DayCounterModule() {
        super("Day Counter", "HUD", false, 6, 120, "Day ", "");
    }

    @Override
    public String getDescription() {
        return "Zeigt den aktuellen Welttag und optional die gesamte Spielzeit an.";
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/daycounter.png");
    }

    public boolean isShowPlaytime() { return showPlaytime; }
    public void setShowPlaytime(boolean showPlaytime) { this.showPlaytime = showPlaytime; ConfigManager.save(); }

    public boolean isShowDay() { return showDay; }
    public void setShowDay(boolean showDay) { this.showDay = showDay; ConfigManager.save(); }

    public boolean isStartAtDayOne() { return startAtDayOne; }
    public void setStartAtDayOne(boolean startAtDayOne) { this.startAtDayOne = startAtDayOne; ConfigManager.save(); }

    public long getTotalTrackedSeconds() { return totalTrackedSeconds; }
    public void setTotalTrackedSeconds(long totalTrackedSeconds) { this.totalTrackedSeconds = Math.max(0, totalTrackedSeconds); }

    @Override
    public void onTick() {
        super.onTick();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            requestedStats = false;
            lastTickTimeMs = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (lastTickTimeMs > 0) {
            long delta = now - lastTickTimeMs;
            if (delta > 0 && delta < 2000) {
                if (++ticks % 20 == 0) {
                    sessionTrackedSeconds++;
                    totalTrackedSeconds++;
                    if (totalTrackedSeconds % 60 == 0) {
                        ConfigManager.save();
                    }
                }
            }
        }
        lastTickTimeMs = now;

        // Periodically request stats packet from server (every ~15s = 300 ticks)
        if (!requestedStats || ticks % 300 == 0) {
            try {
                if (client.getConnection() != null) {
                    client.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
                    requestedStats = true;
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected String value(Minecraft client) {
        if (client == null || client.level == null) return "1";

        long clockTime = client.level.getOverworldClockTime();
        long gameTime = client.level.getGameTime();
        long rawTicks = Math.max(clockTime, gameTime);

        long playTicksFromStats = 0;
        if (client.player != null && client.player.getStats() != null) {
            try {
                playTicksFromStats = client.player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
                if (playTicksFromStats <= 0) {
                    playTicksFromStats = client.player.getStats().getValue(Stats.CUSTOM.get(Stats.TOTAL_WORLD_TIME));
                }
                if (playTicksFromStats <= 0) {
                    playTicksFromStats = client.player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
                }
            } catch (Throwable ignored) {}
        }

        long effectivePlaySeconds = Math.max(playTicksFromStats / 20L, Math.max(sessionTrackedSeconds, totalTrackedSeconds));
        long effectivePlayTicks = effectivePlaySeconds * 20L;

        long day = rawTicks / 24000L;
        if (day <= 0 && effectivePlayTicks >= 24000L) {
            day = effectivePlayTicks / 24000L;
        }
        if (startAtDayOne) {
            day = Math.max(1, day + 1);
        }

        StringBuilder sb = new StringBuilder();
        if (showDay) {
            sb.append(day);
        }

        if (showPlaytime) {
            long hours = effectivePlaySeconds / 3600L;
            long minutes = (effectivePlaySeconds % 3600L) / 60L;
            long seconds = effectivePlaySeconds % 60L;

            String timeFormatted;
            if (hours > 0) {
                timeFormatted = hours + "h " + minutes + "m";
            } else if (minutes > 0) {
                timeFormatted = minutes + "m";
            } else {
                timeFormatted = seconds + "s";
            }

            if (sb.length() > 0) {
                sb.append(" (").append(timeFormatted).append(")");
            } else {
                sb.append(timeFormatted);
            }
        }

        return sb.length() > 0 ? sb.toString() : String.valueOf(day);
    }

    @Override
    public String displayText(Minecraft client) {
        return (showDay ? app.ezclient.util.EzI18n.get("ezclient.hud.day.prefix") : "") + value(client);
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        if (editor) {
            String d = showDay ? app.ezclient.util.EzI18n.get("ezclient.hud.day.prefix") + "42" : "";
            String p = showPlaytime ? (showDay ? " (12h 30m)" : "12h 30m") : "";
            String res = d + p;
            return res.isEmpty() ? app.ezclient.util.EzI18n.get("ezclient.hud.day.prefix") + "42" : res;
        }
        return displayText(client);
    }
}
