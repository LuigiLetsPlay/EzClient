import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import ".."

Rectangle {
    id: skinModal
    anchors.fill: parent
    color: "#D005070A"
    z: 99999
    visible: opacity > 0.001
    opacity: 0.0

    Behavior on opacity { NumberAnimation { duration: 180 } }

    property string selectedFilePath: ""
    property string skinVariant: "classic" // "classic" or "slim"
    property string previewBodyUrl: ""
    property string statusMsg: ""
    property bool isError: false

    function open() {
        selectedFilePath = ""
        previewBodyUrl = ""
        statusMsg = ""
        isError = false
        skinNameInput.text = ""
        skinModal.opacity = 1.0
    }

    function close() {
        skinModal.opacity = 0.0
    }

    Connections {
        target: (typeof accountController !== "undefined") ? accountController : null
        function onSkinUploadStatusChanged(msg, isErr) {
            skinModal.statusMsg = msg
            skinModal.isError = isErr
        }
        function onSkinFetched(path, preview) {
            skinModal.selectedFilePath = path
            skinModal.previewBodyUrl = preview
        }
    }

    MouseArea {
        anchors.fill: parent
        onClicked: skinModal.close()
    }

    Rectangle {
        anchors.centerIn: parent
        width: Math.min(560, parent.width - 32)
        height: Math.min(620, parent.height - 32)
        radius: 16
        color: "#12141C"
        border.color: EzTheme.borderLight
        border.width: 1

        MouseArea {
            anchors.fill: parent
            onClicked: {} // consume clicks
        }

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 22
            spacing: 12

            // Header
            RowLayout {
                spacing: 12
                Rectangle {
                    width: 36; height: 36; radius: 18
                    color: EzTheme.surface2
                    border.color: EzTheme.border
                    border.width: 1
                    Text { text: "👕"; font.pixelSize: 18; anchors.centerIn: parent }
                }
                ColumnLayout {
                    spacing: 2
                    Text {
                        text: "Minecraft Skin wechseln"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 16
                        font.bold: true
                        color: EzTheme.text
                    }
                    Text {
                        text: "Direkt bei Mojang synchronisiert · Kein Client-Neustart nötig"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textSecondary
                    }
                }
                Item { Layout.fillWidth: true }
                Rectangle {
                    width: 28; height: 28; radius: 14
                    color: closeMouse.containsMouse ? "#2A2E39" : "transparent"
                    Text { text: "✕"; color: EzTheme.textMuted; anchors.centerIn: parent; font.pixelSize: 13 }
                    MouseArea {
                        id: closeMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: skinModal.close()
                    }
                }
            }

            // ── Username Grabber Row ──
            Rectangle {
                Layout.fillWidth: true
                height: 42
                radius: 8
                color: "#0B0C10"
                border.color: skinNameInput.activeFocus ? EzTheme.accent : EzTheme.border
                border.width: 1

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 4
                    spacing: 6

                    Text {
                        text: " 🔍"
                        font.pixelSize: 12
                    }

                    TextInput {
                        id: skinNameInput
                        Layout.fillWidth: true
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        color: EzTheme.text
                        selectByMouse: true
                        clip: true
                        verticalAlignment: TextInput.AlignVCenter
                        onAccepted: {
                            if (skinNameInput.text.trim() && accountController) {
                                accountController.fetchSkinByUsername(skinNameInput.text.trim())
                            }
                        }

                        Text {
                            text: "Minecraft-Spielernamen eingeben (z.B. Lu1giLP)…"
                            color: EzTheme.textMuted
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            visible: !skinNameInput.text && !skinNameInput.activeFocus
                            anchors.verticalCenter: parent.verticalCenter
                        }
                    }

                    Rectangle {
                        height: 32
                        width: 95
                        radius: 6
                        color: fetchBtnMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent
                        Text {
                            text: "Skin laden"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            font.bold: true
                            color: "#000000"
                            anchors.centerIn: parent
                        }
                        MouseArea {
                            id: fetchBtnMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (skinNameInput.text.trim() && accountController) {
                                    accountController.fetchSkinByUsername(skinNameInput.text.trim())
                                }
                            }
                        }
                    }
                }
            }

            // ── Skin Preview & Model Selector Area ──
            Rectangle {
                Layout.fillWidth: true
                height: 165
                radius: 10
                color: "#0B0C10"
                border.color: EzTheme.border
                border.width: 1

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 12
                    spacing: 14

                    // Player Model Body preview
                    Rectangle {
                        width: 95; height: 141; radius: 8
                        color: "#151821"
                        border.color: EzTheme.border
                        border.width: 1
                        clip: true

                        property real previewRotation: 0

                        Image {
                            id: modalPreviewImg
                            anchors.fill: parent
                            anchors.margins: 4
                            source: skinModal.previewBodyUrl ? skinModal.previewBodyUrl : (accountController ? accountController.bodyUrl : "")
                            fillMode: Image.PreserveAspectFit
                            smooth: true
                            cache: false
                            rotation: parent.previewRotation
                            Behavior on rotation { NumberAnimation { duration: 250; easing.type: Easing.OutCubic } }
                        }

                        MouseArea {
                            anchors.fill: parent
                            hoverEnabled: true
                            onPositionChanged: {
                                var cDist = (mouse.x - width / 2) / (width / 2)
                                parent.previewRotation = cDist * 12
                            }
                            onExited: {
                                parent.previewRotation = 0
                            }
                        }
                    }

                    // Skin Details & Variant selector
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 8

                        Text {
                            text: skinModal.selectedFilePath ? ("Ausgewählt: " + skinModal.selectedFilePath.split("/").pop().split("\\").pop()) : "Aktiver Account-Skin"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: EzTheme.text
                            elide: Text.ElideMiddle
                            Layout.fillWidth: true
                        }

                        Text {
                            text: "Modell-Armbreite:"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            color: EzTheme.textSecondary
                        }

                        RowLayout {
                            spacing: 8
                            // Classic 4px button
                            Rectangle {
                                height: 30
                                Layout.preferredWidth: 105
                                radius: 6
                                color: skinModal.skinVariant === "classic" ? EzTheme.surfaceActive : (cMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                                border.color: skinModal.skinVariant === "classic" ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                Text {
                                    text: "Classic (4px)"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: skinModal.skinVariant === "classic"
                                    color: skinModal.skinVariant === "classic" ? EzTheme.accentLight : EzTheme.text
                                    anchors.centerIn: parent
                                }
                                MouseArea {
                                    id: cMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: skinModal.skinVariant = "classic"
                                }
                            }

                            // Slim 3px button
                            Rectangle {
                                height: 30
                                Layout.preferredWidth: 105
                                radius: 6
                                color: skinModal.skinVariant === "slim" ? EzTheme.surfaceActive : (sMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                                border.color: skinModal.skinVariant === "slim" ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                Text {
                                    text: "Slim (Alex 3px)"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: skinModal.skinVariant === "slim"
                                    color: skinModal.skinVariant === "slim" ? EzTheme.accentLight : EzTheme.text
                                    anchors.centerIn: parent
                                }
                                MouseArea {
                                    id: sMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: skinModal.skinVariant = "slim"
                                }
                            }
                        }

                        // Pick file button
                        Rectangle {
                            height: 32
                            Layout.fillWidth: true
                            radius: 6
                            color: pickMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                            border.color: EzTheme.borderLight
                            border.width: 1
                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 8
                                Text { text: "📁"; font.pixelSize: 12 }
                                Text {
                                    text: "Eigene Skin-Datei (.png) wählen…"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    font.bold: true
                                    color: EzTheme.text
                                }
                            }
                            MouseArea {
                                id: pickMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    if (accountController) {
                                        var p = accountController.pickSkinFile()
                                        if (p) {
                                            skinModal.selectedFilePath = p
                                            skinModal.previewBodyUrl = ""
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Recent Skins History ──
            ColumnLayout {
                Layout.fillWidth: true
                spacing: 6
                visible: typeof accountController !== "undefined" && accountController && accountController.skinHistory && accountController.skinHistory.length > 0

                Text {
                    text: "Zuletzt verwendete Skins:"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    color: EzTheme.textSecondary
                }

                ScrollView {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 52
                    contentWidth: histRow.implicitWidth
                    clip: true

                    Row {
                        id: histRow
                        spacing: 8

                        Repeater {
                            model: (typeof accountController !== "undefined" && accountController) ? accountController.skinHistory : []

                            Rectangle {
                                width: 120
                                height: 44
                                radius: 8
                                color: histItemMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                                border.color: skinModal.selectedFilePath === modelData.path ? EzTheme.accent : EzTheme.border
                                border.width: 1

                                RowLayout {
                                    anchors.fill: parent
                                    anchors.margins: 6
                                    spacing: 6

                                    Image {
                                        source: modelData.previewUrl ? modelData.previewUrl : "https://mc-heads.net/avatar/" + (modelData.username || "Steve") + "/32"
                                        Layout.preferredWidth: 26
                                        Layout.preferredHeight: 26
                                        fillMode: Image.PreserveAspectFit
                                    }

                                    Text {
                                        text: modelData.username || "Skin"
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 11
                                        font.bold: true
                                        color: EzTheme.text
                                        elide: Text.ElideRight
                                        Layout.fillWidth: true
                                    }
                                }

                                MouseArea {
                                    id: histItemMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        if (modelData.path) {
                                            skinModal.selectedFilePath = modelData.path
                                            if (accountController) {
                                                accountController.uploadSkin(modelData.path, skinModal.skinVariant)
                                            }
                                        } else if (modelData.username) {
                                            skinNameInput.text = modelData.username
                                            if (accountController) {
                                                accountController.fetchSkinByUsername(modelData.username)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Status message
            Rectangle {
                Layout.fillWidth: true
                height: 36
                radius: 6
                visible: skinModal.statusMsg !== ""
                color: skinModal.isError ? "#301014" : "#102C1E"
                border.color: skinModal.isError ? "#802028" : "#208048"
                border.width: 1
                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 8
                    spacing: 8
                    Text { text: skinModal.isError ? "⚠️" : "✓"; font.pixelSize: 12 }
                    Text {
                        text: skinModal.statusMsg
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: skinModal.isError ? "#FFA0A8" : "#80EEAA"
                        Layout.fillWidth: true
                        elide: Text.ElideRight
                    }
                }
            }

            Item { Layout.fillHeight: true }

            // Action Footer
            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                // Reset Skin
                Rectangle {
                    height: 38
                    Layout.preferredWidth: 130
                    radius: 8
                    color: resetMouse.containsMouse ? "#2A1A1E" : "#181418"
                    border.color: EzTheme.border
                    border.width: 1
                    Text {
                        text: "Standard-Skin"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                        anchors.centerIn: parent
                    }
                    MouseArea {
                        id: resetMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            if (accountController) accountController.resetSkin()
                        }
                    }
                }

                Item { Layout.fillWidth: true }

                // Cancel button
                Rectangle {
                    height: 38
                    Layout.preferredWidth: 90
                    radius: 8
                    color: cancelMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                    border.color: EzTheme.border
                    border.width: 1
                    Text {
                        text: "Schließen"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textSecondary
                        anchors.centerIn: parent
                    }
                    MouseArea {
                        id: cancelMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: skinModal.close()
                    }
                }

                // Upload Button
                Rectangle {
                    height: 38
                    Layout.preferredWidth: 150
                    radius: 8
                    color: uploadMouse.containsMouse ? "#2EE080" : "#22C96E"
                    opacity: skinModal.selectedFilePath ? 1.0 : 0.4
                    RowLayout {
                        anchors.centerIn: parent
                        spacing: 6
                        Text { text: "⬆️"; font.pixelSize: 12 }
                        Text {
                            text: "Skin anwenden"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: "#000000"
                        }
                    }
                    MouseArea {
                        id: uploadMouse
                        anchors.fill: parent
                        enabled: skinModal.selectedFilePath !== ""
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            if (accountController && skinModal.selectedFilePath) {
                                accountController.uploadSkin(skinModal.selectedFilePath, skinModal.skinVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
