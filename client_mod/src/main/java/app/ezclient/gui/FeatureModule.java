package app.ezclient.gui;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Declarative, validated settings shared by the third module package. */
public abstract class FeatureModule extends HudModule {
    private static final Map<Class<?>, FeatureModule> INSTANCES = new java.util.concurrent.ConcurrentHashMap<>();
    public record Option(String key, String label, Object initial, double min, double max, String[] choices) {}
    private final List<Option> options = new ArrayList<>();
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final boolean hud;
    protected FeatureModule(String name, boolean hud, int y) {
        super(name, hud ? "HUD" : "Render", false, 10, y, "", "");
        this.hud = hud;
    }
    protected void option(String key, String label, Object initial, double min, double max, String... choices) {
        options.add(new Option(key, label, initial, min, max, choices)); values.put(key, initial);
    }
    protected void flag(String key, String label, boolean value) { option(key, label, value, 0, 1); }
    protected void colorOption(String key, String label, String value) { option(key, label + " #AARRGGBB", value, 0, 0); }
    public List<Option> options() { return Collections.unmodifiableList(options); }
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
    public boolean set(Option option, Object value) {
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
            values.put(option.key(), value); ConfigManager.save(); return true;
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
    public boolean hasHud() { return hud; }
    @Override protected String value(Minecraft client) { return ""; }
    public List<String> lines(Minecraft client, boolean editor) { return List.of(getName()); }
    @Override public int getHeight(Minecraft client) { return Math.max(14, lines(client, false).size() * 12 + 6); }
    @Override public int getWidth(Minecraft client, boolean editor) {
        return lines(client, editor).stream().mapToInt(line -> client.font.width(styledText(line))).max().orElse(60) + 8;
    }
    @Override public int getWidth(Minecraft client) { return getWidth(client, false); }
    public void renderFeature(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        if (!hud || (!editor && (EzScreenBridge.hudHidden(client) || client.getDebugOverlay().showDebugScreen()))) return;
        List<String> rows = lines(client, editor);
        if (rows.isEmpty()) return;
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY()); graphics.pose().scale((float)getScale(), (float)getScale());
        int width = rows.stream().mapToInt(row -> client.font.width(styledText(row))).max().orElse(60) + 8;
        renderBackgroundAndBorder(graphics, 0, 0, width, rows.size() * 12 + 6);
        for (int i = 0; i < rows.size(); i++) graphics.text(client.font, styledText(rows.get(i)), 4, 3 + i * 12, color(), isTextShadow());
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
