package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Super simple pixel-by-pixel crosshair editor with color mode selection
 * (Normal/Solid, Wave, Rainbow), color picker, and borderless live preview.
 */
public final class CrosshairPaintScreen extends Screen {
    private static final int CELL = 8;
    private static final int[] PRESET_COLORS = {
            0xFFFFFFFF, // White
            0xFFFF3333, // Red
            0xFF22C96E, // Emerald Green
            0xFF00D2FF, // Cyan
            0xFFFFD700, // Gold / Yellow
            0xFFD946EF, // Purple / Magenta
            0xFFFF8800, // Orange
            0xFF94A3B8  // Light Slate / Gray
    };

    private final Screen parent;
    private final CrosshairModule crosshair;
    private final String targetKey;
    private final String targetDisplayName;
    private final boolean isTargetRule;
    private final boolean[][] targetPixels = new boolean[CrosshairModule.PAINT_SIZE][CrosshairModule.PAINT_SIZE];
    private int targetColor;

    private int panelX, panelY;
    private int panelWidth = 416, panelHeight = 236;
    private int gridX, gridY;
    private boolean painting;
    private boolean paintValue;
    private boolean isListeningForHotkey;

    public CrosshairPaintScreen(Screen parent, CrosshairModule crosshair) {
        this(parent, crosshair, null, null);
    }

    public CrosshairPaintScreen(Screen parent, CrosshairModule crosshair, String targetKey, String targetDisplayName) {
        super(Component.literal(targetKey != null ? "Fadenkreuz: " + targetDisplayName : "Fadenkreuz gestalten"));
        this.parent = parent;
        this.crosshair = crosshair;
        this.targetKey = targetKey;
        this.targetDisplayName = targetDisplayName;
        this.isTargetRule = (targetKey != null);

        if (isTargetRule) {
            var rule = crosshair.getTargetRule(targetKey);
            this.targetColor = rule != null ? rule.color() : crosshair.getTextColor();
            String pat = rule != null ? rule.pattern() : null;
            if (pat != null && pat.length() == CrosshairModule.PAINT_SIZE * CrosshairModule.PAINT_SIZE) {
                for (int y = 0; y < CrosshairModule.PAINT_SIZE; y++) {
                    for (int x = 0; x < CrosshairModule.PAINT_SIZE; x++) {
                        targetPixels[y][x] = pat.charAt(y * CrosshairModule.PAINT_SIZE + x) == '1';
                    }
                }
            } else {
                for (int y = 0; y < CrosshairModule.PAINT_SIZE; y++) {
                    for (int x = 0; x < CrosshairModule.PAINT_SIZE; x++) {
                        targetPixels[y][x] = crosshair.isPainted(x, y);
                    }
                }
            }
        }
    }

    @Override
    protected void init() {
        panelWidth = 416;
        panelHeight = 236;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        gridX = panelX + 16;
        gridY = panelY + 32;

        int rightX = gridX + CrosshairModule.PAINT_SIZE * CELL + 14;
        int rightW = panelX + panelWidth - 16 - rightX;

        // Close button at top right
        addRenderableWidget(new EzButton(panelX + panelWidth - 22, panelY + 8, 14, 14,
                Component.literal("✕"), false, b -> onClose()));

        if (!isTargetRule) {
            // Row 1: Mode buttons (Einfarbig / Welle / Regenbogen)
            int modeY = panelY + 32;
            int modeBtnW = (rightW - 6) / 3;
            HudModule.ColorMode currentMode = crosshair.getColorMode();

            addRenderableWidget(new EzButton(rightX, modeY, modeBtnW, 18,
                    Component.literal("Einfarbig"), currentMode == HudModule.ColorMode.SOLID,
                    b -> {
                        crosshair.setColorMode(HudModule.ColorMode.SOLID);
                        ConfigManager.save();
                        rebuildWidgets();
                    }));

            addRenderableWidget(new EzButton(rightX + modeBtnW + 3, modeY, modeBtnW, 18,
                    Component.literal("Welle"), currentMode == HudModule.ColorMode.WAVE,
                    b -> {
                        crosshair.setColorMode(HudModule.ColorMode.WAVE);
                        ConfigManager.save();
                        rebuildWidgets();
                    }));

            addRenderableWidget(new EzButton(rightX + (modeBtnW + 3) * 2, modeY, rightW - (modeBtnW + 3) * 2, 18,
                    Component.literal("Regenbogen"), currentMode == HudModule.ColorMode.RAINBOW,
                    b -> {
                        crosshair.setColorMode(HudModule.ColorMode.RAINBOW);
                        ConfigManager.save();
                        rebuildWidgets();
                    }));

            // Row 2: Color selection according to mode
            int colorRowY = modeY + 22;
            if (currentMode == HudModule.ColorMode.SOLID) {
                String hex = String.format(Locale.ROOT, "#%06X", crosshair.getTextColor() & 0xFFFFFF);
                addRenderableWidget(new EzButton(rightX, colorRowY, rightW, 18,
                        Component.literal("Farbe wählen (" + hex + ")"), false,
                        b -> {
                            if (minecraft != null) {
                                EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Fadenkreuz Farbe", crosshair.getTextColor(),
                                         c -> {
                                             crosshair.setTextColor(c);
                                             ConfigManager.save();
                                         }));
                            }
                        }));
            } else if (currentMode == HudModule.ColorMode.WAVE) {
                int halfW = (rightW - 4) / 2;
                String hex1 = String.format(Locale.ROOT, "#%06X", crosshair.getTextColor() & 0xFFFFFF);
                String hex2 = String.format(Locale.ROOT, "#%06X", crosshair.getWaveColor2() & 0xFFFFFF);

                addRenderableWidget(new EzButton(rightX, colorRowY, halfW, 18,
                        Component.literal("Farbe 1 (" + hex1 + ")"), false,
                        b -> {
                            if (minecraft != null) {
                                EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Welle Farbe 1", crosshair.getTextColor(),
                                         c -> {
                                             crosshair.setTextColor(c);
                                             ConfigManager.save();
                                         }));
                            }
                        }));

                addRenderableWidget(new EzButton(rightX + halfW + 4, colorRowY, halfW, 18,
                        Component.literal("Farbe 2 (" + hex2 + ")"), false,
                        b -> {
                            if (minecraft != null) {
                                EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Welle Farbe 2", crosshair.getWaveColor2(),
                                         c -> {
                                             crosshair.setWaveColor2(c);
                                             ConfigManager.save();
                                         }));
                            }
                        }));
            }

            int toolsY = currentMode == HudModule.ColorMode.SOLID ? colorRowY + 40 : colorRowY + 22;

            // Row 4: Tools (Leeren / Kreuz)
            int toolBtnW = (rightW - 4) / 2;
            addRenderableWidget(new EzButton(rightX, toolsY, toolBtnW, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Leeren")), false,
                    b -> {
                        crosshair.clearPaint();
                        ConfigManager.save();
                    }));

            addRenderableWidget(new EzButton(rightX + toolBtnW + 4, toolsY, toolBtnW, 18,
                    Component.literal("Kreuz"), false,
                    b -> crosshair.resetPaintPattern(true)));

            // Row 5: Target rules for any Entity and Block
            int rulesY = toolsY + 22;
            addRenderableWidget(new EzButton(rightX, rulesY, rightW, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Ziel-Regeln (Entity & Block) …")), false,
                    b -> {
                        if (minecraft != null) {
                            EzScreenBridge.set(minecraft, new CrosshairTargetSettingsScreen(this, crosshair));
                        }
                    }));
        } else {
            // Target Rule Mode: Solid Color Selection + Tool buttons
            int colorRowY = panelY + 32;
            String hex = String.format(Locale.ROOT, "#%06X", targetColor & 0xFFFFFF);
            addRenderableWidget(new EzButton(rightX, colorRowY, rightW, 18,
                    Component.literal("Farbe wählen (" + hex + ")"), false,
                    b -> {
                        if (minecraft != null) {
                            EzScreenBridge.set(minecraft, new ModuleColorScreen(this, targetDisplayName + " Farbe", targetColor,
                                     c -> {
                                         targetColor = c;
                                         rebuildWidgets();
                                     }));
                        }
                    }));

            // Tools (Leeren / Kreuz / Kopieren)
            int toolsY = colorRowY + 40;
            int toolBtnW = (rightW - 4) / 3;
            addRenderableWidget(new EzButton(rightX, toolsY, toolBtnW, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Leeren")), false,
                    b -> {
                        clearTargetPixels();
                        rebuildWidgets();
                    }));

            addRenderableWidget(new EzButton(rightX + toolBtnW + 2, toolsY, toolBtnW, 18,
                    Component.literal("Kreuz"), false,
                    b -> {
                        resetTargetToCross();
                        rebuildWidgets();
                    }));

            addRenderableWidget(new EzButton(rightX + (toolBtnW + 2) * 2, toolsY, rightW - (toolBtnW + 2) * 2, 18,
                    Component.literal("Kopieren"), false,
                    b -> {
                        for (int y = 0; y < CrosshairModule.PAINT_SIZE; y++) {
                            for (int x = 0; x < CrosshairModule.PAINT_SIZE; x++) {
                                targetPixels[y][x] = crosshair.isPainted(x, y);
                            }
                        }
                        rebuildWidgets();
                    }));

            // Row 5: Action to revert to Auto mode
            int rulesY = toolsY + 22;
            addRenderableWidget(new EzButton(rightX, rulesY, rightW, 18,
                    Component.literal("Auf Auto zurückstellen"), false,
                    b -> {
                        var cur = crosshair.getTargetRule(targetKey);
                        float sc = cur != null ? cur.scale() : 1.0f;
                        crosshair.setTargetRule(targetKey, new CrosshairModule.CrosshairTargetRule(targetColor, sc, "AUTO", getTargetPatternString()));
                        ConfigManager.save();
                        if (minecraft != null) EzScreenBridge.set(minecraft, parent);
                    }));
        }

        if (!isTargetRule) {
            addRenderableWidget(new EzHotkeyButton(gridX, panelY + 204, CrosshairModule.PAINT_SIZE * CELL,
                    crosshair.getKeyBind(), isListeningForHotkey,
                    () -> { isListeningForHotkey = !isListeningForHotkey; rebuildWidgets(); }));
        }

        // Done button
        addRenderableWidget(new EzButton(rightX, panelY + panelHeight - 24, rightW, 18,
                Component.literal(app.ezclient.util.EzI18n.text("Fertig")), true,
                b -> onClose()));
    }

    private void clearTargetPixels() {
        for (int y = 0; y < CrosshairModule.PAINT_SIZE; y++) {
            for (int x = 0; x < CrosshairModule.PAINT_SIZE; x++) {
                targetPixels[y][x] = false;
            }
        }
    }

    private void resetTargetToCross() {
        clearTargetPixels();
        int c = CrosshairModule.PAINT_SIZE / 2;
        targetPixels[c][c] = true;
        for (int i = 3; i <= 5; i++) {
            targetPixels[c - i][c] = true;
            targetPixels[c + i][c] = true;
            targetPixels[c][c - i] = true;
            targetPixels[c][c + i] = true;
        }
    }

    private String getTargetPatternString() {
        StringBuilder out = new StringBuilder(CrosshairModule.PAINT_SIZE * CrosshairModule.PAINT_SIZE);
        for (boolean[] row : targetPixels) {
            for (boolean pixel : row) {
                out.append(pixel ? '1' : '0');
            }
        }
        return out.toString();
    }

    @Override
    public void rebuildWidgets() {
        clearWidgets();
        init();
    }

    private boolean isPixelPainted(int x, int y) {
        if (x < 0 || y < 0 || x >= CrosshairModule.PAINT_SIZE || y >= CrosshairModule.PAINT_SIZE) return false;
        return isTargetRule ? targetPixels[y][x] : crosshair.isPainted(x, y);
    }

    private void setPixelPainted(int x, int y, boolean value) {
        if (x < 0 || y < 0 || x >= CrosshairModule.PAINT_SIZE || y >= CrosshairModule.PAINT_SIZE) return;
        if (isTargetRule) {
            targetPixels[y][x] = value;
        } else {
            crosshair.setPainted(x, y, value);
        }
    }

    private boolean paint(double mouseX, double mouseY, boolean value) {
        int x = (int) ((mouseX - gridX) / CELL);
        int y = (int) ((mouseY - gridY) / CELL);
        if (mouseX < gridX || mouseY < gridY || x < 0 || y < 0 || x >= CrosshairModule.PAINT_SIZE || y >= CrosshairModule.PAINT_SIZE)
            return false;
        setPixelPainted(x, y, value);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isListeningForHotkey && event.button() != 0) {
            EzKeyBindings.applyModuleKeyBind(crosshair, -100 - event.button());
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        if (event.button() == 0 || event.button() == 1) {
            paintValue = event.button() == 0;
            if (paint(event.x(), event.y(), paintValue)) {
                painting = true;
                return true;
            }
        }

        // Check preset color dots click in Solid mode or Target mode
        boolean solid = isTargetRule || crosshair.getColorMode() == HudModule.ColorMode.SOLID;
        if (solid) {
            int rightX = gridX + CrosshairModule.PAINT_SIZE * CELL + 14;
            int presetY = panelY + 32 + (isTargetRule ? 20 : (22 + 20));
            int dotSize = 14;
            int gap = 5;
            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int dx = rightX + i * (dotSize + gap);
                if (event.x() >= dx && event.x() <= dx + dotSize && event.y() >= presetY && event.y() <= presetY + dotSize) {
                    if (isTargetRule) {
                        targetColor = PRESET_COLORS[i];
                    } else {
                        crosshair.setTextColor(PRESET_COLORS[i]);
                        ConfigManager.save();
                    }
                    rebuildWidgets();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (isListeningForHotkey) {
            int key = event.key();
            EzKeyBindings.applyModuleKeyBind(crosshair, key == 256 || key == 259 || key == 261 ? -1 : key);
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (painting) {
            paint(event.x(), event.y(), paintValue);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (painting) {
            painting = false;
            if (!isTargetRule) {
                crosshair.finishPainting();
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        // Header Title
        String titleText = isTargetRule ? "Fadenkreuz malen: " + targetDisplayName : "Fadenkreuz selbst malen";
        String subtitleText = isTargetRule ? "Linksklick: Malen  ·  Rechtsklick: Radieren  ·  Custom Regel" : "Linksklick: Malen  ·  Rechtsklick: Radieren";
        g.text(font, Component.literal(titleText), panelX + 16, panelY + 12, EzUi.TEXT_WHITE, false);
        g.text(font, Component.literal(subtitleText), panelX + 16, panelY + 24, EzUi.TEXT_MUTED, false);

        int centerIdx = CrosshairModule.PAINT_SIZE / 2;

        // Draw 21x21 pixel grid
        for (int y = 0; y < CrosshairModule.PAINT_SIZE; y++) {
            for (int x = 0; x < CrosshairModule.PAINT_SIZE; x++) {
                int px = gridX + x * CELL;
                int py = gridY + y * CELL;

                if (isPixelPainted(x, y)) {
                    int pixelColor;
                    if (isTargetRule) {
                        pixelColor = targetColor;
                    } else if (crosshair.getColorMode() == HudModule.ColorMode.RAINBOW) {
                        pixelColor = crosshair.color((x + y) * 45L);
                    } else if (crosshair.getColorMode() == HudModule.ColorMode.WAVE) {
                        pixelColor = crosshair.color((x + y) * 35L);
                    } else {
                        pixelColor = crosshair.getTextColor();
                    }
                    g.fill(px, py, px + CELL - 1, py + CELL - 1, pixelColor);
                } else {
                    boolean isCenter = (x == centerIdx && y == centerIdx);
                    boolean isAxis = (x == centerIdx || y == centerIdx);
                    int bg;
                    if (isCenter) bg = 0xFF2F3C4D;
                    else if (isAxis) bg = 0xFF1B222C;
                    else bg = ((x + y) % 2 == 0) ? 0xFF13171F : 0xFF171D26;
                    g.fill(px, py, px + CELL - 1, py + CELL - 1, bg);
                }
            }
        }
        g.outline(gridX - 1, gridY - 1, CrosshairModule.PAINT_SIZE * CELL + 2, CrosshairModule.PAINT_SIZE * CELL + 2, EzUi.BORDER_SUBTLE);

        int rightX = gridX + CrosshairModule.PAINT_SIZE * CELL + 14;
        int rightW = panelX + panelWidth - 16 - rightX;

        // Preset color dots (Solid mode / Target mode)
        if (isTargetRule || crosshair.getColorMode() == HudModule.ColorMode.SOLID) {
            int presetY = panelY + 32 + (isTargetRule ? 20 : (22 + 20));
            int dotSize = 14;
            int gap = 5;
            int curCol = isTargetRule ? targetColor : crosshair.getTextColor();
            for (int i = 0; i < PRESET_COLORS.length; i++) {
                int dx = rightX + i * (dotSize + gap);
                int c = PRESET_COLORS[i];
                boolean sel = (curCol & 0xFFFFFF) == (c & 0xFFFFFF);
                EzUi.roundedRect(g, dx, presetY, dotSize, dotSize, 3, c);
                if (sel) {
                    g.outline(dx - 1, presetY - 1, dotSize + 2, dotSize + 2, 0xFFFFFFFF);
                }
            }
        } else if (crosshair.getColorMode() == HudModule.ColorMode.RAINBOW) {
            int textY = panelY + 32 + 24;
            g.text(font, Component.literal("Flüssiger RGB Farbwechsel"), rightX, textY, EzUi.TEXT_MUTED, false);
        }

        // Live-Vorschau Box (No border/box around the crosshair itself!)
        int previewBoxY = panelY + panelHeight - 82;
        int previewBoxH = 50;
        EzUi.roundedRect(g, rightX, previewBoxY, rightW, previewBoxH, 4, 0xEE090D12);
        g.outline(rightX, previewBoxY, rightW, previewBoxH, EzUi.BORDER_SUBTLE);

        g.text(font, Component.literal(app.ezclient.util.EzI18n.text("Live-Vorschau")), rightX + 6, previewBoxY + 5, EzUi.TEXT_DIM, false);

        int previewCenterX = rightX + rightW / 2;
        int previewCenterY = previewBoxY + previewBoxH / 2 + 2;

        if (isTargetRule) {
            renderTargetPreview(g, previewCenterX, previewCenterY);
        } else {
            crosshair.renderCrosshair(g, minecraft, previewCenterX, previewCenterY, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void renderTargetPreview(GuiGraphicsExtractor g, float cx, float cy) {
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        int center = CrosshairModule.PAINT_SIZE / 2;
        int outline = (crosshair.isShowOutline() ? crosshair.getOutlineColor() : 0xFF000000);
        for (int y = 0; y < CrosshairModule.PAINT_SIZE; y++) {
            for (int x = 0; x < CrosshairModule.PAINT_SIZE; x++) {
                if (targetPixels[y][x]) {
                    g.fill(x - center - 1, y - center - 1, x - center + 2, y - center + 2, outline);
                }
            }
        }
        for (int y = 0; y < CrosshairModule.PAINT_SIZE; y++) {
            for (int x = 0; x < CrosshairModule.PAINT_SIZE; x++) {
                if (targetPixels[y][x]) {
                    g.fill(x - center, y - center, x - center + 1, y - center + 1, targetColor);
                }
            }
        }
        g.pose().popMatrix();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (isTargetRule) {
            var current = crosshair.getTargetRule(targetKey);
            float scale = current != null ? current.scale() : 1.0f;
            String pattern = getTargetPatternString();
            crosshair.setTargetRule(targetKey, new CrosshairModule.CrosshairTargetRule(targetColor, scale, "CUSTOM", pattern));
            ConfigManager.save();
        } else {
            crosshair.finishPainting();
        }
        if (minecraft != null) EzScreenBridge.set(minecraft, parent);
    }
}
