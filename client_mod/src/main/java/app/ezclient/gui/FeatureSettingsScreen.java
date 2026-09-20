package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Modern, scrollable 416x236 modal window for all FeatureModule instances
 * (Hitbox Visualizer, Block Overlay, Item Physics, Time & Weather, etc.).
 * Extends ScrollingSettingsScreen for unified visual presentation across EzClient.
 */
public final class FeatureSettingsScreen extends ScrollingSettingsScreen {
    private final Screen parent;
    private final FeatureModule module;
    private boolean isListeningForHotkey = false;
    private boolean advancedExpanded = false;

    private int panelX, panelY, panelWidth, panelHeight;

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 34; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 32; }

    public FeatureSettingsScreen(Screen parent, FeatureModule module) {
        super(Component.literal(module.getDisplayName() + " " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "").trim()));
        this.parent = parent;
        this.module = module;
    }

    private <T extends AbstractWidget> T described(T widget, String description) {
        if (description != null && !description.isBlank()) {
            widget.setTooltip(Tooltip.create(Component.literal(app.ezclient.util.EzI18n.text(description))));
        }
        return widget;
    }

    private void resetOpenModule() {
        module.resetSettings();
        ConfigManager.save();
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

        // Fixed sidebar preview button if module has live preview
        int sidebarY = panelY + 102;
        if (module.hasPreview()) {
            addFixedWidget(new EzButton(
                    panelX + 6, sidebarY, SETTINGS_SIDEBAR_WIDTH - 12, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Vorschau")), false,
                    ignored -> EzScreenBridge.set(minecraft, new ModulePreviewScreen(this, module))
            ));
            sidebarY += 22;
        }

        // Fixed sidebar Hotkey button
        addFixedWidget(new EzHotkeyButton(panelX + 6, panelY + 66,
                SETTINGS_SIDEBAR_WIDTH - 12, module.getKeyBind(), isListeningForHotkey,
                () -> { isListeningForHotkey = !isListeningForHotkey; rebuildWidgets(); }));

        // Fixed bottom footer buttons
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX), panelY + panelHeight - 24, 100, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Zurücksetzen")), false,
                b -> { resetOpenModule(); rebuildWidgets(); }
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

        // Dedicated sub-screen action buttons at the top of content (if applicable)
        if (module instanceof HitboxModule hitbox) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW, 18,
                    Component.literal("Custom Entity Settings …"), false,
                    b -> EzScreenBridge.set(minecraft, new EntityTypeSettingsScreen(this, hitbox))
            ), "Öffnet detaillierte Hitbox-Regeln für alle Entity-Typen"));
            curY += 22;
        } else if (module instanceof BlockOverlayModule blockOverlay) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW, 18,
                    Component.literal("Custom Block Settings …"), false,
                    b -> EzScreenBridge.set(minecraft, new BlockSettingsScreen(this, blockOverlay))
            ), "Öffnet individuelle Kontur- und Füllregeln pro Block"));
            curY += 22;
        } else if (module instanceof DamageTintModule damageTint) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW, 18,
                    Component.literal("Custom Entity Settings …"), false,
                    b -> EzScreenBridge.set(minecraft, new DamageTintEntityScreen(this, damageTint))
            ), "Öffnet individuelle Schadensfarben pro Entity-Typ"));
            curY += 22;
        } else if (module instanceof WaypointsModule waypoints) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Waypoint Manager öffnen …")), false,
                    b -> EzScreenBridge.set(minecraft, new WaypointScreen(this, waypoints))
            ), "Wegpunkte verwalten, bearbeiten und teleportieren"));
            curY += 22;
        } else if (module instanceof ParticleCustomizerModule particles) {
            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Partikel-Typen verwalten …")), false,
                    b -> EzScreenBridge.set(minecraft, new ParticleTypesScreen(this, particles))
            ), "Öffnet eine durchsuchbare Liste aller Minecraft-Partikeltypen"));
            curY += 22;

        }

        // Complex modules start with at most two approachable categories. Every
        // remaining setting stays available in one explicit Advanced section.
        List<String> categories = module.settingCategories();
        List<String> basicCategories = new ArrayList<>();
        List<String> advancedCategories = new ArrayList<>();
        boolean simplify = module.options().size() >= 6 && categories.size() > 1;
        for (String category : categories) {
            String normalized = category.toLowerCase(Locale.ROOT);
            boolean clearlyAdvanced = normalized.contains("erweit")
                    || normalized.contains("advanced")
                    || normalized.contains("fein")
                    || normalized.contains("performance")
                    || normalized.contains("system");
            if (simplify && !basicCategories.isEmpty() && (clearlyAdvanced || basicCategories.size() >= 2)) {
                advancedCategories.add(category);
            } else {
                basicCategories.add(category);
            }
        }

        List<String> orderedCategories = new ArrayList<>(basicCategories);
        orderedCategories.addAll(advancedCategories);
        for (int categoryIndex = 0; categoryIndex < orderedCategories.size(); categoryIndex++) {
            if (!advancedCategories.isEmpty() && categoryIndex == basicCategories.size()) {
                String advancedLabel = app.ezclient.util.EzI18n.text("Erweitert");
                addRenderableWidget(described(new EzButton(
                        col1X, curY, fullW, 18,
                        Component.literal((advancedExpanded ? "\u25BE " : "\u25B8 ") + advancedLabel
                                + " (" + advancedCategories.size() + ")"),
                        advancedExpanded,
                        b -> {
                            advancedExpanded = !advancedExpanded;
                            rebuildWidgets();
                        }
                ), "Zeigt alle zus\u00E4tzlichen Anpassungen dieses Moduls"));
                curY += 24;
                if (!advancedExpanded) break;
            }

            String cat = orderedCategories.get(categoryIndex);
            List<FeatureModule.Option> catOptions = module.options().stream()
                    .filter(o -> cat.equalsIgnoreCase(module.settingInfo(o.key()).category()))
                    .toList();
            if (catOptions.isEmpty()) continue;

            // Category Header with line
            addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14, Component.literal(app.ezclient.util.EzI18n.text(cat).toUpperCase(Locale.ROOT))));
            curY += 16;

            int col = 0;
            for (int i = 0; i < catOptions.size(); i++) {
                FeatureModule.Option opt = catOptions.get(i);
                Object val = module.setting(opt.key());
                boolean isWide = (val instanceof Number) || (!(val instanceof Boolean) && opt.choices().length == 0 && !isColorOption(opt, val));

                if (isWide) {
                    if (col == 1) {
                        curY += 20;
                        col = 0;
                    }
                    AbstractWidget widget = createWideWidget(col1X, curY, fullW, opt, val);
                    addRenderableWidget(described(widget, module.settingInfo(opt.key()).description()));
                    curY += 22;
                } else {
                    int x = (col == 0) ? col1X : col2X;
                    addCompactOptionWidgets(x, curY, btnW, opt, val);
                    if (col == 0) {
                        if (i + 1 < catOptions.size()) {
                            FeatureModule.Option nextOpt = catOptions.get(i + 1);
                            Object nextVal = module.setting(nextOpt.key());
                            boolean nextIsWide = (nextVal instanceof Number) || (!(nextVal instanceof Boolean) && nextOpt.choices().length == 0 && !isColorOption(nextOpt, nextVal));
                            if (!nextIsWide) {
                                col = 1;
                                continue;
                            }
                        }
                        curY += 20;
                        col = 0;
                    } else {
                        curY += 20;
                        col = 0;
                    }
                }
            }
            if (col == 1) {
                curY += 20;
                col = 0;
            }
            curY += 4; // Padding between categories
        }
    }

    private void addCompactOptionWidgets(int x, int y, int width, FeatureModule.Option opt, Object val) {
        String desc = module.settingInfo(opt.key()).description();
        if (val instanceof Boolean b) {
            EzToggleSwitch toggle = new EzToggleSwitch(
                    x, y, width, 16,
                    Component.literal(opt.label()), b,
                    state -> {
                        module.set(opt, state);
                        rebuildWidgets();
                    }
            );
            addRenderableWidget(described(toggle, desc));
        } else if (isColorOption(opt, val)) {
            String cleanLabel = cleanColorLabel(opt.label());
            int swatchW = 20;
            int btnW = width - swatchW - 3;
            EzButton btn = new EzButton(
                    x, y, btnW, 16,
                    Component.literal(cleanLabel + " …"), true,
                    b -> openColorPicker(cleanLabel, opt)
            );
            int color = module.tint(opt.key(), false);
            ColorSwatchButton swatch = new ColorSwatchButton(
                    x + btnW + 3, y, swatchW, 16,
                    color,
                    b -> openColorPicker(cleanLabel, opt)
            );
            addRenderableWidget(described(btn, desc));
            addRenderableWidget(described(swatch, desc));
        } else if (opt.choices().length > 0) {
            AbstractWidget choiceBtn = createChoiceButton(x, y, width, opt);
            addRenderableWidget(described(choiceBtn, desc));
        }
    }

    private AbstractWidget createWideWidget(int x, int y, int width, FeatureModule.Option opt, Object val) {
        if (val instanceof Number) {
            return createSlider(x, y, width, opt);
        } else {
            return createTextBox(x, y, width, opt);
        }
    }

    private AbstractWidget createChoiceButton(int x, int y, int width, FeatureModule.Option option) {
        String current = module.text(option.key());
        String label = "‹ " + option.label() + ": " + app.ezclient.util.EzI18n.text(current) + " ›";
        EzButton btn = new EzButton(x, y, width, 16, Component.literal(label), true, b -> {
            int idx = Arrays.asList(option.choices()).indexOf(module.text(option.key()));
            if (idx < 0) idx = 0;
            int next = Math.floorMod(idx + 1, option.choices().length);
            module.set(option, option.choices()[next]);
            rebuildWidgets();
        });
        btn.withRightClick(b -> {
            int idx = Arrays.asList(option.choices()).indexOf(module.text(option.key()));
            if (idx < 0) idx = 0;
            int prev = Math.floorMod(idx - 1, option.choices().length);
            module.set(option, option.choices()[prev]);
            rebuildWidgets();
        });
        return btn;
    }

    private AbstractWidget createSlider(int x, int y, int width, FeatureModule.Option option) {
        double min = option.min();
        double max = option.max();
        double val = module.number(option.key());
        double norm = max > min ? (val - min) / (max - min) : 0;
        boolean isInt = option.initial() instanceof Integer || option.initial() instanceof Long;
        return new EzSlider(
                x, y, width, 18, norm,
                v -> module.setTransient(option, min + v * (max - min)),
                v -> {
                    double currentVal = min + v * (max - min);
                    String formatted = isInt
                            ? String.valueOf((int) Math.round(currentVal))
                            : String.format(Locale.ROOT, "%.1f", currentVal);
                    return Component.literal(option.label() + ": " + formatted);
                },
                true,
                ConfigManager::save
        );
    }

    private AbstractWidget createTextBox(int x, int y, int width, FeatureModule.Option option) {
        EditBox edit = new EditBox(font, x, y, width, 16, Component.literal(option.label()));
        edit.setMaxLength(1024);
        edit.setHint(Component.literal(option.label()));
        edit.setValue(module.text(option.key()));
        edit.setResponder(v -> {
            boolean ok = module.set(option, v);
            edit.setTextColor(ok ? 0xffeeeeee : 0xffff5555);
        });
        return edit;
    }

    private void openColorPicker(String label, FeatureModule.Option option) {
        if (minecraft == null) return;
        int currentColor = module.tint(option.key(), false);
        EzScreenBridge.set(minecraft, new ModuleColorScreen(this, label, currentColor, newColor -> {
            module.set(option, String.format("%08X", newColor));
        }));
    }

    private static String cleanColorLabel(String label) {
        if (label == null) return "";
        return label.replace(" #AARRGGBB", "").replace("#AARRGGBB", "").trim();
    }

    private static boolean isColorOption(FeatureModule.Option opt, Object val) {
        if (opt.label().contains("#AARRGGBB")) return true;
        if (val instanceof String s && s.matches("#?[0-9a-fA-F]{6,8}")) return true;
        return false;
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float d) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        g.text(font, EzUi.fitText(getTitle(), panelWidth - SETTINGS_SIDEBAR_WIDTH - 70), settingsContentLeft(panelX), panelY + 10, EzUi.TEXT_WHITE);

        g.fill(settingsContentLeft(panelX), panelY + 28, panelX + panelWidth - 8, panelY + 29, EzUi.BORDER_SUBTLE);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, module.getDisplayName());

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
        private int color;
        private final Consumer<ColorSwatchButton> onClick;

        public ColorSwatchButton(int x, int y, int width, int height, int color, Consumer<ColorSwatchButton> onClick) {
            super(x, y, width, height, Component.empty());
            this.color = color;
            this.onClick = onClick;
        }

        public void setColor(int color) {
            this.color = color;
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
}
