import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Rectangle {
    id: root
    height: 56
    color: EzTheme.titlebarBg

    property string currentRoute: "home"
    property var windowRef: null
    property var skinModalRef: null
    signal navigate(string route)

    readonly property bool hasProfile: typeof profileController !== "undefined" && profileController && profileController.activeName !== "No Profile" && profileController.activeName !== ""
    readonly property string activeName: typeof profileController !== "undefined" && profileController ? profileController.activeName : ""
    readonly property string activeVersion: typeof profileController !== "undefined" && profileController ? profileController.activeVersion : "26.2"
    readonly property string activeLoader: typeof profileController !== "undefined" && profileController ? profileController.activeLoader : "Fabric"

    readonly property string accountUser: typeof accountController !== "undefined" && accountController ? accountController.username : "Player"
    readonly property string avatarSource: typeof accountController !== "undefined" && accountController ? accountController.avatarUrl : ""

    function isTabActive(tabId) {
        if (tabId === "mods") {
            return root.currentRoute === "mods" || root.currentRoute === "modrinth" || root.currentRoute === "store"
        }
        if (tabId === "profiles") {
            return root.currentRoute === "profiles" || root.currentRoute === "profile_detail"
        }
        if (tabId === "installed_mods") {
            return root.currentRoute === "installed_mods" || root.currentRoute === "mods_installed"
        }
        return root.currentRoute === tabId
    }

    // Draggable background
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

    // ── LEFT: Logo + Active Profile Switcher ──
    RowLayout {
        id: leftSection
        anchors.left: parent.left
        anchors.leftMargin: 18
        anchors.verticalCenter: parent.verticalCenter
        spacing: 12

            // Logo mark
            RowLayout {
                spacing: 8
                Image {
                    source: "assets/logo.svg"
                    Layout.preferredWidth: 26
                    Layout.preferredHeight: 26
                    fillMode: Image.PreserveAspectFit
                    smooth: true
                }

                Text {
                    text: "EzClient"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 17
                    font.bold: true
                    color: EzTheme.text
                }
            }

            Rectangle { visible: root.width >= 1250; width: 1; height: 22; color: EzTheme.border; opacity: 0.5 }

            // Active Profile Quick-Switcher Dropdown Pill
            Rectangle {
                id: profilePill
                // Same breakpoint as the navigation labels: as soon as those
                // collapse to icons, this pill collapses to a dot icon too.
                readonly property bool compact: root.width < 1300
                visible: root.width >= 900
                z: 30
                height: 34
                width: profilePill.compact ? 34 : Math.min(200, Math.max(120, pillNameText.implicitWidth + 44))
                implicitWidth: width
                radius: 17
                color: profPopup.opened ? EzTheme.surface3 : (profMouse.containsMouse ? EzTheme.surfaceHover : EzTheme.surface2)

                Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }

                RowLayout {
                    id: profilePillRow
                    anchors.left: parent.left
                    anchors.right: parent.right
                    anchors.leftMargin: 12
                    anchors.rightMargin: 12
                    anchors.verticalCenter: parent.verticalCenter
                    spacing: 8

                    // Active indicator dot with pulse
                    Rectangle {
                        width: 8; height: 8; radius: 4; color: EzTheme.accent
                        Layout.alignment: Qt.AlignVCenter

                        SequentialAnimation on opacity {
                            loops: Animation.Infinite
                            NumberAnimation { to: 0.4; duration: 1200; easing.type: Easing.InOutSine }
                            NumberAnimation { to: 1.0; duration: 1200; easing.type: Easing.InOutSine }
                        }
                    }

                    Text {
                        id: pillNameText
                        visible: !profilePill.compact
                        text: root.hasProfile ? root.activeName : EzI18n.t("topbar_select_profile", "Profil wählen")
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        font.bold: true
                        color: EzTheme.text
                        Layout.fillWidth: true
                        elide: Text.ElideRight
                    }

                    Text {
                        visible: !profilePill.compact
                        text: "▾"
                        font.pixelSize: 9
                        color: profPopup.opened ? EzTheme.accent : EzTheme.textMuted
                        rotation: profPopup.opened ? 180 : 0
                        Behavior on rotation { NumberAnimation { duration: 200; easing.type: Easing.OutCubic } }
                        Layout.alignment: Qt.AlignVCenter
                    }
                }

                ToolTip.visible: profMouse.containsMouse && profilePill.compact && !profPopup.opened
                ToolTip.delay: 350
                ToolTip.text: root.hasProfile ? root.activeName : EzI18n.t("topbar_select_profile", "Profil wählen")

                MouseArea {
                    id: profMouse
                    anchors.fill: parent
                    z: 50
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: profPopup.opened ? profPopup.close() : profPopup.open()
                }

                // Profile Switcher Popup
                Popup {
                    id: profPopup
                    y: profilePill.height + 8
                    width: 380
                    height: Math.min((profileController && profileController.profileModel ? profileController.profileModel.rowCount() : 1) * 56 + 60, 400)
                    padding: 10
                    closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent

                    background: Rectangle {
                        radius: EzTheme.radius
                        color: EzTheme.surface
                        border.color: EzTheme.borderLight
                        border.width: 1
                    }

                    contentItem: ColumnLayout {
                        spacing: 4

                        ListView {
                            id: pList
                            Layout.fillWidth: true
                            Layout.fillHeight: true
                            clip: true
                            model: profileController ? profileController.profileModel : null
                            boundsBehavior: Flickable.StopAtBounds

                            delegate: Rectangle {
                                width: pList.width
                                height: 50
                                radius: EzTheme.radiusSm
                                color: (model.profileId === profileController.activeId)
                                       ? EzTheme.surfaceActive
                                       : (pItemMouse.containsMouse ? EzTheme.surface3 : "transparent")

                                Behavior on color { ColorAnimation { duration: EzTheme.animFast } }

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.leftMargin: 14
                                    anchors.rightMargin: 14
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

                                    Image {
                                        source: "icons/check.svg"
                                        width: 14
                                        height: 14
                                        fillMode: Image.PreserveAspectFit
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

                        Rectangle {
                            Layout.fillWidth: true
                            height: 36
                            radius: EzTheme.radiusSm
                            color: newProfMouse.containsMouse ? EzTheme.surface3 : "transparent"
                            Behavior on color { ColorAnimation { duration: EzTheme.animFast } }

                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 6
                                Text { text: "+"; font.family: EzTheme.fontFamily; font.pixelSize: 14; font.bold: true; color: EzTheme.accentLight }
                                Text { text: EzI18n.t("topbar_new_profile", "Neues Profil anlegen…"); font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.accentLight }
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

    // ── CENTER: Navigation Tabs with Sliding Indicator ──
    Item {
        id: centerSection
        anchors.centerIn: parent
        visible: root.width >= 900
        implicitWidth: navTabsRow.implicitWidth
        implicitHeight: navTabsRow.implicitHeight
        // Explicit size so anchors.centerIn aligns the real hitbox with the
        // visible tiles instead of centering a zero-sized item's edge.
        width: implicitWidth
        height: implicitHeight

        Row {
            id: navTabsRow
            spacing: 2

            Repeater {
                id: tabRepeater
                model: [
                    { id: "home",           labelKey: "nav_home",           fallback: "Home",             icon: "home.svg" },
                    { id: "profiles",       labelKey: "nav_profiles",       fallback: "Profile",          icon: "box.svg" },
                    { id: "installed_mods", labelKey: "nav_installed_mods", fallback: "Installierte Mods", icon: "mods.svg" },
                    { id: "mods",           labelKey: "nav_discover",       fallback: "Entdecken",        icon: "compass.svg" },
                    { id: "cape",           labelKey: "nav_cape",           fallback: "Capes",             icon: "logo.svg" },
                    { id: "settings",       labelKey: "nav_settings",       fallback: "Einstellungen",    icon: "settings.svg" }
                ]

                Rectangle {
                    id: tabItem
                    width: root.width < 1300 ? 42 : (tabRowInner.implicitWidth + 26)
                    height: 36
                    radius: 8
                    scale: tabMouse.pressed ? 0.95 : (tabMouse.containsMouse ? 1.04 : 1.0)
                    Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }

                    color: root.isTabActive(modelData.id)
                           ? EzTheme.surfaceActive
                           : (tabMouse.containsMouse ? EzTheme.surfaceHover : "transparent")

                    border.color: root.isTabActive(modelData.id) ? EzTheme.accent : (tabMouse.containsMouse ? EzTheme.borderLight : "transparent")
                    border.width: 1

                    Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }
                    Behavior on border.color { ColorAnimation { duration: EzTheme.animNormal } }

                    RowLayout {
                        id: tabRowInner
                        anchors.centerIn: parent
                        spacing: root.width < 1300 ? 5 : 7

                        Image {
                            source: "icons/" + modelData.icon
                            width: 14; height: 14
                            fillMode: Image.PreserveAspectFit
                            scale: tabMouse.containsMouse ? 1.15 : 1.0
                            Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutBack } }
                            opacity: root.isTabActive(modelData.id) ? 1.0 : (tabMouse.containsMouse ? 0.8 : 0.45)
                            Behavior on opacity { NumberAnimation { duration: EzTheme.animNormal } }
                        }

                        Text {
                            // Labels disappear early while shrinking; icons stay.
                            visible: root.width >= 1300
                            text: EzI18n.t(modelData.labelKey, modelData.fallback)
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: root.isTabActive(modelData.id)
                                   ? EzTheme.accentLight
                                   : (tabMouse.containsMouse ? EzTheme.text : EzTheme.textSecondary)
                            Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }
                        }
                    }

                    // Active Glowing Bottom Indicator
                    Rectangle {
                        height: 2
                        anchors.bottom: parent.bottom
                        anchors.bottomMargin: 2
                        anchors.horizontalCenter: parent.horizontalCenter
                        width: parent.width - 16
                        radius: 1
                        color: EzTheme.accent
                        visible: root.isTabActive(modelData.id)
                    }

                    MouseArea {
                        id: tabMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.navigate(modelData.id)
                    }

                    // Icon-only mode: explain the icon on hover.
                    ToolTip.visible: tabMouse.containsMouse && root.width < 1300
                    ToolTip.delay: 350
                    ToolTip.text: EzI18n.t(modelData.labelKey, modelData.fallback)
                }
            }
        }
    }

    // ── RIGHT: Account Pill + Window Controls ──
    // Window controls stay pinned to the edge; everything else flows to their left,
    // so the account pill can never sit on top of the minimize/maximize buttons.
    Row {
        id: winControls
        anchors.right: parent.right
        anchors.verticalCenter: parent.verticalCenter
        spacing: 0

                // Minimize
                Rectangle {
                    width: 46; height: root.height
                    color: minMouse.containsMouse ? EzTheme.surface3 : "transparent"
                    Behavior on color { ColorAnimation { duration: EzTheme.animFast } }
                    Text { text: "─"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: minMouse.containsMouse ? EzTheme.text : EzTheme.textMuted; anchors.centerIn: parent; Behavior on color { ColorAnimation { duration: EzTheme.animFast } } }
                    MouseArea { id: minMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor; onClicked: if (root.windowRef) root.windowRef.showMinimized() }
                }

                // Maximize
                Rectangle {
                    width: 46; height: root.height
                    color: maxMouse.containsMouse ? EzTheme.surface3 : "transparent"
                    Behavior on color { ColorAnimation { duration: EzTheme.animFast } }
                    Text { text: "□"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: maxMouse.containsMouse ? EzTheme.text : EzTheme.textMuted; anchors.centerIn: parent; Behavior on color { ColorAnimation { duration: EzTheme.animFast } } }
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
                    width: 48; height: root.height
                    color: closeMouse.containsMouse ? "#C42B1C" : "transparent"
                    Behavior on color { ColorAnimation { duration: EzTheme.animFast } }
                    Image { source: "icons/x.svg"; width: 12; height: 12; anchors.centerIn: parent; opacity: closeMouse.containsMouse ? 1.0 : 0.6; fillMode: Image.PreserveAspectFit }
                    MouseArea { id: closeMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor; onClicked: if (root.windowRef) root.windowRef.close() }
                }
            }

    RowLayout {
        id: rightSection
        anchors.right: winControls.left
        anchors.rightMargin: 20
        anchors.verticalCenter: parent.verticalCenter
        spacing: 36

            // Glowing Update Badge (EzClient Update)
            Rectangle {
                id: updateBadge
                height: 28
                visible: typeof profileController !== "undefined" && profileController && profileController.ezClientUpdateAvailable
                width: updateBadgeRow.implicitWidth + 18
                radius: 14
                color: updateMouse.containsMouse ? "#1c3829" : "#13281c"
                border.color: EzTheme.accent
                border.width: 1
                scale: updateMouse.pressed ? 0.96 : 1.0
                Behavior on scale { NumberAnimation { duration: 80 } }

                RowLayout {
                    id: updateBadgeRow
                    anchors.centerIn: parent
                    spacing: 6
                    Image { source: "icons/zap.svg"; width: 12; height: 12; fillMode: Image.PreserveAspectFit }
                    Text {
                        text: "Upgrade v" + (profileController ? profileController.ezClientLatestVersion : "1.8.0")
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 10
                        font.bold: true
                        color: EzTheme.accentLight
                    }
                }

                MouseArea {
                    id: updateMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: {
                        if (profileController) profileController.applyEzClientUpdates()
                    }
                }
            }

            // Player Avatar & Username
            Rectangle {
                id: accPill
                z: 30
                height: 34
                // The pill is guaranteed to display avatar and full username
                width: Math.max(120, Math.min(220, accRow.implicitWidth + 24))
                radius: 17
                color: accPopup.opened ? EzTheme.surface3 : (accMouse.containsMouse ? EzTheme.surfaceHover : EzTheme.surface2)

                Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }

                RowLayout {
                    id: accRow
                    anchors.centerIn: parent
                    spacing: 8

                    // Avatar head with glow ring
                    Item {
                        width: 26; height: 26

                        Rectangle {
                            anchors.fill: parent
                            radius: 13
                            color: EzTheme.surface3
                            clip: true

                            Image {
                                id: avatarImg
                                anchors.fill: parent
                                source: root.avatarSource
                                fillMode: Image.PreserveAspectCrop
                                cache: false
                                visible: status === Image.Ready
                            }
                            Text {
                                visible: avatarImg.status !== Image.Ready
                                text: root.accountUser ? root.accountUser.charAt(0).toUpperCase() : "P"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzTheme.accentLight
                                anchors.centerIn: parent
                            }
                        }
                    }

                    Text {
                        text: root.accountUser
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        font.bold: true
                        color: EzTheme.text
                        elide: Text.ElideRight
                        Layout.maximumWidth: 130
                    }

                }

                MouseArea {
                    id: accMouse
                    anchors.fill: parent
                    z: 50
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: accPopup.opened ? accPopup.close() : accPopup.open()
                }

                ToolTip.visible: accMouse.containsMouse && root.width < 1100 && !accPopup.opened
                ToolTip.delay: 350
                ToolTip.text: "Konto · " + root.accountUser

                // Account Management Popup
                Popup {
                    id: accPopup
                    y: accPill.height + 8
                    x: Math.round(accPill.width - width)
                    width: 300
                    padding: 14
                    closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent

                    background: Rectangle {
                        radius: EzTheme.radius
                        color: EzTheme.surface
                        border.color: EzTheme.borderLight
                        border.width: 1
                    }

                    contentItem: ColumnLayout {
                        spacing: 10

                        RowLayout {
                            spacing: 12
                            Rectangle {
                                width: 40; height: 40; radius: 20
                                color: EzTheme.surface3
                                clip: true

                                Image {
                                    anchors.fill: parent
                                    source: root.avatarSource
                                    fillMode: Image.PreserveAspectCrop
                                    cache: false
                                }
                            }

                            ColumnLayout {
                                spacing: 2
                                Layout.fillWidth: true
                                Text {
                                    text: root.accountUser
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 14
                                    font.bold: true
                                    color: EzTheme.text
                                }
                                Text {
                                    text: (typeof accountController !== "undefined" && accountController && accountController.isOnline)
                                          ? EzI18n.t("topbar_account_auth_online", "Microsoft Auth (Online)")
                                          : EzI18n.t("topbar_account_auth_offline", "Offline / Lokales Profil")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    color: (typeof accountController !== "undefined" && accountController && accountController.isOnline) ? EzTheme.accentLight : EzTheme.textMuted
                                }
                            }
                        }

                        Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                        // Microsoft Login
                        Rectangle {
                            Layout.fillWidth: true
                            height: 38
                            radius: EzTheme.radiusSm
                            color: loginBtnMouse.containsMouse ? EzTheme.surfaceActive : EzTheme.surface2
                            border.color: loginBtnMouse.containsMouse ? EzTheme.accent : EzTheme.border
                            border.width: 1
                            Behavior on color { ColorAnimation { duration: EzTheme.animFast } }

                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 8
                                Image { source: "icons/user.svg"; width: 14; height: 14; fillMode: Image.PreserveAspectFit }
                                Text {
                                    text: EzI18n.t("topbar_login_btn", "Microsoft Konto anmelden")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 12
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

                        // Skin Changer Action
                        Rectangle {
                            Layout.fillWidth: true
                            height: 34
                            radius: EzTheme.radiusSm
                            color: skinBtnMouse.containsMouse ? EzTheme.surface3 : "transparent"
                            Behavior on color { ColorAnimation { duration: EzTheme.animFast } }

                            RowLayout {
                                anchors.left: parent.left; anchors.leftMargin: 10
                                anchors.verticalCenter: parent.verticalCenter
                                spacing: 8
                                Image { source: "icons/user.svg"; width: 14; height: 14; fillMode: Image.PreserveAspectFit }
                                Text {
                                    text: "Skin ändern (Mojang API)"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: true
                                    color: EzTheme.accentLight
                                }
                            }

                            MouseArea {
                                id: skinBtnMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    accPopup.close()
                                    if (typeof skinModalRef !== "undefined" && skinModalRef) {
                                        skinModalRef.open()
                                    }
                                }
                            }
                        }

                        // Refresh Session
                        Rectangle {
                            Layout.fillWidth: true
                            height: 34
                            radius: EzTheme.radiusSm
                            color: refBtnMouse.containsMouse ? EzTheme.surface3 : "transparent"
                            Behavior on color { ColorAnimation { duration: EzTheme.animFast } }

                            RowLayout {
                                anchors.left: parent.left; anchors.leftMargin: 10
                                anchors.verticalCenter: parent.verticalCenter
                                spacing: 8
                                Image { source: "icons/refresh-cw.svg"; width: 14; height: 14; fillMode: Image.PreserveAspectFit }
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

                        // Logout
                        Rectangle {
                            Layout.fillWidth: true
                            height: 34
                            radius: EzTheme.radiusSm
                            color: logoutBtnMouse.containsMouse ? "#3B1119" : "transparent"
                            Behavior on color { ColorAnimation { duration: EzTheme.animFast } }

                            RowLayout {
                                anchors.left: parent.left; anchors.leftMargin: 10
                                anchors.verticalCenter: parent.verticalCenter
                                spacing: 8
                                Image { source: "icons/x.svg"; width: 14; height: 14; fillMode: Image.PreserveAspectFit }
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

        }
    }
