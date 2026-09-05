package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PotionEffectModule extends Module {
    public PotionEffectModule() {
        super("potions", "Potion Effects", "Displays active potion effects and durations", "HUD", true, 4, 200, true);
    }

    @Override
    public int getWidth() {
        return 110;
    }

    @Override
    public int getHeight() {
        return 38;
    }

    public static class PotionEntry {
        public String name;
        public int amplifier;
        public int durationTicks;

        public PotionEntry(String name, int amplifier, int durationTicks) {
            this.name = name;
            this.amplifier = amplifier;
            this.durationTicks = durationTicks;
        }
    }

    public static List<PotionEntry> getActiveEffects(MinecraftClient client) {
        List<PotionEntry> result = new ArrayList<PotionEntry>();
        if (client.player == null) return result;

        try {
            Object player = client.player;
            Object effectsObj = null;
            for (Method m : player.getClass().getMethods()) {
                String mn = m.getName().toLowerCase();
                if ((mn.contains("statuseffect") || mn.contains("potioneffect") || mn.contains("activepotions")) && m.getParameterTypes().length == 0) {
                    effectsObj = m.invoke(player);
                    if (effectsObj != null) break;
                }
            }

            Collection<?> list = null;
            if (effectsObj instanceof Collection) {
                list = (Collection<?>) effectsObj;
            } else if (effectsObj instanceof Map) {
                list = ((Map<?, ?>) effectsObj).values();
            }

            if (list != null) {
                for (Object inst : list) {
                    if (inst == null) continue;
                    int duration = 0;
                    int amp = 0;
                    String name = "Potion";

                    for (Method m : inst.getClass().getMethods()) {
                        String mn = m.getName().toLowerCase();
                        if (mn.contains("duration") && m.getParameterTypes().length == 0) {
                            duration = ((Number) m.invoke(inst)).intValue();
                        } else if (mn.contains("amplifier") && m.getParameterTypes().length == 0) {
                            amp = ((Number) m.invoke(inst)).intValue();
                        } else if (mn.contains("name") && m.getParameterTypes().length == 0) {
                            Object val = m.invoke(inst);
                            if (val != null) name = val.toString();
                        }
                    }

                    if (name.startsWith("potion.") || name.startsWith("effect.")) {
                        name = name.substring(7);
                    }
                    if (name.length() > 0) {
                        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    }

                    String lower = name.toLowerCase();
                    if (lower.contains("nightvision") || lower.contains("night_vision") || lower.contains("night vision")) {
                        if (ModuleManager.getInstance().getFullbrightModule().isEnabled() || duration > 20000) {
                            continue;
                        }
                    }

                    result.add(new PotionEntry(name, amp + 1, duration));
                }
            }
        } catch (Throwable ignored) {}

        return result;
    }

    private static String formatDuration(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static String getRomanNumeral(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(level);
        }
    }

    @Override
    public void renderHud(float tickDelta) {
        if (!isShowHud()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<PotionEntry> effects = getActiveEffects(client);
        if (effects.isEmpty()) return;

        int x = getPosX();
        int y = getPosY();
        int offset = 0;
        int color = getTextColor(0xFF55FFFF);

        for (PotionEntry inst : effects) {
            String ampStr = inst.amplifier > 1 ? " " + getRomanNumeral(inst.amplifier) : "";
            String durStr = formatDuration(inst.durationTicks);

            String text = inst.name + ampStr + " (" + durStr + ")";
            int w = RenderUtils.getStringWidth(text);

            boolean blink = (inst.durationTicks <= 100) && (System.currentTimeMillis() / 250 % 2 == 0);
            int textColor = blink ? 0xFFFF4444 : color;

            if (isShowBackground()) {
                RenderUtils.drawRect(x - 2, y + offset - 2, x + w + 4, y + offset + 10, 0x80000000);
            }
            RenderUtils.drawString(text, x + 1, y + offset, textColor, true);
            offset += 12;
        }
    }

    @Override
    public void renderEditorPreview(float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<PotionEntry> active = getActiveEffects(client);
        if (!active.isEmpty()) {
            renderHud(tickDelta);
        } else {
            int x = getPosX();
            int y = getPosY();
            int color = getTextColor(0xFF55FFFF);

            String[] samples = {
                "Speed II (1:30)",
                "Strength I (0:45)",
                "Fire Resistance (2:10)"
            };

            int offset = 0;
            for (String sample : samples) {
                int w = RenderUtils.getStringWidth(sample);
                if (isShowBackground()) {
                    RenderUtils.drawRect(x - 2, y + offset - 2, x + w + 4, y + offset + 10, 0x80000000);
                }
                RenderUtils.drawString(sample, x + 1, y + offset, color, true);
                offset += 12;
            }
        }
    }
}
