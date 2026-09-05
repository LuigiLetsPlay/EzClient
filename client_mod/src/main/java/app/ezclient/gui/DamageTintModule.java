package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;

/**
 * Damage Tint / Hit Color Module:
 * Customizes entity hurt flash colors away from vanilla red to custom RGBA hues or chroma effects.
 */
public final class DamageTintModule extends Module {
    public enum TargetScope {
        ALL_ENTITIES("All Entities"),
        PLAYERS_ONLY("Players Only"),
        SELF_ONLY("Self Only");

        private final String label;
        TargetScope(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private TargetScope targetScope = TargetScope.ALL_ENTITIES;
    private int customColor = 0xFFFF2255; // Vibrant Pink-Red
    private int customAlpha = 180; // 0 to 255
    private boolean chromaMode = false;
    private float flashDurationMultiplier = 1.0f; // 0.5x to 2.0x

    public DamageTintModule() {
        super("Damage Tint", "Render", false);
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/armor_status.png");
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    protected void onToggle() {
        applyToTexture(Minecraft.getInstance());
    }

    public TargetScope getTargetScope() { return targetScope; }
    public void setTargetScope(TargetScope targetScope) { this.targetScope = targetScope; ConfigManager.save(); applyToTexture(Minecraft.getInstance()); }

    public int getCustomColor() { return customColor; }
    public void setCustomColor(int customColor) { this.customColor = customColor; ConfigManager.save(); applyToTexture(Minecraft.getInstance()); }

    public int getCustomAlpha() { return customAlpha; }
    public void setCustomAlpha(int customAlpha) { this.customAlpha = Math.max(0, Math.min(255, customAlpha)); ConfigManager.save(); applyToTexture(Minecraft.getInstance()); }

    public boolean isChromaMode() { return chromaMode; }
    public void setChromaMode(boolean chromaMode) { this.chromaMode = chromaMode; ConfigManager.save(); applyToTexture(Minecraft.getInstance()); }

    public float getFlashDurationMultiplier() { return flashDurationMultiplier; }
    public void setFlashDurationMultiplier(float flashDurationMultiplier) { this.flashDurationMultiplier = Math.max(0.5f, Math.min(2.0f, flashDurationMultiplier)); ConfigManager.save(); }

    public void applyToTexture(Minecraft client) {
        if (client == null || client.gameRenderer == null) return;
        try {
            OverlayTexture overlay = client.gameRenderer.overlayTexture();
            if (overlay == null) return;
            DynamicTexture tex = ((app.ezclient.mixin.OverlayTextureAccessor) overlay).ezclient$getTexture();
            if (tex != null && tex.getPixels() != null) {
                var img = tex.getPixels();
                int tint = getTint(null, false);
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 16; x++) {
                        img.setPixel(x, y, tint);
                    }
                }
                tex.upload();
            }
        } catch (Throwable ignored) {}
    }

    public int getTint(Entity entity, boolean isSelf) {
        if (!isEnabled()) return 0xB3FF0000;

        if (targetScope == TargetScope.PLAYERS_ONLY && !(entity instanceof Player)) {
            return 0xB3FF0000;
        }
        if (targetScope == TargetScope.SELF_ONLY && !isSelf) {
            return 0xB3FF0000;
        }

        int alpha = customAlpha;
        if (chromaMode) {
            float hue = (float) ((System.currentTimeMillis() % 2000L) / 2000.0);
            int rgb = Color.HSBtoRGB(hue, 0.9f, 1.0f) & 0x00FFFFFF;
            return (alpha << 24) | rgb;
        }

        return (alpha << 24) | (customColor & 0x00FFFFFF);
    }
}
