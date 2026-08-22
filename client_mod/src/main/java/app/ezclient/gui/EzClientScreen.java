package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact custom module browser rendered with the native Minecraft font. */
public final class EzClientScreen extends Screen {
    private static final String[] FILTERS = {"All", "Utils", "Combat", "Visual"};
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
        panelWidth = Math.min(220, width - 20);
        panelHeight = Math.min(134, height - 20);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int sidebarX = panelX + 6;
        int filterY = panelY + 32;
        for (String filter : FILTERS) {
            boolean selected = filter.equals(selectedFilter);
            addRenderableWidget(new EzButton(
                    sidebarX, filterY, 44, 16, Component.literal(filter), selected,
                    ignored -> {
                        selectedFilter = filter;
                        rebuildWidgets();
                    }
            ));
            filterY += 18;
        }

        addRenderableWidget(new EzButton(
                panelX + 6, panelY + panelHeight - 22, 44, 16,
                Component.literal("Close"), false, ignored -> onClose()
        ));

        int index = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!selectedFilter.equals("All") && !module.getCategory().equals(selectedFilter)) continue;
            int x = panelX + 57 + (index % 3) * 53;
            int y = panelY + 32 + (index / 3) * 39;
            if (module instanceof ZoomModule) addZoomModuleCard(x, y);
            else addToggleModuleCard(module, x, y);
            index++;
        }
    }

    private void addToggleModuleCard(Module module, int x, int y) {
        String label = module.getName().equals("Coordinates") ? "Coords" : module.getName();
        addRenderableWidget(new EzButton(x, y, 50, 35, Component.literal(label), false, ignored -> {
            if (module instanceof HudModule hud) minecraft.gui.setScreen(new HudSettingsScreen(this, hud));
            else { module.toggle(); rebuildWidgets(); }
        }));
        addRenderableWidget(new EzButton(
                x + 34, y + 3, 14, 12,
                Component.literal(module.isEnabled() ? "ON" : "OFF"), module.isEnabled(),
                ignored -> { module.toggle(); rebuildWidgets(); }
        ));
    }

    private void addZoomModuleCard(int x, int y) {
        ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
        addRenderableWidget(new EzButton(
                x, y, 50, 35,
                Component.literal("Zoom"),
                false,
                ignored -> minecraft.gui.setScreen(new ZoomSettingsScreen(this))
        ));
        addRenderableWidget(new EzButton(
                x + 34, y + 3, 14, 12,
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
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF2111419);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF303943);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 24, 0xFF191E24);
        graphics.fill(panelX + 54, panelY + 24, panelX + 55, panelY + panelHeight, 0xFF2A323B);
        graphics.text(font, "EZ", panelX + 7, panelY + 8, 0xFF43DD8C);
        graphics.text(font, "MODULES", panelX + 24, panelY + 8, 0xFF89939E);
        graphics.text(font, "Filter", panelX + 7, panelY + 24, 0xFF77818C);
        graphics.text(font, selectedFilter, panelX + 61, panelY + 24, 0xFFE8EDF1);

        if (selectedFilter.equals("Combat")) {
            graphics.centeredText(font, "No modules in this category yet",
                    panelX + 55 + (panelWidth - 55) / 2, panelY + panelHeight / 2, 0xFF69737D);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
