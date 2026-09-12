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
 * Screen for configuring per-entity and per-block custom crosshair rules.
 * Supports custom colors, scales, and crosshair styles for any Entity or Block.
 */
public final class CrosshairTargetSettingsScreen extends ScrollingSettingsScreen {
    private static final int ROW_HEIGHT = 20;

    public enum TargetCategory { ALL, ENTITIES, BLOCKS }

    private record DisplayRow(String key, String displayName, boolean isEntity, int y) {}

    private final Screen parent;
    private final CrosshairModule module;
    private final List<DisplayRow> displayedRows = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();

    private int panelX, panelY, panelWidth, panelHeight;
    private String search = "";
    private EditBox searchBox;
    private TargetCategory currentCategory = TargetCategory.ALL;

    public CrosshairTargetSettingsScreen(Screen parent, CrosshairModule module) {
        super(Component.literal("Crosshair: Ziel-Regeln (Entity & Block)"));
        this.parent = parent;
        this.module = module;
    }

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 68; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 10; }

    public static String formatName(String key) {
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

    private CrosshairModule.CrosshairTargetRule getOrFallback(String key) {
        var rule = module.getTargetRule(key);
        if (rule != null) return rule;
        int defaultColor = module.getTextColor();
        return new CrosshairModule.CrosshairTargetRule(defaultColor, 1.0f, "AUTO", "");
    }

    @Override
    protected void init() {
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        // Header close button ✕
        addFixedWidget(new EzButton(panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false, b -> onClose()));

        // Row 1: Search Box & Reset All Button
        int resetWidth = 46;
        int searchWidth = contentWidth - resetWidth - 4;
        searchBox = new EditBox(font, left, panelY + 26, searchWidth, 16, Component.literal("Entity / Block suchen …"));
        searchBox.setHint(Component.literal("Entity oder Block suchen …"));
        searchBox.setValue(search);
        searchBox.setMaxLength(80);
        searchBox.setResponder(val -> {
            search = val.trim().toLowerCase(Locale.ROOT);
            populateRows();
        });
        addFixedWidget(searchBox);

        addFixedWidget(new EzButton(left + searchWidth + 4, panelY + 26, resetWidth, 16,
                Component.literal("Reset"), false, b -> {
            module.clearTargetRules();
            ConfigManager.save();
            populateRows();
        }));

        // Row 2: Category Filter Tabs: [ Alle ] [ Entities ] [ Blöcke ]
        int tabY = panelY + 46;
        int tabH = 16;
        int tabSpacing = 4;
        int tabW = (contentWidth - tabSpacing * 2) / 3;

        addFixedWidget(new EzButton(left, tabY, tabW, tabH,
                Component.literal("Alle"), currentCategory == TargetCategory.ALL,
                b -> { currentCategory = TargetCategory.ALL; rebuildWidgets(); }));

        addFixedWidget(new EzButton(left + tabW + tabSpacing, tabY, tabW, tabH,
                Component.literal("Entities"), currentCategory == TargetCategory.ENTITIES,
                b -> { currentCategory = TargetCategory.ENTITIES; rebuildWidgets(); }));

        addFixedWidget(new EzButton(left + (tabW + tabSpacing) * 2, tabY, contentWidth - (tabW + tabSpacing) * 2, tabH,
                Component.literal("Blöcke"), currentCategory == TargetCategory.BLOCKS,
                b -> { currentCategory = TargetCategory.BLOCKS; rebuildWidgets(); }));

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

        List<DisplayRow> allMatching = new ArrayList<>();

        if (currentCategory == TargetCategory.ALL || currentCategory == TargetCategory.ENTITIES) {
            for (var id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                String raw = id.toString().toLowerCase(Locale.ROOT);
                String formatted = formatName(id.getPath()).toLowerCase(Locale.ROOT);
                if (search.isBlank() || raw.contains(search) || formatted.contains(search)) {
                    allMatching.add(new DisplayRow("entity:" + id.toString(), formatName(id.getPath()), true, 0));
                }
            }
        }

        if (currentCategory == TargetCategory.ALL || currentCategory == TargetCategory.BLOCKS) {
            for (var id : BuiltInRegistries.BLOCK.keySet()) {
                String raw = id.toString().toLowerCase(Locale.ROOT);
                String formatted = formatName(id.getPath()).toLowerCase(Locale.ROOT);
                if (search.isBlank() || raw.contains(search) || formatted.contains(search)) {
                    allMatching.add(new DisplayRow("block:" + id.toString(), formatName(id.getPath()), false, 0));
                }
            }
        }

        allMatching.sort(Comparator.comparing(DisplayRow::displayName));

        for (DisplayRow item : allMatching) {
            String key = item.key();
            String displayName = item.displayName();
            boolean isEntity = item.isEntity();

            displayedRows.add(new DisplayRow(key, displayName, isEntity, y));

            // Controls on right:
            // 1. Auto/Custom Mode Button (w: 48 or 32, h: 14) + Edit Button (w: 14, h: 14) if Custom
            // 2. Color Swatch (w: 24, h: 14)
            // 3. Scale Stepper (w: 38, h: 14)
            // 4. Reset Button (w: 14, h: 14)
            int ctrlX = left + contentWidth - 136;
            var currentRule = module.getTargetRule(key);
            boolean isCustom = currentRule != null && "CUSTOM".equalsIgnoreCase(currentRule.type());

            if (isCustom) {
                ModeToggleButton modeBtn = new ModeToggleButton(ctrlX, y + 3, 32, 14, key, displayName, module, this, this::populateRows);
                addRenderableWidget(modeBtn);
                rowWidgets.add(modeBtn);

                EditPatternButton editBtn = new EditPatternButton(ctrlX + 34, y + 3, 14, 14, key, displayName, module, this);
                addRenderableWidget(editBtn);
                rowWidgets.add(editBtn);
            } else {
                ModeToggleButton modeBtn = new ModeToggleButton(ctrlX, y + 3, 48, 14, key, displayName, module, this, this::populateRows);
                addRenderableWidget(modeBtn);
                rowWidgets.add(modeBtn);
            }

            ColorSwatchButton swatch = new ColorSwatchButton(ctrlX + 52, y + 3, 24, 14, getOrFallback(key).color(), b -> {
                EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName, getOrFallback(key).color(), color -> {
                    var current = getOrFallback(key);
                    module.setTargetRule(key, new CrosshairModule.CrosshairTargetRule(color, current.scale(), current.type(), current.pattern()));
                    b.setColor(color);
                    ConfigManager.save();
                }));
            });
            addRenderableWidget(swatch);
            rowWidgets.add(swatch);

            ScaleStepperButton stepper = new ScaleStepperButton(ctrlX + 80, y + 3, 38, 14, key, module, this::populateRows);
            addRenderableWidget(stepper);
            rowWidgets.add(stepper);

            ResetRuleButton resetBtn = new ResetRuleButton(ctrlX + 122, y + 3, 14, 14, key, module, this::populateRows);
            addRenderableWidget(resetBtn);
            rowWidgets.add(resetBtn);

            y += ROW_HEIGHT;
        }
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, "Crosshair");

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        // Header Title & Active Rule Counter
        g.text(font, title, left, panelY + 9, EzUi.TEXT_WHITE);
        int activeCount = module.getTargetRules().size();
        String counterText = activeCount > 0 ? "• " + activeCount + " aktiv" : "• Standard";
        int titleW = font.width(title);
        g.text(font, counterText, left + titleW + 6, panelY + 9, activeCount > 0 ? EzUi.ACCENT_EMERALD : EzUi.TEXT_DIM);

        // Render scrollable rows
        g.enableScissor(scrollLeft(), scrollTop(), scrollRight(), scrollBottom());
        g.pose().pushMatrix();
        g.pose().translate(0, (float) -scrollAmount());

        for (DisplayRow row : displayedRows) {
            int rowY = row.y();
            boolean hovered = mx >= left && mx < left + contentWidth
                    && (my + scrollAmount()) >= rowY && (my + scrollAmount()) < rowY + ROW_HEIGHT;

            if (hovered) {
                EzUi.roundedRect(g, left, rowY, contentWidth, ROW_HEIGHT, 2, 0x14FFFFFF);
            }
            // 1px divider
            g.fill(left, rowY + ROW_HEIGHT - 1, left + contentWidth, rowY + ROW_HEIGHT, 0x10FFFFFF);

            // 3D Block or Mob Head / Spawn Egg Icon
            ItemIconHelper.renderEntryIcon(g, row.key(), row.isEntity(), left + 4, rowY + 2);

            // Display Name
            int textY = rowY + 6;
            int textColor = hovered ? EzUi.TEXT_WHITE : EzUi.TEXT_LIGHT;
            if (module.getTargetRule(row.key()) != null) {
                textColor = EzUi.ACCENT_EMERALD;
            }
            g.text(font, row.displayName(), left + 24, textY, textColor);
        }

        g.pose().popMatrix();
        g.disableScissor();

        super.extractSettings(g, mx, my, delta);
    }

    @Override
    public void onClose() {
        EzScreenBridge.set(minecraft, parent);
    }

    // ── Row Control Widgets ──

    private static final class ModeToggleButton extends AbstractButton {
        private final String key;
        private final String displayName;
        private final CrosshairModule module;
        private final Screen parent;
        private final Runnable onChange;

        public ModeToggleButton(int x, int y, int width, int height, String key, String displayName, CrosshairModule module, Screen parent, Runnable onChange) {
            super(x, y, width, height, Component.empty());
            this.key = key;
            this.displayName = displayName;
            this.module = module;
            this.parent = parent;
            this.onChange = onChange;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            var rule = module.getTargetRule(key);
            boolean isCustom = rule != null && "CUSTOM".equalsIgnoreCase(rule.type());
            if (isCustom) {
                // Switch back to Auto
                module.setTargetRule(key, new CrosshairModule.CrosshairTargetRule(
                        rule.color(), rule.scale(), "AUTO", rule.pattern() != null ? rule.pattern() : ""));
                ConfigManager.save();
                if (onChange != null) onChange.run();
            } else {
                // Switch to Custom and open paint screen
                int color = rule != null ? rule.color() : module.getTextColor();
                float scale = rule != null ? rule.scale() : 1.0f;
                String pat = rule != null && rule.pattern() != null ? rule.pattern() : "";
                module.setTargetRule(key, new CrosshairModule.CrosshairTargetRule(color, scale, "CUSTOM", pat));
                ConfigManager.save();
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    EzScreenBridge.set(mc, new CrosshairPaintScreen(parent, module, key, displayName));
                }
            }
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hovered = isHoveredOrFocused();
            var rule = module.getTargetRule(key);
            boolean isCustom = rule != null && "CUSTOM".equalsIgnoreCase(rule.type());
            String label = isCustom ? "Custom" : "Auto";

            int bg = isCustom ? 0xFF142C20 : (hovered ? 0xFF1E2530 : 0xFF14181F);
            int border = isCustom ? 0xFF22C55E : (hovered ? 0xFF3E4959 : 0xFF2A313C);
            int textColor = isCustom ? 0xFF4ADE80 : (hovered ? 0xFFFFFFFF : 0xFF94A3B8);

            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, border);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, bg);

            var font = Minecraft.getInstance().font;
            g.centeredText(font, Component.literal(label), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static final class EditPatternButton extends AbstractButton {
        private final String key;
        private final String displayName;
        private final CrosshairModule module;
        private final Screen parent;

        public EditPatternButton(int x, int y, int width, int height, String key, String displayName, CrosshairModule module, Screen parent) {
            super(x, y, width, height, Component.empty());
            this.key = key;
            this.displayName = displayName;
            this.module = module;
            this.parent = parent;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                EzScreenBridge.set(mc, new CrosshairPaintScreen(parent, module, key, displayName));
            }
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hovered = isHoveredOrFocused();
            int bg = hovered ? 0xFF1E2C22 : 0xFF142018;
            int border = hovered ? 0xFF4ADE80 : 0xFF22C55E;
            int textColor = hovered ? 0xFFFFFFFF : 0xFF4ADE80;

            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, border);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, bg);

            var font = Minecraft.getInstance().font;
            g.centeredText(font, Component.literal("✎"), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static final class ColorSwatchButton extends AbstractButton {
        private int color;
        private final Consumer<ColorSwatchButton> onClick;

        public ColorSwatchButton(int x, int y, int width, int height, int color, Consumer<ColorSwatchButton> onClick) {
            super(x, y, width, height, Component.empty());
            this.color = color;
            this.onClick = onClick;
        }

        public void setColor(int color) { this.color = color; }

        @Override
        public void onPress(InputWithModifiers input) {
            if (onClick != null) onClick.accept(this);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hovered = isHoveredOrFocused();
            int borderColor = hovered ? 0xFFFFFFFF : 0xFF3E4959;
            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, borderColor);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, 0xFF14181F);
            EzUi.roundedRect(g, getX() + 2, getY() + 2, getWidth() - 4, getHeight() - 4, 1, color);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static final class ScaleStepperButton extends AbstractButton {
        private final String key;
        private final CrosshairModule module;
        private final Runnable onChange;

        public ScaleStepperButton(int x, int y, int width, int height, String key, CrosshairModule module, Runnable onChange) {
            super(x, y, width, height, Component.empty());
            this.key = key;
            this.module = module;
            this.onChange = onChange;
        }

        private void step(int direction) {
            var rule = module.getTargetRule(key);
            int color = rule != null ? rule.color() : module.getTextColor();
            String type = rule != null && rule.type() != null ? rule.type() : "AUTO";
            String pattern = rule != null && rule.pattern() != null ? rule.pattern() : "";
            float currentScale = rule != null ? rule.scale() : 1.0f;
            float nextScale = Math.max(0.5f, Math.min(2.0f, Math.round((currentScale + direction * 0.1f) * 10.0f) / 10.0f));
            module.setTargetRule(key, new CrosshairModule.CrosshairTargetRule(color, nextScale, type, pattern));
            ConfigManager.save();
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
            var rule = module.getTargetRule(key);
            float scaleVal = rule != null ? rule.scale() : 1.0f;
            boolean custom = rule != null && Math.abs(scaleVal - 1.0f) > 0.01f;

            int bg = custom ? 0xFF142C20 : (hovered ? 0xFF1E2530 : 0xFF14181F);
            int border = custom ? 0xFF22C55E : (hovered ? 0xFF3E4959 : 0xFF2A313C);
            int textColor = custom ? 0xFF4ADE80 : (hovered ? 0xFFFFFFFF : 0xFF94A3B8);

            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, border);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, bg);

            String text = String.format(Locale.ROOT, "‹ %.1fx ›", scaleVal);
            var font = Minecraft.getInstance().font;
            g.centeredText(font, Component.literal(text), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private static final class ResetRuleButton extends AbstractButton {
        private final String key;
        private final CrosshairModule module;
        private final Runnable onChange;

        public ResetRuleButton(int x, int y, int width, int height, String key, CrosshairModule module, Runnable onChange) {
            super(x, y, width, height, Component.empty());
            this.key = key;
            this.module = module;
            this.onChange = onChange;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            module.removeTargetRule(key);
            ConfigManager.save();
            if (onChange != null) onChange.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hovered = isHoveredOrFocused();
            boolean activeRule = module.getTargetRule(key) != null;
            int bg = hovered ? 0xFF2E1A1A : 0xFF14181F;
            int border = hovered ? 0xFFEF4444 : (activeRule ? 0xFF7F1D1D : 0xFF2A313C);
            int textColor = activeRule ? (hovered ? 0xFFFF4444 : 0xFFEF4444) : 0xFF475569;

            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, border);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, bg);

            var font = Minecraft.getInstance().font;
            g.centeredText(font, Component.literal("✕"), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
