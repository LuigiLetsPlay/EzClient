package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;
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
        super("armor", "Armor Status", "Displays equipped armor and item durabilities", "HUD", true, 4, 120, true);
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
            for (Field f : invClass.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase("armor")) {
                    armorField = f;
                    armorField.setAccessible(true);
                    break;
                }
            }
            for (Method m : invClass.getMethods()) {
                if (m.getName().toLowerCase().contains("armor") && m.getParameterTypes().length == 1 && m.getParameterTypes()[0] == int.class) {
                    getArmorMethod = m;
                    getArmorMethod.setAccessible(true);
                    break;
                }
            }
        } catch (Throwable ignored) {}
        reflectionInit = true;
    }

    public static ItemStack getArmorStack(int slot) {
        if (slot < 0 || slot > 3) return null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;

        Object inv = null;
        try {
            for (Method m : client.player.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("inventory") && m.getParameterTypes().length == 0) {
                    inv = m.invoke(client.player);
                    break;
                }
            }
            if (inv == null) {
                Field f = client.player.getClass().getField("inventory");
                inv = f.get(client.player);
            }
        } catch (Throwable ignored) {}

        if (inv == null) return null;
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
        if (client.player == null) return;

        int x = getPosX();
        int y = getPosY();
        int offset = 0;
        int color = getTextColor(0xFFFFAA00);

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = getArmorStack(i);
            if (stack != null && !stack.isEmpty()) {
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
        if (client.player != null) {
            for (int i = 0; i < 4; i++) {
                if (getArmorStack(i) != null && !getArmorStack(i).isEmpty()) {
                    hasArmor = true;
                    break;
                }
            }
        }

        if (hasArmor) {
            renderHud(tickDelta);
        } else {
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
