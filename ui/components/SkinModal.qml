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
    property string previewCapeUrl: ""
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
            previewCapeUrl = accountController.capeTextureUrl || ""
            previewName = accountController.activeSkinName || "Aktiver Skin"
        } else {
            previewFilePath = ""
            previewTextureUrl = ""
            previewName = "Steve"
        }
        isApplied = true
        statusMsg = ""
        isError = false
        usernameInput.text = ""
        saveNameInput.text = ""
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
            if (usernameInput.text.trim() !== "") {
                baseName = usernameInput.text.trim()
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

    MouseArea {
        anchors.fill: parent
        onClicked: skinModal.close()
    }

    Rectangle {
        anchors.centerIn: parent
        width: Math.min(840, parent.width - 32)
        height: Math.min(640, parent.height - 24)
        radius: 16
        color: "#12141C"
        border.color: EzTheme.borderLight
        border.width: 1
        clip: true

        MouseArea {
            anchors.fill: parent
            onClicked: {} // consume
        }

        // Close button
        Rectangle {
            z: 10
            anchors.top: parent.top
            anchors.right: parent.right
            anchors.margins: 16
            width: 32; height: 32; radius: 16
            color: closeMouse.containsMouse ? "#2A2D3A" : "#1A1D27"
            Text {
                anchors.centerIn: parent
                text: "✕"
                color: "#A0A8B8"
                font.pixelSize: 14
            }
            MouseArea {
                id: closeMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: skinModal.close()
            }
        }

        RowLayout {
            anchors.fill: parent
            spacing: 0

            // ════ LEFT COLUMN: 3D PREVIEW ════
            Rectangle {
                Layout.preferredWidth: 320
                Layout.fillHeight: true
                color: "#0A0B10"

                ColumnLayout {
                    anchors.fill: parent
                    anchors.margins: 20
                    spacing: 16

                    Text {
                        Layout.alignment: Qt.AlignHCenter
                        text: "Skin Vorschau"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 18
                        font.bold: true
                        color: EzTheme.text
                    }

                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        color: "transparent"

                        Skin3DView {
                            id: modalSkin3D
                            anchors.fill: parent
                            skinSource: skinModal.previewTextureUrl
                            capeSource: skinModal.previewCapeUrl
                            animation: skinModal.currentAnim
                            autoRotate: false
                        }

                        // Badge
                        Rectangle {
                            anchors.top: parent.top
                            anchors.left: parent.left
                            anchors.margins: 10
                            height: 22
                            width: badgeTxt.implicitWidth + 16
                            radius: 11
                            color: skinModal.isApplied ? "#22C96E20" : "#E5A93C20"
                            border.color: skinModal.isApplied ? "#22C96E" : "#E5A93C"
                            border.width: 1
                            Text {
                                id: badgeTxt
                                anchors.centerIn: parent
                                text: skinModal.isApplied ? "Aktiv" : "Vorschau"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 10
                                font.bold: true
                                color: skinModal.isApplied ? "#80EEAA" : "#F5D685"
                            }
                        }
                    }

                    // Variant & Animation Controls
                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 10

                        // Variant
                        Rectangle {
                            Layout.fillWidth: true
                            height: 36
                            radius: 8
                            color: "#161B24"
                            border.color: EzTheme.border
                            border.width: 1
                            RowLayout {
                                anchors.fill: parent; anchors.margins: 4
                                Rectangle {
                                    Layout.fillWidth: true; Layout.fillHeight: true; radius: 6
                                    color: skinModal.skinVariant === "classic" ? EzTheme.accent : "transparent"
                                    Text { anchors.centerIn: parent; text: "Classic"; font.pixelSize: 11; font.bold: true; color: skinModal.skinVariant === "classic" ? "#000" : EzTheme.textSecondary }
                                    MouseArea { anchors.fill: parent; onClicked: skinModal.skinVariant = "classic" }
                                }
                                Rectangle {
                                    Layout.fillWidth: true; Layout.fillHeight: true; radius: 6
                                    color: skinModal.skinVariant === "slim" ? EzTheme.accent : "transparent"
                                    Text { anchors.centerIn: parent; text: "Slim"; font.pixelSize: 11; font.bold: true; color: skinModal.skinVariant === "slim" ? "#000" : EzTheme.textSecondary }
                                    MouseArea { anchors.fill: parent; onClicked: skinModal.skinVariant = "slim" }
                                }
                            }
                        }

                        // Anim
                        Rectangle {
                            Layout.fillWidth: true
                            height: 36
                            radius: 8
                            color: "#161B24"
                            border.color: EzTheme.border
                            border.width: 1
                            RowLayout {
                                anchors.fill: parent; anchors.margins: 4
                                Rectangle {
                                    Layout.fillWidth: true; Layout.fillHeight: true; radius: 6
                                    color: skinModal.currentAnim === "idle" ? EzTheme.surfaceActive : "transparent"
                                    Text { anchors.centerIn: parent; text: "🧍"; font.pixelSize: 14 }
                                    MouseArea { anchors.fill: parent; onClicked: { skinModal.currentAnim = "idle"; modalSkin3D.setAnim("idle") } }
                                }
                                Rectangle {
                                    Layout.fillWidth: true; Layout.fillHeight: true; radius: 6
                                    color: skinModal.currentAnim === "walk" ? EzTheme.surfaceActive : "transparent"
                                    Text { anchors.centerIn: parent; text: "🚶"; font.pixelSize: 14 }
                                    MouseArea { anchors.fill: parent; onClicked: { skinModal.currentAnim = "walk"; modalSkin3D.setAnim("walk") } }
                                }
                                Rectangle {
                                    Layout.fillWidth: true; Layout.fillHeight: true; radius: 6
                                    color: skinModal.currentAnim === "run" ? EzTheme.surfaceActive : "transparent"
                                    Text { anchors.centerIn: parent; text: "🏃"; font.pixelSize: 14 }
                                    MouseArea { anchors.fill: parent; onClicked: { skinModal.currentAnim = "run"; modalSkin3D.setAnim("run") } }
                                }
                            }
                        }
                    }
                }
            }

            Rectangle {
                Layout.preferredWidth: 1
                Layout.fillHeight: true
                color: EzTheme.borderLight
            }

            // ════ RIGHT COLUMN: ACTIONS & LIBRARY ════
            ScrollView {
                Layout.fillWidth: true
                Layout.fillHeight: true
                contentWidth: availableWidth
                clip: true

                ColumnLayout {
                    width: parent.width
                    spacing: 24
                    anchors.margins: 24
                    anchors.left: parent.left
                    anchors.right: parent.right
                    anchors.top: parent.top

                    // Header
                    ColumnLayout {
                        spacing: 4
                        Text {
                            text: "Skin Verwaltung"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 22
                            font.bold: true
                            color: EzTheme.text
                        }
                        Text {
                            text: "Wähle einen neuen Skin oder speichere deinen aktuellen."
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 13
                            color: EzTheme.textSecondary
                        }
                    }

                    // Status Message
                    Rectangle {
                        Layout.fillWidth: true
                        height: statusTxt.implicitHeight + 16
                        radius: 8
                        color: skinModal.isError ? "#3A1B1B" : "#1B3A24"
                        border.color: skinModal.isError ? "#FF4444" : "#22C96E"
                        border.width: 1
                        visible: skinModal.statusMsg !== ""
                        Text {
                            id: statusTxt
                            anchors.fill: parent
                            anchors.margins: 8
                            text: skinModal.statusMsg
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            color: skinModal.isError ? "#FF8888" : "#80EEAA"
                            wrapMode: Text.WordWrap
                            verticalAlignment: Text.AlignVCenter
                        }
                    }

                    // ── LOAD SKIN ──
                    Rectangle {
                        Layout.fillWidth: true
                        height: 120
                        radius: 12
                        color: "#161B24"
                        border.color: EzTheme.border
                        border.width: 1

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 16
                            spacing: 12

                            Text {
                                text: "1. Skin in Vorschau laden"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 14
                                font.bold: true
                                color: EzTheme.text
                            }

                            RowLayout {
                                Layout.fillWidth: true
                                spacing: 12

                                // Username Input
                                Rectangle {
                                    Layout.fillWidth: true
                                    height: 36
                                    radius: 8
                                    color: "#0A0C12"
                                    border.color: usernameInput.activeFocus ? EzTheme.accent : EzTheme.border
                                    border.width: 1
                                    RowLayout {
                                        anchors.fill: parent; anchors.margins: 4; spacing: 8
                                        TextField {
                                            id: usernameInput
                                            Layout.fillWidth: true
                                            placeholderText: "Minecraft Name..."
                                            placeholderTextColor: "#646E82"
                                            color: "#FFF"
                                            font.pixelSize: 12
                                            background: Item {}
                                            onAccepted: fetchBtnMouse.clicked(null)
                                        }
                                        Rectangle {
                                            width: 70; height: 28; radius: 6
                                            color: fetchBtnMouse.containsMouse ? EzTheme.surfaceActive : EzTheme.surface3
                                            Text { anchors.centerIn: parent; text: "Suchen"; color: EzTheme.text; font.pixelSize: 11; font.bold: true }
                                            MouseArea {
                                                id: fetchBtnMouse
                                                anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                                onClicked: {
                                                    if (accountController && usernameInput.text.trim() !== "") {
                                                        accountController.fetchSkinByUsername(usernameInput.text.trim())
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Text { text: "ODER"; font.pixelSize: 11; color: EzTheme.textSecondary; font.bold: true }

                                // Upload Button
                                Rectangle {
                                    width: 120; height: 36; radius: 8
                                    color: uploadBtnMouse.containsMouse ? EzTheme.surfaceActive : EzTheme.surface3
                                    border.color: EzTheme.border
                                    border.width: 1
                                    Text { anchors.centerIn: parent; text: "Bild hochladen"; color: EzTheme.text; font.pixelSize: 12; font.bold: true }
                                    MouseArea {
                                        id: uploadBtnMouse
                                        anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
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

                                Rectangle {
                                    width: 120; height: 36; radius: 8
                                    color: capeBtnMouse.containsMouse ? EzTheme.surfaceActive : EzTheme.surface3
                                    border.color: EzTheme.border
                                    border.width: 1
                                    Text { anchors.centerIn: parent; text: "Cape hochladen"; color: EzTheme.text; font.pixelSize: 12; font.bold: true }
                                    MouseArea {
                                        id: capeBtnMouse
                                        anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                        onClicked: {
                                            if (accountController) {
                                                var cape = accountController.pickCapeFile()
                                                if (cape) {
                                                    skinModal.previewCapeUrl = cape
                                                    if (modalSkin3D) modalSkin3D.updateCape()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── APPLY SKIN ──
                    Rectangle {
                        Layout.fillWidth: true
                        height: 70
                        radius: 12
                        color: skinModal.isApplied ? "#101D16" : "#1B1710"
                        border.color: skinModal.isApplied ? "#208048" : "#E5A93C"
                        border.width: 1

                        RowLayout {
                            anchors.fill: parent
                            anchors.margins: 16
                            spacing: 16

                            ColumnLayout {
                                Layout.fillWidth: true
                                spacing: 2
                                Text {
                                    text: "2. Skin anwenden"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 14
                                    font.bold: true
                                    color: EzTheme.text
                                }
                                Text {
                                    text: skinModal.isApplied ? "Dieser Skin ist bereits aktiv." : "Diesen Skin jetzt für deinen Account übernehmen."
                                    font.pixelSize: 11
                                    color: EzTheme.textSecondary
                                }
                            }

                            Rectangle {
                                width: 140; height: 38; radius: 8
                                color: skinModal.isApplied ? "#1E2A22" : (applyBtnMouse.containsMouse ? "#2EE080" : "#22C96E")
                                border.color: skinModal.isApplied ? "#2A3D30" : "#22C96E"
                                border.width: 1
                                Text {
                                    anchors.centerIn: parent
                                    text: skinModal.isApplied ? "Aktiv" : "Auswählen"
                                    color: skinModal.isApplied ? "#80EEAA" : "#000"
                                    font.pixelSize: 13
                                    font.bold: true
                                }
                                MouseArea {
                                    id: applyBtnMouse
                                    anchors.fill: parent; hoverEnabled: !skinModal.isApplied; cursorShape: enabled ? Qt.PointingHandCursor : Qt.ArrowCursor
                                    enabled: !skinModal.isApplied
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

                    // ── SAVE SKIN ──
                    Rectangle {
                        Layout.fillWidth: true
                        height: 90
                        radius: 12
                        color: "#161B24"
                        border.color: EzTheme.border
                        border.width: 1
                        opacity: skinModal.isApplied ? 1.0 : 0.4 // Disabled state visual

                        ColumnLayout {
                            anchors.fill: parent
                            anchors.margins: 16
                            spacing: 8

                            RowLayout {
                                Layout.fillWidth: true
                                Text {
                                    Layout.fillWidth: true
                                    text: "3. In Bibliothek speichern (Nur für aktiven Skin)"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 14
                                    font.bold: true
                                    color: EzTheme.text
                                }
                            }

                            RowLayout {
                                Layout.fillWidth: true
                                spacing: 12

                                Rectangle {
                                    Layout.fillWidth: true
                                    height: 36
                                    radius: 8
                                    color: "#0A0C12"
                                    border.color: EzTheme.border
                                    border.width: 1
                                    TextField {
                                        id: saveNameInput
                                        anchors.fill: parent; anchors.margins: 4
                                        placeholderText: "Name für die Bibliothek..."
                                        placeholderTextColor: "#646E82"
                                        color: "#FFF"
                                        font.pixelSize: 12
                                        background: Item {}
                                        enabled: skinModal.isApplied
                                    }
                                }

                                Rectangle {
                                    width: 120; height: 36; radius: 8
                                    color: skinModal.isApplied && saveBtnMouse.containsMouse ? EzTheme.surfaceActive : EzTheme.surface3
                                    border.color: EzTheme.border
                                    border.width: 1
                                    Text {
                                        anchors.centerIn: parent
                                        text: "Speichern"
                                        color: skinModal.isApplied ? EzTheme.text : EzTheme.textSecondary
                                        font.pixelSize: 12
                                        font.bold: true
                                    }
                                    MouseArea {
                                        id: saveBtnMouse
                                        anchors.fill: parent; hoverEnabled: skinModal.isApplied; cursorShape: enabled ? Qt.PointingHandCursor : Qt.ArrowCursor
                                        enabled: skinModal.isApplied
                                        onClicked: {
                                            if (accountController) {
                                                accountController.saveCurrentSkin(saveNameInput.text, "")
                                                saveNameInput.text = ""
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── LIBRARY ──
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 12
                        
                        Text {
                            text: "Meine Skin-Bibliothek"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 16
                            font.bold: true
                            color: EzTheme.text
                            Layout.topMargin: 8
                        }

                        GridView {
                            id: libraryGrid
                            Layout.fillWidth: true
                            Layout.preferredHeight: Math.max(120, Math.ceil(count / 4) * 110)
                            cellWidth: (parent.width - 36) / 4
                            cellHeight: 110
                            clip: true
                            interactive: false
                            model: (typeof accountController !== "undefined" && accountController) ? accountController.savedSkins : []
                            
                            delegate: Rectangle {
                                width: libraryGrid.cellWidth - 12
                                height: libraryGrid.cellHeight - 12
                                radius: 8
                                color: libItemMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                                border.color: libItemMouse.containsMouse ? EzTheme.accent : EzTheme.border
                                border.width: 1
                                clip: true

                                ColumnLayout {
                                    anchors.fill: parent
                                    spacing: 0

                                    Rectangle {
                                        Layout.fillWidth: true
                                        Layout.fillHeight: true
                                        color: "#0A0B10"
                                        Image {
                                            anchors.centerIn: parent
                                            width: parent.width * 0.8
                                            height: width
                                            source: modelData.previewUrl
                                            fillMode: Image.PreserveAspectFit
                                            smooth: false
                                            cache: false
                                        }
                                    }

                                    Rectangle {
                                        Layout.fillWidth: true
                                        height: 24
                                        color: "#161B24"
                                        Text {
                                            anchors.centerIn: parent
                                            text: modelData.name
                                            font.pixelSize: 10
                                            font.bold: true
                                            color: EzTheme.text
                                            elide: Text.ElideRight
                                            width: parent.width - 8
                                            horizontalAlignment: Text.AlignHCenter
                                        }
                                    }
                                }

                                Rectangle {
                                    anchors.top: parent.top; anchors.right: parent.right; anchors.margins: 4
                                    width: 20; height: 20; radius: 4; color: "#AA2222"
                                    opacity: libDelMouse.containsMouse ? 1.0 : 0.0
                                    Behavior on opacity { NumberAnimation { duration: 150 } }
                                    Text { anchors.centerIn: parent; text: "✕"; color: "#FFF"; font.pixelSize: 10 }
                                    MouseArea {
                                        id: libDelMouse
                                        anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                        onClicked: {
                                            if (accountController) accountController.deleteSavedSkin(modelData.id)
                                        }
                                    }
                                }

                                MouseArea {
                                    id: libItemMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        if (accountController) {
                                            skinModal.previewFilePath = modelData.path
                                            skinModal.previewName = modelData.name
                                            skinModal.previewTextureUrl = modelData.path ? accountController.getSkinTextureUrl(modelData.path) : modelData.previewUrl
                                            skinModal.isApplied = false
                                            if (modalSkin3D) modalSkin3D.updateSkin()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Item { Layout.preferredHeight: 20 }
                }
            }
        }
    }
}
