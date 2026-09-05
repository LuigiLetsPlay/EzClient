package app.ezclient.performance.visibility;

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

import java.util.Arrays;
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
public final class EzVisibilityEngine implements AutoCloseable {
    public static final EzVisibilityEngine INSTANCE = new EzVisibilityEngine();

    private static final long[] EMPTY_SECTION = new long[64];
    private static final long[] FULL_SECTION = fullSection();
    private static final int MAX_TARGETS_PER_FRAME = 4096;
    private static final long EVALUATION_INTERVAL_NANOS = 100_000_000L;
    private static final long SECTION_PRUNE_INTERVAL_NANOS = 5_000_000_000L;
    private static final int RETAIN_SECTION_RADIUS = 16;
    private static final double MIN_CULL_DISTANCE_SQ = 16.0;
    private static final double MAX_RAY_DISTANCE_SQ = 192.0 * 192.0;

    private final ConcurrentHashMap<Long, long[]> opaqueSections = new ConcurrentHashMap<>();
    private final AtomicReference<VisibilitySnapshot> published =
            new AtomicReference<>(VisibilitySnapshot.EMPTY);
    private final AtomicBoolean evaluationRunning = new AtomicBoolean();
    private final ThreadLocal<SectionCapture> sectionCapture = new ThreadLocal<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "EzClient Occlusion Worker");
        thread.setDaemon(true);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
        return thread;
    });

    // Accessed exclusively during LevelExtractor.extract on the client thread.
    private final TargetBuffer frameTargets = new TargetBuffer(512);
    private Vec3 frameCamera = Vec3.ZERO;
    private volatile ClientLevel activeLevel;
    private boolean collecting;
    private long nextEvaluationNanos;
    private long nextSectionPruneNanos;

    private EzVisibilityEngine() {
    }

    public void beginFrame(Vec3 cameraPosition) {
        if (evaluationRunning.get() || System.nanoTime() < nextEvaluationNanos) {
            collecting = false;
            return;
        }
        frameTargets.clear();
        frameCamera = Objects.requireNonNull(cameraPosition, "cameraPosition");
        collecting = true;
    }

    public void endFrame() {
        if (!collecting) {
            return;
        }
        collecting = false;
        nextEvaluationNanos = System.nanoTime() + EVALUATION_INTERVAL_NANOS;
        if (frameTargets.isEmpty() || !evaluationRunning.compareAndSet(false, true)) {
            return;
        }

        Vec3 camera = frameCamera;
        TargetSnapshot targets = frameTargets.snapshot();
        VisibilitySnapshot previous = published.get();
        worker.execute(() -> evaluate(camera, targets, previous));
    }

    /** Returns false only from a completed, hysteresis-filtered worker result. */
    public boolean shouldRender(Entity entity) {
        VisibilitySnapshot snapshot = published.get();
        if (!collecting && snapshot.culled().isEmpty()) {
            return true;
        }
        if (isEntityExempt(entity)) {
            return true;
        }

        long key = entityKey(entity.getId());
        if (collecting) {
            registerEntity(key, entity.getBoundingBox());
        }
        return !snapshot.culled().contains(key);
    }

    /** Globally rendered block entities are never culled by this subsystem. */
    public boolean shouldRender(BlockEntity blockEntity, boolean globallyRendered) {
        if (globallyRendered || blockEntity.isRemoved()) {
            return true;
        }

        BlockPos pos = blockEntity.getBlockPos();
        long key = blockEntityKey(pos.asLong());
        if (collecting && frameTargets.size() < MAX_TARGETS_PER_FRAME) {
            frameTargets.add(key,
                    pos.getX() - 0.0625, pos.getY() - 0.0625, pos.getZ() - 0.0625,
                    pos.getX() + 1.0625, pos.getY() + 1.0625, pos.getZ() + 1.0625);
        }
        return !published.get().culled().contains(key);
    }

    private static boolean isEntityExempt(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera != null && (entity == camera || entity.hasPassenger(camera) || camera.getVehicle() == entity)) {
            return true;
        }
        return entity instanceof Display.TextDisplay
                || entity.isCurrentlyGlowing()
                || minecraft.shouldEntityAppearGlowing(entity);
    }

    private void registerEntity(long key, AABB box) {
        if (frameTargets.size() >= MAX_TARGETS_PER_FRAME) return;
        double largest = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
        // Large/custom entities receive both proportional and fixed padding at screen edges.
        double padding = largest <= 4.0 ? 0.10 : Math.max(1.5, largest * 0.15);
        frameTargets.add(key, box.minX - padding, box.minY - padding, box.minZ - padding,
                box.maxX + padding, box.maxY + padding, box.maxZ + padding);
    }

    private void evaluate(Vec3 camera, TargetSnapshot targets, VisibilitySnapshot previous) {
        try {
            pruneDistantSections(camera);
            long[] occluded = new long[targets.size()];
            long[] culled = new long[targets.size()];
            int occludedCount = 0;
            int culledCount = 0;

            for (int index = 0; index < targets.size(); index++) {
                long key = targets.keys()[index];
                int offset = index * 6;
                if (isOccluded(camera, targets.packedBounds(), offset)) {
                    occluded[occludedCount++] = key;
                    if (previous.occluded().contains(key)) {
                        culled[culledCount++] = key;
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

    private boolean isOccluded(Vec3 camera, double[] box, int offset) {
        double minX = box[offset];
        double minY = box[offset + 1];
        double minZ = box[offset + 2];
        double maxX = box[offset + 3];
        double maxY = box[offset + 4];
        double maxZ = box[offset + 5];
        double centerX = (minX + maxX) * 0.5;
        double centerY = (minY + maxY) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;
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
            double x = (corner & 1) == 0 ? minX : maxX;
            double y = (corner & 2) == 0 ? minY : maxY;
            double z = (corner & 4) == 0 ? minZ : maxZ;
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
        long cachedSectionKey = Long.MIN_VALUE;
        long[] cachedMask = null;

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
            if (sectionKey != cachedSectionKey) {
                cachedSectionKey = sectionKey;
                cachedMask = opaqueSections.get(sectionKey);
            }
            if (cachedMask == null) {
                return false;
            }
            int bit = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
            if ((cachedMask[bit >>> 6] & (1L << (bit & 63))) != 0L) {
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

    /** Starts piggyback capture on Minecraft's own 4096-block compiler pass. */
    public void beginSectionCapture(ClientLevel sourceLevel, SectionPos section) {
        sectionCapture.set(new SectionCapture(sourceLevel, section.asLong(), new long[64]));
    }

    public void captureCompiledBlock(BlockPos pos, BlockState state) {
        SectionCapture capture = sectionCapture.get();
        if (capture == null || capture.level() != activeLevel) return;
        if (state.canOcclude() && state.isSolidRender()) {
            int bit = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
            capture.mask()[bit >>> 6] |= 1L << (bit & 63);
            capture.any()[0] = true;
        }
    }

    public void endSectionCapture() {
        SectionCapture capture = sectionCapture.get();
        sectionCapture.remove();
        if (capture != null && capture.level() == activeLevel) {
            opaqueSections.put(capture.sectionKey(), canonicalSection(capture.mask(), capture.any()[0]));
        }
    }

    /** Copy-on-write update; readers never observe a partially changed section. */
    public void updateBlock(ClientLevel sourceLevel, BlockPos pos, BlockState state) {
        if (sourceLevel != activeLevel) {
            return;
        }
        long sectionKey = SectionPos.asLong(pos);
        opaqueSections.computeIfPresent(sectionKey, (ignored, oldMask) -> {
            int bit = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
            long bitMask = 1L << (bit & 63);
            boolean opaque = state.canOcclude() && state.isSolidRender();
            boolean wasOpaque = (oldMask[bit >>> 6] & bitMask) != 0L;
            if (opaque == wasOpaque) {
                return oldMask;
            }
            long[] next = oldMask.clone();
            if (opaque) {
                next[bit >>> 6] |= bitMask;
            } else {
                next[bit >>> 6] &= ~bitMask;
            }
            return canonicalSection(next, true);
        });
    }

    private void pruneDistantSections(Vec3 camera) {
        long now = System.nanoTime();
        if (now < nextSectionPruneNanos) return;
        nextSectionPruneNanos = now + SECTION_PRUNE_INTERVAL_NANOS;
        int cameraX = SectionPos.blockToSectionCoord(floor(camera.x));
        int cameraY = SectionPos.blockToSectionCoord(floor(camera.y));
        int cameraZ = SectionPos.blockToSectionCoord(floor(camera.z));
        opaqueSections.keySet().removeIf(key ->
                Math.abs(SectionPos.x(key) - cameraX) > RETAIN_SECTION_RADIUS
                        || Math.abs(SectionPos.y(key) - cameraY) > RETAIN_SECTION_RADIUS
                        || Math.abs(SectionPos.z(key) - cameraZ) > RETAIN_SECTION_RADIUS);
    }

    private static long[] canonicalSection(long[] mask, boolean any) {
        if (!any) return EMPTY_SECTION;
        boolean empty = true;
        boolean full = true;
        for (long word : mask) {
            empty &= word == 0L;
            full &= word == -1L;
            if (!empty && !full) return mask;
        }
        if (empty) return EMPTY_SECTION;
        return full ? FULL_SECTION : mask;
    }

    private static long[] fullSection() {
        long[] mask = new long[64];
        Arrays.fill(mask, -1L);
        return mask;
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

    /** Reusable structure-of-arrays collector: the render thread creates no Target objects. */
    private static final class TargetBuffer {
        private long[] keys;
        private double[] bounds;
        private int size;

        private TargetBuffer(int initialCapacity) {
            keys = new long[initialCapacity];
            bounds = new double[initialCapacity * 6];
        }

        private void add(long key, double minX, double minY, double minZ,
                         double maxX, double maxY, double maxZ) {
            ensureCapacity(size + 1);
            keys[size] = key;
            int offset = size * 6;
            bounds[offset] = minX;
            bounds[offset + 1] = minY;
            bounds[offset + 2] = minZ;
            bounds[offset + 3] = maxX;
            bounds[offset + 4] = maxY;
            bounds[offset + 5] = maxZ;
            size++;
        }

        private void ensureCapacity(int wanted) {
            if (wanted <= keys.length) return;
            int capacity = Math.min(MAX_TARGETS_PER_FRAME, Math.max(wanted, keys.length * 2));
            keys = Arrays.copyOf(keys, capacity);
            bounds = Arrays.copyOf(bounds, capacity * 6);
        }

        private TargetSnapshot snapshot() {
            return new TargetSnapshot(
                    Arrays.copyOf(keys, size),
                    Arrays.copyOf(bounds, size * 6));
        }

        private int size() {
            return size;
        }

        private boolean isEmpty() {
            return size == 0;
        }

        private void clear() {
            size = 0;
        }
    }

    private record TargetSnapshot(long[] keys, double[] packedBounds) {
        private int size() {
            return keys.length;
        }
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

        private boolean isEmpty() {
            return values.length == 0;
        }
    }

    private record SectionCapture(ClientLevel level, long sectionKey, long[] mask, boolean[] any) {
        private SectionCapture(ClientLevel level, long sectionKey, long[] mask) {
            this(level, sectionKey, mask, new boolean[1]);
        }
    }
}
