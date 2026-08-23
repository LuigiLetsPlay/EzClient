package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Modern pill-shaped EzClient control with no vanilla widget texture. */
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
        int background = !active ? 0xFF161A22
                : accent ? (isHoveredOrFocused() ? 0xFFAF91FF : 0xFF8B6CF6)
                : (isHoveredOrFocused() ? 0xFF323A4A : 0xFF242B37);
        EzUi.roundedRect(graphics, getX(), getY(), getWidth(), getHeight(), Math.min(8, getHeight() / 2), background);
        graphics.centeredText(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                active ? (accent ? 0xFFFFFFFF : 0xFFE9EDF5) : 0xFF69717A
        );
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
