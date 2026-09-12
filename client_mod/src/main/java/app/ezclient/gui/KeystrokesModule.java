package app.ezclient.gui;

import app.ezclient.shared.ClickRateTracker;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Keystrokes V2: a freely editable, persistent collection of key widgets. */
public final class KeystrokesModule extends HudModule {
    public enum LayoutPreset { WASD, WASD_MOUSE, WASD_MOUSE_SPACE, WASD_MOUSE_SPACE_CPS, FULL, KEYBOARD, FULL_KEYBOARD }
    public enum SpaceStyle { LINE, BLOCK, TEXT }
    public enum StylePreset { CLASSIC, MINIMAL, PVP, CUSTOM }
    public enum KeyColorMode { SOLID, RAINBOW, GRADIENT, INDIVIDUAL }
    public enum PressAnimation { INSTANT, FADE, PULSE }
    public enum FrameMode { INDIVIDUAL, CONNECTED }
    public enum Binding {
        FORWARD("Vorwärts", "W"), LEFT("Links", "A"), BACK("Rückwärts", "S"), RIGHT("Rechts", "D"),
        JUMP("Springen", "[SPACE]"), SNEAK("Schleichen", "SNEAK"), SPRINT("Sprinten", "SPRINT"),
        ATTACK("Linksklick", "LMB"), USE("Rechtsklick", "RMB"), CUSTOM("Eigene Taste", "KEY");
        private final String title;
        private final String defaultLabel;
        Binding(String title, String defaultLabel) { this.title = title; this.defaultLabel = defaultLabel; }
        public String title() { return title; }
        public String defaultLabel() { return defaultLabel; }
    }
    public enum ColorTarget {
        NORMAL_BOX("Normaler Hintergrund"), PRESSED_BOX("Gedrueckter Hintergrund"), KEY_TEXT("Tastentext"),
        PRESSED_TEXT("Gedrueckter Text"), GRADIENT("Gradient-Farbe"), BORDER("Tastenrahmen"),
        W("Taste W"), A("Taste A"), S("Taste S"), D("Taste D"), LMB("Linke Maustaste"),
        RMB("Rechte Maustaste"), SPACE("Leertaste"), SNEAK("Schleichen"), SPRINT("Sprinten");
        private final String label;
        ColorTarget(String label) { this.label = label; }
        public String label() { return label; }
    }
    private enum Key { W, A, S, D, LMB, RMB, SPACE, SNEAK, SPRINT }

    public static final class KeyElement {
        private final long id;
        private Binding binding;
        private int keyCode;
        private String label;
        private int x, y, width, height;
        private boolean showCps;
        private boolean customColors;
        private int boxColor, pressedBoxColor, textColor, pressedTextColor;

        private KeyElement(long id, Binding binding, int keyCode, String label, int x, int y, int width, int height) {
            this.id = id; this.binding = binding; this.keyCode = keyCode; this.label = label;
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
        public long id() { return id; }
        public Binding binding() { return binding; }
        public int keyCode() { return keyCode; }
        public String label() { return label; }
        public int x() { return x; }
        public int y() { return y; }
        public int width() { return width; }
        public int height() { return height; }
        public boolean showCps() { return showCps; }
        public boolean customColors() { return customColors; }
        public int boxColor() { return boxColor; }
        public int pressedBoxColor() { return pressedBoxColor; }
        public int textColor() { return textColor; }
        public int pressedTextColor() { return pressedTextColor; }
    }

    private LayoutPreset layoutPreset = LayoutPreset.WASD_MOUSE_SPACE_CPS;
    private SpaceStyle spaceStyle = SpaceStyle.LINE;
    private StylePreset stylePreset = StylePreset.CLASSIC;
    private KeyColorMode keyColorMode = KeyColorMode.SOLID;
    private PressAnimation pressAnimation = PressAnimation.FADE;
    private int fadeTimeMs = 150, keySize = 20, keySpacing = 2, keyCornerRadius = 1, boxOpacity = 100, textOpacity = 100;
    private int normalBoxColor = 0x80000000, pressedBoxColor = 0x70FFFFFF, keyTextColor = 0xFFFFFFFF, pressedTextColor = 0xFF000000;
    private int gradientColor = 0xFF22C96E, keyBorderColor = 0xFF35414D, keyBorderWidth = 2;
    private float colorCycleSpeed = 1.0f, fontScale = 1.0f;
    private boolean showMouseCps = true, showKeyLabels = true, showMovementKeys = true, showMouseButtons = true;
    private boolean showSpaceBar = true, showModifierKeys = true, showKeyBorder, animationsEnabled = true, applyingPreset;
    private FrameMode frameMode = FrameMode.CONNECTED;
    private static final int CONNECTED_GAP = 2;
    private final EnumMap<Key, Integer> individualColors = new EnumMap<>(Key.class);
    private final List<KeyElement> elements = new ArrayList<>();
    private final Map<Long, Boolean> previousDown = new HashMap<>();
    private final Map<Long, Long> releaseTimes = new HashMap<>();
    private long nextElementId = 1;

    private static boolean wasLmb, wasRmb;
    private static final ClickRateTracker LEFT_CLICKS = new ClickRateTracker(128), RIGHT_CLICKS = new ClickRateTracker(128);
    private long frameTime;
    private int lastLeftCps = Integer.MIN_VALUE, lastRightCps = Integer.MIN_VALUE;
    private Component leftCps = Component.literal("0 CPS"), rightCps = Component.literal("0 CPS");

    public KeystrokesModule() {
        super("Keystrokes", "HUD", false, 6, 86, "", "");
        super.setTextColor(keyTextColor);
        super.setWaveColor2(gradientColor);
        super.setBackgroundColor(normalBoxColor);
        super.setBorderColor(keyBorderColor);
        super.setBorder(showKeyBorder);
        super.setColorMode(ColorMode.SOLID);
        for (Key key : Key.values()) individualColors.put(key, 0xFFFFFFFF);
        rebuildLayout(layoutPreset, false);
    }

    @Override public String getDescription() { return "Frei platzierbare Tasten mit eigenen Bindings, Größen, Labels, Farben und CPS-Anzeige."; }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/keystrokes.png"); }

    public static void updateClicks(Minecraft client) {
        if (client == null || client.options == null) return;
        long now = System.currentTimeMillis();
        boolean down = client.options.keyAttack.isDown(); if (down && !wasLmb) LEFT_CLICKS.record(now); wasLmb = down;
        down = client.options.keyUse.isDown(); if (down && !wasRmb) RIGHT_CLICKS.record(now); wasRmb = down;
        LEFT_CLICKS.count(now); RIGHT_CLICKS.count(now);
    }
    public static int getLeftCps() { return LEFT_CLICKS.count(System.currentTimeMillis()); }
    public static int getRightCps() { return RIGHT_CLICKS.count(System.currentTimeMillis()); }

    private void changed() { if (!applyingPreset) stylePreset = StylePreset.CUSTOM; ConfigManager.save(); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : min; }

    public LayoutPreset getLayoutPreset() { return layoutPreset; }
    public void setLayoutPreset(LayoutPreset value) { rebuildLayout(value == null ? LayoutPreset.WASD_MOUSE_SPACE_CPS : value, true); }
    public SpaceStyle getSpaceStyle() { return spaceStyle; }
    public void setSpaceStyle(SpaceStyle value) { spaceStyle = value == null ? SpaceStyle.LINE : value; changed(); }
    public StylePreset getStylePreset() { return stylePreset; }

    @Override
    public ColorMode getColorMode() {
        if (keyColorMode == KeyColorMode.RAINBOW) return ColorMode.RAINBOW;
        if (keyColorMode == KeyColorMode.GRADIENT) return ColorMode.WAVE;
        return ColorMode.SOLID;
    }

    @Override
    public void setColorMode(ColorMode value) {
        super.setColorMode(value);
        if (value == ColorMode.RAINBOW) {
            this.keyColorMode = KeyColorMode.RAINBOW;
        } else if (value == ColorMode.WAVE) {
            this.keyColorMode = KeyColorMode.GRADIENT;
        } else {
            this.keyColorMode = KeyColorMode.SOLID;
        }
        changed();
    }

    public KeyColorMode getKeyColorMode() { return keyColorMode; }
    public void setKeyColorMode(KeyColorMode value) {
        keyColorMode = value == null ? KeyColorMode.SOLID : value;
        if (keyColorMode == KeyColorMode.RAINBOW) {
            super.setColorMode(ColorMode.RAINBOW);
        } else if (keyColorMode == KeyColorMode.GRADIENT) {
            super.setColorMode(ColorMode.WAVE);
        } else {
            super.setColorMode(ColorMode.SOLID);
        }
        changed();
    }

    public PressAnimation getPressAnimation() { return pressAnimation; }
    public void setPressAnimation(PressAnimation value) { pressAnimation = value == null ? PressAnimation.FADE : value; changed(); }
    public int getFadeTimeMs() { return fadeTimeMs; }
    public void setFadeTimeMs(int value) { fadeTimeMs = clamp(value, 0, 1000); changed(); }
    public int getKeySize() { return keySize; }
    public void setKeySize(int value) { keySize = clamp(value, 10, 48); changed(); }
    public int getKeySpacing() { return keySpacing; }
    public void setKeySpacing(int value) { keySpacing = clamp(value, 0, 12); changed(); }
    public int getKeyCornerRadius() { return keyCornerRadius; }
    public void setKeyCornerRadius(int value) { keyCornerRadius = clamp(value, 0, 2); changed(); }
    public int getBoxOpacity() { return boxOpacity; }
    public void setBoxOpacity(int value) { boxOpacity = clamp(value, 0, 100); changed(); }
    public int getTextOpacity() { return textOpacity; }
    public void setTextOpacity(int value) { textOpacity = clamp(value, 0, 100); changed(); }
    public float getFontScale() { return fontScale; }
    public void setFontScale(float value) { fontScale = clamp(value, .5f, 2f); changed(); }

    @Override
    public int getBackgroundColor() { return normalBoxColor; }
    @Override
    public void setBackgroundColor(int value) {
        super.setBackgroundColor(value);
        normalBoxColor = value;
        changed();
    }
    public int getNormalBoxColor() { return normalBoxColor; }
    public void setNormalBoxColor(int value) {
        normalBoxColor = value;
        super.setBackgroundColor(value);
        changed();
    }

    public int getPressedBoxColor() { return pressedBoxColor; }
    public void setPressedBoxColor(int value) { pressedBoxColor = value; changed(); }

    @Override
    public int getTextColor() { return keyTextColor; }
    @Override
    public void setTextColor(int value) {
        super.setTextColor(value);
        keyTextColor = value;
        changed();
    }
    public int getKeyTextColor() { return keyTextColor; }
    public void setKeyTextColor(int value) {
        keyTextColor = value;
        super.setTextColor(value);
        changed();
    }

    public int getPressedTextColor() { return pressedTextColor; }
    public void setPressedTextColor(int value) { pressedTextColor = value; changed(); }

    @Override
    public int getWaveColor2() { return gradientColor; }
    @Override
    public void setWaveColor2(int value) {
        super.setWaveColor2(value);
        gradientColor = value;
        changed();
    }
    public int getGradientColor() { return gradientColor; }
    public void setGradientColor(int value) {
        gradientColor = value;
        super.setWaveColor2(value);
        changed();
    }

    @Override
    public int getBorderColor() { return keyBorderColor; }
    @Override
    public void setBorderColor(int value) {
        super.setBorderColor(value);
        keyBorderColor = value;
        changed();
    }
    public int getKeyBorderColor() { return keyBorderColor; }
    public void setKeyBorderColor(int value) {
        keyBorderColor = value;
        super.setBorderColor(value);
        changed();
    }

    public int getKeyBorderWidth() { return keyBorderWidth; }
    public void setKeyBorderWidth(int value) { keyBorderWidth = clamp(value, 2, 4); changed(); }

    @Override
    public void setRainbowSpeed(float value) {
        super.setRainbowSpeed(value);
        colorCycleSpeed = clamp(value, .1f, 5f);
        changed();
    }
    public float getColorCycleSpeed() { return colorCycleSpeed; }
    public void setColorCycleSpeed(float value) {
        colorCycleSpeed = clamp(value, .1f, 5f);
        super.setRainbowSpeed(this.colorCycleSpeed);
        changed();
    }

    public boolean isShowMouseCps() { return showMouseCps; }
    public void setShowMouseCps(boolean value) {
        showMouseCps = value;
        for (KeyElement element : elements) if (element.binding == Binding.ATTACK || element.binding == Binding.USE) element.showCps = value;
        changed();
    }
    public boolean isShowKeyLabels() { return showKeyLabels; }
    public void setShowKeyLabels(boolean value) { showKeyLabels = value; changed(); }
    public boolean isShowMovementKeys() { return showMovementKeys; }
    public void setShowMovementKeys(boolean value) { showMovementKeys = value; changed(); }
    public boolean isShowMouseButtons() { return showMouseButtons; }
    public void setShowMouseButtons(boolean value) { showMouseButtons = value; changed(); }
    public boolean isShowSpaceBar() { return showSpaceBar; }
    public void setShowSpaceBar(boolean value) { showSpaceBar = value; changed(); }
    public boolean isShowModifierKeys() { return showModifierKeys; }
    public void setShowModifierKeys(boolean value) { showModifierKeys = value; changed(); }

    @Override
    public boolean hasBorder() { return showKeyBorder || super.hasBorder(); }
    @Override
    public void setBorder(boolean value) {
        super.setBorder(value);
        showKeyBorder = value;
        changed();
    }
    public boolean isShowKeyBorder() { return showKeyBorder; }
    public void setShowKeyBorder(boolean value) {
        showKeyBorder = value;
        super.setBorder(value);
        changed();
    }

    public boolean isAnimationsEnabled() { return animationsEnabled; }
    public void setAnimationsEnabled(boolean value) { animationsEnabled = value; changed(); }
    public FrameMode getFrameMode() { return frameMode; }
    public void setFrameMode(FrameMode value) {
        frameMode = value == null ? FrameMode.CONNECTED : value;
        changed();
    }

    public List<KeyElement> elements() { return Collections.unmodifiableList(elements); }
    public KeyElement element(long id) { return elements.stream().filter(e -> e.id == id).findFirst().orElse(null); }
    public KeyElement addElement() {
        int index = elements.size();
        KeyElement element = newElement(Binding.CUSTOM, GLFW.GLFW_KEY_K, "K", (index % 8) * (keySize + keySpacing),
                (index / 8) * (keySize + keySpacing), keySize, keySize);
        elements.add(element); changed(); return element;
    }
    public KeyElement duplicateElement(long id) {
        KeyElement source = element(id); if (source == null) return null;
        KeyElement copy = newElement(source.binding, source.keyCode, source.label, source.x + 3, source.y + 3, source.width, source.height);
        copy.showCps = source.showCps; copy.customColors = source.customColors; copy.boxColor = source.boxColor;
        copy.pressedBoxColor = source.pressedBoxColor; copy.textColor = source.textColor; copy.pressedTextColor = source.pressedTextColor;
        elements.add(copy); changed(); return copy;
    }
    public void removeElement(long id) { if (elements.removeIf(e -> e.id == id)) { previousDown.remove(id); releaseTimes.remove(id); changed(); } }
    public void moveElement(KeyElement element, int x, int y) { if (element != null) { element.x = Math.max(0, x); element.y = Math.max(0, y); } }
    public void resizeElement(KeyElement element, int width, int height) { if (element != null) { element.width = clamp(width, 8, 400); element.height = clamp(height, 8, 200); } }
    public void setElementBinding(KeyElement element, Binding binding, int keyCode) {
        if (element == null) return; element.binding = binding == null ? Binding.CUSTOM : binding; element.keyCode = keyCode;
        if (element.label.isBlank() || element.label.equals("KEY")) element.label = element.binding.defaultLabel(); changed();
    }
    public void setElementLabel(KeyElement element, String label) { if (element != null) { element.label = cleanLabel(label); changed(); } }
    public void setElementShowCps(KeyElement element, boolean value) { if (element != null) { element.showCps = value; changed(); } }
    public void setElementCustomColors(KeyElement element, boolean value) {
        if (element == null) return;
        if (value && !element.customColors) { element.boxColor = normalBoxColor; element.pressedBoxColor = pressedBoxColor; element.textColor = keyTextColor; element.pressedTextColor = pressedTextColor; }
        element.customColors = value; changed();
    }
    public void setElementBoxColor(KeyElement e, int value) { if (e != null) { e.boxColor = value; e.customColors = true; changed(); } }
    public void setElementPressedBoxColor(KeyElement e, int value) { if (e != null) { e.pressedBoxColor = value; e.customColors = true; changed(); } }
    public void setElementTextColor(KeyElement e, int value) { if (e != null) { e.textColor = value; e.customColors = true; changed(); } }
    public void setElementPressedTextColor(KeyElement e, int value) { if (e != null) { e.pressedTextColor = value; e.customColors = true; changed(); } }
    public void saveLayout() { changed(); }
    public void normalizePositions() {
        if (elements.isEmpty()) return;
        int minX = elements.stream().mapToInt(e -> e.x).min().orElse(0);
        int minY = elements.stream().mapToInt(e -> e.y).min().orElse(0);
        if (minX > 0 || minY > 0) {
            for (KeyElement e : elements) {
                e.x = Math.max(0, e.x - minX);
                e.y = Math.max(0, e.y - minY);
            }
            changed();
        }
    }

    public int getColor(ColorTarget target) {
        return switch (target) {
            case NORMAL_BOX -> normalBoxColor; case PRESSED_BOX -> pressedBoxColor; case KEY_TEXT -> keyTextColor;
            case PRESSED_TEXT -> pressedTextColor; case GRADIENT -> gradientColor; case BORDER -> keyBorderColor;
            default -> individualColors.get(toKey(target));
        };
    }
    public void setColor(ColorTarget target, int value) {
        switch (target) {
            case NORMAL_BOX -> normalBoxColor = value; case PRESSED_BOX -> pressedBoxColor = value; case KEY_TEXT -> keyTextColor = value;
            case PRESSED_TEXT -> pressedTextColor = value; case GRADIENT -> gradientColor = value; case BORDER -> keyBorderColor = value;
            default -> individualColors.put(toKey(target), value);
        }
        changed();
    }
    private static Key toKey(ColorTarget target) {
        return switch (target) { case W -> Key.W; case A -> Key.A; case S -> Key.S; case D -> Key.D; case LMB -> Key.LMB;
            case RMB -> Key.RMB; case SPACE -> Key.SPACE; case SNEAK -> Key.SNEAK; case SPRINT -> Key.SPRINT;
            default -> throw new IllegalArgumentException("Not a key colour"); };
    }

    public void setStylePreset(StylePreset preset) {
        if (preset == null || preset == StylePreset.CUSTOM) { stylePreset = StylePreset.CUSTOM; ConfigManager.save(); return; }
        applyingPreset = true;
        if (preset == StylePreset.CLASSIC) {
            fadeTimeMs = 150; keySize = 20; keySpacing = 2; keyCornerRadius = 1; keyColorMode = KeyColorMode.SOLID; pressAnimation = PressAnimation.FADE;
            normalBoxColor = 0x80000000; pressedBoxColor = 0x70FFFFFF; keyTextColor = 0xFFFFFFFF; pressedTextColor = 0xFF000000;
            showMouseCps = showKeyLabels = animationsEnabled = true; showKeyBorder = false; rebuildLayout(LayoutPreset.WASD_MOUSE_SPACE_CPS, false);
        } else if (preset == StylePreset.MINIMAL) {
            keySize = 18; keySpacing = 2; keyCornerRadius = 1; fadeTimeMs = 0; pressAnimation = PressAnimation.INSTANT;
            showMouseCps = false; showKeyLabels = true; animationsEnabled = false; showKeyBorder = false; rebuildLayout(LayoutPreset.WASD, false);
        } else {
            keySize = 20; keySpacing = 2; keyCornerRadius = 1; fadeTimeMs = 220; pressAnimation = PressAnimation.PULSE;
            normalBoxColor = 0xA8111419; pressedBoxColor = 0xC722C96E; keyTextColor = pressedTextColor = 0xFFFFFFFF;
            keyBorderColor = 0xFF22C96E; showMouseCps = showKeyLabels = showKeyBorder = animationsEnabled = true; rebuildLayout(LayoutPreset.FULL, false);
        }
        stylePreset = preset; applyingPreset = false; ConfigManager.save();
    }

    public boolean isBoxLayout() { return true; }
    public void setBoxLayout(boolean value) { }
    public boolean isShowSpace() { return showSpaceBar; }
    public void setShowSpace(boolean value) { setShowSpaceBar(value); }
    public boolean isSpaceIsLine() { return spaceStyle == SpaceStyle.LINE; }
    public void setSpaceIsLine(boolean value) { setSpaceStyle(value ? SpaceStyle.LINE : SpaceStyle.TEXT); }
    public boolean isShowMouse() { return showMouseButtons; }
    public void setShowMouse(boolean value) { setShowMouseButtons(value); }
    public boolean isShowCps() { return showMouseCps; }
    public void setShowCps(boolean value) { setShowMouseCps(value); }

    private KeyElement newElement(Binding binding, int keyCode, String label, int x, int y, int width, int height) {
        return new KeyElement(nextElementId++, binding, keyCode, cleanLabel(label), Math.max(0, x), Math.max(0, y), clamp(width, 8, 400), clamp(height, 8, 200));
    }
    private static String cleanLabel(String label) {
        if (label == null) return ""; String clean = label.replace("\n", " ").replace("\r", " ");
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }
    private void rebuildLayout(LayoutPreset preset, boolean save) {
        layoutPreset = preset; elements.clear(); previousDown.clear(); releaseTimes.clear(); nextElementId = 1;
        int s = keySize, gap = keySpacing, y = 0;
        if (preset == LayoutPreset.KEYBOARD || preset == LayoutPreset.FULL_KEYBOARD) {
            if (preset == LayoutPreset.FULL_KEYBOARD) { addKeyboardRow(y, s, new String[]{"F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12"},
                    new int[]{GLFW.GLFW_KEY_F1,GLFW.GLFW_KEY_F2,GLFW.GLFW_KEY_F3,GLFW.GLFW_KEY_F4,GLFW.GLFW_KEY_F5,GLFW.GLFW_KEY_F6,GLFW.GLFW_KEY_F7,GLFW.GLFW_KEY_F8,GLFW.GLFW_KEY_F9,GLFW.GLFW_KEY_F10,GLFW.GLFW_KEY_F11,GLFW.GLFW_KEY_F12}); y += s + gap; }
            addKeyboardRow(y, s, new String[]{"1","2","3","4","5","6","7","8","9","0"}, new int[]{49,50,51,52,53,54,55,56,57,48}); y += s + gap;
            addKeyboardRow(y, s, new String[]{"TAB","Q","W","E","R","T","Z","U","I","O","P"}, new int[]{GLFW.GLFW_KEY_TAB,81,87,69,82,84,90,85,73,79,80}); y += s + gap;
            addKeyboardRow(y, s, new String[]{"CAPS","A","S","D","F","G","H","J","K","L"}, new int[]{GLFW.GLFW_KEY_CAPS_LOCK,65,83,68,70,71,72,74,75,76}); y += s + gap;
            addKeyboardRow(y, s, new String[]{"SHIFT","Y","X","C","V","B","N","M","RSHIFT"}, new int[]{GLFW.GLFW_KEY_LEFT_SHIFT,89,88,67,86,66,78,77,GLFW.GLFW_KEY_RIGHT_SHIFT}); y += s + gap;
            addKeyboardRow(y, s, new String[]{"CTRL","ALT","SPACE","ALT GR"}, new int[]{GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_KEY_LEFT_ALT,GLFW.GLFW_KEY_SPACE,GLFW.GLFW_KEY_RIGHT_ALT});
        } else {
            add(Binding.FORWARD, "W", s + gap, 0, s, s, false); add(Binding.LEFT, "A", 0, s + gap, s, s, false);
            add(Binding.BACK, "S", s + gap, s + gap, s, s, false); add(Binding.RIGHT, "D", (s + gap) * 2, s + gap, s, s, false); y = s * 2 + gap;
            if (preset != LayoutPreset.WASD) { y += gap; int mw = (s * 3 + gap) / 2; boolean cps = showMouseCps && (preset == LayoutPreset.WASD_MOUSE_SPACE_CPS || preset == LayoutPreset.FULL);
                add(Binding.ATTACK, "LMB", 0, y, mw, cps ? s + 4 : s, cps); add(Binding.USE, "RMB", mw + gap, y, mw, cps ? s + 4 : s, cps); y += cps ? s + 4 : s; }
            if (preset == LayoutPreset.WASD_MOUSE_SPACE || preset == LayoutPreset.WASD_MOUSE_SPACE_CPS || preset == LayoutPreset.FULL) {
                y += gap; add(Binding.JUMP, "[SPACE]", 0, y, s * 3 + gap * 2, Math.max(11, s - 7), false); y += Math.max(11, s - 7); }
            if (preset == LayoutPreset.FULL) { y += gap; int h = Math.max(11, s - 7); add(Binding.SNEAK, "SNEAK", 0, y, s * 3 + gap * 2, h, false); y += h + gap;
                add(Binding.SPRINT, "SPRINT", 0, y, s * 3 + gap * 2, h, false); }
        }
        if (save) changed();
    }
    private void addKeyboardRow(int y, int size, String[] labels, int[] codes) {
        int x = 0;
        for (int i = 0; i < labels.length; i++) {
            int width = switch (labels[i]) { case "TAB", "CAPS", "CTRL", "ALT", "ALT GR" -> size + 8; case "SHIFT", "RSHIFT" -> size + 14; case "SPACE" -> size * 4; default -> size; };
            elements.add(newElement(Binding.CUSTOM, codes[i], labels[i], x, y, width, size)); x += width + keySpacing;
        }
    }
    private void add(Binding binding, String label, int x, int y, int width, int height, boolean cps) {
        KeyElement element = newElement(binding, -1, label, x, y, width, height); element.showCps = cps; elements.add(element);
    }

    private int contentMinX() { return elements.stream().mapToInt(e -> e.x).min().orElse(0); }
    private int contentMinY() { return elements.stream().mapToInt(e -> e.y).min().orElse(0); }
    private int contentWidth() {
        int min = contentMinX();
        return elements.stream().mapToInt(e -> e.x + e.width).max().orElse(min + 1) - min;
    }
    private int contentHeight() {
        int min = contentMinY();
        return elements.stream().mapToInt(e -> e.y + e.height).max().orElse(min + 1) - min;
    }
    private boolean connectedFrameActive() { return frameMode == FrameMode.CONNECTED && hasBorder(); }
    private int connectedBorderWidth() {
        return Math.max(1, Math.min(3, showKeyBorder ? keyBorderWidth - 1 : getBorderWidth() - 1));
    }
    private int outerPadding() {
        return connectedFrameActive() ? CONNECTED_GAP + connectedBorderWidth() : 0;
    }
    public int getContentOffsetX() { return outerPadding() - contentMinX(); }
    public int getContentOffsetY() { return outerPadding() - contentMinY(); }
    @Override public int getWidth(Minecraft client) { return contentWidth() + outerPadding() * 2; }
    @Override public int getWidth(Minecraft client, boolean editor) { return getWidth(client); }
    @Override public int getHeight(Minecraft client) { return contentHeight() + outerPadding() * 2; }
    @Override public int getHeight(Minecraft client, boolean editor) { return getHeight(client); }
    @Override protected String value(Minecraft client) { return "Keystrokes"; }

    @Override
    public boolean containsEditorPoint(Minecraft client, double localX, double localY) {
        int pad = outerPadding();
        double contentX = localX + contentMinX() - pad;
        double contentY = localY + contentMinY() - pad;
        for (KeyElement element : elements) {
            int hitPad = connectedFrameActive() ? outerPadding() : 0;
            if (contentX >= element.x - hitPad && contentX < element.x + element.width + hitPad
                    && contentY >= element.y - hitPad && contentY < element.y + element.height + hitPad) {
                return true;
            }
        }
        return false;
    }

    public void renderCustom(GuiGraphicsExtractor g, Minecraft client, boolean editor) {
        if (client == null || client.options == null) return;
        g.pose().pushMatrix();
        g.pose().translate(getX(), getY());
        g.pose().scale((float)getScale(), (float)getScale());
        g.pose().translate(getContentOffsetX(), getContentOffsetY());
        renderElements(g, client, editor);
        g.pose().popMatrix();
    }
    public void renderDesignerPreview(GuiGraphicsExtractor g, Minecraft client, int x, int y, float scale) {
        if (client == null || client.options == null) return;
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale, scale); renderElements(g, client, true); g.pose().popMatrix();
    }
    private void renderElements(GuiGraphicsExtractor g, Minecraft client, boolean editor) {
        frameTime = renderFrameTimeMillis();
        if (connectedFrameActive()) renderConnectedFrame(g);
        for (KeyElement element : elements) {
            boolean down = editor
                    ? element.binding == Binding.FORWARD || element.binding == Binding.ATTACK
                    : isDown(client, element);
            boolean previous = previousDown.getOrDefault(element.id, false);
            if (!down && previous) releaseTimes.put(element.id, frameTime); previousDown.put(element.id, down);
            drawElement(g, client, element, down, releaseTimes.getOrDefault(element.id, Long.MIN_VALUE), editor);
        }
    }

    /**
     * A single shape-following frame: expanded key silhouettes merge across the
     * configured gap, while the second pass removes all internal border seams.
     */
    private void renderConnectedFrame(GuiGraphicsExtractor g) {
        int border = connectedBorderWidth();
        int accent = showKeyBorder ? keyBorderColor : currentBorderColor();
        if (keyColorMode == KeyColorMode.RAINBOW || isRainbowBorder()) accent = color();
        int outer = CONNECTED_GAP + border;
        int radius = Math.max(0, keyCornerRadius + CONNECTED_GAP);

        for (KeyElement element : elements) {
            renderRoundedBox(g, element.x - outer, element.y - outer,
                    element.width + outer * 2, element.height + outer * 2,
                    radius + border, accent);
        }

        // The merged base is intentionally opaque: overlapping translucent
        // silhouettes would create darker seams between otherwise connected keys.
        int surface = 0xFF000000 | (normalBoxColor & 0x00FFFFFF);
        for (KeyElement element : elements) {
            renderRoundedBox(g, element.x - CONNECTED_GAP, element.y - CONNECTED_GAP,
                    element.width + CONNECTED_GAP * 2, element.height + CONNECTED_GAP * 2,
                    radius, surface);
        }
    }
    private static boolean isDown(Minecraft client, KeyElement element) {
        return switch (element.binding) {
            case FORWARD -> client.options.keyUp.isDown(); case LEFT -> client.options.keyLeft.isDown(); case BACK -> client.options.keyDown.isDown(); case RIGHT -> client.options.keyRight.isDown();
            case JUMP -> client.options.keyJump.isDown(); case SNEAK -> client.options.keyShift.isDown(); case SPRINT -> client.options.keySprint.isDown();
            case ATTACK -> client.options.keyAttack.isDown(); case USE -> client.options.keyUse.isDown(); case CUSTOM -> element.keyCode >= 0
                    && GLFW.glfwGetKey(client.getWindow().handle(), element.keyCode) == GLFW.GLFW_PRESS;
        };
    }
    private float press(boolean down, long release) {
        if (down) return 1f; if (!animationsEnabled || pressAnimation == PressAnimation.INSTANT || fadeTimeMs == 0) return 0f;
        long elapsed = frameTime - release; if (elapsed < 0 || elapsed >= fadeTimeMs) return 0f; float remain = 1f - elapsed / (float)fadeTimeMs;
        return pressAnimation == PressAnimation.PULSE ? remain * (.78f + .22f * (float)Math.sin((1f - remain) * Math.PI)) : remain;
    }
    private static int opacity(int color, int percent) { return (color & 0x00FFFFFF) | ((((color >>> 24) * percent + 50) / 100) << 24); }
    private static int rgbWithAlpha(int rgb, int alphaSource) { return (rgb & 0x00FFFFFF) | (alphaSource & 0xFF000000); }
    private double normalizedPhase(KeyElement element) {
        if (elements.isEmpty()) return 0.0;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        for (KeyElement e : elements) {
            minY = Math.min(minY, e.y);
            maxY = Math.max(maxY, e.y + e.height);
            minX = Math.min(minX, e.x);
            maxX = Math.max(maxX, e.x + e.width);
        }
        double totalH = Math.max(1, maxY - minY);
        double totalW = Math.max(1, maxX - minX);
        double centerY = (element.y + element.height / 2.0) - minY;
        double centerX = (element.x + element.width / 2.0) - minX;
        // Clean continuous diagonal flow (80% vertical progression, 20% horizontal tilt)
        return (centerY / totalH) * 0.8 + (centerX / totalW) * 0.2;
    }
    private static Key legacyKey(Binding binding) {
        return switch (binding) { case FORWARD -> Key.W; case LEFT -> Key.A; case BACK -> Key.S; case RIGHT -> Key.D; case ATTACK -> Key.LMB; case USE -> Key.RMB;
            case JUMP -> Key.SPACE; case SNEAK -> Key.SNEAK; case SPRINT -> Key.SPRINT; default -> null; };
    }
    private int color(KeyElement element, boolean text, float pressed) {
        int normal = text ? keyTextColor : normalBoxColor, active = text ? pressedTextColor : pressedBoxColor;
        if (element.customColors) { normal = text ? element.textColor : element.boxColor; active = text ? element.pressedTextColor : element.pressedBoxColor; }
        else if (keyColorMode == KeyColorMode.INDIVIDUAL) { Key key = legacyKey(element.binding); if (key != null) { int individual = individualColors.get(key); normal = text ? individual : rgbWithAlpha(individual, normal); } }
        int result = interpolateColor(normal, active, pressed);
        if (keyColorMode == KeyColorMode.RAINBOW) {
            long period = Math.max(250L, (long)(4000L / Math.max(0.1f, colorCycleSpeed)));
            double phase = normalizedPhase(element);
            double progress = ((frameTime % period) / (double) period) + phase * 0.45;
            float hue = (float) (progress % 1.0);
            if (hue < 0) hue += 1.0f;
            int rgb = java.awt.Color.HSBtoRGB(hue, getRainbowSaturation(), 1.0f) & 0x00FFFFFF;
            return (result & 0xFF000000) | rgb;
        }
        if (keyColorMode == KeyColorMode.GRADIENT) {
            long period = Math.max(250L, (long)(3000L / Math.max(0.1f, colorCycleSpeed)));
            double phase = normalizedPhase(element);
            double time = ((frameTime % period) / (double) period) * Math.PI * 2.0;
            // Smooth continuous wave between primary color (result) and secondary gradient color
            float factor = (float) ((Math.sin(time + phase * Math.PI * 1.5) + 1.0) / 2.0);
            return interpolateColor(result, rgbWithAlpha(gradientColor, result), factor);
        }
        return result;
    }
    private void drawElement(GuiGraphicsExtractor g, Minecraft client, KeyElement element, boolean down, long release, boolean editor) {
        float pressed = press(down, release); int bg = opacity(color(element, false, pressed), boxOpacity);
        if (hasBackground() && bg != 0) renderRoundedBox(g, element.x, element.y, element.width, element.height, keyCornerRadius, bg);
        if ((showKeyBorder || hasBorder()) && !connectedFrameActive()) {
            int accent = showKeyBorder ? keyBorderColor : currentBorderColor(); if (keyColorMode == KeyColorMode.RAINBOW || isRainbowBorder()) accent = color(element, true, pressed);
            int border = showKeyBorder ? keyBorderWidth : getBorderWidth();
            if (hasBorder()) {
                renderMinecraftFrame(g, element.x, element.y, element.width, element.height, getBorderStyle(), accent, border);
            } else {
                g.outline(element.x, element.y, element.width, element.height, (accent & 0xFF000000) | 0x0006080B);
                for (int i = 1; i < border; i++) g.outline(element.x + i, element.y + i, Math.max(1, element.width - i * 2), Math.max(1, element.height - i * 2), accent);
            }
        }
        if (!showKeyLabels) return; int text = opacity(color(element, true, pressed), textOpacity);
        int inset = connectedFrameActive() ? 2 : Math.max(2, (showKeyBorder || hasBorder())
                ? (showKeyBorder ? keyBorderWidth : getBorderWidth()) + 1 : 2);
        int innerW = Math.max(1, element.width - inset * 2);
        int innerH = Math.max(1, element.height - inset * 2);
        boolean cps = element.showCps && (element.binding == Binding.ATTACK || element.binding == Binding.USE);
        if (cps) {
            int rowGap = 1;
            int rowH = Math.max(1, (innerH - rowGap) / 2);
            drawFittedText(g, client, element.label, element.x + element.width / 2f,
                    element.y + inset + rowH / 2f, text, innerW, rowH);
            int count = editor ? (element.binding == Binding.ATTACK ? 12 : 8)
                    : (element.binding == Binding.ATTACK ? getLeftCps() : getRightCps());
            drawFittedText(g, client, cps(element.binding == Binding.ATTACK, count).getString(),
                    element.x + element.width / 2f, element.y + inset + rowH + rowGap + rowH / 2f,
                    text, innerW, rowH);
        } else if (element.binding == Binding.JUMP && spaceStyle != SpaceStyle.TEXT) {
            if (spaceStyle == SpaceStyle.LINE) { int line = Math.min(innerW, Math.max(8, element.width / 2)); g.fill(element.x + (element.width - line) / 2, element.y + (element.height - 2) / 2,
                    element.x + (element.width + line) / 2, element.y + (element.height - 2) / 2 + 2, text); }
            else g.fill(element.x + inset, element.y + inset, element.x + element.width - inset, element.y + element.height - inset, text);
        } else drawFittedText(g, client, element.label, element.x + element.width / 2f,
                element.y + element.height / 2f, text, innerW, innerH);
    }
    private void drawFittedText(GuiGraphicsExtractor g, Minecraft client, String value, float centerX, float centerY,
                                int color, int maxWidth, int maxHeight) {
        int textWidth = Math.max(1, client.font.width(value));
        float fittedScale = Math.min(fontScale, Math.min(maxWidth / (float) textWidth, maxHeight / 9.0f));
        if (!Float.isFinite(fittedScale) || fittedScale <= 0.0f) return;
        g.pose().pushMatrix();
        g.pose().translate(centerX, centerY);
        g.pose().scale(fittedScale, fittedScale);
        g.centeredText(client.font, Component.literal(value), 0, -4, color);
        g.pose().popMatrix();
    }
    private Component cps(boolean left, int value) {
        if (left) { if (value != lastLeftCps) { lastLeftCps = value; leftCps = Component.literal(value + " CPS"); } return leftCps; }
        if (value != lastRightCps) { lastRightCps = value; rightCps = Component.literal(value + " CPS"); } return rightCps;
    }

    @Override public void resetSettings() {
        super.resetSettings(); applyingPreset = true; setStylePreset(StylePreset.CLASSIC); for (Key key : Key.values()) individualColors.put(key, 0xFFFFFFFF);
        applyingPreset = false; stylePreset = StylePreset.CLASSIC; ConfigManager.save();
    }

    public JsonObject saveSettings() {
        JsonObject json = new JsonObject(); json.addProperty("version", 2); json.addProperty("style", stylePreset.name()); json.addProperty("layout", layoutPreset.name());
        json.addProperty("frameMode", frameMode.name());
        json.addProperty("spaceStyle", spaceStyle.name()); json.addProperty("colorMode", keyColorMode.name()); json.addProperty("animation", pressAnimation.name());
        json.addProperty("size", keySize); json.addProperty("spacing", keySpacing); json.addProperty("radius", keyCornerRadius); json.addProperty("boxOpacity", boxOpacity);
        json.addProperty("textOpacity", textOpacity); json.addProperty("fontScale", fontScale); json.addProperty("cycleSpeed", colorCycleSpeed); json.addProperty("labels", showKeyLabels);
        json.addProperty("keyBorder", showKeyBorder); json.addProperty("animations", animationsEnabled); json.addProperty("normal", normalBoxColor); json.addProperty("pressed", pressedBoxColor);
        json.addProperty("text", keyTextColor); json.addProperty("pressedText", pressedTextColor); json.addProperty("gradient", gradientColor); json.addProperty("border", keyBorderColor); json.addProperty("borderWidth", keyBorderWidth);
        for (Key key : Key.values()) json.addProperty("key_" + key.name(), individualColors.get(key));
        JsonArray array = new JsonArray();
        for (KeyElement element : elements) { JsonObject item = new JsonObject(); item.addProperty("id", element.id); item.addProperty("binding", element.binding.name()); item.addProperty("key", element.keyCode);
            item.addProperty("label", element.label); item.addProperty("x", element.x); item.addProperty("y", element.y); item.addProperty("w", element.width); item.addProperty("h", element.height);
            item.addProperty("cps", element.showCps); item.addProperty("customColors", element.customColors); item.addProperty("box", element.boxColor); item.addProperty("pressed", element.pressedBoxColor);
            item.addProperty("text", element.textColor); item.addProperty("pressedText", element.pressedTextColor); array.add(item); }
        json.add("elements", array); return json;
    }
    public void loadSettings(JsonObject json) {
        if (json == null) return; applyingPreset = true;
        try {
            if (json.has("style")) stylePreset = StylePreset.valueOf(json.get("style").getAsString()); if (json.has("layout")) layoutPreset = LayoutPreset.valueOf(json.get("layout").getAsString());
            if (json.has("frameMode")) frameMode = FrameMode.valueOf(json.get("frameMode").getAsString());
            if (json.has("spaceStyle")) spaceStyle = SpaceStyle.valueOf(json.get("spaceStyle").getAsString()); if (json.has("colorMode")) keyColorMode = KeyColorMode.valueOf(json.get("colorMode").getAsString());
            if (json.has("animation")) pressAnimation = PressAnimation.valueOf(json.get("animation").getAsString()); if (json.has("size")) keySize = clamp(json.get("size").getAsInt(), 10, 48);
            if (json.has("spacing")) keySpacing = clamp(json.get("spacing").getAsInt(), 0, 12);
            if (json.has("radius")) {
                int r = json.get("radius").getAsInt();
                keyCornerRadius = clamp(r > 2 ? 1 : r, 0, 2);
            }
            if (json.has("boxOpacity")) boxOpacity = clamp(json.get("boxOpacity").getAsInt(), 0, 100); if (json.has("textOpacity")) textOpacity = clamp(json.get("textOpacity").getAsInt(), 0, 100);
            if (json.has("fontScale")) fontScale = clamp(json.get("fontScale").getAsFloat(), .5f, 2f); if (json.has("cycleSpeed")) colorCycleSpeed = clamp(json.get("cycleSpeed").getAsFloat(), .1f, 5f);
            if (json.has("labels")) showKeyLabels = json.get("labels").getAsBoolean(); if (json.has("keyBorder")) showKeyBorder = json.get("keyBorder").getAsBoolean();
            if (json.has("animations")) animationsEnabled = json.get("animations").getAsBoolean(); if (json.has("normal")) normalBoxColor = json.get("normal").getAsInt();
            if (json.has("pressed")) pressedBoxColor = json.get("pressed").getAsInt(); if (json.has("text")) keyTextColor = json.get("text").getAsInt();
            if (json.has("pressedText")) pressedTextColor = json.get("pressedText").getAsInt(); if (json.has("gradient")) gradientColor = json.get("gradient").getAsInt();
            if (json.has("border")) keyBorderColor = json.get("border").getAsInt(); if (json.has("borderWidth")) keyBorderWidth = clamp(json.get("borderWidth").getAsInt(), 2, 4);
            for (Key key : Key.values()) if (json.has("key_" + key.name())) individualColors.put(key, json.get("key_" + key.name()).getAsInt());
            if (json.has("elements") && json.get("elements").isJsonArray()) {
                elements.clear(); nextElementId = 1;
                for (JsonElement raw : json.getAsJsonArray("elements")) {
                    if (!raw.isJsonObject()) continue; JsonObject item = raw.getAsJsonObject(); Binding binding = item.has("binding") ? Binding.valueOf(item.get("binding").getAsString()) : Binding.CUSTOM;
                    long id = item.has("id") ? Math.max(1, item.get("id").getAsLong()) : nextElementId;
                    KeyElement element = new KeyElement(id, binding, item.has("key") ? item.get("key").getAsInt() : -1, cleanLabel(item.has("label") ? item.get("label").getAsString() : binding.defaultLabel()),
                            clamp(item.has("x") ? item.get("x").getAsInt() : 0, 0, 4000), clamp(item.has("y") ? item.get("y").getAsInt() : 0, 0, 4000),
                            clamp(item.has("w") ? item.get("w").getAsInt() : keySize, 8, 400), clamp(item.has("h") ? item.get("h").getAsInt() : keySize, 8, 200));
                    element.showCps = item.has("cps") && item.get("cps").getAsBoolean(); element.customColors = item.has("customColors") && item.get("customColors").getAsBoolean();
                    element.boxColor = item.has("box") ? item.get("box").getAsInt() : normalBoxColor; element.pressedBoxColor = item.has("pressed") ? item.get("pressed").getAsInt() : pressedBoxColor;
                    element.textColor = item.has("text") ? item.get("text").getAsInt() : keyTextColor; element.pressedTextColor = item.has("pressedText") ? item.get("pressedText").getAsInt() : pressedTextColor;
                    elements.add(element); nextElementId = Math.max(nextElementId, id + 1);
                }
            }
            if (keyColorMode == KeyColorMode.RAINBOW) {
                super.setColorMode(ColorMode.RAINBOW);
            } else if (keyColorMode == KeyColorMode.GRADIENT) {
                super.setColorMode(ColorMode.WAVE);
            } else {
                super.setColorMode(ColorMode.SOLID);
            }
            super.setTextColor(keyTextColor);
            super.setWaveColor2(gradientColor);
            super.setBackgroundColor(normalBoxColor);
            super.setBorderColor(keyBorderColor);
            super.setBorder(showKeyBorder);
            super.setRainbowSpeed(colorCycleSpeed);
            if (elements.isEmpty()) rebuildLayout(layoutPreset, false);
        } catch (IllegalArgumentException | IllegalStateException ignored) { if (elements.isEmpty()) rebuildLayout(LayoutPreset.WASD_MOUSE_SPACE_CPS, false); }
        finally { applyingPreset = false; }
    }
}
