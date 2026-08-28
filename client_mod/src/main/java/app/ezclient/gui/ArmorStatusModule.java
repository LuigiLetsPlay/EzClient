package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Full Equipment Info & ArmorStatus HUD with 6 slots (Armor + MainHand + OffHand),
 * dynamic durability modes (Percent, Hits, Bar, Icon), damage warning alerts, and stack counts.
 */
public final class ArmorStatusModule extends HudModule {
    public enum DurabilityMode { PERCENT, HITS, DAMAGE_BAR, ICON_ONLY }

    private boolean horizontal = false;
    private DurabilityMode durabilityMode = DurabilityMode.PERCENT;
    private boolean colorTiers = true;
    private boolean damageWarning = true;
    private boolean showItemCount = true;
    private boolean showHands = true;

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

    public boolean isShowHands() { return showHands; }
    public void setShowHands(boolean showHands) { this.showHands = showHands; ConfigManager.save(); }

    private int getSlotCount() {
        return showHands ? 6 : 4;
    }

    @Override
    public int getWidth(Minecraft client) {
        int slots = getSlotCount();
        return horizontal ? slots * 22 : 22;
    }

    @Override
    public int getHeight(Minecraft client) {
        int slots = getSlotCount();
        return horizontal ? 22 : slots * 22;
    }

    @Override
    protected String value(Minecraft client) {
        return "Armor";
    }

    private static ItemStack getDummyItem(int index) {
        try {
            return switch (index) {
                case 0 -> { if (dummyHelmet == null) dummyHelmet = new ItemStack(Items.NETHERITE_HELMET); yield dummyHelmet; }
                case 1 -> { if (dummyChest == null) dummyChest = new ItemStack(Items.NETHERITE_CHESTPLATE); yield dummyChest; }
                case 2 -> { if (dummyLegs == null) dummyLegs = new ItemStack(Items.NETHERITE_LEGGINGS); yield dummyLegs; }
                case 3 -> { if (dummyBoots == null) dummyBoots = new ItemStack(Items.NETHERITE_BOOTS); yield dummyBoots; }
                case 4 -> { if (dummyMainHand == null) dummyMainHand = new ItemStack(Items.NETHERITE_SWORD); yield dummyMainHand; }
                case 5 -> { if (dummyOffHand == null) dummyOffHand = new ItemStack(Items.TOTEM_OF_UNDYING); yield dummyOffHand; }
                default -> ItemStack.EMPTY;
            };
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        float scale = (float) getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(scale, scale);

        int totalW = getWidth(client);
        int totalH = getHeight(client);

        renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);

        int slots = getSlotCount();

        // Inner slot dividers
        if (hasBorder()) {
            int divColor = getBorderColor();
            for (int i = 1; i < slots; i++) {
                if (horizontal) {
                    graphics.fill(i * 22, 0, i * 22 + 1, 22, divColor);
                } else {
                    graphics.fill(0, i * 22, 22, i * 22 + 1, divColor);
                }
            }
        }

        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        boolean criticalDamageFound = false;

        for (int i = 0; i < slots; i++) {
            int sx = horizontal ? i * 22 : 0;
            int sy = horizontal ? 0 : i * 22;

            ItemStack item = ItemStack.EMPTY;
            if (client.player != null) {
                if (i < 4) {
                    item = client.player.getItemBySlot(armorSlots[i]);
                } else if (i == 4) {
                    item = client.player.getMainHandItem();
                } else {
                    item = client.player.getOffhandItem();
                }
            }

            if (item.isEmpty() && editor) {
                item = getDummyItem(i);
            }

            if (item != null && !item.isEmpty()) {
                try {
                    graphics.item(item, sx + 3, sy + 3);
                    graphics.itemDecorations(client.font, item, sx + 3, sy + 3);

                    if (item.isDamageableItem()) {
                        int maxDamage = item.getMaxDamage();
                        int damage = item.getDamageValue();
                        int remaining = maxDamage - damage;
                        float ratio = (float) remaining / maxDamage;

                        if (ratio < 0.10f) criticalDamageFound = true;

                        int durColor = 0xFFFFFFFF;
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
                        } else if (durabilityMode == DurabilityMode.DAMAGE_BAR) {
                            int barW = 16;
                            int fillW = Math.max(1, (int) (ratio * barW));
                            graphics.fill(sx + 3, sy + 19, sx + 3 + barW, sy + 21, 0xFF000000);
                            graphics.fill(sx + 3, sy + 19, sx + 3 + fillW, sy + 20, durColor);
                        }
                    } else if (showItemCount && item.getCount() > 1) {
                        graphics.pose().pushMatrix();
                        graphics.pose().translate(sx + 15, sy + 14);
                        graphics.pose().scale(0.6f, 0.6f);
                        graphics.text(client.font, String.valueOf(item.getCount()), 0, 0, 0xFFFFFFFF);
                        graphics.pose().popMatrix();
                    }
                } catch (Throwable ignored) {}
            }
        }

        // Damage Warning Sound trigger
        if (damageWarning && criticalDamageFound && !editor && client.player != null) {
            long now = System.currentTimeMillis();
            if (now - lastWarningTime > 15000) { // Throttle warning sound to every 15s
                lastWarningTime = now;
                try {
                    client.player.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(), 0.8f, 1.2f);
                } catch (Throwable ignored) {}
            }
        }

        graphics.pose().popMatrix();
    }
}
