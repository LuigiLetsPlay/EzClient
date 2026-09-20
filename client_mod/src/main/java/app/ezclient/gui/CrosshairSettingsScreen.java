package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Unified, modern ScrollingSettingsScreen for Custom Crosshair.
 * Features actions (pixel canvas, target rules), presets, geometry sliders,
 * colors with live swatches, and spread behavior toggles.
 */
public final class CrosshairSettingsScreen extends ScrollingSettingsScreen {
    private final Screen parent;
    private final CrosshairModule module;
    private boolean isListeningForHotkey = false;

    private int panelX, panelY, panelWidth, panelHeight;

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 34; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 32; }

    public CrosshairSettingsScreen(Screen parent, CrosshairModule module) {
        super(Component.literal("Custom Crosshair " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()));
        this.parent = parent;
        this.module = module;
    }

    private <T extends AbstractWidget> T described(T widget, String description) {
        if (description != null && !description.isBlank()) {
            widget.setTooltip(Tooltip.create(Component.literal(app.ezclient.util.EzI18n.text(description))));
        }
        return widget;
    }

    private void resetModule() {
        module.resetSettings();
        ConfigManager.save();
        rebuildWidgets();
    }

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

        // Header module enable toggle switch
        addFixedWidget(described(new EzToggleSwitch(
                panelX + panelWidth - 56, panelY + 7, 24, 14,
                module.isEnabled(),
                state -> {
                    module.setEnabled(state);
                    ConfigManager.save();
                }
        ), "Modul aktivieren / deaktivieren"));

        // Sidebar preview button
        int sidebarY = panelY + 102;
        if (module.hasPreview()) {
            addFixedWidget(new EzButton(
                    panelX + 6, sidebarY, SETTINGS_SIDEBAR_WIDTH - 12, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Vorschau")), false,
                    ignored -> EzScreenBridge.set(minecraft, new ModulePreviewScreen(this, module))
            ));
        }

        // Sidebar Hotkey button
        addFixedWidget(new EzHotkeyButton(panelX + 6, panelY + 66,
                SETTINGS_SIDEBAR_WIDTH - 12, module.getKeyBind(), isListeningForHotkey,
                () -> { isListeningForHotkey = !isListeningForHotkey; rebuildWidgets(); }));

        // Footer buttons
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX), panelY + panelHeight - 24, 100, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Zurücksetzen")), false,
                b -> resetModule()
        ));
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX) + 108, panelY + panelHeight - 24, 100, 16,
                app.ezclient.util.EzI18n.comp("ezclient.module_settings.done"), true,
                b -> onClose()
        ));

        int curY = panelY + 38;
        int fullW = settingsContentWidth(panelWidth);
        int btnW = (fullW - 6) / 2;
        int col1X = settingsContentLeft(panelX);
        int col2X = col1X + btnW + 6;

        // ════════════════════════════════════════
        // 1. AKTIONEN
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("AKTIONEN")));
        curY += 16;

        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW, 18,
                Component.literal("✦ Pixel-Editor öffnen …"), false,
                b -> EzScreenBridge.set(minecraft, new CrosshairPaintScreen(this, module))
        ), "Öffnet das interaktive 21x21 Pixel-Raster zum freien Zeichnen des Fadenkreuzes"));
        curY += 22;

        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW, 18,
                Component.literal("✦ Ziel-Regeln (Entity & Block) …"), false,
                b -> EzScreenBridge.set(minecraft, new CrosshairTargetSettingsScreen(this, module))
        ), "Farben und Skalierung ändern, wenn ein Spieler, Monster oder Block anvisiert wird"));
        curY += 26;

        // ════════════════════════════════════════
        // 2. FADENKREUZ-STIL
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("FADENKREUZ-STIL")));
        curY += 16;

        // Preset & Type
        addRenderableWidget(described(new EzButton(
                col1X, curY, btnW, 18,
                Component.literal("Preset: " + module.getPresetLabel()), true,
                b -> {
                    module.cyclePreset(1);
                    rebuildWidgets();
                }
        ), "Vorgefertigte Layouts (Klassisch, Plus, Punkt, Kreis, Custom)"));

        addRenderableWidget(described(new EzButton(
                col2X, curY, btnW, 18,
                Component.literal("Typ: " + module.getCrosshairTypeLabel()), true,
                b -> {
                    module.cycleCrosshairType(1);
                    rebuildWidgets();
                }
        ), "Grundform des Fadenkreuzes (Kreuz, Punkt, Kreis, T-Form, Chevron)"));
        curY += 22;

        // Size & Thickness Sliders
        addRenderableWidget(described(new EzSlider(
                col1X, curY, btnW, 18,
                (module.getSize() - 2.0) / 30.0,
                v -> module.setSize((int) Math.round(2 + v * 30)),
                v -> Component.literal(String.format(Locale.ROOT, "Größe: %d", (int) Math.round(2 + v * 30))),
                true, ConfigManager::save
        ), "Länge der Fadenkreuz-Linien"));

        addRenderableWidget(described(new EzSlider(
                col2X, curY, btnW, 18,
                (module.getThickness() - 1.0) / 7.0,
                v -> module.setThickness((int) Math.round(1 + v * 7)),
                v -> Component.literal(String.format(Locale.ROOT, "Dicke: %d", (int) Math.round(1 + v * 7))),
                true, ConfigManager::save
        ), "Strichstärke der Fadenkreuz-Linien"));
        curY += 22;

        // Gap & Dot Size Sliders
        addRenderableWidget(described(new EzSlider(
                col1X, curY, btnW, 18,
                module.getGap() / 15.0,
                v -> module.setGap((int) Math.round(v * 15)),
                v -> Component.literal(String.format(Locale.ROOT, "Abstand: %d", (int) Math.round(v * 15))),
                true, ConfigManager::save
        ), "Abstand der Linien zum Mittelpunkt"));

        addRenderableWidget(described(new EzSlider(
                col2X, curY, btnW, 18,
                (module.getDotSize() - 1.0) / 11.0,
                v -> module.setDotSize((int) Math.round(1 + v * 11)),
                v -> Component.literal(String.format(Locale.ROOT, "Punkt: %d", (int) Math.round(1 + v * 11))),
                true, ConfigManager::save
        ), "Größe des zentralen Mittelpunkts"));
        curY += 22;

        // Dot & Outline Toggles
        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, btnW, 18,
                Component.literal("Mittelpunkt"),
                module.isShowDot(),
                state -> {
                    module.setShowDot(state);
                    ConfigManager.save();
                }
        ), "Zeigt einen Punkt exakt in der Bildschirmmitte an"));

        addRenderableWidget(described(new EzToggleSwitch(
                col2X, curY, btnW, 18,
                Component.literal("Umrandung"),
                module.isShowOutline(),
                state -> {
                    module.setShowOutline(state);
                    ConfigManager.save();
                }
        ), "Zeigt eine dunkle Kontur um das Fadenkreuz für besseren Kontrast"));
        curY += 26;

        // ════════════════════════════════════════
        // 3. FARBEN & EFFEKTE
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("FARBEN & EFFEKTE")));
        curY += 16;

        // Color mode
        String colorModeLabel = switch (module.getColorMode()) {
            case SOLID -> "Einfarbig";
            case WAVE -> "Welle";
            case RAINBOW -> "Regenbogen";
        };
        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW, 18,
                Component.literal("Farbmodus: " + colorModeLabel), true,
                b -> {
                    HudModule.ColorMode[] modes = HudModule.ColorMode.values();
                    int next = (module.getColorMode().ordinal() + 1) % modes.length;
                    module.setColorMode(modes[next]);
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ), "Schaltet zwischen Einzelfarbe, 2-Farben-Wellenanimation und Chroma-Regenbogen um"));
        curY += 22;

        // Main Crosshair Color + Swatch
        int swatchW = 24;
        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW - swatchW - 4, 18,
                Component.literal(module.getColorMode() == HudModule.ColorMode.WAVE ? "Welle Farbe 1" : "Fadenkreuz-Farbe"), false,
                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Fadenkreuz-Farbe", module.getTextColor(), c -> {
                    module.setTextColor(c);
                    ConfigManager.save();
                }))
        ), "Öffnet den Farbwähler für die Hauptfarbe des Fadenkreuzes"));

        addRenderableWidget(new ColorSwatchButton(
                col1X + fullW - swatchW, curY, swatchW, 18,
                module.getTextColor(),
                b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Fadenkreuz-Farbe", module.getTextColor(), c -> {
                    module.setTextColor(c);
                    ConfigManager.save();
                }))
        ));
        curY += 22;

        // Wave Color 2 (if in WAVE mode)
        if (module.getColorMode() == HudModule.ColorMode.WAVE) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW - swatchW - 4, 18,
                    Component.literal("Welle Farbe 2"), false,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Welle Farbe 2", module.getWaveColor2(), c -> {
                        module.setWaveColor2(c);
                        ConfigManager.save();
                    }))
            ), "Zweite Farbe für den fließenden Farbverlauf im Wellen-Modus"));

            addRenderableWidget(new ColorSwatchButton(
                    col1X + fullW - swatchW, curY, swatchW, 18,
                    module.getWaveColor2(),
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Welle Farbe 2", module.getWaveColor2(), c -> {
                        module.setWaveColor2(c);
                        ConfigManager.save();
                    }))
            ));
            curY += 22;
        }

        // Outline Color + Swatch
        if (module.isShowOutline()) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW - swatchW - 4, 18,
                    Component.literal("Umrandungsfarbe"), false,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Umrandungsfarbe", module.getOutlineColor(), c -> {
                        module.setOutlineColor(c);
                        ConfigManager.save();
                    }))
            ), "Farbe der Fadenkreuz-Kontur"));

            addRenderableWidget(new ColorSwatchButton(
                    col1X + fullW - swatchW, curY, swatchW, 18,
                    module.getOutlineColor(),
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Umrandungsfarbe", module.getOutlineColor(), c -> {
                        module.setOutlineColor(c);
                        ConfigManager.save();
                    }))
            ));
            curY += 22;
        }
        curY += 4;

        // ════════════════════════════════════════
        // 4. DYNAMIK & VERHALTEN
        // ════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("DYNAMIK & VERHALTEN")));
        curY += 16;

        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, btnW, 18,
                Component.literal("Bewegungs-Spreizung"),
                module.isMovementSpread(),
                state -> {
                    module.setMovementSpread(state);
                    ConfigManager.save();
                }
        ), "Fadenkreuz weitet sich dynamisch beim Gehen oder Sprinten auf"));

        addRenderableWidget(described(new EzToggleSwitch(
                col2X, curY, btnW, 18,
                Component.literal("Sprung-Spreizung"),
                module.isJumpSpread(),
                state -> {
                    module.setJumpSpread(state);
                    ConfigManager.save();
                }
        ), "Fadenkreuz weitet sich in der Luft beim Springen oder Fallen auf"));
        curY += 22;

        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, btnW, 18,
                Component.literal("Cooldown-Spreizung"),
                module.isCooldownSpread(),
                state -> {
                    module.setCooldownSpread(state);
                    ConfigManager.save();
                }
        ), "Fadenkreuz spreizt sich während der Angriffserholung der Waffe"));

        addRenderableWidget(described(new EzToggleSwitch(
                col2X, curY, btnW, 18,
                Component.literal("Bei Bogen-Zoom ausblenden"),
                module.isHideOnBowZoom(),
                state -> {
                    module.setHideOnBowZoom(state);
                    ConfigManager.save();
                }
        ), "Versteckt das Fadenkreuz, während ein Bogen gespannt wird"));
        curY += 22;

        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, btnW, 18,
                Component.literal("In F3 ausblenden"),
                module.isHideInF3(),
                state -> {
                    module.setHideInF3(state);
                    ConfigManager.save();
                }
        ), "Versteckt das benutzerdefinierte Fadenkreuz im Debug-Bildschirm"));

        addRenderableWidget(described(new EzToggleSwitch(
                col2X, curY, btnW, 18,
                Component.literal("In 3rd Person ausblenden"),
                module.isHideInThirdPerson(),
                state -> {
                    module.setHideInThirdPerson(state);
                    ConfigManager.save();
                }
        ), "Versteckt das Fadenkreuz in der Außenansicht (F5)"));
        curY += 26;
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        // Sidebar
        renderSettingsSidebar(g, panelX, panelY, panelHeight, module.getCategory());

        // Header Title
        g.text(font, Component.literal(module.getDisplayName() + " " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()),
                settingsContentLeft(panelX), panelY + 10, EzUi.TEXT_WHITE);

        super.extractSettings(g, mx, my, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isListeningForHotkey) {
            EzKeyBindings.applyModuleKeyBind(module, event.key());
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected boolean settingsMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isListeningForHotkey && event.button() != 0) {
            EzKeyBindings.applyModuleKeyBind(module, -100 - event.button());
            isListeningForHotkey = false;
            rebuildWidgets();
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
