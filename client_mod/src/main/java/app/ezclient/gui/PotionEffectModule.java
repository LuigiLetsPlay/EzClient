package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Potion Effects & Status Buff HUD with Compact vs Detailed views,
 * blink warning alerts for expiring effects, and sorting options.
 */
public final class PotionEffectModule extends HudModule {
    public enum DisplayStyle {
        COMPACT,  // Icons + timer
        DETAILED  // Icon + Name + Amplifier + Timer
    }

    public enum SortOrder {
        DURATION_DESC,
        DURATION_ASC,
        BUFF_FIRST
    }

    private DisplayStyle displayStyle = DisplayStyle.DETAILED;
    private SortOrder sortOrder = SortOrder.DURATION_DESC;
    private boolean vertical = true;
    private boolean showTime = true;
    private int blinkWarningSeconds = 5;
    private boolean useCustomColors = true;
    private long cachedEffectTick = Long.MIN_VALUE;
    private List<MobEffectInstance> cachedEffects = List.of();
    private List<MobEffectInstance> editorEffects;

    public PotionEffectModule() {
        super("Potion Effects", "HUD", false, 10, 100, "", "");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/potion_effect.png");
    }

    public DisplayStyle getDisplayStyle() { return displayStyle; }
    public void setDisplayStyle(DisplayStyle displayStyle) { this.displayStyle = displayStyle; ConfigManager.save(); }

    public SortOrder getSortOrder() { return sortOrder; }
    public void setSortOrder(SortOrder sortOrder) { this.sortOrder = sortOrder; ConfigManager.save(); }

    public boolean isVertical() { return vertical; }
    public void setVertical(boolean vertical) { this.vertical = vertical; ConfigManager.save(); }

    public boolean isShowTime() { return showTime; }
    public void setShowTime(boolean showTime) { this.showTime = showTime; ConfigManager.save(); }

    public int getBlinkWarningSeconds() { return blinkWarningSeconds; }
    public void setBlinkWarningSeconds(int val) { this.blinkWarningSeconds = Math.max(0, Math.min(15, val)); ConfigManager.save(); }

    public boolean isUseCustomColors() { return useCustomColors; }
    public void setUseCustomColors(boolean useCustomColors) { this.useCustomColors = useCustomColors; ConfigManager.save(); }

    private List<MobEffectInstance> getSortedEffects(Minecraft client, boolean editor) {
        if (editor) {
            if (editorEffects == null) {
                editorEffects = List.of(
                        new MobEffectInstance(MobEffects.SPEED, 1200, 1, false, false, false),
                        new MobEffectInstance(MobEffects.STRENGTH, 2400, 0, false, false, false),
                        new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0, false, false, false));
            }
            return editorEffects;
        }

        long tick = client.level == null ? Long.MIN_VALUE : client.level.getOverworldClockTime();
        if (tick == cachedEffectTick) return cachedEffects;

        List<MobEffectInstance> list = new ArrayList<>();
        if (client.player != null) {
            list.addAll(client.player.getActiveEffects());
        }

        if (sortOrder == SortOrder.DURATION_DESC) {
            list.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
        } else if (sortOrder == SortOrder.DURATION_ASC) {
            list.sort(Comparator.comparingInt(MobEffectInstance::getDuration));
        } else if (sortOrder == SortOrder.BUFF_FIRST) {
            list.sort((a, b) -> Boolean.compare(!a.getEffect().value().isBeneficial(), !b.getEffect().value().isBeneficial()));
        }
        cachedEffectTick = tick;
        cachedEffects = list;
        return cachedEffects;
    }

    @Override
    public int getWidth(Minecraft client) {
        return widthForCount(3);
    }

    @Override
    public int getHeight(Minecraft client) {
        return heightForCount(3);
    }

    private int widthForCount(int count) {
        if (count <= 0) return 60;
        int maxItemW = displayStyle == DisplayStyle.COMPACT ? 42 : 94;
        return vertical ? maxItemW : count * (maxItemW + 4);
    }

    private int heightForCount(int count) {
        if (count <= 0) return 24;
        int itemH = displayStyle == DisplayStyle.COMPACT ? 16 : 20;
        return vertical ? count * (itemH + 2) : itemH;
    }

    @Override
    protected String value(Minecraft client) {
        return "Potions";
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 0 -> "I";
            case 1 -> "II";
            case 2 -> "III";
            case 3 -> "IV";
            case 4 -> "V";
            default -> String.valueOf(num + 1);
        };
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        List<MobEffectInstance> effects = getSortedEffects(client, editor);
        if (effects.isEmpty() && !editor) return;

        float scale = (float) getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(scale, scale);

        // Reuse the already collected effects. The old code constructed three
        // dummy effect instances again every frame just to calculate this box.
        int totalW = widthForCount(effects.size());
        int totalH = heightForCount(effects.size());

        renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);

        int curX = 3;
        int curY = 3;
        long now = System.currentTimeMillis();

        for (MobEffectInstance effect : effects) {
            int duration = effect.getDuration();
            int secs = duration / 20;
            boolean blinking = blinkWarningSeconds > 0 && secs <= blinkWarningSeconds;
            if (blinking && ((now / 250L) % 2 == 0)) {
                // Blink tick: skip drawing text or draw dim
            }

            String name = net.minecraft.network.chat.Component.translatable(effect.getDescriptionId()).getString();
            String amp = toRoman(effect.getAmplifier());
            int remainingSeconds = secs % 60;
            String timeStr = (secs / 60) + (remainingSeconds < 10 ? ":0" : ":") + remainingSeconds;

            int nameColor = color();
            if (getColorMode() == ColorMode.SOLID && useCustomColors) {
                nameColor = effect.getEffect().value().getColor();
                if ((nameColor & 0xFF000000) == 0) nameColor |= 0xFF000000;
            }

            int timerColor = blinking ? 0xFFFF4444 : ((getColorMode() == ColorMode.RAINBOW || getColorMode() == ColorMode.WAVE) ? color(80L) : 0xFFAAAAAA);

            if (displayStyle == DisplayStyle.COMPACT) {
                String label = name.substring(0, Math.min(3, name.length())).toUpperCase() + " " + amp;
                graphics.text(client.font, label, curX, curY + 1, nameColor);
                if (showTime) {
                    graphics.text(client.font, timeStr, curX + 26, curY + 1, timerColor);
                }
                if (vertical) curY += 14;
                else curX += 44;
            } else {
                graphics.text(client.font, name + " " + amp, curX, curY + 1, nameColor);
                if (showTime) {
                    graphics.text(client.font, timeStr, curX, curY + 10, timerColor);
                }
                if (vertical) curY += 20;
                else curX += client.font.width(name + " " + amp) + 24;
            }
        }

        graphics.pose().popMatrix();
    }
}
