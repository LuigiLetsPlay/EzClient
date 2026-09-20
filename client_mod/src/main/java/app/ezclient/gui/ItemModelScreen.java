package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Unified ScrollingSettingsScreen for Item Model transforms.
 * Allows adjusting position, rotation, and scale for all items or specific items
 * across First Person, Ground, and GUI views.
 */
public final class ItemModelScreen extends ScrollingSettingsScreen {
    private static final String[] LABELS = {
            "Position X", "Position Y", "Position Z",
            "Pitch", "Yaw", "Roll",
            "Skalierung X", "Skalierung Y", "Skalierung Z"
    };

    private final Screen parent;
    private final ItemModelModule module;
    private String itemId = "*";
    private ItemModelModule.View view = ItemModelModule.View.FIRST_PERSON;
    private ItemStack previewStack = ItemStack.EMPTY;
    private boolean isListeningForHotkey = false;
    private boolean advancedExpanded = false;

    private int panelX, panelY, panelWidth, panelHeight;

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 34; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 32; }

    public ItemModelScreen(Screen parent, ItemModelModule module) {
        super(Component.literal("Item Model " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()));
        this.parent = parent;
        this.module = module;
    }

    private <T extends AbstractWidget> T described(T widget, String description) {
        if (description != null && !description.isBlank()) {
            widget.setTooltip(Tooltip.create(Component.literal(app.ezclient.util.EzI18n.text(description))));
        }
        return widget;
    }

    private String displayName() {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return itemId;
        var item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) return itemId;
        return ItemIconHelper.createSafeStack(item).getHoverName().getString();
    }

    @Override
    protected void init() {
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        Identifier selected = Identifier.tryParse(itemId);
        var previewItem = itemId.equals("*") ? Items.DIAMOND_SWORD
                : selected == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(selected);
        previewStack = ItemIconHelper.createSafeStack(previewItem);

        // Top-right close button ✕
        addFixedWidget(new EzButton(
                panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()
        ));

        // Header module enable toggle switch
        addFixedWidget(described(new EzToggleSwitch(
                panelX + panelWidth - 56, panelY + 7, 24, 14,
                module.isEnabled(),
                state -> {
                    module.setEnabled(state);
                    ConfigManager.save();
                }
        ), "Modul aktivieren / deaktivieren"));

        // Sidebar Hotkey button
        addFixedWidget(new EzHotkeyButton(panelX + 6, panelY + 66,
                SETTINGS_SIDEBAR_WIDTH - 12, module.getKeyBind(), isListeningForHotkey,
                () -> { isListeningForHotkey = !isListeningForHotkey; rebuildWidgets(); }));

        // Footer buttons
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX), panelY + panelHeight - 24, 116, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Ansicht zurücksetzen")), false,
                b -> {
                    module.clearTransform(itemId, view);
                    rebuildWidgets();
                }
        ));
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX) + 124, panelY + panelHeight - 24, 84, 16,
                app.ezclient.util.EzI18n.comp("ezclient.module_settings.done"), true,
                b -> onClose()
        ));

        int curY = panelY + 38;
        int fullW = settingsContentWidth(panelWidth);
        int btnW = (fullW - 6) / 2;
        int col1X = settingsContentLeft(panelX);
        int col2X = col1X + btnW + 6;

        // ════════════════════════════════════════
        // 1. ITEM-AUSWAHL & ANSICHT
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("ITEM-AUSWAHL & ANSICHT")));
        curY += 16;

        // Item selector button + Reset to All Items button
        String itemLabel = itemId.equals("*") ? "Item: Alle Items (Standard)" : "Item: " + displayName();
        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW - 90, 18,
                Component.literal(itemLabel), false,
                b -> EzScreenBridge.set(minecraft, new ItemPickerScreen(this,
                        itemId.equals("*") ? "" : itemId, id -> { itemId = id; rebuildWidgets(); }))
        ), "Klicken, um ein bestimmtes Item aus dem gesamten Minecraft-Katalog auszuwählen"));

        addRenderableWidget(described(new EzButton(
                col1X + fullW - 86, curY, 86, 18,
                Component.literal("Alle Items"), itemId.equals("*"),
                b -> { itemId = "*"; rebuildWidgets(); }
        ), "Stellt Werte ein, die für alle Items als Standard gelten"));
        curY += 22;

        // 3 View Tabs: First Person | Gedroppt | GUI
        int tabW = (fullW - 8) / 3;
        String[] tabNames = {"First Person", "Gedroppt", "GUI"};
        ItemModelModule.View[] views = ItemModelModule.View.values();
        for (int i = 0; i < views.length; i++) {
            ItemModelModule.View tab = views[i];
            addRenderableWidget(described(new EzButton(
                    col1X + i * (tabW + 4), curY, tabW, 18,
                    Component.literal(app.ezclient.util.EzI18n.text(tabNames[i])), tab == view,
                    b -> { view = tab; rebuildWidgets(); }
            ), "Wählt die Kameraperspektive / Darstellung für dieses Item aus"));
        }
        curY += 26;

        // ════════════════════════════════════════
        // 2. POSITION
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("POSITION")));
        curY += 16;

        for (int axis = 0; axis < 3; axis++) {
            final int index = axis;
            float min = -2.0f;
            float max = 2.0f;
            float current = module.getTransform(itemId, view).value(index);
            int sliderX = (axis == 2) ? col1X : (axis == 0 ? col1X : col2X);
            int sliderW = (axis == 2) ? fullW : btnW;

            addRenderableWidget(described(new EzSlider(
                    sliderX, curY, sliderW, 18,
                    (current - min) / (max - min),
                    normalized -> {
                        var val = module.getTransform(itemId, view)
                                .with(index, (float) (min + normalized * (max - min)));
                        module.setTransform(itemId, view, val, false);
                    },
                    normalized -> Component.literal(LABELS[index] + ": " + String.format(Locale.ROOT, "%.2f", min + normalized * (max - min))),
                    true, ConfigManager::save
            ), "Verschiebung auf der " + (axis == 0 ? "X-Achse (Horizontal)" : (axis == 1 ? "Y-Achse (Vertikal)" : "Z-Achse (Tiefe)"))));

            if (axis == 1) curY += 22;
        }
        curY += 26;

        // ════════════════════════════════════════
        // Friendly default: one size control. Per-axis scaling remains below in
        // Advanced for users who intentionally want stretched item models.
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14,
                Component.literal(app.ezclient.util.EzI18n.text("GR\u00D6SSE"))));
        curY += 16;
        var currentTransform = module.getTransform(itemId, view);
        float uniformScale = (currentTransform.value(6) + currentTransform.value(7) + currentTransform.value(8)) / 3.0f;
        float scaleMin = 0.05f;
        float scaleMax = 4.0f;
        addRenderableWidget(described(new EzSlider(
                col1X, curY, fullW, 18,
                (uniformScale - scaleMin) / (scaleMax - scaleMin),
                normalized -> {
                    float value = (float) (scaleMin + normalized * (scaleMax - scaleMin));
                    var transform = module.getTransform(itemId, view)
                            .with(6, value).with(7, value).with(8, value);
                    module.setTransform(itemId, view, transform, false);
                },
                normalized -> Component.literal(app.ezclient.util.EzI18n.text("Gr\u00F6\u00DFe") + ": "
                        + String.format(Locale.ROOT, "%.2fx", scaleMin + normalized * (scaleMax - scaleMin))),
                true, ConfigManager::save
        ), "Vergr\u00F6\u00DFert oder verkleinert das Item gleichm\u00E4\u00DFig"));
        curY += 26;

        String advancedLabel = app.ezclient.util.EzI18n.text("Erweitert");
        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW, 18,
                Component.literal((advancedExpanded ? "\u25BE " : "\u25B8 ") + advancedLabel
                        + "  \u00B7  " + app.ezclient.util.EzI18n.text("Drehung & Skalierung pro Achse")),
                advancedExpanded,
                b -> {
                    advancedExpanded = !advancedExpanded;
                    rebuildWidgets();
                }
        ), "Zeigt Drehung und getrennte Skalierung f\u00FCr alle drei Achsen"));
        curY += 24;

        if (advancedExpanded) {
        // 3. DREHUNG (ROTATION)
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("DREHUNG (ROTATION)")));
        curY += 16;

        for (int axis = 0; axis < 3; axis++) {
            final int index = 3 + axis;
            float min = -180.0f;
            float max = 180.0f;
            float current = module.getTransform(itemId, view).value(index);
            int sliderX = (axis == 2) ? col1X : (axis == 0 ? col1X : col2X);
            int sliderW = (axis == 2) ? fullW : btnW;

            addRenderableWidget(described(new EzSlider(
                    sliderX, curY, sliderW, 18,
                    (current - min) / (max - min),
                    normalized -> {
                        var val = module.getTransform(itemId, view)
                                .with(index, (float) (min + normalized * (max - min)));
                        module.setTransform(itemId, view, val, false);
                    },
                    normalized -> Component.literal(LABELS[index] + ": " + String.format(Locale.ROOT, "%.1f°", min + normalized * (max - min))),
                    true, ConfigManager::save
            ), "Winkel der " + (axis == 0 ? "Neigung (Pitch)" : (axis == 1 ? "Drehung (Yaw)" : "Kantung (Roll)"))));

            if (axis == 1) curY += 22;
        }
        curY += 26;

        // ════════════════════════════════════════
        // 4. SKALIERUNG (GRÖSSE)
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("SKALIERUNG (GRÖSSE)")));
        curY += 16;

        for (int axis = 0; axis < 3; axis++) {
            final int index = 6 + axis;
            float min = 0.05f;
            float max = 4.0f;
            float current = module.getTransform(itemId, view).value(index);
            int sliderX = (axis == 2) ? col1X : (axis == 0 ? col1X : col2X);
            int sliderW = (axis == 2) ? fullW : btnW;

            addRenderableWidget(described(new EzSlider(
                    sliderX, curY, sliderW, 18,
                    (current - min) / (max - min),
                    normalized -> {
                        var val = module.getTransform(itemId, view)
                                .with(index, (float) (min + normalized * (max - min)));
                        module.setTransform(itemId, view, val, false);
                    },
                    normalized -> Component.literal(LABELS[index] + ": " + String.format(Locale.ROOT, "%.2fx", min + normalized * (max - min))),
                    true, ConfigManager::save
            ), "Skalierungsfaktor auf der " + (axis == 0 ? "X-Achse" : (axis == 1 ? "Y-Achse" : "Z-Achse"))));

            if (axis == 1) curY += 22;
        }
        curY += 26;
        }
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        // Sidebar
        renderSettingsSidebar(g, panelX, panelY, panelHeight, module.getCategory());

        // Draw live item icon in sidebar
        if (!previewStack.isEmpty()) {
            g.item(previewStack, panelX + (sidebarWidth() - 16) / 2, panelY + 38);
        }

        // Header Title
        g.text(font, Component.literal(module.getDisplayName() + " " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()),
                settingsContentLeft(panelX), panelY + 10, EzUi.TEXT_WHITE);

        // Status indicator (e.g. • Individuell angepasst vs • Standard)
        boolean custom = module.hasOverride(itemId, view);
        String statusText = custom ? "• Individuell angepasst" : "• Standard";
        int statusColor = custom ? EzUi.ACCENT_EMERALD : EzUi.TEXT_DIM;
        g.text(font, Component.literal(statusText), panelX + panelWidth - font.width(statusText) - 62, panelY + 10, statusColor);

        super.extractSettings(g, mx, my, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isListeningForHotkey) {
            EzKeyBindings.applyModuleKeyBind(module, event.key());
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

    @Override
    public void onClose() {
        EzScreenBridge.set(minecraft, parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class CategoryHeader extends AbstractWidget {
        public CategoryHeader(int x, int y, int width, int height, Component title) {
            super(x, y, width, height, title);
            this.active = false;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            var font = Minecraft.getInstance().font;
            Component message = EzUi.fitText(getMessage(), getWidth());
            g.text(font, message, getX(), getY() + (getHeight() - 8) / 2, EzUi.TEXT_MUTED);
            int textW = font.width(message);
            int lineStartX = getX() + textW + 6;
            int lineEndX = getX() + getWidth();
            if (lineStartX < lineEndX) {
                int lineY = getY() + getHeight() / 2;
                g.fill(lineStartX, lineY, lineEndX, lineY + 1, EzUi.BORDER_SUBTLE);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
