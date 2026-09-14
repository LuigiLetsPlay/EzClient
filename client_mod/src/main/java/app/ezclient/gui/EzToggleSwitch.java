package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Compact, sleek toggle switch widget for boolean settings in EzClient. */
public final class EzToggleSwitch extends AbstractButton {
    private boolean state;
    private final Consumer<Boolean> onToggle;
    private Component label = null;

    public EzToggleSwitch(int x, int y, int width, int height, boolean initialState, Consumer<Boolean> onToggle) {
        super(x, y, width, height, Component.empty());
        this.state = initialState;
        this.onToggle = onToggle;
    }

    public EzToggleSwitch(int x, int y, boolean initialState, Consumer<Boolean> onToggle) {
        this(x, y, 24, 13, initialState, onToggle);
    }

    public EzToggleSwitch(int x, int y, int width, int height, Component label, boolean initialState, Consumer<Boolean> onToggle) {
        super(x, y, width, height, label == null ? Component.empty() : label);
        this.label = label == null ? null : Component.literal(app.ezclient.util.EzI18n.text(label.getString())).withStyle(label.getStyle());
        if (this.label != null) {
            setMessage(this.label);
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(this.label));
        }
        this.state = initialState;
        this.onToggle = onToggle;
    }

    public boolean getState() { return state; }
    public void setState(boolean state) { this.state = state; }

    @Override
    public void onPress(InputWithModifiers input) {
        if (!active) return;
        state = !state;
        if (onToggle != null) onToggle.accept(state);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = isHoveredOrFocused();
        if (label != null && !label.getString().isEmpty()) {
            var font = Minecraft.getInstance().font;
            int textColor = hovered ? EzUi.TEXT_WHITE : EzUi.TEXT_LIGHT;
            int textY = getY() + (getHeight() - 8) / 2;
            graphics.text(font, EzUi.fitText(label, Math.max(1, getWidth() - 30)), getX(), textY, textColor);

            int swW = 24;
            int swH = 13;
            int swX = getX() + getWidth() - swW;
            int swY = getY() + (getHeight() - swH) / 2;
            EzUi.toggleSwitch(graphics, swX, swY, swW, swH, state, hovered);
        } else {
            EzUi.toggleSwitch(graphics, getX(), getY(), getWidth(), getHeight(), state, hovered);
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
