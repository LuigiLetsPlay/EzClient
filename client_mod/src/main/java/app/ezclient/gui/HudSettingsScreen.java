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

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Modern, unified ScrollingSettingsScreen for all EzClient HudModule instances
 * (FPS, CPS, Coordinates, Ping, Armor Status, Toggle Sprint/Sneak, Day Counter,
 * Clock, Reach, Combo Counter, Potion Effects, Chat Customizer, etc.).
 * Extends ScrollingSettingsScreen for full design unity across EzClient.
 */
public final class HudSettingsScreen extends ScrollingSettingsScreen {
    private final Screen parent;
    private final HudModule module;
    private boolean isListeningForHotkey = false;

    private int panelX, panelY, panelWidth, panelHeight;

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 34; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 32; }

    public HudSettingsScreen(Screen parent, HudModule module) {
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
        module.resetToDefaults();
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

        // Sidebar preview button (if applicable)
        if (module.hasPreview()) {
            int sidebarY = panelY + 102;
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
                b -> resetOpenModule()
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

        // ══════════════════════════════════════════════════════════════
        // CATEGORY 1: MODUL-OPTIONEN (Module-specific settings)
        // ══════════════════════════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14,
                Component.literal(app.ezclient.util.EzI18n.text("Modul-Optionen").toUpperCase(Locale.ROOT))));
        curY += 16;

        if (module instanceof FpsModule fps) {
            String formatStr = switch (fps.getFormatOption()) {
                case LABEL_VALUE -> "Format: FPS: 240";
                case VALUE_LABEL -> "Format: 240 FPS";
                case MINIMAL -> "Format: 240";
            };
            EzButton fmtBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(formatStr), true, b -> {
                FpsModule.FormatOption[] opts = FpsModule.FormatOption.values();
                fps.setFormatOption(opts[(fps.getFormatOption().ordinal() + 1) % opts.length]);
                rebuildWidgets();
            }).withRightClick(b -> {
                FpsModule.FormatOption[] opts = FpsModule.FormatOption.values();
                fps.setFormatOption(opts[(fps.getFormatOption().ordinal() - 1 + opts.length) % opts.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(fmtBtn, "Wählt das Anzeigeformat der Bildrate (z. B. 'FPS: 240' oder '240 FPS')."));

            String smoothStr = "Glättung: " + (fps.getUpdateIntervalMs() == 0 ? "Aus" : fps.getUpdateIntervalMs() + "ms");
            EzButton smoothBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(smoothStr), fps.getUpdateIntervalMs() > 0, b -> {
                int cur = fps.getUpdateIntervalMs();
                int next = cur == 0 ? 250 : (cur == 250 ? 500 : (cur == 500 ? 1000 : 0));
                fps.setUpdateIntervalMs(next);
                rebuildWidgets();
            }).withRightClick(b -> {
                int cur = fps.getUpdateIntervalMs();
                int prev = cur == 0 ? 1000 : (cur == 1000 ? 500 : (cur == 500 ? 250 : 0));
                fps.setUpdateIntervalMs(prev);
                rebuildWidgets();
            });
            addRenderableWidget(described(smoothBtn, "Glättet die FPS-Anzeige über ein rollendes Zeitfenster gegen Schwankungen."));
            curY += 20;

            EzToggleSwitch minMaxSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Min / Max"), fps.isShowMinMax(),
                    state -> { fps.setShowMinMax(state); rebuildWidgets(); });
            addRenderableWidget(described(minMaxSwitch, "Zeigt die minimale und maximale Bildrate (1% Lows & Peaks) an."));

            EzToggleSwitch colorCodingSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Dynamische Farben"), fps.isColorCoding(),
                    state -> { fps.setColorCoding(state); rebuildWidgets(); });
            addRenderableWidget(described(colorCodingSwitch, "Färbt die FPS dynamisch ein (Grün bei >120, Gelb bei >60, Rot bei Drops)."));
            curY += 20;

        } else if (module instanceof CpsModule cps) {
            String modeStr = switch (cps.getDisplayMode()) {
                case COMBINED -> "Modus: Beide Tasten";
                case LMB_ONLY -> "Modus: Nur Links";
                case RMB_ONLY -> "Modus: Nur Rechts";
            };
            EzButton modeBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(modeStr), true, b -> {
                CpsModule.DisplayMode[] modes = CpsModule.DisplayMode.values();
                cps.setDisplayMode(modes[(cps.getDisplayMode().ordinal() + 1) % modes.length]);
                rebuildWidgets();
            }).withRightClick(b -> {
                CpsModule.DisplayMode[] modes = CpsModule.DisplayMode.values();
                cps.setDisplayMode(modes[(cps.getDisplayMode().ordinal() - 1 + modes.length) % modes.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(modeBtn, "Wählt, welche Maustasten im CPS-Zähler erfasst und angezeigt werden."));

            EzToggleSwitch graphSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Klick-Graph"), cps.isShowHistoryGraph(),
                    state -> { cps.setShowHistoryGraph(state); rebuildWidgets(); });
            addRenderableWidget(described(graphSwitch, "Zeigt eine dynamische Sparkline-Verlaufskurve der letzten Klicks unter dem Text an."));
            curY += 20;

            EzToggleSwitch dynColorSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Dynamische Farben"), cps.isDynamicColor(),
                    state -> { cps.setDynamicColor(state); rebuildWidgets(); });
            addRenderableWidget(described(dynColorSwitch, "Hebt die Farbe des CPS-Zählers bei hohen Klickraten optisch hervor."));
            curY += 20;

        } else if (module instanceof CoordinatesModule coords) {
            String layoutStr = switch (coords.getLayoutMode()) {
                case SINGLE_LINE -> "Layout: Einzeilig";
                case MULTI_LINE -> "Layout: Dreizeilig";
                case COMPASS_BAR -> "Layout: Kompass";
            };
            EzButton layoutBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(layoutStr), true, b -> {
                CoordinatesModule.LayoutMode cur = coords.getLayoutMode();
                if (cur == CoordinatesModule.LayoutMode.SINGLE_LINE) coords.setLayoutMode(CoordinatesModule.LayoutMode.MULTI_LINE);
                else if (cur == CoordinatesModule.LayoutMode.MULTI_LINE) coords.setLayoutMode(CoordinatesModule.LayoutMode.COMPASS_BAR);
                else coords.setLayoutMode(CoordinatesModule.LayoutMode.SINGLE_LINE);
                rebuildWidgets();
            });
            addRenderableWidget(described(layoutBtn, "Wechselt zwischen kompakter Einzeiler-, klassischer 3-Zeilen- oder Kompass-Ansicht."));

            String decStr = "Nachkommastellen: " + coords.getDecimalPrecision();
            EzButton decBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(decStr), true, b -> {
                coords.setDecimalPrecision((coords.getDecimalPrecision() + 1) % 3);
                rebuildWidgets();
            });
            addRenderableWidget(described(decBtn, "Anzahl der Dezimalstellen für die Koordinatenanzeige."));
            curY += 20;

            EzToggleSwitch netherSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Nether-Koordinaten"), coords.isShowNether(),
                    state -> { coords.setShowNether(state); rebuildWidgets(); });
            addRenderableWidget(described(netherSwitch, "Berechnet und zeigt automatisch die korrespondierenden Nether-Koordinaten an."));

            EzToggleSwitch biomeSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Biom-Anzeige"), coords.isShowBiome(),
                    state -> { coords.setShowBiome(state); rebuildWidgets(); });
            addRenderableWidget(described(biomeSwitch, "Blendet das aktuelle Minecraft-Biom neben den Koordinaten ein."));
            curY += 20;

        } else if (module instanceof PingModule ping) {
            String pingLayoutStr = switch (ping.getDisplayLayout()) {
                case LABEL_MS -> "Layout: Ping: 20ms";
                case VALUE_ONLY -> "Layout: 20ms";
                case SERVER_AND_PING -> "Layout: Server & Ping";
            };
            EzButton pingLayoutBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(pingLayoutStr), true, b -> {
                PingModule.DisplayLayout[] layouts = PingModule.DisplayLayout.values();
                ping.setDisplayLayout(layouts[(ping.getDisplayLayout().ordinal() + 1) % layouts.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(pingLayoutBtn, "Schaltet zwischen einfacher Latenzanzeige und detailliertem Modus um."));

            String intStr = "Intervall: " + ping.getUpdateIntervalSeconds() + "s";
            EzButton intBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(intStr), true, b -> {
                int cur = ping.getUpdateIntervalSeconds();
                ping.setUpdateIntervalSeconds(cur >= 10 ? 1 : cur + 1);
                rebuildWidgets();
            }).withRightClick(b -> {
                int cur = ping.getUpdateIntervalSeconds();
                ping.setUpdateIntervalSeconds(cur <= 1 ? 10 : cur - 1);
                rebuildWidgets();
            });
            addRenderableWidget(described(intBtn, "Häufigkeit der Ping-Messung in Sekunden."));
            curY += 20;

            EzToggleSwitch alertSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Warnfarben"), ping.isPingAlert(),
                    state -> { ping.setPingAlert(state); rebuildWidgets(); });
            addRenderableWidget(described(alertSwitch, "Färbt die Latenz bei hohem Ping automatisch gelb oder rot."));

            EzToggleSwitch playersSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Spieleranzahl"), ping.isShowPlayerCount(),
                    state -> { ping.setShowPlayerCount(state); rebuildWidgets(); });
            addRenderableWidget(described(playersSwitch, "Zeigt die aktuelle Spielerzahl des Servers im HUD an."));
            curY += 20;

            EzToggleSwitch iconSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Server-Icon"), ping.isShowServerIcon(),
                    state -> { ping.setShowServerIcon(state); rebuildWidgets(); });
            addRenderableWidget(described(iconSwitch, "Zeigt das Server-Favicon links neben dem Ping-Wert an."));
            curY += 20;

        } else if (module instanceof ArmorStatusModule armor) {
            String orientStr = "Ausrichtung: " + (armor.isHorizontal() ? "Horizontal" : "Vertikal");
            EzButton orientBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(orientStr), true, b -> {
                armor.setHorizontal(!armor.isHorizontal());
                rebuildWidgets();
            });
            addRenderableWidget(described(orientBtn, "Richtet die Rüstungsteile waagerecht oder senkrecht aneinander aus."));

            String durStr = "Haltbarkeit: " + switch (armor.getDurabilityMode()) {
                case PERCENT -> "Prozent";
                case HITS -> "Treffer";
                case DAMAGE_BAR -> "Balken";
                case ICON_ONLY -> "Nur Icon";
            };
            EzButton durBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(durStr), true, b -> {
                ArmorStatusModule.DurabilityMode[] modes = ArmorStatusModule.DurabilityMode.values();
                armor.setDurabilityMode(modes[(armor.getDurabilityMode().ordinal() + 1) % modes.length]);
                rebuildWidgets();
            }).withRightClick(b -> {
                ArmorStatusModule.DurabilityMode[] modes = ArmorStatusModule.DurabilityMode.values();
                armor.setDurabilityMode(modes[(armor.getDurabilityMode().ordinal() - 1 + modes.length) % modes.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(durBtn, "Legt fest, wie der Zustand der Rüstung beziffert oder dargestellt wird."));
            curY += 20;

            EzToggleSwitch dynBoxSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Dynamische Box"), armor.isDynamicBox(),
                    state -> { armor.setDynamicBox(state); rebuildWidgets(); });
            addRenderableWidget(described(dynBoxSwitch, "Passt die Hintergrundbox automatisch an die angelegte Ausrüstung an."));

            EzToggleSwitch dmgWarnSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Schadenswarnung"), armor.isDamageWarning(),
                    state -> { armor.setDamageWarning(state); rebuildWidgets(); });
            addRenderableWidget(described(dmgWarnSwitch, "Warnt optisch bei kritisch niedriger Resthaltbarkeit eines Rüstungsteils."));
            curY += 20;

            String eqStr = "Ausrüstung: " + armor.getEquipmentMode().getLabel();
            EzButton eqBtn = new EzButton(col1X, curY, fullW, 16, Component.literal(eqStr), true, b -> {
                ArmorStatusModule.EquipmentMode[] modes = ArmorStatusModule.EquipmentMode.values();
                armor.setEquipmentMode(modes[(armor.getEquipmentMode().ordinal() + 1) % modes.length]);
                rebuildWidgets();
            }).withRightClick(b -> {
                ArmorStatusModule.EquipmentMode[] modes = ArmorStatusModule.EquipmentMode.values();
                armor.setEquipmentMode(modes[(armor.getEquipmentMode().ordinal() - 1 + modes.length) % modes.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(eqBtn, "Wählt, ob getragene Rüstung, Haupthand/Zweithand oder beides dargestellt wird."));
            curY += 20;

        } else if (module instanceof ToggleSprintSneakModule tss) {
            String sprintStr = "Sprint: " + (tss.getSprintMode() == ToggleSprintSneakModule.SprintMode.TOGGLE ? "Toggle" : "Halten");
            EzButton sprintBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(sprintStr), tss.getSprintMode() == ToggleSprintSneakModule.SprintMode.TOGGLE, b -> {
                tss.setSprintMode(tss.getSprintMode() == ToggleSprintSneakModule.SprintMode.TOGGLE ? ToggleSprintSneakModule.SprintMode.HOLD : ToggleSprintSneakModule.SprintMode.TOGGLE);
                rebuildWidgets();
            });
            addRenderableWidget(described(sprintBtn, "Schaltet zwischen dauerhaftem Umschalt-Sprinten (Toggle) und gedrückt Halten um."));

            String sneakStr = "Sneak: " + (tss.getSneakMode() == ToggleSprintSneakModule.SneakMode.TOGGLE ? "Toggle" : "Halten");
            EzButton sneakBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(sneakStr), tss.getSneakMode() == ToggleSprintSneakModule.SneakMode.TOGGLE, b -> {
                tss.setSneakMode(tss.getSneakMode() == ToggleSprintSneakModule.SneakMode.TOGGLE ? ToggleSprintSneakModule.SneakMode.HOLD : ToggleSprintSneakModule.SneakMode.TOGGLE);
                rebuildWidgets();
            });
            addRenderableWidget(described(sneakBtn, "Schaltet zwischen dauerhaftem Umschalt-Schleichen (Toggle) und gedrückt Halten um."));
            curY += 20;

            EzToggleSwitch hideHudSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("HUD verbergen"), tss.isHideHud(),
                    state -> { tss.setHideHud(state); rebuildWidgets(); });
            addRenderableWidget(described(hideHudSwitch, "Blendet die Sprint/Sneak-Statusanzeige im HUD aus."));
            curY += 20;

        } else if (module instanceof DayCounterModule day) {
            EzToggleSwitch daySwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Tageszähler"), day.isShowDay(),
                    state -> { day.setShowDay(state); rebuildWidgets(); });
            addRenderableWidget(described(daySwitch, "Zeigt den aktuellen Ingame-Tag der Spielwelt an."));

            EzToggleSwitch playtimeSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Spielzeit"), day.isShowPlaytime(),
                    state -> { day.setShowPlaytime(state); rebuildWidgets(); });
            addRenderableWidget(described(playtimeSwitch, "Blendet die gesamte Spielzeit auf dieser Welt / diesem Server ein."));
            curY += 20;

            String startStr = "Startzählung: " + (day.isStartAtDayOne() ? "Tag 1" : "Tag 0");
            EzButton startBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(startStr), true, b -> {
                day.setStartAtDayOne(!day.isStartAtDayOne());
                rebuildWidgets();
            });
            addRenderableWidget(described(startBtn, "Wählt, ob die Welt am ersten Tag als Tag 1 oder Tag 0 beginnt."));
            curY += 20;

        } else if (module instanceof ClockModule clock) {
            String clkStr = "Format: " + clock.getClockFormat().getLabel();
            EzButton clkBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(clkStr), true, b -> {
                ClockModule.ClockFormat[] formats = ClockModule.ClockFormat.values();
                clock.setClockFormat(formats[(clock.getClockFormat().ordinal() + 1) % formats.length]);
                rebuildWidgets();
            }).withRightClick(b -> {
                ClockModule.ClockFormat[] formats = ClockModule.ClockFormat.values();
                clock.setClockFormat(formats[(clock.getClockFormat().ordinal() - 1 + formats.length) % formats.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(clkBtn, "Wählt zwischen 24-Stunden- und 12-Stunden-Format."));

            EzToggleSwitch pfxSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Prefix anzeigen"), clock.isShowPrefix(),
                    state -> { clock.setShowPrefix(state); rebuildWidgets(); });
            addRenderableWidget(described(pfxSwitch, "Blendet das 'Uhr:'-Prefix vor der Zeit ein."));
            curY += 20;

        } else if (module instanceof ReachModule reach) {
            String rchFmt = "Format: " + reach.getDisplayFormat().name();
            EzButton rchBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(rchFmt), true, b -> {
                ReachModule.DisplayFormat[] formats = ReachModule.DisplayFormat.values();
                reach.setDisplayFormat(formats[(reach.getDisplayFormat().ordinal() + 1) % formats.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(rchBtn, "Formatierung der angezeigten Schlagdistanz."));

            String precStr = "Nachkommastellen: " + reach.getPrecision();
            EzButton precBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(precStr), true, b -> {
                int p = reach.getPrecision() >= 3 ? 1 : reach.getPrecision() + 1;
                reach.setPrecision(p);
                rebuildWidgets();
            });
            addRenderableWidget(described(precBtn, "Präzision der Distanzberechnung."));
            curY += 20;

            String fadeStr = "Fade: " + reach.getFadeOutDurationMs() + "ms";
            EzButton fadeBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(fadeStr), true, b -> {
                int f = reach.getFadeOutDurationMs() >= 3000 ? 500 : reach.getFadeOutDurationMs() + 500;
                reach.setFadeOutDurationMs(f);
                rebuildWidgets();
            });
            addRenderableWidget(described(fadeBtn, "Dauer bis zum Ausblenden des Treffer-Wertes."));

            EzToggleSwitch rchColSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Farbcodierung"), reach.isColorCoding(),
                    state -> { reach.setColorCoding(state); rebuildWidgets(); });
            addRenderableWidget(described(rchColSwitch, "Färbt die Schlagdistanz abhängig von der Reichweite ein."));
            curY += 20;

        } else if (module instanceof ComboCounterModule combo) {
            String cmbFmt = "Format: " + combo.getDisplayFormat().name();
            EzButton cmbBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(cmbFmt), true, b -> {
                ComboCounterModule.DisplayFormat[] formats = ComboCounterModule.DisplayFormat.values();
                combo.setDisplayFormat(formats[(combo.getDisplayFormat().ordinal() + 1) % formats.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(cmbBtn, "Darstellungsstil des Trefferserien-Zählers."));

            String rstStr = String.format(Locale.ROOT, "Reset-Zeit: %.1fs", combo.getResetWindowSeconds());
            EzButton rstBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(rstStr), true, b -> {
                float r = combo.getResetWindowSeconds() >= 3.0f ? 1.0f : combo.getResetWindowSeconds() + 0.5f;
                combo.setResetWindowSeconds(r);
                rebuildWidgets();
            });
            addRenderableWidget(described(rstBtn, "Zeitspanne ohne neuen Treffer, bis die Combo zurückgesetzt wird."));
            curY += 20;

            EzToggleSwitch msColSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Meilenstein-Farben"), combo.isMilestoneColors(),
                    state -> { combo.setMilestoneColors(state); rebuildWidgets(); });
            addRenderableWidget(described(msColSwitch, "Ändert die Textfarbe bei Erreichen höherer Treffer-Meilensteine."));

            EzToggleSwitch spSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Scale-Punch"), combo.isScalePunch(),
                    state -> { combo.setScalePunch(state); rebuildWidgets(); });
            addRenderableWidget(described(spSwitch, "Lässt die Combo-Zahl bei jedem erfolgreichen Treffer kurz aufploppen."));
            curY += 20;

        } else if (module instanceof PotionEffectModule potion) {
            String styleStr = "Stil: " + switch (potion.getDisplayStyle()) {
                case COMPACT -> "Kompakt";
                case ORIGINAL -> "Original";
                case DETAILED -> "Detailliert";
            };
            EzButton styleBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(styleStr), true, b -> {
                PotionEffectModule.DisplayStyle[] styles = PotionEffectModule.DisplayStyle.values();
                potion.setDisplayStyle(styles[(potion.getDisplayStyle().ordinal() + 1) % styles.length]);
                rebuildWidgets();
            }).withRightClick(b -> {
                PotionEffectModule.DisplayStyle[] styles = PotionEffectModule.DisplayStyle.values();
                potion.setDisplayStyle(styles[(potion.getDisplayStyle().ordinal() - 1 + styles.length) % styles.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(styleBtn, "Darstellungsstil der aktiven Trankeffekte."));

            String orientStr = "Ausrichtung: " + (potion.isVertical() ? "Vertikal" : "Horizontal");
            EzButton orientBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(orientStr), true, b -> {
                potion.setVertical(!potion.isVertical());
                rebuildWidgets();
            });
            addRenderableWidget(described(orientBtn, "Richtet die Effekte vertikal untereinander oder horizontal nebeneinander aus."));
            curY += 20;

            EzToggleSwitch timeSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                    Component.literal("Dauer anzeigen"), potion.isShowTime(),
                    state -> { potion.setShowTime(state); rebuildWidgets(); });
            addRenderableWidget(described(timeSwitch, "Zeigt die verbleibende Restzeit des Trankeffekts an."));

            EzToggleSwitch customColSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Eigene Farben"), potion.isUseCustomColors(),
                    state -> { potion.setUseCustomColors(state); rebuildWidgets(); });
            addRenderableWidget(described(customColSwitch, "Verwendet die individuellen Trankfarben statt der Modulfarbe."));
            curY += 20;

        } else if (module instanceof ChatCustomizerModule chat) {
            String tsStr = "Zeitstempel: " + chat.getTimestampFormat().getLabel();
            EzButton tsBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(tsStr), true, b -> {
                ChatCustomizerModule.TimestampFormat[] formats = ChatCustomizerModule.TimestampFormat.values();
                chat.setTimestampFormat(formats[(chat.getTimestampFormat().ordinal() + 1) % formats.length]);
                rebuildWidgets();
            });
            addRenderableWidget(described(tsBtn, "Fügt Chat-Nachrichten einen genauen Sende-Zeitstempel hinzu."));

            EzToggleSwitch cpSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                    Component.literal("Klick zum Kopieren"), chat.isCopyOnClick(),
                    state -> { chat.setCopyOnClick(state); rebuildWidgets(); });
            addRenderableWidget(described(cpSwitch, "Kopiert eine Chat-Nachricht per Shift-Klick in die Zwischenablage."));
            curY += 20;

            String opStr = "Deckkraft: " + chat.getBackgroundOpacity() + "%";
            EzButton opBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(opStr), true, b -> {
                int op = chat.getBackgroundOpacity() >= 100 ? 0 : chat.getBackgroundOpacity() + 25;
                chat.setBackgroundOpacity(op);
                rebuildWidgets();
            }).withRightClick(b -> {
                int op = chat.getBackgroundOpacity() <= 0 ? 100 : chat.getBackgroundOpacity() - 25;
                chat.setBackgroundOpacity(op);
                rebuildWidgets();
            });
            addRenderableWidget(described(opBtn, "Deckkraft des Chat-Hintergrundfensters in Prozent."));

            String limStr = "Verlauf: " + chat.getLineLimit() + " Zeilen";
            EzButton limBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(limStr), true, b -> {
                int[] limits = { 100, 1000, 5000, 10000 };
                int next = 5000;
                for (int i = 0; i < limits.length; i++) {
                    if (limits[i] == chat.getLineLimit()) { next = limits[(i + 1) % limits.length]; break; }
                }
                chat.setLineLimit(next);
                rebuildWidgets();
            }).withRightClick(b -> {
                int[] limits = { 100, 1000, 5000, 10000 };
                int prev = 5000;
                for (int i = 0; i < limits.length; i++) {
                    if (limits[i] == chat.getLineLimit()) { prev = limits[(i - 1 + limits.length) % limits.length]; break; }
                }
                chat.setLineLimit(prev);
                rebuildWidgets();
            });
            addRenderableWidget(described(limBtn, "Maximale Anzahl der im Chatverlauf gespeicherten Zeilen."));
            curY += 20;

        } else {
            EditBox prefixBox = new EditBox(font, col1X, curY, btnW, 16, Component.literal("Prefix"));
            prefixBox.setHint(Component.literal("Prefix"));
            prefixBox.setValue(module.getPrefix());
            prefixBox.setResponder(val -> { module.setPrefix(val); ConfigManager.save(); });
            addRenderableWidget(described(prefixBox, "Text vor dem Modul-Wert."));

            EditBox suffixBox = new EditBox(font, col2X, curY, btnW, 16, Component.literal("Suffix"));
            suffixBox.setHint(Component.literal("Suffix"));
            suffixBox.setValue(module.getSuffix());
            suffixBox.setResponder(val -> { module.setSuffix(val); ConfigManager.save(); });
            addRenderableWidget(described(suffixBox, "Text nach dem Modul-Wert."));
            curY += 20;
        }

        curY += 6;

        // ══════════════════════════════════════════════════════════════
        // CATEGORY 2: TEXTFARBE & EFFEKTE
        // ══════════════════════════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14,
                Component.literal(app.ezclient.util.EzI18n.text("Textfarbe & Effekte").toUpperCase(Locale.ROOT))));
        curY += 16;

        String modeLabel = "Farbmodus: " + switch (module.getColorMode()) {
            case SOLID -> "Einfarbig";
            case WAVE -> "Welle";
            case RAINBOW -> "Regenbogen";
        };
        EzButton colorModeBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(modeLabel), true, b -> {
            HudModule.ColorMode[] modes = HudModule.ColorMode.values();
            module.setColorMode(modes[(module.getColorMode().ordinal() + 1) % modes.length]);
            ConfigManager.save();
            rebuildWidgets();
        }).withRightClick(b -> {
            HudModule.ColorMode[] modes = HudModule.ColorMode.values();
            module.setColorMode(modes[(module.getColorMode().ordinal() - 1 + modes.length) % modes.length]);
            ConfigManager.save();
            rebuildWidgets();
        });
        addRenderableWidget(described(colorModeBtn, "Wählt zwischen statischer Einzelfarbe, zweifarbiger Welle oder dynamischem Regenbogen."));

        EzToggleSwitch shadowSwitch = new EzToggleSwitch(col2X, curY, btnW, 16,
                Component.literal("Textschatten"), module.isTextShadow(),
                state -> { module.setTextShadow(state); ConfigManager.save(); rebuildWidgets(); });
        addRenderableWidget(described(shadowSwitch, "Zeichnet einen dezenten Textschatten für bessere Lesbarkeit auf jedem Hintergrund."));
        curY += 20;

        if (module.getColorMode() != HudModule.ColorMode.RAINBOW) {
            addColorOption(col1X, curY, (module.getColorMode() == HudModule.ColorMode.WAVE ? btnW : fullW),
                    "Textfarbe", "Grundfarbe des Textes (öffnet den Hex-Farbwähler).",
                    module.getTextColor(),
                    newCol -> { module.setTextColor(newCol); ConfigManager.save(); rebuildWidgets(); }
            );
            if (module.getColorMode() == HudModule.ColorMode.WAVE) {
                addColorOption(col2X, curY, btnW,
                        "Wellenfarbe 2", "Zweite Farbe für den Wellenfarbverlauf (öffnet den Hex-Farbwähler).",
                        module.getWaveColor2(),
                        newCol -> { module.setWaveColor2(newCol); ConfigManager.save(); rebuildWidgets(); }
                );
            }
            curY += 20;
        }

        curY += 6;

        // ══════════════════════════════════════════════════════════════
        // CATEGORY 3: BOX & RAHMEN
        // ══════════════════════════════════════════════════════════════
        addRenderableWidget(new CategoryHeader(col1X, curY, fullW, 14,
                Component.literal(app.ezclient.util.EzI18n.text("Box & Rahmen").toUpperCase(Locale.ROOT))));
        curY += 16;

        EzToggleSwitch boxSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                Component.literal("Hintergrund-Box"), module.hasBackground(),
                state -> { module.setBackground(state); ConfigManager.save(); rebuildWidgets(); });
        addRenderableWidget(described(boxSwitch, "Aktiviert oder deaktiviert den halbtransparenten Hintergrundkasten."));

        addColorOption(col2X, curY, btnW,
                "Hintergrundfarbe", "Farbe und Transparenz der Hintergrund-Box (öffnet den Hex-Farbwähler).",
                module.getBackgroundColor(),
                newCol -> { module.setBackgroundColor(newCol); ConfigManager.save(); rebuildWidgets(); }
        );
        curY += 20;

        EzToggleSwitch borderSwitch = new EzToggleSwitch(col1X, curY, btnW, 16,
                Component.literal("Rahmen"), module.hasBorder(),
                state -> { module.setBorder(state); ConfigManager.save(); rebuildWidgets(); });
        addRenderableWidget(described(borderSwitch, "Zeichnet eine dekorative Kontur um das HUD-Modul."));

        String styleLabel = "Stil: " + module.getBorderStyle().getLabel();
        EzButton styleBtn = new EzButton(col2X, curY, btnW, 16, Component.literal(styleLabel), module.hasBorder(), b -> {
            HudModule.BorderStyle[] styles = HudModule.BorderStyle.values();
            module.setBorderStyle(styles[(module.getBorderStyle().ordinal() + 1) % styles.length]);
            ConfigManager.save();
            rebuildWidgets();
        }).withRightClick(b -> {
            HudModule.BorderStyle[] styles = HudModule.BorderStyle.values();
            module.setBorderStyle(styles[(module.getBorderStyle().ordinal() - 1 + styles.length) % styles.length]);
            ConfigManager.save();
            rebuildWidgets();
        });
        addRenderableWidget(described(styleBtn, "Wählt das visuelle Design des Modulrahmens."));
        curY += 20;

        if (module.hasBorder()) {
            String borderModeLabel = "Rahmen-Farbmodus: " + switch (module.getBorderColorMode()) {
                case SOLID -> "Einfarbig";
                case WAVE -> "Welle";
                case RAINBOW -> "Regenbogen";
            };
            EzButton borderModeBtn = new EzButton(col1X, curY, btnW, 16, Component.literal(borderModeLabel), true, b -> {
                HudModule.ColorMode[] modes = HudModule.ColorMode.values();
                module.setBorderColorMode(modes[(module.getBorderColorMode().ordinal() + 1) % modes.length]);
                ConfigManager.save();
                rebuildWidgets();
            }).withRightClick(b -> {
                HudModule.ColorMode[] modes = HudModule.ColorMode.values();
                module.setBorderColorMode(modes[(module.getBorderColorMode().ordinal() - 1 + modes.length) % modes.length]);
                ConfigManager.save();
                rebuildWidgets();
            });
            addRenderableWidget(described(borderModeBtn, "Farbmodus der Rahmenkontur."));

            if (module.getBorderColorMode() != HudModule.ColorMode.RAINBOW) {
                addColorOption(col2X, curY, btnW,
                        "Rahmenfarbe", "Hauptfarbe des Rahmens (öffnet den Hex-Farbwähler).",
                        module.getBorderColor(),
                        newCol -> { module.setBorderColor(newCol); ConfigManager.save(); rebuildWidgets(); }
                );
                curY += 20;

                if (module.getBorderColorMode() == HudModule.ColorMode.WAVE) {
                    addColorOption(col1X, curY, btnW,
                            "Rahmen-Wellenfarbe 2", "Zweite Farbe für den Rahmen-Wellenverlauf (öffnet den Hex-Farbwähler).",
                            module.getBorderWaveColor2(),
                            newCol -> { module.setBorderWaveColor2(newCol); ConfigManager.save(); rebuildWidgets(); }
                    );
                    curY += 20;
                }
            } else {
                curY += 20;
            }
        }
    }

    private void addColorOption(int x, int y, int width, String label, String desc, int currentColor, IntConsumer colorSetter) {
        int swatchW = 20;
        int btnW = width - swatchW - 3;
        EzButton btn = new EzButton(
                x, y, btnW, 16,
                Component.literal(app.ezclient.util.EzI18n.text(label) + " …"), true,
                b -> openColorPicker(label, currentColor, colorSetter)
        );
        ColorSwatchButton swatch = new ColorSwatchButton(
                x + btnW + 3, y, swatchW, 16,
                currentColor,
                b -> openColorPicker(label, currentColor, colorSetter)
        );
        addRenderableWidget(described(btn, desc));
        addRenderableWidget(described(swatch, desc));
    }

    private void openColorPicker(String title, int currentColor, IntConsumer onColorChanged) {
        if (minecraft == null) return;
        EzScreenBridge.set(minecraft, new ModuleColorScreen(this, title, currentColor, onColorChanged));
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
