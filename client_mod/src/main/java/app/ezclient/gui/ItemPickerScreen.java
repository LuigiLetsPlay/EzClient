package app.ezclient.gui;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * High-performance full-game item and block picker modal for Waypoints.
 * Enables searching over 1,000+ vanilla items/blocks in real time with visual previews.
 */
public final class ItemPickerScreen extends Screen {
    public record ItemEntry(Identifier id, Item item, ItemStack stack, String name, String path) {}

    private static List<ItemEntry> cachedItems = null;

    private final Screen parent;
    private final String currentId;
    private final Consumer<String> onSelect;
    private final List<ItemEntry> filtered = new ArrayList<>();

    private EditBox searchBox;
    private String query = "";
    private double scroll = 0;
    private boolean draggingScrollbar = false;

    private int panelX, panelY, panelWidth, panelHeight;
    private final int slotSize = 26;
    private final int slotGap = 3;
    private final int cols = 9;

    public ItemPickerScreen(Screen parent, String currentId, Consumer<String> onSelect) {
        super(Component.literal("Icon wählen"));
        this.parent = parent;
        this.currentId = currentId;
        this.onSelect = onSelect;
    }

    private static synchronized void ensureCache() {
        if (cachedItems != null) return;
        ItemIconHelper.ensureComponentsBound();
        List<ItemEntry> list = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            try {
                ItemStack stack = ItemIconHelper.createSafeStack(item);
                if (stack == null || stack.isEmpty()) continue;
                String name = stack.getHoverName().getString();
                list.add(new ItemEntry(id, item, stack, name, id.getPath()));
            } catch (Throwable ignored) {}
        }
        list.sort(Comparator.comparing(ItemEntry::name, String.CASE_INSENSITIVE_ORDER));
        cachedItems = list;
    }

    @Override
    protected void init() {
        ensureCache();
        panelWidth = 416;
        panelHeight = 236;
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        // Close button ✕
        addRenderableWidget(new EzButton(panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false, b -> onClose()));

        // Search Bar
        int searchW = panelWidth - 24;
        searchBox = new EditBox(font, panelX + 12, panelY + 26, searchW, 18, Component.literal("Suchen …"));
        searchBox.setHint(Component.literal(app.ezclient.util.EzI18n.text("Item / Block suchen (z. B. Bett, Eimer) …")));
        searchBox.setValue(query);
        searchBox.setMaxLength(80);
        searchBox.setResponder(text -> {
            query = text.trim().toLowerCase(Locale.ROOT);
            updateFilter();
        });
        addRenderableWidget(searchBox);

        // Cancel button in footer
        int footerY = panelY + panelHeight - 22;
        addRenderableWidget(new EzButton(panelX + panelWidth - 72, footerY, 60, 16,
                Component.literal(app.ezclient.util.EzI18n.text("Abbrechen")), false, b -> onClose()));

        updateFilter();
    }

    private void updateFilter() {
        filtered.clear();
        if (query.isEmpty()) {
            filtered.addAll(cachedItems);
        } else {
            for (var entry : cachedItems) {
                if (entry.name().toLowerCase(Locale.ROOT).contains(query)
                        || entry.path().toLowerCase(Locale.ROOT).contains(query)
                        || entry.id().toString().toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(entry);
                }
            }
        }
        clampScroll();
    }

    private int gridLeft() {
        int gridW = cols * (slotSize + slotGap) - slotGap;
        return panelX + (panelWidth - gridW - 10) / 2;
    }

    private int gridTop() { return panelY + 48; }
    private int gridHeight() { return panelHeight - 74; }

    private int maxScroll() {
        int totalRows = (int) Math.ceil((double) filtered.size() / cols);
        int contentH = totalRows * (slotSize + slotGap);
        return Math.max(0, contentH - gridHeight());
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll() > 0) {
            scroll -= vertical * (slotSize + slotGap);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int gx = gridLeft();
            int gy = gridTop();
            int gh = gridHeight();
            int gridW = cols * (slotSize + slotGap) - slotGap;

            // Check scrollbar click
            int barX = gx + gridW + 4;
            if (maxScroll() > 0 && event.x() >= barX && event.x() <= barX + 6 && event.y() >= gy && event.y() <= gy + gh) {
                draggingScrollbar = true;
                double fraction = (event.y() - gy) / (double) gh;
                scroll = fraction * maxScroll();
                clampScroll();
                return true;
            }

            // Check slot click
            if (event.x() >= gx && event.x() <= gx + gridW && event.y() >= gy && event.y() <= gy + gh) {
                for (int i = 0; i < filtered.size(); i++) {
                    int row = i / cols;
                    int col = i % cols;
                    int sx = gx + col * (slotSize + slotGap);
                    int sy = gy + row * (slotSize + slotGap) - (int) scroll;
                    if (event.x() >= sx && event.x() < sx + slotSize && event.y() >= sy && event.y() < sy + slotSize) {
                        var entry = filtered.get(i);
                        if (onSelect != null) {
                            onSelect.accept(entry.id().toString());
                        }
                        onClose();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar && maxScroll() > 0) {
            int gy = gridTop();
            int gh = gridHeight();
            double fraction = (event.y() - gy) / (double) gh;
            scroll = fraction * maxScroll();
            clampScroll();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);

        // Header Title
        g.text(font, "Icon wählen", panelX + 12, panelY + 9, EzUi.TEXT_WHITE);
        g.text(font, "• " + filtered.size() + " Items", panelX + 12 + font.width("Icon wählen") + 6, panelY + 9, EzUi.TEXT_DIM);

        int gx = gridLeft();
        int gy = gridTop();
        int gh = gridHeight();
        int gridW = cols * (slotSize + slotGap) - slotGap;

        // Grid Scissor Clip
        g.enableScissor(gx - 2, gy, gx + gridW + 12, gy + gh);

        ItemEntry hoveredEntry = null;

        for (int i = 0; i < filtered.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int sx = gx + col * (slotSize + slotGap);
            int sy = gy + row * (slotSize + slotGap) - (int) scroll;

            if (sy + slotSize < gy || sy > gy + gh) continue;

            ItemEntry entry = filtered.get(i);
            boolean isHovered = (mx >= sx && mx < sx + slotSize && my >= sy && my < sy + slotSize && my >= gy && my <= gy + gh);
            boolean isSelected = entry.id().toString().equals(currentId) || entry.path().equalsIgnoreCase(currentId);

            int bg = isHovered ? 0x6022C96E : (isSelected ? 0x4038BDF8 : 0x25141920);
            int border = isHovered ? 0xFF22C96E : (isSelected ? 0xFF38BDF8 : 0xFF28313D);

            EzUi.roundedRect(g, sx, sy, slotSize, slotSize, 3, border);
            EzUi.roundedRect(g, sx + 1, sy + 1, slotSize - 2, slotSize - 2, 2, bg);

            // Render Item Icon
            g.item(entry.stack(), sx + 5, sy + 5);

            if (isHovered) {
                hoveredEntry = entry;
            }
        }

        g.disableScissor();

        // Scrollbar
        if (maxScroll() > 0) {
            int barX = gx + gridW + 4;
            int barW = 5;
            g.fill(barX, gy, barX + barW, gy + gh, 0xFF1C232C);
            int thumbH = Math.max(16, (int) ((double) gh * gh / (gh + maxScroll())));
            int thumbY = gy + (int) ((scroll / maxScroll()) * (gh - thumbH));
            g.fill(barX, thumbY, barX + barW, thumbY + thumbH, EzUi.ACCENT_EMERALD);
        }

        // Footer item name display
        int footerY = panelY + panelHeight - 20;
        if (hoveredEntry != null) {
            String label = hoveredEntry.name() + " (" + hoveredEntry.id().getPath() + ")";
            g.text(font, label, panelX + 12, footerY + 3, EzUi.TEXT_WHITE);
        } else if (currentId != null && !currentId.isBlank()) {
            g.text(font, "Aktuell: " + currentId, panelX + 12, footerY + 3, EzUi.TEXT_MUTED);
        }

        super.extractRenderState(g, mx, my, delta);

        // Hover Floating Tooltip
        if (hoveredEntry != null) {
            String title = hoveredEntry.name();
            String idStr = hoveredEntry.id().toString();
            int tipW = Math.max(font.width(title), font.width(idStr)) + 12;
            int tipH = 26;
            int tipX = Math.min(width - tipW - 6, mx + 10);
            int tipY = Math.max(6, my - tipH - 4);

            EzUi.roundedRect(g, tipX - 1, tipY - 1, tipW + 2, tipH + 2, 4, 0xDD000000);
            EzUi.roundedRect(g, tipX, tipY, tipW, tipH, 3, 0xF0141922);
            g.text(font, title, tipX + 6, tipY + 4, EzUi.TEXT_WHITE);
            g.text(font, idStr, tipX + 6, tipY + 14, 0xFF94A3B8);
        }
    }

    @Override
    public void onClose() {
        EzScreenBridge.set(minecraft, parent);
    }
}