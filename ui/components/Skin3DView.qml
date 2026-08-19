import QtQuick 2.15
import QtQuick.Controls 2.15
import QtWebEngine 1.10

Item {
    id: skin3dRoot

    property string skinSource: ""
    property string skinVariant: "classic"  // "classic" (4px) or "slim" (3px)
    property string animation: "idle"      // "idle", "walk", "run", "fly", "none"
    property bool autoRotate: false
    property real autoRotateSpeed: 1.0
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

    function setRotateY(deg) {
        if (!isLoaded || !webEngine) return
        webEngine.runJavaScript("rotateY(" + Number(deg) + ");")
    }

    function resetView() {
        if (!isLoaded || !webEngine) return
        webEngine.runJavaScript("resetView();")
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
    onSkinVariantChanged: updateSkin()
    onAnimationChanged: setAnim(animation)
    onAutoRotateChanged: setAutoRot(autoRotate, autoRotateSpeed)

    WebEngineView {
        id: webEngine
        anchors.fill: parent
        backgroundColor: "transparent"
        url: skin3dRoot.htmlUrl

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
            skin3dRoot.setAnim(skin3dRoot.animation)
            skin3dRoot.setAutoRot(skin3dRoot.autoRotate, skin3dRoot.autoRotateSpeed)
        }
    }
}
