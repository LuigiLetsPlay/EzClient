package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

public final class ClearGlassModule extends Module {
    private static volatile boolean active;

    public ClearGlassModule() {
        super("Clear Glass", "RENDER", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/clearglass.png");
    }

    @Override
    public void setEnabled(boolean enabled) {
        active = enabled;
        super.setEnabled(enabled);
    }

    /** Hot render-path access without singleton lookups during chunk compilation. */
    public static boolean isConnectedRenderingActive() {
        return active;
    }

    @Override
    protected void onToggle() {
        // Connected-texture decisions are baked into chunk meshes. Without a
        // renderer invalidation the switch changes state but all visible
        // chunks keep their old glass geometry until the player moves away.
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.level != null) {
                //? if >=26.2 {
                if (client.levelExtractor != null) {
                    client.levelExtractor.allChanged();
                }
                //?} else {
                /*if (client.levelRenderer != null) {
                    client.levelRenderer.allChanged();
                }
                *///?}
            }
        });
    }

    @Override
    public boolean hasSettings() {
        return false;
    }
}
