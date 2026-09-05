package app.ezclient.gui;

import java.util.*;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class WaypointsModule extends FeatureModule {
    public record Waypoint(String id, String name, double x, double y, double z, int color, String icon, String world, String dimension, boolean death) {
        public Vec3 position() { return new Vec3(x, y, z); }
    }
    private final List<Waypoint> points = new ArrayList<>();
    private boolean wasDead;
    private Object lastLevel;
    public WaypointsModule() {
        super("Waypoints Minimap Light", true, 210);
        option("marker", "World marker", "Floating", 0, 0, "Floating", "Beam", "Both", "None");
        flag("death", "Last death waypoint", true); flag("compass", "Compass bar", true);
        option("range", "World marker range", 128.0, 16, 512);
    }
    public static String world(Minecraft mc) {
        if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip;
        return mc.getSingleplayerServer() == null ? "" : "local:" + mc.getSingleplayerServer().getWorldData().getLevelName();
    }
    public List<Waypoint> points() { return List.copyOf(points); }
    public List<Waypoint> active(Minecraft mc) {
        if (mc.level == null) return List.of();
        String world = world(mc), dimension = mc.level.dimension().toString();
        return points.stream().filter(p -> p.world().equals(world) && p.dimension().equals(dimension)).toList();
    }
    public void put(Waypoint point) {
        if (point.name() == null || point.name().length() > 80 || point.icon() == null || point.icon().length() > 8
            || !Double.isFinite(point.x()) || !Double.isFinite(point.y()) || !Double.isFinite(point.z())
            || Math.abs(point.x()) > 30000000 || Math.abs(point.z()) > 30000000 || Math.abs(point.y()) > 2048) return;
        points.removeIf(p -> p.id().equals(point.id()));
        if (points.size() < 128) points.add(point);
        ConfigManager.save();
    }
    public void delete(String id) { points.removeIf(p -> p.id().equals(id)); ConfigManager.save(); }
    @Override public JsonObject saveFeature() {
        JsonObject json = super.saveFeature();
        json.add("waypoints", new Gson().toJsonTree(points.stream().filter(p -> !p.death()).toList())); return json;
    }
    @Override public void loadFeature(JsonObject json) {
        super.loadFeature(json); points.clear();
        if (json.has("waypoints") && json.get("waypoints").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("waypoints")) {
                if (points.size() >= 128) break;
                try { Waypoint p = new Gson().fromJson(element, Waypoint.class);
                    if (p != null && p.id() != null && p.world() != null && p.dimension() != null) put(p);
                } catch (RuntimeException ignored) {}
            }
        }
    }
    @Override public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) { lastLevel = mc.level; wasDead = false; }
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        boolean dead = mc.player.isDeadOrDying();
        if (dead && !wasDead && flag("death")) {
            points.removeIf(Waypoint::death);
            put(new Waypoint("last-death", "Last Death", mc.player.getX(), mc.player.getY(), mc.player.getZ(), 0xffff5555, "X", world(mc), mc.level.dimension().toString(), true));
        }
        wasDead = dead;
    }
    public static String direction(Minecraft mc, Vec3 position) {
        if (mc.player == null) return "";
        Vec3 delta = position.subtract(mc.player.position());
        double angle = Math.toDegrees(Math.atan2(-delta.x, delta.z)) - mc.player.getYRot();
        angle = ((angle + 540) % 360 + 360) % 360 - 180;
        return Math.abs(angle) < 25 ? "^" : Math.abs(angle) > 155 ? "v" : angle < 0 ? "<" : ">";
    }
    @Override public List<String> lines(Minecraft mc, boolean editor) {
        if (editor) return List.of("N  E  S  W", "^ Home 125m");
        if (mc.player == null) return List.of();
        List<String> rows = new ArrayList<>();
        if (flag("compass")) rows.add("N  E  S  W | " + mc.player.getDirection().getName());
        active(mc).stream().sorted(Comparator.comparingDouble(p -> p.position().distanceToSqr(mc.player.position()))).limit(8)
            .forEach(p -> rows.add(direction(mc, p.position()) + " " + p.icon() + " " + p.name() + " " + Math.round(p.position().distanceTo(mc.player.position())) + "m"));
        return rows;
    }
}
