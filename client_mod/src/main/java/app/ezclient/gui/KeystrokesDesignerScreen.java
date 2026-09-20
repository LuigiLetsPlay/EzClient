package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern, beginner-friendly visual editor for Keystrokes V2.
 * Features magnetic snapping with visual guide lines, 4 organized inspector tabs,
 * a visual binding picker modal, and a clean spacious layout.
 */
public final class KeystrokesDesignerScreen extends Screen {

    public enum Tab {
        ELEMENT("Taste"),
        COLORS("Farben"),
        PRESETS("Vorlagen"),
        DESIGN("Design");

        private final String label;
        Tab(String label) { this.label = label; }
        public String label() { return label; }
    }

    private final Screen parent;
    private final KeystrokesModule module;
    private KeystrokesModule.KeyElement selected;

    // Geometry
    private int panelX, panelY, panelW, panelH;
    private int canvasX, canvasY, canvasW, canvasH;
    private int inspectorX, inspectorW;
    private float previewScale = 1.0f;
    private int previewX, previewY;

    // Interaction states
    private Tab activeTab = Tab.ELEMENT;
    private boolean dragging, resizing;
    private boolean snappingEnabled = true;
    private boolean bindingPickerOpen = false;
    private boolean listeningForKey = false;

    private double dragOffsetX, dragOffsetY;
    private double resizeStartMouseX, resizeStartMouseY;
    private int resizeStartW, resizeStartH;

    // Snapping state
    private boolean hasSnapX = false;
    private boolean hasSnapY = false;
    private int snapGuideX = 0;
    private int snapGuideY = 0;

    // Modal geometry
    private int modalX, modalY, modalW, modalH;

    // Widgets
    private EditBox labelInput;

    // Inspector scrolling
    private double inspectorScroll = 0.0;
    private int contentBottomY = 0;
    private boolean draggingScrollbar = false;
    private final List<AbstractWidget> inspectorWidgets = new ArrayList<>();

    private int scrollLeft() { return inspectorX + 6; }
    private int scrollTop() { return panelY + 66; }
    private int scrollRight() { return panelX + panelW - 4; }
    private int scrollBottom() { return panelY + panelH - 8; }

    private int maxInspectorScroll() {
        return Math.max(0, contentBottomY + 12 - scrollBottom());
    }

    private void clampInspectorScroll() {
        inspectorScroll = Math.max(0, Math.min(maxInspectorScroll(), inspectorScroll));
    }

    private boolean insideInspectorViewport(double x, double y) {
        return x >= scrollLeft() && x < scrollRight() && y >= scrollTop() && y < scrollBottom();
    }

    private MouseButtonEvent translated(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x(), event.y() + inspectorScroll, event.buttonInfo());
    }

    private void scrollTo(double y) {
        int top = scrollTop();
        int bottom = scrollBottom();
        double fraction = Math.max(0, Math.min(1, (y - top) / Math.max(1, bottom - top)));
        inspectorScroll = fraction * maxInspectorScroll();
        clampInspectorScroll();
    }

    private <T extends AbstractWidget> T addInspectorWidget(T widget) {
        inspectorWidgets.add(widget);
        return addRenderableWidget(widget);
    }

    public KeystrokesDesignerScreen(Screen parent, KeystrokesModule module) {
        super(Component.literal("Keystrokes V2 Designer"));
        this.parent = parent;
        this.module = module;
        this.selected = module.elements().isEmpty() ? null : module.elements().getFirst();
    }

    @Override
    protected void init() {
        inspectorWidgets.clear();

        // Main panel sizing (responsive and well padded)
        panelW = Math.min(780, width - 20);
        panelH = Math.min(420, height - 16);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        // Two columns: Left = Canvas + Toolbar, Right = Inspector
        inspectorW = Math.max(216, Math.min(256, panelW * 36 / 100));
        inspectorX = panelX + panelW - inspectorW;

        canvasX = panelX + 12;
        canvasY = panelY + 40;
        canvasW = inspectorX - canvasX - 12;
        int toolRowH = 18;
        int toolRowGap = 3;
        int toolH = toolRowH * 2 + toolRowGap + 6;
        canvasH = panelH - 52 - toolH;

        updatePreviewGeometry();

        // Top right close button
        addRenderableWidget(new EzButton(panelX + panelW - 27, panelY + 8, 18, 16, Component.literal("✕"), false, b -> onClose()));

        // If Binding Picker modal is open, build ONLY modal widgets
        if (bindingPickerOpen) {
            initBindingPickerWidgets();
            return;
        }

        // --- Canvas Toolbar (Left column bottom: 2 rows of 3 buttons fitted cleanly within canvasW) ---
        int toolY1 = canvasY + canvasH + 5;
        int toolY2 = toolY1 + toolRowH + toolRowGap;
        int btnGap = 3;
        int btnW = Math.max(30, (canvasW - 2 * btnGap) / 3);
        int remW = canvasW - (btnW * 2 + btnGap * 2);

        // Row 1: Element Actions [+ Taste], [Duplizieren], [Löschen]
        addRenderableWidget(new EzButton(canvasX, toolY1, btnW, toolRowH, Component.literal("+ Taste"), true, b -> {
            selected = module.addElement();
            activeTab = Tab.ELEMENT;
            inspectorScroll = 0.0;
            rebuildWidgets();
        }));

        addRenderableWidget(new EzButton(canvasX + btnW + btnGap, toolY1, btnW, toolRowH, Component.literal("Duplizieren"), false, b -> {
            if (selected != null) {
                selected = module.duplicateElement(selected.id());
                rebuildWidgets();
            }
        }));

        addRenderableWidget(new EzButton(canvasX + (btnW + btnGap) * 2, toolY1, remW, toolRowH, Component.literal(app.ezclient.util.EzI18n.text("Löschen")), false, b -> {
            if (selected != null) {
                module.removeElement(selected.id());
                selected = module.elements().isEmpty() ? null : module.elements().getFirst();
                rebuildWidgets();
            }
        }));

        // Row 2: Layout / Canvas Actions [Zentrieren], [🧲 Snap: AN/AUS], [HUD-Stil]
        addRenderableWidget(new EzButton(canvasX, toolY2, btnW, toolRowH, Component.literal("Zentrieren"), false, b -> {
            module.normalizePositions();
            rebuildWidgets();
        }));

        addRenderableWidget(new EzButton(canvasX + btnW + btnGap, toolY2, btnW, toolRowH,
                Component.literal(snappingEnabled ? "🧲 Snap: AN" : "🧲 Snap: AUS"), snappingEnabled, b -> {
            snappingEnabled = !snappingEnabled;
            rebuildWidgets();
        }));

        addRenderableWidget(new EzButton(canvasX + (btnW + btnGap) * 2, toolY2, remW, toolRowH, Component.literal("HUD-Stil"), false,
                b -> EzScreenBridge.set(minecraft, new KeystrokesSettingsScreen(this, module))));

        // --- Inspector Tab Bar (Right column top) ---
        int tabX = inspectorX + 8;
        int tabAvailableW = inspectorW - 16;
        Tab[] tabs = Tab.values();
        int tabGap = 3;
        int tabW = (tabAvailableW - (tabs.length - 1) * tabGap) / tabs.length;
        int tabY = panelY + 40;

        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            addRenderableWidget(new EzButton(tabX + i * (tabW + tabGap), tabY, tabW, 18,
                    Component.literal(t.label()), activeTab == t, b -> {
                activeTab = t;
                inspectorScroll = 0.0;
                rebuildWidgets();
            }));
        }

        // --- Inspector Content Area ---
        int x = inspectorX + 8;
        int w = inspectorW - 16 - 7;
        int y = panelY + 66;
        int half = (w - 4) / 2;
        int qtr = (half - 3) / 2;

        switch (activeTab) {
            case ELEMENT -> {
                if (selected != null) {
                    labelInput = new EditBox(font, x, y, w, 18, Component.literal(app.ezclient.util.EzI18n.text("Beschriftung")));
                    labelInput.setMaxLength(32);
                    labelInput.setValue(selected.label());
                    labelInput.setResponder(val -> module.setElementLabel(selected, val));
                    addInspectorWidget(labelInput);
                    y += 24;

                    addInspectorWidget(new EzButton(x, y, w, 18,
                            Component.literal("Bindung: " + selected.binding().title() + " ▾"), true, b -> {
                        bindingPickerOpen = true;
                        rebuildWidgets();
                    }));
                    y += 22;

                    if (selected.binding() == KeystrokesModule.Binding.CUSTOM) {
                        addInspectorWidget(new EzButton(x, y, w, 18,
                                Component.literal(listeningForKey ? app.ezclient.util.EzI18n.text("Taste drücken …") : app.ezclient.util.EzI18n.text("Taste: ") + keyName(selected.keyCode())),
                                listeningForKey, b -> {
                            listeningForKey = true;
                            rebuildWidgets();
                        }));
                        y += 22;
                    }

                    // Position Stepper
                    addInspectorWidget(new EzButton(x, y, qtr, 18, Component.literal("X−"), false,
                            b -> { module.moveElement(selected, selected.x() - 1, selected.y()); module.saveLayout(); rebuildWidgets(); }));
                    addInspectorWidget(new EzButton(x + qtr + 3, y, qtr, 18, Component.literal("X+"), false,
                            b -> { module.moveElement(selected, selected.x() + 1, selected.y()); module.saveLayout(); rebuildWidgets(); }));
                    addInspectorWidget(new EzButton(x + half + 4, y, qtr, 18, Component.literal("Y−"), false,
                            b -> { module.moveElement(selected, selected.x(), selected.y() - 1); module.saveLayout(); rebuildWidgets(); }));
                    addInspectorWidget(new EzButton(x + half + 4 + qtr + 3, y, qtr, 18, Component.literal("Y+"), false,
                            b -> { module.moveElement(selected, selected.x(), selected.y() + 1); module.saveLayout(); rebuildWidgets(); }));
                    y += 22;

                    // Size Stepper
                    addInspectorWidget(new EzButton(x, y, qtr, 18, Component.literal("B−"), false, b -> resizeSelected(-2, 0)));
                    addInspectorWidget(new EzButton(x + qtr + 3, y, qtr, 18, Component.literal("B+"), false, b -> resizeSelected(2, 0)));
                    addInspectorWidget(new EzButton(x + half + 4, y, qtr, 18, Component.literal("H−"), false, b -> resizeSelected(0, -2)));
                    addInspectorWidget(new EzButton(x + half + 4 + qtr + 3, y, qtr, 18, Component.literal("H+"), false, b -> resizeSelected(0, 2)));
                    y += 24;

                    addInspectorWidget(new EzToggleSwitch(x, y, w, 18, Component.literal("CPS auf Taste"), selected.showCps(),
                            val -> { module.setElementShowCps(selected, val); rebuildWidgets(); }));
                    y += 22;

                    addInspectorWidget(new EzToggleSwitch(x, y, w, 18, Component.literal("Eigene Farben"), selected.customColors(),
                            val -> { module.setElementCustomColors(selected, val); rebuildWidgets(); }));
                    y += 22;
                } else {
                    addInspectorWidget(new EzButton(x, y + 20, w, 20, Component.literal("+ Taste hinzufügen"), true, b -> {
                        selected = module.addElement();
                        rebuildWidgets();
                    }));
                    addInspectorWidget(new EzButton(x, y + 46, w, 20, Component.literal("Vorlagen ansehen …"), false, b -> {
                        activeTab = Tab.PRESETS;
                        inspectorScroll = 0.0;
                        rebuildWidgets();
                    }));
                    y += 72;
                }
            }
            case COLORS -> {
                if (selected != null) {
                    addInspectorWidget(new EzToggleSwitch(x, y, w, 18, Component.literal("Eigene Tastenfarben"), selected.customColors(),
                            val -> { module.setElementCustomColors(selected, val); rebuildWidgets(); }));
                    y += 22;

                    if (selected.customColors()) {
                        addInspectorWidget(new EzButton(x, y, half, 18, Component.literal("Box …"), false,
                                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Tasten-Hintergrund", selected.boxColor(), val -> module.setElementBoxColor(selected, val)))));
                        addInspectorWidget(new EzButton(x + half + 4, y, half, 18, Component.literal("Gedrückt …"), false,
                                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Gedrückte Taste", selected.pressedBoxColor(), val -> module.setElementPressedBoxColor(selected, val)))));
                        y += 22;

                        addInspectorWidget(new EzButton(x, y, half, 18, Component.literal("Text …"), false,
                                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Tastentext", selected.textColor(), val -> module.setElementTextColor(selected, val)))));
                        addInspectorWidget(new EzButton(x + half + 4, y, half, 18, Component.literal("Drucktext …"), false,
                                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Gedrückter Text", selected.pressedTextColor(), val -> module.setElementPressedTextColor(selected, val)))));
                        y += 26;
                    }
                }

                // Global Color Settings
                addInspectorWidget(new EzButton(x, y, w, 18, Component.literal("Farbmodus: " + module.getKeyColorMode().name()), false, b -> {
                    KeystrokesModule.KeyColorMode[] modes = KeystrokesModule.KeyColorMode.values();
                    module.setKeyColorMode(modes[(module.getKeyColorMode().ordinal() + 1) % modes.length]);
                    rebuildWidgets();
                }));
                y += 22;

                if (module.getKeyColorMode() == KeystrokesModule.KeyColorMode.GRADIENT) {
                    addInspectorWidget(new EzButton(x, y, w, 18, Component.literal("Gradient / Wave Farbe 2 …"), false,
                            b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Gradient / Wave Farbe 2", module.getGradientColor(), module::setGradientColor))));
                    y += 22;
                }

                addInspectorWidget(new EzButton(x, y, half, 18, Component.literal("Glob. Box …"), false,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Globaler Hintergrund", module.getNormalBoxColor(), module::setNormalBoxColor))));
                addInspectorWidget(new EzButton(x + half + 4, y, half, 18, Component.literal("Glob. Gedr. …"), false,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Global Gedrückt", module.getPressedBoxColor(), module::setPressedBoxColor))));
                y += 22;

                addInspectorWidget(new EzButton(x, y, half, 18, Component.literal("Glob. Text …"), false,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Globaler Text", module.getKeyTextColor(), module::setKeyTextColor))));
                addInspectorWidget(new EzButton(x + half + 4, y, half, 18, Component.literal("Glob. D-Text …"), false,
                        b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Global Gedrückter Text", module.getPressedTextColor(), module::setPressedTextColor))));
                y += 22;
            }
            case PRESETS -> {
                KeystrokesModule.LayoutPreset[] presets = KeystrokesModule.LayoutPreset.values();
                for (KeystrokesModule.LayoutPreset preset : presets) {
                    boolean isCur = module.getLayoutPreset() == preset;
                    addInspectorWidget(new EzButton(x, y, w, 18, Component.literal(presetTitle(preset)), isCur, b -> {
                        module.setLayoutPreset(preset);
                        selected = module.elements().isEmpty() ? null : module.elements().getFirst();
                        rebuildWidgets();
                    }));
                    y += 21;
                }
                y += 4;
                addInspectorWidget(new EzButton(x, y, w, 18, Component.literal("Layout zentrieren"), false, b -> {
                    module.normalizePositions();
                    rebuildWidgets();
                }));
                y += 22;
            }
            case DESIGN -> {
                // Gap
                addInspectorWidget(new EzButton(x, y, half, 18, Component.literal("Abstand: " + module.getKeySpacing() + "px"), false, null));
                addInspectorWidget(new EzButton(x + half + 4, y, (half - 2) / 2, 18, Component.literal("−"), false,
                        b -> { module.setKeySpacing(module.getKeySpacing() - 1); rebuildWidgets(); }));
                addInspectorWidget(new EzButton(x + half + 4 + (half - 2) / 2 + 2, y, (half - 2) / 2, 18, Component.literal("+"), false,
                        b -> { module.setKeySpacing(module.getKeySpacing() + 1); rebuildWidgets(); }));
                y += 22;

                // Corner radius
                addInspectorWidget(new EzButton(x, y, half, 18, Component.literal("Ecken: " + module.getKeyCornerRadius() + "px"), false, null));
                addInspectorWidget(new EzButton(x + half + 4, y, (half - 2) / 2, 18, Component.literal("−"), false,
                        b -> { module.setKeyCornerRadius(module.getKeyCornerRadius() - 1); rebuildWidgets(); }));
                addInspectorWidget(new EzButton(x + half + 4 + (half - 2) / 2 + 2, y, (half - 2) / 2, 18, Component.literal("+"), false,
                        b -> { module.setKeyCornerRadius(module.getKeyCornerRadius() + 1); rebuildWidgets(); }));
                y += 22;

                // Font scale
                addInspectorWidget(new EzButton(x, y, half, 18,
                        Component.literal(String.format(java.util.Locale.ROOT, "Schrift: %.1fx", module.getFontScale())), false, null));
                addInspectorWidget(new EzButton(x + half + 4, y, (half - 2) / 2, 18, Component.literal("−"), false,
                        b -> { module.setFontScale(module.getFontScale() - .1f); rebuildWidgets(); }));
                addInspectorWidget(new EzButton(x + half + 4 + (half - 2) / 2 + 2, y, (half - 2) / 2, 18, Component.literal("+"), false,
                        b -> { module.setFontScale(module.getFontScale() + .1f); rebuildWidgets(); }));
                y += 22;

                // Key border
                addInspectorWidget(new EzToggleSwitch(x, y, w, 18, Component.literal("Tasten-Border"), module.isShowKeyBorder(),
                        val -> { module.setShowKeyBorder(val); rebuildWidgets(); }));
                y += 22;

                addInspectorWidget(new EzButton(x, y, w, 18,
                        Component.literal("Border: " + (module.getFrameMode() == KeystrokesModule.FrameMode.CONNECTED
                                ? "Verbunden" : "Einzeln")), false, b -> {
                    module.setFrameMode(module.getFrameMode() == KeystrokesModule.FrameMode.CONNECTED
                            ? KeystrokesModule.FrameMode.INDIVIDUAL
                            : KeystrokesModule.FrameMode.CONNECTED);
                    rebuildWidgets();
                }));
                y += 22;

                // Press animation
                addInspectorWidget(new EzButton(x, y, w, 18, Component.literal("Animation: " + module.getPressAnimation().name()), false, b -> {
                    KeystrokesModule.PressAnimation[] modes = KeystrokesModule.PressAnimation.values();
                    module.setPressAnimation(modes[(module.getPressAnimation().ordinal() + 1) % modes.length]);
                    rebuildWidgets();
                }));
                y += 22;

                // Fade time
                addInspectorWidget(new EzButton(x, y, half, 18, Component.literal("Fade: " + module.getFadeTimeMs() + "ms"), false, null));
                addInspectorWidget(new EzButton(x + half + 4, y, (half - 2) / 2, 18, Component.literal("−"), false,
                        b -> { module.setFadeTimeMs(module.getFadeTimeMs() - 25); rebuildWidgets(); }));
                addInspectorWidget(new EzButton(x + half + 4 + (half - 2) / 2 + 2, y, (half - 2) / 2, 18, Component.literal("+"), false,
                        b -> { module.setFadeTimeMs(module.getFadeTimeMs() + 25); rebuildWidgets(); }));
                y += 22;

                // Space style
                addInspectorWidget(new EzButton(x, y, w, 18, Component.literal("Space-Stil: " + module.getSpaceStyle().name()), false, b -> {
                    KeystrokesModule.SpaceStyle[] modes = KeystrokesModule.SpaceStyle.values();
                    module.setSpaceStyle(modes[(module.getSpaceStyle().ordinal() + 1) % modes.length]);
                    rebuildWidgets();
                }));
                y += 22;
            }
        }
        contentBottomY = y;
        clampInspectorScroll();
    }

    private void initBindingPickerWidgets() {
        modalW = 320;
        modalH = 260;
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;

        int mInnerX = modalX + 14;
        int mInnerW = modalW - 28;
        int mY = modalY + 44;

        // Row 1: Movement (W, A, S, D)
        int btnW4 = (mInnerW - 3 * 4) / 4;
        addRenderableWidget(new EzButton(mInnerX, mY, btnW4, 20, Component.literal("W"),
                selected != null && selected.binding() == KeystrokesModule.Binding.FORWARD,
                b -> applyBinding(KeystrokesModule.Binding.FORWARD)));
        addRenderableWidget(new EzButton(mInnerX + btnW4 + 4, mY, btnW4, 20, Component.literal("A"),
                selected != null && selected.binding() == KeystrokesModule.Binding.LEFT,
                b -> applyBinding(KeystrokesModule.Binding.LEFT)));
        addRenderableWidget(new EzButton(mInnerX + (btnW4 + 4) * 2, mY, btnW4, 20, Component.literal("S"),
                selected != null && selected.binding() == KeystrokesModule.Binding.BACK,
                b -> applyBinding(KeystrokesModule.Binding.BACK)));
        addRenderableWidget(new EzButton(mInnerX + (btnW4 + 4) * 3, mY, btnW4, 20, Component.literal("D"),
                selected != null && selected.binding() == KeystrokesModule.Binding.RIGHT,
                b -> applyBinding(KeystrokesModule.Binding.RIGHT)));
        mY += 24;

        // Row 2: Space, Sneak, Sprint
        int btnW3 = (mInnerW - 2 * 4) / 3;
        addRenderableWidget(new EzButton(mInnerX, mY, btnW3, 20, Component.literal("SPACE"),
                selected != null && selected.binding() == KeystrokesModule.Binding.JUMP,
                b -> applyBinding(KeystrokesModule.Binding.JUMP)));
        addRenderableWidget(new EzButton(mInnerX + btnW3 + 4, mY, btnW3, 20, Component.literal("SNEAK"),
                selected != null && selected.binding() == KeystrokesModule.Binding.SNEAK,
                b -> applyBinding(KeystrokesModule.Binding.SNEAK)));
        addRenderableWidget(new EzButton(mInnerX + (btnW3 + 4) * 2, mY, btnW3, 20, Component.literal("SPRINT"),
                selected != null && selected.binding() == KeystrokesModule.Binding.SPRINT,
                b -> applyBinding(KeystrokesModule.Binding.SPRINT)));
        mY += 28;

        // Row 3: Mouse buttons
        int btnW2 = (mInnerW - 4) / 2;
        addRenderableWidget(new EzButton(mInnerX, mY, btnW2, 20, Component.literal("LMB (Linksklick)"),
                selected != null && selected.binding() == KeystrokesModule.Binding.ATTACK,
                b -> applyBinding(KeystrokesModule.Binding.ATTACK)));
        addRenderableWidget(new EzButton(mInnerX + btnW2 + 4, mY, btnW2, 20, Component.literal("RMB (Rechtsklick)"),
                selected != null && selected.binding() == KeystrokesModule.Binding.USE,
                b -> applyBinding(KeystrokesModule.Binding.USE)));
        mY += 28;

        // Row 4: Custom Key
        addRenderableWidget(new EzButton(mInnerX, mY, mInnerW, 20, Component.literal("⌨ Eigene Tastaturtaste drücken …"), false, b -> {
            bindingPickerOpen = false;
            listeningForKey = true;
            rebuildWidgets();
        }));
        mY += 28;

        // Row 5: Cancel
        addRenderableWidget(new EzButton(mInnerX, mY, mInnerW, 20, Component.literal(app.ezclient.util.EzI18n.text("Abbrechen")), false, b -> {
            bindingPickerOpen = false;
            rebuildWidgets();
        }));
    }

    private void applyBinding(KeystrokesModule.Binding binding) {
        if (selected != null) {
            module.setElementBinding(selected, binding, -1);
            module.setElementLabel(selected, binding.defaultLabel());
        }
        bindingPickerOpen = false;
        rebuildWidgets();
    }

    private void resizeSelected(int dw, int dh) {
        if (selected == null) return;
        module.resizeElement(selected, selected.width() + dw, selected.height() + dh);
        module.saveLayout();
        rebuildWidgets();
    }

    private static String presetTitle(KeystrokesModule.LayoutPreset preset) {
        return switch (preset) {
            case WASD -> "WASD (Kompakt)";
            case WASD_MOUSE -> "WASD + Maus";
            case WASD_MOUSE_SPACE -> "WASD + Maus + Space";
            case WASD_MOUSE_SPACE_CPS -> "PvP + CPS (Standard)";
            case FULL -> "Erweitert (+ Sneak / Sprint)";
            case KEYBOARD -> "Tastatur (Kompakt)";
            case FULL_KEYBOARD -> "Volle Tastatur (Komplett)";
        };
    }

    private static String keyName(int key) {
        if (key < 0) return "Nicht gesetzt";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null && !name.isBlank()) return name.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "ALT GR";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            default -> "KEY " + key;
        };
    }

    private void updatePreviewGeometry() {
        int contentW = Math.max(1, module.getWidth(minecraft));
        int contentH = Math.max(1, module.getHeight(minecraft));
        previewScale = Math.min(2.5f, Math.min((canvasW - 32f) / contentW, (canvasH - 32f) / contentH));
        previewScale = Math.max(0.2f, previewScale);

        int renderedW = Math.round(contentW * previewScale);
        int renderedH = Math.round(contentH * previewScale);
        int boxX = canvasX + Math.max(12, (canvasW - renderedW) / 2);
        int boxY = canvasY + Math.max(12, (canvasH - renderedH) / 2);
        // previewX/Y remain the origin of element coordinates; the module's
        // tight outer bounds may begin before them because of a connected frame.
        previewX = boxX + Math.round(module.getContentOffsetX() * previewScale);
        previewY = boxY + Math.round(module.getContentOffsetY() * previewScale);
    }

    private boolean insideCanvas(double x, double y) {
        return x >= canvasX && x < canvasX + canvasW && y >= canvasY && y < canvasY + canvasH;
    }

    private KeystrokesModule.KeyElement hit(double mouseX, double mouseY) {
        double x = (mouseX - previewX) / previewScale;
        double y = (mouseY - previewY) / previewScale;
        for (int i = module.elements().size() - 1; i >= 0; i--) {
            KeystrokesModule.KeyElement e = module.elements().get(i);
            if (x >= e.x() && x < e.x() + e.width() && y >= e.y() && y < e.y() + e.height()) {
                return e;
            }
        }
        return null;
    }

    private boolean resizeHandle(double mouseX, double mouseY) {
        if (selected == null) return false;
        double right = previewX + (selected.x() + selected.width()) * previewScale;
        double bottom = previewY + (selected.y() + selected.height()) * previewScale;
        return mouseX >= right - 8 && mouseX <= right + 4 && mouseY >= bottom - 8 && mouseY <= bottom + 4;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listeningForKey) return true;

        if (bindingPickerOpen) {
            // Clicking outside modal closes it
            if (event.button() == 0 && (event.x() < modalX || event.x() > modalX + modalW
                    || event.y() < modalY || event.y() > modalY + modalH)) {
                bindingPickerOpen = false;
                rebuildWidgets();
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }

        // Check scrollbar click
        if (event.button() == 0 && maxInspectorScroll() > 0 && event.x() >= panelX + panelW - 12 && event.x() <= panelX + panelW - 3
                && event.y() >= scrollTop() && event.y() <= scrollBottom()) {
            draggingScrollbar = true;
            scrollTo(event.y());
            return true;
        }

        if (event.button() == 0 && insideCanvas(event.x(), event.y())) {
            updatePreviewGeometry();
            if (resizeHandle(event.x(), event.y())) {
                resizing = true;
                resizeStartMouseX = event.x();
                resizeStartMouseY = event.y();
                resizeStartW = selected.width();
                resizeStartH = selected.height();
                return true;
            }
            KeystrokesModule.KeyElement hit = hit(event.x(), event.y());
            if (hit != null) {
                boolean changedSelection = selected == null || selected.id() != hit.id();
                selected = hit;
                dragOffsetX = (event.x() - previewX) / previewScale - selected.x();
                dragOffsetY = (event.y() - previewY) / previewScale - selected.y();
                dragging = true;
                if (changedSelection) rebuildWidgets();
                return true;
            } else {
                if (selected != null) {
                    selected = null;
                    rebuildWidgets();
                    return true;
                }
            }
        }

        boolean inside = insideInspectorViewport(event.x(), event.y());
        List<AbstractWidget> temporarilyHidden = new ArrayList<>();
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && widget.visible
                    && (inside ? !inspectorWidgets.contains(widget) : inspectorWidgets.contains(widget))) {
                widget.visible = false;
                temporarilyHidden.add(widget);
            }
        }
        try {
            return super.mouseClicked(inside ? translated(event) : event, doubleClick);
        } finally {
            for (AbstractWidget widget : temporarilyHidden) widget.visible = true;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (bindingPickerOpen || listeningForKey) return true;

        if (draggingScrollbar) {
            scrollTo(event.y());
            return true;
        }

        if (selected != null && event.button() == 0 && dragging) {
            int rawX = (int) Math.round((event.x() - previewX) / previewScale - dragOffsetX);
            int rawY = (int) Math.round((event.y() - previewY) / previewScale - dragOffsetY);
            rawX = Math.max(0, rawX);
            rawY = Math.max(0, rawY);

            int finalX = rawX;
            int finalY = rawY;
            boolean snappedX = false;
            boolean snappedY = false;
            int guideX = 0;
            int guideY = 0;

            if (snappingEnabled) {
                float threshold = 7.0f / previewScale;
                float bestDistX = threshold;
                float bestDistY = threshold;
                int gap = module.getKeySpacing();

                // Snap to origin
                if (Math.abs(rawX) < bestDistX) {
                    bestDistX = Math.abs(rawX); finalX = 0; snappedX = true; guideX = 0;
                }
                if (Math.abs(rawY) < bestDistY) {
                    bestDistY = Math.abs(rawY); finalY = 0; snappedY = true; guideY = 0;
                }

                // Snap to other elements
                for (KeystrokesModule.KeyElement other : module.elements()) {
                    if (other.id() == selected.id()) continue;

                    // Left to Left
                    float d = Math.abs(rawX - other.x());
                    if (d < bestDistX) { bestDistX = d; finalX = other.x(); snappedX = true; guideX = other.x(); }

                    // Left to Right (flush)
                    d = Math.abs(rawX - (other.x() + other.width()));
                    if (d < bestDistX) { bestDistX = d; finalX = other.x() + other.width(); snappedX = true; guideX = other.x() + other.width(); }

                    // Left to Right (with gap)
                    d = Math.abs(rawX - (other.x() + other.width() + gap));
                    if (d < bestDistX) { bestDistX = d; finalX = other.x() + other.width() + gap; snappedX = true; guideX = other.x() + other.width() + gap; }

                    // Right to Left (flush)
                    int cRightToLeft = other.x() - selected.width();
                    d = Math.abs(rawX - cRightToLeft);
                    if (d < bestDistX && cRightToLeft >= 0) { bestDistX = d; finalX = cRightToLeft; snappedX = true; guideX = other.x(); }

                    // Right to Left (with gap)
                    int cRightToLeftGap = other.x() - gap - selected.width();
                    d = Math.abs(rawX - cRightToLeftGap);
                    if (d < bestDistX && cRightToLeftGap >= 0) { bestDistX = d; finalX = cRightToLeftGap; snappedX = true; guideX = other.x() - gap; }

                    // Right to Right
                    int cRightToRight = other.x() + other.width() - selected.width();
                    d = Math.abs(rawX - cRightToRight);
                    if (d < bestDistX && cRightToRight >= 0) { bestDistX = d; finalX = cRightToRight; snappedX = true; guideX = other.x() + other.width(); }

                    // Center X
                    int cCenterX = other.x() + (other.width() - selected.width()) / 2;
                    d = Math.abs(rawX - cCenterX);
                    if (d < bestDistX && cCenterX >= 0) { bestDistX = d; finalX = cCenterX; snappedX = true; guideX = other.x() + other.width() / 2; }

                    // Top to Top
                    d = Math.abs(rawY - other.y());
                    if (d < bestDistY) { bestDistY = d; finalY = other.y(); snappedY = true; guideY = other.y(); }

                    // Top to Bottom (flush)
                    d = Math.abs(rawY - (other.y() + other.height()));
                    if (d < bestDistY) { bestDistY = d; finalY = other.y() + other.height(); snappedY = true; guideY = other.y() + other.height(); }

                    // Top to Bottom (with gap)
                    d = Math.abs(rawY - (other.y() + other.height() + gap));
                    if (d < bestDistY) { bestDistY = d; finalY = other.y() + other.height() + gap; snappedY = true; guideY = other.y() + other.height() + gap; }

                    // Bottom to Top (flush)
                    int cBottomToTop = other.y() - selected.height();
                    d = Math.abs(rawY - cBottomToTop);
                    if (d < bestDistY && cBottomToTop >= 0) { bestDistY = d; finalY = cBottomToTop; snappedY = true; guideY = other.y(); }

                    // Bottom to Top (with gap)
                    int cBottomToTopGap = other.y() - gap - selected.height();
                    d = Math.abs(rawY - cBottomToTopGap);
                    if (d < bestDistY && cBottomToTopGap >= 0) { bestDistY = d; finalY = cBottomToTopGap; snappedY = true; guideY = other.y() - gap; }

                    // Bottom to Bottom
                    int cBottomToBottom = other.y() + other.height() - selected.height();
                    d = Math.abs(rawY - cBottomToBottom);
                    if (d < bestDistY && cBottomToBottom >= 0) { bestDistY = d; finalY = cBottomToBottom; snappedY = true; guideY = other.y() + other.height(); }

                    // Center Y
                    int cCenterY = other.y() + (other.height() - selected.height()) / 2;
                    d = Math.abs(rawY - cCenterY);
                    if (d < bestDistY && cCenterY >= 0) { bestDistY = d; finalY = cCenterY; snappedY = true; guideY = other.y() + other.height() / 2; }
                }
            }

            hasSnapX = snappedX;
            hasSnapY = snappedY;
            snapGuideX = guideX;
            snapGuideY = guideY;

            module.moveElement(selected, finalX, finalY);
            return true;
        }

        if (selected != null && event.button() == 0 && resizing) {
            int rawW = resizeStartW + (int) Math.round((event.x() - resizeStartMouseX) / previewScale);
            int rawH = resizeStartH + (int) Math.round((event.y() - resizeStartMouseY) / previewScale);
            rawW = Math.max(8, rawW);
            rawH = Math.max(8, rawH);

            int finalW = rawW;
            int finalH = rawH;
            boolean snappedW = false;
            boolean snappedH = false;
            int guideW = selected.x() + rawW;
            int guideH = selected.y() + rawH;

            if (snappingEnabled) {
                float threshold = 7.0f / previewScale;
                float bestDistW = threshold;
                float bestDistH = threshold;

                // Match standard key size
                int defS = module.getKeySize();
                if (Math.abs(rawW - defS) < bestDistW) {
                    bestDistW = Math.abs(rawW - defS); finalW = defS; snappedW = true; guideW = selected.x() + defS;
                }
                if (Math.abs(rawH - defS) < bestDistH) {
                    bestDistH = Math.abs(rawH - defS); finalH = defS; snappedH = true; guideH = selected.y() + defS;
                }

                for (KeystrokesModule.KeyElement other : module.elements()) {
                    if (other.id() == selected.id()) continue;

                    // Match other key's width
                    float dw = Math.abs(rawW - other.width());
                    if (dw < bestDistW) { bestDistW = dw; finalW = other.width(); snappedW = true; guideW = selected.x() + other.width(); }

                    // Align right edge to other right edge
                    int alignW = other.x() + other.width() - selected.x();
                    if (alignW >= 8 && Math.abs(rawW - alignW) < bestDistW) {
                        bestDistW = Math.abs(rawW - alignW); finalW = alignW; snappedW = true; guideW = other.x() + other.width();
                    }

                    // Match other key's height
                    float dh = Math.abs(rawH - other.height());
                    if (dh < bestDistH) { bestDistH = dh; finalH = other.height(); snappedH = true; guideH = selected.y() + other.height(); }

                    // Align bottom edge to other bottom edge
                    int alignH = other.y() + other.height() - selected.y();
                    if (alignH >= 8 && Math.abs(rawH - alignH) < bestDistH) {
                        bestDistH = Math.abs(rawH - alignH); finalH = alignH; snappedH = true; guideH = other.y() + other.height();
                    }
                }
            }

            hasSnapX = snappedW;
            hasSnapY = snappedH;
            snapGuideX = guideW;
            snapGuideY = guideH;

            module.resizeElement(selected, finalW, finalH);
            return true;
        }

        boolean inside = insideInspectorViewport(event.x(), event.y());
        return super.mouseDragged(inside ? translated(event) : event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        if (dragging || resizing) {
            dragging = false;
            resizing = false;
            hasSnapX = false;
            hasSnapY = false;
            module.saveLayout();
            rebuildWidgets();
            return true;
        }
        boolean inside = insideInspectorViewport(event.x(), event.y());
        return super.mouseReleased(inside ? translated(event) : event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (bindingPickerOpen || listeningForKey) return super.mouseScrolled(x, y, horizontal, vertical);
        if (x >= panelX && x <= panelX + panelW && y >= panelY && y <= panelY + panelH && maxInspectorScroll() > 0) {
            inspectorScroll -= vertical * 20;
            clampInspectorScroll();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public void mouseMoved(double x, double y) {
        boolean inside = insideInspectorViewport(x, y);
        super.mouseMoved(x, inside ? y + inspectorScroll : y);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (bindingPickerOpen) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                bindingPickerOpen = false;
                rebuildWidgets();
                return true;
            }
        }

        if (listeningForKey && selected != null) {
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                module.setElementBinding(selected, KeystrokesModule.Binding.CUSTOM, event.key());
                module.setElementLabel(selected, keyName(event.key()));
            }
            listeningForKey = false;
            rebuildWidgets();
            return true;
        }

        boolean labelFocused = labelInput != null && labelInput.isFocused();

        // Delete key removes element
        if (selected != null && !labelFocused && (event.key() == GLFW.GLFW_KEY_DELETE || event.key() == GLFW.GLFW_KEY_BACKSPACE)) {
            module.removeElement(selected.id());
            selected = module.elements().isEmpty() ? null : module.elements().getFirst();
            rebuildWidgets();
            return true;
        }

        // Arrow keys nudge selected element
        if (selected != null && !labelFocused && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT
                || event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN)) {
            boolean shift = GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            int step = shift ? 5 : 1;
            int dx = event.key() == GLFW.GLFW_KEY_LEFT ? -step : event.key() == GLFW.GLFW_KEY_RIGHT ? step : 0;
            int dy = event.key() == GLFW.GLFW_KEY_UP ? -step : event.key() == GLFW.GLFW_KEY_DOWN ? step : 0;
            module.moveElement(selected, selected.x() + dx, selected.y() + dy);
            module.saveLayout();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // Backdrop & Main Panel
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelW, panelH);

        // Header Title & Status
        g.text(font, Component.literal("‹ KEYSTROKES V2 DESIGNER"), panelX + 13, panelY + 12, EzUi.TEXT_WHITE, false);
        g.text(font, Component.literal("Ziehen = Verschieben · Ecke = Skalieren · Pfeiltasten = Nudge"),
                canvasX, panelY + 26, EzUi.TEXT_DIM, false);

        // Header Divider
        g.fill(panelX + 6, panelY + 36, panelX + panelW - 6, panelY + 37, EzUi.BORDER_SUBTLE);

        // Inspector Divider
        g.fill(inspectorX - 6, panelY + 36, inspectorX - 5, panelY + panelH - 8, EzUi.BORDER_SUBTLE);

        // --- Canvas Rendering ---
        EzUi.roundedRect(g, canvasX, canvasY, canvasW, canvasH, 3, 0xFF0A0D12);
        g.outline(canvasX, canvasY, canvasW, canvasH, EzUi.BORDER_SUBTLE);

        g.enableScissor(canvasX + 1, canvasY + 1, canvasX + canvasW - 1, canvasY + canvasH - 1);

        // Dotted blueprint grid
        int dotStep = Math.max(12, Math.round(20 * previewScale));
        int offX = Math.floorMod(previewX - canvasX, dotStep);
        int offY = Math.floorMod(previewY - canvasY, dotStep);
        for (int gx = canvasX + offX; gx < canvasX + canvasW; gx += dotStep) {
            for (int gy = canvasY + offY; gy < canvasY + canvasH; gy += dotStep) {
                g.fill(gx, gy, gx + 1, gy + 1, 0x184A5568);
            }
        }

        updatePreviewGeometry();
        module.renderDesignerPreview(g, minecraft, previewX, previewY, previewScale);

        // Hover highlight
        if (!dragging && !resizing && insideCanvas(mouseX, mouseY)) {
            KeystrokesModule.KeyElement hovered = hit(mouseX, mouseY);
            if (hovered != null && (selected == null || hovered.id() != selected.id())) {
                int hx = previewX + Math.round(hovered.x() * previewScale);
                int hy = previewY + Math.round(hovered.y() * previewScale);
                int hw = Math.max(1, Math.round(hovered.width() * previewScale));
                int hh = Math.max(1, Math.round(hovered.height() * previewScale));
                g.outline(hx, hy, hw, hh, 0x605B8F6A);
            }
        }

        // Selection outline & resize handle
        if (selected != null) {
            int x = previewX + Math.round(selected.x() * previewScale);
            int y = previewY + Math.round(selected.y() * previewScale);
            int w = Math.max(1, Math.round(selected.width() * previewScale));
            int h = Math.max(1, Math.round(selected.height() * previewScale));
            g.outline(x - 1, y - 1, w + 2, h + 2, 0xFF06080B);
            g.outline(x, y, w, h, EzUi.ACCENT_EMERALD);

            // Resize handle at bottom-right corner
            int rx = x + w - 5, ry = y + h - 5;
            EzUi.roundedRect(g, rx, ry, 6, 6, 1, EzUi.ACCENT_EMERALD);
            EzUi.roundedRect(g, rx + 1, ry + 1, 4, 4, 1, 0xFFFFFFFF);
        }

        // Magnetic Snapping Guide Lines
        if (hasSnapX) {
            int sx = previewX + (int) Math.round(snapGuideX * previewScale);
            if (sx >= canvasX && sx < canvasX + canvasW) {
                g.fill(sx - 1, canvasY + 1, sx, canvasY + canvasH - 1, 0x405B8F6A);
                g.fill(sx, canvasY + 1, sx + 1, canvasY + canvasH - 1, EzUi.ACCENT_EMERALD);
                g.fill(sx + 1, canvasY + 1, sx + 2, canvasY + canvasH - 1, 0x405B8F6A);
            }
        }
        if (hasSnapY) {
            int sy = previewY + (int) Math.round(snapGuideY * previewScale);
            if (sy >= canvasY && sy < canvasY + canvasH) {
                g.fill(canvasX + 1, sy - 1, canvasX + canvasW - 1, sy, 0x405B8F6A);
                g.fill(canvasX + 1, sy, canvasX + canvasW - 1, sy + 1, EzUi.ACCENT_EMERALD);
                g.fill(canvasX + 1, sy + 1, canvasX + canvasW - 1, sy + 2, 0x405B8F6A);
            }
        }

        // Empty canvas state
        if (module.elements().isEmpty()) {
            g.centeredText(font, Component.literal("Keine Tasten vorhanden"), canvasX + canvasW / 2, canvasY + canvasH / 2 - 8, EzUi.TEXT_MUTED);
            g.centeredText(font, Component.literal("Klicke unten auf '+ Taste' oder lade eine Vorlage"), canvasX + canvasW / 2, canvasY + canvasH / 2 + 6, EzUi.TEXT_DIM);
        }

        g.disableScissor();

        // --- Inspector Header Info ---
        if (!bindingPickerOpen) {
            if (activeTab == Tab.ELEMENT) {
                String sub = selected == null ? "KEINE AUSWAHL"
                        : "TASTE #" + selected.id() + " · POS: " + selected.x() + "," + selected.y() + " · " + selected.width() + "×" + selected.height() + "px";
                g.text(font, Component.literal(sub), inspectorX + 8, panelY + 26, EzUi.TEXT_DIM, false);
            } else if (activeTab == Tab.COLORS) {
                g.text(font, Component.literal("FARBEN & EFFEKTE"), inspectorX + 8, panelY + 26, EzUi.TEXT_DIM, false);
            } else if (activeTab == Tab.PRESETS) {
                g.text(font, Component.literal("FERTIGE VORLAGEN"), inspectorX + 8, panelY + 26, EzUi.TEXT_DIM, false);
            } else {
                g.text(font, Component.literal("FORM & ANIMATION"), inspectorX + 8, panelY + 26, EzUi.TEXT_DIM, false);
            }
        }

        if (bindingPickerOpen) {
            // Draw dim overlay over canvas/inspector
            g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xA006080B);
            EzUi.panel(g, modalX, modalY, modalW, modalH);

            g.text(font, Component.literal("BINDUNG AUSWÄHLEN"), modalX + 14, modalY + 12, EzUi.TEXT_WHITE, false);
            g.text(font, Component.literal("Klicke auf die gewünschte Spielaktion:"), modalX + 14, modalY + 26, EzUi.TEXT_DIM, false);
            super.extractRenderState(g, mouseX, mouseY, delta);
            return;
        }

        clampInspectorScroll();

        // 1. Render inspector widgets clipped to inspector viewport
        g.enableScissor(scrollLeft(), scrollTop(), scrollRight(), scrollBottom());
        for (AbstractWidget widget : inspectorWidgets) {
            widget.setY(widget.getY() - (int) inspectorScroll);
        }
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && !inspectorWidgets.contains(widget)) {
                widget.visible = false;
            }
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
        for (var child : children()) {
            if (child instanceof AbstractWidget widget && !inspectorWidgets.contains(widget)) {
                widget.visible = true;
            }
        }
        for (AbstractWidget widget : inspectorWidgets) {
            widget.setY(widget.getY() + (int) inspectorScroll);
        }
        g.disableScissor();

        // 2. Render fixed widgets (canvas toolbar, tabs, close button)
        for (AbstractWidget widget : inspectorWidgets) {
            widget.visible = false;
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
        for (AbstractWidget widget : inspectorWidgets) {
            widget.visible = true;
        }

        // 3. Render scrollbar track and thumb if content overflows
        if (maxInspectorScroll() > 0) {
            int top = scrollTop();
            int bottom = scrollBottom();
            int track = Math.max(1, bottom - top);
            int maxScroll = maxInspectorScroll();
            int thumb = Math.max(16, (int) ((double) track * track / (track + maxScroll)));
            int thumbY = top + (int) (inspectorScroll / maxScroll * (track - thumb));
            int barX = panelX + panelW - 9;
            g.fill(barX, top, barX + 4, bottom, 0xFF181F28);
            EzUi.roundedRect(g, barX, thumbY, 4, thumb, 2, draggingScrollbar ? 0xFF3CE882 : EzUi.ACCENT_EMERALD);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        module.saveLayout();
        EzScreenBridge.set(minecraft, parent);
    }
}
