package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.v1_16_v1_20.gui.RenderUtils;

public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private boolean enabled;
    private int posX;
    private int posY;
    private final boolean hasHudElement;
    private final boolean optionalHud;
    private boolean showHud = true;
    private int colorMode = 0; // 0=Default, 1=Rainbow, 2=White, 3=Green, 4=Cyan, 5=Yellow, 6=Red, 7=Purple
    private boolean showBackground = true;

    private static final String[] COLOR_NAMES = {"Default", "Rainbow", "White", "Green", "Cyan", "Yellow", "Red", "Purple"};

    public Module(String id, String name, String description, String category, boolean enabled, int posX, int posY, boolean hasHudElement, boolean optionalHud) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = enabled;
        this.posX = posX;
        this.posY = posY;
        this.hasHudElement = hasHudElement;
        this.optionalHud = optionalHud;
    }

    public Module(String id, String name, String description, String category, boolean enabled, int posX, int posY, boolean hasHudElement) {
        this(id, name, description, category, enabled, posX, posY, hasHudElement, false);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void toggle() { this.enabled = !this.enabled; }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }
    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }

    public boolean hasHudElement() { return hasHudElement; }
    public boolean hasOptionalHud() { return optionalHud; }
    public boolean isShowHud() {
        if (!hasHudElement) return false;
        if (!optionalHud) return enabled;
        return showHud;
    }
    public void setShowHud(boolean showHud) { this.showHud = showHud; }
    public void toggleShowHud() {
        if (optionalHud) {
            this.showHud = !this.showHud;
        }
    }

    public int getColorMode() { return colorMode; }
    public void setColorMode(int colorMode) { this.colorMode = (colorMode % 8 + 8) % 8; }
    public void cycleColor() { this.colorMode = (this.colorMode + 1) % 8; }

    public String getColorName() {
        if (colorMode >= 0 && colorMode < COLOR_NAMES.length) {
            return COLOR_NAMES[colorMode];
        }
        return "Custom";
    }

    public int getTextColor(int defaultColor) {
        switch (colorMode) {
            case 1: return RenderUtils.getRainbow(4.0F, 0); // Rainbow / Chroma
            case 2: return 0xFFFFFFFF; // White
            case 3: return 0xFF55FF55; // Green
            case 4: return 0xFF55FFFF; // Cyan
            case 5: return 0xFFFFFF55; // Yellow
            case 6: return 0xFFFF5555; // Red
            case 7: return 0xFFFF55FF; // Purple
            case 0:
            default:
                return defaultColor;
        }
    }

    public boolean isShowBackground() { return showBackground; }
    public void setShowBackground(boolean showBackground) { this.showBackground = showBackground; }
    public void toggleShowBackground() { this.showBackground = !this.showBackground; }

    public int getWidth() { return 60; }
    public int getHeight() { return 14; }

    public void onTick() {}
    public void renderHud(float tickDelta) {}

    public void renderEditorPreview(float tickDelta) {
        renderHud(tickDelta);
    }
}
