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
public class FullbrightModule extends FeatureModule {
    private float currentFade = 0.0f;

    public FullbrightModule() {
        super("Fullbright", false, 0);

        option("Helligkeit", "brightnessLevel", "Helligkeitsstufe (%)", "Stärke der Gammamodifikation in Prozent.", 1000.0, 100.0, 1500.0);
        flag("Übergang", "smoothFade", "Sanftes Einblenden", "Weicher Helligkeitsübergang beim Ein- und Ausschalten.", true);
        flag("Dimensionen", "disableInNether", "Im Nether deaktivieren", "Schaltet Fullbright in der Nether-Dimension automatisch ab.", false);
        flag("Dimensionen", "disableInEnd", "Im Ende deaktivieren", "Schaltet Fullbright in der Ende-Dimension automatisch ab.", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/fullbright.png");
    }

    @Override
    public String getDescription() {
        return "Erhöht die Sichtbarkeit in dunklen Bereichen und kann pro Dimension gezielt deaktiviert werden.";
    }

    public int getBrightnessLevel() { return (int) Math.round(number("brightnessLevel")); }
    public void setBrightnessLevel(int brightnessLevel) {
        set("brightnessLevel", (double) Math.max(100, Math.min(1500, brightnessLevel)));
        ConfigManager.save();
    }

    public boolean isSmoothFade() { return flag("smoothFade"); }
    public void setSmoothFade(boolean smoothFade) {
        set("smoothFade", smoothFade);
        ConfigManager.save();
    }

    public boolean isDisableInNether() { return flag("disableInNether"); }
    public void setDisableInNether(boolean disableInNether) {
        set("disableInNether", disableInNether);
        ConfigManager.save();
    }

    public boolean isDisableInEnd() { return flag("disableInEnd"); }
    public void setDisableInEnd(boolean disableInEnd) {
        set("disableInEnd", disableInEnd);
        ConfigManager.save();
    }

    public boolean isDimensionAllowed(Minecraft client) {
        if (client == null || client.level == null) return true;
        if (isDisableInNether() && client.level.dimension() == Level.NETHER) return false;
        if (isDisableInEnd() && client.level.dimension() == Level.END) return false;
        return true;
    }

    @Override
    public void setKeyBind(int keyBind) {
        super.setKeyBind(keyBind);
        EzKeyBindings.setKeyCode(EzKeyBindings.KEY_FULLBRIGHT, keyBind);
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
