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

    implicitHeight: 36
    implicitWidth: Math.max(90, contentItem.implicitWidth + 28)

    scale: control.down ? 0.96 : (control.hovered ? 1.02 : 1.0)
    Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }

    font.family: control.mcFont ? EzTheme.mcFontFamily : EzTheme.fontFamily
    font.pixelSize: control.mcFont ? 13 : 12
    font.bold: true

    background: Rectangle {
        radius: EzTheme.radiusSm
        color: {
            if (!control.enabled) return EzTheme.surface2
            if (control.primary) return control.down ? EzTheme.accentDark : (control.hovered ? EzTheme.accentHover : EzTheme.accent)
            if (control.danger) {
                return control.down ? "#3A0D15" : (control.hovered ? "#4C0519" : EzTheme.surface2)
            }
            if (control.cyan) {
                return control.down ? "#0E3A42" : (control.hovered ? "#164E63" : EzTheme.surface2)
            }
            return control.down ? EzTheme.surface3 : (control.hovered ? EzTheme.surfaceHover : EzTheme.surface2)
        }
        border.color: {
            if (control.primary) return control.hovered ? EzTheme.accentLight : EzTheme.accentGlow
            if (control.danger && control.hovered) return EzTheme.danger
            if (control.cyan && control.hovered) return EzTheme.cyan
            return control.hovered ? EzTheme.borderLight : EzTheme.border
        }
        border.width: 1

        Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }
        Behavior on border.color { ColorAnimation { duration: EzTheme.animNormal } }

    }

    // Windows Cursor Hand Feedback
    MouseArea {
        anchors.fill: parent
        cursorShape: control.enabled ? Qt.PointingHandCursor : Qt.ArrowCursor
        acceptedButtons: Qt.NoButton
    }

    contentItem: Row {
        spacing: 8
        anchors.centerIn: parent

        Image {
            source: {
                if (!control.iconSource) return ""
                if (control.iconSource.indexOf("/") !== -1 || control.iconSource.startsWith("qrc:") || control.iconSource.startsWith("data:") || control.iconSource.startsWith("http")) {
                    return control.iconSource
                }
                return Qt.resolvedUrl("../icons/" + control.iconSource).toString()
            }
            visible: control.iconSource !== ""
            width: 14
            height: 14
            fillMode: Image.PreserveAspectFit
            anchors.verticalCenter: parent.verticalCenter
        }

        Text {
            text: control.text
            font: control.font
            color: {
                if (!control.enabled) return EzTheme.textMuted
                if (control.primary) return "#000000"
                if (control.danger) return control.hovered ? EzTheme.danger : EzTheme.text
                if (control.cyan) return control.hovered ? EzTheme.cyan : EzTheme.text
                return EzTheme.text
            }
            Behavior on color { ColorAnimation { duration: EzTheme.animFast } }
            anchors.verticalCenter: parent.verticalCenter
        }
    }
}
