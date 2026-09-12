package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.function.IntConsumer;

/** Shared visual RGBA picker used by every module color option. */
public final class ModuleColorScreen extends Screen {
    private static final int[] PRESETS = {
        0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55,
        0xFF55FF55, 0xFF55FFFF, 0xFF5555FF,
        0xFFFF55FF, 0xFFFFFFFF, 0xFF222222
    };
    private final Screen parent;
    private final IntConsumer setter;
    private int color;
    private float hue, saturation, brightness;
    private boolean draggingField, draggingHue;
    private int panelX, panelY, panelWidth, panelHeight;
    private int fieldX, fieldY, fieldWidth = 118, fieldHeight = 82;
    private int hueX, hueY, hueWidth = 12, hueHeight = 82;
    private EditBox hexInput;
    private EzSlider alphaSlider;
    private boolean updatingHexInternally = false;

    public ModuleColorScreen(Screen parent, String label, int color, IntConsumer setter) {
        super(Component.literal(label));
        this.parent = parent; this.color = color; this.setter = setter;
        float[] hsv = Color.RGBtoHSB((color >>> 16) & 255, (color >>> 8) & 255, color & 255, null);
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2];
    }

    @Override protected void init() {
        panelWidth = Math.min(ScrollingSettingsScreen.SETTINGS_WIDTH, Math.max(120, width - 48));
        panelHeight = Math.min(ScrollingSettingsScreen.SETTINGS_HEIGHT, Math.max(100, height - 48));
        panelX = (width - panelWidth) / 2; panelY = (height - panelHeight) / 2;
        fieldX = panelX + ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH + 8; fieldY = panelY + 36;
        hueX = fieldX + fieldWidth + 8; hueY = fieldY;
        int alphaY = panelY + 124;
        int contentWidth = panelWidth - ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH - 16;
        alphaSlider = new EzSlider(fieldX, alphaY, contentWidth, 18,
            ((color >>> 24) & 255) / 255.0,
            value -> {
                color = (color & 0x00FFFFFF) | ((int)Math.round(value * 255) << 24);
                updateHexText();
            },
            value -> Component.literal("Transparenz: " + Math.round(value * 100) + "%"), true);
        addRenderableWidget(alphaSlider);

        int hexY = panelY + 146;
        hexInput = new EditBox(font, fieldX + 32, hexY, 84, 16, Component.literal("Hex"));
        hexInput.setMaxLength(9);
        hexInput.setResponder(this::onHexInputChanged);
        addRenderableWidget(hexInput);
        updateHexText();

        int footerY = panelY + panelHeight - 25, half = (contentWidth - 6) / 2;
        addRenderableWidget(new EzButton(fieldX, footerY, half, 18, Component.literal("Übernehmen"), true,
            button -> { setter.accept(color); onClose(); }));
        addRenderableWidget(new EzButton(fieldX + half + 6, footerY, half, 18, Component.literal("Abbrechen"), false,
            button -> onClose()));
    }

    private void updateHexText() {
        if (hexInput == null) return;
        updatingHexInternally = true;
        int alpha = (color >>> 24) & 255;
        if (alpha == 255) {
            hexInput.setValue(String.format("#%06X", color & 0x00FFFFFF));
        } else {
            hexInput.setValue(String.format("#%08X", color));
        }
        updatingHexInternally = false;
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
        hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2];
        if (alphaSlider != null) {
            alphaSlider.setSliderValue(((color >>> 24) & 255) / 255.0);
        }
    }

    private void updateField(double mouseX, double mouseY) {
        saturation = (float)Math.max(0, Math.min(1, (mouseX - fieldX) / fieldWidth));
        brightness = (float)Math.max(0, Math.min(1, 1 - (mouseY - fieldY) / fieldHeight));
        updateRgb();
    }
    private void updateHue(double mouseY) {
        hue = (float)Math.max(0, Math.min(1, (mouseY - hueY) / hueHeight));
        updateRgb();
    }
    private void updateRgb() {
        color = (color & 0xFF000000) | (Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF);
        updateHexText();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && event.x() >= fieldX && event.x() <= fieldX + fieldWidth
                && event.y() >= fieldY && event.y() <= fieldY + fieldHeight) {
            draggingField = true; updateField(event.x(), event.y()); return true;
        }
        if (event.button() == 0 && event.x() >= hueX && event.x() <= hueX + hueWidth
                && event.y() >= hueY && event.y() <= hueY + hueHeight) {
            draggingHue = true; updateHue(event.y()); return true;
        }
        int presetX = hueX + hueWidth + 8, presetY = fieldY + 5;
        for (int i = 0; i < PRESETS.length; i++) {
            int x = presetX + (i % 3) * 21, y = presetY + (i / 3) * 24;
            if (event.button() == 0 && event.x() >= x && event.x() <= x + 17 && event.y() >= y && event.y() <= y + 18) {
                int alpha = color & 0xFF000000;
                color = alpha | (PRESETS[i] & 0x00FFFFFF);
                float[] hsv = Color.RGBtoHSB((color >>> 16) & 255, (color >>> 8) & 255, color & 255, null);
                hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2];
                updateHexText();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingField) { updateField(event.x(), event.y()); return true; }
        if (draggingHue) { updateHue(event.y()); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        draggingField = false; draggingHue = false; return super.mouseReleased(event);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height); EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);
        renderSidebar(g);
        g.centeredText(font, getTitle(), fieldX + (panelWidth - ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH - 16) / 2, panelY + 12, 0xFFFFFFFF);
        for (int py = 0; py < fieldHeight; py += 2) {
            float value = 1.0f - (float)py / fieldHeight;
            for (int px = 0; px < fieldWidth; px += 2) {
                float sat = (float)px / fieldWidth;
                int rgb = 0xFF000000 | (Color.HSBtoRGB(hue, sat, value) & 0x00FFFFFF);
                g.fill(fieldX + px, fieldY + py, fieldX + px + 2, fieldY + py + 2, rgb);
            }
        }
        g.outline(fieldX - 1, fieldY - 1, fieldWidth + 2, fieldHeight + 2, EzUi.BORDER_SUBTLE);
        int cursorX = fieldX + Math.round(saturation * fieldWidth);
        int cursorY = fieldY + Math.round((1 - brightness) * fieldHeight);
        g.outline(cursorX - 3, cursorY - 3, 7, 7, 0xFFFFFFFF);
        g.outline(cursorX - 4, cursorY - 4, 9, 9, 0xFF000000);
        for (int py = 0; py < hueHeight; py += 2) {
            int rgb = 0xFF000000 | (Color.HSBtoRGB((float)py / hueHeight, 1, 1) & 0x00FFFFFF);
            g.fill(hueX, hueY + py, hueX + hueWidth, hueY + py + 2, rgb);
        }
        int hueMarker = hueY + Math.round(hue * hueHeight);
        g.fill(hueX - 2, hueMarker - 1, hueX + hueWidth + 2, hueMarker + 2, 0xFFFFFFFF);
        g.outline(hueX - 1, hueY - 1, hueWidth + 2, hueHeight + 2, EzUi.BORDER_SUBTLE);
        int presetX = hueX + hueWidth + 8, presetY = fieldY + 5;
        for (int i = 0; i < PRESETS.length; i++) {
            int x = presetX + (i % 3) * 21, y = presetY + (i / 3) * 24;
            EzUi.roundedRect(g, x, y, 17, 18, 2, PRESETS[i]);
            if ((color & 0x00FFFFFF) == (PRESETS[i] & 0x00FFFFFF)) g.outline(x - 1, y - 1, 19, 20, 0xFFFFFFFF);
        }
        int previewX = presetX, previewY = panelY + 104;
        int previewWidth = Math.max(40, panelX + panelWidth - 16 - previewX);
        for (int x = 0; x < previewWidth; x += 8) {
            g.fill(previewX + x, previewY, Math.min(previewX + x + 8, previewX + previewWidth), previewY + 18,
                (x / 8) % 2 == 0 ? 0xFFE5E7EB : 0xFF6B7280);
        }
        g.fill(previewX, previewY, previewX + previewWidth, previewY + 18, color);
        g.outline(previewX, previewY, previewWidth, 18, EzUi.BORDER_SUBTLE);
        g.text(font, Component.literal("Hex:"), fieldX + 4, panelY + 150, 0xFF94A3B8);
        super.extractRenderState(g, mx, my, delta);
        boolean pickerHover = mx >= fieldX && mx <= hueX + hueWidth && my >= fieldY && my <= fieldY + fieldHeight;
        boolean widgetHover = children().stream().filter(child -> child instanceof net.minecraft.client.gui.components.AbstractWidget)
                .map(child -> (net.minecraft.client.gui.components.AbstractWidget)child)
                .anyMatch(widget -> widget.active && widget.visible && mx >= widget.getX() && mx < widget.getX() + widget.getWidth()
                        && my >= widget.getY() && my < widget.getY() + widget.getHeight());
        EzCursor.setPointer(pickerHover || widgetHover);
    }
    private void renderSidebar(GuiGraphicsExtractor g) {
        int sidebar = ScrollingSettingsScreen.SETTINGS_SIDEBAR_WIDTH;
        g.fill(panelX + sidebar, panelY + 6, panelX + sidebar + 1, panelY + panelHeight - 6, EzUi.BORDER_SUBTLE);
        int logoX = panelX + 26, logoY = panelY + 9;
        EzUi.roundedRect(g, logoX, logoY, 20, 20, 3, 0xFF15181C);
        ModuleIconRenderer.drawTexture(g, ScrollingSettingsScreen.SETTINGS_LOGO, logoX + 2, logoY + 2, 16);
        EzUi.roundedRect(g, panelX + 6, panelY + 43, sidebar - 12, 18, 2, EzUi.BG_CARD_ACTIVE);
        g.centeredText(font, Component.literal("Farbe"), panelX + sidebar / 2, panelY + 48, EzUi.TEXT_LIGHT);
    }
    @Override public void removed() { EzCursor.setPointer(false); super.removed(); }
    @Override public void onClose() { EzScreenBridge.set(minecraft, parent); }
}
