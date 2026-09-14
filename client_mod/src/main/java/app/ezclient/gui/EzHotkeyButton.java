package app.ezclient.gui;

import app.ezclient.util.EzI18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/** Shared, always visible key binding control for module settings. */
public final class EzHotkeyButton extends AbstractButton {
    private final boolean listening;
    private final Runnable action;
    private final Component value;

    public EzHotkeyButton(int x, int y, int width, int key, boolean listening, Runnable action) {
        super(x, y, width, 30, Component.literal("Hotkey"));
        this.listening = listening;
        this.action = action;
        String binding = listening ? EzI18n.text("Taste drücken …")
                : key > 0 || key <= -100 ? EzI18n.text(EzKeyBindings.getKeyOrMouseName(key)) : EzI18n.text("Nicht belegt");
        value = Component.literal(binding);
        setMessage(Component.literal("Hotkey: " + binding));
        setTooltip(Tooltip.create(Component.literal(getMessage().getString() + "\n" + EzI18n.text(
                "Klicken, dann eine Taste oder Maustaste drücken. Esc, Entf oder Rücktaste entfernt die Belegung. Erneut klicken bricht ab."))));
    }

    @Override public void onPress(InputWithModifiers input) { if (active) action.run(); }

    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int border = listening || isHoveredOrFocused() ? EzUi.ACCENT_EMERALD : 0xFF526879;
        EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 3, border);
        EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 2,
                listening ? 0xFF132A20 : 0xFF18222D);
        var font = Minecraft.getInstance().font;
        g.centeredText(font, Component.literal("Hotkey"), getX() + getWidth() / 2, getY() + 4, EzUi.TEXT_WHITE);
        g.centeredText(font, EzUi.fitText(value, getWidth() - 8), getX() + getWidth() / 2,
                getY() + 17, listening ? EzUi.ACCENT_EMERALD : EzUi.TEXT_LIGHT);
    }

    @Override public void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
}
