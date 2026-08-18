import QtQuick 2.15
import QtQuick.Controls 2.15
import ".."

// Compact styled ComboBox for filters
ComboBox {
    id: control
    font.family: EzTheme.fontFamily
    font.pixelSize: 11
    implicitHeight: 32

    background: Rectangle {
        radius: 5
        color: control.popup.visible ? EzTheme.surface3 : (control.hovered ? EzTheme.surfaceHover : EzTheme.surface2)
        border.color: control.popup.visible ? EzTheme.accent : (control.hovered ? EzTheme.borderLight : EzTheme.border)
        border.width: 1
        Behavior on color { ColorAnimation { duration: 100 } }
        Behavior on border.color { ColorAnimation { duration: 100 } }
    }

    contentItem: Text {
        leftPadding: 10
        rightPadding: 10
        text: control.displayText
        font: control.font
        color: EzTheme.text
        verticalAlignment: Text.AlignVCenter
        elide: Text.ElideRight
    }

    indicator: Image {
        source: "../icons/more.svg"
        width: 10
        height: 10
        anchors.right: parent.right
        anchors.rightMargin: 8
        anchors.verticalCenter: parent.verticalCenter
        fillMode: Image.PreserveAspectFit
        opacity: 0.5
        rotation: control.popup.visible ? 90 : 0
        Behavior on rotation { NumberAnimation { duration: 120 } }
    }

    popup: Popup {
        y: control.height + 2
        width: Math.max(control.width, 160)
        height: Math.min(listView.contentHeight + 10, 220)
        padding: 4

        background: Rectangle {
            radius: EzTheme.radiusSm
            color: EzTheme.surface2
            border.color: EzTheme.borderLight
            border.width: 1
        }

        contentItem: ListView {
            id: listView
            anchors.fill: parent
            model: control.popup.visible ? control.delegateModel : null
            clip: true
            boundsBehavior: Flickable.StopAtBounds
            ScrollBar.vertical: ScrollBar {
                policy: listView.contentHeight > listView.height ? ScrollBar.AlwaysOn : ScrollBar.AsNeeded
                width: 5
                contentItem: Rectangle {
                    implicitWidth: 5
                    radius: 3
                    color: EzTheme.borderLight
                }
            }
        }
    }

    delegate: ItemDelegate {
        width: parent ? parent.width : 0
        highlighted: control.highlightedIndex === index

        background: Rectangle {
            radius: 4
            color: parent.highlighted ? EzTheme.surface3 : "transparent"
        }

        contentItem: Text {
            text: modelData
            font.family: EzTheme.fontFamily
            font.pixelSize: 11
            color: parent.highlighted ? EzTheme.text : EzTheme.textSecondary
            verticalAlignment: Text.AlignVCenter
            leftPadding: 8
        }
    }
}
