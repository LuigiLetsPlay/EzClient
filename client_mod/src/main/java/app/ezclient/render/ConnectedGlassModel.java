package app.ezclient.render;

import app.ezclient.gui.ClearGlassModule;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Context-aware glass model used by Fabric's renderer API (and Sodium's FRAPI path).
 * It emits only the original texture's one-pixel border at the outside of a connected
 * glass surface. Interior glass faces and the per-block front-face grid are omitted.
 */
public final class ConnectedGlassModel implements BlockStateModel {
    private static final float BORDER = 1.0f / 16.0f;
    private static final ThreadLocal<ArrayList<BlockStateModelPart>> PART_BUFFER =
            ThreadLocal.withInitial(() -> new ArrayList<>(1));
    private final BlockStateModel delegate;

    private ConnectedGlassModel(BlockStateModel delegate) {
        this.delegate = delegate;
    }

    public static void register() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register(
                ModelModifier.WRAP_PHASE,
                (model, bakeContext) -> bakeContext.state().is(Blocks.GLASS)
                        ? new ConnectedGlassModel(model)
                        : model
        ));
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
        if (!ClearGlassModule.isConnectedRenderingActive()) return null;
        int mask = 0;
        Direction[] directions = Direction.values();
        for (int i = 0; i < directions.length; i++) {
            if (level.getBlockState(pos.relative(directions[i])).is(state.getBlock())) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
                          BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        if (!ClearGlassModule.isConnectedRenderingActive()) {
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

    private static void emitQuads(List<BakedQuad> quads, Direction cullFace, TriState ambientOcclusion,
                                  QuadEmitter emitter,
                                  BlockAndTintGetter level, BlockPos pos, BlockState state) {
        for (BakedQuad quad : quads) {
            QuadGeometry geometry = QuadGeometry.of(quad);

            Direction edgeUmin = geometry.edgeDirection(quad.direction(), 0.5f * BORDER, 0.5f);
            Direction edgeUmax = geometry.edgeDirection(quad.direction(), 1.0f - 0.5f * BORDER, 0.5f);
            Direction edgeVmin = geometry.edgeDirection(quad.direction(), 0.5f, 0.5f * BORDER);
            Direction edgeVmax = geometry.edgeDirection(quad.direction(), 0.5f, 1.0f - 0.5f * BORDER);

            boolean hasGlassUmin = edgeUmin != null && level.getBlockState(pos.relative(edgeUmin)).is(state.getBlock());
            boolean hasGlassUmax = edgeUmax != null && level.getBlockState(pos.relative(edgeUmax)).is(state.getBlock());
            boolean hasGlassVmin = edgeVmin != null && level.getBlockState(pos.relative(edgeVmin)).is(state.getBlock());
            boolean hasGlassVmax = edgeVmax != null && level.getBlockState(pos.relative(edgeVmax)).is(state.getBlock());

            // Vertical strips: if neighbor in V direction is glass, horizontal strip is culled,
            // so vertical strip must extend to the full edge (0.0f or 1.0f) to prevent gaps at the seam.
            if (!hasGlassUmin) {
                float v0 = hasGlassVmin ? 0.0f : BORDER;
                float v1 = hasGlassVmax ? 1.0f : (1.0f - BORDER);
                emitBorderStrip(quad, geometry, cullFace, ambientOcclusion, emitter, 0.0f, v0, BORDER, v1);
            }
            if (!hasGlassUmax) {
                float v0 = hasGlassVmin ? 0.0f : BORDER;
                float v1 = hasGlassVmax ? 1.0f : (1.0f - BORDER);
                emitBorderStrip(quad, geometry, cullFace, ambientOcclusion, emitter, 1.0f - BORDER, v0, 1.0f, v1);
            }

            // Horizontal strips own the corner pixels when their neighbor is not glass
            if (!hasGlassVmin) {
                emitBorderStrip(quad, geometry, cullFace, ambientOcclusion, emitter, 0.0f, 0.0f, 1.0f, BORDER);
            }
            if (!hasGlassVmax) {
                emitBorderStrip(quad, geometry, cullFace, ambientOcclusion, emitter, 0.0f, 1.0f - BORDER, 1.0f, 1.0f);
            }
        }
    }

    private static void emitBorderStrip(BakedQuad quad, QuadGeometry geometry, Direction cullFace,
                                        TriState ambientOcclusion, QuadEmitter emitter,
                                        float u0, float v0, float u1, float v1) {
        emitter.cullFace(cullFace);
        emitter.fromBakedQuad(quad);
        for (int vertex = 0; vertex < 4; vertex++) {
            float targetU = geometry.vertexAtMinU(vertex) ? u0 : u1;
            float targetV = geometry.vertexAtMinV(vertex) ? v0 : v1;
            emitter.pos(vertex,
                    geometry.position(targetU, targetV, 0),
                    geometry.position(targetU, targetV, 1),
                    geometry.position(targetU, targetV, 2));
            emitter.uv(vertex,
                    geometry.minU + (geometry.maxU - geometry.minU) * targetU,
                    geometry.minV + (geometry.maxV - geometry.minV) * targetV);
        }
        emitter.ambientOcclusion(ambientOcclusion);
        emitter.shadeMode(ShadeMode.VANILLA);
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
        private final float minU;
        private final float maxU;
        private final float minV;
        private final float maxV;

        private QuadGeometry(BakedQuad quad) {
            minU = min(quad, true);
            maxU = max(quad, true);
            minV = min(quad, false);
            maxV = max(quad, false);
            int c00 = 0, c10 = 0, c01 = 0, c11 = 0;
            for (int i = 0; i < 4; i++) {
                positions[i][0] = quad.position(i).x();
                positions[i][1] = quad.position(i).y();
                positions[i][2] = quad.position(i).z();
                us[i] = net.minecraft.client.model.geom.builders.UVPair.unpackU(quad.packedUV(i));
                vs[i] = net.minecraft.client.model.geom.builders.UVPair.unpackV(quad.packedUV(i));
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
            return Math.abs(us[vertex] - minU) <= Math.abs(us[vertex] - maxU);
        }

        boolean vertexAtMinV(int vertex) {
            return Math.abs(vs[vertex] - minV) <= Math.abs(vs[vertex] - maxV);
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
                bestDistance = Math.abs(x - 0.5f); bestAxis = Direction.Axis.X; bestValue = x;
            }
            if (faceAxis != Direction.Axis.Y && Math.abs(y - 0.5f) > bestDistance) {
                bestDistance = Math.abs(y - 0.5f); bestAxis = Direction.Axis.Y; bestValue = y;
            }
            if (faceAxis != Direction.Axis.Z && Math.abs(z - 0.5f) > bestDistance) {
                bestAxis = Direction.Axis.Z; bestValue = z;
            }
            return bestAxis == null ? null : Direction.fromAxisAndDirection(bestAxis,
                    bestValue < 0.5f ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
        }

        private static float min(BakedQuad quad, boolean u) {
            float result = Float.POSITIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                long packed = quad.packedUV(i);
                result = Math.min(result, u
                        ? net.minecraft.client.model.geom.builders.UVPair.unpackU(packed)
                        : net.minecraft.client.model.geom.builders.UVPair.unpackV(packed));
            }
            return result;
        }

        private static float max(BakedQuad quad, boolean u) {
            float result = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                long packed = quad.packedUV(i);
                result = Math.max(result, u
                        ? net.minecraft.client.model.geom.builders.UVPair.unpackU(packed)
                        : net.minecraft.client.model.geom.builders.UVPair.unpackV(packed));
            }
            return result;
        }

        private static float lerp(float a, float b, float delta) {
            return a + (b - a) * delta;
        }
    }
}
