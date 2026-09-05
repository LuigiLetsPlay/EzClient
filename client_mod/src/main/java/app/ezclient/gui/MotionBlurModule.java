package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Motion Blur Module:
 * Adds smooth visual camera motion blur with intensity controls and automatic FPS protection.
 */
public final class MotionBlurModule extends Module {
    private int blurStrength = 40;
    private boolean fpsProtection = true;
    private int fpsThreshold = 60;
    private boolean hasPreviousCameraSample = false;
    private float previousYaw;
    private float previousPitch;
    private Vec3 previousPosition = Vec3.ZERO;

    public MotionBlurModule() {
        super("Motion Blur", "Render", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/motion_blur.png");
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public int getBlurStrength() { return blurStrength; }
    public void setBlurStrength(int blurStrength) {
        int clamped = Math.max(20, Math.min(100, blurStrength));
        this.blurStrength = Math.round(clamped / 20.0f) * 20;
        ConfigManager.save();
    }

    public boolean isFpsProtection() { return fpsProtection; }
    public void setFpsProtection(boolean fpsProtection) { this.fpsProtection = fpsProtection; ConfigManager.save(); }

    public int getFpsThreshold() { return fpsThreshold; }
    public void setFpsThreshold(int fpsThreshold) { this.fpsThreshold = Math.max(30, Math.min(144, fpsThreshold)); ConfigManager.save(); }

    /** Returns true only on frames where the camera actually moved. */
    public boolean shouldRenderMotionBlur(Camera camera) {
        if (!isEnabled() || camera == null || !camera.isInitialized()) {
            resetCameraTracking();
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (EzScreenBridge.current(client) != null || (fpsProtection && client.getFps() < fpsThreshold)) {
            resetCameraTracking();
            return false;
        }

        float yaw = camera.yaw();
        float pitch = camera.xRot();
        Vec3 position = camera.position();
        if (!hasPreviousCameraSample) {
            rememberCamera(yaw, pitch, position);
            return false;
        }

        float rotationDelta = Math.abs(Mth.wrapDegrees(yaw - previousYaw)) + Math.abs(pitch - previousPitch);
        double positionDelta = position.distanceToSqr(previousPosition);
        rememberCamera(yaw, pitch, position);

        // Ignore tiny floating-point camera jitter while standing still.
        return rotationDelta >= 0.08f || positionDelta >= 0.000004D;
    }

    public Identifier getPostChainId() {
        int preset = Math.min(5, Math.max(1, blurStrength / 20));
        return Identifier.fromNamespaceAndPath("ezclient", "motion_blur_" + preset);
    }

    private void rememberCamera(float yaw, float pitch, Vec3 position) {
        previousYaw = yaw;
        previousPitch = pitch;
        previousPosition = position;
        hasPreviousCameraSample = true;
    }

    private void resetCameraTracking() {
        hasPreviousCameraSample = false;
    }

    @Override
    protected void onToggle() {
        resetCameraTracking();
    }
}
