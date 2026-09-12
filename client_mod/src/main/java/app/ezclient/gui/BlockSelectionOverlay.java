package app.ezclient.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Interactive client-side block selection mode for Waypoints.
 * Allows right-clicking blocks in the world to add/remove them with dedicated HUD hotbar tools.
 */
public final class BlockSelectionOverlay {
    private static boolean active = false;
    private static int currentTool = 0; // 0 = [+] Add, 1 = [-] Remove
    private static final List<WaypointsModule.BlockPosition> selectedBlocks = new ArrayList<>();
    private static int waypointColor = 0xFF22C96E;
    private static Screen returnScreen;
    private static Consumer<List<WaypointsModule.BlockPosition>> onSaveCallback;

    private static boolean clickDown = false;
    private static boolean shiftDown = false;
    private static boolean escDown = false;
    private static boolean key1Down = false;
    private static boolean key2Down = false;

    private BlockSelectionOverlay() {}

    public static boolean isActive() {
        return active;
    }

    public static List<WaypointsModule.BlockPosition> getSelectedBlocks() {
        return selectedBlocks;
    }

    public static void start(Minecraft mc, List<WaypointsModule.BlockPosition> initialBlocks, int color, Screen parent, Consumer<List<WaypointsModule.BlockPosition>> onSave) {
        active = true;
        currentTool = 0;
        selectedBlocks.clear();
        if (initialBlocks != null) selectedBlocks.addAll(initialBlocks);
        waypointColor = color;
        returnScreen = parent;
        onSaveCallback = onSave;
        clickDown = true;
        shiftDown = false;
        escDown = false;
        key1Down = false;
        key2Down = false;
        if (mc != null) {
            EzScreenBridge.set(mc, null);
        }
    }

    public static void finishAndSave(Minecraft mc) {
        if (!active) return;
        active = false;
        clickDown = false;
        shiftDown = false;
        escDown = false;
        key1Down = false;
        key2Down = false;
        List<WaypointsModule.BlockPosition> result = new ArrayList<>(selectedBlocks);
        if (onSaveCallback != null) {
            onSaveCallback.accept(result);
        }
        if (mc != null && returnScreen != null) {
            EzScreenBridge.set(mc, returnScreen);
        }
    }

    public static void cancel(Minecraft mc) {
        if (!active) return;
        active = false;
        clickDown = false;
        shiftDown = false;
        escDown = false;
        key1Down = false;
        key2Down = false;
        if (mc != null && returnScreen != null) {
            EzScreenBridge.set(mc, returnScreen);
        }
    }

    public static void onClientTick(Minecraft mc) {
        if (!active || mc == null || mc.getWindow() == null || mc.player == null || EzScreenBridge.current(mc) != null) return;
        long window = mc.getWindow().handle();

        // 1. Enter to save & exit
        boolean enter = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_ENTER) == GLFW.GLFW_PRESS;
        if (enter && !shiftDown) {
            shiftDown = true;
            try {
                mc.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.0f);
            } catch (Throwable ignored) {}
            finishAndSave(mc);
            return;
        }
        shiftDown = enter;

        // 2. Escape to cancel
        boolean esc = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
        if (esc && !escDown) {
            escDown = true;
            cancel(mc);
            return;
        }
        escDown = esc;

        // 3. Hotbar keys (1 & 2, both number row and numpad)
        boolean k1 = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_1) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_1) == GLFW.GLFW_PRESS;
        if (k1 && !key1Down) {
            currentTool = 0;
            try {
                mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f);
            } catch (Throwable ignored) {}
        }
        key1Down = k1;

        boolean k2 = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_2) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_2) == GLFW.GLFW_PRESS;
        if (k2 && !key2Down) {
            currentTool = 1;
            try {
                mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 0.9f);
            } catch (Throwable ignored) {}
        }
        key2Down = k2;

        // 4. Consume attack / use key clicks and check mouse buttons
        boolean attackConsumed = false;
        if (mc.options != null && mc.options.keyAttack != null) {
            while (mc.options.keyAttack.consumeClick()) attackConsumed = true;
        }
        boolean useConsumed = false;
        if (mc.options != null && mc.options.keyUse != null) {
            while (mc.options.keyUse.consumeClick()) useConsumed = true;
        }
        if (mc.options != null && mc.options.keyPickItem != null) {
            while (mc.options.keyPickItem.consumeClick()) {}
        }

        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean clicked = attackConsumed || useConsumed || ((leftDown || rightDown) && !clickDown);
        clickDown = leftDown || rightDown;

        if (clicked) {
            if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = bhr.getBlockPos();
                var bp = new WaypointsModule.BlockPosition(pos.getX(), pos.getY(), pos.getZ());
                if (currentTool == 0) { // [+] Mode: Add / Toggle
                    if (!selectedBlocks.contains(bp)) {
                        if (selectedBlocks.size() < 512) {
                            selectedBlocks.add(bp);
                            try {
                                mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 0.7f, 1.4f);
                            } catch (Throwable ignored) {}
                            mc.player.sendOverlayMessage(Component.literal("§a[+] Block markiert: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + selectedBlocks.size() + ")"));
                        } else {
                            mc.player.sendOverlayMessage(Component.literal("§cMaximum von 512 Blöcken erreicht!"));
                        }
                    } else {
                        selectedBlocks.remove(bp);
                        try {
                            mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f, 0.9f);
                        } catch (Throwable ignored) {}
                        mc.player.sendOverlayMessage(Component.literal("§c[-] Block abgewählt: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + selectedBlocks.size() + ")"));
                    }
                } else { // [-] Mode: Remove
                    if (selectedBlocks.remove(bp)) {
                        try {
                            mc.player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f, 0.9f);
                        } catch (Throwable ignored) {}
                        mc.player.sendOverlayMessage(Component.literal("§c[-] Block entfernt: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + selectedBlocks.size() + ")"));
                    }
                }
            }
        }
    }

    public static void onMouseScrolled(double vertical) {
        if (!active) return;
        if (vertical != 0) {
            currentTool = (currentTool == 0) ? 1 : 0;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                try {
                    mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, currentTool == 0 ? 1.2f : 0.9f);
                } catch (Throwable ignored) {}
            }
        }
    }

    public static void renderWorldGizmos(Minecraft mc) {
        if (!active || mc == null || mc.player == null) return;
        int rgb = waypointColor & 0xffffff;
        int outline = 0xFF000000 | rgb;
        int fill = 0x35000000 | rgb;

        // 1. Highlight all currently marked blocks (connected external faces)
        WorldVisuals.renderConnectedBlocks(selectedBlocks, outline, 2.0f, fill, true);

        // 2. Targeted block outline indicator
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            boolean isAlreadySelected = selectedBlocks.contains(new WaypointsModule.BlockPosition(pos.getX(), pos.getY(), pos.getZ()));
            int targetColor = (currentTool == 0 && !isAlreadySelected) ? 0xFF22C96E : 0xFFFF453A;
            var g = Gizmos.cuboid(new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).inflate(0.006),
                    new GizmoStyle(targetColor, 2.5f, (targetColor & 0xffffff) | 0x25000000));
            g.setAlwaysOnTop();
        }
    }

    public static void renderHud(GuiGraphicsExtractor g, Minecraft mc) {
        if (!active || mc == null || mc.getWindow() == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // Custom Hotbar at bottom center (above vanilla hotbar or centered)
        int barW = 270;
        int barH = 50;
        int barX = (sw - barW) / 2;
        int barY = sh - 75;

        // Container Panel
        EzUi.panel(g, barX, barY, barW, barH);

        // Tool Slots (Slot 0: [+], Slot 1: [-])
        int slotW = 124;
        int slotH = 22;
        int s1X = barX + 8;
        int s2X = barX + barW - slotW - 8;
        int slotsY = barY + 6;

        // Slot 1: [+] Block markieren
        boolean s1Active = currentTool == 0;
        int s1Border = s1Active ? 0xFF22C96E : 0x40FFFFFF;
        int s1Bg = s1Active ? 0x4022C96E : 0x1A000000;
        EzUi.roundedRect(g, s1X, slotsY, slotW, slotH, 1, s1Bg);
        g.fill(s1X, slotsY, s1X + slotW, slotsY + 1, s1Border);
        g.fill(s1X, slotsY + slotH - 1, s1X + slotW, slotsY + slotH, s1Border);
        g.fill(s1X, slotsY, s1X + 1, slotsY + slotH, s1Border);
        g.fill(s1X + slotW - 1, slotsY, s1X + slotW, slotsY + slotH, s1Border);
        g.centeredText(mc.font, Component.literal("1  [+] Markieren"), s1X + slotW / 2, slotsY + 6, s1Active ? 0xFF5AEEA0 : 0xAAFFFFFF);

        // Slot 2: [-] Block entfernen
        boolean s2Active = currentTool == 1;
        int s2Border = s2Active ? 0xFFFF453A : 0x40FFFFFF;
        int s2Bg = s2Active ? 0x40FF453A : 0x1A000000;
        EzUi.roundedRect(g, s2X, slotsY, slotW, slotH, 1, s2Bg);
        g.fill(s2X, slotsY, s2X + slotW, slotsY + 1, s2Border);
        g.fill(s2X, slotsY + slotH - 1, s2X + slotW, slotsY + slotH, s2Border);
        g.fill(s2X, slotsY, s2X + 1, slotsY + slotH, s2Border);
        g.fill(s2X + slotW - 1, slotsY, s2X + slotW, slotsY + slotH, s2Border);
        g.centeredText(mc.font, Component.literal("2  [-] Entfernen"), s2X + slotW / 2, slotsY + 6, s2Active ? 0xFFFFA49E : 0xAAFFFFFF);

        // Instructions line
        String help = "[Klick] Markieren • [1/2 / Scroll] Modus • [Enter] Fertig & Speichern • [Esc] Abbrechen";
        g.centeredText(mc.font, Component.literal(help), barX + barW / 2, barY + 34, 0xFF94A3B8);

        // Badge: Count of selected blocks (Top right of bar)
        String countText = selectedBlocks.size() + " / 512 Blöcke";
        int badgeW = mc.font.width(countText) + 10;
        EzUi.badge(g, barX + barW - badgeW - 6, barY - 14, badgeW, 14, 0xDD121620, 0xFF22C96E, countText, 0xFF5AEEA0);
    }
}
