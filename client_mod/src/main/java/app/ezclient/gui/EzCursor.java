package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Applies the native pointing-hand cursor to interactive EzClient controls. */
public final class EzCursor {
    private static long pointerCursor;
    private static boolean pointerActive;

    private EzCursor() {}

    public static void setPointer(boolean pointer) {
        if (pointer == pointerActive) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) return;
        if (pointer && pointerCursor == 0L) pointerCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        GLFW.glfwSetCursor(minecraft.getWindow().handle(), pointer ? pointerCursor : 0L);
        pointerActive = pointer;
    }
}
