import QtQuick 2.15
import QtQuick.Controls 2.15

Item {
    id: autoscrollRoot
    anchors.fill: parent
    z: 9999

    property var target: null
    signal scrolledUp()
    signal scrolledToBottom()

    property bool active: false
    property real originX: 0
    property real originY: 0
    property real currentX: 0
    property real currentY: 0
    property bool hasDragged: false
    property real lastStopTime: 0

    // Middle-click detector to toggle mode
    TapHandler {
        target: autoscrollRoot.parent
        acceptedButtons: Qt.MiddleButton
        onTapped: function(eventPoint) {
            var now = Date.now()
            if (now - autoscrollRoot.lastStopTime < 220) return
            if (autoscrollRoot.active) {
                autoscrollRoot.stopAutoscroll()
            } else {
                autoscrollRoot.startAutoscroll(eventPoint.position.x, eventPoint.position.y)
            }
        }
    }

    function startAutoscroll(x, y) {
        originX = x
        originY = y
        currentX = x
        currentY = y
        hasDragged = false
        active = true
    }

    function stopAutoscroll() {
        lastStopTime = Date.now()
        active = false
    }

    Shortcut {
        enabled: autoscrollRoot.active
        sequence: "Escape"
        onActivated: autoscrollRoot.stopAutoscroll()
    }

    // Fullscreen capture area: hides normal OS mouse cursor completely
    MouseArea {
        id: captureArea
        anchors.fill: parent
        enabled: autoscrollRoot.active
        visible: autoscrollRoot.active
        hoverEnabled: true
        acceptedButtons: Qt.LeftButton | Qt.MiddleButton | Qt.RightButton
        preventStealing: true

        // Hide OS cursor so user sees ONLY the custom circle mouse
        cursorShape: Qt.BlankCursor

        onPositionChanged: function(mouse) {
            autoscrollRoot.currentX = mouse.x
            autoscrollRoot.currentY = mouse.y
            if (Math.abs(mouse.y - autoscrollRoot.originY) > 6) {
                autoscrollRoot.hasDragged = true
            }
        }

        onPressed: function(mouse) {
            mouse.accepted = true
            autoscrollRoot.stopAutoscroll()
        }

        onReleased: function(mouse) {
            if (autoscrollRoot.hasDragged && mouse.button === Qt.MiddleButton) {
                mouse.accepted = true
                autoscrollRoot.stopAutoscroll()
            }
        }
    }

    // Origin Anchor (subtle indicator showing the click reference position)
    Rectangle {
        id: originAnchor
        visible: autoscrollRoot.active
        x: autoscrollRoot.originX - width / 2
        y: autoscrollRoot.originY - height / 2
        width: 12
        height: 12
        radius: 6
        color: "#2038BDF8"
        border.color: "#475569"
        border.width: 1

        Rectangle {
            anchors.centerIn: parent
            width: 4
            height: 4
            radius: 2
            color: "#94A3B8"
        }
    }

    // Custom Circle Mouse (replaces normal cursor; follows mouse position)
    Rectangle {
        id: customCircleMouse
        visible: autoscrollRoot.active
        x: autoscrollRoot.currentX - width / 2
        y: autoscrollRoot.currentY - height / 2
        width: 32
        height: 32
        radius: 16
        color: "#F00F131C"
        border.color: (anchorSymbolDeltaY < -6 || anchorSymbolDeltaY > 6) ? "#38BDF8" : "#64748B"
        border.width: 1.5

        readonly property real anchorSymbolDeltaY: autoscrollRoot.currentY - autoscrollRoot.originY

        // Top Arrow
        Text {
            anchors.top: parent.top
            anchors.topMargin: 3
            anchors.horizontalCenter: parent.horizontalCenter
            text: "▲"
            font.pixelSize: 8
            font.bold: true
            color: customCircleMouse.anchorSymbolDeltaY < -6 ? "#38BDF8" : "#94A3B8"
        }

        // Center Dot
        Rectangle {
            anchors.centerIn: parent
            width: 4
            height: 4
            radius: 2
            color: "#FFFFFF"
        }

        // Bottom Arrow
        Text {
            anchors.bottom: parent.bottom
            anchors.bottomMargin: 3
            anchors.horizontalCenter: parent.horizontalCenter
            text: "▼"
            font.pixelSize: 8
            font.bold: true
            color: customCircleMouse.anchorSymbolDeltaY > 6 ? "#38BDF8" : "#94A3B8"
        }
    }

    // Smooth scroll interpolation timer
    Timer {
        interval: 16
        repeat: true
        running: autoscrollRoot.active && autoscrollRoot.target !== null
        onTriggered: {
            if (!autoscrollRoot.target) return
            var diff = autoscrollRoot.currentY - autoscrollRoot.originY
            if (Math.abs(diff) > 6) {
                var maxY = Math.max(0, autoscrollRoot.target.contentHeight - autoscrollRoot.target.height)
                var dir = diff > 0 ? 1 : -1
                var dist = Math.abs(diff) - 6
                var speed = dir * Math.min(50, Math.pow(dist * 0.16, 1.3))
                var newContentY = Math.max(0, Math.min(maxY, autoscrollRoot.target.contentY + speed))
                autoscrollRoot.target.contentY = newContentY

                if (speed < 0) {
                    autoscrollRoot.scrolledUp()
                } else if (autoscrollRoot.target.atYEnd) {
                    autoscrollRoot.scrolledToBottom()
                }
            }
        }
    }
}
