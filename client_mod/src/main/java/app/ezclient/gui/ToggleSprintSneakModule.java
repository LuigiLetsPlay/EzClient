package app.ezclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Advanced Badlion-style ToggleSprint & ToggleSneak module with custom status HUD,
 * sprint/sneak latches, fly boost multipliers, and complete HUD hiding.
 */
public final class ToggleSprintSneakModule extends HudModule {
    public enum SprintMode {
        HOLD,
        TOGGLE
    }

    public enum SneakMode {
        HOLD,
        TOGGLE
    }

    private SprintMode sprintMode = SprintMode.TOGGLE;
    private SneakMode sneakMode = SneakMode.HOLD;
    private boolean isSprintToggled = true;
    private boolean isSneakToggled = false;
    private boolean hideHud = false;
    private float flyBoostMultiplier = 1.0f;
    private String customSprintingText = "[Sprinting (Toggled)]";
    private String customSneakingText = "[Sneaking (Toggled)]";
    private String customVanillaText = "[Vanilla]";

    private boolean wasSprintKeyPressed = false;
    private boolean wasSneakKeyPressed = false;
    private int wallCollisionCooldown = 0;

    public ToggleSprintSneakModule() {
        super("ToggleSprint", "MOVEMENT", true, 6, 120, "", "");
        this.customSprintingText = app.ezclient.util.EzI18n.get("ezclient.hud.sprinting_toggled");
        this.customSneakingText = app.ezclient.util.EzI18n.get("ezclient.hud.sneaking_toggled");
        this.customVanillaText = app.ezclient.util.EzI18n.get("ezclient.hud.vanilla");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/auto_sprint.png");
    }

    public SprintMode getSprintMode() { return sprintMode; }
    public void setSprintMode(SprintMode sprintMode) { this.sprintMode = sprintMode; ConfigManager.save(); }

    public SneakMode getSneakMode() { return sneakMode; }
    public void setSneakMode(SneakMode sneakMode) {
        this.sneakMode = sneakMode;
        if (sneakMode != SneakMode.TOGGLE) {
            isSneakToggled = false;
            Minecraft client = Minecraft.getInstance();
            if (client.options != null) {
                client.options.keyShift.setDown(false);
            }
        }
        ConfigManager.save();
    }

    public boolean isSprintToggled() { return isSprintToggled; }
    public void setSprintToggled(boolean isSprintToggled) { this.isSprintToggled = isSprintToggled; }

    public boolean isSneakToggled() { return isSneakToggled; }
    public void setSneakToggled(boolean isSneakToggled) { this.isSneakToggled = isSneakToggled; }

    public boolean isHideHud() { return hideHud; }
    public void setHideHud(boolean hideHud) { this.hideHud = hideHud; ConfigManager.save(); }

    // Backward compatibility for config
    public boolean isHideWhenInactive() { return hideHud; }
    public void setHideWhenInactive(boolean val) { setHideHud(val); }

    public float getFlyBoostMultiplier() { return flyBoostMultiplier; }
    public void setFlyBoostMultiplier(float flyBoostMultiplier) { this.flyBoostMultiplier = Math.max(1.0f, Math.min(5.0f, flyBoostMultiplier)); ConfigManager.save(); }

    public String getCustomSprintingText() { return customSprintingText; }
    public void setCustomSprintingText(String customSprintingText) { this.customSprintingText = customSprintingText; ConfigManager.save(); }

    public String getCustomSneakingText() { return customSneakingText; }
    public void setCustomSneakingText(String customSneakingText) { this.customSneakingText = customSneakingText; ConfigManager.save(); }

    public String getCustomVanillaText() { return customVanillaText; }
    public void setCustomVanillaText(String customVanillaText) { this.customVanillaText = customVanillaText; ConfigManager.save(); }

    @Override
    protected void onToggle() {
        if (!isEnabled()) {
            Minecraft client = Minecraft.getInstance();
            if (client.options != null) {
                client.options.keyShift.setDown(false);
            }
            isSneakToggled = false;
        }
    }

    @Override
    public void onTick() {
        super.onTick();
        if (!isEnabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options == null) return;

        // ToggleSprint keypress handler
        boolean sprintDown = client.options.keySprint.isDown();
        if (sprintDown && !wasSprintKeyPressed) {
            if (sprintMode == SprintMode.TOGGLE) {
                isSprintToggled = !isSprintToggled;
            }
        }
        wasSprintKeyPressed = sprintDown;

        // ToggleSneak physical keypress handler
        boolean physicalShiftDown = client.getWindow() != null
                && InputConstants.isKeyDown(client.getWindow(), ((app.ezclient.mixin.KeyMappingAccessor) client.options.keyShift).ezclient$getKey().getValue());
        if (physicalShiftDown && !wasSneakKeyPressed) {
            if (sneakMode == SneakMode.TOGGLE) {
                isSneakToggled = !isSneakToggled;
                if (!isSneakToggled) {
                    client.options.keyShift.setDown(false);
                }
            }
        }
        wasSneakKeyPressed = physicalShiftDown;

        // Apply toggle sneak
        if (sneakMode == SneakMode.TOGGLE && isSneakToggled) {
            if (EzScreenBridge.current(client) == null) {
                client.options.keyShift.setDown(true);
            } else {
                client.options.keyShift.setDown(false);
            }
        }

        // Wall collision debounce (fixes sprint FOV shake / stutter against obstacles)
        if (client.player.horizontalCollision) {
            wallCollisionCooldown = 10;
        } else if (wallCollisionCooldown > 0) {
            wallCollisionCooldown--;
        }

        // Apply sprint
        if (sprintMode == SprintMode.TOGGLE && isSprintToggled) {
            if (client.options.keyUp.isDown() && !client.player.isCrouching()
                    && wallCollisionCooldown == 0 && client.player.getFoodData().getFoodLevel() > 6) {
                client.player.setSprinting(true);
            }
        }

        // Apply fly boost
        if (client.player.getAbilities().flying && flyBoostMultiplier > 1.0f) {
            if (client.options.keySprint.isDown()) {
                client.player.getAbilities().setFlyingSpeed(0.05f * flyBoostMultiplier);
            } else {
                client.player.getAbilities().setFlyingSpeed(0.05f);
            }
        }
    }

    public String getCurrentStatus(Minecraft client) {
        if (client == null || client.player == null) return customSprintingText;
        if (client.player.isSprinting()) {
            return sprintMode == SprintMode.TOGGLE ? customSprintingText : app.ezclient.util.EzI18n.get("ezclient.hud.sprinting_key");
        }
        if (client.player.isCrouching()) {
            return sneakMode == SneakMode.TOGGLE ? customSneakingText : app.ezclient.util.EzI18n.get("ezclient.hud.sneaking_key");
        }
        if (client.player.getAbilities().flying) {
            return app.ezclient.util.EzI18n.get("ezclient.hud.flying", flyBoostMultiplier);
        }
        return isSprintToggled ? customSprintingText : customVanillaText;
    }

    @Override
    public int getWidth(Minecraft client) {
        if (client == null || client.font == null) return 80;
        return client.font.width(displayText(client)) + 8;
    }

    @Override
    public int getHeight(Minecraft client) {
        return 14;
    }

    @Override
    protected String value(Minecraft client) {
        return getCurrentStatus(client);
    }

    @Override
    public String displayText(Minecraft client) {
        if (hideHud) return "";
        return getPrefix() + getCurrentStatus(client) + getSuffix();
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        if (editor) {
            String text = getPrefix() + customSprintingText + getSuffix();
            return text.trim().isEmpty() ? "[Sprinting (Toggled)]" : text;
        }
        return displayText(client);
    }

    public void renderCustom(GuiGraphicsExtractor graphics, Minecraft client, boolean editor) {
        if (hideHud && !editor) {
            return;
        }

        float scale = (float) getScale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX(), getY());
        graphics.pose().scale(scale, scale);

        String text = displayText(client, editor);

        int totalW = (client != null && client.font != null) ? client.font.width(text) + 8 : 80;
        int totalH = getHeight(client);

        renderBackgroundAndBorder(graphics, 0, 0, totalW, totalH);

        graphics.text(client.font, text, 4, 3, color());
        graphics.pose().popMatrix();
    }
}
