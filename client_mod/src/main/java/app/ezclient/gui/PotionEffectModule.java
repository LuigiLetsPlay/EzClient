package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
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
        ORIGINAL, // Original Minecraft effect icons
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

    private static final int TILE_SIZE = 30;
    private static final int ITEM_GAP = 2;
    private static final int OUTER_PADDING = 5;
    private static final int ORIGINAL_TEXT_INSET = 4;
    private record EffectLayout(int width, int height, List<Integer> itemWidths, int itemHeight) {}

    public PotionEffectModule() {
        super("Potion Effects", "HUD", false, -1, 6, "", "");
    }

    @Override
    public String getDescription() {
        return "Zeigt aktive Trankeffekte mit Dauer, Stärke und Ablaufwarnung im HUD an.";
    }

    @Override
    public int getX() {
        if (super.getX() == -1) {
            Minecraft client = Minecraft.getInstance();
            if (client.getWindow() != null) {
                int screenW = client.getWindow().getGuiScaledWidth();
                return Math.max(10, screenW - getWidth(client) - 6);
            }
            return 200;
        }
        return super.getX();
    }

    @Override
    protected void onToggle() {
        if (isEnabled() && super.getX() == -1) {
            Minecraft client = Minecraft.getInstance();
            if (client.getWindow() != null) {
                int screenW = client.getWindow().getGuiScaledWidth();
                setX(Math.max(10, screenW - getWidth(client) - 6));
                setY(6);
            }
        }
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

        if (client == null) return cachedEffects;

        long tick = client.level == null ? Long.MIN_VALUE : client.level.getOverworldClockTime();
        if (tick == cachedEffectTick) return cachedEffects;

        List<MobEffectInstance> list = new ArrayList<>();
        if (client.player != null) {
            FullbrightModule fb = ModuleManager.getInstance().getFullbrightModule();
            boolean fbActive = fb != null && fb.isEnabled();

            for (MobEffectInstance inst : client.player.getActiveEffects()) {
                if (inst.getEffect().is(MobEffects.NIGHT_VISION)) {
                    if (fbActive || inst.getDuration() > 20000) {
                        continue;
                    }
                }
                list.add(inst);
            }
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
        return layoutFor(client, getSortedEffects(client, false)).width();
    }

    @Override
    public int getWidth(Minecraft client, boolean editor) {
        return layoutFor(client, getSortedEffects(client, editor)).width();
    }

    @Override
    public int getHeight(Minecraft client) {
        return layoutFor(client, getSortedEffects(client, false)).height();
    }

    @Override
    public int getHeight(Minecraft client, boolean editor) {
        return layoutFor(client, getSortedEffects(client, editor)).height();
    }

    private EffectLayout layoutFor(Minecraft client, List<MobEffectInstance> effects) {
        if (effects.isEmpty()) return new EffectLayout(TILE_SIZE, TILE_SIZE, List.of(TILE_SIZE), TILE_SIZE);
        if (displayStyle == DisplayStyle.ORIGINAL) {
            List<Integer> tileWidths = new ArrayList<>(effects.size());
            int maxTileWidth = TILE_SIZE;
            int combinedTileWidth = 0;
            for (MobEffectInstance effect : effects) {
                int tileWidth = Math.max(TILE_SIZE, showTime
                        ? textWidth(client, timeText(effect)) + ORIGINAL_TEXT_INSET * 2
                        : TILE_SIZE);
                tileWidths.add(tileWidth);
                maxTileWidth = Math.max(maxTileWidth, tileWidth);
                combinedTileWidth += tileWidth;
            }
            int width = vertical ? maxTileWidth : combinedTileWidth + (effects.size() - 1) * ITEM_GAP;
            int height = vertical ? effects.size() * TILE_SIZE + (effects.size() - 1) * ITEM_GAP : TILE_SIZE;
            return new EffectLayout(width, height, List.copyOf(tileWidths), TILE_SIZE);
        }

        int itemHeight = displayStyle == DisplayStyle.COMPACT ? 14 : 20;
        List<Integer> itemWidths = new ArrayList<>(effects.size());
        int maxWidth = 1;
        int combinedWidth = 0;
        for (MobEffectInstance effect : effects) {
            String name = effectName(effect);
            String amplifier = toRoman(effect.getAmplifier());
            String time = timeText(effect);
            int itemWidth;
            if (displayStyle == DisplayStyle.COMPACT) {
                String label = compactLabel(name, amplifier);
                itemWidth = textWidth(client, label) + (showTime ? 4 + textWidth(client, time) : 0);
            } else {
                itemWidth = Math.max(textWidth(client, name + " " + amplifier), showTime ? textWidth(client, time) : 0);
            }
            itemWidth = Math.max(1, itemWidth);
            itemWidths.add(itemWidth);
            maxWidth = Math.max(maxWidth, itemWidth);
            combinedWidth += itemWidth;
        }

        int width = vertical
                ? maxWidth + OUTER_PADDING * 2
                : combinedWidth + OUTER_PADDING * 2 + (effects.size() - 1) * 4;
        int height = vertical
                ? effects.size() * itemHeight + (effects.size() - 1) * ITEM_GAP + OUTER_PADDING * 2
                : itemHeight + OUTER_PADDING * 2;
        return new EffectLayout(width, height, List.copyOf(itemWidths), itemHeight);
    }

    private static int textWidth(Minecraft client, String text) {
        return client == null || client.font == null ? text.length() * 6 : client.font.width(text);
    }

    private static String effectName(MobEffectInstance effect) {
        return net.minecraft.network.chat.Component.translatable(effect.getDescriptionId()).getString();
    }

    private static String compactLabel(String name, String amplifier) {
        return name.substring(0, Math.min(3, name.length())).toUpperCase() + " " + amplifier;
    }

    private static String timeText(MobEffectInstance effect) {
        int seconds = effect.getDuration() / 20;
        int remainingSeconds = seconds % 60;
        return (seconds / 60) + (remainingSeconds < 10 ? ":0" : ":") + remainingSeconds;
    }

    private int effectBorderColor(MobEffectInstance effect, boolean blinking) {
        if (blinking) return 0xFFFF4B4B;
        if (isRainbowBorder() || !useCustomColors || getColorMode() != ColorMode.SOLID) return currentBorderColor();
        int rgb = effect.getEffect().value().getColor() & 0x00FFFFFF;
        return (currentBorderColor() & 0xFF000000) | rgb;
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
        EffectLayout layout = layoutFor(client, effects);
        int totalW = layout.width();
        int totalH = layout.height();

        if (displayStyle != DisplayStyle.ORIGINAL) {
            renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);
        }

        int curX = displayStyle == DisplayStyle.ORIGINAL ? 0 : OUTER_PADDING;
        int curY = displayStyle == DisplayStyle.ORIGINAL ? 0 : OUTER_PADDING;
        long now = System.currentTimeMillis();

        for (int index = 0; index < effects.size(); index++) {
            MobEffectInstance effect = effects.get(index);
            int duration = effect.getDuration();
            int secs = duration / 20;
            boolean blinking = blinkWarningSeconds > 0 && secs <= blinkWarningSeconds;
            if (blinking && ((now / 250L) % 2 == 0)) {
                // Blink tick: skip drawing text or draw dim
            }

            String name = effectName(effect);
            String amp = toRoman(effect.getAmplifier());
            String timeStr = timeText(effect);

            int nameColor = color();
            if (getColorMode() == ColorMode.SOLID && useCustomColors) {
                nameColor = effect.getEffect().value().getColor();
                if ((nameColor & 0xFF000000) == 0) nameColor |= 0xFF000000;
            }

            int timerColor = blinking ? 0xFFFF4444 : ((getColorMode() == ColorMode.RAINBOW || getColorMode() == ColorMode.WAVE) ? color(80L) : 0xFFAAAAAA);

            if (displayStyle == DisplayStyle.ORIGINAL) {
                int tileWidth = layout.itemWidths().get(index);
                renderBackgroundAndBorder(graphics, curX, curY, tileWidth, TILE_SIZE, effectBorderColor(effect, blinking));
                Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                if (effectId != null) {
                    Identifier texture = Identifier.fromNamespaceAndPath(
                            effectId.getNamespace(), "textures/mob_effect/" + effectId.getPath() + ".png");
                    ModuleIconRenderer.drawTexture(graphics, texture, curX + 6, curY + 6, 18);
                }
                graphics.text(client.font, amp,
                        curX + tileWidth - textWidth(client, amp) - ORIGINAL_TEXT_INSET,
                        curY + ORIGINAL_TEXT_INSET, nameColor, true);
                if (showTime) graphics.text(client.font, timeStr,
                        curX + Math.max(ORIGINAL_TEXT_INSET, (tileWidth - textWidth(client, timeStr)) / 2),
                        curY + TILE_SIZE - ORIGINAL_TEXT_INSET - 9, timerColor, true);
                if (vertical) curY += TILE_SIZE + ITEM_GAP;
                else curX += tileWidth + ITEM_GAP;
            } else if (displayStyle == DisplayStyle.COMPACT) {
                String label = compactLabel(name, amp);
                graphics.text(client.font, label, curX, curY + 2, nameColor);
                if (showTime) {
                    graphics.text(client.font, timeStr, curX + textWidth(client, label) + 4, curY + 2, timerColor);
                }
                if (vertical) curY += layout.itemHeight() + ITEM_GAP;
                else curX += layout.itemWidths().get(index) + 4;
            } else {
                graphics.text(client.font, name + " " + amp, curX, curY + 1, nameColor);
                if (showTime) {
                    graphics.text(client.font, timeStr, curX, curY + 10, timerColor);
                }
                if (vertical) curY += layout.itemHeight() + ITEM_GAP;
                else curX += layout.itemWidths().get(index) + 4;
            }
        }

        graphics.pose().popMatrix();
    }
}
