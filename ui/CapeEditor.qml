import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "components"

Item {
    id: root
    signal navigate(string route)
    property string statusMessage: ""
    property bool statusError: false

    function importImage() {
        var previewUrl = accountController.pickCapeFile()
        if (previewUrl) {
            root.statusMessage = "Bild übernommen. Upload läuft im Hintergrund…"
            root.statusError = false
            accountController.publishCape("EzClient Cape")
        }
    }

    Connections {
        target: accountController
        function onSkinUploadStatusChanged(message, isError) {
            root.statusMessage = message
            root.statusError = isError
        }
        function onCapeCommunityStatusChanged(message, isError) {
            if (message) {
                root.statusMessage = message
                root.statusError = isError
            }
        }
    }

    Rectangle { anchors.fill: parent; color: EzTheme.bg }

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 40
        spacing: 24

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

        Rectangle {
            Layout.alignment: Qt.AlignHCenter
            Layout.preferredWidth: Math.min(520, parent.width)
            Layout.preferredHeight: Math.min(620, parent.height - 180)
            radius: 12
            color: "#171126"
            border.color: pickArea.containsMouse ? EzTheme.accentLight : EzTheme.border
            border.width: 2

            MouseArea {
                id: pickArea
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: root.importImage()
            }

            ColumnLayout {
                anchors.centerIn: parent
                spacing: 18
                width: parent.width - 64

                Text {
                    text: "🖼️"
                    font.pixelSize: 54
                    Layout.alignment: Qt.AlignHCenter
                }
                Text {
                    text: "Bild auswählen"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 20
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                }
                Text {
                    text: "PNG, JPG, JPEG oder WEBP\nDas Bild wird automatisch als Cape formatiert."
                    horizontalAlignment: Text.AlignHCenter
                    wrapMode: Text.WordWrap
                    color: EzTheme.textSecondary
                    font.pixelSize: 13
                    Layout.alignment: Qt.AlignHCenter
                    Layout.fillWidth: true
                }
            }
        }

        Rectangle {
            visible: root.statusMessage !== ""
            Layout.fillWidth: true
            Layout.preferredHeight: statusText.implicitHeight + 18
            radius: 8
            color: root.statusError ? "#3A1724" : "#153126"
            border.color: root.statusError ? "#EF4444" : "#22C55E"

            Text {
                id: statusText
                anchors.fill: parent
                anchors.margins: 9
                text: root.statusMessage
                color: EzTheme.text
                wrapMode: Text.WordWrap
                font.pixelSize: 12
            }
        }
    }
}
