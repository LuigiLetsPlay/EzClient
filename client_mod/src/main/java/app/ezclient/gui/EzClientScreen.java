package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Responsive module workspace with a modern glass-panel layout. */
public final class EzClientScreen extends Screen {
    private static final String[] FILTERS = {"All", "HUD", "Utils", "Visual", "Render"};
    private static final Identifier EZCLIENT_ICON = Identifier.fromNamespaceAndPath("ezclient", "title/icon");
    private final Screen parent;
    private String selectedFilter = "All";
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int scrollRows;

    public EzClientScreen(Screen parent) {
        super(Component.literal("EzClient Modules"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(560, width - 24);
        panelHeight = Math.min(340, height - 24);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        addRenderableWidget(new EzButton(
                panelX + panelWidth - 76, panelY + 22, 60, 20,
                Component.literal("Close"), false, ignored -> onClose()
        ));

        int contentX = panelX + 16;
        int contentY = panelY + 72;
        int contentWidth = panelWidth - 32;
        int columns = Math.max(1, Math.min(4, contentWidth / 115));
        int gap = 8;
        int cardWidth = (contentWidth - (columns - 1) * gap) / columns;
        int cardHeight = 50;
        int totalModules = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (selectedFilter.equals("All") || module.getCategory().equals(selectedFilter)) totalModules++;
        }
        int index = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!selectedFilter.equals("All") && !module.getCategory().equals(selectedFilter)) continue;
            int x = contentX + (index % columns) * (cardWidth + gap);
            int row = index / columns;
            int y = contentY + (row - scrollRows) * (cardHeight + gap);
            if (row < scrollRows) { index++; continue; }
            if (y + cardHeight > panelY + panelHeight - 18) break;
            if (module instanceof ZoomModule) addZoomModuleControls(x, y, cardWidth);
            else addToggleModuleControls(module, x, y, cardWidth);
            index++;
        }
    }

    private void addToggleModuleControls(Module module, int x, int y, int cardWidth) {
        if (module instanceof HudModule hud) {
            addRenderableWidget(new EzButton(x + 8, y + 29, 54, 14, Component.literal("Settings"), false,
                    ignored -> minecraft.gui.setScreen(new HudSettingsScreen(this, hud))));
        }
        addRenderableWidget(new EzButton(
                x + cardWidth - 40, y + 8, 30, 16,
                Component.literal(module.isEnabled() ? "ON" : "OFF"), module.isEnabled(),
                ignored -> { module.toggle(); rebuildWidgets(); }
        ));
    }

    private void addZoomModuleControls(int x, int y, int cardWidth) {
        ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
        addRenderableWidget(new EzButton(
                x + 8, y + 29, 54, 14, Component.literal("Settings"),
                false,
                ignored -> minecraft.gui.setScreen(new ZoomSettingsScreen(this))
        ));
        addRenderableWidget(new EzButton(
                x + cardWidth - 40, y + 8, 30, 16,
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
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EZCLIENT_ICON, panelX + 20, panelY + 21, 28, 28);
        graphics.text(font, "EzClient", panelX + 58, panelY + 21, 0xFFFFFFFF);
        graphics.text(font, "Modules  •  " + selectedFilter, panelX + 58, panelY + 37, 0xFFABB5C7);

        int contentX = panelX + 16;
        int contentY = panelY + 72;
        int contentWidth = panelWidth - 32;
        int columns = Math.max(1, Math.min(4, contentWidth / 115));
        int gap = 8;
        int cardWidth = (contentWidth - (columns - 1) * gap) / columns;
        int totalModules = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (selectedFilter.equals("All") || module.getCategory().equals(selectedFilter)) totalModules++;
        }
        int index = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!selectedFilter.equals("All") && !module.getCategory().equals(selectedFilter)) continue;
            int x = contentX + (index % columns) * (cardWidth + gap);
            int row = index / columns;
            int y = contentY + (row - scrollRows) * 58;
            if (row < scrollRows) { index++; continue; }
            if (y + 50 > panelY + panelHeight - 18) break;
            EzUi.card(graphics, x, y, cardWidth, 50, module.isEnabled());
            graphics.text(font, module.getName(), x + 8, y + 10, 0xFFFFFFFF);
            graphics.text(font, module.getCategory().toUpperCase(), x + 8, y + 21, 0xFF9EAABD);
            index++;
        }

        int totalRows = (totalModules + columns - 1) / columns;
        int visibleRows = Math.max(1, (panelHeight - 88) / 58);
        if (totalRows > visibleRows) {
            graphics.text(font, "Scroll: " + (scrollRows + 1) + "/" + (totalRows - visibleRows + 1),
                    panelX + panelWidth - 90, panelY + panelHeight - 14, 0xFF9EAABD);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int contentWidth = panelWidth - 32;
        int columns = Math.max(1, Math.min(4, contentWidth / 115));
        int count = 0;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (selectedFilter.equals("All") || module.getCategory().equals(selectedFilter)) count++;
        }
        int totalRows = (count + columns - 1) / columns;
        int visibleRows = Math.max(1, (panelHeight - 88) / 58);
        int maximum = Math.max(0, totalRows - visibleRows);
        if (maximum > 0 && mouseX >= panelX && mouseX <= panelX + panelWidth
                && mouseY >= panelY + 70 && mouseY <= panelY + panelHeight) {
            scrollRows = Math.max(0, Math.min(maximum, scrollRows - (int) Math.signum(vertical)));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
}
