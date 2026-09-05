package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;

/**
 * Dynamic, ultra-clean Split-View HUD customization screen for EzClient.
 * Left: Live Preview card with scissor-clipping protection + Box & Border toggles.
 * Right: Badlion-style Module Controls, Color Modes, Paint.NET Color Picker with 3x3 preset palette,
 * and a Reset Button with confirmation dialog.
 */
public final class HudSettingsScreen extends Screen {
    private final Screen parent;
    private final HudModule module;

    private int panelX, panelY;
    private int panelWidth = 430;
    private int panelHeight = 260;

    // Color picker state
    private int activeColorSlot = 1; // 1 = Farbe 1 (textColor), 2 = Farbe 2 (waveColor2)
    private float currentHue = 0.40f;
    private float currentSat = 0.83f;
    private float currentVal = 0.79f;

    private boolean isDraggingSV = false;
    private boolean isDraggingHue = false;

    private EditBox hexInput;
    private static int crosshairTab = 0;
    private boolean updatingHexInternally = false;

    // Reset confirmation modal state
    private boolean showResetConfirmation = false;

    // Coordinates for Color Picker elements (Right side)
    private int svX, svY, svW = 90, svH = 56;
    private int hueX, hueY, hueW = 14, hueH = 56;

    // 3x3 Color Palette Presets
    private static final int[] PRESETS = {
            0xFF22C96E, 0xFF00D2FF, 0xFF3B82F6,
            0xFFFF6B4A, 0xFFEF4444, 0xFFEAB308,
            0xFFA855F7, 0xFFFFFFFF, 0xFF334155
    };
    private int selectedPresetIndex = -1;

    public HudSettingsScreen(Screen parent, HudModule module) {
        super(Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.title", module.getDisplayName())));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        panelWidth = 416;
        panelHeight = 236;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        addRenderableWidget(new EzButton(panelX, panelY - 22, 160, 18, Component.literal("Font / Shadow / HUD Style"), true,
            b -> EzScreenBridge.set(minecraft, new FeatureStyleScreen(this, module))));

        int rightX = panelX + 148;
        int rightWidth = panelWidth - 160;

        // Top Right Close Button (Vertically centered in header)
        addRenderableWidget(new EzButton(
                panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()
        ));

        // ── LEFT SIDE: Box & Border toggles under preview ──
        int prevX = panelX + 12;
        addRenderableWidget(new EzButton(
                prevX, panelY + panelHeight - 26, 58, 16,
                Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.box", app.ezclient.util.EzI18n.onOrOff(module.hasBackground()))), module.hasBackground(),
                b -> {
                    module.setBackground(!module.hasBackground());
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ));

        addRenderableWidget(new EzButton(
                prevX + 62, panelY + panelHeight - 26, 62, 16,
                Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.border", app.ezclient.util.EzI18n.onOrOff(module.hasBorder()))), module.hasBorder(),
                b -> {
                    module.setBorder(!module.hasBorder());
                    ConfigManager.save();
                    rebuildWidgets();
                }
        ));

        // ── RIGHT SIDE: 2 Clean Rows of Settings Buttons ──
        int row1Y = panelY + 38;
        int row2Y = panelY + 58;
        int btnW = 124;
        int btnGap = 6;
        int col2X = rightX + btnW + btnGap;

        if (module instanceof CoordinatesModule coords) {
            String layoutStr = switch (coords.getLayoutMode()) {
                case SINGLE_LINE -> app.ezclient.util.EzI18n.get("ezclient.hud_settings.coords_1line");
                case MULTI_LINE -> app.ezclient.util.EzI18n.get("ezclient.hud_settings.coords_3line");
                case COMPASS_BAR -> app.ezclient.util.EzI18n.get("ezclient.hud_settings.coords_compass");
            };
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal(layoutStr), true,
                    b -> {
                        CoordinatesModule.LayoutMode cur = coords.getLayoutMode();
                        if (cur == CoordinatesModule.LayoutMode.SINGLE_LINE) coords.setLayoutMode(CoordinatesModule.LayoutMode.MULTI_LINE);
                        else if (cur == CoordinatesModule.LayoutMode.MULTI_LINE) coords.setLayoutMode(CoordinatesModule.LayoutMode.COMPASS_BAR);
                        else coords.setLayoutMode(CoordinatesModule.LayoutMode.SINGLE_LINE);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.coords_dec", coords.getDecimalPrecision())), coords.getDecimalPrecision() > 0,
                    b -> {
                        coords.setDecimalPrecision((coords.getDecimalPrecision() + 1) % 3);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW, 16,
                    Component.literal("Nether: " + (coords.isShowNether() ? app.ezclient.util.EzI18n.get("ezclient.hud_settings.on") : app.ezclient.util.EzI18n.get("ezclient.hud_settings.off"))), coords.isShowNether(),
                    b -> { coords.setShowNether(!coords.isShowNether()); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row2Y, btnW, 16,
                    Component.literal("Biom: " + (coords.isShowBiome() ? app.ezclient.util.EzI18n.get("ezclient.hud_settings.on") : app.ezclient.util.EzI18n.get("ezclient.hud_settings.off"))), coords.isShowBiome(),
                    b -> { coords.setShowBiome(!coords.isShowBiome()); rebuildWidgets(); }
            ));
        } else if (module instanceof KeystrokesModule ks) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal(ks.getLayoutPreset().name().replace('_', ' ')), true,
                    b -> {
                        KeystrokesModule.LayoutPreset[] presets = KeystrokesModule.LayoutPreset.values();
                        int next = (ks.getLayoutPreset().ordinal() + 1) % presets.length;
                        ks.setLayoutPreset(presets[next]);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.keystrokes_space", ks.getSpaceStyle().name())), true,
                    b -> {
                        KeystrokesModule.SpaceStyle[] styles = KeystrokesModule.SpaceStyle.values();
                        int next = (ks.getSpaceStyle().ordinal() + 1) % styles.length;
                        ks.setSpaceStyle(styles[next]);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.keystrokes_fade", app.ezclient.util.EzI18n.onOrOff(ks.getFadeTimeMs() > 0))), ks.getFadeTimeMs() > 0,
                    b -> {
                        ks.setFadeTimeMs(ks.getFadeTimeMs() > 0 ? 0 : 150);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row2Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.keystrokes_cps", app.ezclient.util.EzI18n.onOrOff(ks.isShowMouseCps()))), ks.isShowMouseCps(),
                    b -> {
                        ks.setShowMouseCps(!ks.isShowMouseCps());
                        rebuildWidgets();
                    }
            ));
        } else if (module instanceof CpsModule cps) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal(cps.getDisplayMode().name().replace('_', ' ')), true,
                    b -> {
                        CpsModule.DisplayMode[] modes = CpsModule.DisplayMode.values();
                        int next = (cps.getDisplayMode().ordinal() + 1) % modes.length;
                        cps.setDisplayMode(modes[next]);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.cps_graph", app.ezclient.util.EzI18n.onOrOff(cps.isShowHistoryGraph()))), cps.isShowHistoryGraph(),
                    b -> { cps.setShowHistoryGraph(!cps.isShowHistoryGraph()); rebuildWidgets(); }
            ));
        } else if (module instanceof FpsModule fps) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal(fps.getFormatOption().name().replace('_', ' ')), true,
                    b -> {
                        FpsModule.FormatOption[] opts = FpsModule.FormatOption.values();
                        int next = (fps.getFormatOption().ordinal() + 1) % opts.length;
                        fps.setFormatOption(opts[next]);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.fps_smooth", (fps.getUpdateIntervalMs() == 0 ? app.ezclient.util.EzI18n.get("ezclient.hud_settings.off") : fps.getUpdateIntervalMs() + "ms"))), fps.getUpdateIntervalMs() > 0,
                    b -> {
                        int cur = fps.getUpdateIntervalMs();
                        int next = cur == 0 ? 250 : (cur == 250 ? 500 : (cur == 500 ? 1000 : 0));
                        fps.setUpdateIntervalMs(next);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW * 2 + btnGap, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.fps_minmax", app.ezclient.util.EzI18n.onOrOff(fps.isShowMinMax()))), fps.isShowMinMax(),
                    b -> { fps.setShowMinMax(!fps.isShowMinMax()); rebuildWidgets(); }
            ));
        } else if (module instanceof ArmorStatusModule armor) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get(armor.isHorizontal() ? "ezclient.hud_settings.armor_horizontal" : "ezclient.hud_settings.armor_vertical")), armor.isHorizontal(),
                    b -> { armor.setHorizontal(!armor.isHorizontal()); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal(armor.getDurabilityMode().name()), armor.getDurabilityMode() != ArmorStatusModule.DurabilityMode.ICON_ONLY,
                    b -> {
                        ArmorStatusModule.DurabilityMode[] modes = ArmorStatusModule.DurabilityMode.values();
                        int next = (armor.getDurabilityMode().ordinal() + 1) % modes.length;
                        armor.setDurabilityMode(modes[next]);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW, 16,
                    Component.literal("Dynamic Box: " + app.ezclient.util.EzI18n.onOrOff(armor.isDynamicBox())), armor.isDynamicBox(),
                    b -> { armor.setDynamicBox(!armor.isDynamicBox()); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row2Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.armor_warning", app.ezclient.util.EzI18n.onOrOff(armor.isDamageWarning()))), armor.isDamageWarning(),
                    b -> { armor.setDamageWarning(!armor.isDamageWarning()); rebuildWidgets(); }
            ));
        } else if (module instanceof ToggleSprintSneakModule ts) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.togglesprint_sprint", ts.getSprintMode().name())), ts.getSprintMode() == ToggleSprintSneakModule.SprintMode.TOGGLE,
                    b -> {
                        ToggleSprintSneakModule.SprintMode[] modes = ToggleSprintSneakModule.SprintMode.values();
                        int next = (ts.getSprintMode().ordinal() + 1) % modes.length;
                        ts.setSprintMode(modes[next]);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.togglesprint_sneak", ts.getSneakMode().name())), ts.getSneakMode() == ToggleSprintSneakModule.SneakMode.TOGGLE,
                    b -> {
                        ToggleSprintSneakModule.SneakMode[] modes = ToggleSprintSneakModule.SneakMode.values();
                        int next = (ts.getSneakMode().ordinal() + 1) % modes.length;
                        ts.setSneakMode(modes[next]);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW * 2 + btnGap, 16,
                    Component.literal("Hide HUD: " + app.ezclient.util.EzI18n.onOrOff(ts.isHideHud())), ts.isHideHud(),
                    b -> { ts.setHideHud(!ts.isHideHud()); rebuildWidgets(); }
            ));
        } else if (module instanceof PotionEffectModule potion) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal(potion.getDisplayStyle().name()), true,
                    b -> {
                        PotionEffectModule.DisplayStyle[] styles = PotionEffectModule.DisplayStyle.values();
                        int next = (potion.getDisplayStyle().ordinal() + 1) % styles.length;
                        potion.setDisplayStyle(styles[next]);
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get(potion.isVertical() ? "ezclient.hud_settings.potion_vertical" : "ezclient.hud_settings.potion_horizontal")), potion.isVertical(),
                    b -> { potion.setVertical(!potion.isVertical()); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.potion_time", app.ezclient.util.EzI18n.onOrOff(potion.isShowTime()))), potion.isShowTime(),
                    b -> { potion.setShowTime(!potion.isShowTime()); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row2Y, btnW, 16,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.hud_settings.potion_blink", potion.getBlinkWarningSeconds())), potion.getBlinkWarningSeconds() > 0,
                    b -> {
                        int cur = potion.getBlinkWarningSeconds();
                        potion.setBlinkWarningSeconds(cur == 0 ? 3 : (cur == 3 ? 5 : (cur == 5 ? 10 : 0)));
                        rebuildWidgets();
                    }
            ));
        } else if (module instanceof CrosshairModule crosshair) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal("Aussehen"), crosshairTab == 0,
                    b -> { crosshairTab = 0; rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal("Trefferfarben"), crosshairTab == 1,
                    b -> { crosshairTab = 1; rebuildWidgets(); }
            ));

            if (crosshairTab == 0) {
                // Style Tab
                addRenderableWidget(new EzButton(
                        rightX, row2Y, btnW, 16,
                        Component.literal("‹ Form: " + crosshair.getCrosshairTypeLabel() + " ›"), true,
                        b -> {
                            crosshair.cycleCrosshairType(1);
                            rebuildWidgets();
                        }
                ).withRightClick(b -> {
                    crosshair.cycleCrosshairType(-1);
                    rebuildWidgets();
                }));

                addRenderableWidget(new EzButton(
                        col2X, row2Y, btnW, 16,
                        Component.literal("‹ Abstand: " + crosshair.getGap() + " ›"), true,
                        b -> {
                            crosshair.setGap((crosshair.getGap() + 1) % 16);
                            rebuildWidgets();
                        }
                ).withRightClick(b -> {
                    crosshair.setGap((crosshair.getGap() - 1 + 16) % 16);
                    rebuildWidgets();
                }));

                int row3Y = panelY + 76;
                addRenderableWidget(new EzButton(
                        rightX, row3Y, btnW, 16,
                        Component.literal("‹ Größe: " + crosshair.getSize() + " ›"), true,
                        b -> {
                            crosshair.setSize(crosshair.getSize() >= 20 ? 2 : crosshair.getSize() + 1);
                            rebuildWidgets();
                        }
                ).withRightClick(b -> {
                    crosshair.setSize(crosshair.getSize() <= 2 ? 20 : crosshair.getSize() - 1);
                    rebuildWidgets();
                }));

                addRenderableWidget(new EzButton(
                        col2X, row3Y, btnW, 16,
                        Component.literal("‹ Dicke: " + crosshair.getThickness() + " ›"), true,
                        b -> {
                            crosshair.setThickness(crosshair.getThickness() % 4 + 1);
                            rebuildWidgets();
                        }
                ).withRightClick(b -> {
                    crosshair.setThickness(crosshair.getThickness() <= 1 ? 4 : crosshair.getThickness() - 1);
                    rebuildWidgets();
                }));

                int row4Y = panelY + 96;
                addRenderableWidget(new EzButton(
                        rightX, row4Y, btnW, 16,
                        Component.literal("‹ Deckkraft: " + crosshair.getOpacity() + "% ›"), true,
                        b -> {
                            crosshair.setOpacity(crosshair.getOpacity() >= 100 ? 10 : crosshair.getOpacity() + 10);
                            rebuildWidgets();
                        }
                ).withRightClick(b -> {
                    crosshair.setOpacity(crosshair.getOpacity() <= 10 ? 100 : crosshair.getOpacity() - 10);
                    rebuildWidgets();
                }));

                addRenderableWidget(new EzButton(
                        col2X, row4Y, btnW, 16,
                        Component.literal("Kontur: " + app.ezclient.util.EzI18n.onOrOff(crosshair.isShowOutline())), crosshair.isShowOutline(),
                        b -> { crosshair.setShowOutline(!crosshair.isShowOutline()); rebuildWidgets(); }
                ));

                int row5Y = panelY + 116;
                if (crosshair.getCrosshairType() == CrosshairModule.CrosshairType.DOT) {
                    addRenderableWidget(new EzButton(
                            rightX, row5Y, btnW * 2 + btnGap, 16,
                            Component.literal("‹ Punktgröße: " + crosshair.getDotSize() + " ›"), true,
                            b -> { crosshair.setDotSize(crosshair.getDotSize() >= 6 ? 1 : crosshair.getDotSize() + 1); rebuildWidgets(); }
                    ).withRightClick(b -> {
                        crosshair.setDotSize(crosshair.getDotSize() <= 1 ? 6 : crosshair.getDotSize() - 1);
                        rebuildWidgets();
                    }));
                } else {
                    addRenderableWidget(new EzButton(
                            rightX, row5Y, btnW, 16,
                            Component.literal("Mittelpunkt: " + app.ezclient.util.EzI18n.onOrOff(crosshair.isShowDot())), crosshair.isShowDot(),
                            b -> { crosshair.setShowDot(!crosshair.isShowDot()); rebuildWidgets(); }
                    ));
                    addRenderableWidget(new EzButton(
                            col2X, row5Y, btnW, 16,
                            Component.literal("Dynamisch: " + app.ezclient.util.EzI18n.onOrOff(crosshair.isDynamicSpread())), crosshair.isDynamicSpread(),
                            b -> { crosshair.setDynamicSpread(!crosshair.isDynamicSpread()); rebuildWidgets(); }
                    ));
                }

                int row6Y = panelY + 136;
                addRenderableWidget(new EzButton(
                        rightX, row6Y, btnW, 16,
                        Component.literal("In F3 ausbl.: " + app.ezclient.util.EzI18n.onOrOff(crosshair.isHideInF3())), crosshair.isHideInF3(),
                        b -> { crosshair.setHideInF3(!crosshair.isHideInF3()); rebuildWidgets(); }
                ));

                addRenderableWidget(new EzButton(
                        col2X, row6Y, btnW, 16,
                        Component.literal("3. Person ausbl.: " + app.ezclient.util.EzI18n.onOrOff(crosshair.isHideInThirdPerson())), crosshair.isHideInThirdPerson(),
                        b -> { crosshair.setHideInThirdPerson(!crosshair.isHideInThirdPerson()); rebuildWidgets(); }
                ));
            } else {
                // Target & Colors Tab
                String targetModeName = switch (crosshair.getTargetMode()) {
                    case OFF -> app.ezclient.util.EzI18n.get("ezclient.hud_settings.crosshair_target_off");
                    case ENTITIES -> app.ezclient.util.EzI18n.get("ezclient.hud_settings.crosshair_target_entities");
                    case PLAYERS -> app.ezclient.util.EzI18n.get("ezclient.hud_settings.crosshair_target_players");
                    case HOSTILE -> "Hostile";
                    case NEUTRAL -> "Neutral";
                    case BLOCKS -> app.ezclient.util.EzI18n.get("ezclient.hud_settings.crosshair_target_blocks");
                    case ALL -> app.ezclient.util.EzI18n.get("ezclient.hud_settings.crosshair_target_all");
                };

                addRenderableWidget(new EzButton(
                        rightX, row2Y, btnW, 16,
                        Component.literal("‹ Target: " + targetModeName + " ›"), crosshair.isTargetHighlight(),
                        b -> {
                            CrosshairModule.TargetMode[] modes = CrosshairModule.TargetMode.values();
                            int next = (crosshair.getTargetMode().ordinal() + 1) % modes.length;
                            crosshair.setTargetMode(modes[next]);
                            rebuildWidgets();
                        }
                ).withRightClick(b -> {
                    CrosshairModule.TargetMode[] modes = CrosshairModule.TargetMode.values();
                    int prev = (crosshair.getTargetMode().ordinal() - 1 + modes.length) % modes.length;
                    crosshair.setTargetMode(modes[prev]);
                    rebuildWidgets();
                }));

                addRenderableWidget(new EzButton(
                        col2X, row2Y, btnW, 16,
                        Component.literal(String.format("‹ Scale: %.2fx ›", crosshair.getSelectedRuleScale())), crosshair.isTargetHighlight(),
                        b -> {
                            float current = crosshair.getSelectedRuleScale();
                            crosshair.setSelectedRuleScale(current >= 1.5f ? 0.75f : current + 0.25f);
                            rebuildWidgets();
                        }
                ).withRightClick(b -> {
                    float current = crosshair.getSelectedRuleScale();
                    crosshair.setSelectedRuleScale(current <= 0.75f ? 1.5f : current - 0.25f);
                    rebuildWidgets();
                }));
            }
        } else if (module instanceof ClockModule clock) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal("‹ " + clock.getClockFormat().getLabel() + " ›"), true,
                    b -> {
                        ClockModule.ClockFormat[] formats = ClockModule.ClockFormat.values();
                        int next = (clock.getClockFormat().ordinal() + 1) % formats.length;
                        clock.setClockFormat(formats[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                ClockModule.ClockFormat[] formats = ClockModule.ClockFormat.values();
                int prev = (clock.getClockFormat().ordinal() - 1 + formats.length) % formats.length;
                clock.setClockFormat(formats[prev]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal("Prefix: " + (clock.isShowPrefix() ? "Time:" : "None")), clock.isShowPrefix(),
                    b -> {
                        clock.setShowPrefix(!clock.isShowPrefix());
                        rebuildWidgets();
                    }
            ));
        } else if (module instanceof MemoryModule memory) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal("‹ " + memory.getMemoryFormat().getLabel() + " ›"), true,
                    b -> {
                        MemoryModule.MemoryFormat[] formats = MemoryModule.MemoryFormat.values();
                        int next = (memory.getMemoryFormat().ordinal() + 1) % formats.length;
                        memory.setMemoryFormat(formats[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                MemoryModule.MemoryFormat[] formats = MemoryModule.MemoryFormat.values();
                int prev = (memory.getMemoryFormat().ordinal() - 1 + formats.length) % formats.length;
                memory.setMemoryFormat(formats[prev]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal("Prefix: " + (memory.isShowPrefix() ? "RAM:" : "None")), memory.isShowPrefix(),
                    b -> {
                        memory.setShowPrefix(!memory.isShowPrefix());
                        rebuildWidgets();
                    }
            ));
        } else if (module instanceof PingModule ping) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal("‹ " + ping.getDisplayLayout().name().replace('_', ' ') + " ›"), true,
                    b -> {
                        PingModule.DisplayLayout[] layouts = PingModule.DisplayLayout.values();
                        int next = (ping.getDisplayLayout().ordinal() + 1) % layouts.length;
                        ping.setDisplayLayout(layouts[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                PingModule.DisplayLayout[] layouts = PingModule.DisplayLayout.values();
                int prev = (ping.getDisplayLayout().ordinal() - 1 + layouts.length) % layouts.length;
                ping.setDisplayLayout(layouts[prev]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal("‹ " + ping.getUpdateIntervalSeconds() + "s Interval ›"), true,
                    b -> {
                        int cur = ping.getUpdateIntervalSeconds();
                        ping.setUpdateIntervalSeconds(cur >= 10 ? 1 : cur + 1);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int cur = ping.getUpdateIntervalSeconds();
                ping.setUpdateIntervalSeconds(cur <= 1 ? 10 : cur - 1);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW, 16,
                    Component.literal("Alert Farben: " + (ping.isPingAlert() ? "ON" : "OFF")), ping.isPingAlert(),
                    b -> { ping.setPingAlert(!ping.isPingAlert()); rebuildWidgets(); }
            ));

            addRenderableWidget(new EzButton(
                    col2X, row2Y, btnW, 16,
                    Component.literal("Spieler: " + (ping.isShowPlayerCount() ? "ON" : "OFF")), ping.isShowPlayerCount(),
                    b -> { ping.setShowPlayerCount(!ping.isShowPlayerCount()); rebuildWidgets(); }
            ));
        } else if (module instanceof ReachModule reach) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal("‹ " + reach.getDisplayFormat().name() + " ›"), true,
                    b -> {
                        ReachModule.DisplayFormat[] formats = ReachModule.DisplayFormat.values();
                        int next = (reach.getDisplayFormat().ordinal() + 1) % formats.length;
                        reach.setDisplayFormat(formats[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                ReachModule.DisplayFormat[] formats = ReachModule.DisplayFormat.values();
                int prev = (reach.getDisplayFormat().ordinal() - 1 + formats.length) % formats.length;
                reach.setDisplayFormat(formats[prev]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal("‹ " + reach.getPrecision() + " Dezimalstellen ›"), true,
                    b -> {
                        int p = reach.getPrecision() >= 3 ? 1 : reach.getPrecision() + 1;
                        reach.setPrecision(p);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int p = reach.getPrecision() <= 1 ? 3 : reach.getPrecision() - 1;
                reach.setPrecision(p);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW, 16,
                    Component.literal("‹ Fade: " + reach.getFadeOutDurationMs() + "ms ›"), true,
                    b -> {
                        int f = reach.getFadeOutDurationMs() >= 3000 ? 500 : reach.getFadeOutDurationMs() + 500;
                        reach.setFadeOutDurationMs(f);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int f = reach.getFadeOutDurationMs() <= 500 ? 3000 : reach.getFadeOutDurationMs() - 500;
                reach.setFadeOutDurationMs(f);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, row2Y, btnW, 16,
                    Component.literal("Farben: " + (reach.isColorCoding() ? "Distanz" : "Custom")), reach.isColorCoding(),
                    b -> { reach.setColorCoding(!reach.isColorCoding()); rebuildWidgets(); }
            ));
        } else if (module instanceof ComboCounterModule combo) {
            addRenderableWidget(new EzButton(
                    rightX, row1Y, btnW, 16,
                    Component.literal("‹ " + combo.getDisplayFormat().name() + " ›"), true,
                    b -> {
                        ComboCounterModule.DisplayFormat[] formats = ComboCounterModule.DisplayFormat.values();
                        int next = (combo.getDisplayFormat().ordinal() + 1) % formats.length;
                        combo.setDisplayFormat(formats[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                ComboCounterModule.DisplayFormat[] formats = ComboCounterModule.DisplayFormat.values();
                int prev = (combo.getDisplayFormat().ordinal() - 1 + formats.length) % formats.length;
                combo.setDisplayFormat(formats[prev]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, row1Y, btnW, 16,
                    Component.literal(String.format("‹ Reset: %.1fs ›", combo.getResetWindowSeconds())), true,
                    b -> {
                        float r = combo.getResetWindowSeconds() >= 3.0f ? 1.0f : combo.getResetWindowSeconds() + 0.5f;
                        combo.setResetWindowSeconds(r);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float r = combo.getResetWindowSeconds() <= 1.0f ? 3.0f : combo.getResetWindowSeconds() - 0.5f;
                combo.setResetWindowSeconds(r);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    rightX, row2Y, btnW, 16,
                    Component.literal("Scale-Punch: " + (combo.isScalePunch() ? "ON" : "OFF")), combo.isScalePunch(),
                    b -> { combo.setScalePunch(!combo.isScalePunch()); rebuildWidgets(); }
            ));

            addRenderableWidget(new EzButton(
                    col2X, row2Y, btnW, 16,
                    Component.literal("Hit-Sound: " + (combo.isSoundFeedback() ? "ON" : "OFF")), combo.isSoundFeedback(),
                    b -> { combo.setSoundFeedback(!combo.isSoundFeedback()); rebuildWidgets(); }
            ));
        } else {
            EditBox prefix = new EditBox(font, rightX + 36, row1Y, 80, 16, Component.literal("Prefix"));
            prefix.setValue(module.getPrefix());
            prefix.setResponder(module::setPrefix);
            addRenderableWidget(prefix);

            EditBox suffix = new EditBox(font, col2X + 36, row1Y, 80, 16, Component.literal("Suffix"));
            suffix.setValue(module.getSuffix());
            suffix.setResponder(module::setSuffix);
            addRenderableWidget(suffix);
        }

        // ── Color Modes (Vanilla / Einfarbig / Welle / Rainbow) ──
        if (!(module instanceof CrosshairModule) || crosshairTab == 1) {
            int modeY = panelY + 76;
            if (module instanceof PotionEffectModule potion) {
                int pW = 60;
                int pGap = 5;
                // 1. Vanilla / Effektfarben
                addRenderableWidget(new EzButton(
                        rightX, modeY, pW, 16,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color_vanilla"),
                        potion.isUseCustomColors() && potion.getColorMode() == HudModule.ColorMode.SOLID,
                        b -> {
                            potion.setUseCustomColors(true);
                            potion.setColorMode(HudModule.ColorMode.SOLID);
                            rebuildWidgets();
                        }
                ));

                // 2. Solid / Einfarbig
                addRenderableWidget(new EzButton(
                        rightX + pW + pGap, modeY, pW, 16,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color_solid"),
                        !potion.isUseCustomColors() && potion.getColorMode() == HudModule.ColorMode.SOLID,
                        b -> {
                            potion.setUseCustomColors(false);
                            potion.setColorMode(HudModule.ColorMode.SOLID);
                            syncPickerFromCurrentSlot();
                            rebuildWidgets();
                        }
                ));

                // 3. Wave / Welle
                addRenderableWidget(new EzButton(
                        rightX + (pW + pGap) * 2, modeY, pW, 16,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color_wave"),
                        potion.getColorMode() == HudModule.ColorMode.WAVE,
                        b -> {
                            potion.setUseCustomColors(false);
                            potion.setColorMode(HudModule.ColorMode.WAVE);
                            syncPickerFromCurrentSlot();
                            rebuildWidgets();
                        }
                ));

                // 4. Rainbow
                addRenderableWidget(new EzButton(
                        rightX + (pW + pGap) * 3, modeY, pW + 4, 16,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color_rainbow"),
                        potion.getColorMode() == HudModule.ColorMode.RAINBOW,
                        b -> {
                            potion.setUseCustomColors(false);
                            potion.setColorMode(HudModule.ColorMode.RAINBOW);
                            rebuildWidgets();
                        }
                ));
            } else {
                int cW = 82;
                int cGap = 4;
                addRenderableWidget(new EzButton(
                        rightX, modeY, cW, 16,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color_solid"), module.getColorMode() == HudModule.ColorMode.SOLID,
                        b -> {
                            module.setColorMode(HudModule.ColorMode.SOLID);
                            syncPickerFromCurrentSlot();
                            rebuildWidgets();
                        }
                ));

                addRenderableWidget(new EzButton(
                        rightX + cW + cGap, modeY, cW, 16,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color_wave"), module.getColorMode() == HudModule.ColorMode.WAVE,
                        b -> {
                            module.setColorMode(HudModule.ColorMode.WAVE);
                            syncPickerFromCurrentSlot();
                            rebuildWidgets();
                        }
                ));

                addRenderableWidget(new EzButton(
                        rightX + (cW + cGap) * 2, modeY, cW, 16,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color_rainbow"), module.getColorMode() == HudModule.ColorMode.RAINBOW,
                        b -> {
                            module.setColorMode(HudModule.ColorMode.RAINBOW);
                            rebuildWidgets();
                        }
                ));
            }

            // If Welle is active: Color 1 / Color 2 switch
            int pickerStartY = modeY + 20;
            if (module.getColorMode() == HudModule.ColorMode.WAVE) {
                addRenderableWidget(new EzButton(
                        rightX, pickerStartY, btnW, 14,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color1"), activeColorSlot == 1,
                        b -> {
                            activeColorSlot = 1;
                            selectedPresetIndex = -1;
                            syncPickerFromCurrentSlot();
                            rebuildWidgets();
                        }
                ));

                addRenderableWidget(new EzButton(
                        col2X, pickerStartY, btnW, 14,
                        app.ezclient.util.EzI18n.comp("ezclient.hud_settings.color2"), activeColorSlot == 2,
                        b -> {
                            activeColorSlot = 2;
                            selectedPresetIndex = -1;
                            syncPickerFromCurrentSlot();
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

            if (isColorPickerVisible()) {
                // Hex Input Field
                int hexX = hueX + hueW + 12;
                int hexY = svY;
                hexInput = new EditBox(font, hexX, hexY, 60, 13, Component.literal("Hex"));
                hexInput.setMaxLength(8);
                hexInput.setResponder(this::onHexInputChanged);
                addRenderableWidget(hexInput);

                syncPickerFromCurrentSlot();
            } else {
                hexInput = null;
            }
        }

        // ── Reset Button (Bottom Right) ──
        addRenderableWidget(new EzButton(
                panelX + panelWidth - 76, panelY + panelHeight - 26, 64, 16,
                app.ezclient.util.EzI18n.comp("ezclient.hud_settings.reset_btn"), false,
                b -> showResetConfirmation = true
        ));
    }

    private boolean isColorPickerVisible() {
        if (module instanceof CrosshairModule) {
            return crosshairTab == 1;
        }
        if (module.getColorMode() == HudModule.ColorMode.RAINBOW) return false;
        if (module instanceof PotionEffectModule potion && potion.isUseCustomColors()) return false;
        return true;
    }

    private void syncPickerFromCurrentSlot() {
        int color = (activeColorSlot == 1)
                ? (module instanceof CrosshairModule crosshair ? crosshair.getSelectedRuleColor() : module.getTextColor())
                : module.getWaveColor2();
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        currentHue = hsv[0];
        currentSat = hsv[1];
        currentVal = hsv[2];

        updateHexInputText(color);
    }

    private void updateHexInputText(int color) {
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
                applyColorToActiveSlot(fullColor);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                float[] hsv = Color.RGBtoHSB(r, g, b, null);
                currentHue = hsv[0];
                currentSat = hsv[1];
                currentVal = hsv[2];
                selectedPresetIndex = -1;
            }
        } catch (Exception ignored) {}
    }

    private void applyColorToActiveSlot(int color) {
        if (activeColorSlot == 1) {
            if (module instanceof CrosshairModule crosshair) crosshair.setSelectedRuleColor(color);
            else module.setTextColor(color);
        } else {
            module.setWaveColor2(color);
        }
        ConfigManager.save();
    }

    private void updateColorFromHSV() {
        int rgb = Color.HSBtoRGB(currentHue, currentSat, currentVal);
        int fullColor = 0xFF000000 | (rgb & 0xFFFFFF);
        applyColorToActiveSlot(fullColor);
        updateHexInputText(fullColor);
        selectedPresetIndex = -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (showResetConfirmation) {
            int diaW = 240, diaH = 84;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;

            // Yes button
            if (e.x() >= diaX + 14 && e.x() <= diaX + 114 && e.y() >= diaY + 52 && e.y() <= diaY + 72) {
                module.resetSettings();
                showResetConfirmation = false;
                syncPickerFromCurrentSlot();
                rebuildWidgets();
                return true;
            }

            // Cancel button
            if (e.x() >= diaX + 126 && e.x() <= diaX + 226 && e.y() >= diaY + 52 && e.y() <= diaY + 72) {
                showResetConfirmation = false;
                return true;
            }
            return true;
        }

        if (isColorPickerVisible()) {
            double mx = e.x();
            double my = e.y();

            // Check SV Box click
            if (mx >= svX && mx <= svX + svW && my >= svY && my <= svY + svH) {
                isDraggingSV = true;
                currentSat = (float) Math.max(0.0, Math.min(1.0, (mx - svX) / svW));
                currentVal = (float) Math.max(0.0, Math.min(1.0, 1.0 - (my - svY) / svH));
                updateColorFromHSV();
                return true;
            }

            // Check Hue Bar click
            if (mx >= hueX && mx <= hueX + hueW && my >= hueY && my <= hueY + hueH) {
                isDraggingHue = true;
                currentHue = (float) Math.max(0.0, Math.min(1.0, (my - hueY) / hueH));
                updateColorFromHSV();
                return true;
            }

            // Check 3x3 Preset grid click
            int presetGridX = hueX + hueW + 12;
            int presetGridY = svY + 16;
            for (int i = 0; i < 9; i++) {
                int px = presetGridX + (i % 3) * 28;
                int py = presetGridY + (i / 3) * 16;
                if (mx >= px && mx <= px + 26 && my >= py && my <= py + 14) {
                    selectedPresetIndex = i;
                    int presetColor = PRESETS[i];
                    applyColorToActiveSlot(presetColor);
                    syncPickerFromCurrentSlot();
                    return true;
                }
            }
        }

        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        isDraggingSV = false;
        isDraggingHue = false;
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double deltaX, double deltaY) {
        if (isColorPickerVisible()) {
            if (isDraggingSV) {
                currentSat = (float) Math.max(0.0, Math.min(1.0, (e.x() - svX) / svW));
                currentVal = (float) Math.max(0.0, Math.min(1.0, 1.0 - (e.y() - svY) / svH));
                updateColorFromHSV();
                return true;
            }
            if (isDraggingHue) {
                currentHue = (float) Math.max(0.0, Math.min(1.0, (e.y() - hueY) / hueH));
                updateColorFromHSV();
                return true;
            }
        }
        return super.mouseDragged(e, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // ESC
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        extractTransparentBackground(g);

        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        // Header Title (Scaled up, crisp, vertically aligned)
        g.pose().pushMatrix();
        g.pose().translate(panelX + 14, panelY + 9);
        g.pose().scale(1.15f, 1.15f);
        g.text(font, app.ezclient.util.EzI18n.get("ezclient.hud_settings.title", module.getDisplayName()), 0, 0, EzUi.TEXT_WHITE);
        g.pose().popMatrix();

        g.fill(panelX + 14, panelY + 28, panelX + panelWidth - 14, panelY + 29, EzUi.BORDER_SUBTLE);

        // ── LEFT SIDE: Preview Box ──
        int prevX = panelX + 12;
        int prevY = panelY + 38;
        int prevW = 124;
        int previewButtonY = panelY + panelHeight - 26;
        int prevH = Math.max(96, previewButtonY - prevY - 4);

        EzUi.roundedRect(g, prevX, prevY, prevW, prevH, 6, 0x95080A0E);
        // Checkerboard
        int gridSize = 10;
        for (int gy = prevY + 18; gy < prevY + prevH - 2; gy += gridSize) {
            for (int gx = prevX + 2; gx < prevX + prevW - 2; gx += gridSize) {
                if (((gx - prevX) / gridSize + (gy - prevY) / gridSize) % 2 == 0) {
                    g.fill(gx, gy, Math.min(gx + gridSize, prevX + prevW - 2), Math.min(gy + gridSize, prevY + prevH - 2), 0x18FFFFFF);
                }
            }
        }
        g.outline(prevX, prevY, prevW, prevH, EzUi.BORDER_SUBTLE);
        g.text(font, app.ezclient.util.EzI18n.get("ezclient.hud_settings.preview"), prevX + 8, prevY + 6, EzUi.TEXT_MUTED);

        // Render preview safely centered without mutating module position & triggering ConfigManager.save()
        g.enableScissor(prevX + 2, prevY + 18, prevX + prevW - 2, prevY + prevH - 2);

        int mw = module.getWidth(minecraft, true);
        int mh = module.getHeight(minecraft);
        double origScale = module.getScale();
        float fitScale = (float) Math.min(1.0, Math.min((double)(prevW - 16) / Math.max(1.0, mw * origScale), (double)(prevH - 28) / Math.max(1.0, mh * origScale)));

        int targetCenterX = prevX + prevW / 2;
        int targetCenterY = prevY + 22 + (prevH - 24) / 2;

        g.pose().pushMatrix();
        g.pose().translate(targetCenterX, targetCenterY);
        g.pose().scale(fitScale, fitScale);
        g.pose().translate(-module.getX() - (mw * (float) origScale) / 2.0f, -module.getY() - (mh * (float) origScale) / 2.0f);

        HudRenderer.draw(g, module, true);

        g.pose().popMatrix();
        g.disableScissor();

        // ── RIGHT SIDE: Color Picker or Info Banner ──
        if (isColorPickerVisible()) {
            int step = 2;
            for (int py = 0; py < svH; py += step) {
                float v = 1.0f - ((float) py / (float) svH);
                for (int px = 0; px < svW; px += step) {
                    float s = (float) px / (float) svW;
                    int col = 0xFF000000 | (Color.HSBtoRGB(currentHue, s, v) & 0xFFFFFF);
                    g.fill(svX + px, svY + py, svX + px + step, svY + py + step, col);
                }
            }
            g.outline(svX - 1, svY - 1, svW + 2, svH + 2, EzUi.BORDER_SUBTLE);

            int cursorX = svX + (int) (currentSat * svW);
            int cursorY = svY + (int) ((1.0f - currentVal) * svH);
            g.outline(cursorX - 2, cursorY - 2, 5, 5, 0xFFFFFFFF);
            g.outline(cursorX - 3, cursorY - 3, 7, 7, 0xFF000000);

            // Hue Bar
            for (int py = 0; py < hueH; py += 2) {
                float h = (float) py / (float) hueH;
                int col = 0xFF000000 | (Color.HSBtoRGB(h, 1.0f, 1.0f) & 0xFFFFFF);
                g.fill(hueX, hueY + py, hueX + hueW, hueY + py + 2, col);
            }
            g.outline(hueX - 1, hueY - 1, hueW + 2, hueH + 2, EzUi.BORDER_SUBTLE);
            int thumbY = hueY + (int) (currentHue * hueH);
            g.fill(hueX - 1, thumbY - 1, hueX + hueW + 1, thumbY + 2, 0xFFFFFFFF);
            g.outline(hueX - 2, thumbY - 2, hueW + 3, 4, 0xFF000000);

            // Swatch & Hex
            int activeColor = (activeColorSlot == 1)
                    ? (module instanceof CrosshairModule crosshair ? crosshair.getSelectedRuleColor() : module.getTextColor())
                    : module.getWaveColor2();
            int swatchX = hueX + hueW + 6;
            int swatchY = svY;
            EzUi.roundedRect(g, swatchX, swatchY, 14, 13, 2, 0xFF35414D);
            EzUi.roundedRect(g, swatchX + 1, swatchY + 1, 12, 11, 2, activeColor);
            g.text(font, "#", swatchX + 16, swatchY + 3, EzUi.TEXT_MUTED);

            // 3x3 Preset Palette
            int presetGridX = hueX + hueW + 6;
            int presetGridY = svY + 16;
            for (int i = 0; i < 9; i++) {
                int px = presetGridX + (i % 3) * 26;
                int py = presetGridY + (i / 3) * 15;
                EzUi.roundedRect(g, px, py, 23, 12, 2, PRESETS[i]);
                if (selectedPresetIndex == i) {
                    g.outline(px - 1, py - 1, 25, 14, 0xFFFFFFFF);
                }
            }
        } else if (module.getColorMode() == HudModule.ColorMode.RAINBOW) {
            // Sleek Rainbow Mode Card
            int rbX = svX;
            int rbY = svY + 6;
            int rbW = panelWidth - 150;
            int rbH = 58;

            EzUi.roundedRect(g, rbX, rbY, rbW, rbH, 6, 0xF50D111A);
            g.outline(rbX, rbY, rbW, rbH, EzUi.BORDER_SUBTLE);

            // Dynamic rainbow gradient bar
            long now = System.currentTimeMillis();
            for (int bx = 0; bx < rbW - 24; bx++) {
                float hue = ((now % 3000L) / 3000.0f + (float) bx / (float) (rbW - 24)) % 1.0f;
                int rgb = 0xFF000000 | (Color.HSBtoRGB(hue, 0.9f, 1.0f) & 0xFFFFFF);
                g.fill(rbX + 12 + bx, rbY + 12, rbX + 13 + bx, rbY + 17, rgb);
            }

            g.centeredText(font, Component.literal("§eRainbow Modus Aktiv"), rbX + rbW / 2, rbY + 26, EzUi.TEXT_WHITE);
            g.centeredText(font, Component.literal("§7Farben wechseln im Spiel dynamisch"), rbX + rbW / 2, rbY + 40, EzUi.TEXT_MUTED);
        } else if (module instanceof PotionEffectModule potion && potion.isUseCustomColors()) {
            // Sleek Vanilla Effect Colors Info Card
            int infoX = svX;
            int infoY = svY + 6;
            int infoW = panelWidth - 150;
            int infoH = 58;

            EzUi.roundedRect(g, infoX, infoY, infoW, infoH, 6, 0xF50D111A);
            g.outline(infoX, infoY, infoW, infoH, EzUi.BORDER_SUBTLE);

            g.centeredText(font, Component.literal("§a" + app.ezclient.util.EzI18n.get("ezclient.hud_settings.vanilla_colors_title")), infoX + infoW / 2, infoY + 20, EzUi.TEXT_WHITE);
            g.centeredText(font, Component.literal("§7" + app.ezclient.util.EzI18n.get("ezclient.hud_settings.vanilla_colors_desc")), infoX + infoW / 2, infoY + 36, EzUi.TEXT_MUTED);
        }

        super.extractRenderState(g, mx, my, d);

        // Reset Dialog Modal
        if (showResetConfirmation) {
            g.fill(0, 0, width, height, 0xAA000000);
            int diaW = 240, diaH = 84;
            int diaX = (width - diaW) / 2, diaY = (height - diaH) / 2;

            EzUi.roundedRect(g, diaX, diaY, diaW, diaH, 8, 0xF5131A26);
            g.outline(diaX, diaY, diaW, diaH, 0xFF2A3644);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_settings.reset_modal_title"), diaX + diaW / 2, diaY + 12, 0xFFFFFFFF);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_settings.reset_modal_desc"), diaX + diaW / 2, diaY + 28, 0xFF94A3B8);

            boolean hovYes = mx >= diaX + 14 && mx <= diaX + 114 && my >= diaY + 52 && my <= diaY + 72;
            EzUi.roundedRect(g, diaX + 14, diaY + 52, 100, 20, 4, hovYes ? 0xFFDC2626 : 0xFFEF4444);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_settings.yes_reset"), diaX + 14 + 50, diaY + 58, 0xFFFFFFFF);

            boolean hovNo = mx >= diaX + 126 && mx <= diaX + 226 && my >= diaY + 52 && my <= diaY + 72;
            EzUi.roundedRect(g, diaX + 126, diaY + 52, 100, 20, 4, hovNo ? 0xFF475569 : 0xFF334155);
            g.centeredText(font, app.ezclient.util.EzI18n.comp("ezclient.hud_settings.cancel"), diaX + 126 + 50, diaY + 58, 0xFFFFFFFF);
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
}
