import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root
    signal profileCreated

    // Wizard steps: "welcome" → "account" → "norisk_detected" (optional) → "create" → "preset" → "mod_selection" → "downloading"
    property string step: "welcome"

    // Form state
    property string newName: ""
    property string newVersion: "26.2"
    property string newLoader: "Fabric"
    property string selectedPreset: "ezclient" // "ezclient" | "raw" | "performance"
    property string selectedIcon: "ezclient"

    function allMinecraftVersions() {
        var result = []
        var families = profileController ? profileController.gameVersionFamilies : []
        for (var familyIndex = 0; familyIndex < families.length; familyIndex++) {
            var releases = families[familyIndex].releases
            for (var releaseIndex = 0; releaseIndex < releases.length; releaseIndex++) result.push(releases[releaseIndex].version)
        }
        return result
    }

    function hasEzClient(version) {
        var families = profileController ? profileController.gameVersionFamilies : []
        for (var familyIndex = 0; familyIndex < families.length; familyIndex++) {
            var releases = families[familyIndex].releases
            for (var releaseIndex = 0; releaseIndex < releases.length; releaseIndex++) {
                if (releases[releaseIndex].version === version) return releases[releaseIndex].hasEzClient
            }
        }
        return false
    }

    function hasFabric(version) {
        var families = profileController ? profileController.gameVersionFamilies : []
        for (var familyIndex = 0; familyIndex < families.length; familyIndex++) {
            var releases = families[familyIndex].releases
            for (var releaseIndex = 0; releaseIndex < releases.length; releaseIndex++) {
                if (releases[releaseIndex].version === version) return releases[releaseIndex].hasFabric
            }
        }
        return false
    }

    // Download / loading state from backend
    property real downloadProgress: 0.0
    property string downloadStatus: "Bereite Profil vor…"
    property bool setupFailed: false
    property bool noriskAddPerformance: true

    function reset() {
        if (typeof completeTimer !== "undefined" && completeTimer) {
            completeTimer.stop()
        }
        step = "welcome"
        newName = ""
        newVersion = "26.2"
        newLoader = "Fabric"
        selectedPreset = "ezclient"
        selectedIcon = "ezclient"
        downloadProgress = 0.0
        downloadStatus = EzI18n.currentLanguage === "en" ? "Preparing profile…" : "Bereite Profil vor…"
        setupFailed = false
        noriskAddPerformance = true
        if (typeof nameInput !== "undefined" && nameInput) {
            nameInput.text = ""
        }
        if (typeof modSelectionItem !== "undefined" && modSelectionItem) {
            modSelectionItem.selectedMods = []
        }
        if (typeof profileController !== "undefined" && profileController) {
            profileController.scanNoRiskProfiles()
        }
    }

    onVisibleChanged: {
        if (visible) {
            root.reset()
        }
    }

    Component.onCompleted: {
        if (typeof profileController !== "undefined" && profileController) {
            profileController.scanNoRiskProfiles()
        }
    }

    function beginProfileSetup(optionalMods) {
        root.step = "downloading"
        root.downloadProgress = 0.0
        root.setupFailed = false
        root.downloadStatus = EzI18n.currentLanguage === "en" ? "Initializing profile…" : "Initialisiere Profil…"
        if (typeof profileController !== "undefined" && profileController) {
            profileController.createAndOnboard(
                root.newName.trim(), root.newVersion, root.newLoader,
                root.selectedPreset, optionalMods, root.selectedIcon
            )
        }
    }

    function importNoRisk(profileId) {
        root.step = "downloading"
        root.downloadProgress = 0.05
        root.setupFailed = false
        root.downloadStatus = "Importiere NoRisk-Profil…"
        if (typeof profileController !== "undefined" && profileController) {
            profileController.importNoRiskProfile(profileId, root.noriskAddPerformance)
        }
    }

    Connections {
        target: typeof profileController !== "undefined" ? profileController : null
        function onOnboardingStepProgress(progress, modName, statusText) {
            root.downloadProgress = progress
            root.downloadStatus = statusText
            if (modName === "Fehler") root.setupFailed = true
        }
        function onOnboardingFinished(profileId) {
            root.downloadProgress = 1.0
            root.downloadStatus = "Profil erfolgreich eingerichtet & optimiert!"
            completeTimer.start()
        }
        function onNoriskImportProgress(progress, statusText) {
            root.downloadProgress = progress
            root.downloadStatus = statusText
        }
        function onNoriskImportFinished(profileId, success, message) {
            if (success) {
                root.downloadProgress = 1.0
                root.downloadStatus = "NoRisk-Profil erfolgreich übernommen!"
                completeTimer.start()
            } else {
                root.downloadStatus = message
                root.setupFailed = true
            }
        }
    }

    Timer {
        id: completeTimer
        interval: 500
        repeat: false
        onTriggered: {
            root.profileCreated()
            root.reset()
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
            width: 540
            height: 540
            radius: 270
            color: EzTheme.accentGlow
            opacity: 0.4
            Behavior on anchors.verticalCenterOffset { NumberAnimation { duration: 280; easing.type: Easing.OutQuad } }
        }

        // ──────────────────────────────────────────
        //  STEP 1: WELCOME
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: 520
            height: 440
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
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { height: 6 }

                Text {
                    text: EzI18n.t("onboard_tagline", "Dein moderner, schneller und optimierter Minecraft Launcher")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 13
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
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
                    width: 260; height: 44
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
                    horizontalAlignment: Text.AlignHCenter
                }
            }
        }

        // ──────────────────────────────────────────
        //  STEP 1.5: MINECRAFT / MICROSOFT ACCOUNT
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: 520
            height: 440
            visible: opacity > 0.001
            opacity: root.step === "account" ? 1.0 : 0.0
            scale: root.step === "account" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Top Navigation Bar
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

                    Item { Layout.fillWidth: true }

                    Text {
                        text: EzI18n.currentLanguage === "en" ? "Step 1 of 3 · Minecraft Account" : "Schritt 1 von 3 · Minecraft Konto"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Item { height: 20 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Connect Minecraft Account" : "Minecraft-Konto verbinden"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 24
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { height: 6 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Sign in with your Microsoft account to play Minecraft online." : "Melde dich mit deinem Microsoft-Konto an, um online auf Servern zu spielen."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 13
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                    Layout.fillWidth: true
                    wrapMode: Text.WordWrap
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
                    Layout.alignment: Qt.AlignHCenter
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
                            if (typeof profileController !== "undefined" && profileController && profileController.noriskProfiles && profileController.noriskProfiles.length > 0) {
                                root.step = "norisk_detected"
                            } else {
                                root.step = "create"
                                nameInput.forceActiveFocus()
                            }
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────
        //  STEP 1.8: NORISK DETECTED FLOW
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: Math.min(760, Math.max(520, root.width - 80))
            height: Math.min(620, Math.max(500, root.height - 72))
            visible: opacity > 0.001
            opacity: root.step === "norisk_detected" ? 1.0 : 0.0
            scale: root.step === "norisk_detected" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Top Navigation Bar
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 8

                    Rectangle {
                        width: 30; height: 30; radius: 6
                        color: backMouseNr.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: EzTheme.border; border.width: 1
                        Behavior on color { ColorAnimation { duration: 100 } }

                        Text { text: "←"; font.family: EzTheme.fontFamily; font.pixelSize: 13; color: EzTheme.text; anchors.centerIn: parent }
                        MouseArea {
                            id: backMouseNr
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                root.forceActiveFocus()
                                root.step = "account"
                            }
                        }
                    }

                    Item { Layout.fillWidth: true }

                    Rectangle {
                        height: 22; radius: 11; color: "#FF553315"; border.color: "#FF5533"; border.width: 1
                        Layout.preferredWidth: nrBadgeText.implicitWidth + 16
                        Text { id: nrBadgeText; text: "⚡ NoRiskClient erkannt"; font.family: EzTheme.fontFamily; font.pixelSize: 10; font.bold: true; color: "#FF7744"; anchors.centerIn: parent }
                    }
                }

                Item { Layout.preferredHeight: 14 }

                Text {
                    text: "NoRisk-Profile übernehmen?"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 24
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { Layout.preferredHeight: 6 }

                Text {
                    text: "Wähle ein vorhandenes Profil. Spielstände, Einstellungen und Mods werden in ein neues EzClient-Profil kopiert."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                    Layout.fillWidth: true
                    wrapMode: Text.WordWrap
                }

                Item { Layout.preferredHeight: 16 }

                // Performance toggle checkbox
                Rectangle {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 44
                    radius: 8
                    color: optCheckMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                    border.color: root.noriskAddPerformance ? EzTheme.accent : EzTheme.border
                    border.width: 1

                    RowLayout {
                        anchors.fill: parent
                        anchors.margins: 10
                        spacing: 8

                        Rectangle {
                            width: 16; height: 16; radius: 4
                            color: root.noriskAddPerformance ? EzTheme.accent : "transparent"
                            border.color: root.noriskAddPerformance ? EzTheme.accent : EzTheme.borderLight
                            border.width: 1
                            Image { source: "icons/check.svg"; width: 10; height: 10; anchors.centerIn: parent; visible: root.noriskAddPerformance }
                        }

                        Text {
                            text: "✨ EzClient Performance & Core Mod hinzufügen (Empfohlen)"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            font.bold: true
                            color: root.noriskAddPerformance ? EzTheme.accentLight : EzTheme.textSecondary
                            Layout.fillWidth: true
                            elide: Text.ElideRight
                        }
                    }

                    MouseArea {
                        id: optCheckMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.noriskAddPerformance = !root.noriskAddPerformance
                    }
                }

                Item { Layout.preferredHeight: 14 }

                RowLayout {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 22

                    Text {
                        text: "Gefundene Profile"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        font.bold: true
                        color: EzTheme.textSecondary
                    }
                    Item { Layout.fillWidth: true }
                    Text {
                        text: ((typeof profileController !== "undefined" && profileController) ? profileController.noriskProfiles.length : 0) + " Profile"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 10
                        color: EzTheme.textMuted
                    }
                }

                Item { Layout.preferredHeight: 6 }

                // Profile list
                Rectangle {
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    color: EzTheme.surface2
                    radius: EzTheme.radiusSm
                    border.color: EzTheme.border
                    border.width: 1
                    clip: true

                    ListView {
                        id: noriskProfileList
                        anchors.fill: parent
                        anchors.margins: 8
                        anchors.rightMargin: 14
                        spacing: 8
                        clip: true
                        boundsBehavior: Flickable.StopAtBounds
                        model: (typeof profileController !== "undefined" && profileController) ? profileController.noriskProfiles : []
                        ScrollBar.vertical: ScrollBar {
                            policy: ScrollBar.AsNeeded
                            width: 6
                        }

                        delegate: Rectangle {
                            width: ListView.view.width
                            height: 74
                            radius: 10
                            color: pItemMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface
                            border.color: pItemMouse.containsMouse ? EzTheme.borderLight : EzTheme.border
                            border.width: 1

                            RowLayout {
                                anchors.fill: parent
                                anchors.leftMargin: 12
                                anchors.rightMargin: 10
                                anchors.topMargin: 9
                                anchors.bottomMargin: 9
                                spacing: 12

                                Rectangle {
                                    Layout.preferredWidth: 44
                                    Layout.preferredHeight: 44
                                    radius: 10
                                    color: "#171B24"
                                    border.color: "#343B49"
                                    border.width: 1

                                    Image {
                                        source: "icons/client-norisk.png"
                                        width: 24; height: 24
                                        anchors.centerIn: parent
                                        fillMode: Image.PreserveAspectFit
                                        smooth: true
                                    }
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    Layout.minimumWidth: 0
                                    spacing: 2
                                    Text {
                                        text: modelData.name || "Unbenanntes NoRisk-Profil"
                                        font.family: EzTheme.mcFontFamily
                                        font.pixelSize: 13
                                        font.bold: true
                                        color: EzTheme.text
                                        Layout.fillWidth: true
                                        elide: Text.ElideRight
                                        maximumLineCount: 1
                                    }
                                    Text {
                                        text: "Minecraft " + (modelData.version || "Unbekannt") + "  ·  " + (modelData.loader || "Vanilla") + "  ·  " + (modelData.modCount || 0) + " Mods"
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 10
                                        color: EzTheme.textMuted
                                        Layout.fillWidth: true
                                        elide: Text.ElideRight
                                        maximumLineCount: 1
                                    }
                                    Text {
                                        text: ((modelData.ramMb || 4096) / 1024).toFixed(1).replace(".0", "") + " GB RAM"
                                        font.family: EzTheme.fontFamily
                                        font.pixelSize: 9
                                        color: EzTheme.textSubtle
                                    }
                                }

                                EzButton {
                                    text: "Importieren"
                                    primary: true
                                    mcFont: true
                                    Layout.preferredHeight: 36
                                    Layout.preferredWidth: 112
                                    onClicked: root.importNoRisk(modelData.id)
                                }
                            }

                            MouseArea {
                                id: pItemMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                acceptedButtons: Qt.NoButton
                            }
                        }
                    }
                }

                Item { Layout.preferredHeight: 16 }

                // Bottom skip button
                RowLayout {
                    Layout.fillWidth: true
                    Item { Layout.fillWidth: true }
                    Text {
                        text: "Neues Standard-Profil erstellen →"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        font.bold: true
                        color: EzTheme.accent
                        MouseArea {
                            anchors.fill: parent
                            anchors.margins: -4
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                root.step = "create"
                                nameInput.forceActiveFocus()
                            }
                        }
                    }
                    Item { Layout.fillWidth: true }
                }
            }
        }

        // ──────────────────────────────────────────
        //  STEP 2: BASIC PROFILE CONFIG (Name, Version, Loader)
        // ──────────────────────────────────────────
        Item {
            anchors.centerIn: parent
            width: 520
            height: 580
            visible: opacity > 0.001
            opacity: root.step === "create" ? 1.0 : 0.0
            scale: root.step === "create" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Top Navigation Bar
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
                                if (typeof profileController !== "undefined" && profileController && profileController.noriskProfiles && profileController.noriskProfiles.length > 0) {
                                    root.step = "norisk_detected"
                                } else {
                                    root.step = "account"
                                }
                            }
                        }
                    }

                    Item { Layout.fillWidth: true }

                    Text {
                        text: EzI18n.currentLanguage === "en" ? "Step 2 of 3 · Basic Settings" : "Schritt 2 von 3 · Basis-Einstellungen"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Item { height: 18 }

                Text {
                    text: EzI18n.t("onboard_step2_profile", "Erstes Profil erstellen")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 24
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { height: 6 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Choose a name, Minecraft version, and mod loader." : "Wähle einen Namen, Version und Mod-Loader."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 13
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { height: 22 }

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

                Item { height: 12 }

                // Profile Icon Selector
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 5

                    RowLayout {
                        spacing: 6
                        Text {
                            text: EzI18n.currentLanguage === "en" ? "Profile Icon" : "Profil-Icon"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 11
                            font.bold: true
                            color: EzTheme.textSecondary
                        }
                        Text {
                            text: EzI18n.currentLanguage === "en" ? "(Preset or custom PNG)" : "(Preset oder eigenes PNG)"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            color: EzTheme.textSubtle
                        }
                    }

                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 8

                        ProfileIcon {
                            Layout.preferredWidth: 32
                            Layout.preferredHeight: 32
                            radius: 7
                            iconNameOrPath: root.selectedIcon
                            fallbackName: root.newName.trim() || "EZ"
                        }

                        Row {
                            spacing: 5
                            Repeater {
                                model: ["ezclient", "norisk", "box", "tnt", "potion", "clock", "flame", "sparkles", "shield", "compass", "zap"]
                                Rectangle {
                                    width: 30
                                    height: 30
                                    radius: 6
                                    color: (root.selectedIcon === modelData) ? EzTheme.surfaceActive : (obIconMouse.containsMouse ? EzTheme.surfaceHover : EzTheme.surface2)
                                    border.color: (root.selectedIcon === modelData) ? EzTheme.accent : EzTheme.borderLight
                                    border.width: (root.selectedIcon === modelData) ? 1.5 : 1

                                    ProfileIcon {
                                        anchors.fill: parent
                                        anchors.margins: 2
                                        iconNameOrPath: modelData
                                        radius: 5
                                        bgColor: "transparent"
                                        borderColor: "transparent"
                                        borderWidth: 0
                                    }

                                    MouseArea {
                                        id: obIconMouse
                                        anchors.fill: parent
                                        hoverEnabled: true
                                        cursorShape: Qt.PointingHandCursor
                                        onClicked: root.selectedIcon = modelData
                                    }
                                }
                            }
                        }

                        Rectangle {
                            height: 30
                            width: obCustomPngTxt.implicitWidth + 14
                            radius: 6
                            color: obUploadMouse.containsMouse ? EzTheme.surfaceHover : EzTheme.surface2
                            border.color: obUploadMouse.containsMouse ? EzTheme.accent : EzTheme.borderLight
                            border.width: 1

                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 4
                                Text {
                                    id: obCustomPngTxt
                                    text: "+ PNG"
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: EzTheme.accentLight
                                }
                            }

                            MouseArea {
                                id: obUploadMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    if (typeof profileController !== "undefined" && profileController) {
                                        var path = profileController.pickProfileIconImage()
                                        if (path && path !== "") {
                                            root.selectedIcon = path
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Item { height: 14 }

                // Version dropdown
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 6

                    Text { text: EzI18n.t("onboard_profile_version", "Minecraft Version"); font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }

                    EzDropDown {
                        id: versionPicker
                        Layout.fillWidth: true
                        currentIndex: 0
                        choices: root.allMinecraftVersions()
                        formatEzClientSupported: true
                        onChoiceChanged: {
                            root.newVersion = choices[currentIndex]
                            if (root.hasEzClient(root.newVersion)) {
                                root.newLoader = "Fabric"
                                root.selectedPreset = "ezclient"
                            } else if (root.hasFabric(root.newVersion)) {
                                root.newLoader = "Fabric"
                                if (root.selectedPreset === "ezclient") {
                                    root.selectedPreset = "performance"
                                }
                            } else {
                                root.newLoader = "Vanilla"
                                root.selectedPreset = "raw"
                            }
                        }
                    }
                }

                Item { height: 16 }

                // Mod-Loader / Game Variant Cards (EzClient vs Fabric vs Vanilla vs Forge)
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 6

                    Text { text: EzI18n.currentLanguage === "en" ? "Game Variant & Mod-Loader" : "Spielvariante & Mod-Loader"; font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }

                    GridLayout {
                        Layout.fillWidth: true
                        columns: 2
                        columnSpacing: 10
                        rowSpacing: 10

                        // EzClient Card (Recommended / Default)
                        Rectangle {
                            id: ezClientPresetCard
                            Layout.fillWidth: true
                            height: 56
                            radius: EzTheme.radiusSm
                            readonly property bool isSelected: root.selectedPreset === "ezclient" && root.newLoader === "Fabric"
                            readonly property bool isAvailable: root.hasEzClient(root.newVersion)
                            color: isSelected ? EzTheme.surfaceActive : (ezMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                            border.color: isSelected ? EzTheme.accent : (ezMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                            border.width: isSelected ? 1.5 : 1
                            scale: ezMouse.containsMouse && isAvailable ? 1.02 : 1.0
                            opacity: isAvailable ? 1.0 : 0.4

                            Behavior on color { ColorAnimation { duration: 110 } }
                            Behavior on border.color { ColorAnimation { duration: 110 } }
                            Behavior on scale { NumberAnimation { duration: 110 } }

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 10
                                spacing: 10

                                Image {
                                    source: "assets/logo.svg"
                                    Layout.preferredWidth: 22
                                    Layout.preferredHeight: 22
                                    fillMode: Image.PreserveAspectFit
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    RowLayout {
                                        spacing: 6
                                        Text { text: "EzClient"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                                        Rectangle {
                                            height: 14; width: recText.implicitWidth + 6; radius: 3; color: EzTheme.accent
                                            Text { id: recText; text: EzI18n.currentLanguage === "en" ? "REC" : "EMPFOHLEN"; font.family: EzTheme.fontFamily; font.pixelSize: 8; font.bold: true; color: "#000000"; anchors.centerIn: parent }
                                        }
                                    }
                                    Text { text: EzI18n.currentLanguage === "en" ? "Full client · Ultra FPS & features" : "Vollversion · Ultra-FPS & Features"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.accentLight; Layout.fillWidth: true; elide: Text.ElideRight }
                                }

                                Rectangle {
                                    width: 16; height: 16; radius: 8
                                    color: ezClientPresetCard.isSelected ? EzTheme.accent : "transparent"
                                    border.color: ezClientPresetCard.isSelected ? EzTheme.accent : EzTheme.borderLight
                                    border.width: 1.5
                                    Rectangle { width: 6; height: 6; radius: 3; color: "#000"; anchors.centerIn: parent; visible: ezClientPresetCard.isSelected }
                                }
                            }

                            MouseArea {
                                id: ezMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: parent.isAvailable ? Qt.PointingHandCursor : Qt.ForbiddenCursor
                                onClicked: {
                                    if (parent.isAvailable) {
                                        root.newLoader = "Fabric"
                                        root.selectedPreset = "ezclient"
                                    }
                                }
                            }
                        }

                        // Fabric Card
                        Rectangle {
                            id: fabricPresetCard
                            Layout.fillWidth: true
                            height: 56
                            radius: EzTheme.radiusSm
                            readonly property bool isSelected: root.selectedPreset === "performance" && root.newLoader === "Fabric"
                            readonly property bool isAvailable: root.hasFabric(root.newVersion)
                            color: isSelected ? EzTheme.surfaceActive : (fabMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                            border.color: isSelected ? EzTheme.accent : (fabMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                            border.width: isSelected ? 1.5 : 1
                            scale: fabMouse.containsMouse && isAvailable ? 1.02 : 1.0
                            opacity: isAvailable ? 1.0 : 0.4

                            Behavior on color { ColorAnimation { duration: 110 } }
                            Behavior on border.color { ColorAnimation { duration: 110 } }
                            Behavior on scale { NumberAnimation { duration: 110 } }

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 10
                                spacing: 10

                                Image {
                                    source: "assets/fabric-logo.png"
                                    Layout.preferredWidth: 22
                                    Layout.preferredHeight: 22
                                    fillMode: Image.PreserveAspectFit
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: "Fabric"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                                    Text { text: EzI18n.currentLanguage === "en" ? "Lightweight & FPS · without EzClient" : "Leicht & FPS · ohne EzClient"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted; Layout.fillWidth: true; elide: Text.ElideRight }
                                }

                                Rectangle {
                                    width: 16; height: 16; radius: 8
                                    color: fabricPresetCard.isSelected ? EzTheme.accent : "transparent"
                                    border.color: fabricPresetCard.isSelected ? EzTheme.accent : EzTheme.borderLight
                                    border.width: 1.5
                                    Rectangle { width: 6; height: 6; radius: 3; color: "#000"; anchors.centerIn: parent; visible: fabricPresetCard.isSelected }
                                }
                            }

                            MouseArea {
                                id: fabMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: parent.isAvailable ? Qt.PointingHandCursor : Qt.ForbiddenCursor
                                onClicked: {
                                    if (parent.isAvailable) {
                                        root.newLoader = "Fabric"
                                        root.selectedPreset = "performance"
                                    }
                                }
                            }
                        }

                        // Vanilla Card
                        Rectangle {
                            id: vanillaPresetCard
                            Layout.fillWidth: true
                            height: 56
                            radius: EzTheme.radiusSm
                            readonly property bool isSelected: root.newLoader === "Vanilla"
                            color: isSelected ? EzTheme.surfaceActive : (forgeMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                            border.color: isSelected ? EzTheme.accent : (forgeMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                            border.width: isSelected ? 1.5 : 1
                            scale: forgeMouse.containsMouse ? 1.02 : 1.0

                            Behavior on color { ColorAnimation { duration: 110 } }
                            Behavior on border.color { ColorAnimation { duration: 110 } }
                            Behavior on scale { NumberAnimation { duration: 110 } }

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 10
                                spacing: 10

                                Image {
                                    source: "icons/loader-vanilla.svg"
                                    Layout.preferredWidth: 22
                                    Layout.preferredHeight: 22
                                    fillMode: Image.PreserveAspectFit
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: "Vanilla"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                                    Text { text: EzI18n.currentLanguage === "en" ? "Original game without loader" : "Originalspiel ohne Mod-Loader"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted; Layout.fillWidth: true; elide: Text.ElideRight }
                                }

                                Rectangle {
                                    width: 16; height: 16; radius: 8
                                    color: vanillaPresetCard.isSelected ? EzTheme.accent : "transparent"
                                    border.color: vanillaPresetCard.isSelected ? EzTheme.accent : EzTheme.borderLight
                                    border.width: 1.5
                                    Rectangle { width: 6; height: 6; radius: 3; color: "#000"; anchors.centerIn: parent; visible: vanillaPresetCard.isSelected }
                                }
                            }

                            MouseArea {
                                id: forgeMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    root.newLoader = "Vanilla"
                                    root.selectedPreset = "raw"
                                }
                            }
                        }

                        // Forge Card
                        Rectangle {
                            id: forgePresetCard
                            Layout.fillWidth: true
                            height: 56
                            radius: EzTheme.radiusSm
                            readonly property bool isSelected: root.newLoader === "Forge"
                            color: isSelected ? EzTheme.surfaceActive : (forgeLoaderMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                            border.color: isSelected ? EzTheme.accent : EzTheme.border
                            border.width: isSelected ? 1.5 : 1
                            scale: forgeLoaderMouse.containsMouse ? 1.02 : 1.0

                            Behavior on color { ColorAnimation { duration: 110 } }
                            Behavior on border.color { ColorAnimation { duration: 110 } }
                            Behavior on scale { NumberAnimation { duration: 110 } }

                            RowLayout {
                                anchors.fill: parent
                                anchors.margins: 10
                                spacing: 10

                                Image {
                                    source: "icons/forge.svg"
                                    Layout.preferredWidth: 22
                                    Layout.preferredHeight: 22
                                    fillMode: Image.PreserveAspectFit
                                }

                                ColumnLayout {
                                    Layout.fillWidth: true
                                    spacing: 2
                                    Text { text: "Forge"; font.family: EzTheme.fontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                                    Text { text: EzI18n.currentLanguage === "en" ? "Forge mods · without EzClient" : "Forge-Mods · ohne EzClient"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted; Layout.fillWidth: true; elide: Text.ElideRight }
                                }

                                Rectangle {
                                    width: 16; height: 16; radius: 8
                                    color: forgePresetCard.isSelected ? EzTheme.accent : "transparent"
                                    border.color: forgePresetCard.isSelected ? EzTheme.accent : EzTheme.borderLight
                                    border.width: 1.5
                                    Rectangle { width: 6; height: 6; radius: 3; color: "#000"; anchors.centerIn: parent; visible: forgePresetCard.isSelected }
                                }
                            }

                            MouseArea {
                                id: forgeLoaderMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    root.newLoader = "Forge"
                                    root.selectedPreset = "raw"
                                }
                            }
                        }
                    }
                }

                Item { height: 26 }

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
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 12
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
            width: 520
            height: 490
            visible: opacity > 0.001
            opacity: root.step === "preset" ? 1.0 : 0.0
            scale: root.step === "preset" ? 1.0 : 0.95

            Behavior on opacity { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }
            Behavior on scale { NumberAnimation { duration: 220; easing.type: Easing.OutCubic } }

            ColumnLayout {
                anchors.fill: parent
                spacing: 0

                // Top Navigation Bar
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

                    Item { Layout.fillWidth: true }

                    Text {
                        text: EzI18n.currentLanguage === "en" ? "Step 3 of 3 · Optimization Setup" : "Schritt 3 von 3 · Optimierungs-Paket"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Item { height: 18 }

                Text {
                    text: EzI18n.t("onboard_profile_preset", "Wähle deine Ausstattung")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 24
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { height: 6 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Choose an optimized EzClient environment or a clean Raw profile." : "Wähle eine optimierte EzClient-Umgebung oder ein sauberes Raw-Profil."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { height: 18 }

                // Preset Cards Stack
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    // Option 1: EzClient (recommended)
                    EzPresetCard {
                        visible: root.hasEzClient(root.newVersion)
                        presetKey: "ezclient"
                        title: "EzClient"
                        tag: EzI18n.currentLanguage === "en" ? "RECOMMENDED" : "EMPFOHLEN"
                        tagColor: EzTheme.accent
                        tagTextColor: "#000000"
                        sub: EzI18n.currentLanguage === "en" ? "Optimized client environment with the managed core stack" : "Optimierte Client-Umgebung mit verwaltetem Core-Stack"
                        mods: "EzClient Vollversion · Sodium · Lithium · Iris Shaders"
                        selected: root.selectedPreset === "ezclient"
                        onClicked: {
                            root.newLoader = "Fabric"
                            root.selectedPreset = "ezclient"
                        }
                    }

                    EzPresetCard {
                        visible: root.newLoader === "Fabric"
                        presetKey: "performance"
                        title: "Fabric Performance"
                        tag: "EMPFOHLEN"
                        tagColor: EzTheme.surface3
                        tagTextColor: EzTheme.text
                        sub: "Stabiles Fabric-Profil ohne EzClient Core"
                        mods: "Sodium · Lithium · Iris Shaders"
                        selected: root.selectedPreset === "performance"
                        onClicked: {
                            root.newLoader = "Fabric"
                            root.selectedPreset = "performance"
                        }
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
                        text: EzI18n.t("onboard_btn_create", "Weiter zur Mod-Auswahl →")
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 12
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
            width: 520
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

                // Top Navigation Bar
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 8

                    Rectangle {
                        width: 30; height: 30; radius: 6
                        color: backMouseMods.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: EzTheme.border; border.width: 1
                        Behavior on color { ColorAnimation { duration: 100 } }

                        Text { text: "←"; font.family: EzTheme.fontFamily; font.pixelSize: 13; color: EzTheme.text; anchors.centerIn: parent }
                        MouseArea {
                            id: backMouseMods
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: root.step = "preset"
                        }
                    }

                    Item { Layout.fillWidth: true }

                    Text {
                        text: EzI18n.currentLanguage === "en" ? "Optional Mods" : "Optionale Mods"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Item { height: 18 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Recommended mods" : "Empfohlene Mods"
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 24
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { height: 6 }

                Text {
                    text: EzI18n.currentLanguage === "en" ? "Optional additions—nothing is selected by default." : "Optionale Ergänzungen – standardmäßig ist nichts ausgewählt."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
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
                            height: 52
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
                                        font.pixelSize: 13
                                        font.bold: true
                                        font.family: EzTheme.mcFontFamily
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
                    spacing: 12

                    Rectangle {
                        Layout.preferredWidth: 140
                        height: 44
                        radius: EzTheme.radius
                        color: skipModsMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: EzTheme.border
                        border.width: 1

                        Text {
                            text: EzI18n.currentLanguage === "en" ? "Skip" : "Überspringen"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 11
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
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 11
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
            width: 520
            height: 340
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
                    }

                    Image {
                        source: root.setupFailed ? "icons/x.svg" : (root.downloadProgress >= 1.0 ? "icons/check.svg" : "icons/zap.svg")
                        width: 28
                        height: 28
                        anchors.centerIn: parent
                        fillMode: Image.PreserveAspectFit
                    }
                }

                Item { height: 26 }

                Text {
                    text: root.setupFailed ? "Profil konnte nicht eingerichtet werden" : (root.downloadProgress >= 1.0 ? "Profil fertiggestellt!" : "Richte " + root.newName + " ein…")
                    font.family: EzTheme.mcFontFamily
                    font.pixelSize: 22
                    font.bold: true
                    color: EzTheme.text
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
                }

                Item { height: 8 }

                Text {
                    text: root.downloadStatus
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.textSecondary
                    Layout.alignment: Qt.AlignHCenter
                    horizontalAlignment: Text.AlignHCenter
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
                        text: (root.selectedPreset === "ezclient" ? "EzClient" : root.newLoader) + " " + root.newVersion
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textSubtle
                    }
                    Item { Layout.fillWidth: true }
                    Text {
                        text: Math.round(root.downloadProgress * 100) + "%"
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 12
                        font.bold: true
                        color: EzTheme.accentLight
                    }
                }

                Item { height: root.setupFailed ? 18 : 0 }

                Rectangle {
                    visible: root.setupFailed
                    Layout.alignment: Qt.AlignHCenter
                    width: 150
                    height: visible ? 38 : 0
                    radius: EzTheme.radiusSm
                    color: retryMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                    border.color: EzTheme.border
                    Text {
                        anchors.centerIn: parent
                        text: "Zurück"
                        color: EzTheme.text
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 12
                        font.bold: true
                    }
                    MouseArea {
                        id: retryMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.step = "create"
                    }
                }
            }
        }
    }
}
