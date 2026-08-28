package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

/**
 * Fullbright & Gamma Boost module with brightness percentage,
 * smooth fade transitions, and Nether / End dimension blacklists.
 */
public class FullbrightModule extends Module {
    private int brightnessLevel = 1000; // 100% - 1500%
    private boolean smoothFade = true;
    private boolean disableInNether = false;
    private boolean disableInEnd = false;

    private float currentFade = 0.0f;

    public FullbrightModule() {
        super("Fullbright", "RENDER", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/fullbright.png");
    }

    public int getBrightnessLevel() { return brightnessLevel; }
    public void setBrightnessLevel(int brightnessLevel) { this.brightnessLevel = Math.max(100, Math.min(1500, brightnessLevel)); ConfigManager.save(); }

    public boolean isSmoothFade() { return smoothFade; }
    public void setSmoothFade(boolean smoothFade) { this.smoothFade = smoothFade; ConfigManager.save(); }

    public boolean isDisableInNether() { return disableInNether; }
    public void setDisableInNether(boolean disableInNether) { this.disableInNether = disableInNether; ConfigManager.save(); }

    public boolean isDisableInEnd() { return disableInEnd; }
    public void setDisableInEnd(boolean disableInEnd) { this.disableInEnd = disableInEnd; ConfigManager.save(); }

    public boolean isDimensionAllowed(Minecraft client) {
        if (client == null || client.level == null) return true;
        if (disableInNether && client.level.dimension() == Level.NETHER) return false;
        if (disableInEnd && client.level.dimension() == Level.END) return false;
        return true;
    }

    @Override
    public void onTick() {
        super.onTick();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        boolean active = isEnabled() && isDimensionAllowed(client);

        if (active) {
            client.player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, 999999, 0, false, false, false
            ));
        } else {
            // Remove night vision effect if client was giving it
            MobEffectInstance inst = client.player.getEffect(MobEffects.NIGHT_VISION);
            if (inst != null && inst.getDuration() > 10000) {
                client.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    @Override
    public boolean hasSettings() {
        return true;
    }
}
