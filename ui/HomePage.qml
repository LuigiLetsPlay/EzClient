import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root

    readonly property bool hasProfile: typeof profileController !== "undefined" && profileController && profileController.activeName !== "No Profile" && profileController.activeName !== ""
    readonly property string activeName: typeof profileController !== "undefined" && profileController ? profileController.activeName : ""
    readonly property string activeVersion: typeof profileController !== "undefined" && profileController ? profileController.activeVersion : "26.2"
    readonly property string activeLoader: typeof profileController !== "undefined" && profileController ? profileController.activeLoader : "Fabric"
    readonly property int activeModsCount: typeof profileController !== "undefined" && profileController ? profileController.activeModsCount : 0
    readonly property int activeRamMb: typeof profileController !== "undefined" && profileController ? profileController.activeRamMb : 4096
    readonly property bool isLaunching: typeof profileController !== "undefined" && profileController ? profileController.isLaunching : false

    readonly property string accountUser: typeof accountController !== "undefined" && accountController ? accountController.username : "Player"
    readonly property string bodyUrl: typeof accountController !== "undefined" && accountController ? accountController.bodyUrl : ""

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
        model: 12
        Rectangle {
            property real startX: Math.random() * root.width
            property real startY: Math.random() * root.height
            property real duration: 4000 + Math.random() * 6000

            x: startX
            width: 2 + Math.random() * 3
            height: width
            radius: width / 2
            color: EzTheme.accent
            opacity: 0.08 + Math.random() * 0.12

            SequentialAnimation on y {
                loops: Animation.Infinite
                NumberAnimation { from: startY; to: startY - 80 - Math.random() * 120; duration: duration; easing.type: Easing.InOutSine }
                NumberAnimation { from: startY - 80 - Math.random() * 120; to: startY; duration: duration; easing.type: Easing.InOutSine }
            }
            SequentialAnimation on opacity {
                loops: Animation.Infinite
                NumberAnimation { to: 0.02; duration: duration * 0.8 }
                NumberAnimation { to: 0.08 + Math.random() * 0.12; duration: duration * 0.8 }
            }
        }
    }

    // Ambient radial glow behind center
    Rectangle {
        anchors.centerIn: parent
        anchors.verticalCenterOffset: -30
        width: 400
        height: 400
        radius: 200
        color: "transparent"

        Rectangle {
            anchors.centerIn: parent
            width: 280
            height: 280
            radius: 140
            color: EzTheme.accent
            opacity: 0.08
            SequentialAnimation on opacity {
                loops: Animation.Infinite
                NumberAnimation { to: 0.16; duration: 3000; easing.type: Easing.InOutSine }
                NumberAnimation { to: 0.08; duration: 3000; easing.type: Easing.InOutSine }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 2. MAIN CENTER HERO CONTENT
    // ─────────────────────────────────────────────────────────
    ColumnLayout {
        anchors.centerIn: parent
        spacing: 0

        // ── Welcome Text ──
        Text {
            Layout.alignment: Qt.AlignHCenter
            text: EzI18n.t("home_welcome", "Willkommen zurück") + ","
            font.family: EzTheme.fontFamily
            font.pixelSize: 14
            color: EzTheme.textSecondary
            opacity: 0.8
        }
        Text {
            Layout.alignment: Qt.AlignHCenter
            text: root.accountUser
            font.family: EzTheme.mcFontFamily
            font.pixelSize: 22
            font.bold: true
            color: EzTheme.text
        }

        Item { height: 10 }

        // ── 3D Minecraft Character Render ──
        Item {
            Layout.alignment: Qt.AlignHCenter
            width: 220
            height: 260

            property real currentRotation: 0
            property real dragStartX: 0

            // Ambient Shadow under player feet
            Rectangle {
                anchors.bottom: parent.bottom
                anchors.bottomMargin: 8
                anchors.horizontalCenter: parent.horizontalCenter
                width: 120
                height: 16
                radius: 8
                color: "#000000"
                opacity: 0.5
                scale: charHover.containsMouse ? 1.06 : 1.0
                Behavior on scale { NumberAnimation { duration: 200 } }
            }

            // Full-body Character
            Image {
                id: skinBody
                anchors.centerIn: parent
                anchors.verticalCenterOffset: charHover.containsMouse ? -5 : 0
                height: 230
                source: root.bodyUrl !== "" ? root.bodyUrl : "https://mc-heads.net/body/Steve/360"
                fillMode: Image.PreserveAspectFit
                smooth: true
                cache: false

                Behavior on anchors.verticalCenterOffset {
                    NumberAnimation { duration: 250; easing.type: Easing.OutCubic }
                }

                // Interactive 3D tilt and smooth rotation
                rotation: parent.currentRotation
                scale: charHover.containsMouse ? 1.04 : 1.0
                Behavior on scale { NumberAnimation { duration: 200 } }
                Behavior on rotation {
                    enabled: !charHover.pressed
                    NumberAnimation { duration: 300; easing.type: Easing.OutCubic }
                }
            }

            // Fallback avatar
            Rectangle {
                anchors.centerIn: parent
                width: 72; height: 72; radius: EzTheme.radius
                color: EzTheme.surface2
                border.color: EzTheme.accent; border.width: 1.5
                visible: skinBody.status !== Image.Ready

                Text {
                    text: root.accountUser ? root.accountUser.charAt(0).toUpperCase() : "P"
                    font.family: EzTheme.mcFontFamily; font.pixelSize: 28; font.bold: true
                    color: EzTheme.accentLight; anchors.centerIn: parent
                }
            }

            MouseArea {
                id: charHover
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: pressed ? Qt.ClosedHandCursor : Qt.PointingHandCursor
                onPressed: {
                    parent.dragStartX = mouse.x
                }
                onPositionChanged: {
                    if (pressed) {
                        var delta = mouse.x - parent.dragStartX
                        parent.currentRotation = Math.max(-25, Math.min(25, delta * 0.4))
                    } else {
                        var centerDist = (mouse.x - width / 2) / (width / 2)
                        parent.currentRotation = centerDist * 8
                    }
                }
                onExited: {
                    parent.currentRotation = 0
                }
                onClicked: {
                    if (Math.abs(parent.currentRotation) < 5) {
                        if (typeof window !== "undefined" && window.openSkinModal) {
                            window.openSkinModal()
                        } else if (typeof globalSkinModal !== "undefined" && globalSkinModal) {
                            globalSkinModal.open()
                        }
                    }
                }
            }
        }

        Item { height: 14 }

        // ── Active Profile Pill ──
        Rectangle {
            Layout.alignment: Qt.AlignHCenter
            height: 32
            width: profPillRow.implicitWidth + 28
            radius: 16
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

                Rectangle {
                    width: 7; height: 7; radius: 3.5
                    color: EzTheme.accent
                    SequentialAnimation on opacity {
                        loops: Animation.Infinite
                        NumberAnimation { to: 0.3; duration: 600; easing.type: Easing.InOutSine }
                        NumberAnimation { to: 1.0; duration: 600; easing.type: Easing.InOutSine }
                    }
                }

                Text {
                    text: (root.hasProfile ? root.activeName : EzI18n.t("home_default_profile", "Standard Profil")) + "  ·  " + root.activeLoader + " " + root.activeVersion + "  ·  " + root.activeModsCount + " Mods"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    font.bold: true
                    color: EzTheme.text
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

        Item { height: 20 }

        // ── GIANT EPIC PLAY BUTTON ──
        Rectangle {
            id: launchBtn
            Layout.alignment: Qt.AlignHCenter
            width: 340
            height: 62
            radius: EzTheme.radius

            scale: launchMouse.pressed ? 0.95 : (launchMouse.containsMouse ? 1.04 : 1.0)
            Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutCubic } }

            gradient: Gradient {
                orientation: Gradient.Vertical
                GradientStop {
                    position: 0.0
                    color: root.isLaunching ? "#F59E0B" : (launchMouse.containsMouse ? "#36FFa0" : "#2EE080")
                }
                GradientStop {
                    position: 1.0
                    color: root.isLaunching ? "#D97706" : (launchMouse.containsMouse ? "#22C96E" : "#18A858")
                }
            }

            border.color: root.isLaunching ? "#FDE68A" : (launchMouse.containsMouse ? "#5AEEA0" : "#22C96E50")
            border.width: 2

            // Outer glow effect
            Rectangle {
                anchors.fill: parent
                anchors.margins: -4
                radius: parent.radius + 4
                color: "transparent"
                border.color: root.isLaunching ? "#F59E0B20" : EzTheme.accentGlow
                border.width: 3
                opacity: launchMouse.containsMouse ? 0.8 : 0.4
                Behavior on opacity { NumberAnimation { duration: EzTheme.animNormal } }
            }

            // Inner highlight
            Rectangle {
                anchors.fill: parent
                radius: parent.radius
                gradient: Gradient {
                    orientation: Gradient.Vertical
                    GradientStop { position: 0.0; color: "#ffffff20" }
                    GradientStop { position: 0.3; color: "transparent" }
                    GradientStop { position: 1.0; color: "#00000015" }
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

                Text {
                    text: root.isLaunching ? EzI18n.t("home_launching", "STARTET…") : EzI18n.t("home_play", "SPIELEN")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 20
                    font.bold: true
                    color: "#000000"
                    font.letterSpacing: 2
                }
            }

            MouseArea {
                id: launchMouse
                anchors.fill: parent
                hoverEnabled: true
                cursorShape: Qt.PointingHandCursor
                onClicked: {
                    if (profileController && !root.isLaunching) {
                        profileController.launchActiveProfile()
                    }
                }
            }
        }

        Item { height: 14 }

        // ── AUTH STATUS BADGE ──
        Rectangle {
            id: launchModePill
            Layout.alignment: Qt.AlignHCenter
            height: 26
            width: modeRow.implicitWidth + 22
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
                    text: "⚡ " + EzI18n.t("home_direct_badge", "DIREKTSTART AKTIV") + " (" + (typeof accountController !== "undefined" && accountController && accountController.isOnline ? "Microsoft Auth" : "Direct") + ")"
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
                        text: EzI18n.t("home_direct_modal_title", "🟢 Direktstart & Online Verifiziert")
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
                            text: EzI18n.t("home_direct_modal_note", "⚡ 100% Online-kompatibel für alle Multiplayer-Server (z.B. Hypixel), Realms und authentische Skins.")
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
            model: [
                { icon: "⚡", label: root.activeModsCount + " Mods", color: EzTheme.accentLight },
                { icon: "💾", label: Math.round(root.activeRamMb / 1024 * 10) / 10 + " GB RAM", color: EzTheme.cyan },
                { icon: "🎮", label: root.activeLoader + " " + root.activeVersion, color: EzTheme.purple }
            ]

            Rectangle {
                height: 28
                width: statRow.implicitWidth + 20
                radius: 14
                color: "#0A0A0F80"
                border.color: EzTheme.border
                border.width: 1

                RowLayout {
                    id: statRow
                    anchors.centerIn: parent
                    spacing: 6

                    Text { text: modelData.icon; font.pixelSize: 10 }
                    Text {
                        text: modelData.label
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 10
                        font.bold: true
                        color: modelData.color
                    }
                }
            }
        }
    }
}
