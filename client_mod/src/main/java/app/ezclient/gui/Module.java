package app.ezclient.gui;

import net.minecraft.client.KeyMapping;

public abstract class Module {
    private String name;
    private boolean enabled;
    private String category;
    private KeyMapping keyBinding;

    public Module(String name, String category, boolean defaultEnabled) {
        this.name = name;
        this.category = category;
        this.enabled = defaultEnabled;
    }

    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public String getCategory() { return category; }
    public KeyMapping getKeyBinding() { return keyBinding; }
    public void setKeyBinding(KeyMapping keyBinding) { this.keyBinding = keyBinding; }

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
        if (keyBinding != null && keyBinding.consumeClick()) {
            toggle();
        }
    }

    protected void onToggle() {}

    // Methods for building settings UI
    public boolean hasSettings() { return false; }
    public boolean mouseClickedSettings(double mouseX, double mouseY, int button, int x, int y, int width, int height) { return false; }
}
