package app.ezclient.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Shared fixed panel geometry and content-only scrolling for settings screens. */
public abstract class ScrollingSettingsScreen extends Screen {
    /** Must stay identical to EzHubScreen. Settings never resize themselves per module. */
    protected static final int SETTINGS_WIDTH = 416;
    protected static final int SETTINGS_HEIGHT = 236;
    protected static final int SETTINGS_SIDEBAR_WIDTH = 80;
    protected static final Identifier SETTINGS_LOGO = Identifier.fromNamespaceAndPath("ezclient", "textures/icons/ezclient.png");

    private double scroll;
    private boolean draggingScrollbar;
    private final Set<AbstractWidget> fixedWidgets = Collections.newSetFromMap(new WeakHashMap<>());

    protected ScrollingSettingsScreen(Component title) { super(title); }

    protected int defaultPanelWidth() { return SETTINGS_WIDTH; }
    protected int defaultPanelHeight() { return SETTINGS_HEIGHT; }
    protected int sidebarWidth() { return SETTINGS_SIDEBAR_WIDTH; }

    @Override
    public void rebuildWidgets() {
        var focused = getFocused();
        int editBoxCursor = -1;
        boolean wasEditBox = false;
        if (focused instanceof EditBox editBox) {
            wasEditBox = true;
            editBoxCursor = editBox.getCursorPosition();
        }

        super.rebuildWidgets();

        if (wasEditBox) {
            for (var child : children()) {
                if (child instanceof EditBox editBox) {
                    editBox.setFocused(true);
                    setFocused(editBox);
                    if (editBoxCursor >= 0 && editBoxCursor <= editBox.getValue().length()) {
                        editBox.setCursorPosition(editBoxCursor);
                    }
                    break;
                }
            }
        }
    }

    protected int settingsPanelWidth() { return Math.min(defaultPanelWidth(), Math.max(120, width - 32)); }
    protected int settingsPanelHeight() { return Math.min(defaultPanelHeight(), Math.max(100, height - 32)); }
    protected int scrollLeft() { return 0; }
    protected int scrollTop() { return 0; }
    protected int scrollRight() { return width - 14; }
    protected int scrollBottom() { return height; }
    /** Bottom edge of custom-drawn scrollable content that is not represented by a widget. */
    protected int scrollContentBottom() { return scrollBottom(); }
    protected final double scrollAmount() { return scroll; }
    protected final void scrollToContentY(int contentY) {
        scroll = contentY - scrollTop();
        clamp();
    }

    protected int settingsContentLeft(int panelX) { return panelX + sidebarWidth() + 8; }
    /** Leaves a visible 7 px gutter before the five-pixel scrollbar. */
    protected int settingsContentWidth(int panelWidth) { return panelWidth - sidebarWidth() - 24; }

    protected void renderSettingsSidebar(GuiGraphicsExtractor g, int panelX, int panelY, int panelHeight, String section) {
        int sbWidth = sidebarWidth();
        g.fill(panelX + sbWidth, panelY + 6, panelX + sbWidth + 1,
                panelY + panelHeight - 6, EzUi.BORDER_SUBTLE);
        int logoX = panelX + (sbWidth - 20) / 2, logoY = panelY + 9;
        EzUi.roundedRect(g, logoX, logoY, 20, 20, 3, 0xFF15181C);
        ModuleIconRenderer.drawTexture(g, SETTINGS_LOGO, logoX + 2, logoY + 2, 16);
        if (section != null && !section.isBlank()) {
            EzUi.roundedRect(g, panelX + 6, panelY + 43, sbWidth - 12, 18, 2, EzUi.BG_CARD_ACTIVE);
            g.centeredText(font, Component.literal(shorten(section, 10)), panelX + sbWidth / 2,
                    panelY + 48, EzUi.TEXT_LIGHT);
        }
    }

    private static String shorten(String value, int max) {
        if (value == null || value.length() <= max) return value == null ? "Modul" : value;
        return value.substring(0, Math.max(1, max - 1)) + "…";
    }

    protected final <T extends AbstractWidget> T addFixedWidget(T widget) {
        fixedWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    private boolean scrolls(AbstractWidget widget) { return !fixedWidgets.contains(widget); }
    protected final boolean isFixedWidget(AbstractWidget widget) { return fixedWidgets.contains(widget); }
    private int maxScroll() {
        int bottom = Math.max(scrollBottom(), scrollContentBottom());
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && scrolls(widget)) {
                bottom = Math.max(bottom, widget.getY() + widget.getHeight() + 6);
            }
        }
        return Math.max(0, bottom - scrollBottom());
    }
    private boolean overflows() { return maxScroll() > 0; }
    private void clamp() { scroll = Math.max(0, Math.min(maxScroll(), scroll)); }
    private boolean insideViewport(double x, double y) {
        return x >= scrollLeft() && x < scrollRight() && y >= scrollTop() && y < scrollBottom();
    }
    private MouseButtonEvent translated(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x(), event.y() + scroll, event.buttonInfo());
    }
    @Override public final void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        clamp();
        extractSettings(g, mx, my, delta);
        if (overflows()) {
            int top = scrollTop(), viewportHeight = Math.max(1, scrollBottom() - top);
            int track = Math.max(1, viewportHeight);
            int thumb = Math.max(18, (int)((double)track * viewportHeight / (viewportHeight + maxScroll())));
            int y = top + (int)(scroll / maxScroll() * (track - thumb));
            g.fill(scrollRight() - 5, top, scrollRight(), scrollBottom(), 0xff263240);
            g.fill(scrollRight() - 5, y, scrollRight(), y + thumb, EzUi.ACCENT_EMERALD);
        }
        updatePointerCursor(mx, my);
    }

    private void updatePointerCursor(double mx, double my) {
        boolean hovered = false;
        for (var child : children()) {
            if (!(child instanceof AbstractWidget widget) || !widget.visible || !widget.active) continue;
            int displayY = widget.getY() - (scrolls(widget) ? (int)scroll : 0);
            if (scrolls(widget) && (displayY < scrollTop() || displayY + widget.getHeight() > scrollBottom())) continue;
            if (mx >= widget.getX() && mx < widget.getX() + widget.getWidth()
                    && my >= displayY && my < displayY + widget.getHeight()) {
                hovered = true;
                break;
            }
        }
        EzCursor.setPointer(hovered);
    }
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.enableScissor(scrollLeft(), scrollTop(), scrollRight(), scrollBottom());
        for (var child : children()) {
            if (child instanceof AbstractWidget widget) {
                if (scrolls(widget)) widget.setY(widget.getY() - (int)scroll);
                else widget.visible = false;
            }
        }
        super.extractRenderState(g, mx, insideViewport(mx, my) ? (int)(my + scroll) : my, delta);
        for (var child : children()) {
            if (child instanceof AbstractWidget widget) {
                if (scrolls(widget)) widget.setY(widget.getY() + (int)scroll);
                else widget.visible = true;
            }
        }
        g.disableScissor();

        for (var child : children()) if (child instanceof AbstractWidget widget && scrolls(widget)) widget.visible = false;
        super.extractRenderState(g, mx, my, delta);
        for (var child : children()) if (child instanceof AbstractWidget widget && scrolls(widget)) widget.visible = true;
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!overflows()) return super.mouseScrolled(x, y, horizontal, vertical);
        scroll -= vertical * 24;
        clamp();
        return true;
    }
    private void scrollTo(double y) {
        double fraction = Math.max(0, Math.min(1, (y - scrollTop()) / Math.max(1, scrollBottom() - scrollTop())));
        scroll = fraction * maxScroll();
    }
    @Override public final boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (e.button() == 0 && overflows() && e.x() >= scrollRight() - 8 && e.x() <= scrollRight()
                && e.y() >= scrollTop() && e.y() <= scrollBottom()) {
            draggingScrollbar = true;
            scrollTo(e.y());
            return true;
        }
        boolean inside = insideViewport(e.x(), e.y());
        java.util.List<AbstractWidget> temporarilyHidden = new java.util.ArrayList<>();
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && widget.visible && (inside ? !scrolls(widget) : scrolls(widget))) {
                widget.visible = false;
                temporarilyHidden.add(widget);
            }
        }
        try {
            return settingsMouseClicked(inside ? translated(e) : e, doubleClick);
        } finally {
            for (AbstractWidget widget : temporarilyHidden) widget.visible = true;
        }
    }
    protected boolean settingsMouseClicked(MouseButtonEvent e, boolean doubleClick) { return super.mouseClicked(e, doubleClick); }
    @Override public final boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (draggingScrollbar) { scrollTo(e.y()); return true; }
        return settingsMouseDragged(insideViewport(e.x(), e.y()) ? translated(e) : e, dx, dy);
    }
    protected boolean settingsMouseDragged(MouseButtonEvent e, double dx, double dy) { return super.mouseDragged(e, dx, dy); }
    @Override public final boolean mouseReleased(MouseButtonEvent e) {
        draggingScrollbar = false;
        return settingsMouseReleased(insideViewport(e.x(), e.y()) ? translated(e) : e);
    }
    protected boolean settingsMouseReleased(MouseButtonEvent e) { return super.mouseReleased(e); }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(x, insideViewport(x, y) ? y + scroll : y); }
    @Override public void removed() { EzCursor.setPointer(false); super.removed(); }
}
