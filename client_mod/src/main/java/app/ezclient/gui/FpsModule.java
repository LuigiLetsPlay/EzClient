package app.ezclient.gui;

/** Toggle for the compact FPS HUD indicator. */
public final class FpsModule extends HudModule {
    public FpsModule() {
        super("FPS", "Visual", true, 6, 6, "FPS: ", "");
    }
    @Override protected String value(net.minecraft.client.Minecraft client) { return Integer.toString(client.getFps()); }
}
