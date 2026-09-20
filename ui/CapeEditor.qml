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
    property string cropImageSource: ""
    property int previewRequestId: 0
    property bool previewProcessing: false
    property bool isUploading: false
    property bool isFullCapeDetected: false
    property bool fullCapeMode: false
    property string activeFace: "back"

    // 6 Faces: Außen (back), Innen (front), Links (left), Rechts (right), Oben (top), Unten (bottom)
    property var faceSources: ({"back":"", "front":"", "left":"", "right":"", "top":"", "bottom":""})
    property var faceCrops: ({"back":[0,0,1,1], "front":[0,0,1,1], "left":[0,0,1,1], "right":[0,0,1,1], "top":[0,0,1,1], "bottom":[0,0,1,1]})
    property var facePaints: ({"back":"", "front":"", "left":"", "right":"", "top":"", "bottom":""})

    // Pencil properties
    property color brushColor: "#000000"
    property real brushSize: 20

    readonly property var account: (typeof accountController !== "undefined" && accountController) ? accountController : null

    function hasFaceImage(face) {
        var s = root.faceSources[face]
        return typeof s === "string" && s !== ""
    }

    function isFaceDone(face) {
        return root.hasFaceImage(face) || (typeof root.facePaints[face] === "string" && root.facePaints[face] !== "")
    }

    function allFacesConfigured() {
        if (root.fullCapeMode) return true
        var names = ["back", "front", "left", "right", "top", "bottom"]
        for (var i = 0; i < names.length; ++i) {
            if (!root.isFaceDone(names[i])) return false
        }
        return true
    }

    function missingFacesCount() {
        if (root.fullCapeMode) return 0
        var names = ["back", "front", "left", "right", "top", "bottom"]
        var count = 0
        for (var i = 0; i < names.length; ++i) {
            if (!root.isFaceDone(names[i])) count++
        }
        return count
    }

    function hasAnyCapeContent() {
        if (root.selectedSource !== "") return true
        var names = ["back", "front", "left", "right", "top", "bottom"]
        for (var i = 0; i < names.length; ++i) {
            if (root.faceSources[names[i]] || root.facePaints[names[i]]) return true
        }
        return false
    }

    function captureFace() {
        var crops = Object.assign({}, root.faceCrops)
        crops[root.activeFace] = [root.cropX, root.cropY, root.cropW, root.cropH]
        root.faceCrops = crops
    }

    function selectFace(face) {
        if (face === root.activeFace) return
        if (root.hasFaceImage(root.activeFace)) {
            root.captureFace()
        }
        root.activeFace = face
        var crop = root.faceCrops[face] || [0,0,1,1]
        root.cropX = crop[0]; root.cropY = crop[1]; root.cropW = crop[2]; root.cropH = crop[3]
        root.cropImageSource = root.faceSources[face] || (face === "back" ? root.selectedSource : "")
    }

    function getActiveCanvas() {
        switch(root.activeFace) {
            case "back": return canvasBack;
            case "front": return canvasFront;
            case "left": return canvasLeft;
            case "right": return canvasRight;
            case "top": return canvasTop;
            case "bottom": return canvasBottom;
            default: return canvasBack;
        }
    }

    function syncFacePaint(face, canvas) {
        if (!canvas || !canvas.available) return
        var dataUrl = canvas.toDataURL("image/png")
        var paints = Object.assign({}, root.facePaints)
        paints[face] = dataUrl
        root.facePaints = paints
        liveCropTimer.stop()
        root.prepare()
    }

    function fillActiveFace(color) {
        var cv = root.getActiveCanvas()
        if (!cv || !cv.available) return
        var ctx = cv.getContext("2d")
        ctx.fillStyle = color
        ctx.fillRect(0, 0, cv.width, cv.height)
        cv.requestPaint()
        root.syncFacePaint(root.activeFace, cv)
    }

    function clearActiveFaceDrawing() {
        var cv = root.getActiveCanvas()
        if (!cv || !cv.available) return
        var ctx = cv.getContext("2d")
        ctx.clearRect(0, 0, cv.width, cv.height)
        cv.requestPaint()
        var paints = Object.assign({}, root.facePaints)
        paints[root.activeFace] = ""
        root.facePaints = paints
        liveCropTimer.stop()
        root.prepare()
    }

    function applyActiveFaceToAll() {
        var names = ["back", "front", "left", "right", "top", "bottom"]
        if (root.hasFaceImage(root.activeFace)) {
            var src = root.faceSources[root.activeFace]
            var crop = root.faceCrops[root.activeFace] || [0, 0, 1, 1]
            var sources = Object.assign({}, root.faceSources)
            var crops = Object.assign({}, root.faceCrops)
            var pImg = Object.assign({}, root.facePaints)
            for (var i = 0; i < names.length; ++i) {
                var k = names[i]
                sources[k] = src
                crops[k] = [crop[0], crop[1], crop[2], crop[3]]
                pImg[k] = ""
            }
            root.faceSources = sources
            root.faceCrops = crops
            root.facePaints = pImg
            liveCropTimer.stop()
            root.prepare()
        } else {
            var activeCv = root.getActiveCanvas()
            if (!activeCv || !activeCv.available) return
            var dataUrl = activeCv.toDataURL("image/png")
            var p = Object.assign({}, root.facePaints)
            var s = Object.assign({}, root.faceSources)
            for (var j = 0; j < names.length; ++j) {
                var fn = names[j]
                p[fn] = dataUrl
                s[fn] = ""
            }
            root.facePaints = p
            root.faceSources = s
            liveCropTimer.stop()
            root.prepare()
        }
    }

    function clearCanvas(cv) {
        if (cv && cv.available) {
            var ctx = cv.getContext("2d")
            ctx.clearRect(0, 0, cv.width, cv.height)
            cv.requestPaint()
        }
    }

    function facesJson() {
        if (root.hasFaceImage(root.activeFace)) {
            root.captureFace()
        }
        var faces = {}
        var names = ["back", "front", "left", "right", "top", "bottom"]
        for (var i = 0; i < names.length; ++i) {
            var key = names[i]
            var src = root.faceSources[key] || ""
            var crop = [0, 0, 1, 1]
            if (!src && root.facePaints[key]) {
                src = root.facePaints[key]
                crop = [0, 0, 1, 1] // Never crop hand-drawn canvas!
            } else if (src) {
                crop = root.faceCrops[key] || [0, 0, 1, 1]
            }
            faces[key] = {"source": src, "crop": crop}
        }
        return JSON.stringify(faces)
    }

    function loadSource(url) {
        if (!url) return
        root.selectedSource = url
        root.activeFace = "back"
        var sources = Object.assign({}, root.faceSources)
        sources.back = url
        root.faceSources = sources
        var crops = Object.assign({}, root.faceCrops)
        crops.back = [0, 0, 1, 1]
        root.faceCrops = crops
        root.cropX = 0; root.cropY = 0; root.cropW = 1; root.cropH = 1
        root.fullCapeMode = false
        root.isFullCapeDetected = false
        var clean = url.toLowerCase().split("?")[0]
        root.animatedSource = clean.endsWith(".gif") || clean.endsWith(".mp4") || clean.endsWith(".webm")
        editorSkin3D.capeAnimationInfo = ({})
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
            root.pendingPreview = ""
            root.prepareAnimation()
        } else {
            root.cropImageSource = url
            root.prepare()
        }
    }

    function chooseImage() {
        var url = accountController.pickCapeImage()
        root.setFaceImage(url)
    }

    function setFaceImage(url) {
        if (!url) return
        if (root.activeFace === "back") {
            root.loadSource(url)
        } else {
            var sources = Object.assign({}, root.faceSources)
            sources[root.activeFace] = url
            root.faceSources = sources
            var crops = Object.assign({}, root.faceCrops)
            crops[root.activeFace] = [0, 0, 1, 1]
            root.faceCrops = crops
            root.cropX = 0; root.cropY = 0; root.cropW = 1; root.cropH = 1
            root.cropImageSource = url
            if (root.animatedSource) root.prepareAnimation()
            else root.prepare()
        }
    }

    function clearFaceImage(face) {
        var sources = Object.assign({}, root.faceSources)
        sources[face] = ""
        root.faceSources = sources
        if (face === "back") {
            root.selectedSource = ""
            root.animatedSource = false
        }
        if (root.cropImageSource === (root.faceSources[face] || "")) {
            root.cropImageSource = ""
        }
        root.cropX = 0; root.cropY = 0; root.cropW = 1; root.cropH = 1
        liveCropTimer.stop()
        if (root.animatedSource) root.prepareAnimation()
        else root.prepare()
    }

    function prepare() {
        if (!root.hasAnyCapeContent()) {
            accountController.cancelPendingCape()
            root.previewProcessing = false
            root.pendingPreview = ""
            editorSkin3D.capeAnimationInfo = ({})
            editorSkin3D.updateCape()
            return
        }
        if (root.animatedSource) {
            root.prepareAnimation()
        } else if (root.fullCapeMode) {
            root.previewProcessing = true
            root.previewRequestId = accountController.requestCapePreview(root.selectedSource, "FullCape|")
        } else {
            root.previewProcessing = true
            root.previewRequestId = accountController.requestCapeFacesPreview(root.facesJson())
        }
    }

    function schedulePreview() {
        if (!liveCropTimer.running)
            liveCropTimer.start()
    }

    function prepareAnimation() {
        if (!root.animatedSource) return
        root.mediaProcessing = true
        root.pendingPreview = ""
        statusText.text = "Animation wird generiert …"
        statusText.color = EzTheme.textSecondary
        var backCrop = root.activeFace === "back" ? [root.cropX, root.cropY, root.cropW, root.cropH] : (root.faceCrops.back || [0,0,1,1])
        var crop = backCrop.map(function(v) { return Number(v).toFixed(4) }).join(",")
        var duration = Math.min(10.0, root.mediaDuration > 0 ? root.mediaDuration : 5.0)
        accountController.prepareAnimatedCape(
            root.selectedSource, 0, duration, 12, false, crop, root.facesJson())
        animationPreviewRefreshTimer.restart()
    }

    function refreshAnimationPreview() {
        if (!root.animatedSource) return
        editorSkin3D.updateCape()
        editorSkin3D.setAnim(root.previewAnimation)
    }

    Timer {
        id: liveCropTimer
        interval: 200
        repeat: false
        onTriggered: root.prepare()
    }

    // Animated sheets can become available a fraction after the WebEngine's
    // first request. Repeat the same action as the visible refresh control once.
    Timer {
        id: animationPreviewRefreshTimer
        interval: 2000
        repeat: false
        onTriggered: root.refreshAnimationPreview()
    }

    function discard() {
        accountController.cancelPendingCape()
        root.selectedSource = ""
        root.pendingPreview = ""
        root.faceSources = {"back":"", "front":"", "left":"", "right":"", "top":"", "bottom":""}
        root.facePaints = {"back":"", "front":"", "left":"", "right":"", "top":"", "bottom":""}
        root.faceCrops = {"back":[0,0,1,1], "front":[0,0,1,1], "left":[0,0,1,1], "right":[0,0,1,1], "top":[0,0,1,1], "bottom":[0,0,1,1]}
        root.clearCanvas(canvasBack)
        root.clearCanvas(canvasFront)
        root.clearCanvas(canvasLeft)
        root.clearCanvas(canvasRight)
        root.clearCanvas(canvasTop)
        root.clearCanvas(canvasBottom)
        editorSkin3D.capeAnimationInfo = ({})
        editorSkin3D.updateCape()
    }

    function confirm() {
        if (root.previewProcessing || root.mediaProcessing || root.isUploading) return
        if (!root.allFacesConfigured()) return
        if (root.capeName.trim().length < 3) return
        root.isUploading = true
        if (!root.pendingPreview || !accountController.confirmPendingCape(root.capeName)) {
            root.isUploading = false
        }
    }

    Connections {
        target: accountController
        function onCapeUploadSuccess() {
            root.isUploading = false
            root.selectedSource = ""
            root.pendingPreview = ""
            root.navigate("cape")
        }
        function onSkinUploadStatusChanged(message, isError) {
            statusText.text = message
            statusText.color = isError ? "#FCA5A5" : "#86EFAC"
            if (isError) {
                root.mediaProcessing = false
                root.isUploading = false
            }
        }
        function onCapeCommunityStatusChanged(message, isError) {
            if (!message) return
            statusText.text = message
            statusText.color = isError ? "#FCA5A5" : "#86EFAC"
            if (isError) {
                root.isUploading = false
            }
        }
        function onCapeMediaPrepared(previewUrl, frameCount, duration) {
            root.pendingPreview = previewUrl
            if (!root.animatedSource) {
                root.mediaProcessing = false
            }
        }
        function onCapeAnimationPrepared(sheetUrl, frameCount, fps, columns, frameW, frameH, pingPong) {
            root.mediaProcessing = false
            root.pendingPreview = sheetUrl
            editorSkin3D.setAnimatedCape(sheetUrl, frameCount, fps, columns, frameW, frameH, pingPong)
            animationPreviewRefreshTimer.restart()
        }
        function onCapePreviewPrepared(previewUrl, revision) {
            if (revision !== root.previewRequestId) return
            root.previewProcessing = false
            root.pendingPreview = previewUrl || ""
            editorSkin3D.updateCape()
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
                text: EzI18n.text("Cape-Studio")
                font.family: EzTheme.mcFontFamily
                font.pixelSize: 24
                font.bold: true
                color: EzTheme.text
                Layout.fillWidth: true
            }
            EzButton { text: EzI18n.text("Zurück"); onClicked: root.navigate("cape") }
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.fillHeight: true
            spacing: 24

            ScrollView {
                id: toolsScroll
                Layout.fillWidth: true
                Layout.fillHeight: true
                clip: true
                rightPadding: 16
                ScrollBar.horizontal.policy: ScrollBar.AlwaysOff
                ScrollBar.vertical: ScrollBar {
                    id: toolsScrollBar
                    parent: toolsScroll
                    x: toolsScroll.mirrored ? 2 : (toolsScroll.width - width - 2)
                    y: toolsScroll.topPadding
                    height: toolsScroll.availableHeight
                    policy: ScrollBar.AsNeeded
                    width: 8
                    contentItem: Rectangle {
                        implicitWidth: 8
                        radius: 4
                        color: toolsScrollBar.pressed ? EzTheme.accent : (toolsScrollBar.hovered ? EzTheme.accentLight : "#596273")
                    }
                }

                ColumnLayout {
                    width: toolsScroll.availableWidth
                    spacing: 16

                    // Face selection section
                    RowLayout {
                        Layout.fillWidth: true
                        Text {
                            text: EzI18n.text("Cape-Seiten (6 Flächen)")
                            color: EzTheme.text
                            font.bold: true
                            font.pixelSize: 15
                            Layout.fillWidth: true
                        }
                        Rectangle {
                            radius: 6
                            color: root.hasFaceImage(root.activeFace) ? "#1E1B4B" : "#064E3B"
                            border.color: root.hasFaceImage(root.activeFace) ? "#6366F1" : "#10B981"
                            implicitWidth: modeBadgeText.implicitWidth + 14
                            implicitHeight: 22
                            Text {
                                id: modeBadgeText
                                anchors.centerIn: parent
                                text: root.hasFaceImage(root.activeFace) ? EzI18n.text("Bild aktiv") : EzI18n.text("Pencil aktiv")
                                font.pixelSize: 11
                                font.bold: true
                                color: root.hasFaceImage(root.activeFace) ? "#A5B4FC" : "#6EE7B7"
                            }
                        }
                    }

                    // 6 Face buttons
                    Flow {
                        width: parent.width
                        Layout.fillWidth: true
                        spacing: 6

                        EzButton {
                            text: EzI18n.text("Hauptseite (Außen)") + (root.hasFaceImage("back") ? " 🖼" : (root.facePaints.back ? " 🎨" : " ⚪"))
                            primary: root.activeFace === "back"
                            minWidth: 84
                            onClicked: root.selectFace("back")
                        }
                        EzButton {
                            text: EzI18n.text("Innenseite (Körper)") + (root.hasFaceImage("front") ? " 🖼" : (root.facePaints.front ? " 🎨" : " ⚪"))
                            primary: root.activeFace === "front"
                            minWidth: 84
                            onClicked: root.selectFace("front")
                        }
                        EzButton {
                            text: EzI18n.text("Links") + (root.hasFaceImage("left") ? " 🖼" : (root.facePaints.left ? " 🎨" : " ⚪"))
                            primary: root.activeFace === "left"
                            minWidth: 60
                            onClicked: root.selectFace("left")
                        }
                        EzButton {
                            text: EzI18n.text("Rechts") + (root.hasFaceImage("right") ? " 🖼" : (root.facePaints.right ? " 🎨" : " ⚪"))
                            primary: root.activeFace === "right"
                            minWidth: 60
                            onClicked: root.selectFace("right")
                        }
                        EzButton {
                            text: EzI18n.text("Oben") + (root.hasFaceImage("top") ? " 🖼" : (root.facePaints.top ? " 🎨" : " ⚪"))
                            primary: root.activeFace === "top"
                            minWidth: 60
                            onClicked: root.selectFace("top")
                        }
                        EzButton {
                            text: EzI18n.text("Unten") + (root.hasFaceImage("bottom") ? " 🖼" : (root.facePaints.bottom ? " 🎨" : " ⚪"))
                            primary: root.activeFace === "bottom"
                            minWidth: 60
                            onClicked: root.selectFace("bottom")
                        }
                    }

                    // SECTION A: IMAGE MODE (When face has image/GIF loaded)
                    ColumnLayout {
                        visible: root.hasFaceImage(root.activeFace) && !root.fullCapeMode
                        Layout.fillWidth: true
                        spacing: 12

                        Rectangle {
                            Layout.fillWidth: true
                            implicitHeight: 40
                            radius: 8
                            color: "#1E1B4B"
                            border.color: "#6366F1"
                            RowLayout {
                                anchors.fill: parent; anchors.margins: 10; spacing: 8
                                Text {
                                    text: EzI18n.text("ℹ Bild/GIF auf dieser Seite aktiv. Pencil ist hier pausiert.")
                                    color: "#C7D2FE"; font.pixelSize: 12; Layout.fillWidth: true
                                }
                            }
                        }

                        Flow {
                            width: parent.width
                            Layout.fillWidth: true
                            spacing: 8
                            EzButton {
                                text: EzI18n.text("Anderes Bild wählen")
                                onClicked: root.chooseImage()
                            }
                            EzButton {
                                text: EzI18n.text("Bild entfernen (Pencil aktivieren)")
                                onClicked: root.clearFaceImage(root.activeFace)
                            }
                            EzButton {
                                text: root.showPixelGrid ? EzI18n.text("Grid aus") : EzI18n.text("Grid an")
                                onClicked: root.showPixelGrid = !root.showPixelGrid
                            }
                        }

                        // Interactive crop editor for uploaded image
                        Rectangle {
                            id: cropStage
                            Layout.fillWidth: true
                            Layout.preferredHeight: 340
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
                                var capeAspect = (root.activeFace === "left" || root.activeFace === "right") ? (1 / 16) :
                                                 ((root.activeFace === "top" || root.activeFace === "bottom") ? (10 / 1) : (10 / 16))
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
                                onStatusChanged: {
                                    if (status === Image.Ready) {
                                        var aspect = (sourceSize.height > 0) ? (sourceSize.width / sourceSize.height) : 0
                                        if (Math.abs(aspect - 2.0) < 0.1 && !root.animatedSource && root.activeFace === "back" && !root.isFullCapeDetected) {
                                            root.isFullCapeDetected = true
                                            root.fullCapeMode = true
                                            root.prepare()
                                        } else {
                                            root.isFullCapeDetected = false
                                            if (cropStage.paintW > 0 && cropStage.paintH > 0) cropStage.resetSelection()
                                        }
                                    }
                                }
                                onPaintedWidthChanged: if (status === Image.Ready && cropStage.paintW > 0 && cropStage.paintH > 0 && root.cropW === 1 && root.cropH === 1 && !root.fullCapeMode) cropStage.resetSelection()
                            }

                            Rectangle {
                                id: cropFrame
                                visible: cropStage.paintW > 0 && cropStage.paintH > 0
                                x: cropStage.paintX + root.cropX * cropStage.paintW
                                y: cropStage.paintY + root.cropY * cropStage.paintH
                                width: root.cropW * cropStage.paintW
                                height: root.cropH * cropStage.paintH
                                color: "transparent"
                                border.color: (cropMouseArea.currentHit !== "none") ? EzTheme.accentLight : EzTheme.accent
                                border.width: (cropMouseArea.currentHit !== "none") ? 2.5 : 2

                                Rectangle { anchors.fill: parent; color: "#16000000" }

                                Text {
                                    anchors.centerIn: parent
                                    text: EzI18n.text("Ausschnitt")
                                    color: "#E8FFFFFF"
                                    font.pixelSize: 12
                                    font.bold: true
                                }

                                Canvas {
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
                                        var cols = (root.activeFace === "left" || root.activeFace === "right") ? 2 : 10
                                        var rows = (root.activeFace === "top" || root.activeFace === "bottom") ? 2 : 16
                                        for (var x = 1; x < cols; ++x) {
                                            ctx.beginPath(); ctx.moveTo(x * width / cols, 0); ctx.lineTo(x * width / cols, height); ctx.stroke()
                                        }
                                        for (var y = 1; y < rows; ++y) {
                                            ctx.beginPath(); ctx.moveTo(0, y * height / rows); ctx.lineTo(width, y * height / rows); ctx.stroke()
                                        }
                                    }
                                }

                                // Visual corner handles for easy grabbing
                                Rectangle { width: 8; height: 8; radius: 2; color: "#FFFFFF"; border.color: EzTheme.accent; border.width: 1.5; x: -4; y: -4 }
                                Rectangle { width: 8; height: 8; radius: 2; color: "#FFFFFF"; border.color: EzTheme.accent; border.width: 1.5; x: parent.width - 4; y: -4 }
                                Rectangle { width: 8; height: 8; radius: 2; color: "#FFFFFF"; border.color: EzTheme.accent; border.width: 1.5; x: -4; y: parent.height - 4 }
                                Rectangle { width: 8; height: 8; radius: 2; color: "#FFFFFF"; border.color: EzTheme.accent; border.width: 1.5; x: parent.width - 4; y: parent.height - 4 }
                            }

                            MouseArea {
                                id: cropMouseArea
                                anchors.fill: parent
                                z: 20
                                hoverEnabled: true
                                preventStealing: true

                                property string currentHit: "none"
                                property string dragMode: "none"
                                property real lastX: 0
                                property real lastY: 0

                                function getHitTest(mx, my) {
                                    var fx = cropFrame.x
                                    var fy = cropFrame.y
                                    var fw = cropFrame.width
                                    var fh = cropFrame.height
                                    var edge = 12

                                    if (mx < fx - edge || mx > fx + fw + edge || my < fy - edge || my > fy + fh + edge) {
                                        return "none"
                                    }

                                    var isL = mx <= fx + edge
                                    var isR = mx >= fx + fw - edge
                                    var isT = my <= fy + edge
                                    var isB = my >= fy + fh - edge

                                    if (isT && isL) return "tl"
                                    if (isT && isR) return "tr"
                                    if (isB && isL) return "bl"
                                    if (isB && isR) return "br"
                                    if (isL) return "l"
                                    if (isR) return "r"
                                    if (isT) return "t"
                                    if (isB) return "b"

                                    if (mx >= fx && mx <= fx + fw && my >= fy && my <= fy + fh) {
                                        return "move"
                                    }
                                    return "none"
                                }

                                function updateCursor(hit) {
                                    switch(hit) {
                                        case "tl":
                                        case "br":
                                            cursorShape = Qt.SizeFDiagCursor
                                            break
                                        case "tr":
                                        case "bl":
                                            cursorShape = Qt.SizeBDiagCursor
                                            break
                                        case "l":
                                        case "r":
                                            cursorShape = Qt.SizeHorCursor
                                            break
                                        case "t":
                                        case "b":
                                            cursorShape = Qt.SizeVerCursor
                                            break
                                        case "move":
                                            cursorShape = Qt.SizeAllCursor
                                            break
                                        default:
                                            cursorShape = Qt.ArrowCursor
                                            break
                                    }
                                }

                                onPressed: function(mouse) {
                                    var hit = getHitTest(mouse.x, mouse.y)
                                    if (hit === "none") {
                                        mouse.accepted = false
                                        return
                                    }
                                    dragMode = hit
                                    currentHit = hit
                                    lastX = mouse.x
                                    lastY = mouse.y
                                    updateCursor(hit)
                                }

                                onPositionChanged: function(mouse) {
                                    if (!pressed) {
                                        currentHit = getHitTest(mouse.x, mouse.y)
                                        updateCursor(currentHit)
                                        return
                                    }
                                    if (cropStage.paintW <= 0 || cropStage.paintH <= 0 || dragMode === "none") return

                                    var pw = cropStage.paintW
                                    var ph = cropStage.paintH
                                    var dx = (mouse.x - lastX) / pw
                                    var dy = (mouse.y - lastY) / ph
                                    lastX = mouse.x
                                    lastY = mouse.y

                                    var minW = 20 / pw
                                    var minH = 20 / ph

                                    if (dragMode === "move") {
                                        root.cropX = Math.max(0, Math.min(1 - root.cropW, root.cropX + dx))
                                        root.cropY = Math.max(0, Math.min(1 - root.cropH, root.cropY + dy))
                                        root.schedulePreview()
                                        return
                                    }

                                    // Resize Right
                                    if (dragMode === "r" || dragMode === "tr" || dragMode === "br") {
                                        var maxW = 1 - root.cropX
                                        root.cropW = Math.max(minW, Math.min(maxW, root.cropW + dx))
                                    }
                                    // Resize Left
                                    if (dragMode === "l" || dragMode === "tl" || dragMode === "bl") {
                                        var newX = Math.max(0, Math.min(root.cropX + root.cropW - minW, root.cropX + dx))
                                        var shiftX = newX - root.cropX
                                        root.cropX = newX
                                        root.cropW -= shiftX
                                    }
                                    // Resize Bottom
                                    if (dragMode === "b" || dragMode === "bl" || dragMode === "br") {
                                        var maxH = 1 - root.cropY
                                        root.cropH = Math.max(minH, Math.min(maxH, root.cropH + dy))
                                    }
                                    // Resize Top
                                    if (dragMode === "t" || dragMode === "tl" || dragMode === "tr") {
                                        var newY = Math.max(0, Math.min(root.cropY + root.cropH - minH, root.cropY + dy))
                                        var shiftY = newY - root.cropY
                                        root.cropY = newY
                                        root.cropH -= shiftY
                                    }

                                    root.schedulePreview()
                                }

                                onReleased: {
                                    dragMode = "none"
                                    currentHit = getHitTest(lastX, lastY)
                                    updateCursor(currentHit)
                                    liveCropTimer.stop()
                                    root.prepare()
                                }

                                onCanceled: {
                                    dragMode = "none"
                                    currentHit = "none"
                                    cursorShape = Qt.ArrowCursor
                                    liveCropTimer.stop()
                                    root.prepare()
                                }
                            }
                        }

                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 10
                            EzButton { text: EzI18n.text("Ausschnitt zentrieren"); onClicked: cropStage.resetSelection() }
                        }
                    }

                    // SECTION B: PENCIL & PAINT MODE (When face has NO image loaded)
                    ColumnLayout {
                        visible: !root.hasFaceImage(root.activeFace) && !root.fullCapeMode
                        Layout.fillWidth: true
                        spacing: 12

                        // Pencil brush controls
                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 12
                            Text {
                                text: EzI18n.text("Stiftgröße: ") + Math.round(root.brushSize) + " px"
                                color: EzTheme.text
                                font.bold: true
                                font.pixelSize: 13
                            }
                            Rectangle {
                                width: 28; height: 28; radius: 14
                                color: "#0F0A1C"
                                border.color: EzTheme.border
                                Rectangle {
                                    anchors.centerIn: parent
                                    width: Math.max(3, Math.min(24, root.brushSize * 24 / 150))
                                    height: width
                                    radius: width / 2
                                    color: root.brushColor
                                }
                            }
                            Slider {
                                Layout.fillWidth: true
                                from: 1; to: 150; stepSize: 1
                                value: root.brushSize
                                onMoved: root.brushSize = value
                            }
                        }

                        // Color palette swatches
                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 6
                            Text { text: EzI18n.text("Farbe wählen"); color: EzTheme.textSecondary; font.pixelSize: 12 }
                            Flow {
                                width: parent.width
                                Layout.fillWidth: true
                                spacing: 6
                                Repeater {
                                    model: [
                                        "#000000", "#262626", "#6B7280", "#FFFFFF",
                                        "#EF4444", "#991B1B", "#F97316", "#EAB308",
                                        "#22C55E", "#15803D", "#06B6D4", "#3B82F6",
                                        "#1D4ED8", "#8B5CF6", "#A855F7", "#EC4899"
                                    ]
                                    delegate: Rectangle {
                                        width: 26; height: 26; radius: 6
                                        color: modelData
                                        border.color: (root.brushColor === modelData) ? "#FFFFFF" : "#33274D"
                                        border.width: (root.brushColor === modelData) ? 2 : 1
                                        scale: (root.brushColor === modelData) ? 1.15 : 1.0
                                        Behavior on scale { NumberAnimation { duration: 100 } }
                                        MouseArea {
                                            anchors.fill: parent
                                            cursorShape: Qt.PointingHandCursor
                                            onClicked: root.brushColor = modelData
                                        }
                                    }
                                }
                            }
                        }

                        // Action buttons: Fill, Clear, Grid, Pick Image
                        Flow {
                            width: parent.width
                            Layout.fillWidth: true
                            spacing: 8
                            EzButton {
                                text: EzI18n.text("Fläche füllen")
                                onClicked: root.fillActiveFace(root.brushColor)
                            }
                            EzButton {
                                text: EzI18n.text("Zeichnung leeren")
                                onClicked: root.clearActiveFaceDrawing()
                            }
                            EzButton {
                                text: root.showPixelGrid ? EzI18n.text("Grid aus") : EzI18n.text("Grid an")
                                onClicked: root.showPixelGrid = !root.showPixelGrid
                            }
                            EzButton {
                                text: EzI18n.text("Bild laden")
                                onClicked: root.chooseImage()
                            }
                            EzButton {
                                text: EzI18n.text("Auf alle Seiten anwenden")
                                onClicked: root.applyActiveFaceToAll()
                            }
                        }

                        // Interactive Drawing Stage
                        Rectangle {
                            id: paintContainer
                            Layout.fillWidth: true
                            Layout.preferredHeight: 340
                            radius: 10
                            color: "#171126"
                            border.color: EzTheme.border
                            clip: true

                            // 1. Außen (back) Canvas: 200 x 320 px (10:16 Minecraft aspect)
                            Item {
                                id: itemBack
                                visible: root.activeFace === "back" && !root.hasFaceImage("back")
                                width: 200; height: 320
                                anchors.centerIn: parent
                                Rectangle { anchors.fill: parent; color: "#0F0A1C"; border.color: "#3B2D54"; border.width: 1 }
                                Canvas {
                                    id: canvasBack
                                    anchors.fill: parent
                                    renderTarget: Canvas.Image
                                    renderStrategy: Canvas.Immediate
                                }
                                Canvas {
                                    anchors.fill: parent
                                    visible: root.showPixelGrid
                                    opacity: 0.35
                                    onPaint: {
                                        var ctx = getContext("2d"); ctx.clearRect(0,0,width,height);
                                        ctx.strokeStyle = "#A78BFA"; ctx.lineWidth = 1;
                                        for (var x=1; x<10; ++x) { ctx.beginPath(); ctx.moveTo(x*width/10,0); ctx.lineTo(x*width/10,height); ctx.stroke(); }
                                        for (var y=1; y<16; ++y) { ctx.beginPath(); ctx.moveTo(0,y*height/16); ctx.lineTo(width,y*height/16); ctx.stroke(); }
                                    }
                                    onVisibleChanged: if (visible) requestPaint()
                                }
                                MouseArea {
                                    id: mouseAreaBack
                                    anchors.fill: parent
                                    cursorShape: Qt.CrossCursor
                                    hoverEnabled: true
                                    preventStealing: true
                                    property real lastX: 0; property real lastY: 0; property bool isPainting: false
                                    onPressed: function(mouse) {
                                        lastX = mouse.x; lastY = mouse.y; isPainting = true
                                        var ctx = canvasBack.getContext("2d")
                                        ctx.fillStyle = root.brushColor; ctx.beginPath(); ctx.arc(mouse.x, mouse.y, root.brushSize/2, 0, Math.PI*2); ctx.fill()
                                        canvasBack.requestPaint()
                                    }
                                    onPositionChanged: function(mouse) {
                                        if (!isPainting) return
                                        var ctx = canvasBack.getContext("2d")
                                        ctx.strokeStyle = root.brushColor; ctx.fillStyle = root.brushColor; ctx.lineWidth = root.brushSize; ctx.lineCap = "round"; ctx.lineJoin = "round"
                                        ctx.beginPath(); ctx.moveTo(lastX, lastY); ctx.lineTo(mouse.x, mouse.y); ctx.stroke()
                                        lastX = mouse.x; lastY = mouse.y
                                        canvasBack.requestPaint()
                                    }
                                    onReleased: { if (isPainting) { isPainting = false; root.syncFacePaint("back", canvasBack) } }
                                    onCanceled: { if (isPainting) { isPainting = false; root.syncFacePaint("back", canvasBack) } }
                                }

                                // Brush radius circle around cursor
                                Rectangle {
                                    width: root.brushSize
                                    height: root.brushSize
                                    radius: root.brushSize / 2
                                    x: mouseAreaBack.mouseX - width / 2
                                    y: mouseAreaBack.mouseY - height / 2
                                    color: Qt.rgba(root.brushColor.r, root.brushColor.g, root.brushColor.b, 0.25)
                                    border.color: "#FFFFFF"
                                    border.width: 1.5
                                    visible: mouseAreaBack.containsMouse
                                    enabled: false
                                    z: 99
                                    Rectangle {
                                        anchors.fill: parent
                                        anchors.margins: 1
                                        radius: width / 2
                                        color: "transparent"
                                        border.color: "#000000"
                                        border.width: 1
                                    }
                                    Rectangle {
                                        anchors.centerIn: parent
                                        width: 3; height: 3
                                        radius: 1.5
                                        color: "#FFFFFF"
                                    }
                                }
                            }

                            // 2. Innen (front) Canvas: 200 x 320 px (10:16 Minecraft aspect)
                            Item {
                                id: itemFront
                                visible: root.activeFace === "front" && !root.hasFaceImage("front")
                                width: 200; height: 320
                                anchors.centerIn: parent
                                Rectangle { anchors.fill: parent; color: "#0F0A1C"; border.color: "#3B2D54"; border.width: 1 }
                                Canvas {
                                    id: canvasFront
                                    anchors.fill: parent
                                    renderTarget: Canvas.Image
                                    renderStrategy: Canvas.Immediate
                                }
                                Canvas {
                                    anchors.fill: parent
                                    visible: root.showPixelGrid
                                    opacity: 0.35
                                    onPaint: {
                                        var ctx = getContext("2d"); ctx.clearRect(0,0,width,height);
                                        ctx.strokeStyle = "#A78BFA"; ctx.lineWidth = 1;
                                        for (var x=1; x<10; ++x) { ctx.beginPath(); ctx.moveTo(x*width/10,0); ctx.lineTo(x*width/10,height); ctx.stroke(); }
                                        for (var y=1; y<16; ++y) { ctx.beginPath(); ctx.moveTo(0,y*height/16); ctx.lineTo(width,y*height/16); ctx.stroke(); }
                                    }
                                    onVisibleChanged: if (visible) requestPaint()
                                }
                                MouseArea {
                                    id: mouseAreaFront
                                    anchors.fill: parent
                                    cursorShape: Qt.CrossCursor
                                    hoverEnabled: true
                                    preventStealing: true
                                    property real lastX: 0; property real lastY: 0; property bool isPainting: false
                                    onPressed: function(mouse) {
                                        lastX = mouse.x; lastY = mouse.y; isPainting = true
                                        var ctx = canvasFront.getContext("2d")
                                        ctx.fillStyle = root.brushColor; ctx.beginPath(); ctx.arc(mouse.x, mouse.y, root.brushSize/2, 0, Math.PI*2); ctx.fill()
                                        canvasFront.requestPaint()
                                    }
                                    onPositionChanged: function(mouse) {
                                        if (!isPainting) return
                                        var ctx = canvasFront.getContext("2d")
                                        ctx.strokeStyle = root.brushColor; ctx.fillStyle = root.brushColor; ctx.lineWidth = root.brushSize; ctx.lineCap = "round"; ctx.lineJoin = "round"
                                        ctx.beginPath(); ctx.moveTo(lastX, lastY); ctx.lineTo(mouse.x, mouse.y); ctx.stroke()
                                        lastX = mouse.x; lastY = mouse.y
                                        canvasFront.requestPaint()
                                    }
                                    onReleased: { if (isPainting) { isPainting = false; root.syncFacePaint("front", canvasFront) } }
                                    onCanceled: { if (isPainting) { isPainting = false; root.syncFacePaint("front", canvasFront) } }
                                }

                                // Brush radius circle around cursor
                                Rectangle {
                                    width: root.brushSize
                                    height: root.brushSize
                                    radius: root.brushSize / 2
                                    x: mouseAreaFront.mouseX - width / 2
                                    y: mouseAreaFront.mouseY - height / 2
                                    color: Qt.rgba(root.brushColor.r, root.brushColor.g, root.brushColor.b, 0.25)
                                    border.color: "#FFFFFF"
                                    border.width: 1.5
                                    visible: mouseAreaFront.containsMouse
                                    enabled: false
                                    z: 99
                                    Rectangle {
                                        anchors.fill: parent
                                        anchors.margins: 1
                                        radius: width / 2
                                        color: "transparent"
                                        border.color: "#000000"
                                        border.width: 1
                                    }
                                    Rectangle {
                                        anchors.centerIn: parent
                                        width: 3; height: 3
                                        radius: 1.5
                                        color: "#FFFFFF"
                                    }
                                }
                            }

                            // 3. Links (left) Canvas: 32 x 320 px (1:16 vertical strip, 16 squares)
                            Item {
                                id: itemLeft
                                visible: root.activeFace === "left" && !root.hasFaceImage("left")
                                width: 32; height: 320
                                anchors.centerIn: parent
                                Rectangle { anchors.fill: parent; color: "#0F0A1C"; border.color: "#3B2D54"; border.width: 1 }
                                Canvas {
                                    id: canvasLeft
                                    anchors.fill: parent
                                    renderTarget: Canvas.Image
                                    renderStrategy: Canvas.Immediate
                                }
                                Canvas {
                                    anchors.fill: parent
                                    visible: root.showPixelGrid
                                    opacity: 0.35
                                    onPaint: {
                                        var ctx = getContext("2d"); ctx.clearRect(0,0,width,height);
                                        ctx.strokeStyle = "#A78BFA"; ctx.lineWidth = 1;
                                        for (var y=1; y<16; ++y) { ctx.beginPath(); ctx.moveTo(0,y*height/16); ctx.lineTo(width,y*height/16); ctx.stroke(); }
                                    }
                                    onVisibleChanged: if (visible) requestPaint()
                                }
                                MouseArea {
                                    id: mouseAreaLeft
                                    anchors.fill: parent
                                    cursorShape: Qt.CrossCursor
                                    hoverEnabled: true
                                    preventStealing: true
                                    property real lastX: 0; property real lastY: 0; property bool isPainting: false
                                    onPressed: function(mouse) {
                                        lastX = mouse.x; lastY = mouse.y; isPainting = true
                                        var ctx = canvasLeft.getContext("2d")
                                        ctx.fillStyle = root.brushColor; ctx.beginPath(); ctx.arc(mouse.x, mouse.y, root.brushSize/2, 0, Math.PI*2); ctx.fill()
                                        canvasLeft.requestPaint()
                                    }
                                    onPositionChanged: function(mouse) {
                                        if (!isPainting) return
                                        var ctx = canvasLeft.getContext("2d")
                                        ctx.strokeStyle = root.brushColor; ctx.fillStyle = root.brushColor; ctx.lineWidth = root.brushSize; ctx.lineCap = "round"; ctx.lineJoin = "round"
                                        ctx.beginPath(); ctx.moveTo(lastX, lastY); ctx.lineTo(mouse.x, mouse.y); ctx.stroke()
                                        lastX = mouse.x; lastY = mouse.y
                                        canvasLeft.requestPaint()
                                    }
                                    onReleased: { if (isPainting) { isPainting = false; root.syncFacePaint("left", canvasLeft) } }
                                    onCanceled: { if (isPainting) { isPainting = false; root.syncFacePaint("left", canvasLeft) } }
                                }

                                // Brush radius circle around cursor
                                Rectangle {
                                    width: root.brushSize
                                    height: root.brushSize
                                    radius: root.brushSize / 2
                                    x: mouseAreaLeft.mouseX - width / 2
                                    y: mouseAreaLeft.mouseY - height / 2
                                    color: Qt.rgba(root.brushColor.r, root.brushColor.g, root.brushColor.b, 0.25)
                                    border.color: "#FFFFFF"
                                    border.width: 1.5
                                    visible: mouseAreaLeft.containsMouse
                                    enabled: false
                                    z: 99
                                    Rectangle {
                                        anchors.fill: parent
                                        anchors.margins: 1
                                        radius: width / 2
                                        color: "transparent"
                                        border.color: "#000000"
                                        border.width: 1
                                    }
                                    Rectangle {
                                        anchors.centerIn: parent
                                        width: 3; height: 3
                                        radius: 1.5
                                        color: "#FFFFFF"
                                    }
                                }
                            }

                            // 4. Rechts (right) Canvas: 32 x 320 px (1:16 vertical strip, 16 squares)
                            Item {
                                id: itemRight
                                visible: root.activeFace === "right" && !root.hasFaceImage("right")
                                width: 32; height: 320
                                anchors.centerIn: parent
                                Rectangle { anchors.fill: parent; color: "#0F0A1C"; border.color: "#3B2D54"; border.width: 1 }
                                Canvas {
                                    id: canvasRight
                                    anchors.fill: parent
                                    renderTarget: Canvas.Image
                                    renderStrategy: Canvas.Immediate
                                }
                                Canvas {
                                    anchors.fill: parent
                                    visible: root.showPixelGrid
                                    opacity: 0.35
                                    onPaint: {
                                        var ctx = getContext("2d"); ctx.clearRect(0,0,width,height);
                                        ctx.strokeStyle = "#A78BFA"; ctx.lineWidth = 1;
                                        for (var y=1; y<16; ++y) { ctx.beginPath(); ctx.moveTo(0,y*height/16); ctx.lineTo(width,y*height/16); ctx.stroke(); }
                                    }
                                    onVisibleChanged: if (visible) requestPaint()
                                }
                                MouseArea {
                                    id: mouseAreaRight
                                    anchors.fill: parent
                                    cursorShape: Qt.CrossCursor
                                    hoverEnabled: true
                                    preventStealing: true
                                    property real lastX: 0; property real lastY: 0; property bool isPainting: false
                                    onPressed: function(mouse) {
                                        lastX = mouse.x; lastY = mouse.y; isPainting = true
                                        var ctx = canvasRight.getContext("2d")
                                        ctx.fillStyle = root.brushColor; ctx.beginPath(); ctx.arc(mouse.x, mouse.y, root.brushSize/2, 0, Math.PI*2); ctx.fill()
                                        canvasRight.requestPaint()
                                    }
                                    onPositionChanged: function(mouse) {
                                        if (!isPainting) return
                                        var ctx = canvasRight.getContext("2d")
                                        ctx.strokeStyle = root.brushColor; ctx.fillStyle = root.brushColor; ctx.lineWidth = root.brushSize; ctx.lineCap = "round"; ctx.lineJoin = "round"
                                        ctx.beginPath(); ctx.moveTo(lastX, lastY); ctx.lineTo(mouse.x, mouse.y); ctx.stroke()
                                        lastX = mouse.x; lastY = mouse.y
                                        canvasRight.requestPaint()
                                    }
                                    onReleased: { if (isPainting) { isPainting = false; root.syncFacePaint("right", canvasRight) } }
                                    onCanceled: { if (isPainting) { isPainting = false; root.syncFacePaint("right", canvasRight) } }
                                }

                                // Brush radius circle around cursor
                                Rectangle {
                                    width: root.brushSize
                                    height: root.brushSize
                                    radius: root.brushSize / 2
                                    x: mouseAreaRight.mouseX - width / 2
                                    y: mouseAreaRight.mouseY - height / 2
                                    color: Qt.rgba(root.brushColor.r, root.brushColor.g, root.brushColor.b, 0.25)
                                    border.color: "#FFFFFF"
                                    border.width: 1.5
                                    visible: mouseAreaRight.containsMouse
                                    enabled: false
                                    z: 99
                                    Rectangle {
                                        anchors.fill: parent
                                        anchors.margins: 1
                                        radius: width / 2
                                        color: "transparent"
                                        border.color: "#000000"
                                        border.width: 1
                                    }
                                    Rectangle {
                                        anchors.centerIn: parent
                                        width: 3; height: 3
                                        radius: 1.5
                                        color: "#FFFFFF"
                                    }
                                }
                            }

                            // 5. Oben (top) Canvas: 320 x 32 px (10:1 horizontal strip, 10 squares)
                            Item {
                                id: itemTop
                                visible: root.activeFace === "top" && !root.hasFaceImage("top")
                                width: 320; height: 32
                                anchors.centerIn: parent
                                Rectangle { anchors.fill: parent; color: "#0F0A1C"; border.color: "#3B2D54"; border.width: 1 }
                                Canvas {
                                    id: canvasTop
                                    anchors.fill: parent
                                    renderTarget: Canvas.Image
                                    renderStrategy: Canvas.Immediate
                                }
                                Canvas {
                                    anchors.fill: parent
                                    visible: root.showPixelGrid
                                    opacity: 0.35
                                    onPaint: {
                                        var ctx = getContext("2d"); ctx.clearRect(0,0,width,height);
                                        ctx.strokeStyle = "#A78BFA"; ctx.lineWidth = 1;
                                        for (var x=1; x<10; ++x) { ctx.beginPath(); ctx.moveTo(x*width/10,0); ctx.lineTo(x*width/10,height); ctx.stroke(); }
                                    }
                                    onVisibleChanged: if (visible) requestPaint()
                                }
                                MouseArea {
                                    id: mouseAreaTop
                                    anchors.fill: parent
                                    cursorShape: Qt.CrossCursor
                                    hoverEnabled: true
                                    preventStealing: true
                                    property real lastX: 0; property real lastY: 0; property bool isPainting: false
                                    onPressed: function(mouse) {
                                        lastX = mouse.x; lastY = mouse.y; isPainting = true
                                        var ctx = canvasTop.getContext("2d")
                                        ctx.fillStyle = root.brushColor; ctx.beginPath(); ctx.arc(mouse.x, mouse.y, root.brushSize/2, 0, Math.PI*2); ctx.fill()
                                        canvasTop.requestPaint()
                                    }
                                    onPositionChanged: function(mouse) {
                                        if (!isPainting) return
                                        var ctx = canvasTop.getContext("2d")
                                        ctx.strokeStyle = root.brushColor; ctx.fillStyle = root.brushColor; ctx.lineWidth = root.brushSize; ctx.lineCap = "round"; ctx.lineJoin = "round"
                                        ctx.beginPath(); ctx.moveTo(lastX, lastY); ctx.lineTo(mouse.x, mouse.y); ctx.stroke()
                                        lastX = mouse.x; lastY = mouse.y
                                        canvasTop.requestPaint()
                                    }
                                    onReleased: { if (isPainting) { isPainting = false; root.syncFacePaint("top", canvasTop) } }
                                    onCanceled: { if (isPainting) { isPainting = false; root.syncFacePaint("top", canvasTop) } }
                                }

                                // Brush radius circle around cursor
                                Rectangle {
                                    width: root.brushSize
                                    height: root.brushSize
                                    radius: root.brushSize / 2
                                    x: mouseAreaTop.mouseX - width / 2
                                    y: mouseAreaTop.mouseY - height / 2
                                    color: Qt.rgba(root.brushColor.r, root.brushColor.g, root.brushColor.b, 0.25)
                                    border.color: "#FFFFFF"
                                    border.width: 1.5
                                    visible: mouseAreaTop.containsMouse
                                    enabled: false
                                    z: 99
                                    Rectangle {
                                        anchors.fill: parent
                                        anchors.margins: 1
                                        radius: width / 2
                                        color: "transparent"
                                        border.color: "#000000"
                                        border.width: 1
                                    }
                                    Rectangle {
                                        anchors.centerIn: parent
                                        width: 3; height: 3
                                        radius: 1.5
                                        color: "#FFFFFF"
                                    }
                                }
                            }

                            // 6. Unten (bottom) Canvas: 320 x 32 px (10:1 horizontal strip, 10 squares)
                            Item {
                                id: itemBottom
                                visible: root.activeFace === "bottom" && !root.hasFaceImage("bottom")
                                width: 320; height: 32
                                anchors.centerIn: parent
                                Rectangle { anchors.fill: parent; color: "#0F0A1C"; border.color: "#3B2D54"; border.width: 1 }
                                Canvas {
                                    id: canvasBottom
                                    anchors.fill: parent
                                    renderTarget: Canvas.Image
                                    renderStrategy: Canvas.Immediate
                                }
                                Canvas {
                                    anchors.fill: parent
                                    visible: root.showPixelGrid
                                    opacity: 0.35
                                    onPaint: {
                                        var ctx = getContext("2d"); ctx.clearRect(0,0,width,height);
                                        ctx.strokeStyle = "#A78BFA"; ctx.lineWidth = 1;
                                        for (var x=1; x<10; ++x) { ctx.beginPath(); ctx.moveTo(x*width/10,0); ctx.lineTo(x*width/10,height); ctx.stroke(); }
                                    }
                                    onVisibleChanged: if (visible) requestPaint()
                                }
                                MouseArea {
                                    id: mouseAreaBottom
                                    anchors.fill: parent
                                    cursorShape: Qt.CrossCursor
                                    hoverEnabled: true
                                    preventStealing: true
                                    property real lastX: 0; property real lastY: 0; property bool isPainting: false
                                    onPressed: function(mouse) {
                                        lastX = mouse.x; lastY = mouse.y; isPainting = true
                                        var ctx = canvasBottom.getContext("2d")
                                        ctx.fillStyle = root.brushColor; ctx.beginPath(); ctx.arc(mouse.x, mouse.y, root.brushSize/2, 0, Math.PI*2); ctx.fill()
                                        canvasBottom.requestPaint()
                                    }
                                    onPositionChanged: function(mouse) {
                                        if (!isPainting) return
                                        var ctx = canvasBottom.getContext("2d")
                                        ctx.strokeStyle = root.brushColor; ctx.fillStyle = root.brushColor; ctx.lineWidth = root.brushSize; ctx.lineCap = "round"; ctx.lineJoin = "round"
                                        ctx.beginPath(); ctx.moveTo(lastX, lastY); ctx.lineTo(mouse.x, mouse.y); ctx.stroke()
                                        lastX = mouse.x; lastY = mouse.y
                                        canvasBottom.requestPaint()
                                    }
                                    onReleased: { if (isPainting) { isPainting = false; root.syncFacePaint("bottom", canvasBottom) } }
                                    onCanceled: { if (isPainting) { isPainting = false; root.syncFacePaint("bottom", canvasBottom) } }
                                }

                                // Brush radius circle around cursor
                                Rectangle {
                                    width: root.brushSize
                                    height: root.brushSize
                                    radius: root.brushSize / 2
                                    x: mouseAreaBottom.mouseX - width / 2
                                    y: mouseAreaBottom.mouseY - height / 2
                                    color: Qt.rgba(root.brushColor.r, root.brushColor.g, root.brushColor.b, 0.25)
                                    border.color: "#FFFFFF"
                                    border.width: 1.5
                                    visible: mouseAreaBottom.containsMouse
                                    enabled: false
                                    z: 99
                                    Rectangle {
                                        anchors.fill: parent
                                        anchors.margins: 1
                                        radius: width / 2
                                        color: "transparent"
                                        border.color: "#000000"
                                        border.width: 1
                                    }
                                    Rectangle {
                                        anchors.centerIn: parent
                                        width: 3; height: 3
                                        radius: 1.5
                                        color: "#FFFFFF"
                                    }
                                }
                            }
                        }
                    }

                    // Cape-Name input
                    Text { text: EzI18n.text("Cape-Name"); color: EzTheme.text; font.bold: true; font.pixelSize: 15 }
                    TextField {
                        id: capeNameField
                        Layout.fillWidth: true
                        placeholderText: EzI18n.text("z. B. MeinCape")
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
                        text: root.capeName.trim().length < 3 ? EzI18n.text("Mindestens 3, maximal 48 Zeichen") : root.capeName.length + EzI18n.text("/48 Zeichen")
                        color: root.capeName.trim().length < 3 ? "#FCA5A5" : EzTheme.textMuted
                        font.pixelSize: 11
                    }

                    // 3D Animation mode selector
                    RowLayout {
                        spacing: 10
                        Text { text: EzI18n.text("3D-Animation"); color: EzTheme.text; font.bold: true; font.pixelSize: 15 }
                        EzButton { text: EzI18n.text("Stehen"); primary: root.previewAnimation === "idle"; onClicked: root.previewAnimation = "idle" }
                        EzButton { text: EzI18n.text("Laufen"); primary: root.previewAnimation === "walk"; onClicked: root.previewAnimation = "walk" }
                    }

                    // Full cape format banner if detected
                    Rectangle {
                        visible: root.selectedSource !== "" && !root.animatedSource && root.fullCapeMode
                        Layout.fillWidth: true
                        Layout.preferredHeight: 52
                        radius: 9
                        color: EzTheme.surface2
                        border.color: EzTheme.accent
                        RowLayout {
                            anchors.fill: parent; anchors.margins: 10; spacing: 10
                            Rectangle {
                                Layout.preferredWidth: 8; Layout.preferredHeight: 8
                                radius: 4; color: "#22C55E"
                            }
                            Text {
                                Layout.fillWidth: true
                                text: EzI18n.text("Minecraft 64x32 Vollcape aktiv: Alle 6 Seiten werden 1:1 original gerendert.")
                                color: EzTheme.text
                                wrapMode: Text.WordWrap
                                font.pixelSize: 11
                            }
                        }
                    }

                    // Status banner for face completion
                    Rectangle {
                        Layout.fillWidth: true
                        implicitHeight: 40
                        radius: 8
                        color: root.allFacesConfigured() ? "#14532D" : "#451A03"
                        border.color: root.allFacesConfigured() ? "#22C55E" : "#F59E0B"
                        border.width: 1
                        RowLayout {
                            anchors.fill: parent
                            anchors.leftMargin: 12
                            anchors.rightMargin: 12
                            spacing: 8
                            Text {
                                text: root.allFacesConfigured() ? "✓" : "⚠"
                                font.bold: true
                                color: root.allFacesConfigured() ? "#86EFAC" : "#FDE68A"
                                font.pixelSize: 13
                            }
                            Text {
                                Layout.fillWidth: true
                                text: root.allFacesConfigured()
                                    ? "Alle 6 Seiten sind gestaltet! Das Cape kann gespeichert werden."
                                    : ("Pflicht: Noch " + root.missingFacesCount() + " von 6 Seiten offen (Tipp: „Auf alle Seiten anwenden“ für schnelles Füllen).")
                                color: root.allFacesConfigured() ? "#86EFAC" : "#FDE68A"
                                font.pixelSize: 11
                                wrapMode: Text.WordWrap
                            }
                        }
                    }

                    Text {
                        text: EzI18n.text("Das Cape wird nach „Bestätigen & hochladen“ aktiviert und im Spiel sichtbar.")
                        color: EzTheme.textSecondary
                        wrapMode: Text.WordWrap
                        Layout.fillWidth: true
                        font.pixelSize: 12
                    }

                    // Confirm and Discard Buttons
                    RowLayout {
                        spacing: 12
                        EzButton {
                            text: root.isUploading ? EzI18n.text("Wird hochgeladen …") : (root.previewProcessing ? EzI18n.text("Vorschau lädt …") : EzI18n.text("Bestätigen & hochladen"))
                            primary: true
                            enabled: !root.isUploading && root.allFacesConfigured() && root.capeName.trim().length >= 3 && !root.previewProcessing && !root.mediaProcessing
                            onClicked: root.confirm()
                        }
                        EzButton {
                            text: EzI18n.text("Verwerfen")
                            enabled: root.hasAnyCapeContent() || root.pendingPreview !== ""
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

                    Item { Layout.fillWidth: true; Layout.preferredHeight: 16 }
                }
            }

            // 3D character preview on the right side.
            Rectangle {
                id: previewCard
                Layout.preferredWidth: Math.max(340, Math.min(460, root.width * 0.36))
                Layout.minimumWidth: 320
                Layout.maximumWidth: 460
                Layout.fillHeight: true
                radius: 10
                color: "#171126"
                border.color: EzTheme.border
                clip: true

                Skin3DView {
                    id: editorSkin3D
                    anchors.fill: parent
                    anchors.margins: 8
                    skinSource: root.account ? root.account.skinTextureUrl : ""
                    capeSource: root.animatedSource ? "" : root.pendingPreview
                    animation: root.previewAnimation
                    autoRotate: false
                    interactive: true
                    initialRotateY: 180
                }

                Rectangle {
                    anchors.fill: parent
                    visible: (root.mediaProcessing && root.cropImageSource === "") || root.previewProcessing
                    color: "#CC0F0B18"
                    z: 50
                    ColumnLayout {
                        anchors.centerIn: parent
                        spacing: 12
                        BusyIndicator {
                            Layout.alignment: Qt.AlignHCenter
                            running: visible
                        }
                        Text {
                            text: root.mediaProcessing ? EzI18n.text("Animation wird vorbereitet …") : EzI18n.text("Vorschau wird geladen …")
                            color: EzTheme.text
                            font.pixelSize: 13
                            font.bold: true
                            Layout.alignment: Qt.AlignHCenter
                        }
                    }
                }

                Rectangle {
                    anchors.right: parent.right; anchors.top: parent.top; anchors.margins: 14
                    width: 32; height: 32; radius: 8
                    color: refreshAnimationMouse.containsMouse ? "#E02A213D" : "#B319132A"
                    border.color: refreshAnimationMouse.containsMouse ? EzTheme.accent : EzTheme.border
                    visible: root.animatedSource
                    z: 51

                    Image {
                        id: refreshAnimationIcon
                        anchors.centerIn: parent
                        width: 17; height: 17
                        source: "icons/refresh-cw.svg"
                        fillMode: Image.PreserveAspectFit
                        opacity: root.mediaProcessing ? 0.65 : 1.0
                        RotationAnimation on rotation {
                            running: root.mediaProcessing
                            from: 0; to: 360; duration: 900
                            loops: Animation.Infinite
                        }
                    }

                    MouseArea {
                        id: refreshAnimationMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.refreshAnimationPreview()
                    }

                    ToolTip.visible: refreshAnimationMouse.containsMouse
                    ToolTip.text: EzI18n.text("Animation neu laden")
                }

                Rectangle {
                    anchors.left: parent.left; anchors.top: parent.top; anchors.margins: 14
                    width: previewHint.implicitWidth + 20; height: 30; radius: 15
                    color: "#B319132A"; border.color: EzTheme.border
                    Text {
                        id: previewHint; anchors.centerIn: parent
                        text: EzI18n.text("Ziehen: 360°  •  Mausrad: Zoom")
                        color: EzTheme.textSecondary; font.pixelSize: 11
                    }
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
            if (drop.hasUrls && drop.urls.length > 0) root.setFaceImage(drop.urls[0].toString())
        }

        Rectangle {
            anchors.fill: parent
            visible: mediaDropArea.containsDrag
            color: "#D9110D20"
            border.color: EzTheme.accent
            border.width: 2
            Text {
                anchors.centerIn: parent
                text: EzI18n.text("Cape-Datei hier ablegen")
                color: EzTheme.text
                font.pixelSize: 20
                font.bold: true
            }
        }
    }
}
