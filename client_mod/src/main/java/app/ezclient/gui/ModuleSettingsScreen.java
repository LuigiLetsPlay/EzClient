package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ModuleSettingsScreen extends ScrollingSettingsScreen {
    private final Screen parent;
    private final Module module;
    private boolean isListeningForHotkey = false;

    private int panelX, panelY, panelWidth, panelHeight;

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 34; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 32; }

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getDisplayName() + " " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "")));
        this.parent = parent;
        this.module = module;
    }

    private AbstractWidget described(AbstractWidget widget, String description) {
        widget.setTooltip(Tooltip.create(Component.literal(app.ezclient.util.EzI18n.text(description))));
        return widget;
    }

    private static <T extends Enum<T>> T cycle(T current, T[] values, int direction) {
        return values[Math.floorMod(current.ordinal() + direction, values.length)];
    }

    /** Resets only the open module and keeps every other module/configuration intact. */
    private void resetOpenModule() {
        module.resetSettings();
        ConfigManager.save();
    }

    @Override
    protected void init() {
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        addFixedWidget(new EzButton(
                panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()
        ));
        int sidebarY = panelY + 102;
        if (module.hasPreview()) {
            addFixedWidget(new EzButton(panelX + 6, sidebarY, SETTINGS_SIDEBAR_WIDTH - 12, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Vorschau")), false, ignored -> EzScreenBridge.set(minecraft, new ModulePreviewScreen(this, module))));
            sidebarY += 22;
        }

        addFixedWidget(new EzHotkeyButton(panelX + 6, panelY + 66,
                SETTINGS_SIDEBAR_WIDTH - 12, module.getKeyBind(), isListeningForHotkey,
                () -> { isListeningForHotkey = !isListeningForHotkey; rebuildWidgets(); }));

        addFixedWidget(new EzButton(
                settingsContentLeft(panelX), panelY + panelHeight - 24, 100, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Zurücksetzen")), false,
                b -> { resetOpenModule(); rebuildWidgets(); }
        ));
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX) + 108, panelY + panelHeight - 24, 100, 16,
                app.ezclient.util.EzI18n.comp("ezclient.module_settings.done"), true,
                b -> onClose()
        ));
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float d) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        g.text(font, EzUi.fitText(getTitle(), panelWidth - SETTINGS_SIDEBAR_WIDTH - 42), settingsContentLeft(panelX), panelY + 10, EzUi.TEXT_WHITE);

        g.fill(settingsContentLeft(panelX), panelY + 28, panelX + panelWidth - 8, panelY + 29, EzUi.BORDER_SUBTLE);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, module.getDisplayName());

        super.extractSettings(g, mx, my, d);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            EzScreenBridge.set(minecraft, parent);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isListeningForHotkey) {
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
                EzKeyBindings.applyModuleKeyBind(module, -1);
            } else {
                EzKeyBindings.applyModuleKeyBind(module, event.key());
            }
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected boolean settingsMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isListeningForHotkey && event.button() != 0) {
            EzKeyBindings.applyModuleKeyBind(module, -100 - event.button());
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        return super.settingsMouseClicked(event, doubleClick);
    }
}
