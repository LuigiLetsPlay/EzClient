package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Custom EzClient settings panel for Zoom. */
public final class ZoomSettingsScreen extends Screen {
    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public ZoomSettingsScreen(Screen parent) {
        super(Component.literal("Zoom Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
        panelWidth = Math.min(390, width - 32);
        panelHeight = Math.min(268, height - 32);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int controlX = panelX + 112;
        int controlWidth = panelWidth - 126;
        int y = panelY + 38;

        addRenderableWidget(new EzButton(controlX, y, controlWidth, 20,
                Component.literal(zoom.isEnabled() ? "Enabled" : "Disabled"), zoom.isEnabled(), ignored -> {
                    zoom.toggle();
                    rebuildWidgets();
                }));
        y += 27;
        addSlider(controlX, y, controlWidth, normalized(zoom.getZoomLevel(), 1, 30),
                v -> zoom.setZoomLevel(scale(v, 1, 30)),
                v -> Component.literal(String.format("Default strength  %.1fx", scale(v, 1, 30))));
        y += 30;
        addSlider(controlX, y, controlWidth, normalized(zoom.getScrollSensitivity(), 0.1, 2),
                v -> zoom.setScrollSensitivity(scale(v, 0.1, 2)),
                v -> Component.literal(String.format("Wheel step  %.1fx", scale(v, 0.1, 2))));
        y += 30;
        addSlider(controlX, y, controlWidth, normalized(zoom.getMinZoom(), 1, 10),
                v -> zoom.setMinZoom(scale(v, 1, 10)),
                v -> Component.literal(String.format("Minimum  %.1fx", scale(v, 1, 10))));
        y += 30;
        addSlider(controlX, y, controlWidth, normalized(zoom.getMaxZoom(), 2, 30),
                v -> zoom.setMaxZoom(scale(v, 2, 30)),
                v -> Component.literal(String.format("Maximum  %.1fx", scale(v, 2, 30))));
        y += 30;
        addRenderableWidget(new EzButton(controlX, y, controlWidth, 20,
                Component.literal("Smooth zoom: " + (zoom.isSmoothZoom() ? "ON" : "OFF")), zoom.isSmoothZoom(), ignored -> {
                    zoom.setSmoothZoom(!zoom.isSmoothZoom());
                    ConfigManager.save();
                    rebuildWidgets();
                }));

        addRenderableWidget(new EzButton(panelX + 12, panelY + panelHeight - 30, 82, 19,
                Component.literal("Back"), false, ignored -> onClose()));
    }

    private void addSlider(int x, int y, int width, double value,
                           java.util.function.DoubleConsumer setter,
                           java.util.function.DoubleFunction<Component> label) {
        addRenderableWidget(new EzSlider(x, y, width, 22, value, v -> {
            setter.accept(v);
            ConfigManager.save();
        }, label));
    }

    private static double normalized(double value, double min, double max) {
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }

    private static double scale(double value, double min, double max) {
        return min + value * (max - min);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF2111419);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF303943);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 30, 0xFF191E24);
        graphics.text(font, "ZOOM", panelX + 12, panelY + 11, 0xFF43DD8C);
        graphics.text(font, "UTILS", panelX + 50, panelY + 11, 0xFF77818C);
        graphics.text(font, "Hold C + mouse wheel", panelX + 12, panelY + 40, 0xFF89939E);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
