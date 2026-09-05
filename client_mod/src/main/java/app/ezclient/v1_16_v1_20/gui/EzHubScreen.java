package app.ezclient.v1_16_v1_20.gui;

import app.ezclient.v1_16_v1_20.EzClientMod_1_16_1_20;
import app.ezclient.v1_16_v1_20.modules.Module;
import app.ezclient.v1_16_v1_20.modules.ModuleManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern Glassmorphism Dashboard for 1.16.5–1.20.1 Compatibility builds.
 */
public class EzHubScreen extends Screen {
    private final Screen parent;
    private final long openTime;

    private int panelX, panelY, panelW, panelH;
    private String searchQuery = "";
    private boolean searchFocused = false;

    private static final String[] CATEGORIES = {"All", "HUD", "PvP", "Movement", "Render"};
    private String selectedCategory = "All";

    private int scrollOffset = 0;
    private int maxScroll = 0;

    public EzHubScreen(Screen parent) {
        super(Text.of("EzClient Dashboard"));
        this.parent = parent;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        panelW = 430;
        panelH = 260;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    private List<Module> getFilteredModules() {
        List<Module> all = ModuleManager.getInstance().getModules();
        List<Module> result = new ArrayList<Module>();
        String query = searchQuery.trim().toLowerCase();

        for (Module m : all) {
            boolean matchesCat = "All".equalsIgnoreCase(selectedCategory) || m.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchesSearch = query.isEmpty()
                    || m.getName().toLowerCase().contains(query)
                    || m.getDescription().toLowerCase().contains(query);

            if (matchesCat && matchesSearch) {
                result.add(m);
            }
        }
        return result;
    }

    //? if <=1.20.1 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (amount != 0) {
            if (amount > 0) {
                scrollOffset = Math.max(0, scrollOffset - 26);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 26);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0) {
            if (verticalAmount > 0) {
                scrollOffset = Math.max(0, scrollOffset - 26);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 26);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    *///?}

    //? if <=1.19.4 {
    /*@Override
    public void render(net.minecraft.client.util.math.MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderDashboard(mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }
    *///?} else {
    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        renderDashboard(mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
    //?}

    private void renderDashboard(int mouseX, int mouseY, float delta) {
        // 1. Dark ambient background tint
        RenderUtils.drawRect(0, 0, width, height, 0x65000000);

        // 2. Glassmorphism Main Window Panel
        RenderUtils.drawGlassPanel(panelX, panelY, panelW, panelH, 0xF210141D, 0xFA0B0E14, 0xFF2B3446, 0x1A55FF55);

        // 3. Header Bar with Emerald Accent
        int headerH = 34;
        RenderUtils.drawGradientRect(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + headerH, 0xFF171D29, 0xFF121721);
        RenderUtils.drawRect(panelX, panelY + headerH - 1, panelX + panelW, panelY + headerH, 0xFF273042);

        // Logo Pill & Title
        RenderUtils.drawBorderedRect(panelX + 10, panelY + 8, panelX + 26, panelY + 24, 1.0F, 0xFF55FF55, 0xFF22AA22);
        RenderUtils.drawString("EZ", panelX + 12, panelY + 12, 0xFF000000, false);
        RenderUtils.drawString("EzClient Dashboard", panelX + 32, panelY + 12, 0xFFFFFFFF, true);
        RenderUtils.drawString("v" + EzClientMod_1_16_1_20.CLIENT_VERSION, panelX + 146, panelY + 12, 0xFF55FF55, false);

        // Search Bar (Top Right)
        int searchBoxW = 104;
        int searchBoxH = 16;
        int searchBoxX = panelX + panelW - searchBoxW - 30;
        int searchBoxY = panelY + 9;

        int searchBg = searchFocused ? 0xFF1F2636 : 0xFF141822;
        int searchBorder = searchFocused ? 0xFF55FF55 : 0xFF354054;
        RenderUtils.drawBorderedRect(searchBoxX, searchBoxY, searchBoxX + searchBoxW, searchBoxY + searchBoxH, 1.0F, searchBg, searchBorder);

        if (searchQuery.isEmpty() && !searchFocused) {
            RenderUtils.drawString("Search...", searchBoxX + 6, searchBoxY + 4, 0xFF586278, false);
        } else {
            String display = searchQuery + (searchFocused && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : "");
            RenderUtils.drawString(display, searchBoxX + 6, searchBoxY + 4, 0xFFFFFFFF, false);
        }

        // Close button [X]
        int closeX = panelX + panelW - 22;
        int closeY = panelY + 9;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14;
        RenderUtils.drawBorderedRect(closeX, closeY, closeX + 14, closeY + 14, 1.0F, closeHover ? 0xFFDD3333 : 0xFF222836, closeHover ? 0xFFFF4444 : 0xFF3A445A);
        RenderUtils.drawString("✕", closeX + 4, closeY + 3, 0xFFFFFFFF, false);

        // 4. Category Tabs
        int tabY = panelY + 40;
        int tabX = panelX + 12;
        for (String cat : CATEGORIES) {
            int catW = RenderUtils.getStringWidth(cat) + 16;
            int catH = 16;
            boolean selected = cat.equalsIgnoreCase(selectedCategory);
            boolean hover = mouseX >= tabX && mouseX <= tabX + catW && mouseY >= tabY && mouseY <= tabY + catH;

            int bg = selected ? 0xFF55FF55 : (hover ? 0xFF262E3E : 0xFF151923);
            int border = selected ? 0xFF22AA22 : (hover ? 0xFF45526C : 0xFF283144);
            int textCol = selected ? 0xFF000000 : (hover ? 0xFFFFFFFF : 0xFF8B96AC);

            RenderUtils.drawBorderedRect(tabX, tabY, tabX + catW, tabY + catH, 1.0F, bg, border);
            RenderUtils.drawString(cat, tabX + 8, tabY + 4, textCol, false);

            tabX += catW + 6;
        }

        // 5. Scrollable Module List Area
        int listX = panelX + 12;
        int listY = panelY + 62;
        int listW = panelW - 24;
        int listH = panelH - 90;

        RenderUtils.drawBorderedRect(listX, listY, listX + listW, listY + listH, 1.0F, 0xCC11151D, 0xFF222B3B);

        List<Module> modules = getFilteredModules();
        int itemH = 38;
        int totalContentH = modules.size() * (itemH + 4);
        maxScroll = Math.max(0, totalContentH - listH);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        int currentY = listY + 4 - scrollOffset;

        for (Module m : modules) {
            if (currentY + itemH >= listY && currentY <= listY + listH) {
                int itemW = listW - 10;
                boolean itemHover = mouseX >= listX + 4 && mouseX <= listX + 4 + itemW && mouseY >= Math.max(listY, currentY) && mouseY <= Math.min(listY + listH, currentY + itemH);

                int itemBg = itemHover ? 0xF01C222E : 0xDD151922;
                int itemBorder = itemHover ? 0xFF3D4B63 : 0xFF222B3B;
                RenderUtils.drawBorderedRect(listX + 4, currentY, listX + 4 + itemW, currentY + itemH, 1.0F, itemBg, itemBorder);

                // Status Indicator Dot
                int dotColor = m.isEnabled() ? 0xFF55FF55 : 0xFF555555;
                RenderUtils.drawRect(listX + 10, currentY + 10, listX + 14, currentY + 14, dotColor);

                // Module Name & Description
                RenderUtils.drawString(m.getName(), listX + 18, currentY + 6, 0xFFFFFFFF, true);
                RenderUtils.drawString(m.getDescription(), listX + 18, currentY + 20, 0xFF6C7588, false);

                // Category pill tag
                int tagW = RenderUtils.getStringWidth(m.getCategory()) + 8;
                int nameW = RenderUtils.getStringWidth(m.getName());
                RenderUtils.drawBorderedRect(listX + 22 + nameW, currentY + 5, listX + 22 + nameW + tagW, currentY + 15, 0.5F, 0xFF202734, 0xFF354157);
                RenderUtils.drawString(m.getCategory(), listX + 26 + nameW, currentY + 6, 0xFF88AACC, false);

                // Buttons on Right Side
                int btnY = currentY + (itemH - 16) / 2;

                // 0. Settings Gear Button [ ⚙ ]
                int gearW = 16;
                int gearH = 16;
                int gearX = listX + 4 + itemW - gearW - 4;
                boolean gearHover = mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= btnY && mouseY <= btnY + gearH;
                RenderUtils.drawBorderedRect(gearX, btnY, gearX + gearW, btnY + gearH, 1.0F, gearHover ? 0xFF353C4D : 0xFF1C222E, gearHover ? 0xFF55FF55 : 0xFF384357);
                RenderUtils.drawString("⚙", gearX + 4, btnY + 4, gearHover ? 0xFF55FF55 : 0xFF8899AA, false);

                // 1. Main Toggle Button [ ON / OFF ]
                int toggleW = 34;
                int toggleH = 16;
                int toggleX = gearX - toggleW - 4;

                boolean toggleHover = mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= btnY && mouseY <= btnY + toggleH;
                int toggleBg = m.isEnabled() ? 0xFF55FF55 : (toggleHover ? 0xFF353C4D : 0xFF242A36);
                int toggleBorder = m.isEnabled() ? 0xFF22AA22 : 0xFF384357;
                int toggleTextColor = m.isEnabled() ? 0xFF000000 : 0xFF7A8396;
                String toggleText = m.isEnabled() ? "ON" : "OFF";

                RenderUtils.drawBorderedRect(toggleX, btnY, toggleX + toggleW, btnY + toggleH, 1.0F, toggleBg, toggleBorder);
                int tStrW = RenderUtils.getStringWidth(toggleText);
                RenderUtils.drawString(toggleText, toggleX + (toggleW - tStrW) / 2.0F, btnY + 4, toggleTextColor, false);

                int nextRightX = toggleX - 6;

                if (m.hasOptionalHud()) {
                    // 2. HUD Toggle Button [ HUD: ON / OFF ] - Only for optional HUD modules (like ToggleSprint)
                    int hudW = 46;
                    int hudH = 16;
                    int hudX = nextRightX - hudW;
                    boolean hudHover = mouseX >= hudX && mouseX <= hudX + hudW && mouseY >= btnY && mouseY <= btnY + hudH;
                    int hudBg = m.isShowHud() ? 0xFF263828 : (hudHover ? 0xFF353C4D : 0xFF1E232E);
                    int hudBorder = m.isShowHud() ? 0xFF55FF55 : 0xFF384357;
                    int hudTextCol = m.isShowHud() ? 0xFF55FF55 : 0xFF777E90;
                    String hudText = m.isShowHud() ? "HUD: ON" : "HUD: OFF";

                    RenderUtils.drawBorderedRect(hudX, btnY, hudX + hudW, btnY + hudH, 1.0F, hudBg, hudBorder);
                    int hStrW = RenderUtils.getStringWidth(hudText);
                    RenderUtils.drawString(hudText, hudX + (hudW - hStrW) / 2.0F, btnY + 4, hudTextCol, false);

                    nextRightX = hudX - 4;
                }

                if (m.hasHudElement()) {
                    // 3. Color Mode Button [ Color Name ]
                    int colW = 50;
                    int colH = 16;
                    int colX = nextRightX - colW;
                    boolean colHover = mouseX >= colX && mouseX <= colX + colW && mouseY >= btnY && mouseY <= btnY + colH;
                    int colBg = colHover ? 0xFF2B3342 : 0xFF1C212C;
                    int colPreview = m.getTextColor(0xFFFFFFFF);

                    RenderUtils.drawBorderedRect(colX, btnY, colX + colW, btnY + colH, 1.0F, colBg, 0xFF364156);
                    String colStr = m.getColorName();
                    int cStrW = RenderUtils.getStringWidth(colStr);
                    RenderUtils.drawString(colStr, colX + (colW - cStrW) / 2.0F, btnY + 4, colPreview, false);

                    nextRightX = colX - 4;

                    // 4. Background Button [ BG ]
                    int bgW = 26;
                    int bgH = 16;
                    int bgX = nextRightX - bgW;
                    boolean bgHover = mouseX >= bgX && mouseX <= bgX + bgW && mouseY >= btnY && mouseY <= btnY + bgH;
                    int bgFill = m.isShowBackground() ? 0xFF263828 : (bgHover ? 0xFF353C4D : 0xFF1E232E);
                    int bgBdr = m.isShowBackground() ? 0xFF55FF55 : 0xFF384357;
                    int bgTextCol = m.isShowBackground() ? 0xFF55FF55 : 0xFF777E90;

                    RenderUtils.drawBorderedRect(bgX, btnY, bgX + bgW, btnY + bgH, 1.0F, bgFill, bgBdr);
                    int bStrW = RenderUtils.getStringWidth("BG");
                    RenderUtils.drawString("BG", bgX + (bgW - bStrW) / 2.0F, btnY + 4, bgTextCol, false);
                }
            }
            currentY += itemH + 4;
        }

        // Scrollbar
        if (maxScroll > 0) {
            int scrollbarX = listX + listW - 6;
            int scrollbarH = Math.max(16, (listH * listH) / Math.max(1, totalContentH));
            int scrollbarY = listY + (scrollOffset * (listH - scrollbarH)) / maxScroll;
            RenderUtils.drawRect(scrollbarX, listY, scrollbarX + 4, listY + listH, 0xFF181D26);
            RenderUtils.drawRect(scrollbarX, scrollbarY, scrollbarX + 4, scrollbarY + scrollbarH, 0xFF55FF55);
        }

        // 6. Footer / Bottom Bar
        int footerY = panelY + panelH - 24;
        RenderUtils.drawGradientRect(panelX + 1, footerY, panelX + panelW - 1, panelY + panelH - 1, 0xFF141822, 0xFF10131B);
        RenderUtils.drawRect(panelX, footerY, panelX + panelW, footerY + 1, 0xFF252D3D);

        RenderUtils.drawString("Press [RSHIFT] or [ESC] to return to game", panelX + 12, footerY + 8, 0xFF666666, false);

        // Edit HUD Layout button in footer
        int hudBtnW = 90;
        int hudBtnH = 15;
        int hudBtnX = panelX + panelW - hudBtnW - 8;
        int hudBtnY = footerY + 5;
        boolean hudHover = mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH;
        RenderUtils.drawBorderedRect(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, 1.0F, hudHover ? 0xFF44DD44 : 0xFF55FF55, 0xFF22AA22);
        int hudStrW = RenderUtils.getStringWidth("HUD Layout");
        RenderUtils.drawString("HUD Layout", hudBtnX + (hudBtnW - hudStrW) / 2.0F, hudBtnY + 4, 0xFF000000, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Close button [X]
            int closeX = panelX + panelW - 22;
            int closeY = panelY + 9;
            if (mouseX >= closeX && mouseX <= closeX + 14 && mouseY >= closeY && mouseY <= closeY + 14) {
                EzClientMod_1_16_1_20.openScreen(client, parent);
                return true;
            }

            // HUD Layout button click
            int footerY = panelY + panelH - 24;
            int hudBtnW = 90;
            int hudBtnH = 15;
            int hudBtnX = panelX + panelW - hudBtnW - 8;
            int hudBtnY = footerY + 5;
            if (mouseX >= hudBtnX && mouseX <= hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY <= hudBtnY + hudBtnH) {
                EzClientMod_1_16_1_20.openScreen(client, new HudEditorScreen(this));
                return true;
            }

            // Search bar click
            int searchBoxW = 104;
            int searchBoxH = 16;
            int searchBoxX = panelX + panelW - searchBoxW - 30;
            int searchBoxY = panelY + 9;
            searchFocused = (mouseX >= searchBoxX && mouseX <= searchBoxX + searchBoxW && mouseY >= searchBoxY && mouseY <= searchBoxY + searchBoxH);

            // Category tabs click
            int tabY = panelY + 40;
            int tabX = panelX + 12;
            for (String cat : CATEGORIES) {
                int catW = RenderUtils.getStringWidth(cat) + 16;
                int catH = 16;
                if (mouseX >= tabX && mouseX <= tabX + catW && mouseY >= tabY && mouseY <= tabY + catH) {
                    selectedCategory = cat;
                    scrollOffset = 0;
                    return true;
                }
                tabX += catW + 6;
            }

            // Module list click
            int listX = panelX + 12;
            int listY = panelY + 62;
            int listW = panelW - 24;
            int listH = panelH - 90;

            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                List<Module> modules = getFilteredModules();
                int itemH = 38;
                int currentY = listY + 4 - scrollOffset;

                for (Module m : modules) {
                    int itemW = listW - 10;
                    if (mouseY >= currentY && mouseY <= currentY + itemH && mouseY >= listY && mouseY <= listY + listH) {
                        int btnY = currentY + (itemH - 16) / 2;

                        // 0. Settings Gear Button
                        int gearW = 16;
                        int gearH = 16;
                        int gearX = listX + 4 + itemW - gearW - 4;
                        if (mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= btnY && mouseY <= btnY + gearH) {
                            EzClientMod_1_16_1_20.openScreen(client, new ModuleSettingsScreen(this, m));
                            return true;
                        }

                        // 1. Toggle Button
                        int toggleW = 34;
                        int toggleH = 16;
                        int toggleX = gearX - toggleW - 4;
                        if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= btnY && mouseY <= btnY + toggleH) {
                            m.toggle();
                            ModuleManager.getInstance().saveConfig();
                            return true;
                        }

                        int nextRightX = toggleX - 6;

                        if (m.hasOptionalHud()) {
                            // 2. HUD Toggle Button
                            int hudW = 46;
                            int hudH = 16;
                            int hudX = nextRightX - hudW;
                            if (mouseX >= hudX && mouseX <= hudX + hudW && mouseY >= btnY && mouseY <= btnY + hudH) {
                                m.toggleShowHud();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            }

                            nextRightX = hudX - 4;
                        }

                        if (m.hasHudElement()) {
                            // 3. Color Mode Button
                            int colW = 50;
                            int colH = 16;
                            int colX = nextRightX - colW;
                            if (mouseX >= colX && mouseX <= colX + colW && mouseY >= btnY && mouseY <= btnY + colH) {
                                m.cycleColor();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            }

                            nextRightX = colX - 4;

                            // 4. Background Button
                            int bgW = 26;
                            int bgH = 16;
                            int bgX = nextRightX - bgW;
                            if (mouseX >= bgX && mouseX <= bgX + bgW && mouseY >= btnY && mouseY <= btnY + bgH) {
                                m.toggleShowBackground();
                                ModuleManager.getInstance().saveConfig();
                                return true;
                            }
                        }

                        // Row click toggles module
                        m.toggle();
                        ModuleManager.getInstance().saveConfig();
                        return true;
                    }
                    currentY += itemH + 4;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused) {
            if (Character.isLetterOrDigit(chr) || chr == ' ' || chr == '_' || chr == '-') {
                searchQuery += chr;
                scrollOffset = 0;
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (searchQuery.length() > 0) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    scrollOffset = 0;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            ModuleManager.getInstance().saveConfig();
            EzClientMod_1_16_1_20.openScreen(client, parent);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (System.currentTimeMillis() - openTime > 250) {
                ModuleManager.getInstance().saveConfig();
                EzClientMod_1_16_1_20.openScreen(client, parent);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
