package app.ezclient.gui;
public final class SpeedModule extends HudModule {
    public SpeedModule() { super("Speed", "Utils", false, 6, 70, "Speed: ", " b/s"); }
    @Override protected String value(net.minecraft.client.Minecraft c) {
        if (c.player == null) return "0.0";
        var v = c.player.getDeltaMovement();
        return String.format(java.util.Locale.ROOT, "%.1f", Math.sqrt(v.x * v.x + v.z * v.z) * 20.0);
    }
}
