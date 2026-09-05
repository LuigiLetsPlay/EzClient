package app.ezclient.v1_16_v1_20.modules;

import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FullbrightModule extends Module {
    private static Field gammaField = null;
    private static Method getGammaMethod = null;
    private static Method setGammaMethod = null;
    private static boolean gammaInit = false;

    public FullbrightModule() {
        super("fullbright", "Fullbright (Gamma)", "Maximizes world brightness in dark caves", "Render", true, 0, 0, false);
    }

    private static void initGamma(Object options) {
        if (gammaInit || options == null) return;
        try {
            for (Field f : options.getClass().getDeclaredFields()) {
                if (f.getName().toLowerCase().contains("gamma")) {
                    gammaField = f;
                    gammaField.setAccessible(true);
                    break;
                }
            }
            for (Method m : options.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("gamma")) {
                    if (m.getParameterTypes().length == 0) getGammaMethod = m;
                    else if (m.getParameterTypes().length == 1) setGammaMethod = m;
                }
            }
        } catch (Throwable ignored) {}
        gammaInit = true;
    }

    @Override
    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) return;
        initGamma(client.options);

        if (isEnabled()) {
            if (gammaField != null) {
                try {
                    Object val = gammaField.get(client.options);
                    if (val instanceof Number) {
                        gammaField.setDouble(client.options, 16.0);
                    } else if (val != null) {
                        // 1.19+ SimpleOption
                        for (Method sm : val.getClass().getMethods()) {
                            if (sm.getName().toLowerCase().contains("setvalue") && sm.getParameterTypes().length == 1) {
                                sm.invoke(val, 16.0);
                                break;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }
    }
}
