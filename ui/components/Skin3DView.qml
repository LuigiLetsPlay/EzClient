import QtQuick 2.15
import QtQuick.Controls 2.15
import QtWebEngine 1.10

Item {
    id: skin3dRoot

    property string skinSource: ""
    property string capeSource: ""
    property var capeAnimationInfo: ({})
    property string skinVariant: "classic"  // "classic" (4px) or "slim" (3px)
    property string animation: "idle"      // "idle", "walk", "run", "fly", "none"
    property bool autoRotate: false
    property real autoRotateSpeed: 1.0
    property real initialRotateX: 0
    property real initialRotateY: 0
    // Home uses its own small click target. Leaving the WebEngine interactive
    // there would make the invisible rotation area cover the whole stage.
    property bool interactive: true
    property bool isLoaded: false

    readonly property string htmlUrl: Qt.resolvedUrl("../assets/skinview3d/skin_viewer.html").toString()

    function updateSkin() {
        if (!isLoaded || !webEngine) return
        var src = skinSource || ""
        var variant = (skinVariant === "slim") ? "slim" : "classic"
        if (src === "") {
            src = Qt.resolvedUrl("../assets/skins/steve.png").toString()
        }
        var cleanSrc = src.replace(/[\r\n]/g, "").replace(/'/g, "\\'")
        var js = "setSkin('" + cleanSrc + "', '" + variant + "');"
        webEngine.runJavaScript(js)
    }

    function updateCape() {
        if (!isLoaded || !webEngine) return
        var info = capeAnimationInfo || {}
        if (info.sheetUrl) {
            setAnimatedCape(info.sheetUrl, info.frameCount, info.fps, info.columns,
                            info.frameWidth, info.frameHeight, info.pingPong)
            return
        }
        var src = (capeSource || "").replace(/[\r\n]/g, "").replace(/'/g, "\\'")
        webEngine.runJavaScript("setCape('" + src + "');")
    }

    function setAnimatedCape(sheetUrl, frameCount, fps, columns, frameW, frameH, pingPong) {
        if (!isLoaded || !webEngine) return
        var cleanSheet = (sheetUrl || "").replace(/[\r\n]/g, "").replace(/'/g, "\\'")
        var js = "setAnimatedCape('" + cleanSheet + "', " + Number(frameCount) + ", " + Number(fps) + ", " + Number(columns) + ", " + Number(frameW) + ", " + Number(frameH) + ", " + (pingPong ? "true" : "false") + ");"
        webEngine.runJavaScript(js)
    }

    function setRotateY(deg) {
        if (!isLoaded || !webEngine) return
        webEngine.runJavaScript("rotateY(" + Number(deg) + ");")
    }

    function resetView() {
        if (!isLoaded || !webEngine) return
        webEngine.runJavaScript("resetView(" + Number(skin3dRoot.initialRotateY) + ");")
    }

    function setAnim(name, speed) {
        if (!isLoaded || !webEngine) return
        var s = (speed !== undefined) ? speed : 0.7
        webEngine.runJavaScript("setAnimation('" + name + "', " + s + ");")
    }

    function setAutoRot(enabled, speed) {
        if (!isLoaded || !webEngine) return
        var s = (speed !== undefined) ? speed : 1.0
        webEngine.runJavaScript("setAutoRotate(" + (enabled ? "true" : "false") + ", " + s + ");")
    }

    onSkinSourceChanged: updateSkin()
    onCapeSourceChanged: updateCape()
    onCapeAnimationInfoChanged: updateCape()
    onSkinVariantChanged: updateSkin()
    onAnimationChanged: setAnim(animation)
    onAutoRotateChanged: setAutoRot(autoRotate, autoRotateSpeed)

    signal skinClicked()

    WebEngineView {
        id: webEngine
        anchors.fill: parent
        backgroundColor: "transparent"
        enabled: skin3dRoot.interactive
        url: skin3dRoot.htmlUrl

        onNavigationRequested: function(request) {
            if (request.url.toString() === "ezclient://openskinmodal") {
                request.action = WebEngineNavigationRequest.IgnoreRequest
                skin3dRoot.skinClicked()
            }
        }

        settings.accelerated2dCanvasEnabled: true
        settings.webGLEnabled: true
        settings.localContentCanAccessRemoteUrls: true
        settings.localContentCanAccessFileUrls: true

        onLoadingChanged: function(loadingInfo) {
            var s = loadingInfo ? loadingInfo.status : (typeof loadRequest !== "undefined" ? loadRequest.status : 2)
            if (s === WebEngineView.LoadStatusSuccess || s === WebEngineView.LoadSucceededStatus || s === 2) {
                skin3dRoot.isLoaded = true
                applyTimer.start()
            }
        }
    }

    Timer {
        id: applyTimer
        interval: 50
        repeat: false
        onTriggered: {
            skin3dRoot.updateSkin()
            skin3dRoot.updateCape()
            skin3dRoot.setAnim(skin3dRoot.animation)
            webEngine.runJavaScript("setInitialRotation(" + Number(skin3dRoot.initialRotateX) + ", " + Number(skin3dRoot.initialRotateY) + ");")
            skin3dRoot.setAutoRot(skin3dRoot.autoRotate, skin3dRoot.autoRotateSpeed)
        }
    }
}
