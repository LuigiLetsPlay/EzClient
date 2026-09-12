package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** Clean EzClient slider with sleek emerald track. */
final class EzSlider extends AbstractSliderButton {
    private final DoubleConsumer setter;
    private final DoubleFunction<Component> label;
    private final boolean drawTextOnSlider;
    private final Runnable commit;

    EzSlider(int x, int y, int width, int height, double normalizedValue,
             DoubleConsumer setter, DoubleFunction<Component> label) {
        this(x, y, width, height, normalizedValue, setter, label, false);
    }

    EzSlider(int x, int y, int width, int height, double normalizedValue,
             DoubleConsumer setter, DoubleFunction<Component> label, boolean drawTextOnSlider) {
        this(x, y, width, height, normalizedValue, setter, label, drawTextOnSlider, null);
    }

    EzSlider(int x, int y, int width, int height, double normalizedValue,
             DoubleConsumer setter, DoubleFunction<Component> label, boolean drawTextOnSlider, Runnable commit) {
        super(x, y, width, height, Component.empty(), normalizedValue);
        this.setter = setter;
        this.label = label;
        this.drawTextOnSlider = drawTextOnSlider;
        this.commit = commit;
        updateMessage();
    }

    public void setSliderValue(double val) {
        this.value = Math.max(0.0, Math.min(1.0, val));
        updateMessage();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dark background & border
        int radius = Math.min(2, getHeight() / 2);
        EzUi.roundedRect(graphics, getX(), getY(), getWidth(), getHeight(), radius, EzUi.BORDER_SUBTLE);
        EzUi.roundedRect(graphics, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, Math.max(1, radius - 1), 0xFF101318);

        // Track
        int trackY = getY() + (getHeight() - 2) / 2;
        int trackX = getX() + 6;
        int trackW = getWidth() - 12;

        EzUi.roundedRect(graphics, trackX, trackY, trackW, 2, 1, 0xFF30343A);

        // Filled progress
        int filled = (int) Math.round(trackW * value);
        if (filled > 0) {
            EzUi.roundedRect(graphics, trackX, trackY, filled, 2, 1, EzUi.ACCENT_EMERALD);
        }

        // Handle thumb
        int thumbX = trackX + filled - 3;
        int thumbY = getY() + (getHeight() - 10) / 2;
        boolean hovered = isHovered();
        EzUi.roundedRect(graphics, thumbX, thumbY, 6, 10, 2, hovered ? 0xFFFFFFFF : 0xFFE2E8F0);
        EzUi.roundedRect(graphics, thumbX + 1, thumbY + 1, 4, 8, 1, EzUi.ACCENT_EMERALD);

        if (drawTextOnSlider && getMessage() != null) {
            graphics.text(Minecraft.getInstance().font, getMessage(), getX() + 8, getY() + (getHeight() - 8) / 2, 0xFFE7EBEF);
        }
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

    @Override
    public void onRelease(MouseButtonEvent event) {
        super.onRelease(event);
        if (commit != null) commit.run();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean handled = super.keyPressed(event);
        if (handled && commit != null) commit.run();
        return handled;
    }
}
