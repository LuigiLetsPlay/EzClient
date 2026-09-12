package app.ezclient.gui;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public final class TimeWeatherModule extends FeatureModule {
    private long startNanos = System.nanoTime();
    public TimeWeatherModule() {
        super("Time Weather Changer", false, 10);
        option("Zeit", "time", "Time mode", "Wählt die lokale Tageszeit ohne Serveränderung.", "Server", 0, 0, "Server", "Custom", "Dynamic", "Day", "Night", "Sunset");
        option("Zeit", "ticks", "Custom time", "Zeitwert für den statischen Modus.", 6000.0, 0, 24000); option("Zeit", "speed", "Cycle speed", "Geschwindigkeit der dynamischen Zeit.", 1.0, 0.1, 20);
        option("Wetter", "weather", "Weather", "Wählt die lokale Wetterdarstellung.", "Server", 0, 0, "Server", "Clear", "Rain", "Thunder");
        flag("Wetter", "precipitation", "Rain / snow particles", "Zeigt Regen- und Schneepartikel.", true); flag("Wetter", "removeFlash", "Remove lightning flash", "Unterdrückt helle Blitzeffekte.", true);
    }
    public boolean customTime() { return isEnabled() && !text("time").equals("Server"); }
    public double visualTicks() {
        return switch (text("time")) {
            case "Day" -> 6000; case "Night" -> 18000; case "Sunset" -> 12000;
            case "Dynamic" -> (number("ticks") + (System.nanoTime() - startNanos) / 50_000_000.0 * number("speed")) % 24000;
            default -> number("ticks");
        };
    }

    @Override
    public boolean set(Option option, Object value) {
        if (!option.key().equals("time")) return super.set(option, value);
        String mode = "Static".equals(String.valueOf(value)) ? "Custom" : String.valueOf(value);
        boolean changed = super.setTransient(option, mode);
        if (!changed) return false;
        setPresetTicks(mode);
        startNanos = System.nanoTime();
        ConfigManager.save();
        return true;
    }

    public void previewCustomTime(double ticks) {
        Option mode = findOption("time");
        Option customTicks = findOption("ticks");
        if (mode != null) super.setTransient(mode, "Custom");
        if (customTicks != null) super.setTransient(customTicks, ticks);
    }

    public void commitCustomTime() {
        ConfigManager.save();
    }

    @Override
    public void loadFeature(JsonObject json) {
        JsonObject normalized = json.deepCopy();
        if (normalized.has("time") && "Static".equals(normalized.get("time").getAsString())) {
            normalized.addProperty("time", "Custom");
        }
        super.loadFeature(normalized);
        setPresetTicks(text("time"));
    }

    private void setPresetTicks(String mode) {
        double ticks = switch (mode) {
            case "Day" -> 6000.0;
            case "Night" -> 18000.0;
            case "Sunset" -> 12000.0;
            default -> -1.0;
        };
        Option customTicks = findOption("ticks");
        if (ticks >= 0 && customTicks != null) super.setTransient(customTicks, ticks);
    }
    @Override protected void onToggle() { startNanos = System.nanoTime(); }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/time_weather.png");
    }
}
