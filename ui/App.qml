import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

ApplicationWindow {
    id: window
    width: 1280
    height: 820
    minimumWidth: 760
    minimumHeight: 560
    title: "EzClient"
    flags: Qt.Window | Qt.FramelessWindowHint
    color: EzTheme.bg

    function openSkinModal() {
        globalSkinModal.open()
    }

    // Load authentic Minecraft TrueType Fonts into QML
    FontLoader {
        id: mcRegularFont
        source: "fonts/MinecraftDefault-Regular.ttf"
    }
    FontLoader {
        id: mcBoldFont
        source: "fonts/MinecraftDefault-Bold.ttf"
    }

    // Determine if we need onboarding (no profiles yet)
    property bool needsOnboarding: typeof profileController !== "undefined" && profileController ? !profileController.hasProfiles : false
    
    // List of integrated mods
    property var integratedMods: typeof profileController !== "undefined" && profileController ? profileController.integratedMods : []

    function navigateTo(route) {
        navController.navigate(route)
    }

    Binding {
        target: EzTheme
        property: "fontMode"
        value: typeof profileController !== "undefined" && profileController ? profileController.appFontMode : "mixed"
    }
    Binding { target: EzTheme; property: "accent"; value: !profileController ? "#A78BFA" : ({ purple: "#A78BFA", blue: "#60A5FA", rose: "#FB7185", orange: "#FB923C" }[profileController.themeColor] || "#A78BFA") }
    Binding { target: EzTheme; property: "accentHover"; value: !profileController ? "#B9A4FF" : ({ purple: "#B9A4FF", blue: "#93C5FD", rose: "#FDA4AF", orange: "#FDBA74" }[profileController.themeColor] || "#B9A4FF") }
    Binding { target: EzTheme; property: "accentDark"; value: !profileController ? "#33235E" : ({ purple: "#33235E", blue: "#1E3A8A", rose: "#881337", orange: "#7C2D12" }[profileController.themeColor] || "#33235E") }
    Binding { target: EzTheme; property: "accentLight"; value: !profileController ? "#C4B5FD" : ({ purple: "#C4B5FD", blue: "#BFDBFE", rose: "#FECDD3", orange: "#FED7AA" }[profileController.themeColor] || "#C4B5FD") }

    ColumnLayout {
        anchors.fill: parent
        spacing: 0

        // ─── TOP BAR NAVIGATION (Full-width modern game launcher bar) ───
        TopBar {
            id: topbar
            Layout.fillWidth: true
            Layout.preferredHeight: 56
            currentRoute: navController.currentRoute
            windowRef: window
            skinModalRef: globalSkinModal
            visible: !window.needsOnboarding
            onNavigate: function(route) {
                navController.navigate(route)
            }
        }

        // Minimal titlebar for onboarding if needed
        TitleBar {
            Layout.fillWidth: true
            Layout.preferredHeight: 40
            windowRef: window
            visible: window.needsOnboarding
        }

        // ─── MAIN CONTENT AREA (Full Width) ───
        Item {
            Layout.fillWidth: true
            Layout.fillHeight: true

            // ONBOARDING – shown when no profiles exist
            OnboardingPage {
                anchors.fill: parent
                visible: window.needsOnboarding
                onProfileCreated: {
                    window.needsOnboarding = false
                    navController.navigate("home")
                }
            }

            // MAIN APP CONTENT AREA – shown once at least one profile exists
            Rectangle {
                anchors.fill: parent
                color: EzTheme.bg
                visible: !window.needsOnboarding

                StackLayout {
                    id: mainStack
                    anchors.fill: parent
                    currentIndex: 0

                    HomePage {}
                    ProfilesPage {}
                    ProfileDetailPage {}
                    ModsPage {}
                    ModrinthPage {}
                    CapePage { onNavigate: function(route) { window.navigateTo(route) } }
                    CapeEditor { onNavigate: function(route) { window.navigateTo(route) } }
                    SettingsPage {}
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // GLOBAL AUTO-SAVE TOAST NOTIFICATION
    // ─────────────────────────────────────────
    Rectangle {
        id: toastBanner
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 24
        anchors.horizontalCenter: parent.horizontalCenter
        height: 36
        width: toastRow.implicitWidth + 28
        radius: 18
        color: "#18261E"
        border.color: EzTheme.accent
        border.width: 1.5
        z: 999
        visible: opacity > 0.001
        opacity: 0.0

        Behavior on opacity { NumberAnimation { duration: 180; easing.type: Easing.OutQuad } }

        RowLayout {
            id: toastRow
            anchors.centerIn: parent
            spacing: 8
            Image { source: "icons/check.svg"; width: 14; height: 14; fillMode: Image.PreserveAspectFit }
            Text {
                id: toastText
                text: "Einstellung gespeichert"
                font.family: EzTheme.mcFontFamily
                font.pixelSize: 11
                font.bold: true
                color: EzTheme.accentLight
            }
        }

        Timer {
            id: toastTimer
            interval: 1800
            onTriggered: toastBanner.opacity = 0.0
        }
    }

    // Crash & Error properties
    property bool showCrashModal: false
    property string crashTitle: ""
    property string crashShortError: ""
    property string crashFullLog: ""

    Connections {
        target: (typeof profileController !== "undefined") ? profileController : null
        function onProfilesChanged() {
            if (profileController) {
                window.needsOnboarding = !profileController.hasProfiles
            }
        }
        function onSettingSaved(msg) {
            toastText.text = msg || "Einstellung gespeichert"
            toastBanner.opacity = 1.0
            toastTimer.restart()
        }
        function onLaunchStatusChanged(statusText, isError) {
            if (!isError) {
                if (statusText.indexOf("wird vorbereitet") >= 0 || statusText.indexOf("Minecraft läuft") >= 0 || statusText.indexOf("Launcher wird gestartet") >= 0 || statusText.indexOf("Spiel gestartet") >= 0) {
                    if (profileController && profileController.showLiveLogs) {
                        globalLiveLogsWindow.show()
                        globalLiveLogsWindow.showNormal()
                        globalLiveLogsWindow.raise()
                        globalLiveLogsWindow.requestActivate()
                        window.hide()
                    } else if (profileController && profileController.closeOnLaunch) {
                        window.showMinimized()
                    }
                } else if (statusText.indexOf("Spiel beendet") >= 0) {
                    window.show()
                    window.showNormal()
                    window.raise()
                    window.requestActivate()
                }
            } else {
                window.show()
                window.showNormal()
                window.raise()
                window.requestActivate()
            }
        }
        function onGameCrashed(title, shortErr, fullLog) {
            window.show()
            window.showNormal()
            window.raise()
            window.requestActivate()
            window.crashTitle = title
            window.crashShortError = shortErr
            window.crashFullLog = fullLog
            window.showCrashModal = true
        }
    }

    // ─── Navigation Controller ───
    QtObject {
        id: navController
        property string currentRoute: "home"

        function navigate(route) {
            currentRoute = route
            var indexMap = {
                "home":           0,
                "profiles":       1,
                "profile_detail": 2,
                "installed_mods": 3,
                "mods_installed": 3,
                "mods":           4,
                "modrinth":       4,
                "store":          4,
                "cape":           5,
                "cape_editor":    6,
                "settings":       7
            }
            if (indexMap[route] !== undefined) {
                mainStack.currentIndex = indexMap[route]
            }
        }
    }

    // ─────────────────────────────────────────
    // MINECRAFT CRASH & ERROR MODAL DIALOG
    // ─────────────────────────────────────────
    Rectangle {
        id: crashModalOverlay
        anchors.fill: parent
        color: "#B805070A"
        z: 99998
        visible: window.showCrashModal
        opacity: visible ? 1.0 : 0.0
        Behavior on opacity { NumberAnimation { duration: 180 } }

        MouseArea {
            anchors.fill: parent
            // blocks interaction with underlying pages
        }

        Rectangle {
            id: crashCard
            anchors.centerIn: parent
            width: Math.min(680, parent.width - 60)
            height: Math.min(520, parent.height - 60)
            radius: 16
            color: "#12141A"
            border.color: "#FF453A"
            border.width: 1.5

            ColumnLayout {
                anchors.fill: parent
                anchors.margins: 22
                spacing: 14

                // Header Row
                RowLayout {
                    spacing: 12
                    Rectangle {
                        width: 38; height: 38; radius: 19
                        color: "#381214"
                        border.color: "#FF453A"
                        border.width: 1
                        Image { source: "icons/alert-triangle.svg"; width: 18; height: 18; anchors.centerIn: parent; sourceSize: Qt.size(18,18) }
                    }
                    ColumnLayout {
                        spacing: 2
                        Text {
                            text: window.crashTitle || "Minecraft Start-Fehler"
                            font.family: EzTheme.mcFontFamily
                            font.pixelSize: 16
                            font.bold: true
                            color: "#FF453A"
                        }
                        Text {
                            text: "Das Spiel konnte nicht gestartet werden oder ist abgestürzt."
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            color: EzTheme.textSecondary
                        }
                    }
                    Item { Layout.fillWidth: true }
                    // Close X button
                    Rectangle {
                        width: 28; height: 28; radius: 14
                        color: closeCrashMouse.containsMouse ? "#2A2E39" : "transparent"
                        Text { text: "✕"; color: EzTheme.textMuted; anchors.centerIn: parent; font.pixelSize: 13 }
                        MouseArea {
                            id: closeCrashMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: window.showCrashModal = false
                        }
                    }
                }

                // Error summary box
                Rectangle {
                    Layout.fillWidth: true
                    height: 44
                    radius: 8
                    color: "#1E1214"
                    border.color: "#5C1D24"
                    border.width: 1
                    RowLayout {
                        anchors.fill: parent
                        anchors.margins: 10
                        spacing: 8
                        Text {
                            text: window.crashShortError || "Unbekannter Fehler"
                            font.family: "Consolas, monospace"
                            font.pixelSize: 11
                            color: "#FFA099"
                            Layout.fillWidth: true
                            elide: Text.ElideRight
                        }
                    }
                }

                // Monospace Log View
                Rectangle {
                    Layout.fillWidth: true
                    Layout.fillHeight: true
                    radius: 8
                    color: "#0A0B0E"
                    border.color: EzTheme.border
                    border.width: 1
                    clip: true

                    Flickable {
                        id: logFlick
                        anchors.fill: parent
                        anchors.margins: 10
                        contentWidth: logText.implicitWidth
                        contentHeight: logText.implicitHeight
                        clip: true

                        TextEdit {
                            id: logText
                            text: window.crashFullLog || "Keine Log-Ausgabe vorhanden."
                            font.family: "Consolas, monospace"
                            font.pixelSize: 11
                            color: "#C5C8D0"
                            readOnly: true
                            selectByMouse: true
                            selectionColor: EzTheme.accent
                            selectedTextColor: "#000000"
                        }
                    }
                }

                // Actions footer
                RowLayout {
                    Layout.fillWidth: true
                    spacing: 12

                    // Copy Error Button
                    Rectangle {
                        height: 36
                        Layout.preferredWidth: copyRow.implicitWidth + 28
                        radius: 8
                        color: copyBtnMouse.containsMouse ? "#32734A" : "#24D677"
                        Behavior on color { ColorAnimation { duration: 120 } }
                        RowLayout {
                            id: copyRow
                            anchors.centerIn: parent
                            spacing: 8
                            Image { source: "icons/copy.svg"; width: 14; height: 14; opacity: 0.8; sourceSize: Qt.size(14,14) }
                            Text {
                                text: "Fehler kopieren"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 12
                                font.bold: true
                                color: "#000000"
                            }
                        }
                        MouseArea {
                            id: copyBtnMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController) {
                                    profileController.copyToClipboard(window.crashFullLog || window.crashShortError)
                                }
                            }
                        }
                    }

                    // Open Crash Folder Button
                    Rectangle {
                        height: 36
                        Layout.preferredWidth: openFoldRow.implicitWidth + 24
                        radius: 8
                        color: openFoldMouse.containsMouse ? "#2A2E39" : "#1A1D24"
                        border.color: EzTheme.border
                        border.width: 1
                        RowLayout {
                            id: openFoldRow
                            anchors.centerIn: parent
                            spacing: 8
                            Image { source: "icons/folder.svg"; width: 14; height: 14; opacity: 0.8; sourceSize: Qt.size(14,14) }
                            Text {
                                text: "Ordner öffnen"
                                font.family: EzTheme.fontFamily
                                font.pixelSize: 12
                                font.bold: true
                                color: EzTheme.text
                            }
                        }
                        MouseArea {
                            id: openFoldMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: {
                                if (profileController && profileController.activeProfilePath) {
                                    profileController.openFolder(profileController.activeProfilePath + "/crash-reports")
                                }
                            }
                        }
                    }

                    Item { Layout.fillWidth: true }

                    // Close Button
                    Rectangle {
                        height: 36
                        Layout.preferredWidth: 90
                        radius: 8
                        color: closeBtnMouse.containsMouse ? "#2A2E39" : "#1A1D24"
                        border.color: EzTheme.border
                        border.width: 1
                        Text {
                            text: "Schließen"
                            font.family: EzTheme.fontFamily
                            font.pixelSize: 12
                            font.bold: true
                            color: EzTheme.textSecondary
                            anchors.centerIn: parent
                        }
                        MouseArea {
                            id: closeBtnMouse
                            anchors.fill: parent
                            hoverEnabled: true
                            cursorShape: Qt.PointingHandCursor
                            onClicked: window.showCrashModal = false
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // GLOBAL SKIN CHANGER MODAL
    // ─────────────────────────────────────────
    SkinModal {
        id: globalSkinModal
    }

    // ─────────────────────────────────────────
    // LIVE LOGS WINDOW
    // ─────────────────────────────────────────
    LiveLogsWindow {
        id: globalLiveLogsWindow
    }

    // ─────────────────────────────────────────
    // WINDOW RESIZE HANDLES (High Z-Index Frameless Resizer)
    // ─────────────────────────────────────────
    WindowResizeHandles {
        windowRef: window
    }
}
