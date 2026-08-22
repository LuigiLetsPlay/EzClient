package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Compact square EzClient control with no vanilla widget texture. */
final class EzButton extends AbstractButton {
    private final Consumer<EzButton> action;
    private final boolean accent;

    EzButton(int x, int y, int width, int height, Component label, boolean accent, Consumer<EzButton> action) {
        super(x, y, width, height, label);
        this.action = action;
        this.accent = accent;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (active) action.accept(this);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int background;
        if (!active) background = 0xFF171A1F;
        else if (isHoveredOrFocused()) background = accent ? 0xFF2FBF71 : 0xFF303741;
        else background = accent ? 0xFF239A59 : 0xFF242A32;

        graphics.fill(getX(), getY(), getRight(), getBottom(), background);
        graphics.outline(getX(), getY(), getWidth(), getHeight(), active ? 0xFF3B4652 : 0xFF242930);
        graphics.centeredText(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                active ? 0xFFF2F5F7 : 0xFF69717A
        );
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
