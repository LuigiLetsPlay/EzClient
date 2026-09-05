import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Rectangle {
    id: root
    height: 58
    color: EzTheme.titlebarBg
    property string currentRoute: "home"
    property var windowRef: null
    property var skinModalRef: null
    signal navigate(string route)
    readonly property string accountUser: typeof accountController !== "undefined" && accountController ? accountController.username : "Player"
    readonly property string avatarSource: typeof accountController !== "undefined" && accountController ? accountController.avatarUrl : ""
    readonly property bool accountOnline: typeof accountController !== "undefined" && accountController && accountController.isOnline
    readonly property bool hasAccount: typeof accountController !== "undefined" && accountController && accountController.hasAccount
    readonly property var routeLabels: ({ home: "Spielen", versions: "Minecraft-Versionen", profiles: "Meine Profile", profile_detail: "Profil verwalten", installed_mods: "Meine Erweiterungen", mods_installed: "Meine Erweiterungen", mods: "Erweiterungs-Bibliothek", modrinth: "Erweiterungs-Bibliothek", store: "Erweiterungs-Bibliothek", resourcepack_library: "Resource Packs", cape: "Meine Capes", cape_editor: "Cape gestalten", settings: "Einstellungen" })
    Rectangle { anchors.bottom: parent.bottom; width: parent.width; height: 1; color: EzTheme.border }
    MouseArea { anchors.fill: parent; z: 0; onPressed: if (root.windowRef) root.windowRef.startSystemMove(); onDoubleClicked: { if (root.windowRef) root.windowRef.visibility === Window.Maximized ? root.windowRef.showNormal() : root.windowRef.showMaximized() } }

    RowLayout {
        anchors.fill: parent; anchors.leftMargin: 20; spacing: 11; z: 1
        ColumnLayout {
            spacing: 0
            Text { text: root.routeLabels[root.currentRoute] || "EzClient"; font.family: EzTheme.fontFamily; font.pixelSize: 14; font.bold: true; color: EzTheme.text }
            Text { text: root.currentRoute === "home" ? "Dein Spiel ist nur einen Klick entfernt" : "EzClient Launcher"; font.family: EzTheme.fontFamily; font.pixelSize: 9; color: EzTheme.textMuted }
        }
        Item { Layout.fillWidth: true }

        // Active Profile Quick-Switcher Pill
        Rectangle {
            id: profilePill
            visible: typeof profileController !== "undefined" && profileController && profileController.activeName !== "" && profileController.activeName !== "No Profile"
            Layout.preferredWidth: Math.min(220, Math.max(120, pillNameText.implicitWidth + 48))
            Layout.preferredHeight: 38
            radius: 11
            color: profPopup.opened ? EzTheme.surface3 : (profMouse.containsMouse ? EzTheme.surfaceHover : EzTheme.surface2)
            border.width: 1
            border.color: profMouse.containsMouse ? EzTheme.borderLight : EzTheme.border

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 11
                anchors.rightMargin: 11
                spacing: 7

                ProfileIcon {
                    Layout.preferredWidth: 20
                    Layout.preferredHeight: 20
                    radius: 5
                    iconNameOrPath: profileController ? profileController.activeIcon : ""
                    fallbackName: profileController ? profileController.activeName : "EZ"
                    fontSize: 9
                }

                Text {
                    id: pillNameText
                    text: profileController ? profileController.activeName : ""
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    font.bold: true
                    color: EzTheme.text
                    Layout.fillWidth: true
                    elide: Text.ElideRight
                }

                Text {
                    text: "▾"
                    font.pixelSize: 9
                    color: profPopup.opened ? EzTheme.accent : EzTheme.textMuted
                }
            }

            MouseArea {
                id: profMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: profPopup.opened ? profPopup.close() : profPopup.open()
            }

            Popup {
                id: profPopup
                y: profilePill.height + 7
                width: 320
                height: Math.min((profileController && profileController.profileModel ? profileController.profileModel.rowCount() : 1) * 52 + 30, 360)
                padding: 8
                closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent
                background: Rectangle { radius: 12; color: EzTheme.surface; border.width: 1; border.color: EzTheme.borderLight }

                contentItem: ListView {
                    id: pList
                    clip: true
                    model: profileController ? profileController.profileModel : null
                    boundsBehavior: Flickable.StopAtBounds

                    delegate: Rectangle {
                        width: pList.width
                        height: 46
                        radius: 8
                        color: (model.profileId === profileController.activeId) ? EzTheme.surfaceActive : (pItemMouse.containsMouse ? EzTheme.surface3 : "transparent")
                        RowLayout {
                            anchors.fill: parent
                            anchors.leftMargin: 10
                            anchors.rightMargin: 10
                            spacing: 8
                            Rectangle { width: 6; height: 6; radius: 3; color: EzTheme.accent; visible: model.profileId === profileController.activeId }
                            ProfileIcon {
                                Layout.preferredWidth: 26
                                Layout.preferredHeight: 26
                                radius: 6
                                iconNameOrPath: model.icon || ""
                                fallbackName: model.profileName || "EZ"
                                fontSize: 10
                            }
                            ColumnLayout {
                                Layout.fillWidth: true
                                spacing: 1
                                Text { text: model.profileName; font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: model.profileId === profileController.activeId ? EzTheme.accentLight : EzTheme.text; elide: Text.ElideRight; Layout.fillWidth: true }
                                Text { text: "Minecraft " + model.minecraftVersion + " · " + model.loader; font.pixelSize: 9; color: EzTheme.textMuted }
                            }
                        }
                        MouseArea {
                            id: pItemMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                profileController.switchProfile(model.profileId)
                                profPopup.close()
                            }
                        }
                    }
                }
            }
        }

        Rectangle {
            id: accountPill
            Layout.preferredWidth: Math.min(210, Math.max(124, accountRow.implicitWidth + 28)); Layout.preferredHeight: 38
            radius: 11; color: accountMouse.containsMouse ? EzTheme.surfaceHover : EzTheme.surface2
            border.width: 1; border.color: accountMouse.containsMouse ? EzTheme.borderLight : EzTheme.border
            RowLayout {
                id: accountRow; anchors.centerIn: parent; spacing: 7
                Rectangle {
                    width: 28; height: 28; radius: 9; color: EzTheme.surface3; clip: true
                    Image { id: avatar; anchors.fill: parent; source: root.avatarSource; fillMode: Image.PreserveAspectCrop; visible: status === Image.Ready }
                    Text { visible: avatar.status !== Image.Ready; anchors.centerIn: parent; text: root.accountUser ? root.accountUser.charAt(0).toUpperCase() : "P"; font.bold: true; font.pixelSize: 10; color: EzTheme.accentLight }
                    Rectangle { anchors.right: parent.right; anchors.bottom: parent.bottom; width: 6; height: 6; radius: 3; color: root.accountOnline ? EzTheme.accent : EzTheme.textMuted; border.width: 1; border.color: EzTheme.surface2 }
                }
                ColumnLayout {
                    spacing: 0
                    Text { text: root.accountUser; font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.text; elide: Text.ElideRight; Layout.maximumWidth: 120 }
                    Text { text: root.accountOnline ? "Online" : "Offline"; font.family: EzTheme.fontFamily; font.pixelSize: 8; color: root.accountOnline ? EzTheme.accentLight : EzTheme.textMuted }
                }
            }
            MouseArea { id: accountMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: accountPopup.opened ? accountPopup.close() : accountPopup.open() }
            Popup {
                id: accountPopup
                x: accountPill.width - width; y: accountPill.height + 7; width: 280; padding: 10
                closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent
                background: Rectangle { radius: 12; color: EzTheme.surface; border.width: 1; border.color: EzTheme.borderLight }
                contentItem: ColumnLayout {
                    spacing: 5
                    Text { text: root.accountUser; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                    Text { text: root.accountOnline ? "Microsoft-Konto verbunden" : "Offline-Profil"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: root.accountOnline ? EzTheme.accentLight : EzTheme.textMuted }
                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                    Repeater {
                        model: (typeof accountController !== "undefined" && accountController) ? accountController.accounts : []
                        Rectangle {
                            required property var modelData
                            Layout.fillWidth: true; height: 42; radius: 8
                            color: modelData.active ? EzTheme.surfaceActive : (savedAccountMouse.containsMouse ? EzTheme.surfaceHover : "transparent")
                            border.width: 1; border.color: modelData.active ? EzTheme.accent : "transparent"
                            MouseArea { id: savedAccountMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: if (!modelData.active) accountController.switchAccount(modelData.uuid) }
                            RowLayout {
                                anchors.fill: parent; anchors.leftMargin: 8; anchors.rightMargin: 6; spacing: 8
                                Rectangle { width: 28; height: 28; radius: 8; color: EzTheme.surface3; clip: true; Image { anchors.fill: parent; source: modelData.avatarUrl; fillMode: Image.PreserveAspectCrop } }
                                ColumnLayout {
                                    Layout.fillWidth: true; spacing: 0
                                    Text { text: modelData.username; font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.text; elide: Text.ElideRight; Layout.fillWidth: true }
                                    Text { text: modelData.active ? "Aktiver Account" : "Account auswählen"; font.pixelSize: 9; color: modelData.active ? EzTheme.accentLight : EzTheme.textMuted }
                                }
                                Rectangle {
                                    width: 25; height: 25; radius: 6; color: removeSavedMouse.containsMouse ? "#3B1119" : "transparent"
                                    Text { anchors.centerIn: parent; text: "×"; font.pixelSize: 16; color: EzTheme.danger }
                                    MouseArea { id: removeSavedMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: accountController.removeAccount(modelData.uuid) }
                                }
                            }
                        }
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }
                    Repeater {
                        model: [
                            { label: "+  Account hinzufügen", action: "login", danger: false },
                            { label: "Skin ändern", action: "skin", danger: false },
                            { label: "Kontoeinstellungen", action: "settings", danger: false },
                            { label: "Aktiven Account abmelden", action: "logout", danger: true }
                        ]
                        Rectangle {
                            Layout.fillWidth: true; height: 34; radius: 7; color: accountActionMouse.containsMouse ? (modelData.danger ? "#3B1119" : EzTheme.surfaceHover) : "transparent"
                            Text { anchors.left: parent.left; anchors.leftMargin: 10; anchors.verticalCenter: parent.verticalCenter; text: modelData.label; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: modelData.danger ? EzTheme.danger : EzTheme.textSecondary }
                            MouseArea {
                                id: accountActionMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    accountPopup.close()
                                    if (modelData.action === "login" && accountController) accountController.openLoginDialog()
                                    else if (modelData.action === "skin" && root.skinModalRef) root.skinModalRef.open()
                                    else if (modelData.action === "settings") root.navigate("settings")
                                    else if (modelData.action === "logout" && accountController) accountController.logout()
                                }
                            }
                        }
                    }
                }
            }
        }
        Row {
            Layout.fillHeight: true
            Repeater {
                model: [ { glyph: "−", action: "min" }, { glyph: "□", action: "max" }, { glyph: "×", action: "close" } ]
                Rectangle {
                    width: 46; height: root.height; color: controlMouse.containsMouse ? (modelData.action === "close" ? "#C42B1C" : EzTheme.surfaceHover) : "transparent"
                    Text { anchors.centerIn: parent; text: modelData.glyph; font.family: "Segoe UI"; font.pixelSize: modelData.action === "close" ? 20 : 15; color: controlMouse.containsMouse ? EzTheme.text : EzTheme.textSecondary }
                    MouseArea { id: controlMouse; anchors.fill: parent; hoverEnabled: true; onClicked: { if (!root.windowRef) return; if (modelData.action === "min") root.windowRef.showMinimized(); else if (modelData.action === "max") root.windowRef.visibility === Window.Maximized ? root.windowRef.showNormal() : root.windowRef.showMaximized(); else { if (typeof root.windowRef.handleClose === "function") root.windowRef.handleClose(); else root.windowRef.close(); } } }
                }
            }
        }
    }
}
