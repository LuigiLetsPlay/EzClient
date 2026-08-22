import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root

    // Inspected mod state
    property var currentInspectedMod: null
    property bool inspectModalOpen: false

    // Version switch modal state
    property string versionSwitchModId: ""
    property string versionSwitchModName: ""
    property string versionSwitchCurrentVer: ""
    property bool versionModalOpen: false

    // Dependency warning state
    property var pendingDeleteMod: null
    property var pendingDeleteDeps: []

    // Filter status state: "all", "enabled", "disabled", "performance"
    property string filterStatus: "all"
    property bool showCoreMods: false
    readonly property bool hasUpdates: profileController && profileController.hasModUpdates

    // Background click handler to deselect / defocus search input
    MouseArea {
        anchors.fill: parent
        onClicked: root.forceActiveFocus()
    }

    // ─────────────────────────────────────────
    // LIVE MODRINTH MOD DETAIL INSPECTION MODAL
    // ─────────────────────────────────────────
    Rectangle {
        id: inspectModal
        anchors.fill: parent
        color: "#B5000000"
        visible: opacity > 0.001
        opacity: root.inspectModalOpen ? 1.0 : 0.0
        z: 60

        Behavior on opacity { NumberAnimation { duration: 150; easing.type: Easing.OutQuad } }

        MouseArea {
            anchors.fill: parent
            onClicked: root.inspectModalOpen = false
        }

        Rectangle {
            anchors.centerIn: parent
            width: Math.min(root.width - 64, 580)
            height: Math.min(root.height - 64, 520)
            radius: EzTheme.radius
            color: EzTheme.surface
            border.color: EzTheme.borderLight
            border.width: 1
            scale: root.inspectModalOpen ? 1.0 : 0.95

            Behavior on scale { NumberAnimation { duration: 150; easing.type: Easing.OutCubic } }

            MouseArea {
                anchors.fill: parent
            }

            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 20
                spacing: 12

                // Header with icon, title, author & close
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 12

                    Rectangle {
                        width: 48; height: 48; radius: 10
                        color: EzTheme.surface3
                        border.color: EzTheme.borderLight
                        border.width: 1
                        clip: true

                        Image {
                            anchors.fill: parent
                            source: (modrinthController.selectedMod && modrinthController.selectedMod.icon_url)
                                    ? modrinthController.selectedMod.icon_url
                                    : ((root.currentInspectedMod && root.currentInspectedMod.iconUrl) ? root.currentInspectedMod.iconUrl : "")
                            fillMode: Image.PreserveAspectCrop
                            visible: status === Image.Ready
                        }
                        Text {
                            visible: !root.currentInspectedMod || !root.currentInspectedMod.iconUrl
                            text: (root.currentInspectedMod && root.currentInspectedMod.name) ? root.currentInspectedMod.name.charAt(0).toUpperCase() : "M"
                            font.family: EzTheme.mcFontFamily; font.pixelSize: 16; font.bold: true
                            color: EzTheme.accentLight; anchors.centerIn: parent
                        }
                    }

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 2
                        Text {
                            text: (modrinthController.selectedMod && modrinthController.selectedMod.title)
                                  ? modrinthController.selectedMod.title
                                  : ((root.currentInspectedMod && root.currentInspectedMod.name) ? root.currentInspectedMod.name : "")
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 16
                            font.bold: true
                            color: EzTheme.text
                        }
                        Text {
                            text: "Modrinth · Autor: " + ((modrinthController.selectedMod && modrinthController.selectedMod.author)
                                  ? modrinthController.selectedMod.author
                                  : ((root.currentInspectedMod && root.currentInspectedMod.author) ? root.currentInspectedMod.author : "Community"))
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            color: EzTheme.textMuted
                        }
                    }

                    Rectangle {
                        width: 28; height: 28; radius: 6
                        color: modalCloseMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        Text { text: "✕"; font.pixelSize: 11; color: EzTheme.textMuted; anchors.centerIn: parent }
                        MouseArea {
                            id: modalCloseMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: root.inspectModalOpen = false
                        }
                    }
                }

                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                // Live Stats & Badges row
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 14

                    Rectangle {
                        height: 24
                        width: verPillText.implicitWidth + 14
                        radius: 4
                        color: EzTheme.surface2
                        border.color: EzTheme.borderLight
                        border.width: 1
                        Text {
                            id: verPillText
                            text: (root.currentInspectedMod && window.integratedMods && window.integratedMods.indexOf(root.currentInspectedMod.slug) !== -1 ? "Integriert: " : "Installiert: ") + ((root.currentInspectedMod && root.currentInspectedMod.version) ? root.currentInspectedMod.version : "Latest")
                            font.family: EzTheme.mcFontFamily; font.pixelSize: 10; font.bold: true
                            color: EzTheme.cyan; anchors.centerIn: parent
                        }
                    }

                    Rectangle {
                        height: 24
                        width: isCorePill.implicitWidth + 14
                        radius: 4
                        color: EzTheme.accentDark
                        visible: root.currentInspectedMod && (root.currentInspectedMod.slug === "fabric-api" || root.currentInspectedMod.name === "Fabric API")
                        Text {
                            id: isCorePill
                            text: "CORE API"
                            font.family: EzTheme.mcFontFamily; font.pixelSize: 9; font.bold: true
                            color: EzTheme.accentLight; anchors.centerIn: parent
                        }
                    }

                    Item { Layout.fillWidth: true }

                    Text {
                        text: (modrinthController.selectedMod && modrinthController.selectedMod.downloads)
                              ? ("⚡ " + formatNum(modrinthController.selectedMod.downloads) + " Downloads auf Modrinth")
                              : ""
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 10
                        font.bold: true
                        color: EzTheme.accentLight
                    }
                }

                // Description
                ScrollView {
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    clip: true
                    contentWidth: availableWidth

                    Text {
                        width: parent.width
                        text: (modrinthController.selectedMod && modrinthController.selectedMod.description)
                              ? modrinthController.selectedMod.description
                              : ((root.currentInspectedMod && root.currentInspectedMod.description) ? root.currentInspectedMod.description : "Lade Modrinth-Daten…")
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        color: EzTheme.textSecondary
                        wrapMode: Text.WordWrap
                    }
                }

                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                // Action buttons
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    EzButton {
                        text: "Versionen"
                        primary: true
                        mcFont: true
                        Layout.preferredHeight: 34
                        Layout.preferredWidth: 110
                        onClicked: {
                            root.inspectModalOpen = false
                            if (root.currentInspectedMod) {
                                root.versionSwitchModId = root.currentInspectedMod.slug || root.currentInspectedMod.name
                                root.versionSwitchModName = root.currentInspectedMod.name
                                root.versionSwitchCurrentVer = root.currentInspectedMod.version
                                var actVer = profileController ? profileController.activeVersion : ""
                                modrinthController.fetchInstalledModVersions(root.versionSwitchModId, actVer)
                                root.versionModalOpen = true
                            }
                        }
                    }

                    Item { Layout.fillWidth: true }

                    EzButton {
                        text: "Schließen"
                        Layout.preferredHeight: 34
                        Layout.preferredWidth: 90
                        onClicked: root.inspectModalOpen = false
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // LIVE MODRINTH VERSION SWITCHER MODAL (With Release/Beta Filter)
    // ─────────────────────────────────────────
    Rectangle {
        id: versionModal
        anchors.fill: parent
        color: "#B5000000"
        visible: opacity > 0.001
        opacity: root.versionModalOpen ? 1.0 : 0.0
        z: 70

        Behavior on opacity { NumberAnimation { duration: 150; easing.type: Easing.OutQuad } }

        MouseArea {
            anchors.fill: parent
            onClicked: root.versionModalOpen = false
        }

        Rectangle {
            anchors.centerIn: parent
            width: Math.min(root.width - 64, 520)
            height: Math.min(root.height - 64, 460)
            radius: EzTheme.radius
            color: EzTheme.surface
            border.color: EzTheme.borderLight
            border.width: 1

            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 20
                spacing: 12

                RowLayout {
                    Layout.fillWidth: true
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 2
                        Text {
                            text: "Version wechseln: " + root.versionSwitchModName
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 15
                            font.bold: true
                            color: EzTheme.text
                        }
                        Text {
                            text: "Wähle ein offizielles Release aus der Modrinth API:"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            color: EzTheme.textMuted
                        }
                    }

                    Rectangle {
                        width: 28; height: 28; radius: 6
                        color: vCloseMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        Text { text: "✕"; font.pixelSize: 11; color: EzTheme.textMuted; anchors.centerIn: parent }
                        MouseArea {
                            id: vCloseMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: root.versionModalOpen = false
                        }
                    }
                }

                // Version filter row (Release, Beta, Alle)
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 6

                    Text { text: "Filter:"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }

                    Repeater {
                        model: [
                            { id: "release", label: "Release" },
                            { id: "beta",    label: "Beta" },
                            { id: "all",     label: "Alle" }
                        ]

                        Rectangle {
                            height: 22
                            width: vFilText.implicitWidth + 14
                            radius: 4
                            color: (modrinthController && modrinthController.versionTypeFilter === modelData.id) ? EzTheme.accent : (vfMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                            border.color: (modrinthController && modrinthController.versionTypeFilter === modelData.id) ? EzTheme.accentLight : EzTheme.border
                            border.width: 1

                            Text {
                                id: vFilText
                                text: modelData.label
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 9
                                font.bold: true
                                color: (modrinthController && modrinthController.versionTypeFilter === modelData.id) ? "#000000" : EzTheme.text
                                anchors.centerIn: parent
                            }

                            MouseArea {
                                id: vfMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: modrinthController.setVersionTypeFilter(modelData.id)
                            }
                        }
                    }
                }

                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                // Live Modrinth Version list
                ListView {
                    id: vModrinthList
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    clip: true
                    spacing: 6
                    model: modrinthController ? modrinthController.filteredVersions : []

                    ScrollBar.vertical: ScrollBar {
                        id: verScrollBar
                        policy: ScrollBar.AsNeeded
                        visible: vModrinthList.contentHeight > vModrinthList.height
                        contentItem: Rectangle {
                            implicitWidth: 5
                            radius: 3
                            color: EzTheme.borderLight
                            visible: verScrollBar.size < 0.999 && (vModrinthList.contentHeight > vModrinthList.height)
                        }
                    }

                    delegate: Rectangle {
                        width: vModrinthList.width
                        height: 44
                        radius: 6
                        color: vItemMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface
                        border.color: modelData.version_number === root.versionSwitchCurrentVer ? EzTheme.accent : EzTheme.border
                        border.width: 1

                        RowLayout {
                            anchors.fill: parent
                            anchors.leftMargin: 12
                            anchors.rightMargin: 12
                            spacing: 10

                            ColumnLayout {
                                Layout.fillWidth: true
                                spacing: 2
                                Text {
                                    text: modelData.name || modelData.version_number || "Release"
                                    font.family: EzTheme.mcFontFamily
                                    font.pixelSize: 11
                                    font.bold: true
                                    color: EzTheme.text
                                    elide: Text.ElideRight
                                    Layout.fillWidth: true
                                }
                                Text {
                                    text: (modelData.game_versions ? modelData.game_versions.join(", ") : "") + " · " + (modelData.loaders ? modelData.loaders.join(", ") : "")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 9
                                    color: EzTheme.textMuted
                                    elide: Text.ElideRight
                                    Layout.fillWidth: true
                                }
                            }

                            Rectangle {
                                height: 26
                                width: 72
                                radius: 4
                                color: modelData.version_number === root.versionSwitchCurrentVer ? EzTheme.accentDark : (vActMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent)
                                border.color: EzTheme.accent
                                border.width: 1

                                Text {
                                    text: modelData.version_number === root.versionSwitchCurrentVer ? "✓ Aktiv" : "Wählen"
                                    font.family: EzTheme.mcFontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: modelData.version_number === root.versionSwitchCurrentVer ? EzTheme.accentLight : "#000000"
                                    anchors.centerIn: parent
                                }

                                MouseArea {
                                    id: vActMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        if (modelData.version_number !== root.versionSwitchCurrentVer) {
                                            profileController.updateModVersion(root.versionSwitchModId, modelData.version_number)
                                            root.versionSwitchCurrentVer = modelData.version_number
                                        }
                                        root.versionModalOpen = false
                                    }
                                }
                            }
                        }

                        MouseArea {
                            id: vItemMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            z: -1
                        }
                    }
                }

                // Empty state fallback
                Rectangle {
                    Layout.fillWidth: true
                    height: 50
                    color: "transparent"
                    visible: !modrinthController || modrinthController.filteredVersions.length === 0

                    Text {
                        text: "Keine Versionen im aktuellen Filter gefunden"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                        anchors.centerIn: parent
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // DEPENDENCY WARNING MODAL DIALOG (QoL!)
    // ─────────────────────────────────────────
    Popup {
        id: depWarningModal
        anchors.centerIn: parent
        width: 420
        height: 220
        modal: true
        focus: true
        closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutside

        background: Rectangle {
            radius: 12
            color: "#181112"
            border.color: EzTheme.danger
            border.width: 1.5
        }

        contentItem: ColumnLayout {
            anchors.fill: parent
            anchors.margins: 18
            spacing: 10

            RowLayout {
                spacing: 10
                Text { text: "⚠️"; font.pixelSize: 22 }
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 2
                    Text { text: "Abhängigkeits-Warnung"; font.family: EzTheme.mcFontFamily; font.pixelSize: 15; font.bold: true; color: EzTheme.danger }
                    Text { text: "Wichtige Kern-Modifikation"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                }
            }

            Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

            Text {
                text: "Warnung: Die Mod '" + (root.pendingDeleteMod ? root.pendingDeleteMod.name : "") + "' wird von folgenden installierten Mods benötigt: " + root.pendingDeleteDeps.join(", ") + ".\n\nDas Löschen kann zu Spielabstürzen führen."
                font.family: EzTheme.fontFamily
                font.pixelSize: 11
                color: EzTheme.text
                wrapMode: Text.WordWrap
                Layout.fillWidth: true
                lineHeight: 1.3
            }

            Item { Layout.fillHeight: true }

            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                EzButton {
                    text: "Abbrechen"
                    Layout.fillWidth: true
                    Layout.preferredHeight: 34
                    onClicked: depWarningModal.close()
                }

                EzButton {
                    text: "Trotzdem löschen"
                    danger: true
                    mcFont: true
                    Layout.fillWidth: true
                    Layout.preferredHeight: 34
                    onClicked: {
                        if (root.pendingDeleteMod) {
                            profileController.uninstallMod(root.pendingDeleteMod.slug || root.pendingDeleteMod.name, root.pendingDeleteMod.name)
                        }
                        depWarningModal.close()
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // MAIN CONTENT
    // ─────────────────────────────────────────
    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 20
        spacing: 12

        // ─── Header & Top Controls ───
        RowLayout {
            Layout.fillWidth: true
            spacing: 12

            ColumnLayout {
                Layout.fillWidth: true
                spacing: 2
                Text {
                    text: "INSTALLIERTE MODS"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 16
                    font.bold: true
                    color: EzTheme.text
                }
                Text {
                    text: (profileController ? profileController.activeModsCount : 0) + " Mods im aktiven Profil (" + (profileController ? profileController.activeName : "") + ")"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    color: EzTheme.textMuted
                }
            }

            // Add mods button taking user to Modrinth Store
            EzButton {
                text: "Hinzufügen"
                primary: true
                Layout.preferredHeight: 34
                Layout.preferredWidth: 120
                onClicked: {
                    if (typeof window !== "undefined" && window.navigateTo) {
                        window.navigateTo("modrinth")
                    }
                }
            }
        }

        // ─── Search Bar + Filter Pills ───
        RowLayout {
            Layout.fillWidth: true
            spacing: 10

            // Search Bar
            Rectangle {
                Layout.fillWidth: true
                Layout.preferredHeight: 36
                color: EzTheme.surface
                border.color: modSearch.activeFocus ? EzTheme.accent : EzTheme.border
                border.width: 1
                radius: EzTheme.radiusSm

                Behavior on border.color { ColorAnimation { duration: 120 } }

                RowLayout {
                    anchors.fill: parent
                    anchors.leftMargin: 12
                    anchors.rightMargin: 12
                    spacing: 8

                    Image {
                        source: "icons/search.svg"
                        Layout.preferredWidth: 13
                        Layout.preferredHeight: 13
                        fillMode: Image.PreserveAspectFit
                    }

                    TextInput {
                        id: modSearch
                        Layout.fillWidth: true
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        color: EzTheme.text
                        selectByMouse: true
                        verticalAlignment: TextInput.AlignVCenter

                        Text {
                            text: EzI18n.t("mods_search_placeholder", "Installierte Mods durchsuchen…")
                            font: parent.font
                            color: EzTheme.textSubtle
                            visible: parent.text === ""
                            anchors.verticalCenter: parent.verticalCenter
                        }
                    }

                    Text {
                        visible: modSearch.text !== ""
                        text: "✕"
                        font.pixelSize: 10
                        color: EzTheme.textMuted
                        MouseArea {
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: modSearch.text = ""
                        }
                    }
                }
            }

            // Quick Status Filter Pills: Alle | ⚡ EzClient Mods | Aktiv | Inaktiv
            Row {
                spacing: 6

                Repeater {
                    model: [
                        { id: "all",         label: EzI18n.t("mods_filter_all", "Alle") },
                        { id: "performance", label: EzI18n.t("mods_filter_perf", "⚡ EzClient Mods") },
                        { id: "enabled",     label: EzI18n.t("mods_filter_enabled", "Aktiv") },
                        { id: "disabled",    label: EzI18n.t("mods_filter_disabled", "Inaktiv") }
                    ]

                    Rectangle {
                        height: 36
                        width: statLabel.implicitWidth + 18
                        radius: EzTheme.radiusSm
                        color: root.filterStatus === modelData.id ? EzTheme.surfaceActive : (stMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                        border.color: root.filterStatus === modelData.id ? EzTheme.accent : EzTheme.border
                        border.width: 1

                        Text {
                            id: statLabel
                            text: modelData.label
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 11
                            font.bold: true
                            color: root.filterStatus === modelData.id ? EzTheme.accentLight : EzTheme.textSecondary
                            anchors.centerIn: parent
                        }

                        MouseArea {
                            id: stMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: root.filterStatus = modelData.id
                        }
                    }
                }
            }

            Item { Layout.fillWidth: true }

            // Toggle for Core Mods (only visible in "all" or "enabled"/"disabled" views)
            RowLayout {
                spacing: 8
                visible: root.filterStatus !== "performance"
                
                EzButton {
                    text: "Alle updaten"
                    primary: true
                    mcFont: true
                    visible: root.hasUpdates
                    Layout.preferredHeight: 28
                    Layout.preferredWidth: 112
                    onClicked: profileController.updateAllMods()
                }

                Text { text: "Integrierte ausblenden"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textMuted }
                
                Rectangle {
                    width: 34; height: 18; radius: 9
                    color: !root.showCoreMods ? EzTheme.accent : EzTheme.surface3
                    border.color: !root.showCoreMods ? EzTheme.accentLight : EzTheme.border
                    border.width: 1
                    
                    Rectangle {
                        width: 14; height: 14; radius: 7
                        color: "#000000"
                        anchors.verticalCenter: parent.verticalCenter
                        x: !root.showCoreMods ? parent.width - width - 2 : 2
                        Behavior on x { NumberAnimation { duration: 150 } }
                    }
                    MouseArea {
                        anchors.fill: parent
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.showCoreMods = !root.showCoreMods
                    }
                }
            }
        }

        // ─── Column headers ───
        Rectangle {
            Layout.fillWidth: true
            height: 1
            color: EzTheme.border
            opacity: 0.6
            visible: profileController && profileController.activeModsCount > 0
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.leftMargin: 12
            Layout.rightMargin: 12
            spacing: 0
            visible: profileController && profileController.activeModsCount > 0

            Text { text: EzI18n.t("mods_col_status", "Status & Version"); font.family: EzTheme.fontFamily; font.pixelSize: 10; font.bold: true; color: EzTheme.textSubtle; Layout.preferredWidth: 160 }
            Text { text: EzI18n.t("mods_col_mod", "Mod / Modrinth Beschreibung (Klicken für Details)"); font.family: EzTheme.fontFamily; font.pixelSize: 10; font.bold: true; color: EzTheme.textSubtle; Layout.fillWidth: true }
            Text { text: EzI18n.t("mods_col_author", "Autor"); font.family: EzTheme.fontFamily; font.pixelSize: 10; font.bold: true; color: EzTheme.textSubtle; Layout.preferredWidth: 120 }
            Item { Layout.preferredWidth: 40 }
        }

        Rectangle {
            Layout.fillWidth: true
            height: 1
            color: EzTheme.border
            opacity: 0.6
            visible: profileController && profileController.activeModsCount > 0
        }

        // ─── Empty State ───
        Rectangle {
            Layout.fillWidth: true
            Layout.fillHeight: true
            color: EzTheme.surface
            radius: EzTheme.radius
            border.color: EzTheme.border
            border.width: 1
            visible: !profileController || profileController.activeModsCount === 0

            ColumnLayout {
                anchors.centerIn: parent
                spacing: 12

                Text {
                    text: "📦"
                    font.pixelSize: 36
                    Layout.alignment: Qt.AlignHCenter
                }

                Text {
                    text: EzI18n.t("mods_no_installed", "Keine Mods installiert")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 15
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                }

                Text {
                    text: EzI18n.t("mods_empty_hint", "Füge Mods aus dem Modrinth Store hinzu, um Leistung und Aussehen anzupassen.")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    color: EzTheme.textMuted
                    Layout.alignment: Qt.AlignHCenter
                }

                Item { height: 4 }

                EzButton {
                    text: EzI18n.t("detail_add_mods", "Hinzufügen")
                    primary: true
                    Layout.alignment: Qt.AlignHCenter
                    Layout.preferredHeight: 36
                    Layout.preferredWidth: 130
                    onClicked: {
                        if (typeof window !== "undefined" && window.navigateTo) {
                            window.navigateTo("modrinth")
                        }
                    }
                }
            }
        }

        // ─── Mod List View ───
        ListView {
            id: modList
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true
            spacing: 4
            visible: profileController && profileController.activeModsCount > 0
            model: profileController ? profileController.modModel : null

            ScrollBar.vertical: ScrollBar {
                id: mScrollBar
                policy: ScrollBar.AsNeeded
                visible: modList.contentHeight > modList.height
                contentItem: Rectangle {
                    implicitWidth: 5
                    radius: 3
                    color: EzTheme.borderLight
                }
            }

            delegate: Rectangle {
                id: modItem
                width: modList.width - (modList.contentHeight > modList.height ? 10 : 0)
                radius: 6

                readonly property bool isFabricApi: model.slug === "fabric-api" || model.name === "Fabric API"
                readonly property bool isPerformanceMod: {
                    var s = (model.slug || model.projectId || "").toLowerCase()
                    return window.integratedMods && window.integratedMods.indexOf(s) !== -1
                }
                readonly property bool matchesSearch: modSearch.text === "" ||
                    (model.name && model.name.toLowerCase().indexOf(modSearch.text.toLowerCase()) !== -1) ||
                    (model.description && model.description.toLowerCase().indexOf(modSearch.text.toLowerCase()) !== -1)
                readonly property bool matchesStatus: root.filterStatus === "all" ||
                    (root.filterStatus === "performance" && modItem.isPerformanceMod) ||
                    (root.filterStatus === "enabled" && model.enabled) ||
                    (root.filterStatus === "disabled" && !model.enabled)
                readonly property string modUpdateVersion: {
                    var updates = profileController ? profileController.modUpdates : ({})
                    return updates[model.projectId || model.slug || model.name] || ""
                }
                readonly property bool updateAvailable: modUpdateVersion !== ""

                visible: matchesSearch && matchesStatus && (!modItem.isPerformanceMod || root.showCoreMods || root.filterStatus === "performance")
                height: visible ? 52 : 0

                color: (modItem.isFabricApi || model.enabled)
                       ? (rowMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                       : (rowMouse.containsMouse ? "#14141A" : "#0E0E12")
                border.color: (modItem.isFabricApi || model.enabled)
                              ? (rowMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                              : "#161620"
                border.width: 1

                Behavior on color { ColorAnimation { duration: 100 } }

                RowLayout {
                    anchors.fill: parent
                    anchors.leftMargin: 12
                    anchors.rightMargin: 12
                    spacing: 12

                    // Toggle Switch (Checkbox for mod active state)
                    Rectangle {
                        Layout.preferredWidth: 36
                        Layout.preferredHeight: 20
                        radius: 10
                        color: modItem.isFabricApi ? EzTheme.accentDark : (model.enabled ? EzTheme.accent : EzTheme.surface3)
                        border.color: modItem.isFabricApi ? EzTheme.accent : (model.enabled ? EzTheme.accentLight : EzTheme.border)
                        border.width: 1

                        Behavior on color { ColorAnimation { duration: 100 } }

                        Rectangle {
                            width: 14; height: 14; radius: 7
                            color: modItem.isFabricApi ? EzTheme.accentLight : (model.enabled ? "#000000" : EzTheme.textMuted)
                            x: (modItem.isFabricApi || model.enabled) ? 19 : 3
                            anchors.verticalCenter: parent.verticalCenter
                            Behavior on x { NumberAnimation { duration: 100 } }

                            Text {
                                visible: modItem.isFabricApi
                                text: "🔒"
                                font.pixelSize: 8
                                anchors.centerIn: parent
                            }
                        }

                        MouseArea {
                            anchors.fill: parent
                            hoverEnabled: !modItem.isFabricApi
                            cursorShape: modItem.isFabricApi ? Qt.ArrowCursor : Qt.PointingHandCursor
                            enabled: !modItem.isFabricApi
                            onClicked: {
                                profileController.toggleMod(model.slug || model.name)
                            }
                        }
                    }

                    // Version Picker Pill
                    Rectangle {
                        Layout.preferredWidth: 100
                        Layout.preferredHeight: 26
                        radius: 4
                        color: vPillMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: EzTheme.borderLight
                        border.width: 1

                        RowLayout {
                            anchors.fill: parent
                            anchors.leftMargin: 8
                            anchors.rightMargin: 8
                            spacing: 4

                            Text {
                                text: model.version ? ("v" + model.version) : "vLatest"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 10
                                color: EzTheme.cyan
                                elide: Text.ElideRight
                                Layout.fillWidth: true
                            }

                            Text {
                                text: "▾"
                                font.pixelSize: 9
                                color: EzTheme.textMuted
                            }
                        }

                        MouseArea {
                            id: vPillMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                root.versionSwitchModId = model.slug || model.projectId || model.name
                                root.versionSwitchModName = model.name
                                root.versionSwitchCurrentVer = model.version
                                var actVer = profileController ? profileController.activeVersion : ""
                                modrinthController.fetchInstalledModVersions(root.versionSwitchModId, actVer)
                                root.versionModalOpen = true
                            }
                        }
                    }

                    // Mod Icon + Title + Description (Clickable to inspect)
                    Item {
                        Layout.fillWidth: true
                        Layout.fillHeight: true

                        RowLayout {
                            anchors.fill: parent
                            spacing: 10

                            Rectangle {
                                width: 32; height: 32; radius: 7
                                color: EzTheme.surface3
                                clip: true

                                Image {
                                    id: modIconImg
                                    anchors.fill: parent
                                    source: model.iconUrl || ""
                                    fillMode: Image.PreserveAspectCrop
                                    visible: status === Image.Ready
                                }

                                Text {
                                    visible: modIconImg.status !== Image.Ready
                                    text: model.name ? model.name.charAt(0).toUpperCase() : "M"
                                    font.family: EzTheme.mcFontFamily
                                    font.pixelSize: 13
                                    font.bold: true
                                    color: EzTheme.accentLight
                                    anchors.centerIn: parent
                                }
                            }

                            ColumnLayout {
                                Layout.fillWidth: true
                                spacing: 1

                                RowLayout {
                                    spacing: 6
                                    Text {
                                        text: model.name
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 13
                                        font.bold: true
                                        color: (modItem.isFabricApi || model.enabled) ? EzTheme.text : EzTheme.textSubtle
                                    }
                                    Rectangle {
                                        height: 14
                                        width: coreText.implicitWidth + 6
                                        radius: 3
                                        color: EzTheme.accentDark
                                        visible: modItem.isFabricApi
                                        Text {
                                            id: coreText
                                            text: "CORE"
                                            font.family: EzTheme.mcFontFamily; font.pixelSize: 8; font.bold: true
                                            color: EzTheme.accentLight; anchors.centerIn: parent
                                        }
                                    }
                                    Rectangle {
                                        height: 14
                                        width: perfRow.implicitWidth + 8
                                        radius: 3
                                        color: "#0E2B1F"
                                        border.color: "#166534"
                                        border.width: 1
                                        visible: modItem.isPerformanceMod && !modItem.isFabricApi
                                        RowLayout {
                                            id: perfRow
                                            anchors.centerIn: parent
                                            spacing: 3
                                            Text { text: "⚡"; font.pixelSize: 8 }
                                            Text {
                                                text: "INTEGRIERT"
                                                font.family: EzTheme.mcFontFamily; font.pixelSize: 8; font.bold: true
                                                color: "#4ADE80"
                                            }
                                        }
                                    }
                                }

                                Text {
                                    text: model.description || "Klicken für Modrinth-Details und Beschreibung…"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    color: EzTheme.textMuted
                                    elide: Text.ElideRight
                                    Layout.fillWidth: true
                                }
                            }
                        }

                        MouseArea {
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                var actVer = profileController ? profileController.activeVersion : ""
                                root.currentInspectedMod = {
                                    name: model.name,
                                    slug: model.slug || model.name,
                                    version: model.version,
                                    author: model.author,
                                    description: model.description,
                                    iconUrl: model.iconUrl
                                }
                                modrinthController.inspectInstalledMod(model.slug || model.name, actVer)
                                root.inspectModalOpen = true
                            }
                        }
                    }

                    // Author
                    Text {
                        text: model.author || "Modrinth"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textSecondary
                        Layout.preferredWidth: 120
                        elide: Text.ElideRight
                    }

                    // Delete / Uninstall Action (Hidden for Fabric API)
                    EzButton {
                        text: "Update " + modItem.modUpdateVersion
                        mcFont: true
                        primary: true
                        visible: !modItem.isFabricApi && modItem.updateAvailable
                        Layout.preferredWidth: 64
                        Layout.preferredHeight: 28
                        onClicked: profileController.updateModVersion(model.slug || model.projectId || model.name, "Latest")
                    }

                    Rectangle {
                        Layout.preferredWidth: 28
                        Layout.preferredHeight: 28
                        radius: 5
                        color: delMouse.containsMouse ? "#3D1418" : "transparent"
                        border.color: delMouse.containsMouse ? EzTheme.danger : "transparent"
                        border.width: 1
                        visible: !modItem.isFabricApi

                        Image {
                            source: "icons/trash.svg"
                            width: 13; height: 13
                            fillMode: Image.PreserveAspectFit
                            anchors.centerIn: parent
                            opacity: delMouse.containsMouse ? 1.0 : 0.4
                        }

                        MouseArea {
                            id: delMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                var slug = model.slug || model.name
                                var deps = profileController ? profileController.checkDependentMods(slug) : []
                                if (deps.length > 0) {
                                    root.pendingDeleteMod = { name: model.name, slug: slug }
                                    root.pendingDeleteDeps = deps
                                    depWarningModal.open()
                                } else {
                                    profileController.uninstallMod(slug, model.name || "")
                                }
                            }
                        }
                    }

                    Item {
                        Layout.preferredWidth: 28
                        visible: modItem.isFabricApi
                    }
                }

                MouseArea {
                    id: rowMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    z: -1
                }
            }
        }
    }

    function formatNum(n) {
        if (!n) return "0"
        if (n >= 1000000) return (n / 1000000).toFixed(1) + "M"
        if (n >= 1000) return (n / 1000).toFixed(1) + "K"
        return n.toString()
    }
}
