package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Responsive module workspace with a modern glass-panel layout. */
public final class EzClientScreen extends Screen {
    private static final String[] FILTERS = {"All", "HUD", "Utils", "Visual", "Render"};
    private final Screen parent;
    private String selectedFilter = "All";
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public EzClientScreen(Screen parent) {
        super(Component.literal("EzClient Modules"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(680, width - 28);
        panelHeight = Math.min(410, height - 28);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int sidebarX = panelX + 16;
        int filterY = panelY + 80;
        for (String filter : FILTERS) {
            boolean selected = filter.equals(selectedFilter);
            addRenderableWidget(new EzButton(
                    sidebarX, filterY, 88, 24, Component.literal(filter), selected,
                    ignored -> {
                        selectedFilter = filter;
                        rebuildWidgets();
                    }
            ));
            filterY += 30;
        }

        addRenderableWidget(new EzButton(
                panelX + 16, panelY + panelHeight - 38, 88, 24,
                Component.literal("Close"), false, ignored -> onClose()
        ));

        int contentX = panelX + 124;
        int contentY = panelY + 76;
        int contentWidth = panelWidth - 142;
        int columns = Math.max(1, Math.min(3, contentWidth / 150));
        int gap = 10;
        int cardWidth = (contentWidth - (columns - 1) * gap) / columns;
        int cardHeight = 58;
        int index = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!selectedFilter.equals("All") && !module.getCategory().equals(selectedFilter)) continue;
            int x = contentX + (index % columns) * (cardWidth + gap);
            int y = contentY + (index / columns) * (cardHeight + gap);
            if (y + cardHeight > panelY + panelHeight - 18) break;
            if (module instanceof ZoomModule) addZoomModuleControls(x, y, cardWidth);
            else addToggleModuleControls(module, x, y, cardWidth);
            index++;
        }
    }

    private void addToggleModuleControls(Module module, int x, int y, int cardWidth) {
        if (module instanceof HudModule hud) {
            addRenderableWidget(new EzButton(x + 12, y + 34, 66, 16, Component.literal("Configure"), false,
                    ignored -> minecraft.gui.setScreen(new HudSettingsScreen(this, hud))));
        }
        addRenderableWidget(new EzButton(
                x + cardWidth - 46, y + 10, 34, 18,
                Component.literal(module.isEnabled() ? "ON" : "OFF"), module.isEnabled(),
                ignored -> { module.toggle(); rebuildWidgets(); }
        ));
    }

    private void addZoomModuleControls(int x, int y, int cardWidth) {
        ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
        addRenderableWidget(new EzButton(
                x + 12, y + 34, 66, 16, Component.literal("Configure"),
                false,
                ignored -> minecraft.gui.setScreen(new ZoomSettingsScreen(this))
        ));
        addRenderableWidget(new EzButton(
                x + cardWidth - 46, y + 10, 34, 18,
                Component.literal(zoom.isEnabled() ? "ON" : "OFF"),
                zoom.isEnabled(),
                button -> {
                    zoom.toggle();
                    rebuildWidgets();
                }
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);
        EzUi.panel(graphics, panelX, panelY, panelWidth, panelHeight);
        EzUi.roundedRect(graphics, panelX + 10, panelY + 10, panelWidth - 20, 52, 12, 0xFF252B37);
        EzUi.roundedRect(graphics, panelX + 18, panelY + 20, 30, 30, 15, 0xFF8B6CF6);
        graphics.centeredText(font, "EZ", panelX + 33, panelY + 31, 0xFFFFFFFF);
        graphics.text(font, "EzClient", panelX + 58, panelY + 21, 0xFFFFFFFF);
        graphics.text(font, "Modules  •  " + selectedFilter, panelX + 58, panelY + 37, 0xFFABB5C7);
        graphics.text(font, "CATEGORIES", panelX + 17, panelY + 66, 0xFF8995AA);
        graphics.text(font, "MODULES", panelX + 124, panelY + 66, 0xFF8995AA);

        int contentX = panelX + 124;
        int contentY = panelY + 76;
        int contentWidth = panelWidth - 142;
        int columns = Math.max(1, Math.min(3, contentWidth / 150));
        int gap = 10;
        int cardWidth = (contentWidth - (columns - 1) * gap) / columns;
        int index = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!selectedFilter.equals("All") && !module.getCategory().equals(selectedFilter)) continue;
            int x = contentX + (index % columns) * (cardWidth + gap);
            int y = contentY + (index / columns) * 68;
            if (y + 58 > panelY + panelHeight - 18) break;
            EzUi.card(graphics, x, y, cardWidth, 58, module.isEnabled());
            graphics.text(font, module.getName(), x + 12, y + 12, 0xFFFFFFFF);
            graphics.text(font, module.getCategory().toUpperCase(), x + 12, y + 24, 0xFF9EAABD);
            index++;
        }

        if (selectedFilter.equals("All") && ModuleManager.getInstance().getModules().isEmpty()) {
            graphics.centeredText(font, "No modules in this category yet",
                    panelX + 124 + (panelWidth - 124) / 2, panelY + panelHeight / 2, 0xFF69737D);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
