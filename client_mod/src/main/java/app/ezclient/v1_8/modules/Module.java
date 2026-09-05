package app.ezclient.v1_8.modules;

import app.ezclient.v1_8.gui.RenderUtils;

public class Module {
    public static final String[] COLOR_NAMES = {
        "Default", "White", "Rainbow", "Green", "Cyan", "Yellow", "Red", "Purple"
    };

    private final String id;
    private final String name;
    private final String description;
    private final String category; // "HUD", "Movement", "Render"
    private final boolean hasHudElement;
    private final boolean optionalHud;

    private boolean enabled;
    private final boolean defaultEnabled;
    private boolean showHud = true;
    private int colorMode = 0; // 0=Default, 1=White, 2=Rainbow, 3=Green, 4=Cyan, 5=Yellow, 6=Red, 7=Purple
    private boolean showBackground = true;

    private int posX;
    private int posY;
    private final int defaultX;
    private final int defaultY;

    public Module(String id, String name, String description, String category, boolean defaultEnabled, int defaultX, int defaultY, boolean hasHudElement, boolean optionalHud) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultEnabled;
        this.defaultEnabled = defaultEnabled;
        this.posX = defaultX;
        this.posY = defaultY;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.hasHudElement = hasHudElement;
        this.optionalHud = optionalHud;
    }

    public Module(String id, String name, String description, String category, boolean defaultEnabled, int defaultX, int defaultY, boolean hasHudElement) {
        this(id, name, description, category, defaultEnabled, defaultX, defaultY, hasHudElement, false);
    }

    public Module(String id, String name, String description, String category, boolean defaultEnabled, int defaultX, int defaultY) {
        this(id, name, description, category, defaultEnabled, defaultX, defaultY, true, false);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public boolean hasHudElement() {
        return hasHudElement;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        onToggle(enabled);
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public boolean hasOptionalHud() {
        return optionalHud;
    }

    public boolean isShowHud() {
        if (!hasHudElement) return false;
        if (!optionalHud) return enabled;
        return showHud;
    }

    public void setShowHud(boolean showHud) {
        this.showHud = showHud;
    }

    public void toggleShowHud() {
        if (optionalHud) {
            this.showHud = !this.showHud;
        }
    }

    public int getColorMode() {
        return colorMode;
    }

    public void setColorMode(int colorMode) {
        this.colorMode = (colorMode % COLOR_NAMES.length + COLOR_NAMES.length) % COLOR_NAMES.length;
    }

    public void cycleColor() {
        setColorMode(colorMode + 1);
    }

    public String getColorName() {
        return COLOR_NAMES[colorMode];
    }

    public int getTextColor(int defaultColor) {
        switch (colorMode) {
            case 1: return 0xFFFFFFFF; // White
            case 2: return RenderUtils.getRainbow(4.0F, (posX + posY) * 4); // Rainbow / Chroma
            case 3: return 0xFF55FF55; // Green
            case 4: return 0xFF55FFFF; // Cyan
            case 5: return 0xFFFFFF55; // Yellow
            case 6: return 0xFFFF5555; // Red
            case 7: return 0xFFAA00AA; // Purple
            default: return defaultColor;
        }
    }

    public boolean isShowBackground() {
        return showBackground;
    }

    public void setShowBackground(boolean showBackground) {
        this.showBackground = showBackground;
    }

    public void toggleShowBackground() {
        this.showBackground = !this.showBackground;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getWidth() {
        return 60;
    }

    public int getHeight() {
        return 14;
    }

    public void resetPosition() {
        this.posX = defaultX;
        this.posY = defaultY;
    }

    public void resetToDefault() {
        this.enabled = defaultEnabled;
        this.showHud = true;
        this.colorMode = 0;
        this.showBackground = true;
        resetPosition();
    }

    public void onTick() {}

    public void onToggle(boolean newState) {}

    public void renderHud(float tickDelta) {}

    public void renderEditorPreview(float tickDelta) {
        renderHud(tickDelta);
    }
}
