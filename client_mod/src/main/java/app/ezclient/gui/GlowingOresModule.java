package app.ezclient.gui;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Pixel-texture based glowing ores.
 *
 * <p>The glow layer is emitted by {@code GlowingOreModel}. This module owns only the
 * configuration snapshot used while chunk meshes are built. It never scans for or renders ores
 * through walls.</p>
 */
public final class GlowingOresModule extends FeatureModule {
    /** Kept temporarily for binary/source compatibility with the retired gizmo renderer. */
    public record Ore(BlockPos pos, int color) {}

    private enum OreKind {
        DIAMOND("diamondColor"), EMERALD("emeraldColor"), ANCIENT("ancientColor"),
        GOLD("goldColor"), IRON("ironColor"), COPPER("copperColor"),
        REDSTONE("redstoneColor"), LAPIS("lapisColor"), COAL("coalColor"),
        QUARTZ("quartzColor");

        private final String colorKey;

        OreKind(String colorKey) {
            this.colorKey = colorKey;
        }
    }

    private static final Map<Block, OreKind> ALL_ORES = createOreMap();
    private static volatile Map<Block, OreKind> activeOres = Collections.emptyMap();
    private static volatile boolean pixelOutlineActive;
    private static volatile boolean connectedVeinsActive;
    private static volatile boolean orePixelsActive;
    private static volatile boolean emissiveActive;
    private static volatile boolean dynamicLightActive;
    private static volatile boolean customColorsActive;
    private static volatile int dynamicLightLevel = 15;
    private static volatile int dynamicLightRadius = 6;
    private static volatile int glowStrength = 100;
    private static volatile Map<OreKind, Integer> colors = Collections.emptyMap();

    public GlowingOresModule() {
        super("Glowing Ores", false, 0);

        flag("Aussehen", "pixelOutline", "1 px Pixel outline",
            "Zeichnet einen echten ein Pixel breiten Farbrand direkt auf die Erztextur.", true);
        flag("Aussehen", "connectedBorder", "Connected veins",
            "Entfernt Innenränder zwischen zusammenhängenden Blöcken derselben Erzader.", true);
        flag("Aussehen", "orePixels", "Glowing ore pixels",
            "Lässt zusätzlich die farbigen Erzadern innerhalb der Textur leuchten.", true);

        flag("Leuchten", "emissive", "Shader-like glow",
            "Rendert Farbrand und Erzpixel selbstleuchtend; Shader können daraus Bloom erzeugen.", true);
        option("Leuchten", "glowStrength", "Glow strength",
            "Helligkeit der leuchtenden Texturpixel.", 100.0, 25, 100);
        flag("Leuchten", "dynamicLight", "Light surroundings",
            "Erzeugt einen hellen Lichtschein um tatsächlich freiliegende Erze.", true);
        option("Leuchten", "lightLevel", "Surrounding light",
            "Maximale Lichtstärke direkt am Erz.", 15.0, 1, 15);
        option("Leuchten", "lightRadius", "Light radius",
            "Reichweite des Lichtscheins um freiliegende Erze.", 6.0, 1, 10);

        flag("Erze", "diamond", "Diamond", "Diamanterz einschließen.", true);
        flag("Erze", "emerald", "Emerald", "Smaragderz einschließen.", true);
        flag("Erze", "ancient", "Ancient debris", "Antiken Schrott einschließen.", true);
        flag("Erze", "gold", "Gold", "Gold- und Nethergolderz einschließen.", true);
        flag("Erze", "iron", "Iron", "Eisenerz einschließen.", true);
        flag("Erze", "copper", "Copper", "Kupfererz einschließen.", true);
        flag("Erze", "redstone", "Redstone", "Redstone-Erz einschließen.", true);
        flag("Erze", "lapis", "Lapis", "Lapislazuli-Erz einschließen.", true);
        flag("Erze", "coal", "Coal", "Kohleerz einschließen.", true);
        flag("Erze", "quartz", "Nether quartz", "Netherquarzerz einschließen.", true);

        flag("Erweitert – Farben", "customColors", "Tint original colors",
            "Mischt eigene Farben in die mehrfarbigen Originaltexturen. Aus behält die authentische Palette.", false);
        colorOption("Erweitert – Farben", "diamondColor", "Diamond", "Farbton für Diamanterz.", "FF35E8FF");
        colorOption("Erweitert – Farben", "emeraldColor", "Emerald", "Farbton für Smaragderz.", "FF22E36B");
        colorOption("Erweitert – Farben", "ancientColor", "Ancient debris", "Farbton für Antiken Schrott.", "FFB8795C");
        colorOption("Erweitert – Farben", "goldColor", "Gold", "Farbton für Golderz.", "FFFFC928");
        colorOption("Erweitert – Farben", "ironColor", "Iron", "Farbton für Eisenerz.", "FFFFE2BF");
        colorOption("Erweitert – Farben", "copperColor", "Copper", "Farbton für Kupfererz.", "FFFF8A55");
        colorOption("Erweitert – Farben", "redstoneColor", "Redstone", "Farbton für Redstone-Erz.", "FFFF3030");
        colorOption("Erweitert – Farben", "lapisColor", "Lapis", "Farbton für Lapislazuli-Erz.", "FF4A7DFF");
        colorOption("Erweitert – Farben", "coalColor", "Coal", "Farbton für Kohleerz.", "FFB9C0CB");
        colorOption("Erweitert – Farben", "quartzColor", "Nether quartz", "Farbton für Netherquarzerz.", "FFFFF4E6");

        updateRenderState();
    }

    private static Map<Block, OreKind> createOreMap() {
        Map<Block, OreKind> result = new HashMap<>();
        put(result, OreKind.DIAMOND, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        put(result, OreKind.EMERALD, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        put(result, OreKind.ANCIENT, Blocks.ANCIENT_DEBRIS);
        put(result, OreKind.GOLD, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        put(result, OreKind.IRON, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        put(result, OreKind.COPPER, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        put(result, OreKind.REDSTONE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        put(result, OreKind.LAPIS, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        put(result, OreKind.COAL, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        put(result, OreKind.QUARTZ, Blocks.NETHER_QUARTZ_ORE);
        return Collections.unmodifiableMap(result);
    }

    private static void put(Map<Block, OreKind> map, OreKind kind, Block... blocks) {
        for (Block block : blocks) map.put(block, kind);
    }

    public static boolean isKnownOre(Block block) {
        return ALL_ORES.containsKey(block);
    }

    public static boolean isActive(Block block) {
        return activeOres.containsKey(block);
    }

    public static boolean connects(Block first, Block second) {
        OreKind kind = activeOres.get(first);
        return kind != null && kind == activeOres.get(second);
    }

    public static boolean pixelOutline() {
        return pixelOutlineActive;
    }

    public static boolean connectedVeins() {
        return connectedVeinsActive;
    }

    public static boolean glowingOrePixels() {
        return orePixelsActive;
    }

    public static boolean emissive() {
        return emissiveActive;
    }

    public static boolean isDynamicLightActive() {
        return dynamicLightActive;
    }

    /** The pixel model no longer needs a CPU-side ore scan. */
    public java.util.List<Ore> ores() {
        return java.util.List.of();
    }

    public static int overlayColor(Block block) {
        OreKind kind = activeOres.get(block);
        int strength = Math.max(0, Math.min(255, Math.round(glowStrength * 2.55f)));
        int brightness = 0xFF000000 | strength << 16 | strength << 8 | strength;
        if (!customColorsActive || kind == null) return brightness;

        int tint = colors.getOrDefault(kind, 0xFFFFFFFF) | 0xFF000000;
        int red = ((tint >> 16) & 0xFF) * strength / 255;
        int green = ((tint >> 8) & 0xFF) * strength / 255;
        int blue = (tint & 0xFF) * strength / 255;
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    public static int getNeighborEmissiveLight(net.minecraft.client.renderer.block.BlockAndTintGetter level, BlockPos pos) {
        if (!dynamicLightActive || level == null || pos == null) return 0;
        BlockPos.MutableBlockPos orePos = new BlockPos.MutableBlockPos();
        // Search nearest-first on Manhattan shells. This follows Minecraft-style light falloff and
        // avoids scanning every point in a large cube while chunk meshes are built.
        for (int distance = 1; distance <= dynamicLightRadius; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                int yAndZ = distance - Math.abs(dx);
                for (int dy = -yAndZ; dy <= yAndZ; dy++) {
                    int absZ = yAndZ - Math.abs(dy);
                    if (isExposedActiveOre(level, orePos,
                        pos.getX() + dx, pos.getY() + dy, pos.getZ() + absZ)
                        || absZ != 0 && isExposedActiveOre(level, orePos,
                            pos.getX() + dx, pos.getY() + dy, pos.getZ() - absZ)) {
                        return Math.max(1, dynamicLightLevel - (distance - 1) * 2);
                    }
                }
            }
        }
        return 0;
    }

    private static boolean isExposedActiveOre(net.minecraft.client.renderer.block.BlockAndTintGetter level,
                                               BlockPos.MutableBlockPos pos, int x, int y, int z) {
        pos.set(x, y, z);
        var state = level.getBlockState(pos);
        if (state == null || !isActive(state.getBlock())) return false;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).canOcclude()) return true;
        }
        return false;
    }

    private void updateRenderState() {
        boolean enabled = isEnabled();
        pixelOutlineActive = enabled && flag("pixelOutline");
        connectedVeinsActive = enabled && flag("connectedBorder");
        orePixelsActive = enabled && flag("orePixels");
        emissiveActive = enabled && flag("emissive");
        dynamicLightActive = enabled && flag("dynamicLight");
        customColorsActive = enabled && flag("customColors");
        glowStrength = (int) Math.round(number("glowStrength"));
        dynamicLightLevel = (int) Math.round(number("lightLevel"));
        dynamicLightRadius = (int) Math.round(number("lightRadius"));

        if (!enabled) {
            activeOres = Collections.emptyMap();
            colors = Collections.emptyMap();
            return;
        }

        Map<Block, OreKind> selected = new HashMap<>();
        addIfEnabled(selected, "diamond", OreKind.DIAMOND);
        addIfEnabled(selected, "emerald", OreKind.EMERALD);
        addIfEnabled(selected, "ancient", OreKind.ANCIENT);
        addIfEnabled(selected, "gold", OreKind.GOLD);
        addIfEnabled(selected, "iron", OreKind.IRON);
        addIfEnabled(selected, "copper", OreKind.COPPER);
        addIfEnabled(selected, "redstone", OreKind.REDSTONE);
        addIfEnabled(selected, "lapis", OreKind.LAPIS);
        addIfEnabled(selected, "coal", OreKind.COAL);
        addIfEnabled(selected, "quartz", OreKind.QUARTZ);
        activeOres = Collections.unmodifiableMap(selected);

        Map<OreKind, Integer> selectedColors = new HashMap<>();
        for (OreKind kind : OreKind.values()) selectedColors.put(kind, tint(kind.colorKey, false));
        colors = Collections.unmodifiableMap(selectedColors);
    }

    private void addIfEnabled(Map<Block, OreKind> selected, String setting, OreKind kind) {
        if (!flag(setting)) return;
        ALL_ORES.forEach((block, candidate) -> {
            if (candidate == kind) selected.put(block, kind);
        });
    }

    public static void reloadLevelRenderer() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(() -> {
            if (client.level == null) return;
            //? if >=26.2 {
            if (client.levelExtractor != null) client.levelExtractor.allChanged();
            //?} else {
            /*if (client.levelRenderer != null) client.levelRenderer.allChanged();
            *///?}
        });
    }

    @Override
    protected void onToggle() {
        super.onToggle();
        updateRenderState();
        reloadLevelRenderer();
    }

    @Override
    public boolean set(Option option, Object value) {
        boolean changed = super.set(option, value);
        if (changed) {
            updateRenderState();
            reloadLevelRenderer();
        }
        return changed;
    }

    @Override
    public void loadFeature(JsonObject json) {
        boolean migrateDirectNeighborLight = !json.has("lightRadius");
        super.loadFeature(json);
        if (migrateDirectNeighborLight) {
            options().stream()
                .filter(option -> option.key().equals("lightLevel"))
                .findFirst()
                .ifPresent(option -> super.set(option, 15.0));
        }
        updateRenderState();
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/glowing_ores.png");
    }
}
