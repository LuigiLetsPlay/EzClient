package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
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
    private static final int HUB_WIDTH = 300;
    private static final int HUB_HEIGHT = 210;
    private static final int SIDEBAR_WIDTH = 60;
    private static final int GRID_GAP = 5;
    private static final int CARD_MIN_WIDTH = 44;
    private static final int CARD_HEIGHT = 32;
    private static final int MODULE_ICON_SIZE = 12;
    private static final int SEARCH_WIDTH = 140;
    private static final int SEARCH_HEIGHT = 13;
    private static final int HUD_BUTTON_WIDTH = 45;
    private static final int HUD_BUTTON_HEIGHT = 14;
    private static final int PILL_WIDTH = 44;
    private static final int PILL_HEIGHT = 14;
    private static final int PILL_GAP = 4;
    private static final String[] FILTERS = {"All", "HUD", "Movement", "Render"};
    private static final Identifier EZCLIENT_ICON = Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ezclient.png");

    private final Screen parent;
    private String selectedFilter = "All";
    private String searchQuery = "";
    private EditBox searchBox;
    private EzButton overallButton;
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
    private Module listeningModule = null;

    public EzHubScreen(Screen parent) {
        super(app.ezclient.util.EzI18n.comp("ezclient.hub.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(HUB_WIDTH, width - 48);
        panelHeight = Math.min(HUB_HEIGHT, height - 48);
        panelX = (width - panelWidth) / 2;
        animProgress = 0.0;
        panelY = (int) ((height - panelHeight) / 2.0 - 8 * (1.0 - animProgress));

        // ── Top Search Box (right-aligned in header, next to close button) ──
        int searchW = Math.min(SEARCH_WIDTH, panelWidth - SIDEBAR_WIDTH - 38);
        searchBox = new EditBox(font, panelX + panelWidth - searchW - 28, panelY + 11, searchW, SEARCH_HEIGHT, Component.literal(app.ezclient.util.EzI18n.text("Search")));
        searchBox.setHint(app.ezclient.util.EzI18n.comp("ezclient.hub.search_hint"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(text -> {
            searchQuery = text.trim().toLowerCase();
            targetScrollOffset = 0.0;
            scrollOffset = 0.0;
        });
        addRenderableWidget(searchBox);

        // ── Bottom Action Bar: Overall & HUD Layout Editor Buttons ──
        int hudBtnW = HUD_BUTTON_WIDTH;
        int hudBtnH = HUD_BUTTON_HEIGHT;
        int overallBtnY = panelY + panelHeight - (hudBtnH * 2) - 10;
        overallButton = new EzButton(
                panelX + (SIDEBAR_WIDTH - hudBtnW) / 2, overallBtnY,
                hudBtnW, hudBtnH,
                Component.literal("Overall"), true,
                b -> EzScreenBridge.set(minecraft, new OverallHudSettingsScreen(this))
        );
        addRenderableWidget(overallButton);

        hudEditorButton = new EzButton(
                panelX + (SIDEBAR_WIDTH - hudBtnW) / 2, panelY + panelHeight - hudBtnH - 6,
                hudBtnW, hudBtnH,
                Component.literal("HUD Editor"), true,
                b -> EzScreenBridge.set(minecraft, new HudEditorScreen(this))
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
                String name = (m.getName() + " " + m.getDisplayName() + " " + m.getLocalizedDescription()).toLowerCase(java.util.Locale.ROOT);
                String cat = (m.getCategory() + " " + app.ezclient.util.EzI18n.text(m.getCategory())).toLowerCase(java.util.Locale.ROOT);
                if (!name.contains(searchQuery) && !cat.contains(searchQuery)) {
                    continue;
                }
            }
            list.add(m);
        }
        list.sort((a, b) -> {
            if (a.isFavorite() != b.isFavorite()) {
                return a.isFavorite() ? -1 : 1;
            }
            return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
        });
        return list;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (listeningModule != null && e.button() != 0) {
            EzKeyBindings.applyModuleKeyBind(listeningModule, -100 - e.button());
            listeningModule = null;
            return true;
        }

        // ── Close button (top right) ──
        if (e.button() == 0) {
            int closeX = panelX + panelWidth - 19;
            int closeY = panelY + 11;
            if (e.x() >= closeX && e.x() <= closeX + 11 && e.y() >= closeY && e.y() <= closeY + 11) {
                onClose();
                return true;
            }
        }



        // ── Check category pill button clicks (Row 2 at panelY + 36) ──
        if (e.button() == 0) {
            int pillY = panelY + 37;
            int pillH = PILL_HEIGHT;
            int pillX = panelX + 8;
            for (int i = 0; i < FILTERS.length; i++) {
                String label = app.ezclient.util.EzI18n.get("ezclient.category." + FILTERS[i].toLowerCase(), FILTERS[i]);
                int pillW = PILL_WIDTH;
                if (e.x() >= pillX && e.x() <= pillX + pillW && e.y() >= pillY && e.y() <= pillY + pillH) {
                    selectedFilter = FILTERS[i];
                    targetScrollOffset = 0.0;
                    scrollOffset = 0.0;
                    return true;
                }
                pillY += pillH + PILL_GAP;
            }
        }

        int contentX = panelX + SIDEBAR_WIDTH + 8;
        int contentY = panelY + 32;
        int contentWidth = panelWidth - SIDEBAR_WIDTH - 16;
        int contentHeight = panelHeight - 40;

        int gap = GRID_GAP;
        int columns = Math.max(1, (contentWidth + gap) / (CARD_MIN_WIDTH + gap));
        int cardWidth = (contentWidth - gap * (columns - 1)) / columns;
        int cardHeight = CARD_HEIGHT;

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
                        // Check if Shift was held: Shift+Left Click on ANY card toggles hotkey listening!
                        if (isShiftDown()) {
                            listeningModule = (listeningModule == module ? null : module);
                            return true;
                        }

                        // Check if left clicked on top-right star (Favorite toggle)
                        boolean starHit = e.x() >= cx + cardWidth - 13 && e.x() <= cx + cardWidth && e.y() >= cy && e.y() <= cy + 13;
                        if (starHit) {
                            module.setFavorite(!module.isFavorite());
                            return true;
                        }

                        // Check if left clicked on top-left icon (⚙ Settings or ◉ Vorschau)
                        boolean gearHit = e.x() >= cx && e.x() <= cx + 14 && e.y() >= cy && e.y() <= cy + 14;
                        if (gearHit) {
                            if (module.hasSettings()) {
                                openModuleSettings(module);
                            } else if (module.hasPreview()) {
                                EzScreenBridge.set(minecraft, new ModulePreviewScreen(this, module));
                            }
                            return true;
                        }

                        module.toggle();
                        ConfigManager.save();
                        return true;
                    }
                }
            }
        }

        // Right click -> Settings or Live Preview
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
                        if (module.hasSettings()) {
                            openModuleSettings(module);
                        } else if (module.hasPreview()) {
                            EzScreenBridge.set(minecraft, new ModulePreviewScreen(this, module));
                        }
                        return true;
                    }
                }
            }
        }

        // Middle click -> Toggle quick hotkey assignment directly on module card
        if (e.button() == 2) {
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
                        listeningModule = (listeningModule == module ? null : module);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningModule != null) {
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
                EzKeyBindings.applyModuleKeyBind(listeningModule, -1);
            } else {
                EzKeyBindings.applyModuleKeyBind(listeningModule, event.key());
            }
            listeningModule = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (isDraggingScrollbar && maxScroll > 0) {
            int contentHeight = panelHeight - 40;
            int contentWidth = panelWidth - SIDEBAR_WIDTH - 16;
            int gap = GRID_GAP;
            int columns = Math.max(1, (contentWidth + gap) / (CARD_MIN_WIDTH + gap));
            int totalRows = (getFilteredModules().size() + columns - 1) / columns;
            int totalHeight = totalRows * (CARD_HEIGHT + gap);
            int thumbH = Math.max(15, (int) (contentHeight * ((double) contentHeight / totalHeight)));
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
        if (maxScroll > 0 && mouseX >= panelX + SIDEBAR_WIDTH && mouseX <= panelX + panelWidth && mouseY >= panelY + 32 && mouseY <= panelY + panelHeight - 8) {
            targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset - vertical * 30.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Keep the world visible with a sleek dark backdrop
        EzUi.backdrop(graphics, width, height);

        // Smooth scroll interpolation (Easing)
        scrollOffset += (targetScrollOffset - scrollOffset) * 0.28;
        if (Math.abs(targetScrollOffset - scrollOffset) < 0.05) {
            scrollOffset = targetScrollOffset;
        }

        // Smooth centered scale / entrance animation
        animProgress += (1.0 - animProgress) * 0.25;
        if (Math.abs(1.0 - animProgress) < 0.01) animProgress = 1.0;

        panelWidth = Math.min(HUB_WIDTH, width - 48);
        panelHeight = Math.min(HUB_HEIGHT, height - 48);
        int targetX = (width - panelWidth) / 2;
        int targetY = (height - panelHeight) / 2;
        panelX = targetX;
        panelY = (int) (targetY - 8 * (1.0 - animProgress));

        int closeX = panelX + panelWidth - 19;
        int searchW = Math.min(SEARCH_WIDTH, panelWidth - SIDEBAR_WIDTH - 38);
        searchBox.setWidth(searchW);
        searchBox.setHeight(SEARCH_HEIGHT);
        searchBox.setX(panelX + panelWidth - searchW - 28);
        searchBox.setY(panelY + 11);

        int hudBtnW = HUD_BUTTON_WIDTH;
        int hudBtnH = HUD_BUTTON_HEIGHT;
        if (overallButton != null) {
            overallButton.setWidth(hudBtnW);
            overallButton.setHeight(hudBtnH);
            overallButton.setX(panelX + (SIDEBAR_WIDTH - hudBtnW) / 2);
            overallButton.setY(panelY + panelHeight - (hudBtnH * 2) - 10);
        }
        hudEditorButton.setWidth(hudBtnW);
        hudEditorButton.setHeight(hudBtnH);
        hudEditorButton.setX(panelX + (SIDEBAR_WIDTH - hudBtnW) / 2);
        hudEditorButton.setY(panelY + panelHeight - hudBtnH - 6);

        // Glass Panel Container
        EzUi.panel(graphics, panelX, panelY, panelWidth, panelHeight);

        // Compact navigation sidebar; content has no separate topbar or footer.
        graphics.fill(panelX + SIDEBAR_WIDTH, panelY + 8, panelX + SIDEBAR_WIDTH + 1, panelY + panelHeight - 8, EzUi.BORDER_SUBTLE);
        int logoX = panelX + 22, logoY = panelY + 11;
        EzUi.roundedRect(graphics, logoX, logoY, 15, 15, 2, 0xFF15181C);
        ModuleIconRenderer.drawTexture(graphics, EZCLIENT_ICON, logoX + 1, logoY + 1, 13);

        // Close button (top right, minimal X)
        int closeY = panelY + 11;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 11 && mouseY >= closeY && mouseY <= closeY + 11;
        if (closeHovered) {
            EzUi.roundedRect(graphics, closeX - 2, closeY - 2, 15, 15, 3, 0xFF2A1520);
        }
        drawScaledCenteredText(graphics, Component.literal("✕"), closeX + 5.5f, closeY + 1.5f, 0.72f,
                closeHovered ? 0xFFEF4444 : EzUi.TEXT_MUTED);

        int pillX = panelX + 8;
        int pillY = panelY + 37;
        int pillH = PILL_HEIGHT;
        hoveredPillIndex = -1;
        for (int i = 0; i < FILTERS.length; i++) {
            String label = app.ezclient.util.EzI18n.get("ezclient.category." + FILTERS[i].toLowerCase(), FILTERS[i]);
            int pillW = PILL_WIDTH;
            boolean active = selectedFilter.equals(FILTERS[i]);
            boolean hovered = mouseX >= pillX && mouseX <= pillX + pillW && mouseY >= pillY && mouseY <= pillY + pillH;
            if (hovered) hoveredPillIndex = i;

            EzUi.pillButton(graphics, pillX, pillY, pillW, pillH, active, hovered);
            int textColor = active ? EzUi.TEXT_WHITE : (hovered ? EzUi.TEXT_LIGHT : EzUi.TEXT_MUTED);
            drawScaledCenteredText(graphics, Component.literal(label), pillX + pillW / 2.0f, pillY + 2.5f, 0.72f, textColor);
            pillY += pillH + PILL_GAP;
        }

        // ── Row 3: Module Cards Grid ──
        int contentX = panelX + SIDEBAR_WIDTH + 8;
        int contentY = panelY + 32;
        int contentWidth = panelWidth - SIDEBAR_WIDTH - 16;
        int contentHeight = panelHeight - 40;

        int gap = GRID_GAP;
        int columns = Math.max(1, (contentWidth + gap) / (CARD_MIN_WIDTH + gap));
        int cardWidth = (contentWidth - gap * (columns - 1)) / columns;
        int cardHeight = CARD_HEIGHT;

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

            // Compact icon, sized for the five-column module grid.
            ModuleIconRenderer.draw(graphics, module,
                    cx + (cardWidth - MODULE_ICON_SIZE) / 2, cy + 5, MODULE_ICON_SIZE);

            // Name (scaled down only as much as needed so neighboring cards never overlap).
            String moduleName = module.getDisplayName();
            float labelScale = Math.min(0.62f,
                    (cardWidth - 6.0f) / Math.max(1.0f, font.width(moduleName)));
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx + cardWidth / 2.0f, cy + cardHeight - 11.0f);
            graphics.pose().scale(labelScale, labelScale);
            graphics.centeredText(font, Component.literal(moduleName), 0, 0, EzUi.TEXT_LIGHT);
            graphics.pose().popMatrix();


            // Favorite star icon (top-right)
            if (module.isFavorite() || hovered) {
                boolean starHovered = mouseX >= cx + cardWidth - 13 && mouseX <= cx + cardWidth && mouseY >= cy && mouseY <= cy + 13;
                String starIcon = module.isFavorite() ? "★" : "☆";
                int starColor = module.isFavorite() ? 0xFFFFD700 : (starHovered ? 0xFFFFEA79 : 0x8094A3B8);
                drawScaledText(graphics, starIcon, cx + cardWidth - 10, cy + 2, 0.65f, starColor);
            }

            // Settings gear icon (top-left) or Live Preview eye icon
            if (module.hasSettings() || module.hasPreview()) {
                boolean iconHovered = mouseX >= cx && mouseX <= cx + 14 && mouseY >= cy && mouseY <= cy + 14;
                String icon = module.hasSettings() ? "⚙" : "◉";
                int iconColor = iconHovered ? 0xFFFFFFFF : (hovered ? 0x8094A3B8 : 0x4094A3B8);
                drawScaledText(graphics, icon, cx + 3, cy + 2, 0.65f, iconColor);
            }


        }

        graphics.disableScissor();

        // ── Smooth Scrollbar ──
        if (maxScroll > 0) {
            int trackX = panelX + panelWidth - 8;
            int trackY = contentY;
            int trackH = contentHeight;
            int thumbH = Math.max(15, (int) (trackH * ((double) trackH / totalHeight)));
            int thumbY = trackY + (int) ((scrollOffset / maxScroll) * (trackH - thumbH));

            boolean sbHovered = mouseX >= trackX - 2 && mouseX <= trackX + 6 && mouseY >= trackY && mouseY <= trackY + trackH;
            // Track
            EzUi.roundedRect(graphics, trackX, trackY, 3, trackH, 1, 0x20FFFFFF);
            // Thumb
            EzUi.roundedRect(graphics, trackX, thumbY, 3, thumbH, 1, sbHovered || isDraggingScrollbar ? EzUi.ACCENT_EMERALD_HOVER : EzUi.ACCENT_EMERALD);
        }

        // Empty state
        if (filtered.isEmpty()) {
            drawScaledCenteredText(graphics, app.ezclient.util.EzI18n.comp("ezclient.hub.no_modules"),
                    panelX + panelWidth / 2.0f, panelY + panelHeight / 2.0f - 8, 0.75f, EzUi.TEXT_MUTED);
        }



        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawScaledCenteredText(GuiGraphicsExtractor graphics, Component text,
                                         float centerX, float y, float scale, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(scale, scale);
        graphics.centeredText(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private void drawScaledText(GuiGraphicsExtractor graphics, String text,
                                float x, float y, float scale, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }


    private void openModuleSettings(Module module) {
        if (minecraft == null) return;
        EzScreenBridge.set(minecraft, createModuleSettingsScreen(this, module));
    }

    /** One routing table shared by the hub and the in-game settings audit. */
    public static Screen createModuleSettingsScreen(Screen parent, Module module) {
        if (module instanceof CrosshairModule crosshair) {
            return new CrosshairSettingsScreen(parent, crosshair);
        } else if (module instanceof KeystrokesModule ks) {
            return new KeystrokesSettingsScreen(parent, ks);
        } else if (module instanceof ItemModelModule itemModel) {
            return new ItemModelScreen(parent, itemModel);
        } else if (module instanceof FeatureModule feature) {
            return new FeatureSettingsScreen(parent, feature);
        } else if (module instanceof HudModule hud) {
            return new HudSettingsScreen(parent, hud);
        } else {
            return new ModuleSettingsScreen(parent, module);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isShiftDown() {
        if (minecraft == null || minecraft.getWindow() == null) return false;
        long handle = minecraft.getWindow().handle();
        return org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(handle, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    @Override
    public void onClose() {
        EzScreenBridge.set(minecraft, parent);
    }
}
