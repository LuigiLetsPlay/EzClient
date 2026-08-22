package app.ezclient.gui;
public final class ClockModule extends HudModule {
    public ClockModule() { super("Clock", "Utils", false, 6, 54, "Time: ", ""); }
    @Override protected String value(net.minecraft.client.Minecraft c) {
        return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
