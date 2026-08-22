package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small Right-Shift hub, analogous to the title-screen EzClient shortcut. */
public final class EzHubScreen extends Screen {
    private final Screen parent;
    private int x, y;
    public EzHubScreen(Screen parent) { super(Component.literal("EzClient")); this.parent = parent; }
    @Override protected void init() {
        x = width / 2 - 126; y = height / 2 - 64;
        addRenderableWidget(new EzButton(x + 14, y + 42, 104, 58, Component.literal("Modules"), true,
                b -> minecraft.gui.setScreen(new EzClientScreen(this))));
        addRenderableWidget(new EzButton(x + 134, y + 42, 104, 58, Component.literal("HUD Editor"), true,
                b -> minecraft.gui.setScreen(new HudEditorScreen(this))));
        addRenderableWidget(new EzButton(x + 190, y + 106, 48, 17, Component.literal("Close"), false, b -> onClose()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        g.fill(x, y, x + 252, y + 128, 0xEE111419);
        g.outline(x, y, 252, 128, 0xFF35414D);
        g.text(font, "EZ", x + 14, y + 14, 0xFF43DD8C);
        g.text(font, "CLIENT", x + 30, y + 14, 0xFFE8EDF1);
        g.centeredText(font, "Choose a workspace", width / 2, y + 28, 0xFF7F8994);
        super.extractRenderState(g, mx, my, d);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
