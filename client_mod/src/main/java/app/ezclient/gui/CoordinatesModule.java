package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Advanced Coordinates & Compass HUD with Single-Line, Multi-Line,
 * and Badlion/Skyrim-style Compass Tape Bar, decimal precision,
 * Biome detection, and calculated Nether coordinates.
 */
public final class CoordinatesModule extends HudModule {
    public enum CopyFormat { LABELED, PLAIN }
    public enum LayoutMode {
        SINGLE_LINE,
        MULTI_LINE,
        COMPASS_BAR
    }

    private LayoutMode layoutMode = LayoutMode.MULTI_LINE;
    private int decimalPrecision = 0; // 0, 1, 2
    private boolean showBiome = false;
    private boolean showDirection = true;
    private boolean showNether = false;
    private int labelColor = 0xFF888888;
    private CopyFormat copyFormat = CopyFormat.LABELED;

    public CoordinatesModule() {
        super("Coordinates", "HUD", true, 6, 38, "XYZ: ", "");
    }

    @Override
    public String getDescription() {
        return "Zeigt Position, Blickrichtung, Biom und optionale Nether-Koordinaten an.";
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/coordinates.png");
    }

    public LayoutMode getLayoutMode() { return layoutMode; }
    public void setLayoutMode(LayoutMode layoutMode) { this.layoutMode = layoutMode; ConfigManager.save(); }

    public int getDecimalPrecision() { return decimalPrecision; }
    public void setDecimalPrecision(int decimalPrecision) { this.decimalPrecision = Math.max(0, Math.min(2, decimalPrecision)); ConfigManager.save(); }

    public boolean isShowBiome() { return showBiome; }
    public void setShowBiome(boolean showBiome) { this.showBiome = showBiome; ConfigManager.save(); }

    public boolean isShowDirection() { return showDirection; }
    public void setShowDirection(boolean showDirection) { this.showDirection = showDirection; ConfigManager.save(); }

    public boolean isShowNether() { return showNether; }
    public void setShowNether(boolean showNether) { this.showNether = showNether; ConfigManager.save(); }

    public int getLabelColor() { return labelColor; }
    public void setLabelColor(int labelColor) { this.labelColor = labelColor; ConfigManager.save(); }

    public CopyFormat getCopyFormat() { return copyFormat; }
    public void setCopyFormat(CopyFormat copyFormat) { this.copyFormat = copyFormat == null ? CopyFormat.LABELED : copyFormat; ConfigManager.save(); }
    public String getCopyFormatLabel() { return copyFormat == CopyFormat.LABELED ? "X: 100 Y: 100 Z: 100" : "100 100 100"; }

    public void copyCurrentCoordinates(Minecraft client) {
        if (client == null || client.player == null || client.keyboardHandler == null) return;
        int x = (int)Math.floor(client.player.getX());
        int y = (int)Math.floor(client.player.getY());
        int z = (int)Math.floor(client.player.getZ());
        String coordinates = copyFormat == CopyFormat.LABELED
                ? "X: " + x + " Y: " + y + " Z: " + z
                : x + " " + y + " " + z;
        client.keyboardHandler.setClipboard(coordinates);
        client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Koordinaten kopiert: " + coordinates));
    }

    // Legacy getters/setters for compatibility
    public boolean isMultiLine() { return layoutMode == LayoutMode.MULTI_LINE; }
    public void setMultiLine(boolean multiLine) { this.layoutMode = multiLine ? LayoutMode.MULTI_LINE : LayoutMode.SINGLE_LINE; }

    private String formatCoord(double val) {
        if (decimalPrecision == 0) return Integer.toString((int) Math.floor(val));
        long factor = decimalPrecision == 1 ? 10L : 100L;
        long scaled = Math.round(val * factor);
        boolean negative = scaled < 0;
        long absolute = Math.abs(scaled);
        long fraction = absolute % factor;
        String fractionText = decimalPrecision == 2 && fraction < 10 ? "0" + fraction : Long.toString(fraction);
        return (negative ? "-" : "") + (absolute / factor) + "." + fractionText;
    }

    @Override
    public int getWidth(Minecraft client) {
        return getWidth(client, false);
    }

    @Override
    public int getWidth(Minecraft client, boolean editor) {
        if (client == null || client.font == null) return 80;
        if (layoutMode == LayoutMode.COMPASS_BAR) {
            return 160;
        }
        if (layoutMode == LayoutMode.SINGLE_LINE && !showBiome && !showNether && !showDirection) {
            return client.font.width(displayText(client, editor)) + CONTENT_PADDING_X * 2;
        }

        int maxW = client.font.width(displayText(client, editor));
        double px = (client.player != null) ? client.player.getX() : 120;
        double py = (client.player != null) ? client.player.getY() : 64;
        double pz = (client.player != null) ? client.player.getZ() : -350;

        if (layoutMode == LayoutMode.MULTI_LINE) {
            maxW = Math.max(maxW, client.font.width("X: " + formatCoord(px)));
            maxW = Math.max(maxW, client.font.width("Y: " + formatCoord(py)));
            maxW = Math.max(maxW, client.font.width("Z: " + formatCoord(pz)));
        }
        String direction = (client.player != null) ? getDirectionString(client) : app.ezclient.util.EzI18n.get("ezclient.direction.north") + " (-Z)";
        String biome = (client.player != null) ? getBiomeString(client) : net.minecraft.network.chat.Component.translatable("biome.minecraft.plains").getString();
        String nether = (client.player != null) ? getNetherCoordsString(client) : "15 / -43";
        if (showDirection) maxW = Math.max(maxW, client.font.width(directionLabel() + direction));
        if (showBiome) maxW = Math.max(maxW, client.font.width(biomeLabel() + biome));
        if (showNether) maxW = Math.max(maxW, client.font.width(netherLabel() + nether));

        return maxW + CONTENT_PADDING_X * 2;
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        if (!editor || (client != null && client.player != null)) return displayText(client);
        return getPrefix() + formatCoord(120) + " / " + formatCoord(64) + " / " + formatCoord(-350) + getSuffix();
    }

    @Override
    public int getHeight(Minecraft client) {
        if (layoutMode == LayoutMode.COMPASS_BAR) {
            return 26;
        }
        int lines = layoutMode == LayoutMode.MULTI_LINE ? 3 : 1;
        if (showDirection) lines++;
        if (showBiome) lines++;
        if (showNether) lines++;
        return lines * 11 + CONTENT_PADDING_Y * 2;
    }

    public String getDirectionString(Minecraft client) {
        if (client == null || client.player == null) return "North (-Z)";
        Direction dir = client.player.getDirection();
        String axis = switch (dir) {
            case NORTH -> "-Z";
            case SOUTH -> "+Z";
            case WEST -> "-X";
            case EAST -> "+X";
            default -> "";
        };
        return app.ezclient.util.EzI18n.get("ezclient.direction." + dir.getName()) + " (" + axis + ")";
    }

    public String getBiomeString(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return "Plains";
        BlockPos pos = client.player.blockPosition();
        Holder<Biome> biome = client.level.getBiome(pos);
        if (biome.unwrapKey().isPresent()) {
            String path = biome.unwrapKey().get().identifier().getPath();
            return net.minecraft.network.chat.Component.translatable(
                    "biome." + biome.unwrapKey().get().identifier().getNamespace() + "." + path).getString();
        }
        return app.ezclient.util.EzI18n.get("ezclient.hud.unknown");
    }

    public String getNetherCoordsString(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return "0 / 0";
        if (client.level.dimension() == Level.NETHER) {
            return ((int) client.player.getX() * 8) + " / " + ((int) client.player.getZ() * 8);
        } else {
            return ((int) client.player.getX() / 8) + " / " + ((int) client.player.getZ() / 8);
        }
    }

    @Override
    protected String value(Minecraft client) {
        if (client == null || client.player == null) return "0 / 0 / 0";
        return formatCoord(client.player.getX()) + " / " + formatCoord(client.player.getY()) + " / " + formatCoord(client.player.getZ());
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        int totalW = getWidth(client, editor);
        int totalH = getHeight(client, editor);
        float scale = (float) getScale();
        int renderX = getRenderX(client, totalW, editor);
        int renderY = getRenderY(client, totalH, editor);

        graphics.pose().pushMatrix();
        graphics.pose().translate(renderX, renderY);
        graphics.pose().scale(scale, scale);

        renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);

        int valCol = color();
        int lblCol = isRainbow() ? color(50L) : labelColor;

        if (layoutMode == LayoutMode.COMPASS_BAR) {
            renderCompassBar(graphics, client, totalW, totalH, valCol, editor);
            graphics.pose().popMatrix();
            return;
        }

        int y = CONTENT_PADDING_Y;

        if (layoutMode == LayoutMode.MULTI_LINE) {
            double px = (client.player != null) ? client.player.getX() : 120;
            double py = (client.player != null) ? client.player.getY() : 64;
            double pz = (client.player != null) ? client.player.getZ() : -350;

            int lwX = client.font.width("X: " + formatCoord(px));
            int lineX1 = Math.max(CONTENT_PADDING_X, (totalW - lwX) / 2);
            graphics.text(client.font, "X: ", lineX1, y, lblCol);
            graphics.text(client.font, formatCoord(px), lineX1 + client.font.width("X: "), y, valCol);
            y += 11;

            int lwY = client.font.width("Y: " + formatCoord(py));
            int lineX2 = Math.max(CONTENT_PADDING_X, (totalW - lwY) / 2);
            graphics.text(client.font, "Y: ", lineX2, y, lblCol);
            graphics.text(client.font, formatCoord(py), lineX2 + client.font.width("Y: "), y, valCol);
            y += 11;

            int lwZ = client.font.width("Z: " + formatCoord(pz));
            int lineX3 = Math.max(CONTENT_PADDING_X, (totalW - lwZ) / 2);
            graphics.text(client.font, "Z: ", lineX3, y, lblCol);
            graphics.text(client.font, formatCoord(pz), lineX3 + client.font.width("Z: "), y, valCol);
            y += 11;
        } else {
            String single = displayText(client, editor);
            int lw = client.font.width(single);
            int lineX = Math.max(CONTENT_PADDING_X, (totalW - lw) / 2);
            graphics.text(client.font, single, lineX, y, valCol);
            y += 11;
        }

        if (showDirection) {
            String dirStr = (client.player != null) ? getDirectionString(client) : app.ezclient.util.EzI18n.get("ezclient.direction.north") + " (-Z)";
            String label = directionLabel();
            int lw = client.font.width(label + dirStr);
            int lineX = Math.max(CONTENT_PADDING_X, (totalW - lw) / 2);
            graphics.text(client.font, label, lineX, y, lblCol);
            graphics.text(client.font, dirStr, lineX + client.font.width(label), y, 0xFFE0E0E0);
            y += 11;
        }

        if (showBiome) {
            String biomeStr = (client.player != null) ? getBiomeString(client) : net.minecraft.network.chat.Component.translatable("biome.minecraft.plains").getString();
            String label = biomeLabel();
            int lw = client.font.width(label + biomeStr);
            int lineX = Math.max(CONTENT_PADDING_X, (totalW - lw) / 2);
            graphics.text(client.font, label, lineX, y, lblCol);
            graphics.text(client.font, biomeStr, lineX + client.font.width(label), y, 0xFF43DD8C);
            y += 11;
        }

        if (showNether) {
            String netherStr = (client.player != null) ? getNetherCoordsString(client) : "15 / -43";
            String label = netherLabel();
            int lw = client.font.width(label + netherStr);
            int lineX = Math.max(CONTENT_PADDING_X, (totalW - lw) / 2);
            graphics.text(client.font, label, lineX, y, lblCol);
            graphics.text(client.font, netherStr, lineX + client.font.width(label), y, 0xFFFF7744);
        }

        graphics.pose().popMatrix();
    }

    private void renderCompassBar(GuiGraphicsExtractor g, Minecraft client, int w, int h, int color, boolean editor) {
        float yaw = (client != null && client.player != null) ? client.player.getYRot() : 0.0f;
        yaw = (yaw % 360.0f + 360.0f) % 360.0f;

        int centerX = w / 2;

        // Center indicator marker
        g.fill(centerX - 1, 4, centerX + 1, 8, 0xFFFF4444);

        String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        var waypoints = FeatureModule.get(WaypointsModule.class);
        if (!editor && client.player != null && waypoints.isEnabled() && waypoints.flag("compass")) {
            int drawn = 0;
            for (var waypoint : waypoints.active(client)) {
                if (++drawn > 32) break;
                var delta = waypoint.position().subtract(client.player.position());
                double bearing = Math.toDegrees(Math.atan2(-delta.x, delta.z));
                float diff = net.minecraft.util.Mth.wrapDegrees((float)bearing - yaw);
                float pos = centerX + diff;
                if (pos >= 8 && pos <= w - 8) {
                    g.text(client.font, waypoint.icon(), (int)pos - 2, 4, waypoint.color(), isTextShadow());
                    g.fill((int)pos, 19, (int)pos + 1, 22, waypoint.color());
                }
            }
        }
        int[] degrees = {0, 45, 90, 135, 180, 225, 270, 315};

        for (int i = 0; i < 8; i++) {
            float deg = degrees[i];
            float diff = deg - yaw;
            while (diff < -180.0f) diff += 360.0f;
            while (diff > 180.0f) diff -= 360.0f;

            float posX = centerX + diff * 1.0f;
            if (posX >= 8 && posX <= w - 8) {
                String label = directions[i];
                int tw = client.font.width(label);
                int tCol = (label.length() == 1) ? color : 0xFFAAAAAA;
                g.text(client.font, label, (int) (posX - tw / 2), 10, tCol);
                g.fill((int) posX, 19, (int) posX + 1, 21, 0x60FFFFFF);
            }
        }
    }

    private static String directionLabel() { return app.ezclient.util.EzI18n.get("ezclient.hud.coords.facing"); }
    private static String biomeLabel() { return app.ezclient.util.EzI18n.get("ezclient.hud.coords.biome"); }
    private static String netherLabel() { return app.ezclient.util.EzI18n.get("ezclient.hud.coords.nether"); }

    @Override
    public void setKeyBind(int keyBind) {
        super.setKeyBind(keyBind);
        EzKeyBindings.setKeyCode(EzKeyBindings.KEY_COPY_COORDINATES, keyBind);
    }
}
