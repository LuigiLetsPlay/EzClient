package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Full Equipment Info & ArmorStatus HUD with 6 slots (Armor + MainHand + OffHand),
 * dynamic durability modes (Percent, Hits, Bar, Icon), damage warning alerts, stack counts,
 * and dynamic adaptive box sizing.
 */
public final class ArmorStatusModule extends HudModule {
    public enum DurabilityMode { PERCENT, HITS, DAMAGE_BAR, ICON_ONLY }
    public enum EquipmentMode {
        ARMOR_ONLY("ezclient.hud_settings.armor_mode.armor"),
        MAIN_HAND_ONLY("ezclient.hud_settings.armor_mode.main_hand"),
        BOTH_HANDS("ezclient.hud_settings.armor_mode.both_hands"),
        ARMOR_AND_MAIN_HAND("ezclient.hud_settings.armor_mode.armor_main"),
        ALL("ezclient.hud_settings.armor_mode.all");

        private final String translationKey;

        EquipmentMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getLabel() {
            return app.ezclient.util.EzI18n.get(translationKey);
        }
    }

    private boolean horizontal = false;
    private DurabilityMode durabilityMode = DurabilityMode.PERCENT;
    private boolean colorTiers = true;
    private boolean damageWarning = true;
    private boolean showItemCount = true;
    private EquipmentMode equipmentMode = EquipmentMode.ALL;
    private boolean dynamicBox = true;

    private static ItemStack dummyHelmet = null;
    private static ItemStack dummyChest = null;
    private static ItemStack dummyLegs = null;
    private static ItemStack dummyBoots = null;
    private static ItemStack dummyMainHand = null;
    private static ItemStack dummyOffHand = null;

    private static long lastWarningTime = 0;

    public ArmorStatusModule() {
        super("Armor Status", "HUD", false, 10, 60, "", "");
    }

    @Override
    public String getDescription() {
        return "Zeigt Rüstung, Hand-Items und deren Haltbarkeit direkt im HUD an.";
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/armor_status.png");
    }

    public boolean isHorizontal() { return horizontal; }
    public void setHorizontal(boolean horizontal) { this.horizontal = horizontal; ConfigManager.save(); }

    public DurabilityMode getDurabilityMode() { return durabilityMode; }
    public void setDurabilityMode(DurabilityMode mode) { this.durabilityMode = mode; ConfigManager.save(); }

    public boolean isColorTiers() { return colorTiers; }
    public void setColorTiers(boolean colorTiers) { this.colorTiers = colorTiers; ConfigManager.save(); }

    public boolean isDamageWarning() { return damageWarning; }
    public void setDamageWarning(boolean damageWarning) { this.damageWarning = damageWarning; ConfigManager.save(); }

    public boolean isShowItemCount() { return showItemCount; }
    public void setShowItemCount(boolean showItemCount) { this.showItemCount = showItemCount; ConfigManager.save(); }

    public EquipmentMode getEquipmentMode() { return equipmentMode; }
    public void setEquipmentMode(EquipmentMode equipmentMode) {
        this.equipmentMode = equipmentMode == null ? EquipmentMode.ALL : equipmentMode;
        ConfigManager.save();
    }

    /** Legacy config bridge: old true/false values map to All or Armor Only. */
    public boolean isShowHands() { return equipmentMode != EquipmentMode.ARMOR_ONLY; }
    public void setShowHands(boolean showHands) {
        setEquipmentMode(showHands ? EquipmentMode.ALL : EquipmentMode.ARMOR_ONLY);
    }

    public boolean isDynamicBox() { return dynamicBox; }
    public void setDynamicBox(boolean dynamicBox) { this.dynamicBox = dynamicBox; ConfigManager.save(); }

    private int getSlotCount() {
        return slotIndices().length;
    }

    private int[] slotIndices() {
        return switch (equipmentMode) {
            case ARMOR_ONLY -> new int[]{0, 1, 2, 3};
            case MAIN_HAND_ONLY -> new int[]{4};
            case BOTH_HANDS -> new int[]{4, 5};
            case ARMOR_AND_MAIN_HAND -> new int[]{0, 1, 2, 3, 4};
            case ALL -> new int[]{0, 1, 2, 3, 4, 5};
        };
    }

    private static ItemStack playerItem(Minecraft client, int index) {
        if (client == null || client.player == null) return ItemStack.EMPTY;
        return switch (index) {
            case 0 -> client.player.getItemBySlot(EquipmentSlot.HEAD);
            case 1 -> client.player.getItemBySlot(EquipmentSlot.CHEST);
            case 2 -> client.player.getItemBySlot(EquipmentSlot.LEGS);
            case 3 -> client.player.getItemBySlot(EquipmentSlot.FEET);
            case 4 -> client.player.getMainHandItem();
            case 5 -> client.player.getOffhandItem();
            default -> ItemStack.EMPTY;
        };
    }

    public int getEquippedCount(Minecraft client) {
        if (client == null || client.player == null) return 0;
        int count = 0;
        for (int index : slotIndices()) {
            if (!playerItem(client, index).isEmpty()) count++;
        }
        return count;
    }

    private int getActiveSlotCount(Minecraft client, boolean editor) {
        if (editor) {
            if (client != null && client.player != null && dynamicBox) {
                int equipped = getEquippedCount(client);
                if (equipped > 0) return equipped;
            }
            return getSlotCount();
        }
        if (!dynamicBox) return getSlotCount();
        return getEquippedCount(client);
    }

    private static final int SLOT_SIZE = 22;
    private static final int SLOT_GAP = 2;

    @Override
    public int getWidth(Minecraft client) {
        return getWidth(client, false);
    }

    @Override
    public int getWidth(Minecraft client, boolean editor) {
        int slots = getActiveSlotCount(client, editor);
        int pad = (hasBackground() || hasBorder()) ? CONTENT_PADDING_X : 2;
        if (dynamicBox && slots == 0 && !editor) return SLOT_SIZE + pad * 2;
        int count = Math.max(1, slots);
        return (horizontal ? count * SLOT_SIZE + (count - 1) * SLOT_GAP : SLOT_SIZE) + pad * 2;
    }

    @Override
    public int getHeight(Minecraft client) {
        return getHeight(client, false);
    }

    @Override
    public int getHeight(Minecraft client, boolean editor) {
        int slots = getActiveSlotCount(client, editor);
        int pad = (hasBackground() || hasBorder()) ? CONTENT_PADDING_Y : 1;
        if (dynamicBox && slots == 0 && !editor) return SLOT_SIZE + pad * 2;
        int count = Math.max(1, slots);
        return (horizontal ? SLOT_SIZE : count * SLOT_SIZE + (count - 1) * SLOT_GAP) + pad * 2;
    }

    @Override
    protected String value(Minecraft client) {
        return "Armor";
    }

    private static ItemStack damagedDummy(net.minecraft.world.level.ItemLike item, float remainingRatio) {
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageableItem()) {
            int remaining = Math.max(1, Math.round(stack.getMaxDamage() * remainingRatio));
            stack.setDamageValue(Math.max(0, stack.getMaxDamage() - remaining));
        }
        return stack;
    }

    public static ItemStack getDummyItem(int index) {
        try {
            return switch (index) {
                case 0 -> { if (dummyHelmet == null) dummyHelmet = damagedDummy(Items.NETHERITE_HELMET, 0.85f); yield dummyHelmet; }
                case 1 -> { if (dummyChest == null) dummyChest = damagedDummy(Items.NETHERITE_CHESTPLATE, 0.62f); yield dummyChest; }
                case 2 -> { if (dummyLegs == null) dummyLegs = damagedDummy(Items.NETHERITE_LEGGINGS, 0.42f); yield dummyLegs; }
                case 3 -> { if (dummyBoots == null) dummyBoots = damagedDummy(Items.NETHERITE_BOOTS, 0.08f); yield dummyBoots; }
                case 4 -> { if (dummyMainHand == null) dummyMainHand = damagedDummy(Items.NETHERITE_SWORD, 0.74f); yield dummyMainHand; }
                case 5 -> { if (dummyOffHand == null) dummyOffHand = damagedDummy(Items.SHIELD, 0.35f); yield dummyOffHand; }
                default -> ItemStack.EMPTY;
            };
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        int activeSlots = getActiveSlotCount(client, editor);
        if (dynamicBox && activeSlots == 0 && !editor) {
            return;
        }

        int totalW = getWidth(client, editor);
        int totalH = getHeight(client, editor);
        float scale = (float) getScale();
        int renderX = getRenderX(client, totalW, editor);
        int renderY = getRenderY(client, totalH, editor);

        graphics.pose().pushMatrix();
        graphics.pose().translate(renderX, renderY);
        graphics.pose().scale(scale, scale);

        int padX = (hasBackground() || hasBorder()) ? CONTENT_PADDING_X : 2;
        int padY = (hasBackground() || hasBorder()) ? CONTENT_PADDING_Y : 1;

        List<ItemStack> itemsToRender = new ArrayList<>();
        if (client != null && client.player != null) {
            boolean hasRealEquipped = false;
            for (int index : slotIndices()) {
                ItemStack item = playerItem(client, index);
                if (!item.isEmpty()) {
                    hasRealEquipped = true;
                    break;
                }
            }
            if (hasRealEquipped || !editor) {
                for (int index : slotIndices()) {
                    ItemStack item = playerItem(client, index);
                    if (!dynamicBox || !item.isEmpty()) itemsToRender.add(item);
                }
            } else {
                for (int index : slotIndices()) {
                    itemsToRender.add(getDummyItem(index));
                }
            }
        } else {
            for (int index : slotIndices()) {
                itemsToRender.add(getDummyItem(index));
            }
        }

        for (int i = 0; i < itemsToRender.size(); i++) {
            int sx = padX + (horizontal ? i * (SLOT_SIZE + SLOT_GAP) : 0);
            int sy = padY + (horizontal ? 0 : i * (SLOT_SIZE + SLOT_GAP));

            if (hasBackground() || hasBorder()) {
                renderBackgroundAndBorder(graphics, sx, sy, SLOT_SIZE, SLOT_SIZE);
            }

            ItemStack item = itemsToRender.get(i);

            if (item != null && !item.isEmpty()) {
                try {
                    graphics.item(item, sx + 3, sy + 3);

                    if (durabilityMode == DurabilityMode.ICON_ONLY) {
                        ItemStack clean = item.copy();
                        clean.setDamageValue(0);
                        graphics.itemDecorations(client.font, clean, sx + 3, sy + 3);
                    } else if (durabilityMode == DurabilityMode.DAMAGE_BAR) {
                        if (colorTiers) {
                            graphics.itemDecorations(client.font, item, sx + 3, sy + 3);
                        } else {
                            ItemStack clean = item.copy();
                            clean.setDamageValue(0);
                            graphics.itemDecorations(client.font, clean, sx + 3, sy + 3);
                            if (item.isDamageableItem()) {
                                int maxDamage = item.getMaxDamage();
                                int damage = item.getDamageValue();
                                int remaining = maxDamage - damage;
                                float ratio = Math.max(0f, Math.min(1f, (float) remaining / maxDamage));
                                int barX = sx + 3 + 2;
                                int barY = sy + 3 + 13;
                                int barW = Math.round(13.0f * ratio);
                                graphics.fill(barX, barY, barX + 13, barY + 2, 0xFF000000);
                                graphics.fill(barX, barY, barX + barW, barY + 1, color());
                            }
                        }
                    } else {
                        ItemStack clean = item.copy();
                        clean.setDamageValue(0);
                        graphics.itemDecorations(client.font, clean, sx + 3, sy + 3);

                        if (item.isDamageableItem()) {
                            int maxDamage = item.getMaxDamage();
                            int damage = item.getDamageValue();
                            int remaining = maxDamage - damage;
                            float ratio = (float) remaining / maxDamage;

                            int durColor = color();
                            if (colorTiers) {
                                if (ratio < 0.15f) durColor = 0xFFFF4444; // Red
                                else if (ratio < 0.50f) durColor = 0xFFFFAA00; // Yellow
                                else durColor = 0xFF55FF55; // Green
                            }

                            if (durabilityMode == DurabilityMode.PERCENT) {
                                graphics.pose().pushMatrix();
                                graphics.pose().translate(sx + 11, sy + 15);
                                graphics.pose().scale(0.55f, 0.55f);
                                String text = Math.round(ratio * 100) + "%";
                                graphics.centeredText(client.font, net.minecraft.network.chat.Component.literal(text), 0, 0, durColor);
                                graphics.pose().popMatrix();
                            } else if (durabilityMode == DurabilityMode.HITS) {
                                graphics.pose().pushMatrix();
                                graphics.pose().translate(sx + 11, sy + 15);
                                graphics.pose().scale(0.55f, 0.55f);
                                String text = String.valueOf(remaining);
                                graphics.centeredText(client.font, net.minecraft.network.chat.Component.literal(text), 0, 0, durColor);
                                graphics.pose().popMatrix();
                            }
                        }
                    }

                    if (damageWarning && !editor && item.isDamageableItem()) {
                        float ratio = (float) (item.getMaxDamage() - item.getDamageValue()) / item.getMaxDamage();
                        if (ratio < 0.10f && (System.currentTimeMillis() / 500) % 2 == 0) {
                            graphics.fill(sx, sy, sx + SLOT_SIZE, sy + 1, 0xFFFF0000);
                            graphics.fill(sx, sy + SLOT_SIZE - 1, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFFFF0000);
                            graphics.fill(sx, sy, sx + 1, sy + SLOT_SIZE, 0xFFFF0000);
                            graphics.fill(sx + SLOT_SIZE - 1, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFFFF0000);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        graphics.pose().popMatrix();
    }
}
