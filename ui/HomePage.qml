import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import QtMultimedia
import "components"

Item {
    id: homeRoot

    Timer {
        interval: 10000
        repeat: true
        running: homeRoot.visible
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
        return /\.(mp4|webm|mov|mkv)$/i.test(p)
    }

    property var recentServers: []

    function refreshRecentServers() {
        if (typeof profileController !== "undefined" && profileController && profileController.getRecentServersForActiveProfile) {
            recentServers = profileController.getRecentServersForActiveProfile()
        } else {
            recentServers = []
        }
    }

    onVisibleChanged: if (visible) refreshRecentServers()
    Component.onCompleted: refreshRecentServers()

    Connections {
        target: typeof profileController !== "undefined" ? profileController : null
        function onActiveProfileChanged() { homeRoot.refreshRecentServers() }
        function onProfilesChanged() { homeRoot.refreshRecentServers() }
        function onSettingsChanged() { homeRoot.refreshRecentServers() }
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
        visible: !homeRoot.hasBackgroundVideo
        source: (!homeRoot.hasBackgroundVideo && typeof profileController !== "undefined" && profileController && profileController.customBackgroundImage) 
                ? homeRoot.formatImageUrl(profileController.customBackgroundImage) 
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
        source: homeRoot.hasBackgroundVideo && profileController ? homeRoot.formatImageUrl(profileController.customBackgroundImage) : ""
        loops: MediaPlayer.Infinite
        autoPlay: homeRoot.hasBackgroundVideo
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
        visible: homeRoot.hasBackgroundVideo
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
            property real startX: Math.random() * homeRoot.width
            property real startY: Math.random() * homeRoot.height
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
        width: Math.min(820, Math.max(480, homeRoot.width * 0.82))
        height: Math.max(540, homeRoot.height + 36)

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
            skinSource: homeRoot.skinTextureUrl
            capeSource: homeRoot.capeTextureUrl
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
                    text: homeRoot.accountUser
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
        width: Math.min(400, parent.width - 40)
        implicitWidth: Math.min(400, parent.width - 40)
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
            width: Math.min(500, Math.max(280, homeRoot.width - 100))
            height: Math.min(430, Math.max(280, homeRoot.height - 250))

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
                        text: homeRoot.accountUser
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
            id: profPill
            Layout.alignment: Qt.AlignHCenter
            Layout.topMargin: 0
            Layout.preferredHeight: 40
            Layout.preferredWidth: Math.min(homeRoot.width - 32, profPillRow.implicitWidth + 28)
            implicitWidth: Layout.preferredWidth
            implicitHeight: 40
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
                    text: (homeRoot.hasProfile ? homeRoot.activeName : EzI18n.t("home_default_profile", "Standard Profil"))
                          + "  ·  " + homeRoot.activeLoader + " " + homeRoot.activeVersion
                          + (homeRoot.activeIsVanilla ? "" : "  ·  " + homeRoot.activeModsCount + " Mods")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    font.bold: true
                    color: EzTheme.text
                    elide: Text.ElideRight
                    Layout.maximumWidth: Math.max(140, homeRoot.width - 70)
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
            Layout.preferredWidth: Math.min(400, homeRoot.width - 40)
            Layout.preferredHeight: 74
            implicitWidth: Layout.preferredWidth
            implicitHeight: 74
            radius: 16

            scale: launchMouse.pressed ? 0.95 : (launchMouse.containsMouse ? 1.04 : 1.0)
            Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }

            color: homeRoot.isLaunching ? EzTheme.warning : (launchMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent)

            border.color: homeRoot.isLaunching ? "#FDE68A" : (launchMouse.containsMouse ? "#5AEEA0" : "#22C96E50")
            border.width: 2

            // Outer glow effect with breathing pulse
            Rectangle {
                anchors.fill: parent
                anchors.margins: -5
                radius: parent.radius + 5
                color: "transparent"
                border.color: homeRoot.isLaunching ? "#F59E0B" : EzTheme.accent
                border.width: 2.5
                opacity: launchMouse.containsMouse ? 0.9 : 0.4
                Behavior on opacity { NumberAnimation { duration: EzTheme.animNormal } }

                SequentialAnimation on scale {
                    loops: Animation.Infinite
                    running: !homeRoot.isLaunching
                    NumberAnimation { from: 1.0; to: 1.03; duration: 1600; easing.type: Easing.InOutSine }
                    NumberAnimation { from: 1.03; to: 1.0; duration: 1600; easing.type: Easing.InOutSine }
                }
                SequentialAnimation on opacity {
                    loops: Animation.Infinite
                    running: !homeRoot.isLaunching && !launchMouse.containsMouse
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
                    visible: !homeRoot.isLaunching
                }

                Rectangle {
                    width: 16; height: 16; radius: 8
                    color: "#000000"
                    visible: homeRoot.isLaunching
                    SequentialAnimation on scale {
                        loops: Animation.Infinite
                        NumberAnimation { to: 0.4; duration: 400 }
                        NumberAnimation { to: 1.0; duration: 400 }
                    }
                }

                ColumnLayout {
                    spacing: -1
                    Text {
                        text: homeRoot.isLaunching ? "WEITERE INSTANZ" : EzI18n.t("home_play", "SPIELEN")
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 19
                        font.bold: true
                        color: "#000000"
                        font.letterSpacing: 1.5
                        Layout.alignment: Qt.AlignHCenter
                    }
                    Text {
                        text: homeRoot.activeName + "  ·  " + homeRoot.activeLoader + " " + homeRoot.activeVersion
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
            Layout.preferredWidth: Math.min(homeRoot.width - 32, modeRow.implicitWidth + 22)
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
            model: (homeRoot.activeIsVanilla ? [] : [
                { iconSource: "icons/zap.svg", label: homeRoot.activeModsCount + " Mods", color: EzTheme.accentLight }
            ]).concat([
                { iconSource: "icons/cpu.svg", label: Math.round(homeRoot.activeRamMb / 1024 * 10) / 10 + " GB RAM", color: EzTheme.cyan },
                { iconSource: "icons/play.svg", label: homeRoot.activeLoader + " " + homeRoot.activeVersion, color: EzTheme.purple }
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

    // ─────────────────────────────────────────────────────────
    // 4. RECENT SERVERS FLOATING CARD (Right Corner / Instant Join)
    // ─────────────────────────────────────────────────────────
    Item {
        id: recentServersWidget
        z: 20
        anchors.right: parent.right
        anchors.rightMargin: 24
        y: Math.max(40, Math.round(parent.height * 0.22))
        width: isCollapsed ? 38 : 276
        height: isCollapsed ? 38 : (serverContentCol.implicitHeight + 24)

        property bool isAddingServer: false
        readonly property bool isEnabled: typeof profileController !== "undefined" && profileController && profileController.showRecentServersHome
        readonly property bool isCollapsed: typeof profileController !== "undefined" && profileController ? profileController.recentServersHomeHidden : false
        readonly property bool hasServers: homeRoot.recentServers && homeRoot.recentServers.length > 0

        visible: isEnabled

        // Collapsed View: Floating Eye Button Badge
        Rectangle {
            id: collapsedBtn
            anchors.right: parent.right
            visible: recentServersWidget.isCollapsed
            width: 38
            height: 38
            radius: 19
            color: collapsedMouse.containsMouse ? "#222D3A" : "#D90B0F15"
            border.color: collapsedMouse.containsMouse ? EzTheme.accent : "#2C3947"
            border.width: 1
            scale: collapsedMouse.pressed ? 0.92 : (collapsedMouse.containsMouse ? 1.08 : 1.0)

            Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }
            Behavior on color { ColorAnimation { duration: 150 } }
            Behavior on border.color { ColorAnimation { duration: 150 } }

            Image {
                anchors.centerIn: parent
                source: "icons/eye-off.svg"
                width: 18
                height: 18
                fillMode: Image.PreserveAspectFit
                opacity: collapsedMouse.containsMouse ? 1.0 : 0.75
            }

            ToolTip.visible: collapsedMouse.containsMouse
            ToolTip.text: "Server-Schnellstart einblenden"
            ToolTip.delay: 300

            MouseArea {
                id: collapsedMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: {
                    if (profileController) profileController.setRecentServersHomeHidden(false)
                }
            }
        }

        // Expanded View: Sleek Glassmorphic Card
        Rectangle {
            id: expandedCard
            anchors.right: parent.right
            visible: !recentServersWidget.isCollapsed
            width: 276
            height: serverContentCol.implicitHeight + 24
            radius: 14
            color: "#E60B0F15"
            border.color: "#283543"
            border.width: 1

            // Subtle top highlight gradient line
            Rectangle {
                anchors.top: parent.top
                anchors.left: parent.left
                anchors.right: parent.right
                anchors.margins: 1
                height: 1
                radius: 1
                color: "#40FFFFFF"
                opacity: 0.15
            }

            ColumnLayout {
                id: serverContentCol
                anchors.fill: parent
                anchors.margins: 12
                spacing: 8

                // Header Row
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 6

                    Image {
                        source: "icons/globe.svg"
                        Layout.preferredWidth: 14
                        Layout.preferredHeight: 14
                        fillMode: Image.PreserveAspectFit
                        opacity: 0.9
                    }

                    Text {
                        text: "SERVER"
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 11
                        font.bold: true
                        font.letterSpacing: 1.0
                        color: EzTheme.text
                        Layout.fillWidth: true
                    }

                    // Plus button: Add custom server
                    Rectangle {
                        width: 22
                        height: 22
                        radius: 11
                        color: recentServersWidget.isAddingServer ? EzTheme.accent : (plusMouse.containsMouse ? "#243242" : "#161F2A")
                        border.color: recentServersWidget.isAddingServer ? EzTheme.accentLight : (plusMouse.containsMouse ? EzTheme.accent : "#2A3644")
                        border.width: 1

                        Image {
                            anchors.centerIn: parent
                            source: recentServersWidget.isAddingServer ? "icons/x.svg" : "icons/plus.svg"
                            width: 10
                            height: 10
                            fillMode: Image.PreserveAspectFit
                            opacity: plusMouse.containsMouse || recentServersWidget.isAddingServer ? 1.0 : 0.75
                        }

                        ToolTip.visible: plusMouse.containsMouse
                        ToolTip.text: recentServersWidget.isAddingServer ? "Schließen" : "Server hinzufügen"
                        ToolTip.delay: 300

                        MouseArea {
                            id: plusMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                recentServersWidget.isAddingServer = !recentServersWidget.isAddingServer
                                if (recentServersWidget.isAddingServer) {
                                    ipInput.text = ""
                                    nameInput.text = ""
                                    ipInput.forceActiveFocus()
                                }
                            }
                        }
                    }

                    // Refresh Button: Ping all servers, reload player counts, status & favicons
                    Rectangle {
                        id: refreshListBtn
                        width: 22
                        height: 22
                        radius: 11
                        color: refListMouse.containsMouse ? "#243242" : "transparent"
                        border.color: refListMouse.containsMouse ? "#3B4D60" : "transparent"
                        border.width: 1

                        readonly property bool isSpinning: Boolean(typeof profileController !== "undefined" && profileController && profileController.isPingingServers)

                        Image {
                            id: refIcon
                            anchors.centerIn: parent
                            source: "icons/refresh-cw.svg"
                            width: 11
                            height: 11
                            fillMode: Image.PreserveAspectFit
                            opacity: refListMouse.containsMouse || refreshListBtn.isSpinning ? 1.0 : 0.75

                            RotationAnimation on rotation {
                                running: refreshListBtn.isSpinning
                                loops: Animation.Infinite
                                from: 0
                                to: 360
                                duration: 800
                            }
                        }

                        ToolTip.visible: Boolean(refListMouse.containsMouse)
                        ToolTip.text: "Serverliste, Status & Bilder aktualisieren"
                        ToolTip.delay: 300

                        MouseArea {
                            id: refListMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController) {
                                    profileController.refreshHomeServers()
                                }
                            }
                        }
                    }

                    // Sparkles button: Toggle suggested servers on/off (Right-click: restore all)
                    Rectangle {
                        width: 22
                        height: 22
                        radius: 11
                        readonly property bool suggestedOn: typeof profileController !== "undefined" && profileController ? profileController.showSuggestedServersHome : true
                        color: sugMouse.containsMouse ? "#243242" : "transparent"
                        border.color: sugMouse.containsMouse ? "#3B4D60" : "transparent"
                        border.width: 1

                        Image {
                            anchors.centerIn: parent
                            source: "icons/sparkles.svg"
                            width: 11
                            height: 11
                            fillMode: Image.PreserveAspectFit
                            opacity: parent.suggestedOn ? (sugMouse.containsMouse ? 1.0 : 0.85) : 0.3
                        }

                        ToolTip.visible: Boolean(sugMouse.containsMouse)
                        ToolTip.text: parent.suggestedOn ? "Vorgeschlagene Server ausblenden (Rechtsklick: Wiederherstellen)" : "Vorgeschlagene Server einblenden"
                        ToolTip.delay: 300

                        MouseArea {
                            id: sugMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            acceptedButtons: Qt.LeftButton | Qt.RightButton
                            cursorShape: Qt.PointingHandCursor
                            onClicked: function(mouse) {
                                if (mouse.button === Qt.RightButton) {
                                    if (profileController) {
                                        profileController.resetSuggestedHomeServers()
                                    }
                                } else {
                                    if (profileController) {
                                        profileController.setShowSuggestedServersHome(!parent.suggestedOn)
                                    }
                                }
                            }
                        }
                    }

                    // Reset / Restore hidden suggestions button (appears whenever suggestions have been hidden/removed)
                    Rectangle {
                        visible: typeof profileController !== "undefined" && profileController && profileController.hasHiddenSuggestedServers
                        width: 22
                        height: 22
                        radius: 11
                        color: resetMouse.containsMouse ? "#243242" : "transparent"
                        border.color: resetMouse.containsMouse ? "#3B4D60" : "transparent"
                        border.width: 1

                        Image {
                            anchors.centerIn: parent
                            source: "icons/eye-off.svg"
                            width: 11
                            height: 11
                            fillMode: Image.PreserveAspectFit
                            opacity: resetMouse.containsMouse ? 1.0 : 0.75
                        }

                        ToolTip.visible: Boolean(resetMouse.containsMouse)
                        ToolTip.text: "Ausgeblendete Vorschläge wiederherstellen"
                        ToolTip.delay: 300

                        MouseArea {
                            id: resetMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController) {
                                    profileController.resetSuggestedHomeServers()
                                }
                            }
                        }
                    }

                    // Eye button: Collapse/hide quickstart
                    Rectangle {
                        width: 22
                        height: 22
                        radius: 11
                        color: eyeMouse.containsMouse ? "#243242" : "transparent"
                        border.color: eyeMouse.containsMouse ? "#3B4D60" : "transparent"
                        border.width: 1

                        Image {
                            anchors.centerIn: parent
                            source: "icons/eye.svg"
                            width: 12
                            height: 12
                            fillMode: Image.PreserveAspectFit
                            opacity: eyeMouse.containsMouse ? 1.0 : 0.6
                        }

                        ToolTip.visible: eyeMouse.containsMouse
                        ToolTip.text: "Schnellstart ausblenden"
                        ToolTip.delay: 300

                        MouseArea {
                            id: eyeMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController) profileController.setRecentServersHomeHidden(true)
                            }
                        }
                    }
                }

                // Separator
                Rectangle {
                    Layout.fillWidth: true
                    height: 1
                    color: "#1E2733"
                }

                // Inline Add Server Drawer
                Rectangle {
                    id: addServerDrawer
                    Layout.fillWidth: true
                    visible: recentServersWidget.isAddingServer
                    Layout.preferredHeight: visible ? (addServerCol.implicitHeight + 16) : 0
                    radius: 10
                    color: "#161F2A"
                    border.color: EzTheme.accent
                    border.width: 1
                    clip: true

                    ColumnLayout {
                        id: addServerCol
                        anchors.fill: parent
                        anchors.margins: 10
                        spacing: 8

                        Text {
                            text: "SERVER HINZUFÜGEN"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 10
                            font.bold: true
                            color: EzTheme.accentLight
                        }

                        // Server IP Input
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 28
                            radius: 6
                            color: "#0B1017"
                            border.color: ipInput.activeFocus ? EzTheme.accent : "#222F3E"
                            border.width: 1

                            TextInput {
                                id: ipInput
                                anchors.fill: parent
                                anchors.leftMargin: 8
                                anchors.rightMargin: 8
                                verticalAlignment: TextInput.AlignVCenter
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.text
                                selectByMouse: true
                                clip: true

                                Text {
                                    text: "Adresse (z. B. hypixel.net)"
                                    font.family: parent.font.family
                                    font.pixelSize: parent.font.pixelSize
                                    color: "#5A6E82"
                                    visible: !parent.text && !parent.activeFocus
                                    anchors.left: parent.left
                                    anchors.right: parent.right
                                    anchors.verticalCenter: parent.verticalCenter
                                    elide: Text.ElideRight
                                }

                                onAccepted: addServerDrawer.doSaveServer()
                            }
                        }

                        // Server Name Input (Optional)
                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 28
                            radius: 6
                            color: "#0B1017"
                            border.color: nameInput.activeFocus ? EzTheme.accent : "#222F3E"
                            border.width: 1

                            TextInput {
                                id: nameInput
                                anchors.fill: parent
                                anchors.leftMargin: 8
                                anchors.rightMargin: 8
                                verticalAlignment: TextInput.AlignVCenter
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.text
                                selectByMouse: true
                                clip: true

                                Text {
                                    text: "Name (optional)"
                                    font.family: parent.font.family
                                    font.pixelSize: parent.font.pixelSize
                                    color: "#5A6E82"
                                    visible: !parent.text && !parent.activeFocus
                                    anchors.left: parent.left
                                    anchors.right: parent.right
                                    anchors.verticalCenter: parent.verticalCenter
                                    elide: Text.ElideRight
                                }

                                onAccepted: addServerDrawer.doSaveServer()
                            }
                        }

                        // Action Buttons: Abbrechen / Hinzufügen
                        RowLayout {
                            Layout.fillWidth: true
                            spacing: 6

                            Rectangle {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 24
                                radius: 6
                                color: cancelBtnMouse.containsMouse ? "#2A3644" : "#1B2430"
                                border.color: "#2E3B4B"
                                border.width: 1

                                Text {
                                    anchors.centerIn: parent
                                    text: "Abbrechen"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    color: EzTheme.textMuted
                                }

                                MouseArea {
                                    id: cancelBtnMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        recentServersWidget.isAddingServer = false
                                        ipInput.text = ""
                                        nameInput.text = ""
                                    }
                                }
                            }

                            Rectangle {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 24
                                radius: 6
                                readonly property bool canAdd: ipInput.text.trim().length > 0
                                color: canAdd ? (addBtnMouse.containsMouse ? EzTheme.accentLight : EzTheme.accent) : "#202832"
                                opacity: canAdd ? 1.0 : 0.5

                                Text {
                                    anchors.centerIn: parent
                                    text: "Speichern"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: "#FFFFFF"
                                }

                                MouseArea {
                                    id: addBtnMouse
                                    anchors.fill: parent
                                    enabled: parent.canAdd
                                    hoverEnabled: true
                                    cursorShape: parent.canAdd ? Qt.PointingHandCursor : Qt.ArrowCursor
                                    onClicked: addServerDrawer.doSaveServer()
                                }
                            }
                        }
                    }

                    function doSaveServer() {
                        var ip = ipInput.text.trim()
                        var name = nameInput.text.trim()
                        if (ip.length > 0 && typeof profileController !== "undefined" && profileController) {
                            profileController.addCustomHomeServer(name, ip)
                            recentServersWidget.isAddingServer = false
                            ipInput.text = ""
                            nameInput.text = ""
                            homeRoot.refreshRecentServers()
                        }
                    }
                }

                // Servers List Repeater
                Repeater {
                    model: homeRoot.recentServers

                    Rectangle {
                        id: serverRowCard
                        Layout.fillWidth: true
                        Layout.preferredHeight: 52
                        radius: 10
                        readonly property bool cardHovered: Boolean(srvMouse.containsMouse || statusHoverMouse.containsMouse || addCustomMouse.containsMouse || delMouse.containsMouse || upMouse.containsMouse || downMouse.containsMouse || playHoverMouse.containsMouse)
                        color: cardHovered ? "#1A2533" : "#0F1620"
                        border.color: cardHovered ? EzTheme.accent : "#1E2835"
                        border.width: 1
                        scale: (srvMouse.pressed || statusHoverMouse.pressed) ? 0.98 : (cardHovered ? 1.01 : 1.0)

                        Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }
                        Behavior on color { ColorAnimation { duration: 120 } }
                        Behavior on border.color { ColorAnimation { duration: 120 } }

                        RowLayout {
                            anchors.fill: parent
                            anchors.margins: 6
                            spacing: 8

                            // Server Icon (36x36)
                            Rectangle {
                                Layout.preferredWidth: 36
                                Layout.preferredHeight: 36
                                radius: 8
                                color: "#0A0D12"
                                border.color: "#25313D"
                                border.width: 1
                                clip: true

                                Image {
                                    anchors.fill: parent
                                    source: (modelData.icon && modelData.icon.length > 0) ? modelData.icon : ""
                                    visible: modelData.icon && modelData.icon.length > 0
                                    fillMode: Image.PreserveAspectFit
                                    smooth: false
                                }

                                Image {
                                    anchors.centerIn: parent
                                    source: "icons/globe.svg"
                                    width: 18
                                    height: 18
                                    fillMode: Image.PreserveAspectFit
                                    visible: !modelData.icon || modelData.icon.length === 0
                                    opacity: 0.6
                                }
                            }

                            // Server Name & IP
                            ColumnLayout {
                                Layout.fillWidth: true
                                spacing: 2

                                RowLayout {
                                    Layout.fillWidth: true
                                    spacing: 4

                                    Text {
                                        text: modelData.name ? modelData.name : modelData.ip
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 11
                                        font.bold: true
                                        color: serverRowCard.cardHovered ? EzTheme.accentLight : EzTheme.text
                                        elide: Text.ElideRight
                                        Layout.fillWidth: true
                                    }

                                    // Yellow Star for Suggested Servers
                                    Rectangle {
                                        visible: modelData.is_custom !== true
                                        Layout.preferredWidth: 16
                                        Layout.preferredHeight: 16
                                        radius: 8
                                        color: "#2C2408"
                                        border.color: "#EAB308"
                                        border.width: 1

                                        Text {
                                            anchors.centerIn: parent
                                            anchors.verticalCenterOffset: -1
                                            text: "★"
                                            font.pixelSize: 10
                                            color: "#FACC15"
                                        }

                                        ToolTip.visible: Boolean(starTipMouse.containsMouse)
                                        ToolTip.text: "Vorschlag (zuletzt gespielt)"
                                        ToolTip.delay: 300

                                        MouseArea {
                                            id: starTipMouse
                                            anchors.fill: parent
                                            hoverEnabled: true
                                        }
                                    }
                                }

                                RowLayout {
                                    Layout.fillWidth: true
                                    spacing: 6

                                    Text {
                                        text: modelData.ip
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.textMuted
                                        elide: Text.ElideRight
                                        Layout.fillWidth: true
                                    }

                                    // Online / Player Count Status Badge
                                    Rectangle {
                                        id: statusBadge
                                        Layout.preferredHeight: 16
                                        Layout.preferredWidth: statusRow.implicitWidth + 8
                                        radius: 4
                                        color: {
                                            if (modelData.online === true) return "#10281E"
                                            if (modelData.online === false) return "#2D1418"
                                            return "#18202A"
                                        }
                                        border.color: {
                                            if (modelData.online === true) return "#15803D"
                                            if (modelData.online === false) return "#991B1B"
                                            return "#334155"
                                        }
                                        border.width: 1

                                        RowLayout {
                                            id: statusRow
                                            anchors.centerIn: parent
                                            spacing: 4

                                            // Status indicator dot
                                            Rectangle {
                                                Layout.preferredWidth: 6
                                                Layout.preferredHeight: 6
                                                radius: 3
                                                color: {
                                                    if (modelData.online === true) return "#22C55E"
                                                    if (modelData.online === false) return "#EF4444"
                                                    return "#94A3B8"
                                                }
                                            }

                                            Text {
                                                text: {
                                                    if (modelData.online === true) {
                                                        return (modelData.players_online !== undefined ? modelData.players_online : 0) +
                                                               " / " +
                                                               (modelData.players_max !== undefined ? modelData.players_max : 0)
                                                    }
                                                    if (modelData.online === false) return "Offline"
                                                    return "…"
                                                }
                                                font.family: EzTheme.fontFamily
                                                font.pixelSize: 9
                                                font.bold: true
                                                color: {
                                                    if (modelData.online === true) return "#86EFAC"
                                                    if (modelData.online === false) return "#FCA5A5"
                                                    return "#94A3B8"
                                                }
                                            }
                                        }

                                        ToolTip.visible: Boolean(statusHoverMouse.containsMouse)
                                        ToolTip.delay: 200
                                        ToolTip.text: {
                                            if (modelData.online === false) {
                                                return "Server ist offline oder nicht erreichbar (" + modelData.ip + ")"
                                            }
                                            if (modelData.online !== true) {
                                                return "Server-Status wird abgerufen…"
                                            }
                                            var txt = "Online: " + (modelData.players_online || 0) + " / " + (modelData.players_max || 0) + " Spieler"
                                            if (modelData.latency_ms && modelData.latency_ms > 0) {
                                                txt += " (" + modelData.latency_ms + " ms)"
                                            }
                                            if (modelData.player_sample && modelData.player_sample.length > 0) {
                                                txt += "\n\nAnwesende Spieler:"
                                                for (var i = 0; i < modelData.player_sample.length; ++i) {
                                                    txt += "\n • " + modelData.player_sample[i]
                                                }
                                                if (modelData.players_online > modelData.player_sample.length) {
                                                    var diff = modelData.players_online - modelData.player_sample.length
                                                    txt += "\n … und " + diff + " weitere"
                                                }
                                            } else if (modelData.players_online > 0) {
                                                txt += "\n(Server blendet Spielernamen aus)"
                                            }
                                            return txt
                                        }

                                        MouseArea {
                                            id: statusHoverMouse
                                            anchors.fill: parent
                                            hoverEnabled: true
                                            cursorShape: Qt.PointingHandCursor
                                            onClicked: {
                                                if (profileController) {
                                                    profileController.launchActiveProfileWithServer(modelData.ip)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Move Up Button (▲) - only for custom servers
                            Rectangle {
                                id: moveUpBtn
                                visible: modelData.is_custom === true
                                Layout.preferredWidth: 18
                                Layout.preferredHeight: 18
                                radius: 9
                                readonly property bool canMoveUp: Boolean(modelData && !modelData.is_first_custom)
                                enabled: canMoveUp
                                color: upMouse.containsMouse ? "#2A3644" : "transparent"
                                border.color: upMouse.containsMouse ? EzTheme.accent : "#222D39"
                                border.width: 1
                                opacity: serverRowCard.cardHovered ? (canMoveUp ? 1.0 : 0.3) : 0.0

                                Behavior on opacity { NumberAnimation { duration: 120 } }

                                Text {
                                    anchors.centerIn: parent
                                    text: "▲"
                                    font.pixelSize: 8
                                    color: upMouse.containsMouse ? EzTheme.accentLight : EzTheme.textMuted
                                }

                                ToolTip.visible: Boolean(upMouse.containsMouse && moveUpBtn.canMoveUp)
                                ToolTip.text: "Nach oben verschieben"
                                ToolTip.delay: 300

                                MouseArea {
                                    id: upMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: moveUpBtn.canMoveUp ? Qt.PointingHandCursor : Qt.ArrowCursor
                                    onClicked: {
                                        if (moveUpBtn.canMoveUp && profileController) {
                                            profileController.moveCustomHomeServer(modelData.ip, -1)
                                        }
                                    }
                                }
                            }

                            // Move Down Button (▼) - only for custom servers
                            Rectangle {
                                id: moveDownBtn
                                visible: modelData.is_custom === true
                                Layout.preferredWidth: 18
                                Layout.preferredHeight: 18
                                radius: 9
                                readonly property bool canMoveDown: Boolean(modelData && !modelData.is_last_custom)
                                enabled: canMoveDown
                                color: downMouse.containsMouse ? "#2A3644" : "transparent"
                                border.color: downMouse.containsMouse ? EzTheme.accent : "#222D39"
                                border.width: 1
                                opacity: serverRowCard.cardHovered ? (canMoveDown ? 1.0 : 0.3) : 0.0

                                Behavior on opacity { NumberAnimation { duration: 120 } }

                                Text {
                                    anchors.centerIn: parent
                                    text: "▼"
                                    font.pixelSize: 8
                                    color: downMouse.containsMouse ? EzTheme.accentLight : EzTheme.textMuted
                                }

                                ToolTip.visible: Boolean(downMouse.containsMouse && moveDownBtn.canMoveDown)
                                ToolTip.text: "Nach unten verschieben"
                                ToolTip.delay: 300

                                MouseArea {
                                    id: downMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: moveDownBtn.canMoveDown ? Qt.PointingHandCursor : Qt.ArrowCursor
                                    onClicked: {
                                        if (moveDownBtn.canMoveDown && profileController) {
                                            profileController.moveCustomHomeServer(modelData.ip, 1)
                                        }
                                    }
                                }
                            }

                            // Add to Custom Servers Button (+) - only for suggested servers
                            Rectangle {
                                id: addCustomBtn
                                visible: modelData.is_custom !== true
                                Layout.preferredWidth: 20
                                Layout.preferredHeight: 20
                                radius: 10
                                color: addCustomMouse.containsMouse ? "#143322" : "transparent"
                                border.color: addCustomMouse.containsMouse ? "#22C55E" : "transparent"
                                border.width: 1
                                opacity: serverRowCard.cardHovered ? 1.0 : 0.0

                                Behavior on opacity { NumberAnimation { duration: 120 } }

                                Image {
                                    anchors.centerIn: parent
                                    source: "icons/plus.svg"
                                    width: 10
                                    height: 10
                                    fillMode: Image.PreserveAspectFit
                                    opacity: addCustomMouse.containsMouse ? 1.0 : 0.7
                                }

                                ToolTip.visible: Boolean(addCustomMouse.containsMouse)
                                ToolTip.text: "Zu meinen Servern hinzufügen"
                                ToolTip.delay: 300

                                MouseArea {
                                    id: addCustomMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        if (profileController) {
                                            profileController.addCustomHomeServer(modelData.name || modelData.ip, modelData.ip)
                                        }
                                    }
                                }
                            }

                            // Remove / Delete Button (✕)
                            Rectangle {
                                Layout.preferredWidth: 20
                                Layout.preferredHeight: 20
                                radius: 10
                                color: delMouse.containsMouse ? "#3D141A" : "transparent"
                                border.color: delMouse.containsMouse ? "#E53E3E" : "transparent"
                                border.width: 1
                                opacity: serverRowCard.cardHovered ? 1.0 : 0.0

                                Behavior on opacity { NumberAnimation { duration: 120 } }

                                Image {
                                    anchors.centerIn: parent
                                    source: "icons/x.svg"
                                    width: 10
                                    height: 10
                                    fillMode: Image.PreserveAspectFit
                                    opacity: delMouse.containsMouse ? 1.0 : 0.6
                                }

                                ToolTip.visible: Boolean(delMouse.containsMouse)
                                ToolTip.text: modelData.is_custom ? "Server löschen" : "Vorschlag ausblenden"
                                ToolTip.delay: 300

                                MouseArea {
                                    id: delMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        if (profileController) {
                                            profileController.removeHomeServer(modelData.ip, modelData.is_custom === true)
                                        }
                                    }
                                }
                            }

                            // Mini Instant-Join Play Action Button
                            Rectangle {
                                Layout.preferredWidth: 26
                                Layout.preferredHeight: 26
                                radius: 13
                                color: (playHoverMouse.containsMouse || serverRowCard.cardHovered) ? EzTheme.accent : "#18212C"
                                border.color: (playHoverMouse.containsMouse || serverRowCard.cardHovered) ? EzTheme.accentLight : "#253240"
                                border.width: 1

                                Image {
                                    anchors.centerIn: parent
                                    anchors.horizontalCenterOffset: 1
                                    source: "icons/play.svg"
                                    width: 10
                                    height: 10
                                    fillMode: Image.PreserveAspectFit
                                    opacity: (playHoverMouse.containsMouse || serverRowCard.cardHovered) ? 1.0 : 0.7
                                }

                                ToolTip.visible: Boolean(playHoverMouse.containsMouse)
                                ToolTip.text: "Sofort beitreten: " + modelData.ip
                                ToolTip.delay: 300

                                MouseArea {
                                    id: playHoverMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        if (profileController) {
                                            profileController.launchActiveProfileWithServer(modelData.ip)
                                        }
                                    }
                                }
                            }
                        }

                        MouseArea {
                            id: srvMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            z: -1
                            onClicked: {
                                if (profileController) {
                                    profileController.launchActiveProfileWithServer(modelData.ip)
                                }
                            }
                        }
                    }
                }

                // Empty State Card (shown when no servers in list and not currently adding)
                Rectangle {
                    visible: (!homeRoot.recentServers || homeRoot.recentServers.length === 0) && !recentServersWidget.isAddingServer
                    Layout.fillWidth: true
                    Layout.preferredHeight: 88
                    radius: 10
                    color: "#0F1620"
                    border.color: "#1E2835"
                    border.width: 1

                    ColumnLayout {
                        anchors.fill: parent
                        anchors.margins: 10
                        spacing: 8

                        Text {
                            text: "Keine Server vorhanden"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 11
                            font.bold: true
                            color: EzTheme.textMuted
                            Layout.alignment: Qt.AlignHCenter
                        }

                        RowLayout {
                            Layout.alignment: Qt.AlignHCenter
                            spacing: 8

                            Rectangle {
                                Layout.preferredWidth: 100
                                Layout.preferredHeight: 24
                                radius: 6
                                color: emptyAddMouse.containsMouse ? EzTheme.accent : "#1A2533"
                                border.color: EzTheme.accent
                                border.width: 1

                                Text {
                                    anchors.centerIn: parent
                                    text: "+ Hinzufügen"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: "#FFFFFF"
                                }

                                MouseArea {
                                    id: emptyAddMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: recentServersWidget.isAddingServer = true
                                }
                            }

                            Rectangle {
                                Layout.preferredWidth: 110
                                Layout.preferredHeight: 24
                                radius: 6
                                color: restoreMouse.containsMouse ? "#2A3644" : "#141D26"
                                border.color: "#283543"
                                border.width: 1

                                Text {
                                    anchors.centerIn: parent
                                    text: "Vorschläge laden"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    color: EzTheme.textMuted
                                }

                                MouseArea {
                                    id: restoreMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: {
                                        if (profileController) {
                                            profileController.resetSuggestedHomeServers()
                                            homeRoot.refreshRecentServers()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
