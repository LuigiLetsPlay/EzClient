import QtQuick 2.15
import QtQuick.Layouts 1.15
import QtQuick.Controls 2.15
import "components"

ApplicationWindow {
    id: window
    visible: true
    width: 1280
    height: 820
    minimumWidth: 1000
    minimumHeight: 640
    title: "EzClient"
    flags: Qt.Window | Qt.FramelessWindowHint
    color: EzTheme.bg

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

    function navigateTo(route) {
        navController.navigate(route)
    }

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
            if (!isError && statusText.indexOf("Launcher wird gestartet") >= 0) {
                if (profileController && profileController.closeOnLaunch) {
                    window.showMinimized()
                }
            }
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
                "settings":       5
            }
            if (indexMap[route] !== undefined) {
                mainStack.currentIndex = indexMap[route]
            }
        }
    }

    // ─────────────────────────────────────────
    // WINDOW RESIZE HANDLES (High Z-Index Frameless Resizer)
    // ─────────────────────────────────────────
    WindowResizeHandles {
        windowRef: window
    }
}
