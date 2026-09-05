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
        panelWidth = Math.min(340, width - 24);
        panelHeight = Math.min(236, height - 24);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        addRenderableWidget(new EzButton(panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()));

        int controlX = panelX + 100;
        int controlWidth = panelWidth - 114;
        int halfWidth = (controlWidth - 6) / 2;
        int y = panelY + 44;

        // Toggle button & Hotkey button side-by-side
        addRenderableWidget(new EzButton(controlX, y, halfWidth, 18,
                Component.literal(app.ezclient.util.EzI18n.get("ezclient.zoom.status", app.ezclient.util.EzI18n.onOrOff(zoom.isEnabled()))), zoom.isEnabled(), ignored -> {
                    zoom.toggle();
                    rebuildWidgets();
                }));

        addRenderableWidget(new EzButton(controlX + halfWidth + 6, y, halfWidth, 18,
                Component.literal(getKeyName(zoom.getKeyBind())), isListeningForHotkey, ignored -> {
                    isListeningForHotkey = true;
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

        addRenderableWidget(new EzButton(panelX + panelWidth - 56, panelY + panelHeight - 24, 46, 16,
                app.ezclient.util.EzI18n.comp("ezclient.zoom.back"), false, ignored -> onClose()));
    }

    private String getKeyName(int key) {
        if (isListeningForHotkey) return "Key: ...";
        if (key == -1) return "Key: None";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name == null || name.isEmpty()) {
            if (key == GLFW.GLFW_KEY_LEFT_SHIFT) return "Key: LShift";
            if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "Key: RShift";
            if (key == GLFW.GLFW_KEY_LEFT_CONTROL) return "Key: LCtrl";
            if (key == GLFW.GLFW_KEY_RIGHT_CONTROL) return "Key: RCtrl";
            if (key == GLFW.GLFW_KEY_LEFT_ALT) return "Key: LAlt";
            if (key == GLFW.GLFW_KEY_RIGHT_ALT) return "Key: RAlt";
            if (key == GLFW.GLFW_KEY_SPACE) return "Key: Space";
            return "Key: " + key;
        }
        return "Key: " + name.toUpperCase();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (isListeningForHotkey) {
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isListeningForHotkey) {
            ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                zoom.setKeyBind(-1);
            } else {
                zoom.setKeyBind(event.key());
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
        extractTransparentBackground(graphics);

        EzUi.panel(graphics, panelX, panelY, panelWidth, panelHeight);

        graphics.pose().pushMatrix();
        graphics.pose().translate(panelX + 14, panelY + 9);
        graphics.pose().scale(1.15f, 1.15f);
        graphics.text(font, app.ezclient.util.EzI18n.get("ezclient.zoom.title"), 0, 0, EzUi.TEXT_WHITE);
        graphics.pose().popMatrix();

        graphics.fill(panelX + 14, panelY + 28, panelX + panelWidth - 14, panelY + 29, EzUi.BORDER_SUBTLE);

        int labelX = panelX + 14;
        graphics.text(font, app.ezclient.util.EzI18n.get("ezclient.zoom.lbl_zoom"), labelX, panelY + 49, EzUi.TEXT_MUTED);
        graphics.text(font, app.ezclient.util.EzI18n.get("ezclient.zoom.lbl_strength"), labelX, panelY + 74, EzUi.TEXT_MUTED);
        graphics.text(font, app.ezclient.util.EzI18n.get("ezclient.zoom.lbl_wheel"), labelX, panelY + 100, EzUi.TEXT_MUTED);
        graphics.text(font, app.ezclient.util.EzI18n.get("ezclient.zoom.lbl_min"), labelX, panelY + 126, EzUi.TEXT_MUTED);
        graphics.text(font, app.ezclient.util.EzI18n.get("ezclient.zoom.lbl_max"), labelX, panelY + 152, EzUi.TEXT_MUTED);
        graphics.text(font, app.ezclient.util.EzI18n.get("ezclient.zoom.lbl_smooth"), labelX, panelY + 178, EzUi.TEXT_MUTED);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

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
