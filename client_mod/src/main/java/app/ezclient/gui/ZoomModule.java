package app.ezclient.gui;

public final class ZoomModule extends Module {
    private double zoomLevel = 4.0;
    private double minZoom = 1.0;
    private double maxZoom = 15.0;
    private double scrollSensitivity = 0.5;
    private boolean smoothZoom = true;

    public ZoomModule() {
        super("Zoom", "Render", true);
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(double zoomLevel) {
        this.zoomLevel = Math.max(minZoom, Math.min(maxZoom, zoomLevel));
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
    }

    public boolean isSmoothZoom() { return smoothZoom; }
    public void setSmoothZoom(boolean value) { smoothZoom = value; }

    @Override
    public boolean hasSettings() {
        return true;
    }
}
