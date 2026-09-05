package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class ArmorStatusModule extends Module {
    private static final String[] SLOT_NAMES = {"Boots", "Leggings", "Chestplate", "Helmet"};
    private static Field armorField = null;
    private static Method getArmorMethod = null;
    private static boolean reflectionInit = false;

    public ArmorStatusModule() {
        super("armor", "Armor Status", "Displays equipped armor and item durabilities", "HUD", true, 4, 140, true);
    }

    @Override
    public int getWidth() {
        return 120;
    }

    @Override
    public int getHeight() {
        return 52;
    }

    private static void initReflection(Class<?> invClass) {
        if (reflectionInit) return;
        try {
            armorField = invClass.getDeclaredField("armor");
            armorField.setAccessible(true);
        } catch (Throwable ignored) {}
        try {
            if (armorField == null) {
                armorField = invClass.getField("armor");
                armorField.setAccessible(true);
            }
        } catch (Throwable ignored) {}

        for (Method m : invClass.getMethods()) {
            if (m.getName().toLowerCase().contains("armor") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == int.class) {
                getArmorMethod = m;
                getArmorMethod.setAccessible(true);
                break;
            }
        }
        reflectionInit = true;
    }

    public static ItemStack getArmorStack(int slot) {
        if (slot < 0 || slot > 3) return null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.inventory == null) return null;

        Object inv = client.player.inventory;
        initReflection(inv.getClass());

        if (armorField != null) {
            try {
                Object val = armorField.get(inv);
                if (val instanceof ItemStack[]) {
                    ItemStack[] arr = (ItemStack[]) val;
                    if (slot < arr.length) return arr[slot];
                } else if (val instanceof List) {
                    List<?> list = (List<?>) val;
                    if (slot < list.size()) return (ItemStack) list.get(slot);
                }
            } catch (Throwable ignored) {}
        }

        if (getArmorMethod != null) {
            try {
                return (ItemStack) getArmorMethod.invoke(inv, slot);
            } catch (Throwable ignored) {}
        }

        return null;
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.player.inventory == null) return;

        int x = getPosX();
        int y = getPosY();
        int offset = 0;
        int color = getTextColor(0xFFFFAA00);

        // Helmet (3), Chestplate (2), Leggings (1), Boots (0)
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = getArmorStack(i);
            if (stack != null && stack.getItem() != null) {
                int maxDurability = stack.getMaxDamage();
                int currentDurability = maxDurability - stack.getDamage();
                String slotLabel = SLOT_NAMES[i];
                String text;
                if (maxDurability > 0) {
                    int percent = Math.max(0, (currentDurability * 100) / maxDurability);
                    text = slotLabel + ": " + currentDurability + "/" + maxDurability + " (" + percent + "%)";
                } else {
                    text = slotLabel;
                }

                int w = RenderUtils.getStringWidth(text);
                if (isShowBackground()) {
                    RenderUtils.drawRect(x - 2, y + offset - 2, x + w + 4, y + offset + 10, 0x80000000);
                }
                RenderUtils.drawString(text, x + 1, y + offset, color, true);
                offset += 13;
            }
        }
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean hasArmor = false;
        if (client.player != null && client.player.inventory != null) {
            for (int i = 0; i < 4; i++) {
                if (getArmorStack(i) != null) {
                    hasArmor = true;
                    break;
                }
            }
        }

        if (hasArmor) {
            renderHud(tickDelta);
        } else {
            // Draw clean preview lines so user can position it easily in HUD editor
            int x = getPosX();
            int y = getPosY();
            int color = getTextColor(0xFFFFAA00);
            String[] samples = {
                "Helmet: 363/363 (100%)",
                "Chestplate: 528/528 (100%)",
                "Leggings: 495/495 (100%)",
                "Boots: 429/429 (100%)"
            };

            int offset = 0;
            for (String sample : samples) {
                int w = RenderUtils.getStringWidth(sample);
                if (isShowBackground()) {
                    RenderUtils.drawRect(x - 2, y + offset - 2, x + w + 4, y + offset + 10, 0x80000000);
                }
                RenderUtils.drawString(sample, x + 1, y + offset, color, true);
                offset += 13;
            }
        }
    }
}
