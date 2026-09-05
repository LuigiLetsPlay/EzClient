import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "components"

Item {
    id: root
    signal navigate(string route)
    property string selectedSource: ""
    property string pendingPreview: ""
    // Normalized crop window over the selected picture (0..1).
    property real cropX: 0
    property real cropY: 0
    property real cropW: 1
    property real cropH: 1
    property string capeName: ""
    property string previewAnimation: "idle"
    property bool showPixelGrid: true
    property bool animatedSource: false
    property bool mediaProcessing: false
    property real mediaDuration: 0
    property real trimStart: 0
    property real trimEnd: 5
    property int animationFps: 12
    property string cropImageSource: ""
    property int previewRequestId: 0
    property bool previewProcessing: false

    function loadSource(url) {
        if (!url) return
        root.selectedSource = url
        root.cropX = 0; root.cropY = 0; root.cropW = 1; root.cropH = 1
        var clean = url.toLowerCase().split("?")[0]
        root.animatedSource = clean.endsWith(".gif") || clean.endsWith(".mp4") || clean.endsWith(".webm")
        if (root.animatedSource) {
            root.mediaProcessing = true
            statusText.text = "Animation wird verarbeitet …"
            statusText.color = EzTheme.textSecondary
            var info = accountController.probeCapeMedia(url)
            if (!info || !info.ok) {
                root.mediaProcessing = false
                return
            }
            root.cropImageSource = (info.thumbnailUrl && info.thumbnailUrl !== "") ? (info.thumbnailUrl + "?t=" + Date.now()) : url
            root.mediaDuration = info.duration
            root.trimStart = 0
            root.trimEnd = Math.min(5, info.duration)
            root.pendingPreview = ""
            root.prepareAnimation()
        } else {
            root.cropImageSource = url
            root.prepare()
        }
    }

    function chooseImage() {
        var url = accountController.pickCapeImage()
        root.loadSource(url)
    }

    function prepare() {
        if (!root.selectedSource) return
        if (root.animatedSource) {
            root.prepareAnimation()
        } else {
            var crop = root.cropX.toFixed(4) + "," + root.cropY.toFixed(4) + "," + root.cropW.toFixed(4) + "," + root.cropH.toFixed(4)
            root.previewProcessing = true
            root.previewRequestId = accountController.requestCapePreview(root.selectedSource, "Crop|" + crop)
        }
    }

    function schedulePreview() {
        if (root.selectedSource !== "" && !liveCropTimer.running)
            liveCropTimer.start()
    }

    function prepareAnimation() {
        if (!root.animatedSource) return
        root.mediaProcessing = true
        statusText.text = "Animation wird generiert …"
        statusText.color = EzTheme.textSecondary
        var crop = root.cropX.toFixed(4) + "," + root.cropY.toFixed(4) + "," + root.cropW.toFixed(4) + "," + root.cropH.toFixed(4)
        accountController.prepareAnimatedCape(
            root.selectedSource, root.trimStart, root.trimEnd, root.animationFps, false, crop)
    }

    Timer {
        id: liveCropTimer
        interval: 120
        repeat: false
        onTriggered: root.prepare()
    }

    function discard() {
        accountController.cancelPendingCape()
        root.selectedSource = ""
        root.pendingPreview = ""
        editorSkin3D.updateCape()
    }

    function confirm() {
        if (root.previewProcessing) return
        if (root.pendingPreview && accountController.confirmPendingCape(root.capeName)) {
            root.selectedSource = ""
            root.pendingPreview = ""
        }
    }

    Connections {
        target: accountController
        function onSkinUploadStatusChanged(message, isError) {
            statusText.text = message
            statusText.color = isError ? "#FCA5A5" : "#86EFAC"
            if (isError) root.mediaProcessing = false
        }
        function onCapeCommunityStatusChanged(message, isError) {
            if (!message) return
            statusText.text = message
            statusText.color = isError ? "#FCA5A5" : "#86EFAC"
        }
        function onCapeMediaPrepared(previewUrl, frameCount, duration) {
            root.pendingPreview = previewUrl + "?v=" + Date.now()
            root.mediaProcessing = false
        }
        function onCapeAnimationPrepared(sheetUrl, frameCount, fps, columns, frameW, frameH, pingPong) {
            root.mediaProcessing = false
            editorSkin3D.setAnimatedCape(sheetUrl, frameCount, fps, columns, frameW, frameH, pingPong)
        }
        function onCapePreviewPrepared(previewUrl, revision) {
            if (revision !== root.previewRequestId) return
            root.previewProcessing = false
            if (previewUrl) {
                root.pendingPreview = previewUrl
                editorSkin3D.updateCape()
            }
        }
    }

    Rectangle { anchors.fill: parent; color: EzTheme.bg }

    ColumnLayout {
        anchors.fill: parent
        anchors.margins: 36
        spacing: 20

        RowLayout {
            Layout.fillWidth: true
            Text {
                text: "Cape-Bild"
                font.family: EzTheme.mcFontFamily
                font.pixelSize: 24
                font.bold: true
                color: EzTheme.text
                Layout.fillWidth: true
            }
            EzButton { text: "Zurück"; onClicked: root.navigate("cape") }
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            spacing: 32
            layoutDirection: Qt.RightToLeft

            // Interactive character preview on the right side.
            Rectangle {
                Layout.preferredWidth: Math.max(340, Math.min(480, root.width * 0.38))
                Layout.fillHeight: true
                radius: 10
                color: "#171126"
                border.color: EzTheme.border
                clip: true

                Skin3DView {
                    id: editorSkin3D
                    anchors.fill: parent
                    anchors.margins: 8
                    skinSource: accountController.skinTextureUrl
                    capeSource: root.pendingPreview !== "" ? root.pendingPreview : accountController.capePreviewTextureUrl
                    animation: root.previewAnimation
                    autoRotate: false
                    interactive: true
                    initialRotateY: 180
                }

                Rectangle {
                    anchors.fill: parent
                    visible: root.mediaProcessing || root.previewProcessing
                    color: "#CC0F0B18"
                    z: 50
                    ColumnLayout {
                        anchors.centerIn: parent
                        spacing: 12
                        BusyIndicator {
                            Layout.alignment: Qt.AlignHCenter
                            running: root.mediaProcessing || root.previewProcessing
                        }
                        Text {
                            text: root.mediaProcessing ? "Animation wird vorbereitet …" : "Vorschau wird geladen …"
                            color: EzTheme.text
                            font.pixelSize: 13
                            font.bold: true
                            Layout.alignment: Qt.AlignHCenter
                        }
                    }
                }

                Rectangle {
                    anchors.left: parent.left; anchors.top: parent.top; anchors.margins: 14
                    width: previewHint.implicitWidth + 20; height: 30; radius: 15
                    color: "#B319132A"; border.color: EzTheme.border
                    Text {
                        id: previewHint; anchors.centerIn: parent
                        text: "Ziehen: 360°  •  Mausrad: Zoom"
                        color: EzTheme.textSecondary; font.pixelSize: 11
                    }
                }
            }

            ScrollView {
                id: toolsScroll
                Layout.fillWidth: true
                Layout.fillHeight: true
                Layout.maximumWidth: 520
                clip: true
                contentWidth: availableWidth
                ScrollBar.horizontal.policy: ScrollBar.AlwaysOff
                ScrollBar.vertical.policy: ScrollBar.AsNeeded

                ColumnLayout {
                    width: toolsScroll.availableWidth
                    spacing: 16

                EzButton { text: "Bild auswählen"; onClicked: root.chooseImage() }

                Text { text: "Cape-Name"; color: EzTheme.text; font.bold: true; font.pixelSize: 15 }
                TextField {
                    id: capeNameField
                    Layout.fillWidth: true
                    placeholderText: "z. B. Enderdrache"
                    text: root.capeName
                    maximumLength: 48
                    color: EzTheme.text
                    onTextChanged: root.capeName = text
                    background: Rectangle {
                        radius: 8
                        color: EzTheme.surface2
                        border.color: capeNameField.text.trim().length >= 3 ? EzTheme.border : "#F87171"
                    }
                }
                Text {
                    text: root.capeName.trim().length < 3 ? "Mindestens 3, maximal 48 Zeichen" : root.capeName.length + "/48 Zeichen"
                    color: root.capeName.trim().length < 3 ? "#FCA5A5" : EzTheme.textMuted
                    font.pixelSize: 11
                }

                Rectangle {
                    visible: root.selectedSource !== ""
                    Layout.fillWidth: true
                    Layout.preferredHeight: 48
                    radius: 9
                    color: EzTheme.surface2
                    border.color: EzTheme.border
                    RowLayout {
                        anchors.fill: parent; anchors.margins: 10; spacing: 10
                        Rectangle {
                            Layout.preferredWidth: 8; Layout.preferredHeight: 8
                            radius: 4; color: EzTheme.accent
                        }
                        Text {
                            Layout.fillWidth: true
                            text: "Motiv frei platzieren: verschieben, breiter, schmaler, größer oder kleiner ziehen."
                            color: EzTheme.textSecondary
                            wrapMode: Text.WordWrap
                            font.pixelSize: 12
                        }
                    }
                }

                RowLayout {
                    spacing: 10
                    Text { text: "3D-Animation"; color: EzTheme.text; font.bold: true; font.pixelSize: 15 }
                    EzButton { text: "Stehen"; primary: root.previewAnimation === "idle"; onClicked: root.previewAnimation = "idle" }
                    EzButton { text: "Laufen"; primary: root.previewAnimation === "walk"; onClicked: root.previewAnimation = "walk" }
                }

                ColumnLayout {
                    visible: root.animatedSource
                    Layout.fillWidth: true
                    spacing: 8

                    Text {
                        text: "Animationstrimmer · " + root.trimStart.toFixed(1) + "–" + root.trimEnd.toFixed(1) + " s"
                        color: EzTheme.text; font.bold: true; font.pixelSize: 15
                    }
                    Text { text: "Start"; color: EzTheme.textMuted; font.pixelSize: 11 }
                    Slider {
                        Layout.fillWidth: true
                        from: 0; to: Math.max(0.1, root.mediaDuration - 0.1)
                        value: root.trimStart
                        onMoved: {
                            root.trimStart = value
                            if (root.trimEnd < value + 0.1)
                                root.trimEnd = Math.min(root.mediaDuration, value + 0.1)
                        }
                        onPressedChanged: if (!pressed) root.prepareAnimation()
                    }
                    Text { text: "Ende · maximal 10 Sekunden"; color: EzTheme.textMuted; font.pixelSize: 11 }
                    Slider {
                        Layout.fillWidth: true
                        from: 0.1; to: Math.max(0.1, root.mediaDuration)
                        value: root.trimEnd
                        onMoved: root.trimEnd = Math.max(root.trimStart + 0.1, Math.min(value, root.trimStart + 10))
                        onPressedChanged: if (!pressed) root.prepareAnimation()
                    }
                    RowLayout {
                        Layout.fillWidth: true
                        Text { text: "FPS: " + root.animationFps; color: EzTheme.textSecondary; Layout.fillWidth: true }
                        Slider {
                            from: 1; to: 20; stepSize: 1; value: root.animationFps
                            onMoved: root.animationFps = Math.round(value)
                            onPressedChanged: if (!pressed) root.prepareAnimation()
                            Layout.preferredWidth: 160
                        }
                    }
                    EzButton {
                        text: root.mediaProcessing ? "Frames werden erstellt …" : "Loop aktualisieren"
                        enabled: !root.mediaProcessing
                        onClicked: root.prepareAnimation()
                    }
                }

                Text { text: "Zuschneiden"; color: EzTheme.text; font.bold: true; font.pixelSize: 15; visible: root.selectedSource !== "" }

                // Interactive crop editor. Drag inside the frame to move it,
                // drag the corner handle to resize. The cape is made from the
                // framed region only.
                Rectangle {
                    id: cropStage
                    visible: root.selectedSource !== ""
                    Layout.fillWidth: true
                    Layout.preferredHeight: 360
                    radius: 10
                    color: "#171126"
                    border.color: EzTheme.border
                    clip: true
                    readonly property real paintX: cropImage.x + (cropImage.width - cropImage.paintedWidth) / 2
                    readonly property real paintY: cropImage.y + (cropImage.height - cropImage.paintedHeight) / 2
                    readonly property real paintW: cropImage.paintedWidth
                    readonly property real paintH: cropImage.paintedHeight

                    function resetSelection() {
                        if (paintW <= 0 || paintH <= 0) return
                        var imageAspect = paintW / paintH
                        var capeAspect = 10 / 16
                        if (imageAspect >= capeAspect) {
                            root.cropH = 1
                            root.cropW = capeAspect / imageAspect
                            root.cropX = (1 - root.cropW) / 2
                            root.cropY = 0
                        } else {
                            root.cropW = 1
                            root.cropH = imageAspect / capeAspect
                            root.cropX = 0
                            root.cropY = (1 - root.cropH) / 2
                        }
                        root.schedulePreview()
                    }

                    Image {
                        id: cropImage
                        anchors.centerIn: parent
                        width: parent.width
                        height: parent.height
                        fillMode: Image.PreserveAspectFit
                        asynchronous: true
                        cache: false
                        source: root.cropImageSource
                        onStatusChanged: if (status === Image.Ready && cropStage.paintW > 0 && cropStage.paintH > 0) cropStage.resetSelection()
                        onPaintedWidthChanged: if (status === Image.Ready && cropStage.paintW > 0 && cropStage.paintH > 0 && root.cropW === 1 && root.cropH === 1) cropStage.resetSelection()
                    }

                    // Cape-proportioned crop frame (10x16 like the real cape).
                    // Drag it to move; drag the handle to resize (aspect kept).
                    Rectangle {
                        id: cropFrame
                        visible: cropStage.paintW > 0 && cropStage.paintH > 0
                        x: cropStage.paintX + root.cropX * cropStage.paintW
                        y: cropStage.paintY + root.cropY * cropStage.paintH
                        width: root.cropW * cropStage.paintW
                        height: root.cropH * cropStage.paintH
                        color: "transparent"
                        border.color: EzTheme.accent
                        border.width: 2

                        MouseArea {
                            id: cropMove
                            anchors.fill: parent
                            enabled: false
                            cursorShape: Qt.SizeAllCursor
                            property real pressX: 0
                            property real pressY: 0
                            property real startX: 0
                            property real startY: 0
                            onPressed: function(mouse) {
                                var point = mapToItem(cropStage, mouse.x, mouse.y)
                                pressX = point.x; pressY = point.y
                                startX = root.cropX; startY = root.cropY
                            }
                            onPositionChanged: function(mouse) {
                                if (!pressed || cropStage.paintW <= 0 || cropStage.paintH <= 0) return
                                var point = mapToItem(cropStage, mouse.x, mouse.y)
                                root.cropX = Math.max(0, Math.min(1 - root.cropW,
                                    startX + (point.x - pressX) / cropStage.paintW))
                                root.cropY = Math.max(0, Math.min(1 - root.cropH,
                                    startY + (point.y - pressY) / cropStage.paintH))
                                root.schedulePreview()
                            }
                            onReleased: { liveCropTimer.stop(); root.prepare() }
                            onCanceled: { liveCropTimer.stop(); root.prepare() }
                        }

                        Rectangle {
                            anchors.fill: parent
                            color: "#16000000"
                        }

                        Text {
                            anchors.centerIn: parent
                            text: "Cape-Motiv"
                            color: "#E8FFFFFF"
                            font.pixelSize: 12
                            font.bold: true
                        }

                        function applyResize(newWidth, newHeight) {
                            if (cropStage.paintW <= 0 || cropStage.paintH <= 0) return
                            var widthPx = Math.max(40, Math.min(cropStage.paintW, newWidth))
                            var heightPx = Math.max(40, Math.min(cropStage.paintH, newHeight))
                            root.cropH = heightPx / cropStage.paintH
                            root.cropW = widthPx / cropStage.paintW
                            // Growing at the right/bottom edge keeps going up to
                            // 100%; move the selection inward instead of changing
                            // the control range or stopping early.
                            root.cropX = Math.max(0, Math.min(root.cropX, 1 - root.cropW))
                            root.cropY = Math.max(0, Math.min(root.cropY, 1 - root.cropH))
                            root.schedulePreview()
                        }

                        Rectangle {
                            id: resizeHandle
                            width: 16; height: 16
                            radius: 8
                            color: EzTheme.accent
                            anchors.right: parent.right
                            anchors.bottom: parent.bottom
                            anchors.margins: -8

                            MouseArea {
                                anchors.fill: parent
                                enabled: false
                                cursorShape: Qt.SizeFDiagCursor
                                property real pressX: 0
                                property real pressY: 0
                                property real startWidth: 0
                                property real startHeight: 0
                                onPressed: function(mouse) {
                                    var point = mapToItem(cropStage, mouse.x, mouse.y)
                                    pressX = point.x; pressY = point.y
                                    startWidth = cropFrame.width
                                    startHeight = cropFrame.height
                                }
                                onPositionChanged: function(mouse) {
                                    if (!pressed) return
                                    var point = mapToItem(cropStage, mouse.x, mouse.y)
                                    cropFrame.applyResize(
                                        startWidth + point.x - pressX,
                                        startHeight + point.y - pressY)
                                }
                                onReleased: { liveCropTimer.stop(); root.prepare() }
                                onCanceled: { liveCropTimer.stop(); root.prepare() }
                            }
                        }

                        Canvas {
                            id: gridCanvas
                            anchors.fill: parent
                            visible: root.showPixelGrid
                            opacity: 0.55
                            onWidthChanged: requestPaint()
                            onHeightChanged: requestPaint()
                            onVisibleChanged: if (visible) requestPaint()
                            onPaint: {
                                var ctx = getContext("2d")
                                ctx.clearRect(0, 0, width, height)
                                if (width <= 0 || height <= 0) return
                                ctx.strokeStyle = "#A78BFA"
                                ctx.lineWidth = 1
                                for (var x = 1; x < 10; ++x) {
                                    ctx.beginPath(); ctx.moveTo(x * width / 10, 0); ctx.lineTo(x * width / 10, height); ctx.stroke()
                                }
                                for (var y = 1; y < 16; ++y) {
                                    ctx.beginPath(); ctx.moveTo(0, y * height / 16); ctx.lineTo(width, y * height / 16); ctx.stroke()
                                }
                            }
                        }
                    }

                    // One stage-sized pointer surface keeps drag coordinates stable
                    // even while the crop rectangle itself is moving.
                    MouseArea {
                        id: cropPointer
                        anchors.fill: parent
                        z: 20
                        hoverEnabled: true
                        cursorShape: !hoverInside ? Qt.ArrowCursor : (resizeMode ? Qt.SizeFDiagCursor : Qt.SizeAllCursor)
                        property bool hoverInside: false
                        property bool resizeMode: false
                        property real pressX: 0
                        property real pressY: 0
                        property real startCropX: 0
                        property real startCropY: 0
                        property real startWidth: 0
                        property real startHeight: 0

                        onPressed: function(mouse) {
                            var inside = mouse.x >= cropFrame.x && mouse.x <= cropFrame.x + cropFrame.width
                                      && mouse.y >= cropFrame.y && mouse.y <= cropFrame.y + cropFrame.height
                            if (!inside) {
                                mouse.accepted = false
                                return
                            }
                            hoverInside = true
                            resizeMode = mouse.x >= cropFrame.x + cropFrame.width - 24
                                      && mouse.y >= cropFrame.y + cropFrame.height - 24
                            pressX = mouse.x; pressY = mouse.y
                            startCropX = root.cropX; startCropY = root.cropY
                            startWidth = cropFrame.width; startHeight = cropFrame.height
                        }
                        onPositionChanged: function(mouse) {
                            if (!pressed) {
                                hoverInside = mouse.x >= cropFrame.x && mouse.x <= cropFrame.x + cropFrame.width
                                           && mouse.y >= cropFrame.y && mouse.y <= cropFrame.y + cropFrame.height
                                resizeMode = hoverInside && mouse.x >= cropFrame.x + cropFrame.width - 24
                                           && mouse.y >= cropFrame.y + cropFrame.height - 24
                                return
                            }
                            if (cropStage.paintW <= 0 || cropStage.paintH <= 0) return
                            if (resizeMode) {
                                cropFrame.applyResize(startWidth + mouse.x - pressX,
                                                      startHeight + mouse.y - pressY)
                            } else {
                                root.cropX = Math.max(0, Math.min(1 - root.cropW,
                                    startCropX + (mouse.x - pressX) / cropStage.paintW))
                                root.cropY = Math.max(0, Math.min(1 - root.cropH,
                                    startCropY + (mouse.y - pressY) / cropStage.paintH))
                                root.schedulePreview()
                            }
                        }
                        onReleased: { liveCropTimer.stop(); root.prepare() }
                        onCanceled: { liveCropTimer.stop(); root.prepare() }
                    }

                    Text {
                        visible: cropImage.status === Image.Loading
                        anchors.centerIn: parent
                        text: "Bild lädt…"
                        color: EzTheme.textSecondary
                    }
                }

                RowLayout {
                    visible: root.selectedSource !== ""
                    Layout.fillWidth: true
                    spacing: 10
                    EzButton { text: "Motiv zurücksetzen"; onClicked: cropStage.resetSelection() }
                    EzButton { text: root.showPixelGrid ? "Grid aus" : "Grid an"; onClicked: root.showPixelGrid = !root.showPixelGrid }
                }

                GridLayout {
                    visible: root.selectedSource !== ""
                    Layout.fillWidth: true
                    columns: 2
                    columnSpacing: 12
                    rowSpacing: 6

                    Text { text: "Breite"; color: EzTheme.textSecondary; font.pixelSize: 12 }
                    Slider {
                        Layout.fillWidth: true
                        from: 0.05
                        to: 1.0
                        value: root.cropW
                        onMoved: {
                            root.cropW = value
                            root.cropX = Math.max(0, Math.min(root.cropX, 1 - root.cropW))
                            root.schedulePreview()
                        }
                        onPressedChanged: if (!pressed) { liveCropTimer.stop(); root.prepare() }
                    }
                    Text { text: "Höhe"; color: EzTheme.textSecondary; font.pixelSize: 12 }
                    Slider {
                        Layout.fillWidth: true
                        from: 0.05
                        to: 1.0
                        value: root.cropH
                        onMoved: {
                            root.cropH = value
                            root.cropY = Math.max(0, Math.min(root.cropY, 1 - root.cropH))
                            root.schedulePreview()
                        }
                        onPressedChanged: if (!pressed) { liveCropTimer.stop(); root.prepare() }
                    }
                }

                Text {
                    text: "Das Bild wird erst nach „Bestätigen“ aktiviert und hochgeladen."
                    color: EzTheme.textSecondary
                    wrapMode: Text.WordWrap
                    Layout.fillWidth: true
                    font.pixelSize: 13
                }

                RowLayout {
                    spacing: 12
                    EzButton {
                        text: root.previewProcessing ? "Vorschau wird aktualisiert …" : "Bestätigen & hochladen"
                        enabled: root.pendingPreview !== "" && root.capeName.trim().length >= 3 && !root.previewProcessing
                        onClicked: root.confirm()
                    }
                    EzButton {
                        text: "Verwerfen"
                        enabled: root.pendingPreview !== ""
                        onClicked: root.discard()
                    }
                }

                Text {
                    id: statusText
                    text: ""
                    color: EzTheme.textSecondary
                    wrapMode: Text.WordWrap
                    Layout.fillWidth: true
                    font.pixelSize: 12
                }
                Item { Layout.fillWidth: true; Layout.preferredHeight: 12 }
                }
            }
        }
    }

    DropArea {
        id: mediaDropArea
        anchors.fill: parent
        z: 100
        onEntered: function(drag) { drag.accepted = drag.hasUrls }
        onDropped: function(drop) {
            if (drop.hasUrls && drop.urls.length > 0) root.loadSource(drop.urls[0].toString())
        }

        Rectangle {
            anchors.fill: parent
            visible: mediaDropArea.containsDrag
            color: "#D9110D20"
            border.color: EzTheme.accent
            border.width: 2
            Text {
                anchors.centerIn: parent
                text: "Cape-Datei hier ablegen"
                color: EzTheme.text
                font.pixelSize: 20
                font.bold: true
            }
        }
    }
}
