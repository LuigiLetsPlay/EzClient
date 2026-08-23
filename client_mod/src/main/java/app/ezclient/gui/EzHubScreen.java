package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Entry point for the modern, non-pausing EzClient workspace. */
public final class EzHubScreen extends Screen {
    private final Screen parent;
    private int x, y;
    public EzHubScreen(Screen parent) { super(Component.literal("EzClient")); this.parent = parent; }
    @Override protected void init() {
        int panelWidth = Math.min(520, width - 28);
        int panelHeight = Math.min(270, height - 28);
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        addRenderableWidget(new EzButton(x + 22, y + 104, (panelWidth - 56) / 2, 104, Component.literal("Modules"), true,
                b -> minecraft.gui.setScreen(new EzClientScreen(this))));
        addRenderableWidget(new EzButton(x + 34 + (panelWidth - 56) / 2, y + 104, (panelWidth - 56) / 2, 104, Component.literal("HUD Editor"), false,
                b -> minecraft.gui.setScreen(new HudEditorScreen(this))));
        addRenderableWidget(new EzButton(x + panelWidth - 88, y + panelHeight - 35, 66, 22, Component.literal("Close"), false, b -> onClose()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        int panelWidth = Math.min(520, width - 28);
        int panelHeight = Math.min(270, height - 28);
        EzUi.panel(g, x, y, panelWidth, panelHeight);
        EzUi.roundedRect(g, x + 22, y + 22, 36, 36, 18, 0xFF8B6CF6);
        g.centeredText(font, "EZ", x + 40, y + 36, 0xFFFFFFFF);
        g.text(font, "EzClient", x + 70, y + 26, 0xFFFFFFFF);
        g.text(font, "Your game, refined.", x + 70, y + 42, 0xFFABB5C7);
        g.text(font, "MODULES", x + 34, y + 86, 0xFFABB5C7);
        g.text(font, "HUD EDITOR", x + 46 + (panelWidth - 56) / 2, y + 86, 0xFFABB5C7);
        super.extractRenderState(g, mx, my, d);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
