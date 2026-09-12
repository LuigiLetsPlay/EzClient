package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Modern high-density waypoint manager.
 * Inspired by modern client interfaces: 18px compact tree view,
 * inline coordinates editor, quick color swatch palette, and fluid search.
 */
public final class WaypointScreen extends ScrollingSettingsScreen {
    private static final int ROW_HEIGHT = 18;

    private static final int[] PRESET_COLORS = {
        0xFFFF4757, 0xFFFFA502, 0xFFECCC68, 0xFF2ED573,
        0xFF1E90FF, 0xFF70A1FF, 0xFF9B59B6, 0xFFFFFFFF
    };

    private record WaypointRow(String id, int x, int y, int width, int height) {}
    private record FolderRow(String id, String name, int x, int y, int width, int height) {}

    private final Screen parent;
    private final WaypointsModule module;
    private final List<WaypointRow> waypointRows = new ArrayList<>();
    private final List<FolderRow> folderRows = new ArrayList<>();
    private final List<AbstractWidget> dynamicWidgets = new ArrayList<>();
    private final List<WaypointsModule.BlockPosition> blocks = new ArrayList<>();

    private String selected;
    private String search = "";
    private String error = "";
    private String icon = "Flag";
    private String folderId = WaypointsModule.DEFAULT_FOLDER;
    private String draggedWaypoint;
    private double dragStartX, dragStartY, dragX, dragY, accumulatedDrag;
    private boolean editor, dragged, draftLoaded;
    private String draftName, draftX, draftY, draftZ, draftDimension;
    private int panelX, panelY, panelWidth, panelHeight;
    private int editColor = 0xFF22C96E;
    private EditBox searchBox, nameField, xField, yField, zField;
    private boolean isListeningForHotkey = false;

    public WaypointScreen(Screen parent, WaypointsModule module) {
        super(Component.literal("WAYPOINTS"));
        this.parent = parent;
        this.module = module;
    }

    @Override protected int scrollLeft() { return settingsContentLeft(panelX); }
    @Override protected int scrollTop() { return panelY + 46; }
    @Override protected int scrollRight() { return panelX + panelWidth - 8; }
    @Override protected int scrollBottom() { return panelY + panelHeight - 28; }
    private int left() { return settingsContentLeft(panelX); }

    @Override
    protected void init() {
        module.setEnabled(true);
        panelWidth = settingsPanelWidth();
        panelHeight = settingsPanelHeight();
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        waypointRows.clear();
        folderRows.clear();
        dynamicWidgets.clear();

        if (editor) {
            buildEditor();
        } else {
            buildManager();
        }
    }

    private void buildManager() {
        int x0 = left();
        int innerWidth = settingsContentWidth(panelWidth);

        // Header close button ✕
        addFixedWidget(new EzButton(panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false, b -> onClose()));

        // Header settings button ⚙
        addFixedWidget(new EzButton(panelX + panelWidth - 40, panelY + 6, 16, 16,
                Component.literal("⚙"), false, b -> EzScreenBridge.set(minecraft, new FeatureSettingsScreen(this, module))));

        // Waypoints Manager Hotkey button
        String hkText = isListeningForHotkey ? "Taste: …" : (module.getKeyBind() > 0 || module.getKeyBind() <= -100 ? "Key: " + EzKeyBindings.getKeyOrMouseName(module.getKeyBind()) : "Key: M");
        addFixedWidget(new EzButton(panelX + panelWidth - 102, panelY + 6, 60, 16,
                Component.literal(hkText), isListeningForHotkey,
                b -> { isListeningForHotkey = !isListeningForHotkey; rebuildWidgets(); }));

        // Module enabled toggle switch in header
        EzToggleSwitch modToggle = new EzToggleSwitch(panelX + panelWidth - 132, panelY + 8, 26, 12, module.isEnabled(),
                val -> { module.setEnabled(val); rebuildWidgets(); });
        addFixedWidget(modToggle);

        // Search Bar (height 16px) & "+ Punkt" button
        int addBtnWidth = 54;
        int searchWidth = innerWidth - addBtnWidth - 4;
        searchBox = new EditBox(font, x0, panelY + 26, searchWidth, 16, Component.literal("Suchen …"));
        searchBox.setHint(Component.literal("Waypoint suchen …"));
        searchBox.setValue(search);
        searchBox.setMaxLength(80);
        searchBox.setResponder(value -> {
            search = value.toLowerCase(Locale.ROOT);
            populateManagerList();
        });
        addFixedWidget(searchBox);

        EzButton addPointButton = new EzButton(x0 + searchWidth + 4, panelY + 26, addBtnWidth, 16,
                Component.literal("+ Punkt"), true,
                button -> { selected = null; editor = true; draftLoaded = false; rebuildWidgets(); });
        addPointButton.active = hasActiveWorld();
        addFixedWidget(addPointButton);

        populateManagerList();

        // Footer Action Bar
        int footerY = panelY + panelHeight - 24;
        int btnW = (innerWidth - 8) / 3;
        addFixedWidget(new EzButton(x0, footerY, btnW, 16, Component.literal("+ Gruppe"), false,
                button -> { module.addFolder("Gruppe " + (module.folders().size() + 1)); rebuildWidgets(); }));
        EzButton positionButton = new EzButton(x0 + btnW + 4, footerY, btnW, 16, Component.literal("+ Position"), true,
                button -> {
                    if (hasActiveWorld()) beginPositionDraft();
                });
        positionButton.active = hasActiveWorld();
        addFixedWidget(positionButton);
        addFixedWidget(new EzButton(x0 + (btnW + 4) * 2, footerY, innerWidth - (btnW + 4) * 2, 16,
                Component.literal("Zurück"), false, button -> onClose()));
    }

    private void populateManagerList() {
        for (AbstractWidget w : dynamicWidgets) {
            removeWidget(w);
        }
        dynamicWidgets.clear();
        waypointRows.clear();
        folderRows.clear();

        int x0 = left();
        int innerWidth = settingsContentWidth(panelWidth);
        int rowY = scrollTop() + 2;

        String currentWorld = WaypointsModule.world(minecraft);
        String serverIp = (minecraft != null && minecraft.getCurrentServer() != null) ? minecraft.getCurrentServer().ip.trim().toLowerCase() : "";
        List<WaypointsModule.Waypoint> points = new ArrayList<>(module.points());
        if (minecraft != null && minecraft.level != null && !currentWorld.isEmpty() && !"default".equals(currentWorld)) {
            points.removeIf(p -> !p.world().equalsIgnoreCase(currentWorld)
                    && (serverIp.isEmpty() || !p.world().equalsIgnoreCase(serverIp)));
        }
        points.sort(Comparator.comparing(WaypointsModule.Waypoint::name, String.CASE_INSENSITIVE_ORDER));

        for (var folder : module.folders()) {
            boolean isDefault = WaypointsModule.DEFAULT_FOLDER.equals(folder.id());
            int folderBtnW = innerWidth - 40;

            folderRows.add(new FolderRow(folder.id(), folder.name(), x0, rowY, folderBtnW, ROW_HEIGHT));
            EzButton folderBtn = new EzButton(x0, rowY, folderBtnW, ROW_HEIGHT,
                    Component.literal((folder.collapsed() ? "▶ " : "▼ ") + folder.name()), false,
                    button -> { module.toggleFolder(folder.id(), true); rebuildWidgets(); })
                    .withRightClick(button -> { module.toggleFolder(folder.id(), false); rebuildWidgets(); });
            addRenderableWidget(folderBtn);
            dynamicWidgets.add(folderBtn);

            EzToggleSwitch folderVisToggle = new EzToggleSwitch(x0 + innerWidth - 36, rowY + 3, 20, 12, folder.visible(),
                    val -> { module.toggleFolder(folder.id(), false); rebuildWidgets(); });
            addRenderableWidget(folderVisToggle);
            dynamicWidgets.add(folderVisToggle);

            if (!isDefault) {
                EzButton delFolderBtn = new EzButton(x0 + innerWidth - 14, rowY + 2, 14, 14,
                        Component.literal("✕"), false,
                        button -> { module.deleteFolder(folder.id()); rebuildWidgets(); });
                addRenderableWidget(delFolderBtn);
                dynamicWidgets.add(delFolderBtn);
            }
            rowY += ROW_HEIGHT + 2;

            if (folder.collapsed()) continue;

            for (var point : points) {
                if (!point.folderId().equals(folder.id())) continue;
                if (!search.isBlank() && !point.name().toLowerCase(Locale.ROOT).contains(search)) continue;

                String distance = minecraft.player == null ? "" : Math.round(point.position().distanceTo(minecraft.player.position())) + "m";
                int labelWidth = innerWidth - 64;

                waypointRows.add(new WaypointRow(point.id(), x0 + 8, rowY, labelWidth, ROW_HEIGHT));

                String cleanName = WaypointsModule.cleanWaypointName(point.name());
                String displayText = cleanName + (distance.isEmpty() ? "" : "  (" + distance + ")");
                EzButton rowBtn = new EzButton(x0 + 8, rowY, labelWidth, ROW_HEIGHT,
                        Component.literal(displayText), false,
                        b -> {
                            selected = point.id();
                            editor = true;
                            draftLoaded = false;
                            rebuildWidgets();
                        });
                addRenderableWidget(rowBtn);
                dynamicWidgets.add(rowBtn);

                // Edit Button ✎ (14x14)
                EzButton editBtn = new EzButton(x0 + innerWidth - 54, rowY + 2, 14, 14,
                        Component.literal("✎"), false,
                        b -> {
                            selected = point.id();
                            editor = true;
                            draftLoaded = false;
                            rebuildWidgets();
                        });
                addRenderableWidget(editBtn);
                dynamicWidgets.add(editBtn);

                // Compact visibility toggle switch (18x12)
                EzToggleSwitch visToggle = new EzToggleSwitch(x0 + innerWidth - 36, rowY + 3, 18, 12, point.visible(),
                        val -> module.toggleWaypoint(point.id()));
                addRenderableWidget(visToggle);
                dynamicWidgets.add(visToggle);

                // Compact delete button (14x14)
                EzButton delBtn = new EzButton(x0 + innerWidth - 14, rowY + 2, 14, 14,
                        Component.literal("✕"), false,
                        b -> { module.delete(point.id()); rebuildWidgets(); });
                addRenderableWidget(delBtn);
                dynamicWidgets.add(delBtn);

                rowY += ROW_HEIGHT;
            }
            rowY += 2;
        }
    }

    private void buildEditor() {
        int x0 = left();
        int innerWidth = settingsContentWidth(panelWidth);
        var point = module.points().stream().filter(item -> item.id().equals(selected)).findFirst().orElse(null);
        var player = minecraft.player;

        if (!draftLoaded) {
            // New waypoints start with an actual empty input. The hint explains the
            // field without forcing the user to first remove a generated name.
            draftName = point == null ? "" : WaypointsModule.cleanWaypointName(point.name());
            draftX = point == null ? String.valueOf(Math.floor(player == null ? 0 : player.getX())) : String.valueOf(point.x());
            draftY = point == null ? String.valueOf(Math.floor(player == null ? 64 : player.getY())) : String.valueOf(point.y());
            draftZ = point == null ? String.valueOf(Math.floor(player == null ? 0 : player.getZ())) : String.valueOf(point.z());
            draftDimension = point == null ? null : point.dimension();
            editColor = point == null ? 0xFF22C96E : point.color();
            if (point == null) editColor = WaypointsModule.randomColor();
            icon = point == null ? "Flag" : point.icon();
            folderId = point == null ? WaypointsModule.DEFAULT_FOLDER : point.folderId();
            blocks.clear();
            if (point != null) blocks.addAll(point.blocks());
            draftLoaded = true;
        }

        // Header close button ✕
        addFixedWidget(new EzButton(panelX + panelWidth - 22, panelY + 6, 16, 16,
                Component.literal("✕"), false,
                button -> { editor = false; draftLoaded = false; rebuildWidgets(); }));

        // Editor controls are scrollable, so they must start inside the scroll
        // viewport. Previously the name field was placed above scrollTop() and was
        // consequently clipped to a two-pixel strip that could not be clicked.
        int y = scrollTop() + 2;

        // Row 1: Name Field
        nameField = new EditBox(font, x0, y, innerWidth, 18, Component.literal("Name"));
        nameField.setHint(Component.literal("Waypoint Name"));
        nameField.setValue(draftName != null ? draftName : (point == null ? "" : point.name()));
        nameField.setMaxLength(80);
        nameField.setResponder(val -> draftName = val);
        addRenderableWidget(nameField);
        setInitialFocus(nameField);
        y += 24;

        // Row 2: Compact 3-Column Coordinates (X, Y, Z) + Current Pos Button
        int hereW = 50;
        int coordW = (innerWidth - hereW - 9) / 3;
        xField = new EditBox(font, x0, y, coordW, 18, Component.literal("X"));
        xField.setHint(Component.literal("X"));
        xField.setValue(draftX != null ? draftX : (point == null ? String.valueOf(Math.floor(player == null ? 0 : player.getX())) : String.valueOf(point.x())));
        addRenderableWidget(xField);

        yField = new EditBox(font, x0 + coordW + 3, y, coordW, 18, Component.literal("Y"));
        yField.setHint(Component.literal("Y"));
        yField.setValue(draftY != null ? draftY : (point == null ? String.valueOf(Math.floor(player == null ? 64 : player.getY())) : String.valueOf(point.y())));
        addRenderableWidget(yField);

        zField = new EditBox(font, x0 + (coordW + 3) * 2, y, coordW, 18, Component.literal("Z"));
        zField.setHint(Component.literal("Z"));
        zField.setValue(draftZ != null ? draftZ : (point == null ? String.valueOf(Math.floor(player == null ? 0 : player.getZ())) : String.valueOf(point.z())));
        addRenderableWidget(zField);

        addRenderableWidget(new EzButton(x0 + innerWidth - hereW, y, hereW, 18, Component.literal("⟳ Hier"), true,
                button -> {
                    if (minecraft.player != null) {
                        xField.setValue(String.valueOf(Math.floor(minecraft.player.getX())));
                        yField.setValue(String.valueOf(Math.floor(minecraft.player.getY())));
                        zField.setValue(String.valueOf(Math.floor(minecraft.player.getZ())));
                    }
                }));
        y += 24;

        // Row 3: Preset Color Swatches + Custom Picker Button
        int swatchSize = 16;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int c = PRESET_COLORS[i];
            int sx = x0 + i * (swatchSize + 4);
            addRenderableWidget(new ColorPresetButton(sx, y, swatchSize, swatchSize, c, () -> editColor = c));
        }
        int customBtnX = x0 + PRESET_COLORS.length * (swatchSize + 4) + 4;
        int customBtnW = innerWidth - (customBtnX - x0);
        addRenderableWidget(new EzButton(customBtnX, y, customBtnW, swatchSize, Component.literal("Farbe …"), false,
                button -> EzScreenBridge.set(minecraft, new ModuleColorScreen(this, "Waypoint-Farbe", editColor, color -> editColor = color))));
        y += 24;

        // Row 4: Icon & Folder Selector Pills
        int half = (innerWidth - 4) / 2;
        net.minecraft.world.item.ItemStack iconStack = WaypointsModule.resolveItemStack(icon);
        String iconName = iconStack.isEmpty() ? icon : iconStack.getHoverName().getString();
        if (iconName.length() > 14) iconName = iconName.substring(0, 13) + "…";
        addRenderableWidget(new EzButton(x0, y, half, 18, Component.literal("Icon: " + iconName), false,
                button -> {
                    if (nameField != null) draftName = nameField.getValue();
                    if (xField != null) draftX = xField.getValue();
                    if (yField != null) draftY = yField.getValue();
                    if (zField != null) draftZ = zField.getValue();
                    EzScreenBridge.set(minecraft, new ItemPickerScreen(this, icon, selectedId -> {
                        this.icon = selectedId;
                        draftLoaded = true;
                        rebuildWidgets();
                    }));
                }));
        addRenderableWidget(new EzButton(x0 + half + 4, y, half, 18, Component.literal("Gruppe: " + module.folder(folderId).name()), false,
                button -> {
                    var folders = module.folders();
                    folderId = folders.get((folders.indexOf(module.folder(folderId)) + 1) % folders.size()).id();
                    rebuildWidgets();
                }));
        y += 24;

        // Row 5: Blocks Counter & Management
        addRenderableWidget(new EzButton(x0, y, half, 18, Component.literal("⛶ Blöcke wählen (" + blocks.size() + ")"), false,
                button -> {
                    if (nameField != null) draftName = nameField.getValue();
                    if (xField != null) draftX = xField.getValue();
                    if (yField != null) draftY = yField.getValue();
                    if (zField != null) draftZ = zField.getValue();
                    BlockSelectionOverlay.start(minecraft, blocks, editColor, this, updated -> {
                        blocks.clear();
                        blocks.addAll(updated);
                        draftLoaded = true;
                        rebuildWidgets();
                    });
                }));
        addRenderableWidget(new EzButton(x0 + half + 4, y, half, 18, Component.literal("Blöcke leeren"), !blocks.isEmpty(),
                button -> {
                    blocks.clear();
                    rebuildWidgets();
                }));

        // Footer Actions
        int footerY = panelY + panelHeight - 24;
        addFixedWidget(new EzButton(x0, footerY, half, 18, Component.literal("Speichern"), true, button -> save()));
        addFixedWidget(new EzButton(x0 + half + 4, footerY, half, 18, Component.literal("Abbrechen"), false,
                button -> { editor = false; draftLoaded = false; rebuildWidgets(); }));
    }

    private void save() {
        try {
            if (minecraft.level == null) throw new IllegalArgumentException();
            String id = selected == null ? UUID.randomUUID().toString() : selected;
            var old = module.points().stream().filter(point -> point.id().equals(id)).findFirst().orElse(null);
            boolean saved = module.put(new WaypointsModule.Waypoint(id, WaypointsModule.cleanWaypointName(nameField.getValue().trim()),
                    Double.parseDouble(xField.getValue()),
                    Double.parseDouble(yField.getValue()),
                    Double.parseDouble(zField.getValue()),
                    editColor, icon,
                    old == null ? WaypointsModule.world(minecraft) : old.world(),
                    old == null ? (draftDimension == null ? minecraft.level.dimension().identifier().toString() : draftDimension) : old.dimension(),
                    old == null || old.visible(), folderId, blocks));
            if (!saved) throw new IllegalArgumentException();
            module.setEnabled(true);
            selected = id;
            editor = false;
            draftLoaded = false;
            error = "";
            rebuildWidgets();
        } catch (RuntimeException exception) {
            error = "Ungültiger Name oder Koordinaten";
        }
    }

    public static WaypointScreen fromShared(Screen parent, XaeroWaypointShare share) {
        WaypointScreen screen = new WaypointScreen(parent, FeatureModule.get(WaypointsModule.class));
        screen.editor = true;
        screen.draftLoaded = true;
        screen.draftName = WaypointsModule.cleanWaypointName(share.name());
        screen.draftX = String.valueOf(share.x());
        screen.draftY = String.valueOf(share.y());
        screen.draftZ = String.valueOf(share.z());
        screen.draftDimension = share.dimension();
        screen.editColor = share.color();
        screen.icon = "Flag";
        screen.folderId = WaypointsModule.DEFAULT_FOLDER;
        return screen;
    }

    private boolean hasActiveWorld() {
        return minecraft != null && minecraft.level != null && minecraft.player != null;
    }

    /** Opens a current-position draft without persisting anything before Save. */
    private void beginPositionDraft() {
        selected = null;
        editor = true;
        draftLoaded = true;
        draftName = "Waypoint " + (module.points().size() + 1);
        draftX = String.valueOf(Math.floor(minecraft.player.getX()));
        draftY = String.valueOf(Math.floor(minecraft.player.getY()));
        draftZ = String.valueOf(Math.floor(minecraft.player.getZ()));
        draftDimension = minecraft.level.dimension().identifier().toString();
        editColor = WaypointsModule.randomColor();
        icon = "Flag";
        folderId = WaypointsModule.DEFAULT_FOLDER;
        blocks.clear();
        error = "";
        rebuildWidgets();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isListeningForHotkey) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_BACKSPACE || event.key() == GLFW.GLFW_KEY_DELETE) {
                EzKeyBindings.applyModuleKeyBind(module, -1);
            } else {
                EzKeyBindings.applyModuleKeyBind(module, event.key());
            }
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        if (editor && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected boolean settingsMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isListeningForHotkey && event.button() != 0) {
            EzKeyBindings.applyModuleKeyBind(module, -100 - event.button());
            isListeningForHotkey = false;
            rebuildWidgets();
            return true;
        }
        if (!editor && event.button() == 0) {
            for (var row : waypointRows) {
                if (event.x() >= row.x() && event.x() <= row.x() + row.width()
                        && event.y() >= row.y() && event.y() <= row.y() + row.height()) {
                    draggedWaypoint = row.id();
                    dragStartX = dragX = event.x();
                    dragStartY = dragY = event.y();
                    dragged = false;
                    accumulatedDrag = 0;
                    return true;
                }
            }
        }
        return super.settingsMouseClicked(event, doubleClick);
    }

    @Override
    protected boolean settingsMouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggedWaypoint != null) {
            dragX = event.x();
            dragY = event.y();
            accumulatedDrag += Math.hypot(dx, dy);
            if (accumulatedDrag > 4) {
                dragged = true;
            }
            return true;
        }
        return super.settingsMouseDragged(event, dx, dy);
    }

    @Override
    protected boolean settingsMouseReleased(MouseButtonEvent event) {
        if (draggedWaypoint != null) {
            String id = draggedWaypoint;
            boolean wasDragged = dragged;
            draggedWaypoint = null;
            dragged = false;
            accumulatedDrag = 0;

            if (wasDragged) {
                FolderRow targetFolder = null;
                for (var folder : folderRows) {
                    int fy = folder.y() - (int) scrollAmount();
                    if (event.x() >= folder.x() && event.x() <= folder.x() + folder.width()
                            && event.y() >= fy && event.y() <= fy + folder.height()) {
                        targetFolder = folder;
                        break;
                    }
                }
                if (targetFolder != null) {
                    module.moveToFolder(id, targetFolder.id());
                }
                rebuildWidgets();
                return true;
            } else {
                selected = id;
                editor = true;
                draftLoaded = false;
                rebuildWidgets();
                return true;
            }
        }
        return super.settingsMouseReleased(event);
    }

    @Override
    protected void extractSettings(GuiGraphicsExtractor g, int mx, int my, float delta) {
        EzUi.backdrop(g, width, height);
        EzUi.panel(g, panelX, panelY, panelWidth, panelHeight);
        renderSettingsSidebar(g, panelX, panelY, panelHeight, "Waypoints");

        int left = left();
        int contentWidth = settingsContentWidth(panelWidth);

        // Header Title & Active World / Server Label
        String currentWorld = WaypointsModule.world(minecraft);
        String worldDisplayName = "";
        if (minecraft != null) {
            if (minecraft.getCurrentServer() != null) {
                worldDisplayName = minecraft.getCurrentServer().ip;
            } else if (minecraft.getSingleplayerServer() != null) {
                try {
                    worldDisplayName = minecraft.getSingleplayerServer().getWorldData().getLevelName();
                } catch (Throwable t) {
                    worldDisplayName = "Singleplayer";
                }
            }
        }

        String headerTitle = editor ? (selected == null ? "Punkt erstellen" : "Punkt bearbeiten") : "Waypoints";
        g.text(font, headerTitle, left, panelY + 9, EzUi.TEXT_WHITE);

        if (!editor) {
            String serverIp = (minecraft != null && minecraft.getCurrentServer() != null) ? minecraft.getCurrentServer().ip.trim().toLowerCase() : "";
            int currentWorldCount = (int) module.points().stream().filter(p -> {
                if (minecraft == null || minecraft.level == null || currentWorld.isEmpty() || "default".equals(currentWorld)) return true;
                return p.world().equalsIgnoreCase(currentWorld) || (!serverIp.isEmpty() && p.world().equalsIgnoreCase(serverIp));
            }).count();

            String subHeader = (worldDisplayName.isEmpty() ? "" : worldDisplayName + " • ") + currentWorldCount + " Punkte";
            g.text(font, "• " + subHeader, left + font.width(headerTitle) + 6, panelY + 9, EzUi.TEXT_DIM);

            if (currentWorldCount == 0) {
                g.centeredText(font, "Keine Waypoints in dieser Welt vorhanden – erstelle oben deinen ersten.",
                        left + contentWidth / 2, panelY + 100, EzUi.TEXT_MUTED);
            } else if (draggedWaypoint != null && dragged) {
                g.centeredText(font, "Auf eine Gruppe ziehen zum Verschieben", left + contentWidth / 2, panelY + 28, EzUi.ACCENT_EMERALD);
            }
        }

        if (editor && !error.isEmpty()) {
            g.text(font, error, left, panelY + panelHeight - 38, 0xFFFF5555);
        }

        // Drop target folder highlight
        FolderRow hoveredFolder = null;
        if (!editor && draggedWaypoint != null && dragged) {
            for (var folder : folderRows) {
                int fy = folder.y() - (int) scrollAmount();
                if (mx >= folder.x() && mx <= folder.x() + folder.width()
                        && my >= fy && my <= fy + folder.height()) {
                    hoveredFolder = folder;
                    break;
                }
            }
            if (hoveredFolder != null) {
                int fy = hoveredFolder.y() - (int) scrollAmount();
                EzUi.roundedRect(g, hoveredFolder.x() - 1, fy - 1, hoveredFolder.width() + 2, hoveredFolder.height() + 2, 3, 0xFF22C96E);
                EzUi.roundedRect(g, hoveredFolder.x(), fy, hoveredFolder.width(), hoveredFolder.height(), 2, 0xFF142B1F);
                g.text(font, "⤓ In '" + hoveredFolder.name() + "' ablegen", hoveredFolder.x() + 8, fy + 5, 0xFF5AEEA0);
            }
        }

        super.extractSettings(g, mx, my, delta);

        // Floating Drag Badge following mouse cursor (drawn on top of all widgets)
        if (!editor && draggedWaypoint != null && dragged) {
            var wp = module.points().stream().filter(p -> p.id().equals(draggedWaypoint)).findFirst().orElse(null);
            if (wp != null) {
                var stack = WaypointsModule.resolveItemStack(wp.icon());
                int cardW = Math.max(140, font.width(wp.name()) + 44);
                int cardH = 22;
                int cardX = Math.min(width - cardW - 8, Math.max(8, mx - cardW / 2));
                int cardY = Math.min(height - cardH - 8, Math.max(8, my - cardH - 6));

                EzUi.roundedRect(g, cardX - 1, cardY - 1, cardW + 2, cardH + 2, 4, 0xBB000000);
                EzUi.roundedRect(g, cardX, cardY, cardW, cardH, 3, 0xF0141922);
                int border = hoveredFolder != null ? 0xFF22C96E : 0xFF3D4B5C;
                g.fill(cardX, cardY, cardX + cardW, cardY + 1, border);
                g.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, border);
                g.fill(cardX, cardY + 1, cardX + 1, cardY + cardH - 1, border);
                g.fill(cardX + cardW - 1, cardY + 1, cardX + cardW, cardY + cardH - 1, border);

                if (!stack.isEmpty()) {
                    g.item(stack, cardX + 4, cardY + 3);
                }
                g.text(font, wp.name(), cardX + 24, cardY + 6, EzUi.TEXT_WHITE);
                if (hoveredFolder != null) {
                    EzUi.badge(g, cardX + cardW - 20, cardY + 4, 14, 14, 0xFF183B28, 0xFF22C96E, "✓", 0xFF5AEEA0);
                }
            }
        }
    }


    @Override
    public void onClose() {
        EzScreenBridge.set(minecraft, parent);
    }

    // ── Helper Color Preset Button ──

    private static final class ColorPresetButton extends net.minecraft.client.gui.components.AbstractButton {
        private final int color;
        private final Runnable onSelect;

        public ColorPresetButton(int x, int y, int width, int height, int color, Runnable onSelect) {
            super(x, y, width, height, Component.empty());
            this.color = color;
            this.onSelect = onSelect;
        }

        @Override
        public void onPress(net.minecraft.client.input.InputWithModifiers input) {
            if (active && onSelect != null) onSelect.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hovered = isHoveredOrFocused();
            EzUi.roundedRect(g, getX(), getY(), getWidth(), getHeight(), 2, hovered ? 0xFFFFFFFF : 0xFF3E4756);
            EzUi.roundedRect(g, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, 1, color);
        }

        @Override
        public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
