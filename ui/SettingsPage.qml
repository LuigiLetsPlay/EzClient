import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root

    // Background click handler to deselect / defocus input fields
    MouseArea {
        anchors.fill: parent
        onClicked: root.forceActiveFocus()
    }

    ScrollView {
        anchors.fill: parent
        clip: true
        contentWidth: availableWidth

        
        ColumnLayout {
            anchors.left: parent.left
            anchors.right: parent.right
            anchors.leftMargin: 32
            anchors.rightMargin: 32
            spacing: 0

            Item { height: 20 }

            Text {
                text: EzI18n.t("settings_title", "EINSTELLUNGEN & UTILITIES")
                font.family: EzTheme.mcFontFamily
                font.pixelSize: 18
                font.bold: true
                color: EzTheme.text
            }

            Item { height: 16 }

            // ─── LANGUAGE & APPEARANCE CARD ───
            Text { text: EzI18n.t("settings_lang_title", "SPRACHE & ERSCHEINUNGSBILD"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 76

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 14
                    spacing: 14

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 2
                        Text {
                            text: EzI18n.t("settings_lang", "Sprache") + " / Language"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 13
                            font.bold: true
                            color: EzTheme.text
                        }
                        Text {
                            text: EzI18n.currentLanguage === "en" ? "Select the launcher display language" : "Wähle die Anzeigesprache des Launchers"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            color: EzTheme.textMuted
                        }
                    }

                    // Language Pills (DE / EN)
                    Row {
                        spacing: 8

                        Rectangle {
                            width: 110
                            height: 34
                            radius: 6
                            color: EzI18n.currentLanguage === "de" ? EzTheme.accentDark : (sDeMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                            border.color: EzI18n.currentLanguage === "de" ? EzTheme.accent : EzTheme.border
                            border.width: 1

                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 6
                                Text { text: "🇩🇪"; font.pixelSize: 12 }
                                Text {
                                    text: "Deutsch"
                                    font.family: EzTheme.mcFontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: EzI18n.currentLanguage === "de" ? EzTheme.accentLight : EzTheme.text
                                }
                            }

                            MouseArea {
                                id: sDeMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: EzI18n.setLanguage("de")
                            }
                        }

                        Rectangle {
                            width: 110
                            height: 34
                            radius: 6
                            color: EzI18n.currentLanguage === "en" ? EzTheme.accentDark : (sEnMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                            border.color: EzI18n.currentLanguage === "en" ? EzTheme.accent : EzTheme.border
                            border.width: 1

                            RowLayout {
                                anchors.centerIn: parent
                                spacing: 6
                                Text { text: "🇬🇧"; font.pixelSize: 12 }
                                Text {
                                    text: "English"
                                    font.family: EzTheme.mcFontFamily
                                    font.pixelSize: 10
                                    font.bold: true
                                    color: EzI18n.currentLanguage === "en" ? EzTheme.accentLight : EzTheme.text
                                }
                            }

                            MouseArea {
                                id: sEnMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: EzI18n.setLanguage("en")
                            }
                        }
                    }
                }
            }

            Item { height: 10 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 76
                RowLayout {
                    anchors.fill: parent; anchors.margins: 14; spacing: 14
                    ColumnLayout {
                        Layout.fillWidth: true; spacing: 2
                        Text { text: "Theme-Farbe"; font.family: EzTheme.mcFontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                        Text { text: "Akzentfarbe für Buttons, Auswahl und Hervorhebungen"; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                    }
                    Repeater {
                        model: [{ id: "green", color: "#22C55E" }, { id: "purple", color: "#A78BFA" }, { id: "blue", color: "#60A5FA" }, { id: "rose", color: "#FB7185" }, { id: "orange", color: "#FB923C" }]
                        delegate: Rectangle {
                            width: 30; height: 30; radius: 15; color: modelData.color
                            border.color: profileController.themeColor === modelData.id ? EzTheme.text : "transparent"; border.width: 2
                            MouseArea { anchors.fill: parent; cursorShape: Qt.PointingHandCursor; onClicked: profileController.setThemeColor(modelData.id) }
                        }
                    }
                }
            }

            Item { height: 10 }

            // Custom Background Setting Card
            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 76

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 14
                    spacing: 14

                    Rectangle {
                        width: 44; height: 44; radius: 8
                        color: "#0F121A"
                        border.color: EzTheme.border
                        border.width: 1
                        clip: true

                        Image {
                            anchors.fill: parent
                            source: {
                                var p = (typeof profileController !== "undefined" && profileController) ? profileController.customBackgroundImage : "";
                                if (!p) return "assets/hero_bg.jpg";
                                if (p.startsWith("file:///") || p.startsWith("http://") || p.startsWith("https://") || p.startsWith("qrc:/")) return p;
                                var clean = p.replace(/\\/g, "/");
                                return clean.startsWith("/") ? ("file://" + clean) : ("file:///" + clean);
                            }
                            fillMode: Image.PreserveAspectCrop
                        }
                    }

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 2
                        Text {
                            text: "Hintergrundbild (Hauptmenü)"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 13
                            font.bold: true
                            color: EzTheme.text
                        }
                        Text {
                            text: (typeof profileController !== "undefined" && profileController && profileController.customBackgroundImage) ? "Eigenes Bild oder Clip aktiv · " + Math.round((profileController.customBackgroundOpacity || 0.6) * 100) + "% Deckkraft" : "Standard Minecraft Artwork aktiv"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            color: EzTheme.textMuted
                        }
                    }

                    RowLayout {
                        spacing: 8
                        Layout.alignment: Qt.AlignVCenter

                        EzButton {
                            text: "Anpassen & Vorschau…"
                            mcFont: true
                            primary: true
                            implicitHeight: 32
                            onClicked: bgModal.open()
                        }

                        EzButton {
                            text: "Video auswählen…"
                            mcFont: true
                            implicitHeight: 32
                            onClicked: {
                                if (profileController) profileController.pickBackgroundClip()
                            }
                        }

                        EzButton {
                            text: "Zurücksetzen"
                            mcFont: true
                            implicitHeight: 32
                            danger: true
                            enabled: typeof profileController !== "undefined" && profileController && profileController.customBackgroundImage !== ""
                            opacity: enabled ? 1.0 : 0.4
                            onClicked: {
                                if (profileController) profileController.setCustomBackgroundImage("")
                            }
                        }
                    }
                }
            }

            Item { height: 18 }

            // ─── FONT & TYPOGRAPHY ───
            Text { text: EzI18n.currentLanguage === "en" ? "FONT & TYPOGRAPHY" : "SCHRIFTART & TYPOGRAFIE"; font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 76

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 14
                    spacing: 14

                    Rectangle {
                        width: 44; height: 44; radius: 8
                        color: EzTheme.surface3
                        border.color: EzTheme.border
                        border.width: 1
                        Text {
                            anchors.centerIn: parent
                            text: "Aa"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 18
                            color: EzTheme.text
                        }
                    }

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 2
                        Text {
                            text: EzI18n.currentLanguage === "en" ? "App Font Style" : "Launcher Schriftart"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 13
                            font.bold: true
                            color: EzTheme.text
                        }
                        Text {
                            text: EzI18n.currentLanguage === "en" ? "Choose between Minecraft, Standard or Mixed" : "Wähle zwischen Minecraft, Standard oder Gemischt"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            color: EzTheme.textMuted
                        }
                    }

                    RowLayout {
                        spacing: 8
                        Layout.alignment: Qt.AlignVCenter

                        property string activeFontMode: typeof profileController !== "undefined" && profileController ? profileController.appFontMode : "mixed"

                        EzButton {
                            text: "Minecraft"
                            implicitHeight: 32
                            primary: parent.activeFontMode === "minecraft"
                            onClicked: if (profileController) profileController.setAppFontMode("minecraft")
                        }
                        EzButton {
                            text: "Gemischt"
                            implicitHeight: 32
                            primary: parent.activeFontMode === "mixed"
                            onClicked: if (profileController) profileController.setAppFontMode("mixed")
                        }
                        EzButton {
                            text: "Standard"
                            implicitHeight: 32
                            primary: parent.activeFontMode === "standard"
                            onClicked: if (profileController) profileController.setAppFontMode("standard")
                        }
                    }
                }
            }

            Item { height: 18 }

            // ─── MINECRAFT ACCOUNT (MICROSOFT AUTH) ───
            Text { text: EzI18n.t("settings_account_title", "MINECRAFT KONTO & AUTHENTIFIZIERUNG"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 76

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 14
                    spacing: 14

                    Rectangle {
                        width: 44; height: 44; radius: 22
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
                        spacing: 2
                        Text {
                            text: typeof accountController !== "undefined" && accountController ? accountController.username : "Player"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 14
                            font.bold: true
                            color: EzTheme.text
                        }
                        Text {
                            text: (typeof accountController !== "undefined" && accountController && accountController.isOnline)
                                  ? EzI18n.t("settings_account_online", "🟢 Microsoft Auth (Online Verifiziert)")
                                  : EzI18n.t("settings_account_offline", "⚪ Lokales Konto / Offline")
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 10
                            color: (typeof accountController !== "undefined" && accountController && accountController.isOnline) ? EzTheme.accentLight : EzTheme.textMuted
                        }
                    }

                    EzButton {
                        text: EzI18n.t("settings_login_btn", "Konto anmelden")
                        mcFont: true
                        Layout.preferredHeight: 32
                        onClicked: if (typeof accountController !== "undefined" && accountController) accountController.openLoginDialog()
                    }

                    EzButton {
                        text: EzI18n.t("settings_sync_btn", "Sync")
                        Layout.preferredHeight: 32
                        onClicked: if (typeof accountController !== "undefined" && accountController) accountController.refresh()
                    }
                }
            }

            Item { height: 18 }

            // ─── QUICK UTILITY TOOLS (QoL Feature) ───
            Text { text: EzI18n.t("settings_tools_title", "SCHNELLWERKZEUGE & ORDNER"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 90

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 14
                    spacing: 12

                    // Screenshots Folder
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        radius: EzTheme.radiusSm
                        color: sMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: sMouse.containsMouse ? EzTheme.accent : EzTheme.border
                        border.width: 1
                        scale: sMouse.pressed ? 0.98 : 1.0

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Image { source: "icons/camera.svg"; width: 16; height: 16; fillMode: Image.PreserveAspectFit }
                            Text { text: EzI18n.t("settings_open_screenshots", "Screenshots"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.text }
                        }

                        MouseArea {
                            id: sMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: profileController.openScreenshotsFolder()
                        }
                    }

                    // Logs & Crash Reports Folder
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        radius: EzTheme.radiusSm
                        color: lMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: lMouse.containsMouse ? EzTheme.cyan : EzTheme.border
                        border.width: 1
                        scale: lMouse.pressed ? 0.98 : 1.0

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Image { source: "icons/file-text.svg"; width: 16; height: 16; fillMode: Image.PreserveAspectFit }
                            Text { text: EzI18n.t("settings_open_logs", "Logs & Berichte"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.text }
                        }

                        MouseArea {
                            id: lMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: profileController.openLogsFolder()
                        }
                    }

                    // Active Profile Folder
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        radius: EzTheme.radiusSm
                        color: pMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: pMouse.containsMouse ? EzTheme.warning : EzTheme.border
                        border.width: 1
                        scale: pMouse.pressed ? 0.98 : 1.0

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Image { source: "icons/folder.svg"; width: 16; height: 16; fillMode: Image.PreserveAspectFit }
                            Text { text: EzI18n.t("settings_open_profile_folder", "Profil-Ordner"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.text }
                        }

                        MouseArea {
                            id: pMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: profileController.openFolder("")
                        }
                    }

                    // Duplicate Profile Backup
                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        radius: EzTheme.radiusSm
                        color: dMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.color: dMouse.containsMouse ? EzTheme.purple : EzTheme.border
                        border.width: 1
                        scale: dMouse.pressed ? 0.98 : 1.0

                        RowLayout {
                            anchors.centerIn: parent
                            spacing: 8
                            Image { source: "icons/copy.svg"; width: 16; height: 16; fillMode: Image.PreserveAspectFit }
                            Text { text: EzI18n.t("settings_duplicate_profile", "Profil duplizieren"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.text }
                        }

                        MouseArea {
                            id: dMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: profileController.duplicateActiveProfile()
                        }
                    }
                }
            }

            Item { height: 20 }

            // ─── RAM & SPEICHER-ZUWEISUNG ───
            Text { text: EzI18n.t("settings_ram_title", "ARBEITSSPEICHER (RAM-ZUWEISUNG)"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 125

                ColumnLayout {
                    anchors.fill: parent
                    anchors.margins: 16
                    spacing: 12

                    RowLayout {
                        Layout.fillWidth: true
                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 1
                            Text { text: EzI18n.t("settings_ram_allocation", "Zugewiesener Arbeitsspeicher für Minecraft"); font.family: EzTheme.mcFontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                            Text { text: EzI18n.t("settings_ram_desc", "Empfohlen: 4 GB für Sodium & Fabric · 6-8 GB für schwere Shader"); font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                        }

                        Rectangle {
                            height: 26
                            width: ramDisplay.implicitWidth + 16
                            radius: 4
                            color: EzTheme.surfaceActive
                            border.color: EzTheme.accent
                            border.width: 1

                            Text {
                                id: ramDisplay
                                text: Math.round((profileController ? profileController.activeRamMb : 4096) / 1024) + " GB RAM"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 11
                                font.bold: true
                                color: EzTheme.accentLight
                                anchors.centerIn: parent
                            }
                        }
                    }

                    // Quick RAM Buttons
                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 8

                        Repeater {
                            model: [
                                { label: "2 GB",  mb: 2048 },
                                { label: "4 GB (Standard)", mb: 4096 },
                                { label: "6 GB",  mb: 6144 },
                                { label: "8 GB (Shader)", mb: 8192 },
                                { label: "12 GB", mb: 12288 },
                                { label: "16 GB", mb: 16384 }
                            ]

                            Rectangle {
                                Layout.fillWidth: true
                                height: 34
                                radius: EzTheme.radiusSm
                                color: (profileController && profileController.activeRamMb === modelData.mb)
                                       ? EzTheme.surfaceActive
                                       : (ramBtnMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                                border.color: (profileController && profileController.activeRamMb === modelData.mb)
                                              ? EzTheme.accent
                                              : (ramBtnMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                                border.width: 1

                                Behavior on color { ColorAnimation { duration: 100 } }
                                Behavior on border.color { ColorAnimation { duration: 100 } }

                                Text {
                                    text: modelData.label
                                    font.family: EzTheme.mcFontFamily
                                    font.pixelSize: 10
                                    font.bold: profileController && profileController.activeRamMb === modelData.mb
                                    color: (profileController && profileController.activeRamMb === modelData.mb) ? EzTheme.accentLight : EzTheme.textSecondary
                                    anchors.centerIn: parent
                                }

                                MouseArea {
                                    id: ramBtnMouse
                                    anchors.fill: parent
                                    hoverEnabled: true
                                    cursorShape: Qt.PointingHandCursor
                                    onClicked: profileController.setActiveRamMb(modelData.mb)
                                }
                            }
                        }
                    }
                }
            }

            Item { height: 20 }

            // ─── LAUNCHER ───
            Text { text: EzI18n.t("settings_launcher_title", "LAUNCHER & SPIELSTART"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: launcherCol.implicitHeight + 32

                ColumnLayout {
                    id: launcherCol
                    anchors.fill: parent
                    anchors.margins: 16
                    spacing: 0

                    EzToggleRow {
                        label: "Live-Logs Konsole beim Spielstart anzeigen"
                        sub: "Öffnet das moderne Terminal-Fenster mit Syntax-Highlighting, Filter-Pills und Echtzeit-Statistiken"
                        toggleValue: profileController ? profileController.showLiveLogs : true
                        onToggled: function(val) { if (profileController) profileController.setShowLiveLogs(val) }
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                    EzToggleRow {
                        label: EzI18n.t("settings_direct_launch", "Direktstart (Direkt mit Java starten)")
                        sub: EzI18n.t("settings_direct_launch_desc", "Startet Minecraft blitzschnell mit deinem .minecraft Token ohne Minecraft Launcher")
                        toggleValue: profileController ? profileController.preferDirectLaunch : true
                        onToggled: function(val) { if (profileController) profileController.setPreferDirectLaunch(val) }
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                    EzToggleRow {
                        label: EzI18n.t("settings_kill_official", "Minecraft Launcher automatisch beenden")
                        sub: EzI18n.t("settings_kill_official_desc", "Beendet den offiziellen Mojang Launcher sofort, falls dieser als Fallback genutzt wird")
                        toggleValue: profileController ? profileController.killOfficialLauncher : true
                        onToggled: function(val) { if (profileController) profileController.setKillOfficialLauncher(val) }
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                    EzToggleRow {
                        label: EzI18n.t("settings_close_on_launch", "In Infobereich (Tray) minimieren")
                        sub: EzI18n.t("settings_close_on_launch_desc", "Minimiert EzClient beim Starten in das Icon unten rechts")
                        toggleValue: profileController ? profileController.minimizeToTray : true
                        onToggled: function(val) { if (profileController) profileController.setMinimizeToTray(val) }
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                    EzToggleRow {
                        label: EzI18n.t("settings_check_updates", "Automatisch nach Mod-Updates suchen")
                        sub: EzI18n.t("settings_check_updates_desc", "Prüft beim Start auf Aktualisierungen installierter Mods")
                        toggleValue: profileController ? profileController.checkUpdates : true
                        onToggled: function(val) { if (profileController) profileController.setCheckUpdates(val) }
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                    EzToggleRow {
                        label: EzI18n.t("settings_discord_rpc", "Discord Rich Presence (RPC) aktivieren")
                        sub: EzI18n.t("settings_discord_rpc_desc", "Zeigt deinen aktuellen Server und Status in Discord an")
                        toggleValue: profileController ? profileController.discordRpc : true
                        onToggled: function(val) { if (profileController) profileController.setDiscordRpc(val) }
                    }

                    Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6; Layout.topMargin: 8; Layout.bottomMargin: 8 }

                    RowLayout {
                        Layout.fillWidth: true
                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 1
                            Text { text: EzI18n.t("settings_profiles_location", "Speicherort der Profile"); font.family: EzTheme.fontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text }
                            Text { text: profileController ? profileController.activeGameDir : ""; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted; elide: Text.ElideMiddle; Layout.fillWidth: true }
                        }
                        EzButton {
                            text: EzI18n.t("settings_open", "Öffnen")
                            Layout.preferredHeight: 28
                            onClicked: profileController.openFolder("")
                        }
                    }
                }
            }

            Item { height: 20 }

            // ─── JAVA ───
            Text { text: EzI18n.t("settings_java_title", "JAVA LAUFZEITUMGEBUNG"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 70

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 16
                    spacing: 14

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 2
                        Text { text: EzI18n.t("settings_java_path", "Java Runtime Pfad"); font.family: EzTheme.mcFontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                        Text { text: profileController ? profileController.javaRuntime : ""; font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted; elide: Text.ElideMiddle; Layout.fillWidth: true }
                    }

                    EzButton {
                        text: EzI18n.t("settings_java_detect", "Erkennen")
                        mcFont: true
                        Layout.preferredHeight: 30
                        Layout.preferredWidth: 90
                        onClicked: {
                            if (profileController) profileController.detectJava()
                        }
                    }
                }
            }

            Item { height: 20 }

            // ─── UPDATES & GITHUB SECTION ───
            Text {
                text: EzI18n.t("update_section_title", "UPDATES & VERSION")
                font.family: EzTheme.mcFontFamily
                font.pixelSize: 11
                color: EzTheme.textSecondary
            }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: updateCol.implicitHeight + 28

                ColumnLayout {
                    id: updateCol
                    anchors.fill: parent
                    anchors.margins: 14
                    spacing: 12

                    RowLayout {
                        Layout.fillWidth: true
                        spacing: 12

                        Image {
                            source: "icons/zap.svg"
                            Layout.preferredWidth: 24
                            Layout.preferredHeight: 24
                            fillMode: Image.PreserveAspectFit
                        }

                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 2

                            Text {
                                text: (typeof updateController !== "undefined" && updateController)
                                      ? "EzClient v" + updateController.currentVersion
                                      : "EzClient v1.0.0"
                                font.family: EzTheme.mcFontFamily
                                font.pixelSize: 13
                                font.bold: true
                                color: EzTheme.text
                            }

                            Text {
                                text: (typeof updateController !== "undefined" && updateController)
                                      ? updateController.statusMessage
                                      : EzI18n.t("update_up_to_date", "✓ EzClient ist auf dem neuesten Stand")
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 10
                                color: (typeof updateController !== "undefined" && updateController && updateController.updateAvailable)
                                       ? EzTheme.accentLight
                                       : EzTheme.textMuted
                            }
                        }

                        // Check / Download Button
                        EzButton {
                            id: checkBtn
                            text: (typeof updateController !== "undefined" && updateController && updateController.isChecking)
                                  ? EzI18n.t("update_checking", "Prüfe…")
                                  : EzI18n.t("update_check_btn", "Nach Updates suchen")
                            Layout.preferredHeight: 32
                            onClicked: {
                                if (typeof updateController !== "undefined" && updateController) {
                                    updateController.checkForUpdates(false)
                                }
                            }
                        }

                        EzButton {
                            text: EzI18n.t("update_view_github", "GitHub")
                            Layout.preferredHeight: 32
                            onClicked: {
                                if (typeof updateController !== "undefined" && updateController) {
                                    updateController.openReleasePage()
                                }
                            }
                        }
                    }

                    // Expandable update available box
                    Rectangle {
                        Layout.fillWidth: true
                        implicitHeight: updateAvailCol.implicitHeight + 20
                        radius: EzTheme.radiusSm
                        color: EzTheme.surfaceActive
                        border.color: EzTheme.accent
                        border.width: 1
                        visible: typeof updateController !== "undefined" && updateController && updateController.updateAvailable

                        ColumnLayout {
                            id: updateAvailCol
                            anchors.fill: parent
                            anchors.margins: 12
                            spacing: 8

                            RowLayout {
                                Layout.fillWidth: true
                                spacing: 8

                                Text {
                                    text: "⚡ " + (typeof updateController !== "undefined" && updateController ? updateController.releaseName : "Update")
                                    font.family: EzTheme.mcFontFamily
                                    font.pixelSize: 12
                                    font.bold: true
                                    color: EzTheme.accentLight
                                    Layout.fillWidth: true
                                }

                                Text {
                                    text: (typeof updateController !== "undefined" && updateController && updateController.assetSizeMb > 0)
                                          ? "(" + updateController.assetSizeMb + " MB)"
                                          : ""
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    color: EzTheme.textMuted
                                }
                            }

                            Text {
                                text: (typeof updateController !== "undefined" && updateController) ? updateController.changelog : ""
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 10
                                color: EzTheme.textSecondary
                                Layout.fillWidth: true
                                wrapMode: Text.WordWrap
                                maximumLineCount: 4
                                elide: Text.ElideRight
                            }

                            // Download progress bar
                            Rectangle {
                                Layout.fillWidth: true
                                height: 6
                                radius: 3
                                color: EzTheme.surface2
                                visible: typeof updateController !== "undefined" && updateController && updateController.isDownloading

                                Rectangle {
                                    height: parent.height
                                    radius: 3
                                    color: EzTheme.accent
                                    width: parent.width * (typeof updateController !== "undefined" && updateController ? updateController.downloadProgress : 0)
                                    Behavior on width { NumberAnimation { duration: 100 } }
                                }
                            }

                            // Action button (Download or Install)
                            EzButton {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 34
                                primary: true
                                text: {
                                    if (typeof updateController !== "undefined" && updateController) {
                                        if (updateController.updateReady) return EzI18n.t("update_install_now", "Jetzt neu starten & installieren")
                                        if (updateController.isDownloading) return updateController.downloadStatus || EzI18n.t("update_downloading", "Lade Update herunter…")
                                        return EzI18n.t("update_download_btn", "Update herunterladen & installieren")
                                    }
                                    return "Update"
                                }
                                onClicked: {
                                    if (typeof updateController !== "undefined" && updateController) {
                                        if (updateController.updateReady) {
                                            updateController.installAndRestart()
                                        } else if (!updateController.isDownloading) {
                                            updateController.startDownload()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Item { height: 20 }

            // ─── ABOUT ───
            Text { text: EzI18n.t("settings_about_title", "ÜBER EZCLIENT"); font.family: EzTheme.mcFontFamily; font.pixelSize: 11; color: EzTheme.textSecondary }
            Item { height: 8 }

            EzSurface {
                Layout.fillWidth: true
                implicitHeight: 80

                RowLayout {
                    anchors.fill: parent
                    anchors.margins: 16
                    spacing: 16

                    Image {
                        source: "assets/logo.svg"
                        Layout.preferredWidth: 40
                        Layout.preferredHeight: 40
                        fillMode: Image.PreserveAspectFit
                        smooth: true
                    }

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 2
                        Text { text: "EzClient Launcher v" + (typeof updateController !== "undefined" && updateController ? updateController.currentVersion : "1.0.0") + " (Release)"; font.family: EzTheme.mcFontFamily; font.pixelSize: 14; font.bold: true; color: EzTheme.text }
                        Text { text: EzI18n.t("settings_about_edition", "Offizielle Vollversion · PySide6 & Qt Quick Edition · High-Performance Minecraft Client"); font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                    }

                    Rectangle {
                        height: 24
                        width: relTagText.implicitWidth + 14
                        radius: 4
                        color: EzTheme.surfaceActive
                        border.color: EzTheme.accent
                        border.width: 1

                        Text {
                            id: relTagText
                            text: EzI18n.t("settings_full_version_badge", "VOLLVERSION")
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 9
                            font.bold: true
                            color: EzTheme.accentLight
                            anchors.centerIn: parent
                        }
                    }
                }
            }

            Item { height: 32 }
        }
    }

    // Background customizer & live preview modal
    BackgroundModal {
        id: bgModal
    }
}
