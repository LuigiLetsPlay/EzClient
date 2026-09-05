import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import ".."

Item {
    id: root
    anchors.fill: parent
    visible: opacity > 0.001
    opacity: isOpen ? 1.0 : 0.0
    z: 99995

    Behavior on opacity { NumberAnimation { duration: 160; easing.type: Easing.OutQuad } }

    property bool isOpen: false
    property string profileId: ""
    property string selectedIcon: ""
    property string profileName: ""

    signal iconSelected(string icon)

    readonly property var presetIcons: [
        { id: "grass-block", label: "Grasblock", icon: "grass-block" },
        { id: "sand-block", label: "Sandblock", icon: "sand-block" },
        { id: "norisk", label: "NoRisk", icon: "norisk" },
        { id: "ezclient", label: "EzClient", icon: "ezclient" },
        { id: "tnt", label: "TNT", icon: "tnt" },
        { id: "potion", label: "Trank", icon: "potion" },
        { id: "clock", label: "Uhr", icon: "clock" },
        { id: "compass", label: "Kompass", icon: "compass" },
        { id: "star", label: "Netherstern", icon: "star" },
        { id: "flint", label: "Feuerzeug", icon: "flint" }
    ]

    function open(profId, currentIcon, name) {
        profileId = profId || ""
        selectedIcon = currentIcon || ""
        profileName = name || ""
        isOpen = true
    }

    function close() {
        isOpen = false
    }

    // Modal background overlay
    Rectangle {
        anchors.fill: parent
        color: "#C6000000"
        MouseArea {
            anchors.fill: parent
            onClicked: root.close()
        }
    }

    // Modal card
    Rectangle {
        anchors.centerIn: parent
        width: Math.min(parent.width - 40, 560)
        height: Math.min(parent.height - 40, 580)
        radius: 18
        color: EzTheme.surface
        border.color: EzTheme.borderLight
        border.width: 1
        scale: root.isOpen ? 1.0 : 0.95
        Behavior on scale { NumberAnimation { duration: 170; easing.type: Easing.OutCubic } }

        MouseArea { anchors.fill: parent }

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 24
            spacing: 16

            // Header Row
            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 2

                    Text {
                        text: "Profil-Icon anpassen"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 18
                        font.bold: true
                        color: EzTheme.text
                    }
                    Text {
                        text: "Wähle ein Icon aus oder lade ein eigenes Bild (PNG) hoch."
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                Rectangle {
                    width: 32
                    height: 32
                    radius: 8
                    color: closeMouse.containsMouse ? EzTheme.surface3 : "transparent"
                    Text {
                        anchors.centerIn: parent
                        text: "✕"
                        font.pixelSize: 13
                        color: EzTheme.textMuted
                    }
                    MouseArea {
                        id: closeMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.close()
                    }
                }
            }

            Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

            // Active Preview Card
            RowLayout {
                Layout.fillWidth: true
                spacing: 16

                ProfileIcon {
                    Layout.preferredWidth: 64
                    Layout.preferredHeight: 64
                    radius: 16
                    iconNameOrPath: root.selectedIcon
                    fallbackName: root.profileName || "EZ"
                    fontSize: 22
                    borderColor: EzTheme.accent
                    borderWidth: 2
                }

                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 4

                    Text {
                        text: root.selectedIcon ? (root.selectedIcon.indexOf("file:") >= 0 ? "Eigenes PNG-Bild" : ("Gewählt: " + root.selectedIcon)) : "Standard-Initialen"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 13
                        font.bold: true
                        color: EzTheme.text
                    }

                    Text {
                        text: "Dieses Icon wird in der Profilübersicht, Titelleiste und Startseite angezeigt."
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 10
                        color: EzTheme.textMuted
                        Layout.fillWidth: true
                        wrapMode: Text.WordWrap
                    }
                }

                EzButton {
                    text: "Eigenes PNG…"
                    Layout.preferredHeight: 34
                    Layout.preferredWidth: 125
                    onClicked: {
                        if (typeof profileController !== "undefined" && profileController) {
                            var picked = profileController.pickProfileIconImage()
                            if (picked) {
                                root.selectedIcon = picked
                            }
                        }
                    }
                }
            }

            Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

            Text {
                text: "Vorlagen:"
                font.family: EzTheme.fontFamily
                font.pixelSize: 12
                font.bold: true
                color: EzTheme.textSecondary
            }

            // Grid of Preset Icons
            ScrollView {
                id: presetScroll
                Layout.fillWidth: true
                Layout.fillHeight: true
                clip: true
                contentWidth: availableWidth
                ScrollBar.horizontal.policy: ScrollBar.AlwaysOff
                ScrollBar.vertical.policy: ScrollBar.AsNeeded

                GridLayout {
                    width: presetScroll.availableWidth
                    columns: 3
                    columnSpacing: 10
                    rowSpacing: 10

                    Repeater {
                        model: root.presetIcons

                        Rectangle {
                            Layout.fillWidth: true
                            Layout.preferredHeight: 82
                            radius: 14
                            color: isCurrent ? EzTheme.surfaceActive : (cardMouse.containsMouse ? EzTheme.surfaceHover : EzTheme.surface2)
                            border.width: isCurrent ? 2 : 1
                            border.color: isCurrent ? EzTheme.accent : (cardMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                            readonly property bool isCurrent: root.selectedIcon === modelData.id

                            Behavior on color { ColorAnimation { duration: 120 } }
                            Behavior on border.color { ColorAnimation { duration: 120 } }

                            ColumnLayout {
                                anchors.centerIn: parent
                                spacing: 6

                                ProfileIcon {
                                    Layout.alignment: Qt.AlignHCenter
                                    Layout.preferredWidth: 38
                                    Layout.preferredHeight: 38
                                    radius: 10
                                    iconNameOrPath: modelData.id
                                    fallbackName: modelData.label
                                    fontSize: 14
                                    bgColor: "transparent"
                                    borderWidth: 0
                                }

                                Text {
                                    Layout.alignment: Qt.AlignHCenter
                                    text: modelData.label
                                    font.family: EzTheme.fontFamily
                                    font.pixelSize: 10
                                    font.bold: isCurrent
                                    color: isCurrent ? EzTheme.accentLight : EzTheme.textMuted
                                }
                            }

                            MouseArea {
                                id: cardMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    root.selectedIcon = modelData.id
                                }
                            }
                        }
                    }
                }
            }

            Rectangle { Layout.fillWidth: true; height: 1; color: EzTheme.border }

            // Bottom Buttons
            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                EzButton {
                    text: "Standard (Initialen)"
                    Layout.preferredHeight: 36
                    Layout.preferredWidth: 140
                    onClicked: {
                        root.selectedIcon = ""
                    }
                }

                Item { Layout.fillWidth: true }

                EzButton {
                    text: "Abbrechen"
                    Layout.preferredHeight: 36
                    Layout.preferredWidth: 100
                    onClicked: root.close()
                }

                EzButton {
                    text: "Speichern"
                    primary: true
                    Layout.preferredHeight: 36
                    Layout.preferredWidth: 110
                    onClicked: {
                        if (root.profileId && typeof profileController !== "undefined" && profileController) {
                            profileController.setProfileIcon(root.profileId, root.selectedIcon)
                        }
                        root.iconSelected(root.selectedIcon)
                        root.close()
                    }
                }
            }
        }
    }
}
