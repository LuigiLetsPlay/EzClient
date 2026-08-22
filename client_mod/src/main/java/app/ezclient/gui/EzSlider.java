package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** EzClient slider rendered with flat rectangles and Minecraft's font. */
final class EzSlider extends AbstractSliderButton {
    private final DoubleConsumer setter;
    private final DoubleFunction<Component> label;

    EzSlider(int x, int y, int width, int height, double normalizedValue,
             DoubleConsumer setter, DoubleFunction<Component> label) {
        super(x, y, width, height, Component.empty(), normalizedValue);
        this.setter = setter;
        this.label = label;
        updateMessage();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getRight(), getBottom(), 0xFF1C2128);
        int trackY = getY() + getHeight() - 7;
        graphics.fill(getX() + 8, trackY, getRight() - 8, trackY + 2, 0xFF3A424C);
        int filled = (int) Math.round((getWidth() - 16) * value);
        graphics.fill(getX() + 8, trackY, getX() + 8 + filled, trackY + 2, 0xFF35D07F);
        graphics.fill(getX() + 6 + filled, trackY - 3, getX() + 10 + filled, trackY + 5, 0xFFE8FFF3);
        graphics.text(Minecraft.getInstance().font, getMessage(), getX() + 8, getY() + 6, 0xFFE7EBEF);
    }

    @Override
    protected void updateMessage() {
        if (label != null) setMessage(label.apply(value));
    }

    @Override
    protected void applyValue() {
        if (setter != null) setter.accept(value);
        updateMessage();
    }
}
