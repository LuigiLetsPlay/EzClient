package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer {
    private HudRenderer() {}

    public static void draw(GuiGraphicsExtractor graphics, HudModule module, boolean editor) {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled() && !editor) return;

        if (module instanceof KeystrokesModule keystrokes) {
            keystrokes.renderCustom(graphics, client, editor);
            return;
        }

        if (module instanceof CpsModule cps) {
            cps.renderCustom(graphics, client, editor);
            return;
        }

        if (module instanceof FpsModule fps) {
            fps.renderCustom(graphics, client, editor);
            return;
        }

        if (module instanceof ArmorStatusModule armor) {
            armor.renderCustom(graphics, client, editor);
            return;
        }

        if (module instanceof CoordinatesModule coords) {
            coords.renderCustom(graphics, client, editor);
            return;
        }

        if (module instanceof PotionEffectModule potion) {
            potion.renderCustom(graphics, client, editor);
            return;
        }

        if (module instanceof ToggleSprintSneakModule toggleSprint) {
            toggleSprint.renderCustom(graphics, client, editor);
            return;
        }

        if (module instanceof CrosshairModule crosshair) {
            if (editor) crosshair.renderCustom(graphics, client, true);
            return;
        }

        // Generic HudModule rendering with systemwide Badlion styling
        String text = module.displayText(client);
        float scale = (float) module.getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(module.getX(), module.getY());
        graphics.pose().scale(scale, scale);

        int w = client.font.width(text) + 8;
        int h = module.getHeight(client);

        module.renderBackgroundAndBorder(graphics, 0, 0, w, h);
        graphics.text(client.font, text, 4, 3, module.color());

        graphics.pose().popMatrix();
    }
}
