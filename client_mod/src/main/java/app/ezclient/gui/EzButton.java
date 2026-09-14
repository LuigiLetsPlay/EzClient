package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Modern, clean button with crisp typography, stepper arrows, toggle switches, and right-click support. */
public final class EzButton extends AbstractButton {
    private final Consumer<EzButton> action;
    private Consumer<EzButton> rightClickAction = null;
    private final boolean accent;
    private final net.minecraft.resources.Identifier icon;
    private boolean asToggle = false;
    private boolean toggleState = false;

    public EzButton(int x, int y, int width, int height, Component label, boolean accent, Consumer<EzButton> action) {
        this(x, y, width, height, label, null, accent, action);
    }

    public EzButton(int x, int y, int width, int height, Component label, net.minecraft.resources.Identifier icon, boolean accent, Consumer<EzButton> action) {
        super(x, y, width, height, label);
        this.action = action;
        this.accent = accent;
        this.icon = icon;
        setTooltip(net.minecraft.client.gui.components.Tooltip.create(label));
    }

    public void setAsToggle(boolean state) {
        this.asToggle = true;
        this.toggleState = state;
    }

    public boolean isAsToggle() { return asToggle; }
    public boolean getToggleState() { return toggleState; }

    public EzButton withRightClick(Consumer<EzButton> rightClickAction) {
        this.rightClickAction = rightClickAction;
        return this;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (active && visible && event.x() >= getX() && event.x() < getX() + getWidth()
                && event.y() >= getY() && event.y() < getY() + getHeight()) {
            if (event.button() == 1 && rightClickAction != null) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                rightClickAction.accept(this);
                return true;
            }
            if (event.button() == 0 && rightClickAction != null) {
                // Split exactly in the middle: left half steps left/back, right half steps right/forward
                if (event.x() < getX() + (getWidth() / 2.0)) {
                    playDownSound(Minecraft.getInstance().getSoundManager());
                    rightClickAction.accept(this);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (active) action.accept(this);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = isHoveredOrFocused();

        if (asToggle) {
            EzUi.toggleSwitch(graphics, getX(), getY(), getWidth(), getHeight(), toggleState, hovered);
            return;
        }

        int bg;
        int border;
        int textColor;

        if (!active) {
            bg = 0xFF12151D;
            border = 0xFF1B202A;
            textColor = 0xFF4A5568;
        } else if (accent) {
            bg = hovered ? 0xFF1B3224 : 0xFF132218;
            border = hovered ? 0xFF2FD17A : 0xFF22C96E;
            textColor = EzUi.TEXT_WHITE;
        } else {
            bg = hovered ? 0xFF202632 : 0xFF141920;
            border = hovered ? 0xFF3D4856 : 0xFF28313D;
            textColor = hovered ? EzUi.TEXT_WHITE : EzUi.TEXT_LIGHT;
        }

        int radius = Math.min(3, getHeight() / 2);
        EzUi.roundedRect(graphics, getX(), getY(), getWidth(), getHeight(), radius, border);
        EzUi.roundedRect(graphics, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, Math.max(1, radius - 1), bg);

        if (icon != null) {
            int iconSize = Math.min(getWidth() - 4, getHeight() - 4);
            int ix = getX() + (getWidth() - iconSize) / 2;
            int iy = getY() + (getHeight() - iconSize) / 2;
            ModuleIconRenderer.drawTexture(graphics, icon, ix, iy, iconSize);
        } else {
            var font = Minecraft.getInstance().font;
            Component displayMessage = compactToggleLabel(getMessage());
            int available = Math.max(1, getWidth() - 6);
            if (font.width(displayMessage) > available / 0.85f) {
                displayMessage = EzUi.fitText(displayMessage, (int) (available / 0.85f));
            }
            float textScale = Math.min(1.0f, available / (float)Math.max(1, font.width(displayMessage)));
            graphics.pose().pushMatrix();
            graphics.pose().translate(getX() + getWidth() / 2.0f, getY() + getHeight() / 2.0f);
            graphics.pose().scale(textScale, textScale);
            graphics.centeredText(font, displayMessage, 0, -4, textColor);
            graphics.pose().popMatrix();
        }
    }

    private static Component compactToggleLabel(Component label) {
        String value = label.getString().replaceFirst("(?i)\\s*:\\s*(on|off|an|aus|ein|true|false)$", "");
        return value.equals(label.getString()) ? label : Component.literal(value);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
