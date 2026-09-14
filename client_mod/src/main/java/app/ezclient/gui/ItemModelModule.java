package app.ezclient.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

/** Client-side model transforms, keyed by the actual registered item rather than model overrides. */
public final class ItemModelModule extends FeatureModule {
    public enum View { FIRST_PERSON, GROUND, GUI }
    public record Transform(float x, float y, float z, float pitch, float yaw, float roll,
                            float scaleX, float scaleY, float scaleZ) {
        public static final Transform IDENTITY = new Transform(0, 0, 0, 0, 0, 0, 1, 1, 1);
        public boolean isIdentity() { return equals(IDENTITY); }
        public float value(int index) {
            return switch (index) {
                case 0 -> x; case 1 -> y; case 2 -> z;
                case 3 -> pitch; case 4 -> yaw; case 5 -> roll;
                case 6 -> scaleX; case 7 -> scaleY; default -> scaleZ;
            };
        }
        public Transform with(int index, float value) {
            float[] v = {x, y, z, pitch, yaw, roll, scaleX, scaleY, scaleZ};
            v[index] = clamp(index, value);
            return new Transform(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
        }
        public void apply(PoseStack pose) {
            pose.translate(x, y, z);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.ZP.rotationDegrees(roll));
            pose.scale(scaleX, scaleY, scaleZ);
        }
    }

    private volatile Map<String, Map<View, Transform>> transforms = Map.of();

    public ItemModelModule() { super("Item Model", false, 10); }
    @Override public String getDescription() { return "Position, Drehung und Größe jedes Items für First Person, Boden und GUI anpassen."; }
    @Override public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/item_physics.png"); }

    public static View view(ItemDisplayContext context) {
        return switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> View.FIRST_PERSON;
            case GROUND -> View.GROUND;
            case GUI -> View.GUI;
            default -> null;
        };
    }
    public Transform getTransform(String itemId, View view) {
        Map<String, Map<View, Transform>> snapshot = transforms;
        Map<View, Transform> item = snapshot.get(itemId);
        if (item != null && item.containsKey(view)) return item.get(view);
        Map<View, Transform> all = snapshot.get("*");
        return all == null ? Transform.IDENTITY : all.getOrDefault(view, Transform.IDENTITY);
    }
    public Transform renderingTransform(String itemId, ItemDisplayContext context) {
        View view = view(context);
        return !isEnabled() || itemId == null || view == null ? Transform.IDENTITY : getTransform(itemId, view);
    }
    public boolean hasOverride(String itemId, View view) {
        Map<View, Transform> item = transforms.get(itemId);
        return item != null && item.containsKey(view);
    }
    public void setTransform(String itemId, View view, Transform value, boolean save) {
        if (itemId == null || view == null || value == null) return;
        Map<String, Map<View, Transform>> copy = new HashMap<>(transforms);
        EnumMap<View, Transform> item = new EnumMap<>(View.class);
        if (copy.containsKey(itemId)) item.putAll(copy.get(itemId));
        item.put(view, value);
        copy.put(itemId, Map.copyOf(item));
        transforms = Map.copyOf(copy);
        if (save) ConfigManager.save();
    }
    public void clearTransform(String itemId, View view) {
        Map<String, Map<View, Transform>> copy = new HashMap<>(transforms);
        EnumMap<View, Transform> item = new EnumMap<>(View.class);
        if (copy.containsKey(itemId)) item.putAll(copy.get(itemId));
        item.remove(view);
        if (item.isEmpty()) copy.remove(itemId); else copy.put(itemId, Map.copyOf(item));
        transforms = Map.copyOf(copy);
        ConfigManager.save();
    }
    @Override public void resetSettings() {
        transforms = Map.of();
        super.resetSettings();
    }
    @Override public JsonObject saveFeature() {
        JsonObject json = super.saveFeature();
        JsonObject entries = new JsonObject();
        for (var item : transforms.entrySet()) {
            JsonObject views = new JsonObject();
            for (var view : item.getValue().entrySet()) {
                Transform t = view.getValue();
                JsonObject data = new JsonObject();
                String[] names = {"x", "y", "z", "pitch", "yaw", "roll", "scaleX", "scaleY", "scaleZ"};
                for (int i = 0; i < names.length; i++) data.addProperty(names[i], t.value(i));
                views.add(view.getKey().name(), data);
            }
            entries.add(item.getKey(), views);
        }
        json.add("transforms", entries);
        return json;
    }
    @Override public void loadFeature(JsonObject json) {
        super.loadFeature(json);
        Map<String, Map<View, Transform>> loaded = new HashMap<>();
        if (json.has("transforms") && json.get("transforms").isJsonObject()) {
            JsonObject entries = json.getAsJsonObject("transforms");
            for (var item : entries.entrySet()) {
                if (!item.getKey().equals("*") && Identifier.tryParse(item.getKey()) == null) continue;
                if (!item.getValue().isJsonObject()) continue;
                EnumMap<View, Transform> views = new EnumMap<>(View.class);
                for (var view : item.getValue().getAsJsonObject().entrySet()) {
                    try {
                        View context = View.valueOf(view.getKey());
                        if (!view.getValue().isJsonObject()) continue;
                        JsonObject data = view.getValue().getAsJsonObject();
                        String[] names = {"x", "y", "z", "pitch", "yaw", "roll", "scaleX", "scaleY", "scaleZ"};
                        Transform t = Transform.IDENTITY;
                        for (int i = 0; i < names.length; i++) {
                            JsonElement raw = data.get(names[i]);
                            if (raw != null) t = t.with(i, raw.getAsFloat());
                        }
                        views.put(context, t);
                    } catch (RuntimeException ignored) {}
                }
                if (!views.isEmpty()) loaded.put(item.getKey(), Map.copyOf(views));
            }
        }
        transforms = Map.copyOf(loaded);
    }
    private static float clamp(int index, float value) {
        if (!Float.isFinite(value)) return index >= 6 ? 1 : 0;
        return Math.max(index >= 6 ? 0.05f : index >= 3 ? -180 : -2,
                Math.min(index >= 6 ? 4 : index >= 3 ? 180 : 2, value));
    }
}
