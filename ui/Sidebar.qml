import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Rectangle {
    id: root
    color: EzTheme.sidebarBg
    width: EzTheme.sidebarWidth

    property string currentRoute: "home"
    signal navigate(string route)

    readonly property bool isLoggedIn: typeof accountController !== "undefined" && accountController && accountController.username !== "Player" && accountController.username !== ""
    readonly property string accountUser: typeof accountController !== "undefined" && accountController ? accountController.username : ""
    readonly property string accountTypeName: typeof accountController !== "undefined" && accountController ? accountController.accountType : ""

    // 1px right border
    Rectangle {
        width: 1
        anchors { right: parent.right; top: parent.top; bottom: parent.bottom }
        color: EzTheme.border
    }

    ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // ── BRAND HEADER ──
        Rectangle {
            Layout.fillWidth: true
            height: 52
            color: "transparent"

            RowLayout {
                anchors { fill: parent; leftMargin: 16; rightMargin: 12 }
                spacing: 10

                Rectangle {
                    width: 26
                    height: 26
                    radius: 7
                    gradient: Gradient {
                        orientation: Gradient.Horizontal
                        GradientStop { position: 0.0; color: EzTheme.accent }
                        GradientStop { position: 1.0; color: "#2BE88A" }
                    }
                    Text {
                        text: "E"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 14
                        font.bold: true
                        color: "#000000"
                        anchors.centerIn: parent
                    }
                }

                Text {
                    text: "EzClient"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 14
                    font.bold: true
                    color: EzTheme.text
                    Layout.fillWidth: true
                }
            }
        }

        Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }
        Item { height: 8 }

        // ── EZCLIENT section ──
        Text {
            text: EzI18n.t("sidebar_ezclient", "EZCLIENT")
            font.family: EzTheme.mcFontFamily
            font.pixelSize: 9
            font.bold: true
            color: EzTheme.textSubtle
            font.letterSpacing: 0.8
            Layout.leftMargin: 16
            Layout.bottomMargin: 3
        }

        // HOME
        Rectangle {
            id: homeItem
            Layout.fillWidth: true
            Layout.leftMargin: 8
            Layout.rightMargin: 8
            height: 34
            radius: 6
            color: root.currentRoute === "home" ? EzTheme.surfaceActive : (homeMouse.containsMouse ? EzTheme.surface2 : "transparent")
            Behavior on color { ColorAnimation { duration: 110 } }
            Rectangle {
                width: 3
                height: 18
                radius: 2
                color: EzTheme.accent
                visible: root.currentRoute === "home"
                anchors.left: parent.left
                anchors.leftMargin: -4
                anchors.verticalCenter: parent.verticalCenter
            }
            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 12
                anchors.rightMargin: 10
                spacing: 9
                Image {
                    source: "icons/home.svg"
                    Layout.preferredWidth: 15
                    Layout.preferredHeight: 15
                    fillMode: Image.PreserveAspectFit
                    opacity: root.currentRoute === "home" ? 1.0 : 0.55
                }
                Text {
                    text: EzI18n.t("nav_home", "Home")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 11
                    font.bold: root.currentRoute === "home"
                    color: root.currentRoute === "home" ? EzTheme.accentLight : EzTheme.textMuted
                    Layout.fillWidth: true
                }
            }
            MouseArea {
                id: homeMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: root.navigate("home")
            }
        }

        Item { height: 16 }

        // ── LIBRARY section ──
        Text {
            text: EzI18n.t("sidebar_library", "BIBLIOTHEK")
            font.family: EzTheme.mcFontFamily
            font.pixelSize: 9
            font.bold: true
            color: EzTheme.textSubtle
            font.letterSpacing: 0.8
            Layout.leftMargin: 16
            Layout.bottomMargin: 3
        }

        // PROFILES
        Rectangle {
            id: profilesItem
            Layout.fillWidth: true
            Layout.leftMargin: 8
            Layout.rightMargin: 8
            height: 34
            radius: 6
            property bool isActive: root.currentRoute === "profiles" || root.currentRoute === "profile_detail"
            color: isActive ? EzTheme.surfaceActive : (profilesMouse.containsMouse ? EzTheme.surface2 : "transparent")
            Behavior on color { ColorAnimation { duration: 110 } }
            Rectangle {
                width: 3
                height: 18
                radius: 2
                color: EzTheme.accent
                visible: parent.isActive
                anchors.left: parent.left
                anchors.leftMargin: -4
                anchors.verticalCenter: parent.verticalCenter
            }
            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 12
                anchors.rightMargin: 10
                spacing: 9
                Image {
                    source: "icons/box.svg"
                    Layout.preferredWidth: 15
                    Layout.preferredHeight: 15
                    fillMode: Image.PreserveAspectFit
                    opacity: profilesItem.isActive ? 1.0 : 0.55
                }
                Text {
                    text: EzI18n.t("nav_profiles", "Profile")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 11
                    font.bold: profilesItem.isActive
                    color: profilesItem.isActive ? EzTheme.accentLight : EzTheme.textMuted
                    Layout.fillWidth: true
                }
            }
            MouseArea {
                id: profilesMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: root.navigate("profile_detail")
            }
        }

        // INSTALLED MODS
        Rectangle {
            id: modsItem
            Layout.fillWidth: true
            Layout.leftMargin: 8
            Layout.rightMargin: 8
            height: 34
            radius: 6
            color: root.currentRoute === "installed_mods" ? EzTheme.surfaceActive : (modsMouse.containsMouse ? EzTheme.surface2 : "transparent")
            Behavior on color { ColorAnimation { duration: 110 } }
            Rectangle {
                width: 3
                height: 18
                radius: 2
                color: EzTheme.accent
                visible: root.currentRoute === "installed_mods"
                anchors.left: parent.left
                anchors.leftMargin: -4
                anchors.verticalCenter: parent.verticalCenter
            }
            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 12
                anchors.rightMargin: 10
                spacing: 9
                Image {
                    source: "icons/mods.svg"
                    Layout.preferredWidth: 15
                    Layout.preferredHeight: 15
                    fillMode: Image.PreserveAspectFit
                    opacity: root.currentRoute === "installed_mods" ? 1.0 : 0.55
                }
                Text {
                    text: EzI18n.t("nav_installed_mods", "Mods")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 11
                    font.bold: root.currentRoute === "installed_mods"
                    color: root.currentRoute === "installed_mods" ? EzTheme.accentLight : EzTheme.textMuted
                    Layout.fillWidth: true
                }
            }
            MouseArea {
                id: modsMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: root.navigate("installed_mods")
            }
        }

        // MODRINTH EXPLORER
        Rectangle {
            id: modrinthItem
            Layout.fillWidth: true
            Layout.leftMargin: 8
            Layout.rightMargin: 8
            height: 34
            radius: 6
            color: root.currentRoute === "mods" ? EzTheme.surfaceActive : (modrinthMouse.containsMouse ? EzTheme.surface2 : "transparent")
            Behavior on color { ColorAnimation { duration: 110 } }
            Rectangle {
                width: 3
                height: 18
                radius: 2
                color: EzTheme.accent
                visible: root.currentRoute === "mods"
                anchors.left: parent.left
                anchors.leftMargin: -4
                anchors.verticalCenter: parent.verticalCenter
            }
            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 12
                anchors.rightMargin: 10
                spacing: 9
                Image {
                    source: "icons/modrinth.svg"
                    Layout.preferredWidth: 15
                    Layout.preferredHeight: 15
                    fillMode: Image.PreserveAspectFit
                    opacity: root.currentRoute === "mods" ? 1.0 : 0.55
                }
                Text {
                    text: EzI18n.t("nav_modrinth", "Modrinth Store")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 11
                    font.bold: root.currentRoute === "mods"
                    color: root.currentRoute === "mods" ? EzTheme.accentLight : EzTheme.textMuted
                    Layout.fillWidth: true
                }
            }
            MouseArea {
                id: modrinthMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: root.navigate("mods")
            }
        }

        Item { height: 16 }

        // ── SYSTEM section ──
        Text {
            text: EzI18n.t("sidebar_system", "SYSTEM")
            font.family: EzTheme.mcFontFamily
            font.pixelSize: 9
            font.bold: true
            color: EzTheme.textSubtle
            font.letterSpacing: 0.8
            Layout.leftMargin: 16
            Layout.bottomMargin: 3
        }

        // SETTINGS
        Rectangle {
            id: settingsItem
            Layout.fillWidth: true
            Layout.leftMargin: 8
            Layout.rightMargin: 8
            height: 34
            radius: 6
            color: root.currentRoute === "settings" ? EzTheme.surfaceActive : (settingsMouse.containsMouse ? EzTheme.surface2 : "transparent")
            Behavior on color { ColorAnimation { duration: 110 } }
            Rectangle {
                width: 3
                height: 18
                radius: 2
                color: EzTheme.accent
                visible: root.currentRoute === "settings"
                anchors.left: parent.left
                anchors.leftMargin: -4
                anchors.verticalCenter: parent.verticalCenter
            }
            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 12
                anchors.rightMargin: 10
                spacing: 9
                Image {
                    source: "icons/settings.svg"
                    Layout.preferredWidth: 15
                    Layout.preferredHeight: 15
                    fillMode: Image.PreserveAspectFit
                    opacity: root.currentRoute === "settings" ? 1.0 : 0.55
                }
                Text {
                    text: EzI18n.t("nav_settings", "Einstellungen")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 11
                    font.bold: root.currentRoute === "settings"
                    color: root.currentRoute === "settings" ? EzTheme.accentLight : EzTheme.textMuted
                    Layout.fillWidth: true
                }
            }
            MouseArea {
                id: settingsMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: root.navigate("settings")
            }
        }

        // Spacer
        Item { Layout.fillHeight: true }

        // ── ACCOUNT WIDGET ──
        Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

        // ── ACCOUNT WIDGET ──
        Rectangle {
            Layout.fillWidth: true
            height: 52
            color: accountHover.containsMouse ? EzTheme.surface2 : "transparent"
            Behavior on color { ColorAnimation { duration: 120 } }

            // When NOT logged in → show Login button
            RowLayout {
                anchors { fill: parent; leftMargin: 14; rightMargin: 12 }
                spacing: 10
                visible: !root.isLoggedIn

                Rectangle {
                    width: 30; height: 30; radius: 15
                    color: EzTheme.surface3
                    border.color: EzTheme.borderLight; border.width: 1
                    Text { text: "?"; font.family: EzTheme.fontFamily; font.pixelSize: 14; font.bold: true; color: EzTheme.textSubtle; anchors.centerIn: parent }
                }

                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 1
                    Text { text: EzI18n.t("sidebar_not_logged_in", "Nicht eingeloggt"); font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.textSecondary }
                    Text { text: EzI18n.t("sidebar_link_account", "Microsoft-Konto verknüpfen"); font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.accent }
                }
            }

            // When logged in → show avatar + username
            RowLayout {
                anchors { fill: parent; leftMargin: 14; rightMargin: 12 }
                spacing: 10
                visible: root.isLoggedIn

                Rectangle {
                    width: 30; height: 30; radius: 15
                    color: EzTheme.surface2
                    border.color: EzTheme.borderLight; border.width: 1
                    clip: true

                    Image {
                        id: avatarImg
                        anchors.fill: parent
                        source: root.accountUser !== "" ? ("https://mc-heads.net/avatar/" + root.accountUser + "/30") : ""
                        fillMode: Image.PreserveAspectCrop
                        visible: status === Image.Ready
                    }
                    Text {
                        visible: avatarImg.status !== Image.Ready
                        text: root.accountUser !== "" ? root.accountUser.charAt(0).toUpperCase() : "M"
                        font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true
                        color: EzTheme.accentLight; anchors.centerIn: parent
                    }
                }

                ColumnLayout {
                    Layout.fillWidth: true; spacing: 1
                    Text { text: root.accountUser; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text; elide: Text.ElideRight; Layout.fillWidth: true }
                    Text { text: root.accountTypeName; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textSubtle }
                }

                Image { source: "icons/more.svg"; width: 13; height: 13; fillMode: Image.PreserveAspectFit; opacity: 0.35 }
            }

            MouseArea { id: accountHover; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; z: -1 }
        }
    }
}
