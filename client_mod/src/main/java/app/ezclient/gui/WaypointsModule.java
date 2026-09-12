package app.ezclient.gui;

import java.util.*;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class WaypointsModule extends FeatureModule {
    public static final String DEFAULT_FOLDER = "general";
    public static final List<String> ICONS = List.of("Home", "Base", "Chest", "Diamond", "Pickaxe", "Sword", "Skull", "Player", "Trap", "Star", "Flag", "Portal");
    public record BlockPosition(int x, int y, int z) { public BlockPos blockPos() { return new BlockPos(x, y, z); } }
    public record Folder(String id, String name, boolean visible, boolean collapsed) {}
    public record Waypoint(String id, String name, double x, double y, double z, int color, String icon, String world, String dimension, boolean visible, String folderId, List<BlockPosition> blocks) {
        public Waypoint {
            blocks = List.copyOf(blocks == null ? List.of() : blocks);
            if (folderId == null || folderId.isBlank()) folderId = DEFAULT_FOLDER;
        }
        public Vec3 position() { return new Vec3(x, y, z); }
        public Waypoint visible(boolean value) { return new Waypoint(id, name, x, y, z, color, icon, world, dimension, value, folderId, blocks); }
    }
    private final List<Waypoint> points = new ArrayList<>();
    private final List<Folder> folders = new ArrayList<>();
    private boolean managerDown, quickDown;

    public WaypointsModule() {
        super("Waypoints", false, 210);
        setEnabled(true);
        flag("Beschriftung", "showName", "Show Name", "Zeigt den Namen des Waypoints.", true);
        flag("Beschriftung", "showDistance", "Show Distance", "Zeigt die Entfernung zum Waypoint.", true);
        flag("Beschriftung", "showCoordinates", "Show Coordinates", "Zeigt XYZ-Koordinaten.", false);
        flag("Beschriftung", "showIcon", "Show Icon", "Zeigt das gewählte Symbol.", true);
        flag("Beschriftung", "throughWalls", "Labels Through Walls", "Hält Labels bei verdeckten Zielen sichtbar.", true);
        flag("Beschriftung", "textBackground", "Text Background", "Hintergrund für lesbare Beschriftungen.", true);
        option("Beschriftung", "backgroundAlpha", "Background Alpha", "Transparenz des Texthintergrunds.", .55, 0, 1);
        option("Darstellung", "scale", "Scale", "Gut lesbare Basisgröße von Waypoint-Labels.", 1.0, .5, 2);
        option("Darstellung", "minScale", "Min Scale", "Kleinste Labelgröße auf großer Distanz.", .85, .5, 2);
        option("Darstellung", "maxScale", "Max Scale", "Größte Labelgröße in der Nähe.", 1.15, .5, 3);
        option("Performance", "maxDistance", "Max Distance", "Maximale Renderdistanz für Waypoints (maximal = Unendlich).", 10000.0, 16, 10000);
        flag("Block-Markierung", "highlightBlocks", "Highlight Blocks", "Markiert gespeicherte Blockpositionen.", true);
        flag("Block-Markierung", "outline", "Outline", "Zeigt die Kontur markierter Blöcke.", true);
        flag("Block-Markierung", "fill", "Fill", "Füllt markierte Blöcke transparent.", true);
        option("Block-Markierung", "outlineAlpha", "Outline Alpha", "Transparenz der Blockkontur.", 1.0, 0, 1);
        option("Block-Markierung", "fillAlpha", "Fill Alpha", "Transparenz der Blockfüllung.", .20, 0, 1);
        option("Block-Markierung", "lineWidth", "Line Width", "Stärke der Blockkontur.", 1.5, 1, 5);
        flag("Block-Markierung", "highlightThroughWalls", "Highlights Through Walls", "Zeigt Markierungen durch Wände.", true);
        flag("Block-Markierung", "beam", "Beam", "Zeigt einen vertikalen Richtstrahl.", false);
        option("Block-Markierung", "beamHeight", "Beam Height", "Höhe des Richtstrahls.", 24.0, 1, 256);
        option("Block-Markierung", "beamAlpha", "Beam Alpha", "Transparenz des Richtstrahls.", .25, 0, 1);
        folders.add(new Folder(DEFAULT_FOLDER, "GENERAL", true, false));
        setKeyBind(GLFW.GLFW_KEY_M);
    }

    public static String world(Minecraft mc) {
        if (mc == null) return "default";
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);
        if (mc.getSingleplayerServer() != null) {
            try {
                return "local:" + mc.getSingleplayerServer().getWorldData().getLevelName().trim().toLowerCase(Locale.ROOT);
            } catch (Throwable t) {
                return "local:world";
            }
        }
        if (mc.level != null) {
            try {
                return "level:" + mc.level.dimension().identifier().toString();
            } catch (Throwable ignored) {}
        }
        return "default";
    }

    public static boolean matchesWorld(String savedWorld, String currentWorld, Minecraft mc) {
        if (savedWorld == null || savedWorld.isBlank() || "default".equalsIgnoreCase(savedWorld)) return true;
        if (currentWorld == null || currentWorld.isBlank() || "default".equalsIgnoreCase(currentWorld)) return true;
        if (savedWorld.equalsIgnoreCase(currentWorld)) return true;
        String sClean = savedWorld.toLowerCase(Locale.ROOT).replace("server:", "").trim();
        String cClean = currentWorld.toLowerCase(Locale.ROOT).replace("server:", "").trim();
        if (sClean.equals(cClean)) return true;
        if (sClean.split(":")[0].equals(cClean.split(":")[0])) return true;
        if (mc != null && mc.getCurrentServer() != null) {
            String ip = mc.getCurrentServer().ip.toLowerCase(Locale.ROOT).trim();
            if (sClean.equals(ip) || sClean.equals(ip.split(":")[0])) return true;
        }
        return false;
    }

    public static boolean matchesDimension(String savedDim, String currentDim) {
        if (savedDim == null || savedDim.isBlank()) return true;
        if (savedDim.equalsIgnoreCase(currentDim)) return true;
        String s = cleanDim(savedDim);
        String c = cleanDim(currentDim);
        return s.equals(c) || s.endsWith(c) || c.endsWith(s);
    }

    private static String cleanDim(String dim) {
        if (dim == null) return "";
        String s = dim.toLowerCase(Locale.ROOT);
        int slash = s.lastIndexOf('/');
        if (slash >= 0) s = s.substring(slash + 1);
        int bracket = s.lastIndexOf(']');
        if (bracket >= 0) s = s.substring(0, bracket);
        return s.replace("minecraft:", "").replace("dim-1", "the_nether").replace("dim1", "the_end").replace("dim0", "overworld").replace("-1", "the_nether").replace("1", "the_end").replace("0", "overworld").trim();
    }

    public List<Waypoint> points() { return List.copyOf(points); }
    public List<Folder> folders() { return List.copyOf(folders); }

    public static int randomColor() {
        int[] colors = {0xFFFF4757, 0xFFFFA502, 0xFFECCC68, 0xFF2ED573, 0xFF1E90FF, 0xFF70A1FF, 0xFF9B59B6, 0xFFFF6B81};
        return colors[java.util.concurrent.ThreadLocalRandom.current().nextInt(colors.length)];
    }

    public Folder folder(String id) {
        String searchId = (id == null || id.isBlank()) ? DEFAULT_FOLDER : id;
        return folders.stream().filter(f -> f.id().equals(searchId)).findFirst().orElse(folders.isEmpty() ? new Folder(DEFAULT_FOLDER, "GENERAL", true, false) : folders.getFirst());
    }

    public static String cleanWaypointName(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        // Remove banner prefixes in brackets: [white banner], [White Banner], [white_banner], [banner], etc.
        s = s.replaceAll("(?i)^\\[(?:[a-z_\\s]+_)?banner\\]\\s*", "");
        // Remove German banner prefixes in brackets: [weißes banner], [weisses banner], etc.
        s = s.replaceAll("(?i)^\\[[a-zäöü_\\s]+banner\\]\\s*", "");
        // Remove Xaero localization keys
        if (s.equalsIgnoreCase("gui.xaero_deathpoint")) return "Deathpoint";
        if (s.toLowerCase(Locale.ROOT).startsWith("gui.xaero_")) {
            s = s.substring(10).replace('_', ' ').trim();
            s = s.replaceAll("(?i)^([a-z_\\s]+_)?banner\\s*", "").trim();
        }
        s = s.trim();
        return s.isEmpty() ? "Waypoint" : s;
    }

    public List<Waypoint> active(Minecraft mc) {
        if (mc == null || mc.level == null) return List.of();
        String w = world(mc), d = mc.level.dimension().identifier().toString();
        return points.stream().filter(p -> p.visible()
                && matchesWorld(p.world(), w, mc)
                && matchesDimension(p.dimension(), d)
                && folder(p.folderId()).visible()).toList();
    }

    public boolean put(Waypoint p) {
        if (!valid(p)) return false;
        String clean = cleanWaypointName(p.name());
        final Waypoint toAdd = clean.equals(p.name()) ? p :
                new Waypoint(p.id(), clean, p.x(), p.y(), p.z(), p.color(), p.icon(), p.world(), p.dimension(), p.visible(), p.folderId(), p.blocks());
        points.removeIf(old -> old.id().equals(toAdd.id()));
        if (points.size() >= 4096) return false;
        points.add(toAdd);
        setEnabled(true);
        save();
        return true;
    }

    private static net.minecraft.world.item.ItemStack fromId(String idStr) {
        try {
            Identifier id = Identifier.tryParse(idStr);
            if (id != null) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    return new net.minecraft.world.item.ItemStack(item);
                }
            }
        } catch (Throwable ignored) {}
        return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND);
    }

    public static net.minecraft.world.item.ItemStack resolveItemStack(String icon) {
        if (icon == null || icon.isBlank()) return fromId("minecraft:red_bed");
        switch (icon) {
            case "Star" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR); }
            case "Home" -> { return fromId("minecraft:red_bed"); }
            case "Base" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEACON); }
            case "Chest" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST); }
            case "Diamond" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND); }
            case "Pickaxe" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE); }
            case "Sword" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD); }
            case "Skull" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SKELETON_SKULL); }
            case "Player" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD); }
            case "Trap" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TNT); }
            case "Flag" -> { return fromId("minecraft:white_banner"); }
            case "Portal" -> { return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_EYE); }
        }
        try {
            Identifier id = Identifier.tryParse(icon.contains(":") ? icon : "minecraft:" + icon.toLowerCase(Locale.ROOT));
            if (id != null) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    return new net.minecraft.world.item.ItemStack(item);
                }
            }
        } catch (Throwable ignored) {}
        return fromId("minecraft:red_bed");
    }

    private boolean valid(Waypoint p) {
        return p != null && p.id() != null && p.name() != null && !p.name().isBlank() && p.name().length() <= 80
                && p.icon() != null && !p.icon().isBlank() && p.icon().length() <= 120
                && p.world() != null && p.dimension() != null
                && Double.isFinite(p.x()) && Double.isFinite(p.y()) && Double.isFinite(p.z())
                && Math.abs(p.x()) <= 30_000_000 && Math.abs(p.z()) <= 30_000_000 && Math.abs(p.y()) <= 2048
                && p.blocks().size() <= 4096;
    }

    public void delete(String id) { points.removeIf(p -> p.id().equals(id)); save(); }

    public void toggleWaypoint(String id) {
        points.stream().filter(p -> p.id().equals(id)).findFirst().ifPresent(p -> put(p.visible(!p.visible())));
    }

    public void moveToFolder(String waypointId, String targetFolderId) {
        if (folders.stream().noneMatch(folder -> folder.id().equals(targetFolderId))) return;
        for (int i = 0; i < points.size(); i++) {
            Waypoint p = points.get(i);
            if (p.id().equals(waypointId)) {
                points.set(i, new Waypoint(p.id(), p.name(), p.x(), p.y(), p.z(), p.color(), p.icon(),
                    p.world(), p.dimension(), p.visible(), targetFolderId, p.blocks()));
                save();
                return;
            }
        }
    }

    public void addFolder(String name) {
        if (name != null && !name.isBlank() && name.length() <= 40 && folders.size() < 64) {
            folders.add(new Folder(UUID.randomUUID().toString(), name.trim(), true, false));
            save();
        }
    }

    public void toggleFolder(String id, boolean collapse) {
        for (int i = 0; i < folders.size(); i++) {
            Folder f = folders.get(i);
            if (f.id().equals(id)) {
                folders.set(i, collapse ? new Folder(f.id(), f.name(), f.visible(), !f.collapsed()) : new Folder(f.id(), f.name(), !f.visible(), f.collapsed()));
                save();
                return;
            }
        }
    }

    public void deleteFolder(String id) {
        if (DEFAULT_FOLDER.equals(id)) return;
        points.replaceAll(p -> p.folderId().equals(id) ? new Waypoint(p.id(), p.name(), p.x(), p.y(), p.z(), p.color(), p.icon(), p.world(), p.dimension(), p.visible(), DEFAULT_FOLDER, p.blocks()) : p);
        folders.removeIf(f -> f.id().equals(id));
        save();
    }

    public Waypoint quick(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) return null;
        Waypoint p = new Waypoint(UUID.randomUUID().toString(), "Waypoint " + (points.size() + 1), Math.floor(mc.player.getX()), Math.floor(mc.player.getY()), Math.floor(mc.player.getZ()), randomColor(), "Flag", world(mc), mc.level.dimension().identifier().toString(), true, DEFAULT_FOLDER, List.of());
        put(p);
        setEnabled(true);
        return p;
    }

    private void save() {
        if (!ConfigManager.isLoading()) ConfigManager.save();
    }

    @Override
    public JsonObject saveFeature() {
        JsonObject o = super.saveFeature();
        Gson g = new Gson();
        o.add("waypoints", g.toJsonTree(points));
        o.add("folders", g.toJsonTree(folders));
        return o;
    }

    @Override
    public void loadFeature(JsonObject o) {
        boolean legacyScale = hasLegacyScaleDefaults(o);
        super.loadFeature(o);
        if (legacyScale) migrateLegacyScaleDefaults();
        points.clear();
        folders.clear();
        Gson g = new Gson();
        try {
            if (o.has("folders")) {
                for (JsonElement e : o.getAsJsonArray("folders")) {
                    Folder f = g.fromJson(e, Folder.class);
                    if (f != null && f.id() != null && f.name() != null) folders.add(f);
                }
            }
        } catch (RuntimeException ignored) {}
        if (folders.stream().noneMatch(f -> DEFAULT_FOLDER.equals(f.id()))) {
            folders.add(new Folder(DEFAULT_FOLDER, "GENERAL", true, false));
        }
        try {
            if (o.has("waypoints")) {
                for (JsonElement e : o.getAsJsonArray("waypoints")) {
                    Waypoint p = g.fromJson(e, Waypoint.class);
                    if (valid(p) && points.size() < 4096) {
                        String clean = cleanWaypointName(p.name());
                        if (!clean.equals(p.name())) {
                            p = new Waypoint(p.id(), clean, p.x(), p.y(), p.z(), p.color(), p.icon(), p.world(), p.dimension(), p.visible(), p.folderId(), p.blocks());
                        }
                        points.add(p);
                    }
                }
            }
        } catch (RuntimeException ignored) {}
        if (!points.isEmpty()) {
            setEnabled(true);
        }
        try {
            if (o.has("maxDistance") && Math.abs(o.get("maxDistance").getAsDouble() - 4096.0) <= 0.001) {
                set(options().stream().filter(op -> op.key().equals("maxDistance")).findFirst().orElse(null), 10000.0);
            }
        } catch (RuntimeException ignored) {}
    }

    private static boolean hasLegacyScaleDefaults(JsonObject json) {
        try {
            return Math.abs(json.get("scale").getAsDouble() - .32) <= .001
                    && Math.abs(json.get("minScale").getAsDouble() - .20) <= .001
                    && Math.abs(json.get("maxScale").getAsDouble() - .70) <= .001;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void migrateLegacyScaleDefaults() {
        for (Option option : options()) {
            switch (option.key()) {
                case "scale" -> set(option, 1.0);
                case "minScale" -> set(option, .85);
                case "maxScale" -> set(option, 1.15);
            }
        }
    }

    @Override
    public void setKeyBind(int key) {
        super.setKeyBind(key);
        EzKeyBindings.setKeyCode(EzKeyBindings.KEY_WAYPOINT_MANAGER, key);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null || mc.player == null) return;
        // Primary key handling is processed via KeyMapping.consumeClick() in EzClientMod.
        // Fallback for when KeyMapping is unavailable:
        if (EzKeyBindings.KEY_WAYPOINT_MANAGER == null) {
            int mKey = getKeyBind();
            boolean m = mKey != -1 && app.ezclient.EzClientMod.isKeyOrMouseDown(mc.getWindow(), mKey);
            if (m && !managerDown && EzScreenBridge.current(mc) == null && !BlockSelectionOverlay.isActive()) {
                EzScreenBridge.set(mc, new WaypointScreen(null, this));
            }
            managerDown = m;
        }
        if (EzKeyBindings.KEY_QUICK_WAYPOINT == null) {
            int qKey = GLFW.GLFW_KEY_B;
            boolean q = app.ezclient.EzClientMod.isKeyOrMouseDown(mc.getWindow(), qKey);
            if (q && !quickDown && EzScreenBridge.current(mc) == null && !BlockSelectionOverlay.isActive()) {
                quick(mc);
            }
            quickDown = q;
        }
    }

    /** Reused by directional HUD features without allocating per frame. */
    public static String direction(Minecraft mc, Vec3 position) {
        if (mc == null || mc.player == null || position == null) return "";
        Vec3 delta = position.subtract(mc.player.position());
        double angle = Math.toDegrees(Math.atan2(-delta.x, delta.z)) - mc.player.getYRot();
        angle = ((angle + 540) % 360 + 360) % 360 - 180;
        return Math.abs(angle) < 25 ? "^" : Math.abs(angle) > 155 ? "v" : angle < 0 ? "<" : ">";
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/waypoints.png");
    }
}
