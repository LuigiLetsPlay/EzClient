import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import ".."

Item {
    id: dd
    property var choices: []
    property int currentIndex: 0
    property alias placeholder: placeholderText.text
    property bool formatEzClientSupported: false
    signal choiceChanged

    height: 40
    implicitHeight: 40

    function isEzClientSupported(version) {
        if (!version) return false
        var supported = ["26.2", "26.1"]
        return supported.indexOf(version) !== -1
    }

    function formatText(val) {
        if (dd.formatEzClientSupported && dd.isEzClientSupported(val)) {
            return val + " ⭐"
        }
        return val
    }

    readonly property string currentText: (choices && choices.length > currentIndex && choices[currentIndex]) ? formatText(choices[currentIndex]) : ""

    Rectangle {
        id: ddBox
        anchors.fill: parent
        radius: EzTheme.radiusSm
        color: ddMouse.containsMouse ? EzTheme.surface3 : EzTheme.surface2
        border.color: popup.opened ? EzTheme.accent : (ddMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
        border.width: 1

        Behavior on color { ColorAnimation { duration: 120 } }
        Behavior on border.color { ColorAnimation { duration: 120 } }

        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 14
            anchors.rightMargin: 12
            spacing: 8

            Text {
                id: placeholderText
                text: dd.currentText
                font.family: EzTheme.fontFamily
                font.pixelSize: 13
                color: EzTheme.text
                Layout.fillWidth: true
                elide: Text.ElideRight
            }

            Text {
                text: "▾"
                font.pixelSize: 11
                color: popup.opened ? EzTheme.accent : EzTheme.textMuted
                rotation: popup.opened ? 180 : 0
                Behavior on rotation { NumberAnimation { duration: 150; easing.type: Easing.OutQuad } }
                Behavior on color { ColorAnimation { duration: 120 } }
            }
        }

        MouseArea {
            id: ddMouse
            anchors.fill: parent
            hoverEnabled: true
            cursorShape: Qt.PointingHandCursor
            onClicked: {
                // Clear active focus on any text field when opening dropdown
                if (dd.parent) dd.parent.forceActiveFocus()
                popup.opened ? popup.close() : popup.open()
            }
        }
    }

    Popup {
        id: popup
        y: dd.height + 4
        width: dd.width
        height: Math.min((dd.choices ? dd.choices.length : 1), 6) * 36 + 12
        padding: 4
        closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutsideParent

        enter: Transition {
            NumberAnimation { property: "opacity"; from: 0.0; to: 1.0; duration: 140; easing.type: Easing.OutCubic }
            NumberAnimation { property: "scale"; from: 0.96; to: 1.0; duration: 140; easing.type: Easing.OutCubic }
        }
        exit: Transition {
            NumberAnimation { property: "opacity"; from: 1.0; to: 0.0; duration: 100; easing.type: Easing.InCubic }
            NumberAnimation { property: "scale"; from: 1.0; to: 0.96; duration: 100; easing.type: Easing.InCubic }
        }

        background: Rectangle {
            radius: EzTheme.radiusSm
            color: EzTheme.surface2
            border.color: EzTheme.borderLight
            border.width: 1

            // Soft shadow under popup
            Rectangle {
                anchors.fill: parent
                anchors.margins: -1
                radius: EzTheme.radiusSm + 1
                color: "transparent"
                border.color: EzTheme.borderAccent
                border.width: 1
                opacity: 0.5
            }
        }

        contentItem: ListView {
            id: listView
            anchors.fill: parent
            clip: true
            model: dd.choices
            boundsBehavior: Flickable.StopAtBounds

            ScrollBar.vertical: ScrollBar {
                policy: listView.contentHeight > listView.height ? ScrollBar.AlwaysOn : ScrollBar.AsNeeded
                width: 6
                contentItem: Rectangle {
                    implicitWidth: 6
                    radius: 3
                    color: parent.pressed ? EzTheme.accent : (parent.hovered ? EzTheme.accentHover : EzTheme.borderLight)
                    Behavior on color { ColorAnimation { duration: 100 } }
                }
            }

            delegate: Rectangle {
                width: listView.width - (listView.contentHeight > listView.height ? 8 : 0)
                height: 34
                radius: 5
                color: optMouse.containsMouse ? EzTheme.surface3 : (index === dd.currentIndex ? EzTheme.surfaceActive : "transparent")
                Behavior on color { ColorAnimation { duration: 90 } }

                RowLayout {
                    anchors.fill: parent
                    anchors.leftMargin: 10
                    anchors.rightMargin: 10
                    spacing: 6

                    Text {
                        id: optionText
                        text: dd.formatText(modelData)
                        font.family: EzTheme.fontFamily
                        font.pixelSize: 12
                        font.bold: index === dd.currentIndex
                        color: index === dd.currentIndex ? EzTheme.accentLight : (optMouse.containsMouse ? EzTheme.text : EzTheme.textSecondary)
                        Layout.fillWidth: true
                        Behavior on color { ColorAnimation { duration: 90 } }
                        
                        ToolTip {
                            visible: optMouse.containsMouse && dd.formatEzClientSupported && dd.isEzClientSupported(modelData)
                            text: "EzClient verfügbar für diese Version"
                            delay: 200
                        }
                    }

                    Rectangle {
                        width: 4
                        height: 4
                        radius: 2
                        color: EzTheme.accent
                        visible: index === dd.currentIndex
                    }
                }

                MouseArea {
                    id: optMouse
                    anchors.fill: parent
                    hoverEnabled: true
                    cursorShape: Qt.PointingHandCursor
                    onClicked: {
                        dd.currentIndex = index
                        dd.choiceChanged()
                        popup.close()
                    }
                }
            }
        }
    }
}
