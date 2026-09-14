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
    private static final int ROW_HEIGHT = 18;

    private record DisplayRow(String key, String displayName, int y) {}

    private final Screen parent;
    private final DamageTintModule module;
    private final List<DisplayRow> displayedRows = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();

    private int panelX, panelY, panelWidth, panelHeight;
    private String search = "";
    private EditBox searchBox;

    public DamageTintEntityScreen(Screen parent, DamageTintModule module) {
        super(Component.literal("Damage Tint: Entity-Regeln"));
        this.parent = parent;
        this.module = module;
    }

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 46; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 10; }

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
            displayedRows.add(new DisplayRow(key, displayName, y));

            Integer ruleColor = module.getEntityRule(key);
            boolean isCustom = ruleColor != null;

            // Mode Toggle Button: [ Default ] or [ Custom ]
            EzButton modeBtn = new EzButton(left + contentWidth - 84, y + 2, 48, 14,
                    Component.literal(isCustom ? "Custom" : app.ezclient.util.EzI18n.text("Default")), isCustom, b -> {
                if (isCustom) {
                    module.removeEntityRule(key);
                } else {
                    int col = (module.getCustomAlpha() << 24) | (module.getCustomColor() & 0x00FFFFFF);
                    module.setEntityRule(key, col);
                }
                populateRows();
            });
            addRenderableWidget(modeBtn);
            rowWidgets.add(modeBtn);

            // Color Swatch Button (only clickable when Custom)
            int effectiveColor = isCustom ? ruleColor : (module.getCustomAlpha() << 24) | (module.getCustomColor() & 0x00FFFFFF);
            ColorSwatchButton swatch = new ColorSwatchButton(left + contentWidth - 32, y + 2, 28, 14, effectiveColor, b -> {
                if (!isCustom) {
                    module.setEntityRule(key, effectiveColor);
                    populateRows();
                }
                EzScreenBridge.set(minecraft, new ModuleColorScreen(this, displayName, module.getEntityRule(key) != null ? module.getEntityRule(key) : effectiveColor, color -> {
                    module.setEntityRule(key, color);
                    b.setColor(color);
                }));
            });
            addRenderableWidget(swatch);
            rowWidgets.add(swatch);

            y += ROW_HEIGHT;
        }
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
            boolean hovered = mx >= left && mx < left + contentWidth
                    && (my + scrollAmount()) >= rowY && (my + scrollAmount()) < rowY + ROW_HEIGHT;

            if (hovered) {
                EzUi.roundedRect(g, left, rowY, contentWidth, ROW_HEIGHT, 2, 0x14FFFFFF);
            }
            g.fill(left, rowY + ROW_HEIGHT - 1, left + contentWidth, rowY + ROW_HEIGHT, 0x10FFFFFF);

            // Entity Icon
            ItemIconHelper.renderEntryIcon(g, row.key(), true, left + 4, rowY + 2);

            int textY = rowY + 6;
            g.text(font, row.displayName(), left + 24, textY, hovered ? EzUi.TEXT_WHITE : EzUi.TEXT_LIGHT);
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
