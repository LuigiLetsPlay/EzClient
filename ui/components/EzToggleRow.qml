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
        spacing: 2

        Text {
            text: control.label
            font.family: EzTheme.fontFamily
            font.pixelSize: 13
            font.bold: true
            color: EzTheme.text
        }

        Text {
            text: control.sub
            font.family: EzTheme.fontFamily
            font.pixelSize: 11
            color: EzTheme.textMuted
            visible: control.sub !== ""
            elide: Text.ElideRight
            Layout.fillWidth: true
        }
    }

    // iOS-style premium toggle switch
    Rectangle {
        id: toggleTrack
        width: 44
        height: 24
        radius: 12
        color: control.toggleValue ? EzTheme.accent : EzTheme.surface3
        border.color: control.toggleValue ? EzTheme.accentHover : EzTheme.borderLight
        border.width: 1.5

        Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }
        Behavior on border.color { ColorAnimation { duration: EzTheme.animNormal } }

        // Active glow effect
        Rectangle {
            anchors.fill: parent
            anchors.margins: -3
            radius: parent.radius + 3
            color: "transparent"
            border.color: control.toggleValue ? EzTheme.accentGlow : "transparent"
            border.width: 2
            Behavior on border.color { ColorAnimation { duration: EzTheme.animSlow } }
        }

        // Toggle knob
        Rectangle {
            id: toggleKnob
            width: 18
            height: 18
            radius: 9
            anchors.verticalCenter: parent.verticalCenter
            x: control.toggleValue ? parent.width - width - 3 : 3

            color: "#FFFFFF"

            // Subtle inner shadow on knob
            Rectangle {
                anchors.fill: parent
                radius: parent.radius
                gradient: Gradient {
                    orientation: Gradient.Vertical
                    GradientStop { position: 0.0; color: "#ffffff" }
                    GradientStop { position: 1.0; color: "#e8e8e8" }
                }
            }

            Behavior on x { NumberAnimation { duration: 200; easing.type: Easing.InOutCubic } }

            scale: toggleMouse.pressed ? 0.85 : 1.0
            Behavior on scale { NumberAnimation { duration: 80 } }
        }

        MouseArea {
            id: toggleMouse
            anchors.fill: parent
            cursorShape: Qt.PointingHandCursor
            onClicked: {
                control.toggleValue = !control.toggleValue
                control.toggled(control.toggleValue)
            }
        }
    }
}
