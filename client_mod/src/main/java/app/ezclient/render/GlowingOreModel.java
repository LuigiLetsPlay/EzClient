package app.ezclient.render;

import app.ezclient.gui.GlowingOresModule;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Adds a cutout glow texture to vanilla ore models through Fabric's renderer API.
 * The center and each one-pixel edge are emitted separately so internal vein seams can be removed
 * without drawing geometric wireframes or exposing covered blocks.
 */
public final class GlowingOreModel implements BlockStateModel {
    private static final float PIXEL = 1.0f / 16.0f;
    private static final float LAYER_OFFSET = 0.001f;
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final ThreadLocal<ArrayList<BlockStateModelPart>> PART_BUFFER =
        ThreadLocal.withInitial(() -> new ArrayList<>(1));
    private static final Map<Block, String> TEXTURES = createTextureMap();

    private final BlockStateModel delegate;
    private final Material.Baked sideGlow;
    private final Material.Baked topGlow;

    private GlowingOreModel(BlockStateModel delegate, Material.Baked sideGlow, Material.Baked topGlow) {
        this.delegate = delegate;
        this.sideGlow = sideGlow;
        this.topGlow = topGlow;
    }

    public static void register() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register(
            ModelModifier.WRAP_PHASE,
            (model, bakeContext) -> {
                Block block = bakeContext.state().getBlock();
                String texture = TEXTURES.get(block);
                if (texture == null) return model;

                Material.Baked side = bakeGlowMaterial(bakeContext.baker(), texture);
                Material.Baked top = block == Blocks.ANCIENT_DEBRIS
                    ? bakeGlowMaterial(bakeContext.baker(), "ancient_debris_top")
                    : side;
                return new GlowingOreModel(model, side, top);
            }
        ));
    }

    private static Material.Baked bakeGlowMaterial(net.minecraft.client.resources.model.ModelBaker baker, String texture) {
        Identifier id = Identifier.fromNamespaceAndPath("ezclient", "block/glowing_ores/" + texture + "_e");
        return baker.materials().get(new Material(id), () -> "EzClient glowing ore " + id);
    }

    private static Map<Block, String> createTextureMap() {
        Map<Block, String> result = new IdentityHashMap<>();
        result.put(Blocks.COAL_ORE, "coal_ore");
        result.put(Blocks.DEEPSLATE_COAL_ORE, "deepslate_coal_ore");
        result.put(Blocks.COPPER_ORE, "copper_ore");
        result.put(Blocks.DEEPSLATE_COPPER_ORE, "deepslate_copper_ore");
        result.put(Blocks.DIAMOND_ORE, "diamond_ore");
        result.put(Blocks.DEEPSLATE_DIAMOND_ORE, "deepslate_diamond_ore");
        result.put(Blocks.EMERALD_ORE, "emerald_ore");
        result.put(Blocks.DEEPSLATE_EMERALD_ORE, "deepslate_emerald_ore");
        result.put(Blocks.GOLD_ORE, "gold_ore");
        result.put(Blocks.DEEPSLATE_GOLD_ORE, "deepslate_gold_ore");
        result.put(Blocks.IRON_ORE, "iron_ore");
        result.put(Blocks.DEEPSLATE_IRON_ORE, "deepslate_iron_ore");
        result.put(Blocks.LAPIS_ORE, "lapis_ore");
        result.put(Blocks.DEEPSLATE_LAPIS_ORE, "deepslate_lapis_ore");
        result.put(Blocks.REDSTONE_ORE, "redstone_ore");
        result.put(Blocks.DEEPSLATE_REDSTONE_ORE, "deepslate_redstone_ore");
        result.put(Blocks.NETHER_GOLD_ORE, "nether_gold_ore");
        result.put(Blocks.NETHER_QUARTZ_ORE, "nether_quartz_ore");
        result.put(Blocks.ANCIENT_DEBRIS, "ancient_debris_side");
        return result;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        delegate.collectParts(random, output);
    }

    @Override
    public Material.Baked particleMaterial() {
        return delegate.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return delegate.materialFlags();
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        if (!GlowingOresModule.isActive(state.getBlock())) return null;
        int mask = 0;
        Direction[] directions = Direction.values();
        if (GlowingOresModule.connectedVeins()) {
            for (int i = 0; i < directions.length; i++) {
                if (GlowingOresModule.connects(
                    state.getBlock(), level.getBlockState(pos.relative(directions[i])).getBlock())) {
                    mask |= 1 << i;
                }
            }
        }
        if (GlowingOresModule.pixelOutline()) mask |= 1 << 6;
        if (GlowingOresModule.glowingOrePixels()) mask |= 1 << 7;
        if (GlowingOresModule.emissive()) mask |= 1 << 8;
        return mask;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
                          BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        if (!GlowingOresModule.isActive(state.getBlock())) {
            BlockStateModel.super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        ArrayList<BlockStateModelPart> parts = PART_BUFFER.get();
        parts.clear();
        delegate.collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            TriState ambientOcclusion = part.useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
            if (!cullTest.test(null)) {
                emitQuads(part.getQuads(null), null, ambientOcclusion, emitter, level, pos, state);
            }
            for (Direction face : Direction.values()) {
                if (!cullTest.test(face)) {
                    emitQuads(part.getQuads(face), face, ambientOcclusion, emitter, level, pos, state);
                }
            }
        }
        parts.clear();
    }

    private void emitQuads(List<BakedQuad> quads, Direction cullFace, TriState ambientOcclusion,
                           QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state) {
        for (BakedQuad quad : quads) {
            emitter.cullFace(cullFace);
            emitter.fromBakedQuad(quad);
            emitter.ambientOcclusion(ambientOcclusion);
            emitter.shadeMode(ShadeMode.VANILLA);
            emitter.emit();

            Direction face = quad.direction();
            if (face == null) continue;
            QuadGeometry geometry = QuadGeometry.of(quad);
            Material.Baked glow = face.getAxis() == Direction.Axis.Y ? topGlow : sideGlow;

            Direction edgeUmin = geometry.edgeDirection(face, 0.5f * PIXEL, 0.5f);
            Direction edgeUmax = geometry.edgeDirection(face, 1.0f - 0.5f * PIXEL, 0.5f);
            Direction edgeVmin = geometry.edgeDirection(face, 0.5f, 0.5f * PIXEL);
            Direction edgeVmax = geometry.edgeDirection(face, 0.5f, 1.0f - 0.5f * PIXEL);

            boolean connected = GlowingOresModule.connectedVeins();
            boolean hasUmin = connected && connectedOnFace(level, pos, state, edgeUmin, face);
            boolean hasUmax = connected && connectedOnFace(level, pos, state, edgeUmax, face);
            boolean hasVmin = connected && connectedOnFace(level, pos, state, edgeVmin, face);
            boolean hasVmax = connected && connectedOnFace(level, pos, state, edgeVmax, face);

            boolean outline = GlowingOresModule.pixelOutline();
            boolean orePixels = GlowingOresModule.glowingOrePixels();
            if (outline && orePixels && !hasUmin && !hasUmax && !hasVmin && !hasVmax) {
                // The common case stays one continuous quad: no 1 px seams and fewer vertices.
                emitGlowRegion(quad, geometry, cullFace, emitter, glow, state,
                    0.0f, 0.0f, 1.0f, 1.0f);
                continue;
            }
            if (orePixels) {
                emitGlowRegion(quad, geometry, cullFace, emitter, glow, state,
                    PIXEL, PIXEL, 1.0f - PIXEL, 1.0f - PIXEL);
            }
            if (!outline) continue;

            if (!hasUmin) {
                emitGlowRegion(quad, geometry, cullFace, emitter, glow, state,
                    0.0f, hasVmin ? 0.0f : PIXEL, PIXEL, hasVmax ? 1.0f : 1.0f - PIXEL);
            }
            if (!hasUmax) {
                emitGlowRegion(quad, geometry, cullFace, emitter, glow, state,
                    1.0f - PIXEL, hasVmin ? 0.0f : PIXEL, 1.0f, hasVmax ? 1.0f : 1.0f - PIXEL);
            }
            if (!hasVmin) {
                emitGlowRegion(quad, geometry, cullFace, emitter, glow, state,
                    0.0f, 0.0f, 1.0f, PIXEL);
            }
            if (!hasVmax) {
                emitGlowRegion(quad, geometry, cullFace, emitter, glow, state,
                    0.0f, 1.0f - PIXEL, 1.0f, 1.0f);
            }
        }
    }

    private static boolean connectedOnFace(BlockAndTintGetter level, BlockPos pos, BlockState state,
                                           Direction neighborDirection, Direction face) {
        if (neighborDirection == null) return false;
        BlockPos neighborPos = pos.relative(neighborDirection);
        if (!GlowingOresModule.connects(state.getBlock(), level.getBlockState(neighborPos).getBlock())) {
            return false;
        }
        return !level.getBlockState(neighborPos.relative(face)).canOcclude();
    }

    private static void emitGlowRegion(BakedQuad quad, QuadGeometry geometry, Direction cullFace,
                                       QuadEmitter emitter, Material.Baked material, BlockState state,
                                       float u0, float v0, float u1, float v1) {
        if (u1 <= u0 || v1 <= v0) return;
        emitter.cullFace(cullFace);
        emitter.fromBakedQuad(quad);
        Direction face = quad.direction();
        float offsetX = face == null ? 0.0f : face.getStepX() * LAYER_OFFSET;
        float offsetY = face == null ? 0.0f : face.getStepY() * LAYER_OFFSET;
        float offsetZ = face == null ? 0.0f : face.getStepZ() * LAYER_OFFSET;
        for (int vertex = 0; vertex < 4; vertex++) {
            float targetU = geometry.vertexAtMinU(vertex) ? u0 : u1;
            float targetV = geometry.vertexAtMinV(vertex) ? v0 : v1;
            emitter.pos(vertex,
                geometry.position(targetU, targetV, 0) + offsetX,
                geometry.position(targetU, targetV, 1) + offsetY,
                geometry.position(targetU, targetV, 2) + offsetZ);
            emitter.uv(vertex, targetU, targetV);
        }
        emitter.materialBake(material, MutableQuadView.BAKE_NORMALIZED);
        int color = GlowingOresModule.overlayColor(state.getBlock());
        emitter.color(color, color, color, color);
        emitter.tintIndex(-1);
        emitter.emissive(GlowingOresModule.emissive());
        emitter.diffuseShade(!GlowingOresModule.emissive());
        emitter.ambientOcclusion(GlowingOresModule.emissive() ? TriState.FALSE : TriState.DEFAULT);
        emitter.shadeMode(ShadeMode.VANILLA);
        if (GlowingOresModule.emissive()) emitter.minLightmap(FULL_BRIGHT);
        emitter.emit();
    }

    private static final class QuadGeometry {
        private final float[][] positions = new float[4][3];
        private final float[] us = new float[4];
        private final float[] vs = new float[4];
        private final int i00;
        private final int i10;
        private final int i01;
        private final int i11;

        private QuadGeometry(BakedQuad quad) {
            TextureAtlasSprite sprite = quad.materialInfo().sprite();
            float du = sprite.getU1() - sprite.getU0();
            float dv = sprite.getV1() - sprite.getV0();
            int c00 = 0, c10 = 0, c01 = 0, c11 = 0;
            for (int i = 0; i < 4; i++) {
                positions[i][0] = quad.position(i).x();
                positions[i][1] = quad.position(i).y();
                positions[i][2] = quad.position(i).z();
                us[i] = du == 0.0f ? 0.0f : (UVPair.unpackU(quad.packedUV(i)) - sprite.getU0()) / du;
                vs[i] = dv == 0.0f ? 0.0f : (UVPair.unpackV(quad.packedUV(i)) - sprite.getV0()) / dv;
                boolean lowU = vertexAtMinU(i);
                boolean lowV = vertexAtMinV(i);
                if (lowU && lowV) c00 = i;
                else if (!lowU && lowV) c10 = i;
                else if (lowU) c01 = i;
                else c11 = i;
            }
            i00 = c00;
            i10 = c10;
            i01 = c01;
            i11 = c11;
        }

        static QuadGeometry of(BakedQuad quad) {
            return new QuadGeometry(quad);
        }

        boolean vertexAtMinU(int vertex) {
            return us[vertex] < 0.5f;
        }

        boolean vertexAtMinV(int vertex) {
            return vs[vertex] < 0.5f;
        }

        float position(float u, float v, int axis) {
            float top = lerp(positions[i00][axis], positions[i10][axis], u);
            float bottom = lerp(positions[i01][axis], positions[i11][axis], u);
            return lerp(top, bottom, v);
        }

        Direction edgeDirection(Direction face, float u, float v) {
            float x = position(u, v, 0);
            float y = position(u, v, 1);
            float z = position(u, v, 2);
            Direction.Axis faceAxis = face.getAxis();
            float bestDistance = -1.0f;
            Direction.Axis bestAxis = null;
            float bestValue = 0.5f;
            if (faceAxis != Direction.Axis.X && Math.abs(x - 0.5f) > bestDistance) {
                bestDistance = Math.abs(x - 0.5f);
                bestAxis = Direction.Axis.X;
                bestValue = x;
            }
            if (faceAxis != Direction.Axis.Y && Math.abs(y - 0.5f) > bestDistance) {
                bestDistance = Math.abs(y - 0.5f);
                bestAxis = Direction.Axis.Y;
                bestValue = y;
            }
            if (faceAxis != Direction.Axis.Z && Math.abs(z - 0.5f) > bestDistance) {
                bestAxis = Direction.Axis.Z;
                bestValue = z;
            }
            return bestAxis == null ? null : Direction.fromAxisAndDirection(bestAxis,
                bestValue < 0.5f ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
        }

        private static float lerp(float a, float b, float delta) {
            return a + (b - a) * delta;
        }
    }
}
