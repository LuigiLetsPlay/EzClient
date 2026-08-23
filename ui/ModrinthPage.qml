import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root

    property bool isInitialized: false
    property var pendingDeleteMod: null
    property var pendingDeleteDeps: []

    readonly property var selMod: (typeof modrinthController !== "undefined" && modrinthController && modrinthController.selectedMod) ? modrinthController.selectedMod : ({})
    readonly property var curResults: (typeof modrinthController !== "undefined" && modrinthController && modrinthController.results) ? modrinthController.results : []
    readonly property var curVersions: (typeof modrinthController !== "undefined" && modrinthController && modrinthController.filteredVersions) ? modrinthController.filteredVersions : []
    readonly property string curVersionFilter: (typeof modrinthController !== "undefined" && modrinthController) ? modrinthController.versionTypeFilter : "all"
    readonly property bool curLoading: (typeof modrinthController !== "undefined" && modrinthController) ? modrinthController.loading : false
    readonly property int curTotalHits: (typeof modrinthController !== "undefined" && modrinthController) ? modrinthController.totalHits : 0
    readonly property string selectedProjectId: root.selMod ? (root.selMod.project_id || root.selMod.id || root.selMod.slug || "") : ""
    readonly property string curActiveName: (typeof profileController !== "undefined" && profileController && profileController.activeName) ? profileController.activeName : ""
    readonly property bool isCoreMod: Boolean(root.selMod && (root.selMod.slug === "fabric-api" || root.selMod.project_id === "P7dR8mSH" || (root.selMod.title && root.selMod.title.toLowerCase() === "fabric api")))

    property var pendingShaderMod: null

    function triggerInstall(modItem) {
        if (!modItem) return
        var pType = modItem.project_type || (modrinthController ? modrinthController.projectType : "mod")
        if (pType === "shader" && profileController && !profileController.isIrisInstalled()) {
            irisPromptModal.open(modItem)
            return
        }
        performInstall(modItem)
    }

    function performInstall(modItem) {
        if (!profileController || !modItem) return
        var pType = modItem.project_type || (modrinthController ? modrinthController.projectType : "mod")
        var slug = modItem.slug || modItem.project_id || modItem.id || "item"
        var title = modItem.title || modItem.name || slug
        var ext = (pType === "shader" || pType === "resourcepack") ? ".zip" : ".jar"
        var file = (modItem.filename) ? modItem.filename : (slug + ext)
        profileController.installMod(
            modItem.project_id || slug,
            title,
            "Latest",
            file,
            modItem.author || "Modrinth",
            modItem.description || "",
            modItem.icon_url || ""
        )
    }

    Component.onCompleted: {
        var actVer = (typeof profileController !== "undefined" && profileController && profileController.activeVersion) ? profileController.activeVersion : "26.2"
        if (actVer) {
            var idx = versionCombo.find(actVer)
            if (idx >= 0) versionCombo.currentIndex = idx
            modrinthController.setMcVersion(actVer)
        }
        isInitialized = true
        if (root.curResults.length === 0) {
            modrinthController.search()
        }
    }

    property var pendingInstalls: []

    Connections {
        target: (typeof profileController !== "undefined") ? profileController : null
        function onActiveProfileChanged() {
            if (profileController && profileController.activeVersion) {
                var idx = versionCombo.find(profileController.activeVersion)
                if (idx >= 0) versionCombo.currentIndex = idx
                modrinthController.setMcVersion(profileController.activeVersion)
                if (root.isInitialized) {
                    modrinthController.search()
                }
            }
        }
        function onModInstallStarted(modId) {
            var arr = root.pendingInstalls.slice()
            if (arr.indexOf(modId) === -1) {
                arr.push(modId)
                root.pendingInstalls = arr
            }
        }
        function onModInstallFinished(modId) {
            var arr = root.pendingInstalls.slice()
            var idx = arr.indexOf(modId)
            if (idx !== -1) {
                arr.splice(idx, 1)
                root.pendingInstalls = arr
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // LAYOUT: Vertical stack  [ Toolbar | SplitView ]
    // ─────────────────────────────────────────────────────────
    ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // ─────────── TOOLBAR ───────────
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 48
            color: EzTheme.surface
            border.color: EzTheme.border
            border.width: 1

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 16
                anchors.rightMargin: 16
                spacing: 10

                // Source Selector (All / Modrinth / CurseForge)
                Row {
                    spacing: 4
                    Repeater {
                        model: [
                            { id: "all",        label: "Alle",        icon: "🌐" },
                            { id: "modrinth",   label: "Modrinth",   icon: "🟢" },
                            { id: "curseforge", label: "CurseForge", icon: "🔥" }
                        ]
                        Rectangle {
                            height: 32
                            width: srcRow.implicitWidth + 14
                            radius: 6
                            color: (modrinthController && modrinthController.source === modelData.id) ? EzTheme.surfaceActive : (sMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                            border.color: (modrinthController && modrinthController.source === modelData.id) ? EzTheme.accent : EzTheme.border
                            border.width: 1

                            RowLayout {
                                id: srcRow
                                anchors.centerIn: parent
                                spacing: 5
                                Text { text: modelData.icon; font.pixelSize: 10 }
                                Text {
                                    text: modelData.label
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: (modrinthController && modrinthController.source === modelData.id)
                                    color: (modrinthController && modrinthController.source === modelData.id) ? EzTheme.accentLight : EzTheme.text
                                }
                            }

                            MouseArea {
                                id: sMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    if (modrinthController) {
                                        modrinthController.setSource(modelData.id)
                                    }
                                }
                            }
                        }
                    }
                }

                Rectangle { width: 1; height: 20; color: EzTheme.border }

                // Project Type Selector Tabs
                Row {
                    spacing: 4
                    Repeater {
                        model: [
                            { id: "mod",          label: "Mods",           icon: "📦" },
                            { id: "shader",       label: "Shader",         icon: "✨" },
                            { id: "resourcepack", label: "Resource Packs", icon: "🎨" }
                        ]
                        Rectangle {
                            height: 32
                            width: typeRow.implicitWidth + 14
                            radius: 6
                            color: (modrinthController && modrinthController.projectType === modelData.id) ? EzTheme.surfaceActive : (tMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                            border.color: (modrinthController && modrinthController.projectType === modelData.id) ? EzTheme.accent : EzTheme.border
                            border.width: 1

                            RowLayout {
                                id: typeRow
                                anchors.centerIn: parent
                                spacing: 6
                                Text { text: modelData.icon; font.pixelSize: 11 }
                                Text {
                                    text: modelData.label
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: (modrinthController && modrinthController.projectType === modelData.id)
                                    color: (modrinthController && modrinthController.projectType === modelData.id) ? EzTheme.accentLight : EzTheme.text
                                }
                            }

                            MouseArea {
                                id: tMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    if (modrinthController) {
                                        modrinthController.setProjectType(modelData.id)
                                    }
                                }
                            }
                        }
                    }
                }

                // Search input
                Rectangle {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 32
                    color: EzTheme.bg
                    border.color: searchInput.activeFocus ? EzTheme.accent : EzTheme.border
                    border.width: 1
                    radius: 5

                    RowLayout {
                        anchors.fill: parent
                        anchors.leftMargin: 10
                        anchors.rightMargin: 8
                        spacing: 8

                        Image {
                            source: "icons/search.svg"
                            Layout.preferredWidth: 13
                            Layout.preferredHeight: 13
                            fillMode: Image.PreserveAspectFit
                        }

                        TextInput {
                            id: searchInput
                            Layout.fillWidth: true
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            color: EzTheme.text
                            selectByMouse: true
                            clip: true
                            verticalAlignment: TextInput.AlignVCenter

                            Text {
                                text: EzI18n.t("modrinth_search_placeholder", "Modrinth durchsuchen…")
                                font: parent.font
                                color: EzTheme.textSubtle
                                visible: parent.text === ""
                                anchors.verticalCenter: parent.verticalCenter
                            }

                            Timer {
                                id: liveSearchTimer
                                interval: 300
                                repeat: false
                                onTriggered: {
                                    if (root.isInitialized) {
                                        modrinthController.setQuery(searchInput.text)
                                        modrinthController.search()
                                    }
                                }
                            }

                            onTextChanged: {
                                if (root.isInitialized) {
                                    liveSearchTimer.restart()
                                }
                            }

                            Keys.onReturnPressed: {
                                liveSearchTimer.stop()
                                modrinthController.setQuery(text)
                                modrinthController.search()
                            }
                        }

                        // Clear button
                        Rectangle {
                            width: 16
                            height: 16
                            radius: 8
                            color: clearMouse.containsMouse ? EzTheme.surface3 : "transparent"
                            visible: searchInput.text !== ""

                            Text {
                                text: "✕"
                                font.pixelSize: 9
                                color: EzTheme.textMuted
                                anchors.centerIn: parent
                            }

                            MouseArea {
                                id: clearMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    liveSearchTimer.stop()
                                    searchInput.text = ""
                                    modrinthController.setQuery("")
                                    modrinthController.search()
                                }
                            }
                        }
                    }

                    Behavior on border.color { ColorAnimation { duration: 120 } }
                }

                // Version filter
                EzComboBox {
                    id: versionCombo
                    model: modrinthController ? modrinthController.gameVersions : []
                    Layout.preferredWidth: 110
                    Layout.preferredHeight: 32
                    onCurrentTextChanged: {
                        if (root.isInitialized) {
                            modrinthController.setMcVersion(currentText)
                            modrinthController.search()
                        }
                    }
                }

                // Category filter
                EzComboBox {
                    id: categoryCombo
                    model: ["All", "Optimization", "Utility", "Library", "Adventure", "Decoration", "Technology", "Storage", "Food", "Magic"]
                    Layout.preferredWidth: 120
                    Layout.preferredHeight: 32
                    onCurrentTextChanged: {
                        if (root.isInitialized) {
                            modrinthController.setCategory(currentText)
                            modrinthController.search()
                        }
                    }
                }

                // Sort filter
                EzComboBox {
                    id: sortCombo
                    model: ["relevance", "downloads", "follows", "newest", "updated"]
                    Layout.preferredWidth: 115
                    Layout.preferredHeight: 32
                    onCurrentTextChanged: {
                        if (root.isInitialized) {
                            modrinthController.setSort(currentText)
                            modrinthController.search()
                        }
                    }
                }

                // Search button
                EzButton {
                    text: EzI18n.t("modrinth_search_btn", "Suchen")
                    primary: true
                    mcFont: true
                    Layout.preferredWidth: 80
                    Layout.preferredHeight: 32
                    onClicked: {
                        modrinthController.setQuery(searchInput.text)
                        modrinthController.search()
                    }
                }
            }
        }

        // ─────────── MAIN CONTENT: Left List + Right Inspector ───────────
        RowLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            spacing: 0

            // ══════════════ LEFT: RESULTS LIST ══════════════
            Rectangle {
                Layout.fillWidth: true
                Layout.fillHeight: true
                color: EzTheme.bg
                clip: true

                ColumnLayout {
                    anchors.fill: parent
                    spacing: 0

                    // Search Results Header Bar
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 32
                        color: EzTheme.surface
                        border.color: EzTheme.border
                        border.width: 1

                        RowLayout {
                            anchors.fill: parent
                            anchors.leftMargin: 16
                            anchors.rightMargin: 16
                            spacing: 8

                            Rectangle {
                                width: 6; height: 6; radius: 3
                                color: root.curLoading ? EzTheme.warning : EzTheme.accent
                                visible: root.curLoading
                                SequentialAnimation on opacity {
                                    loops: Animation.Infinite
                                    NumberAnimation { to: 0.3; duration: 400 }
                                    NumberAnimation { to: 1.0; duration: 400 }
                                }
                            }

                            Text {
                                text: root.curLoading ? "Suche läuft…" : (root.curTotalHits > 0 ? (root.curTotalHits + " Mods") : "0 Mods")
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzTheme.textSecondary
                                Layout.fillWidth: true
                            }
                        }
                    }

                    // Empty State
                    ColumnLayout {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        spacing: 12
                        visible: root.curResults.length === 0 && !root.curLoading

                        Item { Layout.fillHeight: true }

                        Image {
                            source: "icons/search.svg"
                            Layout.preferredWidth: 40
                            Layout.preferredHeight: 40
                            fillMode: Image.PreserveAspectFit
                            opacity: 0.2
                            Layout.alignment: Qt.AlignHCenter
                        }

                        Text {
                            text: "Keine Ergebnisse"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 13
                            color: EzTheme.textSubtle
                            Layout.alignment: Qt.AlignHCenter
                        }

                        Item { Layout.fillHeight: true }
                    }

                    // Results List View
                    ListView {
                        id: resultsList
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        visible: root.curResults.length > 0
                        clip: true
                        spacing: 1
                        model: root.curResults

                        delegate: Rectangle {
                            id: resultItem
                            width: resultsList.width
                            height: 70
                            readonly property bool isSel: root.selectedProjectId !== "" && root.selectedProjectId === (modelData.project_id || modelData.id || modelData.slug)
                            readonly property bool isInstalled: {
                                if (modelData.is_installed) return true
                                if (typeof profileController !== "undefined" && profileController && profileController.isModInstalled) {
                                    return profileController.isModInstalled(
                                        modelData.project_id || modelData.id || "",
                                        modelData.slug || "",
                                        modelData.title || modelData.name || "",
                                        modelData.filename || ""
                                    )
                                }
                                return false
                            }
                            readonly property bool isPending: {
                                var s = modelData.slug || modelData.project_id || modelData.id || ""
                                return root.pendingInstalls.indexOf(s) !== -1
                            }

                            color: isSel ? EzTheme.surfaceActive : (rowMouse.containsMouse ? EzTheme.surfaceHover : "transparent")

                            Behavior on color { ColorAnimation { duration: 100 } }

                            RowLayout {
                                anchors.fill: parent
                                anchors.leftMargin: 14
                                anchors.rightMargin: 14
                                spacing: 12

                                // Mod Icon
                                Rectangle {
                                    Layout.preferredWidth: 42
                                    Layout.preferredHeight: 42
                                    radius: 8
                                    color: EzTheme.surface2
                                    clip: true

                                    Image {
                                        id: modIcon
                                        anchors.fill: parent
                                        source: modelData.icon_url || ""
                                        fillMode: Image.PreserveAspectCrop
                                        visible: status === Image.Ready
                                    }

                                    Text {
                                        visible: modIcon.status !== Image.Ready
                                        text: modelData.title ? modelData.title.charAt(0).toUpperCase() : "M"
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 16
                                        font.bold: true
                                        color: EzTheme.accentLight
                                        anchors.centerIn: parent
                                    }
                                }

                                // Mod info
                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 3

                                    RowLayout {
                                        spacing: 8
                                        Text {
                                            text: modelData.title || ""
                                            font.family: EzTheme.mcFontFamily
                                            font.pixelSize: 13
                                            font.bold: true
                                            color: resultItem.isSel ? EzTheme.accentLight : EzTheme.text
                                            elide: Text.ElideRight
                                            Layout.fillWidth: true
                                        }

                                        // Source badge (Modrinth / CurseForge)
                                        Rectangle {
                                            height: 16
                                            width: srcText.implicitWidth + 8
                                            radius: 3
                                            color: (modelData.source === "curseforge") ? "#2B140B" : "#0D2616"
                                            border.color: (modelData.source === "curseforge") ? "#E04E14" : EzTheme.accent
                                            border.width: 1

                                            Text {
                                                id: srcText
                                                text: (modelData.source === "curseforge") ? "CurseForge" : "Modrinth"
                                                font.family: EzTheme.mcFontFamily
                                                font.pixelSize: 8
                                                font.bold: true
                                                color: (modelData.source === "curseforge") ? "#F57C00" : EzTheme.accentLight
                                                anchors.centerIn: parent
                                            }
                                        }

                                        // INSTALLED BADGE IN STORE LIST (Instant visual feedback!)
                                        Rectangle {
                                            height: 18
                                            width: instText.implicitWidth + 10
                                            radius: 3
                                            color: "#0B2D19"
                                            border.color: EzTheme.accent
                                            border.width: 1
                                            visible: resultItem.isInstalled

                                            Text {
                                                id: instText
                                                text: "✓ " + (window.integratedMods && window.integratedMods.indexOf(modelData.slug) !== -1 ? "Integriert" : EzI18n.t("modrinth_installed", "Installiert"))
                                                font.family: EzTheme.mcFontFamily
                                                font.pixelSize: 9
                                                font.bold: true
                                                color: EzTheme.accentLight
                                                anchors.centerIn: parent
                                            }
                                        }

                                        // Downloads badge
                                        Text {
                                            text: "⬇ " + formatNum(modelData.downloads || 0)
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 10
                                            color: EzTheme.textMuted
                                        }
                                    }

                                    Text {
                                        text: modelData.description || ""
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.textMuted
                                        elide: Text.ElideRight
                                        Layout.fillWidth: true
                                    }
                                }

                                // 1-Click Quick Install Button
                                Rectangle {
                                    Layout.preferredWidth: 96
                                    Layout.preferredHeight: 30
                                    radius: 6
                                    color: resultItem.isPending ? "#808080" : (resultItem.isInstalled ? "#14281E" : (cInstMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent))
                                    border.color: resultItem.isInstalled && !resultItem.isPending ? EzTheme.accent : "transparent"
                                    border.width: 1
                                    z: 10

                                    RowLayout {
                                        anchors.centerIn: parent
                                        spacing: 4
                                        Text {
                                            text: resultItem.isPending ? "..." : (resultItem.isInstalled ? "✓" : "+")
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 11
                                            font.bold: true
                                            color: (resultItem.isInstalled || resultItem.isPending) ? EzTheme.accentLight : "#000000"
                                        }
                                        Text {
                                            text: resultItem.isPending ? "Lädt..." : (resultItem.isInstalled ? (window.integratedMods && window.integratedMods.indexOf(modelData.slug) !== -1 ? "Integriert" : "Installiert") : "Installieren")
                                            font.family: EzTheme.mcFontFamily
                                            font.pixelSize: 10
                                            font.bold: true
                                            color: (resultItem.isInstalled || resultItem.isPending) ? EzTheme.accentLight : "#000000"
                                        }
                                    }

                                    MouseArea {
                                        id: cInstMouse
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        cursorShape: Qt.PointingHandCursor
                                        enabled: !resultItem.isInstalled && !resultItem.isPending
                                        onClicked: {
                                            root.triggerInstall(modelData)
                                        }
                                    }
                                }
                            }

                            MouseArea {
                                id: rowMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: modrinthController.selectMod(modelData)
                            }
                        }
                    }

                    // Load More Footer
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 38
                        color: EzTheme.surface
                        border.color: EzTheme.border
                        border.width: 1
                        visible: root.curResults.length > 0 && root.curResults.length < root.curTotalHits

                        EzButton {
                            anchors.centerIn: parent
                            text: "Mehr"
                            Layout.preferredHeight: 26
                            onClicked: modrinthController.loadMore()
                        }
                    }
                }
            }

            // 1px Vertical Separator
            Rectangle {
                Layout.preferredWidth: 1
                Layout.fillHeight: true
                color: EzTheme.border
            }

            // ══════════════ RIGHT: INSPECTOR PANEL ══════════════
            Rectangle {
                Layout.preferredWidth: Math.min(420, Math.max(340, Math.floor(root.width * 0.36)))
                Layout.fillHeight: true
                color: EzTheme.surface
                clip: true

                // Empty state – no mod selected
                ColumnLayout {
                    anchors.centerIn: parent
                    spacing: 8
                    visible: root.selectedProjectId === ""

                    Image {
                        source: "icons/box.svg"
                        Layout.preferredWidth: 36
                        Layout.preferredHeight: 36
                        fillMode: Image.PreserveAspectFit
                        opacity: 0.25
                        Layout.alignment: Qt.AlignHCenter
                    }

                    Text {
                        text: "Wähle einen Mod für Details"
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 12
                        color: EzTheme.textSubtle
                        Layout.alignment: Qt.AlignHCenter
                    }
                }

                // Mod inspector (scrollable)
                ScrollView {
                    anchors.fill: parent
                    clip: true
                    contentWidth: availableWidth

                    FastWheelHandler {}

                    ScrollBar.horizontal.policy: ScrollBar.AlwaysOff
                    ScrollBar.vertical.policy: ScrollBar.AsNeeded
                    ScrollBar.vertical.visible: ScrollBar.vertical.size < 0.999
                    visible: root.selectedProjectId !== ""

                    ColumnLayout {
                        width: parent.width
                        spacing: 0

                        // ── Inspector Header ──
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 90
                            color: EzTheme.surface2

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 14
                                spacing: 12

                                // Icon
                                Rectangle {
                                    Layout.preferredWidth: 52
                                    Layout.preferredHeight: 52
                                    radius: 10
                                    color: EzTheme.surface3
                                    clip: true

                                    Image {
                                        id: inspectorIcon
                                        anchors.fill: parent
                                        source: root.selMod ? (root.selMod.icon_url || "") : ""
                                        fillMode: Image.PreserveAspectCrop
                                        visible: status === Image.Ready
                                    }

                                    Text {
                                        visible: inspectorIcon.status !== Image.Ready
                                        text: root.selMod && root.selMod.title ? root.selMod.title.charAt(0).toUpperCase() : "M"
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 20
                                        font.bold: true
                                        color: EzTheme.accentLight
                                        anchors.centerIn: parent
                                    }
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 3

                                    Text {
                                        text: root.selMod ? (root.selMod.title || "") : ""
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 14
                                        font.bold: true
                                        color: EzTheme.text
                                        wrapMode: Text.WordWrap
                                        Layout.fillWidth: true
                                    }

                                    Text {
                                        text: "by " + (root.selMod ? (root.selMod.author || "Modrinth") : "Modrinth")
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.textMuted
                                    }
                                }
                            }
                        }

                        // ── Install / Uninstall Banner ──
                        Rectangle {
                            id: installBanner
                            Layout.fillWidth: true
                            Layout.preferredHeight: 52
                            color: EzTheme.surface2

                            readonly property bool isInstalled: {
                                if (typeof profileController === "undefined" || !profileController) return false;
                                var dummy = profileController.installedMods; // trigger UI reactivity when installed mods change
                                if (root.selMod) {
                                    return profileController.isModInstalled(
                                        root.selMod.project_id || root.selMod.id || "",
                                        root.selMod.slug || "",
                                        root.selMod.title || root.selMod.name || "",
                                        root.selMod.filename || ""
                                    )
                                }
                                return false
                            }

                            RowLayout {
                                anchors.fill: parent
                                anchors.leftMargin: 14
                                anchors.rightMargin: 14
                                spacing: 8

                                EzButton {
                                    text: installBanner.isInstalled ? (window.integratedMods && window.integratedMods.indexOf(root.selMod ? root.selMod.slug : "") !== -1 ? "Integriert" : EzI18n.t("modrinth_installed", "Installiert")) : EzI18n.t("modrinth_install", "Installieren")
                                    primary: !installBanner.isInstalled
                                    mcFont: true
                                    Layout.fillWidth: true
                                    Layout.preferredHeight: 34
                                    onClicked: {
                                        var mod = root.selMod
                                        var bestVer = root.curVersions && root.curVersions.length > 0 ? root.curVersions[0] : null
                                        var latestVer = bestVer ? (bestVer.version_number || "Latest") : "Latest"
                                        var file = (bestVer && bestVer.files && bestVer.files.length > 0) ? bestVer.files[0].filename : (mod.slug + ".jar")
                                        var src = mod.source || (modrinthController ? modrinthController.source : "modrinth")
                                        if (src === "all") src = "modrinth"
                                        profileController.installMod(
                                            mod.project_id || mod.slug,
                                            mod.title || mod.name || "",
                                            latestVer,
                                            file,
                                            mod.author || (src === "curseforge" ? "CurseForge" : "Modrinth"),
                                            mod.description || "",
                                            mod.icon_url || "",
                                            src
                                        )
                                    }
                                }

                                // Delete button (HIDDEN for Fabric API / Core Mods!)
                                EzButton {
                                    iconSource: "trash.svg"
                                    danger: true
                                    visible: installBanner.isInstalled && !root.isCoreMod
                                    Layout.preferredWidth: 34
                                    Layout.preferredHeight: 34
                                    onClicked: {
                                        var slug = root.selMod.slug || root.selMod.project_id || root.selMod.title
                                        var deps = profileController ? profileController.checkDependentMods(slug) : []
                                        if (deps.length > 0) {
                                            root.pendingDeleteMod = root.selMod
                                            root.pendingDeleteDeps = deps
                                            depWarningModal.open()
                                        } else {
                                            profileController.uninstallMod(slug, root.selMod.title || root.selMod.name || "")
                                        }
                                    }
                                }

                                // Core badge for Fabric API instead of trash can
                                Rectangle {
                                    height: 28
                                    width: coreBadgeText.implicitWidth + 14
                                    radius: 4
                                    color: EzTheme.surface3
                                    border.color: EzTheme.accentDark
                                    border.width: 1
                                    visible: installBanner.isInstalled && root.isCoreMod

                                    Text {
                                        id: coreBadgeText
                                        text: "🔒 CORE MOD"
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 9
                                        font.bold: true
                                        color: EzTheme.accentLight
                                        anchors.centerIn: parent
                                    }
                                }
                            }
                        }

                        // ── Dependency Warning Banner (Fabric API check) ──
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: depCol.implicitHeight + 16
                            color: "#181308"
                            border.color: EzTheme.warning
                            border.width: 1
                            visible: Boolean(root.selMod && root.selMod.title && root.selMod.slug !== "fabric-api" && (!profileController || !profileController.isModInstalled("Fabric API")))

                            RowLayout {
                                id: depCol
                                anchors.fill: parent
                                anchors.margins: 10
                                spacing: 8

                                Text {
                                    text: "⚠️"
                                    font.pixelSize: 14
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 1

                                    Text {
                                        text: "Benötigt Fabric API"
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 11
                                        font.bold: true
                                        color: EzTheme.warning
                                    }

                                    Text {
                                        text: "Für diesen Mod wird Fabric API im Profil benötigt."
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 9
                                        color: EzTheme.textMuted
                                    }
                                }
                            }
                        }

                        // ── Stats Row ──
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.leftMargin: 14
                            Layout.rightMargin: 14
                            Layout.topMargin: 8
                            Layout.bottomMargin: 8
                            Layout.preferredHeight: 52
                            radius: 8
                            color: EzTheme.surface2
                            border.color: EzTheme.border
                            border.width: 1

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 4
                                spacing: 0

                                // Downloads
                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: formatNum(root.selMod.downloads || 0); font.family: EzTheme.mcFontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text; Layout.alignment: Qt.AlignHCenter }
                                    Text { text: EzI18n.t("modrinth_downloads", "DOWNLOADS"); font.family: EzTheme.fontFamily; font.pixelSize: 8; color: EzTheme.textMuted; Layout.alignment: Qt.AlignHCenter }
                                }

                                Rectangle { width: 1; Layout.preferredHeight: 28; Layout.alignment: Qt.AlignVCenter; color: EzTheme.border }

                                // Follows
                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: formatNum(root.selMod.follows || 0); font.family: EzTheme.mcFontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text; Layout.alignment: Qt.AlignHCenter }
                                    Text { text: EzI18n.t("modrinth_followers", "FOLLOWER"); font.family: EzTheme.fontFamily; font.pixelSize: 8; color: EzTheme.textMuted; Layout.alignment: Qt.AlignHCenter }
                                }

                                Rectangle { width: 1; Layout.preferredHeight: 28; Layout.alignment: Qt.AlignVCenter; color: EzTheme.border }

                                // Client / Server
                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: (root.selMod && root.selMod.client_side) ? root.selMod.client_side : "both"; font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.cyan; Layout.alignment: Qt.AlignHCenter }
                                    Text { text: EzI18n.t("modrinth_side", "SEITE"); font.family: EzTheme.fontFamily; font.pixelSize: 8; color: EzTheme.textMuted; Layout.alignment: Qt.AlignHCenter }
                                }
                            }
                        }

                        // ── Description ──
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: descText.implicitHeight + 16
                            color: "transparent"

                            Text {
                                id: descText
                                anchors.left: parent.left
                                anchors.right: parent.right
                                anchors.top: parent.top
                                anchors.leftMargin: 14
                                anchors.rightMargin: 14
                                anchors.topMargin: 4
                                anchors.bottomMargin: 4
                                text: root.selMod.description || ""
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textSecondary
                                wrapMode: Text.WordWrap
                                lineHeight: 1.4
                            }
                        }

                        // ── Categories Tags ──
                        Flow {
                            Layout.fillWidth: true
                            Layout.leftMargin: 14
                            Layout.rightMargin: 14
                            spacing: 4
                            visible: Boolean(root.selMod && root.selMod.categories && root.selMod.categories.length > 0)

                            Repeater {
                                model: (root.selMod && root.selMod.categories) ? root.selMod.categories : []
                                Rectangle {
                                    height: 18
                                    width: tagT.implicitWidth + 10
                                    radius: 3
                                    color: EzTheme.surface3
                                    Text {
                                        id: tagT
                                        text: modelData
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 9
                                        color: EzTheme.textMuted
                                        anchors.centerIn: parent
                                    }
                                }
                            }
                        }

                        Item { height: 16 }

                        // ── Versions Section Header with Release/Beta Filter Bar (QoL!) ──
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 36
                            color: EzTheme.surface2
                            border.color: EzTheme.border
                            border.width: 1

                            RowLayout {
                                anchors.fill: parent
                                anchors.leftMargin: 14
                                anchors.rightMargin: 10
                                spacing: 8

                                Text {
                                    text: EzI18n.t("modrinth_versions", "VERSIONEN") + " (" + root.curVersions.length + ")"
                                    font.family: EzTheme.mcFontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: EzTheme.textSecondary
                                    Layout.fillWidth: true
                                }

                                // Segmented Filter Bar: Release (Standard) | Beta | Alle
                                Row {
                                    spacing: 4

                                    Repeater {
                                        model: [
                                            { id: "release", label: EzI18n.t("modrinth_release", "Release") },
                                            { id: "beta",    label: EzI18n.t("modrinth_beta", "Beta") },
                                            { id: "all",     label: EzI18n.t("modrinth_all_versions", "Alle") }
                                        ]

                                        Rectangle {
                                            height: 22
                                            width: fLabel.implicitWidth + 14
                                            radius: 3
                                            color: root.curVersionFilter === modelData.id ? EzTheme.accent : (fMouse.containsMouse ? EzTheme.surface3 : "transparent")
                                            border.color: root.curVersionFilter === modelData.id ? EzTheme.accentLight : EzTheme.border
                                            border.width: 1

                                            Text {
                                                id: fLabel
                                                text: modelData.label
                                                font.family: EzTheme.mcFontFamily
                                                font.pixelSize: 9
                                                font.bold: true
                                                color: root.curVersionFilter === modelData.id ? "#000000" : (fMouse.containsMouse ? EzTheme.text : EzTheme.textMuted)
                                                anchors.centerIn: parent
                                            }

                                            MouseArea {
                                                id: fMouse
                                                anchors.fill: parent
                                                hoverEnabled: true
                                                cursorShape: Qt.PointingHandCursor
                                                onClicked: modrinthController.setVersionTypeFilter(modelData.id)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Versions List ──
                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 1

                            Repeater {
                                model: root.curVersions

                                Rectangle {
                                    id: versionItem
                                    Layout.fillWidth: true
                                    height: 48
                                    color: verMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2

                                    // These states belong to the version delegate, so the
                                    // button can reliably access them through versionItem.
                                    // Defining them in the RowLayout left them out of the
                                    // button's QML scope and caused the ReferenceErrors.
                                    property bool isThisVerInstalled: {
                                        if (!profileController) return false;
                                        var dummy = profileController.installedMods; // trigger re-eval
                                        var selSlug = root.selMod ? (root.selMod.project_id || root.selMod.slug || "") : "";
                                        var verNum = modelData.version_number || "";
                                        return profileController.hasModVersion(selSlug, verNum);
                                    }
                                    property bool isProjectInstalled: {
                                        if (!profileController) return false;
                                        var dummy = profileController.installedMods;
                                        var selSlug = root.selMod ? (root.selMod.project_id || root.selMod.slug || "") : "";
                                        return profileController.isModInstalled(selSlug, root.selMod ? (root.selMod.title || "") : "");
                                    }
                                    property bool isPending: {
                                        var s = root.selMod ? (root.selMod.project_id || root.selMod.slug || root.selMod.id || "") : ""
                                        return root.pendingInstalls.indexOf(s) !== -1
                                    }

                                    RowLayout {
                                        anchors.fill: parent
                                        anchors.leftMargin: 14
                                        anchors.rightMargin: 14
                                        spacing: 8

                                        // Release type tag
                                        Rectangle {
                                            width: 14; height: 14; radius: 3
                                            color: modelData.version_type === "release" ? EzTheme.accentDark : EzTheme.surface3
                                            Text {
                                                text: modelData.version_type === "release" ? "R" : (modelData.version_type === "beta" ? "B" : "A")
                                                font.pixelSize: 8; font.bold: true
                                                color: modelData.version_type === "release" ? EzTheme.accentLight : EzTheme.warning
                                                anchors.centerIn: parent
                                            }
                                        }

                                        ColumnLayout {
                                            Layout.fillWidth: true
                                            spacing: 1
                                            Text {
                                                text: modelData.version_number || modelData.name || ""
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

                                        EzButton {
                                            visible: true
                                            opacity: 1.0
                                            text: versionItem.isPending ? "Lädt..." : (versionItem.isThisVerInstalled ? EzI18n.t("modrinth_installed", "Installiert") : (versionItem.isProjectInstalled ? "Wechseln" : EzI18n.t("modrinth_install", "Installieren")))
                                            primary: !versionItem.isThisVerInstalled && !versionItem.isPending
                                            mcFont: true
                                            Layout.preferredHeight: 26
                                            Layout.preferredWidth: 125
                                            Layout.minimumWidth: 125
                                            enabled: !versionItem.isThisVerInstalled && !versionItem.isPending
                                            onClicked: {
                                                var mod = root.selMod
                                                var file = (modelData.files && modelData.files.length > 0) ? modelData.files[0].filename : (mod.slug + ".jar")
                                                profileController.installMod(
                                                    mod.project_id || mod.slug,
                                                    mod.title || mod.name || "",
                                                    modelData.version_number || "Latest",
                                                    file,
                                                    mod.author || "Modrinth",
                                                    mod.description || "",
                                                    mod.icon_url || ""
                                                )
                                            }
                                        }
                                    }

                                    MouseArea {
                                        id: verMouse
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        z: -1
                                    }
                                }
                            }
                        }

                        // No versions fallback
                        Rectangle {
                            Layout.fillWidth: true
                            height: 60
                            color: "transparent"
                            visible: root.curVersions.length === 0 && root.selectedProjectId !== ""

                            Text {
                                text: root.curVersionFilter === "release" ? "Keine Release-Versionen (stelle Filter auf 'Alle')" : "Lade Versionen von Modrinth…"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textMuted
                                anchors.centerIn: parent
                            }
                        }

                        Item { height: 20 }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // DEPENDENCY WARNING DIALOG MODAL (QoL Feature)
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
                text: "Warnung: Die Mod '" + (root.pendingDeleteMod ? (root.pendingDeleteMod.title || root.pendingDeleteMod.name || "") : "") + "' wird von folgenden installierten Mods benötigt: " + root.pendingDeleteDeps.join(", ") + ".\n\nDas Löschen kann zu Spielabstürzen führen."
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
                            profileController.uninstallMod(root.pendingDeleteMod.slug || root.pendingDeleteMod.project_id || root.pendingDeleteMod.title, root.pendingDeleteMod.title || root.pendingDeleteMod.name || "")
                        }
                        depWarningModal.close()
                    }
                }
            }
        }
    }

    // ─── IRIS SHADERS AUTO-PROMPT DIALOG ───
    Rectangle {
        id: irisPromptModal
        anchors.fill: parent
        color: "#C805070A"
        z: 99999
        visible: opacity > 0.001
        opacity: 0.0

        Behavior on opacity { NumberAnimation { duration: 180 } }

        function open(shaderMod) {
            root.pendingShaderMod = shaderMod
            irisPromptModal.opacity = 1.0
        }
        function close() {
            irisPromptModal.opacity = 0.0
        }

        MouseArea {
            anchors.fill: parent
            onClicked: {}
        }

        Rectangle {
            anchors.centerIn: parent
            width: Math.min(480, parent.width - 32)
            height: 280
            radius: 14
            color: "#12151E"
            border.color: "#38BDF8"
            border.width: 1

            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 20
                spacing: 14

                RowLayout {
                    spacing: 10
                    Text { text: "✨"; font.pixelSize: 22 }
                    ColumnLayout {
                        spacing: 2
                        Text {
                            text: "Iris Shaders Mod empfohlen"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 14
                            font.bold: true
                            color: "#38BDF8"
                        }
                        Text {
                            text: "Shader benötigen eine Shader-Engine wie Iris, um in Minecraft dargestellt zu werden."
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            color: EzTheme.textSecondary
                            wrapMode: Text.Wrap
                            Layout.fillWidth: true
                        }
                    }
                }

                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                Text {
                    text: "Möchtest du Iris Shaders automatisch mitinstallieren?"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    font.bold: true
                    color: EzTheme.text
                }

                Item { Layout.fillHeight: true }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    EzButton {
                        text: "Nur Shader installieren"
                        Layout.fillWidth: true
                        Layout.preferredHeight: 34
                        onClicked: {
                            irisPromptModal.close()
                            if (root.pendingShaderMod) {
                                root.performInstall(root.pendingShaderMod)
                            }
                        }
                    }

                    EzButton {
                        text: "✓ Iris & Shader installieren"
                        primary: true
                        mcFont: true
                        Layout.fillWidth: true
                        Layout.preferredHeight: 34
                        onClicked: {
                            irisPromptModal.close()
                            if (profileController) {
                                profileController.installIris()
                            }
                            if (root.pendingShaderMod) {
                                root.performInstall(root.pendingShaderMod)
                            }
                        }
                    }
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
