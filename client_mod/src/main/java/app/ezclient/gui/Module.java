package app.ezclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public abstract class Module {
    private String name;
    private boolean enabled;
    private String category;
    private int keyBind = -1;
    private boolean lastKeyState = false;

    public Module(String name, String category, boolean defaultEnabled) {
        this.name = name;
        this.category = category;
        this.enabled = defaultEnabled;
    }

    public String getName() { return name; }
    public String getDisplayName() {
        String cleanId = name.toLowerCase().replaceAll("[^a-z0-9]", "");
        return app.ezclient.util.EzI18n.getOrDefault("ezclient.module." + cleanId + ".name", name);
    }
    public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/module.png"); }
    public boolean isEnabled() { return enabled; }
    public String getCategory() { return category; }
    public int getKeyBind() { return keyBind; }
    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind >= GLFW.GLFW_KEY_SPACE && keyBind <= GLFW.GLFW_KEY_LAST ? keyBind : -1;
        ConfigManager.save();
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            onToggle();
            ConfigManager.save();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void onTick() {
        if (keyBind != -1) {
            Minecraft client = Minecraft.getInstance();
            if (client.getWindow() != null) {
                boolean isDown = keyBind >= GLFW.GLFW_KEY_SPACE
                        && keyBind <= GLFW.GLFW_KEY_LAST
                        && InputConstants.isKeyDown(client.getWindow(), keyBind);
                // Debounce to trigger only on initial press
                if (isDown && !lastKeyState) {
                    toggle();
                }
                lastKeyState = isDown;
            }
        }
    }

    protected void onToggle() {}

    // Methods for building settings UI
    public boolean hasSettings() { return false; }
    public boolean mouseClickedSettings(double mouseX, double mouseY, int button, int x, int y, int width, int height) { return false; }
}
