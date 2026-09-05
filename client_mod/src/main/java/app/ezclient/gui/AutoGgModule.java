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
public final class AutoGgModule extends Module {
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

    private String customMessage = "gg";
    private int delayMs = 1000; // 100 to 3000ms
    private long lastTriggeredTime = 0L;

    public AutoGgModule() {
        super("AutoGG", "Utility", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/chat.png");
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage == null ? "gg" : customMessage; ConfigManager.save(); }

    public int getDelayMs() { return delayMs; }
    public void setDelayMs(int delayMs) { this.delayMs = Math.max(100, Math.min(3000, delayMs)); ConfigManager.save(); }

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
            String msgToSend = customMessage;
            SCHEDULER.schedule(() -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.getConnection() != null && mc.player != null) {
                        mc.getConnection().sendChat(msgToSend);
                    }
                });
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }
}
