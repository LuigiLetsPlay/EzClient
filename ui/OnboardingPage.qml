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
                        choices: ["26.2", "26.1", "1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21"]
                        formatEzClientSupported: true
                        onChoiceChanged: {
                            root.newVersion = choices[currentIndex]
                            
                            // Reset selected preset to raw if not supported to avoid invisible preset selection
                            if (!versionPicker.isEzClientSupported(root.newVersion) && (root.selectedPreset === "essentials" || root.selectedPreset === "performance")) {
                                root.selectedPreset = "perf_essentials"
                            } else if (versionPicker.isEzClientSupported(root.newVersion) && (root.selectedPreset === "perf_essentials" || root.selectedPreset === "perf_only")) {
                                root.selectedPreset = "performance"
                            }
                        }
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
                                    Text { text: "Fabric"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text; Layout.alignment: Qt.AlignLeft; Layout.fillWidth: true; wrapMode: Text.WordWrap }
                                    Text { text: EzI18n.currentLanguage === "en" ? "Lightweight & Ultra FPS" : "Leicht & Ultra-FPS"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.accentLight; Layout.alignment: Qt.AlignLeft; Layout.fillWidth: true; wrapMode: Text.WordWrap }
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
                                    Text { text: "Forge"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text; Layout.alignment: Qt.AlignLeft; Layout.fillWidth: true; wrapMode: Text.WordWrap }
                                    Text { text: EzI18n.currentLanguage === "en" ? "Classic Mod Ecosystem" : "Klassisches Ökosystem"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted; Layout.alignment: Qt.AlignLeft; Layout.fillWidth: true; wrapMode: Text.WordWrap }
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

                    // Supported Presets
                    // Option 1: EzClient Standard (Recommended)
                    EzPresetCard {
                        visible: versionPicker.isEzClientSupported(root.newVersion)
                        presetKey: "performance"
                        title: EzI18n.t("onboard_preset_perf", "EzClient Only")
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
                        visible: versionPicker.isEzClientSupported(root.newVersion)
                        presetKey: "essentials"
                        title: EzI18n.t("onboard_preset_ess", "EzClient + Essentials")
                        tag: "FEATURE CLIENT"
                        tagColor: EzTheme.cyan
                        tagTextColor: "#000000"
                        sub: EzI18n.currentLanguage === "en" ? "EzClient setup plus world hosting, friend list & chat" : "EzClient Setup plus Welten hosten, Freundesliste & Chat"
                        mods: "Inklusive: EzClient Setup + Essential Mod (Host Worlds & Friends)"
                        selected: root.selectedPreset === "essentials"
                        onClicked: root.selectedPreset = "essentials"
                    }
                    
                    // Unsupported Presets
                    // Option 3: Performance Mods + Essentials
                    EzPresetCard {
                        visible: !versionPicker.isEzClientSupported(root.newVersion)
                        presetKey: "perf_essentials"
                        title: "Performance Mods + Essentials"
                        tag: "MODPACK"
                        tagColor: EzTheme.cyan
                        tagTextColor: "#000000"
                        sub: "Sodium, Lithium, Essential Mod for World Hosting"
                        mods: "Inklusive: Sodium, Lithium, Essential Mod"
                        selected: root.selectedPreset === "perf_essentials"
                        onClicked: root.selectedPreset = "perf_essentials"
                    }

                    // Option 4: Performance Mods Only
                    EzPresetCard {
                        visible: !versionPicker.isEzClientSupported(root.newVersion)
                        presetKey: "perf_only"
                        title: "Performance Mods"
                        tag: "MAX FPS"
                        tagColor: EzTheme.accent
                        tagTextColor: "#000000"
                        sub: "FPS Boost Engine, Chunk Optimization"
                        mods: "Inklusive: Sodium, Lithium, FerriteCore"
                        selected: root.selectedPreset === "perf_only"
                        onClicked: root.selectedPreset = "perf_only"
                    }

                    // Always Available Presets
                    // Option 5: Raw
                    EzPresetCard {
                        presetKey: "raw"
                        title: EzI18n.t("onboard_preset_vanilla", "Raw / Vanilla")
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
                            if (root.selectedPreset === "raw") {
                                root.step = "downloading"
                                root.downloadProgress = 0.0
                                root.downloadStatus = EzI18n.t("onboard_downloading", "Richte EzClient ein & lade Mods…")
                                if (typeof profileController !== "undefined" && profileController) {
                                    profileController.createAndOnboard(root.newName.trim(), root.newVersion, root.newLoader, root.selectedPreset, [])
                                }
                            } else {
                                root.step = "mod_selection"
                            }
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────
        //  STEP 3.5: MOD SELECTION
        // ──────────────────────────────────────────
        Item {
            id: modSelectionItem
            anchors.centerIn: parent
            width: 460
            height: 480
            visible: opacity > 0.001
            opacity: root.step === "mod_selection" ? 1.0 : 0.0
            scale: root.step === "mod_selection" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            property var optionalMods: [
                { name: "Sodium", slug: "sodium", desc: "FPS Boost Engine", icon: "https://cdn.modrinth.com/data/AANobbMI/295862f4724dc3f78df3447ad6072b2dcd3ef0c9_96.webp" },
                { name: "Lithium", slug: "lithium", desc: "Physik & Chunk Optimierung", icon: "https://cdn.modrinth.com/data/gvQqBUqZ/bcc8686c13af0143adf4285d741256af824f70b7_96.webp" },
                { name: "FerriteCore", slug: "ferrite-core", desc: "Reduziert RAM-Verbrauch", icon: "https://cdn.modrinth.com/data/uXXizFIs/222a126f26f8f9ae1eb339f3b767677f18bff31f_96.webp" },
                { name: "Memory Leak Fix", slug: "memoryleakfix", desc: "Behebt Java Speicherlecks", icon: "https://cdn.modrinth.com/data/NRjRiSSD/a279c19f9c3574339fa90f675aa8a94f8f6cff92_96.webp" },
                { name: "ImmediatelyFast", slug: "immediatelyfast", desc: "Schnelleres UI Rendering", icon: "https://cdn.modrinth.com/data/5ZwdcRci/e57b6b451425692ac17ad322d5e14bea686a383a_96.webp" },
                { name: "Entity Culling", slug: "entityculling", desc: "Versteckt unsichtbare Mobs", icon: "https://cdn.modrinth.com/data/NNAgCjsB/7873452d6cede4daed12da3d7d8c193ab88b4fd6_96.webp" },
                { name: "Krypton", slug: "krypton", desc: "Optimiertes Netzwerk", icon: "https://cdn.modrinth.com/data/fQEb0iXm/3ea60899d060a9286e03b87bfa9e71d0cbe2dde7_96.webp" }
            ]
            property var selectedMods: ["sodium", "lithium", "ferrite-core", "memoryleakfix", "immediatelyfast", "entityculling", "krypton"]

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Back Button
                Item {
                    Layout.fillWidth: true
                    height: 24
                    RowLayout {
                        anchors.fill: parent
                        spacing: 8
                        Text { text: "←"; font.pixelSize: 18; color: EzTheme.textMuted }
                        Text { text: EzI18n.currentLanguage === "en" ? "Back" : "Zurück"; font.family: EzTheme.fontFamily; font.pixelSize: 13; color: EzTheme.textMuted }
                    }
                    MouseArea {
                        anchors.fill: parent
                        anchors.margins: -10
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.step = "preset"
                    }
                }

                Item { height: 18 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Select Performance Mods" : "Wähle deine Performance Mods"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 22
                    font.bold: true
                    color: EzTheme.text
                }

                Item { height: 4 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Uncheck the ones you don't need. We recommend leaving the default selection." : "Wähle Mods ab, die du nicht brauchst. Es wird empfohlen, die Standardauswahl beizubehalten."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.textSecondary
                }

                Item { height: 16 }

                // Mods List
                Rectangle {
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    color: EzTheme.surface2
                    radius: EzTheme.radius
                    border.color: EzTheme.border
                    border.width: 1
                    clip: true

                    ListView {
                        anchors.fill: parent
                        anchors.margins: 4
                        model: modSelectionItem.optionalMods
                        delegate: Rectangle {
                            width: ListView.view.width
                            height: 48
                            color: modMouse.containsMouse ? EzTheme.surface3 : "transparent"
                            radius: 6
                            
                            property bool isSelected: modSelectionItem.selectedMods.indexOf(modelData.slug) !== -1

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 12
                                spacing: 12

                                Rectangle {
                                    width: 20
                                    height: 20
                                    radius: 4
                                    color: isSelected ? EzTheme.accent : "transparent"
                                    border.color: isSelected ? EzTheme.accent : EzTheme.border
                                    border.width: 1
                                    
                                    Text {
                                        anchors.centerIn: parent
                                        text: "✓"
                                        color: "#000000"
                                        font.pixelSize: 14
                                        font.bold: true
                                        visible: isSelected
                                    }
                                }

                                Rectangle {
                                    width: 32; height: 32
                                    radius: 6
                                    color: EzTheme.surface
                                    clip: true
                                    Image {
                                        anchors.fill: parent
                                        source: modelData.icon ? modelData.icon : ""
                                        fillMode: Image.PreserveAspectCrop
                                        smooth: true
                                    }
                                }

                                ColumnLayout {
                                    spacing: 2
                                    Layout.fillWidth: true
                                    Text {
                                        text: modelData.name
                                        color: EzTheme.text
                                        font.pixelSize: 14
                                        font.bold: true
                                        font.family: EzTheme.fontFamily
                                        Layout.alignment: Qt.AlignLeft
                                        Layout.fillWidth: true
                                        wrapMode: Text.WordWrap
                                    }
                                    Text {
                                        text: modelData.desc
                                        color: EzTheme.textSecondary
                                        font.pixelSize: 11
                                        font.family: EzTheme.fontFamily
                                        Layout.alignment: Qt.AlignLeft
                                        Layout.fillWidth: true
                                        wrapMode: Text.WordWrap
                                    }
                                }
                            }

                            MouseArea {
                                id: modMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    var list = modSelectionItem.selectedMods;
                                    var idx = list.indexOf(modelData.slug);
                                    if (idx !== -1) {
                                        list.splice(idx, 1);
                                    } else {
                                        list.push(modelData.slug);
                                    }
                                    modSelectionItem.selectedMods = list;
                                    modSelectionItem.selectedModsChanged();
                                }
                            }
                        }
                    }
                }

                Item { height: 20 }

                // Finish & Install Button
                Rectangle {
                    Layout.fillWidth: true
                    height: 44
                    radius: EzTheme.radius
                    color: finalInstallBtnMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent
                    scale: finalInstallBtnMouse.containsMouse ? 1.02 : 1.0

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
                        id: finalInstallBtnMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            root.step = "downloading"
                            root.downloadProgress = 0.0
                            root.downloadStatus = EzI18n.t("onboard_downloading", "Richte EzClient ein & lade Mods…")
                            
                            var optionalSlugs = ["sodium", "lithium", "ferrite-core", "memoryleakfix", "immediatelyfast", "entityculling", "krypton", "zoomify"];
                            var excluded = [];
                            var selectedList = parent.parent.parent.selectedMods;
                            for (var i = 0; i < optionalSlugs.length; i++) {
                                if (selectedList.indexOf(optionalSlugs[i]) === -1) {
                                    excluded.push(optionalSlugs[i]);
                                }
                            }
                            
                            if (typeof profileController !== "undefined" && profileController) {
                                profileController.createAndOnboard(root.newName.trim(), root.newVersion, root.newLoader, root.selectedPreset, excluded)
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
