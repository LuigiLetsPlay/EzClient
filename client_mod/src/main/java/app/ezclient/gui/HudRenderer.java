package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudRenderer {
    private HudRenderer() {}

    /**
     * Renders the module through its normal editor path and centers the resulting
     * HUD geometry in the supplied preview area without changing saved X/Y values.
     */
    public static void drawCenteredPreview(GuiGraphicsExtractor graphics, HudModule module,
                                           int centerX, int centerY, int maxWidth, int maxHeight) {
        Minecraft client = Minecraft.getInstance();
        int moduleWidth = Math.max(1, module.getWidth(client, true));
        int moduleHeight = Math.max(1, module.getHeight(client, true));
        float moduleScale = (float) module.getScale();
        float renderedWidth = moduleWidth * moduleScale;
        float renderedHeight = moduleHeight * moduleScale;
        float fitScale = Math.min(1.0f, Math.min(
                Math.max(1, maxWidth) / renderedWidth,
                Math.max(1, maxHeight) / renderedHeight
        ));

        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(fitScale, fitScale);
        graphics.pose().translate(
                -module.getX() - renderedWidth / 2.0f,
                -module.getY() - renderedHeight / 2.0f
        );
        draw(graphics, module, true);
        graphics.pose().popMatrix();
    }

    public static void draw(GuiGraphicsExtractor graphics, HudModule module, boolean editor) {
        Minecraft client = Minecraft.getInstance();
        if (!module.isEnabled() && !editor) return;
        if (!editor && EzScreenBridge.hudHidden(client)) return;
        if (module instanceof FeatureModule feature) {
            feature.renderFeature(graphics, client, editor); return;
        }

        if (module instanceof ChatCustomizerModule chat) {
            chat.renderCustom(graphics, client, editor);
            return;
        }

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
        if (module instanceof PingModule ping) {
            ping.renderCustom(graphics, client, editor);
            return;
        }
        if (module instanceof CrosshairModule crosshair) {
            if (editor) crosshair.renderCustom(graphics, client, true);
            return;
        }

        // Generic HudModule rendering with systemwide Badlion styling
        String text = module.displayText(client, editor);
        float scale = (float) module.getScale();
        int w = module.getWidth(client, editor);
        int h = module.getHeight(client, editor);
        int renderX = module.getRenderX(client, w, editor);
        int renderY = module.getRenderY(client, h, editor);

        graphics.pose().pushMatrix();
        graphics.pose().translate(renderX, renderY);
        graphics.pose().scale(scale, scale);

        int padX = (module.hasBackground() || module.hasBorder()) ? HudModule.CONTENT_PADDING_X : 2;
        int padY = (module.hasBackground() || module.hasBorder()) ? HudModule.CONTENT_PADDING_Y : 1;
        module.renderBackgroundAndBorder(graphics, 0, 0, w, h);
        int textW = client.font.width(module.styledText(text));
        int textX = Math.max(padX, (w - textW) / 2);
        graphics.text(client.font, module.styledText(text), textX,
                padY, module.color(), module.isTextShadow());

        graphics.pose().popMatrix();
    }
}
