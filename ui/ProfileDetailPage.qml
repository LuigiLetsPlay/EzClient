import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root

    property string selectedTab: "overview"

    readonly property string activeName: typeof profileController !== "undefined" && profileController ? profileController.activeName : ""
    readonly property string activeVersion: typeof profileController !== "undefined" && profileController ? profileController.activeVersion : "1.21.4"
    readonly property string activeLoader: typeof profileController !== "undefined" && profileController ? profileController.activeLoader : "Fabric"
    readonly property int activeModsCount: typeof profileController !== "undefined" && profileController ? profileController.activeModsCount : 0

    ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // ==========================================
        // GAME HERO HEADER
        // ==========================================
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 88
            color: EzTheme.surface
            border.color: EzTheme.border
            border.width: 1

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 24
                anchors.rightMargin: 24
                spacing: 16

                // Profile Avatar/Icon (48x48 rounded)
                Rectangle {
                    Layout.preferredWidth: 48
                    Layout.preferredHeight: 48
                    radius: 10
                    color: EzTheme.surface2
                    border.color: EzTheme.borderLight
                    border.width: 1

                    Text {
                        text: root.activeName.length >= 2 ? root.activeName.substring(0, 2).toUpperCase() : (root.activeName.length === 1 ? root.activeName.toUpperCase() : "EZ")
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 15
                        font.bold: true
                        color: EzTheme.accentLight
                        anchors.centerIn: parent
                    }
                }

                // Profile Title & Meta
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 3

                    Text {
                        text: EzI18n.t("detail_back_to_profiles", "← Zurück zu Profilen")
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 10
                        font.bold: true
                        color: backMouse.containsMouse ? EzTheme.accentLight : EzTheme.textMuted

                        Behavior on color { ColorAnimation { duration: 90 } }

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

                    Text {
                        text: root.activeName
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 18
                        font.bold: true
                        color: EzTheme.text
                    }

                    RowLayout {
                        spacing: 8
                        Text {
                            text: "Minecraft " + root.activeVersion + "  ·  " + root.activeLoader + "  ·  " + root.activeModsCount + " Mods"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            color: EzTheme.textMuted
                        }
                        Rectangle { width: 5; height: 5; radius: 3; color: EzTheme.accent }
                        Text {
                            text: EzI18n.t("home_ready_to_play", "Spielbereit")
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            font.bold: true
                            color: EzTheme.accentLight
                        }
                    }
                }

                // Right Action Cluster
                RowLayout {
                    spacing: 8

                    // Play Button with scale on hover
                    Rectangle {
                        Layout.preferredWidth: 120
                        Layout.preferredHeight: 38
                        radius: EzTheme.radiusSm
                        scale: playBtnMouse.containsMouse ? 1.03 : 1.0
                        color: playBtnMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent
                        Behavior on color { ColorAnimation { duration: 100 } }
                        Behavior on scale { NumberAnimation { duration: 100 } }

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 6
                            Text { text: "▶"; font.pixelSize: 11; color: "#000000" }
                            Text { text: EzI18n.t("home_play", "SPIELEN"); font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: "#000000" }
                        }

                        MouseArea {
                            id: playBtnMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: profileController.launchActiveProfile()
                        }
                    }

                    EzButton {
                        text: EzI18n.t("detail_folder_btn", "Ordner")
                        Layout.preferredWidth: 80
                        Layout.preferredHeight: 38
                        onClicked: profileController.openFolder("")
                    }
                }
            }
        }

        // ==========================================
        // TAB BAR WITH SLIDING UNDERLINE
        // ==========================================
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 36
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
                        { tabId: "overview", label: EzI18n.t("detail_tab_overview", "Übersicht") },
                        { tabId: "mods", label: EzI18n.t("detail_tab_mods", "Mods") + " (" + root.activeModsCount + ")" },
                        { tabId: "settings", label: EzI18n.t("detail_tab_settings", "Einstellungen") }
                    ]

                    Rectangle {
                        width: tabText.implicitWidth + 24
                        height: 36
                        color: "transparent"

                        Text {
                            id: tabText
                            text: modelData.label
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            font.bold: root.selectedTab === modelData.tabId
                            color: root.selectedTab === modelData.tabId ? EzTheme.text : (tabMouse.containsMouse ? EzTheme.textSecondary : EzTheme.textMuted)
                            anchors.centerIn: parent
                            Behavior on color { ColorAnimation { duration: 100 } }
                        }

                        Rectangle {
                            height: 2
                            anchors.left: parent.left
                            anchors.right: parent.right
                            anchors.bottom: parent.bottom
                            color: EzTheme.accent
                            visible: root.selectedTab === modelData.tabId
                        }

                        MouseArea {
                            id: tabMouse
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
        // MAIN CONTENT AREA
        // ==========================================
        ScrollView {
            id: mainScrollView
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true
            contentWidth: availableWidth

            ColumnLayout {
                width: mainScrollView.availableWidth - 48
                x: 24
                spacing: 16

                Item { height: 6 }

                // ──────────────────────────────────────────
                // TAB 1: OVERVIEW
                // ──────────────────────────────────────────
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 16
                    visible: root.selectedTab === "overview"

                    // SECTION 1: ALLGEMEIN
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 6

                        Text {
                            text: EzI18n.t("detail_sec_general", "ALLGEMEIN")
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            font.bold: true
                            color: EzTheme.textSubtle
                            font.letterSpacing: 0.8
                        }

                        EzSurface {
                            Layout.fillWidth: true
                            implicitHeight: 185

                            ColumnLayout {
                                anchors.fill: parent
                                anchors.margins: 16
                                spacing: 0

                                // Row 1: Minecraft
                                RowLayout {
                                    Layout.fillWidth: true
                                    Text { text: EzI18n.t("profiles_version_label", "Minecraft Version"); font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; Layout.fillWidth: true }
                                    Text { text: root.activeVersion; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.cyan }
                                }
                                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                                // Row 2: Loader
                                RowLayout {
                                    Layout.fillWidth: true
                                    Text { text: EzI18n.t("profiles_loader_label", "Mod-Loader"); font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; Layout.fillWidth: true }
                                    Text { text: root.activeLoader; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.accentLight }
                                }
                                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                                // Row 3: Java
                                RowLayout {
                                    Layout.fillWidth: true
                                    Text { text: EzI18n.t("detail_java_runtime", "Java Runtime"); font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; Layout.fillWidth: true }
                                    Text { text: profileController ? profileController.javaRuntime : ""; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
                                }
                                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                                // Row 4: Mods
                                RowLayout {
                                    Layout.fillWidth: true
                                    Text { text: EzI18n.t("detail_installed_mods", "Installierte Mods"); font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; Layout.fillWidth: true }
                                    Text { text: root.activeModsCount + " " + EzI18n.t("detail_mods_active", "Mods aktiv"); font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                }
                            }
                        }
                    }

                    // SECTION 2: PERFORMANCE
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 6

                        Text {
                            text: EzI18n.t("detail_sec_optimizations", "OPTIMIERUNGEN")
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            font.bold: true
                            color: EzTheme.textSubtle
                            font.letterSpacing: 0.8
                        }

                        EzSurface {
                            Layout.fillWidth: true
                            implicitHeight: 120

                            ColumnLayout {
                                anchors.fill: parent
                                anchors.margins: 16
                                spacing: 10

                                GridLayout {
                                    Layout.fillWidth: true
                                    columns: 2
                                    rowSpacing: 10
                                    columnSpacing: 40

                                    RowLayout {
                                        spacing: 8
                                        Image { source: "icons/check.svg"; Layout.preferredWidth: 12; Layout.preferredHeight: 12 }
                                        Text { text: "Sodium / Embeddium"; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                        Text { text: EzI18n.t("detail_active", "Aktiv"); font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.accentLight }
                                    }

                                    RowLayout {
                                        spacing: 8
                                        Image { source: "icons/check.svg"; Layout.preferredWidth: 12; Layout.preferredHeight: 12 }
                                        Text { text: "Lithium / Canary"; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                        Text { text: EzI18n.t("detail_active", "Aktiv"); font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.accentLight }
                                    }

                                    RowLayout {
                                        spacing: 8
                                        Image { source: "icons/check.svg"; Layout.preferredWidth: 12; Layout.preferredHeight: 12 }
                                        Text { text: "FerriteCore (RAM)"; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                        Text { text: EzI18n.t("detail_active", "Aktiv"); font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.accentLight }
                                    }

                                    RowLayout {
                                        spacing: 8
                                        Image { source: "icons/check.svg"; Layout.preferredWidth: 12; Layout.preferredHeight: 12 }
                                        Text { text: "ImmediatelyFast (FPS)"; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                        Text { text: EzI18n.t("detail_active", "Aktiv"); font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.accentLight }
                                    }
                                }

                                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6 }

                                Text {
                                    text: EzI18n.t("detail_show_all_mods", "Alle {0} installierten Mods anzeigen →").replace("{0}", root.activeModsCount)
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: true
                                    color: EzTheme.cyan

                                    MouseArea {
                                        anchors.fill: parent
                                        cursorShape: Qt.PointingHandCursor
                                        onClicked: root.selectedTab = "mods"
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: HEALTH
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 6

                        Text {
                            text: EzI18n.t("detail_sec_health", "PROFIL STATUS")
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            font.bold: true
                            color: EzTheme.textSubtle
                            font.letterSpacing: 0.8
                        }

                        EzSurface {
                            Layout.fillWidth: true
                            implicitHeight: 74

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 16
                                spacing: 14

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 3

                                    RowLayout {
                                        spacing: 6
                                        Rectangle { width: 7; height: 7; radius: 4; color: EzTheme.accent }
                                        Text {
                                            text: EzI18n.t("detail_all_ready", "Alles einsatzbereit")
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 12
                                            font.bold: true
                                            color: EzTheme.accentLight
                                        }
                                    }

                                    Text {
                                        text: EzI18n.t("detail_configured_for", "Profil ist vollständig konfiguriert für {0} {1}.").replace("{0}", root.activeLoader).replace("{1}", root.activeVersion)
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 11
                                        color: EzTheme.textMuted
                                    }
                                }

                                EzButton {
                                    text: EzI18n.t("detail_check_btn", "Prüfen")
                                    Layout.preferredHeight: 30
                                    Layout.preferredWidth: 90
                                    onClicked: console.log("Checking updates...")
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

                    RowLayout {
                        Layout.fillWidth: true
                        Text {
                            text: root.activeModsCount + " " + EzI18n.t("detail_mods_in_profile", "Mods in diesem Profil")
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 13
                            font.bold: true
                            color: EzTheme.text
                        }
                        Item { Layout.fillWidth: true }
                        EzButton {
                            text: EzI18n.t("detail_add_mods", "Hinzufügen")
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
                        model: profileController ? profileController.modModel : null
                        Rectangle {
                            Layout.fillWidth: true
                            height: 48
                            radius: EzTheme.radiusSm
                            color: modItemMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface
                            border.color: EzTheme.border
                            border.width: 1
                            Behavior on color { ColorAnimation { duration: 90 } }

                            RowLayout {
                                anchors.fill: parent
                                anchors.leftMargin: 12
                                anchors.rightMargin: 12
                                spacing: 10

                                // Toggle
                                Rectangle {
                                    Layout.preferredWidth: 30
                                    Layout.preferredHeight: 16
                                    radius: 8
                                    color: model.enabled ? EzTheme.accent : EzTheme.surface3
                                    border.color: model.enabled ? EzTheme.accent : EzTheme.borderLight
                                    border.width: 1

                                    Rectangle {
                                        width: 10
                                        height: 10
                                        radius: 5
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

                                Text {
                                    text: model.name
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 12
                                    font.bold: true
                                    color: model.enabled ? EzTheme.text : EzTheme.textSubtle
                                    Layout.fillWidth: true
                                }

                                Text {
                                    text: model.version || "—"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    color: EzTheme.textSecondary
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
                // TAB 3: SETTINGS
                // ──────────────────────────────────────────
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 12
                    visible: root.selectedTab === "settings"

                    EzSurface {
                        Layout.fillWidth: true
                        implicitHeight: 140

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 16
                            spacing: 12

                            Text {
                                text: EzI18n.t("detail_settings_title", "PROFILEINSTELLUNGEN")
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzTheme.textSubtle
                                font.letterSpacing: 0.8
                            }

                            RowLayout {
                                Layout.fillWidth: true
                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: EzI18n.t("detail_profile_dir", "Speicherort des Profils"); font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                                    Text { text: profileController ? profileController.activeGameDir : ""; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted; elide: Text.ElideMiddle; Layout.fillWidth: true }
                                }
                                EzButton {
                                    text: EzI18n.t("detail_open_btn", "Öffnen")
                                    Layout.preferredHeight: 30
                                    onClicked: profileController.openFolder("")
                                }
                            }

                            Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                            RowLayout {
                                Layout.fillWidth: true
                                Text { text: EzI18n.t("detail_delete_profile", "Profil löschen"); font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.danger; Layout.fillWidth: true }
                                EzButton {
                                    text: EzI18n.t("detail_delete_btn", "Löschen")
                                    danger: true
                                    Layout.preferredHeight: 30
                                    onClicked: {
                                        if (profileController) {
                                            profileController.deleteProfile(profileController.activeId)
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
