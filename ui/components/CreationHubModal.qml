import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import ".."

Item {
    id: root
    objectName: "globalCreationHubModal"
    anchors.fill: parent
    visible: opacity > 0.001
    opacity: isOpen ? 1.0 : 0.0

    Behavior on opacity { NumberAnimation { duration: 180; easing.type: Easing.OutQuad } }

    property bool isOpen: false
    property string creationHubView: "choices"
    property bool noriskImporting: false
    property string noriskStatus: ""
    property bool noriskAddPerformance: true
    property var windowRef: null

    function open(initialView = "choices") {
        creationHubView = initialView || "choices"
        noriskStatus = ""
        noriskImporting = false
        isOpen = true
        if (creationHubView === "norisk" && typeof profileController !== "undefined" && profileController) {
            profileController.scanNoRiskProfiles()
        }
    }

    function close() {
        isOpen = false
        creationHubView = "choices"
    }

    Connections {
        target: (typeof profileController !== "undefined") ? profileController : null
        function onNoriskImportProgress(progress, message) {
            root.noriskStatus = message
        }
        function onNoriskImportFinished(profileId, success, message) {
            root.noriskImporting = false
            root.noriskStatus = message
            if (success) {
                root.close()
                if (typeof windowRef !== "undefined" && windowRef && windowRef.navigateTo) {
                    windowRef.navigateTo("profiles")
                }
            }
        }
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

    // Dialog card
    Rectangle {
        id: hubCard
        anchors.centerIn: parent
        width: Math.min(parent.width - 56, 880)
        height: Math.min(parent.height - 56, 610)
        radius: 18
        color: EzTheme.surface
        border.color: EzTheme.borderLight
        border.width: 1
        scale: root.isOpen ? 1.0 : 0.96
        Behavior on scale { NumberAnimation { duration: 190; easing.type: Easing.OutCubic } }

        MouseArea {
            anchors.fill: parent
        }

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 24
            spacing: 16

            // Header Row
            RowLayout {
                Layout.fillWidth: true
                spacing: 12

                Rectangle {
                    visible: root.creationHubView !== "choices"
                    width: 38
                    height: 38
                    radius: 10
                    color: backMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                    Image {
                        anchors.centerIn: parent
                        width: 18
                        height: 18
                        source: "../icons/nav-home.svg"
                        rotation: 180
                    }
                    MouseArea {
                        id: backMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: root.creationHubView = "choices"
                    }
                }

                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 3

                    Text {
                        text: root.creationHubView === "norisk" ? "NoRiskClient-Profil übernehmen" : "Neues Profil"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 23
                        font.bold: true
                        color: EzTheme.text
                    }
                    Text {
                        text: root.creationHubView === "norisk" ? "Wähle ein lokal installiertes Profil aus." : "Wie möchtest du starten?"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        color: EzTheme.textMuted
                    }
                }

                Rectangle {
                    width: 38
                    height: 38
                    radius: 10
                    color: closeHubMouse.containsMouse ? EzTheme.surface3 : "transparent"
                    Image {
                        anchors.centerIn: parent
                        width: 17
                        height: 17
                        source: "../icons/x.svg"
                    }
                    MouseArea {
                        id: closeHubMouse
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

            // NoRisk Options Bar (when in norisk view)
            Rectangle {
                visible: root.creationHubView === "norisk"
                Layout.fillWidth: true
                Layout.preferredHeight: 58
                radius: 12
                color: EzTheme.surface2
                border.color: EzTheme.border
                border.width: 1

                RowLayout {
                    anchors.fill: parent
                    anchors.leftMargin: 14
                    anchors.rightMargin: 14
                    spacing: 10

                    Image {
                        source: "../icons/zap.svg"
                        width: 22
                        height: 22
                    }
                    ColumnLayout {
                        Layout.fillWidth: true
                        spacing: 1

                        Text {
                            text: "EzClient-Performancepaket hinzufügen"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: EzTheme.text
                        }
                        Text {
                            text: "Installiert nur kompatible Komponenten; EzClient Core nur auf geprüften Versionen."
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 9
                            color: EzTheme.textMuted
                        }
                    }
                    CheckBox {
                        checked: root.noriskAddPerformance
                        onToggled: root.noriskAddPerformance = checked
                    }
                }
            }

            // 4 Choices Grid
            GridLayout {
                visible: root.creationHubView === "choices"
                Layout.fillWidth: true
                Layout.fillHeight: true
                columns: 2
                rowSpacing: 14
                columnSpacing: 14

                Repeater {
                    model: [
                        {
                            title: "Profil-Assistent",
                            description: "Ein sauberes Profil in wenigen Schritten erstellen.",
                            icon: "sparkles.svg",
                            action: "wizard"
                        },
                        {
                            title: "Modpack installieren",
                            description: "Modpacks aus der Bibliothek entdecken und spielen.",
                            icon: "modpack-stack.svg",
                            action: "modpack"
                        },
                        {
                            title: "Eigenes Profil",
                            description: "Version und Loader frei wählen – ohne EzClient-Vorgaben.",
                            icon: "folder.svg",
                            action: "custom"
                        },
                        {
                            title: "Von NoRiskClient",
                            description: "Vorhandene Profile samt Mods und Einstellungen übernehmen.",
                            icon: "client-norisk.svg",
                            action: "norisk"
                        }
                    ]

                    Rectangle {
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        Layout.minimumHeight: 190
                        radius: 16
                        color: choiceMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                        border.width: 1
                        border.color: choiceMouse.containsMouse ? EzTheme.accent : EzTheme.border
                        scale: choiceMouse.pressed ? 0.98 : (choiceMouse.containsMouse ? 1.008 : 1)

                        Behavior on color { ColorAnimation { duration: 140 } }
                        Behavior on border.color { ColorAnimation { duration: 140 } }
                        Behavior on scale { NumberAnimation { duration: 140; easing.type: Easing.OutCubic } }

                        ColumnLayout {
                            anchors.centerIn: parent
                            width: parent.width - 44
                            spacing: 10

                            Rectangle {
                                Layout.alignment: Qt.AlignHCenter
                                width: 68
                                height: 68
                                radius: 20
                                color: EzTheme.accentSoft
                                Image {
                                    anchors.centerIn: parent
                                    width: 34
                                    height: 34
                                    source: "../icons/" + modelData.icon
                                }
                            }

                            Text {
                                Layout.fillWidth: true
                                horizontalAlignment: Text.AlignHCenter
                                text: modelData.title
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 17
                                font.bold: true
                                color: EzTheme.text
                            }

                            Text {
                                Layout.fillWidth: true
                                horizontalAlignment: Text.AlignHCenter
                                wrapMode: Text.WordWrap
                                text: modelData.description
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textMuted
                            }
                        }

                        MouseArea {
                            id: choiceMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (modelData.action === "wizard") {
                                    root.close()
                                    if (typeof windowRef !== "undefined" && windowRef && windowRef.navigateTo) {
                                        windowRef.navigateTo("versions")
                                    }
                                } else if (modelData.action === "modpack") {
                                    root.close()
                                    if (typeof modrinthController !== "undefined" && modrinthController) {
                                        modrinthController.setProjectType("modpack")
                                    }
                                    if (typeof windowRef !== "undefined" && windowRef && windowRef.navigateTo) {
                                        windowRef.navigateTo("mods")
                                    }
                                } else if (modelData.action === "custom") {
                                    root.close()
                                    if (typeof windowRef !== "undefined" && windowRef && windowRef.navigateTo) {
                                        windowRef.navigateTo("profiles")
                                    }
                                    if (typeof windowRef !== "undefined" && windowRef && windowRef.profilesPageRef) {
                                        windowRef.profilesPageRef.openCreateCustomDialog()
                                    }
                                } else if (modelData.action === "norisk") {
                                    root.creationHubView = "norisk"
                                    root.noriskStatus = ""
                                    if (typeof profileController !== "undefined" && profileController) {
                                        profileController.scanNoRiskProfiles()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // NoRisk Profiles List
            ListView {
                visible: root.creationHubView === "norisk" && typeof profileController !== "undefined" && profileController && profileController.noriskProfiles.length > 0
                Layout.fillWidth: true
                Layout.fillHeight: true
                clip: true
                spacing: 10
                model: (typeof profileController !== "undefined" && profileController) ? profileController.noriskProfiles : []

                delegate: Rectangle {
                    width: ListView.view.width
                    height: 86
                    radius: 14
                    color: importMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
                    border.width: 1
                    border.color: importMouse.containsMouse ? EzTheme.accent : EzTheme.border

                    RowLayout {
                        anchors.fill: parent
                        anchors.margins: 14
                        spacing: 14

                        Rectangle {
                            width: 54
                            height: 54
                            radius: 14
                            color: EzTheme.accentSoft
                            Image {
                                anchors.centerIn: parent
                                width: 28
                                height: 28
                                source: "../icons/client-norisk.svg"
                            }
                        }

                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 4

                            Text {
                                text: modelData.name
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 15
                                font.bold: true
                                color: EzTheme.text
                            }
                            Text {
                                text: "Minecraft " + modelData.version + "  •  " + modelData.loader + "  •  " + modelData.modCount + " Mods"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                color: EzTheme.textMuted
                            }
                        }

                        EzButton {
                            text: root.noriskImporting ? "Importiert …" : "Importieren"
                            primary: true
                            enabled: !root.noriskImporting
                            Layout.preferredWidth: 125
                            onClicked: {
                                root.noriskImporting = true
                                root.noriskStatus = "Import wird vorbereitet …"
                                if (profileController) {
                                    profileController.importNoRiskProfile(modelData.id, root.noriskAddPerformance)
                                }
                            }
                        }
                    }

                    MouseArea {
                        id: importMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        acceptedButtons: Qt.NoButton
                    }
                }
            }

            // NoRisk Empty State
            ColumnLayout {
                visible: root.creationHubView === "norisk" && (!profileController || profileController.noriskProfiles.length === 0)
                Layout.fillWidth: true
                Layout.fillHeight: true
                spacing: 12
                Item { Layout.fillHeight: true }

                Image {
                    Layout.alignment: Qt.AlignHCenter
                    source: "../icons/client-norisk.svg"
                    width: 48
                    height: 48
                    opacity: 0.4
                }

                Text {
                    Layout.fillWidth: true
                    horizontalAlignment: Text.AlignHCenter
                    text: "Keine lokal installierten NoRiskClient-Profile gefunden."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 14
                    font.bold: true
                    color: EzTheme.textMuted
                }

                Text {
                    Layout.fillWidth: true
                    horizontalAlignment: Text.AlignHCenter
                    text: "Stelle sicher, dass NoRiskClient auf diesem PC installiert ist und Profile angelegt wurden."
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    color: EzTheme.textSubtle
                }

                EzButton {
                    Layout.alignment: Qt.AlignHCenter
                    text: "Erneut suchen"
                    Layout.preferredWidth: 140
                    Layout.preferredHeight: 34
                    onClicked: {
                        if (typeof profileController !== "undefined" && profileController) {
                            profileController.scanNoRiskProfiles()
                        }
                    }
                }

                Item { Layout.fillHeight: true }
            }

            // Status message
            Text {
                visible: root.creationHubView === "norisk" && root.noriskStatus !== ""
                Layout.fillWidth: true
                text: root.noriskStatus
                font.family: EzTheme.fontFamily
                font.pixelSize: 11
                color: EzTheme.accentLight
                horizontalAlignment: Text.AlignHCenter
            }
        }
    }
}
