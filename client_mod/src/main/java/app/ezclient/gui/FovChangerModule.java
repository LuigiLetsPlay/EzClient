package app.ezclient.gui;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;

/**
 * FOV Changer & Dynamic FOV Lock Module:
 * Uncouples field of view from aggressive gameplay effects with customizable multipliers
 * for sprinting, potions, bow charging, flying, and smooth interpolation.
 */
public final class FovChangerModule extends FeatureModule {
    private float currentFovModifier = 1.0f;

    public FovChangerModule() {
        super("FOV Changer", false, 0);

        flag("Allgemein", "staticFovLock", "Statisches FOV (Lock)", "Hält das Sichtfeld konstant und sperrt alle dynamischen FOV-Modifikatoren.", false);
        flag("Allgemein", "smoothInterpolation", "Sanfte Übergänge", "Glättet Änderungen des Sichtfelds mit weichen Animationen.", true);

        option("Modifikatoren", "sprintMultiplier", "Sprint-Multiplikator", "Sichtfeld-Faktor beim normalen Sprinten.", 1.15, 0.70, 1.40);
        option("Modifikatoren", "speedPotionMultiplier", "Speed-Effekt", "Sichtfeld-Faktor unter dem Schnelligkeitseffekt.", 1.20, 0.70, 1.40);
        option("Modifikatoren", "slownessPotionMultiplier", "Slowness-Effekt", "Sichtfeld-Faktor unter dem Langsamkeitseffekt.", 0.85, 0.60, 1.10);
        option("Modifikatoren", "bowAimMultiplier", "Bogen-Spannen", "Sichtfeld-Faktor beim Spannen des Bogens.", 0.85, 0.50, 1.10);
        option("Modifikatoren", "flyingMultiplier", "Fliegen-Multiplikator", "Sichtfeld-Faktor beim Fliegen.", 1.10, 0.70, 1.40);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/zoom.png");
    }

    @Override
    public String getDescription() {
        return "Steuert, wie Sprinten, Effekte, Bogen und Fliegen dein Sichtfeld beeinflussen.";
    }

    public boolean isStaticFovLock() { return flag("staticFovLock"); }
    public void setStaticFovLock(boolean staticFovLock) {
        set("staticFovLock", staticFovLock);
        ConfigManager.save();
    }

    public float getSprintMultiplier() { return (float) number("sprintMultiplier"); }
    public void setSprintMultiplier(float sprintMultiplier) {
        set("sprintMultiplier", (double) sprintMultiplier);
        ConfigManager.save();
    }

    public float getSpeedPotionMultiplier() { return (float) number("speedPotionMultiplier"); }
    public void setSpeedPotionMultiplier(float speedPotionMultiplier) {
        set("speedPotionMultiplier", (double) speedPotionMultiplier);
        ConfigManager.save();
    }

    public float getSlownessPotionMultiplier() { return (float) number("slownessPotionMultiplier"); }
    public void setSlownessPotionMultiplier(float slownessPotionMultiplier) {
        set("slownessPotionMultiplier", (double) slownessPotionMultiplier);
        ConfigManager.save();
    }

    public float getBowAimMultiplier() { return (float) number("bowAimMultiplier"); }
    public void setBowAimMultiplier(float bowAimMultiplier) {
        set("bowAimMultiplier", (double) bowAimMultiplier);
        ConfigManager.save();
    }

    public float getFlyingMultiplier() { return (float) number("flyingMultiplier"); }
    public void setFlyingMultiplier(float flyingMultiplier) {
        set("flyingMultiplier", (double) flyingMultiplier);
        ConfigManager.save();
    }

    public boolean isSmoothInterpolation() { return flag("smoothInterpolation"); }
    public void setSmoothInterpolation(boolean smoothInterpolation) {
        set("smoothInterpolation", smoothInterpolation);
        ConfigManager.save();
    }

    public float modifyFov(AbstractClientPlayer player, float vanillaModifier, float delta) {
        if (!isEnabled()) return vanillaModifier;

        if (isStaticFovLock()) {
            if (isSmoothInterpolation()) {
                currentFovModifier += (1.0f - currentFovModifier) * Math.min(1.0f, delta * 0.5f);
                return currentFovModifier;
            }
            return 1.0f;
        }

        float target = 1.0f;

        if (player.isSprinting()) {
            target *= getSprintMultiplier();
        }
        if (player.hasEffect(MobEffects.SPEED)) {
            target *= getSpeedPotionMultiplier();
        }
        if (player.hasEffect(MobEffects.SLOWNESS)) {
            target *= getSlownessPotionMultiplier();
        }
        if (player.isUsingItem() && player.getUseItem().is(Items.BOW)) {
            target *= getBowAimMultiplier();
        }
        if (player.getAbilities().flying) {
            target *= getFlyingMultiplier();
        }

        if (isSmoothInterpolation()) {
            currentFovModifier += (target - currentFovModifier) * Math.min(1.0f, 0.25f);
            return currentFovModifier;
        }

        return target;
    }
}
