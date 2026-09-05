import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import ".."

Item {
    id: root
    anchors.fill: parent
    visible: opacity > 0.001
    opacity: isOpen ? 1.0 : 0.0
    z: 99996

    Behavior on opacity { NumberAnimation { duration: 160; easing.type: Easing.OutQuad } }

    property bool isOpen: false
    property string profileId: ""
    property string profileName: ""
    property var onDeletedCallback: null

    signal profileDeleted(string profileId)

    function open(profId, name, callback) {
        profileId = profId || ""
        profileName = name || profId || "Unbenanntes Profil"
        onDeletedCallback = (typeof callback === "function") ? callback : null
        isOpen = true
    }

    function close() {
        isOpen = false
        profileId = ""
        profileName = ""
        onDeletedCallback = null
    }

    function confirmDelete() {
        var targetId = profileId
        var cb = onDeletedCallback

        if (targetId && typeof profileController !== "undefined" && profileController) {
            profileController.deleteProfile(targetId)
        }

        if (cb) {
            cb()
        }

        profileDeleted(targetId)
        close()
    }

    // Modal background overlay
    Rectangle {
        anchors.fill: parent
        color: "#C8000000"
        MouseArea {
            anchors.fill: parent
            onClicked: root.close()
        }
    }

    // Modal dialog card
    Rectangle {
        id: dialogCard
        anchors.centerIn: parent
        width: Math.min(parent.width - 40, 480)
        height: contentCol.implicitHeight + 48
        radius: 16
        color: EzTheme.surface
        border.color: "#3F1B22"
        border.width: 1
        scale: root.isOpen ? 1.0 : 0.94
        Behavior on scale { NumberAnimation { duration: 170; easing.type: Easing.OutCubic } }

        MouseArea { anchors.fill: parent }

        ColumnLayout {
            id: contentCol
            anchors.left: parent.left
            anchors.right: parent.right
            anchors.top: parent.top
            anchors.margins: 24
            spacing: 18

            // Header Row: Warning Icon + Title + Close Button
            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                Rectangle {
                    width: 38
                    height: 38
                    radius: 10
                    color: "#2E1117"
                    border.color: "#6B212E"
                    border.width: 1

                    Image {
                        source: "../icons/alert-triangle.svg"
                        width: 20
                        height: 20
                        fillMode: Image.PreserveAspectFit
                        anchors.centerIn: parent
                    }
                }

                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 2

                    Text {
                        text: "Profil wirklich löschen?"
                        font.family: EzTheme.mcFontFamily
                        font.pixelSize: 16
                        font.bold: true
                        color: "#FFAAA8"
                    }
                    Text {
                        text: "Unwiderrufliche Aktion"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textMuted
                    }
                }

                // X Close Button
                Rectangle {
                    width: 28
                    height: 28
                    radius: 6
                    color: closeM.containsMouse ? EzTheme.surfaceHover : "transparent"

                    Text {
                        text: "✕"
                        font.pixelSize: 12
                        color: EzTheme.textMuted
                        anchors.centerIn: parent
                    }

                    MouseArea {
                        id: closeM
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.close()
                    }
                }
            }

            Rectangle {
                Layout.fillWidth: true
                height: 1
                color: EzTheme.border
            }

            // Description / Target profile
            ColumnLayout {
                Layout.fillWidth: true
                spacing: 8

                Text {
                    Layout.fillWidth: true
                    text: "Möchtest du das folgende Profil wirklich dauerhaft entfernen?"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 13
                    color: EzTheme.text
                    wrapMode: Text.WordWrap
                }

                // Profile Name Badge
                Rectangle {
                    Layout.fillWidth: true
                    Layout.preferredHeight: 38
                    radius: 8
                    color: EzTheme.surface2
                    border.color: EzTheme.border
                    border.width: 1

                    RowLayout {
                        anchors.fill: parent
                        anchors.leftMargin: 12
                        anchors.rightMargin: 12
                        spacing: 8

                        Image {
                            source: "../icons/nav-profiles.svg"
                            width: 16
                            height: 16
                            fillMode: Image.PreserveAspectFit
                            opacity: 0.8
                        }

                        Text {
                            Layout.fillWidth: true
                            text: root.profileName
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 13
                            font.bold: true
                            color: EzTheme.text
                            elide: Text.ElideRight
                        }
                    }
                }
            }

            // High-visibility Danger Warning Callout Box
            Rectangle {
                Layout.fillWidth: true
                Layout.preferredHeight: warningRow.implicitHeight + 20
                radius: 10
                color: "#1C0D11"
                border.color: "#831826"
                border.width: 1

                RowLayout {
                    id: warningRow
                    anchors.fill: parent
                    anchors.margins: 12
                    spacing: 10

                    Text {
                        text: "⚠️"
                        font.pixelSize: 18
                        Layout.alignment: Qt.AlignTop
                    }

                    Text {
                        Layout.fillWidth: true
                        text: "Dies kann nicht mehr rückgängig gemacht werden! Alle installierten Mods, Konfigurationen und lokalen Daten dieses Profils werden vollständig und unwiederbringlich gelöscht."
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        lineHeight: 1.2
                        color: "#FF9898"
                        wrapMode: Text.WordWrap
                    }
                }
            }

            Rectangle {
                Layout.fillWidth: true
                height: 1
                color: EzTheme.border
            }

            // Bottom Buttons: Abbrechen & Endgültig löschen
            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                Item { Layout.fillWidth: true }

                EzButton {
                    text: "Abbrechen"
                    Layout.preferredHeight: 38
                    Layout.preferredWidth: 110
                    onClicked: root.close()
                }

                Rectangle {
                    id: deleteBtn
                    Layout.preferredHeight: 38
                    Layout.preferredWidth: 155
                    radius: EzTheme.radiusSm
                    color: delM.pressed ? "#7F1D1D" : (delM.containsMouse ? "#DC2626" : "#B91C1C")
                    border.color: delM.containsMouse ? "#EF4444" : "#991B1B"
                    border.width: 1

                    scale: delM.pressed ? 0.97 : (delM.containsMouse ? 1.02 : 1.0)
                    Behavior on scale { NumberAnimation { duration: 100; easing.type: Easing.OutCubic } }
                    Behavior on color { ColorAnimation { duration: 120 } }

                    RowLayout {
                        anchors.centerIn: parent
                        spacing: 8

                        Image {
                            source: "../icons/trash.svg"
                            width: 14
                            height: 14
                            fillMode: Image.PreserveAspectFit
                        }

                        Text {
                            text: "Endgültig löschen"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: "#FFFFFF"
                        }
                    }

                    MouseArea {
                        id: delM
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.confirmDelete()
                    }
                }
            }
        }
    }
}
