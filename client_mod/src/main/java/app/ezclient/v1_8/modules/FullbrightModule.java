package app.ezclient.v1_8.modules;

import net.minecraft.client.MinecraftClient;

public class FullbrightModule extends Module {
    public FullbrightModule() {
        super("fullbright", "Fullbright", "Sets gamma brightness to 1000% so caves are bright", "Render", true, 0, 0, false);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null && client.options.gamma < 10.0F) {
            client.options.gamma = 15.0F;
        }
    }

    @Override
    public void onToggle(boolean newState) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            client.options.gamma = newState ? 15.0F : 1.0F;
        }
    }
}
