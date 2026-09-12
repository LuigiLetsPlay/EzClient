package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Compact, clean settings screen for Zoom with configurable Hotkey. */
public final class ZoomSettingsScreen extends Screen {
    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private boolean isListeningForHotkey = false;

    public ZoomSettingsScreen(Screen parent) {
        super(app.ezclient.util.EzI18n.comp("ezclient.zoom.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
        panelWidth = Math.min(ScrollingSettingsScreen.SETTINGS_WIDTH, width - 48);
        panelHeight = Math.min(ScrollingSettingsScreen.SETTINGS_HEIGHT, height - 48);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        addRenderableWidget(new EzButton(panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()));
        addRenderableWidget(new EzButton(panelX + 6, panelY + 43, ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH - 12, 18,
                Component.literal("Vorschau"), false, ignored -> EzScreenBridge.set(minecraft, new ModulePreviewScreen(this, zoom))));

        String sbHkText = isListeningForHotkey ? "Taste: …" : (zoom.getKeyBind() > 0 || zoom.getKeyBind() <= -100 ? "Key: " + EzKeyBindings.getKeyOrMouseName(zoom.getKeyBind()) : "Taste: Keine");
        addRenderableWidget(new EzButton(panelX + 6, panelY + 65, ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH - 12, 18,
                Component.literal(sbHkText), isListeningForHotkey, ignored -> {
                    isListeningForHotkey = !isListeningForHotkey;
                    rebuildWidgets();
                }));

        int controlX = panelX + ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH + 8;
        int controlWidth = panelWidth - ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH - 16;
        int halfWidth = (controlWidth - 6) / 2;
        int y = panelY + 44;

        // Status Toggle button
        addRenderableWidget(new EzButton(controlX, y, controlWidth, 18,
                Component.literal(app.ezclient.util.EzI18n.get("ezclient.zoom.status", app.ezclient.util.EzI18n.onOrOff(zoom.isEnabled()))), zoom.isEnabled(), ignored -> {
                    zoom.toggle();
                    rebuildWidgets();
                }));

        y += 24;
        addSlider(controlX, y, controlWidth, normalized(zoom.getZoomLevel(), 1, 30),
                v -> zoom.setZoomLevel(scale(v, 1, 30)),
                v -> Component.literal(app.ezclient.util.EzI18n.get("ezclient.zoom.default_level", scale(v, 1, 30))));
        y += 26;
        addSlider(controlX, y, controlWidth, normalized(zoom.getScrollSensitivity(), 0.1, 2),
                v -> zoom.setScrollSensitivity(scale(v, 0.1, 2)),
                v -> Component.literal(app.ezclient.util.EzI18n.get("ezclient.zoom.wheel_step", scale(v, 0.1, 2))));
        y += 26;
        addSlider(controlX, y, controlWidth, normalized(zoom.getMinZoom(), 1, 10),
                v -> zoom.setMinZoom(scale(v, 1, 10)),
                v -> Component.literal(app.ezclient.util.EzI18n.get("ezclient.zoom.min_zoom", scale(v, 1, 10))));
        y += 26;
        addSlider(controlX, y, controlWidth, normalized(zoom.getMaxZoom(), 2, 30),
                v -> zoom.setMaxZoom(scale(v, 2, 30)),
                v -> Component.literal(app.ezclient.util.EzI18n.get("ezclient.zoom.max_zoom", scale(v, 2, 30))));
        y += 26;
        addRenderableWidget(new EzButton(controlX, y, controlWidth, 18,
                Component.literal(app.ezclient.util.EzI18n.get("ezclient.zoom.smooth", app.ezclient.util.EzI18n.onOrOff(zoom.isSmoothZoom()))), zoom.isSmoothZoom(), ignored -> {
                    zoom.setSmoothZoom(!zoom.isSmoothZoom());
                    ConfigManager.save();
                    rebuildWidgets();
                }));

        addRenderableWidget(new EzButton(controlX, panelY + panelHeight - 24, controlWidth, 16,
                app.ezclient.util.EzI18n.comp("ezclient.zoom.back"), false, ignored -> onClose()));
    }

    private String getKeyName(int key) {
        if (isListeningForHotkey) return "Taste: …";
        if (key <= 0 && key > -100) return "Taste: Keine";
        return "Key: " + EzKeyBindings.getKeyOrMouseName(key);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (isListeningForHotkey && e.button() != 0) {
            ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
            int code = -100 - e.button();
            EzKeyBindings.applyModuleKeyBind(zoom, code);
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isListeningForHotkey) {
            ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_BACKSPACE || event.key() == GLFW.GLFW_KEY_DELETE) {
                EzKeyBindings.applyModuleKeyBind(zoom, -1);
            } else {
                EzKeyBindings.applyModuleKeyBind(zoom, event.key());
            }
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    private void addSlider(int x, int y, int width, double initial,
                           java.util.function.DoubleConsumer onValueChange,
                           java.util.function.DoubleFunction<Component> labelFactory) {
        addRenderableWidget(new EzSlider(x, y, width, 18, initial, onValueChange, labelFactory));
    }

    private static double normalized(double value, double min, double max) {
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    private static double scale(double normalized, double min, double max) {
        return min + normalized * (max - min);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        EzUi.backdrop(graphics, width, height);

        EzUi.panel(graphics, panelX, panelY, panelWidth, panelHeight);

        int sidebar = ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH;
        graphics.fill(panelX + sidebar, panelY + 6, panelX + sidebar + 1, panelY + panelHeight - 6, EzUi.BORDER_SUBTLE);
        int logoX = panelX + 26, logoY = panelY + 9;
        EzUi.roundedRect(graphics, logoX, logoY, 20, 20, 3, 0xFF15181C);
        ModuleIconRenderer.drawTexture(graphics, ScrollingSettingsScreen.SETTINGS_LOGO, logoX + 2, logoY + 2, 16);
        EzUi.roundedRect(graphics, panelX + 6, panelY + 43, sidebar - 12, 18, 2, EzUi.BG_CARD_ACTIVE);
        graphics.centeredText(font, Component.literal("Zoom"), panelX + sidebar / 2, panelY + 48, EzUi.TEXT_LIGHT);

        graphics.pose().pushMatrix();
        graphics.pose().translate(panelX + sidebar + 8, panelY + 9);
        graphics.pose().scale(1.15f, 1.15f);
        graphics.text(font, app.ezclient.util.EzI18n.get("ezclient.zoom.title"), 0, 0, EzUi.TEXT_WHITE);
        graphics.pose().popMatrix();

        graphics.fill(panelX + sidebar + 8, panelY + 28, panelX + panelWidth - 8, panelY + 29, EzUi.BORDER_SUBTLE);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        boolean hover = children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget)
                .map(child -> (net.minecraft.client.gui.components.AbstractWidget) child)
                .anyMatch(widget -> widget.active && widget.visible && mouseX >= widget.getX() && mouseX < widget.getX() + widget.getWidth()
                        && mouseY >= widget.getY() && mouseY < widget.getY() + widget.getHeight());
        EzCursor.setPointer(hover);
    }

    @Override public void removed() { EzCursor.setPointer(false); super.removed(); }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        EzScreenBridge.set(minecraft, parent);
    }
}
