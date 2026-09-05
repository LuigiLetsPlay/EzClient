import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

Item {
    id: root
    property var families: profileController ? profileController.gameVersionFamilies : []
    property int selectedFamilyIndex: 0
    property int selectedReleaseIndex: 0
    property string selectedLoader: "EzClient"
    property bool creatingProfile: false
    property string creationStatus: ""
    readonly property var selectedFamily: families.length > selectedFamilyIndex ? families[selectedFamilyIndex] : ({ family: "", releases: [] })
    readonly property var releases: selectedFamily.releases || []
    readonly property var selectedRelease: releases.length > selectedReleaseIndex ? releases[selectedReleaseIndex] : ({ version: "", java: 0, hasEzClient: false, hasFabric: false, isFrozen: false, supportLabel: "" })

    function bannerFor(index) {
        var family = String(families.length > index ? families[index].family : "")
        var banners = {
            "26": "version-26.png", "1.21": "version-1-21.jpg", "1.20": "version-1-20.png",
            "1.19": "version-1-19.png", "1.18": "version-1-18.jpg", "1.17": "version-1-17.jpg",
            "1.16": "version-1-16.jpg", "1.15": "version-1-15.jpg", "1.14": "version-1-14.jpg",
            "1.13": "version-1-13.png", "1.12": "version-1-12.jpg", "1.11": "version-1-11.png",
            "1.10": "version-1-10.jpg", "1.9": "version-1-9.png", "1.8": "version-1-8.png"
        }
        return "assets/" + (banners[family] || "version-legacy.jpg")
    }
    function familyHasEzClient(index) {
        var family = families.length > index ? families[index] : null
        if (!family || !family.releases) return false
        for (var i = 0; i < family.releases.length; ++i) {
            if (family.releases[i].hasEzClient) return true
        }
        return false
    }
    function chooseLoader(loader) {
        if (loader === "EzClient" && !selectedRelease.hasEzClient) return
        if (loader === "Fabric" && !selectedRelease.hasFabric) return
        selectedLoader = loader
    }

    Connections {
        target: profileController
        function onOnboardingStepProgress(progress, modName, status) {
            if (!root.creatingProfile) return
            root.creationStatus = status
            if (modName === "Fehler") root.creatingProfile = false
        }
        function onOnboardingFinished(profileId) {
            if (!root.creatingProfile) return
            root.creatingProfile = false
            if (typeof window !== "undefined" && window.navigateTo) window.navigateTo("profiles")
        }
    }

    Rectangle { anchors.fill: parent; color: EzTheme.bg }
    RowLayout {
        anchors.fill: parent; anchors.margins: 22; spacing: 20
        ColumnLayout {
            Layout.fillWidth: true; Layout.fillHeight: true; spacing: 16
            RowLayout {
                Layout.fillWidth: true
                ColumnLayout { spacing: 3
                    Text { text: "MINECRAFT VERSIONEN"; font.family: EzTheme.mcFontFamily; font.pixelSize: 19; font.bold: true; color: EzTheme.text }
                    Text { text: "Wähle zuerst eine Generation und danach die genaue Version"; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: EzTheme.textMuted }
                }
                Item { Layout.fillWidth: true }
                Rectangle { width: 230; height: 38; radius: 11; color: EzTheme.surface; border.color: search.activeFocus ? EzTheme.accent : EzTheme.border
                    RowLayout { anchors.fill: parent; anchors.margins: 11; spacing: 8
                        Image { source: "icons/search.svg"; width: 14; height: 14; opacity: .55 }
                        TextInput { id: search; Layout.fillWidth: true; color: EzTheme.text; font.family: EzTheme.fontFamily; font.pixelSize: 11; verticalAlignment: TextInput.AlignVCenter
                            Text { anchors.verticalCenter: parent.verticalCenter; visible: parent.text === ""; text: "Version suchen…"; color: EzTheme.textSubtle; font: parent.font }
                        }
                    }
                }
            }
            GridView {
                id: familyGrid
                Layout.fillWidth: true; Layout.fillHeight: true; clip: true
                readonly property int columns: Math.max(2, Math.floor(width / 215))
                cellWidth: width / columns; cellHeight: 154; model: root.families
                delegate: Item {
                    width: familyGrid.cellWidth; height: familyGrid.cellHeight
                    visible: search.text === "" || String(modelData.family).indexOf(search.text) >= 0
                    Rectangle {
                        anchors.fill: parent; anchors.rightMargin: 12; anchors.bottomMargin: 12; radius: 14; clip: true
                        color: EzTheme.surface; border.width: index === root.selectedFamilyIndex ? 2 : 1
                        border.color: index === root.selectedFamilyIndex ? EzTheme.accent : (familyMouse.containsMouse ? EzTheme.borderLight : EzTheme.border)
                        Image {
                            anchors.fill: parent
                            source: root.bannerFor(index)
                            fillMode: Image.PreserveAspectCrop
                            scale: String(modelData.family) === "1.12" ? 1.42 : 1.0
                        }
                        Rectangle { anchors.fill: parent; gradient: Gradient { GradientStop { position: 0; color: "#08000000" } GradientStop { position: 1; color: "#B0101118" } } }
                        Item {
                            anchors.left: parent.left; anchors.top: parent.top; anchors.margins: 8
                            width: 30; height: 30
                            z: 5
                            visible: root.familyHasEzClient(index)
                            Text { anchors.centerIn: parent; text: "★"; font.pixelSize: 17; color: "#FFD76A" }
                            MouseArea {
                                id: compatibilityStarMouse
                                anchors.fill: parent
                                hoverEnabled: true
                                cursorShape: Qt.PointingHandCursor
                                onClicked: {
                                    root.selectedFamilyIndex = index
                                    root.selectedReleaseIndex = 0
                                    root.selectedLoader = modelData.releases[0].hasEzClient ? "EzClient" : (modelData.releases[0].hasFabric ? "Fabric" : "Vanilla")
                                }
                            }
                            ToolTip.visible: compatibilityStarMouse.containsMouse
                            ToolTip.text: "EzClient Compatible"
                            ToolTip.delay: 100
                        }
                        Text { anchors.left: parent.left; anchors.leftMargin: 14; anchors.bottom: parent.bottom; anchors.bottomMargin: 13; text: "Minecraft " + modelData.family; font.family: EzTheme.mcFontFamily; font.pixelSize: 18; font.bold: true; color: "white" }
                        MouseArea { id: familyMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: { root.selectedFamilyIndex = index; root.selectedReleaseIndex = 0; root.selectedLoader = modelData.releases[0].hasEzClient ? "EzClient" : (modelData.releases[0].hasFabric ? "Fabric" : "Vanilla") } }
                    }
                }
            }
        }

        Rectangle {
            Layout.preferredWidth: 350; Layout.fillHeight: true; radius: 16; color: EzTheme.surface; border.color: EzTheme.border
            ColumnLayout {
                anchors.fill: parent; anchors.margins: 18; spacing: 13
                Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 156; radius: 13; clip: true
                    Image {
                        anchors.fill: parent
                        source: root.bannerFor(root.selectedFamilyIndex)
                        fillMode: Image.PreserveAspectCrop
                        scale: String(root.selectedFamily.family) === "1.12" ? 1.42 : 1.0
                    }
                    Rectangle { anchors.fill: parent; color: "#70070A0F" }
                    Column { anchors.left: parent.left; anchors.bottom: parent.bottom; anchors.margins: 16; spacing: 4
                        Text { text: "Minecraft " + (root.selectedRelease.version || ""); font.family: EzTheme.mcFontFamily; font.pixelSize: 23; font.bold: true; color: "white" }
                        Text { text: "Java " + root.selectedRelease.java; font.family: EzTheme.fontFamily; font.pixelSize: 11; color: "#D7FBE4" }
                        Text { visible: root.selectedRelease.hasEzClient; text: root.selectedRelease.supportLabel || "EzClient Compatible"; font.family: EzTheme.fontFamily; font.pixelSize: 9; color: "#FFD76A" }
                    }
                }
                Text { text: "UNTERVERSION"; font.family: EzTheme.mcFontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                Flow { Layout.fillWidth: true; Layout.preferredHeight: 90; spacing: 6
                    Repeater { model: root.releases
                        Rectangle { width: releaseLabel.implicitWidth + (modelData.hasEzClient ? 14 : 0) + 18; height: 29; radius: 8; color: index === root.selectedReleaseIndex ? EzTheme.accent : EzTheme.surface2; border.color: index === root.selectedReleaseIndex ? EzTheme.accent : EzTheme.border
                            Row {
                                anchors.centerIn: parent
                                spacing: 4
                                Text { visible: modelData.hasEzClient; text: "★"; font.pixelSize: 10; color: "#FFD76A" }
                                Text { id: releaseLabel; text: modelData.version; font.family: EzTheme.fontFamily; font.pixelSize: 10; font.bold: true; color: index === root.selectedReleaseIndex ? "#07130D" : EzTheme.textSecondary }
                            }
                            MouseArea { id: releaseMouse; anchors.fill: parent; hoverEnabled: true; cursorShape: Qt.PointingHandCursor; onClicked: { root.selectedReleaseIndex = index; root.selectedLoader = modelData.hasEzClient ? "EzClient" : (modelData.hasFabric ? "Fabric" : "Vanilla") } }
                            ToolTip.visible: releaseMouse.containsMouse && modelData.hasEzClient
                            ToolTip.text: modelData.supportLabel || "EzClient Compatible"
                            ToolTip.delay: 100
                        }
                    }
                }
                Text { text: "SPIELVARIANTE"; font.family: EzTheme.mcFontFamily; font.pixelSize: 10; color: EzTheme.textMuted }
                RowLayout { Layout.fillWidth: true; spacing: 8
                    Repeater { model: [ { id: "EzClient", label: "EzClient", icon: "assets/logo.svg" }, { id: "Fabric", label: "Fabric", icon: "assets/fabric-logo.png" }, { id: "Vanilla", label: "Vanilla", icon: "icons/loader-vanilla.svg" }, { id: "Forge", label: "Forge", icon: "icons/forge.svg" } ]
                        Rectangle {
                            Layout.fillWidth: true; Layout.preferredHeight: 82; radius: 11
                            readonly property bool available: modelData.id === "Vanilla" || modelData.id === "Forge" || (modelData.id === "Fabric" && root.selectedRelease.hasFabric) || (modelData.id === "EzClient" && root.selectedRelease.hasEzClient)
                            color: root.selectedLoader === modelData.id ? EzTheme.surfaceActive : EzTheme.surface2
                            border.width: 1; border.color: root.selectedLoader === modelData.id ? EzTheme.accent : EzTheme.border; opacity: available ? 1 : .32
                            Column { anchors.centerIn: parent; spacing: 5
                                Image { anchors.horizontalCenter: parent.horizontalCenter; width: 38; height: 38; source: modelData.icon; fillMode: Image.PreserveAspectFit }
                                Text { anchors.horizontalCenter: parent.horizontalCenter; text: modelData.label; font.family: EzTheme.fontFamily; font.pixelSize: 10; font.bold: true; color: EzTheme.text }
                            }
                            MouseArea { anchors.fill: parent; enabled: parent.available; cursorShape: enabled ? Qt.PointingHandCursor : Qt.ForbiddenCursor; onClicked: root.chooseLoader(modelData.id) }
                        }
                    }
                }
                Item { Layout.fillHeight: true }
                Rectangle { Layout.fillWidth: true; height: 40; radius: 9; color: EzTheme.bg; border.color: nameInput.activeFocus ? EzTheme.accent : EzTheme.border
                    TextInput { id: nameInput; anchors.fill: parent; anchors.margins: 11; color: EzTheme.text; font.family: EzTheme.fontFamily; font.pixelSize: 11; verticalAlignment: TextInput.AlignVCenter
                        Text { visible: parent.text === ""; anchors.verticalCenter: parent.verticalCenter; text: "Profilname (optional)"; color: EzTheme.textSubtle; font: parent.font }
                    }
                }
                Text { visible: root.creatingProfile; Layout.fillWidth: true; text: root.creationStatus || "Prüfe Profil-Kompatibilität …"; horizontalAlignment: Text.AlignHCenter; font.family: EzTheme.fontFamily; font.pixelSize: 9; color: EzTheme.textMuted }
                EzButton { Layout.fillWidth: true; Layout.preferredHeight: 44; primary: true; enabled: !root.creatingProfile; text: root.creatingProfile ? "Profil wird eingerichtet …" : "Profil erstellen"
                    onClicked: { var version = root.selectedRelease.version; if (!version) return; var preset = root.selectedLoader === "EzClient" ? "ezclient" : (root.selectedLoader === "Fabric" ? "performance" : "raw"); var loader = root.selectedLoader === "EzClient" ? "Fabric" : root.selectedLoader; var generatedName = nameInput.text.trim() || root.selectedLoader + " " + version; var profIcon = preset === "ezclient" ? "ezclient" : (loader === "Fabric" ? "box" : (loader === "Forge" ? "forge" : "vanilla")); root.creatingProfile = true; root.creationStatus = "Prüfe Loader und Mods …"; profileController.createAndOnboard(generatedName, version, loader, preset, [], profIcon) }
                }
            }
        }
    }
}
