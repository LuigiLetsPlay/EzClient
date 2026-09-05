import QtQuick 2.15
import ".."

Item {
    id: root

    property string iconNameOrPath: ""
    property string fallbackName: "MC"
    property real radius: 8
    property color bgColor: EzTheme.surface3
    property color borderColor: EzTheme.borderLight
    property real borderWidth: 1
    property color textColor: EzTheme.accentLight
    property int fontSize: 13
    property bool isCircular: false
    property bool interactive: false
    signal clicked()

    function resolveSource(raw) {
        if (!raw) return ""
        var s = String(raw).trim()
        if (s === "") return ""
        if (s === "norisk" || s === "client-norisk" || s === "client-norisk.png") {
            return Qt.resolvedUrl("../icons/client-norisk.png")
        }
        if (s === "ezclient" || s === "logo" || s === "ezclient.png") {
            return Qt.resolvedUrl("../assets/logo.png")
        }
        if (s === "grass-block" || s === "box") return Qt.resolvedUrl("../icons/grass-block.png")
        if (s === "sand-block" || s === "sand") return Qt.resolvedUrl("../icons/sand-block.png")
        if (s === "tnt") return Qt.resolvedUrl("../icons/tnt.png")
        if (s === "potion") return Qt.resolvedUrl("../icons/potion.png")
        if (s === "clock") return Qt.resolvedUrl("../icons/clock.png")
        if (s === "flame" || s === "flint") return Qt.resolvedUrl("../icons/minecraft-flint.png")
        if (s === "sparkles" || s === "star") return Qt.resolvedUrl("../icons/minecraft-star.png")
        if (s === "compass") return Qt.resolvedUrl("../icons/minecraft-compass.png")
        // Keep old saved preset ids working, but resolve them to raster art.
        if (s === "shield" || s === "vanilla") return Qt.resolvedUrl("../icons/grass-block.png")
        if (s === "zap" || s === "forge") return Qt.resolvedUrl("../icons/minecraft-flint.png")

        if (s.indexOf("file://") === 0 || s.indexOf("http://") === 0 || s.indexOf("https://") === 0) {
            return s
        }
        if (s.indexOf(":") === 1 || s.indexOf("/") === 0 || s.indexOf("\\") === 0) {
            return "file:///" + s.replace(/\\/g, "/")
        }
        // Profile presets are raster-only. Legacy SVG ids deliberately fall
        // back to a same-named PNG (or initials when no PNG exists).
        if (s.toLowerCase().endsWith(".svg")) s = s.substring(0, s.length - 4) + ".png"
        return Qt.resolvedUrl("../icons/" + s + (s.indexOf(".") === -1 ? ".png" : ""))
    }

    readonly property string effectiveSource: resolveSource(iconNameOrPath)
    readonly property bool isCustomImage: effectiveSource.indexOf("file:") >= 0 || effectiveSource.indexOf("http:") >= 0

    Rectangle {
        anchors.fill: parent
        radius: root.isCircular ? width / 2 : root.radius
        color: root.bgColor
        border.color: (root.interactive && mouseArea.containsMouse) ? EzTheme.accent : root.borderColor
        border.width: (root.interactive && mouseArea.containsMouse) ? Math.max(root.borderWidth, 1.5) : root.borderWidth
        clip: true

        Behavior on border.color { ColorAnimation { duration: 100 } }

        Image {
            id: img
            anchors.fill: parent
            anchors.margins: root.isCustomImage ? 0 : ((root.iconNameOrPath === "norisk" || root.iconNameOrPath === "ezclient" || root.iconNameOrPath === "grass-block" || root.iconNameOrPath === "box") ? 4 : 7)
            fillMode: root.isCustomImage ? Image.PreserveAspectCrop : Image.PreserveAspectFit
            source: root.effectiveSource
            visible: status === Image.Ready && root.effectiveSource !== ""
            smooth: true
            mipmap: true
            asynchronous: true
        }

        Text {
            visible: !img.visible
            anchors.centerIn: parent
            text: {
                var name = (root.fallbackName || "").trim()
                if (name.length >= 2) return name.substring(0, 2).toUpperCase()
                if (name.length === 1) return name.toUpperCase()
                return "MC"
            }
            font.family: EzTheme.fontFamily
            font.pixelSize: root.fontSize
            font.bold: true
            color: root.textColor
        }

        Rectangle {
            anchors.fill: parent
            radius: parent.radius
            color: "#77000000"
            visible: root.interactive && mouseArea.containsMouse

            Text {
                anchors.centerIn: parent
                text: "✎"
                color: "#FFFFFF"
                font.pixelSize: Math.max(10, Math.floor(root.height * 0.38))
            }
        }

        MouseArea {
            id: mouseArea
            anchors.fill: parent
            enabled: root.interactive
            hoverEnabled: root.interactive
            cursorShape: root.interactive ? Qt.PointingHandCursor : Qt.ArrowCursor
            onClicked: root.clicked()
        }
    }
}
