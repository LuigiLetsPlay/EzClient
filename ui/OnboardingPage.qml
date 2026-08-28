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
    property string selectedPreset: "ezclient" // "ezclient" | "raw"

    // Download / loading state from backend
    property real downloadProgress: 0.0
    property string downloadStatus: "Bereite Profil vor…"

    function beginProfileSetup(optionalMods) {
        root.step = "downloading"
        root.downloadProgress = 0.0
        root.downloadStatus = EzI18n.currentLanguage === "en" ? "Initializing profile…" : "Initialisiere Profil…"
        if (typeof profileController !== "undefined" && profileController) {
            profileController.createAndOnboard(
                root.newName.trim(), root.newVersion, root.newLoader,
                root.selectedPreset, optionalMods
            )
        }
    }

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
                        text: (EzI18n.t("onboard_continue", "Weiter")) + " →"
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
                            root.step = "account"
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
        //  STEP 1.5: MINECRAFT / MICROSOFT ACCOUNT
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: 460
            height: 440
            visible: opacity > 0.001
            opacity: root.step === "account" ? 1.0 : 0.0
            scale: root.step === "account" ? 1.0 : 0.95

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
                        color: backMouseAcc.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: EzTheme.border; border.width: 1
                        Behavior on color { ColorAnimation { duration: 100 } }

                        Text { text: "←"; font.family: EzTheme.fontFamily; font.pixelSize: 13; color: EzTheme.text; anchors.centerIn: parent }
                        MouseArea {
                            id: backMouseAcc
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
                        text: EzI18n.currentLanguage === "en" ? "Step 1 of 3: Minecraft Account" : "Schritt 1 von 3: Minecraft Konto"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Item { height: 24 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Connect Minecraft Account" : "Minecraft-Konto verbinden"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 24
                    font.bold: true
                    color: EzTheme.text
                }

                Item { height: 6 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Sign in with your Microsoft account to play Minecraft online." : "Melde dich mit deinem Microsoft-Konto an, um online auf Servern zu spielen."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 13
                    color: EzTheme.textSecondary
                }

                Item { height: 24 }

                // Account status card
                EzSurface {
                    Layout.fillWidth: true
                    implicitHeight: 80

                    RowLayout {
                        anchors.fill: parent
                        anchors.margins: 16
                        spacing: 14

                        Rectangle {
                            width: 48; height: 48; radius: 24
                            color: EzTheme.surface3
                            clip: true

                            Image {
                                anchors.fill: parent
                                source: typeof accountController !== "undefined" && accountController ? accountController.avatarUrl : ""
                                fillMode: Image.PreserveAspectCrop
                            }
                        }

                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 3
                            Text {
                                text: typeof accountController !== "undefined" && accountController ? accountController.username : "Player"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 15
                                font.bold: true
                                color: EzTheme.text
                            }
                            RowLayout {
                                spacing: 5
                                Rectangle {
                                    width: 7; height: 7; radius: 3.5
                                    color: (typeof accountController !== "undefined" && accountController && accountController.isOnline) ? EzTheme.accent : EzTheme.textMuted
                                }
                                Text {
                                    text: (typeof accountController !== "undefined" && accountController && accountController.isOnline)
                                          ? "Microsoft Auth (Online Verifiziert)"
                                          : "Nicht angemeldet (Lokales Profil)"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 11
                                    color: (typeof accountController !== "undefined" && accountController && accountController.isOnline) ? EzTheme.accentLight : EzTheme.textMuted
                                }
                            }
                        }

                        EzButton {
                            text: (typeof accountController !== "undefined" && accountController && accountController.isOnline) ? "Konto wechseln" : "Anmelden"
                            primary: !(typeof accountController !== "undefined" && accountController && accountController.isOnline)
                            mcFont: true
                            Layout.preferredHeight: 34
                            onClicked: {
                                if (typeof accountController !== "undefined" && accountController) {
                                    accountController.openLoginDialog()
                                }
                            }
                        }
                    }
                }

                Item { height: 28 }

                // Continue button
                Rectangle {
                    Layout.fillWidth: true
                    height: 44
                    radius: EzTheme.radius
                    color: nextAccMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent
                    scale: nextAccMouse.containsMouse ? 1.02 : 1.0
                    Behavior on scale { NumberAnimation { duration: 100 } }

                    Text {
                        text: (EzI18n.t("onboard_continue", "Weiter")) + " →"
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 12
                        font.bold: true
                        color: "#000000"
                        anchors.centerIn: parent
                    }

                    MouseArea {
                        id: nextAccMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: {
                            root.step = "create"
                            nameInput.forceActiveFocus()
                        }
                    }
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
                                root.step = "account"
                            }
                        }
                    }

                    Text {
                        text: EzI18n.currentLanguage === "en" ? "Step 2 of 3: Basic Settings" : "Schritt 2 von 3: Basis-Einstellungen"
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
                            if (!versionPicker.isEzClientSupported(root.newVersion) && root.selectedPreset === "ezclient") {
                                root.selectedPreset = "raw"
                            } else if (versionPicker.isEzClientSupported(root.newVersion) && root.selectedPreset !== "raw") {
                                root.selectedPreset = "ezclient"
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
                    text: EzI18n.currentLanguage === "en" ? "Choose an optimized EzClient environment or a clean Raw profile." : "Wähle eine optimierte EzClient-Umgebung oder ein sauberes Raw-Profil."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.textSecondary
                }

                Item { height: 16 }

                // Preset Cards Stack
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    // Option 1: EzClient (recommended)
                    EzPresetCard {
                        visible: versionPicker.isEzClientSupported(root.newVersion)
                        presetKey: "ezclient"
                        title: "EzClient"
                        tag: EzI18n.currentLanguage === "en" ? "RECOMMENDED" : "EMPFOHLEN"
                        tagColor: EzTheme.accent
                        tagTextColor: "#000000"
                        sub: EzI18n.currentLanguage === "en" ? "Optimized client environment with the managed core stack" : "Optimierte Client-Umgebung mit verwaltetem Core-Stack"
                        mods: "EzClient.jar · Sodium · Lithium · Iris Shaders"
                        selected: root.selectedPreset === "ezclient"
                        onClicked: root.selectedPreset = "ezclient"
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
                        onClicked: root.step = "mod_selection"
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
                { name: "Simple Voice Chat", slug: "simple-voice-chat", desc: "Proximity Voice Chat im Spiel", icon: "https://cdn.modrinth.com/data/9eGKb6K1/icon.png" },
                { name: "Essential Mod", slug: "essential", desc: "Freunde einladen, Welt-Hosting und Kosmetika", icon: "https://cdn.modrinth.com/data/k2ZPuTBm/7f7ac7cf2a46d5f02e9644372c44b3095ad61ffb_96.webp" }
            ]
            property var selectedMods: []

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
                    text: EzI18n.currentLanguage === "en" ? "Recommended mods" : "Empfohlene Mods"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 22
                    font.bold: true
                    color: EzTheme.text
                }

                Item { height: 4 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Optional additions—nothing is selected by default." : "Optionale Ergänzungen – standardmäßig ist nichts ausgewählt."
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
                                    
                                    Image {
                                        source: "icons/check.svg"
                                        width: 12
                                        height: 12
                                        anchors.centerIn: parent
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

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    Rectangle {
                        Layout.preferredWidth: 130
                        height: 44
                        radius: EzTheme.radius
                        color: skipModsMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: EzTheme.border
                        border.width: 1

                        Text {
                            text: EzI18n.currentLanguage === "en" ? "Skip" : "Überspringen"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: EzTheme.text
                            anchors.centerIn: parent
                        }

                        MouseArea {
                            id: skipModsMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: root.beginProfileSetup([])
                        }
                    }

                    Rectangle {
                        Layout.fillWidth: true
                        height: 44
                        radius: EzTheme.radius
                        color: finalInstallBtnMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent

                        Text {
                            text: EzI18n.currentLanguage === "en" ? "Install selected →" : "Ausgewählte installieren →"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
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
                                var selected = []
                                for (var i = 0; i < modSelectionItem.selectedMods.length; i++) {
                                    selected.push(modSelectionItem.selectedMods[i])
                                }
                                root.beginProfileSetup(selected)
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
                    Image {
                        source: root.downloadProgress >= 1.0 ? "icons/check.svg" : "icons/zap.svg"
                        width: 28
                        height: 28
                        anchors.centerIn: parent
                        fillMode: Image.PreserveAspectFit
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


}
