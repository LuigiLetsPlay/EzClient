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
import java.util.function.Consumer;

/**
 * High-density entity hitbox settings screen.
 * Displays ~11 entities simultaneously with 18px compact rows,
 * 3-state status pills, live color swatches, and inline thickness steppers.
 */
public final class EntityTypeSettingsScreen extends ScrollingSettingsScreen {
    private static final int HEADER_HEIGHT = 22;
    private static final int EXPANDED_PANEL_HEIGHT = 48;

    private record DisplayRow(String key, String displayName, int y, int height, boolean expanded) {}

    private final Screen parent;
    private final HitboxModule module;
    private final List<DisplayRow> displayedRows = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private final java.util.Set<String> expandedKeys = new java.util.HashSet<>();

    private int panelX, panelY, panelWidth, panelHeight;
    private String search = "";
    private EditBox searchBox;
    private int totalContentBottom = 0;

    public EntityTypeSettingsScreen(Screen parent, HitboxModule module) {
        super(Component.literal("Hitbox: Custom Entity Settings"));
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

    private HitboxModule.EntityRule value(String id) {
        var val = module.rule(id);
        return val == null ? new HitboxModule.EntityRule(true, module.tint("box", false), (float) module.number("width")) : val;
    }

    @Override
    protected void init() {
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        // Header close button ✕ in top right corner
        addFixedWidget(new EzButton(panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false, b -> onClose()));

        // Search Bar (height 16px) & compact Reset button
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
                Component.literal(app.ezclient.util.EzI18n.text("Reset")), false, b -> resetAll()));

        populateRows();
    }

    private void resetAll() {
        for (var id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            module.setRule(id.toString(), null);
        }
        populateRows();
    }

    private void populateRows() {
        // Remove only dynamic row widgets to preserve searchBox focus completely
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

            // Full-width clickable Header row button (toggles expand / collapse)
            int headerBtnW = contentWidth - 46;
            EzButton toggleHeaderBtn = new EzButton(left, y, headerBtnW, HEADER_HEIGHT,
                    Component.literal((isExpanded ? "▼ " : "▶ ") + "   " + displayName), isExpanded,
                    b -> {
                        if (isExpanded) expandedKeys.remove(key);
                        else expandedKeys.add(key);
                        populateRows();
                    });
            addRenderableWidget(toggleHeaderBtn);
            rowWidgets.add(toggleHeaderBtn);

            // Header Status Pill on right
            StatusPillButton pill = new StatusPillButton(left + contentWidth - 42, y + 3, 42, 16, key, module, this::populateRows);
            addRenderableWidget(pill);
            rowWidgets.add(pill);

            // ── Expanded Accordion Controls ──
            if (isExpanded) {
                int panelY = y + HEADER_HEIGHT + 2;
                var currentVal = value(key);

                // Row 1: Color button + swatch + thickness stepper
                EzButton colorBtn = new EzButton(left + 6, panelY + 4, 105, 16,
                        Component.literal("Hitbox-Farbe …"), true,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName + " (Hitbox)", currentVal.color(), color -> {
                            module.setRule(key, new HitboxModule.EntityRule(currentVal.enabled(), color, currentVal.width()));
                            populateRows();
                        })));
                addRenderableWidget(colorBtn);
                rowWidgets.add(colorBtn);

                ColorSwatchButton swatch = new ColorSwatchButton(left + 114, panelY + 4, 20, 16, currentVal.color(),
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName + " (Hitbox)", currentVal.color(), color -> {
                            module.setRule(key, new HitboxModule.EntityRule(currentVal.enabled(), color, currentVal.width()));
                            populateRows();
                        })));
                addRenderableWidget(swatch);
                rowWidgets.add(swatch);

                CompactStepperButton stepper = new CompactStepperButton(left + 138, panelY + 4, contentWidth - 144, 16, key, module, this::populateRows);
                addRenderableWidget(stepper);
                rowWidgets.add(stepper);

                // Row 2: Reset & Collapse buttons
                EzButton resetBtn = new EzButton(left + 6, panelY + 26, 130, 16,
                        Component.literal("Auf Standard zurücksetzen"), module.rule(key) != null,
                        b -> {
                            module.setRule(key, null);
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
        renderSettingsSidebar(g, panelX, panelY, panelHeight, "Hitbox");

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        // Header Title & Counter Badge
        g.text(font, title, left, panelY + 9, EzUi.TEXT_WHITE);
        int activeCount = (int) BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .filter(id -> module.rule(id.toString()) != null).count();
        String counterText = activeCount > 0 ? "• " + activeCount + " angepasst" : "• Standard";
        int titleW = font.width(title);
        g.text(font, counterText, left + titleW + 6, panelY + 9, activeCount > 0 ? EzUi.ACCENT_EMERALD : EzUi.TEXT_DIM);

        // Render scrollable rows (background highlight, divider, and formatted entity names)
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
        EzScreenBridge.set(minecraft, parent);
    }

    // ── Modern Compact Custom Widgets ──

    private static final class StatusPillButton extends AbstractButton {
        private final String key;
        private final HitboxModule module;
        private final Runnable onChange;

        public StatusPillButton(int x, int y, int width, int height, String key, HitboxModule module, Runnable onChange) {
            super(x, y, width, height, Component.empty());
            this.key = key;
            this.module = module;
            this.onChange = onChange;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            var current = module.rule(key);
            if (current == null) {
                // Auto -> An
                module.setRule(key, new HitboxModule.EntityRule(true, module.tint("box", false), (float) module.number("width")));
            } else if (current.enabled()) {
                // An -> Aus
                module.setRule(key, new HitboxModule.EntityRule(false, current.color(), current.width()));
            } else {
                // Aus -> Auto
                module.setRule(key, null);
            }
            if (onChange != null) onChange.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hovered = isHoveredOrFocused();
            var rule = module.rule(key);
            int bg, border, textColor;
            String text;
            if (rule == null) {
                text = "Auto";
                bg = hovered ? 0xFF1E2530 : 0xFF14181F;
                border = hovered ? 0xFF3E4959 : 0xFF2A313C;
                textColor = hovered ? 0xFFE2E8F0 : 0xFF94A3B8;
            } else if (rule.enabled()) {
                text = "An";
                bg = hovered ? 0xFF1B3625 : 0xFF132319;
                border = hovered ? 0xFF2FD17A : 0xFF22C96E;
                textColor = 0xFFFFFFFF;
            } else {
                text = "Aus";
                bg = hovered ? 0xFF2F181C : 0xFF1E1214;
                border = hovered ? 0xFF8A3A40 : 0xFF5A2A2E;
                textColor = hovered ? 0xFFFCA5A5 : 0xFFF87171;
            }

            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, border);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, bg);
            var font = Minecraft.getInstance().font;
            g.centeredText(font, Component.literal(text), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static final class ColorSwatchButton extends AbstractButton {
        private int color;
        private final Consumer<ColorSwatchButton> action;

        public ColorSwatchButton(int x, int y, int width, int height, int color, Consumer<ColorSwatchButton> action) {
            super(x, y, width, height, Component.empty());
            this.color = color;
            this.action = action;
        }

        public void setColor(int color) { this.color = color; }

        @Override
        public void onPress(InputWithModifiers input) {
            if (active && action != null) action.accept(this);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hovered = isHoveredOrFocused();
            int borderColor = hovered ? 0xFFFFFFFF : 0xFF3E4756;
            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, borderColor);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, 0xFF14181F);
            // Draw swatch with active color
            EzUi.roundedRect(g, getX() + 2, getY() + 2, getWidth() - 4, getHeight() - 4, 1, color);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static final class CompactStepperButton extends AbstractButton {
        private final String key;
        private final HitboxModule module;
        private final Runnable onChange;

        public CompactStepperButton(int x, int y, int width, int height, String key, HitboxModule module, Runnable onChange) {
            super(x, y, width, height, Component.empty());
            this.key = key;
            this.module = module;
            this.onChange = onChange;
        }

        private void step(int direction) {
            var rule = module.rule(key);
            boolean enabled = rule == null || rule.enabled();
            int color = rule == null ? module.tint("box", false) : rule.color();
            float currentWidth = rule == null ? (float) module.number("width") : rule.width();
            float nextWidth = Math.max(1.0f, Math.min(3.0f, Math.round((currentWidth + direction * 0.5f) * 10.0f) / 10.0f));
            module.setRule(key, new HitboxModule.EntityRule(enabled, color, nextWidth));
            if (onChange != null) onChange.run();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (active && visible && event.x() >= getX() && event.x() < getX() + getWidth()
                    && event.y() >= getY() && event.y() < getY() + getHeight()) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                if (event.button() == 1 || event.x() < getX() + getWidth() / 2.0) {
                    step(-1);
                } else {
                    step(1);
                }
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public void onPress(InputWithModifiers input) {
            step(1);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hovered = isHoveredOrFocused();
            int bg = hovered ? 0xFF1E2530 : 0xFF14181F;
            int border = hovered ? 0xFF3E4959 : 0xFF2A313C;
            int textColor = hovered ? 0xFFFFFFFF : 0xFFCBD5E1;

            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, border);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, bg);

            var rule = module.rule(key);
            float widthVal = rule == null ? (float) module.number("width") : rule.width();
            String text = String.format(Locale.ROOT, "‹ %.1f ›", widthVal);

            var font = Minecraft.getInstance().font;
            g.centeredText(font, Component.literal(text), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
