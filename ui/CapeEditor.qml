import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "components"

Item {
    id: root
    signal navigate(string route)
    property color selectedColor: "#A78BFA"
    property var pixels: []

    function resetCanvas() {
        var values = []
        for (var i = 0; i < 64 * 32; i++) values.push("")
        root.pixels = values
        editorCanvas.requestPaint()
    }
    function setPixel(x, y) {
        x = Math.floor(x); y = Math.floor(y)
        if (x < 0 || x >= 64 || y < 0 || y >= 32) return
        var values = root.pixels.slice(0)
        values[y * 64 + x] = root.selectedColor
        root.pixels = values
        editorCanvas.requestPaint()
    }
    function importActiveCape() {
        if (importCape.status !== Image.Ready) return
        var ctx = editorCanvas.getContext("2d")
        ctx.clearRect(0, 0, 64, 32)
        ctx.drawImage(importCape, 0, 0, 64, 32)
        var imageData = ctx.getImageData(0, 0, 64, 32).data
        var values = []
        for (var i = 0; i < 64 * 32; i++) {
            var offset = i * 4
            if (imageData[offset + 3] === 0) values.push("")
            else values.push("#" + imageData[offset].toString(16).padStart(2, "0")
                              + imageData[offset + 1].toString(16).padStart(2, "0")
                              + imageData[offset + 2].toString(16).padStart(2, "0"))
        }
        root.pixels = values
        editorCanvas.requestPaint()
    }
    function loadActiveCape() {
        if (typeof accountController === "undefined" || !accountController || accountController.capeTextureUrl === "") {
            root.resetCanvas()
            return
        }
        importCape.source = accountController.capeTextureUrl
        if (importCape.status === Image.Ready) root.importActiveCape()
    }
    Component.onCompleted: loadActiveCape()
    onVisibleChanged: if (visible) loadActiveCape()

    Image {
        id: importCape
        visible: false
        asynchronous: false
        onStatusChanged: if (status === Image.Ready) root.importActiveCape()
    }

    Rectangle { anchors.fill: parent; color: EzTheme.bg }
    ColumnLayout {
        anchors.fill: parent; anchors.margins: 30; spacing: 16
        RowLayout {
            Layout.fillWidth: true
            Text { text: "Cape Editor"; font.family: EzTheme.mcFontFamily; font.pixelSize: 22; font.bold: true; color: EzTheme.text; Layout.fillWidth: true }
            EzButton { text: "Zurück"; onClicked: root.navigate("cape") }
        }
        Text { text: "Male dein Cape Pixel für Pixel. Transparente Bereiche bleiben unsichtbar; das Ergebnis ist ein sicheres 64×32-PNG."; color: EzTheme.textSecondary; font.pixelSize: 12; wrapMode: Text.WordWrap; Layout.fillWidth: true }
        RowLayout {
            Layout.fillWidth: true; Layout.fillHeight: true; spacing: 24
            Rectangle {
                Layout.preferredWidth: 448; Layout.preferredHeight: 224; Layout.alignment: Qt.AlignHCenter | Qt.AlignVCenter
                color: "#171126"; border.color: EzTheme.border; border.width: 1; clip: true
                Canvas {
                    id: editorCanvas; width: 64; height: 32; scale: 7; transformOrigin: Item.TopLeft
                    onPaint: {
                        var ctx = getContext("2d")
                        ctx.clearRect(0, 0, width, height)
                        for (var y = 0; y < 32; y++) for (var x = 0; x < 64; x++) {
                            var color = root.pixels[y * 64 + x]
                            if (color) { ctx.fillStyle = color; ctx.fillRect(x, y, 1, 1) }
                        }
                    }
                    MouseArea {
                        anchors.fill: parent; hoverEnabled: true
                        onPressed: root.setPixel(mouse.x, mouse.y)
                        onPositionChanged: if (pressed) root.setPixel(mouse.x, mouse.y)
                    }
                }
            }
            ColumnLayout {
                Layout.fillWidth: true; Layout.alignment: Qt.AlignTop; spacing: 12
                Text { text: "Farbe"; color: EzTheme.text; font.family: EzTheme.mcFontFamily; font.bold: true }
                GridLayout {
                    columns: 5; columnSpacing: 8; rowSpacing: 8
                    Repeater {
                        model: ["#FFFFFF", "#1A102E", "#A78BFA", "#6D4CC7", "#EC4899", "#EF4444", "#F59E0B", "#EAB308", "#22C55E", "#06B6D4", "#3B82F6", "#111827", "#6B7280", "#C084FC", "#FDE68A"]
                        delegate: Rectangle {
                            Layout.preferredWidth: 30; Layout.preferredHeight: 30; radius: 7; color: modelData
                            border.color: root.selectedColor === modelData ? EzTheme.text : "transparent"; border.width: 2
                            MouseArea { anchors.fill: parent; cursorShape: Qt.PointingHandCursor; onClicked: root.selectedColor = modelData }
                        }
                    }
                }
                Item { Layout.preferredHeight: 10 }
                EzButton { text: "Leeren"; onClicked: root.resetCanvas() }
                EzButton { text: "Cape speichern"; onClicked: accountController.saveCapeDataUrl(editorCanvas.toDataURL("image/png")) }
                Text { text: "Tipp: Das linke Ende ist die Außenseite des Capes."; color: EzTheme.textMuted; font.pixelSize: 11; wrapMode: Text.WordWrap; Layout.fillWidth: true }
            }
        }
    }
}
