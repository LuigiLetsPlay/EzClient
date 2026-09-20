package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modern, unified OverallHudSettingsScreen extending ScrollingSettingsScreen.
 * Configures global background boxes, borders, styles, drop shadows, and text colors
 * across all HUD modules at once using the unified EzClient design system and ModuleColorScreen.
 */
public final class OverallHudSettingsScreen extends ScrollingSettingsScreen {
    private final Screen parent;
    private int panelX, panelY, panelWidth, panelHeight;

    // Global states mirrored across all HUD modules
    private boolean globalBox = false;
    private int globalBoxColor = 0x80000000;
    private boolean globalBorder = false;
    private HudModule.BorderStyle globalBorderStyle = HudModule.BorderStyle.PIXEL;
    private HudModule.ColorMode globalBorderColorMode = HudModule.ColorMode.SOLID;
    private int globalBorderColor = 0xFF22C96E;
    private int globalBorderWave2 = 0xFF00D2FF;

    private boolean globalShadow = true;
    private HudModule.ColorMode globalTextColorMode = HudModule.ColorMode.SOLID;
    private int globalTextColor = 0xFFFFFFFF;
    private int globalTextWave2 = 0xFF43DD8C;

    private boolean showResetConfirmation = false;

    public OverallHudSettingsScreen(Screen parent) {
        super(Component.literal("Overall HUD " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()));
        this.parent = parent;

        // Sample initial state from first available HUD module
        var first = allHudModules().stream().findFirst().orElse(null);
        if (first != null) {
            this.globalBox = first.hasBackground();
            this.globalBoxColor = first.getBackgroundColor();
            this.globalBorder = first.hasBorder();
            this.globalBorderStyle = first.getBorderStyle();
            this.globalBorderColorMode = first.getBorderColorMode();
            this.globalBorderColor = first.getBorderColor();
            this.globalBorderWave2 = first.getWaveColor2();
            this.globalShadow = first.isTextShadow();
            this.globalTextColorMode = first.getColorMode();
            this.globalTextColor = first.getTextColor();
            this.globalTextWave2 = first.getWaveColor2();
        }
    }

    private List<HudModule> allHudModules() {
        return ModuleManager.getInstance().getHudModules();
    }

    private <T extends AbstractWidget> T described(T widget, String description) {
        if (description != null && !description.isBlank()) {
            widget.setTooltip(Tooltip.create(Component.literal(app.ezclient.util.EzI18n.text(description))));
        }
        return widget;
    }

    private void applyAll() {
        for (HudModule m : allHudModules()) {
            m.setBackground(globalBox);
            m.setBackgroundColor(globalBoxColor);
            m.setBorder(globalBorder);
            m.setBorderStyle(globalBorderStyle);
            m.setBorderColorMode(globalBorderColorMode);
            m.setBorderColor(globalBorderColor);
            m.setTextShadow(globalShadow);
            m.setColorMode(globalTextColorMode);
            m.setTextColor(globalTextColor);
            m.setWaveColor2(globalTextColorMode == HudModule.ColorMode.WAVE ? globalTextWave2 : globalBorderWave2);
        }
        ConfigManager.save();
    }

    private void resetAllToDefaults() {
        for (HudModule m : allHudModules()) {
            m.resetToDefaults();
        }
        ConfigManager.save();
        rebuildWidgets();
    }

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 34; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 32; }

    @Override
    protected void init() {
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        // Top-right close button ✕
        addFixedWidget(new EzButton(
                panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()
        ));

        // Footer buttons
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX), panelY + panelHeight - 24, 100, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Zurücksetzen")), false,
                b -> showResetConfirmation = true
        ));
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX) + 108, panelY + panelHeight - 24, 100, 16,
                app.ezclient.util.EzI18n.comp("ezclient.module_settings.done"), true,
                b -> {
                    applyAll();
                    onClose();
                }
        ));

        int curY = panelY + 38;
        int fullW = settingsContentWidth(panelWidth);
        int btnW = (fullW - 6) / 2;
        int col1X = settingsContentLeft(panelX);
        int col2X = col1X + btnW + 6;
        int swatchW = 24;

        // ════════════════════════════════════════
        // 1. BOX & RAHMEN (GLOBAL)
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("BOX & RAHMEN (GLOBAL)")));
        curY += 16;

        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, btnW, 18,
                Component.literal("Hintergrund-Box"),
                globalBox,
                state -> {
                    globalBox = state;
                    for (HudModule m : allHudModules()) m.setBackground(globalBox);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ), "Schaltet dunkle Hintergrundkisten für alle HUD-Module an oder aus"));

        // Background Color + Swatch
        addRenderableWidget(described(new EzButton(
                col2X, curY, btnW - swatchW - 4, 18,
                Component.literal("Box-Farbe"), false,
                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Globale Box-Farbe", globalBoxColor, c -> {
                    globalBoxColor = c;
                    for (HudModule m : allHudModules()) m.setBackgroundColor(c);
                    ConfigManager.save();
                }))
        ), "Globale Hintergrundfarbe aller HUD-Kisten"));

        addRenderableWidget(new ColorSwatchButton(
                col2X + btnW - swatchW, curY, swatchW, 18,
                globalBoxColor,
                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Globale Box-Farbe", globalBoxColor, c -> {
                    globalBoxColor = c;
                    for (HudModule m : allHudModules()) m.setBackgroundColor(c);
                    ConfigManager.save();
                }))
        ));
        curY += 22;

        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, btnW, 18,
                Component.literal("Rahmen"),
                globalBorder,
                state -> {
                    globalBorder = state;
                    for (HudModule m : allHudModules()) m.setBorder(globalBorder);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ), "Schaltet Rahmen um alle HUD-Module an oder aus"));

        addRenderableWidget(described(new EzButton(
                col2X, curY, btnW, 18,
                Component.literal("Stil: " + globalBorderStyle.getLabel()), true,
                b -> {
                    HudModule.BorderStyle[] styles = HudModule.BorderStyle.values();
                    int next = (globalBorderStyle.ordinal() + 1) % styles.length;
                    globalBorderStyle = styles[next];
                    for (HudModule m : allHudModules()) m.setBorderStyle(globalBorderStyle);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ), "Rahmenstil für alle HUD-Module (Pixel, Vanilla, Glass Neon, etc.)"));
        curY += 22;

        // Border Color Mode
        String borderModeLabel = switch (globalBorderColorMode) {
            case SOLID -> "Einfarbig";
            case WAVE -> "Welle";
            case RAINBOW -> "Regenbogen";
        };
        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW, 18,
                Component.literal("Rahmen-Farbmodus: " + borderModeLabel), true,
                b -> {
                    HudModule.ColorMode[] modes = HudModule.ColorMode.values();
                    int next = (globalBorderColorMode.ordinal() + 1) % modes.length;
                    globalBorderColorMode = modes[next];
                    for (HudModule m : allHudModules()) m.setBorderColorMode(globalBorderColorMode);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ), "Schaltet Rahmenfarben zwischen Einzelfarbe, Welle und Regenbogen um"));
        curY += 22;

        // Border Color 1
        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW - swatchW - 4, 18,
                Component.literal(globalBorderColorMode == HudModule.ColorMode.WAVE ? "Rahmen Welle Farbe 1" : "Rahmenfarbe"), false,
                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Rahmenfarbe", globalBorderColor, c -> {
                    globalBorderColor = c;
                    for (HudModule m : allHudModules()) m.setBorderColor(c);
                    ConfigManager.save();
                }))
        ), "Globale Rahmenfarbe aller HUD-Module"));

        addRenderableWidget(new ColorSwatchButton(
                col1X + fullW - swatchW, curY, swatchW, 18,
                globalBorderColor,
                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Rahmenfarbe", globalBorderColor, c -> {
                    globalBorderColor = c;
                    for (HudModule m : allHudModules()) m.setBorderColor(c);
                    ConfigManager.save();
                }))
        ));
        curY += 22;

        // Border Wave 2 (if WAVE)
        if (globalBorderColorMode == HudModule.ColorMode.WAVE) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW - swatchW - 4, 18,
                    Component.literal("Rahmen Welle Farbe 2"), false,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Rahmen Welle Farbe 2", globalBorderWave2, c -> {
                        globalBorderWave2 = c;
                        for (HudModule m : allHudModules()) m.setWaveColor2(c);
                        ConfigManager.save();
                    }))
            ), "Zweite Farbe für den fließenden Rahmen-Farbverlauf"));

            addRenderableWidget(new ColorSwatchButton(
                    col1X + fullW - swatchW, curY, swatchW, 18,
                    globalBorderWave2,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Rahmen Welle Farbe 2", globalBorderWave2, c -> {
                        globalBorderWave2 = c;
                        for (HudModule m : allHudModules()) m.setWaveColor2(c);
                        ConfigManager.save();
                    }))
            ));
            curY += 22;
        }
        curY += 4;

        // ════════════════════════════════════════
        // 2. TEXTFARBE & EFFEKTE (GLOBAL)
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("TEXTFARBE & EFFEKTE (GLOBAL)")));
        curY += 16;

        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, fullW, 18,
                Component.literal("Textschatten"),
                globalShadow,
                state -> {
                    globalShadow = state;
                    for (HudModule m : allHudModules()) m.setTextShadow(globalShadow);
                    ConfigManager.save();
                }
        ), "Schaltet den dezenten Text-Schlagschatten für alle HUDs an oder aus"));
        curY += 22;

        // Text Color Mode
        String textModeLabel = switch (globalTextColorMode) {
            case SOLID -> "Einfarbig";
            case WAVE -> "Welle";
            case RAINBOW -> "Regenbogen";
        };
        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW, 18,
                Component.literal("Text-Farbmodus: " + textModeLabel), true,
                b -> {
                    HudModule.ColorMode[] modes = HudModule.ColorMode.values();
                    int next = (globalTextColorMode.ordinal() + 1) % modes.length;
                    globalTextColorMode = modes[next];
                    for (HudModule m : allHudModules()) m.setColorMode(globalTextColorMode);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ), "Schaltet Textfarben zwischen Einzelfarbe, Welle und Regenbogen um"));
        curY += 22;

        // Text Color 1
        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW - swatchW - 4, 18,
                Component.literal(globalTextColorMode == HudModule.ColorMode.WAVE ? "Text Welle Farbe 1" : "Textfarbe"), false,
                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Textfarbe", globalTextColor, c -> {
                    globalTextColor = c;
                    for (HudModule m : allHudModules()) m.setTextColor(c);
                    ConfigManager.save();
                }))
        ), "Globale Textfarbe aller HUD-Module"));

        addRenderableWidget(new ColorSwatchButton(
                col1X + fullW - swatchW, curY, swatchW, 18,
                globalTextColor,
                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Textfarbe", globalTextColor, c -> {
                    globalTextColor = c;
                    for (HudModule m : allHudModules()) m.setTextColor(c);
                    ConfigManager.save();
                }))
        ));
        curY += 22;

        // Text Wave 2 (if WAVE)
        if (globalTextColorMode == HudModule.ColorMode.WAVE) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW - swatchW - 4, 18,
                    Component.literal("Text Welle Farbe 2"), false,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Text Welle Farbe 2", globalTextWave2, c -> {
                        globalTextWave2 = c;
                        for (HudModule m : allHudModules()) m.setWaveColor2(c);
                        ConfigManager.save();
                    }))
            ), "Zweite Farbe für den fließenden Text-Farbverlauf"));

            addRenderableWidget(new ColorSwatchButton(
                    col1X + fullW - swatchW, curY, swatchW, 18,
                    globalTextWave2,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Text Welle Farbe 2", globalTextWave2, c -> {
                        globalTextWave2 = c;
                        for (HudModule m : allHudModules()) m.setWaveColor2(c);
                        ConfigManager.save();
                    }))
            ));
            curY += 22;
        }
        curY += 4;

        // ════════════════════════════════════════
        // 3. AKTIONEN
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("AKTIONEN")));
        curY += 16;

        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW, 18,
                Component.literal("✦ Auf alle HUDs anwenden"), true,
                b -> {
                    applyAll();
                    b.setMessage(Component.literal("✓ Auf alle HUDs angewendet!"));
                }
        ), "Überträgt die obigen Einstellungen auf ausnahmslos alle aktiven HUD-Module"));
        curY += 26;
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        // Sidebar
        renderSettingsSidebar(g, panelX, panelY, panelHeight, "GLOBAL");

        // Header Title
        g.text(font, Component.literal("Overall HUD " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()),
                settingsContentLeft(panelX), panelY + 10, EzUi.TEXT_WHITE);

        super.extractSettings(g, mx, my, delta);

        if (showResetConfirmation) {
            EzUi.backdrop(g, width, height);
            int diaW = 240, diaH = 84;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;
            EzUi.panel(g, diaX, diaY, diaW, diaH);
            g.centeredText(font, Component.literal("Alle HUD-Styles zurücksetzen?"), diaX + diaW / 2, diaY + 18, 0xFFFFFFFF);
            g.centeredText(font, Component.literal("Dies setzt alle HUDs auf Standard!"), diaX + diaW / 2, diaY + 32, 0xFFA0A0A0);

            EzUi.roundedRect(g, diaX + 14, diaY + 52, 100, 20, 3, 0xFFCC3333);
            g.centeredText(font, Component.literal(app.ezclient.util.EzI18n.text("Zurücksetzen")), diaX + 64, diaY + 58, 0xFFFFFFFF);

            EzUi.roundedRect(g, diaX + 126, diaY + 52, 100, 20, 3, 0xFF2A3441);
            g.centeredText(font, Component.literal(app.ezclient.util.EzI18n.text("Abbrechen")), diaX + 176, diaY + 58, 0xFFFFFFFF);
        }
    }

    @Override
    protected boolean settingsMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (showResetConfirmation && event.button() == 0) {
            int diaW = 240, diaH = 84;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;
            double mx = event.x(), my = event.y();
            if (mx >= diaX + 14 && mx <= diaX + 114 && my >= diaY + 52 && my <= diaY + 72) {
                resetAllToDefaults();
                showResetConfirmation = false;
                return true;
            }
            if (mx >= diaX + 126 && mx <= diaX + 226 && my >= diaY + 52 && my <= diaY + 72) {
                showResetConfirmation = false;
                return true;
            }
            return true;
        }
        return super.settingsMouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        EzScreenBridge.set(minecraft, parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class CategoryHeader extends AbstractWidget {
        public CategoryHeader(int x, int y, int width, int height, Component title) {
            super(x, y, width, height, title);
            this.active = false;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            var font = Minecraft.getInstance().font;
            Component message = EzUi.fitText(getMessage(), getWidth());
            g.text(font, message, getX(), getY() + (getHeight() - 8) / 2, EzUi.TEXT_MUTED);
            int textW = font.width(message);
            int lineStartX = getX() + textW + 6;
            int lineEndX = getX() + getWidth();
            if (lineStartX < lineEndX) {
                int lineY = getY() + getHeight() / 2;
                g.fill(lineStartX, lineY, lineEndX, lineY + 1, EzUi.BORDER_SUBTLE);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }

    private static final class ColorSwatchButton extends AbstractButton {
        private final int color;
        private final Consumer<ColorSwatchButton> onClick;

        public ColorSwatchButton(int x, int y, int width, int height, int color, Consumer<ColorSwatchButton> onClick) {
            super(x, y, width, height, Component.empty());
            this.color = color;
            this.onClick = onClick;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            onClick.accept(this);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 3, 0xFF0D121D);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 2, color);
            EzUi.outline(g, getX(), getY(), getWidth(), getHeight(), isHovered() ? 0xFFFFFFFF : 0x40FFFFFF);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
