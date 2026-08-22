package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Durability summary for armor plus both held items. */
public final class ArmorStatusModule extends HudModule {
    public ArmorStatusModule() { super("Armor Status", "HUD", false, 6, 70, "Armor: ", ""); }
    @Override protected String value(Minecraft client) {
        if (client.player == null) return "-";
        int usable = 0, total = 0;
        for (ItemStack stack : client.player.getArmorSlots()) {
            if (!stack.isDamageableItem()) continue;
            usable += stack.getMaxDamage() - stack.getDamageValue(); total += stack.getMaxDamage();
        }
        for (ItemStack stack : new ItemStack[]{client.player.getMainHandItem(), client.player.getOffhandItem()}) {
            if (!stack.isDamageableItem()) continue;
            usable += stack.getMaxDamage() - stack.getDamageValue(); total += stack.getMaxDamage();
        }
        return total == 0 ? "No durable gear" : Math.round(usable * 100.0 / total) + "%";
    }
}
