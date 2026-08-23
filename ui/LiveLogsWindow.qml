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

    ListModel {
        id: logListModel
    }

    function loadBufferedLogs() {
        logListModel.clear()
        if (!liveLogService)
            return
        var entries = liveLogService.getBufferedLogs()
        for (var i = 0; i < entries.length; ++i) {
            var entry = entries[i]
            logListModel.append({
                "raw": entry.raw,
                "level": entry.level,
                "time": entry.time,
                "msg": entry.message
            })
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
            logListModel.append({
                "raw": raw,
                "level": level,
                "time": timeStr,
                "msg": msg
            })
            if (logListModel.count > 5000) {
                logListModel.remove(0, 500)
            }
            if (liveLogsWindow.autoScroll && !searchQuery) {
                logListView.positionViewAtEnd()
            }
        }
        function onStatsUpdated(cpu, ram, uptimeSec) {
            liveLogsWindow.cpuUsage = cpu + "%"
            liveLogsWindow.ramUsage = (ram > 1024 ? (ram / 1024).toFixed(1) + " GB" : Math.round(ram) + " MB")
            liveLogsWindow.uptimeStr = liveLogsWindow.formatUptime(uptimeSec)
        }
        function onLogsCleared() {
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
                            onTextChanged: liveLogsWindow.searchQuery = text.toLowerCase().trim()
                            Text {
                                text: "Search logs…"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textMuted
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
                            { id: "ALL",   color: "#38BDF8", label: "ALL" },
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
                        Text { text: "✕"; font.pixelSize: 12; color: closeM.containsMouse ? "#ffffff" : EzTheme.textSecondary; anchors.centerIn: parent }
                        MouseArea { id: closeM; anchors.fill: parent; hoverEnabled: true; onClicked: liveLogsWindow.close() }
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

                    delegate: Item {
                        width: logListView.width
                        visible: {
                            if (liveLogsWindow.activeFilter !== "ALL" && model.level !== liveLogsWindow.activeFilter) return false
                            if (liveLogsWindow.searchQuery && model.raw.toLowerCase().indexOf(liveLogsWindow.searchQuery) === -1) return false
                            return true
                        }
                        height: visible ? (logTextRow.implicitHeight + 2) : 0

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

                            // Log message
                            Text {
                                text: model.msg
                                font.family: "Consolas, monospace"
                                font.pixelSize: 11
                                color: model.level === "ERROR" ? "#FFA49E" : (model.level === "WARN" ? "#FFE58F" : "#D0D4DC")
                                Layout.fillWidth: true
                                wrapMode: Text.WrapAnywhere
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
                            text: "Instances"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 13
                            font.bold: true
                            color: EzTheme.text
                        }
                    }

                    // Active Instance Card
                    Rectangle {
                        Layout.fillWidth: true
                        height: 140
                        radius: 10
                        color: "#141924"
                        border.color: (liveLogsWindow.liveLogService && liveLogsWindow.liveLogService.isRunning) ? "#22C96E40" : "#222838"
                        border.width: 1

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 12
                            spacing: 8

                            RowLayout {
                                spacing: 10
                                Rectangle {
                                    width: 32; height: 32; radius: 6; color: "#1F2636"
                                    Image {
                                        anchors.fill: parent; anchors.margins: 4
                                        source: "assets/logo.svg"; fillMode: Image.PreserveAspectFit
                                    }
                                }
                                ColumnLayout {
                                    spacing: 1
                                    Text {
                                        text: (liveLogsWindow.liveLogService ? liveLogsWindow.liveLogService.instanceName : "Minecraft")
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 12
                                        font.bold: true
                                        color: EzTheme.text
                                        elide: Text.ElideRight
                                    }
                                    Text {
                                        text: (liveLogsWindow.liveLogService ? liveLogsWindow.liveLogService.loaderVersion : "Fabric 26.2")
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.textMuted
                                    }
                                }
                            }

                            Rectangle { Layout.fillWidth: true; height: 1; color: "#1E2433" }

                            // Player & Uptime
                            RowLayout {
                                Layout.fillWidth: true
                                RowLayout {
                                    spacing: 4
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
                                }
                                RowLayout {
                                    spacing: 4
                                    Rectangle { width: 6; height: 6; radius: 3; color: "#22C96E" }
                                    Text {
                                        text: liveLogsWindow.uptimeStr
                                        font.family: "Consolas, monospace"
                                        font.pixelSize: 10
                                        font.bold: true
                                        color: "#22C96E"
                                    }
                                }
                            }

                            // RAM & CPU Gauges
                            RowLayout {
                                Layout.fillWidth: true
                                RowLayout {
                                    spacing: 4
                                    Image { source: "icons/database.svg"; width: 12; height: 12; opacity: 0.8; sourceSize: Qt.size(12,12) }
                                    Text {
                                        text: "RAM: " + liveLogsWindow.ramUsage
                                        font.family: "Consolas, monospace"
                                        font.pixelSize: 10
                                        color: "#38BDF8"
                                    }
                                }
                                Item { Layout.fillWidth: true }
                                RowLayout {
                                    spacing: 4
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
                    }

                    // Stop Button
                    Rectangle {
                        Layout.fillWidth: true
                        height: 36
                        radius: 8
                        color: stopM.containsMouse ? "#5A161E" : "#3D1016"
                        border.color: "#80222A"
                        border.width: 1
                        visible: liveLogsWindow.liveLogService && liveLogsWindow.liveLogService.isRunning

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Image { source: "icons/stop-circle.svg"; width: 14; height: 14; opacity: 0.8; sourceSize: Qt.size(14,14) }
                            Text {
                                text: "Minecraft beenden"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: "#FFA49E"
                            }
                        }

                        MouseArea {
                            id: stopM
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (liveLogsWindow.liveLogService) liveLogsWindow.liveLogService.stopInstance()
                            }
                        }
                    }

                    // Open Profile Folder Button
                    Rectangle {
                        Layout.fillWidth: true
                        height: 36
                        radius: 8
                        color: foldM.containsMouse ? "#1E2536" : "#141924"
                        border.color: "#222838"
                        border.width: 1

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Image { source: "icons/folder.svg"; width: 14; height: 14; opacity: 0.8; sourceSize: Qt.size(14,14) }
                            Text {
                                text: "Profil-Ordner öffnen"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textSecondary
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

                    Item { Layout.fillHeight: true }

                    // Instance Count Footer
                    Text {
                        text: (liveLogsWindow.liveLogService && liveLogsWindow.liveLogService.isRunning) ? "1 Running Instance" : "0 Instances Active"
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
                    text: "📄 " + logListModel.count + " LINES"
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
                            text: "Following"
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
                        Text { text: "🗑️"; font.pixelSize: 9 }
                        Text { text: "Clear"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
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
                        Text { text: "📋"; font.pixelSize: 9 }
                        Text { text: "Copy Logs"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textSecondary }
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
