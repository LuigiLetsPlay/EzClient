package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Clean, focused HUD Editor for EzClient modules:
 * - Unified 1px snug emerald selection outline
 * - Bottom toolbar: Save, Reset All, Cancel
 * - Inter-module vertical stacking and boundary snapping
 * - Corner resize handle with live scale indicator
 * - Unobstructed view during drag
 * - Zero interference with vanilla elements
 */
public final class HudEditorScreen extends Screen {
    private static class ModuleSnapshot {
        final int x, y;
        final double scale;
        ModuleSnapshot(int x, int y, double scale) { this.x = x; this.y = y; this.scale = scale; }
    }

    private final Screen parent;
    private HudModule selected;

    private double offsetX, offsetY;
    private boolean dragging;
    private boolean resizing;
    private double resizeStartScale;
    private double resizeStartX, resizeStartY;
    private int resizeStartWidth;

    // Visual snapping guides
    private int guideX = -1, guideY = -1;
    private static final int SNAP_DISTANCE = 5;
    private static final int STACK_GAP = 4;

    private boolean hasUnsavedChanges = false;

    // Snapshots for Discard / Cancel
    private final Map<HudModule, ModuleSnapshot> initialModules = new HashMap<>();

    // Modals
    private boolean showUnsavedModal = false;
    private boolean showResetAllModal = false;
    private boolean showResetElementModal = false;

    // Context menu
    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private HudModule contextModule;

    private EzButton btnSave, btnResetAll, btnCancel;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Capture initial snapshot once on open
        if (initialModules.isEmpty()) {
            for (HudModule m : ModuleManager.getInstance().getHudModules()) {
                initialModules.put(m, new ModuleSnapshot(m.getX(), m.getY(), m.getScale()));
            }
        }

        // Bottom Action Toolbar (3 cohesive buttons)
        int btnY = height - 26;
        btnSave = new EzButton(width / 2 - 155, btnY, 100, 20, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.save"), true, b -> saveAndClose());
        btnResetAll = new EzButton(width / 2 - 50, btnY, 110, 20, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.reset_all"), false, b -> showResetAllModal = true);
        btnCancel = new EzButton(width / 2 + 65, btnY, 100, 20, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.cancel"), false, b -> handleCloseRequest());

        addRenderableWidget(btnSave);
        addRenderableWidget(btnResetAll);
        addRenderableWidget(btnCancel);
    }

    private void saveAndClose() {
        ConfigManager.save();
        hasUnsavedChanges = false;
        EzScreenBridge.set(minecraft, parent);
    }

    private void discardAndClose() {
        for (Map.Entry<HudModule, ModuleSnapshot> entry : initialModules.entrySet()) {
            entry.getKey().setPosition(entry.getValue().x, entry.getValue().y);
            entry.getKey().setScale(entry.getValue().scale);
        }
        ConfigManager.save();
        hasUnsavedChanges = false;
        EzScreenBridge.set(minecraft, parent);
    }

    private void handleCloseRequest() {
        if (hasUnsavedChanges) {
            showUnsavedModal = true;
        } else {
            EzScreenBridge.set(minecraft, parent);
        }
    }

    private int getModuleWidth(HudModule h) {
        return (int) (h.getWidth(minecraft, true) * h.getScale());
    }

    private int getModuleHeight(HudModule h) {
        return (int) (h.getHeight(minecraft) * h.getScale());
    }

    private HudModule hit(double mx, double my) {
        // First check enabled modules
        for (HudModule h : ModuleManager.getInstance().getHudModules()) {
            if (!h.isEnabled()) continue;
            int w = getModuleWidth(h);
            int he = getModuleHeight(h);
            if (mx >= h.getX() && mx <= h.getX() + w && my >= h.getY() && my <= h.getY() + he) return h;
        }
        // Then check disabled modules
        for (HudModule h : ModuleManager.getInstance().getHudModules()) {
            if (h.isEnabled()) continue;
            int w = getModuleWidth(h);
            int he = getModuleHeight(h);
            if (mx >= h.getX() && mx <= h.getX() + w && my >= h.getY() && my <= h.getY() + he) return h;
        }
        return null;
    }

    private boolean isResizeHandleHit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x + w - 12 && mx <= x + w + 4 && my >= y + h - 12 && my <= y + h + 4;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        // ── Check Modals First ──
        if (showUnsavedModal) {
            int diaW = 276, diaH = 92;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;
            if (e.button() == 0) {
                if (e.x() >= diaX + 12 && e.x() <= diaX + 88 && e.y() >= diaY + 54 && e.y() <= diaY + 76) {
                    saveAndClose();
                    return true;
                }
                if (e.x() >= diaX + 94 && e.x() <= diaX + 174 && e.y() >= diaY + 54 && e.y() <= diaY + 76) {
                    discardAndClose();
                    return true;
                }
                if (e.x() >= diaX + 180 && e.x() <= diaX + 258 && e.y() >= diaY + 54 && e.y() <= diaY + 76) {
                    showUnsavedModal = false;
                    return true;
                }
            }
            return true;
        }

        if (showResetAllModal) {
            int diaW = 260, diaH = 90;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;
            if (e.button() == 0) {
                if (e.x() >= diaX + 16 && e.x() <= diaX + 120 && e.y() >= diaY + 54 && e.y() <= diaY + 76) {
                    for (HudModule m : ModuleManager.getInstance().getHudModules()) {
                        m.resetToDefaults();
                    }
                    ConfigManager.save();
                    hasUnsavedChanges = true;
                    showResetAllModal = false;
                    return true;
                }
                if (e.x() >= diaX + 136 && e.x() <= diaX + 244 && e.y() >= diaY + 54 && e.y() <= diaY + 76) {
                    showResetAllModal = false;
                    return true;
                }
            }
            return true;
        }

        if (showResetElementModal) {
            int diaW = 250, diaH = 88;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;
            if (e.button() == 0) {
                if (e.x() >= diaX + 16 && e.x() <= diaX + 116 && e.y() >= diaY + 52 && e.y() <= diaY + 74) {
                    if (contextModule != null) {
                        contextModule.resetToDefaults();
                        ConfigManager.save();
                        hasUnsavedChanges = true;
                    }
                    showResetElementModal = false;
                    return true;
                }
                if (e.x() >= diaX + 130 && e.x() <= diaX + 234 && e.y() >= diaY + 52 && e.y() <= diaY + 74) {
                    showResetElementModal = false;
                    return true;
                }
            }
            return true;
        }

        // ── Check Context Menu Clicks ──
        if (showContextMenu) {
            int cmW = 142;
            int cmH = 68;
            if (e.x() >= contextMenuX && e.x() <= contextMenuX + cmW && e.y() >= contextMenuY && e.y() <= contextMenuY + cmH) {
                if (e.button() == 0) {
                    int relY = (int) e.y() - contextMenuY;
                    if (relY < 22) {
                        // Settings item
                        showContextMenu = false;
                        if (contextModule != null) {
                            EzScreenBridge.set(minecraft, new HudSettingsScreen(this, contextModule));
                        }
                    } else if (relY < 44) {
                        // Toggle enabled/disabled item
                        showContextMenu = false;
                        if (contextModule != null) {
                            contextModule.setEnabled(!contextModule.isEnabled());
                            ConfigManager.save();
                            hasUnsavedChanges = true;
                        }
                    } else {
                        // Reset item
                        showContextMenu = false;
                        showResetElementModal = true;
                    }
                    return true;
                }
            } else {
                showContextMenu = false;
            }
        }

        // ── Right Click -> Context Menu ──
        if (e.button() == 1) {
            HudModule mHit = hit(e.x(), e.y());
            if (mHit != null) {
                selected = mHit;
                contextModule = mHit;
                contextMenuX = Math.min((int) e.x(), width - 146);
                contextMenuY = Math.min((int) e.y(), height - 52);
                showContextMenu = true;
                return true;
            }
            showContextMenu = false;
        }

        // ── Left Click: Select, Drag or Resize ──
        if (e.button() == 0) {
            // Check if clicking resize handle on selected module
            if (selected != null) {
                int sw = getModuleWidth(selected);
                int sh = getModuleHeight(selected);
                if (isResizeHandleHit(e.x(), e.y(), selected.getX(), selected.getY(), sw, sh)) {
                    resizing = true;
                    resizeStartX = e.x();
                    resizeStartY = e.y();
                    resizeStartWidth = selected.getWidth(minecraft);
                    resizeStartScale = selected.getScale();
                    hasUnsavedChanges = true;
                    return true;
                }
            }

            HudModule mHit = hit(e.x(), e.y());
            if (mHit != null) {
                selected = mHit;
                offsetX = e.x() - selected.getX();
                offsetY = e.y() - selected.getY();
                dragging = true;
                hasUnsavedChanges = true;
                return true;
            }

            // Clicked background -> deselect
            if (e.y() < height - 32) {
                selected = null;
            }
        }

        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (e.button() == 0) {
            dragging = false;
            resizing = false;
            guideX = -1;
            guideY = -1;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (resizing && selected != null && e.button() == 0) {
            int baseW = Math.max(20, resizeStartWidth);
            double deltaX = e.x() - resizeStartX;
            double newScale = resizeStartScale + (deltaX / baseW);
            newScale = Math.round(Math.max(0.5, Math.min(2.5, newScale)) * 10.0) / 10.0;
            selected.setScale(newScale);
            return true;
        }

        if (dragging && selected != null && e.button() == 0) {
            int mw = getModuleWidth(selected);
            int mh = getModuleHeight(selected);

            int targetX = (int) (e.x() - offsetX);
            int targetY = (int) (e.y() - offsetY);

            int snapX = targetX;
            int snapY = targetY;
            guideX = -1;
            guideY = -1;

            // Screen edge snapping
            if (Math.abs(targetX) < SNAP_DISTANCE) { snapX = 0; guideX = 0; }
            if (Math.abs(targetX + mw - width) < SNAP_DISTANCE) { snapX = width - mw; guideX = width; }
            if (Math.abs(targetY) < SNAP_DISTANCE) { snapY = 0; guideY = 0; }
            if (Math.abs(targetY + mh - (height - 30)) < SNAP_DISTANCE) { snapY = height - 30 - mh; guideY = height - 30; }

            // Inter-module snapping
            for (HudModule other : ModuleManager.getInstance().getHudModules()) {
                if (other == selected || !other.isEnabled()) continue;
                int ox = other.getX(), oy = other.getY();
                int ow = getModuleWidth(other), oh = getModuleHeight(other);

                // Left-edge align
                if (Math.abs(targetX - ox) < SNAP_DISTANCE) { snapX = ox; guideX = ox; }
                // Right-edge align
                if (Math.abs(targetX + mw - (ox + ow)) < SNAP_DISTANCE) { snapX = ox + ow - mw; guideX = ox + ow; }
                // Top-edge align
                if (Math.abs(targetY - oy) < SNAP_DISTANCE) { snapY = oy; guideY = oy; }
                // Bottom stack align with 4px gap
                if (Math.abs(targetY - (oy + oh + STACK_GAP)) < SNAP_DISTANCE) { snapY = oy + oh + STACK_GAP; guideY = snapY; }
                // Top stack align with 4px gap
                if (Math.abs(targetY + mh + STACK_GAP - oy) < SNAP_DISTANCE) { snapY = oy - mh - STACK_GAP; guideY = oy; }
            }

            // Keep within visible bounds
            snapX = Math.max(0, Math.min(width - mw, snapX));
            snapY = Math.max(0, Math.min(height - 30 - mh, snapY));

            selected.setPosition(snapX, snapY);
            return true;
        }

        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            handleCloseRequest();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        extractTransparentBackground(g);

        boolean isInteracting = dragging || resizing;

        // ── 1. Render Snapping Guides ──
        if (guideX >= 0) g.fill(guideX, 0, guideX + 1, height, 0xAA00D2FF);
        if (guideY >= 0) g.fill(0, guideY, width, guideY + 1, 0xAA00D2FF);

        // ── 2. Render EzClient Modules ──
        for (HudModule h : ModuleManager.getInstance().getHudModules()) {
            if (h.isEnabled()) {
                HudRenderer.draw(g, h, true);
            } else {
                int hx = h.getX();
                int hy = h.getY();
                int hw = getModuleWidth(h);
                int hh = getModuleHeight(h);
                // Ghost preview with outline and OFF badge so disabled modules are visible and positionable
                g.fill(hx, hy, hx + hw, hy + hh, 0x40101722);
                g.outline(hx, hy, hw, hh, 0x608090A0);
                HudRenderer.draw(g, h, true);
                int badgeW = font.width("AUS") + 4;
                g.fill(hx + hw - badgeW - 1, hy + 1, hx + hw - 1, hy + 9, 0xC0992222);
                g.text(font, "AUS", hx + hw - badgeW + 1, hy + 1, 0xFFFFFFFF);
            }
        }

        // ── 3. Highlight Active Selection with Snug 1px Outline & Corner Resize Handle ──
        if (selected != null) {
            int sx = selected.getX();
            int sy = selected.getY();
            int sw = getModuleWidth(selected);
            int sh = getModuleHeight(selected);

            // Snug 1px emerald border fitting tightly around module
            g.outline(sx, sy, sw, sh, 0xFF22C96E);

            // Bottom-Right Corner Resize Handle (↘)
            int hx = sx + sw - 8;
            int hy = sy + sh - 8;
            EzUi.roundedRect(g, hx, hy, 8, 8, 2, 0xFF22C96E);
            g.text(font, "↘", hx + 1, hy - 1, 0xFFFFFFFF);

            if (resizing) {
                String scaleStr = String.format(java.util.Locale.ROOT, "%.1fx", selected.getScale());
                EzUi.roundedRect(g, hx + 10, hy - 4, 30, 14, 3, 0xF0121722);
                g.centeredText(font, Component.literal(scaleStr), hx + 25, hy - 1, 0xFF43DD8C);
            }
        }

        // Top Status Bar (Hidden during drag)
        if (!isInteracting && hasUnsavedChanges) {
            EzUi.roundedRect(g, width / 2 - 80, 8, 160, 18, 4, 0xD0121722);
            g.centeredText(font, Component.literal("● " + app.ezclient.util.EzI18n.get("ezclient.hud_editor.modal.unsaved_title")), width / 2, 13, 0xFFEAB308);
        }

        // ── 4. Render Right-Click Context Menu Popup ──
        if (showContextMenu && !isInteracting && contextModule != null) {
            int cmW = 142;
            int cmH = 68;

            EzUi.roundedRect(g, contextMenuX - 2, contextMenuY - 2, cmW + 4, cmH + 4, 6, 0xFF202736);
            EzUi.roundedRect(g, contextMenuX, contextMenuY, cmW, cmH, 5, 0xF5131A26);

            boolean hovSettings = mx >= contextMenuX && mx <= contextMenuX + cmW && my >= contextMenuY && my < contextMenuY + 22;
            boolean hovToggle = mx >= contextMenuX && mx <= contextMenuX + cmW && my >= contextMenuY + 22 && my < contextMenuY + 44;
            boolean hovReset = mx >= contextMenuX && mx <= contextMenuX + cmW && my >= contextMenuY + 44 && my <= contextMenuY + cmH;

            if (hovSettings) EzUi.roundedRect(g, contextMenuX + 2, contextMenuY + 2, cmW - 4, 19, 4, 0xFF22C96E);
            g.text(font, app.ezclient.util.EzI18n.get("ezclient.hud_editor.context.edit"), contextMenuX + 8, contextMenuY + 7, 0xFFFFFFFF);

            if (hovToggle) EzUi.roundedRect(g, contextMenuX + 2, contextMenuY + 23, cmW - 4, 19, 4, contextModule.isEnabled() ? 0xFF992222 : 0xFF22C96E);
            String toggleText = contextModule.isEnabled() ? "✕ Deaktivieren" : "✓ Aktivieren";
            g.text(font, toggleText, contextMenuX + 8, contextMenuY + 28, 0xFFFFFFFF);

            if (hovReset) EzUi.roundedRect(g, contextMenuX + 2, contextMenuY + 45, cmW - 4, 19, 4, 0xFF334155);
            g.text(font, "↺ " + app.ezclient.util.EzI18n.get("ezclient.hud_editor.context.reset_pos"), contextMenuX + 8, contextMenuY + 50, 0xFFFFFFFF);
        }

        super.extractRenderState(g, mx, my, d);

        // ── 5. Render Modals ──
        if (showUnsavedModal) {
            g.fill(0, 0, width, height, 0xAA000000);
            int diaW = 276, diaH = 92;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;

            EzUi.roundedRect(g, diaX, diaY, diaW, diaH, 8, 0xF5131A26);
            g.outline(diaX, diaY, diaW, diaH, 0xFF2A3644);

            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.unsaved_title"), diaX + diaW / 2, diaY + 12, 0xFFFFFFFF);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.unsaved_desc"), diaX + diaW / 2, diaY + 30, 0xFF94A3B8);

            boolean hovSave = mx >= diaX + 12 && mx <= diaX + 88 && my >= diaY + 54 && my <= diaY + 76;
            EzUi.roundedRect(g, diaX + 12, diaY + 54, 76, 22, 4, hovSave ? 0xFF2DD47A : 0xFF22C96E);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.save"), diaX + 12 + 38, diaY + 61, 0xFFFFFFFF);

            boolean hovDisc = mx >= diaX + 94 && mx <= diaX + 174 && my >= diaY + 54 && my <= diaY + 76;
            EzUi.roundedRect(g, diaX + 94, diaY + 54, 80, 22, 4, hovDisc ? 0xFFDC2626 : 0xFFEF4444);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.discard"), diaX + 94 + 40, diaY + 61, 0xFFFFFFFF);

            boolean hovAbr = mx >= diaX + 180 && mx <= diaX + 258 && my >= diaY + 54 && my <= diaY + 76;
            EzUi.roundedRect(g, diaX + 180, diaY + 54, 84, 22, 4, hovAbr ? 0xFF475569 : 0xFF334155);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.cancel"), diaX + 180 + 42, diaY + 61, 0xFFFFFFFF);
        } else if (showResetAllModal) {
            g.fill(0, 0, width, height, 0xAA000000);
            int diaW = 260, diaH = 90;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;

            EzUi.roundedRect(g, diaX, diaY, diaW, diaH, 8, 0xF5131A26);
            g.outline(diaX, diaY, diaW, diaH, 0xFF2A3644);

            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.reset_all_title"), diaX + diaW / 2, diaY + 12, 0xFFFFFFFF);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.reset_all_desc"), diaX + diaW / 2, diaY + 30, 0xFF94A3B8);

            boolean hovYes = mx >= diaX + 16 && mx <= diaX + 120 && my >= diaY + 54 && my <= diaY + 76;
            EzUi.roundedRect(g, diaX + 16, diaY + 54, 104, 22, 4, hovYes ? 0xFFDC2626 : 0xFFEF4444);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.yes_reset"), diaX + 16 + 52, diaY + 61, 0xFFFFFFFF);

            boolean hovNo = mx >= diaX + 136 && mx <= diaX + 244 && my >= diaY + 54 && my <= diaY + 76;
            EzUi.roundedRect(g, diaX + 136, diaY + 54, 108, 22, 4, hovNo ? 0xFF475569 : 0xFF334155);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.cancel"), diaX + 136 + 54, diaY + 61, 0xFFFFFFFF);
        } else if (showResetElementModal) {
            g.fill(0, 0, width, height, 0xAA000000);
            int diaW = 250, diaH = 88;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;

            EzUi.roundedRect(g, diaX, diaY, diaW, diaH, 8, 0xF5131A26);
            g.outline(diaX, diaY, diaW, diaH, 0xFF2A3644);

            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.reset_element_title"), diaX + diaW / 2, diaY + 12, 0xFFFFFFFF);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.reset_element_desc"), diaX + diaW / 2, diaY + 30, 0xFF94A3B8);

            boolean hovYes = mx >= diaX + 16 && mx <= diaX + 116 && my >= diaY + 52 && my <= diaY + 74;
            EzUi.roundedRect(g, diaX + 16, diaY + 52, 100, 22, 4, hovYes ? 0xFFDC2626 : 0xFFEF4444);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.modal.yes_reset"), diaX + 16 + 50, diaY + 59, 0xFFFFFFFF);

            boolean hovNo = mx >= diaX + 130 && mx <= diaX + 234 && my >= diaY + 52 && my <= diaY + 74;
            EzUi.roundedRect(g, diaX + 130, diaY + 52, 104, 22, 4, hovNo ? 0xFF475569 : 0xFF334155);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.cancel"), diaX + 130 + 52, diaY + 59, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        handleCloseRequest();
    }
}
