package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ClearGlassModule extends Module {
    private boolean connectedGlass = true;

    public ClearGlassModule() {
        super("Clear Glass", "RENDER", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/clearglass.png");
    }

    public boolean isConnectedGlass() { return connectedGlass; }
    public void setConnectedGlass(boolean connectedGlass) {
        this.connectedGlass = connectedGlass;
        ConfigManager.save();
        triggerWorldRendererReload();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        triggerWorldRendererReload();
    }

    private void triggerWorldRendererReload() {
        Minecraft client = Minecraft.getInstance();
        if (client.levelRenderer != null) {
            client.levelRenderer.resetLevelRenderData();
        }
    }
}
