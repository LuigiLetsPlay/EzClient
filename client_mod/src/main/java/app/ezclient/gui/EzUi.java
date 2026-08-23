package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small, texture-free primitives used by the modern EzClient screens. */
final class EzUi {
    private EzUi() {}

    static void roundedRect(GuiGraphicsExtractor g, int x, int y, int width, int height, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) { g.fill(x, y, x + width, y + height, color); return; }
        g.fill(x + r, y, x + width - r, y + height, color);
        g.fill(x, y + r, x + width, y + height - r, color);
        for (int row = 0; row < r; row++) {
            int inset = (int) Math.ceil(Math.sqrt((double) r * r - (r - row - 1) * (double) (r - row - 1)));
            g.fill(x + r - inset, y + row, x + width - r + inset, y + row + 1, color);
            g.fill(x + r - inset, y + height - row - 1, x + width - r + inset, y + height - row, color);
        }
    }

    static void panel(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        roundedRect(g, x + 2, y + 3, width, height, 16, 0x50000000);
        roundedRect(g, x, y, width, height, 16, 0xF21B202B);
        roundedRect(g, x, y, width, height, 16, 0x263D4858);
        roundedRect(g, x + 1, y + 1, width - 2, height - 2, 15, 0xF21B202B);
    }

    static void card(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean active) {
        roundedRect(g, x, y, width, height, 11, active ? 0xFF252B38 : 0xE0151921);
        roundedRect(g, x, y, width, 1, 11, active ? 0xFF9D7BFF : 0xFF30394A);
    }
}
