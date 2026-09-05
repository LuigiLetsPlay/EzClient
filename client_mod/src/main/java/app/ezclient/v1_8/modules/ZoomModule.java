package app.ezclient.v1_8.modules;

import app.ezclient.shared.ZoomState;
import org.lwjgl.input.Keyboard;

public class ZoomModule extends Module {
    private int keyBind = Keyboard.KEY_C;
    private boolean zoomPressed = false;
    private final ZoomState state = new ZoomState(4.0, 1.5, 15.0);
    private float currentFovFactor = 1.0F;

    public ZoomModule() {
        super("zoom", "Zoom", "Hold 'C' to zoom in, scroll mouse wheel to adjust", "Render", true, 0, 0, false);
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public boolean isZoomPressed() {
        return zoomPressed;
    }

    /** Mirrors the 26.2 rising-edge behavior: every new C press starts at the configured default. */
    public void updateZoomPressed(boolean pressed) {
        boolean next = isEnabled() && pressed;
        if (next && !zoomPressed) {
            state.beginZoom();
        }
        zoomPressed = next;
    }

    public void zoomIn() {
        state.adjust(0.5);
    }

    public void zoomOut() {
        state.adjust(-0.5);
    }

    public void resetZoomLevel() {
        state.beginZoom();
    }

    public float getFovFactor() {
        boolean pressed = isZoomPressed();
        float target = state.getFovFactor(pressed);
        currentFovFactor += (target - currentFovFactor) * 0.35F;
        return currentFovFactor;
    }
}
