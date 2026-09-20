package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AutoGG & AutoText Module:
 * Automatically sends a friendly post-match "gg" after minigames conclude,
 * equipped with delay protection and fast trigger detection for popular servers.
 */
public final class AutoGgModule extends FeatureModule {
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "EzClient-AutoGG");
        t.setDaemon(true);
        return t;
    });

    private static final List<String> TRIGGERS = List.of(
            "1st Killer -",
            "1st Place -",
            "Winner:",
            "WINNER!",
            "VICTORY!",
            "The game has ended!",
            "gewonnen",
            "Won the game",
            "Game Over",
            "GAME OVER"
    );

    private long lastTriggeredTime = 0L;

    public AutoGgModule() {
        super("AutoGG", false, 0);

        option("Nachricht", "customMessage", "Nachricht", "Die nach Spielende automatisch in den Chat gesendete Nachricht.", "gg", 0, 0, "gg", "Good Game! <3", "gg wp", "Good Game!");
        option("Verzögerung", "delayMs", "Verzögerung (ms)", "Wartezeit in Millisekunden vor dem Senden der Chat-Nachricht.", 1000.0, 500.0, 3000.0);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/chat.png");
    }

    @Override
    public String getDescription() {
        return "Sendet nach einem erkannten Spielende mit einstellbarer Verzögerung automatisch eine freundliche Nachricht.";
    }

    public String getCustomMessage() {
        return text("customMessage");
    }

    public void setCustomMessage(String customMessage) {
        set("customMessage", customMessage == null ? "gg" : customMessage);
        ConfigManager.save();
    }

    public int getDelayMs() {
        return (int) Math.round(number("delayMs"));
    }

    public void setDelayMs(int delayMs) {
        set("delayMs", (double) Math.max(100, Math.min(3000, delayMs)));
        ConfigManager.save();
    }

    public void onChatMessage(String text) {
        if (!isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastTriggeredTime < 10000L) return; // 10s debounce per game

        boolean matched = false;
        for (String trigger : TRIGGERS) {
            if (text.contains(trigger)) {
                matched = true;
                break;
            }
        }

        if (matched) {
            lastTriggeredTime = now;
            String msgToSend = getCustomMessage();
            SCHEDULER.schedule(() -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.getConnection() != null && mc.player != null) {
                        mc.getConnection().sendChat(msgToSend);
                    }
                });
            }, getDelayMs(), TimeUnit.MILLISECONDS);
        }
    }
}
