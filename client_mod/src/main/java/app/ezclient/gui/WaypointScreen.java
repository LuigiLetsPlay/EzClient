package app.ezclient.gui;

import java.util.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class WaypointScreen extends Screen {
    private final Screen parent;
    private final WaypointsModule module;
    private String selected;
    private int page;
    private String error = "";
    public WaypointScreen(Screen parent, WaypointsModule module) { super(Component.literal("Waypoint Manager")); this.parent = parent; this.module = module; }
    @Override protected void init() {
        int x = Math.max(10, width / 2 - 190);
        var points = module.points();
        WaypointsModule.Waypoint point = points.stream().filter(p -> p.id().equals(selected)).findFirst().orElse(null);
        String[] labels = {"Name", "X", "Y", "Z", "Color AARRGGBB", "Icon"};
        var player = minecraft.player;
        String[] values = point == null ? new String[]{"Home", "" + (player == null ? 0 : Math.floor(player.getX())), "" + (player == null ? 64 : Math.floor(player.getY())), "" + (player == null ? 0 : Math.floor(player.getZ())), "FF22C96E", "*"}
            : new String[]{point.name(), "" + point.x(), "" + point.y(), "" + point.z(), String.format("%08X", point.color()), point.icon()};
        EditBox[] fields = new EditBox[6];
        for (int i = 0; i < 6; i++) {
            fields[i] = new EditBox(font, x + 90, 36 + i * 24, 130, 20, Component.literal(labels[i]));
            fields[i].setMaxLength(i == 0 ? 80 : i == 5 ? 8 : 32); fields[i].setValue(values[i]); addRenderableWidget(fields[i]);
        }
        int count = Math.max(1, (height - 100) / 24), pages = Math.max(1, (points.size() + count - 1) / count);
        page = Math.min(page, pages - 1);
        for (int i = page * count; i < Math.min(points.size(), (page + 1) * count); i++) {
            var p = points.get(i);
            addRenderableWidget(new EzButton(x + 230, 36 + (i % count) * 24, 150, 20, Component.literal(p.name()), p.id().equals(selected), b -> { selected = p.id(); rebuildWidgets(); }));
        }
        addRenderableWidget(new EzButton(x, height - 54, 70, 20, Component.literal("Save"), true, b -> {
            try {
                if (minecraft.level == null) { error = "Join a world first"; return; }
                double px = Double.parseDouble(fields[1].getValue()), py = Double.parseDouble(fields[2].getValue()), pz = Double.parseDouble(fields[3].getValue());
                if (!Double.isFinite(px) || !Double.isFinite(py) || !Double.isFinite(pz) || Math.abs(px) > 30000000 || Math.abs(pz) > 30000000 || Math.abs(py) > 2048) throw new IllegalArgumentException();
                String color = fields[4].getValue().replace("#", ""); if (!color.matches("[0-9a-fA-F]{8}")) throw new IllegalArgumentException();
                if (selected == null && module.points().size() >= 128) { error = "Waypoint limit: 128"; return; }
                String id = selected == null ? UUID.randomUUID().toString() : selected;
                module.put(new WaypointsModule.Waypoint(id, fields[0].getValue(), px, py, pz,
                    (int)Long.parseLong(color, 16), fields[5].getValue(), point == null ? WaypointsModule.world(minecraft) : point.world(),
                    point == null ? minecraft.level.dimension().toString() : point.dimension(), false));
                selected = id; error = ""; rebuildWidgets();
            } catch (RuntimeException e) { error = "Invalid coordinates / color"; }
        }));
        addRenderableWidget(new EzButton(x + 75, height - 54, 70, 20, Component.literal("New"), true, b -> { selected = null; rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 150, height - 54, 70, 20, Component.literal("Delete"), true, b -> { if (selected != null) module.delete(selected); selected = null; rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 230, height - 54, 70, 20, Component.literal("<"), true, b -> { page = Math.max(0, page - 1); rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 310, height - 54, 70, 20, Component.literal(">"), true, b -> { page = Math.min(pages - 1, page + 1); rebuildWidgets(); }));
        addRenderableWidget(new EzButton(x + 230, height - 28, 150, 20, Component.literal("Back"), true, b -> onClose()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0xee10151c); g.centeredText(font, title, width / 2, 12, 0xffffffff);
        String[] labels = {"Name", "X", "Y", "Z", "AARRGGBB", "Icon"};
        for (int i = 0; i < 6; i++) g.text(font, labels[i], Math.max(10, width / 2 - 190), 42 + 24 * i, 0xffeeeeee);
        g.text(font, error, 10, height - 18, 0xffff5555); super.extractRenderState(g, mx, my, delta);
    }
    @Override public void onClose() { EzScreenBridge.set(minecraft, parent); }
}
