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
public final class FovChangerModule extends Module {
    private boolean staticFovLock = false;
    private float sprintMultiplier = 1.15f; // 0.8x to 1.4x
    private float speedPotionMultiplier = 1.20f; // 0.8x to 1.4x
    private float slownessPotionMultiplier = 0.85f; // 0.6x to 1.0x
    private float bowAimMultiplier = 0.85f; // 0.5x to 1.0x
    private float flyingMultiplier = 1.10f; // 0.8x to 1.4x
    private boolean smoothInterpolation = true;

    private float currentFovModifier = 1.0f;

    public FovChangerModule() {
        super("FOV Changer", "Render", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/zoom.png");
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public boolean isStaticFovLock() { return staticFovLock; }
    public void setStaticFovLock(boolean staticFovLock) { this.staticFovLock = staticFovLock; ConfigManager.save(); }

    public float getSprintMultiplier() { return sprintMultiplier; }
    public void setSprintMultiplier(float sprintMultiplier) { this.sprintMultiplier = sprintMultiplier; ConfigManager.save(); }

    public float getSpeedPotionMultiplier() { return speedPotionMultiplier; }
    public void setSpeedPotionMultiplier(float speedPotionMultiplier) { this.speedPotionMultiplier = speedPotionMultiplier; ConfigManager.save(); }

    public float getSlownessPotionMultiplier() { return slownessPotionMultiplier; }
    public void setSlownessPotionMultiplier(float slownessPotionMultiplier) { this.slownessPotionMultiplier = slownessPotionMultiplier; ConfigManager.save(); }

    public float getBowAimMultiplier() { return bowAimMultiplier; }
    public void setBowAimMultiplier(float bowAimMultiplier) { this.bowAimMultiplier = bowAimMultiplier; ConfigManager.save(); }

    public float getFlyingMultiplier() { return flyingMultiplier; }
    public void setFlyingMultiplier(float flyingMultiplier) { this.flyingMultiplier = flyingMultiplier; ConfigManager.save(); }

    public boolean isSmoothInterpolation() { return smoothInterpolation; }
    public void setSmoothInterpolation(boolean smoothInterpolation) { this.smoothInterpolation = smoothInterpolation; ConfigManager.save(); }

    public float modifyFov(AbstractClientPlayer player, float vanillaModifier, float delta) {
        if (!isEnabled()) return vanillaModifier;

        if (staticFovLock) {
            if (smoothInterpolation) {
                currentFovModifier += (1.0f - currentFovModifier) * Math.min(1.0f, delta * 0.5f);
                return currentFovModifier;
            }
            return 1.0f;
        }

        float target = 1.0f;

        if (player.isSprinting()) {
            target *= sprintMultiplier;
        }
        if (player.hasEffect(MobEffects.SPEED)) {
            target *= speedPotionMultiplier;
        }
        if (player.hasEffect(MobEffects.SLOWNESS)) {
            target *= slownessPotionMultiplier;
        }
        if (player.isUsingItem() && player.getUseItem().is(Items.BOW)) {
            target *= bowAimMultiplier;
        }
        if (player.getAbilities().flying) {
            target *= flyingMultiplier;
        }

        if (smoothInterpolation) {
            currentFovModifier += (target - currentFovModifier) * Math.min(1.0f, 0.25f);
            return currentFovModifier;
        }

        return target;
    }
}
