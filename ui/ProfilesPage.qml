import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root

    property string createName: ""
    property string createVersion: "1.21.4"
    property string createLoader: "Fabric"
    property string createPreset: "performance"

    // Background click handler to deselect / defocus search input
    MouseArea {
        anchors.fill: parent
        onClicked: root.forceActiveFocus()
    }

    // ─── Create Profile Dialog ───
    Rectangle {
        id: createDialog
        visible: opacity > 0.001
        opacity: 0.0
        anchors.fill: parent
        color: "#B5000000"
        z: 50

        Behavior on opacity { NumberAnimation { duration: 160; easing.type: Easing.OutQuad } }

        MouseArea {
            anchors.fill: parent
            onClicked: {
                createDialog.opacity = 0.0
                nameField.text = ""
                root.forceActiveFocus()
            }
        }

        Rectangle {
            anchors.centerIn: parent
            width: 480
            height: 390
            radius: EzTheme.radius
            color: EzTheme.surface
            border.color: EzTheme.borderLight
            border.width: 1
            scale: createDialog.opacity > 0.5 ? 1.0 : 0.95

            Behavior on scale { NumberAnimation { duration: 160; easing.type: Easing.OutCubic } }

            MouseArea {
                anchors.fill: parent
            }

            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 24
                spacing: 12

                RowLayout {
                    Layout.fillWidth: true
                    Text {
                        text: EzI18n.t("profiles_create_title", "Neues Profil erstellen")
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 16
                        font.bold: true
                        color: EzTheme.text
                        Layout.fillWidth: true
                    }
                    Text {
                        text: "✕"
                        font.pixelSize: 13
                        color: EzTheme.textMuted
                        MouseArea {
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                createDialog.opacity = 0.0
                                nameField.text = ""
                            }
                        }
                    }
                }

                Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

                // Name field
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 4
                    Text { text: EzI18n.t("profiles_name_label", "Profilname"); font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }

                    Rectangle {
                        Layout.fillWidth: true
                        height: 38
                        color: EzTheme.bg
                        border.color: nameField.activeFocus ? EzTheme.accent : EzTheme.border
                        border.width: 1
                        radius: EzTheme.radiusSm

                        TextInput {
                            id: nameField
                            anchors.fill: parent
                            anchors.leftMargin: 12
                            anchors.rightMargin: 12
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 13
                            color: EzTheme.text
                            verticalAlignment: TextInput.AlignVCenter
                            selectByMouse: true
                            Keys.onReturnPressed: createBtnAction()

                            Text {
                                text: EzI18n.t("profiles_name_placeholder", "z.B. Mein zweites Profil…")
                                font: parent.font
                                color: EzTheme.textSubtle
                                visible: parent.text === ""
                                anchors.verticalCenter: parent.verticalCenter
                            }
                        }
                    }
                }

                // Version & Loader row
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 12

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 4
                        Text { text: EzI18n.t("profiles_version_label", "Minecraft Version"); font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }

                        EzDropDown {
                            Layout.fillWidth: true
                            currentIndex: 0
                            choices: ["26.2", "26.1", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"]
                            onChoiceChanged: root.createVersion = choices[currentIndex]
                        }
                    }

                    ColumnLayout {
                        Layout.preferredWidth: 140
                        spacing: 4
                        Text { text: EzI18n.t("profiles_loader_label", "Mod-Loader"); font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }

                        EzDropDown {
                            Layout.fillWidth: true
                            currentIndex: 0
                            choices: ["Fabric", "Forge"]
                            onChoiceChanged: root.createLoader = choices[currentIndex]
                        }
                    }
                }

                // Preset selection dropdown
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 4
                    Text { text: EzI18n.t("profiles_preset_label", "Client-Ausstattung"); font.family: EzTheme.fontFamily; font.pixelSize: 11; font.bold: true; color: EzTheme.textSecondary }

                    EzDropDown {
                        Layout.fillWidth: true
                        currentIndex: 0
                        choices: ["EzClient (Empfohlen)", "EzClient + Essentials", "Vanilla Pure (Keine Mods)"]
                        onChoiceChanged: {
                            if (currentIndex === 0) root.createPreset = "performance"
                            else if (currentIndex === 1) root.createPreset = "essentials"
                            else root.createPreset = "raw"
                        }
                    }
                }

                Item { height: 4 }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 10

                    Item { Layout.fillWidth: true }

                    EzButton {
                        text: EzI18n.t("profiles_cancel", "Abbrechen")
                        Layout.preferredWidth: 100
                        Layout.preferredHeight: 34
                        onClicked: {
                            createDialog.opacity = 0.0
                            nameField.text = ""
                        }
                    }

                    EzButton {
                        text: EzI18n.t("profiles_create", "Erstellen")
                        primary: true
                        Layout.preferredWidth: 100
                        Layout.preferredHeight: 34
                        onClicked: createBtnAction()
                    }
                }
            }
        }
    }

    function createBtnAction() {
        if (nameField.text.trim() !== "") {
            profileController.createProfile(nameField.text.trim(), root.createVersion, root.createLoader, root.createPreset)
            createDialog.opacity = 0.0
            nameField.text = ""
        }
    }

    ColumnLayout {
        anchors.fill: parent
        anchors.leftMargin: 24
        anchors.rightMargin: 24
        anchors.topMargin: 20
        anchors.bottomMargin: 16
        spacing: 16

        // ── Header ──
        RowLayout {
            Layout.fillWidth: true

            ColumnLayout {
                spacing: 2
                Text {
                    text: EzI18n.t("profiles_title", "MEINE PROFILE")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 16
                    font.bold: true
                    color: EzTheme.text
                }
                Text {
                    text: (profileController && profileController.profileModel ? profileController.profileModel.rowCount() : 0) + " " + EzI18n.t("profiles_subtitle", "Profile verfügbar · Du kannst unendlich viele Profile anlegen")
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    color: EzTheme.textMuted
                }
            }

            Item { Layout.fillWidth: true }

            EzButton {
                text: EzI18n.t("profiles_create", "Erstellen")
                primary: true
                Layout.preferredHeight: 36
                Layout.preferredWidth: 110
                onClicked: {
                    createDialog.opacity = 1.0
                    nameField.forceActiveFocus()
                }
            }
        }

        // ── Search bar ──
        Rectangle {
            Layout.fillWidth: true
            Layout.preferredHeight: 36
            color: EzTheme.surface
            border.color: profileSearch.activeFocus ? EzTheme.accent : EzTheme.border
            border.width: 1
            radius: EzTheme.radiusSm

            Behavior on border.color { ColorAnimation { duration: 120 } }

            RowLayout {
                anchors.fill: parent
                anchors.leftMargin: 12
                anchors.rightMargin: 12
                spacing: 8

                Image {
                    source: "icons/search.svg"
                    Layout.preferredWidth: 13
                    Layout.preferredHeight: 13
                    fillMode: Image.PreserveAspectFit
                }

                TextInput {
                    id: profileSearch
                    Layout.fillWidth: true
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 12
                    color: EzTheme.text
                    selectByMouse: true
                    verticalAlignment: TextInput.AlignVCenter

                    Text {
                        text: EzI18n.t("profiles_search_placeholder", "Profile durchsuchen…")
                        font: parent.font
                        color: EzTheme.textSubtle
                        visible: parent.text === ""
                        anchors.verticalCenter: parent.verticalCenter
                    }
                }

                Text {
                    visible: profileSearch.text !== ""
                    text: "✕"
                    font.pixelSize: 10
                    color: EzTheme.textMuted
                    MouseArea {
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: profileSearch.text = ""
                    }
                }
            }
        }

        Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border; opacity: 0.6 }

        // ── Profiles List ──
        ListView {
            id: profileList
            Layout.fillWidth: true
            Layout.fillHeight: true
            clip: true
            spacing: 8
            model: profileController ? profileController.profileModel : null

            ScrollBar.vertical: ScrollBar {
                id: pScrollBar
                policy: ScrollBar.AsNeeded
                visible: profileList.contentHeight > profileList.height
                contentItem: Rectangle {
                    implicitWidth: 5
                    radius: 3
                    color: EzTheme.borderLight
                }
            }

            delegate: Rectangle {
                id: profileItem
                width: profileList.width - (profileList.contentHeight > profileList.height ? 10 : 0)
                height: 72
                radius: EzTheme.radius
                color: model.profileId === profileController.activeId
                       ? EzTheme.surfaceActive
                       : (itemMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
                border.color: model.profileId === profileController.activeId
                              ? EzTheme.accent : (itemMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                border.width: 1

                Behavior on color { ColorAnimation { duration: 100 } }
                Behavior on border.color { ColorAnimation { duration: 100 } }

                RowLayout {
                    anchors.fill: parent
                    anchors.leftMargin: 16
                    anchors.rightMargin: 16
                    spacing: 16

                    // Avatar
                    Rectangle {
                        Layout.preferredWidth: 42
                        Layout.preferredHeight: 42
                        radius: 8
                        color: EzTheme.surface3
                        border.color: EzTheme.borderLight
                        border.width: 1

                        Text {
                            text: model.profileName ? model.profileName.substring(0, 2).toUpperCase() : "MC"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 14
                            font.bold: true
                            color: EzTheme.accentLight
                            anchors.centerIn: parent
                        }
                    }

                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 3

                        RowLayout {
                            spacing: 8
                            Text {
                                text: model.profileName
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 14
                                font.bold: true
                                color: EzTheme.text
                            }
                            Rectangle {
                                width: activePillText.implicitWidth + 8
                                height: 16
                                radius: 4
                                color: EzTheme.accentDark
                                visible: model.profileId === profileController.activeId
                                Text {
                                    id: activePillText
                                    text: EzI18n.t("profiles_active_badge", "AKTIV")
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 8
                                    font.bold: true
                                    color: EzTheme.accentLight
                                    anchors.centerIn: parent
                                }
                            }
                        }

                        RowLayout {
                            spacing: 8
                            Text {
                                text: "Minecraft " + model.minecraftVersion + "  ·  " + model.loader + "  ·  " + model.modsCount + " Mods"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textMuted
                            }
                            Rectangle { width: 4; height: 4; radius: 2; color: EzTheme.borderLight }
                            Text {
                                text: model.lastPlayed && model.lastPlayed !== "Never" ? (EzI18n.t("profiles_last_played", "Zuletzt: ") + model.lastPlayed) : EzI18n.t("profiles_never_played", "Noch nie gespielt")
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 10
                                color: EzTheme.textSubtle
                            }
                        }
                    }

                    // Action Buttons Row
                    RowLayout {
                        spacing: 8

                        // Switch / Select Profile Button (if not active)
                        EzButton {
                            text: EzI18n.t("profiles_select", "Auswählen")
                            visible: model.profileId !== profileController.activeId
                            Layout.preferredHeight: 32
                            Layout.preferredWidth: 90
                            onClicked: profileController.selectProfile(model.profileId)
                        }

                        // Play Button
                        Rectangle {
                            Layout.preferredWidth: 38
                            Layout.preferredHeight: 32
                            radius: EzTheme.radiusSm
                            color: playBtnMouse.containsMouse ? EzTheme.accentHover : EzTheme.accent
                            Behavior on color { ColorAnimation { duration: 100 } }

                            Text {
                                text: "▶"
                                font.pixelSize: 11
                                color: "#000000"
                                anchors.centerIn: parent
                            }

                            MouseArea {
                                id: playBtnMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    profileController.selectProfile(model.profileId)
                                    profileController.launchActiveProfile()
                                }
                            }
                        }

                        // Duplicate Button
                        Rectangle {
                            Layout.preferredWidth: 32
                            Layout.preferredHeight: 32
                            radius: 6
                            color: dupBtnMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                            border.color: EzTheme.border
                            border.width: 1
                            Behavior on color { ColorAnimation { duration: 100 } }

                            Text {
                                text: "📋"
                                font.pixelSize: 11
                                color: EzTheme.textSecondary
                                anchors.centerIn: parent
                            }

                            MouseArea {
                                id: dupBtnMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: profileController.duplicateProfile(model.profileId)
                            }
                        }

                        // Manage Mods Button
                        EzButton {
                            text: EzI18n.t("profiles_mods_btn", "Mods")
                            Layout.preferredHeight: 32
                            Layout.preferredWidth: 65
                            onClicked: {
                                profileController.selectProfile(model.profileId)
                                if (typeof window !== "undefined" && window.navigateTo) {
                                    window.navigateTo("installed_mods")
                                }
                            }
                        }

                        // Delete button
                        Rectangle {
                            Layout.preferredWidth: 32
                            Layout.preferredHeight: 32
                            radius: 6
                            color: delBtnMouse.containsMouse ? "#3D1418" : EzTheme.surface2
                            border.color: delBtnMouse.containsMouse ? EzTheme.danger : EzTheme.border
                            border.width: 1
                            Behavior on color { ColorAnimation { duration: 100 } }

                            Text {
                                text: "🗑"
                                font.pixelSize: 11
                                color: delBtnMouse.containsMouse ? EzTheme.danger : EzTheme.textMuted
                                anchors.centerIn: parent
                            }

                            MouseArea {
                                id: delBtnMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: profileController.deleteProfile(model.profileId)
                            }
                        }
                    }
                }

                MouseArea {
                    id: itemMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    z: -1
                    onClicked: {
                        profileController.selectProfile(model.profileId)
                        if (typeof window !== "undefined" && window.navigateTo) {
                            window.navigateTo("profile_detail")
                        }
                    }
                }
            }
        }
    }
}
