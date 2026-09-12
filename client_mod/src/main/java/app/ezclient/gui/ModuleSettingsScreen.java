package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ModuleSettingsScreen extends ScrollingSettingsScreen {
    private final Screen parent;
    private final Module module;
    private boolean isListeningForHotkey = false;

    private int panelX, panelY, panelWidth, panelHeight;

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 34; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 32; }

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getDisplayName() + " " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "")));
        this.parent = parent;
        this.module = module;
    }

    private AbstractWidget described(AbstractWidget widget, String description) {
        widget.setTooltip(Tooltip.create(Component.literal(description)));
        return widget;
    }

    private static <T extends Enum<T>> T cycle(T current, T[] values, int direction) {
        return values[Math.floorMod(current.ordinal() + direction, values.length)];
    }

    /** Resets only the open module and keeps every other module/configuration intact. */
    private void resetOpenModule() {
        if (module instanceof ScoreboardModule scoreboard) {
            scoreboard.resetSettings();
        } else if (module instanceof HudModule hud) {
            hud.resetToDefaults();
        } else if (module instanceof FovChangerModule fov) {
            fov.setStaticFovLock(false);
            fov.setSprintMultiplier(1.15f);
            fov.setSpeedPotionMultiplier(1.20f);
            fov.setSlownessPotionMultiplier(0.85f);
            fov.setBowAimMultiplier(0.85f);
            fov.setFlyingMultiplier(1.10f);
            fov.setSmoothInterpolation(true);
        } else if (module instanceof MotionBlurModule blur) {
            blur.setBlurStrength(40);
            blur.setFpsProtection(true);
            blur.setFpsThreshold(60);
        } else if (module instanceof ChatCustomizerModule chat) {
            chat.setTimestampFormat(ChatCustomizerModule.TimestampFormat.HH_MM);
            chat.setBackgroundOpacity(50);
            chat.setLineLimit(5000);
            chat.setCopyOnClick(true);
        } else if (module instanceof TntTimerModule tnt) {
            tnt.setPrecision(2);
            tnt.setColorShift(true);
            tnt.setRenderThroughWalls(true);
        } else if (module instanceof AutoGgModule autoGg) {
            autoGg.setCustomMessage("gg");
            autoGg.setDelayMs(1000);
        } else {
            module.resetSettings();
        }
        ConfigManager.save();
    }

    @Override
    protected void init() {
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        addFixedWidget(new EzButton(
                panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()
        ));
        int sidebarY = panelY + 66;
        if (module.hasPreview()) {
            addFixedWidget(new EzButton(panelX + 6, sidebarY, SETTINGS_SIDEBAR_WIDTH - 12, 18,
                    Component.literal("Vorschau"), false, ignored -> EzScreenBridge.set(minecraft, new ModulePreviewScreen(this, module))));
            sidebarY += 22;
        }

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
        ), "Tastenkombination / Hotkey für dieses Modul belegen (ESC zum Löschen)"));

        int curY = panelY + 38;
        int fullW = settingsContentWidth(panelWidth);
        int btnW = (fullW - 6) / 2;
        int col1X = settingsContentLeft(panelX);
        int col2X = col1X + btnW + 6;

        if (module instanceof ScoreboardModule scoreboard) {
            addRenderableWidget(new EzToggleSwitch(
                    col1X, curY, btnW, 16,
                    Component.literal("Rote Zahlen"), scoreboard.isRemoveRedNumbers(),
                    b -> { scoreboard.setRemoveRedNumbers(b); ConfigManager.save(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("‹ Hintergrund: " + scoreboard.getBackgroundStyle().getLabel() + " ›"), true,
                    b -> {
                        ScoreboardModule.BackgroundStyle[] styles = ScoreboardModule.BackgroundStyle.values();
                        int next = (scoreboard.getBackgroundStyle().ordinal() + 1) % styles.length;
                        scoreboard.setBackgroundStyle(styles[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                ScoreboardModule.BackgroundStyle[] styles = ScoreboardModule.BackgroundStyle.values();
                int prev = (scoreboard.getBackgroundStyle().ordinal() - 1 + styles.length) % styles.length;
                scoreboard.setBackgroundStyle(styles[prev]);
                rebuildWidgets();
            }));
            curY += 20;

            addRenderableWidget(new EzToggleSwitch(
                    col1X, curY, btnW, 16,
                    Component.literal("Text-Schatten"), scoreboard.isTextShadow(),
                    b -> { scoreboard.setTextShadow(b); ConfigManager.save(); }
            ));
            addRenderableWidget(new EzToggleSwitch(
                    col2X, curY, btnW, 16,
                    Component.literal("IP verbergen"), scoreboard.isHideServerIpFooter(),
                    b -> { scoreboard.setHideServerIpFooter(b); ConfigManager.save(); }
            ));
            curY += 20;

            addRenderableWidget(described(new EzButton(
                    col1X, curY, fullW, 16,
                    Component.literal("Preset: " + scoreboard.getPreset().getLabel()), true,
                    b -> { scoreboard.setPreset(cycle(scoreboard.getPreset(), ScoreboardModule.Preset.values(), 1)); rebuildWidgets(); }
            ).withRightClick(b -> {
                scoreboard.setPreset(cycle(scoreboard.getPreset(), ScoreboardModule.Preset.values(), -1));
                rebuildWidgets();
            }), scoreboard.getSettingDescription("preset")));
            curY += 20;

            addRenderableWidget(described(new EzToggleSwitch(
                    col1X, curY, btnW, 16,
                    Component.literal("Titel"), scoreboard.isShowTitle(),
                    b -> { scoreboard.setShowTitle(b); ConfigManager.save(); }
            ), scoreboard.getSettingDescription("title")));
            addRenderableWidget(described(new EzToggleSwitch(
                    col2X, curY, btnW, 16,
                    Component.literal("Trenner"), scoreboard.isShowSeparator(),
                    b -> { scoreboard.setShowSeparator(b); ConfigManager.save(); }
            ), scoreboard.getSettingDescription("separator")));
            curY += 20;

            addRenderableWidget(described(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("Rand: " + scoreboard.getPadding()), true,
                    b -> { scoreboard.setPadding(scoreboard.getPadding() >= 12 ? 0 : scoreboard.getPadding() + 2); rebuildWidgets(); }
            ).withRightClick(b -> { scoreboard.setPadding(scoreboard.getPadding() <= 0 ? 12 : scoreboard.getPadding() - 2); rebuildWidgets(); }),
                    scoreboard.getSettingDescription("padding")));
            addRenderableWidget(described(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("Abstand: " + scoreboard.getExtraLineSpacing()), true,
                    b -> { scoreboard.setExtraLineSpacing(scoreboard.getExtraLineSpacing() >= 6 ? 0 : scoreboard.getExtraLineSpacing() + 1); rebuildWidgets(); }
            ).withRightClick(b -> { scoreboard.setExtraLineSpacing(scoreboard.getExtraLineSpacing() <= 0 ? 6 : scoreboard.getExtraLineSpacing() - 1); rebuildWidgets(); }),
                    scoreboard.getSettingDescription("spacing")));
            curY += 20;

            addRenderableWidget(described(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("Einträge: " + scoreboard.getMaxEntries()), true,
                    b -> { scoreboard.setMaxEntries(scoreboard.getMaxEntries() >= 15 ? 5 : scoreboard.getMaxEntries() + 5); rebuildWidgets(); }
            ).withRightClick(b -> { scoreboard.setMaxEntries(scoreboard.getMaxEntries() <= 5 ? 15 : scoreboard.getMaxEntries() - 5); rebuildWidgets(); }),
                    scoreboard.getSettingDescription("entries")));
            addRenderableWidget(described(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("Alpha: " + scoreboard.getBackgroundAlpha() + "%"), true,
                    b -> { scoreboard.setBackgroundAlpha(scoreboard.getBackgroundAlpha() >= 100 ? 0 : scoreboard.getBackgroundAlpha() + 20); rebuildWidgets(); }
            ).withRightClick(b -> { scoreboard.setBackgroundAlpha(scoreboard.getBackgroundAlpha() <= 0 ? 100 : scoreboard.getBackgroundAlpha() - 20); rebuildWidgets(); }),
                    scoreboard.getSettingDescription("backgroundAlpha")));
            curY += 20;

            addRenderableWidget(described(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("Ausrichtung: " + scoreboard.getHorizontalAlignment().getLabel()), true,
                    b -> { scoreboard.setHorizontalAlignment(cycle(scoreboard.getHorizontalAlignment(), ScoreboardModule.HorizontalAlignment.values(), 1)); rebuildWidgets(); }
            ).withRightClick(b -> { scoreboard.setHorizontalAlignment(cycle(scoreboard.getHorizontalAlignment(), ScoreboardModule.HorizontalAlignment.values(), -1)); rebuildWidgets(); }),
                    scoreboard.getSettingDescription("alignment")));
            addRenderableWidget(described(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("Schatten: " + (scoreboard.isTextShadow() ? "An" : "Aus")), scoreboard.isTextShadow(),
                    b -> { scoreboard.setTextShadow(!scoreboard.isTextShadow()); rebuildWidgets(); }
            ), scoreboard.getSettingDescription("shadow")));
            curY += 20;
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16, Component.literal("Textfarbe …"), true,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Scoreboard-Text",
                        scoreboard.getTextColor(), scoreboard::setTextColor))));
            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16, Component.literal("Titelfarbe …"), true,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Scoreboard-Titel",
                        scoreboard.getTitleColor(), scoreboard::setTitleColor))));
            curY += 20;
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16, Component.literal("Trennerfarbe …"), true,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Scoreboard-Trenner",
                        scoreboard.getSeparatorColor(), scoreboard::setSeparatorColor))));
            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16, Component.literal("Hintergrund …"), true,
                    b -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Scoreboard-Hintergrund",
                        scoreboard.getCustomBackgroundColor(), color -> {
                            scoreboard.setCustomBackgroundColor(color);
                            scoreboard.setBackgroundStyle(ScoreboardModule.BackgroundStyle.CUSTOM);
                        }))));
            curY += 24;
        } else if (module instanceof FovChangerModule fov) {
            boolean activeSliders = !fov.isStaticFovLock();
            addRenderableWidget(new EzToggleSwitch(
                    col1X, curY, btnW, 16,
                    Component.literal("Static FOV"), fov.isStaticFovLock(),
                    b -> { fov.setStaticFovLock(b); ConfigManager.save(); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzToggleSwitch(
                    col2X, curY, btnW, 16,
                    Component.literal("Sanfte FOV"), fov.isSmoothInterpolation(),
                    b -> { fov.setSmoothInterpolation(b); ConfigManager.save(); }
            ));
            curY += 20;

            EzButton sprintBtn = new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal(String.format(java.util.Locale.ROOT, "‹ Sprint: %.2fx ›", fov.getSprintMultiplier())), true,
                    b -> {
                        float v = Math.round((fov.getSprintMultiplier() + 0.05f) * 100.0f) / 100.0f;
                        if (v > 1.405f) v = 0.70f;
                        fov.setSprintMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = Math.round((fov.getSprintMultiplier() - 0.05f) * 100.0f) / 100.0f;
                if (v < 0.695f) v = 1.40f;
                fov.setSprintMultiplier(v);
                rebuildWidgets();
            });
            sprintBtn.active = activeSliders;
            addRenderableWidget(sprintBtn);

            EzButton speedBtn = new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal(String.format(java.util.Locale.ROOT, "‹ Speed: %.2fx ›", fov.getSpeedPotionMultiplier())), true,
                    b -> {
                        float v = Math.round((fov.getSpeedPotionMultiplier() + 0.05f) * 100.0f) / 100.0f;
                        if (v > 1.405f) v = 0.70f;
                        fov.setSpeedPotionMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = Math.round((fov.getSpeedPotionMultiplier() - 0.05f) * 100.0f) / 100.0f;
                if (v < 0.695f) v = 1.40f;
                fov.setSpeedPotionMultiplier(v);
                rebuildWidgets();
            });
            speedBtn.active = activeSliders;
            addRenderableWidget(speedBtn);
            curY += 20;

            EzButton bowBtn = new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal(String.format(java.util.Locale.ROOT, "‹ Bogen: %.2fx ›", fov.getBowAimMultiplier())), true,
                    b -> {
                        float v = Math.round((fov.getBowAimMultiplier() + 0.05f) * 100.0f) / 100.0f;
                        if (v > 1.105f) v = 0.50f;
                        fov.setBowAimMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = Math.round((fov.getBowAimMultiplier() - 0.05f) * 100.0f) / 100.0f;
                if (v < 0.495f) v = 1.10f;
                fov.setBowAimMultiplier(v);
                rebuildWidgets();
            });
            bowBtn.active = activeSliders;
            addRenderableWidget(bowBtn);

            EzButton flyBtn = new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal(String.format(java.util.Locale.ROOT, "‹ Flug: %.2fx ›", fov.getFlyingMultiplier())), true,
                    b -> {
                        float v = Math.round((fov.getFlyingMultiplier() + 0.05f) * 100.0f) / 100.0f;
                        if (v > 1.405f) v = 0.70f;
                        fov.setFlyingMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = Math.round((fov.getFlyingMultiplier() - 0.05f) * 100.0f) / 100.0f;
                if (v < 0.695f) v = 1.40f;
                fov.setFlyingMultiplier(v);
                rebuildWidgets();
            });
            flyBtn.active = activeSliders;
            addRenderableWidget(flyBtn);
            curY += 20;

            EzButton slowBtn = new EzButton(
                    col1X, curY, fullW, 16,
                    Component.literal(String.format(java.util.Locale.ROOT, "‹ Slowness: %.2fx ›", fov.getSlownessPotionMultiplier())), true,
                    b -> {
                        float v = Math.round((fov.getSlownessPotionMultiplier() + 0.05f) * 100.0f) / 100.0f;
                        if (v > 1.105f) v = 0.60f;
                        fov.setSlownessPotionMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = Math.round((fov.getSlownessPotionMultiplier() - 0.05f) * 100.0f) / 100.0f;
                if (v < 0.595f) v = 1.10f;
                fov.setSlownessPotionMultiplier(v);
                rebuildWidgets();
            });
            slowBtn.active = activeSliders;
            addRenderableWidget(slowBtn);
            curY += 24;
        } else if (module instanceof MotionBlurModule blur) {
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal(String.format("‹ Stärke: %d%% ›", blur.getBlurStrength())), true,
                    b -> {
                        int s = blur.getBlurStrength() >= 100 ? 20 : blur.getBlurStrength() + 20;
                        blur.setBlurStrength(s);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int s = blur.getBlurStrength() <= 20 ? 100 : blur.getBlurStrength() - 20;
                blur.setBlurStrength(s);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzToggleSwitch(
                    col2X, curY, btnW, 16,
                    Component.literal("FPS-Schutz"), blur.isFpsProtection(),
                    b -> { blur.setFpsProtection(b); ConfigManager.save(); rebuildWidgets(); }
            ));
            curY += 20;

            addRenderableWidget(new EzButton(
                    col1X, curY, fullW, 16,
                    Component.literal("‹ FPS-Schwelle: " + blur.getFpsThreshold() + " FPS ›"), true,
                    b -> {
                        int[] th = { 30, 60, 75, 120, 144 };
                        int next = 60;
                        for (int i = 0; i < th.length; i++) {
                            if (th[i] == blur.getFpsThreshold()) { next = th[(i + 1) % th.length]; break; }
                        }
                        blur.setFpsThreshold(next);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int[] th = { 30, 60, 75, 120, 144 };
                int prev = 60;
                for (int i = 0; i < th.length; i++) {
                    if (th[i] == blur.getFpsThreshold()) { prev = th[(i - 1 + th.length) % th.length]; break; }
                }
                blur.setFpsThreshold(prev);
                rebuildWidgets();
            }));
            curY += 24;
        } else if (module instanceof ChatCustomizerModule chat) {
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("‹ Zeitstempel: " + chat.getTimestampFormat().getLabel() + " ›"), true,
                    b -> {
                        ChatCustomizerModule.TimestampFormat[] fmts = ChatCustomizerModule.TimestampFormat.values();
                        int next = (chat.getTimestampFormat().ordinal() + 1) % fmts.length;
                        chat.setTimestampFormat(fmts[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                ChatCustomizerModule.TimestampFormat[] fmts = ChatCustomizerModule.TimestampFormat.values();
                int prev = (chat.getTimestampFormat().ordinal() - 1 + fmts.length) % fmts.length;
                chat.setTimestampFormat(fmts[prev]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("‹ Deckkraft: " + chat.getBackgroundOpacity() + "% ›"), true,
                    b -> {
                        int op = chat.getBackgroundOpacity() >= 100 ? 0 : chat.getBackgroundOpacity() + 25;
                        chat.setBackgroundOpacity(op);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int op = chat.getBackgroundOpacity() <= 0 ? 100 : chat.getBackgroundOpacity() - 25;
                chat.setBackgroundOpacity(op);
                rebuildWidgets();
            }));
            curY += 20;

            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("‹ Zeilen: " + chat.getLineLimit() + " ›"), true,
                    b -> {
                        int[] limits = { 100, 1000, 5000, 10000 };
                        int next = 5000;
                        for (int i = 0; i < limits.length; i++) {
                            if (limits[i] == chat.getLineLimit()) { next = limits[(i + 1) % limits.length]; break; }
                        }
                        chat.setLineLimit(next);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int[] limits = { 100, 1000, 5000, 10000 };
                int prev = 5000;
                for (int i = 0; i < limits.length; i++) {
                    if (limits[i] == chat.getLineLimit()) { prev = limits[(i - 1 + limits.length) % limits.length]; break; }
                }
                chat.setLineLimit(prev);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzToggleSwitch(
                    col2X, curY, btnW, 16,
                    Component.literal("Click-Copy"), chat.isCopyOnClick(),
                    b -> { chat.setCopyOnClick(b); ConfigManager.save(); }
            ));
            curY += 24;
        } else if (module instanceof TntTimerModule tnt) {
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("‹ " + tnt.getPrecision() + " Dezimalstellen ›"), true,
                    b -> {
                        tnt.setPrecision(tnt.getPrecision() == 1 ? 2 : 1);
                        rebuildWidgets();
                    }
            ));

            addRenderableWidget(new EzToggleSwitch(
                    col2X, curY, btnW, 16,
                    Component.literal("Farb-Shift"), tnt.isColorShift(),
                    b -> { tnt.setColorShift(b); ConfigManager.save(); }
            ));
            curY += 20;

            addRenderableWidget(new EzToggleSwitch(
                    col1X, curY, fullW, 16,
                    Component.literal("Durch Wände sehen"), tnt.isRenderThroughWalls(),
                    b -> { tnt.setRenderThroughWalls(b); ConfigManager.save(); }
            ));
            curY += 24;
        } else if (module instanceof AutoGgModule autoGg) {
            String[] msgs = { "gg", "Good Game! <3", "gg wp", "Good Game!" };
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("‹ Text: \"" + autoGg.getCustomMessage() + "\" ›"), true,
                    b -> {
                        int nextIdx = 0;
                        for (int i = 0; i < msgs.length; i++) {
                            if (msgs[i].equalsIgnoreCase(autoGg.getCustomMessage())) { nextIdx = (i + 1) % msgs.length; break; }
                        }
                        autoGg.setCustomMessage(msgs[nextIdx]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int prevIdx = 0;
                for (int i = 0; i < msgs.length; i++) {
                    if (msgs[i].equalsIgnoreCase(autoGg.getCustomMessage())) { prevIdx = (i - 1 + msgs.length) % msgs.length; break; }
                }
                autoGg.setCustomMessage(msgs[prevIdx]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal(String.format("‹ Delay: %dms ›", autoGg.getDelayMs())), true,
                    b -> {
                        int d = autoGg.getDelayMs() >= 3000 ? 500 : autoGg.getDelayMs() + 500;
                        autoGg.setDelayMs(d);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int d = autoGg.getDelayMs() <= 500 ? 3000 : autoGg.getDelayMs() - 500;
                autoGg.setDelayMs(d);
                rebuildWidgets();
            }));
            curY += 24;
        }

        // A module-local reset is shared by every legacy settings panel.
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX), panelY + panelHeight - 24, 100, 16,
                Component.literal("Zurücksetzen"), false,
                b -> { resetOpenModule(); rebuildWidgets(); }
        ));
        addFixedWidget(new EzButton(
                settingsContentLeft(panelX) + 108, panelY + panelHeight - 24, 100, 16,
                app.ezclient.util.EzI18n.comp("ezclient.module_settings.done"), true,
                b -> onClose()
        ));
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float d) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        g.text(font, app.ezclient.util.EzI18n.get("ezclient.module_settings.title", module.getDisplayName()), settingsContentLeft(panelX), panelY + 10, EzUi.TEXT_WHITE);

        g.fill(settingsContentLeft(panelX), panelY + 28, panelX + panelWidth - 8, panelY + 29, EzUi.BORDER_SUBTLE);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, module.getDisplayName());

        // ── Render Live Preview Box on Right Side ──
        super.extractSettings(g, mx, my, d);
    }

    /**
     * Legacy previews contain dynamic text whose actual width can exceed the small
     * sidebar canvas. Draw them on one predictable virtual canvas and fit that
     * complete canvas into the preview card.
     */
    private void renderFittedModulePreview(GuiGraphicsExtractor g, int px, int py, int pw, int ph) {
        int virtualWidth;
        int virtualHeight;
        if (module instanceof ScoreboardModule) { virtualWidth = 120; virtualHeight = 94; }
        else if (module instanceof BossBarModule) { virtualWidth = 190; virtualHeight = 40; }
        else if (module instanceof ChatCustomizerModule || module instanceof AutoGgModule) { virtualWidth = 180; virtualHeight = 72; }
        else if (module instanceof TntTimerModule) { virtualWidth = 64; virtualHeight = 82; }
        else if (module instanceof FovChangerModule) { virtualWidth = 120; virtualHeight = 78; }
        else { virtualWidth = 100; virtualHeight = 40; }
        float scale = Math.min(1.0f, Math.min(pw / (float) virtualWidth, ph / (float) virtualHeight));
        float drawWidth = virtualWidth * scale;
        float drawHeight = virtualHeight * scale;
        g.pose().pushMatrix();
        g.pose().translate(px + (pw - drawWidth) / 2.0f, py + (ph - drawHeight) / 2.0f);
        g.pose().scale(scale, scale);
        renderModulePreview(g, 0, 0, virtualWidth, virtualHeight);
        g.pose().popMatrix();
    }

    private void renderModulePreview(GuiGraphicsExtractor g, int px, int py, int pw, int ph) {
        if (module instanceof ScoreboardModule sb) {
            int bgCol = switch (sb.getBackgroundStyle()) {
                case INVISIBLE -> 0x00000000;
                case TRANSLUCENT -> 0x85111722;
                case CUSTOM -> sb.getCustomBackgroundColor();
                case VANILLA -> 0x60000000;
            };
            int scoreW = pw;
            int scoreH = 82;
            int sx = px + (pw - scoreW) / 2;
            int sy = py + 8;
            if ((bgCol >>> 24) > 0) {
                g.fill(sx, sy, sx + scoreW, sy + scoreH, bgCol);
            }
            g.centeredText(font, Component.literal("§e§lBED WARS"), sx + scoreW / 2, sy + 3, 0xFFFFFFFF);
            String[] sampleLines = { "§703/09/26 m12A", "§fKills: §a4", "§fFinal Kills: §a2", "§fBeds Broken: §a1", "§eyourserver.net" };
            for (int i = 0; i < sampleLines.length; i++) {
                if (i == 4 && sb.isHideServerIpFooter()) continue;
                int ly = sy + 15 + i * 11;
                g.text(font, sampleLines[i], sx + 4, ly, 0xFFFFFFFF);
                if (!sb.isRemoveRedNumbers() && i > 0 && i < 4) {
                    g.text(font, "§c" + (4 - i), sx + scoreW - 10, ly, 0xFFFF5555);
                }
            }
        } else if (module instanceof BossBarModule boss) {
            BossBarModule.renderBossBar(g, minecraft, boss, Component.literal("§d§lEnderdrache"), 0.75f, net.minecraft.world.BossEvent.BossBarColor.PURPLE, px, py + 8, true);
        } else if (module instanceof ChatCustomizerModule chat) {
            ChatCustomizerModule.renderDummyChat(g, minecraft, chat, px + 2, py + 10, pw - 4, 0.85);
        } else if (module instanceof TntTimerModule tnt) {
            EzUi.roundedRect(g, px + pw / 2 - 18, py + 30, 36, 36, 4, 0xFFCC3333);
            g.centeredText(font, Component.literal("TNT"), px + pw / 2, py + 43, 0xFFFFFFFF);
            int tagCol = tnt.isColorShift() ? 0xFFFFAA00 : 0xFFFFFFFF;
            EzUi.roundedRect(g, px + pw / 2 - 20, py + 14, 40, 12, 3, 0x90000000);
        } else if (module instanceof FovChangerModule fov) {
            EzUi.roundedRect(g, px, py + 15, pw, 60, 4, 0x85111722);
            g.text(font, "FOV Modifiers:", px + 4, py + 20, 0xFFFFFFFF);
            g.text(font, String.format("Sprint: %.2fx", fov.getSprintMultiplier()), px + 4, py + 34, 0xFF43DD8C);
            g.text(font, String.format("Speed: %.2fx", fov.getSpeedPotionMultiplier()), px + 4, py + 46, 0xFF38BDF8);
            g.text(font, fov.isStaticFovLock() ? "§c● FOV Lock Active" : "§a● Dynamic FOV", px + 4, py + 58, 0xFFFFFFFF);
        } else {
            g.centeredText(font, Component.literal(module.getDisplayName()), px + pw / 2, py + ph / 2 - 6, 0xFFFFFFFF);
            g.centeredText(font, Component.literal(module.isEnabled() ? "§aAktiv" : "§cInaktiv"), px + pw / 2, py + ph / 2 + 6, 0xFFFFFFFF);
        }
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
}
