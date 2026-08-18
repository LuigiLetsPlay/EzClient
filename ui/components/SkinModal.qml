import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import ".."

Item {
    id: skinModal
    anchors.fill: parent
    visible: opacity > 0
    opacity: 0.0

    Behavior on opacity { NumberAnimation { duration: 200; easing.type: Easing.OutCubic } }

    property string selectedFilePath: ""
    property string previewBodyUrl: ""
    property string skinVariant: "classic"
    property string statusMsg: ""
    property bool isError: false
    property real modelAngle: 0
    property real lastDragX: 0

    function open() {
        selectedFilePath = ""
        previewBodyUrl = ""
        statusMsg = ""
        isError = false
        modelAngle = 0
        skinNameInput.text = ""
        saveSkinNameInput.text = ""
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
        width: Math.min(580, parent.width - 32)
        height: Math.min(680, parent.height - 24)
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
            anchors.margins: 20
            spacing: 10

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
                        text: "Minecraft Skin wechseln & verwalten"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 15
                        font.bold: true
                        color: EzTheme.text
                    }
                    Text {
                        text: "Skin per Name laden, eigene PNG-Datei wählen oder in Bibliothek speichern"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }
                Item { Layout.fillWidth: true }
                Rectangle {
                    width: 28; height: 28; radius: 14
                    color: closeMouse.containsMouse ? EzTheme.surface3 : "transparent"
                    Text { text: "✕"; font.pixelSize: 13; color: EzTheme.textSecondary; anchors.centerIn: parent }
                    MouseArea {
                        id: closeMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: skinModal.close()
                    }
                }
            }

            // ── Section 1: Fetch by Player Username ──
            Rectangle {
                Layout.fillWidth: true
                height: 42
                radius: 8
                color: "#0F121A"
                border.color: skinNameInput.activeFocus ? EzTheme.accent : EzTheme.border
                border.width: 1

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 4
                    spacing: 8

                    Text {
                        text: "🔍"
                        font.pixelSize: 12
                        Layout.leftMargin: 6
                    }

                    TextField {
                        id: skinNameInput
                        Layout.fillWidth: true
                        placeholderText: "Spielernamen eingeben (z. B. Jektross, Notch)..."
                        placeholderTextColor: "#727B8E"
                        color: "#FFFFFF"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        background: Rectangle {
                            color: "transparent"
                        }
                        onAccepted: fetchBtnMouse.clicked(null)
                    }

                    Rectangle {
                        width: 90
                        height: 32
                        radius: 6
                        color: fetchBtnMouse.containsMouse ? "#2EE080" : "#22C96E"
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
                                if (accountController && skinNameInput.text.trim() !== "") {
                                    skinModal.selectedFilePath = ""
                                    accountController.fetchSkinByUsername(skinNameInput.text.trim())
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 2: Skin Preview & 3D Control Area ──
            Rectangle {
                Layout.fillWidth: true
                height: 175
                radius: 10
                color: "#0B0C10"
                border.color: EzTheme.border
                border.width: 1

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 10
                    spacing: 12

                    // 3D Player Model preview box
                    ColumnLayout {
                        spacing: 4
                        Layout.preferredWidth: 110

                        Rectangle {
                            id: previewBox
                            width: 110; height: 130; radius: 8
                            color: "#151821"
                            border.color: previewDragArea.containsMouse ? EzTheme.accent : EzTheme.border
                            border.width: 1
                            clip: true

                            Item {
                                id: modelRotateWrapper
                                anchors.fill: parent
                                anchors.margins: 4

                                transform: Rotation {
                                    origin.x: modelRotateWrapper.width / 2
                                    origin.y: modelRotateWrapper.height / 2
                                    axis { x: 0; y: 1; z: 0 }
                                    angle: skinModal.modelAngle
                                }

                                Image {
                                    id: modalPreviewImg
                                    anchors.fill: parent
                                    source: skinModal.previewBodyUrl ? skinModal.previewBodyUrl : (accountController ? accountController.bodyUrl : "")
                                    fillMode: Image.PreserveAspectFit
                                    smooth: true
                                    cache: false
                                }
                            }

                            Rectangle {
                                anchors.bottom: parent.bottom
                                anchors.left: parent.left
                                anchors.right: parent.right
                                height: 18
                                color: "#CC0B0C10"
                                Text {
                                    anchors.centerIn: parent
                                    text: "↔ 3D Drehen"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 8
                                    font.bold: true
                                    color: EzTheme.accentLight
                                }
                            }

                            MouseArea {
                                id: previewDragArea
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.SizeHorCursor
                                onPressed: {
                                    skinModal.lastDragX = mouse.x
                                }
                                onPositionChanged: {
                                    if (pressed) {
                                        var dx = mouse.x - skinModal.lastDragX
                                        skinModal.modelAngle = (skinModal.modelAngle + dx * 2.0) % 360
                                        skinModal.lastDragX = mouse.x
                                    }
                                }
                            }
                        }

                        RowLayout {
                            Layout.alignment: Qt.AlignHCenter
                            spacing: 4
                            Rectangle {
                                width: 32; height: 18; radius: 4
                                color: rotLMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                                border.color: EzTheme.border; border.width: 1
                                Text { text: "↺"; font.pixelSize: 10; color: EzTheme.text; anchors.centerIn: parent }
                                MouseArea {
                                    id: rotLMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                    onClicked: skinModal.modelAngle = (skinModal.modelAngle - 45) % 360
                                }
                            }
                            Rectangle {
                                width: 36; height: 18; radius: 4
                                color: rotRstMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                                border.color: EzTheme.border; border.width: 1
                                Text { text: "0°"; font.pixelSize: 9; color: EzTheme.textSecondary; anchors.centerIn: parent }
                                MouseArea {
                                    id: rotRstMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                    onClicked: skinModal.modelAngle = 0
                                }
                            }
                            Rectangle {
                                width: 32; height: 18; radius: 4
                                color: rotRMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                                border.color: EzTheme.border; border.width: 1
                                Text { text: "↻"; font.pixelSize: 10; color: EzTheme.text; anchors.centerIn: parent }
                                MouseArea {
                                    id: rotRMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                    onClicked: skinModal.modelAngle = (skinModal.modelAngle + 45) % 360
                                }
                            }
                        }
                    }

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 6

                        Text {
                            text: skinModal.selectedFilePath ? ("Datei: " + skinModal.selectedFilePath.split("/").pop().split("\\").pop()) : "Aktiver Skin"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: EzTheme.text
                            elide: Text.ElideMiddle
                            Layout.fillWidth: true
                        }

                        RowLayout {
                            spacing: 8
                            Rectangle {
                                height: 28
                                Layout.preferredWidth: 105
                                radius: 6
                                color: skinModal.skinVariant === "classic" ? EzTheme.surfaceActive : (cMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                                border.color: skinModal.skinVariant === "classic" ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                Text { text: "Classic (4px)"; font.pixelSize: 11; color: EzTheme.text; anchors.centerIn: parent }
                                MouseArea {
                                    id: cMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                    onClicked: skinModal.skinVariant = "classic"
                                }
                            }
                            Rectangle {
                                height: 28
                                Layout.preferredWidth: 105
                                radius: 6
                                color: skinModal.skinVariant === "slim" ? EzTheme.surfaceActive : (sMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                                border.color: skinModal.skinVariant === "slim" ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                Text { text: "Slim (3px)"; font.pixelSize: 11; color: EzTheme.text; anchors.centerIn: parent }
                                MouseArea {
                                    id: sMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                    onClicked: skinModal.skinVariant = "slim"
                                }
                            }
                        }

                        Rectangle {
                            height: 32
                            Layout.fillWidth: true
                            radius: 6
                            color: pickMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                            border.color: EzTheme.border
                            border.width: 1
                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 8
                                Text { text: "📁"; font.pixelSize: 12 }
                                Text { text: "Eigene Skin-Datei (.png) auswählen…"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.text }
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
                                            skinModal.previewBodyUrl = accountController.bodyUrl
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 3: Save Current Skin ──
            Rectangle {
                Layout.fillWidth: true
                height: 40
                radius: 8
                color: "#0F121A"
                border.color: saveSkinNameInput.activeFocus ? EzTheme.accent : EzTheme.border
                border.width: 1
                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 4
                    spacing: 8
                    TextField {
                        id: saveSkinNameInput
                        Layout.fillWidth: true
                        placeholderText: "Skin-Name für Bibliothek..."
                        placeholderTextColor: "#727B8E"
                        color: "#FFFFFF"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        background: Rectangle {
                            color: "transparent"
                        }
                        onAccepted: saveSkinBtnMouse.clicked(null)
                    }
                    Rectangle {
                        width: 125; height: 32; radius: 6
                        color: saveSkinBtnMouse.containsMouse ? EzTheme.surfaceActive : EzTheme.surface2
                        border.color: EzTheme.accent; border.width: 1
                        RowLayout { anchors.centerIn: parent; spacing: 4
                            Text { text: "💾"; font.pixelSize: 10 }
                            Text { text: "Skin speichern"; font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.accentLight }
                        }
                        MouseArea {
                            id: saveSkinBtnMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (accountController) {
                                    var n = saveSkinNameInput.text.trim() || (skinNameInput.text.trim()) || "Mein Skin"
                                    accountController.saveCurrentSkin(n, skinModal.selectedFilePath)
                                    saveSkinNameInput.text = ""
                                }
                            }
                        }
                    }
                }
            }

            // ── Section 4: Library ──
            ColumnLayout {
                Layout.fillWidth: true
                spacing: 4
                visible: typeof accountController !== "undefined" && accountController && accountController.savedSkins && accountController.savedSkins.length > 0
                Text { text: "Meine Skin-Bibliothek:"; font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }
                ScrollView {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 50
                    contentWidth: savedRow.implicitWidth
                    clip: true
                    Row {
                        id: savedRow
                        spacing: 8
                        Repeater {
                            model: (typeof accountController !== "undefined" && accountController) ? accountController.savedSkins : []
                            Rectangle {
                                width: 140; height: 44; radius: 8
                                color: savedMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                                border.color: skinModal.selectedFilePath === modelData.path ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                RowLayout {
                                    anchors.fill: parent; anchors.margins: 4; spacing: 6
                                    Image { source: modelData.previewUrl ? modelData.previewUrl : "https://mc-heads.net/avatar/" + (modelData.name || "Steve") + "/32"; Layout.preferredWidth: 26; Layout.preferredHeight: 26; fillMode: Image.PreserveAspectFit }
                                    Text { text: modelData.name || "Skin"; font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.text; elide: Text.ElideRight; Layout.fillWidth: true }
                                    Rectangle { width: 18; height: 18; radius: 9; color: delMouse.containsMouse ? "#4A181C" : "transparent"
                                        Text { text: "✕"; font.pixelSize: 9; color: EzTheme.textMuted; anchors.centerIn: parent }
                                        MouseArea { id: delMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: if (accountController) accountController.deleteSavedSkin(modelData.id || modelData.name) }
                                    }
                                }
                                MouseArea { id: savedMouse; anchors.fill: parent; anchors.rightMargin: 20; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: { if (modelData.path) { skinModal.selectedFilePath = modelData.path; if (accountController) accountController.uploadSkin(modelData.path, skinModal.skinVariant) } else if (modelData.name) { skinNameInput.text = modelData.name; if (accountController) accountController.fetchSkinByUsername(modelData.name) } } }
                            }
                        }
                    }
                }
            }

            // Status message
            Rectangle {
                Layout.fillWidth: true
                height: 32
                radius: 6
                visible: skinModal.statusMsg !== ""
                color: skinModal.isError ? "#301014" : "#102C1E"
                border.color: skinModal.isError ? "#802028" : "#208048"
                border.width: 1
                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 6
                    spacing: 8
                    Text { text: skinModal.isError ? "⚠️" : "✓"; font.pixelSize: 11 }
                    Text { text: skinModal.statusMsg; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: skinModal.isError ? "#FFA0A8" : "#80EEAA"; Layout.fillWidth: true }
                }
            }

            Item { Layout.fillHeight: true }

            // Footer
            RowLayout {
                Layout.fillWidth: true
                spacing: 10
                Rectangle { height: 36; Layout.preferredWidth: 130; radius: 8; color: resetMouse.containsMouse ? "#2A1A1E" : "#181418"; border.color: EzTheme.border; border.width: 1
                    Text { text: "Standard-Skin"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textMuted; anchors.centerIn: parent }
                    MouseArea { id: resetMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: if (accountController) accountController.resetSkin() }
                }
                Item { Layout.fillWidth: true }
                Rectangle { height: 36; Layout.preferredWidth: 90; radius: 8; color: cancelMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2; border.color: EzTheme.border; border.width: 1
                    Text { text: "Schließen"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textSecondary; anchors.centerIn: parent }
                    MouseArea { id: cancelMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: skinModal.close() }
                }
                Rectangle { height: 36; Layout.preferredWidth: 150; radius: 8; color: uploadMouse.containsMouse ? "#2EE080" : "#22C96E"; opacity: skinModal.selectedFilePath ? 1.0 : 0.6
                    RowLayout { anchors.centerIn: parent; spacing: 6
                        Text { text: "⬆️"; font.pixelSize: 12 }
                        Text { text: "Skin anwenden"; font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: "#000000" }
                    }
                    MouseArea { id: uploadMouse; anchors.fill: parent; enabled: skinModal.selectedFilePath !== ""; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: if (accountController && skinModal.selectedFilePath) accountController.uploadSkin(skinModal.selectedFilePath, skinModal.skinVariant) }
                }
            }
        }
    }
}
