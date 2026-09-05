package app.ezclient.v1_16_v1_20.gui;

import app.ezclient.v1_16_v1_20.EzClientMod_1_16_1_20;
import app.ezclient.v1_16_v1_20.modules.Module;
import app.ezclient.v1_16_v1_20.modules.ModuleManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HudEditorScreen extends Screen {
    private final Screen parent;
    private Module draggingModule = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudEditorScreen(Screen parent) {
        super(Text.of("EzClient HUD Editor"));
        this.parent = parent;
    }

    //? if <=1.19.4 {
    /*@Override
    public void render(net.minecraft.client.util.math.MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderEditor(mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderEditor(mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
    //?}

    private void renderEditor(int mouseX, int mouseY, float delta) {
        // Dark translucent overlay
        RenderUtils.drawRect(0, 0, width, height, 0x60000000);

        // Header instructions
        String title = "EzClient HUD Layout Editor";
        int tw = RenderUtils.getStringWidth(title);
        RenderUtils.drawBorderedRect(width / 2.0F - tw / 2.0F - 12, 10, width / 2.0F + tw / 2.0F + 12, 28, 1.0F, 0xDD151923, 0xFF55FF55);
        RenderUtils.drawString(title, width / 2.0F - tw / 2.0F, 15, 0xFF55FF55, true);

        String sub = "Drag elements to reposition | Press [ESC] to save and exit";
        int sw = RenderUtils.getStringWidth(sub);
        RenderUtils.drawString(sub, width / 2.0F - sw / 2.0F, 33, 0xFFAAAAAA, true);

        List<Module> hudModules = ModuleManager.getInstance().getHudModules();

        // Render preview and 1px snug bounding boxes
        for (Module m : hudModules) {
            if (!m.isEnabled() || !m.isShowHud()) continue;

            int x = m.getPosX();
            int y = m.getPosY();
            int w = m.getWidth();
            int h = m.getHeight();

            boolean hover = mouseX >= x - 2 && mouseX <= x + w + 2 && mouseY >= y - 2 && mouseY <= y + h + 2;
            boolean dragging = (draggingModule == m);

            m.renderEditorPreview(delta);

            if (dragging) {
                RenderUtils.drawOutline(x - 2, y - 2, x + w + 2, y + h + 2, 1.0F, 0xFF55FF55);
            } else if (hover) {
                RenderUtils.drawOutline(x - 2, y - 2, x + w + 2, y + h + 2, 1.0F, 0xAA55FF55);
            } else {
                RenderUtils.drawOutline(x - 2, y - 2, x + w + 2, y + h + 2, 1.0F, 0x40FFFFFF);
            }
        }

        // Update dragged position
        if (draggingModule != null) {
            draggingModule.setPosX(Math.max(2, Math.min(width - draggingModule.getWidth() - 2, mouseX - dragOffsetX)));
            draggingModule.setPosY(Math.max(2, Math.min(height - draggingModule.getHeight() - 2, mouseY - dragOffsetY)));
        }

        // Save & Exit Button in bottom center
        int btnW = 90;
        int btnH = 20;
        int btnX = (width - btnW) / 2;
        int btnY = height - 30;
        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

        RenderUtils.drawBorderedRect(btnX, btnY, btnX + btnW, btnY + btnH, 1.0F, btnHover ? 0xFF44DD44 : 0xFF55FF55, 0xFF22AA22);
        int bStrW = RenderUtils.getStringWidth("Save & Exit");
        RenderUtils.drawString("Save & Exit", btnX + (btnW - bStrW) / 2.0F, btnY + 6, 0xFF000000, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Save & Exit button click
            int btnW = 90;
            int btnH = 20;
            int btnX = (width - btnW) / 2;
            int btnY = height - 30;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                ModuleManager.getInstance().saveConfig();
                EzClientMod_1_16_1_20.openScreen(client, parent);
                return true;
            }

            // Check clicked HUD module
            List<Module> hudModules = ModuleManager.getInstance().getHudModules();
            for (Module m : hudModules) {
                if (!m.isEnabled() || !m.isShowHud()) continue;
                int x = m.getPosX();
                int y = m.getPosY();
                int w = m.getWidth();
                int h = m.getHeight();

                if (mouseX >= x - 2 && mouseX <= x + w + 2 && mouseY >= y - 2 && mouseY <= y + h + 2) {
                    draggingModule = m;
                    dragOffsetX = (int) (mouseX - x);
                    dragOffsetY = (int) (mouseY - y);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingModule != null) {
            draggingModule = null;
            ModuleManager.getInstance().saveConfig();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
