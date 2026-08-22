import QtQuick 2.15
import QtQuick.Layouts 1.15
import ".."

Rectangle {
    id: pCard
    property string presetKey: ""
    property string title: ""
    property string tag: ""
    property color tagColor: EzTheme.accent
    property color tagTextColor: "#000000"
    property string sub: ""
    property string mods: ""
    property bool selected: false
    signal clicked

    Layout.fillWidth: true
    height: 74
    radius: EzTheme.radiusSm
    color: selected ? EzTheme.surfaceActive : (cardMouse.containsMouse ? EzTheme.surface2 : EzTheme.surface)
    border.color: selected ? EzTheme.accent : (cardMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
    border.width: 1

    Behavior on color { ColorAnimation { duration: 110 } }
    Behavior on border.color { ColorAnimation { duration: 110 } }

    RowLayout {
        anchors.fill: parent
        anchors.leftMargin: 14
        anchors.rightMargin: 14
        anchors.topMargin: 10
        anchors.bottomMargin: 10
        spacing: 14

        // Radio Circle
        Rectangle {
            Layout.preferredWidth: 20
            Layout.preferredHeight: 20
            Layout.alignment: Qt.AlignVCenter
            radius: 10
            color: selected ? EzTheme.accent : "transparent"
            border.color: selected ? EzTheme.accent : EzTheme.borderLight
            border.width: 1.5

            Rectangle {
                width: 6; height: 6; radius: 3
                color: "#000000"
                anchors.centerIn: parent
                visible: selected
            }
        }

        ColumnLayout {
            Layout.fillWidth: true
            Layout.alignment: Qt.AlignVCenter
            spacing: 2

            RowLayout {
                spacing: 8
                Layout.alignment: Qt.AlignLeft
                Text {
                    text: pCard.title
                    font.family: EzTheme.fontFamily
                    font.pixelSize: 13
                    font.bold: true
                    color: EzTheme.text
                }

                Rectangle {
                    height: 16
                    width: tagText.implicitWidth + 8
                    radius: 3
                    color: pCard.tagColor
                    visible: pCard.tag !== ""

                    Text {
                        id: tagText
                        text: pCard.tag
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 8
                        font.bold: true
                        color: pCard.tagTextColor
                        anchors.centerIn: parent
                    }
                }
            }

            Text {
                text: pCard.sub
                font.family: EzTheme.fontFamily
                font.pixelSize: 11
                color: selected ? EzTheme.textSecondary : EzTheme.textMuted
                Layout.alignment: Qt.AlignLeft
                Layout.fillWidth: true
                wrapMode: Text.WordWrap
            }

            Text {
                text: pCard.mods
                font.family: EzTheme.fontFamily
                font.pixelSize: 10
                color: selected ? EzTheme.accentLight : EzTheme.textSubtle
                Layout.alignment: Qt.AlignLeft
                Layout.fillWidth: true
                wrapMode: Text.WordWrap
            }
        }
    }

    MouseArea {
        id: cardMouse
        anchors.fill: parent
        hoverEnabled: true
        cursorShape: Qt.PointingHandCursor
        onClicked: pCard.clicked()
    }
}
