package app.ezclient.gui;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shader-style Glowing Ores module.
 * Emissive vertex lighting during chunk meshing + depth-tested in-world borders on exposed faces.
 * Completely occluded by stone and walls (strictly no X-Ray).
 */
public final class GlowingOresModule extends FeatureModule {
    public record Ore(BlockPos pos, int color) {}

    private static volatile boolean emissiveActive = false;
    private static volatile boolean dynamicLightActive = false;
    private static volatile Set<Block> activeOreBlocks = Collections.emptySet();

    private volatile List<Ore> visible = List.of();
    private final List<Ore> working = new ArrayList<>();
    private int cursor;
    private BlockPos scanCenter = BlockPos.ZERO;

    public GlowingOresModule() {
        super("Glowing Ores", false, 0);
        option("Performance", "radius", "Scan radius", "Suchradius um den Spieler.", 24.0, 8, 64);
        option("Performance", "blocksPerTick", "Blocks per tick", "Begrenzt die Scan-Arbeit pro Tick.", 4096.0, 256, 16384);
        option("Performance", "maxOres", "Maximum markers", "Maximale Zahl gleichzeitig markierter Erze.", 512.0, 32, 2048);

        flag("Darstellung", "emissive", "Emissive glowing", "Lässt freiliegende Erze in dunklen Höhlen hell leuchten.", true);
        flag("Darstellung", "faceBorder", "Glowing borders", "Zeichnet sichtbare leuchtende Ränder auf freiliegende Erz-Seiten.", true);
        flag("Darstellung", "connectedBorder", "Connected veins", "Verbindet benachbarte Erze einer Ader ohne Innenränder.", true);
        flag("Darstellung", "fill", "Fill", "Füllt sichtbare Erz-Seiten transparent aus.", true);
        flag("Darstellung", "dynamicLight", "Dynamic Light", "Erhellt die Umgebung um gefundene Erze.", true);
        option("Darstellung", "lineWidth", "Line width", "Stärke der Erz-Kontur.", 3.0, 0.5, 6.0);
        option("Darstellung", "fillAlpha", "Fill opacity", "Deckkraft der Füllung.", 0.28, 0, 0.8);

        flag("Erze", "diamond", "Diamond", "Markiert Diamanterz.", true);
        flag("Erze", "emerald", "Emerald", "Markiert Smaragderz.", true);
        flag("Erze", "ancient", "Ancient debris", "Markiert Antiken Schrott.", true);
        flag("Erze", "gold", "Gold", "Markiert Golderz und Nethergolderz.", true);
        flag("Erze", "iron", "Iron", "Markiert Eisenerz.", true);
        flag("Erze", "copper", "Copper", "Markiert Kupfererz.", true);
        flag("Erze", "redstone", "Redstone", "Markiert Redstone-Erz.", true);
        flag("Erze", "lapis", "Lapis", "Markiert Lapislazuli-Erz.", true);
        flag("Erze", "coal", "Coal", "Markiert Kohleerz.", true);
        flag("Erze", "quartz", "Nether quartz", "Markiert Netherquarzerz.", true);

        colorOption("Farben", "diamondColor", "Diamond", "Farbe für Diamanterz.", "FF35E8FF");
        colorOption("Farben", "emeraldColor", "Emerald", "Farbe für Smaragderz.", "FF22E36B");
        colorOption("Farben", "ancientColor", "Ancient debris", "Farbe für Antiken Schrott.", "FF8B5A45");
        colorOption("Farben", "goldColor", "Gold", "Farbe für Golderz.", "FFFFC928");
        colorOption("Farben", "ironColor", "Iron", "Farbe für Eisenerz.", "FFD8B08C");
        colorOption("Farben", "copperColor", "Copper", "Farbe für Kupfererz.", "FFFF7A45");
        colorOption("Farben", "redstoneColor", "Redstone", "Farbe für Redstone-Erz.", "FFFF3030");
        colorOption("Farben", "lapisColor", "Lapis", "Farbe für Lapislazuli-Erz.", "FF3468FF");
        colorOption("Farben", "coalColor", "Coal", "Farbe für Kohleerz.", "FF6E7480");
        colorOption("Farben", "quartzColor", "Nether quartz", "Farbe für Netherquarzerz.", "FFEBE7E0");

        updateActiveOres();
    }

    public static boolean isEmissive(Block b) {
        return emissiveActive && activeOreBlocks.contains(b);
    }

    public static boolean isDynamicLightActive() {
        return dynamicLightActive;
    }

    public static int getNeighborEmissiveLight(net.minecraft.client.renderer.block.BlockAndTintGetter level, BlockPos pos) {
        if (!emissiveActive || !dynamicLightActive || level == null || pos == null) return 0;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            var state = level.getBlockState(pos.relative(dir));
            if (state != null && isEmissive(state.getBlock())) {
                return 11;
            }
        }
        return 0;
    }

    private void updateActiveOres() {
        emissiveActive = isEnabled() && flag("emissive");
        dynamicLightActive = isEnabled() && flag("dynamicLight");
        if (!isEnabled()) {
            activeOreBlocks = Collections.emptySet();
            return;
        }
        Set<Block> set = new HashSet<>();
        if (flag("diamond")) { set.add(Blocks.DIAMOND_ORE); set.add(Blocks.DEEPSLATE_DIAMOND_ORE); }
        if (flag("emerald")) { set.add(Blocks.EMERALD_ORE); set.add(Blocks.DEEPSLATE_EMERALD_ORE); }
        if (flag("ancient")) { set.add(Blocks.ANCIENT_DEBRIS); }
        if (flag("gold")) { set.add(Blocks.GOLD_ORE); set.add(Blocks.DEEPSLATE_GOLD_ORE); set.add(Blocks.NETHER_GOLD_ORE); }
        if (flag("iron")) { set.add(Blocks.IRON_ORE); set.add(Blocks.DEEPSLATE_IRON_ORE); }
        if (flag("copper")) { set.add(Blocks.COPPER_ORE); set.add(Blocks.DEEPSLATE_COPPER_ORE); }
        if (flag("redstone")) { set.add(Blocks.REDSTONE_ORE); set.add(Blocks.DEEPSLATE_REDSTONE_ORE); }
        if (flag("lapis")) { set.add(Blocks.LAPIS_ORE); set.add(Blocks.DEEPSLATE_LAPIS_ORE); }
        if (flag("coal")) { set.add(Blocks.COAL_ORE); set.add(Blocks.DEEPSLATE_COAL_ORE); }
        if (flag("quartz")) { set.add(Blocks.NETHER_QUARTZ_ORE); }
        activeOreBlocks = Collections.unmodifiableSet(set);
    }

    public static void reloadLevelRenderer() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.execute(() -> {
            if (client.level != null) {
                //? if >=26.2 {
                if (client.levelExtractor != null) {
                    client.levelExtractor.allChanged();
                }
                //?} else {
                /*if (client.levelRenderer != null) {
                    client.levelRenderer.allChanged();
                }
                *///?}
            }
        });
    }

    @Override
    protected void onToggle() {
        super.onToggle();
        updateActiveOres();
        visible = List.of();
        working.clear();
        cursor = 0;
        reloadLevelRenderer();
    }

    @Override
    public boolean set(Option option, Object value) {
        boolean ok = super.set(option, value);
        if (ok) {
            updateActiveOres();
            visible = List.of();
            working.clear();
            cursor = 0;
            reloadLevelRenderer();
        }
        return ok;
    }

    @Override
    public void loadFeature(JsonObject json) {
        super.loadFeature(json);
        updateActiveOres();
    }

    public List<Ore> ores() { return visible; }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) {
            visible = List.of();
            working.clear();
            cursor = 0;
            return;
        }
        int r = (int) number("radius"), side = r * 2 + 1, total = side * side * side;
        BlockPos center = mc.player.blockPosition();
        if (cursor == 0 || center.distManhattan(scanCenter) > Math.max(4, r / 4)) {
            scanCenter = center.immutable();
            working.clear();
            cursor = 0;
        }
        int budget = (int) number("blocksPerTick"), max = (int) number("maxOres");
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int n = 0; n < budget && cursor < total; n++, cursor++) {
            int i = cursor;
            int dx = i % side - r; i /= side;
            int dy = i % side - r; int dz = i / side - r;
            pos.set(scanCenter.getX() + dx, scanCenter.getY() + dy, scanCenter.getZ() + dz);
            if (working.size() >= max || !mc.level.isLoaded(pos)) continue;
            int color = oreColor(mc.level.getBlockState(pos));
            if (color != 0 && isExposed(mc, pos)) {
                working.add(new Ore(pos.immutable(), color));
            }
        }
        if (cursor >= total) {
            visible = Collections.unmodifiableList(new ArrayList<>(working));
            working.clear();
            cursor = 0;
        }
    }

    private boolean isExposed(Minecraft mc, BlockPos pos) {
        return !mc.level.getBlockState(pos.above()).canOcclude()
            || !mc.level.getBlockState(pos.below()).canOcclude()
            || !mc.level.getBlockState(pos.north()).canOcclude()
            || !mc.level.getBlockState(pos.south()).canOcclude()
            || !mc.level.getBlockState(pos.west()).canOcclude()
            || !mc.level.getBlockState(pos.east()).canOcclude();
    }

    private int oreColor(BlockState state) {
        Block b = state.getBlock();
        if (flag("diamond") && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE)) return tint("diamondColor", false);
        if (flag("emerald") && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE)) return tint("emeraldColor", false);
        if (flag("ancient") && b == Blocks.ANCIENT_DEBRIS) return tint("ancientColor", false);
        if (flag("gold") && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE || b == Blocks.NETHER_GOLD_ORE)) return tint("goldColor", false);
        if (flag("iron") && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE)) return tint("ironColor", false);
        if (flag("copper") && (b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE)) return tint("copperColor", false);
        if (flag("redstone") && (b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE)) return tint("redstoneColor", false);
        if (flag("lapis") && (b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE)) return tint("lapisColor", false);
        if (flag("coal") && (b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE)) return tint("coalColor", false);
        if (flag("quartz") && b == Blocks.NETHER_QUARTZ_ORE) return tint("quartzColor", false);
        return 0;
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/glowing_ores.png");
    }
}
