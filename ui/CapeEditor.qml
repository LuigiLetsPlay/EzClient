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

    function chooseImage() {
        var url = accountController.pickCapeImage()
        if (!url) return
        root.selectedSource = url
        root.prepare()
    }

    function prepare() {
        if (!root.selectedSource) return
        root.pendingPreview = accountController.prepareCapeImage(root.selectedSource, root.fitMode)
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
