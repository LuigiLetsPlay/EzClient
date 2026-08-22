package app.ezclient.gui;

import net.minecraft.client.Minecraft;

/** Keyboard state HUD; individual styling is provided by the HUD settings editor. */
public final class KeystrokesModule extends HudModule {
    public KeystrokesModule() { super("Keystrokes", "HUD", false, 6, 86, "", ""); }
    @Override protected String value(Minecraft client) {
        return (client.options.keyUp.isDown() ? "W" : "-") + " " +
               (client.options.keyLeft.isDown() ? "A" : "-") + " " +
               (client.options.keyDown.isDown() ? "S" : "-") + " " +
               (client.options.keyRight.isDown() ? "D" : "-");
    }
}
