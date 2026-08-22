import QtQuick 2.15

pragma Singleton

QtObject {
    // ── BACKGROUNDS ──────────────────────────────────────────────
    readonly property color bg:           "#0A0A0F"
    readonly property color titlebarBg:   "#08080D"
    readonly property color sidebarBg:    "#0E0E15"
    readonly property color surface:      "#12121B"
    readonly property color surface2:     "#181823"
    readonly property color surface3:     "#1F1F2C"
    readonly property color surfaceHover: "#252534"
    readonly property color surfaceActive:"#21183A"
    readonly property color overlay:      "#00000088"

    // ── GLASSMORPHISM ────────────────────────────────────────────
    readonly property color glass:        "#15152240"
    readonly property color glassBorder:  "#ffffff08"

    // ── BORDERS ──────────────────────────────────────────────────
    readonly property color border:       "#1A1A28"
    readonly property color borderLight:  "#28283A"
    readonly property color borderAccent: "#A78BFA30"

    // ── TEXT ─────────────────────────────────────────────────────
    readonly property color text:          "#F0F0F5"
    readonly property color textSecondary: "#9898B0"
    readonly property color textMuted:     "#55556A"
    readonly property color textSubtle:    "#353548"

    // ── ACCENT — vivid EzClient green ────────────────────────────
    readonly property color accent:      "#A78BFA"
    readonly property color accentHover: "#B9A4FF"
    readonly property color accentDark:  "#33235E"
    readonly property color accentLight: "#C4B5FD"
    readonly property color accentGlow:  "#A78BFA25"
    readonly property color accentSoft:  "#A78BFA12"

    // ── GRADIENTS (start / end) ──────────────────────────────────
    readonly property color gradStart:   "#A78BFA"
    readonly property color gradEnd:     "#7C5CE0"

    // ── SEMANTIC ──────────────────────────────────────────────────
    readonly property color cyan:    "#38BDF8"
    readonly property color purple:  "#A78BFA"
    readonly property color orange:  "#FB923C"
    readonly property color danger:  "#F43F5E"
    readonly property color warning: "#FBBF24"

    // ── TYPOGRAPHY ────────────────────────────────────────────────
    property string fontMode: "mixed" // "minecraft", "standard", "mixed"
    
    readonly property string mcFontFamily: fontMode === "standard" ? "Segoe UI" : "Minecraft"
    readonly property string fontFamily: fontMode === "minecraft" ? "Minecraft" : "Segoe UI"

    // ── DIMENSIONS ────────────────────────────────────────────────
    readonly property int sidebarWidth: 224
    readonly property int titlebarHeight: 40
    readonly property int statusbarHeight: 28
    readonly property int radius: 12
    readonly property int radiusSm: 6
    readonly property int radiusLg: 16

    // ── ANIMATION DURATIONS ──────────────────────────────────────
    readonly property int animFast: 100
    readonly property int animNormal: 180
    readonly property int animSlow: 300
}
