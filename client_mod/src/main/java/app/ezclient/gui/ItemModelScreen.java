package app.ezclient.gui;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Per-item editor; the existing searchable item picker covers the full item registry. */
public final class ItemModelScreen extends Screen {
    private static final String[] LABELS = {"X", "Y", "Z", "Pitch", "Yaw", "Roll", "X", "Y", "Z"};
    private final Screen parent;
    private final ItemModelModule module;
    private String itemId = "*";
    private ItemModelModule.View view = ItemModelModule.View.FIRST_PERSON;
    private int panelX, panelY, panelW, panelH;
    private ItemStack previewStack = ItemStack.EMPTY;

    public ItemModelScreen(Screen parent, ItemModelModule module) {
        super(Component.literal("Item Model"));
        this.parent = parent;
        this.module = module;
    }

    @Override protected void init() {
        panelW = Math.min(480, width - 24);
        panelH = Math.min(236, height - 24);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        Identifier selected = Identifier.tryParse(itemId);
        var previewItem = itemId.equals("*") ? Items.DIAMOND_SWORD
                : selected == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(selected);
        previewStack = ItemIconHelper.createSafeStack(previewItem);
        int left = panelX + 12;
        int right = panelX + panelW - 12;
        int contentW = right - left;
        int gap = 6;
        int colW = (contentW - 2 * gap) / 3;

        addRenderableWidget(new EzButton(right - 18, panelY + 7, 18, 16,
                Component.literal("✕"), false, b -> onClose()));
        addRenderableWidget(new EzToggleSwitch(right - 52, panelY + 8, 26, 14,
                module.isEnabled(), module::setEnabled));

        addRenderableWidget(new EzButton(left, panelY + 29, contentW - 94, 20,
                Component.literal(itemId.equals("*") ? app.ezclient.util.EzI18n.text("Alle Items (Standard)") : displayName()), false,
                b -> EzScreenBridge.set(minecraft, new ItemPickerScreen(this,
                        itemId.equals("*") ? "" : itemId, id -> { itemId = id; rebuildWidgets(); }))));
        addRenderableWidget(new EzButton(right - 88, panelY + 29, 88, 20,
                Component.literal(app.ezclient.util.EzI18n.text("Alle Items")), itemId.equals("*"),
                b -> { itemId = "*"; rebuildWidgets(); }));

        String[] tabLabels = {"First Person", "Gedroppt", "GUI"};
        ItemModelModule.View[] views = ItemModelModule.View.values();
        for (int i = 0; i < views.length; i++) {
            ItemModelModule.View tab = views[i];
            addRenderableWidget(new EzButton(left + i * (colW + gap), panelY + 57, colW, 19,
                    Component.literal(app.ezclient.util.EzI18n.text(tabLabels[i])), tab == view,
                    b -> { view = tab; rebuildWidgets(); }));
        }

        int baseY = panelY + Math.min(106, panelH - 114);
        int[] sectionY = {baseY, baseY + 33, baseY + 66};
        for (int section = 0; section < 3; section++) {
            for (int axis = 0; axis < 3; axis++) {
                int index = section * 3 + axis;
                float min = index < 3 ? -2 : index < 6 ? -180 : 0.05f;
                float max = index < 3 ? 2 : index < 6 ? 180 : 4;
                float current = module.getTransform(itemId, view).value(index);
                int sliderX = left + axis * (colW + gap);
                String label = LABELS[index];
                addRenderableWidget(new EzSlider(sliderX, sectionY[section], colW, 18,
                        (current - min) / (max - min),
                        normalized -> {
                            var value = module.getTransform(itemId, view)
                                    .with(index, (float)(min + normalized * (max - min)));
                            module.setTransform(itemId, view, value, false);
                        },
                        normalized -> Component.literal(label + ": " + String.format(Locale.ROOT,
                                index < 6 ? "%.1f" : "%.2f", min + normalized * (max - min))),
                        true, ConfigManager::save));
            }
        }

        addRenderableWidget(new EzButton(left, panelY + panelH - 27, 116, 18,
                Component.literal(app.ezclient.util.EzI18n.text("Ansicht zurücksetzen")), false,
                b -> { module.clearTransform(itemId, view); rebuildWidgets(); }));
        addRenderableWidget(new EzButton(right - 72, panelY + panelH - 27, 72, 18,
                Component.literal(app.ezclient.util.EzI18n.text("Fertig")), true, b -> onClose()));
    }

    private String displayName() {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return itemId;
        var item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) return itemId;
        return ItemIconHelper.createSafeStack(item).getHoverName().getString();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelW, panelH);
        int left = panelX + 12;
        g.text(font, "ITEM MODEL", left, panelY + 10, EzUi.TEXT_WHITE);
        String caption = itemId.equals("*") ? app.ezclient.util.EzI18n.text("Standardwerte für alle Items") : itemId;
        if (font.width(caption) > panelW - 104) caption = font.plainSubstrByWidth(caption, panelW - 110) + "…";
        g.text(font, caption,
                left, panelY + 80, EzUi.TEXT_MUTED);
        g.text(font, "GUI", panelX + panelW - 47, panelY + 80, EzUi.TEXT_DIM);
        if (!previewStack.isEmpty()) g.item(previewStack, panelX + panelW - 29, panelY + 76);
        int baseY = panelY + Math.min(106, panelH - 114) - 11;
        g.text(font, "POSITION", left, baseY, EzUi.TEXT_LIGHT);
        g.text(font, app.ezclient.util.EzI18n.text("DREHUNG"), left, baseY + 33, EzUi.TEXT_LIGHT);
        g.text(font, app.ezclient.util.EzI18n.text("SKALIERUNG"), left, baseY + 66, EzUi.TEXT_LIGHT);
        g.text(font, module.hasOverride(itemId, view) ? app.ezclient.util.EzI18n.text("• Individuell angepasst") : app.ezclient.util.EzI18n.text("• Standard"),
                left + 122, panelY + panelH - 23,
                module.hasOverride(itemId, view) ? EzUi.ACCENT_EMERALD : EzUi.TEXT_DIM);
        super.extractRenderState(g, mx, my, delta);
    }

    @Override public void onClose() { EzScreenBridge.set(minecraft, parent); }
    @Override public boolean isPauseScreen() { return false; }
}
