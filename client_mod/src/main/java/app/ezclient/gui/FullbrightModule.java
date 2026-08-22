package app.ezclient.gui;

import net.minecraft.client.Minecraft;

/** Client-side brightness toggle. Restores the player's previous gamma on disable. */
public final class FullbrightModule extends Module {
    private double previousGamma = 1.0;

    public FullbrightModule() {
        super("Fullbright", "Visual", false);
    }

    @Override
    protected void onToggle() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        if (isEnabled()) {
            previousGamma = client.options.gamma().get();
            client.options.gamma().set(16.0);
        } else {
            client.options.gamma().set(previousGamma);
        }
    }
}
