package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.function.IntConsumer;

/**
 * Universal FPS-style visual RGBA and Hex-code color picker screen.
 * Features an interactive Saturation/Brightness 2D gradient, vertical Hue slider,
 * prominent '#' Hex-Code EditBox with instant bidirectional sync, the standard
 * 3x3 FPS preset palette, alpha slider, and live before/after preview.
 */
public final class ModuleColorScreen extends Screen {
    // Standard EzClient / Badlion 3x3 Color Palette Presets (matching FPS module)
    private static final int[] PRESETS = {
            0xFF22C96E, 0xFF00D2FF, 0xFF3B82F6,
            0xFFFF6B4A, 0xFFEF4444, 0xFFEAB308,
            0xFFA855F7, 0xFFFFFFFF, 0xFF334155
    };

    private final Screen parent;
    private final IntConsumer setter;
    private final int originalColor;
    private int color;
    private float hue, saturation, brightness;
    private boolean draggingField, draggingHue;
    private int selectedPresetIndex = -1;

    private int panelX, panelY, panelWidth, panelHeight;
    private int fieldX, fieldY, fieldWidth = 96, fieldHeight = 64;
    private int hueX, hueY, hueWidth = 14, hueHeight = 64;
    private EditBox hexInput;
    private EzSlider alphaSlider;
    private boolean updatingHexInternally = false;

    public ModuleColorScreen(Screen parent, String label, int color, IntConsumer setter) {
        super(Component.literal(label));
        this.parent = parent;
        this.originalColor = color;
        this.color = color;
        this.setter = setter;
        float[] hsv = Color.RGBtoHSB((color >>> 16) & 255, (color >>> 8) & 255, color & 255, null);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.brightness = hsv[2];
        checkSelectedPreset();
    }

    private void checkSelectedPreset() {
        selectedPresetIndex = -1;
        int currentRgb = color & 0x00FFFFFF;
        for (int i = 0; i < PRESETS.length; i++) {
            if ((PRESETS[i] & 0x00FFFFFF) == currentRgb) {
                selectedPresetIndex = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        panelWidth = Math.min(ScrollingSettingsScreen.SETTINGS_WIDTH, Math.max(120, width - 48));
        panelHeight = Math.min(ScrollingSettingsScreen.SETTINGS_HEIGHT, Math.max(100, height - 48));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int sidebar = ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH;
        int contentX = panelX + sidebar + 10;
        int contentWidth = panelWidth - sidebar - 20;

        // Top right close button ✕
        addRenderableWidget(new EzButton(
                panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false, b -> onClose()
        ));

        fieldX = contentX;
        fieldY = panelY + 36;
        hueX = fieldX + fieldWidth + 8;
        hueY = fieldY;

        // Prominent Hex EditBox directly beside Hue slider
        int hexX = hueX + hueWidth + 24;
        int hexY = fieldY + 1;
        int hexW = Math.max(68, contentX + contentWidth - hexX);
        hexInput = new EditBox(font, hexX, hexY, hexW, 16, Component.literal("Hex"));
        hexInput.setMaxLength(9);
        hexInput.setResponder(this::onHexInputChanged);
        addRenderableWidget(hexInput);
        updateHexText();

        // Alpha Slider below picker components
        int alphaY = fieldY + fieldHeight + 8;
        alphaSlider = new EzSlider(contentX, alphaY, contentWidth, 18,
                ((color >>> 24) & 255) / 255.0,
                value -> {
                    color = (color & 0x00FFFFFF) | ((int) Math.round(value * 255) << 24);
                    updateHexText();
                },
                value -> Component.literal("Transparenz: " + Math.round(value * 100) + "%"), true);
        addRenderableWidget(alphaSlider);

        // Footer buttons: [Übernehmen] and [Abbrechen]
        int footerY = panelY + panelHeight - 24;
        int halfBtn = (contentWidth - 6) / 2;
        addRenderableWidget(new EzButton(contentX, footerY, halfBtn, 16, Component.literal("Übernehmen"), true,
                button -> {
                    setter.accept(color);
                    onClose();
                }));
        addRenderableWidget(new EzButton(contentX + halfBtn + 6, footerY, halfBtn, 16, Component.literal(app.ezclient.util.EzI18n.text("Abbrechen")), false,
                button -> onClose()));
    }

    private void updateHexText() {
        if (hexInput == null) return;
        updatingHexInternally = true;
        int alpha = (color >>> 24) & 255;
        if (alpha == 255) {
            hexInput.setValue(String.format("%06X", color & 0x00FFFFFF));
        } else {
            hexInput.setValue(String.format("%08X", color));
        }
        updatingHexInternally = false;
        checkSelectedPreset();
    }

    private void onHexInputChanged(String text) {
        if (updatingHexInternally) return;
        try {
            String clean = text.replace("#", "").trim();
            if (clean.length() == 6) {
                int rgb = (int) Long.parseLong(clean, 16);
                int alpha = color & 0xFF000000;
                color = alpha | (rgb & 0x00FFFFFF);
                syncFromColor();
            } else if (clean.length() == 8) {
                color = (int) Long.parseLong(clean, 16);
                syncFromColor();
            }
        } catch (Exception ignored) {}
    }

    private void syncFromColor() {
        float[] hsv = Color.RGBtoHSB((color >>> 16) & 255, (color >>> 8) & 255, color & 255, null);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
        if (alphaSlider != null) {
            alphaSlider.setSliderValue(((color >>> 24) & 255) / 255.0);
        }
        checkSelectedPreset();
    }

    private void updateField(double mouseX, double mouseY) {
        saturation = (float) Math.max(0, Math.min(1, (mouseX - fieldX) / fieldWidth));
        brightness = (float) Math.max(0, Math.min(1, 1 - (mouseY - fieldY) / fieldHeight));
        updateRgb();
    }

    private void updateHue(double mouseY) {
        hue = (float) Math.max(0, Math.min(1, (mouseY - hueY) / hueHeight));
        updateRgb();
    }

    private void updateRgb() {
        color = (color & 0xFF000000) | (Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF);
        updateHexText();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (event.x() >= fieldX && event.x() <= fieldX + fieldWidth
                    && event.y() >= fieldY && event.y() <= fieldY + fieldHeight) {
                draggingField = true;
                updateField(event.x(), event.y());
                return true;
            }
            if (event.x() >= hueX && event.x() <= hueX + hueWidth
                    && event.y() >= hueY && event.y() <= hueY + hueHeight) {
                draggingHue = true;
                updateHue(event.y());
                return true;
            }

            // 3x3 Preset grid click (Right side, below Hex input)
            int presetGridX = hueX + hueWidth + 8;
            int presetGridY = fieldY + 22;
            for (int i = 0; i < PRESETS.length; i++) {
                int px = presetGridX + (i % 3) * 27;
                int py = presetGridY + (i / 3) * 14;
                if (event.x() >= px && event.x() <= px + 25 && event.y() >= py && event.y() <= py + 12) {
                    selectedPresetIndex = i;
                    int alpha = color & 0xFF000000;
                    color = alpha | (PRESETS[i] & 0x00FFFFFF);
                    syncFromColor();
                    updateHexText();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingField) {
            updateField(event.x(), event.y());
            return true;
        }
        if (draggingHue) {
            updateHue(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingField = false;
        draggingHue = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 257 || event.key() == 335) { // Enter / Numpad Enter
            setter.accept(color);
            onClose();
            return true;
        }
        if (event.key() == 256) { // ESC
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);
        renderSidebar(g);

        int sidebar = ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH;
        int contentX = panelX + sidebar + 10;
        int contentWidth = panelWidth - sidebar - 20;

        // Header Title
        g.text(font, EzUi.fitText(getTitle(), contentWidth - 24), contentX, panelY + 10, EzUi.TEXT_WHITE);
        g.fill(contentX, panelY + 28, panelX + panelWidth - 8, panelY + 29, EzUi.BORDER_SUBTLE);

        // 1. Saturation / Brightness 2D Field
        for (int py = 0; py < fieldHeight; py += 2) {
            float value = 1.0f - (float) py / fieldHeight;
            for (int px = 0; px < fieldWidth; px += 2) {
                float sat = (float) px / fieldWidth;
                int rgb = 0xFF000000 | (Color.HSBtoRGB(hue, sat, value) & 0x00FFFFFF);
                g.fill(fieldX + px, fieldY + py, fieldX + px + 2, fieldY + py + 2, rgb);
            }
        }
        g.outline(fieldX - 1, fieldY - 1, fieldWidth + 2, fieldHeight + 2, EzUi.BORDER_SUBTLE);
        int cursorX = fieldX + Math.round(saturation * fieldWidth);
        int cursorY = fieldY + Math.round((1.0f - brightness) * fieldHeight);
        g.outline(cursorX - 2, cursorY - 2, 5, 5, 0xFFFFFFFF);
        g.outline(cursorX - 3, cursorY - 3, 7, 7, 0xFF000000);

        // 2. Vertical Rainbow Hue Bar
        for (int py = 0; py < hueHeight; py += 2) {
            int rgb = 0xFF000000 | (Color.HSBtoRGB((float) py / hueHeight, 1.0f, 1.0f) & 0x00FFFFFF);
            g.fill(hueX, hueY + py, hueX + hueWidth, hueY + py + 2, rgb);
        }
        int hueMarker = hueY + Math.round(hue * hueHeight);
        g.fill(hueX - 1, hueMarker - 1, hueX + hueWidth + 1, hueMarker + 2, 0xFFFFFFFF);
        g.outline(hueX - 2, hueMarker - 2, hueWidth + 3, 4, 0xFF000000);
        g.outline(hueX - 1, hueY - 1, hueWidth + 2, hueHeight + 2, EzUi.BORDER_SUBTLE);

        // 3. Right Side: Swatch & Hex indicator
        int rightAreaX = hueX + hueWidth + 8;
        int swatchY = fieldY + 2;
        // Mini swatch
        EzUi.roundedRect(g, rightAreaX, swatchY, 14, 14, 2, 0xFF35414D);
        EzUi.roundedRect(g, rightAreaX + 1, swatchY + 1, 12, 12, 2, color);
        g.text(font, "#", rightAreaX + 16, swatchY + 3, EzUi.TEXT_MUTED);

        // 4. 3x3 Preset Grid
        int presetGridX = rightAreaX;
        int presetGridY = fieldY + 22;
        for (int i = 0; i < PRESETS.length; i++) {
            int px = presetGridX + (i % 3) * 27;
            int py = presetGridY + (i / 3) * 14;
            EzUi.roundedRect(g, px, py, 25, 12, 2, PRESETS[i]);
            if (selectedPresetIndex == i || (color & 0x00FFFFFF) == (PRESETS[i] & 0x00FFFFFF)) {
                g.outline(px - 1, py - 1, 27, 14, 0xFFFFFFFF);
            }
        }

        // 5. Dual Live Comparison Preview (Original vs New)
        int previewY = fieldY + fieldHeight + 30;
        int previewH = 20;
        int halfPreview = (contentWidth - 6) / 2;

        // Original Preview Card
        renderCheckerboardPreview(g, contentX, previewY, halfPreview, previewH, originalColor);
        g.text(font, "Vorher", contentX + 4, previewY + 6, 0xFFFFFFFF);

        // New Preview Card
        renderCheckerboardPreview(g, contentX + halfPreview + 6, previewY, halfPreview, previewH, color);
        g.text(font, "Neu", contentX + halfPreview + 10, previewY + 6, 0xFFFFFFFF);

        super.extractRenderState(g, mx, my, delta);

        boolean pickerHover = (mx >= fieldX && mx <= hueX + hueWidth && my >= fieldY && my <= fieldY + fieldHeight)
                || (mx >= rightAreaX && mx <= rightAreaX + 81 && my >= presetGridY && my <= presetGridY + 42);
        boolean widgetHover = children().stream()
                .filter(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget)
                .map(child -> (net.minecraft.client.gui.components.AbstractWidget) child)
                .anyMatch(widget -> widget.active && widget.visible && mx >= widget.getX() && mx < widget.getX() + widget.getWidth()
                        && my >= widget.getY() && my < widget.getY() + widget.getHeight());
        EzCursor.setPointer(pickerHover || widgetHover);
    }

    private void renderCheckerboardPreview(GuiGraphicsExtractor g, int px, int py, int pw, int ph, int previewCol) {
        int gridSize = 6;
        for (int gy = py; gy < py + ph; gy += gridSize) {
            for (int gx = px; gx < px + pw; gx += gridSize) {
                int col = (((gx - px) / gridSize + (gy - py) / gridSize) % 2 == 0) ? 0xFF374151 : 0xFF1F2937;
                g.fill(gx, gy, Math.min(gx + gridSize, px + pw), Math.min(gy + gridSize, py + ph), col);
            }
        }
        g.fill(px, py, px + pw, py + ph, previewCol);
        EzUi.outline(g, px, py, pw, ph, EzUi.BORDER_SUBTLE);
    }

    private void renderSidebar(GuiGraphicsExtractor g) {
        int sidebar = ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH;
        g.fill(panelX + sidebar, panelY + 6, panelX + sidebar + 1, panelY + panelHeight - 6, EzUi.BORDER_SUBTLE);
        int logoX = panelX + 26, logoY = panelY + 9;
        EzUi.roundedRect(g, logoX, logoY, 20, 20, 3, 0xFF15181C);
        ModuleIconRenderer.drawTexture(g, ScrollingSettingsScreen.SETTINGS_LOGO, logoX + 2, logoY + 2, 16);
        EzUi.roundedRect(g, panelX + 6, panelY + 43, sidebar - 12, 18, 2, EzUi.BG_CARD_ACTIVE);
        g.centeredText(font, Component.literal(app.ezclient.util.EzI18n.text("Farbe")), panelX + sidebar / 2, panelY + 48, EzUi.TEXT_LIGHT);
    }

    @Override
    public void removed() {
        EzCursor.setPointer(false);
        super.removed();
    }

    @Override
    public void onClose() {
        EzScreenBridge.set(minecraft, parent);
    }
}
