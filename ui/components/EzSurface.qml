import QtQuick 2.15
import ".."

Rectangle {
    color: EzTheme.surface
    radius: EzTheme.radius
    border.color: EzTheme.border
    border.width: 1

    // Subtle top highlight for depth
    Rectangle {
        anchors.top: parent.top
        anchors.left: parent.left
        anchors.right: parent.right
        height: 1
        radius: parent.radius
        color: "#ffffff06"
    }
}
