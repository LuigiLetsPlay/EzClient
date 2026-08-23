import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "components"

Item {
    id: root
    signal navigate(string route)
    property string selectedSource: ""
    property string pendingPreview: ""
    property string fitMode: "Cover"
    // Normalized crop window over the selected picture (0..1).
    property real cropX: 0
    property real cropY: 0
    property real cropW: 1
    property real cropH: 1

    function chooseImage() {
        var url = accountController.pickCapeImage()
        if (!url) return
        root.selectedSource = url
        root.cropX = 0; root.cropY = 0; root.cropW = 1; root.cropH = 1
        root.prepare()
    }

    function prepare() {
        if (!root.selectedSource) return
        var crop = root.cropX.toFixed(4) + "," + root.cropY.toFixed(4) + "," + root.cropW.toFixed(4) + "," + root.cropH.toFixed(4)
        root.pendingPreview = accountController.prepareCapeImage(root.selectedSource, root.fitMode + "|" + crop)
    }

    function discard() {
        accountController.cancelPendingCape()
        root.selectedSource = ""
        root.pendingPreview = ""
    }

    function confirm() {
        if (root.pendingPreview && accountController.confirmPendingCape()) {
            root.selectedSource = ""
            root.pendingPreview = ""
        }
    }

    Connections {
        target: accountController
        function onSkinUploadStatusChanged(message, isError) {
            statusText.text = message
            statusText.color = isError ? "#FCA5A5" : "#86EFAC"
        }
        function onCapeCommunityStatusChanged(message, isError) {
            if (!message) return
            statusText.text = message
            statusText.color = isError ? "#FCA5A5" : "#86EFAC"
        }
    }

    Rectangle { anchors.fill: parent; color: EzTheme.bg }

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 36
        spacing: 20

        RowLayout {
            Layout.fillWidth: true
            Text {
                text: "Cape-Bild"
                font.family: EzTheme.mcFontFamily
                font.pixelSize: 24
                font.bold: true
                color: EzTheme.text
                Layout.fillWidth: true
            }
            EzButton { text: "Zurück"; onClicked: root.navigate("cape") }
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            spacing: 32

            // Cape-shaped live preview of the formatted pending image.
            Rectangle {
                Layout.preferredWidth: Math.min(280, parent.width * 0.38)
                Layout.preferredHeight: width * 1.6
                radius: 10
                color: "#171126"
                border.color: EzTheme.border
                clip: true

                Item {
                    id: capeCrop
                    anchors.fill: parent
                    anchors.margins: 14

                    // The pending atlas is 1280x640; its visible cape face is
                    // 200x320 at position 20,20. Show exactly that region.
                    Image {
                        source: root.pendingPreview
                        visible: root.pendingPreview !== ""
                        asynchronous: true
                        fillMode: Image.Stretch
                        width: capeCrop.width * 6.4
                        height: capeCrop.height * 2.0
                        x: -capeCrop.width * 0.1
                        y: -capeCrop.height * 0.0625
                    }

                    ColumnLayout {
                        visible: root.pendingPreview === ""
                        anchors.centerIn: parent
                        spacing: 12
                        Text { text: "Noch kein Bild"; color: EzTheme.textSecondary; font.pixelSize: 15; Layout.alignment: Qt.AlignHCenter }
                    }
                }
            }

            ColumnLayout {
                Layout.fillWidth: true
                Layout.maximumWidth: 520
                spacing: 16

                EzButton { text: "📁 Bild auswählen"; onClicked: root.chooseImage() }

                Text { text: "Skalierung"; color: EzTheme.text; font.bold: true; font.pixelSize: 15 }

                RowLayout {
                    spacing: 10
                    Repeater {
                        model: [{ id: "Cover", label: "Ausfüllen" }, { id: "Stretch", label: "Strecken" }]
                        delegate: Rectangle {
                            property var item: modelData
                            width: 130; height: 40; radius: 8
                            color: root.fitMode === item.id ? EzTheme.surfaceActive : EzTheme.surface2
                            border.color: root.fitMode === item.id ? EzTheme.accent : EzTheme.border

                            MouseArea { anchors.fill: parent; onClicked: { root.fitMode = item.id; root.prepare() } }
                            Text { anchors.centerIn: parent; text: item.label; color: EzTheme.text; font.pixelSize: 13 }
                        }
                    }
                }

                Text { text: "Zuschneiden"; color: EzTheme.text; font.bold: true; font.pixelSize: 15; visible: root.selectedSource !== "" }

                // Interactive crop editor. Drag inside the frame to move it,
                // drag the corner handle to resize. The cape is made from the
                // framed region only.
                Rectangle {
                    id: cropStage
                    visible: root.selectedSource !== ""
                    Layout.fillWidth: true
                    Layout.preferredHeight: 300
                    radius: 10
                    color: "#171126"
                    border.color: EzTheme.border
                    clip: true

                    Image {
                        id: cropImage
                        anchors.centerIn: parent
                        width: parent.width
                        height: parent.height
                        fillMode: Image.PreserveAspectFit
                        asynchronous: true
                        source: root.selectedSource
                    }

                    // Darken everything outside the crop window.
                    Rectangle {
                        x: cropImage.x + root.cropX * cropImage.paintedWidth
                        y: cropImage.y + root.cropY * cropImage.paintedHeight
                        width: Math.max(20, root.cropW * cropImage.paintedWidth)
                        height: Math.max(20, root.cropH * cropImage.paintedHeight)
                        color: "transparent"
                        border.color: EzTheme.accent
                        border.width: 2

                        MouseArea {
                            id: cropMove
                            anchors.fill: parent
                            cursorShape: Qt.SizeAllCursor
                            property point startMouse
                            property rect startRect
                            onPressed: function(mouse) {
                                startMouse = Qt.point(mouse.x, mouse.y)
                                startRect = Qt.rect(parent.x, parent.y, parent.width, parent.height)
                            }
                            onPositionChanged: function(mouse) {
                                if (!pressed) return
                                var dx = mouse.x - startMouse.x
                                var dy = mouse.y - startMouse.y
                                var nx = Math.min(cropImage.x + cropImage.paintedWidth - parent.width, Math.max(cropImage.x, startRect.x + dx))
                                var ny = Math.min(cropImage.y + cropImage.paintedHeight - parent.height, Math.max(cropImage.y, startRect.y + dy))
                                parent.x = nx; parent.y = ny
                                root.cropX = (parent.x - cropImage.x) / cropImage.paintedWidth
                                root.cropY = (parent.y - cropImage.y) / cropImage.paintedHeight
                            }
                        }

                        Rectangle {
                            id: resizeHandle
                            width: 16; height: 16
                            radius: 8
                            color: EzTheme.accent
                            anchors.right: parent.right
                            anchors.bottom: parent.bottom
                            anchors.margins: -8

                            MouseArea {
                                anchors.fill: parent
                                cursorShape: Qt.SizeFDiagCursor
                                onPositionChanged: function(mouse) {
                                    if (!pressed) return
                                    var absX = mapToItem(cropStage, mouse.x, mouse.y).x
                                    var absY = mapToItem(cropStage, mouse.x, mouse.y).y
                                    var maxX = cropImage.x + cropImage.paintedWidth - parent.parent.x
                                    var maxY = cropImage.y + cropImage.paintedHeight - parent.parent.y
                                    parent.parent.width = Math.max(20, Math.min(maxX, absX - parent.parent.x))
                                    parent.parent.height = Math.max(20, Math.min(maxY, absY - parent.parent.y))
                                    root.cropW = parent.parent.width / cropImage.paintedWidth
                                    root.cropH = parent.parent.height / cropImage.paintedHeight
                                }
                            }
                        }
                    }

                    Text {
                        visible: cropImage.status === Image.Loading
                        anchors.centerIn: parent
                        text: "Bild lädt…"
                        color: EzTheme.textSecondary
                    }
                }

                RowLayout {
                    visible: root.selectedSource !== ""
                    spacing: 10
                    EzButton { text: "Zuschnitt anwenden"; onClicked: root.prepare() }
                    EzButton { text: "Alles auswählen"; onClicked: { root.cropX = 0; root.cropY = 0; root.cropW = 1; root.cropH = 1 } }
                }

                Text {
                    text: "Das Bild wird erst nach „Bestätigen“ aktiviert und hochgeladen."
                    color: EzTheme.textSecondary
                    wrapMode: Text.WordWrap
                    Layout.fillWidth: true
                    font.pixelSize: 13
                }

                RowLayout {
                    spacing: 12
                    EzButton {
                        text: "Bestätigen & hochladen"
                        enabled: root.pendingPreview !== ""
                        onClicked: root.confirm()
                    }
                    EzButton {
                        text: "Verwerfen"
                        enabled: root.pendingPreview !== ""
                        onClicked: root.discard()
                    }
                }

                Text {
                    id: statusText
                    text: ""
                    color: EzTheme.textSecondary
                    wrapMode: Text.WordWrap
                    Layout.fillWidth: true
                    font.pixelSize: 12
                }
            }
        }
    }
}
