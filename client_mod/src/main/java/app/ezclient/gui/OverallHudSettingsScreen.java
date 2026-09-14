package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.List;

/**
 * Global HUD styling screen:
 * Allows configuring text and border colors, styles, shadows, and boxes across all HUD modules at once,
 * with individual module overrides still respected afterwards.
 */
public final class OverallHudSettingsScreen extends Screen {
    private final Screen parent;
    private int panelX, panelY, panelWidth, panelHeight;

    private int activeTarget = 0; // 0 = Text, 1 = Border
    private int activeSlot = 1;   // 1 = Color 1, 2 = Color 2

    // Color picker state
    private float currentHue = 0.38f;
    private float currentSat = 0.83f;
    private float currentVal = 0.79f;
    private boolean isDraggingSV = false;
    private boolean isDraggingHue = false;
    private int svX, svY, svW, svH;
    private int hueX, hueY, hueW, hueH;
    private EditBox hexInput;
    private boolean updatingHexInternally = false;
    private boolean showResetConfirmation = false;

    // Overall global states (mirrored to all modules)
    private boolean globalBox = false;
    private boolean globalBorder = false;
    private boolean globalShadow = true;
    private HudModule.BorderStyle globalBorderStyle = HudModule.BorderStyle.PIXEL;
    private HudModule.ColorMode globalTextColorMode = HudModule.ColorMode.SOLID;
    private HudModule.ColorMode globalBorderColorMode = HudModule.ColorMode.SOLID;
    private int globalTextColor = 0xFFFFFFFF;
    private int globalTextWave2 = 0xFF43DD8C;
    private int globalBorderColor = 0xFF22C96E;
    private int globalBorderWave2 = 0xFF00D2FF;

    public OverallHudSettingsScreen(Screen parent) {
        super(Component.literal("Overall HUD Einstellungen"));
        this.parent = parent;
    }

    private List<HudModule> allHudModules() {
        return ModuleManager.getInstance().getHudModules();
    }

    @Override
    protected void init() {
        panelWidth = 416;
        panelHeight = 236;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int rightX = panelX + 148;
        int rightWidth = panelWidth - 160;

        // Close button (Top Right)
        addRenderableWidget(new EzButton(
                panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()
        ));

        // ── LEFT SIDE: Overall Box, Border, Style, Shadow toggles ──
        int leftX = panelX + 12;
        int leftW = 124;
        int leftY = panelY + 38;

        addRenderableWidget(new EzButton(
                leftX, leftY, leftW, 16,
                Component.literal("Box: " + (globalBox ? "An" : "Aus")), globalBox,
                b -> {
                    globalBox = !globalBox;
                    for (HudModule m : allHudModules()) m.setBackground(globalBox);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ));
        leftY += 20;

        addRenderableWidget(new EzButton(
                leftX, leftY, leftW, 16,
                Component.literal("Border: " + (globalBorder ? "An" : "Aus")), globalBorder,
                b -> {
                    globalBorder = !globalBorder;
                    for (HudModule m : allHudModules()) m.setBorder(globalBorder);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ));
        leftY += 20;

        addRenderableWidget(new EzButton(
                leftX, leftY, leftW, 16,
                Component.literal("Stil: " + globalBorderStyle.name().replace('_', ' ')), true,
                b -> {
                    HudModule.BorderStyle[] styles = HudModule.BorderStyle.values();
                    int next = (globalBorderStyle.ordinal() + 1) % styles.length;
                    globalBorderStyle = styles[next];
                    for (HudModule m : allHudModules()) m.setBorderStyle(globalBorderStyle);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ));
        leftY += 20;

        addRenderableWidget(new EzButton(
                leftX, leftY, leftW, 16,
                Component.literal("Schatten: " + (globalShadow ? "An" : "Aus")), globalShadow,
                b -> {
                    globalShadow = !globalShadow;
                    for (HudModule m : allHudModules()) m.setTextShadow(globalShadow);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ));

        // ── RIGHT SIDE: Target Selector, Mode, Slots, Color Picker ──
        int targetY = panelY + 38;
        int targetW = (rightWidth - 4) / 2;

        addRenderableWidget(new EzButton(
                rightX, targetY, targetW, 16,
                Component.literal("Text"), activeTarget == 0,
                b -> {
                    activeTarget = 0;
                    activeSlot = 1;
                    syncPicker();
                    rebuildWidgets();
                }
        ));

        addRenderableWidget(new EzButton(
                rightX + targetW + 4, targetY, targetW, 16,
                Component.literal("Border"), activeTarget == 1,
                b -> {
                    activeTarget = 1;
                    activeSlot = 1;
                    syncPicker();
                    rebuildWidgets();
                }
        ));

        // Mode buttons: Solid / Wave / Rainbow
        int modeY = targetY + 22;
        int cW = 82;
        int cGap = 4;
        HudModule.ColorMode curMode = activeTarget == 0 ? globalTextColorMode : globalBorderColorMode;

        addRenderableWidget(new EzButton(
                rightX, modeY, cW, 16,
                Component.literal("Einfarbig"), curMode == HudModule.ColorMode.SOLID,
                b -> {
                    if (activeTarget == 0) {
                        globalTextColorMode = HudModule.ColorMode.SOLID;
                        for (HudModule m : allHudModules()) m.setColorMode(HudModule.ColorMode.SOLID);
                    } else {
                        globalBorderColorMode = HudModule.ColorMode.SOLID;
                        for (HudModule m : allHudModules()) m.setBorderColorMode(HudModule.ColorMode.SOLID);
                    }
                    ConfigManager.save();
                    syncPicker();
                    rebuildWidgets();
                }
        ));

        addRenderableWidget(new EzButton(
                rightX + cW + cGap, modeY, cW, 16,
                Component.literal("Welle"), curMode == HudModule.ColorMode.WAVE,
                b -> {
                    if (activeTarget == 0) {
                        globalTextColorMode = HudModule.ColorMode.WAVE;
                        for (HudModule m : allHudModules()) m.setColorMode(HudModule.ColorMode.WAVE);
                    } else {
                        globalBorderColorMode = HudModule.ColorMode.WAVE;
                        for (HudModule m : allHudModules()) m.setBorderColorMode(HudModule.ColorMode.WAVE);
                    }
                    ConfigManager.save();
                    syncPicker();
                    rebuildWidgets();
                }
        ));

        addRenderableWidget(new EzButton(
                rightX + (cW + cGap) * 2, modeY, cW, 16,
                Component.literal("Rainbow"), curMode == HudModule.ColorMode.RAINBOW,
                b -> {
                    if (activeTarget == 0) {
                        globalTextColorMode = HudModule.ColorMode.RAINBOW;
                        for (HudModule m : allHudModules()) m.setColorMode(HudModule.ColorMode.RAINBOW);
                    } else {
                        globalBorderColorMode = HudModule.ColorMode.RAINBOW;
                        for (HudModule m : allHudModules()) m.setBorderColorMode(HudModule.ColorMode.RAINBOW);
                    }
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ));

        int pickerStartY = modeY + 20;

        // If Wave mode: Slot 1 / Slot 2 buttons
        if (curMode == HudModule.ColorMode.WAVE) {
            int slotW = (rightWidth - 4) / 2;
            addRenderableWidget(new EzButton(
                    rightX, pickerStartY, slotW, 14,
                    Component.literal("Farbe 1"), activeSlot == 1,
                    b -> {
                        activeSlot = 1;
                        syncPicker();
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    rightX + slotW + 4, pickerStartY, slotW, 14,
                    Component.literal("Farbe 2"), activeSlot == 2,
                    b -> {
                        activeSlot = 2;
                        syncPicker();
                        rebuildWidgets();
                    }
            ));
            pickerStartY += 18;
        }

        svX = rightX;
        svY = pickerStartY;
        svW = 84;
        svH = 68;
        hueX = svX + svW + 6;
        hueY = pickerStartY;
        hueW = 12;
        hueH = 68;

        if (curMode != HudModule.ColorMode.RAINBOW) {
            int hexX = hueX + hueW + 12;
            int hexY = svY;
            hexInput = new EditBox(font, hexX, hexY, 60, 13, Component.literal("Hex"));
            hexInput.setMaxLength(8);
            hexInput.setResponder(this::onHexInputChanged);
            addRenderableWidget(hexInput);
            syncPicker();
        } else {
            hexInput = null;
        }

        // Reset Button (Bottom Right)
        addRenderableWidget(new EzButton(
                panelX + panelWidth - 76, panelY + panelHeight - 26, 64, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Reset")), false,
                b -> showResetConfirmation = true
        ));
    }

    private void applyActiveColor(int color) {
        if (activeTarget == 0) {
            if (activeSlot == 1) {
                globalTextColor = color;
                for (HudModule m : allHudModules()) m.setTextColor(color);
            } else {
                globalTextWave2 = color;
                for (HudModule m : allHudModules()) m.setWaveColor2(color);
            }
        } else {
            if (activeSlot == 1) {
                globalBorderColor = color;
                for (HudModule m : allHudModules()) m.setBorderColor(color);
            } else {
                globalBorderWave2 = color;
                for (HudModule m : allHudModules()) m.setBorderWaveColor2(color);
            }
        }
        ConfigManager.save();
    }

    private int currentTargetColor() {
        if (activeTarget == 0) {
            return activeSlot == 1 ? globalTextColor : globalTextWave2;
        } else {
            return activeSlot == 1 ? globalBorderColor : globalBorderWave2;
        }
    }

    private void syncPicker() {
        int color = currentTargetColor();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        currentHue = hsv[0];
        currentSat = hsv[1];
        currentVal = hsv[2];

        updateHexText(color);
    }

    private void updateHexText(int color) {
        if (hexInput == null) return;
        updatingHexInternally = true;
        String hex = String.format("%06X", (color & 0xFFFFFF));
        hexInput.setValue(hex);
        updatingHexInternally = false;
    }

    private void onHexInputChanged(String text) {
        if (updatingHexInternally) return;
        try {
            String clean = text.replace("#", "").trim();
            if (clean.length() == 6) {
                int rgb = (int) Long.parseLong(clean, 16);
                int fullColor = 0xFF000000 | rgb;
                applyActiveColor(fullColor);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                float[] hsv = Color.RGBtoHSB(r, g, b, null);
                currentHue = hsv[0];
                currentSat = hsv[1];
                currentVal = hsv[2];
            }
        } catch (Exception ignored) {}
    }

    private void updateFromHSV() {
        int rgb = Color.HSBtoRGB(currentHue, currentSat, currentVal);
        int fullColor = 0xFF000000 | (rgb & 0xFFFFFF);
        applyActiveColor(fullColor);
        updateHexText(fullColor);
    }

    private void resetAllOverall() {
        globalBox = false;
        globalBorder = false;
        globalShadow = true;
        globalBorderStyle = HudModule.BorderStyle.PIXEL;
        globalTextColorMode = HudModule.ColorMode.SOLID;
        globalBorderColorMode = HudModule.ColorMode.SOLID;
        globalTextColor = 0xFFFFFFFF;
        globalTextWave2 = 0xFF43DD8C;
        globalBorderColor = 0xFF22C96E;
        globalBorderWave2 = 0xFF00D2FF;

        for (HudModule m : allHudModules()) {
            m.setBackground(false);
            m.setBorder(false);
            m.setBorderStyle(HudModule.BorderStyle.PIXEL);
            m.setTextShadow(true);
            m.setColorMode(HudModule.ColorMode.SOLID);
            m.setBorderColorMode(HudModule.ColorMode.SOLID);
            m.setTextColor(0xFFFFFFFF);
            m.setWaveColor2(0xFF43DD8C);
            m.setBorderColor(0xFF22C96E);
            m.setBorderWaveColor2(0xFF00D2FF);
        }
        ConfigManager.save();
        syncPicker();
        rebuildWidgets();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (showResetConfirmation) {
            int diaW = 240, diaH = 84;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;

            if (e.x() >= diaX + 14 && e.x() <= diaX + 114 && e.y() >= diaY + 52 && e.y() <= diaY + 72) {
                resetAllOverall();
                showResetConfirmation = false;
                return true;
            }
            if (e.x() >= diaX + 126 && e.x() <= diaX + 226 && e.y() >= diaY + 52 && e.y() <= diaY + 72) {
                showResetConfirmation = false;
                return true;
            }
            return true;
        }

        if (e.button() == 0) {
            if (e.x() >= svX && e.x() <= svX + svW && e.y() >= svY && e.y() <= svY + svH) {
                isDraggingSV = true;
                updateSVDrag(e.x(), e.y());
                return true;
            }
            if (e.x() >= hueX && e.x() <= hueX + hueW && e.y() >= hueY && e.y() <= hueY + hueH) {
                isDraggingHue = true;
                updateHueDrag(e.y());
                return true;
            }
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (e.button() == 0) {
            isDraggingSV = false;
            isDraggingHue = false;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double deltaX, double deltaY) {
        if (isDraggingSV) {
            updateSVDrag(e.x(), e.y());
            return true;
        }
        if (isDraggingHue) {
            updateHueDrag(e.y());
            return true;
        }
        return super.mouseDragged(e, deltaX, deltaY);
    }

    private void updateSVDrag(double mouseX, double mouseY) {
        currentSat = (float) Math.max(0.0, Math.min(1.0, (mouseX - svX) / svW));
        currentVal = (float) Math.max(0.0, Math.min(1.0, 1.0 - ((mouseY - svY) / svH)));
        updateFromHSV();
    }

    private void updateHueDrag(double mouseY) {
        currentHue = (float) Math.max(0.0, Math.min(1.0, (mouseY - hueY) / hueH));
        updateFromHSV();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        g.text(font, "Overall HUD Einstellungen", panelX + 12, panelY + 11, EzUi.TEXT_WHITE);
        g.text(font, "Globales Styling für alle HUD-Module", panelX + 12, panelY + 23, EzUi.TEXT_DIM);

        HudModule.ColorMode curMode = activeTarget == 0 ? globalTextColorMode : globalBorderColorMode;

        // Render SV Box & Hue Slider if not rainbow
        if (curMode != HudModule.ColorMode.RAINBOW) {
            EzUi.roundedRect(g, svX - 1, svY - 1, svW + 2, svH + 2, 2, 0xFF35414D);
            for (int px = 0; px < svW; px += 2) {
                float s = (float) px / svW;
                for (int py = 0; py < svH; py += 2) {
                    float v = 1.0f - ((float) py / svH);
                    int rgb = Color.HSBtoRGB(currentHue, s, v);
                    g.fill(svX + px, svY + py, svX + px + 2, svY + py + 2, 0xFF000000 | rgb);
                }
            }

            int handleX = svX + (int) (currentSat * svW);
            int handleY = svY + (int) ((1.0f - currentVal) * svH);
            EzUi.circle(g, handleX, handleY, 3, 0xFFFFFFFF);
            EzUi.circle(g, handleX, handleY, 2, 0xFF000000);

            // Hue Bar
            EzUi.roundedRect(g, hueX - 1, hueY - 1, hueW + 2, hueH + 2, 2, 0xFF35414D);
            for (int py = 0; py < hueH; py++) {
                float h = (float) py / hueH;
                int rgb = Color.HSBtoRGB(h, 1.0f, 1.0f);
                g.fill(hueX, hueY + py, hueX + hueW, hueY + py + 1, 0xFF000000 | rgb);
            }
            int hueHandleY = hueY + (int) (currentHue * hueH);
            g.fill(hueX - 1, hueHandleY - 1, hueX + hueW + 1, hueHandleY + 2, 0xFFFFFFFF);
            g.fill(hueX, hueHandleY, hueX + hueW, hueHandleY + 1, 0xFF000000);
        }

        super.extractRenderState(g, mx, my, delta);

        if (showResetConfirmation) {
            EzUi.backdrop(g, width, height);
            int diaW = 240, diaH = 84;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;
            EzUi.panel(g, diaX, diaY, diaW, diaH);
            g.centeredText(font, Component.literal("Alle HUD-Styles zurücksetzen?"), diaX + diaW / 2, diaY + 18, 0xFFFFFFFF);
            g.centeredText(font, Component.literal("Dies überschreibt alle Module!"), diaX + diaW / 2, diaY + 32, 0xFFA0A0A0);

            EzUi.roundedRect(g, diaX + 14, diaY + 52, 100, 20, 3, 0xFFCC3333);
            g.centeredText(font, Component.literal(app.ezclient.util.EzI18n.text("Zurücksetzen")), diaX + 64, diaY + 58, 0xFFFFFFFF);

            EzUi.roundedRect(g, diaX + 126, diaY + 52, 100, 20, 3, 0xFF2A3441);
            g.centeredText(font, Component.literal(app.ezclient.util.EzI18n.text("Abbrechen")), diaX + 176, diaY + 58, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) EzScreenBridge.set(minecraft, parent);
    }
}
