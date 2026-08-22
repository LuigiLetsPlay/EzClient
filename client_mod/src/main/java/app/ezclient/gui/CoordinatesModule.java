package app.ezclient.gui;
public final class CoordinatesModule extends HudModule {
    public CoordinatesModule() { super("Coordinates", "Utils", true, 6, 38, "XYZ: ", ""); }
    @Override protected String value(net.minecraft.client.Minecraft c) {
        if (c.player == null) return "0 / 0 / 0";
        return (int)c.player.getX() + " / " + (int)c.player.getY() + " / " + (int)c.player.getZ();
    }
}
