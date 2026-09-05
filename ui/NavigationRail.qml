import QtQuick 2.15
import QtQuick.Controls 2.15

Rectangle {
    id: root
    objectName: "navRail"
    width: 88
    color: EzTheme.sidebarBg
    property string currentRoute: "home"
    signal navigate(string route)
    signal createProfileClicked()

    function isActive(route) {
        if (route === "profiles") return currentRoute === "profiles" || currentRoute === "profile_detail"
        if (route === "installed_mods") return currentRoute === "installed_mods" || currentRoute === "mods_installed"
        if (route === "mods") return currentRoute === "mods" || currentRoute === "modrinth" || currentRoute === "store"
        if (route === "cape") return currentRoute === "cape" || currentRoute === "cape_editor"
        return currentRoute === route
    }

    Rectangle { anchors.right: parent.right; width: 1; height: parent.height; color: EzTheme.border }
    Image { anchors.top: parent.top; anchors.topMargin: 17; anchors.horizontalCenter: parent.horizontalCenter; width: 45; height: 45; source: "assets/logo.svg"; fillMode: Image.PreserveAspectFit; smooth: true }

    Column {
        anchors.top: parent.top; anchors.topMargin: 85; anchors.horizontalCenter: parent.horizontalCenter; spacing: 9
        Repeater {
            model: [
                { route: "home", label: "Spielen", icon: "nav-home.svg" },
                { route: "profiles", label: "Profile", icon: "nav-profiles.svg" },
                { route: "installed_mods", label: "Installiert", icon: "nav-mods.svg" },
                { route: "mods", label: "Bibliothek", icon: "nav-discover.svg" },
                { route: "versions", label: "Versionen", icon: "nav-versions.svg" },
                { route: "cape", label: "Capes", icon: "nav-cape.svg" }
            ]
            Rectangle {
                id: navButton
                readonly property bool requiresMods: modelData.route === "installed_mods"
                readonly property bool requiresEzClient: modelData.route === "cape"
                readonly property bool active: root.isActive(modelData.route)
                visible: (!requiresMods || !profileController || profileController.activeSupportsMods) && (!requiresEzClient || !profileController || profileController.activeHasEzClient)
                width: 58; height: visible ? 58 : 0; radius: 14
                color: active ? EzTheme.surfaceActive : (navMouse.containsMouse ? EzTheme.surfaceHover : "transparent")
                border.width: 1; border.color: active ? EzTheme.borderAccent : (navMouse.containsMouse ? EzTheme.border : "transparent")
                scale: navMouse.pressed ? 0.94 : (navMouse.containsMouse ? 1.035 : 1)
                Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }
                Behavior on border.color { ColorAnimation { duration: EzTheme.animNormal } }
                Behavior on scale { NumberAnimation { duration: EzTheme.animFast; easing.type: Easing.OutCubic } }
                Rectangle { anchors.left: parent.left; anchors.leftMargin: -15; anchors.verticalCenter: parent.verticalCenter; width: 4; height: navButton.active ? 34 : 0; radius: 2; color: EzTheme.accent; Behavior on height { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } } }
                Image { anchors.centerIn: parent; width: 27; height: 27; source: "icons/" + modelData.icon; opacity: navButton.active ? 1 : (navMouse.containsMouse ? 0.9 : 0.58); Behavior on opacity { NumberAnimation { duration: EzTheme.animFast } } }
                MouseArea { id: navMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: root.navigate(modelData.route) }
                ToolTip.visible: navMouse.containsMouse
                ToolTip.delay: 350
                ToolTip.text: modelData.label
            }
        }
    }

    Rectangle {
        id: createProfileButton
        anchors.bottom: parent.bottom; anchors.bottomMargin: 86; anchors.horizontalCenter: parent.horizontalCenter
        width: 58; height: 58; radius: 14
        color: createProfileMouse.containsMouse ? EzTheme.surfaceHover : "transparent"
        border.width: 1; border.color: createProfileMouse.containsMouse ? EzTheme.borderLight : EzTheme.border
        scale: createProfileMouse.pressed ? 0.94 : 1
        Behavior on scale { NumberAnimation { duration: EzTheme.animFast } }
        Image { anchors.centerIn: parent; width: 27; height: 27; source: "icons/plus.svg"; opacity: createProfileMouse.containsMouse ? 1 : 0.7 }
        MouseArea { id: createProfileMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: root.createProfileClicked() }
        ToolTip.visible: createProfileMouse.containsMouse
        ToolTip.delay: 350
        ToolTip.text: "Neues Profil"
    }

    Rectangle {
        id: settingsButton
        anchors.bottom: parent.bottom; anchors.bottomMargin: 16; anchors.horizontalCenter: parent.horizontalCenter
        width: 58; height: 58; radius: 14
        readonly property bool active: root.isActive("settings")
        color: active ? EzTheme.surfaceActive : (settingsMouse.containsMouse ? EzTheme.surfaceHover : "transparent")
        border.width: 1; border.color: active ? EzTheme.borderAccent : (settingsMouse.containsMouse ? EzTheme.border : "transparent")
        scale: settingsMouse.pressed ? 0.94 : 1
        Behavior on scale { NumberAnimation { duration: EzTheme.animFast } }
        Image { anchors.centerIn: parent; width: 27; height: 27; source: "icons/nav-settings.svg"; opacity: settingsButton.active ? 1 : 0.62 }
        MouseArea { id: settingsMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: root.navigate("settings") }
        ToolTip.visible: settingsMouse.containsMouse
        ToolTip.delay: 350
        ToolTip.text: "Einstellungen"
    }
}
