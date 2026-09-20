package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * Screen for configuring per-block Block Overlay rules with expandable accordion rows.
 */
public final class BlockSettingsScreen extends ScrollingSettingsScreen {
    private static final int HEADER_HEIGHT = 22;
    private static final int EXPANDED_PANEL_HEIGHT = 70;

    private record DisplayRow(String key, String displayName, int y, int height, boolean expanded) {}

    private final Screen parent;
    private final BlockOverlayModule module;
    private final List<DisplayRow> displayedRows = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final Set<String> expandedKeys = new HashSet<>();

    private int panelX, panelY, panelWidth, panelHeight;
    private String search = "";
    private EditBox searchBox;
    private int totalContentBottom = 0;

    public BlockSettingsScreen(Screen parent, BlockOverlayModule module) {
        super(Component.literal("Block Overlay: Custom Block Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 46; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 10; }
    @Override protected int scrollContentBottom() { return totalContentBottom; }

    public static String formatBlockName(String key) {
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

        // Header Close Button ✕
        addFixedWidget(new EzButton(panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false, b -> onClose()));

        int resetWidth = 46;
        int searchWidth = contentWidth - resetWidth - 4;
        searchBox = new EditBox(font, left, panelY + 26, searchWidth, 16, Component.literal(app.ezclient.util.EzI18n.text("Block suchen …")));
        searchBox.setHint(Component.literal(app.ezclient.util.EzI18n.text("Block suchen …")));
        searchBox.setValue(search);
        searchBox.setMaxLength(80);
        searchBox.setResponder(val -> {
            search = val.trim().toLowerCase(Locale.ROOT);
            populateRows();
        });
        addFixedWidget(searchBox);

        addFixedWidget(new EzButton(left + searchWidth + 4, panelY + 26, resetWidth, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Reset")), false, b -> {
            module.clearBlockRules();
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

        var matching = BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> {
                    if (search.isBlank()) return true;
                    String raw = id.toString().toLowerCase(Locale.ROOT);
                    String formatted = formatBlockName(id.toString()).toLowerCase(Locale.ROOT);
                    return raw.contains(search) || formatted.contains(search);
                })
                .sorted(Comparator.comparing(id -> formatBlockName(id.toString())))
                .toList();

        for (var id : matching) {
            String key = id.toString();
            String displayName = formatBlockName(key);
            boolean isExpanded = expandedKeys.contains(key);
            int rowHeight = isExpanded ? (HEADER_HEIGHT + EXPANDED_PANEL_HEIGHT + 4) : (HEADER_HEIGHT + 2);
            displayedRows.add(new DisplayRow(key, displayName, y, rowHeight, isExpanded));

            BlockOverlayModule.BlockRule rule = module.getBlockRule(key);
            boolean isCustom = rule != null;

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

                boolean outActive = rule != null
                        ? (!"Fill".equalsIgnoreCase(rule.style()) && !"None".equalsIgnoreCase(rule.style()))
                        : module.isOutlineActive();
                boolean fillActive = rule != null
                        ? (!"Outline".equalsIgnoreCase(rule.style()) && !"None".equalsIgnoreCase(rule.style()))
                        : module.isFillActive();

                int outlineCol = rule != null ? rule.outlineColor() : module.tint("outline", false);
                int fillCol = rule != null ? rule.fillColor() : module.tint("fill", false);
                double op = rule != null ? rule.fillOpacity() : module.number("fillOpacity");

                // Row 1: Outline Controls
                EzButton outToggle = new EzButton(left + 6, panelY + 4, 82, 16,
                        Component.literal("Outline: " + (outActive ? "An" : "Aus")), outActive,
                        b -> {
                            boolean newOut = !outActive;
                            updateBlockRule(key, newOut, fillActive, outlineCol, fillCol, op);
                            populateRows();
                        });
                addRenderableWidget(outToggle);
                rowWidgets.add(outToggle);

                EzButton outColorBtn = new EzButton(left + 92, panelY + 4, 96, 16,
                        Component.literal("Outline-Farbe …"), true,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName + " (Outline)", outlineCol, color -> {
                            updateBlockRule(key, outActive, fillActive, color, fillCol, op);
                            populateRows();
                        })));
                addRenderableWidget(outColorBtn);
                rowWidgets.add(outColorBtn);

                ColorSwatchButton outSwatch = new ColorSwatchButton(left + 191, panelY + 4, 20, 16, outlineCol,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName + " (Outline)", outlineCol, color -> {
                            updateBlockRule(key, outActive, fillActive, color, fillCol, op);
                            populateRows();
                        })));
                addRenderableWidget(outSwatch);
                rowWidgets.add(outSwatch);

                // Row 2: Filling Controls
                EzButton fillToggle = new EzButton(left + 6, panelY + 24, 82, 16,
                        Component.literal("Filling: " + (fillActive ? "An" : "Aus")), fillActive,
                        b -> {
                            boolean newFill = !fillActive;
                            updateBlockRule(key, outActive, newFill, outlineCol, fillCol, op);
                            populateRows();
                        });
                addRenderableWidget(fillToggle);
                rowWidgets.add(fillToggle);

                EzButton fillColorBtn = new EzButton(left + 92, panelY + 24, 96, 16,
                        Component.literal("Füll-Farbe …"), true,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName + " (Fill)", fillCol, color -> {
                            updateBlockRule(key, outActive, fillActive, outlineCol, color, op);
                            populateRows();
                        })));
                addRenderableWidget(fillColorBtn);
                rowWidgets.add(fillColorBtn);

                ColorSwatchButton fillSwatch = new ColorSwatchButton(left + 191, panelY + 24, 20, 16, fillCol,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName + " (Fill)", fillCol, color -> {
                            updateBlockRule(key, outActive, fillActive, outlineCol, color, op);
                            populateRows();
                        })));
                addRenderableWidget(fillSwatch);
                rowWidgets.add(fillSwatch);

                int stepperW = contentWidth - 219;
                EzButton opStepper = new EzButton(left + 215, panelY + 24, stepperW, 16,
                        Component.literal("‹ Deckk: " + Math.round(op) + "% ›"), true,
                        b -> {
                            double nextOp = (op + 10 > 100) ? 0 : op + 10;
                            updateBlockRule(key, outActive, fillActive, outlineCol, fillCol, nextOp);
                            populateRows();
                        }).withRightClick(b -> {
                            double prevOp = (op - 10 < 0) ? 100 : op - 10;
                            updateBlockRule(key, outActive, fillActive, outlineCol, fillCol, prevOp);
                            populateRows();
                        });
                addRenderableWidget(opStepper);
                rowWidgets.add(opStepper);

                // Row 3: Action Buttons
                EzButton resetRuleBtn = new EzButton(left + 6, panelY + 46, 120, 16,
                        Component.literal("Auf Standard zurücksetzen"), isCustom,
                        b -> {
                            module.removeBlockRule(key);
                            populateRows();
                        });
                addRenderableWidget(resetRuleBtn);
                rowWidgets.add(resetRuleBtn);

                EzButton collapseBtn = new EzButton(left + contentWidth - 75, panelY + 46, 70, 16,
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

    private void updateBlockRule(String key, boolean outline, boolean fill, int outlineColor, int fillColor, double fillOpacity) {
        String style;
        if (outline && fill) style = "Both";
        else if (outline) style = "Outline";
        else if (fill) style = "Fill";
        else style = "None";

        module.setBlockRule(key, new BlockOverlayModule.BlockRule(style, outlineColor, fillColor, fillOpacity));
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, "Block Overlay");

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        // Header Title & Counter Badge
        g.text(font, title, left, panelY + 9, EzUi.TEXT_WHITE);
        int activeCount = module.getBlockRules().size();
        String counterText = activeCount > 0 ? "• " + activeCount + " angepasst" : "• Standard";
        int titleW = font.width(title);
        g.text(font, counterText, left + titleW + 6, panelY + 9, activeCount > 0 ? EzUi.ACCENT_EMERALD : EzUi.TEXT_DIM);

        g.enableScissor(scrollLeft(), scrollTop(), scrollRight(), scrollBottom());
        g.pose().pushMatrix();
        g.pose().translate(0, (float) -scrollAmount());

        for (DisplayRow row : displayedRows) {
            int rowY = row.y();
            if (row.expanded()) {
                // Background card for expanded block panel
                EzUi.roundedRect(g, left, rowY + HEADER_HEIGHT, contentWidth, EXPANDED_PANEL_HEIGHT, 4, 0x22000000);
                EzUi.outline(g, left, rowY + HEADER_HEIGHT, contentWidth, EXPANDED_PANEL_HEIGHT, EzUi.BORDER_SUBTLE);
            }

            // Render 3D Item/Block Icon directly inside header
            ItemIconHelper.renderEntryIcon(g, row.key(), false, left + 18, rowY + 3);
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
