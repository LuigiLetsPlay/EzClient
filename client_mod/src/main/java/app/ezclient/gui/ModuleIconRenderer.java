package app.ezclient.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Draws direct PNG resources and never lets Minecraft's missing sprite leak into the UI. */
public final class ModuleIconRenderer {
    private ModuleIconRenderer() {}

    public static void draw(GuiGraphicsExtractor graphics, Module module, int x, int y, int size) {
        Identifier icon = module.getIcon();
        if (icon != null) {
            try {
                graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0.0F, 0.0F, size, size, size, size);
                return;
            } catch (Throwable ignored) {
                // Fallback
            }
        }
        drawFallback(graphics, module.getCategory(), x, y, size);
    }

    public static void drawTexture(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int size) {
        if (texture != null) {
            try {
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, size, size);
                return;
            } catch (Throwable ignored) {}
        }
        int pad = Math.max(1, size / 8);
        EzUi.roundedRect(graphics, x + pad, y + pad, size - 2 * pad, size - 2 * pad, size / 4, 0xFF14221B);
        int midX = x + size / 2;
        int midY = y + size / 2;
        EzUi.roundedRect(graphics, midX - 3, midY - 3, 6, 6, 2, EzUi.ACCENT_EMERALD);
    }

    private static boolean resourceExists(Identifier id) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void drawFallback(GuiGraphicsExtractor g, String category, int x, int y, int size) {
        int pad = Math.max(3, size / 7);
        int left = x + pad;
        int top = y + pad;
        int right = x + size - pad;
        int bottom = y + size - pad;
        int color = 0xFF8FA3B8;
        int accent = EzUi.ACCENT_EMERALD;

        EzUi.roundedRect(g, x, y, size, size, Math.max(4, size / 4), 0xFF18202B);
        if ("Movement".equalsIgnoreCase(category)) {
            g.fill(left + 2, top + size / 3, right - 4, bottom - 3, color);
            g.fill(right - 6, top + 2, right - 1, top + size / 2, accent);
            g.fill(right - 4, top + size / 2, right - 1, bottom - 3, accent);
        } else if ("Render".equalsIgnoreCase(category)) {
            EzUi.roundedRect(g, left, top + size / 5, right - left, size / 3, size / 6, color);
            EzUi.roundedRect(g, x + size / 2 - 2, y + size / 2 - 2, 4, 4, 2, accent);
        } else if ("HUD".equalsIgnoreCase(category)) {
            EzUi.roundedRect(g, left, top, (right - left) / 2 - 1, (bottom - top) / 2 - 1, 2, accent);
            EzUi.roundedRect(g, left + (right - left) / 2 + 1, top, (right - left) / 2 - 1, (bottom - top) / 2 - 1, 2, color);
            EzUi.roundedRect(g, left, top + (bottom - top) / 2 + 1, right - left, (bottom - top) / 2 - 1, 2, color);
        } else {
            int midX = x + size / 2;
            int midY = y + size / 2;
            EzUi.roundedRect(g, midX - 3, midY - 3, 6, 6, 2, accent);
            g.fill(left, midY - 1, midX - 4, midY + 1, color);
            g.fill(midX + 4, midY - 1, right, midY + 1, color);
        }
    }
}
