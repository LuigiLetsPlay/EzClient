package app.ezclient.gui;

import net.minecraft.client.gui.screens.Screen;

/** Backward-compatible wrapper delegating directly to the unified EzHubScreen. */
public final class EzClientScreen extends Screen {
    public EzClientScreen(Screen parent) {
        super(net.minecraft.network.chat.Component.literal("EzClient"));
    }

    @Override
    protected void init() {
        minecraft.gui.setScreen(new EzHubScreen(null));
    }
}
