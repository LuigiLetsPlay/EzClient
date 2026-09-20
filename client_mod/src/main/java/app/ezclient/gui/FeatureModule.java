package app.ezclient.gui;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Declarative, validated settings shared by the third module package. */
public abstract class FeatureModule extends HudModule {
    private static final Map<Class<?>, FeatureModule> INSTANCES = new java.util.concurrent.ConcurrentHashMap<>();
    public record Option(String key, String label, Object initial, double min, double max, String[] choices) {
        @Override public String label() {
            String suffix = " #AARRGGBB";
            return label.endsWith(suffix)
                    ? app.ezclient.util.EzI18n.text(label.substring(0, label.length() - suffix.length())) + suffix
                    : app.ezclient.util.EzI18n.text(label);
        }
    }
    /** Presentation metadata lives beside values so old configs and old modules remain compatible. */
    public record SettingInfo(String category, String description) {
        @Override public String description() { return app.ezclient.util.EzI18n.text(description); }
    }
    private final List<Option> options = new ArrayList<>();
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, SettingInfo> settingInfo = new LinkedHashMap<>();
    private final boolean hud;
    protected FeatureModule(String name, boolean hud, int y) {
        super(name, hud ? "HUD" : "Render", false, 10, y, "", "");
        this.hud = hud;
    }
    protected void option(String key, String label, Object initial, double min, double max, String... choices) {
        options.add(new Option(key, label, initial, min, max, choices)); values.put(key, initial);
        settingInfo.put(key, new SettingInfo(defaultCategory(label, initial), defaultDescription(label)));
    }
    /** Preferred declaration for new and polished modules. */
    protected void option(String category, String key, String label, String description, Object initial, double min, double max, String... choices) {
        option(key, label, initial, min, max, choices);
        settingInfo.put(key, new SettingInfo(category, description));
    }
    protected void flag(String category, String key, String label, String description, boolean value) {
        option(category, key, label, description, value, 0, 1);
    }
    private static String defaultCategory(String label, Object value) {
        if (label.contains("#AARRGGBB")) return "Farben";
        if (value instanceof Boolean) return "Darstellung";
        if (value instanceof Number) return "Feineinstellung";
        return "Allgemein";
    }
    private static String defaultDescription(String label) { return "Passt „" + label.replace(" #AARRGGBB", "") + "“ für dieses Modul an."; }
    protected void flag(String key, String label, boolean value) { option(key, label, value, 0, 1); }
    protected void colorOption(String key, String label, String value) { option(key, label + " #AARRGGBB", value, 0, 0); }
    protected void colorOption(String category, String key, String label, String description, String value) {
        option(category, key, label + " #AARRGGBB", description, value, 0, 0);
    }
    public List<Option> options() { return Collections.unmodifiableList(options); }
    public SettingInfo settingInfo(String key) { return settingInfo.getOrDefault(key, new SettingInfo("Allgemein", "Konfiguriert diese Einstellung.")); }
    public List<String> settingCategories() { return settingInfo.values().stream().map(SettingInfo::category).distinct().toList(); }
    public String categoryDescription(String category) {
        return switch (category) {
            case "Allgemein" -> "Grundlegende Einstellungen dieses Moduls.";
            case "Darstellung" -> "Ändert das sichtbare Verhalten und Layout.";
            case "Farbe", "Farben" -> "Farbe, Transparenz und dynamische Effekte.";
            case "Animation" -> "Steuert sanfte und dynamische Effekte.";
            case "Performance" -> "Begrenzt Aufwand und schützt die Bildrate.";
            case "Erweitert" -> "Zusätzliche Expertenoptionen.";
            default -> "Einstellungen für „" + category + "“.";
        };
    }
    public Object setting(String key) { return values.get(key); }
    public boolean flag(String key) { return Boolean.TRUE.equals(values.get(key)); }
    public double number(String key) { return ((Number)values.get(key)).doubleValue(); }
    public String text(String key) { return String.valueOf(values.get(key)); }
    public int tint(String key, boolean rainbow) {
        int rgba = (int)Long.parseLong(text(key).replace("#", ""), 16);
        if (!rainbow) return rgba;
        float hue = (System.currentTimeMillis() % 100000L) * getRainbowSpeed() / 4000f % 1f;
        return (rgba & 0xff000000) | (java.awt.Color.HSBtoRGB(hue, getRainbowSaturation(), 1) & 0xffffff);
    }
    public Option findOption(String key) {
        for (Option opt : options) {
            if (opt.key().equals(key)) return opt;
        }
        return null;
    }

    public boolean set(String key, Object value) {
        Option opt = findOption(key);
        if (opt != null) {
            return set(opt, value);
        }
        values.put(key, value);
        ConfigManager.save();
        return true;
    }

    public boolean set(Option option, Object value) {
        return setValue(option, value, true);
    }
    public boolean setTransient(Option option, Object value) {
        return setValue(option, value, false);
    }
    private boolean setValue(Option option, Object value, boolean persist) {
        try {
            if (option.initial() instanceof Boolean) value = Boolean.parseBoolean(value.toString());
            else if (option.initial() instanceof Number) {
                double n = Double.parseDouble(value.toString());
                if (!Double.isFinite(n)) return false;
                value = Math.max(option.min(), Math.min(option.max(), n));
            } else {
                String s = value.toString();
                if (s.length() > 1024) return false;
                if (option.label().contains("#AARRGGBB") && !s.matches("#?[0-9a-fA-F]{8}")) return false;
                if (option.choices().length > 0 && !Arrays.asList(option.choices()).contains(s)) return false;
                value = s;
            }
            values.put(option.key(), value);
            if (persist) ConfigManager.save();
            return true;
        } catch (RuntimeException ignored) { return false; }
    }
    public JsonObject saveFeature() {
        JsonObject json = new JsonObject(); json.addProperty("enabled", isEnabled());
        for (Option option : options) json.addProperty(option.key(), String.valueOf(values.get(option.key())));
        return json;
    }
    public void loadFeature(JsonObject json) {
        for (Option option : options) if (json.has(option.key())) {
            try { set(option, json.get(option.key()).getAsString()); } catch (RuntimeException ignored) {}
        }
        if (json.has("enabled")) setEnabled(json.get("enabled").getAsBoolean());
    }
    /** Resets this module only; persisted immediately and safe for live previews. */
    @Override public void resetSettings() {
        super.resetSettings();
        for (Option option : options) values.put(option.key(), option.initial());
        if (!ConfigManager.isLoading()) ConfigManager.save();
    }
    @Override public boolean hasHud() { return hud; }
    @Override public boolean hasPreview() { return hasHud(); }
    @Override protected String value(Minecraft client) { return ""; }
    public List<String> lines(Minecraft client, boolean editor) { return List.of(getName()); }
    @Override public int getHeight(Minecraft client) { return getHeight(client, false); }
    @Override public int getHeight(Minecraft client, boolean editor) {
        int padY = (hasBackground() || hasBorder()) ? CONTENT_PADDING_Y : 1;
        return lines(client, editor).size() * 12 + padY * 2;
    }
    @Override public int getWidth(Minecraft client, boolean editor) {
        int padX = (hasBackground() || hasBorder()) ? CONTENT_PADDING_X : 2;
        return lines(client, editor).stream().mapToInt(line -> client.font.width(styledText(line))).max().orElse(60) + padX * 2;
    }
    @Override public int getWidth(Minecraft client) { return getWidth(client, false); }
    public void renderFeature(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        if (!hud || (!editor && (EzScreenBridge.hudHidden(client) || client.getDebugOverlay().showDebugScreen()))) return;
        List<String> rows = lines(client, editor);
        if (rows.isEmpty()) return;
        int padX = (hasBackground() || hasBorder()) ? CONTENT_PADDING_X : 2;
        int padY = (hasBackground() || hasBorder()) ? CONTENT_PADDING_Y : 1;
        int width = rows.stream().mapToInt(row -> client.font.width(styledText(row))).max().orElse(60) + padX * 2;
        int height = rows.size() * 12 + padY * 2;
        float scale = (float) getScale();

        int renderX = getRenderX(client, width, editor);
        int renderY = getRenderY(client, height, editor);

        graphics.pose().pushMatrix();
        graphics.pose().translate(renderX, renderY);
        graphics.pose().scale(scale, scale);
        renderBackgroundAndBorder(graphics, 0, 0, width, height);
        for (int i = 0; i < rows.size(); i++) {
            net.minecraft.network.chat.Component comp = styledText(rows.get(i));
            int textW = client.font.width(comp);
            int lineX = Math.max(padX, (width - textW) / 2);
            graphics.text(client.font, comp, lineX, padY + i * 12, color(), isTextShadow());
        }
        graphics.pose().popMatrix();
    }
    public static <T extends FeatureModule> T get(Class<T> type) {
        FeatureModule cached = INSTANCES.get(type);
        if (cached != null) return type.cast(cached);
        for (Module module : ModuleManager.getInstance().getModules()) if (type.isInstance(module)) {
            INSTANCES.put(type, (FeatureModule)module); return type.cast(module);
        }
        throw new IllegalArgumentException(type.getName());
    }
}
