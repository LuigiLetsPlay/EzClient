package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.shared.ZoomState;

public class ZoomModule extends Module {
    private boolean zooming = false;
    private final ZoomState state = new ZoomState(4.0, 1.5, 15.0);

    public ZoomModule() {
        super("zoom", "OptiFine Zoom", "Smooth camera zoom when holding C key with mouse wheel scroll", "Render", true, 0, 0, false);
    }

    public boolean isZooming() { return zooming; }
    public void setZooming(boolean zooming) {
        if (zooming && !this.zooming) {
            state.beginZoom();
        }
        this.zooming = zooming;
    }

    public float getZoomLevel() { return (float) state.getConfiguredZoom(); }
    public void setZoomLevel(float zoomLevel) { state.setConfiguredZoom(zoomLevel); }

    public void adjustZoom(float delta) {
        if (zooming) {
            state.adjust(delta);
        }
    }

    public float getFovFactor() {
        return state.getFovFactor(zooming);
    }
}
