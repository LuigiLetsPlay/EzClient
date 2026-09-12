package app.ezclient.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Configurable session/speedrun timer inspired by BastiGHG's HUD timer. */
public final class BastiTimerModule extends FeatureModule {
    private long accumulatedMillis;
    private long startedAt;
    private boolean running;
    private boolean pausedByMenu;
    private Object lastLevel;

    public BastiTimerModule() {
        super("BastiGHG Timer", true, 140);
        flag("Timer", "autoStart", "Auto start on world join", "Startet automatisch beim Betreten einer Welt.", true);
        flag("Timer", "pauseMenus", "Pause in menus", "Pausiert im Einzelspieler bei geöffnetem Menü.", true);
        flag("Timer", "resetWorld", "Reset on world change", "Setzt den Timer beim Wechsel der Welt zurück.", true);
        option("Darstellung", "precision", "Precision", "Anzeige mit Sekunden oder Millisekunden.", "Tenths", 0, 0,
                "Seconds", "Tenths", "Milliseconds");
        flag("Darstellung", "showHours", "Always show hours", "Zeigt Stunden auch vor 60 Minuten.", false);
        flag("Darstellung", "prefix", "Show timer label", "Zeigt das Label Timer vor der Zeit.", true);
        colorOption("Farben", "runningColor", "Running color", "Farbe während der Timer läuft.", "FFFFFFFF");
        colorOption("Farben", "pausedColor", "Paused color", "Farbe bei pausiertem Timer.", "FFFFB020");
    }

    public void toggleTimer() { if (running) pause(); else start(); }
    public void start() { if (!running) { startedAt = System.nanoTime(); running = true; } }
    public void pause() { if (running) { accumulatedMillis += (System.nanoTime() - startedAt) / 1_000_000L; running = false; } }
    public void resetTimer() { accumulatedMillis = 0; startedAt = running ? System.nanoTime() : 0; }
    public boolean isRunning() { return running; }
    private long elapsed() { return accumulatedMillis + (running ? (System.nanoTime() - startedAt) / 1_000_000L : 0); }

    public int getToggleKey() {
        return EzKeyBindings.getKeyCode(EzKeyBindings.KEY_TIMER_TOGGLE);
    }

    public void setToggleKey(int key) {
        EzKeyBindings.setKeyCode(EzKeyBindings.KEY_TIMER_TOGGLE, key);
    }

    public int getResetKey() {
        return EzKeyBindings.getKeyCode(EzKeyBindings.KEY_TIMER_RESET);
    }

    public void setResetKey(int key) {
        EzKeyBindings.setKeyCode(EzKeyBindings.KEY_TIMER_RESET, key);
    }

    @Override
    public com.google.gson.JsonObject saveFeature() {
        var json = super.saveFeature();
        json.addProperty("toggleKey", getToggleKey());
        json.addProperty("resetKey", getResetKey());
        return json;
    }

    @Override
    public void loadFeature(com.google.gson.JsonObject json) {
        super.loadFeature(json);
        if (json.has("toggleKey") && json.get("toggleKey").isJsonPrimitive()) {
            setToggleKey(json.get("toggleKey").getAsInt());
        }
        if (json.has("resetKey") && json.get("resetKey").isJsonPrimitive()) {
            setResetKey(json.get("resetKey").getAsInt());
        }
    }

    @Override public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            if (lastLevel != null && flag("resetWorld")) { pause(); resetTimer(); }
            lastLevel = mc.level;
            if (mc.level != null && flag("autoStart")) start();
        }
        if (EzKeyBindings.KEY_TIMER_TOGGLE != null) while (EzKeyBindings.KEY_TIMER_TOGGLE.consumeClick()) toggleTimer();
        if (EzKeyBindings.KEY_TIMER_RESET != null) while (EzKeyBindings.KEY_TIMER_RESET.consumeClick()) resetTimer();
        boolean menuPause = flag("pauseMenus") && mc.getSingleplayerServer() != null && EzScreenBridge.current(mc) != null;
        if (menuPause && running) { pause(); pausedByMenu = true; }
        else if (!menuPause && pausedByMenu) { pausedByMenu = false; start(); }
    }

    private String format(long ms) {
        long hours = ms / 3_600_000L, minutes = (ms / 60_000L) % 60, seconds = (ms / 1000L) % 60;
        String base = (hours > 0 || flag("showHours"))
                ? String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
                : String.format(java.util.Locale.ROOT, "%02d:%02d", ms / 60_000L, seconds);
        return switch (text("precision")) {
            case "Milliseconds" -> base + String.format(java.util.Locale.ROOT, ".%03d", ms % 1000);
            case "Tenths" -> base + "." + (ms / 100 % 10);
            default -> base;
        };
    }

    @Override public List<String> lines(Minecraft mc, boolean editor) {
        String time = editor ? "12:34.5" : format(elapsed());
        return List.of((flag("prefix") ? "Timer " : "") + time + (running || editor ? "" : " (paused)"));
    }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/timer.png"); }
}
