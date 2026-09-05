package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.phys.*;

/** Uses Minecraft's depth-tested gizmo collector; never changes the world or sends packets. */
public final class WorldVisuals {
    private WorldVisuals() {}
    public static boolean visible() {
        var mc = Minecraft.getInstance(); return mc.level != null && mc.player != null && !EzScreenBridge.hudHidden(mc);
    }
    public static void extract(Minecraft mc) {
        if (!visible()) return;
        var hitboxes = FeatureModule.get(HitboxModule.class);
        var blocks = FeatureModule.get(BlockOverlayModule.class);
        var waypoints = FeatureModule.get(WaypointsModule.class);
        if (!hitboxes.isEnabled() && !blocks.isEnabled() && !waypoints.isEnabled()) return;
        //? if >=26.2 {
        try (var ignored = mc.levelExtractor.collectPerFrameMainThreadGizmos()) {
        //?} else {
        /*try (var ignored = mc.levelRenderer.collectPerFrameGizmos()) {
        *///?}
            if (hitboxes.isEnabled() && !hitboxes.flag("debugOnly")) hitboxes(mc, hitboxes);
            if (blocks.isEnabled() && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                var pos = hit.getBlockPos();
                var shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
                boolean outline = !blocks.text("style").equals("Fill"), fill = !blocks.text("style").equals("Outline");
                for (AABB box : shape.toAabbs()) {
                    Gizmos.cuboid(box.move(pos).inflate(0.002), new GizmoStyle(outline ? blocks.tint("outline", blocks.flag("chroma")) : 0,
                        (float)blocks.number("width"), fill ? (blocks.tint("fill", false) & 0xffffff) | ((int)Math.round(blocks.number("fillOpacity") * (blocks.tint("fill", false) >>> 24) / 100) << 24) : 0));
                    if (blocks.text("break").equals("Tint overlay") && mc.gameMode != null && mc.gameMode.isDestroying())
                        Gizmos.cuboid(box.move(pos).inflate(0.003), GizmoStyle.fill(blocks.tint("breakColor", false)));
                }
            }
            if (waypoints.isEnabled() && !waypoints.text("marker").equals("None")) {
                int count = 0;
                for (var point : waypoints.active(mc)) {
                    if (++count > 32) break;
                    if (point.position().distanceToSqr(mc.player.position()) > Math.pow(waypoints.number("range"), 2)) continue;
                    if (!waypoints.text("marker").equals("Floating")) {
                        Gizmos.cuboid(new AABB(point.x() - .08, point.y(), point.z() - .08, point.x() + .08, point.y() + 24, point.z() + .08),
                            GizmoStyle.fill((point.color() & 0xffffff) | 0x40000000));
                    }
                    if (!waypoints.text("marker").equals("Beam")) {
                        Gizmos.billboardText(point.icon() + " " + point.name() + " " + Math.round(point.position().distanceTo(mc.player.position())) + "m",
                            point.position().add(0, 1.5, 0), net.minecraft.gizmos.TextGizmo.Style.forColorAndCentered(point.color()).withScale(.32f * (float)waypoints.getScale()));
                    }
                }
            }
        }
    }
    public static void hitboxes(Minecraft mc, HitboxModule module) {
        if (!visible()) return;
        int count = 0;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || entity.isInvisible() || !module.accepts(entity) || entity.distanceToSqr(mc.player) > 96 * 96) continue;
            if (++count > 128) break;
            float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 offset = entity.getPosition(partial).subtract(entity.position());
            AABB box = entity.getBoundingBox().move(offset);
            Gizmos.cuboid(box, new GizmoStyle(module.tint("box", module.flag("chroma")), (float)module.number("width"), module.flag("fill") ? module.tint("fillColor", false) : 0));
            Vec3 eye = entity.getEyePosition(partial);
            if (module.flag("eyes")) Gizmos.cuboid(new AABB(box.minX, eye.y - .005, box.minZ, box.maxX, eye.y + .005, box.maxZ), GizmoStyle.stroke(module.tint("eyeColor", false)));
            if (module.flag("look")) Gizmos.line(eye, eye.add(entity.getViewVector(partial).scale(2)), module.tint("lookColor", false), (float)module.number("width"));
        }
    }
}
