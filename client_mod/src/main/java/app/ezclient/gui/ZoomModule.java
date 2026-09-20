package app.ezclient.gui;

import app.ezclient.shared.ZoomState;
import net.minecraft.resources.Identifier;

/**
 * OptiFine-Style Smooth Camera Zoom with dynamic mouse sensitivity dampening,
 * mouse scroll magnification, and cinematic smoothing options.
 */
public final class ZoomModule extends FeatureModule {
    private final ZoomState state = new ZoomState(4.0, 1.5, 15.0);

    public ZoomModule() {
        super("Zoom", false, 0);
        setKeyBind(org.lwjgl.glfw.GLFW.GLFW_KEY_C);

        option("Zoom-Faktor", "zoomLevel", "Standard-Zoom", "Legt die standardmäßige Vergrößerung beim Drücken der Zoom-Taste fest.", 4.0, 1.0, 30.0);
        option("Zoom-Faktor", "minZoom", "Minimaler Zoom", "Die minimale Vergrößerungsstufe beim Scrollen mit dem Mausrad.", 1.5, 1.0, 10.0);
        option("Zoom-Faktor", "maxZoom", "Maximaler Zoom", "Die maximale Vergrößerungsstufe beim Scrollen mit dem Mausrad.", 15.0, 2.0, 30.0);
        option("Steuerung", "scrollSensitivity", "Mausrad-Schrittweite", "Bestimmt, wie schnell die Vergrößerung mit dem Mausrad angepasst wird.", 0.5, 0.1, 2.0);
        flag("Kamera & Glättung", "smoothZoom", "Sanfter Zoom", "Aktiviert eine weiche, cineastische Kamerafahrt beim Ein- und Auszoomen.", true);
        flag("Kamera & Glättung", "mouseSensitivityScaling", "Maus-Dämpfung", "Reduziert die Maus-Empfindlichkeit proportional zum aktuellen Zoom.", true);
        flag("Kamera & Glättung", "cinematicCamera", "Cineastische Kamera", "Aktiviert Minecrafts träge, cineastische Kamerabewegung während des Zooms.", false);
        syncState();
    }

    private void syncState() {
        state.setConfiguredZoom(getZoomLevel());
        state.setMinZoom(getMinZoom());
        state.setMaxZoom(getMaxZoom());
    }

    @Override
    public boolean set(Option option, Object value) {
        boolean ok = super.set(option, value);
        if (ok) {
            syncState();
        }
        return ok;
    }

    @Override
    public void loadFeature(com.google.gson.JsonObject json) {
        super.loadFeature(json);
        syncState();
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/zoom.png");
    }

    @Override
    public void setKeyBind(int keyBind) {
        super.setKeyBind(keyBind);
        EzKeyBindings.setKeyCode(EzKeyBindings.KEY_ZOOM, keyBind);
    }

    @Override
    public String getDescription() {
        return "Vergrößert die Sicht mit sanftem Zoom, Mausrad-Steuerung und optional angepasster Maus-Empfindlichkeit.";
    }

    public double getZoomLevel() { return number("zoomLevel"); }
    public void setZoomLevel(double zoomLevel) {
        set("zoomLevel", zoomLevel);
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

    public double getMinZoom() { return number("minZoom"); }
    public void setMinZoom(double value) {
        set("minZoom", value);
        state.setMinZoom(value);
        ConfigManager.save();
    }

    public double getMaxZoom() { return number("maxZoom"); }
    public void setMaxZoom(double value) {
        set("maxZoom", value);
        state.setMaxZoom(value);
        ConfigManager.save();
    }

    public double getScrollSensitivity() { return number("scrollSensitivity"); }
    public void setScrollSensitivity(double value) {
        set("scrollSensitivity", Math.max(0.1, Math.min(2.0, value)));
        ConfigManager.save();
    }

    public boolean isSmoothZoom() { return flag("smoothZoom"); }
    public void setSmoothZoom(boolean value) {
        set("smoothZoom", value);
        ConfigManager.save();
    }

    public boolean isMouseSensitivityScaling() { return flag("mouseSensitivityScaling"); }
    public void setMouseSensitivityScaling(boolean val) {
        set("mouseSensitivityScaling", val);
        ConfigManager.save();
    }

    public boolean isCinematicCamera() { return flag("cinematicCamera"); }
    public void setCinematicCamera(boolean val) {
        set("cinematicCamera", val);
        ConfigManager.save();
    }

    @Override
    public boolean hasSettings() {
        return true;
    }
}
