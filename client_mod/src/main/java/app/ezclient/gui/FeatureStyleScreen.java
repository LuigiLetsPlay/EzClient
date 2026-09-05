package app.ezclient.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.function.*;

/** All shared HUD controls, including values previously present only in the config. */
public final class FeatureStyleScreen extends Screen {
    private final Screen parent;
    private final HudModule module;
    private int page, x, y;
    public FeatureStyleScreen(Screen parent, HudModule module) { super(Component.literal("HUD Style")); this.parent = parent; this.module = module; }
    private void slider(String label, double value, double min, double max, DoubleConsumer setter) {
        addRenderableWidget(new EzSlider(x, y, 340, 20, (value - min) / (max - min), v -> setter.accept(min + v * (max - min)),
            v -> Component.literal(label + ": " + String.format(java.util.Locale.ROOT, "%.2f", min + v * (max - min))), true)); y += 24;
    }
    private void toggle(String label, boolean value, Consumer<Boolean> setter) {
        addRenderableWidget(new EzButton(x, y, 340, 20, Component.literal(label + ": " + value), value, b -> { setter.accept(!value); rebuildWidgets(); })); y += 24;
    }
    private void color(String label, int value, IntConsumer setter) {
        EditBox field = new EditBox(font, x, y, 340, 20, Component.literal(label)); field.setMaxLength(8);
        field.setValue(String.format("%08X", value));
        field.setResponder(v -> { if (v.matches("[0-9a-fA-F]{8}")) { setter.accept((int)Long.parseLong(v,16)); field.setTextColor(0xffeeeeee); } else field.setTextColor(0xffff5555); });
        field.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(label + " (AARRGGBB)")));
        addRenderableWidget(field); y += 24;
    }
    @Override protected void init() {
        x = Math.max(10, width / 2 - 170); y = 36;
        if (page == 0) {
            slider("X", module.getX(), 0, Math.max(1, width), v -> module.setX((int)v));
            slider("Y", module.getY(), 0, Math.max(1, height), v -> module.setY((int)v));
            slider("Scale", module.getScale(), .5, 2, module::setScale);
            toggle("Smooth TTF", module.isCustomFont(), module::setCustomFont);
            toggle("Text shadow", module.isTextShadow(), module::setTextShadow);
            slider("Corner radius", module.getCornerRadius(), 0, 10, v -> module.setCornerRadius((int)v));
        } else if (page == 1) {
            toggle("Background", module.hasBackground(), module::setBackground);
            toggle("Border", module.hasBorder(), module::setBorder);
            color("Text", module.getTextColor(), module::setTextColor);
            color("Background", module.getBackgroundColor(), module::setBackgroundColor);
            color("Border", module.getBorderColor(), module::setBorderColor);
            slider("Background opacity %", (module.getBackgroundColor() >>> 24) * 100 / 255.0, 0, 100,
                v -> module.setBackgroundColor((module.getBackgroundColor() & 0xffffff) | ((int)Math.round(v * 255 / 100) << 24)));
        } else {
            toggle("Rainbow text", module.isRainbow(), module::setRainbow);
            toggle("Rainbow border", module.isRainbowBorder(), module::setRainbowBorder);
            slider("Rainbow speed", module.getRainbowSpeed(), .2, 5, v -> module.setRainbowSpeed((float)v));
            slider("Saturation", module.getRainbowSaturation(), 0, 1, v -> module.setRainbowSaturation((float)v));
            slider("Border width", module.getBorderWidth(), 1, 3, v -> module.setBorderWidth((int)v));
        }
        int bottom = Math.max(y + 4, height - 28);
        addRenderableWidget(new EzButton(x, bottom, 80, 20, Component.literal("<"), true, b -> { page = (page + 2) % 3; rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 85, bottom, 80, 20, Component.literal(">"), true, b -> { page = (page + 1) % 3; rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 170, bottom, 170, 20, Component.literal("Back"), true, b -> onClose()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0,0,width,height,0xee10151c); g.centeredText(font, "HUD Style " + (page + 1) + "/3", width/2,12,0xffffffff);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { EzScreenBridge.set(minecraft,parent); }
}
