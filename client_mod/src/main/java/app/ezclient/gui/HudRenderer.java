package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer {
    private HudRenderer() {}
    public static void draw(GuiGraphicsExtractor graphics, HudModule module, boolean editor) {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled() && !editor) return;
        String text = module.displayText(client);
        float scale = (float) module.getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(module.getX(), module.getY());
        graphics.pose().scale(scale, scale);
        int w = client.font.width(text) + 8;
        if (module.hasBackground()) graphics.fill(0, 0, w, 13, 0xA8111419);
        if (editor) graphics.outline(0, 0, w, 13, module.isEnabled() ? 0xFF43DD8C : 0xFF777777);
        graphics.text(client.font, text, 4, 3, module.color());
        graphics.pose().popMatrix();
    }
}
