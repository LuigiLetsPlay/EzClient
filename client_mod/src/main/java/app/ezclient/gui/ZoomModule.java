package app.ezclient.gui;

import app.ezclient.shared.ZoomState;
import net.minecraft.resources.Identifier;

/**
 * OptiFine-Style Smooth Camera Zoom with dynamic mouse sensitivity dampening,
 * mouse scroll magnification, and cinematic smoothing options.
 */
public final class ZoomModule extends Module {
    private final ZoomState state = new ZoomState(4.0, 1.5, 15.0);
    private double scrollSensitivity = 0.5;
    private boolean smoothZoom = true;
    private boolean mouseSensitivityScaling = true;
    private boolean cinematicCamera = false;

    public ZoomModule() {
        super("Zoom", "RENDER", true);
        setKeyBind(org.lwjgl.glfw.GLFW.GLFW_KEY_C);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/zoom.png");
    }

    public double getZoomLevel() { return state.getConfiguredZoom(); }
    public void setZoomLevel(double zoomLevel) {
        state.setConfiguredZoom(zoomLevel);
        ConfigManager.save();
    }

    public double getActiveZoomLevel() { return state.getActiveZoom(); }

    public void adjustScrollZoom(double delta) {
        state.adjust(delta);
    }

    public void resetToDefault() {
        state.beginZoom();
    }

    public double getMinZoom() { return state.getMinZoom(); }
    public void setMinZoom(double value) {
        state.setMinZoom(value);
        ConfigManager.save();
    }

    public double getMaxZoom() { return state.getMaxZoom(); }
    public void setMaxZoom(double value) {
        state.setMaxZoom(value);
        ConfigManager.save();
    }

    public double getScrollSensitivity() { return scrollSensitivity; }
    public void setScrollSensitivity(double value) {
        scrollSensitivity = Math.max(0.1, Math.min(2.0, value));
        ConfigManager.save();
    }

    public boolean isSmoothZoom() { return smoothZoom; }
    public void setSmoothZoom(boolean value) {
        smoothZoom = value;
        ConfigManager.save();
    }

    public boolean isMouseSensitivityScaling() { return mouseSensitivityScaling; }
    public void setMouseSensitivityScaling(boolean val) {
        this.mouseSensitivityScaling = val;
        ConfigManager.save();
    }

    public boolean isCinematicCamera() { return cinematicCamera; }
    public void setCinematicCamera(boolean val) {
        this.cinematicCamera = val;
        ConfigManager.save();
    }

    @Override
    public boolean hasSettings() {
        return true;
    }
}
