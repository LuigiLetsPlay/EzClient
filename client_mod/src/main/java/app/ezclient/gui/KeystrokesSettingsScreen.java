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
 * Modern, scrollable settings screen for KeystrokesModule organized into clean categories
 * (Actions, Layout & Keys, Animation & Behavior, Design & Spacing, Colors & Chroma).
 * Extends ScrollingSettingsScreen for unified visual presentation across EzClient.
 */
public final class KeystrokesSettingsScreen extends ScrollingSettingsScreen {
    private final Screen parent;
    private final KeystrokesModule module;
    private boolean isListeningForHotkey = false;

    private int panelX, panelY, panelWidth, panelHeight;

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 34; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 32; }

    public KeystrokesSettingsScreen(Screen parent, KeystrokesModule module) {
        super(Component.literal("Keystrokes " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()));
        this.parent = parent;
        this.module = module;
    }

    private <T extends AbstractWidget> T described(T widget, String description) {
        if (description != null && !description.isBlank()) {
            widget.setTooltip(Tooltip.create(Component.literal(description)));
        }
        return widget;
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
        ), "Keystrokes aktivieren / deaktivieren"));

        // Fixed sidebar Hotkey button
        int sidebarY = panelY + 66;
        String hotkeyLabel;
        if (isListeningForHotkey) {
            hotkeyLabel = "Taste: …";
        } else if (module.getKeyBind() > 0 || module.getKeyBind() <= -100) {
            hotkeyLabel = "Key: " + EzKeyBindings.getKeyOrMouseName(module.getKeyBind());
        } else {
            hotkeyLabel = "Taste: Keine";
        }
        addFixedWidget(described(new EzButton(
                panelX + 6, sidebarY, SETTINGS_SIDEBAR_WIDTH - 12, 18,
                Component.literal(hotkeyLabel), isListeningForHotkey,
                b -> { isListeningForHotkey = !isListeningForHotkey; rebuildWidgets(); }
        ), "Tastenkombination / Hotkey für Keystrokes festlegen (ESC zum Löschen)"));

        // Fixed bottom footer buttons
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX), panelY + panelHeight - 24, 100, 16,
                Component.literal("Zurücksetzen"), false,
                b -> {
                    module.resetSettings();
                    ConfigManager.save();
                    rebuildWidgets();
                }
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

        // ── AKTIONEN ──
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("AKTIONEN")));
        curY += 16;

        addRenderableWidget(described(new EzButton(
                col1X, curY, fullW, 20,
                Component.literal("✦ Keystrokes Designer V2 öffnen …"), true,
                b -> EzScreenBridge.set(minecraft, new KeystrokesDesignerScreen(this, module))
        ), "Öffnet den interaktiven Drag-and-Drop Designer für pixelgenaue Tastenanordnung."));
        curY += 26;

        // ── LAYOUT & TASTEN ──
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("LAYOUT & TASTEN")));
        curY += 16;

        // Layout preset cycler
        EzButton layoutBtn = new EzButton(col1X, curY, btnW, 16,
                Component.literal("‹ Layout: " + module.getLayoutPreset().name() + " ›"), true,
                b -> {
                    KeystrokesModule.LayoutPreset[] presets = KeystrokesModule.LayoutPreset.values();
                    int next = (module.getLayoutPreset().ordinal() + 1) % presets.length;
                    module.setLayoutPreset(presets[next]);
                    rebuildWidgets();
                });
        layoutBtn.withRightClick(b -> {
            KeystrokesModule.LayoutPreset[] presets = KeystrokesModule.LayoutPreset.values();
            int prev = (module.getLayoutPreset().ordinal() - 1 + presets.length) % presets.length;
            module.setLayoutPreset(presets[prev]);
            rebuildWidgets();
        });
        addRenderableWidget(described(layoutBtn, "Vordefiniertes Tasten-Layout auswählen (WASD, Maus, Leertaste, etc.)."));

        // Spacebar style cycler
        EzButton spaceBtn = new EzButton(col2X, curY, btnW, 16,
                Component.literal("‹ Leertaste: " + module.getSpaceStyle().name() + " ›"), true,
                b -> {
                    KeystrokesModule.SpaceStyle[] styles = KeystrokesModule.SpaceStyle.values();
                    int next = (module.getSpaceStyle().ordinal() + 1) % styles.length;
                    module.setSpaceStyle(styles[next]);
                    rebuildWidgets();
                });
        spaceBtn.withRightClick(b -> {
            KeystrokesModule.SpaceStyle[] styles = KeystrokesModule.SpaceStyle.values();
            int prev = (module.getSpaceStyle().ordinal() - 1 + styles.length) % styles.length;
            module.setSpaceStyle(styles[prev]);
            rebuildWidgets();
        });
        addRenderableWidget(described(spaceBtn, "Darstellungsstil der Leertaste (Linie, Block oder Text)."));
        curY += 20;

        // Toggle Key labels & Toggle CPS
        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, btnW, 16,
                Component.literal("Tasten-Text"), module.isShowKeyLabels(),
                state -> { module.setShowKeyLabels(state); rebuildWidgets(); }
        ), "Zeigt oder versteckt die Textbeschriftung auf den Tasten."));

        addRenderableWidget(described(new EzToggleSwitch(
                col2X, curY, btnW, 16,
                Component.literal("CPS anzeigen"), module.isShowMouseCps(),
                state -> { module.setShowMouseCps(state); rebuildWidgets(); }
        ), "Zeigt die aktuellen Klicks pro Sekunde auf Maustasten an."));
        curY += 20;

        // Sliders: Key size & Key spacing
        double sizeNorm = (module.getKeySize() - 14.0) / (36.0 - 14.0);
        addRenderableWidget(described(new EzSlider(
                col1X, curY, fullW, 18, sizeNorm,
                v -> module.setKeySize((int) Math.round(14.0 + v * 22.0)),
                v -> Component.literal("Tastengröße: " + (int) Math.round(14.0 + v * 22.0) + " px"),
                true, ConfigManager::save
        ), "Breite und Höhe der Standard-Tasten."));
        curY += 22;

        double spacingNorm = module.getKeySpacing() / 8.0;
        addRenderableWidget(described(new EzSlider(
                col1X, curY, fullW, 18, spacingNorm,
                v -> module.setKeySpacing((int) Math.round(v * 8.0)),
                v -> Component.literal("Tastenabstand: " + (int) Math.round(v * 8.0) + " px"),
                true, ConfigManager::save
        ), "Abstand zwischen den einzelnen Tastenfeldern."));
        curY += 26;

        // ── ANIMATION & EFFEKTE ──
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("ANIMATION & EFFEKTE")));
        curY += 16;

        addRenderableWidget(described(new EzToggleSwitch(
                col1X, curY, btnW, 16,
                Component.literal("Animationen"), module.isAnimationsEnabled(),
                state -> { module.setAnimationsEnabled(state); rebuildWidgets(); }
        ), "Schaltet Tasten-Druckanimationen und Sanftes Verblassen ein/aus."));

        EzButton animBtn = new EzButton(col2X, curY, btnW, 16,
                Component.literal("‹ Typ: " + module.getPressAnimation().name() + " ›"), true,
                b -> {
                    KeystrokesModule.PressAnimation[] anims = KeystrokesModule.PressAnimation.values();
                    int next = (module.getPressAnimation().ordinal() + 1) % anims.length;
                    module.setPressAnimation(anims[next]);
                    rebuildWidgets();
                });
        animBtn.withRightClick(b -> {
            KeystrokesModule.PressAnimation[] anims = KeystrokesModule.PressAnimation.values();
            int prev = (module.getPressAnimation().ordinal() - 1 + anims.length) % anims.length;
            module.setPressAnimation(anims[prev]);
            rebuildWidgets();
        });
        addRenderableWidget(described(animBtn, "Animationskurve beim Loslassen der Tasten (Fade, Pulse, Instant)."));
        curY += 20;

        double fadeNorm = module.getFadeTimeMs() / 500.0;
        addRenderableWidget(described(new EzSlider(
                col1X, curY, fullW, 18, fadeNorm,
                v -> module.setFadeTimeMs((int) Math.round(v * 500.0)),
                v -> Component.literal("Fade-Dauer: " + (int) Math.round(v * 500.0) + " ms"),
                true, ConfigManager::save
        ), "Dauer des Ausfadens beim Loslassen einer Taste."));
        curY += 22;

        double fontScaleNorm = (module.getFontScale() - 0.5f) / 1.5f;
        addRenderableWidget(described(new EzSlider(
                col1X, curY, fullW, 18, fontScaleNorm,
                v -> module.setFontScale((float) (0.5 + v * 1.5)),
                v -> Component.literal(String.format(Locale.ROOT, "Schriftgröße: %.2fx", 0.5 + v * 1.5)),
                true, ConfigManager::save
        ), "Skalierung der Beschriftung auf den Tasten."));
        curY += 26;

        // ── DESIGN & RAHMEN ──
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("DESIGN & RAHMEN")));
        curY += 16;

        EzButton frameBtn = new EzButton(col1X, curY, btnW, 16,
                Component.literal("‹ Rahmen: " + module.getFrameMode().name() + " ›"), true,
                b -> {
                    KeystrokesModule.FrameMode[] modes = KeystrokesModule.FrameMode.values();
                    int next = (module.getFrameMode().ordinal() + 1) % modes.length;
                    module.setFrameMode(modes[next]);
                    rebuildWidgets();
                });
        frameBtn.withRightClick(b -> {
            KeystrokesModule.FrameMode[] modes = KeystrokesModule.FrameMode.values();
            int prev = (module.getFrameMode().ordinal() - 1 + modes.length) % modes.length;
            module.setFrameMode(modes[prev]);
            rebuildWidgets();
        });
        addRenderableWidget(described(frameBtn, "Zusammenhängender oder individueller Umrissrahmen um Tasten."));

        addRenderableWidget(described(new EzToggleSwitch(
                col2X, curY, btnW, 16,
                Component.literal("Tastenrahmen"), module.isShowKeyBorder(),
                state -> { module.setShowKeyBorder(state); rebuildWidgets(); }
        ), "Zeigt Konturen um jede einzelne Taste an."));
        curY += 20;

        double radiusNorm = module.getKeyCornerRadius() / 2.0;
        addRenderableWidget(described(new EzSlider(
                col1X, curY, fullW, 18, radiusNorm,
                v -> module.setKeyCornerRadius((int) Math.round(v * 2.0)),
                v -> Component.literal("Ecken-Radius: " + (int) Math.round(v * 2.0) + " px"),
                true, ConfigManager::save
        ), "Abrundung der Tastenecken."));
        curY += 22;

        double borderNorm = (module.getKeyBorderWidth() - 2.0) / 2.0;
        addRenderableWidget(described(new EzSlider(
                col1X, curY, fullW, 18, borderNorm,
                v -> module.setKeyBorderWidth((int) Math.round(2.0 + v * 2.0)),
                v -> Component.literal("Rahmendicke: " + (int) Math.round(2.0 + v * 2.0) + " px"),
                true, ConfigManager::save
        ), "Dicke der Umrandung."));
        curY += 26;

        // ── FARBEN & EFFEKTE ──
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal("FARBEN & EFFEKTE")));
        curY += 16;

        EzButton colorModeBtn = new EzButton(col1X, curY, btnW, 16,
                Component.literal("‹ Modus: " + module.getKeyColorMode().name() + " ›"), true,
                b -> {
                    KeystrokesModule.KeyColorMode[] modes = KeystrokesModule.KeyColorMode.values();
                    int next = (module.getKeyColorMode().ordinal() + 1) % modes.length;
                    module.setKeyColorMode(modes[next]);
                    rebuildWidgets();
                });
        colorModeBtn.withRightClick(b -> {
            KeystrokesModule.KeyColorMode[] modes = KeystrokesModule.KeyColorMode.values();
            int prev = (module.getKeyColorMode().ordinal() - 1 + modes.length) % modes.length;
            module.setKeyColorMode(modes[prev]);
            rebuildWidgets();
        });
        addRenderableWidget(described(colorModeBtn, "Farbeffekt (Einfarbig, Regenbogen oder Gradient / Welle)."));

        double speedNorm = (module.getColorCycleSpeed() - 0.2f) / 4.8f;
        addRenderableWidget(described(new EzSlider(
                col2X, curY, btnW, 18, speedNorm,
                v -> module.setColorCycleSpeed((float) (0.2 + v * 4.8)),
                v -> Component.literal(String.format(Locale.ROOT, "Speed: %.1fx", 0.2 + v * 4.8)),
                true, ConfigManager::save
        ), "Geschwindigkeit der Regenbogen- und Welleneffekte."));
        curY += 22;

        // Color swatches
        addColorOption(col1X, curY, btnW, "Tasten-Hintergrund", KeystrokesModule.ColorTarget.NORMAL_BOX);
        addColorOption(col2X, curY, btnW, "Gedrückte Taste", KeystrokesModule.ColorTarget.PRESSED_BOX);
        curY += 20;

        addColorOption(col1X, curY, btnW, "Tastentext", KeystrokesModule.ColorTarget.KEY_TEXT);
        addColorOption(col2X, curY, btnW, "Gedrückter Text", KeystrokesModule.ColorTarget.PRESSED_TEXT);
        curY += 20;

        addColorOption(col1X, curY, btnW, "Rahmenfarbe", KeystrokesModule.ColorTarget.BORDER);
        addColorOption(col2X, curY, btnW, "Gradient / Welle", KeystrokesModule.ColorTarget.GRADIENT);
        curY += 24;
    }

    private void addColorOption(int x, int y, int width, String label, KeystrokesModule.ColorTarget target) {
        int swatchW = 20;
        int btnW = width - swatchW - 3;
        int color = module.getColor(target);

        EzButton btn = new EzButton(x, y, btnW, 16, Component.literal(label + " …"), true,
                b -> openColorPicker(label, target));
        ColorSwatchButton swatch = new ColorSwatchButton(x + btnW + 3, y, swatchW, 16, color,
                b -> openColorPicker(label, target));

        addRenderableWidget(described(btn, "Wählt Farbe und Deckkraft für „" + label + "“."));
        addRenderableWidget(described(swatch, "Aktuelle Farbe für „" + label + "“."));
    }

    private void openColorPicker(String label, KeystrokesModule.ColorTarget target) {
        int currentColor = module.getColor(target);
        EzScreenBridge.set(minecraft, new ModuleColorScreen(this, label, currentColor, newColor -> {
            module.setColor(target, newColor);
            ConfigManager.save();
        }));
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float d) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        g.text(font, Component.literal("Keystrokes " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()),
                settingsContentLeft(panelX), panelY + 10, EzUi.TEXT_WHITE);

        g.fill(settingsContentLeft(panelX), panelY + 28, panelX + panelWidth - 8, panelY + 29, EzUi.BORDER_SUBTLE);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, "Keystrokes");

        super.extractSettings(g, mx, my, d);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            EzScreenBridge.set(minecraft, parent);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isListeningForHotkey) {
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE || event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
                EzKeyBindings.applyModuleKeyBind(module, -1);
            } else {
                EzKeyBindings.applyModuleKeyBind(module, event.key());
            }
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

    private static final class CategoryHeader extends AbstractWidget {
        public CategoryHeader(int x, int y, int width, int height, Component title) {
            super(x, y, width, height, title);
            this.active = false;
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            var font = Minecraft.getInstance().font;
            Component message = getMessage();
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
        private int color;
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
            g.outline(getX(), getY(), getWidth(), getHeight(), EzUi.BORDER_SUBTLE);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {}
    }
}
