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
    private int panelX, panelY;
    private static final int PANEL_WIDTH = 372;
    private static final int PANEL_HEIGHT = 218;
    public FeatureStyleScreen(Screen parent, HudModule module) { super(app.ezclient.util.EzI18n.comp("ezclient.style.title")); this.parent = parent; this.module = module; }
    private static String tr(String key) { return app.ezclient.util.EzI18n.get(key); }
    private void slider(String label, double value, double min, double max, DoubleConsumer setter) {
        addRenderableWidget(new EzSlider(x, y, 340, 20, (value - min) / (max - min), v -> setter.accept(min + v * (max - min)),
            v -> Component.literal(label + ": " + String.format(java.util.Locale.ROOT, "%.2f", min + v * (max - min))), true)); y += 24;
    }
    private void toggle(String label, boolean value, Consumer<Boolean> setter) {
        addRenderableWidget(new EzButton(x, y, 340, 20,
                Component.literal(label + ": " + app.ezclient.util.EzI18n.onOrOff(value)), value,
                b -> { setter.accept(!value); rebuildWidgets(); })); y += 24;
    }
    private void color(String label, int value, IntConsumer setter) {
        EditBox field = new EditBox(font, x, y, 340, 20, Component.literal(label)); field.setMaxLength(8);
        field.setValue(String.format("%08X", value));
        field.setResponder(v -> { if (v.matches("[0-9a-fA-F]{8}")) { setter.accept((int)Long.parseLong(v,16)); field.setTextColor(0xffeeeeee); } else field.setTextColor(0xffff5555); });
        field.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(label + " (AARRGGBB)")));
        addRenderableWidget(field); y += 24;
    }
    @Override protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        x = panelX + 16;
        y = panelY + 36;
        if (page == 0) {
            slider("X", module.getX(), 0, Math.max(1, width), v -> module.setX((int)v));
            slider("Y", module.getY(), 0, Math.max(1, height), v -> module.setY((int)v));
            slider(tr("ezclient.style.scale"), module.getScale(), .5, 2, module::setScale);
            toggle(tr("ezclient.style.smooth_font"), module.isCustomFont(), module::setCustomFont);
            toggle(tr("ezclient.style.text_shadow"), module.isTextShadow(), module::setTextShadow);
            slider(tr("ezclient.style.corner_radius"), module.getCornerRadius(), 0, 1, v -> module.setCornerRadius((int)v));
        } else if (page == 1) {
            toggle(tr("ezclient.style.box"), module.hasBackground(), module::setBackground);
            toggle(tr("ezclient.style.border"), module.hasBorder(), module::setBorder);
            addRenderableWidget(new EzButton(x, y, 340, 20,
                    Component.literal("‹ " + app.ezclient.util.EzI18n.get("ezclient.hud_settings.border_style", module.getBorderStyle().getLabel()) + " ›"), true,
                    b -> {
                        HudModule.BorderStyle[] styles = HudModule.BorderStyle.values();
                        int next = (module.getBorderStyle().ordinal() + 1) % styles.length;
                        module.setBorderStyle(styles[next]);
                        rebuildWidgets();
                    }
            ).withRightClick(b -> {
                HudModule.BorderStyle[] styles = HudModule.BorderStyle.values();
                int prev = (module.getBorderStyle().ordinal() - 1 + styles.length) % styles.length;
                module.setBorderStyle(styles[prev]);
                rebuildWidgets();
            }));
            y += 24;
            color(tr("ezclient.style.text"), module.getTextColor(), module::setTextColor);
            color(tr("ezclient.style.background"), module.getBackgroundColor(), module::setBackgroundColor);
            color(tr("ezclient.style.border"), module.getBorderColor(), module::setBorderColor);
            slider(tr("ezclient.style.background_opacity"), (module.getBackgroundColor() >>> 24) * 100 / 255.0, 0, 100,
                v -> module.setBackgroundColor((module.getBackgroundColor() & 0xffffff) | ((int)Math.round(v * 255 / 100) << 24)));
        } else {
            toggle(tr("ezclient.style.rainbow_text"), module.isRainbow(), module::setRainbow);
            toggle(tr("ezclient.style.rainbow_border"), module.isRainbowBorder(), module::setRainbowBorder);
            slider(tr("ezclient.style.rainbow_speed"), module.getRainbowSpeed(), .2, 5, v -> module.setRainbowSpeed((float)v));
            slider(tr("ezclient.style.saturation"), module.getRainbowSaturation(), 0, 1, v -> module.setRainbowSaturation((float)v));
            slider(tr("ezclient.style.border_width"), module.getBorderWidth(), 2, 4, v -> module.setBorderWidth((int)v));
        }
        int bottom = panelY + PANEL_HEIGHT - 28;
        addRenderableWidget(new EzButton(x, bottom, 80, 20, Component.literal("<"), true, b -> { page = (page + 2) % 3; rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 85, bottom, 80, 20, Component.literal(">"), true, b -> { page = (page + 1) % 3; rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 170, bottom, 170, 20, app.ezclient.util.EzI18n.comp("ezclient.style.back"), true, b -> onClose()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        g.centeredText(font, app.ezclient.util.EzI18n.get("ezclient.style.page", page + 1, 3), panelX + PANEL_WIDTH / 2, panelY + 12, EzUi.TEXT_WHITE);
        g.fill(panelX + 16, panelY + 28, panelX + PANEL_WIDTH - 16, panelY + 29, EzUi.BORDER_SUBTLE);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose() { EzScreenBridge.set(minecraft,parent); }
}
