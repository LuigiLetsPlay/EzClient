package app.ezclient.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class FeatureSettingsScreen extends Screen {
    private final Screen parent;
    private final FeatureModule module;
    private int page;
    public FeatureSettingsScreen(Screen parent, FeatureModule module) {
        super(Component.literal(module.getName())); this.parent = parent; this.module = module;
    }
    @Override protected void init() {
        int x = Math.max(10, width / 2 - 180), y = 38;
        int count = Math.max(1, (height - 104) / 30);
        int pages = Math.max(1, (module.options().size() + count - 1) / count);
        page = Math.min(page, pages - 1);
        for (int i = page * count; i < Math.min(module.options().size(), (page + 1) * count); i++) {
            var option = module.options().get(i);
            Object value = module.setting(option.key());
            if (value instanceof Boolean) {
                addRenderableWidget(new EzButton(x, y, 360, 20, Component.literal(option.label() + ": " + value), (boolean)value,
                    b -> { module.set(option, !module.flag(option.key())); rebuildWidgets(); }));
            } else if (option.choices().length > 0) {
                addRenderableWidget(new EzButton(x, y, 360, 20, Component.literal(option.label() + ": " + value), true,
                    b -> { int idx = java.util.Arrays.asList(option.choices()).indexOf(module.text(option.key()));
                        module.set(option, option.choices()[(idx + 1) % option.choices().length]); rebuildWidgets(); }));
            } else if (value instanceof Number) {
                addRenderableWidget(new EzSlider(x, y, 360, 20, (module.number(option.key()) - option.min()) / (option.max() - option.min()),
                    v -> module.set(option, option.min() + v * (option.max() - option.min())),
                    v -> Component.literal(option.label() + ": " + String.format(java.util.Locale.ROOT, "%.2f", option.min() + v * (option.max() - option.min()))), true));
            } else {
                EditBox edit = new EditBox(font, x + 174, y, 186, 20, Component.literal(option.label()));
                edit.setMaxLength(1024); edit.setValue(value.toString());
                edit.setResponder(v -> { edit.setTextColor(module.set(option, v) ? 0xffeeeeee : 0xffff5555); });
                addRenderableWidget(edit);
            }
            y += 30;
        }
        addRenderableWidget(new EzButton(x, height - 54, 80, 20, Component.literal("<"), true, b -> { page = Math.max(0, page - 1); rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 90, height - 54, 80, 20, Component.literal(">"), true, b -> { page = Math.min(pages - 1, page + 1); rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 180, height - 54, 180, 20, Component.literal("HUD / Font / Chroma"), true,
            b -> EzScreenBridge.set(minecraft, new FeatureStyleScreen(this, module))));
        if (module instanceof WaypointsModule waypoints) addRenderableWidget(new EzButton(x, height - 28, 170, 20, Component.literal("Waypoint Manager"), true,
            b -> EzScreenBridge.set(minecraft, new WaypointScreen(this, waypoints))));
        addRenderableWidget(new EzButton(x + 180, height - 28, 180, 20, Component.literal("Back"), true, b -> onClose()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0xee10151c); g.centeredText(font, title, width / 2, 14, 0xffffffff);
        int count = Math.max(1, (height - 104) / 30), y = 44;
        for (int i = page * count; i < Math.min(module.options().size(), (page + 1) * count); i++) {
            var option = module.options().get(i);
            if (option.initial() instanceof String && option.choices().length == 0)
                g.text(font, option.label(), Math.max(10, width / 2 - 180), y, 0xffeeeeee);
            y += 30;
        }
        super.extractRenderState(g, mx, my, delta);
    }
    @Override public void onClose() { EzScreenBridge.set(minecraft, parent); }
}
