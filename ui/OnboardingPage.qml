import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root
    signal profileCreated

    // Wizard steps: "welcome" → "create" → "preset" → "downloading"
    property string step: "welcome"

    // Form state
    property string newName: ""
    property string newVersion: "26.2"
    property string newLoader: "Fabric"
    property string selectedPreset: "performance" // "raw" | "performance" | "essentials"

    // Download / loading state from backend
    property real downloadProgress: 0.0
    property string downloadStatus: "Bereite Profil vor…"

    Connections {
        target: typeof profileController !== "undefined" ? profileController : null
        function onOnboardingStepProgress(progress, modName, statusText) {
            root.downloadProgress = progress
            root.downloadStatus = statusText
        }
        function onOnboardingFinished(profileId) {
            root.downloadProgress = 1.0
            root.downloadStatus = "Profil erfolgreich eingerichtet & optimiert!"
            completeTimer.start()
        }
    }

    Timer {
        id: completeTimer
        interval: 500
        repeat: false
        onTriggered: {
            root.profileCreated()
        }
    }

    Rectangle {
        anchors.fill: parent
        color: EzTheme.bg

        // Background click handler to deselect / defocus input fields
        MouseArea {
            anchors.fill: parent
            onClicked: root.forceActiveFocus()
        }

        // Ambient radial glow behind center
        Rectangle {
            anchors.centerIn: parent
            anchors.verticalCenterOffset: root.step === "welcome" ? -140 : -80
            width: 520
            height: 520
            radius: 260
            color: EzTheme.accentGlow
            opacity: 0.4
            Behavior on anchors.verticalCenterOffset { NumberAnimation { duration: 280; easing.type: Easing.OutQuad } }
        }

        // ──────────────────────────────────────────
        //  STEP 1: WELCOME
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: 460
            height: 420
            visible: opacity > 0.001
            opacity: root.step === "welcome" ? 1.0 : 0.0
            scale: root.step === "welcome" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Glowing Transparent logo mark
                Image {
                    Layout.alignment: Qt.AlignHCenter
                    source: "assets/logo.svg"
                    Layout.preferredWidth: 72
                    Layout.preferredHeight: 72
                    fillMode: Image.PreserveAspectFit
                    smooth: true
                }

                Item { height: 20 }

                Text {
                    text: EzI18n.t("onboard_welcome", "Willkommen bei EzClient")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 26
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                }

                Item { height: 6 }

                Text {
                    text: EzI18n.t("onboard_tagline", "Dein moderner, schneller und optimierter Minecraft Launcher")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 13
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                }

                Item { height: 24 }

                // ── LANGUAGE SELECTION PILLS (DE / EN) ──
                RowLayout {
                    Layout.alignment: Qt.AlignHCenter
                    spacing: 12

                    // German
                    Rectangle {
                        width: 140
                        height: 38
                        radius: 8
                        color: EzI18n.currentLanguage === "de" ? EzTheme.accentDark : (deMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                        border.color: EzI18n.currentLanguage === "de" ? EzTheme.accent : EzTheme.border
                        border.width: 1
                        Behavior on color { ColorAnimation { duration: 120 } }
                        Behavior on border.color { ColorAnimation { duration: 120 } }

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Text { text: "🇩🇪"; font.pixelSize: 14 }
                            Text {
                                text: "Deutsch"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzI18n.currentLanguage === "de" ? EzTheme.accentLight : EzTheme.text
                            }
                        }

                        MouseArea {
                            id: deMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: EzI18n.setLanguage("de")
                        }
                    }

                    // English
                    Rectangle {
                        width: 140
                        height: 38
                        radius: 8
                        color: EzI18n.currentLanguage === "en" ? EzTheme.accentDark : (enMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                        border.color: EzI18n.currentLanguage === "en" ? EzTheme.accent : EzTheme.border
                        border.width: 1
                        Behavior on color { ColorAnimation { duration: 120 } }
                        Behavior on border.color { ColorAnimation { duration: 120 } }

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Text { text: "🇬🇧"; font.pixelSize: 14 }
                            Text {
                                text: "English"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzI18n.currentLanguage === "en" ? EzTheme.accentLight : EzTheme.text
                            }
                        }

                        MouseArea {
                            id: enMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: EzI18n.setLanguage("en")
                        }
                    }
                }

                Item { height: 28 }

                Rectangle {
                    Layout.alignment: Qt.AlignHCenter
                    width: 250; height: 44
                    radius: EzTheme.radius
                    scale: ctaMouse.containsMouse ? 1.03 : 1.0
                    gradient: Gradient {
                        orientation: Gradient.Horizontal
                        GradientStop { position: 0.0; color: ctaMouse.containsMouse ? "#22D474" : EzTheme.accent }
                        GradientStop { position: 1.0; color: ctaMouse.containsMouse ? "#44FF99" : "#33E880" }
                    }
                    Behavior on scale { NumberAnimation { duration: 120; easing.type: Easing.OutQuad } }

                    Text {
                        text: (EzI18n.t("onboard_step2_profile", "Erstes Profil erstellen")) + " →"
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 12
                        font.bold: true
                        color: "#000000"
                        anchors.centerIn: parent
                    }

                    MouseArea {
                        id: ctaMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            root.step = "create"
                            nameInput.forceActiveFocus()
                        }
                    }
                }

                Item { height: 16 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "You can customize profiles and settings anytime" : "Du kannst Profile jederzeit anpassen und verwalten"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    color: EzTheme.textSubtle
                    Layout.alignment: Qt.AlignHCenter
                }
            }
        }

        // ──────────────────────────────────────────
        //  STEP 2: BASIC PROFILE CONFIG (Name, Version, Loader)
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: 440
            height: 440
            visible: opacity > 0.001
            opacity: root.step === "create" ? 1.0 : 0.0
            scale: root.step === "create" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Back button & Step indicator
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 8

                    Rectangle {
                        width: 30; height: 30; radius: 6
                        color: backMouse1.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: EzTheme.border; border.width: 1
                        Behavior on color { ColorAnimation { duration: 100 } }

                        Text { text: "←"; font.family: EzTheme.fontFamily; font.pixelSize: 13; color: EzTheme.text; anchors.centerIn: parent }
                        MouseArea {
                            id: backMouse1
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                root.forceActiveFocus()
                                root.step = "welcome"
                            }
                        }
                    }

                    Text {
                        text: EzI18n.currentLanguage === "en" ? "Step 1 of 2: Basic Settings" : "Schritt 1 von 2: Basis-Einstellungen"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Item { height: 24 }

                Text {
                    text: EzI18n.t("onboard_step2_profile", "Erstes Profil erstellen")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 24
                    font.bold: true
                    color: EzTheme.text
                }

                Item { height: 6 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Choose a name, Minecraft version, and mod loader." : "Wähle einen Namen, Version und Mod-Loader."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 13
                    color: EzTheme.textSecondary
                }

                Item { height: 26 }

                // Profile name input
                Rectangle {
                    Layout.fillWidth: true
                    height: 48
                    radius: EzTheme.radiusSm
                    color: EzTheme.surface
                    border.color: nameInput.activeFocus ? EzTheme.accent : (nameBoxMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                    border.width: 1

                    Behavior on border.color { ColorAnimation { duration: 120 } }

                    TextInput {
                        id: nameInput
                        anchors.fill: parent
                        anchors.leftMargin: 16
                        anchors.rightMargin: 16
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 14
                        color: EzTheme.text
                        selectByMouse: true
                        verticalAlignment: TextInput.AlignVCenter
                        maximumLength: 40
                        onTextChanged: root.newName = text
                        Keys.onReturnPressed: {
                            if (root.newName.trim() !== "") {
                                root.step = "preset"
                            }
                        }

                        Text {
                            text: EzI18n.t("profiles_name_placeholder", "z.B. Mein erstes Profil…")
                            font: parent.font
                            color: EzTheme.textSubtle
                            visible: parent.text === ""
                            anchors.verticalCenter: parent.verticalCenter
                        }
                    }

                    MouseArea {
                        id: nameBoxMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.IBeamCursor
                        onClicked: nameInput.forceActiveFocus()
                    }
                }

                Item { height: 18 }

                // Version dropdown
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 6

                    Text { text: EzI18n.t("onboard_profile_version", "Minecraft Version"); font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }

                    EzDropDown {
                        id: versionPicker
                        Layout.fillWidth: true
                        currentIndex: 0
                        choices: ["26.2", "26.1", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"]
                        onChoiceChanged: root.newVersion = choices[currentIndex]
                    }
                }

                Item { height: 16 }

                // Mod-Loader Tactile Cards (Fabric vs Forge)
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 6

                    Text { text: EzI18n.t("onboard_profile_loader", "Mod-Loader auswählen"); font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }

                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 12

                        // Fabric Card
                        Rectangle {
                            Layout.fillWidth: true
                            height: 56
                            radius: EzTheme.radiusSm
                            color: root.newLoader === "Fabric" ? EzTheme.surfaceActive : (fabMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                            border.color: root.newLoader === "Fabric" ? EzTheme.accent : (fabMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                            border.width: root.newLoader === "Fabric" ? 1.5 : 1
                            scale: fabMouse.containsMouse ? 1.02 : 1.0

                            Behavior on color { ColorAnimation { duration: 110 } }
                            Behavior on border.color { ColorAnimation { duration: 110 } }
                            Behavior on scale { NumberAnimation { duration: 110 } }

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 10
                                spacing: 10

                                Rectangle {
                                    width: 16; height: 16; radius: 8
                                    color: root.newLoader === "Fabric" ? EzTheme.accent : "transparent"
                                    border.color: root.newLoader === "Fabric" ? EzTheme.accent : EzTheme.borderLight
                                    border.width: 1.5
                                    Rectangle { width: 6; height: 6; radius: 3; color: "#000"; anchors.centerIn: parent; visible: root.newLoader === "Fabric" }
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 1
                                    Text { text: "Fabric"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                                    Text { text: EzI18n.currentLanguage === "en" ? "Lightweight & Ultra FPS" : "Leicht & Ultra-FPS"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.accentLight }
                                }
                            }

                            MouseArea {
                                id: fabMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: root.newLoader = "Fabric"
                            }
                        }

                        // Forge Card
                        Rectangle {
                            Layout.fillWidth: true
                            height: 56
                            radius: EzTheme.radiusSm
                            color: root.newLoader === "Forge" ? EzTheme.surfaceActive : (forgeMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                            border.color: root.newLoader === "Forge" ? EzTheme.accent : (forgeMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                            border.width: root.newLoader === "Forge" ? 1.5 : 1
                            scale: forgeMouse.containsMouse ? 1.02 : 1.0

                            Behavior on color { ColorAnimation { duration: 110 } }
                            Behavior on border.color { ColorAnimation { duration: 110 } }
                            Behavior on scale { NumberAnimation { duration: 110 } }

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 10
                                spacing: 10

                                Rectangle {
                                    width: 16; height: 16; radius: 8
                                    color: root.newLoader === "Forge" ? EzTheme.accent : "transparent"
                                    border.color: root.newLoader === "Forge" ? EzTheme.accent : EzTheme.borderLight
                                    border.width: 1.5
                                    Rectangle { width: 6; height: 6; radius: 3; color: "#000"; anchors.centerIn: parent; visible: root.newLoader === "Forge" }
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 1
                                    Text { text: "Forge"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                                    Text { text: EzI18n.currentLanguage === "en" ? "Classic Mod Ecosystem" : "Klassisches Ökosystem"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                                }
                            }

                            MouseArea {
                                id: forgeMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: root.newLoader = "Forge"
                            }
                        }
                    }
                }

                Item { height: 28 }

                // Continue to Preset Selection
                Rectangle {
                    Layout.fillWidth: true
                    height: 44
                    radius: EzTheme.radius
                    color: root.newName.trim() !== ""
                           ? (step2BtnMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent)
                           : EzTheme.surface3
                    opacity: root.newName.trim() !== "" ? 1.0 : 0.45
                    scale: step2BtnMouse.containsMouse && root.newName.trim() !== "" ? 1.02 : 1.0

                    Behavior on color { ColorAnimation { duration: 120 } }
                    Behavior on opacity { NumberAnimation { duration: 120 } }
                    Behavior on scale { NumberAnimation { duration: 120 } }

                    Text {
                        text: EzI18n.t("onboard_btn_next", "Weiter")
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 13
                        font.bold: true
                        color: root.newName.trim() !== "" ? "#000000" : EzTheme.textSubtle
                        anchors.centerIn: parent
                    }

                    MouseArea {
                        id: step2BtnMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: root.newName.trim() !== "" ? Qt.PointingHandCursor : Qt.ForbiddenCursor
                        enabled: root.newName.trim() !== ""
                        onClicked: {
                            root.step = "preset"
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────
        //  STEP 3: MOD PRESET SELECTION (Raw, Performance, Essentials)
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: 580
            height: 520
            visible: opacity > 0.001
            opacity: root.step === "preset" ? 1.0 : 0.0
            scale: root.step === "preset" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Back button & Step indicator
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 8

                    Rectangle {
                        width: 30; height: 30; radius: 6
                        color: backMouse2.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: EzTheme.border; border.width: 1
                        Behavior on color { ColorAnimation { duration: 100 } }

                        Text { text: "←"; font.family: EzTheme.fontFamily; font.pixelSize: 13; color: EzTheme.text; anchors.centerIn: parent }
                        MouseArea {
                            id: backMouse2
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                root.step = "create"
                            }
                        }
                    }

                    Text {
                        text: EzI18n.currentLanguage === "en" ? "Step 2 of 2: Optimization Setup" : "Schritt 2 von 2: Optimierungs-Paket"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Item { height: 18 }

                Text {
                    text: EzI18n.t("onboard_profile_preset", "Wähle deine Ausstattung")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 22
                    font.bold: true
                    color: EzTheme.text
                }

                Item { height: 4 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "EzClient can automatically equip your profile with top performance mods." : "EzClient kann dein Profil automatisch mit Best-in-Class Performance-Mods ausstatten."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.textSecondary
                }

                Item { height: 16 }

                // Preset Cards Stack
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    // Option 1: EzClient Standard (Recommended)
                    EzPresetCard {
                        presetKey: "performance"
                        title: EzI18n.t("onboard_preset_perf", "EzClient (Empfohlen)")
                        tag: EzI18n.currentLanguage === "en" ? "FULL CLIENT · MAX FPS" : "VOLLVERSION · MAX FPS"
                        tagColor: EzTheme.accent
                        tagTextColor: "#000000"
                        sub: EzI18n.currentLanguage === "en" ? "Full client with max FPS, minimum RAM & optimized PvP setup" : "Vollwertiger Client mit maximalen FPS, minimalem RAM & optimiertem PvP-Setup"
                        mods: "Inklusive: EzClient Core, Sodium, Lithium, FerriteCore, Memory Leak Fix, Krypton, ImmediatelyFast, EntityCulling"
                        selected: root.selectedPreset === "performance"
                        onClicked: root.selectedPreset = "performance"
                    }

                    // Option 2: EzClient + Essential Mod
                    EzPresetCard {
                        presetKey: "essentials"
                        title: EzI18n.t("onboard_preset_ess", "EzClient + Essential Mod")
                        tag: "FEATURE CLIENT"
                        tagColor: EzTheme.cyan
                        tagTextColor: "#000000"
                        sub: EzI18n.currentLanguage === "en" ? "EzClient setup plus world hosting, friend list & chat" : "EzClient Setup plus Welten hosten, Freundesliste & Chat"
                        mods: "Inklusive: EzClient Setup + Essential Mod (Host Worlds & Friends)"
                        selected: root.selectedPreset === "essentials"
                        onClicked: root.selectedPreset = "essentials"
                    }

                    // Option 3: Raw
                    EzPresetCard {
                        presetKey: "raw"
                        title: EzI18n.t("onboard_preset_vanilla", "Vanilla Pure (Keine Mods)")
                        tag: "RAW"
                        tagColor: EzTheme.surface3
                        tagTextColor: EzTheme.textSecondary
                        sub: EzI18n.currentLanguage === "en" ? "Untouched standard Minecraft without modifications" : "Unberührtes Standard-Minecraft ohne Modifikationen"
                        mods: EzI18n.currentLanguage === "en" ? "No additional mods preinstalled" : "Keine zusätzlichen Mods vorinstalliert"
                        selected: root.selectedPreset === "raw"
                        onClicked: root.selectedPreset = "raw"
                    }
                }

                Item { height: 20 }

                // Finish & Install Button
                Rectangle {
                    Layout.fillWidth: true
                    height: 44
                    radius: EzTheme.radius
                    color: installBtnMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent
                    scale: installBtnMouse.containsMouse ? 1.02 : 1.0

                    Behavior on color { ColorAnimation { duration: 120 } }
                    Behavior on scale { NumberAnimation { duration: 120 } }

                    Text {
                        text: EzI18n.t("onboard_btn_create", "Client-Profil erstellen & starten")
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 13
                        font.bold: true
                        color: "#000000"
                        anchors.centerIn: parent
                    }

                    MouseArea {
                        id: installBtnMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            root.step = "downloading"
                            root.downloadProgress = 0.0
                            root.downloadStatus = EzI18n.t("onboard_downloading", "Richte EzClient ein & lade Mods…")
                            if (typeof profileController !== "undefined" && profileController) {
                                profileController.createProfileWithLiveDownloads(root.newName.trim(), root.newVersion, root.newLoader, root.selectedPreset)
                            }
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────
        //  STEP 4: ULTRA-CLEAN LIVE LOADING ANIMATION
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: 460
            height: 320
            visible: opacity > 0.001
            opacity: root.step === "downloading" ? 1.0 : 0.0
            scale: root.step === "downloading" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Animated glowing icon
                Rectangle {
                    Layout.alignment: Qt.AlignHCenter
                    width: 64; height: 64; radius: 18
                    color: EzTheme.surface2
                    border.color: EzTheme.accent
                    border.width: 1.5

                    // Pulse glow
                    Rectangle {
                        anchors.fill: parent
                        anchors.margins: -4
                        radius: 22
                        color: "transparent"
                        border.color: EzTheme.accentGlow
                        border.width: 2
                        opacity: root.downloadProgress < 1.0 ? 0.8 : 0.0
                        Behavior on opacity { NumberAnimation { duration: 200 } }
                    }

                    Text {
                        text: root.downloadProgress >= 1.0 ? "✓" : "⚡"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 26
                        font.bold: true
                        color: EzTheme.accentLight
                        anchors.centerIn: parent
                    }
                }

                Item { height: 26 }

                Text {
                    text: root.downloadProgress >= 1.0 ? "Profil fertiggestellt!" : "Richte " + root.newName + " ein…"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 20
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                }

                Item { height: 8 }

                Text {
                    text: root.downloadStatus
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                }

                Item { height: 24 }

                // Progress Bar Container
                Rectangle {
                    Layout.fillWidth: true
                    height: 8
                    radius: 4
                    color: EzTheme.surface2
                    border.color: EzTheme.border
                    border.width: 1

                    Rectangle {
                        height: parent.height
                        width: parent.width * root.downloadProgress
                        radius: 4
                        gradient: Gradient {
                            orientation: Gradient.Horizontal
                            GradientStop { position: 0.0; color: EzTheme.accent }
                            GradientStop { position: 1.0; color: "#44FF99" }
                        }
                        Behavior on width { NumberAnimation { duration: 60; easing.type: Easing.Linear } }
                    }
                }

                Item { height: 10 }

                RowLayout {
                    Layout.fillWidth: true
                    Text {
                        text: root.newLoader + " " + root.newVersion
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textSubtle
                    }
                    Item { Layout.fillWidth: true }
                    Text {
                        text: Math.round(root.downloadProgress * 100) + "%"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        font.bold: true
                        color: EzTheme.accentLight
                    }
                }
            }
        }
    }

}
