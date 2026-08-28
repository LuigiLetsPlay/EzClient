package app.ezclient.performance.culling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Conservative, lock-free render visibility cache.
 *
 * <p>The render/extract thread only appends immutable target snapshots and reads the last
 * published result. The worker never touches the live level: its voxel source consists of
 * immutable 4096-bit section masks captured from {@link RenderSectionRegion}, which itself is
 * the immutable region used by Minecraft's asynchronous section compiler.</p>
 *
 * <p>Missing section data and all failures mean "visible". A target must be occluded in two
 * consecutive completed evaluations before it is culled. These two rules deliberately trade a
 * little potential work for immunity to chunk-load holes and one-frame pop-in.</p>
 */
public final class OcclusionCullingManager implements AutoCloseable {
    public static final OcclusionCullingManager INSTANCE = new OcclusionCullingManager();

    private static final long[] EMPTY_SECTION = new long[64];
    private static final int MAX_TARGETS_PER_FRAME = 4096;
    private static final double MIN_CULL_DISTANCE_SQ = 16.0;
    private static final double MAX_RAY_DISTANCE_SQ = 192.0 * 192.0;

    private final ConcurrentHashMap<Long, long[]> opaqueSections = new ConcurrentHashMap<>();
    private final AtomicReference<VisibilitySnapshot> published =
            new AtomicReference<>(VisibilitySnapshot.EMPTY);
    private final AtomicBoolean evaluationRunning = new AtomicBoolean();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "EzClient Occlusion Worker");
        thread.setDaemon(true);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
        return thread;
    });

    // Accessed exclusively during LevelExtractor.extract on the client thread.
    private final ArrayList<Target> frameTargets = new ArrayList<>(512);
    private Vec3 frameCamera = Vec3.ZERO;
    private volatile ClientLevel activeLevel;
    private boolean collecting;

    private OcclusionCullingManager() {
    }

    public void beginFrame(Vec3 cameraPosition) {
        frameTargets.clear();
        frameCamera = Objects.requireNonNull(cameraPosition, "cameraPosition");
        collecting = true;
    }

    public void endFrame() {
        collecting = false;
        if (frameTargets.isEmpty() || !evaluationRunning.compareAndSet(false, true)) {
            return;
        }

        Vec3 camera = frameCamera;
        List<Target> targets = List.copyOf(frameTargets);
        VisibilitySnapshot previous = published.get();
        worker.execute(() -> evaluate(camera, targets, previous));
    }

    /** Returns false only from a completed, hysteresis-filtered worker result. */
    public boolean shouldRender(Entity entity) {
        if (isEntityExempt(entity)) {
            return true;
        }

        long key = entityKey(entity.getId());
        AABB bounds = expandedBounds(entity.getBoundingBox());
        register(new Target(key, bounds));
        return !published.get().culled().contains(key);
    }

    /** Globally rendered block entities are never culled by this subsystem. */
    public boolean shouldRender(BlockEntity blockEntity, boolean globallyRendered) {
        if (globallyRendered || blockEntity.isRemoved()) {
            return true;
        }

        BlockPos pos = blockEntity.getBlockPos();
        long key = blockEntityKey(pos.asLong());
        register(new Target(key, new AABB(pos).inflate(0.0625)));
        return !published.get().culled().contains(key);
    }

    private void register(Target target) {
        if (collecting && frameTargets.size() < MAX_TARGETS_PER_FRAME) {
            frameTargets.add(target);
        }
    }

    private static boolean isEntityExempt(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return entity == minecraft.getCameraEntity()
                || entity instanceof Display.TextDisplay
                || entity.getCustomName() != null
                || entity.isCurrentlyGlowing()
                || minecraft.shouldEntityAppearGlowing(entity);
    }

    private static AABB expandedBounds(AABB box) {
        double largest = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
        if (largest <= 4.0) {
            return box.inflate(0.10);
        }
        // Large/custom entities receive both proportional and fixed padding at screen edges.
        return box.inflate(Math.max(1.5, largest * 0.15));
    }

    private void evaluate(Vec3 camera, List<Target> targets, VisibilitySnapshot previous) {
        try {
            long[] occluded = new long[targets.size()];
            long[] culled = new long[targets.size()];
            int occludedCount = 0;
            int culledCount = 0;

            for (Target target : targets) {
                if (isOccluded(camera, target.bounds())) {
                    occluded[occludedCount++] = target.key();
                    if (previous.occluded().contains(target.key())) {
                        culled[culledCount++] = target.key();
                    }
                }
            }

            published.set(new VisibilitySnapshot(
                    LongSetSnapshot.copyOf(culled, culledCount),
                    LongSetSnapshot.copyOf(occluded, occludedCount)));
        } catch (Throwable ignored) {
            // Rendering correctness is more important than an optimization result.
            published.set(VisibilitySnapshot.EMPTY);
        } finally {
            evaluationRunning.set(false);
        }
    }

    private boolean isOccluded(Vec3 camera, AABB box) {
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerY = (box.minY + box.maxY) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double dx = centerX - camera.x;
        double dy = centerY - camera.y;
        double dz = centerZ - camera.z;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        if (distanceSq < MIN_CULL_DISTANCE_SQ || distanceSq > MAX_RAY_DISTANCE_SQ) {
            return false;
        }

        // Center first: most visible entities exit after one traversal.
        if (!rayBlocked(camera.x, camera.y, camera.z, centerX, centerY, centerZ)) {
            return false;
        }
        for (int corner = 0; corner < 8; corner++) {
            double x = (corner & 1) == 0 ? box.minX : box.maxX;
            double y = (corner & 2) == 0 ? box.minY : box.maxY;
            double z = (corner & 4) == 0 ? box.minZ : box.maxZ;
            if (!rayBlocked(camera.x, camera.y, camera.z, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    /** Amanatides-Woo voxel traversal over immutable section bitsets. */
    private boolean rayBlocked(double startX, double startY, double startZ,
                               double endX, double endY, double endZ) {
        int x = floor(startX);
        int y = floor(startY);
        int z = floor(startZ);
        int endCellX = floor(endX);
        int endCellY = floor(endY);
        int endCellZ = floor(endZ);

        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;
        int stepX = Integer.compare(endCellX, x);
        int stepY = Integer.compare(endCellY, y);
        int stepZ = Integer.compare(endCellZ, z);
        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
        double tMaxX = initialT(startX, x, stepX, dx);
        double tMaxY = initialT(startY, y, stepY, dy);
        double tMaxZ = initialT(startZ, z, stepZ, dz);
        int maximumSteps = Math.abs(endCellX - x) + Math.abs(endCellY - y)
                + Math.abs(endCellZ - z) + 3;

        // Do not test the camera voxel or the target voxel.
        for (int traversed = 0; traversed < maximumSteps; traversed++) {
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY <= tMaxZ) {
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }

            if (x == endCellX && y == endCellY && z == endCellZ) {
                return false;
            }
            long sectionKey = SectionPos.asLong(
                    SectionPos.blockToSectionCoord(x),
                    SectionPos.blockToSectionCoord(y),
                    SectionPos.blockToSectionCoord(z));
            long[] mask = opaqueSections.get(sectionKey);
            if (mask == null) {
                return false;
            }
            int bit = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            if ((mask[bit >>> 6] & (1L << (bit & 63))) != 0L) {
                return true;
            }
        }
        return false;
    }

    private static double initialT(double start, int cell, int step, double delta) {
        if (step == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double boundary = step > 0 ? cell + 1.0 : cell;
        return (boundary - start) / delta;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    /** Captures one immutable section from Minecraft's render-region snapshot. */
    public void captureSection(ClientLevel sourceLevel, SectionPos section, RenderSectionRegion region) {
        if (sourceLevel != activeLevel) {
            return;
        }
        long[] mask = new long[64];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int baseX = section.minBlockX();
        int baseY = section.minBlockY();
        int baseZ = section.minBlockZ();
        boolean any = false;

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    cursor.set(baseX + x, baseY + y, baseZ + z);
                    BlockState state = region.getBlockState(cursor);
                    if (state.canOcclude() && state.isSolidRender()) {
                        int bit = (y << 8) | (z << 4) | x;
                        mask[bit >>> 6] |= 1L << (bit & 63);
                        any = true;
                    }
                }
            }
        }
        if (sourceLevel == activeLevel) {
            opaqueSections.put(section.asLong(), any ? mask : EMPTY_SECTION);
        }
    }

    /** Copy-on-write update; readers never observe a partially changed section. */
    public void updateBlock(ClientLevel sourceLevel, BlockPos pos, BlockState state) {
        if (sourceLevel != activeLevel) {
            return;
        }
        long sectionKey = SectionPos.asLong(pos);
        opaqueSections.compute(sectionKey, (ignored, oldMask) -> {
            long[] next = oldMask == null ? new long[64] : oldMask.clone();
            int bit = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
            long bitMask = 1L << (bit & 63);
            if (state.canOcclude() && state.isSolidRender()) {
                next[bit >>> 6] |= bitMask;
            } else {
                next[bit >>> 6] &= ~bitMask;
            }
            return next;
        });
    }

    public void clear() {
        frameTargets.clear();
        collecting = false;
        opaqueSections.clear();
        published.set(VisibilitySnapshot.EMPTY);
    }

    public void setLevel(ClientLevel level) {
        activeLevel = level;
        clear();
    }

    @Override
    public void close() {
        clear();
        worker.shutdownNow();
    }

    private static long entityKey(int entityId) {
        return mix64(0x454e544954590000L ^ Integer.toUnsignedLong(entityId));
    }

    private static long blockEntityKey(long packedPosition) {
        return mix64(0x424c4f434b000000L ^ packedPosition);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private record Target(long key, AABB bounds) {
    }

    private record VisibilitySnapshot(LongSetSnapshot culled, LongSetSnapshot occluded) {
        private static final VisibilitySnapshot EMPTY =
                new VisibilitySnapshot(LongSetSnapshot.EMPTY, LongSetSnapshot.EMPTY);
    }

    /** Immutable sorted primitive set: allocation occurs only on the worker. */
    private record LongSetSnapshot(long[] values) {
        private static final LongSetSnapshot EMPTY = new LongSetSnapshot(new long[0]);

        private static LongSetSnapshot copyOf(long[] source, int length) {
            if (length == 0) {
                return EMPTY;
            }
            long[] copy = Arrays.copyOf(source, length);
            Arrays.sort(copy);
            return new LongSetSnapshot(copy);
        }

        private boolean contains(long value) {
            return Arrays.binarySearch(values, value) >= 0;
        }
    }
}
