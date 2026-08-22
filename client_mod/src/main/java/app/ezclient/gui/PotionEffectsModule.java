package app.ezclient.gui;

import net.minecraft.client.Minecraft;

/** Safe compact effect count; detailed effect icons are added by the overlay renderer. */
public final class PotionEffectsModule extends HudModule {
    public PotionEffectsModule() { super("Potion Effects", "HUD", false, 6, 102, "Effects: ", ""); }
    @Override protected String value(Minecraft client) {
        if (client.player == null) return "0";
        return Integer.toString(client.player.getActiveEffects().size());
    }
}
