package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Clean, smooth HUD Editor:
 * - Unified 1px snug selection outline (same across all modules)
 * - 4-button unified bottom toolbar (Vanilla Toggle, Speichern, Alles Resetten, Abbrechen)
 * - Inter-Module Vertical Stacking Snapping (4px gap alignment)
 * - Authentic 4-Slot Vanilla Armor HUD & Real Status Effect Badges
 * - Instant persistence of Vanilla toggle
 * - Unobstructed view during drag
 */
public final class HudEditorScreen extends Screen {
    public enum VanillaType {
        NONE, BOSSBAR, SCOREBOARD, EFFECTS
    }

    private static class ModuleSnapshot {
        final int x, y;
        final double scale;
        ModuleSnapshot(int x, int y, double scale) { this.x = x; this.y = y; this.scale = scale; }
    }

    private final Screen parent;
    private HudModule selected;
    private VanillaType selectedVanilla = VanillaType.NONE;

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
    private int initialBossbarX, initialBossbarY;
    private double initialBossbarScale;
    private int initialScoreboardX, initialScoreboardY;
    private double initialScoreboardScale;
    private int initialEffectsX, initialEffectsY;
    private double initialEffectsScale;
    private boolean initialCustomVanilla;

    // Modals
    private boolean showUnsavedModal = false;
    private boolean showResetAllModal = false;
    private boolean showResetElementModal = false;

    // Context menu
    private boolean showContextMenu = false;
    private int contextMenuX, contextMenuY;
    private HudModule contextModule;
    private VanillaType contextVanilla = VanillaType.NONE;

    private EzButton btnVanillaToggle, btnSave, btnResetAll, btnCancel;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (ConfigManager.bossbarX == -1) ConfigManager.bossbarX = (width - 182) / 2;
        if (ConfigManager.bossbarY == -1) ConfigManager.bossbarY = 12;
        if (ConfigManager.scoreboardX == -1) ConfigManager.scoreboardX = width - 116;
        if (ConfigManager.scoreboardY == -1) ConfigManager.scoreboardY = Math.max(0, height / 2 - 45);
        if (ConfigManager.effectsX == -1) ConfigManager.effectsX = width - 70;
        if (ConfigManager.effectsY == -1) ConfigManager.effectsY = 12;

        // Capture initial snapshot once on open
        if (initialModules.isEmpty()) {
            for (HudModule m : ModuleManager.getInstance().getHudModules()) {
                initialModules.put(m, new ModuleSnapshot(m.getX(), m.getY(), m.getScale()));
            }
            initialBossbarX = ConfigManager.bossbarX; initialBossbarY = ConfigManager.bossbarY; initialBossbarScale = ConfigManager.bossbarScale;
            initialScoreboardX = ConfigManager.scoreboardX; initialScoreboardY = ConfigManager.scoreboardY; initialScoreboardScale = ConfigManager.scoreboardScale;
            initialEffectsX = ConfigManager.effectsX; initialEffectsY = ConfigManager.effectsY; initialEffectsScale = ConfigManager.effectsScale;
            initialCustomVanilla = ConfigManager.customVanillaHud;
        }

        // Bottom Action Toolbar (4 cohesive buttons)
        int btnY = height - 26;
        btnVanillaToggle = new EzButton(
                width / 2 - 210, btnY, 102, 20,
                Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_editor.vanilla", app.ezclient.util.EzI18n.onOrOff(ConfigManager.customVanillaHud))),
                ConfigManager.customVanillaHud,
                b -> {
                    ConfigManager.customVanillaHud = !ConfigManager.customVanillaHud;
                    ConfigManager.save();
                    hasUnsavedChanges = true;
                    if (!ConfigManager.customVanillaHud) selectedVanilla = VanillaType.NONE;
                    rebuildWidgets();
                }
        );

        btnSave = new EzButton(width / 2 - 102, btnY, 94, 20, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.save"), true, b -> saveAndClose());
        btnResetAll = new EzButton(width / 2 - 2, btnY, 114, 20, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.reset_all"), false, b -> showResetAllModal = true);
        btnCancel = new EzButton(width / 2 + 118, btnY, 94, 20, app.ezclient.util.EzI18n.comp("ezclient.hud_editor.cancel"), false, b -> handleCloseRequest());

        addRenderableWidget(btnVanillaToggle);
        addRenderableWidget(btnSave);
        addRenderableWidget(btnResetAll);
        addRenderableWidget(btnCancel);
    }

    private void saveAndClose() {
        ConfigManager.save();
        hasUnsavedChanges = false;
        minecraft.gui.setScreen(parent);
    }

    private void discardAndClose() {
        for (Map.Entry<HudModule, ModuleSnapshot> entry : initialModules.entrySet()) {
            entry.getKey().setPosition(entry.getValue().x, entry.getValue().y);
            entry.getKey().setScale(entry.getValue().scale);
        }
        ConfigManager.bossbarX = initialBossbarX; ConfigManager.bossbarY = initialBossbarY; ConfigManager.bossbarScale = initialBossbarScale;
        ConfigManager.scoreboardX = initialScoreboardX; ConfigManager.scoreboardY = initialScoreboardY; ConfigManager.scoreboardScale = initialScoreboardScale;
        ConfigManager.effectsX = initialEffectsX; ConfigManager.effectsY = initialEffectsY; ConfigManager.effectsScale = initialEffectsScale;
        ConfigManager.customVanillaHud = initialCustomVanilla;
        ConfigManager.save();
        hasUnsavedChanges = false;
        minecraft.gui.setScreen(parent);
    }

    private void handleCloseRequest() {
        if (hasUnsavedChanges) {
            showUnsavedModal = true;
        } else {
            minecraft.gui.setScreen(parent);
        }
    }

    private int getModuleWidth(HudModule h) {
        return (int) (h.getWidth(minecraft) * h.getScale());
    }

    private int getModuleHeight(HudModule h) {
        return (int) (h.getHeight(minecraft) * h.getScale());
    }

    private int getVanillaWidth(VanillaType type) {
        return switch (type) {
            case BOSSBAR -> (int) (182 * ConfigManager.bossbarScale);
            case SCOREBOARD -> (int) (110 * ConfigManager.scoreboardScale);
            case EFFECTS -> (int) (66 * ConfigManager.effectsScale);
            default -> 0;
        };
    }

    private int getVanillaHeight(VanillaType type) {
        return switch (type) {
            case BOSSBAR -> (int) (20 * ConfigManager.bossbarScale);
            case SCOREBOARD -> (int) (90 * ConfigManager.scoreboardScale);
            case EFFECTS -> (int) (22 * ConfigManager.effectsScale);
            default -> 0;
        };
    }

    private int getVanillaX(VanillaType type) {
        return switch (type) {
            case BOSSBAR -> ConfigManager.bossbarX;
            case SCOREBOARD -> ConfigManager.scoreboardX;
            case EFFECTS -> ConfigManager.effectsX;
            default -> 0;
        };
    }

    private int getVanillaY(VanillaType type) {
        return switch (type) {
            case BOSSBAR -> ConfigManager.bossbarY;
            case SCOREBOARD -> ConfigManager.scoreboardY;
            case EFFECTS -> ConfigManager.effectsY;
            default -> 0;
        };
    }

    private HudModule hit(double mx, double my) {
        for (HudModule h : ModuleManager.getInstance().getHudModules()) {
            if (!h.isEnabled()) continue;
            int w = getModuleWidth(h);
            int he = getModuleHeight(h);
            if (mx >= h.getX() && mx <= h.getX() + w && my >= h.getY() && my <= h.getY() + he) return h;
        }
        return null;
    }

    private VanillaType hitVanilla(double mx, double my) {
        if (!ConfigManager.customVanillaHud) return VanillaType.NONE;

        for (VanillaType t : new VanillaType[]{VanillaType.BOSSBAR, VanillaType.SCOREBOARD, VanillaType.EFFECTS}) {
            int vx = getVanillaX(t), vy = getVanillaY(t);
            int vw = getVanillaWidth(t), vh = getVanillaHeight(t);
            if (mx >= vx && mx <= vx + vw && my >= vy && my <= vy + vh) return t;
        }
        return VanillaType.NONE;
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
                    ConfigManager.resetAllLayout(width, height);
                    hasUnsavedChanges = true;
                    showResetAllModal = false;
                    return true;
                }
                if (e.x() >= diaX + 136 && e.x() <= diaX + 244 && e.y() >= diaY + 54 && e.y() <= diaY + 76) {
                    showResetAllModal = false;
                    return true;
                }
            }
            showResetAllModal = false;
            return true;
        }

        if (showResetElementModal) {
            int diaW = 250, diaH = 88;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;
            if (e.button() == 0) {
                if (e.x() >= diaX + 16 && e.x() <= diaX + 116 && e.y() >= diaY + 52 && e.y() <= diaY + 74) {
                    if (contextModule != null) {
                        contextModule.setPosition(10, 10);
                        contextModule.setScale(1.0);
                    } else if (contextVanilla != VanillaType.NONE) {
                        if (contextVanilla == VanillaType.BOSSBAR) { ConfigManager.bossbarX = (width - 182) / 2; ConfigManager.bossbarY = 12; ConfigManager.bossbarScale = 1.0; }
                        else if (contextVanilla == VanillaType.SCOREBOARD) { ConfigManager.scoreboardX = width - 116; ConfigManager.scoreboardY = Math.max(0, height / 2 - 45); ConfigManager.scoreboardScale = 1.0; }
                        else if (contextVanilla == VanillaType.EFFECTS) { ConfigManager.effectsX = width - 70; ConfigManager.effectsY = 12; ConfigManager.effectsScale = 1.0; }
                    }
                    hasUnsavedChanges = true;
                    showResetElementModal = false;
                    return true;
                }
                if (e.x() >= diaX + 130 && e.x() <= diaX + 234 && e.y() >= diaY + 52 && e.y() <= diaY + 74) {
                    showResetElementModal = false;
                    return true;
                }
            }
            showResetElementModal = false;
            return true;
        }

        // ── Context Menu Click ──
        if (showContextMenu) {
            int cmW = 142;
            int cmH = (contextModule != null) ? 46 : 26;
            if (e.x() >= contextMenuX && e.x() <= contextMenuX + cmW && e.y() >= contextMenuY && e.y() <= contextMenuY + cmH) {
                if (e.button() == 0) {
                    if (contextModule != null) {
                        if (e.y() < contextMenuY + 23) {
                            showContextMenu = false;
                            minecraft.gui.setScreen(new HudSettingsScreen(this, contextModule));
                            return true;
                        } else {
                            showContextMenu = false;
                            showResetElementModal = true;
                            return true;
                        }
                    } else if (contextVanilla != VanillaType.NONE) {
                        showContextMenu = false;
                        showResetElementModal = true;
                        return true;
                    }
                }
            }
            showContextMenu = false;
        }

        // ── Resize Handle Click ──
        if (e.button() == 0) {
            if (selected != null) {
                int sw = getModuleWidth(selected);
                int sh = getModuleHeight(selected);
                if (isResizeHandleHit(e.x(), e.y(), selected.getX(), selected.getY(), sw, sh)) {
                    resizing = true;
                    dragging = true;
                    resizeStartScale = selected.getScale();
                    resizeStartX = e.x();
                    resizeStartY = e.y();
                    resizeStartWidth = Math.max(1, selected.getWidth(minecraft));
                    return true;
                }
            } else if (selectedVanilla != VanillaType.NONE) {
                int vx = getVanillaX(selectedVanilla);
                int vy = getVanillaY(selectedVanilla);
                int vw = getVanillaWidth(selectedVanilla);
                int vh = getVanillaHeight(selectedVanilla);
                if (isResizeHandleHit(e.x(), e.y(), vx, vy, vw, vh)) {
                    resizing = true;
                    dragging = true;
                    resizeStartScale = (selectedVanilla == VanillaType.BOSSBAR) ? ConfigManager.bossbarScale :
                            (selectedVanilla == VanillaType.SCOREBOARD) ? ConfigManager.scoreboardScale : ConfigManager.effectsScale;
                    resizeStartX = e.x();
                    resizeStartY = e.y();
                    resizeStartWidth = (selectedVanilla == VanillaType.BOSSBAR) ? 182 :
                            (selectedVanilla == VanillaType.SCOREBOARD) ? 110 : 66;
                    return true;
                }
            }
        }

        // ── Module Selection ──
        HudModule hitModule = hit(e.x(), e.y());
        if (hitModule != null) {
            selected = hitModule;
            selectedVanilla = VanillaType.NONE;

            if (e.button() == 1) {
                showContextMenu = true;
                contextMenuX = (int) Math.min(width - 146, e.x());
                contextMenuY = (int) Math.min(height - 52, e.y());
                contextModule = hitModule;
                contextVanilla = VanillaType.NONE;
                return true;
            }

            offsetX = e.x() - hitModule.getX();
            offsetY = e.y() - hitModule.getY();
            dragging = e.button() == 0;
            resizing = false;
            return true;
        }

        // ── Vanilla Selection ──
        VanillaType vHit = hitVanilla(e.x(), e.y());
        if (vHit != VanillaType.NONE) {
            selected = null;
            selectedVanilla = vHit;

            if (e.button() == 1) {
                showContextMenu = true;
                contextMenuX = (int) Math.min(width - 146, e.x());
                contextMenuY = (int) Math.min(height - 35, e.y());
                contextModule = null;
                contextVanilla = vHit;
                return true;
            }

            offsetX = e.x() - getVanillaX(vHit);
            offsetY = e.y() - getVanillaY(vHit);
            dragging = e.button() == 0;
            resizing = false;
            return true;
        }

        selected = null;
        selectedVanilla = VanillaType.NONE;
        dragging = false;
        resizing = false;
        showContextMenu = false;
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (showUnsavedModal || showResetAllModal || showResetElementModal) return true;

        if (selected != null && dragging && e.button() == 0) {
            hasUnsavedChanges = true;
            if (resizing) {
                double deltaDist = ((e.x() - resizeStartX) + (e.y() - resizeStartY)) / 2.0;
                double newScale = Math.max(0.5, Math.min(3.0, resizeStartScale + deltaDist / resizeStartWidth));
                newScale = Math.round(newScale * 20.0) / 20.0;
                selected.setScale(newScale);
                return true;
            }

            // Smooth Jitter-Free Magnetic Snapping with Inter-Module Stacking Gap
            int rawX = (int) (e.x() - offsetX);
            int rawY = (int) (e.y() - offsetY);
            int sw = getModuleWidth(selected);
            int sh = getModuleHeight(selected);

            int snapX = rawX;
            int snapY = rawY;
            guideX = -1;
            guideY = -1;

            // Snap against screen boundaries
            if (Math.abs(rawX - 10) <= SNAP_DISTANCE) { snapX = 10; guideX = 10; }
            if (Math.abs((rawX + sw) - (width - 10)) <= SNAP_DISTANCE) { snapX = width - 10 - sw; guideX = width - 10; }
            if (Math.abs(rawY - 10) <= SNAP_DISTANCE) { snapY = 10; guideY = 10; }
            if (Math.abs((rawY + sh) - (height - 10)) <= SNAP_DISTANCE) { snapY = height - 10 - sh; guideY = height - 10; }

            // Snap against other modules (Edge alignments + Stacking Gap)
            for (HudModule other : ModuleManager.getInstance().getHudModules()) {
                if (other == selected || !other.isEnabled()) continue;
                int ox = other.getX(), oy = other.getY();
                int ow = getModuleWidth(other), oh = getModuleHeight(other);

                // Horizontal alignments
                if (Math.abs(rawX - ox) <= SNAP_DISTANCE) { snapX = ox; guideX = ox; }
                else if (Math.abs((rawX + sw) - (ox + ow)) <= SNAP_DISTANCE) { snapX = ox + ow - sw; guideX = ox + ow; }
                else if (Math.abs((rawX + sw / 2) - (ox + ow / 2)) <= SNAP_DISTANCE) { snapX = ox + ow / 2 - sw / 2; guideX = ox + ow / 2; }
                // Horizontal Stacking (directly right of other)
                else if (Math.abs(rawX - (ox + ow + STACK_GAP)) <= SNAP_DISTANCE) { snapX = ox + ow + STACK_GAP; guideX = snapX; }

                // Vertical alignments
                if (Math.abs(rawY - oy) <= SNAP_DISTANCE) { snapY = oy; guideY = oy; }
                else if (Math.abs((rawY + sh) - (oy + oh)) <= SNAP_DISTANCE) { snapY = oy + oh - sh; guideY = oy + oh; }
                else if (Math.abs((rawY + sh / 2) - (oy + oh / 2)) <= SNAP_DISTANCE) { snapY = oy + oh / 2 - sh / 2; guideY = oy + oh / 2; }
                // Vertical Stacking (directly below other with crisp 4px gap)
                else if (Math.abs(rawY - (oy + oh + STACK_GAP)) <= SNAP_DISTANCE) { snapY = oy + oh + STACK_GAP; guideY = snapY; }
                // Vertical Stacking (directly above other with crisp 4px gap)
                else if (Math.abs((rawY + sh) - (oy - STACK_GAP)) <= SNAP_DISTANCE) { snapY = oy - STACK_GAP - sh; guideY = oy - STACK_GAP; }
            }

            snapX = Math.max(0, Math.min(width - sw, snapX));
            snapY = Math.max(0, Math.min(height - sh, snapY));
            selected.setPosition(snapX, snapY);
            return true;
        }

        if (selectedVanilla != VanillaType.NONE && dragging && e.button() == 0) {
            hasUnsavedChanges = true;
            if (resizing) {
                double deltaDist = ((e.x() - resizeStartX) + (e.y() - resizeStartY)) / 2.0;
                double newScale = Math.max(0.5, Math.min(2.5, resizeStartScale + deltaDist / resizeStartWidth));
                newScale = Math.round(newScale * 20.0) / 20.0;
                if (selectedVanilla == VanillaType.BOSSBAR) ConfigManager.bossbarScale = newScale;
                else if (selectedVanilla == VanillaType.SCOREBOARD) ConfigManager.scoreboardScale = newScale;
                else if (selectedVanilla == VanillaType.EFFECTS) ConfigManager.effectsScale = newScale;
                return true;
            }

            int rawX = (int) (e.x() - offsetX);
            int rawY = (int) (e.y() - offsetY);
            int vw = getVanillaWidth(selectedVanilla);
            int vh = getVanillaHeight(selectedVanilla);

            int snapX = rawX;
            int snapY = rawY;
            guideX = -1;
            guideY = -1;

            if (Math.abs(rawX - (width - vw) / 2) <= SNAP_DISTANCE) { snapX = (width - vw) / 2; guideX = width / 2; }
            if (Math.abs(rawY - 12) <= SNAP_DISTANCE) { snapY = 12; guideY = 12; }

            snapX = Math.max(0, Math.min(width - vw, snapX));
            snapY = Math.max(0, Math.min(height - vh, snapY));

            if (selectedVanilla == VanillaType.BOSSBAR) { ConfigManager.bossbarX = snapX; ConfigManager.bossbarY = snapY; }
            else if (selectedVanilla == VanillaType.SCOREBOARD) { ConfigManager.scoreboardX = snapX; ConfigManager.scoreboardY = snapY; }
            else if (selectedVanilla == VanillaType.EFFECTS) { ConfigManager.effectsX = snapX; ConfigManager.effectsY = snapY; }
            return true;
        }

        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (dragging) {
            dragging = false;
            resizing = false;
            guideX = -1;
            guideY = -1;
            return true;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            handleCloseRequest();
            return true;
        }

        if (selected != null) {
            int step = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
            int moveX = 0, moveY = 0;
            if (event.key() == GLFW.GLFW_KEY_LEFT) moveX = -step;
            else if (event.key() == GLFW.GLFW_KEY_RIGHT) moveX = step;
            else if (event.key() == GLFW.GLFW_KEY_UP) moveY = -step;
            else if (event.key() == GLFW.GLFW_KEY_DOWN) moveY = step;
            else return super.keyPressed(event);

            int moduleWidth = getModuleWidth(selected);
            int moduleHeight = getModuleHeight(selected);
            int nextX = Math.max(0, Math.min(width - moduleWidth, selected.getX() + moveX));
            int nextY = Math.max(0, Math.min(height - moduleHeight, selected.getY() + moveY));
            selected.setPosition(nextX, nextY);
            hasUnsavedChanges = true;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        // Dynamic visibility: Hide ALL overlays & buttons during drag/resize for complete clarity
        boolean isInteracting = dragging || resizing;

        if (btnVanillaToggle != null) btnVanillaToggle.visible = !isInteracting;
        if (btnSave != null) btnSave.visible = !isInteracting;
        if (btnResetAll != null) btnResetAll.visible = !isInteracting;
        if (btnCancel != null) btnCancel.visible = !isInteracting;

        // ── 1. Render Snapping Guide Lines ──
        if (guideX >= 0) g.fill(guideX, 0, guideX + 1, height, 0xAA00D2FF);
        if (guideY >= 0) g.fill(0, guideY, width, guideY + 1, 0xAA00D2FF);

        // ── 2. Render Vanilla HUD (Live or Template) ──
        if (ConfigManager.customVanillaHud) {
            renderVanillaElements(g);
        }

        // ── 3. Render Mod Modules ──
        for (HudModule h : ModuleManager.getInstance().getHudModules()) {
            if (h.isEnabled()) HudRenderer.draw(g, h, true);
        }

        // ── 4. Highlight Active Selection with Snug 1px Outline & Corner Resize Handle ──
        if (selected != null) {
            int sx = selected.getX();
            int sy = selected.getY();
            int sw = getModuleWidth(selected);
            int sh = getModuleHeight(selected);

            // Clean, exact 1px emerald border fitting tightly around module
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
        } else if (ConfigManager.customVanillaHud && selectedVanilla != VanillaType.NONE) {
            int vx = getVanillaX(selectedVanilla);
            int vy = getVanillaY(selectedVanilla);
            int vw = getVanillaWidth(selectedVanilla);
            int vh = getVanillaHeight(selectedVanilla);

            g.outline(vx, vy, vw, vh, 0xFF00D2FF);

            int hx = vx + vw - 8;
            int hy = vy + vh - 8;
            EzUi.roundedRect(g, hx, hy, 8, 8, 2, 0xFF00D2FF);
            g.text(font, "↘", hx + 1, hy - 1, 0xFFFFFFFF);

            if (resizing) {
                double scale = (selectedVanilla == VanillaType.BOSSBAR) ? ConfigManager.bossbarScale :
                        (selectedVanilla == VanillaType.SCOREBOARD) ? ConfigManager.scoreboardScale : ConfigManager.effectsScale;
                String scaleStr = String.format(java.util.Locale.ROOT, "%.1fx", scale);
                EzUi.roundedRect(g, hx + 10, hy - 4, 30, 14, 3, 0xF0121722);
                g.centeredText(font, Component.literal(scaleStr), hx + 25, hy - 1, 0xFF00D2FF);
            }
        }

        // Top Status Bar (Hidden during drag)
        if (!isInteracting && hasUnsavedChanges) {
            EzUi.roundedRect(g, width / 2 - 80, 8, 160, 18, 4, 0xD0121722);
            g.centeredText(font, Component.literal("● " + app.ezclient.util.EzI18n.get("ezclient.hud_editor.modal.unsaved_title")), width / 2, 13, 0xFFEAB308);
        }

        // ── 5. Render Right-Click Context Menu Popup (100% White text) ──
        if (showContextMenu && !isInteracting) {
            int cmW = 142;
            int cmH = (contextModule != null) ? 46 : 26;

            EzUi.roundedRect(g, contextMenuX - 2, contextMenuY - 2, cmW + 4, cmH + 4, 6, 0xFF202736);
            EzUi.roundedRect(g, contextMenuX, contextMenuY, cmW, cmH, 5, 0xF5131A26);

            if (contextModule != null) {
                boolean hovSettings = mx >= contextMenuX && mx <= contextMenuX + cmW && my >= contextMenuY && my < contextMenuY + 23;
                boolean hovReset = mx >= contextMenuX && mx <= contextMenuX + cmW && my >= contextMenuY + 23 && my <= contextMenuY + cmH;

                if (hovSettings) EzUi.roundedRect(g, contextMenuX + 2, contextMenuY + 2, cmW - 4, 19, 4, 0xFF22C96E);
                g.text(font, app.ezclient.util.EzI18n.get("ezclient.hud_editor.context.edit"), contextMenuX + 8, contextMenuY + 7, 0xFFFFFFFF);

                if (hovReset) EzUi.roundedRect(g, contextMenuX + 2, contextMenuY + 24, cmW - 4, 19, 4, 0xFF334155);
                g.text(font, "↺ " + app.ezclient.util.EzI18n.get("ezclient.hud_editor.context.reset_pos"), contextMenuX + 8, contextMenuY + 29, 0xFFFFFFFF);
            } else if (contextVanilla != VanillaType.NONE) {
                boolean hovReset = mx >= contextMenuX && mx <= contextMenuX + cmW && my >= contextMenuY && my <= contextMenuY + cmH;
                if (hovReset) EzUi.roundedRect(g, contextMenuX + 2, contextMenuY + 2, cmW - 4, 22, 4, 0xFF334155);
                g.text(font, "↺ " + app.ezclient.util.EzI18n.get("ezclient.hud_editor.context.reset_pos"), contextMenuX + 8, contextMenuY + 8, 0xFFFFFFFF);
            }
        }

        super.extractRenderState(g, mx, my, d);

        // ── 6. Render Modals ──
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

    private void renderVanillaElements(GuiGraphicsExtractor g) {
        // 1. Bossbar
        int bx = ConfigManager.bossbarX, by = ConfigManager.bossbarY;
        int bw = getVanillaWidth(VanillaType.BOSSBAR), bh = getVanillaHeight(VanillaType.BOSSBAR);
        EzUi.roundedRect(g, bx, by, bw, bh, 3, selectedVanilla == VanillaType.BOSSBAR ? 0x6000D2FF : 0x40000000);
        g.fill(bx + 2, by + 10, bx + bw - 2, by + 15, 0xFF351240);
        g.fill(bx + 3, by + 11, bx + (int) (bw * 0.78), by + 14, 0xFFD830E8);
        g.centeredText(font, Component.literal("§d§lEnder Dragon"), bx + bw / 2, by + 1, 0xFFFFFFFF);
        if (selectedVanilla == VanillaType.BOSSBAR) {
            g.outline(bx, by, bw, bh, 0xFF00D2FF);
        }

        // 2. Scoreboard
        int sx = ConfigManager.scoreboardX, sy = ConfigManager.scoreboardY;
        int sw = getVanillaWidth(VanillaType.SCOREBOARD), sh = getVanillaHeight(VanillaType.SCOREBOARD);
        boolean hasLiveScoreboard = false;
        if (minecraft.level != null) {
            Scoreboard sb = minecraft.level.getScoreboard();
            Objective sidebar = sb.getDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR);
            if (sidebar != null) {
                hasLiveScoreboard = true;
                EzUi.roundedRect(g, sx, sy, sw, sh, 4, selectedVanilla == VanillaType.SCOREBOARD ? 0x8000D2FF : 0x60000000);
                if (selectedVanilla == VanillaType.SCOREBOARD) g.outline(sx, sy, sw, sh, 0xFF00D2FF);
                g.centeredText(font, sidebar.getDisplayName(), sx + sw / 2, sy + 5, 0xFFFFFFFF);
                g.text(font, "§7[Live Scoreboard]", sx + 6, sy + 18, 0xFF43DD8C);
            }
        }
        if (!hasLiveScoreboard) {
            EzUi.roundedRect(g, sx, sy, sw, sh, 4, selectedVanilla == VanillaType.SCOREBOARD ? 0x8000D2FF : 0xA00D111A);
            if (selectedVanilla == VanillaType.SCOREBOARD) g.outline(sx, sy, sw, sh, 0xFF00D2FF);
            g.centeredText(font, Component.literal("§e§lEzClient §7(v1.8.0)"), sx + sw / 2, sy + 5, 0xFFFFFFFF);
            g.text(font, "§7-----------------", sx + 6, sy + 16, 0xFF888888);
            g.text(font, "§fKills: §a12", sx + 6, sy + 27, 0xFFFFFFFF);
            g.text(font, "§fDeaths: §c2", sx + 6, sy + 38, 0xFFFFFFFF);
            g.text(font, "§fPing: §a24ms", sx + 6, sy + 49, 0xFFFFFFFF);
            g.text(font, "§fFPS: §a240", sx + 6, sy + 60, 0xFFFFFFFF);
            g.text(font, "§eezclient.app", sx + 6, sy + 74, 0xFFFFAA00);
        }

        // 3. Status Effects (Top Right - 3 Potion Slots)
        int ex = ConfigManager.effectsX, ey = ConfigManager.effectsY;
        int ew = getVanillaWidth(VanillaType.EFFECTS), eh = getVanillaHeight(VanillaType.EFFECTS);
        if (selectedVanilla == VanillaType.EFFECTS) {
            g.outline(ex, ey, ew, eh, 0xFF00D2FF);
        }
        String[] effectIcons = {"STR", "SPD", "HP"};
        String[] effectTimes = {"8:42", "3:15", "0:45"};
        int[] effectColors = {0xFFEF4444, 0xFF00D2FF, 0xFFFF69B4};
        for (int i = 0; i < 3; i++) {
            int ix = ex + i * 22;
            EzUi.roundedRect(g, ix, ey, 20, 20, 3, 0x90111419);
            g.outline(ix, ey, 20, 20, 0x40FFFFFF);
            g.centeredText(font, Component.literal(effectIcons[i]), ix + 10, ey + 2, effectColors[i]);
            g.centeredText(font, Component.literal(effectTimes[i]), ix + 10, ey + 11, 0xFFE2E8F0);
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
