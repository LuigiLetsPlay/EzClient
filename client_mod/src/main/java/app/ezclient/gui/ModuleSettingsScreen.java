package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final Module module;

    private int panelX, panelY, panelWidth, panelHeight;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getDisplayName() + " " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "")));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(430, width - 20);
        panelHeight = 220;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        addRenderableWidget(new EzButton(
                panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()
        ));

        int curY = panelY + 38;
        int btnW = 120;
        int col1X = panelX + 16;
        int col2X = panelX + 144;
        int fullW = 248;

        if (module instanceof ScoreboardModule scoreboard) {
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("Rote Zahlen: " + (scoreboard.isRemoveRedNumbers() ? "Aus" : "An")), scoreboard.isRemoveRedNumbers(),
                    b -> { scoreboard.setRemoveRedNumbers(!scoreboard.isRemoveRedNumbers()); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("‹ " + scoreboard.getBackgroundStyle().getLabel() + " ›"), true,
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

            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("Text-Schatten: " + (scoreboard.isTextShadow() ? "An" : "Aus")), scoreboard.isTextShadow(),
                    b -> { scoreboard.setTextShadow(!scoreboard.isTextShadow()); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("IP-Werbung: " + (scoreboard.isHideServerIpFooter() ? "Versteckt" : "Sichtbar")), scoreboard.isHideServerIpFooter(),
                    b -> { scoreboard.setHideServerIpFooter(!scoreboard.isHideServerIpFooter()); rebuildWidgets(); }
            ));
            curY += 20;

            addRenderableWidget(new EzButton(
                    col1X, curY, fullW, 16,
                    Component.literal(String.format("‹ Skalierung: %.2fx ›", scoreboard.getScale())), true,
                    b -> {
                        double s = scoreboard.getScale() >= 1.5 ? 0.75 : scoreboard.getScale() + 0.25;
                        scoreboard.setScale(s);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                double s = scoreboard.getScale() <= 0.75 ? 1.5 : scoreboard.getScale() - 0.25;
                scoreboard.setScale(s);
                rebuildWidgets();
            }));
            curY += 24;
        } else if (module instanceof FovChangerModule fov) {
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("Static FOV: " + (fov.isStaticFovLock() ? "Lock" : "Dynamisch")), fov.isStaticFovLock(),
                    b -> { fov.setStaticFovLock(!fov.isStaticFovLock()); rebuildWidgets(); }
            ));
            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("Sanfte FOV: " + (fov.isSmoothInterpolation() ? "An" : "Aus")), fov.isSmoothInterpolation(),
                    b -> { fov.setSmoothInterpolation(!fov.isSmoothInterpolation()); rebuildWidgets(); }
            ));
            curY += 20;

            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal(String.format("‹ Sprint: %.2fx ›", fov.getSprintMultiplier())), true,
                    b -> {
                        float v = fov.getSprintMultiplier() >= 1.35f ? 0.85f : fov.getSprintMultiplier() + 0.1f;
                        fov.setSprintMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = fov.getSprintMultiplier() <= 0.85f ? 1.35f : fov.getSprintMultiplier() - 0.1f;
                fov.setSprintMultiplier(v);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal(String.format("‹ Speed: %.2fx ›", fov.getSpeedPotionMultiplier())), true,
                    b -> {
                        float v = fov.getSpeedPotionMultiplier() >= 1.35f ? 0.85f : fov.getSpeedPotionMultiplier() + 0.1f;
                        fov.setSpeedPotionMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = fov.getSpeedPotionMultiplier() <= 0.85f ? 1.35f : fov.getSpeedPotionMultiplier() - 0.1f;
                fov.setSpeedPotionMultiplier(v);
                rebuildWidgets();
            }));
            curY += 20;

            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal(String.format("‹ Bogen: %.2fx ›", fov.getBowAimMultiplier())), true,
                    b -> {
                        float v = fov.getBowAimMultiplier() >= 1.0f ? 0.65f : fov.getBowAimMultiplier() + 0.1f;
                        fov.setBowAimMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = fov.getBowAimMultiplier() <= 0.65f ? 1.0f : fov.getBowAimMultiplier() - 0.1f;
                fov.setBowAimMultiplier(v);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal(String.format("‹ Flug: %.2fx ›", fov.getFlyingMultiplier())), true,
                    b -> {
                        float v = fov.getFlyingMultiplier() >= 1.35f ? 0.85f : fov.getFlyingMultiplier() + 0.1f;
                        fov.setFlyingMultiplier(v);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float v = fov.getFlyingMultiplier() <= 0.85f ? 1.35f : fov.getFlyingMultiplier() - 0.1f;
                fov.setFlyingMultiplier(v);
                rebuildWidgets();
            }));
            curY += 24;
        } else if (module instanceof DamageTintModule tint) {
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("‹ " + tint.getTargetScope().getLabel() + " ›"), true,
                    b -> {
                        DamageTintModule.TargetScope[] scopes = DamageTintModule.TargetScope.values();
                        int next = (tint.getTargetScope().ordinal() + 1) % scopes.length;
                        tint.setTargetScope(scopes[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                DamageTintModule.TargetScope[] scopes = DamageTintModule.TargetScope.values();
                int prev = (tint.getTargetScope().ordinal() - 1 + scopes.length) % scopes.length;
                tint.setTargetScope(scopes[prev]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("Chroma: " + (tint.isChromaMode() ? "ON" : "OFF")), tint.isChromaMode(),
                    b -> { tint.setChromaMode(!tint.isChromaMode()); rebuildWidgets(); }
            ));
            curY += 20;

            int[] colors = { 0xFFFF2255, 0xFFFF0000, 0xFF00E5FF, 0xFFFFEA00, 0xFF00FF66, 0xFFAA00FF };
            String[] colorNames = { "Pink-Rot", "Klassisch Rot", "Cyan", "Gelb", "Grün", "Lila" };
            addRenderableWidget(new EzButton(
                    col1X, curY, btnW, 16,
                    Component.literal("‹ Farbe ›"), !tint.isChromaMode(),
                    b -> {
                        int curCol = tint.getCustomColor();
                        int nextIdx = 0;
                        for (int i = 0; i < colors.length; i++) {
                            if (colors[i] == curCol) { nextIdx = (i + 1) % colors.length; break; }
                        }
                        tint.setCustomColor(colors[nextIdx]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int curCol = tint.getCustomColor();
                int prevIdx = 0;
                for (int i = 0; i < colors.length; i++) {
                    if (colors[i] == curCol) { prevIdx = (i - 1 + colors.length) % colors.length; break; }
                }
                tint.setCustomColor(colors[prevIdx]);
                rebuildWidgets();
            }));

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal(String.format("‹ Alpha: %d%% ›", (int) ((tint.getCustomAlpha() / 255.0f) * 100))), true,
                    b -> {
                        int a = tint.getCustomAlpha() >= 240 ? 60 : tint.getCustomAlpha() + 45;
                        tint.setCustomAlpha(a);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                int a = tint.getCustomAlpha() <= 60 ? 240 : tint.getCustomAlpha() - 45;
                tint.setCustomAlpha(a);
                rebuildWidgets();
            }));
            curY += 20;

            addRenderableWidget(new EzButton(
                    col1X, curY, fullW, 16,
                    Component.literal(String.format("‹ Flash-Dauer: %.1fx ›", tint.getFlashDurationMultiplier())), true,
                    b -> {
                        float f = tint.getFlashDurationMultiplier() >= 2.0f ? 0.5f : tint.getFlashDurationMultiplier() + 0.5f;
                        tint.setFlashDurationMultiplier(f);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                float f = tint.getFlashDurationMultiplier() <= 0.5f ? 2.0f : tint.getFlashDurationMultiplier() - 0.5f;
                tint.setFlashDurationMultiplier(f);
                rebuildWidgets();
            }));
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

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("FPS-Schutz: " + (blur.isFpsProtection() ? "ON" : "OFF")), blur.isFpsProtection(),
                    b -> { blur.setFpsProtection(!blur.isFpsProtection()); rebuildWidgets(); }
            ));
            curY += 20;

            addRenderableWidget(new EzButton(
                    col1X, curY, fullW, 16,
                    Component.literal("‹ FPS-Schwelle: " + blur.getFpsThreshold() + " FPS ›"), blur.isFpsProtection(),
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

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("Click-Copy: " + (chat.isCopyOnClick() ? "ON" : "OFF")), chat.isCopyOnClick(),
                    b -> { chat.setCopyOnClick(!chat.isCopyOnClick()); rebuildWidgets(); }
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

            addRenderableWidget(new EzButton(
                    col2X, curY, btnW, 16,
                    Component.literal("Farb-Shift: " + (tnt.isColorShift() ? "ON" : "OFF")), tnt.isColorShift(),
                    b -> { tnt.setColorShift(!tnt.isColorShift()); rebuildWidgets(); }
            ));
            curY += 20;

            addRenderableWidget(new EzButton(
                    col1X, curY, fullW, 16,
                    Component.literal("Durch Wände sehen: " + (tnt.isRenderThroughWalls() ? "ON" : "OFF")), tnt.isRenderThroughWalls(),
                    b -> { tnt.setRenderThroughWalls(!tnt.isRenderThroughWalls()); rebuildWidgets(); }
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

        // Done button
        addRenderableWidget(new EzButton(
                panelX + (panelWidth - 90) / 2, panelY + panelHeight - 24, 90, 16,
                app.ezclient.util.EzI18n.comp("ezclient.module_settings.done"), true,
                b -> onClose()
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        extractTransparentBackground(g);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        g.pose().pushMatrix();
        g.pose().translate(panelX + 14, panelY + 9);
        g.pose().scale(1.15f, 1.15f);
        g.text(font, app.ezclient.util.EzI18n.get("ezclient.module_settings.title", module.getDisplayName()), 0, 0, EzUi.TEXT_WHITE);
        g.pose().popMatrix();

        g.fill(panelX + 14, panelY + 28, panelX + panelWidth - 14, panelY + 29, EzUi.BORDER_SUBTLE);

        // ── Render Live Preview Box on Right Side ──
        int prevX = panelX + 272;
        int prevY = panelY + 36;
        int prevW = Math.max(96, panelX + panelWidth - 14 - prevX);
        int doneButtonY = panelY + panelHeight - 24;
        int prevH = Math.max(96, doneButtonY - prevY - 6);

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

        g.enableScissor(prevX + 2, prevY + 18, prevX + prevW - 2, prevY + prevH - 2);
        renderModulePreview(g, prevX + 6, prevY + 22, prevW - 12, prevH - 26);
        g.disableScissor();

        super.extractRenderState(g, mx, my, d);
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
        } else if (module instanceof DamageTintModule tint) {
            int hurtCol = tint.getCustomColor();
            EzUi.roundedRect(g, px + pw / 2 - 25, py + 15, 50, 60, 6, 0x80101520);
            g.outline(px + pw / 2 - 25, py + 15, 50, 60, hurtCol);
            g.fill(px + pw / 2 - 23, py + 17, px + pw / 2 + 23, py + 73, (hurtCol & 0x00FFFFFF) | ((tint.getCustomAlpha() & 0xFF) << 24));
            g.centeredText(font, Component.literal("Hurt Flash"), px + pw / 2, py + 38, 0xFFFFFFFF);
            g.centeredText(font, Component.literal(tint.isChromaMode() ? "Chroma" : tint.getTargetScope().getLabel()), px + pw / 2, py + 50, hurtCol);
        } else if (module instanceof ChatCustomizerModule chat) {
            int bg = (int) ((chat.getBackgroundOpacity() / 100.0f) * 255.0f) << 24;
            g.fill(px, py + 15, px + pw, py + 65, bg);
            String timePrefix = chat.getTimestampFormat() != ChatCustomizerModule.TimestampFormat.NONE ? "§7[14:30] " : "";
            g.text(font, timePrefix + "§aLu1giLP§f: gg", px + 3, py + 20, 0xFFFFFFFF);
            g.text(font, timePrefix + "§bPlayer2§f: well played!", px + 3, py + 33, 0xFFFFFFFF);
            g.text(font, timePrefix + "§6Server§f: Match ended.", px + 3, py + 46, 0xFFFFFFFF);
        } else if (module instanceof TntTimerModule tnt) {
            EzUi.roundedRect(g, px + pw / 2 - 18, py + 30, 36, 36, 4, 0xFFCC3333);
            g.centeredText(font, Component.literal("TNT"), px + pw / 2, py + 43, 0xFFFFFFFF);
            int tagCol = tnt.isColorShift() ? 0xFFFFAA00 : 0xFFFFFFFF;
            EzUi.roundedRect(g, px + pw / 2 - 20, py + 14, 40, 12, 3, 0x90000000);
            g.centeredText(font, Component.literal("2.45s"), px + pw / 2, py + 16, tagCol);
        } else if (module instanceof AutoGgModule autogg) {
            EzUi.roundedRect(g, px, py + 15, pw, 50, 4, 0x85111722);
            g.text(font, "§6[Server] §aVICTORY!", px + 4, py + 22, 0xFFFFFFFF);
            g.text(font, "§7Sending after " + autogg.getDelayMs() + "ms:", px + 4, py + 35, 0xFFA0A0A0);
            g.text(font, "§fLu1giLP: §a" + autogg.getCustomMessage(), px + 4, py + 48, 0xFFFFFFFF);
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
}
