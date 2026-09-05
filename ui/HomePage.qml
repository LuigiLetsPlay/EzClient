import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import QtMultimedia
import "components"

Item {
    id: root

    Timer {
        interval: 10000
        repeat: true
        running: root.visible
        triggeredOnStart: true
        onTriggered: if (profileController) profileController.refreshEzClientUpdateState()
    }

    readonly property bool hasProfile: typeof profileController !== "undefined" && profileController && profileController.activeName !== "No Profile" && profileController.activeName !== ""
    readonly property string activeName: typeof profileController !== "undefined" && profileController ? profileController.activeName : ""
    readonly property string activeVersion: typeof profileController !== "undefined" && profileController ? profileController.activeVersion : "26.2"
    readonly property string activeLoader: typeof profileController !== "undefined" && profileController ? profileController.activeLoader : "Fabric"
    readonly property bool activeIsVanilla: activeLoader.toLowerCase() === "vanilla"
    readonly property int activeModsCount: typeof profileController !== "undefined" && profileController ? profileController.activeModsCount : 0
    readonly property int activeRamMb: typeof profileController !== "undefined" && profileController ? profileController.activeRamMb : 4096
    readonly property bool isLaunching: typeof profileController !== "undefined" && profileController ? profileController.isLaunching : false

    readonly property string accountUser: typeof accountController !== "undefined" && accountController ? accountController.username : "Player"
    readonly property string bodyUrl: typeof accountController !== "undefined" && accountController ? accountController.bodyUrl : ""
    readonly property string skinTextureUrl: typeof accountController !== "undefined" && accountController ? accountController.skinTextureUrl : ""
    readonly property string capeTextureUrl: typeof accountController !== "undefined" && accountController ? accountController.capePreviewTextureUrl : ""
    readonly property bool hasBackgroundVideo: {
        var p = typeof profileController !== "undefined" && profileController ? profileController.customBackgroundImage : ""
        return /\.(mp4|webm|mov)$/i.test(p)
    }

    function formatImageUrl(path) {
        if (!path) return "assets/hero_bg.jpg";
        if (path.startsWith("file:///") || path.startsWith("http://") || path.startsWith("https://") || path.startsWith("qrc:/")) return path;
        var clean = path.replace(/\\/g, "/");
        if (clean.startsWith("/")) return "file://" + clean;
        return "file:///" + clean;
    }

    // ─────────────────────────────────────────────────────────
    // 1. FULL-BLEED CINEMATIC MINECRAFT BACKGROUND
    // ─────────────────────────────────────────────────────────
    Image {
        id: bgHero
        anchors.fill: parent
        visible: !root.hasBackgroundVideo
        source: (typeof profileController !== "undefined" && profileController && profileController.customBackgroundImage) 
                ? root.formatImageUrl(profileController.customBackgroundImage) 
                : "assets/hero_bg.jpg"
        fillMode: (typeof profileController !== "undefined" && profileController && profileController.customBackgroundFillMode === "PreserveAspectFit") 
                  ? Image.PreserveAspectFit 
                  : (profileController && profileController.customBackgroundFillMode === "Stretch" ? Image.Stretch : Image.PreserveAspectCrop)
        opacity: (typeof profileController !== "undefined" && profileController && profileController.customBackgroundImage) 
                 ? profileController.customBackgroundOpacity 
                 : 0.35
        Behavior on opacity { NumberAnimation { duration: 300 } }
    }

    MediaPlayer {
        id: backgroundClipPlayer
        source: root.hasBackgroundVideo && profileController ? root.formatImageUrl(profileController.customBackgroundImage) : ""
        loops: MediaPlayer.Infinite
        autoPlay: root.hasBackgroundVideo
        audioOutput: backgroundClipAudio
        videoOutput: backgroundClipOutput
    }
    AudioOutput {
        id: backgroundClipAudio
        muted: true
    }
    VideoOutput {
        id: backgroundClipOutput
        anchors.fill: parent
        visible: root.hasBackgroundVideo
        fillMode: VideoOutput.PreserveAspectCrop
        opacity: profileController ? profileController.customBackgroundOpacity : 0.35
    }

    // Cinematic Vignette Overlay
    Rectangle {
        anchors.fill: parent
        gradient: Gradient {
            orientation: Gradient.Vertical
            GradientStop { position: 0.0; color: "#F00A0A0F" }
            GradientStop { position: 0.2; color: "#600A0A0F" }
            GradientStop { position: 0.7; color: "#600A0A0F" }
            GradientStop { position: 1.0; color: "#F50A0A0F" }
        }
    }

    // Side vignette
    Rectangle {
        anchors.fill: parent
        gradient: Gradient {
            orientation: Gradient.Horizontal
            GradientStop { position: 0.0; color: "#880A0A0F" }
            GradientStop { position: 0.15; color: "transparent" }
            GradientStop { position: 0.85; color: "transparent" }
            GradientStop { position: 1.0; color: "#880A0A0F" }
        }
    }

    // ─────────────────────────────────────────────────────────
    // AMBIENT FLOATING PARTICLES (Subtle Minecraft aesthetic)
    // ─────────────────────────────────────────────────────────
    Repeater {
        model: 16
        visible: true
        Rectangle {
            property real startX: Math.random() * root.width
            property real startY: Math.random() * root.height
            property real animDuration: 4000 + Math.random() * 6000

            x: startX
            width: 2 + Math.random() * 3
            height: width
            radius: width / 2
            color: EzTheme.accent
            opacity: 0.12 + Math.random() * 0.15

            SequentialAnimation on y {
                loops: Animation.Infinite
                NumberAnimation { from: startY; to: startY - 90 - Math.random() * 120; duration: animDuration; easing.type: Easing.InOutSine }
                NumberAnimation { from: startY - 90 - Math.random() * 120; to: startY; duration: animDuration; easing.type: Easing.InOutSine }
            }
            SequentialAnimation on opacity {
                loops: Animation.Infinite
                NumberAnimation { to: 0.04; duration: animDuration * 0.8 }
                NumberAnimation { to: 0.14 + Math.random() * 0.16; duration: animDuration * 0.8 }
            }
        }
    }



    // Independent character layer. It never participates in the controls'
    // layout calculation, so resizing buttons or fonts cannot move the skin.
    Item {
        id: independentSkinStage
        z: 2
        anchors.horizontalCenter: parent.horizontalCenter
        anchors.top: parent.top
        anchors.topMargin: -24
        width: Math.min(820, Math.max(480, root.width * 0.82))
        height: Math.max(540, root.height + 36)

        SequentialAnimation {
            id: accountSwitchAnimation
            NumberAnimation { target: centeredHomeSkin3D; property: "opacity"; to: 0; duration: 110; easing.type: Easing.InQuad }
            PauseAnimation { duration: 70 }
            NumberAnimation { target: centeredHomeSkin3D; property: "opacity"; to: 1; duration: 190; easing.type: Easing.OutCubic }
        }
        Connections {
            target: typeof accountController !== "undefined" ? accountController : null
            function onAccountChanged() { accountSwitchAnimation.restart() }
        }

        Skin3DView {
            id: centeredHomeSkin3D
            anchors.fill: parent
            skinSource: root.skinTextureUrl
            capeSource: root.capeTextureUrl
            capeAnimationInfo: typeof accountController !== "undefined" && accountController ? accountController.capeAnimationInfo : ({})
            animation: "idle"
            autoRotate: false
            interactive: false
            initialRotateX: 0
            initialRotateY: -14
            onSkinClicked: {
                if (typeof window !== "undefined" && window.openSkinModal) window.openSkinModal()
            }
        }

        // Compact custom hitbox: drag rotates the skin, a plain click opens
        // the skin modal. Everything outside stays clickable as usual.
        MouseArea {
            id: skinHitbox
            z: 5
            anchors.horizontalCenter: parent.horizontalCenter
            y: Math.max(90, parent.height * 0.20)
            width: Math.min(200, parent.width * 0.28)
            height: Math.min(330, parent.height * 0.55)
            hoverEnabled: true
            cursorShape: pressed ? Qt.ClosedHandCursor : (containsMouse ? Qt.OpenHandCursor : Qt.ArrowCursor)
            property real startX: 0
            property real startY: 0
            property real lastX: 0
            property real angle: -14
            property bool wasDragged: false

            onPressed: function(mouse) {
                startX = mouse.x
                startY = mouse.y
                lastX = mouse.x
                wasDragged = false
            }

            onPositionChanged: function(mouse) {
                if (!pressed) return
                var dx = mouse.x - lastX
                var totalDist = Math.hypot(mouse.x - startX, mouse.y - startY)
                if (totalDist > 5) {
                    wasDragged = true
                }
                lastX = mouse.x
                if (dx === 0) return
                angle += dx * 0.8
                centeredHomeSkin3D.setRotateY(angle)
            }

            onClicked: function(mouse) {
                if (wasDragged) return
                if (typeof window !== "undefined" && window.openSkinModal) {
                    window.openSkinModal()
                }
            }
        }

        Rectangle {
            z: 4
            anchors.horizontalCenter: parent.horizontalCenter
            anchors.top: parent.top
            anchors.topMargin: Math.max(64, parent.height * 0.12)
            height: 26
            width: independentNameRow.implicitWidth + 20
            radius: 3
            color: "#D90B0E12"
            border.color: "#553B4652"
            RowLayout {
                id: independentNameRow
                anchors.centerIn: parent
                spacing: 5
                Image {
                    source: "assets/logo.svg"
                    Layout.preferredWidth: 8
                    Layout.preferredHeight: 8
                    sourceSize.width: 8
                    sourceSize.height: 8
                    fillMode: Image.PreserveAspectFit
                    clip: true
                }
                Text {
                    text: root.accountUser
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 13
                    color: EzTheme.text
                }
            }
        }

        Row {
            z: 7
            anchors.horizontalCenter: parent.horizontalCenter
            anchors.top: parent.top
            anchors.topMargin: Math.max(96, parent.height * 0.12 + 34)
            spacing: 7
            visible: typeof accountController !== "undefined" && accountController && accountController.accounts.length > 1
            Repeater {
                model: (typeof accountController !== "undefined" && accountController) ? accountController.accounts : []
                Rectangle {
                    width: 34; height: 34; radius: 17
                    color: modelData.active ? EzTheme.accent : "#D912151B"
                    border.width: modelData.active ? 2 : 1
                    border.color: modelData.active ? EzTheme.accentLight : EzTheme.border
                    Image { anchors.fill: parent; anchors.margins: 3; source: modelData.avatarUrl; fillMode: Image.PreserveAspectCrop; smooth: true }
                    MouseArea { anchors.fill: parent; cursorShape: Qt.PointingHandCursor; onClicked: accountController.switchAccount(modelData.uuid) }
                    ToolTip.visible: accountMouse.containsMouse
                    ToolTip.text: modelData.username
                    MouseArea { id: accountMouse; anchors.fill: parent; hoverEnabled: true; acceptedButtons: Qt.NoButton }
                }
            }
        }

    }

    // Controls are a separate overlay in front of the character's legs.
    ColumnLayout {
        z: 10
        anchors.horizontalCenter: parent.horizontalCenter
        y: Math.round(parent.height / 2 + 72)
        spacing: 0

        // ── Welcome Text ──
        Text {
            visible: false
            Layout.alignment: Qt.AlignHCenter
            text: EzI18n.t("home_welcome", "Willkommen zurück") + ","
            font.family: EzTheme.fontFamily
            font.pixelSize: 14
            color: EzTheme.textSecondary
            opacity: 0.8
        }
        Item { height: 4 }

        // ── Legacy character container ──
        // Keep this disabled without constructing another Chromium WebEngine.
        // The actual character is rendered once by centeredHomeSkin3D above.
        Item {
            id: skinContainer
            visible: false
            Layout.preferredWidth: 0
            Layout.preferredHeight: 0
            Layout.alignment: Qt.AlignHCenter
            width: Math.min(500, Math.max(280, root.width - 100))
            height: Math.min(430, Math.max(280, root.height - 250))

            // In-launcher nametag, matching the EzClient identity used in game.
            Rectangle {
                z: 4
                anchors.horizontalCenter: parent.horizontalCenter
                anchors.top: parent.top
                anchors.topMargin: 8
                height: 21
                width: homeNameRow.implicitWidth + 14
                radius: 3
                color: "#D90B0E12"
                border.color: "#553B4652"
                RowLayout {
                    id: homeNameRow
                    anchors.centerIn: parent
                    spacing: 5
                    Image {
                        source: "assets/logo.svg"
                        Layout.preferredWidth: 8
                        Layout.preferredHeight: 8
                        sourceSize.width: 8
                        sourceSize.height: 8
                        fillMode: Image.PreserveAspectFit
                        clip: true
                    }
                    Text {
                        text: root.accountUser
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 11
                        color: EzTheme.text
                    }
                }
            }

            // Real 3D Minecraft Skin Model Container
            Item {
                anchors.fill: parent
                anchors.topMargin: 18
                // Keep Qt WebEngine untransformed. Scaling a WebEngine surface can
                // detach its GPU layer and place it in the bottom-right corner.

                Loader {
                    id: homeSkin3D
                    anchors.fill: parent
                    active: false
                }
            }

        }

        Item { Layout.preferredHeight: 0 }

        // ── Active Profile Pill ──
        Rectangle {
            Layout.alignment: Qt.AlignHCenter
            Layout.topMargin: 0
            Layout.preferredHeight: 40
            Layout.preferredWidth: Math.min(root.width - 32, profPillRow.implicitWidth + 28)
            radius: 12
            color: profPillMouse.containsMouse ? "#1A2520" : "#111B17"
            border.color: profPillMouse.containsMouse ? EzTheme.accentLight : EzTheme.borderLight
            border.width: 1
            scale: profPillMouse.containsMouse ? 1.03 : 1.0

            Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }
            Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }
            Behavior on border.color { ColorAnimation { duration: EzTheme.animNormal } }

            RowLayout {
                id: profPillRow
                anchors.centerIn: parent
                spacing: 8

                Text {
                    text: (root.hasProfile ? root.activeName : EzI18n.t("home_default_profile", "Standard Profil"))
                          + "  ·  " + root.activeLoader + " " + root.activeVersion
                          + (root.activeIsVanilla ? "" : "  ·  " + root.activeModsCount + " Mods")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    font.bold: true
                    color: EzTheme.text
                    elide: Text.ElideRight
                    Layout.maximumWidth: Math.max(140, root.width - 70)
                }
            }

            MouseArea {
                id: profPillMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: {
                    if (typeof window !== "undefined" && window.navigateTo) {
                        window.navigateTo("profile_detail")
                    }
                }
            }
        }

        Item { Layout.preferredHeight: 16 }

        // ── GIANT EPIC PLAY BUTTON ──
        Rectangle {
            id: launchBtn
            z: 6
            Layout.alignment: Qt.AlignHCenter
            Layout.preferredWidth: Math.min(400, root.width - 40)
            Layout.preferredHeight: 74
            radius: 16

            scale: launchMouse.pressed ? 0.95 : (launchMouse.containsMouse ? 1.04 : 1.0)
            Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }

            color: root.isLaunching ? EzTheme.warning : (launchMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent)

            border.color: root.isLaunching ? "#FDE68A" : (launchMouse.containsMouse ? "#5AEEA0" : "#22C96E50")
            border.width: 2

            // Outer glow effect with breathing pulse
            Rectangle {
                anchors.fill: parent
                anchors.margins: -5
                radius: parent.radius + 5
                color: "transparent"
                border.color: root.isLaunching ? "#F59E0B" : EzTheme.accent
                border.width: 2.5
                opacity: launchMouse.containsMouse ? 0.9 : 0.4
                Behavior on opacity { NumberAnimation { duration: EzTheme.animNormal } }

                SequentialAnimation on scale {
                    loops: Animation.Infinite
                    running: !root.isLaunching
                    NumberAnimation { from: 1.0; to: 1.03; duration: 1600; easing.type: Easing.InOutSine }
                    NumberAnimation { from: 1.03; to: 1.0; duration: 1600; easing.type: Easing.InOutSine }
                }
                SequentialAnimation on opacity {
                    loops: Animation.Infinite
                    running: !root.isLaunching && !launchMouse.containsMouse
                    NumberAnimation { from: 0.3; to: 0.7; duration: 1600; easing.type: Easing.InOutSine }
                    NumberAnimation { from: 0.7; to: 0.3; duration: 1600; easing.type: Easing.InOutSine }
                }
            }


            RowLayout {
                anchors.centerIn: parent
                spacing: 14

                Image {
                    source: "icons/play.svg"
                    width: 20; height: 20
                    fillMode: Image.PreserveAspectFit
                    visible: !root.isLaunching
                }

                Rectangle {
                    width: 16; height: 16; radius: 8
                    color: "#000000"
                    visible: root.isLaunching
                    SequentialAnimation on scale {
                        loops: Animation.Infinite
                        NumberAnimation { to: 0.4; duration: 400 }
                        NumberAnimation { to: 1.0; duration: 400 }
                    }
                }

                ColumnLayout {
                    spacing: -1
                    Text {
                        text: root.isLaunching ? "WEITERE INSTANZ" : EzI18n.t("home_play", "SPIELEN")
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 19
                        font.bold: true
                        color: "#000000"
                        font.letterSpacing: 1.5
                        Layout.alignment: Qt.AlignHCenter
                    }
                    Text {
                        text: root.activeName + "  ·  " + root.activeLoader + " " + root.activeVersion
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 10
                        font.bold: true
                        color: "#B5000000"
                        Layout.alignment: Qt.AlignHCenter
                    }
                }
            }

            MouseArea {
                id: launchMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: {
                    if (profileController) {
                        profileController.launchActiveProfile()
                    }
                }
            }
        }

        Item { Layout.preferredHeight: 14 }

        // ── AUTH STATUS BADGE ──
        Rectangle {
            id: launchModePill
            visible: false
            Layout.alignment: Qt.AlignHCenter
            Layout.preferredHeight: 0
            Layout.preferredWidth: Math.min(root.width - 32, modeRow.implicitWidth + 22)
            radius: 13
            color: modeMouse.containsMouse ? "#1A261F" : "#111C15"
            border.color: EzTheme.accentGlow
            border.width: 1

            Behavior on color { ColorAnimation { duration: EzTheme.animNormal } }

            RowLayout {
                id: modeRow
                anchors.centerIn: parent
                spacing: 6

                Rectangle {
                    width: 6; height: 6; radius: 3
                    color: EzTheme.accent
                    SequentialAnimation on opacity {
                        loops: Animation.Infinite
                        NumberAnimation { to: 0.3; duration: 700; easing.type: Easing.InOutSine }
                        NumberAnimation { to: 1.0; duration: 700; easing.type: Easing.InOutSine }
                    }
                }

                Text {
                    text: "" + EzI18n.t("home_direct_badge", "SPIELBEREIT (NATIVE ENGINE)") + " (" + (typeof accountController !== "undefined" && accountController && accountController.isOnline ? "Microsoft Auth" : "Ready") + ")"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 9
                    font.bold: true
                    color: EzTheme.accentLight
                    font.letterSpacing: 0.5
                }

                Text {
                    text: "ⓘ"
                    font.pixelSize: 10
                    color: EzTheme.textMuted
                }
            }

            MouseArea {
                id: modeMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: infoPopup.opened ? infoPopup.close() : infoPopup.open()
            }

            Popup {
                id: infoPopup
                x: Math.round((launchModePill.width - 340) / 2)
                y: -height - 10
                width: 340
                padding: 16
                closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent

                background: Rectangle {
                    color: EzTheme.surface
                    radius: EzTheme.radius
                    border.color: EzTheme.accent
                    border.width: 1

                    Rectangle {
                        anchors.fill: parent; anchors.margins: -4
                        radius: parent.radius + 4; color: "transparent"
                        border.color: EzTheme.accentGlow; border.width: 2; opacity: 0.3
                    }
                }

                contentItem: ColumnLayout {
                    spacing: 10

                    Text {
                        text: EzI18n.t("home_direct_modal_title", "Online-Authentifizierung & Spielstart")
                        font.family: EzTheme.fontFamily; font.pixelSize: 14; font.bold: true
                        color: EzTheme.accentLight
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                    Text {
                        text: EzI18n.t("home_direct_modal_desc", "EzClient startet Minecraft blitzschnell direkt über Java mit deinem aus dem .minecraft-Ordner ausgelesenen Microsoft/Xbox-Token – komplett ohne den Minecraft Launcher zu öffnen!")
                        font.family: EzTheme.fontFamily; font.pixelSize: 12
                        color: EzTheme.text; wrapMode: Text.WordWrap; Layout.fillWidth: true
                    }

                    Rectangle {
                        Layout.fillWidth: true
                        Layout.preferredHeight: infoNoteText.implicitHeight + 16
                        radius: EzTheme.radiusSm
                        color: "#0B0F14"
                        border.color: EzTheme.borderLight; border.width: 1

                        Text {
                            id: infoNoteText
                            anchors.fill: parent; anchors.margins: 8
                            text: EzI18n.t("home_direct_modal_note", "100% Online-kompatibel für alle Multiplayer-Server (z.B. Hypixel), Realms und authentische Skins.")
                            font.family: EzTheme.fontFamily; font.pixelSize: 11
                            color: EzTheme.cyan; wrapMode: Text.WordWrap
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 3. BOTTOM STATUS BAR (Stats & Quick Info)
    // ─────────────────────────────────────────────────────────
    RowLayout {
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 16
        anchors.horizontalCenter: parent.horizontalCenter
        spacing: 20

        Repeater {
            model: (root.activeIsVanilla ? [] : [
                { iconSource: "icons/zap.svg", label: root.activeModsCount + " Mods", color: EzTheme.accentLight }
            ]).concat([
                { iconSource: "icons/cpu.svg", label: Math.round(root.activeRamMb / 1024 * 10) / 10 + " GB RAM", color: EzTheme.cyan },
                { iconSource: "icons/play.svg", label: root.activeLoader + " " + root.activeVersion, color: EzTheme.purple }
            ])

            Rectangle {
                height: 32
                width: statRow.implicitWidth + 20
                radius: 14
                color: "#0A0A0F80"
                border.color: EzTheme.border
                border.width: 1

                RowLayout {
                    id: statRow
                    anchors.centerIn: parent
                    spacing: 6

                    Image { source: modelData.iconSource; width: 14; height: 14; fillMode: Image.PreserveAspectFit }
                    Text {
                        text: modelData.label
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        font.bold: true
                        color: modelData.color
                    }
                }
            }
        }
    }

}
