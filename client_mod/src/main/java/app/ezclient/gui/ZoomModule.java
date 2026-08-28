package app.ezclient.gui;

import net.minecraft.resources.Identifier;

/**
 * OptiFine-Style Smooth Camera Zoom with dynamic mouse sensitivity dampening,
 * mouse scroll magnification, and cinematic smoothing options.
 */
public final class ZoomModule extends Module {
    private double zoomLevel = 4.0;
    private double currentDynamicZoom = 4.0;
    private double minZoom = 1.5;
    private double maxZoom = 15.0;
    private double scrollSensitivity = 0.5;
    private boolean smoothZoom = true;
    private boolean mouseSensitivityScaling = true;
    private boolean cinematicCamera = false;

    public ZoomModule() {
        super("Zoom", "RENDER", true);
        this.currentDynamicZoom = this.zoomLevel;
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/zoom.png");
    }

    public double getZoomLevel() { return zoomLevel; }
    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = Math.max(minZoom, Math.min(maxZoom, zoomLevel));
        this.currentDynamicZoom = this.zoomLevel;
        ConfigManager.save();
    }

    public double getActiveZoomLevel() { return currentDynamicZoom; }

    public void adjustScrollZoom(double delta) {
        this.currentDynamicZoom = Math.max(minZoom, Math.min(maxZoom, this.currentDynamicZoom + delta));
    }

    public void resetToDefault() {
        this.currentDynamicZoom = this.zoomLevel;
    }

    public double getMinZoom() { return minZoom; }
    public void setMinZoom(double value) {
        minZoom = Math.max(1.0, Math.min(value, maxZoom));
        setZoomLevel(zoomLevel);
    }

    public double getMaxZoom() { return maxZoom; }
    public void setMaxZoom(double value) {
        maxZoom = Math.max(minZoom, Math.min(30.0, value));
        setZoomLevel(zoomLevel);
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
