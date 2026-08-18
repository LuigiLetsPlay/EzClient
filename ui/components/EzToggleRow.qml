import QtQuick 2.15
import QtQuick.Layouts 1.15
import ".."

RowLayout {
    id: control
    property string label: ""
    property string sub: ""
    property bool toggleValue: false
    signal toggled(bool value)

    Layout.fillWidth: true

    ColumnLayout {
        Layout.fillWidth: true
        spacing: 1

        Text {
            text: control.label
            font.family: EzTheme.fontFamily
            font.pixelSize: 12
            font.bold: true
            color: EzTheme.text
        }

        Text {
            text: control.sub
            font.family: EzTheme.fontFamily
            font.pixelSize: 10
            color: EzTheme.textMuted
            visible: control.sub !== ""
            elide: Text.ElideRight
            Layout.fillWidth: true
        }
    }

    Rectangle {
        width: 34
        height: 18
        radius: 9
        color: control.toggleValue ? EzTheme.accent : EzTheme.surface3
        border.color: control.toggleValue ? EzTheme.accent : EzTheme.borderLight
        border.width: 1

        Behavior on color { ColorAnimation { duration: 120 } }
        Behavior on border.color { ColorAnimation { duration: 120 } }

        Rectangle {
            width: 12
            height: 12
            radius: 6
            color: "#FFFFFF"
            anchors.verticalCenter: parent.verticalCenter
            x: control.toggleValue ? parent.width - width - 3 : 3

            Behavior on x { NumberAnimation { duration: 130; easing.type: Easing.InOutQuad } }
        }

        MouseArea {
            anchors.fill: parent
            cursorShape: Qt.PointingHandCursor
            onClicked: {
                control.toggleValue = !control.toggleValue
                control.toggled(control.toggleValue)
            }
        }
    }
}
