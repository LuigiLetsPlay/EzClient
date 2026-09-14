import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Window {
    id: liveLogsWindow
    width: 1060
    height: 640
    minimumWidth: 800
    minimumHeight: 480
    title: "Minecraft Logs · EzClient"
    flags: Qt.Window | Qt.FramelessWindowHint | Qt.WindowSystemMenuHint | Qt.WindowMinimizeButtonHint
    transientParent: null
    color: "#0B0E14"

    onClosing: function(close) {
        close.accepted = false
        liveLogsWindow.hide()
    }

    property var liveLogService: (typeof profileController !== "undefined" && profileController) ? profileController.liveLogService : null
    property string activeFilter: "ALL" // "ALL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"
    property string searchQuery: ""
    property bool autoScroll: true
    property string cpuUsage: "0%"
    property string ramUsage: "0 MB"
    property string uptimeStr: "00:00:00"

    function formatUptime(sec) {
        var h = Math.floor(sec / 3600)
        var m = Math.floor((sec % 3600) / 60)
        var s = sec % 60
        return (h > 0 ? (h + ":") : "") + (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s
    }

    property int currentTimestampSec: Math.floor(Date.now() / 1000)
    Timer {
        interval: 1000
        running: liveLogsWindow.visible
        repeat: true
        onTriggered: {
            liveLogsWindow.currentTimestampSec = Math.floor(Date.now() / 1000)
        }
    }

    property var allLogs: []

    ListModel {
        id: logListModel
    }

    function matchesFilter(entry) {
        if (liveLogsWindow.activeFilter !== "ALL" && entry.level !== liveLogsWindow.activeFilter)
            return false
        var q = liveLogsWindow.searchQuery.toLowerCase().trim()
        if (q && entry.raw.toLowerCase().indexOf(q) === -1)
            return false
        return true
    }

    function refilterLogs() {
        logListModel.clear()
        for (var i = 0; i < allLogs.length; ++i) {
            var item = allLogs[i]
            if (matchesFilter(item)) {
                logListModel.append(item)
            }
        }
        if (autoScroll && !searchQuery) {
            Qt.callLater(function() { logListView.positionViewAtEnd() })
        }
    }

    onActiveFilterChanged: refilterLogs()
    onSearchQueryChanged: refilterLogs()

    function loadBufferedLogs() {
        allLogs = []
        logListModel.clear()
        if (!liveLogService)
            return
        var entries = liveLogService.getBufferedLogs()
        for (var i = 0; i < entries.length; ++i) {
            var entry = entries[i]
            var item = {
                "raw": entry.raw,
                "level": entry.level,
                "time": entry.time,
                "msg": entry.message
            }
            allLogs.push(item)
            if (matchesFilter(item)) {
                logListModel.append(item)
            }
        }
        if (autoScroll && !searchQuery)
            Qt.callLater(function() { logListView.positionViewAtEnd() })
    }

    Component.onCompleted: loadBufferedLogs()
    onVisibleChanged: {
        if (visible)
            loadBufferedLogs()
    }

    Connections {
        target: liveLogsWindow.liveLogService
        function onLogAppended(raw, level, timeStr, msg) {
            var item = {
                "raw": raw,
                "level": level,
                "time": timeStr,
                "msg": msg
            }
            allLogs.push(item)
            if (allLogs.length > 5000) {
                allLogs.shift()
            }
            if (matchesFilter(item)) {
                logListModel.append(item)
                if (logListModel.count > 3000) {
                    logListModel.remove(0, 300)
                }
                if (liveLogsWindow.autoScroll && !searchQuery) {
                    logListView.positionViewAtEnd()
                }
            }
        }
        function onStatsUpdated(cpu, ram, uptimeSec) {
            liveLogsWindow.cpuUsage = cpu + "%"
            liveLogsWindow.ramUsage = (ram > 1024 ? (ram / 1024).toFixed(1) + " GB" : Math.round(ram) + " MB")
            liveLogsWindow.uptimeStr = liveLogsWindow.formatUptime(uptimeSec)
        }
        function onSelectedInstanceChanged() {
            liveLogsWindow.loadBufferedLogs()
        }
        function onLogsCleared() {
            allLogs = []
            logListModel.clear()
        }
    }

    // Main Layout
    ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // ── TOP HEADER / TOOLBAR ──
        Rectangle {
            Layout.fillWidth: true
            height: 52
            color: "#0F131C"
            border.color: "#181D2A"
            border.width: 1

            // Drag area for frameless window
            MouseArea {
                anchors.fill: parent
                onPressed: liveLogsWindow.startSystemMove()
                onDoubleClicked: {
                    liveLogsWindow.visibility === Window.Maximized
                        ? liveLogsWindow.showNormal()
                        : liveLogsWindow.showMaximized()
                }
            }

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 16
                anchors.rightMargin: 0
                spacing: 12

                // Logo / Title
                RowLayout {
                    spacing: 8
                    Image { source: "icons/terminal.svg"; width: 16; height: 16; opacity: 0.8; fillMode: Image.PreserveAspectFit; sourceSize: Qt.size(16,16) }
                    Text {
                        text: "Minecraft Logs"
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 14
                        font.bold: true
                        color: EzTheme.text
                    }
                }

                Rectangle { width: 1; height: 20; color: "#222736" }

                // Search Bar
                Rectangle {
                    height: 32
                    Layout.preferredWidth: 220
                    radius: 8
                    color: "#161B26"
                    border.color: searchInput.activeFocus ? EzTheme.accent : "#222838"
                    border.width: 1

                    RowLayout {
                        anchors.fill: parent
                        anchors.margins: 8
                        spacing: 6
                        Image { source: "icons/search.svg"; width: 14; height: 14; opacity: 0.6; fillMode: Image.PreserveAspectFit; sourceSize: Qt.size(14,14) }
                        TextInput {
                            id: searchInput
                            Layout.fillWidth: true
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            color: EzTheme.text
                            clip: true
                            verticalAlignment: TextInput.AlignVCenter
                            onTextChanged: liveLogsWindow.searchQuery = text.toLowerCase().trim()
                            Text {
                                text: EzI18n.text("Search logs…")
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textMuted
                                anchors.left: parent.left
                                anchors.right: parent.right
                                anchors.verticalCenter: parent.verticalCenter
                                elide: Text.ElideRight
                                visible: !searchInput.text && !searchInput.activeFocus
                            }
                        }
                    }
                }

                // Filter Chips
                Row {
                    spacing: 6
                    Repeater {
                        model: [
                            { id: "ALL",   color: "#38BDF8", label: EzI18n.text("ALL") },
                            { id: "ERROR", color: "#FF453A", label: "ERROR" },
                            { id: "WARN",  color: "#FFD60A", label: "WARN" },
                            { id: "INFO",  color: "#30D158", label: "INFO" },
                            { id: "DEBUG", color: "#BF5AF2", label: "DEBUG" },
                            { id: "TRACE", color: "#8E8E93", label: "TRACE" }
                        ]
                        Rectangle {
                            height: 26
                            width: chipText.implicitWidth + 16
                            radius: 6
                            color: liveLogsWindow.activeFilter === modelData.id ? (modelData.color + "28") : "#161B26"
                            border.color: liveLogsWindow.activeFilter === modelData.id ? modelData.color : "#222838"
                            border.width: 1

                            Text {
                                id: chipText
                                text: modelData.label
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 9
                                font.bold: true
                                color: liveLogsWindow.activeFilter === modelData.id ? modelData.color : EzTheme.textMuted
                                anchors.centerIn: parent
                            }

                            MouseArea {
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: liveLogsWindow.activeFilter = modelData.id
                            }
                        }
                    }
                }

                Item { Layout.fillWidth: true }

                // Window Controls
                Row {
                    spacing: 0
                    Rectangle {
                        width: 44; height: 52; color: minM.containsMouse ? "#1E2433" : "transparent"
                        Text { text: "─"; font.pixelSize: 12; color: EzTheme.textSecondary; anchors.centerIn: parent }
                        MouseArea { id: minM; anchors.fill: parent; hoverEnabled: true; onClicked: liveLogsWindow.showMinimized() }
                    }
                    Rectangle {
                        width: 44; height: 52; color: maxM.containsMouse ? "#1E2433" : "transparent"
                        Text { text: "□"; font.pixelSize: 12; color: EzTheme.textSecondary; anchors.centerIn: parent }
                        MouseArea {
                            id: maxM; anchors.fill: parent; hoverEnabled: true
                            onClicked: liveLogsWindow.visibility === Window.Maximized ? liveLogsWindow.showNormal() : liveLogsWindow.showMaximized()
                        }
                    }
                    Rectangle {
                        width: 46; height: 52; color: closeM.containsMouse ? "#C42B1C" : "transparent"
                        Image { source: "icons/x.svg"; width: 12; height: 12; anchors.centerIn: parent; opacity: closeM.containsMouse ? 1.0 : 0.6; fillMode: Image.PreserveAspectFit }
                        MouseArea { id: closeM; anchors.fill: parent; hoverEnabled: true; onClicked: liveLogsWindow.hide() }
                    }
                }
            }
        }

        // ── CONTENT AREA (Console + Sidebar) ──
        RowLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            spacing: 0

            // Log Console View
            Rectangle {
                Layout.fillWidth: true
                Layout.fillHeight: true
                color: "#080B10"

                ListView {
                    id: logListView
                    anchors.fill: parent
                    anchors.margins: 10
                    clip: true
                    model: logListModel
                    spacing: 2
                    boundsBehavior: Flickable.StopAtBounds
                    onContentYChanged: {
                        if (moving || flicking) {
                            liveLogsWindow.autoScroll = atYEnd
                        }
                    }
                    onMovementEnded: {
                        liveLogsWindow.autoScroll = atYEnd
                    }
                    onFlickEnded: {
                        liveLogsWindow.autoScroll = atYEnd
                    }

                    ScrollBar.vertical: ScrollBar {
                        id: logScrollBar
                        policy: ScrollBar.AsNeeded
                        width: 10
                        onPositionChanged: {
                            if (logScrollBar.pressed) {
                                liveLogsWindow.autoScroll = logListView.atYEnd
                            }
                        }
                        contentItem: Rectangle {
                            implicitWidth: 7
                            radius: 4
                            color: logScrollBar.pressed ? EzTheme.accent : (logScrollBar.hovered ? EzTheme.accentLight : "#596273")
                        }
                        background: Rectangle {
                            implicitWidth: 10
                            color: "#111722"
                            radius: 4
                        }
                    }

                    AutoscrollOverlay {
                        target: logListView
                        onScrolledUp: liveLogsWindow.autoScroll = false
                        onScrolledToBottom: liveLogsWindow.autoScroll = true
                    }

                    delegate: Item {
                        width: logListView.width - (logScrollBar.visible ? 14 : 4)
                        height: logTextRow.implicitHeight + 2

                        RowLayout {
                            id: logTextRow
                            width: parent.width
                            spacing: 8

                            // Timestamp
                            Text {
                                text: "[" + model.time + "]"
                                font.family: "Consolas, monospace"
                                font.pixelSize: 11
                                color: "#38BDF8"
                            }

                            // Level Tag
                            Rectangle {
                                height: 16
                                width: levelTagText.implicitWidth + 8
                                radius: 3
                                color: model.level === "ERROR" ? "#441216" : (model.level === "WARN" ? "#443410" : (model.level === "DEBUG" ? "#321644" : "#142218"))
                                border.color: model.level === "ERROR" ? "#FF453A" : (model.level === "WARN" ? "#FFD60A" : (model.level === "DEBUG" ? "#BF5AF2" : "#22C96E30"))
                                border.width: 1
                                visible: model.level !== "INFO"

                                Text {
                                    id: levelTagText
                                    text: model.level
                                    font.family: "Consolas, monospace"
                                    font.pixelSize: 9
                                    font.bold: true
                                    color: model.level === "ERROR" ? "#FF453A" : (model.level === "WARN" ? "#FFD60A" : (model.level === "DEBUG" ? "#BF5AF2" : "#5AEEA0"))
                                    anchors.centerIn: parent
                                }
                            }

                            // Log message (Selectable & copyable)
                            TextEdit {
                                text: model.msg
                                font.family: "Consolas, monospace"
                                font.pixelSize: 11
                                color: model.level === "ERROR" ? "#FFA49E" : (model.level === "WARN" ? "#FFE58F" : "#D0D4DC")
                                Layout.fillWidth: true
                                wrapMode: Text.WrapAtWordBoundaryOrAnywhere
                                readOnly: true
                                selectByMouse: true
                                activeFocusOnPress: true
                                selectionColor: "#2563EB"
                                selectedTextColor: "#FFFFFF"
                            }
                        }
                    }
                }
            }

            // Divider
            Rectangle { width: 1; Layout.fillHeight: true; color: "#181D2A" }

            // ── RIGHT SIDEBAR: Instances & Resource Stats ──
            Rectangle {
                Layout.preferredWidth: 260
                Layout.fillHeight: true
                color: "#0D111A"

                ColumnLayout {
                    anchors.fill: parent
                    anchors.margins: 16
                    spacing: 14

                    // Sidebar Header
                    RowLayout {
                        spacing: 8
                        Image { source: "icons/box.svg"; width: 16; height: 16; opacity: 0.8; sourceSize: Qt.size(16,16) }
                        Text {
                            text: EzI18n.text("Instances (") + (liveLogsWindow.liveLogService ? liveLogsWindow.liveLogService.runningCount : 0) + ")"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 13
                            font.bold: true
                            color: EzTheme.text
                        }
                    }

                    // Instances List
                    ScrollView {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        clip: true
                        ScrollBar.horizontal.policy: ScrollBar.AlwaysOff

                        ColumnLayout {
                            width: parent.width
                            spacing: 8

                            Repeater {
                                model: liveLogsWindow.liveLogService ? liveLogsWindow.liveLogService.instances : []

                                Rectangle {
                                    id: instCard
                                    Layout.fillWidth: true
                                    implicitHeight: cardInnerCol.implicitHeight + 16
                                    radius: 8
                                    property bool isSelected: liveLogsWindow.liveLogService && liveLogsWindow.liveLogService.selectedInstanceId === modelData.instanceId
                                    color: isSelected ? "#182234" : (instCardHover.containsMouse ? "#141A26" : "#10141E")
                                    border.color: isSelected ? EzTheme.accent : (modelData.running ? "#22C96E30" : "#222838")
                                    border.width: isSelected ? 1.5 : 1

                                    MouseArea {
                                        id: instCardHover
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        cursorShape: Qt.PointingHandCursor
                                        onClicked: {
                                            if (liveLogsWindow.liveLogService) {
                                                liveLogsWindow.liveLogService.selectInstance(modelData.instanceId)
                                            }
                                        }
                                    }

                                    ColumnLayout {
                                        id: cardInnerCol
                                        anchors.left: parent.left
                                        anchors.right: parent.right
                                        anchors.top: parent.top
                                        anchors.margins: 8
                                        spacing: 6

                                        RowLayout {
                                            Layout.fillWidth: true
                                            spacing: 6

                                            Rectangle {
                                                width: 8; height: 8; radius: 4
                                                color: modelData.running ? "#22C96E" : "#596273"
                                            }

                                            Text {
                                                text: modelData.name || "Minecraft"
                                                font.family: EzTheme.mcFontFamily
                                                font.pixelSize: 11
                                                font.bold: true
                                                color: instCard.isSelected ? "#FFFFFF" : EzTheme.text
                                                elide: Text.ElideRight
                                                Layout.fillWidth: true
                                            }

                                            // Stop or Remove Button
                                            Rectangle {
                                                width: 22; height: 22; radius: 4
                                                color: btnHover.containsMouse ? (modelData.running ? "#631720" : "#283042") : (modelData.running ? "#441217" : "#1C2230")
                                                border.color: modelData.running ? "#B91C1C" : "#323B50"
                                                border.width: 1

                                                Text {
                                                    anchors.centerIn: parent
                                                    text: modelData.running ? "⏹" : "✕"
                                                    font.pixelSize: 9
                                                    color: modelData.running ? "#FFA49E" : "#94A3B8"
                                                }

                                                MouseArea {
                                                    id: btnHover
                                                    anchors.fill: parent
                                                    hoverEnabled: true
                                                    cursorShape: Qt.PointingHandCursor
                                                    onClicked: {
                                                        if (!liveLogsWindow.liveLogService) return
                                                        if (modelData.running) {
                                                            liveLogsWindow.liveLogService.stopInstance(modelData.instanceId)
                                                        } else {
                                                            liveLogsWindow.liveLogService.removeInstance(modelData.instanceId)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        RowLayout {
                                            Layout.fillWidth: true
                                            spacing: 4

                                            Text {
                                                text: modelData.loader || "Vanilla"
                                                font.family: EzTheme.fontFamily
                                                font.pixelSize: 9
                                                color: EzTheme.textMuted
                                                elide: Text.ElideRight
                                                Layout.fillWidth: true
                                            }

                                            Text {
                                                text: modelData.running
                                                      ? liveLogsWindow.formatUptime(Math.max(0, liveLogsWindow.currentTimestampSec - Math.floor(modelData.startTime || liveLogsWindow.currentTimestampSec)))
                                                      : ("Beendet · " + liveLogsWindow.formatUptime(modelData.uptime || 0))
                                                font.family: "Consolas, monospace"
                                                font.pixelSize: 9
                                                color: modelData.running ? "#22C96E" : EzTheme.textMuted
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Resource Stats for selected running instance
                    Rectangle {
                        Layout.fillWidth: true
                        implicitHeight: statsCol.implicitHeight + 16
                        radius: 8
                        color: "#131824"
                        border.color: "#222838"
                        border.width: 1
                        visible: liveLogsWindow.liveLogService && liveLogsWindow.liveLogService.isRunning

                        ColumnLayout {
                            id: statsCol
                            anchors.left: parent.left
                            anchors.right: parent.right
                            anchors.top: parent.top
                            anchors.margins: 10
                            spacing: 8

                            RowLayout {
                                Layout.fillWidth: true
                                Image { source: "icons/user.svg"; width: 12; height: 12; opacity: 0.8; sourceSize: Qt.size(12,12) }
                                Text {
                                    text: accountController ? accountController.username : "Player"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    color: EzTheme.textSecondary
                                    elide: Text.ElideRight
                                    Layout.fillWidth: true
                                }
                                Rectangle { width: 6; height: 6; radius: 3; color: "#22C96E" }
                                Text {
                                    text: liveLogsWindow.uptimeStr
                                    font.family: "Consolas, monospace"
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: "#22C96E"
                                }
                            }

                            RowLayout {
                                Layout.fillWidth: true
                                Image { source: "icons/database.svg"; width: 12; height: 12; opacity: 0.8; sourceSize: Qt.size(12,12) }
                                Text {
                                    text: "RAM: " + liveLogsWindow.ramUsage
                                    font.family: "Consolas, monospace"
                                    font.pixelSize: 10
                                    color: "#38BDF8"
                                }
                                Item { Layout.fillWidth: true }
                                Image { source: "icons/cpu.svg"; width: 12; height: 12; opacity: 0.8; sourceSize: Qt.size(12,12) }
                                Text {
                                    text: "CPU: " + liveLogsWindow.cpuUsage
                                    font.family: "Consolas, monospace"
                                    font.pixelSize: 10
                                    color: "#FB923C"
                                }
                            }
                        }
                    }

                    // Open Profile Folder Button
                    Rectangle {
                        Layout.fillWidth: true
                        height: 30
                        radius: 6
                        color: foldM.containsMouse ? "#202738" : "#171D2B"
                        border.color: foldM.containsMouse ? "#3B4761" : "#263045"
                        border.width: 1

                        Row {
                            anchors.centerIn: parent
                            spacing: 8
                            Image {
                                source: "icons/folder.svg"
                                width: 14
                                height: 14
                                opacity: 0.8
                                sourceSize: Qt.size(14,14)
                                anchors.verticalCenter: parent.verticalCenter
                            }
                            Text {
                                text: EzI18n.text("Profil-Ordner öffnen")
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textSecondary
                                elide: Text.ElideRight
                                anchors.verticalCenter: parent.verticalCenter
                            }
                        }

                        MouseArea {
                            id: foldM
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController && profileController.activeProfilePath) {
                                    profileController.openFolder(profileController.activeProfilePath)
                                }
                            }
                        }
                    }

                    // Instance Count Footer
                    Text {
                        text: liveLogsWindow.liveLogService
                            ? (liveLogsWindow.liveLogService.runningCount + (liveLogsWindow.liveLogService.runningCount === 1 ? " Running Instance" : " Running Instances"))
                            : "0 Instances Active"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 10
                        color: EzTheme.textMuted
                        Layout.alignment: Qt.AlignHCenter
                    }
                }
            }
        }

        // ── BOTTOM STATUS BAR ──
        Rectangle {
            Layout.fillWidth: true
            height: 38
            color: "#0F131C"
            border.color: "#181D2A"
            border.width: 1

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 16
                anchors.rightMargin: 16
                spacing: 14

                Text {
                    text: "" + logListModel.count + (logListModel.count === allLogs.length ? EzI18n.text(" LINES") : " / " + allLogs.length + EzI18n.text(" LINES"))
                    font.family: "Consolas, monospace"
                    font.pixelSize: 10
                    color: EzTheme.textMuted
                }

                // Auto Scroll / Following Toggle
                Rectangle {
                    height: 24
                    width: follRow.implicitWidth + 14
                    radius: 4
                    color: liveLogsWindow.autoScroll ? "#162E21" : "#161B26"
                    border.color: liveLogsWindow.autoScroll ? "#22C96E" : "#222838"
                    border.width: 1

                    RowLayout {
                        id: follRow
                        anchors.centerIn: parent
                        spacing: 4
                        Text { text: "⬇️"; font.pixelSize: 9 }
                        Text {
                            text: EzI18n.text("Following")
                            font.family: "Consolas, monospace"
                            font.pixelSize: 10
                            font.bold: liveLogsWindow.autoScroll
                            color: liveLogsWindow.autoScroll ? "#5AEEA0" : EzTheme.textMuted
                        }
                    }

                    MouseArea {
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: liveLogsWindow.autoScroll = !liveLogsWindow.autoScroll
                    }
                }

                Item { Layout.fillWidth: true }

                // Clear Button
                Rectangle {
                    height: 24
                    width: clearRow.implicitWidth + 14
                    radius: 4
                    color: clearM.containsMouse ? "#222838" : "#161B26"
                    border.color: "#222838"
                    border.width: 1

                    RowLayout {
                        id: clearRow
                        anchors.centerIn: parent
                        spacing: 4
                        Image { source: "icons/trash.svg"; width: 12; height: 12; fillMode: Image.PreserveAspectFit }
                        Text { text: EzI18n.text("Clear"); font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                    }

                    MouseArea {
                        id: clearM
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            if (liveLogsWindow.liveLogService) liveLogsWindow.liveLogService.clearLogs()
                        }
                    }
                }

                // Copy All Logs Button
                Rectangle {
                    height: 24
                    width: copyLogR.implicitWidth + 14
                    radius: 4
                    color: copyLogM.containsMouse ? "#222838" : "#161B26"
                    border.color: "#222838"
                    border.width: 1

                    RowLayout {
                        id: copyLogR
                        anchors.centerIn: parent
                        spacing: 4
                        Image { source: "icons/clipboard.svg"; width: 14; height: 14; fillMode: Image.PreserveAspectFit }
                        Text { text: EzI18n.text("Copy Logs"); font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textSecondary }
                    }

                    MouseArea {
                        id: copyLogM
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            if (liveLogsWindow.liveLogService && profileController) {
                                profileController.copyToClipboard(liveLogsWindow.liveLogService.getAllLogsText())
                            }
                        }
                    }
                }
            }
        }
    }
}
