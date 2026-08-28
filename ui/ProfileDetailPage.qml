import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root

    property string selectedTab: "overview"
    property string modSearchQuery: ""

    readonly property string inspectedId: typeof profileController !== "undefined" && profileController ? profileController.inspectedId : ""
    readonly property string inspectedName: typeof profileController !== "undefined" && profileController ? profileController.inspectedName : "Profil"
    readonly property string inspectedVersion: typeof profileController !== "undefined" && profileController ? profileController.inspectedVersion : "26.2"
    readonly property string inspectedLoader: typeof profileController !== "undefined" && profileController ? profileController.inspectedLoader : "Fabric"
    readonly property int inspectedModsCount: typeof profileController !== "undefined" && profileController ? profileController.inspectedModsCount : 0
    readonly property string inspectedLastPlayed: typeof profileController !== "undefined" && profileController ? profileController.inspectedLastPlayed : "Never"
    readonly property string inspectedGameDir: typeof profileController !== "undefined" && profileController ? profileController.inspectedGameDir : ""
    readonly property bool isInspectedActive: typeof profileController !== "undefined" && profileController ? profileController.isInspectedActive : true

    // Shader active state lookup
    readonly property var shaderList: typeof profileController !== "undefined" && profileController && profileController.inspectedShaderPacks ? profileController.inspectedShaderPacks : []
    readonly property var resourcePackList: typeof profileController !== "undefined" && profileController && profileController.inspectedResourcePacks ? profileController.inspectedResourcePacks : []

    readonly property string activeShaderName: {
        for (var i = 0; i < shaderList.length; i++) {
            if (shaderList[i].isActive) return shaderList[i].name;
        }
        return "";
    }

    ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // ==========================================
        // 1. HERO HEADER (Glassmorphic Profile Header)
        // ==========================================
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 92
            color: EzTheme.surface
            border.color: EzTheme.border
            border.width: 1

            // Subtle glow highlight line at top
            Rectangle {
                anchors.top: parent.top
                anchors.left: parent.left
                anchors.right: parent.right
                height: 1
                color: root.isInspectedActive ? EzTheme.accent : EzTheme.borderLight
                opacity: root.isInspectedActive ? 0.8 : 0.3
            }

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 24
                anchors.rightMargin: 24
                spacing: 16

                // Profile Avatar / Icon (52x52 with gradient & border glow)
                Rectangle {
                    Layout.preferredWidth: 52
                    Layout.preferredHeight: 52
                    radius: 12
                    color: root.isInspectedActive ? EzTheme.surfaceActive : EzTheme.surface2
                    border.color: root.isInspectedActive ? EzTheme.accent : EzTheme.borderLight
                    border.width: 1.5

                    // Inner letter
                    Text {
                        text: root.inspectedName.length >= 2 ? root.inspectedName.substring(0, 2).toUpperCase() : (root.inspectedName.length === 1 ? root.inspectedName.toUpperCase() : "EZ")
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 18
                        font.bold: true
                        color: root.isInspectedActive ? EzTheme.accentLight : EzTheme.text
                        anchors.centerIn: parent
                    }
                }

                // Profile Title & Meta Badges
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 4

                    // Back Navigation Button
                    RowLayout {
                        spacing: 6
                        Rectangle {
                            height: 20
                            width: backText.implicitWidth + 14
                            radius: 4
                            color: backMouse.containsMouse ? EzTheme.surface3 : "transparent"
                            Behavior on color { ColorAnimation { duration: 90 } }

                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 4
                                Text {
                                    id: backText
                                    text: "← Zurück zu allen Profilen"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: true
                                    color: backMouse.containsMouse ? EzTheme.accentLight : EzTheme.textMuted
                                    Behavior on color { ColorAnimation { duration: 90 } }
                                }
                            }

                            MouseArea {
                                id: backMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    if (typeof window !== "undefined" && window.navigateTo) {
                                        window.navigateTo("profiles")
                                    }
                                }
                            }
                        }
                    }

                    // Main Profile Name
                    RowLayout {
                        spacing: 10
                        Text {
                            text: root.inspectedName
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 18
                            font.bold: true
                            color: EzTheme.text
                            elide: Text.ElideRight
                        }

                        // Active Indicator Pill
                        Rectangle {
                            height: 18
                            width: activeBadgeTxt.implicitWidth + 12
                            radius: 4
                            color: root.isInspectedActive ? "#0E331A" : "#1B1D26"
                            border.color: root.isInspectedActive ? EzTheme.accent : EzTheme.border
                            border.width: 1

                            Text {
                                id: activeBadgeTxt
                                anchors.centerIn: parent
                                text: root.isInspectedActive ? "● AKTIV" : "INAKTIV"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 9
                                font.bold: true
                                color: root.isInspectedActive ? EzTheme.accentLight : EzTheme.textMuted
                            }
                        }
                    }

                    // Meta Chips Row (Minecraft Version, Loader, Stats)
                    RowLayout {
                        spacing: 8

                        Rectangle {
                            height: 18
                            width: mcVerTxt.implicitWidth + 10
                            radius: 4
                            color: EzTheme.surface2
                            border.color: EzTheme.border
                            border.width: 1
                            Text {
                                id: mcVerTxt
                                anchors.centerIn: parent
                                text: "Minecraft " + root.inspectedVersion
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 10
                                font.bold: true
                                color: EzTheme.textSecondary
                            }
                        }

                        Rectangle {
                            height: 18
                            width: loaderTxt.implicitWidth + 10
                            radius: 4
                            color: "#1F1A30"
                            border.color: "#6D28D9"
                            border.width: 1
                            Text {
                                id: loaderTxt
                                anchors.centerIn: parent
                                text: root.inspectedLoader
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 10
                                font.bold: true
                                color: "#C084FC"
                            }
                        }

                        Text {
                            text: root.inspectedModsCount + " Mods  ·  " + root.shaderList.length + " Shader  ·  " + root.resourcePackList.length + " Packs"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            color: EzTheme.textMuted
                        }
                    }
                }

                // Right Action Cluster
                RowLayout {
                    spacing: 8

                    // "Als aktives Profil wählen" button (shown when inspecting non-active profile)
                    EzButton {
                        text: "Als aktiv festlegen"
                        primary: true
                        visible: !root.isInspectedActive
                        Layout.preferredHeight: 38
                        Layout.preferredWidth: 145
                        onClicked: {
                            if (profileController) {
                                profileController.activateInspectedProfile()
                            }
                        }
                    }

                    // Play Button (shown when inspecting active profile)
                    Rectangle {
                        visible: root.isInspectedActive
                        Layout.preferredWidth: 125
                        Layout.preferredHeight: 38
                        radius: EzTheme.radiusSm
                        scale: playBtnMouse.containsMouse ? 1.03 : 1.0
                        color: playBtnMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent
                        Behavior on color { ColorAnimation { duration: 100 } }
                        Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Text { text: "▶"; font.pixelSize: 12; color: "#000000" }
                            Text {
                                text: EzI18n.t("home_play", "SPIELEN")
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 12
                                font.bold: true
                                color: "#000000"
                            }
                        }

                        MouseArea {
                            id: playBtnMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController) {
                                    profileController.launchActiveProfile()
                                }
                            }
                        }
                    }

                    // Open Directory Button
                    Rectangle {
                        Layout.preferredWidth: 38
                        Layout.preferredHeight: 38
                        radius: 6
                        color: fldMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: fldMouse.containsMouse ? EzTheme.accent : EzTheme.border
                        border.width: 1
                        Behavior on color { ColorAnimation { duration: 100 } }

                        Image {
                            source: "icons/folder.svg"
                            width: 16; height: 16
                            fillMode: Image.PreserveAspectFit
                            anchors.centerIn: parent
                            opacity: fldMouse.containsMouse ? 1.0 : 0.7
                        }

                        ToolTip.visible: fldMouse.containsMouse
                        ToolTip.text: "Profil-Ordner öffnen"
                        ToolTip.delay: 300

                        MouseArea {
                            id: fldMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController) {
                                    profileController.openFolder("")
                                }
                            }
                        }
                    }

                    // Duplicate Profile Button
                    Rectangle {
                        Layout.preferredWidth: 38
                        Layout.preferredHeight: 38
                        radius: 6
                        color: dupMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: dupMouse.containsMouse ? EzTheme.accent : EzTheme.border
                        border.width: 1
                        Behavior on color { ColorAnimation { duration: 100 } }

                        Image {
                            source: "icons/copy.svg"
                            width: 16; height: 16
                            fillMode: Image.PreserveAspectFit
                            anchors.centerIn: parent
                            opacity: dupMouse.containsMouse ? 1.0 : 0.7
                        }

                        ToolTip.visible: dupMouse.containsMouse
                        ToolTip.text: "Profil duplizieren"
                        ToolTip.delay: 300

                        MouseArea {
                            id: dupMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController) {
                                    profileController.duplicateProfile(root.inspectedId)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. TAB BAR WITH SLIDING UNDERLINE INDICATOR
        // ==========================================
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 40
            color: EzTheme.titlebarBg
            border.color: EzTheme.border
            border.width: 1

            Row {
                anchors.left: parent.left
                anchors.leftMargin: 24
                anchors.verticalCenter: parent.verticalCenter
                spacing: 8

                Repeater {
                    model: [
                        { tabId: "overview", label: "Übersicht", icon: "home.svg" },
                        { tabId: "mods", label: "Mods (" + root.inspectedModsCount + ")", icon: "mods.svg" },
                        { tabId: "shaders", label: "Shader (" + root.shaderList.length + ")", icon: "sparkles.svg" },
                        { tabId: "resourcepacks", label: "Resource Packs (" + root.resourcePackList.length + ")", icon: "box.svg" },
                        { tabId: "settings", label: "Einstellungen", icon: "settings.svg" }
                    ]

                    Rectangle {
                        width: tabInnerRow.implicitWidth + 24
                        height: 40
                        color: "transparent"

                        RowLayout {
                            id: tabInnerRow
                            anchors.centerIn: parent
                            spacing: 7

                            Image {
                                source: "icons/" + modelData.icon
                                width: 14; height: 14
                                fillMode: Image.PreserveAspectFit
                                opacity: root.selectedTab === modelData.tabId ? 1.0 : (tMouse.containsMouse ? 0.8 : 0.45)
                                Behavior on opacity { NumberAnimation { duration: 100 } }
                            }

                            Text {
                                text: modelData.label
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 12
                                font.bold: root.selectedTab === modelData.tabId
                                color: root.selectedTab === modelData.tabId ? EzTheme.accentLight : (tMouse.containsMouse ? EzTheme.text : EzTheme.textMuted)
                                Behavior on color { ColorAnimation { duration: 100 } }
                            }
                        }

                        // Sliding Active Underline
                        Rectangle {
                            height: 2.5
                            anchors.left: parent.left
                            anchors.right: parent.right
                            anchors.bottom: parent.bottom
                            color: EzTheme.accent
                            visible: root.selectedTab === modelData.tabId
                        }

                        MouseArea {
                            id: tMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: root.selectedTab = modelData.tabId
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. MAIN TAB CONTENT AREA (Stable ScrollView)
        // ==========================================
        ScrollView {
            id: mainScrollView
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true
            contentWidth: availableWidth

            Item {
                width: mainScrollView.availableWidth
                implicitHeight: contentColumn.implicitHeight + 48

                ColumnLayout {
                    id: contentColumn
                    anchors.left: parent.left
                    anchors.right: parent.right
                    anchors.top: parent.top
                    anchors.leftMargin: 24
                    anchors.rightMargin: 24
                    anchors.topMargin: 20
                    spacing: 16

                    // ──────────────────────────────────────────
                    // TAB 1: ÜBERSICHT (OVERVIEW)
                    // ──────────────────────────────────────────
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 16
                        visible: root.selectedTab === "overview"

                        // 1. TOP 3 METRICS / CONTENT SUMMARY CARDS
                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 12

                            // Card 1: Mods Metric
                            Rectangle {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 84
                                radius: 10
                                color: EzTheme.surface
                                border.color: modCardMouse.containsMouse ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                scale: modCardMouse.containsMouse ? 1.01 : 1.0
                                Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }
                                Behavior on border.color { ColorAnimation { duration: 100 } }

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.margins: 14
                                    spacing: 12

                                    Rectangle {
                                        Layout.preferredWidth: 44
                                        Layout.preferredHeight: 44
                                        radius: 8
                                        color: EzTheme.surface2
                                        Image { source: "icons/mods.svg"; width: 22; height: 22; anchors.centerIn: parent; fillMode: Image.PreserveAspectFit }
                                    }

                                    ColumnLayout {
                                        Layout.fillWidth: true
                                        spacing: 2
                                        Text { text: "Installierte Mods"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textMuted }
                                        Text { text: root.inspectedModsCount + " Mods"; font.family: EzTheme.mcFontFamily; font.pixelSize: 16; font.bold: true; color: EzTheme.text }
                                        Text { text: "Verwalten & hinzufügen →"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.accentLight }
                                    }
                                }

                                MouseArea {
                                    id: modCardMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: root.selectedTab = "mods"
                                }
                            }

                            // Card 2: Shaderpacks Metric
                            Rectangle {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 84
                                radius: 10
                                color: EzTheme.surface
                                border.color: shaderCardMouse.containsMouse ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                scale: shaderCardMouse.containsMouse ? 1.01 : 1.0
                                Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }
                                Behavior on border.color { ColorAnimation { duration: 100 } }

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.margins: 14
                                    spacing: 12

                                    Rectangle {
                                        Layout.preferredWidth: 44
                                        Layout.preferredHeight: 44
                                        radius: 8
                                        color: EzTheme.surface2
                                        Image { source: "icons/sparkles.svg"; width: 22; height: 22; anchors.centerIn: parent; fillMode: Image.PreserveAspectFit }
                                    }

                                    ColumnLayout {
                                        Layout.fillWidth: true
                                        spacing: 2
                                        Text { text: "Shader-Engine"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textMuted }
                                        Text {
                                            text: root.activeShaderName ? root.activeShaderName : (root.shaderList.length > 0 ? root.shaderList.length + " Shader verfügbar" : "Kein Shader aktiv")
                                            font.family: EzTheme.mcFontFamily
                                            font.pixelSize: 14
                                            font.bold: true
                                            color: root.activeShaderName ? EzTheme.accentLight : EzTheme.text
                                            elide: Text.ElideRight
                                            Layout.fillWidth: true
                                        }
                                        Text { text: "Shader wechseln →"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.accentLight }
                                    }
                                }

                                MouseArea {
                                    id: shaderCardMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: root.selectedTab = "shaders"
                                }
                            }

                            // Card 3: Resource Packs Metric
                            Rectangle {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 84
                                radius: 10
                                color: EzTheme.surface
                                border.color: rpCardMouse.containsMouse ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                scale: rpCardMouse.containsMouse ? 1.01 : 1.0
                                Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }
                                Behavior on border.color { ColorAnimation { duration: 100 } }

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.margins: 14
                                    spacing: 12

                                    Rectangle {
                                        Layout.preferredWidth: 44
                                        Layout.preferredHeight: 44
                                        radius: 8
                                        color: EzTheme.surface2
                                        Image { source: "icons/box.svg"; width: 22; height: 22; anchors.centerIn: parent; fillMode: Image.PreserveAspectFit }
                                    }

                                    ColumnLayout {
                                        Layout.fillWidth: true
                                        spacing: 2
                                        Text { text: "Resource Packs"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textMuted }
                                        Text { text: root.resourcePackList.length + " Packs"; font.family: EzTheme.mcFontFamily; font.pixelSize: 16; font.bold: true; color: EzTheme.text }
                                        Text { text: "Packs verwalten →"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.accentLight }
                                    }
                                }

                                MouseArea {
                                    id: rpCardMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: root.selectedTab = "resourcepacks"
                                }
                            }
                        }

                        // 2. SYSTEM & PROFIL-SPEZIFIKATIONEN
                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 6

                            Text {
                                text: "PROFIL & SYSTEM-DETAILS"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzTheme.textSecondary
                                font.letterSpacing: 0.8
                            }

                            EzSurface {
                                Layout.fillWidth: true
                                implicitHeight: 180

                                ColumnLayout {
                                    anchors.fill: parent
                                    anchors.margins: 16
                                    spacing: 12

                                    RowLayout {
                                        Layout.fillWidth: true
                                        Text { text: "Minecraft Version"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; Layout.preferredWidth: 160 }
                                        Text { text: root.inspectedVersion + " (" + root.inspectedLoader + ")"; font.family: "Consolas, monospace"; font.pixelSize: 12; font.bold: true; color: EzTheme.text; Layout.fillWidth: true }
                                    }

                                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                                    RowLayout {
                                        Layout.fillWidth: true
                                        Text { text: "Java Laufzeit"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; Layout.preferredWidth: 160 }
                                        Text { text: "Adoptium OpenJDK 25 (64-Bit Server VM)"; font.family: "Consolas, monospace"; font.pixelSize: 12; font.bold: true; color: EzTheme.text; Layout.fillWidth: true }
                                    }

                                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                                    RowLayout {
                                        Layout.fillWidth: true
                                        Text { text: "Zuletzt gespielt"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; Layout.preferredWidth: 160 }
                                        Text { text: root.inspectedLastPlayed && root.inspectedLastPlayed !== "Never" ? root.inspectedLastPlayed : "Noch keine Spielzeit verzeichnet"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textSecondary; Layout.fillWidth: true }
                                    }

                                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                                    RowLayout {
                                        Layout.fillWidth: true
                                        Text { text: "Speicherort"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; Layout.preferredWidth: 160 }
                                        Text { text: root.inspectedGameDir; font.family: "Consolas, monospace"; font.pixelSize: 11; color: EzTheme.textSecondary; elide: Text.ElideMiddle; Layout.fillWidth: true }
                                    }
                                }
                            }
                        }

                        // 3. SCHNELLZUGRIFF & PROFIL-TOOLS
                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 6

                            Text {
                                text: "SCHNELLZUGRIFF & ORDNER"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzTheme.textSecondary
                                font.letterSpacing: 0.8
                            }

                            RowLayout {
                                Layout.fillWidth: true
                                spacing: 12

                                // Screenshots Tool
                                Rectangle {
                                    Layout.fillWidth: true
                                    Layout.preferredHeight: 64
                                    radius: 8
                                    color: EzTheme.surface
                                    border.color: scrM.containsMouse ? EzTheme.accent : EzTheme.border
                                    border.width: 1
                                    scale: scrM.containsMouse ? 1.01 : 1.0
                                    Behavior on scale { NumberAnimation { duration: 100 } }

                                    RowLayout {
                                        anchors.fill: parent
                                        anchors.margins: 14
                                        spacing: 12
                                        Image { source: "icons/camera.svg"; width: 20; height: 20; fillMode: Image.PreserveAspectFit }
                                        ColumnLayout {
                                            spacing: 2
                                            Text { text: "Screenshots"; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                            Text { text: "Bilder & Screenshots öffnen"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                                        }
                                    }
                                    MouseArea {
                                        id: scrM
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        cursorShape: Qt.PointingHandCursor
                                        onClicked: if (profileController) profileController.openFolder("screenshots")
                                    }
                                }

                                // Logs Tool
                                Rectangle {
                                    Layout.fillWidth: true
                                    Layout.preferredHeight: 64
                                    radius: 8
                                    color: EzTheme.surface
                                    border.color: logM.containsMouse ? EzTheme.accent : EzTheme.border
                                    border.width: 1
                                    scale: logM.containsMouse ? 1.01 : 1.0
                                    Behavior on scale { NumberAnimation { duration: 100 } }

                                    RowLayout {
                                        anchors.fill: parent
                                        anchors.margins: 14
                                        spacing: 12
                                        Image { source: "icons/file-text.svg"; width: 20; height: 20; fillMode: Image.PreserveAspectFit }
                                        ColumnLayout {
                                            spacing: 2
                                            Text { text: "Crash- & Spiel-Logs"; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                            Text { text: "latest.log & Crash-Dateien"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                                        }
                                    }
                                    MouseArea {
                                        id: logM
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        cursorShape: Qt.PointingHandCursor
                                        onClicked: if (profileController) profileController.openFolder("logs")
                                    }
                                }

                                // Copy Settings Tool
                                Rectangle {
                                    Layout.fillWidth: true
                                    Layout.preferredHeight: 64
                                    radius: 8
                                    color: EzTheme.surface
                                    border.color: cstM.containsMouse ? EzTheme.accent : EzTheme.border
                                    border.width: 1
                                    scale: cstM.containsMouse ? 1.01 : 1.0
                                    Behavior on scale { NumberAnimation { duration: 100 } }

                                    RowLayout {
                                        anchors.fill: parent
                                        anchors.margins: 14
                                        spacing: 12
                                        Image { source: "icons/copy.svg"; width: 20; height: 20; fillMode: Image.PreserveAspectFit }
                                        ColumnLayout {
                                            spacing: 2
                                            Text { text: "Settings übertragen"; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                            Text { text: "Optionen von Profil kopieren"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                                        }
                                    }
                                    MouseArea {
                                        id: cstM
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        cursorShape: Qt.PointingHandCursor
                                        onClicked: copySettingsModal.open()
                                    }
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────
                    // TAB 2: MODS
                    // ──────────────────────────────────────────
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 12
                        visible: root.selectedTab === "mods"

                        // Action Toolbar
                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 10

                            // Search Bar
                            Rectangle {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 34
                                radius: 6
                                color: EzTheme.surface
                                border.color: mSearchInput.activeFocus ? EzTheme.accent : EzTheme.border
                                border.width: 1

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.leftMargin: 10
                                    anchors.rightMargin: 10
                                    spacing: 8

                                    Image {
                                        source: "icons/search.svg"
                                        width: 14; height: 14
                                        opacity: 0.5
                                    }

                                    TextInput {
                                        id: mSearchInput
                                        Layout.fillWidth: true
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 12
                                        color: EzTheme.text
                                        selectByMouse: true
                                        onTextChanged: root.modSearchQuery = text.trim().toLowerCase()

                                        Text {
                                            text: "Mods filtern…"
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 12
                                            color: EzTheme.textMuted
                                            visible: !mSearchInput.text
                                        }
                                    }
                                }
                            }

                            EzButton {
                                text: "Mod-Ordner öffnen"
                                Layout.preferredHeight: 34
                                onClicked: {
                                    if (profileController) profileController.openFolder("mods")
                                }
                            }

                            EzButton {
                                text: "+ Mods entdecken"
                                primary: true
                                Layout.preferredHeight: 34
                                onClicked: {
                                    if (typeof window !== "undefined" && window.navigateTo) {
                                        window.navigateTo("mods")
                                    }
                                }
                            }
                        }

                        // Mods List Repeater
                        Repeater {
                            model: profileController ? profileController.modModel : null
                            Rectangle {
                                Layout.fillWidth: true
                                height: 64
                                radius: 10
                                scale: modItemMouse.containsMouse ? 1.008 : 1.0
                                Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }
                                color: modItemMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface
                                border.color: model.enabled ? (modItemMouse.containsMouse ? EzTheme.accentLight : EzTheme.border) : "#1A1A22"
                                border.width: 1
                                opacity: model.enabled ? 1.0 : 0.65
                                visible: root.modSearchQuery === "" || (model.name && model.name.toLowerCase().indexOf(root.modSearchQuery) !== -1)

                                Behavior on color { ColorAnimation { duration: 90 } }
                                Behavior on border.color { ColorAnimation { duration: 90 } }
                                Behavior on opacity { NumberAnimation { duration: 90 } }

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.leftMargin: 14
                                    anchors.rightMargin: 14
                                    spacing: 12

                                    // Mod Icon / Initial
                                    Rectangle {
                                        Layout.preferredWidth: 40
                                        Layout.preferredHeight: 40
                                        radius: 8
                                        color: EzTheme.surface3
                                        clip: true

                                        Image {
                                            id: pModIcon
                                            anchors.fill: parent
                                            source: (model.name === "EzClient" || model.name === "EzClient Core") ? "assets/logo.png" : (model.icon_url || "")
                                            fillMode: Image.PreserveAspectCrop
                                            asynchronous: true
                                            visible: status === Image.Ready
                                        }

                                        Text {
                                            visible: pModIcon.status !== Image.Ready
                                            text: model.name ? model.name.charAt(0).toUpperCase() : "M"
                                            font.family: EzTheme.mcFontFamily
                                            font.pixelSize: 16
                                            font.bold: true
                                            color: EzTheme.accentLight
                                            anchors.centerIn: parent
                                        }
                                    }

                                    // Mod Info
                                    ColumnLayout {
                                        Layout.fillWidth: true
                                        spacing: 3

                                        RowLayout {
                                            spacing: 8
                                            Text {
                                                text: model.name
                                                font.family: EzTheme.fontFamily
                                                font.pixelSize: 13
                                                font.bold: true
                                                color: model.enabled ? EzTheme.text : EzTheme.textMuted
                                            }

                                            Rectangle {
                                                height: 18
                                                width: pVerTxt.implicitWidth + 10
                                                radius: 4
                                                color: "#16221A"
                                                border.color: "#22C96E40"
                                                border.width: 1

                                                Text {
                                                    id: pVerTxt
                                                    text: model.version || "Latest"
                                                    font.family: "Consolas, monospace"
                                                    font.pixelSize: 9
                                                    font.bold: true
                                                    color: EzTheme.accentLight
                                                    anchors.centerIn: parent
                                                }
                                            }

                                            Text {
                                                text: "von " + (model.author || "Modrinth")
                                                font.family: EzTheme.fontFamily
                                                font.pixelSize: 10
                                                color: EzTheme.textMuted
                                                visible: model.author !== ""
                                            }
                                        }

                                        Text {
                                            text: model.description || "Keine Beschreibung hinterlegt"
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 10
                                            color: EzTheme.textMuted
                                            elide: Text.ElideRight
                                            Layout.fillWidth: true
                                        }
                                    }

                                    // Toggle Switch
                                    Rectangle {
                                        Layout.preferredWidth: 38
                                        Layout.preferredHeight: 22
                                        radius: 11
                                        color: model.enabled ? EzTheme.accent : EzTheme.surface3
                                        border.color: model.enabled ? EzTheme.accent : EzTheme.borderLight
                                        border.width: 1

                                        Rectangle {
                                            width: 16
                                            height: 16
                                            radius: 8
                                            color: "#FFFFFF"
                                            anchors.verticalCenter: parent.verticalCenter
                                            x: model.enabled ? parent.width - width - 3 : 3
                                            Behavior on x { NumberAnimation { duration: 120 } }
                                        }

                                        MouseArea {
                                            anchors.fill: parent
                                            cursorShape: Qt.PointingHandCursor
                                            onClicked: profileController.toggleMod(model.slug || model.name)
                                        }
                                    }

                                    // Delete Button
                                    Rectangle {
                                        property bool isEzCore: (model.slug || "").toLowerCase() === "ezclient" || (model.name || "").toLowerCase() === "ezclient" || (model.name || "").toLowerCase() === "ezclient core"
                                        Layout.preferredWidth: 28
                                        Layout.preferredHeight: 28
                                        radius: 6
                                        color: pDelM.containsMouse ? "#3B1119" : "transparent"
                                        visible: !isEzCore

                                        Image {
                                            source: "icons/trash.svg"
                                            width: 13
                                            height: 13
                                            fillMode: Image.PreserveAspectFit
                                            anchors.centerIn: parent
                                            opacity: pDelM.containsMouse ? 1.0 : 0.6
                                        }

                                        MouseArea {
                                            id: pDelM
                                            anchors.fill: parent
                                            hoverEnabled: true
                                            cursorShape: Qt.PointingHandCursor
                                            onClicked: profileController.uninstallMod(model.slug || model.name, model.name)
                                        }
                                    }
                                }

                                MouseArea {
                                    id: modItemMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    z: -1
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────
                    // TAB 3: SHADER (WITH PACK ICONS)
                    // ──────────────────────────────────────────
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 12
                        visible: root.selectedTab === "shaders"

                        // Toolbar
                        RowLayout {
                            Layout.fillWidth: true
                            Text {
                                text: root.shaderList.length + " Shaderpacks in diesem Profil"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 13
                                font.bold: true
                                color: EzTheme.text
                            }
                            Item { Layout.fillWidth: true }
                            EzButton {
                                text: "Shader deaktivieren"
                                visible: root.activeShaderName !== ""
                                Layout.preferredHeight: 30
                                onClicked: {
                                    if (profileController) profileController.disableShaderPack()
                                }
                            }
                            EzButton {
                                text: "Ordner öffnen"
                                Layout.preferredHeight: 30
                                onClicked: {
                                    if (profileController) profileController.openShaderPacksFolder()
                                }
                            }
                            EzButton {
                                text: "+ Shader im Store suchen"
                                primary: true
                                Layout.preferredHeight: 30
                                onClicked: {
                                    if (typeof window !== "undefined" && window.navigateTo) {
                                        window.navigateTo("mods")
                                    }
                                }
                            }
                        }

                        // Active Shader Banner
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 52
                            radius: 8
                            color: root.activeShaderName ? "#0B2D19" : EzTheme.surface
                            border.color: root.activeShaderName ? EzTheme.accent : EzTheme.border
                            border.width: 1

                            RowLayout {
                                anchors.fill: parent
                                anchors.leftMargin: 16
                                anchors.rightMargin: 16
                                spacing: 12

                                Image {
                                    source: "icons/sparkles.svg"
                                    width: 18; height: 18
                                    fillMode: Image.PreserveAspectFit
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text {
                                        text: "Aktiver Shader: " + (root.activeShaderName ? root.activeShaderName : "Standard (Kein Shader aktiv)")
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 12
                                        font.bold: true
                                        color: root.activeShaderName ? EzTheme.accentLight : EzTheme.textSecondary
                                    }
                                    Text {
                                        text: root.activeShaderName ? "Iris Shader-Engine rendert aktuell dieses Preset" : "Wähle unten einen Shader aus oder lade neue herunter."
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.textMuted
                                    }
                                }
                            }
                        }

                        // Shader List
                        Repeater {
                            model: root.shaderList
                            Rectangle {
                                Layout.fillWidth: true
                                height: 60
                                radius: 10
                                scale: sItemMouse.containsMouse ? 1.008 : 1.0
                                Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }
                                color: modelData.isActive ? EzTheme.surfaceActive : (sItemMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                                border.color: modelData.isActive ? EzTheme.accent : (sItemMouse.containsMouse ? EzTheme.accentLight : EzTheme.border)
                                border.width: 1
                                opacity: modelData.enabled ? 1.0 : 0.6

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.leftMargin: 14
                                    anchors.rightMargin: 14
                                    spacing: 12

                                    // Shader Pack Icon / Preview
                                    Rectangle {
                                        Layout.preferredWidth: 38
                                        Layout.preferredHeight: 38
                                        radius: 8
                                        color: modelData.isActive ? EzTheme.accentDark : EzTheme.surface3
                                        clip: true

                                        Image {
                                            id: shPackImg
                                            anchors.fill: parent
                                            source: modelData.icon_url || ""
                                            fillMode: Image.PreserveAspectCrop
                                            visible: status === Image.Ready
                                        }

                                        Image {
                                            visible: shPackImg.status !== Image.Ready
                                            source: "icons/sparkles.svg"
                                            width: 18; height: 18
                                            fillMode: Image.PreserveAspectFit
                                            anchors.centerIn: parent
                                        }
                                    }

                                    ColumnLayout {
                                        Layout.fillWidth: true
                                        spacing: 2
                                        RowLayout {
                                            spacing: 8
                                            Text {
                                                text: modelData.name
                                                font.family: EzTheme.fontFamily
                                                font.pixelSize: 13
                                                font.bold: true
                                                color: modelData.isActive ? EzTheme.accentLight : EzTheme.text
                                            }
                                            Rectangle {
                                                height: 16
                                                width: actShTxt.implicitWidth + 8
                                                radius: 3
                                                color: EzTheme.accentDark
                                                visible: modelData.isActive
                                                Text {
                                                    id: actShTxt
                                                    anchors.centerIn: parent
                                                    text: "AKTIV"
                                                    font.family: EzTheme.mcFontFamily
                                                    font.pixelSize: 8
                                                    font.bold: true
                                                    color: EzTheme.accentLight
                                                }
                                            }
                                        }
                                        Text {
                                            text: modelData.filename
                                            font.family: "Consolas, monospace"
                                            font.pixelSize: 10
                                            color: EzTheme.textMuted
                                        }
                                    }

                                    // Activate / Select Shader Button
                                    EzButton {
                                        text: modelData.isActive ? "Aktiv" : "Aktivieren"
                                        primary: !modelData.isActive
                                        enabled: !modelData.isActive && modelData.enabled
                                        Layout.preferredHeight: 30
                                        Layout.preferredWidth: 90
                                        onClicked: {
                                            if (profileController) profileController.selectShaderPack(modelData.filename)
                                        }
                                    }

                                    // Toggle Switch
                                    Rectangle {
                                        Layout.preferredWidth: 36
                                        Layout.preferredHeight: 20
                                        radius: 10
                                        color: modelData.enabled ? EzTheme.accent : EzTheme.surface3
                                        border.color: modelData.enabled ? EzTheme.accent : EzTheme.borderLight
                                        border.width: 1

                                        Rectangle {
                                            width: 14
                                            height: 14
                                            radius: 7
                                            color: "#FFFFFF"
                                            anchors.verticalCenter: parent.verticalCenter
                                            x: modelData.enabled ? parent.width - width - 3 : 3
                                            Behavior on x { NumberAnimation { duration: 120 } }
                                        }

                                        MouseArea {
                                            anchors.fill: parent
                                            cursorShape: Qt.PointingHandCursor
                                            onClicked: {
                                                if (profileController) profileController.toggleShaderPack(modelData.filename)
                                            }
                                        }
                                    }
                                }

                                MouseArea {
                                    id: sItemMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    z: -1
                                }
                            }
                        }

                        // Empty State
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 140
                            radius: 10
                            color: EzTheme.surface
                            border.color: EzTheme.border
                            border.width: 1
                            visible: root.shaderList.length === 0

                            ColumnLayout {
                                anchors.centerIn: parent
                                spacing: 8
                                Image {
                                    source: "icons/sparkles.svg"
                                    Layout.preferredWidth: 28; Layout.preferredHeight: 28
                                    Layout.alignment: Qt.AlignHCenter
                                    opacity: 0.5
                                }
                                Text {
                                    text: "Noch keine Shaderpacks in diesem Profil installiert."
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 13
                                    font.bold: true
                                    color: EzTheme.text
                                    Layout.alignment: Qt.AlignHCenter
                                }
                                Text {
                                    text: "Lade beliebte Shader wie Complementary, BSL oder Iris Shaders herunter."
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    color: EzTheme.textMuted
                                    Layout.alignment: Qt.AlignHCenter
                                }
                                EzButton {
                                    text: "Shader im Store entdecken"
                                    primary: true
                                    Layout.alignment: Qt.AlignHCenter
                                    Layout.preferredHeight: 30
                                    onClicked: {
                                        if (typeof window !== "undefined" && window.navigateTo) {
                                            window.navigateTo("mods")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────
                    // TAB 4: RESOURCE PACKS (WITH PACK ICONS)
                    // ──────────────────────────────────────────
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 12
                        visible: root.selectedTab === "resourcepacks"

                        RowLayout {
                            Layout.fillWidth: true
                            Text {
                                text: root.resourcePackList.length + " Resource Packs in diesem Profil"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 13
                                font.bold: true
                                color: EzTheme.text
                            }
                            Item { Layout.fillWidth: true }
                            EzButton {
                                text: "Ordner öffnen"
                                Layout.preferredHeight: 30
                                onClicked: {
                                    if (profileController) profileController.openResourcePacksFolder()
                                }
                            }
                            EzButton {
                                text: "+ Packs durchsuchen"
                                primary: true
                                Layout.preferredHeight: 30
                                onClicked: {
                                    if (typeof window !== "undefined" && window.navigateTo) {
                                        window.navigateTo("mods")
                                    }
                                }
                            }
                        }

                        Repeater {
                            model: root.resourcePackList
                            Rectangle {
                                Layout.fillWidth: true
                                height: 58
                                radius: 10
                                scale: rpMouse.containsMouse ? 1.008 : 1.0
                                Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }
                                color: rpMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface
                                border.color: modelData.enabled ? (rpMouse.containsMouse ? EzTheme.accentLight : EzTheme.border) : "#1A1A22"
                                border.width: 1
                                opacity: modelData.enabled ? 1.0 : 0.6

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.leftMargin: 14
                                    anchors.rightMargin: 14
                                    spacing: 12

                                    // Resource Pack Icon
                                    Rectangle {
                                        Layout.preferredWidth: 38
                                        Layout.preferredHeight: 38
                                        radius: 6
                                        color: EzTheme.surface3
                                        clip: true

                                        Image {
                                            id: rpPackImg
                                            anchors.fill: parent
                                            source: modelData.icon_url || ""
                                            fillMode: Image.PreserveAspectCrop
                                            visible: status === Image.Ready
                                        }

                                        Image {
                                            visible: rpPackImg.status !== Image.Ready
                                            source: "icons/box.svg"
                                            width: 18; height: 18
                                            fillMode: Image.PreserveAspectFit
                                            anchors.centerIn: parent
                                        }
                                    }

                                    ColumnLayout {
                                        Layout.fillWidth: true
                                        spacing: 2
                                        Text {
                                            text: modelData.name
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 13
                                            font.bold: true
                                            color: EzTheme.text
                                        }
                                        Text {
                                            text: modelData.filename
                                            font.family: "Consolas, monospace"
                                            font.pixelSize: 10
                                            color: EzTheme.textMuted
                                        }
                                    }

                                    Rectangle {
                                        Layout.preferredWidth: 36
                                        Layout.preferredHeight: 20
                                        radius: 10
                                        color: modelData.enabled ? EzTheme.accent : EzTheme.surface3
                                        border.color: modelData.enabled ? EzTheme.accent : EzTheme.borderLight
                                        border.width: 1

                                        Rectangle {
                                            width: 14
                                            height: 14
                                            radius: 7
                                            color: "#FFFFFF"
                                            anchors.verticalCenter: parent.verticalCenter
                                            x: modelData.enabled ? parent.width - width - 3 : 3
                                            Behavior on x { NumberAnimation { duration: 120 } }
                                        }

                                        MouseArea {
                                            anchors.fill: parent
                                            cursorShape: Qt.PointingHandCursor
                                            onClicked: {
                                                if (profileController) profileController.toggleResourcePack(modelData.filename)
                                            }
                                        }
                                    }
                                }

                                MouseArea {
                                    id: rpMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    z: -1
                                }
                            }
                        }

                        // Empty State
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 120
                            radius: 10
                            color: EzTheme.surface
                            border.color: EzTheme.border
                            border.width: 1
                            visible: root.resourcePackList.length === 0

                            ColumnLayout {
                                anchors.centerIn: parent
                                spacing: 8
                                Image {
                                    source: "icons/box.svg"
                                    Layout.preferredWidth: 24; Layout.preferredHeight: 24
                                    Layout.alignment: Qt.AlignHCenter
                                    opacity: 0.5
                                }
                                Text {
                                    text: "Noch keine Resource Packs in diesem Profil installiert."
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 12
                                    color: EzTheme.textMuted
                                    Layout.alignment: Qt.AlignHCenter
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────
                    // TAB 5: EINSTELLUNGEN (SETTINGS)
                    // ──────────────────────────────────────────
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 14
                        visible: root.selectedTab === "settings"

                        // Card 1: Minecraft Settings Transfer
                        EzSurface {
                            Layout.fillWidth: true
                            implicitHeight: 90

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 16
                                spacing: 16

                                Rectangle {
                                    Layout.preferredWidth: 42
                                    Layout.preferredHeight: 42
                                    radius: 8
                                    color: EzTheme.surface3
                                    Image { source: "icons/copy.svg"; width: 20; height: 20; anchors.centerIn: parent; fillMode: Image.PreserveAspectFit }
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 3
                                    Text {
                                        text: "Minecraft-Einstellungen übertragen"
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 13
                                        font.bold: true
                                        color: EzTheme.text
                                    }
                                    Text {
                                        text: "Kopiere Tastenbelegungen, Video- & Audio-Optionen, Shader & Sodium-Settings aus einem anderen Profil."
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.textMuted
                                    }
                                }

                                EzButton {
                                    text: "Settings übertragen…"
                                    primary: true
                                    Layout.preferredHeight: 34
                                    onClicked: copySettingsModal.open()
                                }
                            }
                        }

                        // Card 2: Speicherort
                        EzSurface {
                            Layout.fillWidth: true
                            implicitHeight: 80

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 16
                                spacing: 16

                                Rectangle {
                                    Layout.preferredWidth: 42
                                    Layout.preferredHeight: 42
                                    radius: 8
                                    color: EzTheme.surface3
                                    Image { source: "icons/folder.svg"; width: 20; height: 20; anchors.centerIn: parent; fillMode: Image.PreserveAspectFit }
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: "Speicherort auf Festplatte"; font.family: EzTheme.mcFontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                                    Text { text: root.inspectedGameDir; font.family: "Consolas, monospace"; font.pixelSize: 10; color: EzTheme.textMuted; elide: Text.ElideMiddle; Layout.fillWidth: true }
                                }

                                EzButton {
                                    text: "Ordner öffnen"
                                    Layout.preferredHeight: 34
                                    onClicked: {
                                        if (profileController) profileController.openFolder("")
                                    }
                                }
                            }
                        }

                        // Card 3: Danger Zone (Profil Löschen)
                        EzSurface {
                            Layout.fillWidth: true
                            implicitHeight: 80

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 16
                                spacing: 16

                                Rectangle {
                                    Layout.preferredWidth: 42
                                    Layout.preferredHeight: 42
                                    radius: 8
                                    color: "#301217"
                                    Image { source: "icons/trash.svg"; width: 20; height: 20; anchors.centerIn: parent; fillMode: Image.PreserveAspectFit }
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: "Profil löschen"; font.family: EzTheme.mcFontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.danger }
                                    Text { text: "Entfernt dieses Profil und alle darin gespeicherten Mods unwiderruflich."; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                                }

                                Rectangle {
                                    Layout.preferredWidth: 100
                                    Layout.preferredHeight: 34
                                    radius: 6
                                    color: delBtnM.containsMouse ? "#5C1D24" : "#3B1217"
                                    border.color: EzTheme.danger
                                    border.width: 1
                                    Behavior on color { ColorAnimation { duration: 100 } }

                                    Text {
                                        text: "Löschen"
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 11
                                        font.bold: true
                                        color: "#FFAAA8"
                                        anchors.centerIn: parent
                                    }

                                    MouseArea {
                                        id: delBtnM
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        cursorShape: Qt.PointingHandCursor
                                        onClicked: {
                                            if (profileController) {
                                                profileController.deleteProfile(root.inspectedId)
                                                if (typeof window !== "undefined" && window.navigateTo) {
                                                    window.navigateTo("profiles")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Item { height: 24 }
                }
            }
        }
    }

    // ==========================================
    // COPY SETTINGS MODAL POPUP
    // ==========================================
    Dialog {
        id: copySettingsModal
        anchors.centerIn: parent
        width: 440
        height: 240
        modal: true
        padding: 0
        closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutside

        background: Rectangle {
            color: EzTheme.surface
            radius: 12
            border.color: EzTheme.borderLight
            border.width: 1
        }

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 20
            spacing: 12

            Text {
                text: "Minecraft-Einstellungen übertragen"
                font.family: EzTheme.mcFontFamily
                font.pixelSize: 14
                font.bold: true
                color: EzTheme.text
            }

            Text {
                text: "Wähle das Quellprofil aus, dessen Optionen (options.txt, Sodium, Shader, Keybinds) nach '" + root.inspectedName + "' kopiert werden sollen:"
                font.family: EzTheme.fontFamily
                font.pixelSize: 11
                color: EzTheme.textMuted
                wrapMode: Text.WordWrap
                Layout.fillWidth: true
            }

            EzComboBox {
                id: sourceProfileCombo
                Layout.fillWidth: true
                Layout.preferredHeight: 36
                model: {
                    var list = (profileController && profileController.allProfilesList) ? profileController.allProfilesList : []
                    var filtered = []
                    for (var i = 0; i < list.length; i++) {
                        if (list[i].id !== root.inspectedId) {
                            filtered.push(list[i].name + " (" + list[i].version + ")")
                        }
                    }
                    return filtered.length > 0 ? filtered : ["Kein anderes Profil verfügbar"]
                }
            }

            Item { Layout.fillHeight: true }

            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                Item { Layout.fillWidth: true }

                EzButton {
                    text: "Abbrechen"
                    Layout.preferredHeight: 34
                    onClicked: copySettingsModal.close()
                }

                EzButton {
                    text: "Jetzt übertragen"
                    primary: true
                    Layout.preferredHeight: 34
                    onClicked: {
                        var list = (profileController && profileController.allProfilesList) ? profileController.allProfilesList : []
                        var targetSrcId = ""
                        var count = 0
                        for (var i = 0; i < list.length; i++) {
                            if (list[i].id !== root.inspectedId) {
                                if (count === sourceProfileCombo.currentIndex) {
                                    targetSrcId = list[i].id
                                    break
                                }
                                count++
                            }
                        }
                        if (targetSrcId && profileController) {
                            profileController.copyMinecraftSettings(targetSrcId, root.inspectedId)
                        }
                        copySettingsModal.close()
                    }
                }
            }
        }
    }
}
