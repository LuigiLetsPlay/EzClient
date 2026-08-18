import QtQuick 2.15
import QtQuick.Controls 2.15
import ".."

Button {
    id: control
    property bool primary: false
    property bool danger: false
    property bool cyan: false
    property bool mcFont: true
    property string iconSource: ""

    implicitHeight: 32
    implicitWidth: Math.max(80, contentItem.implicitWidth + 24)

    scale: control.down ? 0.95 : (control.hovered ? 1.025 : 1.0)
    Behavior on scale { NumberAnimation { duration: 90; easing.type: Easing.OutQuad } }

    font.family: control.mcFont ? EzTheme.mcFontFamily : EzTheme.fontFamily
    font.pixelSize: control.mcFont ? 13 : 11
    font.bold: true

    background: Rectangle {
        radius: 6
        color: {
            if (!control.enabled) return EzTheme.surface2
            if (control.primary) {
                return control.down ? "#16A358" : (control.hovered ? "#24E07B" : EzTheme.accent)
            }
            if (control.danger) {
                return control.down ? "#3A0D15" : (control.hovered ? "#4C0519" : EzTheme.surface2)
            }
            if (control.cyan) {
                return control.down ? "#0E3A42" : (control.hovered ? "#164E63" : EzTheme.surface2)
            }
            return control.down ? EzTheme.surface3 : (control.hovered ? EzTheme.surfaceHover : EzTheme.surface2)
        }
        border.color: {
            if (control.primary) return control.hovered ? EzTheme.accentLight : "transparent"
            if (control.danger && control.hovered) return EzTheme.danger
            if (control.cyan && control.hovered) return EzTheme.cyan
            return control.hovered ? EzTheme.borderLight : EzTheme.border
        }
        border.width: 1

        Behavior on color { ColorAnimation { duration: 100 } }
        Behavior on border.color { ColorAnimation { duration: 100 } }
    }

    // Windows Cursor Hand Feedback
    MouseArea {
        anchors.fill: parent
        cursorShape: control.enabled ? Qt.PointingHandCursor : Qt.ArrowCursor
        acceptedButtons: Qt.NoButton
    }

    contentItem: Row {
        spacing: 6
        anchors.centerIn: parent

        Image {
            id: btnIcon
            visible: control.iconSource !== ""
            source: control.iconSource !== "" ? (control.iconSource.indexOf("/") !== -1 ? control.iconSource : "../icons/" + control.iconSource) : ""
            width: 14
            height: 14
            anchors.verticalCenter: parent.verticalCenter
            fillMode: Image.PreserveAspectFit
        }

        Text {
            text: control.text
            font: control.font
            color: {
                if (!control.enabled) return EzTheme.textSubtle
                if (control.primary) return "#000000"
                if (control.danger) return EzTheme.danger
                if (control.cyan) return EzTheme.cyan
                return EzTheme.text
            }
            anchors.verticalCenter: parent.verticalCenter
        }
    }
}
