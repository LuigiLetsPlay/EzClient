import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "components"

Item {
    id: root
    signal navigate(string route)
    property color selectedColor: "#A78BFA"
    property var pixels: []
    property int capeWidth: 256
    property int capeHeight: 128
    property bool autoSave: true
    property var layers: []
    property int selectedLayer: -1
    property var selectedTextLayer: (selectedLayer >= 0 && selectedLayer < layers.length && layers[selectedLayer]) ? layers[selectedLayer] : ({})
    property int snapSize: 4
    property int brushRadius: 4
    property int stageWidth: 224
    property int stageHeight: 448
    property string stageAction: ""
    property string stageEdge: ""
    property int stageLayer: -1
    property real stageStartX: 0
    property real stageStartY: 0
    property real stageLayerX: 0
    property real stageLayerY: 0
    property real stageLayerW: 0
    property real stageLayerH: 0
    property string actionMessage: ""
    property bool actionError: false
    onLayersChanged: if (typeof editorCanvas !== "undefined") editorCanvas.requestPaint()
    property string tool: "select"
    property real zoom: 1.0
    property bool showGrid: true

    function addTextLayer() {
        var values = root.layers.slice(0)
        values.push({ type: "text", text: "Text", x: 62, y: 120, w: 100, h: 38, scale: 1, color: root.selectedColor, font: "Segoe UI", bold: false })
        root.layers = values; root.selectedLayer = values.length - 1
    }
    function saveCurrentCape() {
        root.paintCape(exportCanvas.getContext("2d"), true)
        var ok = accountController.saveCapeDataUrl(exportCanvas.toDataURL("image/png"))
        if (!ok) { root.actionMessage = "Cape konnte nicht gespeichert werden."; root.actionError = true }
        return ok
    }
    function exportCurrentCape() {
        if (root.saveCurrentCape()) accountController.exportCapeFile()
    }
    function paintCape(ctx, includeLayers) {
        ctx.clearRect(0, 0, root.capeWidth, root.capeHeight)
        for (var y = 0; y < root.capeHeight; y++) for (var x = 0; x < root.capeWidth; x++) {
            var color = root.pixels[y * root.capeWidth + x]
            if (color) { ctx.fillStyle = color; ctx.fillRect(x, y, 1, 1) }
        }
        if (!includeLayers) return
        for (var i = 0; i < root.layers.length; ++i) {
            var layer = root.layers[i]
            if (!layer) continue
            // Invert the 90° portrait preview transformation so the actual
            // landscape cape texture produces the same result in-game.
            var targetW = Math.max(1, Math.round((layer.h || 36) / 1.75))
            var targetH = Math.max(1, Math.round((layer.w || 80) / 1.75))
            var targetX = Math.round((layer.y || 0) / 1.75)
            var targetY = Math.round((root.stageWidth - (layer.x || 0) - (layer.w || 80)) / 1.75)
            if (layer.type === "image") {
                var sourceImage = exportLayerImages.itemAt(i)
                if (sourceImage && sourceImage.status === Image.Ready) {
                    ctx.save()
                    ctx.translate(targetX + targetW, targetY)
                    ctx.rotate(Math.PI / 2)
                    ctx.drawImage(sourceImage, 0, 0, targetH, targetW)
                    ctx.restore()
                }
            } else if (layer.type === "text") {
                ctx.fillStyle = layer.color || "#FFFFFF"
                ctx.font = ((layer.bold ? "bold " : "") + Math.max(10, Math.round(12 * (layer.scale || 1))) + "px " + (layer.font || "sans-serif"))
                ctx.save()
                ctx.translate(targetX + targetW, targetY)
                ctx.rotate(Math.PI / 2)
                ctx.fillText(layer.text || "", 0, Math.max(10, Math.round(12 * (layer.scale || 1))))
                ctx.restore()
            }
        }
    }
    function addImageLayer() {
        var imageUrl = accountController.pickCapeLayerImage()
        if (!imageUrl) return
        var values = root.layers.slice(0)
        values.push({ type: "image", source: imageUrl, x: 52, y: 132, w: 120, h: 120 })
        root.layers = values; root.selectedLayer = values.length - 1
    }
    function updateLayer(index, dx, dy) {
        var values = root.layers.slice(0); var oldLayer = values[index]
        var layer = {
            type: oldLayer.type, text: oldLayer.text, x: oldLayer.x, y: oldLayer.y,
            scale: oldLayer.scale, color: oldLayer.color, font: oldLayer.font,
            bold: oldLayer.bold, source: oldLayer.source, w: oldLayer.w, h: oldLayer.h
        }
        layer.x = Math.max(0, Math.min(root.capeWidth - 1, Math.round((layer.x + dx) / root.snapSize) * root.snapSize))
        layer.y = Math.max(0, Math.min(root.capeHeight - 1, Math.round((layer.y + dy) / root.snapSize) * root.snapSize))
        values[index] = layer; root.layers = values
    }
    function resizeLayer(index, dw, dh) {
        var values = root.layers.slice(0); var oldLayer = values[index]
        var layer = {
            type: oldLayer.type, text: oldLayer.text, x: oldLayer.x, y: oldLayer.y,
            scale: oldLayer.scale, color: oldLayer.color, font: oldLayer.font,
            bold: oldLayer.bold, source: oldLayer.source, w: oldLayer.w, h: oldLayer.h
        }
        if (layer.type !== "image") return
        layer.w = Math.max(12, Math.min(root.capeWidth - layer.x, Math.round((layer.w + dw) / root.snapSize) * root.snapSize))
        layer.h = Math.max(12, Math.min(root.capeHeight - layer.y, Math.round((layer.h + dh) / root.snapSize) * root.snapSize))
        values[index] = layer; root.layers = values
    }
    function layerAt(px, py) {
        for (var i = root.layers.length - 1; i >= 0; --i) {
            var layer = root.layers[i]
            if (!layer) continue
            var width = layer.w || 80
            var height = layer.h || 36
            if (px >= layer.x && px <= layer.x + width && py >= layer.y && py <= layer.y + height) return i
        }
        return -1
    }
    function setStageLayerGeometry(index, x, y, width, height) {
        if (index < 0 || index >= root.layers.length) return
        var values = root.layers.slice(0); var oldLayer = values[index]
        var layer = {
            type: oldLayer.type, text: oldLayer.text, source: oldLayer.source,
            color: oldLayer.color, font: oldLayer.font, bold: oldLayer.bold,
            scale: oldLayer.scale, x: x, y: y, w: width, h: height
        }
        layer.w = Math.max(layer.type === "text" ? 50 : 24, Math.min(4096, layer.w))
        layer.h = Math.max(layer.type === "text" ? 22 : 24, Math.min(4096, layer.h))
        values[index] = layer
        root.layers = values
        if (root.autoSave) autoSaveTimer.restart()
    }
    function resizeEdgeAt(layer, px, py) {
        if (!layer) return ""
        var x = layer.x || 0; var y = layer.y || 0; var w = layer.w || 80; var h = layer.h || 36; var grip = 18
        var left = px <= x + grip; var right = px >= x + w - grip
        var top = py <= y + grip; var bottom = py >= y + h - grip
        if (left && top) return "nw"
        if (right && top) return "ne"
        if (left && bottom) return "sw"
        if (right && bottom) return "se"
        return ""
    }
    function updateTextLayer(key, value) {
        if (root.selectedLayer < 0 || root.selectedLayer >= root.layers.length) return
        var values = root.layers.slice(0); var oldLayer = values[root.selectedLayer]
        var layer = {
            type: oldLayer.type, text: oldLayer.text, x: oldLayer.x, y: oldLayer.y,
            scale: oldLayer.scale, color: oldLayer.color, font: oldLayer.font,
            bold: oldLayer.bold, source: oldLayer.source, w: oldLayer.w, h: oldLayer.h
        }
        if (layer.type !== "text") return
        layer[key] = value
        if (key === "text" || key === "scale") {
            var textScale = layer.scale || 1
            layer.w = Math.max(50, Math.min(root.stageWidth - layer.x, (String(layer.text || "").length * 9 + 18) * textScale))
            layer.h = Math.max(22, Math.min(root.stageHeight - layer.y, 24 * textScale + 10))
        }
        values[root.selectedLayer] = layer; root.layers = values
        if (root.autoSave) autoSaveTimer.restart()
    }
    function removeSelectedLayer() {
        if (root.selectedLayer < 0) return
        var values = root.layers.slice(0); values.splice(root.selectedLayer, 1)
        root.layers = values; root.selectedLayer = -1
    }
    function moveSelectedLayer(delta) {
        if (root.selectedLayer < 0) return
        var values = root.layers.slice(0); var target = root.selectedLayer + delta
        if (target < 0 || target >= values.length) return
        var item = values[root.selectedLayer]; values[root.selectedLayer] = values[target]; values[target] = item
        root.layers = values; root.selectedLayer = target
    }

    function resetCanvas() {
        var values = []
        for (var i = 0; i < root.capeWidth * root.capeHeight; i++) values.push("")
        root.pixels = values
        editorCanvas.requestPaint()
        if (root.autoSave) autoSaveTimer.restart()
    }
    function setPixel(x, y) {
        if (root.tool === "select") return
        x = Math.floor(x); y = Math.floor(y)
        if (x < 0 || x >= root.capeWidth || y < 0 || y >= root.capeHeight) return
        var values = root.pixels.slice(0)
        var radius = Math.max(1, root.brushRadius)
        for (var py = Math.max(0, y - radius); py <= Math.min(root.capeHeight - 1, y + radius); py++) {
            for (var px = Math.max(0, x - radius); px <= Math.min(root.capeWidth - 1, x + radius); px++) {
                var dx = px - x; var dy = py - y
                if (dx * dx + dy * dy <= radius * radius)
                    values[py * root.capeWidth + px] = root.tool === "erase" ? "" : root.selectedColor
            }
        }
        root.pixels = values
        editorCanvas.requestPaint()
    }
    function importActiveCape() {
        if (importCape.status !== Image.Ready) return
        var ctx = editorCanvas.getContext("2d")
        ctx.clearRect(0, 0, root.capeWidth, root.capeHeight)
        ctx.drawImage(importCape, 0, 0, root.capeWidth, root.capeHeight)
        var imageData = ctx.getImageData(0, 0, root.capeWidth, root.capeHeight).data
        var values = []
        for (var i = 0; i < root.capeWidth * root.capeHeight; i++) {
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
    Timer {
        id: autoSaveTimer
        interval: 900
        repeat: false
        onTriggered: root.saveCurrentCape()
    }
    Connections {
        target: accountController
        function onSkinUploadStatusChanged(message, isError) {
            root.actionMessage = message
            root.actionError = isError
        }
        function onCapeCommunityStatusChanged(message, isError) {
            root.actionMessage = message
            root.actionError = isError
        }
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
        Rectangle {
            visible: root.actionMessage !== ""
            Layout.fillWidth: true; Layout.preferredHeight: messageText.implicitHeight + 16
            radius: 8; color: root.actionError ? "#3A1724" : "#153126"; border.color: root.actionError ? "#EF4444" : "#22C55E"
            Text { id: messageText; anchors.fill: parent; anchors.margins: 8; text: root.actionMessage; color: EzTheme.text; wrapMode: Text.WordWrap; font.pixelSize: 12 }
        }
        RowLayout {
            Layout.fillWidth: true; Layout.fillHeight: true; spacing: 24
            Rectangle {
                Layout.preferredWidth: 224; Layout.preferredHeight: 448; Layout.alignment: Qt.AlignHCenter | Qt.AlignVCenter
                color: "#171126"; border.color: EzTheme.border; border.width: 1; clip: true
                Canvas {
                    id: editorCanvas; anchors.centerIn: parent; width: root.capeWidth; height: root.capeHeight; scale: 1.75; rotation: 90; transformOrigin: Item.Center
                    onPaint: root.paintCape(getContext("2d"), true)
                }
                Canvas { id: exportCanvas; width: root.capeWidth; height: root.capeHeight; visible: false }
                Repeater {
                    id: exportLayerImages
                    model: root.layers.length
                    delegate: Image {
                        width: 1; height: 1; opacity: 0
                        source: root.layers[index] && root.layers[index].type === "image" ? root.layers[index].source : ""
                        asynchronous: false
                        onStatusChanged: if (status === Image.Ready) { editorCanvas.requestPaint(); exportCanvas.requestPaint() }
                    }
                }
                Repeater {
                    model: root.layers.length
                    delegate: Item {
                        property var capeLayer: root.layers[index] || ({ type: "image", x: 0, y: 0, w: 12, h: 12, source: "", text: "", color: "#ffffff", scale: 1 })
                        x: capeLayer.x || 0; y: capeLayer.y || 0; z: 10
                        width: capeLayer.w || (capeLayer.type === "image" ? 80 : textPreview.implicitWidth + 14)
                        height: capeLayer.h || (capeLayer.type === "image" ? 80 : textPreview.implicitHeight + 10)
                        Image { anchors.fill: parent; source: capeLayer.type === "image" ? capeLayer.source : ""; fillMode: Image.PreserveAspectFit; visible: false }
                        Text {
                            id: textPreview
                            anchors.centerIn: parent
                            visible: false
                            text: capeLayer.text || ""
                            color: capeLayer.color || "#FFFFFF"
                            font.family: capeLayer.font || "Segoe UI"
                            font.bold: capeLayer.bold || false
                            font.pixelSize: Math.max(12, 12 * (capeLayer.scale || 1))
                        }
                        Rectangle { anchors.fill: parent; color: "transparent"; border.color: root.selectedLayer === index ? EzTheme.accentLight : "transparent"; border.width: 1 }
                    }
                }
                MouseArea {
                    id: stageInput
                    anchors.fill: parent; z: 100; hoverEnabled: true; preventStealing: true
                    cursorShape: {
                        if (root.tool !== "select") return Qt.CrossCursor
                        var hit = root.layerAt(mouseX, mouseY)
                        if (hit < 0) return Qt.ArrowCursor
                        var layer = root.layers[hit]
                        var edge = root.resizeEdgeAt(layer, mouseX, mouseY)
                        if (edge === "nw" || edge === "se") return Qt.SizeFDiagCursor
                        if (edge === "ne" || edge === "sw") return Qt.SizeBDiagCursor
                        return Qt.SizeAllCursor
                    }
                    onPressed: function(mouse) {
                        if (root.tool !== "select") {
                            root.stageAction = "paint"
                            root.setPixel(Math.floor(mouse.y / 1.75), Math.floor(mouse.x / 1.75))
                            return
                        }
                        var hit = root.layerAt(mouse.x, mouse.y)
                        root.selectedLayer = hit
                        root.stageLayer = hit
                        if (hit < 0) { root.stageAction = ""; return }
                        var layer = root.layers[hit]
                        root.stageStartX = mouse.x; root.stageStartY = mouse.y
                        root.stageLayerX = layer.x || 0; root.stageLayerY = layer.y || 0
                        root.stageLayerW = layer.w || 80; root.stageLayerH = layer.h || 36
                        root.stageEdge = root.resizeEdgeAt(layer, mouse.x, mouse.y)
                        root.stageAction = root.stageEdge !== "" ? "resize" : "move"
                    }
                    onPositionChanged: function(mouse) {
                        if (!pressed) return
                        if (root.stageAction === "paint") {
                            root.setPixel(Math.floor(mouse.y / 1.75), Math.floor(mouse.x / 1.75)); return
                        }
                        if (root.stageLayer < 0) return
                        var dx = mouse.x - root.stageStartX; var dy = mouse.y - root.stageStartY
                        if (root.stageAction === "move") {
                            var nx = root.stageLayerX + dx
                            var ny = root.stageLayerY + dy
                            root.setStageLayerGeometry(root.stageLayer, Math.round(nx / root.snapSize) * root.snapSize, Math.round(ny / root.snapSize) * root.snapSize, root.stageLayerW, root.stageLayerH)
                        } else if (root.stageAction === "resize") {
                            var nx2 = root.stageLayerX; var ny2 = root.stageLayerY
                            var nw = root.stageLayerW; var nh = root.stageLayerH
                            if (root.stageEdge.indexOf("e") >= 0) nw += dx
                            if (root.stageEdge.indexOf("s") >= 0) nh += dy
                            if (root.stageEdge.indexOf("w") >= 0) { nw -= dx; nx2 += dx }
                            if (root.stageEdge.indexOf("n") >= 0) { nh -= dy; ny2 += dy }
                            if (nw > 24 && nh > 24)
                                root.setStageLayerGeometry(root.stageLayer, Math.round(nx2 / root.snapSize) * root.snapSize, Math.round(ny2 / root.snapSize) * root.snapSize, Math.round(nw / root.snapSize) * root.snapSize, Math.round(nh / root.snapSize) * root.snapSize)
                        }
                    }
                    onReleased: { root.stageAction = ""; root.stageEdge = ""; root.stageLayer = root.selectedLayer }
                }
                Rectangle {
                    visible: root.selectedLayer >= 0 && root.selectedLayer < root.layers.length
                    z: 101
                    property var selected: root.selectedTextLayer
                    x: selected.x || 0; y: selected.y || 0
                    width: selected.w || 80; height: selected.h || 36
                    color: "transparent"; border.color: EzTheme.accentLight; border.width: 1
                    Rectangle { width: 14; height: 14; radius: 3; anchors.left: parent.left; anchors.top: parent.top; color: EzTheme.accentLight; border.color: EzTheme.bg }
                    Rectangle { width: 14; height: 14; radius: 3; anchors.right: parent.right; anchors.top: parent.top; color: EzTheme.accentLight; border.color: EzTheme.bg }
                    Rectangle { width: 14; height: 14; radius: 3; anchors.left: parent.left; anchors.bottom: parent.bottom; color: EzTheme.accentLight; border.color: EzTheme.bg }
                    Rectangle { width: 14; height: 14; radius: 3; anchors.right: parent.right; anchors.bottom: parent.bottom; color: EzTheme.accentLight; border.color: EzTheme.bg }
                }
                Rectangle {
                    visible: root.tool !== "select" && stageInput.containsMouse
                    z: 110; x: stageInput.mouseX - root.brushRadius * 1.75; y: stageInput.mouseY - root.brushRadius * 1.75
                    width: root.brushRadius * 3.5; height: root.brushRadius * 3.5; radius: width / 2
                    color: "transparent"; border.color: root.tool === "erase" ? "#F87171" : "#FFFFFF"; border.width: 1
                }
            }
            ScrollView {
                Layout.fillWidth: true
                Layout.fillHeight: true
                clip: true
                contentWidth: availableWidth

                ColumnLayout {
                id: editorTools
                width: parent.availableWidth
                spacing: 12
                Text { text: "Werkzeuge"; color: EzTheme.text; font.family: EzTheme.mcFontFamily; font.bold: true }
                RowLayout {
                    EzButton { text: "Auswahl"; primary: root.tool === "select"; onClicked: root.tool = "select" }
                    EzButton { text: "Pinsel"; primary: root.tool === "brush"; onClicked: root.tool = "brush" }
                    EzButton { text: "Radierer"; primary: root.tool === "erase"; onClicked: root.tool = "erase" }
                }
                RowLayout {
                    Text { text: "Radius " + root.brushRadius; color: EzTheme.textSecondary }
                    Slider { Layout.fillWidth: true; from: 1; to: 24; stepSize: 1; value: root.brushRadius; onMoved: root.brushRadius = Math.round(value) }
                }
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
                EzButton { text: "Cape importieren"; onClicked: { if (accountController.pickCapeFile()) root.loadActiveCape() } }
                EzButton { text: "Als PNG exportieren"; onClicked: root.exportCurrentCape() }
                EzButton { text: "Auf Mojang-Cape zurücksetzen"; onClicked: { accountController.resetCustomCape(); root.resetCanvas() } }
                EzButton { text: "Bild hinzufügen"; onClicked: root.addImageLayer() }
                EzButton { text: "Text hinzufügen"; onClicked: root.addTextLayer() }
                Rectangle {
                    Layout.fillWidth: true; Layout.preferredHeight: layerPanel.implicitHeight + 18; radius: 10; color: EzTheme.surface2; border.color: EzTheme.border
                    ColumnLayout {
                        id: layerPanel; anchors.fill: parent; anchors.margins: 9; spacing: 5
                        Text { text: "Ebenen"; color: EzTheme.text; font.family: EzTheme.mcFontFamily; font.bold: true }
                        Repeater { model: root.layers.length; delegate: EzButton { Layout.fillWidth: true; text: (root.layers[index].type === "text" ? "T  " + root.layers[index].text : "▣ Bild") ; primary: root.selectedLayer === index; onClicked: root.selectedLayer = index } }
                        RowLayout { EzButton { text: "▲"; onClicked: root.moveSelectedLayer(-1) } EzButton { text: "▼"; onClicked: root.moveSelectedLayer(1) } EzButton { text: "Löschen"; onClicked: root.removeSelectedLayer() } }
                    }
                }
                Rectangle {
                    visible: root.selectedTextLayer.type === "text"
                    Layout.fillWidth: true; Layout.preferredHeight: textOptions.implicitHeight + 18
                    radius: 10; color: EzTheme.surface2; border.color: EzTheme.border
                    ColumnLayout {
                        id: textOptions; anchors.fill: parent; anchors.margins: 9; spacing: 6
                        Text { text: "Text gestalten"; font.family: EzTheme.mcFontFamily; font.bold: true; color: EzTheme.text }
                        TextField { Layout.fillWidth: true; text: root.selectedTextLayer.text || ""; onTextEdited: root.updateTextLayer("text", text) }
                        ComboBox {
                            Layout.fillWidth: true
                            model: ["Segoe UI", "Arial", "Minecraft", "Verdana", "Courier New"]
                            currentIndex: Math.max(0, model.indexOf(root.selectedTextLayer.font || "Segoe UI"))
                            onActivated: function(activatedIndex) { root.updateTextLayer("font", model[activatedIndex]) }
                        }
                        RowLayout {
                            Layout.fillWidth: true
                            Text { text: "Größe"; color: EzTheme.textSecondary }
                            Slider { Layout.fillWidth: true; from: 0.5; to: 4; value: root.selectedTextLayer.scale || 1; onMoved: root.updateTextLayer("scale", value) }
                            CheckBox { text: "Fett"; checked: root.selectedTextLayer.bold || false; onToggled: root.updateTextLayer("bold", checked) }
                        }
                    }
                }
                EzButton { text: "Cape speichern"; onClicked: root.saveCurrentCape() }
                EzButton { text: "Veröffentlichen"; primary: true; onClicked: { if (root.saveCurrentCape()) publishDialog.open() } }
                Text { text: "Tipp: Das linke Ende ist die Außenseite des Capes."; color: EzTheme.textMuted; font.pixelSize: 11; wrapMode: Text.WordWrap; Layout.fillWidth: true }
                }
            }
        }
    }
    Dialog {
        id: publishDialog
        modal: true; anchors.centerIn: parent; width: 380; title: "Cape veröffentlichen"
        background: Rectangle { radius: 14; color: EzTheme.surface2; border.color: EzTheme.border }
        contentItem: ColumnLayout {
            Text { text: "Veröffentlicht wird dein aktuell gespeichertes Cape."; color: EzTheme.textSecondary; wrapMode: Text.WordWrap; Layout.fillWidth: true }
            TextField { id: capeTitle; Layout.fillWidth: true; placeholderText: "Cape-Name"; color: EzTheme.text }
        }
        footer: DialogButtonBox {
            Button { text: "Abbrechen"; DialogButtonBox.buttonRole: DialogButtonBox.RejectRole }
            Button { text: "Veröffentlichen"; DialogButtonBox.buttonRole: DialogButtonBox.AcceptRole }
        }
        onAccepted: { if (root.saveCurrentCape()) accountController.publishCape(capeTitle.text) }
    }
}
