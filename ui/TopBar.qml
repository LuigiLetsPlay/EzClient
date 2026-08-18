import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Rectangle {
    id: root
    height: 52
    color: EzTheme.titlebarBg

    property string currentRoute: "home"
    property var windowRef: null
    signal navigate(string route)

    readonly property bool hasProfile: typeof profileController !== "undefined" && profileController && profileController.activeName !== "No Profile" && profileController.activeName !== ""
    readonly property string activeName: typeof profileController !== "undefined" && profileController ? profileController.activeName : ""
    readonly property string activeVersion: typeof profileController !== "undefined" && profileController ? profileController.activeVersion : "1.21.4"
    readonly property string activeLoader: typeof profileController !== "undefined" && profileController ? profileController.activeLoader : "Fabric"

    readonly property string accountUser: typeof accountController !== "undefined" && accountController ? accountController.username : "Player"
    readonly property string avatarSource: typeof accountController !== "undefined" && accountController ? accountController.avatarUrl : ""

    // 1px bottom border
    Rectangle {
        anchors { bottom: parent.bottom; left: parent.left; right: parent.right }
        height: 1
        color: EzTheme.border
    }

    // Draggable background (behind interactive items, z: -1)
    MouseArea {
        anchors.fill: parent
        z: -1
        onPressed: if (root.windowRef) root.windowRef.startSystemMove()
        onDoubleClicked: {
            if (!root.windowRef) return
            root.windowRef.visibility === Window.Maximized
                ? root.windowRef.showNormal()
                : root.windowRef.showMaximized()
        }
    }

    RowLayout {
        anchors.fill: parent
        anchors.leftMargin: 16
        anchors.rightMargin: 0
        spacing: 12

        // ── LEFT: Logo + Active Profile Switcher ──
        RowLayout {
            spacing: 10

            // Logo mark
            RowLayout {
                spacing: 8
                Image {
                    source: "assets/logo.svg"
                    Layout.preferredWidth: 24
                    Layout.preferredHeight: 24
                    fillMode: Image.PreserveAspectFit
                    smooth: true
                }

                Text {
                    text: "EzClient"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 16
                    font.bold: true
                    color: EzTheme.text
                }
            }

            Rectangle { width: 1; height: 20; color: EzTheme.border }

            // Active Profile Quick-Switcher Dropdown Pill (Expands dynamically to fit full name)
            Rectangle {
                id: profilePill
                height: 32
                width: Math.max(130, pillNameText.implicitWidth + 44)
                radius: EzTheme.radiusSm
                color: profMouse.containsMouse || profPopup.opened ? EzTheme.surface3 : EzTheme.surface2
                border.color: profPopup.opened ? EzTheme.accent : (profMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                border.width: 1

                Behavior on color { ColorAnimation { duration: 100 } }
                Behavior on border.color { ColorAnimation { duration: 100 } }

                RowLayout {
                    id: profilePillRow
                    anchors.left: parent.left
                    anchors.right: parent.right
                    anchors.leftMargin: 10
                    anchors.rightMargin: 10
                    anchors.verticalCenter: parent.verticalCenter
                    spacing: 8

                    Rectangle {
                        width: 7; height: 7; radius: 3.5; color: EzTheme.accent
                        Layout.alignment: Qt.AlignVCenter
                    }

                    Text {
                        id: pillNameText
                        text: root.hasProfile ? root.activeName : EzI18n.t("topbar_select_profile", "Profil wählen")
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 13
                        font.bold: true
                        color: EzTheme.text
                        Layout.fillWidth: true
                    }

                    Text {
                        text: "▾"
                        font.pixelSize: 10
                        color: profPopup.opened ? EzTheme.accent : EzTheme.textMuted
                        rotation: profPopup.opened ? 180 : 0
                        Behavior on rotation { NumberAnimation { duration: 150 } }
                        Layout.alignment: Qt.AlignVCenter
                    }
                }

                MouseArea {
                    id: profMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: profPopup.opened ? profPopup.close() : profPopup.open()
                }

                // Profile Switcher Popup (Roomy 360px)
                Popup {
                    id: profPopup
                    y: profilePill.height + 6
                    width: 360
                    height: Math.min((profileController && profileController.profileModel ? profileController.profileModel.rowCount() : 1) * 52 + 56, 380)
                    padding: 8
                    closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent

                    background: Rectangle {
                        radius: EzTheme.radius
                        color: EzTheme.surface2
                        border.color: EzTheme.borderLight
                        border.width: 1
                    }

                    contentItem: ColumnLayout {
                        spacing: 4

                        // Profiles list
                        ListView {
                            id: pList
                            Layout.fillWidth: true
                            Layout.fillHeight: true
                            clip: true
                            model: profileController ? profileController.profileModel : null
                            boundsBehavior: Flickable.StopAtBounds

                            delegate: Rectangle {
                                width: pList.width
                                height: 46
                                radius: 6
                                color: (model.profileId === profileController.activeId)
                                       ? EzTheme.surfaceActive
                                       : (pItemMouse.containsMouse ? EzTheme.surface3 : "transparent")

                                Behavior on color { ColorAnimation { duration: 80 } }

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.leftMargin: 12
                                    anchors.rightMargin: 12
                                    spacing: 10

                                    Rectangle {
                                        width: 8; height: 8; radius: 4
                                        color: EzTheme.accent
                                        visible: model.profileId === profileController.activeId
                                    }

                                    ColumnLayout {
                                        Layout.fillWidth: true
                                        spacing: 2
                                        Text {
                                            text: model.profileName
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 13
                                            font.bold: true
                                            color: model.profileId === profileController.activeId ? EzTheme.accentLight : EzTheme.text
                                            elide: Text.ElideRight
                                            Layout.fillWidth: true
                                        }
                                        Text {
                                            text: "Minecraft " + model.minecraftVersion + " · " + model.loader + " · " + model.modsCount + " Mods"
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 10
                                            color: EzTheme.textMuted
                                        }
                                    }

                                    Text {
                                        text: "✓"
                                        font.bold: true
                                        font.pixelSize: 12
                                        color: EzTheme.accentLight
                                        visible: model.profileId === profileController.activeId
                                    }
                                }

                                MouseArea {
                                    id: pItemMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        profileController.selectProfile(model.profileId)
                                        profPopup.close()
                                    }
                                }
                            }
                        }

                        Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                        // Create New Profile Button in popup
                        Rectangle {
                            Layout.fillWidth: true
                            height: 32
                            radius: 4
                            color: newProfMouse.containsMouse ? EzTheme.surface3 : "transparent"

                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 6
                                Text { text: "+"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.accentLight }
                                Text { text: EzI18n.t("topbar_new_profile", "Neues Profil anlegen…"); font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.accentLight }
                            }

                            MouseArea {
                                id: newProfMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    profPopup.close()
                                    root.navigate("profiles")
                                }
                            }
                        }
                    }
                }
            }
        }

        Item { Layout.fillWidth: true }

        // ── CENTER: Top Navigation Tabs ──
        Row {
            Layout.alignment: Qt.AlignHCenter
            spacing: 4

            Repeater {
                model: [
                    { id: "home",           labelKey: "nav_home",           fallback: "Home",          icon: "home.svg" },
                    { id: "profiles",       labelKey: "nav_profiles",       fallback: "Profile",       icon: "box.svg" },
                    { id: "installed_mods", labelKey: "nav_installed_mods", fallback: "Mods",          icon: "mods.svg" },
                    { id: "mods",           labelKey: "nav_modrinth",       fallback: "Modrinth",      icon: "modrinth.svg" },
                    { id: "settings",       labelKey: "nav_settings",       fallback: "Einstellungen", icon: "settings.svg" }
                ]

                Rectangle {
                    width: tabRow.implicitWidth + 24
                    height: 34
                    radius: EzTheme.radiusSm
                    color: root.currentRoute === modelData.id
                           ? EzTheme.surfaceActive
                           : (tabMouse.containsMouse ? EzTheme.surface2 : "transparent")
                    border.color: root.currentRoute === modelData.id ? EzTheme.borderAccent : "transparent"
                    border.width: 1

                    Behavior on color { ColorAnimation { duration: 100 } }

                    RowLayout {
                        id: tabRow
                        anchors.centerIn: parent
                        spacing: 7

                        Image {
                            source: "icons/" + modelData.icon
                            width: 13; height: 13
                            fillMode: Image.PreserveAspectFit
                            opacity: root.currentRoute === modelData.id ? 1.0 : (tabMouse.containsMouse ? 0.8 : 0.45)
                            Behavior on opacity { NumberAnimation { duration: 100 } }
                        }

                        Text {
                            text: EzI18n.t(modelData.labelKey, modelData.fallback)
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: root.currentRoute === modelData.id
                                   ? EzTheme.accentLight
                                   : (tabMouse.containsMouse ? EzTheme.text : EzTheme.textSecondary)
                            Behavior on color { ColorAnimation { duration: 100 } }
                        }
                    }

                    // Active bottom bar glow
                    Rectangle {
                        anchors { bottom: parent.bottom; horizontalCenter: parent.horizontalCenter }
                        width: parent.width - 16
                        height: 2
                        radius: 1
                        color: EzTheme.accent
                        visible: root.currentRoute === modelData.id
                    }

                    MouseArea {
                        id: tabMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.navigate(modelData.id)
                    }
                }
            }
        }

        Item { Layout.fillWidth: true }

        // ── RIGHT: Gamer Profile Deco + Window Controls ──
        RowLayout {
            spacing: 10

            // Player Avatar & Username (Interactive Account Switcher / Login)
            Rectangle {
                id: accPill
                height: 32
                width: accRow.implicitWidth + 24
                radius: 16
                color: accMouse.containsMouse || accPopup.opened ? EzTheme.surface3 : EzTheme.surface2
                border.color: accPopup.opened ? EzTheme.accent : (accMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                border.width: 1

                Behavior on color { ColorAnimation { duration: 100 } }
                Behavior on border.color { ColorAnimation { duration: 100 } }

                RowLayout {
                    id: accRow
                    anchors.centerIn: parent
                    spacing: 7

                    // Avatar head
                    Rectangle {
                        width: 20; height: 20; radius: 10
                        color: EzTheme.surface3
                        clip: true

                        Image {
                            id: avatarImg
                            anchors.fill: parent
                            source: root.avatarSource
                            fillMode: Image.PreserveAspectCrop
                            visible: status === Image.Ready
                        }
                        Text {
                            visible: avatarImg.status !== Image.Ready
                            text: root.accountUser ? root.accountUser.charAt(0).toUpperCase() : "P"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            font.bold: true
                            color: EzTheme.accentLight
                            anchors.centerIn: parent
                        }
                    }

                    Text {
                        text: root.accountUser
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        font.bold: true
                        color: EzTheme.text
                    }

                    Rectangle {
                        width: 6; height: 6; radius: 3
                        color: (typeof accountController !== "undefined" && accountController && accountController.isOnline) ? EzTheme.accent : EzTheme.textMuted
                    }
                }

                MouseArea {
                    id: accMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: accPopup.opened ? accPopup.close() : accPopup.open()
                }

                // Account Management Popup
                Popup {
                    id: accPopup
                    y: accPill.height + 6
                    x: Math.round(accPill.width - width)
                    width: 280
                    padding: 12
                    closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent

                    background: Rectangle {
                        radius: EzTheme.radius
                        color: "#13171F"
                        border.color: EzTheme.borderLight
                        border.width: 1
                    }

                    contentItem: ColumnLayout {
                        spacing: 10

                        RowLayout {
                            spacing: 10
                            Rectangle {
                                width: 36; height: 36; radius: 18
                                color: EzTheme.surface3
                                clip: true

                                Image {
                                    anchors.fill: parent
                                    source: root.avatarSource
                                    fillMode: Image.PreserveAspectCrop
                                }
                            }

                            ColumnLayout {
                                spacing: 1
                                Layout.fillWidth: true
                                Text {
                                    text: root.accountUser
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 13
                                    font.bold: true
                                    color: EzTheme.text
                                }
                                Text {
                                    text: (typeof accountController !== "undefined" && accountController && accountController.isOnline)
                                          ? EzI18n.t("topbar_account_auth_online", "🟢 Microsoft Auth (Online)")
                                          : EzI18n.t("topbar_account_auth_offline", "⚪ Offline / Lokales Profil")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    color: (typeof accountController !== "undefined" && accountController && accountController.isOnline) ? EzTheme.accentLight : EzTheme.textMuted
                                }
                            }
                        }

                        Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                        // Microsoft Login / Switch Button
                        Rectangle {
                            Layout.fillWidth: true
                            height: 34
                            radius: 6
                            color: loginBtnMouse.containsMouse ? EzTheme.surfaceActive : EzTheme.surface2
                            border.color: loginBtnMouse.containsMouse ? EzTheme.accent : EzTheme.border
                            border.width: 1

                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 8
                                Text { text: "🔑"; font.pixelSize: 12 }
                                Text {
                                    text: EzI18n.t("topbar_login_btn", "Microsoft Konto anmelden")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: true
                                    color: EzTheme.accentLight
                                }
                            }

                            MouseArea {
                                id: loginBtnMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    accPopup.close()
                                    if (typeof accountController !== "undefined" && accountController) {
                                        accountController.openLoginDialog()
                                    }
                                }
                            }
                        }

                        // Refresh Session Button
                        Rectangle {
                            Layout.fillWidth: true
                            height: 30
                            radius: 6
                            color: refBtnMouse.containsMouse ? EzTheme.surface3 : "transparent"

                            RowLayout {
                                anchors.left: parent.left
                                anchors.leftMargin: 8
                                anchors.verticalCenter: parent.verticalCenter
                                spacing: 8
                                Text { text: "🔄"; font.pixelSize: 11 }
                                Text {
                                    text: EzI18n.t("topbar_resync_btn", "Token neu synchronisieren")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    color: EzTheme.textSecondary
                                }
                            }

                            MouseArea {
                                id: refBtnMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    accPopup.close()
                                    if (typeof accountController !== "undefined" && accountController) {
                                        accountController.refresh()
                                    }
                                }
                            }
                        }

                        // Logout Button
                        Rectangle {
                            Layout.fillWidth: true
                            height: 30
                            radius: 6
                            color: logoutBtnMouse.containsMouse ? "#3B1119" : "transparent"

                            RowLayout {
                                anchors.left: parent.left
                                anchors.leftMargin: 8
                                anchors.verticalCenter: parent.verticalCenter
                                spacing: 8
                                Text { text: "🚪"; font.pixelSize: 11 }
                                Text {
                                    text: EzI18n.t("topbar_logout_btn", "Konto abmelden")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    color: EzTheme.danger
                                }
                            }

                            MouseArea {
                                id: logoutBtnMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    accPopup.close()
                                    if (typeof accountController !== "undefined" && accountController) {
                                        accountController.logout()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Window Controls
            Row {
                // Minimize
                Rectangle {
                    width: 44; height: root.height
                    color: minMouse.containsMouse ? EzTheme.surface3 : "transparent"
                    Behavior on color { ColorAnimation { duration: 80 } }
                    Text { text: "─"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; anchors.centerIn: parent }
                    MouseArea { id: minMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor; onClicked: if (root.windowRef) root.windowRef.showMinimized() }
                }

                // Maximize
                Rectangle {
                    width: 44; height: root.height
                    color: maxMouse.containsMouse ? EzTheme.surface3 : "transparent"
                    Behavior on color { ColorAnimation { duration: 80 } }
                    Text { text: "□"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; anchors.centerIn: parent }
                    MouseArea {
                        id: maxMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor
                        onClicked: {
                            if (!root.windowRef) return
                            root.windowRef.visibility === Window.Maximized ? root.windowRef.showNormal() : root.windowRef.showMaximized()
                        }
                    }
                }

                // Close
                Rectangle {
                    width: 46; height: root.height
                    color: closeMouse.containsMouse ? "#C42B1C" : "transparent"
                    Behavior on color { ColorAnimation { duration: 80 } }
                    Text { text: "✕"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: closeMouse.containsMouse ? "#ffffff" : EzTheme.textMuted; anchors.centerIn: parent; Behavior on color { ColorAnimation { duration: 80 } } }
                    MouseArea { id: closeMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor; onClicked: if (root.windowRef) root.windowRef.close() }
                }
            }
        }
    }
}
