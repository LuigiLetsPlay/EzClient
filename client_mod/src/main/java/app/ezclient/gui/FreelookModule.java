package app.ezclient.gui;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Keeps player aim fixed while the camera is rotated with a held or toggled key. */
public final class FreelookModule extends FeatureModule {
    private boolean active;
    private float yaw;
    private float pitch;
    private CameraType prevCameraType = null;

    public FreelookModule() {
        super("Freelook", false, 0);
        setKeyBind(GLFW.GLFW_KEY_LEFT_ALT);
        flag("Allgemein", "hold", "Hold to freelook", "Freelook ist nur aktiv, solange die Taste gehalten wird.", true);
        flag("Kamera", "thirdPerson", "Switch to third person", "Wechselt beim Aktivieren automatisch in die Rückansicht.", true);
        flag("Kamera", "invertY", "Invert Y", "Kehrt die vertikale Kamerabewegung um.", false);
        option("Kamera", "sensitivity", "Sensitivity", "Multiplikator für die Kamerabewegung.", 1.0, 0.1, 3.0);
        option("Kamera", "pitchLimit", "Pitch limit", "Maximaler vertikaler Blickwinkel.", 90.0, 30, 90);
    }

    @Override
    public void setKeyBind(int keyBind) {
        super.setKeyBind(keyBind);
        EzKeyBindings.setKeyCode(EzKeyBindings.KEY_FREELOOK, keyBind);
    }

    public boolean isActive() { return isEnabled() && active; }
    public float cameraYaw() { return yaw; }
    public float cameraPitch() { return pitch; }

    public void begin(Minecraft mc) {
        if (active || mc == null || mc.player == null) return;
        active = true;
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
        if (flag("thirdPerson") && mc.options != null && mc.options.getCameraType().isFirstPerson()) {
            prevCameraType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else {
            prevCameraType = null;
        }
    }

    public void end() {
        if (!active) return;
        active = false;
        Minecraft mc = Minecraft.getInstance();
        if (prevCameraType != null && mc != null && mc.options != null) {
            mc.options.setCameraType(prevCameraType);
            prevCameraType = null;
        }
    }

    public void turn(double x, double y) {
        double multiplier = number("sensitivity");
        yaw += (float)(x * multiplier);
        pitch += (float)(y * multiplier * (flag("invertY") ? -1.0 : 1.0));
        pitch = Mth.clamp(pitch, (float)-number("pitchLimit"), (float)number("pitchLimit"));
    }

    @Override public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null) {
            if (active) end();
            return;
        }
        if (EzScreenBridge.current(mc) != null) {
            if (flag("hold") && active) end();
            return;
        }
        if (flag("hold")) {
            boolean pressed = EzKeyBindings.KEY_FREELOOK != null && EzKeyBindings.KEY_FREELOOK.isDown();
            if (pressed) begin(mc); else end();
        } else if (EzKeyBindings.KEY_FREELOOK != null) {
            while (EzKeyBindings.KEY_FREELOOK.consumeClick()) {
                if (active) end(); else begin(mc);
            }
        }
    }

    @Override protected void onToggle() { if (!isEnabled()) end(); }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/freelook.png"); }
}
