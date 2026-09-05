package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Modern, clean button with crisp typography, stepper arrows, and right-click support. */
public final class EzButton extends AbstractButton {
    private final Consumer<EzButton> action;
    private Consumer<EzButton> rightClickAction = null;
    private final boolean accent;
    private final net.minecraft.resources.Identifier icon;

    public EzButton(int x, int y, int width, int height, Component label, boolean accent, Consumer<EzButton> action) {
        this(x, y, width, height, label, null, accent, action);
    }

    public EzButton(int x, int y, int width, int height, Component label, net.minecraft.resources.Identifier icon, boolean accent, Consumer<EzButton> action) {
        super(x, y, width, height, label);
        this.action = action;
        this.accent = accent;
        this.icon = icon;
    }

    public EzButton withRightClick(Consumer<EzButton> rightClickAction) {
        this.rightClickAction = rightClickAction;
        return this;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (active && visible && isHovered()) {
            if (event.button() == 1 && rightClickAction != null) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                rightClickAction.accept(this);
                return true;
            }
            if (event.button() == 0 && rightClickAction != null) {
                // If clicked on left 25% of button (where left arrow ‹ is), step backwards
                if (event.x() < getX() + (getWidth() * 0.28)) {
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

        int bg;
        int border;
        int textColor;

        if (!active) {
            bg = 0xFF12151D;
            border = 0xFF1B202A;
            textColor = 0xFF4A5568;
        } else if (accent) {
            bg = hovered ? EzUi.ACCENT_EMERALD_BG_HOVER : EzUi.ACCENT_EMERALD_BG;
            border = hovered ? EzUi.ACCENT_EMERALD_HOVER : EzUi.ACCENT_EMERALD;
            textColor = EzUi.TEXT_WHITE;
        } else {
            bg = hovered ? 0xFF1E2636 : 0xFF161C27;
            border = hovered ? EzUi.BORDER_HOVER : EzUi.BORDER_SUBTLE;
            textColor = hovered ? EzUi.TEXT_WHITE : EzUi.TEXT_LIGHT;
        }

        int radius = Math.min(6, getHeight() / 2);
        EzUi.roundedRect(graphics, getX(), getY(), getWidth(), getHeight(), radius, border);
        EzUi.roundedRect(graphics, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, Math.max(1, radius - 1), bg);

        // Subtle top highlight for depth on accent buttons
        if (active && accent) {
            EzUi.roundedRect(graphics, getX() + 2, getY() + 1, getWidth() - 4, 1, 0, 0x18FFFFFF);
        }

        if (icon != null) {
            int iconSize = Math.min(getWidth() - 4, getHeight() - 4);
            int ix = getX() + (getWidth() - iconSize) / 2;
            int iy = getY() + (getHeight() - iconSize) / 2;
            ModuleIconRenderer.drawTexture(graphics, icon, ix, iy, iconSize);
        } else {
            graphics.centeredText(
                    Minecraft.getInstance().font,
                    getMessage(),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    textColor
            );
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
