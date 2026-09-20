package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared, fully user-configurable state for every EzClient HUD module. */
public abstract class HudModule extends Module {
    private static long renderFrameTimeMillis;
    public enum ColorMode {
        SOLID,
        WAVE,
        RAINBOW
    }

    public enum BorderStyle {
        PIXEL("ezclient.border_style.pixel"),
        VANILLA_PREMIUM("ezclient.border_style.vanilla_premium"),
        GLASS_NEON("ezclient.border_style.glass_neon"),
        CYBER_BRACKET("ezclient.border_style.cyber_bracket"),
        DARK_RPG("ezclient.border_style.dark_rpg"),
        MINIMAL_PRO("ezclient.border_style.minimal_pro");

        private final String translationKey;
        BorderStyle(String translationKey) { this.translationKey = translationKey; }
        public String getLabel() { return app.ezclient.util.EzI18n.get(translationKey); }
    }

    public enum AnchorX {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum AnchorY {
        TOP,
        CENTER,
        BOTTOM
    }

    public static final int CONTENT_PADDING_X = 6;
    public static final int CONTENT_PADDING_Y = 4;

    private int x;
    private int y;
    private double scale = 1.0;
    private AnchorX anchorX = AnchorX.LEFT;
    private AnchorY anchorY = AnchorY.TOP;
    private int editorWidth = 0;
    private int editorHeight = 0;
    private String prefix;
    private String suffix;
    private ColorMode colorMode = ColorMode.SOLID;
    private boolean background = true;
    private int textColor = 0xFFFFFFFF;     // Color 1 (Solid / Wave Primary)
    private int waveColor2 = 0xFF22C96E;    // Color 2 (Wave Secondary)
    private int backgroundColor = 0x80000000;
    private int borderColor = 0xFF22C96E;
    private boolean border = false;
    private BorderStyle borderStyle = BorderStyle.PIXEL;
    private ColorMode borderColorMode = ColorMode.SOLID;
    private int borderWaveColor2 = 0xFF00D2FF;

    // ── Badlion Systemwide Standard Styling ──
    private boolean textShadow = true;
    private boolean customFont = false;
    private int cornerRadius = 3;
    private int borderWidth = 3;
    private float rainbowSpeed = 1.0f;
    private float rainbowSaturation = 0.85f;
    private boolean rainbowBorder = false;

    private final int defaultX;
    private final int defaultY;

    protected HudModule(String name, String category, boolean enabled, int x, int y, String prefix, String suffix) {
        super(name, category, enabled);
        this.defaultX = x;
        this.defaultY = y;
        this.x = x;
        this.y = y;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public void resetToDefaults() {
        setPosition(defaultX, defaultY);
        setScale(1.0);
        this.anchorX = AnchorX.LEFT;
        this.anchorY = AnchorY.TOP;
        this.editorWidth = 0;
        this.editorHeight = 0;
        resetSettings();
    }

    protected abstract String value(Minecraft client);
    public String displayText(Minecraft client) { return prefix + value(client) + suffix; }
    public String displayText(Minecraft client, boolean editor) { return displayText(client); }

    public int getWidth(Minecraft client) {
        if (client == null || client.font == null) return 40;
        int pad = (background || border) ? CONTENT_PADDING_X : 2;
        return client.font.width(displayText(client)) + pad * 2;
    }

    public int getWidth(Minecraft client, boolean editor) {
        if (client == null || client.font == null) return 40;
        int pad = (background || border) ? CONTENT_PADDING_X : 2;
        return client.font.width(displayText(client, editor)) + pad * 2;
    }

    public int getHeight(Minecraft client) {
        int pad = (background || border) ? CONTENT_PADDING_Y : 1;
        return 9 + pad * 2;
    }

    /**
     * Editor-aware counterpart to {@link #getHeight(Minecraft)}. Custom HUDs whose
     * preview contains a different number of rows override this method so rendering,
     * selection, dragging and snapping all use the same geometry.
     */
    public int getHeight(Minecraft client, boolean editor) {
        return getHeight(client);
    }

    /**
     * Editor hit testing in unscaled module-local coordinates. Most HUD modules
     * are rectangular; irregular modules can override this to avoid clickable
     * empty space inside their bounding box.
     */
    public boolean containsEditorPoint(Minecraft client, double localX, double localY) {
        return localX >= 0.0 && localX < getWidth(client, true)
                && localY >= 0.0 && localY < getHeight(client, true);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public double getScale() { return scale; }
    public String getPrefix() { return prefix; }
    public String getSuffix() { return suffix; }

    public ColorMode getColorMode() { return colorMode; }
    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode == null ? ColorMode.SOLID : colorMode;
        ConfigManager.save();
    }

    public boolean isRainbow() { return colorMode == ColorMode.RAINBOW; }
    public void setRainbow(boolean rainbow) {
        setColorMode(rainbow ? ColorMode.RAINBOW : ColorMode.SOLID);
    }

    public boolean hasBackground() { return background; }
    public int getTextColor() { return textColor; }
    public int getWaveColor2() { return waveColor2; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
    public boolean hasBorder() { return border; }
    public BorderStyle getBorderStyle() { return borderStyle == null ? BorderStyle.PIXEL : borderStyle; }
    public void setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle == null ? BorderStyle.PIXEL : borderStyle;
        ConfigManager.save();
    }

    public boolean isTextShadow() { return textShadow; }
    public void setTextShadow(boolean textShadow) { this.textShadow = textShadow; ConfigManager.save(); }

    public boolean isCustomFont() { return customFont; }
    public void setCustomFont(boolean customFont) { this.customFont = customFont; ConfigManager.save(); }

    public int getCornerRadius() { return cornerRadius; }
    public void setCornerRadius(int cornerRadius) { this.cornerRadius = Math.max(0, Math.min(6, cornerRadius)); ConfigManager.save(); }

    public int getBorderWidth() { return borderWidth; }
    public void setBorderWidth(int borderWidth) { this.borderWidth = Math.max(2, Math.min(4, borderWidth)); ConfigManager.save(); }

    public float getRainbowSpeed() { return rainbowSpeed; }
    public void setRainbowSpeed(float rainbowSpeed) { this.rainbowSpeed = Math.max(0.2f, Math.min(5.0f, rainbowSpeed)); ConfigManager.save(); }

    public float getRainbowSaturation() { return rainbowSaturation; }
    public void setRainbowSaturation(float rainbowSaturation) { this.rainbowSaturation = Math.max(0.0f, Math.min(1.0f, rainbowSaturation)); ConfigManager.save(); }

    public boolean isRainbowBorder() { return rainbowBorder; }
    public void setRainbowBorder(boolean rainbowBorder) { this.rainbowBorder = rainbowBorder; ConfigManager.save(); }

    public void setPosition(int x, int y) { this.x = Math.max(0, x); this.y = Math.max(0, y); ConfigManager.save(); }
    public void setX(int x) { this.x = x; ConfigManager.save(); }
    public void setY(int y) { this.y = y; ConfigManager.save(); }
    public void setScale(double scale) { this.scale = Double.isFinite(scale) ? Math.max(0.5, Math.min(2.0, scale)) : 1.0; ConfigManager.save(); }
    public void setPrefix(String prefix) { this.prefix = prefix == null ? "" : prefix; ConfigManager.save(); }
    public void setSuffix(String suffix) { this.suffix = suffix == null ? "" : suffix; ConfigManager.save(); }
    public void setBackground(boolean background) { this.background = background; ConfigManager.save(); }
    public void setTextColor(int color) { this.textColor = color; ConfigManager.save(); }
    public void setWaveColor2(int color) { this.waveColor2 = color; ConfigManager.save(); }
    public void setBackgroundColor(int color) { this.backgroundColor = color; ConfigManager.save(); }
    public void setBorderColor(int color) { this.borderColor = color; ConfigManager.save(); }
    public void setBorder(boolean border) { this.border = border; ConfigManager.save(); }
    public ColorMode getBorderColorMode() { return borderColorMode == null ? ColorMode.SOLID : borderColorMode; }
    public void setBorderColorMode(ColorMode mode) { this.borderColorMode = mode == null ? ColorMode.SOLID : mode; ConfigManager.save(); }
    public int getBorderWaveColor2() { return borderWaveColor2; }
    public void setBorderWaveColor2(int color) { this.borderWaveColor2 = color; ConfigManager.save(); }
    public AnchorX getAnchorX() { return anchorX != null ? anchorX : AnchorX.LEFT; }
    public void setAnchorX(AnchorX anchorX) { this.anchorX = anchorX != null ? anchorX : AnchorX.LEFT; ConfigManager.save(); }

    public AnchorY getAnchorY() { return anchorY != null ? anchorY : AnchorY.TOP; }
    public void setAnchorY(AnchorY anchorY) { this.anchorY = anchorY != null ? anchorY : AnchorY.TOP; ConfigManager.save(); }

    public int getEditorWidth() { return editorWidth; }
    public void setEditorWidth(int editorWidth) { this.editorWidth = editorWidth; }

    public int getEditorHeight() { return editorHeight; }
    public void setEditorHeight(int editorHeight) { this.editorHeight = editorHeight; }

    public void updateAnchor(int screenWidth, int screenHeight, int editorW, int editorH) {
        this.editorWidth = Math.max(1, editorW);
        this.editorHeight = Math.max(1, editorH);

        double s = Double.isFinite(this.scale) && this.scale > 0 ? this.scale : 1.0;
        int scaledW = (int) Math.ceil(this.editorWidth * s);
        int scaledH = (int) Math.ceil(this.editorHeight * s);

        int distLeft = this.x;
        int distRight = screenWidth - (this.x + scaledW);
        int distTop = this.y;
        int distBottom = screenHeight - (this.y + scaledH);

        int thresholdX = Math.max(40, screenWidth / 5);
        int thresholdY = Math.max(30, screenHeight / 5);

        if (distLeft <= thresholdX && distLeft <= distRight) {
            this.anchorX = AnchorX.LEFT;
        } else if (distRight <= thresholdX) {
            this.anchorX = AnchorX.RIGHT;
        } else {
            this.anchorX = AnchorX.CENTER;
        }

        if (distTop <= thresholdY && distTop <= distBottom) {
            this.anchorY = AnchorY.TOP;
        } else if (distBottom <= thresholdY) {
            this.anchorY = AnchorY.BOTTOM;
        } else {
            this.anchorY = AnchorY.CENTER;
        }
    }

    public int getRenderX(Minecraft client, int currentWidth, boolean editor) {
        if (editor) return this.x;
        if (this.editorWidth <= 0 && client != null && client.getWindow() != null) {
            updateAnchor(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(),
                    getWidth(client, true), getHeight(client, true));
        }
        int refW = this.editorWidth > 0 ? this.editorWidth : getWidth(client, true);
        double s = Double.isFinite(this.scale) && this.scale > 0 ? this.scale : 1.0;
        double rx;
        switch (getAnchorX()) {
            case RIGHT -> rx = this.x + (refW - currentWidth) * s;
            case CENTER -> rx = this.x + ((refW - currentWidth) * s) / 2.0;
            case LEFT -> rx = this.x;
            default -> rx = this.x;
        }
        if (client != null && client.getWindow() != null) {
            int screenW = client.getWindow().getGuiScaledWidth();
            int maxRx = Math.max(0, screenW - (int) Math.ceil(currentWidth * s));
            return Math.max(0, Math.min(maxRx, (int) Math.round(rx)));
        }
        return (int) Math.round(rx);
    }

    public int getRenderY(Minecraft client, int currentHeight, boolean editor) {
        if (editor) return this.y;
        if (this.editorHeight <= 0 && client != null && client.getWindow() != null) {
            updateAnchor(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(),
                    getWidth(client, true), getHeight(client, true));
        }
        int refH = this.editorHeight > 0 ? this.editorHeight : getHeight(client, true);
        double s = Double.isFinite(this.scale) && this.scale > 0 ? this.scale : 1.0;
        double ry;
        switch (getAnchorY()) {
            case BOTTOM -> ry = this.y + (refH - currentHeight) * s;
            case CENTER -> ry = this.y + ((refH - currentHeight) * s) / 2.0;
            case TOP -> ry = this.y;
            default -> ry = this.y;
        }
        if (client != null && client.getWindow() != null) {
            int screenH = client.getWindow().getGuiScaledHeight();
            int maxRy = Math.max(0, screenH - (int) Math.ceil(currentHeight * s));
            return Math.max(0, Math.min(maxRy, (int) Math.round(ry)));
        }
        return (int) Math.round(ry);
    }

    public int color() {
        return color(0);
    }

    public int color(long offsetMs) {
        long now = renderFrameTimeMillis != 0L ? renderFrameTimeMillis : System.currentTimeMillis();
        if (colorMode == ColorMode.RAINBOW) {
            long period = (long) (4000L / Math.max(0.1f, rainbowSpeed));
            float hue = ((now + offsetMs) % period) / (float) period;
            return (textColor & 0xFF000000) | (java.awt.Color.HSBtoRGB(hue, rainbowSaturation, 1.0f) & 0xFFFFFF);
        } else if (colorMode == ColorMode.WAVE) {
            double time = ((now + offsetMs) % 3000L) / 3000.0;
            float factor = (float) ((Math.sin(time * Math.PI * 2.0) + 1.0) / 2.0);
            return interpolateColor(textColor, waveColor2, factor);
        }
        return textColor;
    }

    public static void beginRenderFrame(long nowMillis) {
        renderFrameTimeMillis = nowMillis;
    }

    protected static long renderFrameTimeMillis() {
        return renderFrameTimeMillis != 0L ? renderFrameTimeMillis : System.currentTimeMillis();
    }

    public int currentBorderColor() {
        return currentBorderColor(0);
    }

    public int currentBorderColor(long offsetMs) {
        long now = renderFrameTimeMillis != 0L ? renderFrameTimeMillis : System.currentTimeMillis();
        if (borderColorMode == ColorMode.RAINBOW || rainbowBorder) {
            long period = (long) (4000L / Math.max(0.1f, rainbowSpeed));
            float hue = ((now + offsetMs) % period) / (float) period;
            return (borderColor & 0xFF000000) | (java.awt.Color.HSBtoRGB(hue, rainbowSaturation, 1.0f) & 0xFFFFFF);
        } else if (borderColorMode == ColorMode.WAVE) {
            double time = ((now + offsetMs) % 3000L) / 3000.0;
            float factor = (float) ((Math.sin(time * Math.PI * 2.0) + 1.0) / 2.0);
            return interpolateColor(borderColor, borderWaveColor2, factor);
        }
        return borderColor;
    }

    public void renderBackgroundAndBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        renderBackgroundAndBorder(graphics, x, y, w, h, currentBorderColor());
    }

    /** Draws the module background and authentic Minecraft 3D frames. */
    protected void renderBackgroundAndBorder(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int accentColor) {
        if (w <= 0 || h <= 0) return;
        if (background) {
            renderRoundedBox(graphics, x, y, w, h, cornerRadius, backgroundColor);
            if (border && borderStyle == BorderStyle.GLASS_NEON) {
                int sheenH = Math.max(2, Math.min(h / 3, 6));
                graphics.fill(x + 1, y + 1, x + w - 1, y + 1 + sheenH, 0x1AFFFFFF);
            }
            if (border && borderStyle == BorderStyle.DARK_RPG && w > 8 && h > 8) {
                renderRoundedOutline(graphics, x + 3, y + 3, w - 6, h - 6,
                        Math.max(0, cornerRadius - 1), 1, 0x55000000);
            }
        }
        if (border) {
            renderMinecraftFrame(graphics, x, y, w, h, borderStyle, accentColor, borderWidth);
        }
    }

    public static void renderMinecraftFrame(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                           BorderStyle style, int accentColor, int borderWidth) {
        if (w < 8 || h < 8) return;
        BorderStyle resolved = style == null ? BorderStyle.PIXEL : style;
        int t = Math.max(1, Math.min(3, borderWidth - 1));

        switch (resolved) {
            case PIXEL -> {
                // Sharp, stepped pixels inspired by the classic Minecraft GUI.
                int outer = 0xFF080A0D;
                int highlight = lightenColor(accentColor, 1.28f);
                int shadow = darkenColor(accentColor, 0.52f);
                drawSteppedOutline(graphics, x, y, w, h, outer);
                graphics.fill(x + 2, y + 1, x + w - 2, y + 2, highlight);
                graphics.fill(x + 1, y + 2, x + 2, y + h - 2, highlight);
                graphics.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, shadow);
                graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, shadow);
            }
            case VANILLA_PREMIUM -> {
                // Hotbar/inventory depth: dark silhouette, raised metal and inner groove.
                int outer = 0xFF090A0C;
                int metal = interpolateColor(0xFF72767C, accentColor, 0.22f);
                int highlight = lightenColor(metal, 1.32f);
                int shadow = darkenColor(metal, 0.42f);
                drawSteppedOutline(graphics, x, y, w, h, outer);
                graphics.fill(x + 2, y + 1, x + w - 2, y + 1 + t, highlight);
                graphics.fill(x + 1, y + 2, x + 1 + t, y + h - 2, highlight);
                graphics.fill(x + 2, y + h - 1 - t, x + w - 2, y + h - 1, shadow);
                graphics.fill(x + w - 1 - t, y + 2, x + w - 1, y + h - 2, shadow);
                if (w > 10 && h > 10) {
                    renderRoundedOutline(graphics, x + t + 1, y + t + 1, w - (t + 1) * 2,
                            h - (t + 1) * 2, 1, 1, 0x66000000);
                }
            }
            case GLASS_NEON -> {
                // Three restrained halo layers and a crisp cyan/PvP-client core.
                renderRoundedOutline(graphics, x, y, w, h, 4, 1, withAlpha(accentColor, 0x3A));
                if (w > 4 && h > 4) {
                    renderRoundedOutline(graphics, x + 1, y + 1, w - 2, h - 2, 3, 1,
                            withAlpha(accentColor, 0x78));
                    renderRoundedOutline(graphics, x + 2, y + 2, w - 4, h - 4, 2, 1,
                            withAlpha(lightenColor(accentColor, 1.35f), 0xFF));
                }
                int bracket = Math.max(4, Math.min(8, Math.min(w, h) / 3));
                drawCornerBrackets(graphics, x, y, w, h, bracket, 2, accentColor);
            }
            case CYBER_BRACKET -> {
                // Deliberately open sides: only competitive corner brackets remain.
                int bracket = Math.max(5, Math.min(10, Math.min(w, h) / 2));
                drawCornerBrackets(graphics, x, y, w, h, bracket, t, accentColor);
                drawCornerBrackets(graphics, x + 1, y + 1, w - 2, h - 2,
                        Math.max(3, bracket - 2), 1, withAlpha(lightenColor(accentColor, 1.35f), 0xAA));
            }
            case DARK_RPG -> {
                // Layered dark steel with recessed channel and gem-like corner studs.
                int outer = 0xFF08090B;
                int steel = interpolateColor(0xFF454A52, accentColor, 0.16f);
                int highlight = lightenColor(steel, 1.42f);
                int shadow = darkenColor(steel, 0.32f);
                drawSteppedOutline(graphics, x, y, w, h, outer);
                graphics.fill(x + 3, y + 1, x + w - 3, y + 3, highlight);
                graphics.fill(x + 1, y + 3, x + 3, y + h - 3, darkenColor(steel, 0.72f));
                graphics.fill(x + 3, y + h - 3, x + w - 3, y + h - 1, shadow);
                graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 3, shadow);
                if (w > 12 && h > 12) {
                    renderRoundedOutline(graphics, x + 4, y + 4, w - 8, h - 8, 1, 1, 0xAA090A0C);
                }
                drawStud(graphics, x + 1, y + 1, accentColor);
                drawStud(graphics, x + w - 4, y + 1, accentColor);
                drawStud(graphics, x + 1, y + h - 4, accentColor);
                drawStud(graphics, x + w - 4, y + h - 4, accentColor);
            }
            case MINIMAL_PRO -> {
                // One clean outline, a faint depth line, and short side anchors.
                int line = withAlpha(lightenColor(accentColor, 1.18f), 0xE8);
                renderRoundedOutline(graphics, x + 1, y + 1, w - 2, h - 2, 2, 1, line);
                graphics.fill(x + 3, y + h - 1, x + w - 3, y + h, 0x55000000);
                int anchor = Math.max(3, Math.min(7, h / 3));
                int center = y + h / 2;
                graphics.fill(x, center - anchor, x + 1, center + anchor, withAlpha(accentColor, 0xB8));
                graphics.fill(x + w - 1, center - anchor, x + w, center + anchor, withAlpha(accentColor, 0xB8));
            }
        }
    }

    private static void drawSteppedOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x + 1, y, x + w - 1, y + 1, color);
        graphics.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void drawCornerBrackets(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                           int length, int thickness, int color) {
        if (w < 4 || h < 4) return;
        int l = Math.min(length, Math.min(w / 2, h / 2));
        int t = Math.max(1, Math.min(thickness, 3));
        graphics.fill(x, y, x + l, y + t, color);
        graphics.fill(x, y, x + t, y + l, color);
        graphics.fill(x + w - l, y, x + w, y + t, color);
        graphics.fill(x + w - t, y, x + w, y + l, color);
        graphics.fill(x, y + h - t, x + l, y + h, color);
        graphics.fill(x, y + h - l, x + t, y + h, color);
        graphics.fill(x + w - l, y + h - t, x + w, y + h, color);
        graphics.fill(x + w - t, y + h - l, x + w, y + h, color);
    }

    private static void drawStud(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 3, y + 3, 0xFF090A0C);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, lightenColor(color, 1.35f));
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    public static int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.max(0, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.max(0, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.max(0, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void renderRoundedOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                                             int radius, int thickness, int color) {
        if (w <= 0 || h <= 0 || thickness <= 0) return;
        int r = Math.min(Math.max(0, radius), Math.min(w, h) / 2);
        int t = Math.min(thickness, Math.max(1, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int outer = roundedInset(row, h, r);
            if (row < t || row >= h - t) {
                graphics.fill(x + outer, y + row, x + w - outer, y + row + 1, color);
            } else {
                int innerHeight = h - t * 2;
                int innerRadius = Math.max(0, r - t);
                int inner = t + roundedInset(row - t, innerHeight, innerRadius);
                graphics.fill(x + outer, y + row, x + inner, y + row + 1, color);
                graphics.fill(x + w - inner, y + row, x + w - outer, y + row + 1, color);
            }
        }
    }

    private static int roundedInset(int row, int height, int radius) {
        int edge = Math.min(row, height - 1 - row);
        return edge >= radius ? 0 : (int)Math.ceil(radius - Math.sqrt(radius * radius - Math.pow(radius - edge - .5, 2)));
    }

    public static void renderRoundedBox(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
        int r = Math.min(Math.max(0, radius), Math.min(w, h) / 2);
        if (r == 0) { graphics.fill(x, y, x + w, y + h, color); return; }
        graphics.fill(x, y + r, x + w, y + h - r, color);
        for (int row = 0; row < r; row++) {
            int inset = (int)Math.ceil(r - Math.sqrt(r * r - Math.pow(r - row - 0.5, 2)));
            graphics.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            graphics.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    public net.minecraft.network.chat.Component styledText(String text) {
        var component = net.minecraft.network.chat.Component.literal(text);
        return customFont ? component.withStyle(style -> style.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("ezclient", "smooth")))) : component;
    }

    public void resetSettings() {
        this.colorMode = ColorMode.SOLID;
        this.textColor = 0xFFFFFFFF;
        this.waveColor2 = 0xFF22C96E;
        this.background = true;
        this.border = false;
        this.borderStyle = BorderStyle.PIXEL;
        this.backgroundColor = 0xA8111419;
        this.borderColor = 0xFF22C96E;
        this.textShadow = true;
        this.customFont = false;
        this.cornerRadius = 3;
        this.borderWidth = 3;
        this.rainbowSpeed = 1.0f;
        this.rainbowSaturation = 0.85f;
        this.rainbowBorder = false;

        if (this instanceof FpsModule fps) {
            fps.setPrefix("FPS: ");
            fps.setFormatOption(FpsModule.FormatOption.LABEL_VALUE);
            fps.setUpdateIntervalMs(0);
            fps.setColorCoding(false);
            fps.setShowMinMax(false);
        } else if (this instanceof PingModule ping) {
            ping.setPrefix("Ping: ");
        } else if (this instanceof CoordinatesModule coords) {
            coords.setLayoutMode(CoordinatesModule.LayoutMode.MULTI_LINE);
            coords.setDecimalPrecision(0);
            coords.setShowBiome(false);
            coords.setShowDirection(true);
            coords.setShowNether(false);
        } else if (this instanceof KeystrokesModule ks) {
            ks.setLayoutPreset(KeystrokesModule.LayoutPreset.WASD_MOUSE_SPACE_CPS);
            ks.setSpaceStyle(KeystrokesModule.SpaceStyle.LINE);
            ks.setFadeTimeMs(150);
            ks.setNormalBoxColor(0xA8111419);
            ks.setPressedBoxColor(0x70FFFFFF);
            ks.setKeyTextColor(0xFFFFFFFF);
            ks.setPressedTextColor(0xFFFFFFFF);
        } else if (this instanceof ArmorStatusModule armor) {
            armor.setHorizontal(false);
            armor.setDurabilityMode(ArmorStatusModule.DurabilityMode.PERCENT);
            armor.setColorTiers(true);
            armor.setDamageWarning(true);
            armor.setShowItemCount(true);
            armor.setEquipmentMode(ArmorStatusModule.EquipmentMode.ALL);
        } else if (this instanceof PotionEffectModule potion) {
            potion.setDisplayStyle(PotionEffectModule.DisplayStyle.DETAILED);
            potion.setVertical(true);
            potion.setBlinkWarningSeconds(5);
            potion.setUseCustomColors(true);
        } else if (this instanceof CpsModule cps) {
            cps.setDisplayMode(CpsModule.DisplayMode.COMBINED);
            cps.setDynamicColor(true);
            cps.setShowHistoryGraph(false);
        } else if (this instanceof CrosshairModule crosshair) {
            crosshair.setCrosshairType(CrosshairModule.CrosshairType.CLASSIC_CROSS);
            crosshair.setGap(3);
            crosshair.setSize(5);
            crosshair.setVerticalSize(5);
            crosshair.setThickness(1);
            crosshair.setDotSize(2);
            crosshair.setOpacity(100);
            crosshair.setShowDot(false);
            crosshair.setShowOutline(true);
            crosshair.setDynamicSpread(false);
            crosshair.setMovementSpread(true);
            crosshair.setJumpSpread(true);
            crosshair.setCooldownSpread(true);
            crosshair.setTargetMode(CrosshairModule.TargetMode.ALL);
            crosshair.setTargetEntityColor(0xFFFF3333);
            crosshair.setTargetPlayerColor(0xFF38BDF8);
            crosshair.setTargetHostileColor(0xFFFF3333);
            crosshair.setTargetNeutralColor(0xFFFFB020);
            crosshair.setTargetBlockColor(0xFFFACC15);
            crosshair.setTargetEntityScale(1.05f);
            crosshair.setTargetPlayerScale(1.15f);
            crosshair.setTargetHostileScale(1.20f);
            crosshair.setTargetNeutralScale(1.10f);
        }
        ConfigManager.save();
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public boolean hasHud() {
        return true;
    }

    @Override
    public boolean hasPreview() {
        return true;
    }

    public static int interpolateColor(int c1, int c2, float factor) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * factor);
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
