import QtQuick 2.15
import QtQuick.Controls 2.15
import QtQuick.Layouts 1.15
import "components"

Item {
    id: root
    signal navigate(string route)
    property string reportedCapeId: ""
    readonly property string activeCommunityCapeUrl: typeof accountController !== "undefined" && accountController ? accountController.activeCommunityCapeUrl : ""
    property string previewCapeUrl: ""
    property string previewCapeTitle: ""
    readonly property var capes: typeof accountController !== "undefined" && accountController ? accountController.communityCapes : []
    readonly property string status: typeof accountController !== "undefined" && accountController ? accountController.capeCommunityStatus : ""

    Rectangle { anchors.fill: parent; color: EzTheme.bg }

    ColumnLayout {
        visible: true
        anchors.fill: parent
        anchors.margins: Math.max(20, Math.min(42, parent.width * 0.04))
        spacing: 18

        RowLayout {
            Layout.fillWidth: true
            spacing: 14
            Rectangle {
                Layout.preferredWidth: 48; Layout.preferredHeight: 48; radius: 14
                color: EzTheme.surfaceActive; border.color: EzTheme.accent; border.width: 1
                Text { anchors.centerIn: parent; text: "♜"; font.pixelSize: 25; color: EzTheme.accentLight }
            }
            ColumnLayout {
                Layout.fillWidth: true; spacing: 2
                Text { text: "Cape Community"; font.family: EzTheme.mcFontFamily; font.pixelSize: 22; font.bold: true; color: EzTheme.text }
                Text { text: "Wähle ein Cape oder teile dein eigenes mit der EzClient-Community."; font.family: EzTheme.fontFamily; font.pixelSize: 12; color: EzTheme.textSecondary; elide: Text.ElideRight; Layout.fillWidth: true }
            }
            EzButton { text: "Editor"; onClicked: root.navigate("cape_editor") }
            EzButton { text: "↻ Aktualisieren"; onClicked: accountController.refreshCapeCommunity() }
        }

        Rectangle {
            visible: false; Layout.fillWidth: true; Layout.preferredHeight: 0; radius: 14
            color: EzTheme.surface2; border.color: EzTheme.border
            RowLayout {
                anchors.fill: parent; anchors.margins: 16; spacing: 16
                Rectangle {
                    Layout.preferredWidth: 70; Layout.preferredHeight: 70; radius: 10; color: EzTheme.surface3; clip: true
                    Image { id: activeCapeImage; anchors.fill: parent; anchors.margins: 8; source: typeof accountController !== "undefined" ? accountController.capeTextureUrl : ""; fillMode: Image.PreserveAspectFit; rotation: 90; transformOrigin: Item.Center; visible: source !== "" }
                    Text { anchors.centerIn: parent; visible: !activeCapeImage.visible; text: "♜"; font.pixelSize: 30; color: EzTheme.textMuted }
                }
                ColumnLayout {
                    Layout.fillWidth: true; spacing: 5
                    Text { text: "Dein aktives Cape"; font.family: EzTheme.mcFontFamily; font.pixelSize: 13; font.bold: true; color: EzTheme.text }
                    Text { text: "Das Cape erscheint im Home-Skin und wird beim Spielstart verwendet."; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textSecondary; wrapMode: Text.WordWrap; Layout.fillWidth: true }
                }
                EzButton { text: "Editor"; onClicked: root.navigate("cape_editor") }
                EzButton {
                    text: "PNG auswählen & bearbeiten"
                    onClicked: {
                        var capeUrl = accountController.pickCapeFile()
                        if (capeUrl && capeUrl !== "") root.navigate("cape_editor")
                    }
                }
                EzButton { text: "Veröffentlichen"; enabled: typeof accountController !== "undefined" && accountController.capeTextureUrl !== ""; onClicked: publishDialog.open() }
            }
        }

        Text { text: root.status; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: root.capes.length ? EzTheme.textMuted : EzTheme.textSecondary }

        ScrollView {
            Layout.fillWidth: true; Layout.fillHeight: true; clip: true

            FastWheelHandler {}

            GridView {
                id: capeGrid
                anchors.fill: parent
                cellWidth: Math.max(180, Math.min(250, width / Math.max(2, Math.floor(width / 220))))
                cellHeight: 238
                model: root.capes
                delegate: Item {
                    width: capeGrid.cellWidth; height: capeGrid.cellHeight
                    Rectangle {
                        anchors.fill: parent; anchors.margins: 6; radius: 14; color: EzTheme.surface2; border.color: cardMouse.containsMouse ? EzTheme.accent : EzTheme.border
                        Behavior on border.color { ColorAnimation { duration: 120 } }
                        ColumnLayout {
                            anchors.fill: parent; anchors.margins: 12; spacing: 8
                            Rectangle {
                                Layout.preferredWidth: 72; Layout.fillHeight: true; Layout.alignment: Qt.AlignHCenter; radius: 9; color: EzTheme.surface3; clip: true
                                CapeTextureImage {
                                    anchors.fill: parent
                                    anchors.margins: 7
                                    capeSource: modelData.imageUrl || ""
                                }
                                MouseArea {
                                    anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor
                                    onClicked: { root.previewCapeUrl = modelData.imageUrl || ""; root.previewCapeTitle = modelData.title || "Community Cape"; capePreviewDialog.open() }
                                }
                            }
                            Text { text: modelData.title || "Community Cape"; font.family: EzTheme.mcFontFamily; font.pixelSize: 12; font.bold: true; color: EzTheme.text; Layout.fillWidth: true; elide: Text.ElideRight }
                            RowLayout {
                                Layout.fillWidth: true
                                Text { text: "von " + (modelData.owner || "EzClient Spieler"); font.family: EzTheme.fontFamily; font.pixelSize: 10; color: EzTheme.textMuted; Layout.fillWidth: true; elide: Text.ElideRight }
                                EzButton {
                                    text: root.activeCommunityCapeUrl === (modelData.imageUrl || "") ? "Aktiv" : "Nutzen"
                                    primary: root.activeCommunityCapeUrl === (modelData.imageUrl || "")
                                    onClicked: {
                                        if (root.activeCommunityCapeUrl === (modelData.imageUrl || "")) return
                                        if (accountController.activateCommunityCape(modelData.imageUrl)) root.activeCommunityCapeUrl = modelData.imageUrl || ""
                                    }
                                }
                            }
                            Text { text: "Melden"; color: EzTheme.textMuted; font.pixelSize: 10; MouseArea { anchors.fill: parent; cursorShape: Qt.PointingHandCursor; onClicked: { root.reportedCapeId = modelData.id; reportDialog.open() } } }
                        }
                        MouseArea { id: cardMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.ArrowCursor; z: -1 }
                    }
                }
            }
        }
    }

    ColumnLayout {
        visible: false
        anchors.centerIn: parent
        width: Math.min(460, parent.width - 48)
        spacing: 14
        Rectangle {
            Layout.alignment: Qt.AlignHCenter
            Layout.preferredWidth: 76; Layout.preferredHeight: 96
            radius: 18; color: EzTheme.surfaceActive; border.color: EzTheme.accent
            Text { anchors.centerIn: parent; text: "♜"; font.pixelSize: 42; color: EzTheme.accentLight }
        }
        Text { Layout.alignment: Qt.AlignHCenter; text: "Cape Studio"; font.family: EzTheme.mcFontFamily; font.pixelSize: 25; font.bold: true; color: EzTheme.text }
        Text { Layout.fillWidth: true; horizontalAlignment: Text.AlignHCenter; wrapMode: Text.WordWrap; text: "Entwirf, importiere und veröffentliche Capes vollständig im Editor."; font.family: EzTheme.fontFamily; font.pixelSize: 13; color: EzTheme.textSecondary }
        EzButton { Layout.alignment: Qt.AlignHCenter; text: "Cape Editor öffnen"; primary: true; onClicked: root.navigate("cape_editor") }
    }

    Dialog {
        id: capePreviewDialog; modal: true; anchors.centerIn: parent; width: 440; height: 620; title: root.previewCapeTitle
        background: Rectangle { radius: 16; color: EzTheme.surface2; border.color: EzTheme.border }
        contentItem: Rectangle {
            color: EzTheme.surface3; radius: 12; clip: true
            CapeTextureImage {
                anchors.fill: parent
                anchors.margins: 12
                capeSource: root.previewCapeUrl
            }
        }
    }

    Dialog {
        id: publishDialog; modal: true; anchors.centerIn: parent; width: 380; title: "Cape veröffentlichen"
        background: Rectangle { radius: 14; color: EzTheme.surface2; border.color: EzTheme.border }
        contentItem: ColumnLayout {
            spacing: 12
            Text { text: "Gib deinem Cape einen Namen. Es wird unter deinem Minecraft-Namen veröffentlicht."; color: EzTheme.textSecondary; wrapMode: Text.WordWrap; Layout.fillWidth: true; font.pixelSize: 12 }
            TextField { id: capeTitle; Layout.fillWidth: true; placeholderText: "Mein Cape"; color: EzTheme.text; background: Rectangle { radius: 7; color: EzTheme.surface3; border.color: EzTheme.border } }
        }
        footer: DialogButtonBox { Button { text: "Abbrechen"; DialogButtonBox.buttonRole: DialogButtonBox.RejectRole } Button { text: "Veröffentlichen"; DialogButtonBox.buttonRole: DialogButtonBox.AcceptRole } }
        onAccepted: accountController.publishCape(capeTitle.text)
    }

    Dialog {
        id: reportDialog; modal: true; anchors.centerIn: parent; width: 380; title: "Cape melden"
        background: Rectangle { radius: 14; color: EzTheme.surface2; border.color: EzTheme.border }
        contentItem: ColumnLayout {
            spacing: 12
            Text { text: "Beschreibe bitte kurz, warum dieses Cape gemeldet wird."; color: EzTheme.textSecondary; wrapMode: Text.WordWrap; Layout.fillWidth: true; font.pixelSize: 12 }
            TextArea { id: reportReason; Layout.fillWidth: true; Layout.preferredHeight: 90; placeholderText: "Grund der Meldung"; color: EzTheme.text; wrapMode: TextEdit.Wrap; background: Rectangle { radius: 7; color: EzTheme.surface3; border.color: EzTheme.border } }
        }
        footer: DialogButtonBox { Button { text: "Abbrechen"; DialogButtonBox.buttonRole: DialogButtonBox.RejectRole } Button { text: "Melden"; DialogButtonBox.buttonRole: DialogButtonBox.AcceptRole } }
        onAccepted: accountController.reportCape(root.reportedCapeId, reportReason.text)
    }

}
