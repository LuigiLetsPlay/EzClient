package app.ezclient.v1_16_v1_20.gui;

import app.ezclient.v1_16_v1_20.EzClientMod_1_16_1_20;
import app.ezclient.v1_16_v1_20.modules.CrosshairModule;
import app.ezclient.v1_16_v1_20.modules.Module;
import app.ezclient.v1_16_v1_20.modules.ModuleManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final Module module;

    private int panelX, panelY, panelW, panelH;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Text.of(module.getName() + " Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        super.init();
        panelW = 280;
        panelH = (module instanceof CrosshairModule) ? 190 : 150;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    //? if <=1.19.4 {
    /*@Override
    public void render(net.minecraft.client.util.math.MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderModal(mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderModal(mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
    //?}

    private void renderModal(int mouseX, int mouseY, float delta) {
        // Dark translucent backdrop
        RenderUtils.drawRect(0, 0, width, height, 0x75000000);

        // Glass Panel
        RenderUtils.drawGlassPanel(panelX, panelY, panelW, panelH, 0xF210141D, 0xFA0B0E14, 0xFF2B3446, 0x1A55FF55);

        // Header
        RenderUtils.drawGradientRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 28, 0xFF171D29, 0xFF121721);
        RenderUtils.drawRect(panelX, panelY + 27, panelX + panelW, panelY + 28, 0xFF273042);

        String title = module.getName() + " Settings";
        RenderUtils.drawString(title, panelX + 12, panelY + 10, 0xFF55FF55, true);

        // Close button [X]
        int closeX = panelX + panelW - 20;
        int closeY = panelY + 7;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14;
        RenderUtils.drawBorderedRect(closeX, closeY, closeX + 14, closeY + 14, 1.0F, closeHover ? 0xFFDD3333 : 0xFF222836, closeHover ? 0xFFFF4444 : 0xFF3A445A);
        RenderUtils.drawString("✕", closeX + 4, closeY + 3, 0xFFFFFFFF, false);

        int curY = panelY + 36;

        // 1. Enable / Disable Toggle
        RenderUtils.drawString("Module State:", panelX + 16, curY + 4, 0xFFCCCCCC, false);
        int togW = 50;
        int togX = panelX + panelW - togW - 16;
        boolean togHov = mouseX >= togX && mouseX <= togX + togW && mouseY >= curY && mouseY <= curY + 16;
        int togBg = module.isEnabled() ? 0xFF55FF55 : (togHov ? 0xFF353C4D : 0xFF242A36);
        RenderUtils.drawBorderedRect(togX, curY, togX + togW, curY + 16, 1.0F, togBg, module.isEnabled() ? 0xFF22AA22 : 0xFF384357);
        String togTxt = module.isEnabled() ? "ON" : "OFF";
        int tW = RenderUtils.getStringWidth(togTxt);
        RenderUtils.drawString(togTxt, togX + (togW - tW) / 2.0F, curY + 4, module.isEnabled() ? 0xFF000000 : 0xFF7A8396, false);

        curY += 22;

        if (module.hasOptionalHud()) {
            // 2. HUD Visibility
            RenderUtils.drawString("Show on HUD:", panelX + 16, curY + 4, 0xFFCCCCCC, false);
            int hudW = 60;
            int hudX = panelX + panelW - hudW - 16;
            boolean hudHov = mouseX >= hudX && mouseX <= hudX + hudW && mouseY >= curY && mouseY <= curY + 16;
            int hudBg = module.isShowHud() ? 0xFF263828 : (hudHov ? 0xFF353C4D : 0xFF1E232E);
            RenderUtils.drawBorderedRect(hudX, curY, hudX + hudW, curY + 16, 1.0F, hudBg, module.isShowHud() ? 0xFF55FF55 : 0xFF384357);
            String hudTxt = module.isShowHud() ? "Visible" : "Hidden";
            int hW = RenderUtils.getStringWidth(hudTxt);
            RenderUtils.drawString(hudTxt, hudX + (hudW - hW) / 2.0F, curY + 4, module.isShowHud() ? 0xFF55FF55 : 0xFF777E90, false);

            curY += 22;
        }

        if (module.hasHudElement()) {
            // 3. Color Theme
            RenderUtils.drawString("Text Color:", panelX + 16, curY + 4, 0xFFCCCCCC, false);
            int colW = 74;
            int colX = panelX + panelW - colW - 16;
            boolean colHov = mouseX >= colX && mouseX <= colX + colW && mouseY >= curY && mouseY <= curY + 16;
            RenderUtils.drawBorderedRect(colX, curY, colX + colW, curY + 16, 1.0F, colHov ? 0xFF2B3342 : 0xFF1C212C, 0xFF364156);
            String colStr = module.getColorName();
            int cW = RenderUtils.getStringWidth(colStr);
            RenderUtils.drawString(colStr, colX + (colW - cW) / 2.0F, curY + 4, module.getTextColor(0xFFFFFFFF), false);

            curY += 22;

            // 4. Background Box
            RenderUtils.drawString("Background Tint:", panelX + 16, curY + 4, 0xFFCCCCCC, false);
            int bgW = 60;
            int bgX = panelX + panelW - bgW - 16;
            boolean bgHov = mouseX >= bgX && mouseX <= bgX + bgW && mouseY >= curY && mouseY <= curY + 16;
            RenderUtils.drawBorderedRect(bgX, curY, bgX + bgW, curY + 16, 1.0F, module.isShowBackground() ? 0xFF263828 : (bgHov ? 0xFF353C4D : 0xFF1E232E), module.isShowBackground() ? 0xFF55FF55 : 0xFF384357);
            String bgTxt = module.isShowBackground() ? "Box ON" : "Box OFF";
            int bW = RenderUtils.getStringWidth(bgTxt);
            RenderUtils.drawString(bgTxt, bgX + (bgW - bW) / 2.0F, curY + 4, module.isShowBackground() ? 0xFF55FF55 : 0xFF777E90, false);

            curY += 22;
        }

        // Custom Crosshair options
        if (module instanceof CrosshairModule) {
            CrosshairModule ch = (CrosshairModule) module;
            RenderUtils.drawString("Center Dot:", panelX + 16, curY + 4, 0xFFCCCCCC, false);
            int dotW = 50;
            int dotX = panelX + panelW - dotW - 16;
            boolean dotHov = mouseX >= dotX && mouseX <= dotX + dotW && mouseY >= curY && mouseY <= curY + 16;
            RenderUtils.drawBorderedRect(dotX, curY, dotX + dotW, curY + 16, 1.0F, ch.isDrawDot() ? 0xFF263828 : (dotHov ? 0xFF353C4D : 0xFF1E232E), ch.isDrawDot() ? 0xFF55FF55 : 0xFF384357);
            String dTxt = ch.isDrawDot() ? "ON" : "OFF";
            int dW = RenderUtils.getStringWidth(dTxt);
            RenderUtils.drawString(dTxt, dotX + (dotW - dW) / 2.0F, curY + 4, ch.isDrawDot() ? 0xFF55FF55 : 0xFF777E90, false);
            curY += 22;
        }

        // Save & Done Button
        int doneW = 100;
        int doneH = 18;
        int doneX = panelX + (panelW - doneW) / 2;
        int doneY = panelY + panelH - 26;
        boolean doneHov = mouseX >= doneX && mouseX <= doneX + doneW && mouseY >= doneY && mouseY <= doneY + doneH;
        RenderUtils.drawBorderedRect(doneX, doneY, doneX + doneW, doneY + doneH, 1.0F, doneHov ? 0xFF44DD44 : 0xFF55FF55, 0xFF22AA22);
        int doneTxtW = RenderUtils.getStringWidth("Save & Done");
        RenderUtils.drawString("Save & Done", doneX + (doneW - doneTxtW) / 2.0F, doneY + 5, 0xFF000000, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Close button [X]
            int closeX = panelX + panelW - 20;
            int closeY = panelY + 7;
            if (mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14) {
                ModuleManager.getInstance().saveConfig();
                EzClientMod_1_16_1_20.openScreen(client, parent);
                return true;
            }

            // Save & Done button
            int doneW = 100;
            int doneH = 18;
            int doneX = panelX + (panelW - doneW) / 2;
            int doneY = panelY + panelH - 26;
            if (mouseX >= doneX && mouseX <= doneX + doneW && mouseY >= doneY && mouseY <= doneY + doneH) {
                ModuleManager.getInstance().saveConfig();
                EzClientMod_1_16_1_20.openScreen(client, parent);
                return true;
            }

            int curY = panelY + 36;

            // 1. Toggle Button
            int togW = 50;
            int togX = panelX + panelW - togW - 16;
            if (mouseX >= togX && mouseX <= togX + togW && mouseY >= curY && mouseY <= curY + 16) {
                module.toggle();
                ModuleManager.getInstance().saveConfig();
                return true;
            }

            curY += 22;

            if (module.hasOptionalHud()) {
                // 2. HUD Visibility
                int hudW = 60;
                int hudX = panelX + panelW - hudW - 16;
                if (mouseX >= hudX && mouseX <= hudX + hudW && mouseY >= curY && mouseY <= curY + 16) {
                    module.toggleShowHud();
                    ModuleManager.getInstance().saveConfig();
                    return true;
                }

                curY += 22;
            }

            if (module.hasHudElement()) {
                // 3. Color Mode
                int colW = 74;
                int colX = panelX + panelW - colW - 16;
                if (mouseX >= colX && mouseX <= colX + colW && mouseY >= curY && mouseY <= curY + 16) {
                    module.cycleColor();
                    ModuleManager.getInstance().saveConfig();
                    return true;
                }

                curY += 22;

                // 4. Background
                int bgW = 60;
                int bgX = panelX + panelW - bgW - 16;
                if (mouseX >= bgX && mouseX <= bgX + bgW && mouseY >= curY && mouseY <= curY + 16) {
                    module.toggleShowBackground();
                    ModuleManager.getInstance().saveConfig();
                    return true;
                }

                curY += 22;
            }

            // Custom crosshair dot
            if (module instanceof CrosshairModule) {
                CrosshairModule ch = (CrosshairModule) module;
                int dotW = 50;
                int dotX = panelX + panelW - dotW - 16;
                if (mouseX >= dotX && mouseX <= dotX + dotW && mouseY >= curY && mouseY <= curY + 16) {
                    ch.setDrawDot(!ch.isDrawDot());
                    ModuleManager.getInstance().saveConfig();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            ModuleManager.getInstance().saveConfig();
            EzClientMod_1_16_1_20.openScreen(client, parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
