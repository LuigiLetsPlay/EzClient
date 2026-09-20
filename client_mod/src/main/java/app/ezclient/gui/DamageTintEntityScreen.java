package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Screen for configuring per-entity Damage Tint rules.
 */
public final class DamageTintEntityScreen extends ScrollingSettingsScreen {
    private static final int HEADER_HEIGHT = 22;
    private static final int EXPANDED_PANEL_HEIGHT = 48;

    private record DisplayRow(String key, String displayName, int y, int height, boolean expanded) {}

    private final Screen parent;
    private final DamageTintModule module;
    private final List<DisplayRow> displayedRows = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final java.util.Set<String> expandedKeys = new java.util.HashSet<>();

    private int panelX, panelY, panelWidth, panelHeight;
    private String search = "";
    private EditBox searchBox;
    private int totalContentBottom = 0;

    public DamageTintEntityScreen(Screen parent, DamageTintModule module) {
        super(Component.literal("Damage Tint: Custom Entity Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 46; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 10; }
    @Override protected int scrollContentBottom() { return totalContentBottom; }

    public static String formatEntityName(String key) {
        if (key == null || key.isBlank()) return "";
        String name = key;
        int colon = name.indexOf(':');
        if (colon >= 0) name = name.substring(colon + 1);
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.toString();
    }

    @Override
    protected void init() {
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        addFixedWidget(new EzButton(panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false, b -> onClose()));

        int resetWidth = 46;
        int searchWidth = contentWidth - resetWidth - 4;
        searchBox = new EditBox(font, left, panelY + 26, searchWidth, 16, Component.literal(app.ezclient.util.EzI18n.text("Entity suchen …")));
        searchBox.setHint(Component.literal(app.ezclient.util.EzI18n.text("Entity suchen …")));
        searchBox.setValue(search);
        searchBox.setMaxLength(80);
        searchBox.setResponder(val -> {
            search = val.trim().toLowerCase(Locale.ROOT);
            populateRows();
        });
        addFixedWidget(searchBox);

        addFixedWidget(new EzButton(left + searchWidth + 4, panelY + 26, resetWidth, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Reset")), false, b -> {
            module.clearEntityRules();
            populateRows();
        }));

        populateRows();
    }

    private void populateRows() {
        for (AbstractWidget w : rowWidgets) {
            removeWidget(w);
        }
        rowWidgets.clear();
        displayedRows.clear();

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);
        int y = scrollTop() + 2;

        var matching = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .filter(id -> {
                    if (search.isBlank()) return true;
                    String raw = id.toString().toLowerCase(Locale.ROOT);
                    String formatted = formatEntityName(id.toString()).toLowerCase(Locale.ROOT);
                    return raw.contains(search) || formatted.contains(search);
                })
                .sorted(Comparator.comparing(id -> formatEntityName(id.toString())))
                .toList();

        for (var id : matching) {
            String key = id.toString();
            String displayName = formatEntityName(key);
            boolean isExpanded = expandedKeys.contains(key);
            int rowHeight = isExpanded ? (HEADER_HEIGHT + EXPANDED_PANEL_HEIGHT + 4) : (HEADER_HEIGHT + 2);
            displayedRows.add(new DisplayRow(key, displayName, y, rowHeight, isExpanded));

            Integer ruleColor = module.getEntityRule(key);
            boolean isCustom = ruleColor != null;

            // Full-width clickable Header row button (toggles expand / collapse)
            int headerBtnW = contentWidth - 68;
            EzButton toggleHeaderBtn = new EzButton(left, y, headerBtnW, HEADER_HEIGHT,
                    Component.literal((isExpanded ? "▼ " : "▶ ") + "   " + displayName), isExpanded,
                    b -> {
                        if (isExpanded) expandedKeys.remove(key);
                        else expandedKeys.add(key);
                        populateRows();
                    });
            addRenderableWidget(toggleHeaderBtn);
            rowWidgets.add(toggleHeaderBtn);

            // Quick Status Pill on the right side of header
            String statusText = isCustom ? "Angepasst" : "Standard";
            EzButton statusBtn = new EzButton(left + contentWidth - 64, y + 2, 64, 16,
                    Component.literal(statusText), isCustom,
                    b -> {
                        if (isExpanded) expandedKeys.remove(key);
                        else expandedKeys.add(key);
                        populateRows();
                    });
            addRenderableWidget(statusBtn);
            rowWidgets.add(statusBtn);

            // ── Expanded Accordion Controls ──
            if (isExpanded) {
                int panelY = y + HEADER_HEIGHT + 2;
                int effectiveColor = isCustom ? ruleColor : ((module.getCustomAlpha() << 24) | (module.getCustomColor() & 0x00FFFFFF));

                // Row 1: Color button + Swatch
                EzButton colorBtn = new EzButton(left + 6, panelY + 4, 130, 16,
                        Component.literal("Schadensfarbe …"), true,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName + " (Damage)", effectiveColor, color -> {
                            module.setEntityRule(key, color);
                            populateRows();
                        })));
                addRenderableWidget(colorBtn);
                rowWidgets.add(colorBtn);

                ColorSwatchButton swatch = new ColorSwatchButton(left + 140, panelY + 4, 22, 16, effectiveColor,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName + " (Damage)", effectiveColor, color -> {
                            module.setEntityRule(key, color);
                            populateRows();
                        })));
                addRenderableWidget(swatch);
                rowWidgets.add(swatch);

                // Row 2: Reset & Collapse buttons
                EzButton resetBtn = new EzButton(left + 6, panelY + 26, 130, 16,
                        Component.literal("Auf Standard zurücksetzen"), isCustom,
                        b -> {
                            module.removeEntityRule(key);
                            populateRows();
                        });
                addRenderableWidget(resetBtn);
                rowWidgets.add(resetBtn);

                EzButton collapseBtn = new EzButton(left + contentWidth - 75, panelY + 26, 70, 16,
                        Component.literal("Einklappen ▲"), false,
                        b -> {
                            expandedKeys.remove(key);
                            populateRows();
                        });
                addRenderableWidget(collapseBtn);
                rowWidgets.add(collapseBtn);
            }

            y += rowHeight;
        }
        totalContentBottom = y + 10;
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, "Damage Tint");

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        g.text(font, title, left, panelY + 9, EzUi.TEXT_WHITE);
        int activeCount = module.getEntityRules().size();
        String counterText = activeCount > 0 ? "• " + activeCount + " angepasst" : "• Standard";
        int titleW = font.width(title);
        g.text(font, counterText, left + titleW + 6, panelY + 9, activeCount > 0 ? EzUi.ACCENT_EMERALD : EzUi.TEXT_DIM);

        g.enableScissor(scrollLeft(), scrollTop(), scrollRight(), scrollBottom());
        g.pose().pushMatrix();
        g.pose().translate(0, (float) -scrollAmount());

        for (DisplayRow row : displayedRows) {
            int rowY = row.y();
            if (row.expanded()) {
                EzUi.roundedRect(g, left, rowY + HEADER_HEIGHT, contentWidth, EXPANDED_PANEL_HEIGHT, 4, 0x22000000);
                EzUi.outline(g, left, rowY + HEADER_HEIGHT, contentWidth, EXPANDED_PANEL_HEIGHT, EzUi.BORDER_SUBTLE);
            }

            // Entity Icon inside header
            ItemIconHelper.renderEntryIcon(g, row.key(), true, left + 18, rowY + 3);
        }

        g.pose().popMatrix();
        g.disableScissor();

        super.extractSettings(g, mx, my, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) EzScreenBridge.set(minecraft, parent);
    }

    private static class ColorSwatchButton extends AbstractButton {
        private int color;
        private final java.util.function.Consumer<ColorSwatchButton> onClick;

        public ColorSwatchButton(int x, int y, int width, int height, int color, java.util.function.Consumer<ColorSwatchButton> onClick) {
            super(x, y, width, height, Component.empty());
            this.color = color;
            this.onClick = onClick;
        }

        public void setColor(int color) { this.color = color; }

        @Override public void onPress(InputWithModifiers input) { onClick.accept(this); }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 3, 0xFF0D121D);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 2, color);
            EzUi.outline(g, getX(), getY(), getWidth(), getHeight(), isHovered() ? 0xFFFFFFFF : 0x40FFFFFF);
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
