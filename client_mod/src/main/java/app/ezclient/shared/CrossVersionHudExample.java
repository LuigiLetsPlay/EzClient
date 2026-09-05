package app.ezclient.shared;

// This class documents the exact Stonecutter pattern used for renderer ports.
//? if >=26.2 {
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
*///?}

public final class CrossVersionHudExample {
    private CrossVersionHudExample() {}

    //? if >=26.2 {
    public static void draw(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        graphics.text(client.font, "EzClient", 4, 4, 0x55FF55);
    }
    //?} else {
    /*public static void draw() {
        MinecraftClient client = MinecraftClient.getInstance();
        GL11.glPushMatrix();
        client.textRenderer.drawWithShadow("EzClient", 4.0F, 4.0F, 0x55FF55);
        GL11.glPopMatrix();
    }
    *///?}
}
