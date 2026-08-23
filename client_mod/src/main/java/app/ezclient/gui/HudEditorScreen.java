package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.cursor.CursorType;
import org.lwjgl.glfw.GLFW;

/** Live HUD canvas with window-like corner resize, guides and scale snapping. */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule selected;
    private double offsetX, offsetY;
    private boolean dragging;
    private boolean resizing;
    private int resizeCorner;
    private double resizeStartScale;
    private double resizeStartX, resizeStartY;
    private int resizeStartModuleX, resizeStartModuleY, resizeStartWidth, resizeStartHeight;
    private int guideX = -1, guideY = -1;
    private boolean sizeSnapped;
    private static final int SNAP_DISTANCE = 4;
    private static CursorType nwseCursor;
    private static CursorType neswCursor;
    private int activeCursorShape;
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
    private int resizeCorner(HudModule h, double mx, double my) {
        int w = (int)((font.width(h.displayText(minecraft)) + 8) * h.getScale());
        int he = (int)(13 * h.getScale());
        boolean left = Math.abs(mx - h.getX()) <= 7, right = Math.abs(mx - (h.getX() + w)) <= 7;
        boolean top = Math.abs(my - h.getY()) <= 7, bottom = Math.abs(my - (h.getY() + he)) <= 7;
        if (left && top) return 1;
        if (right && top) return 2;
        if (left && bottom) return 3;
        return right && bottom ? 4 : 0;
    }
    private HudModule resizeTarget(double mx, double my) {
        for (HudModule h : ModuleManager.getInstance().getHudModules())
            if (h.isEnabled() && resizeCorner(h, mx, my) != 0) return h;
        return null;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        HudModule handle = e.button() == 0 ? resizeTarget(e.x(), e.y()) : null;
        HudModule hit = handle != null ? handle : hit(e.x(), e.y());
        if (hit != null) {
            selected = hit; offsetX = e.x() - hit.getX(); offsetY = e.y() - hit.getY();
            if (e.button() == 1) minecraft.gui.setScreen(new HudSettingsScreen(this, hit));
            dragging = e.button() == 0;
            if (handle != null) {
                resizing = true; resizeCorner = resizeCorner(hit, e.x(), e.y()); resizeStartScale = hit.getScale(); resizeStartX = e.x(); resizeStartY = e.y();
                resizeStartModuleX = hit.getX(); resizeStartModuleY = hit.getY();
                resizeStartWidth = (int)((font.width(hit.displayText(minecraft)) + 8) * hit.getScale());
                resizeStartHeight = (int)(13 * hit.getScale());
            }
            return true;
        }
        selected = null; dragging = false; resizing = false;
        return super.mouseClicked(e, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (selected != null && dragging && e.button() == 0) {
            if (resizing) {
                int textWidth = Math.max(1, font.width(selected.displayText(minecraft)) + 8);
                boolean resizeLeft = resizeCorner == 1 || resizeCorner == 3;
                boolean resizeTop = resizeCorner == 1 || resizeCorner == 2;
                double dxResize = resizeLeft ? resizeStartX - e.x() : e.x() - resizeStartX;
                double dyResize = resizeTop ? resizeStartY - e.y() : e.y() - resizeStartY;
                double scale = snapScale(resizeStartScale + Math.max(dxResize, dyResize) / textWidth);
                double maxScaleForScreen = Math.max(0.5, Math.min(
                        width / (double) textWidth, (height - 38) / 13.0));
                scale = Math.min(scale, Math.min(3.0, maxScaleForScreen));
                int newWidth = (int)(textWidth * scale), newHeight = (int)(13 * scale);
                int newX = resizeLeft ? resizeStartModuleX + resizeStartWidth - newWidth : resizeStartModuleX;
                int newY = resizeTop ? resizeStartModuleY + resizeStartHeight - newHeight : resizeStartModuleY;
                selected.setPosition(Math.max(0, newX), Math.max(22, newY));
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
            boolean snappedSpacing = false;
            int preferredGap = preferredVerticalGap();
            if (preferredGap >= 0) {
                for (HudModule other : ModuleManager.getInstance().getHudModules()) {
                    if (other == selected || !other.isEnabled()) continue;
                    int otherHeight = (int)(13 * other.getScale());
                    int targetY = other.getY() + otherHeight + preferredGap;
                    if (Math.abs(ny - targetY) <= SNAP_DISTANCE) {
                        ny = targetY;
                        guideY = targetY;
                        snappedSpacing = true;
                        break;
                    }
                }
            }
            if (!snappedSpacing) outerY: for (HudModule other : ModuleManager.getInstance().getHudModules()) {
                if (other == selected || !other.isEnabled()) continue;
                int oh = (int)(13 * other.getScale());
                int[] otherY = {other.getY(), other.getY() + oh / 2, other.getY() + oh};
                for (int si = 0; si < 3; si++) for (int target : otherY) {
                    if (Math.abs(selectedY[si] - target) <= SNAP_DISTANCE) {
                        ny += target - selectedY[si]; guideY = target; break outerY;
                    }
                }
            }
            nx = Math.max(0, Math.min(width - sw, nx));
            ny = Math.max(22, Math.min(height - 32 - sh, ny));
            selected.setPosition(nx, ny); return true;
        }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (dragging) { dragging = false; resizing = false; guideX = -1; guideY = -1; sizeSnapped = false; ConfigManager.save(); return true; }
        return super.mouseReleased(e);
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        HudModule hit = hit(mx, my); if (hit != null) selected = hit;
        if (selected != null) {
            double scale = snapScale(selected.getScale() + vertical * 0.1);
            selected.setScale(scale); return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (selected != null) {
            int step = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
            int moveX = 0, moveY = 0;
            if (event.key() == GLFW.GLFW_KEY_LEFT) moveX = -step;
            else if (event.key() == GLFW.GLFW_KEY_RIGHT) moveX = step;
            else if (event.key() == GLFW.GLFW_KEY_UP) moveY = -step;
            else if (event.key() == GLFW.GLFW_KEY_DOWN) moveY = step;
            else return super.keyPressed(event);

            int moduleWidth = (int)((font.width(selected.displayText(minecraft)) + 8) * selected.getScale());
            int moduleHeight = (int)(13 * selected.getScale());
            int nextX = Math.max(0, Math.min(width - moduleWidth, selected.getX() + moveX));
            int nextY = Math.max(22, Math.min(height - 32 - moduleHeight, selected.getY() + moveY));
            selected.setPosition(nextX, nextY);
            ConfigManager.save();
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        if (!dragging) {
            EzUi.roundedRect(g, 8, 8, width - 16, 30, 12, 0xE81B202B);
            g.text(font, "HUD EDITOR", 19, 19, 0xFFC4B5FD);
        g.text(font, "Drag: move  •  Corner: resize + snap  •  Right-click: customize", 96, 19, 0xFFE8EDF1);
        }
        for (HudModule h : ModuleManager.getInstance().getHudModules()) if (h.isEnabled()) HudRenderer.draw(g, h, true);
        if (selected != null) {
            int sw = (int)((font.width(selected.displayText(minecraft)) + 8) * selected.getScale());
            int sh = (int)(13 * selected.getScale());
            g.outline(selected.getX() - 2, selected.getY() - 2, sw + 4, sh + 4, 0xFFC4B5FD);
            for (int cornerX : new int[]{selected.getX(), selected.getX() + sw - 6})
                for (int cornerY : new int[]{selected.getY(), selected.getY() + sh - 6})
                    EzUi.roundedRect(g, cornerX, cornerY, 6, 6, 3, 0xFFC4B5FD);
        }
        if (guideX >= 0) g.fill(guideX, 21, guideX + 1, height - 31, 0xFFFF3B3B);
        if (guideY >= 0) g.fill(0, guideY, width, guideY + 1, 0xFFFF3B3B);
        if (sizeSnapped && selected != null) {
            int sw = (int)((font.width(selected.displayText(minecraft)) + 8) * selected.getScale());
            int sh = (int)(13 * selected.getScale());
            g.outline(selected.getX() - 1, selected.getY() - 1, sw + 2, sh + 2, 0xFFFF3B3B);
        }
        if (selected != null && !dragging) g.text(font, "Selected: " + selected.getName(), 7, height - 19, 0xFFFFFF88);
        super.extractRenderState(g, mx, my, d);
    }
    private double snapScale(double scale) {
        sizeSnapped = false;
        for (HudModule other : ModuleManager.getInstance().getHudModules()) {
            if (other != selected && other.isEnabled() && Math.abs(scale - other.getScale()) <= 0.12) {
                sizeSnapped = true;
                return other.getScale();
            }
        }
        double grid = Math.round(scale * 20.0) / 20.0;
        sizeSnapped = Math.abs(grid - scale) <= 0.026;
        return grid;
    }
    private int preferredVerticalGap() {
        int bestGap = Integer.MAX_VALUE;
        for (HudModule upper : ModuleManager.getInstance().getHudModules()) {
            if (!upper.isEnabled()) continue;
            int upperHeight = (int)(13 * upper.getScale());
            for (HudModule lower : ModuleManager.getInstance().getHudModules()) {
                if (upper == lower || !lower.isEnabled()) continue;
                int gap = lower.getY() - (upper.getY() + upperHeight);
                if (gap >= 0 && gap <= 32 && Math.abs(lower.getX() - upper.getX()) <= SNAP_DISTANCE) {
                    bestGap = Math.min(bestGap, gap);
                }
            }
        }
        return bestGap == Integer.MAX_VALUE ? -1 : bestGap;
    }
    @Override public void mouseMoved(double mouseX, double mouseY) {
        HudModule target = resizeTarget(mouseX, mouseY);
        if (target != null) {
            int corner = resizeCorner(target, mouseX, mouseY);
            int shape = corner == 1 || corner == 4 ? GLFW.GLFW_RESIZE_NWSE_CURSOR : GLFW.GLFW_RESIZE_NESW_CURSOR;
            if (activeCursorShape != shape) {
                activeCursorShape = shape;
                if (shape == GLFW.GLFW_RESIZE_NWSE_CURSOR) {
                    if (nwseCursor == null) nwseCursor = CursorType.createStandardCursor(shape, "ezclient_nwse", CursorType.DEFAULT);
                    minecraft.getWindow().selectCursor(nwseCursor);
                } else {
                    if (neswCursor == null) neswCursor = CursorType.createStandardCursor(shape, "ezclient_nesw", CursorType.DEFAULT);
                    minecraft.getWindow().selectCursor(neswCursor);
                }
            }
        } else if (activeCursorShape != 0) {
            activeCursorShape = 0;
            minecraft.getWindow().selectCursor(CursorType.DEFAULT);
        }
        super.mouseMoved(mouseX, mouseY);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
