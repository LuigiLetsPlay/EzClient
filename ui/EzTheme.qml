import QtQuick 2.15

pragma Singleton

QtObject {
    // ── BACKGROUNDS ──────────────────────────────────────────────
    readonly property color bg:           "#0A0A0F"
    property color titlebarBg:            "#07130D"
    property color sidebarBg:             "#0A1710"
    readonly property color surface:      "#12121B"
    readonly property color surface2:     "#181823"
    readonly property color surface3:     "#1F1F2C"
    readonly property color surfaceHover: "#252534"
    property color surfaceActive:         "#123323"
    readonly property color overlay:      "#00000088"

    // ── GLASSMORPHISM ────────────────────────────────────────────
    readonly property color glass:        "#15152240"
    readonly property color glassBorder:  "#ffffff08"

    // ── BORDERS ──────────────────────────────────────────────────
    readonly property color border:       "#1A1A28"
    readonly property color borderLight:  "#28283A"
    property color borderAccent:          "#22C55E30"

    // ── TEXT ─────────────────────────────────────────────────────
    readonly property color text:          "#F0F0F5"
    readonly property color textSecondary: "#9898B0"
    readonly property color textMuted:     "#55556A"
    readonly property color textSubtle:    "#353548"

    // ── ACCENT — vivid EzClient green ────────────────────────────
    property color accent:      "#22C55E"
    property color accentHover: "#4ADE80"
    property color accentDark:  "#14532D"
    property color accentLight: "#86EFAC"
    property color accentGlow:  "#22C55E25"
    property color accentSoft:  "#22C55E12"

    // ── GRADIENTS (start / end) ──────────────────────────────────
    property color gradStart:             "#22C55E"
    property color gradEnd:               "#16A34A"

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
