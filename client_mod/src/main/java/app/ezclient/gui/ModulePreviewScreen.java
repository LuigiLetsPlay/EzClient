package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.Locale;

/**
 * Full-world live preview shared by every module settings screen.
 * Every single module in EzClient features a dedicated, animated, and informative live preview.
 */
public final class ModulePreviewScreen extends Screen {
    private final Screen parent;
    private final Module module;

    public ModulePreviewScreen(Screen parent, Module module) {
        super(Component.literal(app.ezclient.util.EzI18n.text("Live-Vorschau")));
        this.parent = parent;
        this.module = module;
    }

    private void returnToSettings() {
        if (minecraft != null) EzScreenBridge.set(minecraft, parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        // 1. Crosshair Module (Centered HUD Reticle)
        if (module instanceof CrosshairModule crosshair) {
            renderCrosshairPreview(g, crosshair);
        }
        // 2. Armor Status Preview
        else if (module instanceof ArmorStatusModule armor) {
            renderArmorStatusPreview(g, armor);
        }
        // 3. Boss Bar Customizer Preview
        else if (module instanceof BossBarModule boss) {
            renderBossBarPreview(g, boss);
        }
        // 4. Chat Customizer Preview
        else if (module instanceof ChatCustomizerModule chat) {
            renderChatPreview(g, chat);
        }
        // 5. HUD Modules (Always rendered live on screen + Info card)
        else if (module instanceof HudModule hud) {
            int previewTop = 60;
            int previewBottom = Math.max(previewTop + 1, height - 36);
            HudRenderer.drawCenteredPreview(
                    g, hud,
                    width / 2,
                    previewTop + (previewBottom - previewTop) / 2,
                    Math.max(1, width - 40),
                    Math.max(1, previewBottom - previewTop - 20)
            );
            renderHudCard(g, hud);
        }
        // 6. Item Physics (User's prime showcase: flat items, ground rotation & trajectory arc)
        else if (module instanceof ItemPhysicsModule physics) {
            renderItemPhysicsPreview(g, physics);
        }
        // 8. TNT Timer (3D Primed TNT Countdown Preview)
        else if (module instanceof TntTimerModule tnt) {
            renderTntTimerPreview(g, tnt);
        }
        // 9. Hitbox Visualizer Preview
        else if (module instanceof HitboxModule hitbox) {
            renderHitboxPreview(g, hitbox);
        }
        // 10. Block Overlay Preview
        else if (module instanceof BlockOverlayModule blockOverlay) {
            renderBlockOverlayPreview(g, blockOverlay);
        }
        // 11. Bedwars Overlay Preview
        else if (module instanceof BedwarsModule bedwars) {
            renderBedwarsPreview(g, bedwars);
        }
        // 12. Scoreboard Customizer Preview
        else if (module instanceof ScoreboardModule scoreboard) {
            renderScoreboardPreview(g, scoreboard);
        }
        // 13. Nameplate Module Preview
        else if (module instanceof NameplateModule nameplate) {
            renderNameplatePreview(g, nameplate);
        }
        // 14. Time & Weather Changer Preview
        else if (module instanceof TimeWeatherModule tw) {
            renderTimeWeatherPreview(g, tw);
        }
        // 15. Particle Customizer Preview
        else if (module instanceof ParticleCustomizerModule particle) {
            renderParticlePreview(g, particle);
        }
        // 16. Sound Enhancer Subtitles Preview
        else if (module instanceof SoundEnhancerModule sound) {
            renderSoundEnhancerPreview(g, sound);
        }
        // 17. AutoGG Match End Preview
        else if (module instanceof AutoGgModule autoGg) {
            renderAutoGgPreview(g, autoGg);
        }
        // 18. Motion Blur Preview
        else if (module instanceof MotionBlurModule mb) {
            renderMotionBlurPreview(g, mb);
        }
        // 19. FOV Changer Corridor Preview
        else if (module instanceof FovChangerModule fov) {
            renderFovPreview(g, fov);
        }
        // 20. Zoom Module Lens Preview
        else if (module instanceof ZoomModule zoom) {
            renderZoomPreview(g, zoom);
        }
        // 21. Waypoints 3D Beacon Preview
        else if (module instanceof WaypointsModule waypoints) {
            renderWaypointsPreview(g, waypoints);
        }
        // 22. Fullbright Gamma Split Preview
        else if (module instanceof FullbrightModule fb) {
            renderFullbrightPreview(g, fb);
        }
        // 23. Clear Glass Texture Preview
        else if (module instanceof ClearGlassModule glass) {
            renderClearGlassPreview(g, glass);
        }
        // 24. Toggle Sprint & Sneak Preview
        else if (module instanceof ToggleSprintSneakModule sprint) {
            renderToggleSprintPreview(g, sprint);
        }
        // 25. General Fallback Card
        else {
            renderGeneralPreview(g, module);
        }

        // Bottom Navigation & Return Hint
        String hint = "◉  " + module.getDisplayName() + "  ·  Klick oder ESC zum Zurückkehren";
        int hintWidth = Math.min(width - 16, font.width(hint) + 24);
        int hintX = (width - hintWidth) / 2;
        EzUi.roundedRect(g, hintX, height - 27, hintWidth, 19, 5, 0xE80D1117);
        g.outline(hintX, height - 27, hintWidth, 19, EzUi.BORDER_SUBTLE);
        g.centeredText(font, Component.literal(hint), width / 2, height - 21, EzUi.TEXT_LIGHT);
    }

    // ── 1. HUD Card Overlay ──
    private void renderHudCard(GuiGraphicsExtractor g, HudModule hud) {
        int cardW = 240, cardH = 36;
        int cardX = (width - cardW) / 2, cardY = 16;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 5, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        ModuleIconRenderer.draw(g, hud, cardX + 8, cardY + 9, 18);
        g.text(font, Component.literal(hud.getDisplayName() + " HUD-Liveanzeige"), cardX + 32, cardY + 7, EzUi.TEXT_WHITE);
        String details = "Skalierung: " + String.format(Locale.ROOT, "%.2fx", (float) hud.getScale())
                + "  ·  " + (hud.hasBackground() ? "Hintergrund An" : "Hintergrund Aus");
        g.text(font, Component.literal(details), cardX + 32, cardY + 19, EzUi.TEXT_MUTED);
    }

    // ── 1b. Armor Status Preview ──
    private void renderArmorStatusPreview(GuiGraphicsExtractor g, ArmorStatusModule armor) {
        int previewTop = 45;
        int previewBottom = Math.max(previewTop + 1, height - 76);
        HudRenderer.drawCenteredPreview(
                g, armor,
                width / 2,
                previewTop + (previewBottom - previewTop) / 2,
                Math.max(1, width - 40),
                Math.max(1, previewBottom - previewTop - 20)
        );

        int cardW = 310, cardH = 52;
        int cardX = (width - cardW) / 2, cardY = height - 84;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 5, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        ModuleIconRenderer.draw(g, armor, cardX + 8, cardY + 9, 20);
        g.text(font, Component.literal("Armor Status – Ausrüstungs-Vorschau"), cardX + 34, cardY + 7, EzUi.TEXT_WHITE);
        String details = "Modus: " + armor.getDurabilityMode().name() + "  ·  Slots: " + armor.getEquipmentMode().getLabel();
        g.text(font, Component.literal(details), cardX + 34, cardY + 20, EzUi.TEXT_MUTED);
        String sub = "Layout: " + (armor.isHorizontal() ? "Horizontal" : "Vertikal") + "  ·  Warnung: " + (armor.isDamageWarning() ? "Aktiv (<10%)" : "Aus");
        g.text(font, Component.literal(sub), cardX + 34, cardY + 34, EzUi.TEXT_DIM);
    }

    // ── 2. Crosshair Preview ──
    private void renderCrosshairPreview(GuiGraphicsExtractor g, CrosshairModule crosshair) {
        crosshair.renderCrosshair(g, minecraft, width / 2.0f, height / 2.0f, false);

        int cardW = 220, cardH = 46;
        int cardX = (width - cardW) / 2, cardY = height / 2 + 28;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 5, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);
        g.centeredText(font, Component.literal("Fadenkreuz Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);
        String sub = "Pixel-Crosshair aktiv";
        g.centeredText(font, Component.literal(sub), cardX + cardW / 2, cardY + 24, EzUi.TEXT_MUTED);
    }

    // ── 3. Item Physics Preview ──
    private void renderItemPhysicsPreview(GuiGraphicsExtractor g, ItemPhysicsModule physics) {
        int cardW = 264, cardH = 152;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Item Physics – 3D Physik-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        // Ground / floor simulation box (Wood floor)
        int boxX = cardX + 12, boxY = cardY + 23, boxW = cardW - 24, boxH = 74;
        EzUi.roundedRect(g, boxX, boxY, boxW, boxH, 4, 0xFF191714);
        g.outline(boxX, boxY, boxW, boxH, 0xFF3D3225);

        // Wooden planks perspective floor
        int floorY = boxY + 36;
        g.fill(boxX, floorY, boxX + boxW, boxY + boxH, 0xFF4A3525);
        g.fill(boxX, floorY, boxX + boxW, floorY + 1, 0xFF6D4E36);
        g.fill(boxX, floorY + 18, boxX + boxW, floorY + 19, 0xFF35261A);

        long time = System.currentTimeMillis();
        boolean isRotating = "Rotating".equalsIgnoreCase(physics.text("mode"));
        double speed = Math.max(0.1, physics.number("speed"));
        boolean trajectory = physics.flag("physics");

        // Item 1: Diamond Sword (lying flat or spinning on floor)
        int it1X = boxX + 34, it1Y = floorY + 10;
        g.pose().pushMatrix();
        if (isRotating) {
            double angle = (time * 0.003 * speed);
            float scaleX = (float) Math.cos(angle);
            g.pose().translate(it1X + 8, it1Y + 8);
            g.pose().scale(scaleX, 1.0f);
            g.pose().translate(-it1X - 8, -it1Y - 8);
        } else {
            g.pose().translate(it1X + 8, it1Y + 8);
            g.pose().scale(1.1f, 0.45f);
            g.pose().translate(-it1X - 8, -it1Y - 8);
        }
        EzUi.roundedRect(g, it1X - 2, it1Y + 10, 20, 5, 2, 0x55000000);
        g.item(new ItemStack(Items.DIAMOND_SWORD), it1X, it1Y);
        g.pose().popMatrix();

        // Item 2: Golden Apple
        int it2X = boxX + 86, it2Y = floorY + 11;
        g.pose().pushMatrix();
        if (isRotating) {
            double angle = (time * 0.003 * speed + 1.2);
            float scaleX = (float) Math.cos(angle);
            g.pose().translate(it2X + 8, it2Y + 8);
            g.pose().scale(scaleX, 1.0f);
            g.pose().translate(-it2X - 8, -it2Y - 8);
        } else {
            g.pose().translate(it2X + 8, it2Y + 8);
            g.pose().scale(1.0f, 0.6f);
            g.pose().translate(-it2X - 8, -it2Y - 8);
        }
        EzUi.roundedRect(g, it2X - 1, it2Y + 10, 18, 5, 2, 0x55000000);
        g.item(new ItemStack(Items.GOLDEN_APPLE), it2X, it2Y);
        g.pose().popMatrix();

        // Item 3: Flying item on a trajectory arc (if trajectory physics is on) or resting Ender Pearl
        if (trajectory) {
            double progress = ((time % 2000) / 2000.0);
            int startX = boxX + 130, startY = boxY + 12;
            int endX = boxX + boxW - 28, endY = floorY + 10;
            int curX = (int) (startX + (endX - startX) * progress);
            int arcPeakY = boxY + 6;
            int curY = (int) (startY + progress * (endY - startY) - 4.0 * (startY - arcPeakY) * progress * (1.0 - progress));

            // Parabolic trajectory guide dots
            for (double p = 0.0; p <= 1.0; p += 0.09) {
                int px = (int) (startX + (endX - startX) * p);
                int py = (int) (startY + p * (endY - startY) - 4.0 * (startY - arcPeakY) * p * (1.0 - p));
                g.fill(px, py, px + 2, py + 2, 0x6600D2FF);
            }

            g.pose().pushMatrix();
            double spin = (progress * 720.0 * speed);
            float sx = (float) Math.cos(Math.toRadians(spin));
            g.pose().translate(curX + 8, curY + 8);
            g.pose().scale(sx, 1.0f);
            g.pose().translate(-curX - 8, -curY - 8);
            g.item(new ItemStack(Items.EMERALD), curX, curY);
            g.pose().popMatrix();
        } else {
            int it3X = boxX + 175, it3Y = floorY + 11;
            EzUi.roundedRect(g, it3X - 1, it3Y + 10, 18, 5, 2, 0x55000000);
            g.item(new ItemStack(Items.ENDER_PEARL), it3X, it3Y);
        }

        // Details below
        String row1 = "Bodendrehung: " + physics.text("mode") + "  ·  Geschwindigkeit: " + String.format(Locale.ROOT, "%.1fx", physics.number("speed"));
        g.centeredText(font, Component.literal(row1), cardX + cardW / 2, cardY + 104, EzUi.TEXT_WHITE);
        String row2 = "Flugbahn-Physik: " + (trajectory ? "Aktiviert" : "Aus") + "  ·  Item-Cap: " + (int) physics.number("cap");
        g.centeredText(font, Component.literal(row2), cardX + cardW / 2, cardY + 118, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Gegenstände fallen flach auf den Boden statt zu schweben"), cardX + cardW / 2, cardY + 132, EzUi.TEXT_DIM);
    }


    // ── 5. TNT Timer Preview ──
    private void renderTntTimerPreview(GuiGraphicsExtractor g, TntTimerModule tnt) {
        long time = System.currentTimeMillis();
        float remaining = 4.0f - ((time % 4000) / 1000.0f);

        int cardW = 220, cardH = 120;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("TNT Timer – Live Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        String timeStr = String.format(Locale.ROOT, "%." + tnt.getPrecision() + "fs", remaining);
        int tagColor;
        if (!tnt.isColorShift()) {
            tagColor = 0xFFFFFFFF;
        } else if (remaining > 3.0f) {
            tagColor = 0xFF2ED573;
        } else if (remaining > 1.5f) {
            tagColor = 0xFFFFA502;
        } else {
            tagColor = ((time / 150) % 2 == 0) ? 0xFFFF4757 : 0xFFFFFFFF;
        }

        int tagW = font.width(timeStr) + 14;
        int tagX = cardX + (cardW - tagW) / 2;
        int tagY = cardY + 26;
        EzUi.roundedRect(g, tagX, tagY, tagW, 14, 3, 0xCC000000);
        g.outline(tagX, tagY, tagW, 14, 0x40FFFFFF);
        g.centeredText(font, Component.literal(timeStr), tagX + tagW / 2, tagY + 3, tagColor);

        int bSize = 28;
        int bX = cardX + (cardW - bSize) / 2;
        int bY = tagY + 18;
        boolean whiteFlash = remaining <= 1.5f && ((time / 150) % 2 == 0);
        int tntBg = whiteFlash ? 0xFFFFFFFF : 0xFFD63031;
        EzUi.roundedRect(g, bX, bY, bSize, bSize, 3, tntBg);
        int bandH = 8;
        g.fill(bX, bY + (bSize - bandH) / 2, bX + bSize, bY + (bSize + bandH) / 2, whiteFlash ? 0xFFDDDDDD : 0xFFF5F6FA);
        g.centeredText(font, Component.literal("TNT"), bX + bSize / 2, bY + 10, 0xFF2D3436);

        String stats = "Präzision: " + tnt.getPrecision() + " Dec  ·  Farbwechsel: " + (tnt.isColorShift() ? "An" : "Aus");
        g.centeredText(font, Component.literal(stats), cardX + cardW / 2, cardY + 86, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Durch Wände: " + (tnt.isRenderThroughWalls() ? "Aktiviert" : "Deaktiviert")),
                cardX + cardW / 2, cardY + 99, EzUi.TEXT_DIM);
    }

    // ── 6. Hitbox Preview ──
    private void renderHitboxPreview(GuiGraphicsExtractor g, HitboxModule hitbox) {
        int cardW = 230, cardH = 135;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Hitbox Visualizer – Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int boxColor = hitbox.tint("box", false);
        int boxW = 36, boxH = 52;
        int cx = cardX + cardW / 2, cy = cardY + 58;
        int bx1 = cx - boxW / 2, by1 = cy - boxH / 2;

        if (hitbox.flag("fill")) {
            EzUi.roundedRect(g, bx1, by1, boxW, boxH, 2, hitbox.tint("fillColor", false));
        }

        g.outline(bx1, by1, boxW, boxH, boxColor);
        g.outline(bx1 - 4, by1 - 4, boxW, boxH, boxColor);
        g.fill(bx1 - 4, by1 - 4, bx1, by1 - 3, boxColor);
        g.fill(bx1 + boxW - 4, by1 - 4, bx1 + boxW, by1 - 3, boxColor);
        g.fill(bx1 - 4, by1 + boxH - 4, bx1, by1 + boxH - 3, boxColor);
        g.fill(bx1 + boxW - 4, by1 + boxH - 4, bx1 + boxW, by1 + boxH - 3, boxColor);

        if (hitbox.flag("eyes")) {
            int eyeY = by1 + (int) (boxH * 0.22f);
            g.fill(bx1 - 2, eyeY, bx1 + boxW + 2, eyeY + 1, hitbox.tint("eyeColor", false));
        }

        if (hitbox.flag("look")) {
            int eyeY = by1 + (int) (boxH * 0.22f);
            g.fill(cx, eyeY, cx + 18, eyeY + 1, hitbox.tint("lookColor", false));
            g.fill(cx + 17, eyeY - 2, cx + 19, eyeY + 3, hitbox.tint("lookColor", false));
        }

        String details = "Kontur: " + String.format(Locale.ROOT, "%.1fpx", (float) hitbox.number("width"))
                + "  ·  Augen: " + (hitbox.flag("eyes") ? "An" : "Aus")
                + "  ·  Blick: " + (hitbox.flag("look") ? "An" : "Aus");
        g.centeredText(font, Component.literal(details), cardX + cardW / 2, cardY + 98, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("EntityType-Regeln können separat angepasst werden"), cardX + cardW / 2, cardY + 112, EzUi.TEXT_DIM);
    }

    // ── 7. Block Overlay Preview ──
    private void renderBlockOverlayPreview(GuiGraphicsExtractor g, BlockOverlayModule block) {
        int cardW = 200, cardH = 115;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Block Overlay – Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int bSize = 38;
        int bx = cardX + (cardW - bSize) / 2, by = cardY + 28;
        int color = block.tint("color", false);

        if (block.flag("fill")) {
            EzUi.roundedRect(g, bx, by, bSize, bSize, 2, block.tint("fillColor", false));
        }
        g.outline(bx, by, bSize, bSize, color);
        g.outline(bx + 4, by - 4, bSize, bSize, color);
        g.fill(bx, by - 4, bx + 4, by - 3, color);
        g.fill(bx + bSize, by - 4, bx + bSize + 4, by - 3, color);
        g.fill(bx, by + bSize - 4, bx + 4, by + bSize - 3, color);
        g.fill(bx + bSize, by + bSize - 4, bx + bSize + 4, by + bSize - 3, color);

        String info = "Stärke: " + String.format(Locale.ROOT, "%.1fpx", (float) block.number("lineWidth"))
                + "  ·  Füllung: " + (block.flag("fill") ? "An" : "Aus");
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 84, EzUi.TEXT_MUTED);
    }

    // ── 8. Bedwars Preview ──
    private void renderBedwarsPreview(GuiGraphicsExtractor g, BedwarsModule bedwars) {
        int cardW = 240, cardH = 120;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Hypixel Bedwars Overlay"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int rowY = cardY + 26;
        if (bedwars.flag("upgradeTimers")) {
            g.centeredText(font, Component.literal("§bDiamant II: §f0:24   §aSmaragd II: §f0:48"), cardX + cardW / 2, rowY, 0xFFFFFFFF);
            rowY += 15;
        }
        if (bedwars.flag("teamBeds")) {
            g.centeredText(font, Component.literal("§cR: §a✓   §9B: §a2   §aG: §c✗   §eY: §a✓"), cardX + cardW / 2, rowY, 0xFFFFFFFF);
            rowY += 15;
        }
        if (bedwars.flag("inventory")) {
            g.centeredText(font, Component.literal("§f16 Eisen   §64 Gold   §b2 Diamanten"), cardX + cardW / 2, rowY, 0xFFFFFFFF);
            rowY += 15;
        }

        g.centeredText(font, Component.literal("Automatisches Hypixel HUD-Overlay aktiv"), cardX + cardW / 2, cardY + 98, EzUi.TEXT_DIM);
    }

    // ── 9. Scoreboard Preview ──
    private void renderScoreboardPreview(GuiGraphicsExtractor g, ScoreboardModule scoreboard) {
        int sbW = 120, sbH = 145;
        int sbX = width - sbW - 14, sbY = (height - sbH) / 2;
        int bgAlpha = (int) (scoreboard.getBackgroundAlpha() * 2.55f);
        int bgColor = (bgAlpha << 24) | 0x000000;
        EzUi.roundedRect(g, sbX, sbY, sbW, sbH, 4, bgColor);
        g.outline(sbX, sbY, sbW, sbH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("§e§lBED WARS"), sbX + sbW / 2, sbY + 6, 0xFFFFFF55);
        int ly = sbY + 20;
        String[] mockLines = {
                "§724/09/26 §8m128",
                "§fR §cRot: §a✓",
                "§fB §9Blau: §a2",
                "§fG §aGrün: §c✗",
                "§fY §eGelb: §a✓",
                "§fKills: §a4",
                "§fFinal Kills: §a2",
                "§fBetten: §a1",
                "§ewww.hypixel.net"
        };
        for (int i = 0; i < mockLines.length; i++) {
            g.text(font, Component.literal(mockLines[i]), sbX + 6, ly, 0xFFFFFFFF);
            if (!scoreboard.isRemoveRedNumbers()) {
                g.text(font, Component.literal("§c" + (mockLines.length - i)), sbX + sbW - 14, ly, 0xFFFF5555);
            }
            ly += 12;
        }
    }

    // ── 10. Chat Preview ──
    private void renderChatPreview(GuiGraphicsExtractor g, ChatCustomizerModule chat) {
        int cardW = 264, cardH = 145;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Chat – Live Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int chatX = cardX + 12;
        int chatY = cardY + 28;
        ChatCustomizerModule.renderDummyChat(g, minecraft, chat, chatX, chatY, cardW - 24, 1.0);

        String row1 = "Deckkraft: " + chat.getBackgroundOpacity() + "%  ·  Zeitstempel: " + chat.getTimestampFormat().getLabel();
        g.centeredText(font, Component.literal(row1), cardX + cardW / 2, cardY + 98, EzUi.TEXT_WHITE);
        String row2 = "Zeilenlimit: " + chat.getLineLimit() + "  ·  Klick-Kopieren: " + (chat.isCopyOnClick() ? "An" : "Aus");
        g.centeredText(font, Component.literal(row2), cardX + cardW / 2, cardY + 114, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Echtes Minecraft-Chat-Layout (ohne Rahmen)"), cardX + cardW / 2, cardY + 128, EzUi.TEXT_DIM);
    }

    // ── 11. Nameplate Preview ──
    private void renderNameplatePreview(GuiGraphicsExtractor g, NameplateModule nameplate) {
        int cardW = 220, cardH = 95;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Nameplate – Live Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        StringBuilder tag = new StringBuilder("§6[MVP§c+§6] §fEzPlayer");
        if (nameplate.flag("health")) tag.append("  §c20/20 ❤");
        if (nameplate.flag("ping")) tag.append("  §a14ms");

        int tagW = font.width(Component.literal(tag.toString())) + 16;
        int tagX = cardX + (cardW - tagW) / 2, tagY = cardY + 34;
        EzUi.roundedRect(g, tagX, tagY, tagW, 16, 3, 0xCC000000);
        g.outline(tagX, tagY, tagW, 16, 0x40FFFFFF);
        g.centeredText(font, Component.literal(tag.toString()), tagX + tagW / 2, tagY + 4, 0xFFFFFFFF);

        String desc = "Skalierung: " + String.format(Locale.ROOT, "%.2fx", (float) nameplate.number("scale"));
        g.centeredText(font, Component.literal(desc), cardX + cardW / 2, cardY + 68, EzUi.TEXT_MUTED);
    }

    // ── 12. Time & Weather Preview ──
    private void renderTimeWeatherPreview(GuiGraphicsExtractor g, TimeWeatherModule tw) {
        int cardW = 264, cardH = 152;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Time & Weather – Himmel-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int skyX = cardX + 12, skyY = cardY + 23, skyW = cardW - 24, skyH = 74;
        EzUi.roundedRect(g, skyX, skyY, skyW, skyH, 4, 0xFF0B101D);
        g.enableScissor(skyX, skyY, skyX + skyW, skyY + skyH);

        double ticks = tw.visualTicks();
        int topColor, botColor;
        boolean isNight = (ticks > 13000 && ticks < 23000);
        boolean isSunset = (ticks >= 11500 && ticks <= 13000) || (ticks >= 23000 || ticks <= 1000);
        if (isNight) {
            topColor = 0xFF050813;
            botColor = 0xFF141E34;
        } else if (isSunset) {
            topColor = 0xFF351B42;
            botColor = 0xFFE65C00;
        } else {
            topColor = 0xFF3A88E9;
            botColor = 0xFF87CEEB;
        }

        for (int row = 0; row < skyH; row++) {
            float f = (float) row / skyH;
            int r = (int) (((topColor >> 16) & 0xFF) * (1 - f) + ((botColor >> 16) & 0xFF) * f);
            int gr = (int) (((topColor >> 8) & 0xFF) * (1 - f) + ((botColor >> 8) & 0xFF) * f);
            int b = (int) ((topColor & 0xFF) * (1 - f) + (botColor & 0xFF) * f);
            int col = 0xFF000000 | (r << 16) | (gr << 8) | b;
            g.fill(skyX, skyY + row, skyX + skyW, skyY + row + 1, col);
        }

        long time = System.currentTimeMillis();
        if (isNight) {
            int[][] stars = {{20, 15}, {60, 25}, {110, 10}, {150, 30}, {190, 18}, {215, 28}};
            for (int[] star : stars) {
                int alpha = ((star[0] + (int)(time / 300)) % 2 == 0) ? 0xFFFFFFFF : 0x77FFFFFF;
                g.fill(skyX + star[0], skyY + star[1], skyX + star[0] + 1, skyY + star[1] + 1, alpha);
            }
            int mx = skyX + 180, my = skyY + 20;
            EzUi.roundedRect(g, mx, my, 14, 14, 7, 0xFFF5F6FA);
            EzUi.roundedRect(g, mx + 4, my - 2, 12, 12, 6, topColor);
        } else {
            int sx = skyX + 180, sy = skyY + 18;
            EzUi.roundedRect(g, sx - 2, sy - 2, 18, 18, 9, 0x40FFA500);
            EzUi.roundedRect(g, sx, sy, 14, 14, 7, 0xFFFFD700);
        }

        String weather = tw.text("weather");
        if ("Rain".equalsIgnoreCase(weather) || "Thunder".equalsIgnoreCase(weather)) {
            for (int i = 0; i < 20; i++) {
                int rx = (int) ((skyX + (i * 14) + (time * 0.05)) % skyW);
                int ry = (int) ((skyY + ((i * 19) + (time * 0.2))) % skyH);
                g.fill(skyX + rx, skyY + ry, skyX + rx + 1, skyY + ry + 4, 0xAA74B9FF);
            }
        }

        // Mountain horizon silhouette
        g.fill(skyX, skyY + skyH - 12, skyX + skyW, skyY + skyH, 0xFF1C2833);
        g.fill(skyX, skyY + skyH - 6, skyX + skyW, skyY + skyH, 0xFF17202A);

        g.disableScissor();
        g.outline(skyX, skyY, skyW, skyH, EzUi.BORDER_SUBTLE);

        String row1 = "Zeit: " + tw.text("time") + " (" + (int) ticks + " Ticks)  ·  Wetter: " + weather;
        g.centeredText(font, Component.literal(row1), cardX + cardW / 2, cardY + 104, EzUi.TEXT_WHITE);
        String row2 = "Partikel: " + (tw.flag("precipitation") ? "An" : "Aus") + "  ·  Blitzschutz: " + (tw.flag("removeFlash") ? "Aktiv" : "Aus");
        g.centeredText(font, Component.literal(row2), cardX + cardW / 2, cardY + 118, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Client-seitige Zeit- und Wetterdarstellung"), cardX + cardW / 2, cardY + 132, EzUi.TEXT_DIM);
    }

    // ── 13. Particle Customizer Preview ──
    private void renderParticlePreview(GuiGraphicsExtractor g, ParticleCustomizerModule particle) {
        int cardW = 264, cardH = 152;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Particle Customizer – Treffer-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int simX = cardX + 12, simY = cardY + 23, simW = cardW - 24, simH = 74;
        EzUi.roundedRect(g, simX, simY, simW, simH, 4, 0xFF10141D);
        g.outline(simX, simY, simW, simH, EzUi.BORDER_SUBTLE);
        g.enableScissor(simX, simY, simX + simW, simY + simH);

        long time = System.currentTimeMillis();
        int targetX = simX + simW / 2, targetY = simY + simH / 2;

        EzUi.roundedRect(g, targetX - 8, targetY - 18, 16, 14, 2, 0xFF34495E);
        EzUi.roundedRect(g, targetX - 10, targetY - 2, 20, 24, 3, 0xFF2C3E50);

        double cycle = (time % 1500) / 1500.0;
        boolean justHit = cycle < 0.35;
        if (justHit) {
            EzUi.roundedRect(g, targetX - 10, targetY - 2, 20, 24, 3, 0x88FF3333);
        }

        int swordX = (int) (targetX - 26 + (justHit ? 8 : -4));
        int swordY = targetY - 14;
        g.item(new ItemStack(Items.DIAMOND_SWORD), swordX, swordY);

        if (justHit) {
            double pAge = cycle / 0.35;
            double mult = Math.max(0.5, particle.number("multiplier"));
            int count = (int) (8 * mult);
            int tintCol = particle.flag("tint") ? particle.tint("color", false) : 0xFFFFD700;

            for (int i = 0; i < count; i++) {
                double ang = (i * 2.0 * Math.PI / count) + (time * 0.002);
                double dist = pAge * 28.0;
                int px = (int) (targetX + Math.cos(ang) * dist);
                int py = (int) (targetY + Math.sin(ang) * dist);
                int alpha = (int) ((1.0 - pAge) * 255);
                int col = (alpha << 24) | (tintCol & 0x00FFFFFF);
                g.fill(px - 1, py - 1, px + 2, py + 2, col);
            }

            if (particle.flag("alwaysSharpness")) {
                for (int i = 0; i < 4; i++) {
                    int sx = (int) (targetX + Math.cos(i * Math.PI / 2) * (pAge * 20.0));
                    int sy = (int) (targetY + Math.sin(i * Math.PI / 2) * (pAge * 20.0));
                    g.fill(sx, sy, sx + 2, sy + 2, 0xFF00D2FF);
                }
            }
        }

        g.disableScissor();

        String row1 = "Multiplikator: " + String.format(Locale.ROOT, "%.1fx", particle.number("multiplier"))
                + "  ·  Immer Kritisch: " + (particle.flag("alwaysCrit") ? "An" : "Aus");
        g.centeredText(font, Component.literal(row1), cardX + cardW / 2, cardY + 104, EzUi.TEXT_WHITE);
        String row2 = "Farbe: " + (particle.flag("tint") ? "Benutzerdefiniert" : "Standard")
                + "  ·  Schärfe: " + (particle.flag("alwaysSharpness") ? "An" : "Aus");
        g.centeredText(font, Component.literal(row2), cardX + cardW / 2, cardY + 118, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Multipliziert und modifiziert Treffer-Partikel"), cardX + cardW / 2, cardY + 132, EzUi.TEXT_DIM);
    }

    // ── 14. Boss Bar Preview ──
    private void renderBossBarPreview(GuiGraphicsExtractor g, BossBarModule boss) {
        int cardW = 264, cardH = 145;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Boss Bar – Leisten-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        if (boss.flag("hide")) {
            EzUi.badge(g, cardX + 30, cardY + 45, cardW - 60, 22, 0x33FF4444, 0xFFFF5555, "Bossleisten vollständig ausgeblendet", 0xFFFFFFFF);
        } else {
            int bbW = boss.getWidth(minecraft, true);
            int bbX = cardX + (cardW - bbW) / 2;
            int bbY = cardY + 42;
            BossBarModule.renderBossBar(g, minecraft, boss, Component.literal("§d§lEnderdrache"), 0.75f, net.minecraft.world.BossEvent.BossBarColor.PURPLE, bbX, bbY, true);
        }

        String row1 = "Stil: " + boss.text("style") + "  ·  Anzeige: " + boss.text("health");
        g.centeredText(font, Component.literal(row1), cardX + cardW / 2, cardY + 86, EzUi.TEXT_WHITE);
        String row2 = "Farbe: " + (boss.flag("chroma") ? "Rainbow (Chroma)" : (boss.flag("override") ? "Benutzerdefiniert" : "Standard"));
        g.centeredText(font, Component.literal(row2), cardX + cardW / 2, cardY + 100, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Synchron mit Ingame-Bossleiste & HUD-Editor"), cardX + cardW / 2, cardY + 114, EzUi.TEXT_DIM);
    }

    // ── 15. Sound Enhancer Subtitles Preview ──
    private void renderSoundEnhancerPreview(GuiGraphicsExtractor g, SoundEnhancerModule sound) {
        int cardW = 264, cardH = 152;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Sound Subtitles – Richtungs-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int listX = cardX + 16, listY = cardY + 25, listW = cardW - 32;

        int hlColor = sound.tint("highlightColor", false);
        EzUi.roundedRect(g, listX, listY, listW, 18, 3, 0x2BFF5500);
        g.outline(listX, listY, listW, 18, (hlColor & 0x00FFFFFF) | 0x66000000);
        g.text(font, Component.literal("! ‹ TNT zischen"), listX + 8, listY + 5, hlColor);
        g.text(font, Component.literal("Vorne Links"), listX + listW - 60, listY + 5, hlColor);

        listY += 22;
        EzUi.roundedRect(g, listX, listY, listW, 18, 3, 0x1AFFFFFF);
        g.outline(listX, listY, listW, 18, EzUi.BORDER_SUBTLE);
        g.text(font, Component.literal("› Schritte (Gras)"), listX + 8, listY + 5, 0xFFFFFFFF);
        g.text(font, Component.literal(app.ezclient.util.EzI18n.text("Rechts")), listX + listW - 45, listY + 5, EzUi.TEXT_MUTED);

        listY += 22;
        EzUi.roundedRect(g, listX, listY, listW, 18, 3, 0x1AFFFFFF);
        g.outline(listX, listY, listW, 18, EzUi.BORDER_SUBTLE);
        g.text(font, Component.literal("‹ Truhe öffnen"), listX + 8, listY + 5, 0xFFFFFFFF);
        g.text(font, Component.literal(app.ezclient.util.EzI18n.text("Links")), listX + listW - 38, listY + 5, EzUi.TEXT_MUTED);

        String row1 = "Richtungspfeile: " + (sound.flag("arrows") ? "Aktiviert" : "Aus")
                + "  ·  Dauer: " + String.format(Locale.ROOT, "%.1fs", sound.number("duration"));
        g.centeredText(font, Component.literal(row1), cardX + cardW / 2, cardY + 104, EzUi.TEXT_WHITE);
        String row2 = "Lautstärke: Regen " + (int)(sound.number("rainVolume") * 100) + "%  ·  Schritte " + (int)(sound.number("stepVolume") * 100) + "%";
        g.centeredText(font, Component.literal(row2), cardX + cardW / 2, cardY + 118, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Erweiterte Untertitel mit dynamischen 3D-Richtungspfeilen"), cardX + cardW / 2, cardY + 132, EzUi.TEXT_DIM);
    }

    // ── 16. AutoGG Preview ──
    private void renderAutoGgPreview(GuiGraphicsExtractor g, AutoGgModule autoGg) {
        int cardW = 264, cardH = 148;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("AutoGG – Match-Ende Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        long time = System.currentTimeMillis();
        int period = Math.max(1200, autoGg.getDelayMs() + 1500);
        double cycle = (time % period) / (double) period;
        double delayFrac = autoGg.getDelayMs() / (double) period;

        EzUi.roundedRect(g, cardX + 30, cardY + 24, cardW - 60, 22, 4, 0xFF2A2000);
        g.outline(cardX + 30, cardY + 24, cardW - 60, 22, 0xFFFFD700);
        g.centeredText(font, Component.literal("★ VICTORY! ★"), cardX + cardW / 2, cardY + 30, 0xFFFFD700);

        if (cycle < delayFrac) {
            float progress = (float) (cycle / delayFrac);
            int barW = cardW - 60;
            EzUi.roundedRect(g, cardX + 30, cardY + 54, barW, 8, 3, 0xFF1C222D);
            EzUi.roundedRect(g, cardX + 30, cardY + 54, (int) (barW * progress), 8, 3, EzUi.ACCENT_EMERALD);
            g.centeredText(font, Component.literal("Verzögerung: " + autoGg.getDelayMs() + "ms …"), cardX + cardW / 2, cardY + 66, EzUi.TEXT_MUTED);
        } else {
            int chatW = cardW - 40;
            EzUi.roundedRect(g, cardX + 20, cardY + 52, chatW, 20, 3, 0xDD000000);
            g.outline(cardX + 20, cardY + 52, chatW, 20, 0x40FFFFFF);
            g.text(font, Component.literal("§6[All] §fEzPlayer§7: §a" + autoGg.getCustomMessage()), cardX + 26, cardY + 58, 0xFFFFFFFF);
        }

        String info = "Nachricht: \"" + autoGg.getCustomMessage() + "\"  ·  Verzögerung: " + autoGg.getDelayMs() + "ms";
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 98, EzUi.TEXT_WHITE);
        g.centeredText(font, Component.literal("Erkennt Spielende auf Hypixel, GommeHD & Minigames"), cardX + cardW / 2, cardY + 112, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Sendet automatisch die konfigurierte Nachricht"), cardX + cardW / 2, cardY + 126, EzUi.TEXT_DIM);
    }

    // ── 17. Motion Blur Preview ──
    private void renderMotionBlurPreview(GuiGraphicsExtractor g, MotionBlurModule mb) {
        int cardW = 264, cardH = 148;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Motion Blur – Unschärfe-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int boxX = cardX + 12, boxY = cardY + 23, boxW = cardW - 24, boxH = 68;
        EzUi.roundedRect(g, boxX, boxY, boxW, boxH, 4, 0xFF121620);
        g.outline(boxX, boxY, boxW, boxH, EzUi.BORDER_SUBTLE);

        long time = System.currentTimeMillis();
        double swing = Math.sin(time * 0.004);
        int centerX = boxX + boxW / 2;
        int currentX = (int) (centerX + swing * 55.0);
        int itemY = boxY + 25;

        int trails = Math.max(2, mb.getBlurStrength() / 15);
        for (int i = trails; i >= 1; i--) {
            double pastSwing = Math.sin((time - i * 35) * 0.004);
            int ghostX = (int) (centerX + pastSwing * 55.0);
            int alpha = (int) ((1.0 - (double) i / (trails + 1)) * (mb.getBlurStrength() * 1.8));
            g.fill(ghostX + 4, itemY + 4, ghostX + 12, itemY + 12, (Math.min(255, alpha) << 24) | 0x00D2FF);
        }

        g.item(new ItemStack(Items.DIAMOND_SWORD), currentX, itemY);

        String info = "Unschärfe: " + mb.getBlurStrength() + "%  ·  FPS-Schutz: " + (mb.isFpsProtection() ? "Aktiv" : "Aus");
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 102, EzUi.TEXT_WHITE);
        g.centeredText(font, Component.literal("Sanfte Nachzieheffekte bei schnellen Kopfdrehungen"), cardX + cardW / 2, cardY + 116, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Schützt Performance ab unter " + mb.getFpsThreshold() + " FPS"), cardX + cardW / 2, cardY + 128, EzUi.TEXT_DIM);
    }

    // ── 18. FOV Changer Preview ──
    private void renderFovPreview(GuiGraphicsExtractor g, FovChangerModule fov) {
        int cardW = 264, cardH = 148;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("FOV Changer – Sichtfeld-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int vX = cardX + 12, vY = cardY + 23, vW = cardW - 24, vH = 68;
        EzUi.roundedRect(g, vX, vY, vW, vH, 4, 0xFF0E131E);
        g.outline(vX, vY, vW, vH, EzUi.BORDER_SUBTLE);
        g.enableScissor(vX, vY, vX + vW, vY + vH);

        long time = System.currentTimeMillis();
        int state = (int) ((time / 2000) % 4);
        String stateName;
        float mult;
        if (fov.isStaticFovLock()) {
            stateName = "Gesperrt (Static Lock)";
            mult = 1.0f;
        } else {
            switch (state) {
                case 1 -> { stateName = "Sprinten"; mult = fov.getSprintMultiplier(); }
                case 2 -> { stateName = "Speed II Trank"; mult = fov.getSpeedPotionMultiplier(); }
                case 3 -> { stateName = "Bogen zielen"; mult = fov.getBowAimMultiplier(); }
                default -> { stateName = "Normal"; mult = 1.0f; }
            }
        }

        int cx = vX + vW / 2, cy = vY + vH / 2;
        int fovSpan = (int) (40 * mult);
        g.fill(cx - fovSpan, vY + 4, cx - fovSpan + 1, vY + vH - 4, 0xFF00D2FF);
        g.fill(cx + fovSpan, vY + 4, cx + fovSpan + 1, vY + vH - 4, 0xFF00D2FF);
        g.fill(cx - fovSpan, vY + 4, cx + fovSpan, vY + 5, 0x4400D2FF);
        g.fill(cx - fovSpan, vY + vH - 5, cx + fovSpan, vY + vH - 4, 0x4400D2FF);
        g.fill(cx - 3, cy, cx + 4, cy + 1, 0xFFFFFFFF);
        g.fill(cx, cy - 3, cx + 1, cy + 4, 0xFFFFFFFF);

        g.centeredText(font, Component.literal("Modus: " + stateName + " (" + String.format(Locale.ROOT, "%.2fx", mult) + ")"), cx, vY + vH - 14, 0xFFFFFFFF);

        g.disableScissor();

        String info = "Sprint: " + fov.getSprintMultiplier() + "x  ·  Speed: " + fov.getSpeedPotionMultiplier() + "x  ·  Bogen: " + fov.getBowAimMultiplier() + "x";
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 102, EzUi.TEXT_WHITE);
        g.centeredText(font, Component.literal("Statische Sperre: " + (fov.isStaticFovLock() ? "Aktiviert (Kein Verzug)" : "Deaktiviert")), cardX + cardW / 2, cardY + 116, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Gleicht übermäßige FOV-Sprünge sanft aus"), cardX + cardW / 2, cardY + 128, EzUi.TEXT_DIM);
    }

    // ── 19. Zoom Preview ──
    private void renderZoomPreview(GuiGraphicsExtractor g, ZoomModule zoom) {
        int cardW = 264, cardH = 148;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Zoom – Fernglas-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int zX = cardX + 12, zY = cardY + 23, zW = cardW - 24, zH = 68;
        EzUi.roundedRect(g, zX, zY, zW, zH, 4, 0xFF0D121B);
        g.outline(zX, zY, zW, zH, EzUi.BORDER_SUBTLE);
        g.enableScissor(zX, zY, zX + zW, zY + zH);

        int cx = zX + zW / 2, cy = zY + zH / 2;
        int radius = 28;
        EzUi.roundedRect(g, cx - radius, cy - radius, radius * 2, radius * 2, radius, 0xFF1A2333);
        g.outline(cx - radius, cy - radius, radius * 2, radius * 2, 0xFFD4AF37);

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        float factor = (float) Math.min(2.5f, zoom.getZoomLevel() * 0.4f);
        g.pose().scale(factor, factor);
        g.pose().translate(-8, -8);
        g.item(new ItemStack(Items.ENDER_PEARL), 0, 0);
        g.pose().popMatrix();

        g.fill(cx - radius + 4, cy, cx + radius - 4, cy + 1, 0x88FFFFFF);
        g.fill(cx, cy - radius + 4, cx + 1, cy + radius - 4, 0x88FFFFFF);

        g.disableScissor();

        String info = "Faktor: " + String.format(Locale.ROOT, "%.1fx", zoom.getZoomLevel())
                + "  ·  Sanfter Zoom: " + (zoom.isSmoothZoom() ? "An" : "Aus");
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 104, EzUi.TEXT_WHITE);
        g.centeredText(font, Component.literal("Maus-Skalierung: " + (zoom.isMouseSensitivityScaling() ? app.ezclient.util.EzI18n.text("Aktiv") : "Inaktiv")), cardX + cardW / 2, cardY + 118, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Erlaubt stufenlose Vergrößerung per Zoom-Taste"), cardX + cardW / 2, cardY + 130, EzUi.TEXT_DIM);
    }

    // ── 20. Waypoints Preview ──
    private void renderWaypointsPreview(GuiGraphicsExtractor g, WaypointsModule waypoints) {
        int cardW = 264, cardH = 148;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Waypoints – 3D Marker-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int scX = cardX + 12, scY = cardY + 23, scW = cardW - 24, scH = 68;
        EzUi.roundedRect(g, scX, scY, scW, scH, 4, 0xFF0E131C);
        g.outline(scX, scY, scW, scH, EzUi.BORDER_SUBTLE);
        g.enableScissor(scX, scY, scX + scW, scY + scH);

        int groundY = scY + scH - 14;
        g.fill(scX, groundY, scX + scW, groundY + 1, 0xFF2A374A);

        int beamX = scX + 65;
        g.fill(beamX - 2, scY + 4, beamX + 4, groundY, 0x3300D2FF);
        g.fill(beamX, scY + 4, beamX + 2, groundY, 0xCC00D2FF);

        String tag1 = "◆ Home Base [142m]";
        int t1W = font.width(tag1) + 12;
        EzUi.roundedRect(g, beamX - t1W / 2 + 1, scY + 12, t1W, 14, 3, 0xDD0D1117);
        g.outline(beamX - t1W / 2 + 1, scY + 12, t1W, 14, 0xFF00D2FF);
        g.centeredText(font, Component.literal(tag1), beamX + 1, scY + 15, 0xFFFFFFFF);

        int tag2X = scX + 175;
        String tag2 = "♦ Nether Portal [320m]";
        int t2W = font.width(tag2) + 12;
        EzUi.roundedRect(g, tag2X - t2W / 2, scY + 26, t2W, 14, 3, 0xDD0D1117);
        g.outline(tag2X - t2W / 2, scY + 26, t2W, 14, 0xFFA855F7);
        g.centeredText(font, Component.literal(tag2), tag2X, scY + 29, 0xFFFFFFFF);

        g.disableScissor();

        String info = "3D Leuchtfeuer-Strahl  ·  Entfernungsanzeige in Metern";
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 104, EzUi.TEXT_WHITE);
        g.centeredText(font, Component.literal("Setze unbegrenzt viele Wegpunkte in jeder Dimension"), cardX + cardW / 2, cardY + 118, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Einstellbare Farben, Sichtbarkeit und Beacon-Balken"), cardX + cardW / 2, cardY + 130, EzUi.TEXT_DIM);
    }

    // ── 21. Fullbright Preview ──
    private void renderFullbrightPreview(GuiGraphicsExtractor g, FullbrightModule fb) {
        int cardW = 264, cardH = 148;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Fullbright – Beleuchtungs-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int bX = cardX + 12, bY = cardY + 23, bW = cardW - 24, bH = 68;
        int halfW = bW / 2;

        EzUi.roundedRect(g, bX, bY, halfW, bH, 3, 0xFF08090C);
        g.centeredText(font, Component.literal("Vanilla (Dunkel)"), bX + halfW / 2, bY + 8, 0xFF777777);
        g.fill(bX + halfW / 2 - 8, bY + 28, bX + halfW / 2 + 8, bY + 44, 0xFF14161B);

        EzUi.roundedRect(g, bX + halfW, bY, halfW, bH, 3, 0xFF2C3E50);
        g.centeredText(font, Component.literal("Fullbright"), bX + halfW + halfW / 2, bY + 8, 0xFF2ED573);
        EzUi.roundedRect(g, bX + halfW + halfW / 2 - 8, bY + 28, 16, 16, 2, 0xFF4A6572);
        g.fill(bX + halfW + halfW / 2 - 3, bY + 33, bX + halfW + halfW / 2 + 3, bY + 39, 0xFF00D2FF);

        g.fill(bX + halfW, bY, bX + halfW + 1, bY + bH, 0xFFFFFFFF);
        g.outline(bX, bY, bW, bH, EzUi.BORDER_SUBTLE);

        String info = "Gamma Boost: 1000%  ·  Sanftes Einblenden: " + (fb.isSmoothFade() ? "An" : "Aus");
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 104, EzUi.TEXT_WHITE);
        String netherStr = "Nether: " + (fb.isDisableInNether() ? "Aus" : "Aktiv") + "  ·  End: " + (fb.isDisableInEnd() ? "Aus" : "Aktiv");
        g.centeredText(font, Component.literal(netherStr), cardX + cardW / 2, cardY + 118, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Permanente maximale Sichtbarkeit in Minen & Höhlen"), cardX + cardW / 2, cardY + 130, EzUi.TEXT_DIM);
    }

    // ── 22. Clear Glass Preview ──
    private void renderClearGlassPreview(GuiGraphicsExtractor g, ClearGlassModule glass) {
        int cardW = 264, cardH = 148;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Clear Glass – Glas-Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int bX = cardX + 12, bY = cardY + 23, bW = cardW - 24, bH = 68;
        int halfW = bW / 2;

        EzUi.roundedRect(g, bX, bY, halfW, bH, 3, 0xFF141A24);
        g.centeredText(font, Component.literal("Vanilla Glas"), bX + halfW / 2, bY + 8, 0xFF888888);
        int g1X = bX + 12, g1Y = bY + 24;
        g.outline(g1X, g1Y, 18, 18, 0x99FFFFFF);
        g.outline(g1X + 20, g1Y, 18, 18, 0x99FFFFFF);
        g.fill(g1X + 4, g1Y + 4, g1X + 10, g1Y + 6, 0x66FFFFFF);

        EzUi.roundedRect(g, bX + halfW, bY, halfW, bH, 3, 0xFF141A24);
        g.centeredText(font, Component.literal("Clear Glass"), bX + halfW + halfW / 2, bY + 8, 0xFF00D2FF);
        int g2X = bX + halfW + 14, g2Y = bY + 24;
        EzUi.roundedRect(g, g2X, g2Y, 40, 20, 2, 0x2200D2FF);
        g.outline(g2X, g2Y, 40, 20, 0xCC00D2FF);

        g.fill(bX + halfW, bY, bX + halfW + 1, bY + bH, 0xFFFFFFFF);
        g.outline(bX, bY, bW, bH, EzUi.BORDER_SUBTLE);

        String info = "Connected Textures: Aktiv  ·  Innere Streifen: Entfernt";
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 104, EzUi.TEXT_WHITE);
        g.centeredText(font, Component.literal("Verbindet benachbarte Glasblöcke zu klaren Fenstern"), cardX + cardW / 2, cardY + 118, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Bietet optimale Durchsicht ohne störende Gitter"), cardX + cardW / 2, cardY + 130, EzUi.TEXT_DIM);
    }

    // ── 23. Toggle Sprint & Sneak Preview ──
    private void renderToggleSprintPreview(GuiGraphicsExtractor g, ToggleSprintSneakModule sprint) {
        int cardW = 264, cardH = 148;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        g.centeredText(font, Component.literal("Toggle Sprint & Sneak – Vorschau"), cardX + cardW / 2, cardY + 8, EzUi.TEXT_WHITE);

        int bX = cardX + 16, bY = cardY + 26, bW = cardW - 32;

        EzUi.roundedRect(g, bX, bY, bW, 20, 3, 0xDD000000);
        g.outline(bX, bY, bW, 20, 0x40FFFFFF);
        g.text(font, Component.literal("§a[Sprinting (Toggled)]"), bX + 8, bY + 6, 0xFF2ED573);
        g.text(font, Component.literal("§6[Sneaking]"), bX + bW - 68, bY + 6, 0xFFFFA502);

        String s1 = "Sprint: " + (sprint.getSprintMode() == ToggleSprintSneakModule.SprintMode.TOGGLE ? "Toggle" : "Halten");
        String s2 = "Sneak: " + (sprint.getSneakMode() == ToggleSprintSneakModule.SneakMode.TOGGLE ? "Toggle" : "Halten");
        g.centeredText(font, Component.literal(s1 + "  ·  " + s2), cardX + cardW / 2, cardY + 60, EzUi.TEXT_WHITE);

        String info = "Flugbeschleunigung: " + String.format(Locale.ROOT, "%.1fx", sprint.getFlyBoostMultiplier());
        g.centeredText(font, Component.literal(info), cardX + cardW / 2, cardY + 102, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Kein dauerhaftes Halten der Sprinten-/Sneaken-Tasten nötig"), cardX + cardW / 2, cardY + 116, EzUi.TEXT_MUTED);
        g.centeredText(font, Component.literal("Inklusive konfigurierbarer HUD-Statusanzeige"), cardX + cardW / 2, cardY + 128, EzUi.TEXT_DIM);
    }

    // ── 24. General Fallback Card ──
    private void renderGeneralPreview(GuiGraphicsExtractor g, Module mod) {
        int cardW = 240, cardH = 110;
        int cardX = (width - cardW) / 2, cardY = (height - cardH) / 2;
        EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 6, 0xEE0D1117);
        g.outline(cardX, cardY, cardW, cardH, EzUi.BORDER_SUBTLE);

        ModuleIconRenderer.draw(g, mod, cardX + 14, cardY + 12, 20);
        g.text(font, Component.literal(mod.getDisplayName()), cardX + 40, cardY + 14, EzUi.TEXT_WHITE);
        g.text(font, Component.literal("Kategorie: " + mod.getCategory()), cardX + 40, cardY + 25, EzUi.TEXT_DIM);

        int descY = cardY + 44;
        String desc = mod.getLocalizedDescription();
        if (font.width(desc) > cardW - 20) {
            String[] words = desc.split(" ");
            StringBuilder l1 = new StringBuilder(), l2 = new StringBuilder();
            for (String w : words) {
                if (l2.isEmpty() && font.width(l1 + " " + w) < cardW - 24) {
                    if (!l1.isEmpty()) l1.append(' ');
                    l1.append(w);
                } else {
                    if (!l2.isEmpty()) l2.append(' ');
                    l2.append(w);
                }
            }
            g.text(font, Component.literal(l1.toString()), cardX + 12, descY, EzUi.TEXT_MUTED);
            g.text(font, Component.literal(l2.toString()), cardX + 12, descY + 11, EzUi.TEXT_MUTED);
        } else {
            g.text(font, Component.literal(desc), cardX + 12, descY, EzUi.TEXT_MUTED);
        }

        boolean active = mod.isEnabled();
        int bColor = active ? EzUi.ACCENT_EMERALD : 0xFF4A5568;
        int bText = active ? 0xFFFFFFFF : 0xFF94A3B8;
        EzUi.badge(g, cardX + 12, cardY + 76, cardW - 24, 18, active ? 0xFF142419 : 0xFF181C22, bColor,
                active ? "Status: Aktiviert" : "Status: Deaktiviert", bText);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        returnToSettings();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            returnToSettings();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public void onClose() { returnToSettings(); }
    @Override public boolean isPauseScreen() { return false; }
}
