package app.ezclient.v1_8.gui;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import java.awt.Color;

/**
 * OpenGL 1.1 / Fixed-Function rendering utility for legacy Minecraft (1.8.9).
 */
public final class RenderUtils {
    private RenderUtils() {}

    public static void drawRect(float left, float top, float right, float bottom, int color) {
        if (left > right) {
            float temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            float temp = top;
            top = bottom;
            bottom = temp;
        }

        float a = (float) ((color >> 24) & 0xFF) / 255.0F;
        float r = (float) ((color >> 16) & 0xFF) / 255.0F;
        float g = (float) ((color >> 8) & 0xFF) / 255.0F;
        float b = (float) (color & 0xFF) / 255.0F;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, top);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glEnd();

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    public static void drawBorderedRect(float left, float top, float right, float bottom, float borderWidth, int fillColor, int borderColor) {
        drawRect(left, top, right, bottom, fillColor);

        // Draw 1px snug border lines
        drawRect(left, top, right, top + borderWidth, borderColor); // Top
        drawRect(left, bottom - borderWidth, right, bottom, borderColor); // Bottom
        drawRect(left, top + borderWidth, left + borderWidth, bottom - borderWidth, borderColor); // Left
        drawRect(right - borderWidth, top + borderWidth, right, bottom - borderWidth, borderColor); // Right
    }

    public static void drawOutline(float left, float top, float right, float bottom, float borderWidth, int borderColor) {
        drawRect(left, top, right, top + borderWidth, borderColor); // Top
        drawRect(left, bottom - borderWidth, right, bottom, borderColor); // Bottom
        drawRect(left, top + borderWidth, left + borderWidth, bottom - borderWidth, borderColor); // Left
        drawRect(right - borderWidth, top + borderWidth, right, bottom - borderWidth, borderColor); // Right
    }

    public static void drawGlassPanel(float x, float y, float w, float h, int bgStart, int bgEnd, int borderColor, int glowColor) {
        // 1. Soft ambient glow behind panel
        if ((glowColor & 0xFF000000) != 0) {
            drawOutline(x - 2, y - 2, x + w + 2, y + h + 2, 2.0F, glowColor);
        }

        // 2. Glass gradient fill
        drawGradientRect(x, y, x + w, y + h, bgStart, bgEnd);

        // 3. Crisp 1px glass border
        drawOutline(x, y, x + w, y + h, 1.0F, borderColor);
    }

    public static void drawGradientRect(float left, float top, float right, float bottom, int startColor, int endColor) {
        if (left > right) {
            float temp = left;
            left = right;
            right = temp;
        }
        if (top > bottom) {
            float temp = top;
            top = bottom;
            bottom = temp;
        }

        float sa = (float) ((startColor >> 24) & 0xFF) / 255.0F;
        float sr = (float) ((startColor >> 16) & 0xFF) / 255.0F;
        float sg = (float) ((startColor >> 8) & 0xFF) / 255.0F;
        float sb = (float) (startColor & 0xFF) / 255.0F;

        float ea = (float) ((endColor >> 24) & 0xFF) / 255.0F;
        float er = (float) ((endColor >> 16) & 0xFF) / 255.0F;
        float eg = (float) ((endColor >> 8) & 0xFF) / 255.0F;
        float eb = (float) (endColor & 0xFF) / 255.0F;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(sr, sg, sb, sa);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glColor4f(er, eg, eb, ea);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    public static void drawString(String text, float x, float y, int color, boolean shadow) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null || text == null) return;
        if (shadow) {
            client.textRenderer.drawWithShadow(text, x, y, color);
        } else {
            client.textRenderer.draw(text, (int) x, (int) y, color);
        }
    }

    public static int getStringWidth(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.textRenderer == null || text == null) return 0;
        return client.textRenderer.getStringWidth(text);
    }

    public static int getRainbow(float speedSeconds, int offset) {
        long time = System.currentTimeMillis() + offset;
        float hue = (time % (long) (speedSeconds * 1000.0F)) / (speedSeconds * 1000.0F);
        int rgb = Color.HSBtoRGB(hue, 0.75F, 1.0F);
        return 0xFF000000 | rgb;
    }
}
