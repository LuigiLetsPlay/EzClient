package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final Module module;

    private int panelX, panelY, panelWidth, panelHeight;
    private boolean isListeningForHotkey = false;

    public ModuleSettingsScreen(Screen parent, Module module) {
        super(Component.literal(module.getDisplayName() + " " + app.ezclient.util.EzI18n.get("ezclient.module_settings.title").replace("%s ", "")));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        panelWidth = 280;
        panelHeight = (module instanceof FullbrightModule) ? 172 : 130;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        addRenderableWidget(new EzButton(
                panelX + panelWidth - 26, panelY + 6, 18, 16,
                Component.literal("✕"), false, ignored -> onClose()
        ));

        int curY = panelY + 44;

        // Custom module-specific toggles
        if (module instanceof FullbrightModule fullbright) {
            addRenderableWidget(new EzButton(
                    panelX + 16, curY, 118, 18,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.module_settings.fullbright_nether", app.ezclient.util.EzI18n.onOrOff(!fullbright.isDisableInNether()))), !fullbright.isDisableInNether(),
                    b -> {
                        fullbright.setDisableInNether(!fullbright.isDisableInNether());
                        rebuildWidgets();
                    }
            ));
            addRenderableWidget(new EzButton(
                    panelX + 142, curY, 118, 18,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.module_settings.fullbright_end", app.ezclient.util.EzI18n.onOrOff(!fullbright.isDisableInEnd()))), !fullbright.isDisableInEnd(),
                    b -> {
                        fullbright.setDisableInEnd(!fullbright.isDisableInEnd());
                        rebuildWidgets();
                    }
            ));
            curY += 22;

            addRenderableWidget(new EzButton(
                    panelX + 16, curY, 244, 18,
                    Component.literal(app.ezclient.util.EzI18n.get("ezclient.module_settings.fullbright_smooth", app.ezclient.util.EzI18n.onOrOff(fullbright.isSmoothFade()))), fullbright.isSmoothFade(),
                    b -> {
                        fullbright.setSmoothFade(!fullbright.isSmoothFade());
                        rebuildWidgets();
                    }
            ));
            curY += 24;
        }

        // Hotkey Button
        addRenderableWidget(new EzButton(
                panelX + (panelWidth - 150) / 2, curY, 150, 18,
                Component.literal(app.ezclient.util.EzI18n.get("ezclient.module_settings.hotkey", getKeyName())), false,
                b -> isListeningForHotkey = true
        ));

        // Done button
        addRenderableWidget(new EzButton(
                panelX + (panelWidth - 90) / 2, panelY + panelHeight - 24, 90, 16,
                app.ezclient.util.EzI18n.comp("ezclient.module_settings.done"), true,
                b -> onClose()
        ));
    }

    private String getKeyName() {
        if (isListeningForHotkey) return "...";
        int key = module.getKeyBind();
        if (key == -1) return app.ezclient.util.EzI18n.get("ezclient.module_settings.none");
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name == null || name.isEmpty()) {
            if (key == GLFW.GLFW_KEY_LEFT_SHIFT) return "LShift";
            if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RShift";
            if (key == GLFW.GLFW_KEY_LEFT_CONTROL) return "LCtrl";
            if (key == GLFW.GLFW_KEY_RIGHT_CONTROL) return "RCtrl";
            if (key == GLFW.GLFW_KEY_LEFT_ALT) return "LAlt";
            if (key == GLFW.GLFW_KEY_RIGHT_ALT) return "RAlt";
            if (key == GLFW.GLFW_KEY_SPACE) return "Space";
            return "Key " + key;
        }
        return name.toUpperCase();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (isListeningForHotkey) {
            return true; 
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isListeningForHotkey) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                module.setKeyBind(-1);
            } else {
                module.setKeyBind(event.key());
            }
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float d) {
        extractTransparentBackground(g);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        g.pose().pushMatrix();
        g.pose().translate(panelX + 14, panelY + 9);
        g.pose().scale(1.15f, 1.15f);
        g.text(font, app.ezclient.util.EzI18n.get("ezclient.module_settings.title", module.getDisplayName()), 0, 0, EzUi.TEXT_WHITE);
        g.pose().popMatrix();

        g.fill(panelX + 14, panelY + 28, panelX + panelWidth - 14, panelY + 29, EzUi.BORDER_SUBTLE);
        
        super.extractRenderState(g, mx, my, d);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }
}
