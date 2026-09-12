package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Screen for toggling individual particle types with real-time search.
 */
public final class ParticleTypesScreen extends ScrollingSettingsScreen {
    private static final int ROW_HEIGHT = 18;

    private record DisplayRow(Identifier id, String displayName, int y) {}

    private final Screen parent;
    private final ParticleCustomizerModule module;
    private final List<DisplayRow> displayedRows = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();

    private int panelX, panelY, panelWidth, panelHeight;
    private String search = "";
    private EditBox searchBox;

    public ParticleTypesScreen(Screen parent, ParticleCustomizerModule module) {
        super(Component.literal("Partikel-Typen"));
        this.parent = parent;
        this.module = module;
    }

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 46; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 10; }

    @Override
    protected int scrollContentBottom() {
        return displayedRows.isEmpty() ? scrollTop() : displayedRows.get(displayedRows.size() - 1).y() + ROW_HEIGHT + 6;
    }

    @Override
    protected void init() {
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        // Header close button
        addFixedWidget(new EzButton(panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false, b -> onClose()));

        // Search Bar & quick action buttons
        int btnWidth = 44;
        int searchWidth = contentWidth - (btnWidth * 2) - 8;
        searchBox = new EditBox(font, left, panelY + 26, searchWidth, 16, Component.literal("Partikel suchen …"));
        searchBox.setHint(Component.literal("Partikel suchen …"));
        searchBox.setValue(search);
        searchBox.setMaxLength(80);
        searchBox.setResponder(val -> {
            search = val.trim().toLowerCase(Locale.ROOT);
            populateRows();
        });
        addFixedWidget(searchBox);

        // Alle an
        addFixedWidget(new EzButton(left + searchWidth + 4, panelY + 26, btnWidth, 16,
                Component.literal("Alle an"), false, b -> {
            module.enableAllParticles();
            populateRows();
        }));

        // Alle aus
        addFixedWidget(new EzButton(left + searchWidth + 4 + btnWidth + 4, panelY + 26, btnWidth, 16,
                Component.literal("Alle aus"), false, b -> {
            module.disableAllParticles();
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

        var matching = BuiltInRegistries.PARTICLE_TYPE.keySet().stream()
                .filter(id -> {
                    if (search.isBlank()) return true;
                    String raw = id.toString().toLowerCase(Locale.ROOT);
                    String formatted = ParticleCustomizerModule.displayName(id).toLowerCase(Locale.ROOT);
                    return raw.contains(search) || formatted.contains(search);
                })
                .sorted(Comparator.comparing(ParticleCustomizerModule::displayName))
                .toList();

        for (var id : matching) {
            String displayName = ParticleCustomizerModule.displayName(id);
            displayedRows.add(new DisplayRow(id, displayName, y));

            boolean enabled = module.isParticleEnabled(id);

            // Toggle Button (width 38, height 14) -> [ AN / AUS ]
            EzButton toggle = new EzButton(left + contentWidth - 42, y + 2, 38, 14,
                    Component.literal(enabled ? "AN" : "AUS"), enabled, b -> {
                module.setParticleEnabled(id, !enabled);
                populateRows();
            });
            addRenderableWidget(toggle);
            rowWidgets.add(toggle);

            y += ROW_HEIGHT;
        }
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, "Typen");

        int left = settingsContentLeft(panelX);
        int contentWidth = settingsContentWidth(panelWidth);

        // Header Title
        g.text(font, title, left, panelY + 9, EzUi.TEXT_WHITE);

        // Total count badge
        int total = BuiltInRegistries.PARTICLE_TYPE.size();
        int activeCount = total - module.getDisabledCount();
        String counterText = "• " + activeCount + " / " + total + " aktiv";
        int titleW = font.width(title);
        g.text(font, counterText, left + titleW + 6, panelY + 9, activeCount < total ? EzUi.ACCENT_EMERALD : EzUi.TEXT_DIM);

        // Render scrollable rows inside scissor box
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
            // 1px subtle divider
            g.fill(left, rowY + ROW_HEIGHT - 1, left + contentWidth, rowY + ROW_HEIGHT, 0x10FFFFFF);

            // Display Name
            g.text(font, row.displayName(), left + 4, rowY + 5, hovered ? EzUi.TEXT_WHITE : EzUi.TEXT_LIGHT);

            // Namespace / ID subtext
            String path = row.id().toString();
            int nameW = font.width(row.displayName());
            g.text(font, path, left + nameW + 10, rowY + 5, 0xFF475569);
        }

        g.pose().popMatrix();
        g.disableScissor();

        super.extractSettings(g, mx, my, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) EzScreenBridge.set(minecraft, parent);
    }
}
