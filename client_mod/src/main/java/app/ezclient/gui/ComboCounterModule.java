package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

/**
 * Combo Counter HUD Module:
 * Tracks consecutive PvP attacks against opponent players, resets on personal damage
 * or timeout, featuring scale-punch animations, milestone colors, and sound cues.
 */
public final class ComboCounterModule extends HudModule {
    public enum DisplayFormat {
        COMBO_NUM("Combo: %d"),
        HITS_SUFFIX("%d Hits"),
        NUMBER_ONLY("%d");

        private final String template;
        DisplayFormat(String template) { this.template = template; }
        public String format(int count) { return String.format(template, count); }
    }

    private static int comboCount = 0;
    private static long lastHitTime = 0L;
    private static float punchProgress = 0.0f;

    private DisplayFormat displayFormat = DisplayFormat.COMBO_NUM;
    private float resetWindowSeconds = 2.0f; // 1.0 to 3.0s
    private boolean scalePunch = true;
    private boolean milestoneColors = true;
    private boolean soundFeedback = false;

    public ComboCounterModule() {
        super("Combo Counter", "Combat", false, 6, 160, "", "");
    }

    @Override
    public String getDescription() {
        return "Zählt aufeinanderfolgende Treffer gegen Gegner und visualisiert Combos im HUD.";
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/crosshair.png");
    }

    public static void onPlayerAttack() {
        Minecraft client = Minecraft.getInstance();
        ComboCounterModule module = ModuleManager.getInstance().getComboCounterModule();
        long now = System.currentTimeMillis();
        long windowMs = (long) (module.getResetWindowSeconds() * 1000.0f);

        if (now - lastHitTime <= windowMs) {
            comboCount++;
        } else {
            comboCount = 1;
        }
        lastHitTime = now;
        punchProgress = 1.0f;

        if (module.isSoundFeedback() && client.player != null) {
            try {
                float pitch = Math.min(2.0f, 1.0f + (comboCount * 0.05f));
                client.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 0.6f, pitch);
            } catch (Throwable ignored) {}
        }
    }

    public static void onPlayerHurt() {
        comboCount = 0;
    }

    public static int getComboCount() { return comboCount; }
    public static float getPunchProgress() { return punchProgress; }

    public DisplayFormat getDisplayFormat() { return displayFormat; }
    public void setDisplayFormat(DisplayFormat displayFormat) { this.displayFormat = displayFormat; ConfigManager.save(); }

    public float getResetWindowSeconds() { return resetWindowSeconds; }
    public void setResetWindowSeconds(float resetWindowSeconds) { this.resetWindowSeconds = Math.max(1.0f, Math.min(3.0f, resetWindowSeconds)); ConfigManager.save(); }

    public boolean isScalePunch() { return scalePunch; }
    public void setScalePunch(boolean scalePunch) { this.scalePunch = scalePunch; ConfigManager.save(); }

    public boolean isMilestoneColors() { return milestoneColors; }
    public void setMilestoneColors(boolean milestoneColors) { this.milestoneColors = milestoneColors; ConfigManager.save(); }

    public boolean isSoundFeedback() { return soundFeedback; }
    public void setSoundFeedback(boolean soundFeedback) { this.soundFeedback = soundFeedback; ConfigManager.save(); }

    @Override
    public int color() {
        if (milestoneColors) {
            Minecraft client = Minecraft.getInstance();
            boolean editor = client != null && (EzScreenBridge.current(client) instanceof HudEditorScreen || EzScreenBridge.current(client) instanceof HudSettingsScreen);
            int current;
            if (editor) {
                current = 7;
            } else {
                long now = System.currentTimeMillis();
                long windowMs = (long) (resetWindowSeconds * 1000.0f);
                current = (now - lastHitTime <= windowMs) ? comboCount : 0;
            }
            if (current >= 15) return 0xFFFF55FF;
            if (current >= 10) return 0xFFFFAA00;
            if (current >= 5)  return 0xFFFFFFFF;
            if (current >= 3)  return 0xFFFFFF55;
        }
        return super.color();
    }

    @Override
    protected String value(Minecraft client) {
        long now = System.currentTimeMillis();
        long windowMs = (long) (resetWindowSeconds * 1000.0f);
        int current = (now - lastHitTime <= windowMs) ? comboCount : 0;
        return displayFormat.format(current);
    }

    @Override
    public String displayText(Minecraft client) {
        return value(client);
    }

    @Override
    public String displayText(Minecraft client, boolean editor) {
        if (editor) {
            return displayFormat.format(7);
        }
        return displayText(client);
    }
}
