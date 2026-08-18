import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root

    readonly property bool hasProfile: typeof profileController !== "undefined" && profileController && profileController.activeName !== "No Profile" && profileController.activeName !== ""
    readonly property string activeName: typeof profileController !== "undefined" && profileController ? profileController.activeName : ""
    readonly property string activeVersion: typeof profileController !== "undefined" && profileController ? profileController.activeVersion : "1.21.4"
    readonly property string activeLoader: typeof profileController !== "undefined" && profileController ? profileController.activeLoader : "Fabric"
    readonly property int activeModsCount: typeof profileController !== "undefined" && profileController ? profileController.activeModsCount : 0
    readonly property int activeRamMb: typeof profileController !== "undefined" && profileController ? profileController.activeRamMb : 4096
    readonly property bool isLaunching: typeof profileController !== "undefined" && profileController ? profileController.isLaunching : false

    readonly property string accountUser: typeof accountController !== "undefined" && accountController ? accountController.username : "Player"
    readonly property string bodyUrl: typeof accountController !== "undefined" && accountController ? accountController.bodyUrl : ""

    // ─────────────────────────────────────────────────────────
    // 1. FULL-BLEED CINEMATIC MINECRAFT BACKGROUND
    // ─────────────────────────────────────────────────────────
    Image {
        id: bgHero
        anchors.fill: parent
        source: "assets/hero_bg.jpg"
        fillMode: Image.PreserveAspectCrop
        opacity: 0.42
    }

    // Cinematic Vignette Overlay (Dark top & bottom gradients)
    Rectangle {
        anchors.fill: parent
        gradient: Gradient {
            orientation: Gradient.Vertical
            GradientStop { position: 0.0; color: "#EE0B0F14" }
            GradientStop { position: 0.25; color: "#550B0F14" }
            GradientStop { position: 0.65; color: "#550B0F14" }
            GradientStop { position: 1.0; color: "#F0080C10" }
        }
    }

    // Ambient radial glow behind the center character
    Rectangle {
        anchors.centerIn: parent
        anchors.verticalCenterOffset: -40
        width: 480
        height: 480
        radius: 240
        color: "transparent"
        border.color: "transparent"

        Rectangle {
            anchors.centerIn: parent
            width: 320
            height: 320
            radius: 160
            color: EzTheme.accent
            opacity: 0.12
            SequentialAnimation on opacity {
                loops: Animation.Infinite
                NumberAnimation { to: 0.22; duration: 2500; easing.type: Easing.InOutQuad }
                NumberAnimation { to: 0.12; duration: 2500; easing.type: Easing.InOutQuad }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // 2. MAIN CENTER HERO CONTENT (Character + Launch Button)
    // ─────────────────────────────────────────────────────────
    ColumnLayout {
        anchors.centerIn: parent
        spacing: 0

        // ── 3D Minecraft Character Render ──
        Item {
            Layout.alignment: Qt.AlignHCenter
            width: 240
            height: 280

            // Ambient Shadow under player feet
            Rectangle {
                anchors.bottom: parent.bottom
                anchors.bottomMargin: 10
                anchors.horizontalCenter: parent.horizontalCenter
                width: 140
                height: 20
                radius: 10
                color: "#000000"
                opacity: 0.6
                scale: charHover.containsMouse ? 1.08 : 1.0
                Behavior on scale { NumberAnimation { duration: 200 } }
            }

            // Full-body 3D Character Skin Image
            Image {
                id: skinBody
                anchors.centerIn: parent
                anchors.verticalCenterOffset: charHover.containsMouse ? -6 : 0
                height: 250
                source: root.bodyUrl !== "" ? root.bodyUrl : "https://mc-heads.net/body/Steve/360"
                fillMode: Image.PreserveAspectFit
                smooth: false

                Behavior on anchors.verticalCenterOffset {
                    NumberAnimation { duration: 180; easing.type: Easing.OutQuad }
                }
            }

            // Fallback avatar box if skin loading
            Rectangle {
                anchors.centerIn: parent
                width: 72
                height: 72
                radius: 16
                color: EzTheme.surface2
                border.color: EzTheme.accent
                border.width: 1.5
                visible: skinBody.status !== Image.Ready

                Text {
                    text: root.accountUser ? root.accountUser.charAt(0).toUpperCase() : "P"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 28
                    font.bold: true
                    color: EzTheme.accentLight
                    anchors.centerIn: parent
                }
            }

            // Interactive hover on skin
            MouseArea {
                id: charHover
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

        Item { height: 16 }

        // ── Active Profile Pill ──
        Rectangle {
            Layout.alignment: Qt.AlignHCenter
            height: 30
            width: profPillRow.implicitWidth + 24
            radius: 15
            color: "#161D24"
            border.color: profPillMouse.containsMouse ? EzTheme.accentLight : EzTheme.borderLight
            border.width: 1
            scale: profPillMouse.containsMouse ? 1.03 : 1.0

            Behavior on scale { NumberAnimation { duration: 100 } }
            Behavior on border.color { ColorAnimation { duration: 100 } }

            RowLayout {
                id: profPillRow
                anchors.centerIn: parent
                spacing: 8

                Rectangle {
                    width: 7; height: 7; radius: 3.5
                    color: EzTheme.accent
                    SequentialAnimation on opacity {
                        loops: Animation.Infinite
                        NumberAnimation { to: 0.3; duration: 500 }
                        NumberAnimation { to: 1.0; duration: 500 }
                    }
                }

                Text {
                    text: (root.hasProfile ? root.activeName : EzI18n.t("home_default_profile", "Standard Profil")) + "  ·  " + root.activeLoader + " " + root.activeVersion + "  ·  " + root.activeModsCount + " Mods"
                    font.family: EzTheme.mcFontFamily
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

        Item { height: 16 }

        // ── GIANT EPIC MINECRAFT LAUNCH BUTTON ──
        Rectangle {
            id: launchBtn
            Layout.alignment: Qt.AlignHCenter
            width: 320
            height: 58
            radius: 8

            scale: launchMouse.pressed ? 0.95 : (launchMouse.containsMouse ? 1.03 : 1.0)
            Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutQuad } }

            gradient: Gradient {
                orientation: Gradient.Vertical
                GradientStop {
                    position: 0.0
                    color: root.isLaunching ? "#F59E0B" : (launchMouse.containsMouse ? "#33FF8A" : EzTheme.accent)
                }
                GradientStop {
                    position: 1.0
                    color: root.isLaunching ? "#D97706" : (launchMouse.containsMouse ? "#00E676" : "#00C853")
                }
            }

            // Glow Border
            border.color: root.isLaunching ? "#FDE68A" : (launchMouse.containsMouse ? "#B9F6CA" : "#00E676")
            border.width: 2

            RowLayout {
                anchors.centerIn: parent
                spacing: 12

                Image {
                    source: "icons/play.svg"
                    width: 18
                    height: 18
                    fillMode: Image.PreserveAspectFit
                    visible: !root.isLaunching
                }

                Rectangle {
                    width: 14; height: 14; radius: 7
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
                    font.pixelSize: 18
                    font.bold: true
                    color: "#000000"
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

        Item { height: 12 }

        // ── ONLINE AUTHENTICATION STATUS BADGE & TOOLTIP ──
        Rectangle {
            id: launchModePill
            Layout.alignment: Qt.AlignHCenter
            height: 24
            width: modeRow.implicitWidth + 20
            radius: 12
            color: modeMouse.containsMouse ? "#1A261F" : "#111C15"
            border.color: EzTheme.accent
            border.width: 1

            Behavior on color { ColorAnimation { duration: 120 } }
            Behavior on border.color { ColorAnimation { duration: 120 } }

            RowLayout {
                id: modeRow
                anchors.centerIn: parent
                spacing: 6

                Rectangle {
                    width: 6; height: 6; radius: 3
                    color: EzTheme.accent
                    SequentialAnimation on opacity {
                        loops: Animation.Infinite
                        NumberAnimation { to: 0.3; duration: 600 }
                        NumberAnimation { to: 1.0; duration: 600 }
                    }
                }

                Text {
                    text: "⚡ " + EzI18n.t("home_direct_badge", "DIREKTSTART AKTIV") + " (" + (typeof accountController !== "undefined" && accountController && accountController.isOnline ? "Microsoft Auth" : "Direct") + ")"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 9
                    font.bold: true
                    color: EzTheme.accentLight
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

            // Interactive Info Popup explaining 100% online authentication & direct launch
            Popup {
                id: infoPopup
                x: Math.round((launchModePill.width - 320) / 2)
                y: launchModePill.height + 8
                width: 320
                padding: 14
                closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent

                background: Rectangle {
                    color: "#13171F"
                    radius: 10
                    border.color: EzTheme.accent
                    border.width: 1

                    Rectangle {
                        anchors.fill: parent
                        anchors.margins: -4
                        radius: 14
                        color: "transparent"
                        border.color: EzTheme.accentGlow
                        border.width: 1.5
                        opacity: 0.3
                    }
                }

                contentItem: ColumnLayout {
                    spacing: 8

                    RowLayout {
                        spacing: 8
                        Text {
                            text: EzI18n.t("home_direct_modal_title", "🟢 Direktstart & Online Verifiziert")
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 13
                            font.bold: true
                            color: EzTheme.accentLight
                        }
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                    Text {
                        text: EzI18n.t("home_direct_modal_desc", "EzClient startet Minecraft blitzschnell direkt über Java mit deinem aus dem .minecraft-Ordner ausgelesenen Microsoft/Xbox-Token – komplett ohne den Minecraft Launcher zu öffnen!")
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.text
                        wrapMode: Text.WordWrap
                        Layout.fillWidth: true
                    }

                    Rectangle {
                        Layout.fillWidth: true
                        Layout.preferredHeight: infoNoteText.implicitHeight + 14
                        radius: 6
                        color: "#0B0F14"
                        border.color: EzTheme.borderLight
                        border.width: 1

                        Text {
                            id: infoNoteText
                            anchors.fill: parent
                            anchors.margins: 7
                            text: EzI18n.t("home_direct_modal_note", "⚡ 100% Online-kompatibel für alle Multiplayer-Server (z.B. Hypixel), Realms und authentische Skins.")
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            color: EzTheme.cyan
                            wrapMode: Text.WordWrap
                        }
                    }
                }
            }
        }
    }
}
