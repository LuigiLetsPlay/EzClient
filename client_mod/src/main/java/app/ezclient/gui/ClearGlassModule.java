package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ClearGlassModule extends Module {
    private static volatile boolean active;
    private static volatile boolean connected = true;
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
        connected = connectedGlass;
        ConfigManager.save();
        triggerWorldRendererReload();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        active = enabled;
        triggerWorldRendererReload();
    }

    /** Hot render-path access without singleton lookups during chunk compilation. */
    public static boolean isConnectedRenderingActive() {
        return active && connected;
    }

    private void triggerWorldRendererReload() {
        Minecraft client = Minecraft.getInstance();
        if (client.levelRenderer != null && client.level != null && client.gameRenderer != null) {
            try {
                client.levelRenderer.invalidateCompiledGeometry(
                    client.level,
                    client.options,
                    client.gameRenderer.mainCamera(),
                    client.getBlockColors()
                );
            } catch (Throwable ignored) {}
        }
    }
}
