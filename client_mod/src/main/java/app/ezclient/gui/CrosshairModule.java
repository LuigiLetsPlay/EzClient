package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;

/**
 * Highly customizable Crosshair module supporting 5 distinct shapes
 * (Classic Cross, Dot, Circle, T-Shape, Chevron), dynamic spread,
 * target entity highlighting, outline styling, and auto-hide rules.
 */
public class CrosshairModule extends HudModule {
    public enum CrosshairType {
        CLASSIC_CROSS,
        CUSTOM_CROSS,
        DOT,
        CIRCLE,
        T_SHAPE,
        CHEVRON
    }

    private CrosshairType crosshairType = CrosshairType.CLASSIC_CROSS;
    private int gap = 3;
    private int size = 5;
    private int thickness = 1;
    private int verticalSize = 5;
    private int dotSize = 2;
    private int opacity = 100;
    private boolean showDot = false;
    private boolean showOutline = true;
    private int outlineColor = 0xFF000000;
    private boolean dynamicSpread = false;
    public enum TargetMode {
        OFF,
        ENTITIES,
        PLAYERS,
        HOSTILE,
        NEUTRAL,
        BLOCKS,
        ALL
    }

    private TargetMode targetMode = TargetMode.ALL;
    private int targetEntityColor = 0xFFFF3333;
    private int targetPlayerColor = 0xFF38BDF8;
    private int targetHostileColor = 0xFFFF3333;
    private int targetNeutralColor = 0xFFFFB020;
    private int targetBlockColor = 0xFFFACC15;
    private float targetEntityScale = 1.05f;
    private float targetPlayerScale = 1.15f;
    private float targetHostileScale = 1.20f;
    private float targetNeutralScale = 1.10f;
    private boolean movementSpread = true;
    private boolean jumpSpread = true;
    private boolean cooldownSpread = true;
    private boolean hideOnBowZoom = true;
    private boolean hideInF3 = true;
    private boolean hideInThirdPerson = true;

    public CrosshairModule() {
        super("Custom Crosshair", "RENDER", false, 0, 0, "", "");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/crosshair.png");
    }

    public CrosshairType getCrosshairType() { return crosshairType; }
    public void setCrosshairType(CrosshairType crosshairType) { this.crosshairType = crosshairType; ConfigManager.save(); }

    public int getGap() { return gap; }
    public void setGap(int gap) { this.gap = Math.max(0, Math.min(15, gap)); ConfigManager.save(); }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = Math.max(2, Math.min(20, size)); ConfigManager.save(); }

    public int getThickness() { return thickness; }
    public void setThickness(int thickness) { this.thickness = Math.max(1, Math.min(4, thickness)); ConfigManager.save(); }

    public int getVerticalSize() { return verticalSize; }
    public void setVerticalSize(int verticalSize) { this.verticalSize = Math.max(2, Math.min(20, verticalSize)); ConfigManager.save(); }

    public int getDotSize() { return dotSize; }
    public void setDotSize(int dotSize) { this.dotSize = Math.max(1, Math.min(6, dotSize)); ConfigManager.save(); }

    public int getOpacity() { return opacity; }
    public void setOpacity(int opacity) { this.opacity = Math.max(10, Math.min(100, opacity)); ConfigManager.save(); }

    public boolean isShowDot() { return showDot; }
    public void setShowDot(boolean showDot) { this.showDot = showDot; ConfigManager.save(); }

    public boolean isShowOutline() { return showOutline; }
    public void setShowOutline(boolean showOutline) { this.showOutline = showOutline; ConfigManager.save(); }

    public int getOutlineColor() { return outlineColor; }
    public void setOutlineColor(int outlineColor) { this.outlineColor = outlineColor; ConfigManager.save(); }

    public boolean isDynamicSpread() { return dynamicSpread; }
    public void setDynamicSpread(boolean dynamicSpread) { this.dynamicSpread = dynamicSpread; ConfigManager.save(); }

    public TargetMode getTargetMode() { return targetMode; }
    public void setTargetMode(TargetMode targetMode) { this.targetMode = targetMode == null ? TargetMode.OFF : targetMode; ConfigManager.save(); }

    public boolean isTargetHighlight() { return targetMode != TargetMode.OFF; }
    public void setTargetHighlight(boolean targetHighlight) { this.targetMode = targetHighlight ? TargetMode.ALL : TargetMode.OFF; ConfigManager.save(); }

    public int getTargetEntityColor() { return targetEntityColor; }
    public void setTargetEntityColor(int targetEntityColor) { this.targetEntityColor = targetEntityColor; ConfigManager.save(); }

    public int getTargetPlayerColor() { return targetPlayerColor; }
    public void setTargetPlayerColor(int targetPlayerColor) { this.targetPlayerColor = targetPlayerColor; ConfigManager.save(); }

    public int getTargetHostileColor() { return targetHostileColor; }
    public void setTargetHostileColor(int color) { this.targetHostileColor = color; ConfigManager.save(); }

    public int getTargetNeutralColor() { return targetNeutralColor; }
    public void setTargetNeutralColor(int color) { this.targetNeutralColor = color; ConfigManager.save(); }

    public int getTargetBlockColor() { return targetBlockColor; }
    public void setTargetBlockColor(int targetBlockColor) { this.targetBlockColor = targetBlockColor; ConfigManager.save(); }

    public float getTargetEntityScale() { return targetEntityScale; }
    public void setTargetEntityScale(float scale) { this.targetEntityScale = clampScale(scale); ConfigManager.save(); }
    public float getTargetPlayerScale() { return targetPlayerScale; }
    public void setTargetPlayerScale(float scale) { this.targetPlayerScale = clampScale(scale); ConfigManager.save(); }
    public float getTargetHostileScale() { return targetHostileScale; }
    public void setTargetHostileScale(float scale) { this.targetHostileScale = clampScale(scale); ConfigManager.save(); }
    public float getTargetNeutralScale() { return targetNeutralScale; }
    public void setTargetNeutralScale(float scale) { this.targetNeutralScale = clampScale(scale); ConfigManager.save(); }

    public boolean isMovementSpread() { return movementSpread; }
    public void setMovementSpread(boolean enabled) { movementSpread = enabled; ConfigManager.save(); }
    public boolean isJumpSpread() { return jumpSpread; }
    public void setJumpSpread(boolean enabled) { jumpSpread = enabled; ConfigManager.save(); }
    public boolean isCooldownSpread() { return cooldownSpread; }
    public void setCooldownSpread(boolean enabled) { cooldownSpread = enabled; ConfigManager.save(); }

    private static float clampScale(float scale) {
        return Math.max(0.5f, Math.min(2.0f, scale));
    }

    /** Color edited by the shared HUD color picker for the selected target rule. */
    public int getSelectedRuleColor() {
        return switch (targetMode) {
            case PLAYERS -> targetPlayerColor;
            case HOSTILE -> targetHostileColor;
            case NEUTRAL -> targetNeutralColor;
            case ENTITIES -> targetEntityColor;
            case BLOCKS -> targetBlockColor;
            default -> getTextColor();
        };
    }

    public void setSelectedRuleColor(int color) {
        switch (targetMode) {
            case PLAYERS -> setTargetPlayerColor(color);
            case HOSTILE -> setTargetHostileColor(color);
            case NEUTRAL -> setTargetNeutralColor(color);
            case ENTITIES -> setTargetEntityColor(color);
            case BLOCKS -> setTargetBlockColor(color);
            default -> setTextColor(color);
        }
    }

    public float getSelectedRuleScale() {
        return switch (targetMode) {
            case PLAYERS -> targetPlayerScale;
            case HOSTILE -> targetHostileScale;
            case NEUTRAL -> targetNeutralScale;
            default -> targetEntityScale;
        };
    }

    public void setSelectedRuleScale(float scale) {
        switch (targetMode) {
            case PLAYERS -> setTargetPlayerScale(scale);
            case HOSTILE -> setTargetHostileScale(scale);
            case NEUTRAL -> setTargetNeutralScale(scale);
            default -> setTargetEntityScale(scale);
        }
    }

    public boolean isHideOnBowZoom() { return hideOnBowZoom; }
    public void setHideOnBowZoom(boolean hideOnBowZoom) { this.hideOnBowZoom = hideOnBowZoom; ConfigManager.save(); }

    public boolean isHideInF3() { return hideInF3; }
    public void setHideInF3(boolean hideInF3) { this.hideInF3 = hideInF3; ConfigManager.save(); }

    public boolean isHideInThirdPerson() { return hideInThirdPerson; }
    public void setHideInThirdPerson(boolean hideInThirdPerson) { this.hideInThirdPerson = hideInThirdPerson; ConfigManager.save(); }

    // Legacy getters for backward compatibility
    public boolean isCustomColor() { return isTargetHighlight(); }
    public void setCustomColor(boolean val) { setTargetHighlight(val); }
    public int getDefaultColor() { return getTextColor(); }
    public void setDefaultColor(int val) { setTextColor(val); }
    public int getBlockColor() { return targetBlockColor; }
    public void setBlockColor(int val) { this.targetBlockColor = val; }
    public int getEntityColor() { return targetEntityColor; }
    public void setEntityColor(int val) { this.targetEntityColor = val; }
    public boolean isDynamicForm() { return crosshairType == CrosshairType.CLASSIC_CROSS; }
    public void setDynamicForm(boolean val) { this.crosshairType = val ? CrosshairType.CLASSIC_CROSS : CrosshairType.DOT; }

    @Override
    public int getWidth(Minecraft client) {
        return 40;
    }

    @Override
    public int getHeight(Minecraft client) {
        return 40;
    }

    @Override
    protected String value(Minecraft client) {
        return "";
    }

    public int getEffectiveColor(Minecraft client, boolean editor) {
        int selected = color();
        if (!editor && isTargetActive(client)) {
            Entity entity = client.crosshairPickEntity;
            if (entity instanceof Player) selected = targetPlayerColor;
            else if (entity instanceof Enemy) selected = targetHostileColor;
            else if (entity instanceof NeutralMob) selected = targetNeutralColor;
            else if (entity != null) selected = targetEntityColor;
            else if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) selected = targetBlockColor;
        }
        return withOpacity(selected);
    }

    public float getEffectiveScale(Minecraft client, boolean editor) {
        if (editor || !isTargetActive(client)) return 1.0f;
        Entity entity = client.crosshairPickEntity;
        if (entity instanceof Player) return targetPlayerScale;
        if (entity instanceof Enemy) return targetHostileScale;
        if (entity instanceof NeutralMob) return targetNeutralScale;
        return entity != null ? targetEntityScale : 1.0f;
    }

    private boolean isTargetActive(Minecraft client) {
        if (targetMode == TargetMode.OFF || client == null || client.hitResult == null) return false;
        if (client.hitResult.getType() == HitResult.Type.BLOCK) {
            return targetMode == TargetMode.BLOCKS || targetMode == TargetMode.ALL;
        }
        if (client.hitResult.getType() != HitResult.Type.ENTITY) return false;
        Entity entity = client.crosshairPickEntity;
        return switch (targetMode) {
            case PLAYERS -> entity instanceof Player;
            case HOSTILE -> entity instanceof Enemy;
            case NEUTRAL -> entity instanceof NeutralMob;
            case ENTITIES, ALL -> entity != null;
            default -> false;
        };
    }

    private int withOpacity(int color) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        int alpha = sourceAlpha * opacity / 100;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public int getEffectiveGap(Minecraft client, boolean editor) {
        int g = gap;
        if (!editor && dynamicSpread && client.player != null) {
            if (movementSpread && client.player.getDeltaMovement().horizontalDistanceSqr() > 0.0025) g += 2;
            if (jumpSpread && !client.player.onGround()) g += 3;
            if (cooldownSpread && client.player.getAttackStrengthScale(0.0f) < 0.98f) g += 2;
        }
        return g;
    }

    public void renderCrosshair(GuiGraphicsExtractor graphics, Minecraft client, int cx, int cy, boolean editor) {
        int col = getEffectiveColor(client, editor);
        int g = getEffectiveGap(client, editor);
        int s = size;
        int t = thickness;
        int halfT = t / 2;

        float targetScale = getEffectiveScale(client, editor);
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().scale(targetScale, targetScale);
        graphics.pose().translate(-cx, -cy);

        if (showDot || crosshairType == CrosshairType.DOT) {
            int ds = dotSize;
            int dsHalf = ds / 2;
            if (showOutline) {
                graphics.fill(cx - dsHalf - 1, cy - dsHalf - 1, cx + ds - dsHalf + 1, cy + ds - dsHalf + 1, withOpacity(outlineColor));
            }
            graphics.fill(cx - dsHalf, cy - dsHalf, cx + ds - dsHalf, cy + ds - dsHalf, col);
        }

        switch (crosshairType) {
            case CLASSIC_CROSS -> {
                // Top
                drawRect(graphics, cx - halfT, cy - g - s, t, s, col);
                // Bottom
                drawRect(graphics, cx - halfT, cy + g + 1, t, s, col);
                // Left
                drawRect(graphics, cx - g - s, cy - halfT, s, t, col);
                // Right
                drawRect(graphics, cx + g + 1, cy - halfT, s, t, col);
            }
            case CUSTOM_CROSS -> {
                drawRect(graphics, cx - halfT, cy - g - verticalSize, t, verticalSize, col);
                drawRect(graphics, cx - halfT, cy + g + 1, t, verticalSize, col);
                drawRect(graphics, cx - g - s, cy - halfT, s, t, col);
                drawRect(graphics, cx + g + 1, cy - halfT, s, t, col);
            }
            case T_SHAPE -> {
                // Bottom
                drawRect(graphics, cx - halfT, cy + g + 1, t, s, col);
                // Left
                drawRect(graphics, cx - g - s, cy - halfT, s, t, col);
                // Right
                drawRect(graphics, cx + g + 1, cy - halfT, s, t, col);
            }
            case CHEVRON -> {
                // Diagonal V-shaped tick marks
                for (int i = 0; i < s; i++) {
                    drawRect(graphics, cx - g - i, cy + g + i, t, t, col);
                    drawRect(graphics, cx + g + i, cy + g + i, t, t, col);
                }
            }
            case CIRCLE -> {
                int r = g + s / 2;
                for (int angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    int px = cx + (int) Math.round(Math.cos(rad) * r);
                    int py = cy + (int) Math.round(Math.sin(rad) * r);
                    drawRect(graphics, px - halfT, py - halfT, t, t, col);
                }
            }
            case DOT -> {
                // Dot already drawn above
            }
        }
        graphics.pose().popMatrix();
    }

    private void drawRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int col) {
        if (showOutline) {
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, withOpacity(outlineColor));
        }
        g.fill(x, y, x + w, y + h, col);
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        if (!editor) return; // In-game is rendered via GuiMixin
        int cx = getX() + 20;
        int cy = getY() + 20;
        renderCrosshair(graphics, client, cx, cy, true);
    }
}
