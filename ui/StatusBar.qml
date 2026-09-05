import QtQuick 2.15
import QtQuick.Layouts 1.15

Rectangle {
    id: root
    height: 26
    color: EzTheme.surface
    border.color: EzTheme.border
    border.width: 1

    property string statusText: "● Online  ·  Modrinth API Connected"
    property string launcherStatus: "● Official Launcher Ready"

    RowLayout {
        anchors.fill: parent
        anchors.leftMargin: 12
        anchors.rightMargin: 12

        Text {
            text: root.statusText
            font.family: EzTheme.fontFamily
            font.pixelSize: 9
            font.bold: true
            color: EzTheme.accentLight
        }

        Text {
            text: root.launcherStatus
            font.family: EzTheme.fontFamily
            font.pixelSize: 9
            color: EzTheme.textSecondary
            Layout.leftMargin: 16
        }

        Item { Layout.fillWidth: true }

        Text {
            text: "EzClient v" + (typeof updateController !== "undefined" && updateController ? updateController.currentVersion : "2.0.1")
            font.family: EzTheme.fontFamily
            font.pixelSize: 9
            color: EzTheme.textSubtle
        }
    }
}
