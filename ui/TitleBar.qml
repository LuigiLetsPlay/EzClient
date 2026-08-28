import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Rectangle {
    id: root
    height: 40
    color: EzTheme.titlebarBg

    property string breadcrumbText: ""
    property var windowRef: null

    // Bottom border line
    Rectangle {
        anchors { bottom: parent.bottom; left: parent.left; right: parent.right }
        height: 1
        color: EzTheme.border
    }

    // ── Left section: Logo + Breadcrumb
    RowLayout {
        anchors.verticalCenter: parent.verticalCenter
        anchors.left: parent.left
        anchors.leftMargin: 16
        spacing: 10

        // Transparent Logo Mark
        Image {
            source: "assets/logo.svg"
            Layout.preferredWidth: 18
            Layout.preferredHeight: 18
            fillMode: Image.PreserveAspectFit
            smooth: true
        }

        Text {
            text: "EzClient"
            font.family: EzTheme.fontFamily
            font.pixelSize: 12
            font.bold: true
            color: EzTheme.textMuted
        }

        // Divider + Breadcrumb
        Rectangle {
            width: 1; height: 14; color: EzTheme.border
            visible: root.breadcrumbText !== ""
        }
        Text {
            text: root.breadcrumbText
            font.family: EzTheme.fontFamily
            font.pixelSize: 12
            color: EzTheme.textSubtle
            visible: root.breadcrumbText !== ""
        }
    }

    // Full-width transparent drag area (behind everything, z: -2)
    MouseArea {
        anchors.fill: parent
        z: -2
        onPressed: if (root.windowRef) root.windowRef.startSystemMove()
        onDoubleClicked: {
            if (!root.windowRef) return
            root.windowRef.visibility === Window.Maximized
                ? root.windowRef.showNormal()
                : root.windowRef.showMaximized()
        }
    }

    // ── Right section: Window controls (z: 1 so they capture clicks first)
    Row {
        anchors { right: parent.right; top: parent.top; bottom: parent.bottom }
        z: 1

        // Minimize
        Rectangle {
            width: 46; height: root.height
            color: minMouse.containsMouse ? EzTheme.surface3 : "transparent"
            Behavior on color { ColorAnimation { duration: 80 } }
            Text { text: "─"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; anchors.centerIn: parent }
            MouseArea { id: minMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor; onClicked: if (root.windowRef) root.windowRef.showMinimized() }
        }

        // Maximize
        Rectangle {
            width: 46; height: root.height
            color: maxMouse.containsMouse ? EzTheme.surface3 : "transparent"
            Behavior on color { ColorAnimation { duration: 80 } }
            Text { text: "□"; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textMuted; anchors.centerIn: parent }
            MouseArea {
                id: maxMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor
                onClicked: {
                    if (!root.windowRef) return
                    root.windowRef.visibility === Window.Maximized ? root.windowRef.showNormal() : root.windowRef.showMaximized()
                }
            }
        }

        // Close
        Rectangle {
            width: 46; height: root.height
            color: closeMouse.containsMouse ? "#C42B1C" : "transparent"
            Behavior on color { ColorAnimation { duration: 80 } }
            Image { source: "icons/x.svg"; width: 10; height: 10; anchors.centerIn: parent; opacity: closeMouse.containsMouse ? 1.0 : 0.6; fillMode: Image.PreserveAspectFit }
            MouseArea { id: closeMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor; onClicked: if (root.windowRef) root.windowRef.close() }
        }
    }
}
