package app.ezclient.gui;

import app.ezclient.account.AccountManager;
import app.ezclient.account.EzAccount;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern EzClient Accounts Popup Screen.
 * Allows live account switching, Microsoft session refreshing,
 * account removal, and adding Microsoft or Offline accounts.
 */
public final class EzAccountScreen extends Screen {
    private static final int VIEW_LIST = 0;
    private static final int VIEW_ADD_MS = 1;

    private final Screen parent;
    private int currentView = VIEW_LIST;

    private int panelWidth = 320;
    private int panelHeight = 230;
    private int panelX, panelY;

    private double scrollOffset = 0.0;
    private double targetScrollOffset = 0.0;
    private double maxScroll = 0.0;

    private List<EzAccount> accounts = new ArrayList<>();
    private String activeUuid = "";

    private String statusMessage = "";
    private boolean statusIsError = false;
    private long statusClearTime = 0L;

    // View List widgets
    private EzButton addMsBtn;

    // View Add MS widgets
    private EditBox msCodeBox;
    private EzButton msBrowserBtn;
    private EzButton msSubmitBtn;
    private EzButton msCancelBtn;

    public EzAccountScreen(Screen parent) {
        super(Component.literal("Accounts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(320, width - 32);
        panelHeight = Math.min(230, height - 32);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        reloadAccounts();
        buildCurrentWidgets();
    }

    private void reloadAccounts() {
        this.accounts = AccountManager.getAccountList();
        this.activeUuid = AccountManager.getActiveUuid();
        int contentH = panelHeight - 78;
        int totalH = accounts.size() * 42;
        this.maxScroll = Math.max(0, totalH - contentH);
        this.targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset));
    }

    private void buildCurrentWidgets() {
        clearWidgets();

        if (currentView == VIEW_LIST) {
            int btnW = panelWidth - 24;
            int btnH = 16;
            int btnY = panelY + 28;

            addMsBtn = new EzButton(
                    panelX + 12, btnY, btnW, btnH,
                    Component.literal("+ Microsoft-Account hinzufügen"), true,
                    b -> switchView(VIEW_ADD_MS)
            );
            addRenderableWidget(addMsBtn);

        } else if (currentView == VIEW_ADD_MS) {
            int inputW = panelWidth - 24;
            int inputY = panelY + 76;

            msBrowserBtn = new EzButton(
                    panelX + 12, panelY + 36, inputW, 18,
                    Component.literal("1. Im Browser anmelden (hier klicken)"), true,
                    b -> {
                        try {
                            Util.getPlatform().openUri(new URI(AccountManager.MICROSOFT_AUTH_URL));
                            setStatus("Browser geöffnet. Melde dich an und kopiere den Link/Code.", false);
                        } catch (Exception e) {
                            setStatus("Fehler beim Öffnen des Browsers.", true);
                        }
                    }
            );
            addRenderableWidget(msBrowserBtn);

            msCodeBox = new EditBox(font, panelX + 12, inputY, inputW, 16, Component.literal("Code oder URL"));
            msCodeBox.setHint(Component.literal("Weiterleitungs-URL oder Code hier einfügen"));
            msCodeBox.setMaxLength(1024);
            addRenderableWidget(msCodeBox);

            int btnW = (panelWidth - 30) / 2;
            int btnY = panelY + panelHeight - 28;

            msSubmitBtn = new EzButton(
                    panelX + 12, btnY, btnW, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Anmelden")), true,
                    b -> {
                        String code = msCodeBox.getValue();
                        if (code == null || code.trim().isEmpty()) {
                            setStatus("Bitte Code oder Link einfügen.", true);
                            return;
                        }
                        setStatus("Melde an...", false);
                        AccountManager.addMicrosoftAccount(minecraft, code, msg -> {
                            boolean err = msg.startsWith("Fehler") || msg.startsWith("Ungültig");
                            setStatus(msg, err);
                            if (!err) {
                                switchView(VIEW_LIST);
                            }
                        });
                    }
            );
            addRenderableWidget(msSubmitBtn);

            msCancelBtn = new EzButton(
                    panelX + 18 + btnW, btnY, btnW, 18,
                    Component.literal(app.ezclient.util.EzI18n.text("Abbrechen")), false,
                    b -> switchView(VIEW_LIST)
            );
            addRenderableWidget(msCancelBtn);
        }
    }

    private void switchView(int view) {
        this.currentView = view;
        reloadAccounts();
        buildCurrentWidgets();
    }

    private void setStatus(String message, boolean isError) {
        this.statusMessage = message;
        this.statusIsError = isError;
        this.statusClearTime = System.currentTimeMillis() + 4500L;
    }

    @Override
    public void onClose() {
        EzScreenBridge.set(minecraft, parent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (e.button() == 0) {
            // Close button (top right of panel)
            int closeX = panelX + panelWidth - 18;
            int closeY = panelY + 9;
            if (e.x() >= closeX && e.x() <= closeX + 12 && e.y() >= closeY && e.y() <= closeY + 12) {
                onClose();
                return true;
            }

            // In List view: click account cards or delete button
            if (currentView == VIEW_LIST) {
                int cx = panelX + 12;
                int cy = panelY + 48;
                int cw = panelWidth - 24;
                int ch = panelHeight - 72;

                if (e.x() >= cx && e.x() <= cx + cw && e.y() >= cy && e.y() <= cy + ch) {
                    for (int i = 0; i < accounts.size(); i++) {
                        EzAccount acc = accounts.get(i);
                        int itemY = (int) (cy + i * 42 - scrollOffset);
                        if (itemY + 38 < cy || itemY > cy + ch) continue;

                        if (e.x() >= cx && e.x() <= cx + cw && e.y() >= itemY && e.y() <= itemY + 38) {
                            // Check if Delete button clicked (right 24px)
                            int delX = cx + cw - 22;
                            int delY = itemY + 10;
                            if (e.x() >= delX && e.x() <= delX + 16 && e.y() >= delY && e.y() <= delY + 16) {
                                setStatus("Entferne Account...", false);
                                AccountManager.removeAccount(minecraft, acc.getUuid(), msg -> {
                                    setStatus(msg, msg.startsWith("Fehler"));
                                    reloadAccounts();
                                    buildCurrentWidgets();
                                });
                                return true;
                            }

                            // If active account clicked: Refresh session
                            if (acc.getUuid().equalsIgnoreCase(activeUuid)) {
                                setStatus("Session wird aktualisiert...", false);
                                AccountManager.refreshSession(minecraft, acc, msg -> {
                                    setStatus(msg, msg.startsWith("Fehler"));
                                    reloadAccounts();
                                    buildCurrentWidgets();
                                });
                                return true;
                            }

                            // If inactive account clicked: Switch account
                            setStatus("Wechsle zu " + acc.getUsername() + "...", false);
                            AccountManager.switchAccount(minecraft, acc, msg -> {
                                setStatus(msg, msg.startsWith("Fehler"));
                                reloadAccounts();
                                buildCurrentWidgets();
                            });
                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentView == VIEW_LIST && maxScroll > 0) {
            targetScrollOffset = Math.max(0, Math.min(maxScroll, targetScrollOffset - verticalAmount * 24.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (currentView != VIEW_LIST) {
                switchView(VIEW_LIST);
                return true;
            }
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dim backdrop
        EzUi.backdrop(graphics, width, height);

        // Panel background
        EzUi.roundedRect(graphics, panelX, panelY, panelWidth, panelHeight, 6, EzUi.BG_PANEL);

        // Subtle outer border
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, EzUi.BORDER_SUBTLE);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, EzUi.BORDER_SUBTLE);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, EzUi.BORDER_SUBTLE);
        graphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, EzUi.BORDER_SUBTLE);

        // Emerald glow line at top
        EzUi.glowLine(graphics, panelX + 8, panelY + 1, panelWidth - 16);

        // Header Title
        String titleText = currentView == VIEW_LIST ? "ACCOUNTS" : "MICROSOFT ANMELDUNG";
        graphics.text(font, titleText, panelX + 14, panelY + 10, EzUi.TEXT_WHITE);

        // Header Close 'X' Button
        int closeX = panelX + panelWidth - 18;
        int closeY = panelY + 9;
        boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 12 && mouseY >= closeY && mouseY <= closeY + 12;
        graphics.text(font, "x", closeX, closeY, closeHovered ? 0xFFFF6B6B : EzUi.TEXT_MUTED);

        // Render Current View
        if (currentView == VIEW_LIST) {
            renderAccountList(graphics, mouseX, mouseY);
        } else if (currentView == VIEW_ADD_MS) {
            renderAddMsView(graphics);
        }

        // Status Banner at bottom
        if (statusMessage != null && !statusMessage.isEmpty()) {
            if (System.currentTimeMillis() > statusClearTime) {
                statusMessage = "";
            } else {
                int bannerY = panelY + panelHeight - 19;
                int bgColor = statusIsError ? 0xCC3D1515 : 0xCC133824;
                int textColor = statusIsError ? 0xFFFF7373 : EzUi.ACCENT_EMERALD;
                EzUi.roundedRect(graphics, panelX + 10, bannerY, panelWidth - 20, 14, 3, bgColor);
                graphics.centeredText(font, statusMessage, panelX + panelWidth / 2, bannerY + 3, textColor);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderAccountList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int cx = panelX + 12;
        int cy = panelY + 48;
        int cw = panelWidth - 24;
        int ch = panelHeight - 72;

        scrollOffset += (targetScrollOffset - scrollOffset) * 0.35;

        if (accounts.isEmpty()) {
            graphics.centeredText(font, "Keine Microsoft-Accounts registriert.", panelX + panelWidth / 2, cy + 30, EzUi.TEXT_MUTED);
            return;
        }

        graphics.enableScissor(cx, cy, cx + cw, cy + ch);

        for (int i = 0; i < accounts.size(); i++) {
            EzAccount acc = accounts.get(i);
            int itemY = (int) (cy + i * 42 - scrollOffset);
            if (itemY + 38 < cy || itemY > cy + ch) continue;

            boolean isActive = acc.getUuid().equalsIgnoreCase(activeUuid);
            boolean isHovered = mouseX >= cx && mouseX <= cx + cw && mouseY >= itemY && mouseY <= itemY + 38 && mouseY >= cy && mouseY <= cy + ch;

            int bg = isActive ? EzUi.BG_CARD_ACTIVE : (isHovered ? EzUi.BG_CARD_HOVER : EzUi.BG_CARD);
            int border = isActive ? EzUi.BORDER_ACTIVE : (isHovered ? EzUi.BORDER_HOVER : EzUi.BORDER_SUBTLE);

            // Card background & border
            EzUi.roundedRect(graphics, cx, itemY, cw, 38, 4, bg);
            graphics.fill(cx, itemY, cx + cw, itemY + 1, border);
            graphics.fill(cx, itemY + 37, cx + cw, itemY + 38, border);
            graphics.fill(cx, itemY + 1, cx + 1, itemY + 38, border);
            graphics.fill(cx + cw - 1, itemY, cx + cw, itemY + 38, border);

            // Player Avatar Face
            try {
                //? if >=26.2 {
                PlayerFaceExtractor.extractRenderState(
                        graphics,
                        ResolvableProfile.createUnresolved(acc.getUsername()),
                        cx + 6, itemY + 6, 26
                );
                //?} else {
                /*PlayerFaceExtractor.extractRenderState(
                        graphics,
                        minecraft.playerSkinRenderCache().getOrDefault(ResolvableProfile.createUnresolved(acc.getUsername())).playerSkin(),
                        cx + 6, itemY + 6, 26
                );
                *///?}
            } catch (Throwable ignored) {
                // Fallback avatar box
                EzUi.roundedRect(graphics, cx + 6, itemY + 6, 26, 26, 3, 0xFF2A3444);
            }

            // Username
            graphics.text(font, acc.getUsername(), cx + 38, itemY + 8, EzUi.TEXT_WHITE);

            // Subtitle
            if (isActive) {
                graphics.text(font, "● Aktiv (Klicken = Session erneuern)", cx + 38, itemY + 21, EzUi.ACCENT_EMERALD);
            } else {
                graphics.text(font, "Klicken zum Wechseln", cx + 38, itemY + 21, EzUi.TEXT_MUTED);
            }

            // Delete Button (right side)
            int delX = cx + cw - 22;
            int delY = itemY + 11;
            boolean delHovered = isHovered && mouseX >= delX && mouseX <= delX + 16 && mouseY >= delY && mouseY <= delY + 16;
            if (delHovered) {
                EzUi.roundedRect(graphics, delX - 2, delY - 2, 18, 18, 3, 0x55FF3333);
            }
            graphics.text(font, "x", delX + 4, delY + 2, delHovered ? 0xFFFF4D4D : 0xFF687588);
        }

        graphics.disableScissor();

        // Scrollbar
        if (maxScroll > 0) {
            int trackX = cx + cw + 4;
            int trackY = cy;
            int trackH = ch;
            EzUi.roundedRect(graphics, trackX, trackY, 3, trackH, 1, 0x331C2433);

            int thumbH = Math.max(14, (int) (trackH * (trackH / (float) (accounts.size() * 42))));
            int thumbY = (int) (trackY + (scrollOffset / maxScroll) * (trackH - thumbH));
            EzUi.roundedRect(graphics, trackX, thumbY, 3, thumbH, 1, EzUi.ACCENT_EMERALD);
        }
    }

    private void renderAddMsView(GuiGraphicsExtractor graphics) {
        graphics.text(font, "2. Weiterleitungs-URL oder Code nach dem Login einfügen:", panelX + 14, panelY + 62, EzUi.TEXT_LIGHT);
    }
}
