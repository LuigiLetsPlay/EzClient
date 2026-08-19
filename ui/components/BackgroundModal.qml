import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import ".."

Rectangle {
    id: bgModal
    anchors.fill: parent
    color: "#D005070A"
    z: 99999
    visible: opacity > 0.001
    opacity: 0.0

    Behavior on opacity { NumberAnimation { duration: 180 } }

    property string previewPath: ""
    property real currentOpacity: 0.60
    property string currentFillMode: "PreserveAspectCrop"

    function open() {
        if (typeof profileController !== "undefined" && profileController) {
            previewPath = profileController.customBackgroundImage
            currentOpacity = profileController.customBackgroundOpacity
            currentFillMode = profileController.customBackgroundFillMode
        }
        bgModal.opacity = 1.0
    }

    function close() {
        bgModal.opacity = 0.0
    }

    function formatUrl(p) {
        if (!p) return Qt.resolvedUrl("../assets/hero_bg.jpg").toString();
        if (p.startsWith("file:///") || p.startsWith("http://") || p.startsWith("https://") || p.startsWith("qrc:/")) return p;
        var clean = p.replace(/\\/g, "/");
        if (clean.startsWith("/")) return "file://" + clean;
        return "file:///" + clean;
    }

    MouseArea {
        anchors.fill: parent
        onClicked: bgModal.close()
    }

    Rectangle {
        anchors.centerIn: parent
        width: Math.min(600, parent.width - 32)
        height: Math.min(640, parent.height - 32)
        radius: 16
        color: "#12141C"
        border.color: EzTheme.borderLight
        border.width: 1

        MouseArea {
            anchors.fill: parent
            onClicked: {} // block click through
        }

        ColumnLayout {
            anchors.fill: parent
            anchors.margins: 22
            spacing: 14

            // Header
            RowLayout {
                spacing: 12
                Rectangle {
                    width: 36; height: 36; radius: 18
                    color: EzTheme.surface2
                    border.color: EzTheme.border
                    border.width: 1
                    Text { text: "🖼️"; font.pixelSize: 18; anchors.centerIn: parent }
                }
                ColumnLayout {
                    spacing: 2
                    Text {
                        text: "Hintergrundbild anpassen"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 16
                        font.bold: true
                        color: EzTheme.text
                    }
                    Text {
                        text: "Wähle ein individuelles Wallpaper für das EzClient-Hauptmenü"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        color: EzTheme.textSecondary
                    }
                }
                Item { Layout.fillWidth: true }
                Rectangle {
                    width: 28; height: 28; radius: 14
                    color: closeMouse.containsMouse ? "#2A2E39" : "transparent"
                    Text { text: "✕"; color: EzTheme.textMuted; anchors.centerIn: parent; font.pixelSize: 13 }
                    MouseArea {
                        id: closeMouse
                        anchors.fill: parent
                        hoverEnabled: true
                        cursorShape: Qt.PointingHandCursor
                        onClicked: bgModal.close()
                    }
                }
            }

            // ── Live Interactive Preview Screen ──
            Rectangle {
                Layout.fillWidth: true
                height: 220
                radius: 10
                color: "#08090C"
                border.color: EzTheme.border
                border.width: 1
                clip: true

                // Background Image
                Image {
                    id: previewImg
                    anchors.fill: parent
                    source: bgModal.formatUrl(bgModal.previewPath)
                    fillMode: bgModal.currentFillMode === "PreserveAspectFit" 
                              ? Image.PreserveAspectFit 
                              : (bgModal.currentFillMode === "Stretch" ? Image.Stretch : Image.PreserveAspectCrop)
                    opacity: bgModal.currentOpacity
                }

                // Vignette mock
                Rectangle {
                    anchors.fill: parent
                    gradient: Gradient {
                        orientation: Gradient.Vertical
                        GradientStop { position: 0.0; color: "#A00A0A0F" }
                        GradientStop { position: 0.3; color: "#200A0A0F" }
                        GradientStop { position: 0.7; color: "#200A0A0F" }
                        GradientStop { position: 1.0; color: "#D00A0A0F" }
                    }
                }

                // Center Mock Content (Avatar silhouette & Play button)
                ColumnLayout {
                    anchors.centerIn: parent
                    spacing: 6
                    Rectangle {
                        Layout.alignment: Qt.AlignHCenter
                        width: 48; height: 64; radius: 6
                        color: "#182028"
                        border.color: EzTheme.border; border.width: 1
                        Text { text: "👤"; anchors.centerIn: parent; font.pixelSize: 24 }
                    }
                    Rectangle {
                        Layout.alignment: Qt.AlignHCenter
                        width: 110; height: 26; radius: 13
                        color: "#22C96E"
                        Text { text: "▶ SPIELEN"; font.family: EzTheme.fontFamily; font.pixelSize: 10; font.bold: true; color: "#000"; anchors.centerIn: parent }
                    }
                }

                // Live Preview Tag
                Rectangle {
                    anchors.top: parent.top; anchors.left: parent.left; anchors.margins: 10
                    height: 22; width: 92; radius: 4
                    color: "#C0000000"
                    border.color: EzTheme.borderLight; border.width: 1
                    Text { text: "👁️ Live-Vorschau"; color: EzTheme.accentLight; font.pixelSize: 10; anchors.centerIn: parent; font.bold: true }
                }
            }

            // ── Controls: File Picker & Fill Mode ──
            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                EzButton {
                    text: "📁 Neues Bild wählen…"
                    mcFont: false
                    implicitHeight: 34
                    Layout.fillWidth: true
                    onClicked: {
                        if (profileController) {
                            var p = profileController.pickBackgroundImage()
                            if (p) bgModal.previewPath = p
                        }
                    }
                }

                EzButton {
                    text: "Standard Artwork"
                    mcFont: false
                    implicitHeight: 34
                    danger: true
                    Layout.preferredWidth: 140
                    enabled: bgModal.previewPath !== ""
                    opacity: enabled ? 1.0 : 0.4
                    onClicked: {
                        bgModal.previewPath = ""
                    }
                }
            }

            // ── Fill Mode Selector ──
            ColumnLayout {
                Layout.fillWidth: true
                spacing: 6

                Text {
                    text: "Bild-Skalierung (Fit Mode):"
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 11
                    font.bold: true
                    color: EzTheme.textSecondary
                }

                RowLayout {
                    Layout.fillWidth: true
                    spacing: 8

                    Repeater {
                        model: [
                            { id: "PreserveAspectCrop", label: "Ausfüllen (Cover)" },
                            { id: "PreserveAspectFit", label: "Einpassen (Contain)" },
                            { id: "Stretch", label: "Gestreckt (Stretch)" }
                        ]

                        Rectangle {
                            Layout.fillWidth: true
                            height: 32
                            radius: 6
                            color: bgModal.currentFillMode === modelData.id ? EzTheme.surfaceActive : (modeM.containsMouse ? EzTheme.surface3 : EzTheme.surface2)
                            border.color: bgModal.currentFillMode === modelData.id ? EzTheme.accent : EzTheme.border
                            border.width: 1

                            Text {
                                text: modelData.label
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 11
                                font.bold: bgModal.currentFillMode === modelData.id
                                color: bgModal.currentFillMode === modelData.id ? EzTheme.accentLight : EzTheme.text
                                anchors.centerIn: parent
                            }

                            MouseArea {
                                id: modeM
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: bgModal.currentFillMode = modelData.id
                            }
                        }
                    }
                }
            }

            // ── Opacity / Brightness Slider ──
            ColumnLayout {
                Layout.fillWidth: true
                spacing: 4

                RowLayout {
                    Layout.fillWidth: true
                    Text {
                        text: "Deckkraft & Helligkeit:"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        font.bold: true
                        color: EzTheme.textSecondary
                    }
                    Item { Layout.fillWidth: true }
                    Text {
                        text: Math.round(bgModal.currentOpacity * 100) + "%"
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 11
                        font.bold: true
                        color: EzTheme.accentLight
                    }
                }

                Slider {
                    id: opSlider
                    Layout.fillWidth: true
                    from: 0.10
                    to: 1.0
                    value: bgModal.currentOpacity
                    onValueChanged: bgModal.currentOpacity = value
                }
            }

            Item { Layout.fillHeight: true }

            // ── Footer ──
            RowLayout {
                Layout.fillWidth: true
                spacing: 10

                Item { Layout.fillWidth: true }

                EzButton {
                    text: "Abbrechen"
                    implicitHeight: 38
                    Layout.preferredWidth: 100
                    onClicked: bgModal.close()
                }

                EzButton {
                    text: "Speichern & Anwenden"
                    primary: true
                    implicitHeight: 38
                    Layout.preferredWidth: 180
                    onClicked: {
                        if (profileController) {
                            profileController.setCustomBackgroundImage(bgModal.previewPath)
                            profileController.setCustomBackgroundOpacity(bgModal.currentOpacity)
                            profileController.setCustomBackgroundFillMode(bgModal.currentFillMode)
                        }
                        bgModal.close()
                    }
                }
            }
        }
    }
}
