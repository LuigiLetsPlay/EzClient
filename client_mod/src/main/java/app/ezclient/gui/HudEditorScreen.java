package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Live HUD canvas: drag elements, wheel-resize, right-click for all style options. */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule selected;
    private double offsetX, offsetY;
    private boolean resizing;
    private double resizeStartScale;
    private double resizeStartX, resizeStartY;
    private int guideX = -1, guideY = -1;
    private boolean sizeSnapped;
    private static final int SNAP_DISTANCE = 4;
    public HudEditorScreen(Screen parent) { super(Component.literal("HUD Editor")); this.parent = parent; }
    @Override protected void init() {
        addRenderableWidget(new EzButton(width / 2 - 80, height - 27, 76, 19, Component.literal("Settings"), true,
                b -> { if (selected != null) minecraft.gui.setScreen(new HudSettingsScreen(this, selected)); }));
        addRenderableWidget(new EzButton(width / 2 + 4, height - 27, 76, 19, Component.literal("Done"), false, b -> onClose()));
    }
    private HudModule hit(double mx, double my) {
        for (HudModule h : ModuleManager.getInstance().getHudModules()) {
            if (!h.isEnabled()) continue;
            int w = (int)((font.width(h.displayText(minecraft)) + 8) * h.getScale());
            int he = (int)(13 * h.getScale());
            if (mx >= h.getX() && mx <= h.getX() + w && my >= h.getY() && my <= h.getY() + he) return h;
        }
        return null;
    }
    private boolean hitsResizeHandle(HudModule h, double mx, double my) {
        int w = (int)((font.width(h.displayText(minecraft)) + 8) * h.getScale());
        int he = (int)(13 * h.getScale());
        return mx >= h.getX() + w - 5 && my >= h.getY() + he - 5 && mx <= h.getX() + w + 3 && my <= h.getY() + he + 3;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        HudModule hit = hit(e.x(), e.y());
        if (hit != null) {
            selected = hit; offsetX = e.x() - hit.getX(); offsetY = e.y() - hit.getY();
            if (e.button() == 1) minecraft.gui.setScreen(new HudSettingsScreen(this, hit));
            if (e.button() == 0 && hitsResizeHandle(hit, e.x(), e.y())) {
                resizing = true; resizeStartScale = hit.getScale(); resizeStartX = e.x(); resizeStartY = e.y();
            }
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (selected != null && e.button() == 0) {
            if (resizing) {
                int textWidth = Math.max(1, font.width(selected.displayText(minecraft)) + 8);
                double scale = resizeStartScale + Math.max(e.x() - resizeStartX, e.y() - resizeStartY) / textWidth;
                sizeSnapped = false;
                for (HudModule other : ModuleManager.getInstance().getHudModules()) {
                    if (other != selected && other.isEnabled() && Math.abs(scale - other.getScale()) <= 0.08) {
                        scale = other.getScale(); sizeSnapped = true; break;
                    }
                }
                selected.setScale(scale); return true;
            }
            int nx = (int)(e.x() - offsetX), ny = (int)(e.y() - offsetY);
            int sw = (int)((font.width(selected.displayText(minecraft)) + 8) * selected.getScale());
            int sh = (int)(13 * selected.getScale());
            guideX = -1; guideY = -1;
            int[] selectedX = {nx, nx + sw / 2, nx + sw};
            int[] selectedY = {ny, ny + sh / 2, ny + sh};
            outerX: for (HudModule other : ModuleManager.getInstance().getHudModules()) {
                if (other == selected || !other.isEnabled()) continue;
                int ow = (int)((font.width(other.displayText(minecraft)) + 8) * other.getScale());
                int[] otherX = {other.getX(), other.getX() + ow / 2, other.getX() + ow};
                for (int si = 0; si < 3; si++) for (int target : otherX) {
                    if (Math.abs(selectedX[si] - target) <= SNAP_DISTANCE) {
                        nx += target - selectedX[si]; guideX = target; break outerX;
                    }
                }
            }
            outerY: for (HudModule other : ModuleManager.getInstance().getHudModules()) {
                if (other == selected || !other.isEnabled()) continue;
                int oh = (int)(13 * other.getScale());
                int[] otherY = {other.getY(), other.getY() + oh / 2, other.getY() + oh};
                for (int si = 0; si < 3; si++) for (int target : otherY) {
                    if (Math.abs(selectedY[si] - target) <= SNAP_DISTANCE) {
                        ny += target - selectedY[si]; guideY = target; break outerY;
                    }
                }
            }
            selected.setPosition(nx, ny); return true;
        }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (selected != null) { resizing = false; guideX = -1; guideY = -1; sizeSnapped = false; ConfigManager.save(); return true; }
        return super.mouseReleased(e);
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        HudModule hit = hit(mx, my); if (hit != null) selected = hit;
        if (selected != null) {
            double scale = selected.getScale() + vertical * 0.1;
            sizeSnapped = false;
            for (HudModule other : ModuleManager.getInstance().getHudModules()) {
                if (other != selected && other.isEnabled() && Math.abs(scale - other.getScale()) <= 0.08) {
                    scale = other.getScale(); sizeSnapped = true; break;
                }
            }
            selected.setScale(scale); return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        g.fill(0, 0, width, 21, 0xB0111419);
        g.text(font, "HUD EDITOR", 7, 7, 0xFF43DD8C);
        g.text(font, "Drag = move  |  Wheel = size  |  Right click = customize", 75, 7, 0xFFE8EDF1);
        for (HudModule h : ModuleManager.getInstance().getHudModules()) if (h.isEnabled()) HudRenderer.draw(g, h, true);
        if (selected != null) {
            int sw = (int)((font.width(selected.displayText(minecraft)) + 8) * selected.getScale());
            int sh = (int)(13 * selected.getScale());
            g.fill(selected.getX() + sw - 4, selected.getY() + sh - 4, selected.getX() + sw, selected.getY() + sh, 0xFFC4B5FD);
        }
        if (guideX >= 0) g.fill(guideX, 21, guideX + 1, height - 31, 0xFFFF3B3B);
        if (guideY >= 0) g.fill(0, guideY, width, guideY + 1, 0xFFFF3B3B);
        if (sizeSnapped && selected != null) {
            int sw = (int)((font.width(selected.displayText(minecraft)) + 8) * selected.getScale());
            int sh = (int)(13 * selected.getScale());
            g.outline(selected.getX() - 1, selected.getY() - 1, sw + 2, sh + 2, 0xFFFF3B3B);
        }
        if (selected != null) g.text(font, "Selected: " + selected.getName(), 7, height - 19, 0xFFFFFF88);
        super.extractRenderState(g, mx, my, d);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
