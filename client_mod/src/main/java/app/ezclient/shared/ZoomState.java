package app.ezclient.shared;

/** Minecraft-independent zoom state shared by every version adapter. */
public final class ZoomState {
    private double configuredZoom;
    private double activeZoom;
    private double minZoom;
    private double maxZoom;

    public ZoomState(double configuredZoom, double minZoom, double maxZoom) {
        this.minZoom = minZoom;
        this.maxZoom = Math.max(minZoom, maxZoom);
        setConfiguredZoom(configuredZoom);
    }

    public void beginZoom() {
        activeZoom = configuredZoom;
    }

    public void adjust(double delta) {
        activeZoom = clamp(activeZoom + delta);
    }

    public double getConfiguredZoom() { return configuredZoom; }
    public double getActiveZoom() { return activeZoom; }
    public double getMinZoom() { return minZoom; }
    public double getMaxZoom() { return maxZoom; }

    public void setConfiguredZoom(double value) {
        configuredZoom = clamp(value);
        activeZoom = configuredZoom;
    }

    public void setMinZoom(double value) {
        minZoom = Math.max(1.0, Math.min(value, maxZoom));
        setConfiguredZoom(configuredZoom);
    }

    public void setMaxZoom(double value) {
        maxZoom = Math.max(minZoom, Math.min(30.0, value));
        setConfiguredZoom(configuredZoom);
    }

    public float getFovFactor(boolean zooming) {
        return zooming ? (float) (1.0 / activeZoom) : 1.0F;
    }

    private double clamp(double value) {
        return Math.max(minZoom, Math.min(maxZoom, value));
    }
}
