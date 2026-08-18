import QtQuick 2.15

Item {
    id: root
    anchors.fill: parent
    z: 9999
    property var windowRef: null

    readonly property int edgeSize: 6
    readonly property int cornerSize: 14

    // Top Edge
    MouseArea {
        anchors.top: parent.top
        anchors.left: parent.left
        anchors.right: parent.right
        anchors.leftMargin: root.cornerSize
        anchors.rightMargin: root.cornerSize
        height: root.edgeSize
        cursorShape: Qt.SizeVerCursor
        onPressed: if (root.windowRef && root.windowRef.visibility !== 4) root.windowRef.startSystemResize(Qt.TopEdge)
    }

    // Bottom Edge
    MouseArea {
        anchors.bottom: parent.bottom
        anchors.left: parent.left
        anchors.right: parent.right
        anchors.leftMargin: root.cornerSize
        anchors.rightMargin: root.cornerSize
        height: root.edgeSize
        cursorShape: Qt.SizeVerCursor
        onPressed: if (root.windowRef && root.windowRef.visibility !== 4) root.windowRef.startSystemResize(Qt.BottomEdge)
    }

    // Left Edge
    MouseArea {
        anchors.left: parent.left
        anchors.top: parent.top
        anchors.bottom: parent.bottom
        anchors.topMargin: root.cornerSize
        anchors.bottomMargin: root.cornerSize
        width: root.edgeSize
        cursorShape: Qt.SizeHorCursor
        onPressed: if (root.windowRef && root.windowRef.visibility !== 4) root.windowRef.startSystemResize(Qt.LeftEdge)
    }

    // Right Edge
    MouseArea {
        anchors.right: parent.right
        anchors.top: parent.top
        anchors.bottom: parent.bottom
        anchors.topMargin: root.cornerSize
        anchors.bottomMargin: root.cornerSize
        width: root.edgeSize
        cursorShape: Qt.SizeHorCursor
        onPressed: if (root.windowRef && root.windowRef.visibility !== 4) root.windowRef.startSystemResize(Qt.RightEdge)
    }

    // Top-Left Corner
    MouseArea {
        anchors.top: parent.top
        anchors.left: parent.left
        width: root.cornerSize
        height: root.cornerSize
        cursorShape: Qt.SizeFDiagCursor
        onPressed: if (root.windowRef && root.windowRef.visibility !== 4) root.windowRef.startSystemResize(Qt.TopEdge | Qt.LeftEdge)
    }

    // Top-Right Corner
    MouseArea {
        anchors.top: parent.top
        anchors.right: parent.right
        width: root.cornerSize
        height: root.cornerSize
        cursorShape: Qt.SizeBDiagCursor
        onPressed: if (root.windowRef && root.windowRef.visibility !== 4) root.windowRef.startSystemResize(Qt.TopEdge | Qt.RightEdge)
    }

    // Bottom-Left Corner
    MouseArea {
        anchors.bottom: parent.bottom
        anchors.left: parent.left
        width: root.cornerSize
        height: root.cornerSize
        cursorShape: Qt.SizeBDiagCursor
        onPressed: if (root.windowRef && root.windowRef.visibility !== 4) root.windowRef.startSystemResize(Qt.BottomEdge | Qt.LeftEdge)
    }

    // Bottom-Right Corner
    MouseArea {
        anchors.bottom: parent.bottom
        anchors.right: parent.right
        width: root.cornerSize
        height: root.cornerSize
        cursorShape: Qt.SizeFDiagCursor
        onPressed: if (root.windowRef && root.windowRef.visibility !== 4) root.windowRef.startSystemResize(Qt.BottomEdge | Qt.RightEdge)
    }
}
