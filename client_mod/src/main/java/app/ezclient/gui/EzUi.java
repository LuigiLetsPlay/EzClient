package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Ultra-clean, minimalist UI design tokens for EzClient in-game screens. */
public final class EzUi {
    // ── Signature Color Tokens ──
    public static final int ACCENT_EMERALD = 0xFF22C96E;
    public static final int ACCENT_EMERALD_HOVER = 0xFF38E384;
    public static final int ACCENT_EMERALD_BG = 0xFF143D2A;
    public static final int ACCENT_EMERALD_BG_HOVER = 0xFF1A5238;

    public static final int BG_PANEL = 0xF50D111A;
    public static final int BG_PANEL_HEADER = 0xF8121722;
    public static final int BG_CARD = 0xF0151A24;
    public static final int BG_CARD_HOVER = 0xF81C2332;
    public static final int BG_CARD_ACTIVE = 0xF012211C;

    public static final int BORDER_SUBTLE = 0xFF202736;
    public static final int BORDER_HOVER = 0xFF334155;
    public static final int BORDER_ACTIVE = 0xFF22C96E;

    // ── High Contrast Typography ──
    public static final int TEXT_WHITE = 0xFFFFFFFF;
    public static final int TEXT_LIGHT = 0xFFE2E8F0;
    public static final int TEXT_MUTED = 0xFF838E9E;
    public static final int TEXT_DIM = 0xFF586273;

    private EzUi() {}

    public static void roundedRect(GuiGraphicsExtractor g, int x, int y, int width, int height, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            g.fill(x, y, x + width, y + height, color);
            return;
        }

        // 1. Central vertical column (spanning full height between corners)
        g.fill(x + r, y, x + width - r, y + height, color);

        // 2. Left and right vertical middle strips (between corner vertical ranges)
        if (height > 2 * r) {
            g.fill(x, y + r, x + r, y + height - r, color);
            g.fill(x + width - r, y + r, x + width, y + height - r, color);
        }

        // 3. Four corner curves (rendered strictly without overlapping the center)
        for (int row = 0; row < r; row++) {
            int inset = (int) Math.ceil(Math.sqrt((double) r * r - (r - row - 1) * (double) (r - row - 1)));
            int cornerW = Math.max(0, Math.min(r, inset));
            if (cornerW > 0) {
                // Top-Left
                g.fill(x + r - cornerW, y + row, x + r, y + row + 1, color);
                // Top-Right
                g.fill(x + width - r, y + row, x + width - r + cornerW, y + row + 1, color);
                // Bottom-Left
                g.fill(x + r - cornerW, y + height - row - 1, x + r, y + height - row, color);
                // Bottom-Right
                g.fill(x + width - r, y + height - row - 1, x + width - r + cornerW, y + height - row, color);
            }
        }
    }

    /** Soft emerald glow line (3px with fading alpha). */
    public static void glowLine(GuiGraphicsExtractor g, int x, int y, int width) {
        g.fill(x, y - 1, x + width, y, 0x1822C96E);
        g.fill(x, y, x + width, y + 1, ACCENT_EMERALD);
        g.fill(x, y + 1, x + width, y + 2, 0x1822C96E);
    }

    /** Pill-shaped category filter button. */
    public static void pillButton(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean active, boolean hovered) {
        if (active) {
            roundedRect(g, x, y, width, height, height / 2, ACCENT_EMERALD);
        } else if (hovered) {
            roundedRect(g, x, y, width, height, height / 2, 0xFF252D3D);
        } else {
            roundedRect(g, x, y, width, height, height / 2, 0xFF1A1F2B);
        }
    }

    /** Tiny toggle switch indicator (6x6 dot). */
    public static void toggleDot(GuiGraphicsExtractor g, int x, int y, boolean on) {
        int color = on ? ACCENT_EMERALD : 0xFF3A4050;
        roundedRect(g, x, y, 6, 6, 3, color);
        if (on) {
            // Subtle glow ring
            roundedRect(g, x - 1, y - 1, 8, 8, 4, 0x3022C96E);
        }
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int width, int height) {
        // Drop shadow
        roundedRect(g, x + 2, y + 3, width, height, 10, 0x40000000);

        // Panel background & border
        roundedRect(g, x, y, width, height, 10, BORDER_SUBTLE);
        roundedRect(g, x + 1, y + 1, width - 2, height - 2, 9, BG_PANEL);

        // Subtle top header background tint
        for (int row = 0; row < 28; row++) {
            float t = (float) row / 27.0f;
            int alpha = (int) (0x28 * (1.0f - t));
            int color = (alpha << 24) | 0x1E293B;
            int rr = (row < 8) ? 8 - row : 0;
            if (rr > 0) {
                g.fill(x + 1 + rr, y + 1 + row, x + width - 1 - rr, y + 2 + row, color);
            } else {
                g.fill(x + 1, y + 1 + row, x + width - 1, y + 2 + row, color);
            }
        }
    }

    public static void heroCard(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean hovered, boolean active) {
        int bg = active ? (hovered ? 0xF8162822 : BG_CARD_ACTIVE)
                : (hovered ? BG_CARD_HOVER : BG_CARD);
        int border = active ? (hovered ? ACCENT_EMERALD_HOVER : BORDER_ACTIVE)
                : (hovered ? BORDER_HOVER : BORDER_SUBTLE);

        roundedRect(g, x, y, width, height, 8, border);
        roundedRect(g, x + 1, y + 1, width - 2, height - 2, 7, bg);
    }

    public static void card(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean active, boolean hovered) {
        moduleCard(g, x, y, width, height, active, hovered);
    }

    public static void card(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean active) {
        moduleCard(g, x, y, width, height, active, false);
    }

    public static void moduleCard(GuiGraphicsExtractor g, int x, int y, int width, int height, boolean active, boolean hovered) {
        int bg = active ? (hovered ? 0xF8162D24 : BG_CARD_ACTIVE)
                : (hovered ? BG_CARD_HOVER : BG_CARD);
        int border = active ? (hovered ? ACCENT_EMERALD_HOVER : BORDER_ACTIVE)
                : (hovered ? BORDER_HOVER : BORDER_SUBTLE);

        // Card body
        roundedRect(g, x, y, width, height, 6, border);
        roundedRect(g, x + 1, y + 1, width - 2, height - 2, 5, bg);

        // Hover glow effect (very subtle)
        if (hovered) {
            roundedRect(g, x, y, width, 1, 0, 0x1022C96E);
            roundedRect(g, x, y + height - 1, width, 1, 0, 0x1022C96E);
        }
    }

    public static void badge(GuiGraphicsExtractor g, int x, int y, int width, int height, int bgColor, int borderColor, String text, int textColor) {
        roundedRect(g, x, y, width, height, 4, borderColor);
        roundedRect(g, x + 1, y + 1, width - 2, height - 2, 3, bgColor);
        g.centeredText(Minecraft.getInstance().font, Component.literal(text), x + width / 2, y + (height - 8) / 2, textColor);
    }
}
