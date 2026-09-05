package app.ezclient.v1_8.gui;

import app.ezclient.v1_8.modules.Module;
import app.ezclient.v1_8.modules.ModuleManager;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * Clean & Smooth Drag & Drop HUD Layout Editor for Minecraft 1.8.9.
 */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private final long openTime;

    private Module draggingModule = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudEditorScreen(Screen parent) {
        this.parent = parent;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        // 1. Subtle dark tint background so player can see real in-game context
        RenderUtils.drawRect(0, 0, width, height, 0x50000000);

        // 2. Center instructional badge
        String headerText = "HUD LAYOUT EDITOR — Drag elements to reposition";
        int headerW = RenderUtils.getStringWidth(headerText);
        int badgeX = (width - headerW) / 2;
        RenderUtils.drawBorderedRect(badgeX - 10, 8, badgeX + headerW + 10, 24, 1.0F, 0xEE12161F, 0xFF283142);
        RenderUtils.drawString(headerText, badgeX, 12, 0xFF55FF55, true);

        // 3. Render and handle active HUD modules
        List<Module> hudModules = ModuleManager.getInstance().getHudModules();

        // Update dragged position with clamping & border snapping
        if (draggingModule != null) {
            int newX = mouseX - dragOffsetX;
            int newY = mouseY - dragOffsetY;

            // Snap to screen borders (8px magnet threshold)
            if (newX < 8) newX = 4;
            if (newY < 8) newY = 4;
            if (newX + draggingModule.getWidth() > width - 8) newX = width - draggingModule.getWidth() - 4;
            if (newY + draggingModule.getHeight() > height - 8) newY = height - draggingModule.getHeight() - 4;

            // Clamping inside screen
            newX = Math.max(0, Math.min(width - draggingModule.getWidth(), newX));
            newY = Math.max(0, Math.min(height - draggingModule.getHeight(), newY));

            draggingModule.setPosX(newX);
            draggingModule.setPosY(newY);
        }

        // Render each module
        for (Module m : hudModules) {
            if (!m.isEnabled() || !m.isShowHud()) continue;

            // Render preview
            m.renderEditorPreview(delta);

            int x = m.getPosX();
            int y = m.getPosY();
            int w = m.getWidth();
            int h = m.getHeight();

            boolean hovered = mouseX >= x - 2 && mouseX <= x + w + 2 && mouseY >= y - 2 && mouseY <= y + h + 2;
            boolean isDragging = (m == draggingModule);

            // Snug 1px outline (clean & subtle, NOT huge green boxes)
            if (isDragging) {
                RenderUtils.drawBorderedRect(x - 2, y - 2, x + w + 2, y + h + 2, 1.0F, 0x2555FF55, 0xFF55FF55);
            } else if (hovered) {
                RenderUtils.drawBorderedRect(x - 2, y - 2, x + w + 2, y + h + 2, 1.0F, 0x1555FF55, 0xAA55FF55);
            } else {
                RenderUtils.drawOutline(x - 2, y - 2, x + w + 2, y + h + 2, 1.0F, 0x40FFFFFF);
            }

            // Tag pill only on hover or drag
            if (hovered || isDragging) {
                String tag = m.getName();
                int tagW = RenderUtils.getStringWidth(tag);
                int tagX = x;
                int tagY = y - 13 >= 0 ? y - 13 : y + h + 4;
                RenderUtils.drawBorderedRect(tagX - 3, tagY - 1, tagX + tagW + 3, tagY + 9, 1.0F, 0xEE161A22, 0xFF364052);
                RenderUtils.drawString(tag, tagX, tagY, 0xFF55FF55, false);
            }
        }

        // 4. Bottom Toolbar: "Reset Positions", "Save & Exit"
        int btnH = 20;
        int btnW = 120;
        int bottomY = height - btnH - 12;

        // Reset Button (Bottom Left)
        int resetX = width / 2 - btnW - 8;
        boolean resetHover = mouseX >= resetX && mouseX <= resetX + btnW && mouseY >= bottomY && mouseY <= bottomY + btnH;
        RenderUtils.drawBorderedRect(resetX, bottomY, resetX + btnW, bottomY + btnH, 1.0F, resetHover ? 0xFF353C4D : 0xFF1E232E, 0xFF4A5266);
        int resetStrW = RenderUtils.getStringWidth("Reset Positions");
        RenderUtils.drawString("Reset Positions", resetX + (btnW - resetStrW) / 2.0F, bottomY + 6, resetHover ? 0xFFFFFFFF : 0xFFAAAAAA, false);

        // Save & Exit Button (Bottom Right)
        int saveX = width / 2 + 8;
        boolean saveHover = mouseX >= saveX && mouseX <= saveX + btnW && mouseY >= bottomY && mouseY <= bottomY + btnH;
        RenderUtils.drawBorderedRect(saveX, bottomY, saveX + btnW, bottomY + btnH, 1.0F, saveHover ? 0xFF44DD44 : 0xFF55FF55, 0xFF22AA22);
        int saveStrW = RenderUtils.getStringWidth("Save & Exit (ESC)");
        RenderUtils.drawString("Save & Exit (ESC)", saveX + (btnW - saveStrW) / 2.0F, bottomY + 6, 0xFF000000, false);

        super.render(mouseX, mouseY, delta);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            int btnH = 20;
            int btnW = 120;
            int bottomY = height - btnH - 12;

            // Check Reset button
            int resetX = width / 2 - btnW - 8;
            if (mouseX >= resetX && mouseX <= resetX + btnW && mouseY >= bottomY && mouseY <= bottomY + btnH) {
                for (Module m : ModuleManager.getInstance().getModules()) {
                    m.resetPosition();
                }
                ModuleManager.getInstance().saveConfig();
                return;
            }

            // Check Save button
            int saveX = width / 2 + 8;
            if (mouseX >= saveX && mouseX <= saveX + btnW && mouseY >= bottomY && mouseY <= bottomY + btnH) {
                ModuleManager.getInstance().saveConfig();
                if (client != null) client.setScreen(parent);
                return;
            }

            // Check clicking on a module to drag
            List<Module> hudModules = ModuleManager.getInstance().getHudModules();
            for (int i = hudModules.size() - 1; i >= 0; i--) {
                Module m = hudModules.get(i);
                if (!m.isEnabled() || !m.isShowHud()) continue;

                int x = m.getPosX();
                int y = m.getPosY();
                int w = m.getWidth();
                int h = m.getHeight();

                if (mouseX >= x - 2 && mouseX <= x + w + 2 && mouseY >= y - 2 && mouseY <= y + h + 2) {
                    draggingModule = m;
                    dragOffsetX = mouseX - x;
                    dragOffsetY = mouseY - y;
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0 && draggingModule != null) {
            draggingModule = null;
            ModuleManager.getInstance().saveConfig();
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyPressed(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            ModuleManager.getInstance().saveConfig();
            if (client != null) client.setScreen(parent);
            return;
        }
        if (keyCode == Keyboard.KEY_RSHIFT) {
            if (System.currentTimeMillis() - openTime > 250) {
                ModuleManager.getInstance().saveConfig();
                if (client != null) client.setScreen(parent);
            }
            return;
        }
        super.keyPressed(typedChar, keyCode);
    }
}
