package app.ezclient.gui;

import app.ezclient.EzClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

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
        var ores = FeatureModule.get(GlowingOresModule.class);
        if (!hitboxes.isEnabled() && !blocks.isEnabled() && !waypoints.isEnabled() && !ores.isEnabled() && !BlockSelectionOverlay.isActive()) return;
        //? if >=26.2 {
        try (var ignored = mc.levelExtractor.collectPerFrameMainThreadGizmos()) {
        //?} else {
        /*try (var ignored = mc.levelRenderer.collectPerFrameGizmos()) {
        *///?}
            if (hitboxes.isEnabled() && !hitboxes.flag("debugOnly")) hitboxes(mc, hitboxes);
            if (blocks.isEnabled() && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                var pos = hit.getBlockPos();
                var state = mc.level.getBlockState(pos);
                String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                BlockOverlayModule.BlockRule rule = blocks.getBlockRule(blockId);
                String style = rule != null ? rule.style() : blocks.text("style");
                if (!style.equals("None")) {
                    var shape = state.getShape(mc.level, pos);
                    boolean outline = !style.equals("Fill");
                    boolean fill = !style.equals("Outline");
                    int outlineCol = rule != null ? rule.outlineColor() : blocks.tint("outline", blocks.flag("chroma"));
                    int fillCol = rule != null ? rule.fillColor() : blocks.tint("fill", false);
                    double fillOp = rule != null ? rule.fillOpacity() : blocks.number("fillOpacity");
                    int finalFill = fill ? (fillCol & 0xffffff) | ((int) Math.round(fillOp * (fillCol >>> 24) / 100.0) << 24) : 0;
                    for (AABB box : shape.toAabbs()) {
                        Gizmos.cuboid(box.move(pos).inflate(0.002), new GizmoStyle(outline ? outlineCol : 0,
                            (float) blocks.number("width"), finalFill));
                        if (blocks.text("break").equals("Tint overlay") && mc.gameMode != null && mc.gameMode.isDestroying())
                            Gizmos.cuboid(box.move(pos).inflate(0.003), GizmoStyle.fill(blocks.tint("breakColor", false)));
                    }
                }
            }
            if (waypoints.isEnabled()) waypointVisuals(mc, waypoints);
            if (BlockSelectionOverlay.isActive()) BlockSelectionOverlay.renderWorldGizmos(mc);
            if (ores.isEnabled()) glowingOres(ores);
        }
    }

    private static boolean isExposedNeighbor(Minecraft mc, java.util.Set<BlockPos> orePositions, BlockPos neighborPos, net.minecraft.core.Direction face) {
        if (!orePositions.contains(neighborPos)) return false;
        return !mc.level.getBlockState(neighborPos.relative(face)).canOcclude();
    }

    private static void glowingOres(GlowingOresModule module) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        boolean faceBorder = module.flag("faceBorder");
        boolean fill = module.flag("fill");
        if (!faceBorder && !fill) return;

        boolean connected = module.flag("connectedBorder");
        float lineWidth = (float) module.number("lineWidth");
        int alpha = Math.max(0, Math.min(255, (int) Math.round(module.number("fillAlpha") * 255)));

        var ores = module.ores();
        if (ores.isEmpty()) return;

        java.util.Set<BlockPos> orePositions = new java.util.HashSet<>();
        if (connected) {
            for (var ore : ores) {
                orePositions.add(ore.pos());
            }
        }

        double eps = 0.002;

        for (var ore : ores) {
            BlockPos pos = ore.pos();
            int color = ore.color();
            int fillColor = fill ? ((alpha << 24) | (color & 0xffffff)) : 0;
            GizmoStyle fillStyle = fillColor != 0 ? GizmoStyle.fill(fillColor) : null;

            double x = pos.getX(), y = pos.getY(), z = pos.getZ();
            double x0 = x - eps, x1 = x + 1.0 + eps;
            double y0 = y - eps, y1 = y + 1.0 + eps;
            double z0 = z - eps, z1 = z + 1.0 + eps;

            // UP (+Y)
            if (!mc.level.getBlockState(pos.above()).canOcclude()) {
                if (fillStyle != null) {
                    Gizmos.rect(new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), fillStyle);
                }
                if (faceBorder) {
                    if (!connected) {
                        Gizmos.rect(new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), GizmoStyle.stroke(color, lineWidth));
                    } else {
                        if (!isExposedNeighbor(mc, orePositions, pos.north(), net.minecraft.core.Direction.UP)) Gizmos.line(new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.south(), net.minecraft.core.Direction.UP)) Gizmos.line(new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.west(), net.minecraft.core.Direction.UP)) Gizmos.line(new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.east(), net.minecraft.core.Direction.UP)) Gizmos.line(new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), color, lineWidth);
                    }
                }
            }

            // DOWN (-Y)
            if (!mc.level.getBlockState(pos.below()).canOcclude()) {
                if (fillStyle != null) {
                    Gizmos.rect(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), new Vec3(x0, y0, z0), fillStyle);
                }
                if (faceBorder) {
                    if (!connected) {
                        Gizmos.rect(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), new Vec3(x0, y0, z0), GizmoStyle.stroke(color, lineWidth));
                    } else {
                        if (!isExposedNeighbor(mc, orePositions, pos.south(), net.minecraft.core.Direction.DOWN)) Gizmos.line(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.north(), net.minecraft.core.Direction.DOWN)) Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.west(), net.minecraft.core.Direction.DOWN)) Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.east(), net.minecraft.core.Direction.DOWN)) Gizmos.line(new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), color, lineWidth);
                    }
                }
            }

            // NORTH (-Z)
            if (!mc.level.getBlockState(pos.north()).canOcclude()) {
                if (fillStyle != null) {
                    Gizmos.rect(new Vec3(x1, y0, z0), new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), fillStyle);
                }
                if (faceBorder) {
                    if (!connected) {
                        Gizmos.rect(new Vec3(x1, y0, z0), new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), GizmoStyle.stroke(color, lineWidth));
                    } else {
                        if (!isExposedNeighbor(mc, orePositions, pos.below(), net.minecraft.core.Direction.NORTH)) Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.above(), net.minecraft.core.Direction.NORTH)) Gizmos.line(new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.west(), net.minecraft.core.Direction.NORTH)) Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.east(), net.minecraft.core.Direction.NORTH)) Gizmos.line(new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), color, lineWidth);
                    }
                }
            }

            // SOUTH (+Z)
            if (!mc.level.getBlockState(pos.south()).canOcclude()) {
                if (fillStyle != null) {
                    Gizmos.rect(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), fillStyle);
                }
                if (faceBorder) {
                    if (!connected) {
                        Gizmos.rect(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), GizmoStyle.stroke(color, lineWidth));
                    } else {
                        if (!isExposedNeighbor(mc, orePositions, pos.below(), net.minecraft.core.Direction.SOUTH)) Gizmos.line(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.above(), net.minecraft.core.Direction.SOUTH)) Gizmos.line(new Vec3(x0, y1, z1), new Vec3(x1, y1, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.west(), net.minecraft.core.Direction.SOUTH)) Gizmos.line(new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.east(), net.minecraft.core.Direction.SOUTH)) Gizmos.line(new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), color, lineWidth);
                    }
                }
            }

            // WEST (-X)
            if (!mc.level.getBlockState(pos.west()).canOcclude()) {
                if (fillStyle != null) {
                    Gizmos.rect(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), fillStyle);
                }
                if (faceBorder) {
                    if (!connected) {
                        Gizmos.rect(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), GizmoStyle.stroke(color, lineWidth));
                    } else {
                        if (!isExposedNeighbor(mc, orePositions, pos.below(), net.minecraft.core.Direction.WEST)) Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.above(), net.minecraft.core.Direction.WEST)) Gizmos.line(new Vec3(x0, y1, z0), new Vec3(x0, y1, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.north(), net.minecraft.core.Direction.WEST)) Gizmos.line(new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.south(), net.minecraft.core.Direction.WEST)) Gizmos.line(new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), color, lineWidth);
                    }
                }
            }

            // EAST (+X)
            if (!mc.level.getBlockState(pos.east()).canOcclude()) {
                if (fillStyle != null) {
                    Gizmos.rect(new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), fillStyle);
                }
                if (faceBorder) {
                    if (!connected) {
                        Gizmos.rect(new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), GizmoStyle.stroke(color, lineWidth));
                    } else {
                        if (!isExposedNeighbor(mc, orePositions, pos.below(), net.minecraft.core.Direction.EAST)) Gizmos.line(new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.above(), net.minecraft.core.Direction.EAST)) Gizmos.line(new Vec3(x1, y1, z1), new Vec3(x1, y1, z0), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.south(), net.minecraft.core.Direction.EAST)) Gizmos.line(new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), color, lineWidth);
                        if (!isExposedNeighbor(mc, orePositions, pos.north(), net.minecraft.core.Direction.EAST)) Gizmos.line(new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), color, lineWidth);
                    }
                }
            }
        }
    }
    public static String getWaypointSymbol(String icon) {
        if (icon == null) return "★";
        return switch (icon) {
            case "Star" -> "★";
            case "Home" -> "⌂";
            case "Base" -> "▲";
            case "Chest" -> "■";
            case "Diamond" -> "♦";
            case "Pickaxe" -> "⛏";
            case "Sword" -> "⚔";
            case "Skull" -> "☠";
            case "Player" -> "웃";
            case "Trap" -> "⚠";
            case "Flag" -> "⚑";
            case "Portal" -> "◎";
            default -> "★";
        };
    }

    public static void renderConnectedBlocks(java.util.List<WaypointsModule.BlockPosition> blocks, int outlineColor, float lineWidth, int fillColor, boolean alwaysOnTop) {
        if (blocks == null || blocks.isEmpty()) return;
        java.util.Set<WaypointsModule.BlockPosition> set = new java.util.HashSet<>(blocks);
        GizmoStyle style = new GizmoStyle(outlineColor, lineWidth, fillColor);
        double eps = 0.002;

        for (var b : blocks) {
            double x = b.x(), y = b.y(), z = b.z();
            double x0 = x - eps, x1 = x + 1.0 + eps;
            double y0 = y - eps, y1 = y + 1.0 + eps;
            double z0 = z - eps, z1 = z + 1.0 + eps;

            // Render only external faces (skip faces that share an edge with another selected block)
            if (!set.contains(new WaypointsModule.BlockPosition(b.x(), b.y() + 1, b.z()))) {
                var g = Gizmos.rect(new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), style);
                if (alwaysOnTop) g.setAlwaysOnTop();
            }
            if (!set.contains(new WaypointsModule.BlockPosition(b.x(), b.y() - 1, b.z()))) {
                var g = Gizmos.rect(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), new Vec3(x0, y0, z0), style);
                if (alwaysOnTop) g.setAlwaysOnTop();
            }
            if (!set.contains(new WaypointsModule.BlockPosition(b.x(), b.y(), b.z() - 1))) {
                var g = Gizmos.rect(new Vec3(x1, y0, z0), new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), style);
                if (alwaysOnTop) g.setAlwaysOnTop();
            }
            if (!set.contains(new WaypointsModule.BlockPosition(b.x(), b.y(), b.z() + 1))) {
                var g = Gizmos.rect(new Vec3(x0, y0, z1), new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), style);
                if (alwaysOnTop) g.setAlwaysOnTop();
            }
            if (!set.contains(new WaypointsModule.BlockPosition(b.x() - 1, b.y(), b.z()))) {
                var g = Gizmos.rect(new Vec3(x0, y0, z0), new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), style);
                if (alwaysOnTop) g.setAlwaysOnTop();
            }
            if (!set.contains(new WaypointsModule.BlockPosition(b.x() + 1, b.y(), b.z()))) {
                var g = Gizmos.rect(new Vec3(x1, y0, z1), new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), style);
                if (alwaysOnTop) g.setAlwaysOnTop();
            }
        }
    }

    private static void waypointVisuals(Minecraft mc, WaypointsModule module) {
        boolean throughWalls = module.flag("throughWalls");
        boolean highlightThroughWalls = module.flag("highlightThroughWalls");
        double limit = module.number("maxDistance");
        boolean infinite = limit <= 0 || limit >= 10000.0;
        double limitSq = infinite ? Double.POSITIVE_INFINITY : limit * limit;
        int count = 0;

        for (var p : module.active(mc)) {
            double sq = p.position().distanceToSqr(mc.player.position());
            if (sq > limitSq || ++count > 2048) continue;

            int rgb = p.color() & 0xffffff;
            int outline = ((int) (module.number("outlineAlpha") * 255) << 24) | rgb;
            int fill = ((int) (module.number("fillAlpha") * 255) << 24) | rgb;
            if (module.flag("highlightBlocks")) {
                renderConnectedBlocks(p.blocks(), module.flag("outline") ? outline : 0, (float) module.number("lineWidth"), module.flag("fill") ? fill : 0, highlightThroughWalls);
            }
            if (module.flag("beam")) {
                int beam = ((int) (module.number("beamAlpha") * 255) << 24) | rgb;
                var g = Gizmos.cuboid(new AABB(p.x() - .035, p.y(), p.z() - .035, p.x() + .035, p.y() + module.number("beamHeight"), p.z() + .035), GizmoStyle.fill(beam));
                if (highlightThroughWalls || throughWalls) g.setAlwaysOnTop();
            }
        }
    }

    /**
     * High-contrast HUD projection pass:
     * Renders crisp item icons and text labels in dark background plates directly onto the screen.
     * Guaranteed perfect contrast on snow, sky, and terrain from any distance.
     */
    private static final class ProjectedWaypoint {
        final WaypointsModule.Waypoint p;
        final double dist;
        final int screenX;
        final int screenY;
        final float baseScale;
        float finalScale;

        ProjectedWaypoint(WaypointsModule.Waypoint p, double dist, int screenX, int screenY, float baseScale) {
            this.p = p;
            this.dist = dist;
            this.screenX = screenX;
            this.screenY = screenY;
            this.baseScale = baseScale;
            this.finalScale = baseScale;
        }
    }

    public static void renderWaypointsHud(net.minecraft.client.gui.GuiGraphicsExtractor g, Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null || mc.getWindow() == null) return;
        WaypointsModule module = FeatureModule.get(WaypointsModule.class);
        if (module == null || !module.isEnabled()) return;

        var activePoints = module.active(mc);
        if (activePoints.isEmpty()) return;

        //? if >=26.2 {
        var camera = mc.gameRenderer.mainCamera();
        //?} else {
        /*var camera = mc.gameRenderer.getMainCamera();
        *///?}
        if (camera == null || !camera.isInitialized()) return;

        Vec3 camPos = camera.position();
        Matrix4f viewRotProj = camera.getViewRotationProjectionMatrix(new Matrix4f());

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        double limit = module.number("maxDistance");
        boolean infinite = limit <= 0 || limit >= 10000.0;
        double limitSq = infinite ? Double.POSITIVE_INFINITY : limit * limit;

        boolean showIcon = module.flag("showIcon");
        boolean showName = module.flag("showName");
        boolean showDist = module.flag("showDistance");
        boolean showCoords = module.flag("showCoordinates");

        double bgAlpha = module.number("backgroundAlpha");
        int bgAlphaInt = Math.max(0, Math.min(255, (int) Math.round(bgAlpha * 255)));
        int plateBg = (bgAlphaInt << 24) | 0x000000;
        if (bgAlphaInt < 40) plateBg = 0xAA000000; // default solid contrast

        java.util.List<ProjectedWaypoint> projected = new java.util.ArrayList<>();
        for (var p : activePoints) {
            double sq = p.position().distanceToSqr(camPos);
            if (sq > limitSq) continue;
            double dist = Math.sqrt(sq);

            // Slightly elevated center above block
            double rx = (p.x() + 0.5) - camPos.x;
            double ry = (p.y() + 1.25) - camPos.y;
            double rz = (p.z() + 0.5) - camPos.z;

            Vector4f clip = new Vector4f((float) rx, (float) ry, (float) rz, 1.0f);
            viewRotProj.transform(clip);

            // Behind near clipping plane
            if (clip.w <= 0.05f) continue;

            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;

            // Frustum boundary check
            if (ndcX < -1.3f || ndcX > 1.3f || ndcY < -1.3f || ndcY > 1.3f) continue;

            int screenX = Math.round((ndcX * 0.5f + 0.5f) * sw);
            int screenY = Math.round((-ndcY * 0.5f + 0.5f) * sh);

            float baseScale = balancedWaypointScale(module, dist);
            projected.add(new ProjectedWaypoint(p, dist, screenX, screenY, baseScale));
        }

        if (projected.isEmpty()) return;

        // Anti-overlap density detection: if waypoints cluster closely together in screen space,
        // dynamically scale them down so every waypoint remains legible and unobstructed!
        for (int i = 0; i < projected.size(); i++) {
            var pt = projected.get(i);
            int overlaps = 0;
            for (int j = 0; j < projected.size(); j++) {
                if (i == j) continue;
                var other = projected.get(j);
                double dx = pt.screenX - other.screenX;
                double dy = pt.screenY - other.screenY;
                if (dx * dx + dy * dy < 42.0 * 42.0) {
                    overlaps++;
                }
            }
            float crowdFactor = (float) Math.max(0.50, 1.0 - (overlaps * 0.12));
            pt.finalScale = pt.baseScale * crowdFactor;
        }

        // Back-to-front depth sort: draw farthest waypoints first, nearest waypoints last
        // so near waypoints always render cleanly in front of distant ones!
        projected.sort((a, b) -> Double.compare(b.dist, a.dist));

        for (var pw : projected) {
            var p = pw.p;
            double dist = pw.dist;
            int screenX = pw.screenX;
            int screenY = pw.screenY;
            float finalScale = pw.finalScale;

            int wpColor = p.color() | 0xFF000000;
            int accentBorder = wpColor;

            g.pose().pushMatrix();
            g.pose().translate(screenX, screenY);
            g.pose().scale(finalScale, finalScale);

            // 1. Icon in dark square plate (e.g. [ 🪣 ])
            if (showIcon) {
                int iconBoxSize = 20;
                int iconBoxX = -iconBoxSize / 2;
                int iconBoxY = -iconBoxSize - 2;

                // Translucent black plate with crisp border
                EzUi.roundedRect(g, iconBoxX - 1, iconBoxY - 1, iconBoxSize + 2, iconBoxSize + 2, 3, 0x66000000);
                EzUi.roundedRect(g, iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 2, plateBg);
                g.fill(iconBoxX, iconBoxY, iconBoxX + iconBoxSize, iconBoxY + 1, accentBorder);
                g.fill(iconBoxX, iconBoxY + iconBoxSize - 1, iconBoxX + iconBoxSize, iconBoxY + iconBoxSize, accentBorder);
                g.fill(iconBoxX, iconBoxY, iconBoxX + 1, iconBoxY + iconBoxSize, accentBorder);
                g.fill(iconBoxX + iconBoxSize - 1, iconBoxY, iconBoxX + iconBoxSize, iconBoxY + iconBoxSize, accentBorder);

                net.minecraft.world.item.ItemStack stack = WaypointsModule.resolveItemStack(p.icon());
                if (!stack.isEmpty()) {
                    g.item(stack, iconBoxX + 2, iconBoxY + 2);
                } else {
                    String symbol = getWaypointSymbol(p.icon());
                    g.centeredText(mc.font, Component.literal(symbol), 0, iconBoxY + 6, wpColor);
                }
            }

            // 2. Name & Distance Badge in dark rounded plate (e.g. [Home] or [11m])
            StringBuilder label = new StringBuilder();
            if (showName) label.append(WaypointsModule.cleanWaypointName(p.name()));
            if (showDist) {
                if (!label.isEmpty()) label.append(" ");
                label.append("[").append(Math.round(dist)).append("m]");
            }

            if (!label.isEmpty()) {
                String labelStr = label.toString();
                int textW = mc.font.width(labelStr);
                int badgeW = textW + 8;
                int badgeH = 12;
                int badgeX = -badgeW / 2;
                int badgeY = 2;

                EzUi.roundedRect(g, badgeX - 1, badgeY - 1, badgeW + 2, badgeH + 2, 3, 0x66000000);
                EzUi.roundedRect(g, badgeX, badgeY, badgeW, badgeH, 2, plateBg);

                g.centeredText(mc.font, Component.literal(labelStr), 0, badgeY + 2, 0xFFFFFFFF);
            }

            // 3. Optional Coordinates Plate underneath (cleanly shown within 100m to keep far horizons uncluttered)
            if (showCoords && dist < 100.0) {
                String coords = (int) p.x() + ", " + (int) p.y() + ", " + (int) p.z();
                int coordsW = mc.font.width(coords);
                int cBadgeW = coordsW + 6;
                int cBadgeH = 10;
                int cBadgeX = -cBadgeW / 2;
                int cBadgeY = 16;

                EzUi.roundedRect(g, cBadgeX, cBadgeY, cBadgeW, cBadgeH, 2, plateBg);
                g.centeredText(mc.font, Component.literal(coords), 0, cBadgeY + 1, 0xDDCCCCCC);
            }

            g.pose().popMatrix();
        }
    }

    /**
     * Keeps labels readable at every useful range. Size changes are intentionally
     * shallow because projection already communicates distance. During EzClient
     * zoom the HUD marker grows moderately instead of looking smaller relative to
     * the magnified world behind it.
     */
    private static float balancedWaypointScale(WaypointsModule module, double distance) {
        double min = Math.min(module.number("minScale"), module.number("maxScale"));
        double max = Math.max(module.number("minScale"), module.number("maxScale"));
        double base = Math.max(min, Math.min(max, module.number("scale")));
        double scale;

        if (distance <= 16.0) {
            scale = max;
        } else if (distance <= 96.0) {
            double t = (distance - 16.0) / 80.0;
            scale = max + (base - max) * t;
        } else {
            double t = Math.min(1.0, Math.log(distance / 96.0) / Math.log(1024.0 / 96.0));
            scale = base + (min - base) * t;
        }

        var zoom = ModuleManager.getInstance().getZoomModule();
        if (zoom != null && zoom.isEnabled() && EzClientMod.isZooming()) {
            double zoomCompensation = Math.min(1.75, Math.sqrt(Math.max(1.0, zoom.getActiveZoomLevel())));
            scale *= zoomCompensation;
        }
        return (float) scale;
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
            Gizmos.cuboid(box, new GizmoStyle(module.colorFor(entity), module.widthFor(entity), module.flag("fill") ? (module.tint("fillColor", false) & 0xff000000) | (module.colorFor(entity) & 0xffffff) : 0));
            Vec3 eye = entity.getEyePosition(partial);
            if (module.flag("eyes")) Gizmos.cuboid(new AABB(box.minX, eye.y - .005, box.minZ, box.maxX, eye.y + .005, box.maxZ), GizmoStyle.stroke(module.tint("eyeColor", false)));
            if (module.flag("look")) Gizmos.line(eye, eye.add(entity.getViewVector(partial).scale(2)), module.tint("lookColor", false), (float)module.number("width"));
        }
    }
}
