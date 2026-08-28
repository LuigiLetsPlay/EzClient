package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Premium EzClient In-Game Dashboard.
 * Features fluid smooth scrolling, live search, pill-shaped category tabs,
 * spacious module cards with toggle dots, and clean bottom action bar.
 */
public final class EzHubScreen extends Screen {
    private static final String[] FILTERS = {"All", "HUD", "Movement", "Render"};
    private static final Identifier EZCLIENT_ICON = Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ezclient.png");

    private final Screen parent;
    private String selectedFilter = "All";
    private String searchQuery = "";
    private EditBox searchBox;
    private EzButton hudEditorButton;

    private int panelX, panelY;
    private int panelWidth, panelHeight;
    private double animProgress = 0.0;

    // Fluid smooth scrolling state
    private double scrollOffset = 0.0;
    private double targetScrollOffset = 0.0;
    private double maxScroll = 0.0;
    private boolean isDraggingScrollbar = false;
    private double dragScrollStartY = 0.0;

    // Pill button hover tracking
    private int hoveredPillIndex = -1;

    public EzHubScreen(Screen parent) {
        super(app.ezclient.util.EzI18n.comp("ezclient.hub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(370, Math.max(300, width * 35 / 100));
        panelHeight = height - 16;
        panelX = -panelWidth; // Start off-screen for animation
        panelY = 8;

        // ── Top Search Box (right-aligned in header, next to close button) ──
        int searchW = 76;
        searchBox = new EditBox(font, panelX + panelWidth - searchW - 28, panelY + 9, searchW, 16, Component.literal("Search"));
        searchBox.setHint(app.ezclient.util.EzI18n.comp("ezclient.hub.search_hint"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(text -> {
            searchQuery = text.trim().toLowerCase();
            targetScrollOffset = 0.0;
            scrollOffset = 0.0;
        });
        addRenderableWidget(searchBox);

        // ── Bottom Action Bar: HUD Layout Editor Button ──
        int hudBtnW = 135;
        int hudBtnH = 20;
        hudEditorButton = new EzButton(
                panelX + panelWidth - hudBtnW - 12, panelY + panelHeight - hudBtnH - 9,
                hudBtnW, hudBtnH,
                app.ezclient.util.EzI18n.comp("ezclient.hub.hud_editor_btn"), true,
                b -> minecraft.gui.setScreen(new HudEditorScreen(this))
        );
        addRenderableWidget(hudEditorButton);
    }

    private List<Module> getFilteredModules() {
        List<Module> list = new ArrayList<>();
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (!selectedFilter.equals("All") && !m.getCategory().equalsIgnoreCase(selectedFilter)) {
                continue;
            }
            if (!searchQuery.isEmpty()) {
                String name = m.getName().toLowerCase();
                String cat = m.getCategory().toLowerCase();
                if (!name.contains(searchQuery) && !cat.contains(searchQuery)) {
                    continue;
                }
            }
            list.add(m);
        }
        return list;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        // ── Close button (top right) ──
        if (e.button() == 0) {
            int closeX = panelX + panelWidth - 22;
            int closeY = panelY + 10;
            if (e.x() >= closeX && e.x() <= closeX + 14 && e.y() >= closeY && e.y() <= closeY + 14) {
                onClose();
                return true;
            }
        }

        // ── Check category pill button clicks (Row 2 at panelY + 36) ──
        if (e.button() == 0) {
            int pillY = panelY + 36;
            int pillH = 16;
            int pillX = panelX + 12;
            for (int i = 0; i < FILTERS.length; i++) {
                String label = app.ezclient.util.EzI18n.get("ezclient.category." + FILTERS[i].toLowerCase(), FILTERS[i]);
                int pillW = font.width(label) + 10;
                if (e.x() >= pillX && e.x() <= pillX + pillW && e.y() >= pillY && e.y() <= pillY + pillH) {
                    selectedFilter = FILTERS[i];
                    targetScrollOffset = 0.0;
                    scrollOffset = 0.0;
                    return true;
                }
                pillX += pillW + 4;
            }
        }

        int contentX = panelX + 12;
        int contentY = panelY + 64;
        int contentWidth = panelWidth - 24;
        int contentHeight = panelHeight - 104;

        int gap = 8;
        int columns = Math.max(1, (contentWidth + gap) / (74 + gap));
        int cardWidth = (contentWidth - gap * (columns - 1)) / columns;
        int cardHeight = 72;

        if (e.button() == 0) {
            // Check scrollbar click
            if (maxScroll > 0) {
                int trackX = panelX + panelWidth - 8;
                int trackY = contentY;
                int trackH = contentHeight;

                if (e.x() >= trackX - 2 && e.x() <= trackX + 6 && e.y() >= trackY && e.y() <= trackY + trackH) {
                    isDraggingScrollbar = true;
                    dragScrollStartY = e.y();
                    return true;
                }
            }

            // Check module cards click
            if (e.x() >= contentX && e.x() <= contentX + contentWidth && e.y() >= contentY && e.y() <= contentY + contentHeight) {
                List<Module> filtered = getFilteredModules();
                for (int i = 0; i < filtered.size(); i++) {
                    Module module = filtered.get(i);
                    int col = i % columns;
                    int row = i / columns;
                    int cx = contentX + col * (cardWidth + gap);
                    int cy = (int) (contentY + row * (cardHeight + gap) - scrollOffset);

                    if (cy + cardHeight < contentY || cy > contentY + contentHeight) continue;

                    if (e.x() >= cx && e.x() <= cx + cardWidth && e.y() >= cy && e.y() <= cy + cardHeight) {
                        // Check if left clicked on top-left gear icon
                        boolean gearHit = e.x() >= cx && e.x() <= cx + 18 && e.y() >= cy && e.y() <= cy + 18;
                        if (gearHit) {
                            openModuleSettings(module);
                            return true;
                        }

                        module.toggle();
                        ConfigManager.save();
                        return true;
                    }
                }
            }
        }

        // Right click -> Settings
        if (e.button() == 1) {
            if (e.x() >= contentX && e.x() <= contentX + contentWidth && e.y() >= contentY && e.y() <= contentY + contentHeight) {
                List<Module> filtered = getFilteredModules();
                for (int i = 0; i < filtered.size(); i++) {
                    Module module = filtered.get(i);
                    int col = i % columns;
                    int row = i / columns;
                    int cx = contentX + col * (cardWidth + gap);
                    int cy = (int) (contentY + row * (cardHeight + gap) - scrollOffset);

                    if (cy + cardHeight < contentY || cy > contentY + contentHeight) continue;

                    if (e.x() >= cx && e.x() <= cx + cardWidth && e.y() >= cy && e.y() <= cy + cardHeight) {
                        openModuleSettings(module);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (isDraggingScrollbar && maxScroll > 0) {
            int contentHeight = panelHeight - 104;
            int contentWidth = panelWidth - 24;
            int gap = 8;
            int columns = Math.max(1, (contentWidth + gap) / (74 + gap));
            int totalRows = (getFilteredModules().size() + columns - 1) / columns;
            int totalHeight = totalRows * (72 + gap);
            int thumbH = Math.max(20, (int) (contentHeight * ((double) contentHeight / totalHeight)));
            double travel = contentHeight - thumbH;
            if (travel > 0) {
                double deltaNorm = dy / travel;
                targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset + deltaNorm * maxScroll));
                scrollOffset = targetScrollOffset;
            }
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll > 0 && mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY + 60 && mouseY <= panelY + panelHeight - 36) {
            targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset - vertical * 30.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Keep the world visible; the drawer only adds a very light focus veil.
        graphics.fill(0, 0, width, height, 0x24000000);

        // Smooth scroll interpolation (Easing)
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.28;
        if (Math.abs(targetScrollOffset - scrollOffset) < 0.05) {
            scrollOffset = targetScrollOffset;
        }

        // Swipe-in animation from left
        animProgress += (1.0 - animProgress) * 0.15;
        if (Math.abs(1.0 - animProgress) < 0.01) animProgress = 1.0;
        panelX = (int) (-panelWidth + (panelWidth + 10) * animProgress);

        int closeX = panelX + panelWidth - 22;
        int searchW = 76;
        searchBox.setWidth(searchW);
        searchBox.setX(closeX - searchW - 6);
        searchBox.setY(panelY + 9);

        int hudBtnW = 135;
        int hudBtnH = 20;
        hudEditorButton.setWidth(hudBtnW);
        hudEditorButton.setHeight(hudBtnH);
        hudEditorButton.setX(panelX + panelWidth - hudBtnW - 12);
        hudEditorButton.setY(panelY + panelHeight - hudBtnH - 8);

        // Glass Panel Container
        EzUi.panel(graphics, panelX, panelY, panelWidth, panelHeight);

        // ── Row 1: Header (Logo + Title + Version Badge + Search + Close) ──
        int logoX = panelX + 12;
        int logoY = panelY + 8;
        // Icon background circle
        EzUi.roundedRect(graphics, logoX, logoY, 20, 20, 10, 0xFF14221B);
        ModuleIconRenderer.drawTexture(graphics, EZCLIENT_ICON, logoX + 2, logoY + 2, 16);
        // Title
        graphics.text(font, "EzClient", logoX + 24, logoY + 6, EzUi.TEXT_WHITE);

        // Version badge
        String version = "v1.8.2";
        int vBadgeW = font.width(version) + 6;
        int vBadgeX = logoX + 24 + font.width("EzClient") + 4;
        EzUi.roundedRect(graphics, vBadgeX, logoY + 4, vBadgeW, 12, 3, 0xFF1A2630);
        graphics.centeredText(font, Component.literal(version), vBadgeX + vBadgeW / 2, logoY + 6, EzUi.ACCENT_EMERALD);

        // Close button (top right, minimal X)
        int closeY = panelY + 10;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14;
        if (closeHovered) {
            EzUi.roundedRect(graphics, closeX - 2, closeY - 2, 18, 18, 4, 0xFF2A1520);
        }
        graphics.centeredText(font, Component.literal("✕"), closeX + 7, closeY + 3, closeHovered ? 0xFFEF4444 : EzUi.TEXT_MUTED);

        // ── Row 2: Category Filter Tabs (Single clean section) ──
        int pillX = panelX + 12;
        int pillY = panelY + 34;
        int pillH = 16;
        hoveredPillIndex = -1;
        for (int i = 0; i < FILTERS.length; i++) {
            String label = app.ezclient.util.EzI18n.get("ezclient.category." + FILTERS[i].toLowerCase(), FILTERS[i]);
            int pillW = font.width(label) + 10;
            boolean active = selectedFilter.equals(FILTERS[i]);
            boolean hovered = mouseX >= pillX && mouseX <= pillX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
            if (hovered) hoveredPillIndex = i;

            EzUi.pillButton(graphics, pillX, pillY, pillW, pillH, active, hovered);
            int textColor = active ? EzUi.TEXT_WHITE : (hovered ? EzUi.TEXT_LIGHT : EzUi.TEXT_MUTED);
            graphics.centeredText(font, Component.literal(label), pillX + pillW / 2, pillY + 4, textColor);
            pillX += pillW + 4;
        }

        // Single Separator below filters with generous margin
        graphics.fill(panelX + 12, panelY + 56, panelX + panelWidth - 12, panelY + 57, EzUi.BORDER_SUBTLE);

        // ── Row 3: Module Cards Grid ──
        int contentX = panelX + 12;
        int contentY = panelY + 66;
        int contentWidth = panelWidth - 24;
        int contentHeight = panelHeight - 106;

        int gap = 8;
        int columns = Math.max(1, (contentWidth + gap) / (74 + gap));
        int cardWidth = (contentWidth - gap * (columns - 1)) / columns;
        int cardHeight = 72;

        List<Module> filtered = getFilteredModules();
        int totalRows = (filtered.size() + columns - 1) / columns;
        int totalHeight = totalRows * (cardHeight + gap);
        maxScroll = Math.max(0, totalHeight - contentHeight);

        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        for (int i = 0; i < filtered.size(); i++) {
            Module module = filtered.get(i);
            int col = i % columns;
            int row = i / columns;
            int cx = contentX + col * (cardWidth + gap);
            int cy = (int) (contentY + row * (cardHeight + gap) - scrollOffset);

            if (cy + cardHeight < contentY || cy > contentY + contentHeight) continue;

            boolean hovered = mouseX >= cx && mouseX <= cx + cardWidth && mouseY >= cy && mouseY <= cy + cardHeight
                    && mouseY >= contentY && mouseY <= contentY + contentHeight;

            EzUi.moduleCard(graphics, cx, cy, cardWidth, cardHeight, module.isEnabled(), hovered);

            // Icon (centered, 28x28)
            ModuleIconRenderer.draw(graphics, module, cx + (cardWidth - 28) / 2, cy + 10, 28);

            // Name (centered below icon)
            graphics.centeredText(font, Component.literal(module.getDisplayName()), cx + cardWidth / 2, cy + cardHeight - 22, EzUi.TEXT_LIGHT);

            // Toggle dot indicator (bottom right)
            EzUi.toggleDot(graphics, cx + cardWidth - 12, cy + cardHeight - 12, module.isEnabled());

            // Settings gear icon (top-left) with clean text brightness highlight (no white box)
            if (hovered) {
                boolean gearHovered = mouseX >= cx && mouseX <= cx + 18 && mouseY >= cy && mouseY <= cy + 18;
                if (gearHovered) {
                    graphics.text(font, "⚙", cx + 4, cy + 3, 0xFFFFFFFF);
                } else {
                    graphics.text(font, "⚙", cx + 4, cy + 3, 0x8094A3B8);
                }
            }
        }

        graphics.disableScissor();

        // ── Smooth Scrollbar ──
        if (maxScroll > 0) {
            int trackX = panelX + panelWidth - 8;
            int trackY = contentY;
            int trackH = contentHeight;
            int thumbH = Math.max(20, (int) (trackH * ((double) trackH / totalHeight)));
            int thumbY = trackY + (int) ((scrollOffset / maxScroll) * (trackH - thumbH));

            boolean sbHovered = mouseX >= trackX - 2 && mouseX <= trackX + 6 && mouseY >= trackY && mouseY <= trackY + trackH;
            // Track
            EzUi.roundedRect(graphics, trackX, trackY, 4, trackH, 2, 0x20FFFFFF);
            // Thumb
            EzUi.roundedRect(graphics, trackX, thumbY, 4, thumbH, 2, sbHovered || isDraggingScrollbar ? EzUi.ACCENT_EMERALD_HOVER : EzUi.ACCENT_EMERALD);
        }

        // ── Row 4: Bottom Bar ──
        int bottomBarY = panelY + panelHeight - 34;
        graphics.fill(panelX + 12, bottomBarY - 2, panelX + panelWidth - 12, bottomBarY - 1, EzUi.BORDER_SUBTLE);

        // Active modules count (left)
        int activeCount = 0;
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (m.isEnabled()) activeCount++;
        }
        String activeText = app.ezclient.util.EzI18n.get("ezclient.hub.active_modules", activeCount);
        int badgeW = font.width(activeText) + 10;
        EzUi.roundedRect(graphics, panelX + 12, bottomBarY + 3, badgeW, 16, 4, 0xFF14221B);
        graphics.text(font, activeText, panelX + 17, bottomBarY + 7, EzUi.ACCENT_EMERALD);

        // Empty state
        if (filtered.isEmpty()) {
            graphics.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hub.no_modules"),
                    panelX + panelWidth / 2, panelY + panelHeight / 2 - 10, EzUi.TEXT_MUTED);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void openModuleSettings(Module module) {
        if (minecraft == null) return;
        if (module instanceof HudModule hud) {
            minecraft.gui.setScreen(new HudSettingsScreen(this, hud));
        } else if (module instanceof ZoomModule) {
            minecraft.gui.setScreen(new ZoomSettingsScreen(this));
        } else {
            minecraft.gui.setScreen(new ModuleSettingsScreen(this, module));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
