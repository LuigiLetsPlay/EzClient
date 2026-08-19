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

    property string previewFilePath: ""
    property string previewTextureUrl: ""
    property string previewName: "Steve"
    property string skinVariant: "classic"
    property string currentAnim: "idle"
    property bool isApplied: true
    property string statusMsg: ""
    property bool isError: false

    function open() {
        if (accountController) {
            previewFilePath = accountController.activeSkinPath || ""
            previewTextureUrl = accountController.skinTextureUrl || ""
            previewName = accountController.activeSkinName || "Aktiver Skin"
        } else {
            previewFilePath = ""
            previewTextureUrl = ""
            previewName = "Steve"
        }
        isApplied = true
        statusMsg = ""
        isError = false
        skinNameInput.text = ""
        saveSkinNameInput.text = ""
        skinModal.opacity = 1.0
        if (modalSkin3D) {
            modalSkin3D.resetView()
            modalSkin3D.setAnim(skinModal.currentAnim)
        }
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
            skinModal.previewFilePath = path
            var baseName = ""
            if (skinNameInput.text.trim() !== "") {
                baseName = skinNameInput.text.trim()
            } else if (path) {
                var segs = path.replace(/\\/g, "/").split("/")
                baseName = segs[segs.length - 1].replace(".png", "")
            } else {
                baseName = "Vorschau"
            }
            skinModal.previewName = baseName
            if (accountController) {
                skinModal.previewTextureUrl = accountController.getSkinTextureUrl(path)
            } else {
                skinModal.previewTextureUrl = preview
            }
            skinModal.isApplied = false
            if (modalSkin3D) {
                modalSkin3D.updateSkin()
            }
        }
    }

    // Modal background overlay
    MouseArea {
        anchors.fill: parent
        onClicked: skinModal.close()
    }

    Rectangle {
        anchors.centerIn: parent
        width: Math.min(860, parent.width - 32)
        height: Math.min(660, parent.height - 24)
        radius: 16
        color: "#12141C"
        border.color: EzTheme.borderLight
        border.width: 1

        MouseArea {
            anchors.fill: parent
            onClicked: {} // consume clicks inside modal
        }

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 18
            spacing: 12

            // ── Header ──
            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                Rectangle {
                    width: 38; height: 38; radius: 19
                    color: EzTheme.surface2
                    border.color: EzTheme.border
                    border.width: 1
                    Text { text: "👕"; font.pixelSize: 18; anchors.centerIn: parent }
                }

                ColumnLayout {
                    spacing: 2
                    Text {
                        text: "Minecraft Skin-Studio"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 16
                        font.bold: true
                        color: EzTheme.text
                    }
                    Text {
                        text: "3D-Vorschau ansehen, Modell anpassen & Skin für deinen Account auswählen"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Item { Layout.fillWidth: true }

                // State Badge: Preview vs. Applied
                Rectangle {
                    height: 26
                    width: stateBadgeText.implicitWidth + 24
                    radius: 13
                    color: skinModal.isApplied ? "#122E1F" : "#302610"
                    border.color: skinModal.isApplied ? "#22C96E" : "#E5A93C"
                    border.width: 1
                    RowLayout {
                        anchors.centerIn: parent
                        spacing: 5
                        Text {
                            text: skinModal.isApplied ? "🟢" : "🟡"
                            font.pixelSize: 9
                        }
                        Text {
                            id: stateBadgeText
                            text: skinModal.isApplied ? ("Aktiv: " + skinModal.previewName) : ("Vorschau: " + skinModal.previewName)
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            font.bold: true
                            color: skinModal.isApplied ? "#80EEAA" : "#F5D075"
                            elide: Text.ElideRight
                        }
                    }
                }

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

            // ── Main Body Split: Left 3D Stage | Right Controls ──
            RowLayout {
                Layout.fillWidth: true
                Layout.fillHeight: true
                spacing: 16

                // ════ LEFT COLUMN: 3D Stage & Pose Controls ════
                ColumnLayout {
                    Layout.preferredWidth: 320
                    Layout.maximumWidth: 340
                    Layout.minimumWidth: 300
                    Layout.fillWidth: false
                    Layout.fillHeight: true
                    spacing: 8

                    // 3D Canvas Box
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        Layout.minimumHeight: 240
                        radius: 12
                        color: "#0A0C12"
                        border.color: skinModal.isApplied ? "#1E2A22" : (skinModal.previewTextureUrl ? EzTheme.accent : EzTheme.border)
                        border.width: 1
                        clip: true

                        Skin3DView {
                            id: modalSkin3D
                            anchors.fill: parent
                            anchors.margins: 2
                            skinSource: skinModal.previewTextureUrl ? skinModal.previewTextureUrl : (accountController ? accountController.skinTextureUrl : "")
                            skinVariant: skinModal.skinVariant
                            animation: skinModal.currentAnim
                            autoRotate: false
                        }

                        // Top-left Mode Indicator
                        Rectangle {
                            anchors.top: parent.top
                            anchors.left: parent.left
                            anchors.margins: 8
                            height: 20
                            width: modeBadgeText.implicitWidth + 12
                            radius: 10
                            color: skinModal.isApplied ? "#CC11291C" : "#CC2B210E"
                            border.color: skinModal.isApplied ? "#22C96E" : "#E5A93C"
                            border.width: 1
                            Text {
                                id: modeBadgeText
                                anchors.centerIn: parent
                                text: skinModal.isApplied ? "✓ Aktiv" : "👁️ Vorschau"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 9
                                font.bold: true
                                color: skinModal.isApplied ? "#80EEAA" : "#F5D075"
                            }
                        }

                        // Bottom Mouse Interaction Hint
                        Rectangle {
                            anchors.bottom: parent.bottom
                            anchors.left: parent.left
                            anchors.right: parent.right
                            height: 22
                            color: "#D80A0C12"
                            Text {
                                anchors.centerIn: parent
                                text: "🖱️ Klicken & Ziehen zum 3D-Drehen"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 9
                                font.bold: true
                                color: EzTheme.accentLight
                            }
                        }
                    }

                    // 3D Angle Quick Presets
                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 4

                        Rectangle {
                            Layout.fillWidth: true; height: 26; radius: 5
                            color: rotLMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                            border.color: EzTheme.border; border.width: 1
                            Text { text: "↺ -90°"; font.pixelSize: 9; color: EzTheme.text; anchors.centerIn: parent }
                            MouseArea {
                                id: rotLMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                onClicked: modalSkin3D.setRotateY(-90)
                            }
                        }
                        Rectangle {
                            Layout.fillWidth: true; height: 26; radius: 5
                            color: rotRstMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                            border.color: EzTheme.border; border.width: 1
                            Text { text: "0° Front"; font.pixelSize: 9; color: EzTheme.textSecondary; anchors.centerIn: parent }
                            MouseArea {
                                id: rotRstMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                onClicked: modalSkin3D.resetView()
                            }
                        }
                        Rectangle {
                            Layout.fillWidth: true; height: 26; radius: 5
                            color: rotRMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                            border.color: EzTheme.border; border.width: 1
                            Text { text: "↻ +90°"; font.pixelSize: 9; color: EzTheme.text; anchors.centerIn: parent }
                            MouseArea {
                                id: rotRMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                onClicked: modalSkin3D.setRotateY(90)
                            }
                        }
                        Rectangle {
                            Layout.fillWidth: true; height: 26; radius: 5
                            color: rotBackMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                            border.color: EzTheme.border; border.width: 1
                            Text { text: "180° Rück"; font.pixelSize: 9; color: EzTheme.textSecondary; anchors.centerIn: parent }
                            MouseArea {
                                id: rotBackMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                onClicked: modalSkin3D.setRotateY(180)
                            }
                        }
                    }

                    // Arm Model Variant Switcher
                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 6

                        Rectangle {
                            Layout.fillWidth: true; height: 28; radius: 6
                            color: skinModal.skinVariant === "classic" ? EzTheme.surfaceActive : (cMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                            border.color: skinModal.skinVariant === "classic" ? EzTheme.accent : EzTheme.border
                            border.width: 1
                            Text {
                                text: "Classic (4px)"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 10
                                font.bold: skinModal.skinVariant === "classic"
                                color: skinModal.skinVariant === "classic" ? "#FFFFFF" : EzTheme.textMuted
                                anchors.centerIn: parent
                            }
                            MouseArea {
                                id: cMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                onClicked: skinModal.skinVariant = "classic"
                            }
                        }

                        Rectangle {
                            Layout.fillWidth: true; height: 28; radius: 6
                            color: skinModal.skinVariant === "slim" ? EzTheme.surfaceActive : (sMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                            border.color: skinModal.skinVariant === "slim" ? EzTheme.accent : EzTheme.border
                            border.width: 1
                            Text {
                                text: "Slim (3px)"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 10
                                font.bold: skinModal.skinVariant === "slim"
                                color: skinModal.skinVariant === "slim" ? "#FFFFFF" : EzTheme.textMuted
                                anchors.centerIn: parent
                            }
                            MouseArea {
                                id: sMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                onClicked: skinModal.skinVariant = "slim"
                            }
                        }
                    }

                    // 3D Animation Switcher
                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 4

                        Repeater {
                            model: [
                                { label: "🧍 Idle", id: "idle" },
                                { label: "🚶 Walk", id: "walk" },
                                { label: "🏃 Run", id: "run" },
                                { label: "👋 Wave", id: "wave" }
                            ]
                            Rectangle {
                                Layout.fillWidth: true; height: 26; radius: 5
                                color: skinModal.currentAnim === modelData.id ? EzTheme.surfaceActive : (animMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                                border.color: skinModal.currentAnim === modelData.id ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                Text {
                                    text: modelData.label
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 9
                                    font.bold: skinModal.currentAnim === modelData.id
                                    color: skinModal.currentAnim === modelData.id ? "#FFFFFF" : EzTheme.textMuted
                                    anchors.centerIn: parent
                                }
                                MouseArea {
                                    id: animMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        skinModal.currentAnim = modelData.id
                                        modalSkin3D.setAnim(modelData.id)
                                    }
                                }
                            }
                        }
                    }
                }

                // ════ RIGHT COLUMN: Controls, Action, Save & Library ════
                ColumnLayout {
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    spacing: 10

                    // ── Card 1: Neuen Skin laden (Username oder Datei) ──
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 90
                        radius: 10
                        color: "#0F121A"
                        border.color: EzTheme.border
                        border.width: 1

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 10
                            spacing: 6

                            Text {
                                text: "Skin auswählen & in 3D-Vorschau laden:"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzTheme.textSecondary
                            }

                            // Option A: Username input
                            RowLayout {
                                Layout.fillWidth: true
                                spacing: 6

                                Rectangle {
                                    Layout.fillWidth: true
                                    height: 30
                                    radius: 6
                                    color: "#0A0C12"
                                    border.color: skinNameInput.activeFocus ? EzTheme.accent : EzTheme.border
                                    border.width: 1

                                    RowLayout {
                                        anchors.fill: parent
                                        anchors.margins: 4
                                        spacing: 6

                                        Text { text: "🔍"; font.pixelSize: 10; Layout.leftMargin: 4 }

                                        TextField {
                                            id: skinNameInput
                                            Layout.fillWidth: true
                                            placeholderText: "Spielernamen eingeben (z. B. Jektross)..."
                                            placeholderTextColor: "#646E82"
                                            color: "#FFFFFF"
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 11
                                            background: Rectangle { color: "transparent" }
                                            onAccepted: fetchBtnMouse.clicked(null)
                                        }
                                    }
                                }

                                Rectangle {
                                    width: 105; height: 30; radius: 6
                                    color: fetchBtnMouse.containsMouse ? "#2EE080" : "#22C96E"
                                    RowLayout {
                                        anchors.centerIn: parent
                                        spacing: 4
                                        Text { text: "👁️"; font.pixelSize: 10 }
                                        Text { text: "Vorschau"; font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: "#000000" }
                                    }
                                    MouseArea {
                                        id: fetchBtnMouse
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        cursorShape: Qt.PointingHandCursor
                                        onClicked: {
                                            var u = skinNameInput.text.trim()
                                            if (accountController && u !== "") {
                                                skinModal.previewName = u
                                                accountController.fetchSkinByUsername(u)
                                            }
                                        }
                                    }
                                }
                            }

                            // Option B: File Picker Button
                            Rectangle {
                                Layout.fillWidth: true
                                height: 26
                                radius: 6
                                color: pickFileMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                                border.color: EzTheme.border
                                border.width: 1

                                RowLayout {
                                    anchors.centerIn: parent
                                    spacing: 6
                                    Text { text: "📁"; font.pixelSize: 11 }
                                    Text {
                                        text: "Eigene Skin-Datei (.png) als Vorschau laden…"
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.text
                                    }
                                }
                                MouseArea {
                                    id: pickFileMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        if (accountController) {
                                            var p = accountController.pickSkinFile()
                                            if (p) {
                                                skinModal.previewFilePath = p
                                                var parts = p.replace(/\\/g, "/").split("/")
                                                skinModal.previewName = parts[parts.length - 1].replace(".png", "")
                                                skinModal.previewTextureUrl = accountController.getSkinTextureUrl(p)
                                                skinModal.isApplied = false
                                                if (modalSkin3D) modalSkin3D.updateSkin()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Card 2: Vorschau-Status & "Skin anwenden" (HERZSTÜCK) ──
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 74
                        radius: 10
                        color: skinModal.isApplied ? "#101D16" : "#1B1710"
                        border.color: skinModal.isApplied ? "#208048" : "#E5A93C"
                        border.width: 1

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 8
                            spacing: 5

                            RowLayout {
                                Layout.fillWidth: true
                                spacing: 6
                                Text {
                                    text: skinModal.isApplied ? "✓" : "👁️"
                                    font.pixelSize: 11
                                    color: skinModal.isApplied ? "#80EEAA" : "#F5D075"
                                }
                                Text {
                                    text: skinModal.isApplied ?
                                          ("Skin '" + skinModal.previewName + "' ist aktiv ausgewählt.") :
                                          ("Vorschau aktiv für '" + skinModal.previewName + "'. Noch nicht ausgewählt!")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: skinModal.isApplied ? "#80EEAA" : "#F5D075"
                                    elide: Text.ElideRight
                                    Layout.fillWidth: true
                                }
                            }

                            // Prominent Apply Button
                            Rectangle {
                                Layout.fillWidth: true
                                height: 32
                                radius: 6
                                color: skinModal.isApplied ?
                                       "#1E2A22" :
                                       (applyBtnMouse.containsMouse ? "#2EE080" : "#22C96E")
                                border.color: skinModal.isApplied ? "#2A3D30" : "#22C96E"
                                border.width: 1

                                RowLayout {
                                    anchors.centerIn: parent
                                    spacing: 6
                                    Text {
                                        text: skinModal.isApplied ? "✓" : "✨"
                                        font.pixelSize: 12
                                    }
                                    Text {
                                        text: skinModal.isApplied ? "Skin ist bereits ausgewählt" : "Skin auswählen & anwenden"
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 11
                                        font.bold: true
                                        color: skinModal.isApplied ? "#80EEAA" : "#000000"
                                    }
                                }

                                MouseArea {
                                    id: applyBtnMouse
                                    anchors.fill: parent
                                    enabled: !skinModal.isApplied && (skinModal.previewFilePath !== "" || skinModal.previewName !== "")
                                    hoverEnabled: true
                                    cursorShape: enabled ? Qt.PointingHandCursor : Qt.ArrowCursor
                                    onClicked: {
                                        if (accountController) {
                                            var target = skinModal.previewFilePath || skinModal.previewName
                                            accountController.applySkin(target, skinModal.skinVariant, skinModal.previewName)
                                            skinModal.isApplied = true
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Card 3: In Bibliothek speichern (Nur nach Anwenden aktiv!) ──
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.preferredHeight: 64
                        radius: 10
                        color: "#0F121A"
                        border.color: skinModal.isApplied ? (saveSkinNameInput.activeFocus ? EzTheme.accent : EzTheme.border) : "#1E2028"
                        border.width: 1
                        opacity: skinModal.isApplied ? 1.0 : 0.6

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 7
                            spacing: 4

                            RowLayout {
                                Layout.fillWidth: true
                                spacing: 4
                                Text {
                                    text: skinModal.isApplied ? "💾" : "🔒"
                                    font.pixelSize: 10
                                }
                                Text {
                                    text: skinModal.isApplied ?
                                          "Aktiven Skin in eigener Bibliothek speichern:" :
                                          "Skin vor dem Speichern zuerst auswählen & anwenden."
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    font.bold: skinModal.isApplied
                                    color: skinModal.isApplied ? EzTheme.textSecondary : EzTheme.textMuted
                                }
                            }

                            RowLayout {
                                Layout.fillWidth: true
                                spacing: 6

                                Rectangle {
                                    Layout.fillWidth: true
                                    height: 28
                                    radius: 6
                                    color: "#0A0C12"
                                    border.color: skinModal.isApplied ? (saveSkinNameInput.activeFocus ? EzTheme.accent : EzTheme.border) : "#1A1C24"
                                    border.width: 1

                                    TextField {
                                        id: saveSkinNameInput
                                        anchors.fill: parent
                                        anchors.margins: 3
                                        enabled: skinModal.isApplied
                                        placeholderText: skinModal.isApplied ? "Name für Bibliothek (z. B. Mein PvP Skin)..." : "Zuerst Skin anwenden..."
                                        placeholderTextColor: "#5A6273"
                                        color: skinModal.isApplied ? "#FFFFFF" : "#5A6273"
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 11
                                        background: Rectangle { color: "transparent" }
                                        onAccepted: saveSkinBtnMouse.clicked(null)
                                    }
                                }

                                Rectangle {
                                    width: 100; height: 28; radius: 6
                                    color: !skinModal.isApplied ? "#181A22" : (saveSkinBtnMouse.containsMouse ? EzTheme.surfaceActive : EzTheme.surface2)
                                    border.color: skinModal.isApplied ? EzTheme.accent : "#222530"
                                    border.width: 1

                                    RowLayout {
                                        anchors.centerIn: parent
                                        spacing: 4
                                        Text { text: "💾"; font.pixelSize: 9; opacity: skinModal.isApplied ? 1.0 : 0.4 }
                                        Text {
                                            text: "Speichern"
                                            font.family: EzTheme.fontFamily
                                            font.pixelSize: 11
                                            font.bold: true
                                            color: skinModal.isApplied ? EzTheme.accentLight : "#555A6B"
                                        }
                                    }

                                    MouseArea {
                                        id: saveSkinBtnMouse
                                        anchors.fill: parent
                                        enabled: skinModal.isApplied
                                        hoverEnabled: true
                                        cursorShape: enabled ? Qt.PointingHandCursor : Qt.ArrowCursor
                                        onClicked: {
                                            if (accountController && skinModal.isApplied) {
                                                var n = saveSkinNameInput.text.trim() || skinModal.previewName || "Mein Skin"
                                                accountController.saveCurrentSkin(n, skinModal.previewFilePath)
                                                saveSkinNameInput.text = ""
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Card 4: Meine Skin-Bibliothek (Klick = VORSCHAU!) ──
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        Layout.minimumHeight: 90
                        radius: 10
                        color: "#0F121A"
                        border.color: EzTheme.border
                        border.width: 1

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 8
                            spacing: 6

                            RowLayout {
                                Layout.fillWidth: true
                                Text {
                                    text: "📚 Meine Skin-Bibliothek (" + ((typeof accountController !== "undefined" && accountController && accountController.savedSkins) ? accountController.savedSkins.length : 0) + "):"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: EzTheme.textSecondary
                                }
                                Item { Layout.fillWidth: true }
                                Text {
                                    text: "💡 Klick zeigt 3D-Vorschau"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 9
                                    color: EzTheme.textMuted
                                }
                            }

                            // Saved Skins List
                            ScrollView {
                                Layout.fillWidth: true
                                Layout.fillHeight: true
                                contentWidth: savedRow.implicitWidth
                                clip: true

                                Row {
                                    id: savedRow
                                    spacing: 8
                                    anchors.verticalCenter: parent.verticalCenter

                                    Repeater {
                                        model: (typeof accountController !== "undefined" && accountController) ? accountController.savedSkins : []
                                        Rectangle {
                                            width: 155; height: 44; radius: 8
                                            color: savedMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                                            border.color: (skinModal.previewFilePath === modelData.path || skinModal.previewName === modelData.name) ? EzTheme.accent : EzTheme.border
                                            border.width: (skinModal.previewFilePath === modelData.path || skinModal.previewName === modelData.name) ? 2 : 1

                                            RowLayout {
                                                anchors.fill: parent
                                                anchors.margins: 4
                                                spacing: 6

                                                Image {
                                                    source: modelData.previewUrl ? modelData.previewUrl : ("https://mc-heads.net/avatar/" + (modelData.name || "Steve") + "/32")
                                                    Layout.preferredWidth: 28
                                                    Layout.preferredHeight: 28
                                                    fillMode: Image.PreserveAspectFit
                                                }

                                                ColumnLayout {
                                                    Layout.fillWidth: true
                                                    spacing: 1
                                                    Text {
                                                        text: modelData.name || "Skin"
                                                        font.family: EzTheme.fontFamily
                                                        font.pixelSize: 10
                                                        font.bold: true
                                                        color: EzTheme.text
                                                        elide: Text.ElideRight
                                                        Layout.fillWidth: true
                                                    }
                                                    Text {
                                                        text: "Vorschau laden"
                                                        font.family: EzTheme.fontFamily
                                                        font.pixelSize: 8
                                                        color: EzTheme.accentLight
                                                    }
                                                }

                                                Rectangle {
                                                    width: 20; height: 20; radius: 10
                                                    color: delMouse.containsMouse ? "#4A181C" : "transparent"
                                                    Text {
                                                        text: "✕"
                                                        font.pixelSize: 10
                                                        color: EzTheme.textMuted
                                                        anchors.centerIn: parent
                                                    }
                                                    MouseArea {
                                                        id: delMouse
                                                        anchors.fill: parent
                                                        hoverEnabled: true
                                                        cursorShape: Qt.PointingHandCursor
                                                        onClicked: {
                                                            if (accountController) {
                                                                accountController.deleteSavedSkin(modelData.id || modelData.name)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            MouseArea {
                                                id: savedMouse
                                                anchors.fill: parent
                                                anchors.rightMargin: 24
                                                hoverEnabled: true
                                                cursorShape: Qt.PointingHandCursor
                                                onClicked: {
                                                    // IMMER NUR VORSCHAU LADEN - NICHT DIREKT ANWENDEN!
                                                    skinModal.previewName = modelData.name || "Gespeicherter Skin"
                                                    if (modelData.path) {
                                                        skinModal.previewFilePath = modelData.path
                                                        if (accountController) {
                                                            skinModal.previewTextureUrl = accountController.getSkinTextureUrl(modelData.path)
                                                        }
                                                        skinModal.isApplied = (accountController && accountController.activeSkinPath === modelData.path)
                                                        if (modalSkin3D) modalSkin3D.updateSkin()
                                                        skinModal.statusMsg = "Vorschau von '" + skinModal.previewName + "' geladen. Klicke auf 'Skin auswählen & anwenden'."
                                                        skinModal.isError = false
                                                    } else if (modelData.name) {
                                                        skinNameInput.text = modelData.name
                                                        if (accountController) {
                                                            accountController.fetchSkinByUsername(modelData.name)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Empty state
                                    Text {
                                        visible: !(typeof accountController !== "undefined" && accountController && accountController.savedSkins && accountController.savedSkins.length > 0)
                                        text: "Noch keine Skins in der Bibliothek gespeichert."
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.textMuted
                                        anchors.verticalCenter: parent.verticalCenter
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Status Message Banner ──
            Rectangle {
                Layout.fillWidth: true
                height: 28
                radius: 6
                visible: skinModal.statusMsg !== ""
                color: skinModal.isError ? "#301014" : "#102C1E"
                border.color: skinModal.isError ? "#802028" : "#208048"
                border.width: 1

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 4
                    spacing: 6
                    Text { text: skinModal.isError ? "⚠️" : "✓"; font.pixelSize: 10 }
                    Text {
                        text: skinModal.statusMsg
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 10
                        color: skinModal.isError ? "#FFA0A8" : "#80EEAA"
                        elide: Text.ElideRight
                        Layout.fillWidth: true
                    }
                }
            }

            // ── Footer Action Row ──
            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                Rectangle {
                    height: 32
                    Layout.preferredWidth: 140
                    radius: 6
                    color: resetMouse.containsMouse ? "#2A1A1E" : "#181418"
                    border.color: EzTheme.border
                    border.width: 1

                    RowLayout {
                        anchors.centerIn: parent
                        spacing: 5
                        Text { text: "🔄"; font.pixelSize: 10 }
                        Text {
                            text: "Standard (Steve)"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            color: EzTheme.textMuted
                        }
                    }

                    MouseArea {
                        id: resetMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            if (accountController) {
                                accountController.resetSkin()
                                skinModal.previewFilePath = ""
                                skinModal.previewTextureUrl = Qt.resolvedUrl("../assets/skins/steve.png").toString()
                                skinModal.previewName = "Steve"
                                skinModal.isApplied = true
                                if (modalSkin3D) modalSkin3D.updateSkin()
                            }
                        }
                    }
                }

                Item { Layout.fillWidth: true }

                Rectangle {
                    height: 32
                    Layout.preferredWidth: 100
                    radius: 6
                    color: closeBtnMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                    border.color: EzTheme.border
                    border.width: 1

                    Text {
                        text: "Schließen"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        font.bold: true
                        color: EzTheme.textSecondary
                        anchors.centerIn: parent
                    }

                    MouseArea {
                        id: closeBtnMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: skinModal.close()
                    }
                }
            }
        }
    }
}
