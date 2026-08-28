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

    public CoordinatesModule() {
        super("Coordinates", "HUD", true, 6, 38, "XYZ: ", "");
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
        if (client == null || client.font == null) return 80;
        if (layoutMode == LayoutMode.COMPASS_BAR) {
            return 160;
        }
        if (layoutMode == LayoutMode.SINGLE_LINE && !showBiome && !showNether && !showDirection) {
            return client.font.width(displayText(client)) + 8;
        }

        int maxW = client.font.width(displayText(client));
        double px = (client.player != null) ? client.player.getX() : 120;
        double py = (client.player != null) ? client.player.getY() : 64;
        double pz = (client.player != null) ? client.player.getZ() : -350;

        if (layoutMode == LayoutMode.MULTI_LINE) {
            maxW = Math.max(maxW, client.font.width("X: " + formatCoord(px)));
            maxW = Math.max(maxW, client.font.width("Y: " + formatCoord(py)));
            maxW = Math.max(maxW, client.font.width("Z: " + formatCoord(pz)));
        }
        if (showDirection) maxW = Math.max(maxW, client.font.width("Facing: " + getDirectionString(client)));
        if (showBiome) maxW = Math.max(maxW, client.font.width("Biome: " + getBiomeString(client)));
        if (showNether) maxW = Math.max(maxW, client.font.width("Nether: " + getNetherCoordsString(client)));

        return maxW + 8;
    }

    @Override
    public int getHeight(Minecraft client) {
        if (layoutMode == LayoutMode.COMPASS_BAR) {
            return 20;
        }
        int lines = layoutMode == LayoutMode.MULTI_LINE ? 3 : 1;
        if (showDirection) lines++;
        if (showBiome) lines++;
        if (showNether) lines++;
        return lines * 11 + 3;
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
        return dir.getName().substring(0, 1).toUpperCase() + dir.getName().substring(1) + " (" + axis + ")";
    }

    public String getBiomeString(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return "Plains";
        BlockPos pos = client.player.blockPosition();
        Holder<Biome> biome = client.level.getBiome(pos);
        if (biome.unwrapKey().isPresent()) {
            String path = biome.unwrapKey().get().identifier().getPath();
            return path.replace('_', ' ');
        }
        return "Unknown";
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
        float scale = (float) getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(scale, scale);

        int totalW = getWidth(client);
        int totalH = getHeight(client);

        renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);

        int valCol = color();
        int lblCol = isRainbow() ? color(50L) : labelColor;

        if (layoutMode == LayoutMode.COMPASS_BAR) {
            renderCompassBar(graphics, client, totalW, totalH, valCol, editor);
            graphics.pose().popMatrix();
            return;
        }

        int y = 2;

        if (layoutMode == LayoutMode.MULTI_LINE) {
            double px = (client.player != null && !editor) ? client.player.getX() : 120;
            double py = (client.player != null && !editor) ? client.player.getY() : 64;
            double pz = (client.player != null && !editor) ? client.player.getZ() : -350;

            graphics.text(client.font, "X: ", 4, y, lblCol);
            graphics.text(client.font, formatCoord(px), 4 + client.font.width("X: "), y, valCol);
            y += 11;

            graphics.text(client.font, "Y: ", 4, y, lblCol);
            graphics.text(client.font, formatCoord(py), 4 + client.font.width("Y: "), y, valCol);
            y += 11;

            graphics.text(client.font, "Z: ", 4, y, lblCol);
            graphics.text(client.font, formatCoord(pz), 4 + client.font.width("Z: "), y, valCol);
            y += 11;
        } else {
            graphics.text(client.font, displayText(client), 4, y, valCol);
            y += 11;
        }

        if (showDirection) {
            String dirStr = editor ? "North (-Z)" : getDirectionString(client);
            graphics.text(client.font, "Facing: ", 4, y, lblCol);
            graphics.text(client.font, dirStr, 4 + client.font.width("Facing: "), y, 0xFFE0E0E0);
            y += 11;
        }

        if (showBiome) {
            String biomeStr = editor ? "Plains" : getBiomeString(client);
            graphics.text(client.font, "Biome: ", 4, y, lblCol);
            graphics.text(client.font, biomeStr, 4 + client.font.width("Biome: "), y, 0xFF43DD8C);
            y += 11;
        }

        if (showNether) {
            String netherStr = editor ? "15 / -43" : getNetherCoordsString(client);
            graphics.text(client.font, "Nether: ", 4, y, lblCol);
            graphics.text(client.font, netherStr, 4 + client.font.width("Nether: "), y, 0xFFFF7744);
        }

        graphics.pose().popMatrix();
    }

    private void renderCompassBar(GuiGraphicsExtractor g, Minecraft client, int w, int h, int color, boolean editor) {
        float yaw = (client.player != null && !editor) ? client.player.getYRot() : 0.0f;
        yaw = (yaw % 360.0f + 360.0f) % 360.0f;

        int centerX = w / 2;

        // Center indicator marker
        g.fill(centerX - 1, 1, centerX + 1, 5, 0xFFFF4444);

        String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
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
                g.text(client.font, label, (int) (posX - tw / 2), 7, tCol);
                g.fill((int) posX, 16, (int) posX + 1, 18, 0x60FFFFFF);
            }
        }
    }
}
