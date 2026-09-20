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
public final class MotionBlurModule extends FeatureModule {
    private boolean hasPreviousCameraSample = false;
    private float previousYaw;
    private float previousPitch;
    private Vec3 previousPosition = Vec3.ZERO;

    public MotionBlurModule() {
        super("Motion Blur", false, 0);

        option("Einstellungen", "blurStrength", "Unschärfe-Stärke (%)", "Bestimmt die Intensität der Bewegungsunschärfe bei Kameradrehungen.", 40.0, 20.0, 100.0);
        flag("Leistungsschutz", "fpsProtection", "FPS-Schutz", "Reduziert oder pausiert den Unschärfe-Effekt bei niedrigen Bildraten.", true);
        option("Leistungsschutz", "fpsThreshold", "FPS-Schwelle", "Minimale Bildrate, ab welcher der Unschärfe-Effekt aktiv bleibt.", "60 FPS", 0, 0, "30 FPS", "60 FPS", "75 FPS", "120 FPS", "144 FPS");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/motion_blur.png");
    }

    @Override
    public String getDescription() {
        return "Erzeugt bei Kamerabewegung einen einstellbaren Bewegungsunschärfe-Effekt mit FPS-Schutz.";
    }

    public int getBlurStrength() {
        return (int) Math.round(number("blurStrength"));
    }

    public void setBlurStrength(int blurStrength) {
        int clamped = Math.max(20, Math.min(100, blurStrength));
        set("blurStrength", (double) (Math.round(clamped / 20.0f) * 20));
        ConfigManager.save();
    }

    public boolean isFpsProtection() { return flag("fpsProtection"); }
    public void setFpsProtection(boolean fpsProtection) {
        set("fpsProtection", fpsProtection);
        ConfigManager.save();
    }

    public int getFpsThreshold() {
        String s = text("fpsThreshold").replace(" FPS", "").trim();
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 60;
        }
    }

    public void setFpsThreshold(int fpsThreshold) {
        set("fpsThreshold", fpsThreshold + " FPS");
        ConfigManager.save();
    }

    /** Returns true only on frames where the camera actually moved. */
    public boolean shouldRenderMotionBlur(Camera camera) {
        if (!isEnabled() || camera == null || !camera.isInitialized()) {
            resetCameraTracking();
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (EzScreenBridge.current(client) != null || (isFpsProtection() && client.getFps() < getFpsThreshold())) {
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
        int preset = Math.min(5, Math.max(1, getBlurStrength() / 20));
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
