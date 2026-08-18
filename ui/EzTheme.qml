import QtQuick 2.15

pragma Singleton

QtObject {
    // ── BACKGROUNDS ──────────────────────────────────────────────
    readonly property color bg:           "#09090C"   // near-black, warmish
    readonly property color titlebarBg:   "#060608"
    readonly property color sidebarBg:    "#0D0D12"   // very slightly lighter
    readonly property color surface:      "#111118"   // primary card/panel
    readonly property color surface2:     "#17171F"   // raised elements
    readonly property color surface3:     "#1E1E28"   // hover/selected
    readonly property color surfaceHover: "#232330"
    readonly property color surfaceActive:"#14261C"   // active nav item (green tint)
    readonly property color overlay:      "#00000080"

    // ── BORDERS ──────────────────────────────────────────────────
    readonly property color border:       "#1E1E2A"
    readonly property color borderLight:  "#2A2A38"
    readonly property color borderAccent: "#1DB96840"

    // ── TEXT ─────────────────────────────────────────────────────
    readonly property color text:          "#FFFFFF"
    readonly property color textSecondary: "#A0A0B8"
    readonly property color textMuted:     "#606078"
    readonly property color textSubtle:    "#3A3A52"

    // ── ACCENT — vivid EzClient green ────────────────────────────
    readonly property color accent:      "#1DB968"
    readonly property color accentHover: "#25D474"
    readonly property color accentDark:  "#0A3D22"
    readonly property color accentLight: "#4AE896"
    readonly property color accentGlow:  "#1DB96830"

    // ── SEMANTIC ──────────────────────────────────────────────────
    readonly property color cyan:    "#38BDF8"
    readonly property color purple:  "#A78BFA"
    readonly property color orange:  "#FB923C"
    readonly property color danger:  "#F43F5E"
    readonly property color warning: "#FBBF24"

    // ── TYPOGRAPHY ────────────────────────────────────────────────
    readonly property string mcFontFamily: "Minecraft Default"
    readonly property string fontFamily:   "Minecraft Default"

    // ── DIMENSIONS ────────────────────────────────────────────────
    readonly property int sidebarWidth: 224
    readonly property int titlebarHeight: 40
    readonly property int statusbarHeight: 28
    readonly property int radius: 8
    readonly property int radiusSm: 5
}
