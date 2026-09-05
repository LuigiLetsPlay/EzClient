package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Isolates the screen ownership move introduced by Minecraft 26.2. */
public final class EzScreenBridge {
    private static Integer previousMenuBlurRadius;

    private EzScreenBridge() {}

    public static boolean hudHidden(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gui.hud.isHidden();
        //?} else {
        /*return minecraft.options.hideGui;
        *///?}
    }

    public static Screen current(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gui.screen();
        //?} else {
        /*return minecraft.screen;
        *///?}
    }

    public static void set(Minecraft minecraft, Screen screen) {
        Screen current = current(minecraft);
        boolean leavingEzClient = isEzClientScreen(current) && !isEzClientScreen(screen);
        boolean enteringEzClient = !isEzClientScreen(current) && isEzClientScreen(screen);

        if (enteringEzClient && previousMenuBlurRadius == null) {
            previousMenuBlurRadius = minecraft.options.getMenuBackgroundBlurriness();
            // One pixel keeps the world readable instead of applying Minecraft's
            // much stronger user-configured menu blur to this compact overlay.
            minecraft.options.menuBackgroundBlurriness().set(1);
        } else if (leavingEzClient && previousMenuBlurRadius != null) {
            minecraft.options.menuBackgroundBlurriness().set(previousMenuBlurRadius);
            previousMenuBlurRadius = null;
        }

        //? if >=26.2 {
        minecraft.gui.setScreen(screen);
        //?} else {
        /*minecraft.setScreen(screen);
        *///?}
    }

    private static boolean isEzClientScreen(Screen screen) {
        return screen instanceof EzClientScreen
                || screen instanceof EzHubScreen
                || screen instanceof HudEditorScreen
                || screen instanceof HudSettingsScreen
                || screen instanceof ModuleSettingsScreen
                || screen instanceof FeatureSettingsScreen
                || screen instanceof FeatureStyleScreen
                || screen instanceof WaypointScreen
                || screen instanceof ZoomSettingsScreen;
    }

    public static void rescaleChat(Minecraft minecraft) {
        //? if >=26.2 {
        if (minecraft.gui != null && minecraft.gui.hud != null) {
            minecraft.gui.hud.getChat().rescaleChat();
        }
        //?} else {
        /*if (minecraft.gui != null) {
            minecraft.gui.getChat().rescaleChat();
        }
        *///?}
    }
}
