package app.ezclient.gui;

public final class TimeWeatherModule extends FeatureModule {
    private long startNanos = System.nanoTime();
    public TimeWeatherModule() {
        super("Time Weather Changer", false, 10);
        option("time", "Time mode", "Server", 0, 0, "Server", "Static", "Dynamic", "Day", "Night", "Sunset");
        option("ticks", "Custom time", 6000.0, 0, 24000); option("speed", "Cycle speed", 1.0, 0.1, 20);
        option("weather", "Weather", "Server", 0, 0, "Server", "Clear", "Rain", "Thunder");
        flag("precipitation", "Rain / snow particles", true); flag("removeFlash", "Remove lightning flash", true);
    }
    public boolean customTime() { return isEnabled() && !text("time").equals("Server"); }
    public double visualTicks() {
        return switch (text("time")) {
            case "Day" -> 6000; case "Night" -> 18000; case "Sunset" -> 12000;
            case "Dynamic" -> (number("ticks") + (System.nanoTime() - startNanos) / 50_000_000.0 * number("speed")) % 24000;
            default -> number("ticks");
        };
    }
    @Override protected void onToggle() { startNanos = System.nanoTime(); }
}
