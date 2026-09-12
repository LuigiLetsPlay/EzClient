# EzClient 2.1.0

Willkommen zu **EzClient 2.1.0**! 🚀  
Dieses monumentale Release ist das bisher größte Update von EzClient und umfasst über **11.000 neue Codezeilen**, dutzende brandneue Client-Module, ein von Grund auf renoviertes Ingame-UI-System mit HEX-Farbauswahl und 3D-Blockselektion, volle Unterstützung für Standard-Minecraft-Capes (64x32 Vollformat) sowie einen hochmodernen Launcher mit Video-Hintergründen, 3D-Skin-Inspektion, Modpack-Installationen und Server-Direktstart.

---

## 🌟 Highlights & Wichtigste Neuerungen

### 🎮 Brandneue Ingame-Module & Gameplay-Features

1. 🏹 **Arrow Trail Module (`ArrowTrailModule`)**
   - Erzeugt anpassbare Partikelspuren und Schweife hinter abgefeuerten Pfeilen.
   - Verschiedene Partikel-Modi, Farben, Dichte und Lebensdauer einstellbar.

2. ⏱️ **Basti Timer Module (`BastiTimerModule`)**
   - Integrierter Timer und Stoppuhr für Speedruns, Challenges und Minigames.
   - Start-, Pause- und Reset-Funktionen direkt per Tastenkürzel oder Ingame-Menü.
   - Formatierung als `hh:mm:ss` oder `mm:ss:ms`, frei positionierbar auf dem HUD.

3. 🧊 **Block Overlay & Interaktive 3D-Blockauswahl (`BlockSelectionOverlay` & `BlockSettingsScreen`)**
   - Individuelle Farb- und Konturregeln für jeden einzelnen Minecraft-Blocktyp.
   - **Interaktiver 3D-Auswahlmodus:** Mit `[ ⛶ Blöcke wählen ]` können Blöcke direkt in der Spielwelt mit der Maus markiert werden.
   - Visuelle Gizmos heben anvisierte und markierte Blöcke farbig hervor.
   - Schutz vor versehentlichem Abbauen oder Platzieren während der Auswahl. Speichern mit `Enter`, Abbruch mit `ESC`.

4. 🎯 **Crosshair Designer V2 & Target-Regeln (`CrosshairPaintScreen` & `CrosshairTargetSettingsScreen`)**
   - Neuer Pixel-by-Pixel Crosshair-Editor zum Zeichnen komplett eigener Fadenkreuze.
   - Dynamische Target-Einfärbung (Farbe ändert sich automatisch beim Anvisieren von Spielern oder Mobs).
   - Punkt-Modus, Fadenkreuz-Spreizung bei Bewegung/Angriff und Liniendicke frei konfigurierbar.

5. 🩸 **Damage Tint Overhaul (`DamageTintEntityScreen`)**
   - Individuelle Schadensfarben pro Entity-Typ.
   - Neuer durchsuchbarer Einstellungsdialog mit Live-Suche und einheitlicher Farbauswahl.
   - Störende, statische Vorschauanzeigen wurden entfernt.

6. 📆 **Day Counter Upgrade (`DayCounterModule`)**
   - Zuverlässige Erfassung der Spielzeit und verstrichenen Tage über echte Minecraft-Serverstatistiken (`Stats.PLAY_TIME` / `Stats.TOTAL_WORLD_TIME`).
   - Umschaltung zwischen Start bei `Tag 0` oder `Tag 1`.

7. 🔄 **Freelook / 360° Orbit-Kamera (`FreelookModule` & `FreelookCameraMixin`)**
   - Echter 4-Block-Orbit um den Spieler im F5-Modus ohne synchrone Kopfrotation des Spielermodels.
   - Vollständige Behebung von Chunk-Culling: Chunks hinter dem Spieler bleiben beim Umschauen nahtlos sichtbar.

8. ✨ **Glint Customizer (`GlintCustomizerModule` & Mixins)**
   - Individuelle Verzauberungs-Farbe, Animationsgeschwindigkeit und Glanz-Intensität für Items und angelegte Rüstung.

9. 💎 **Glowing Ores (`GlowingOresModule` & `BlockModelLighterMixin`)**
   - Lässt Erze in Höhlen und Dunkelheit leuchten.
   - Unterstützt eigene Farben pro Erzart, Helligkeitsstufen und optionale Kontur-/Outline-Modi.

10. 📸 **High-Quality Screenshot Module (`HighQualityScreenshotModule`)**
    - Hochauflösende Screenshots mit 2x oder 4x Supersampling ohne Verzerrungen des HUDs.

11. 📦 **Hitbox Visualizer & Entity-Type-Regeln (`EntityTypeSettingsScreen`)**
    - Kompakte, hochauflösende Tabellenansicht mit Live-Suche nach Entity-Namen.
    - 3-State-Pills (`Auto` / `An` / `Aus`), stufenlose Liniendicke-Stepper (`‹ 1.0 ›`) und Farbswatches für jeden Entity-Typ.

12. ⌨️ **Keystrokes Designer V2 & RGB-Farbwelle (`KeystrokesSettingsScreen` & `KeystrokesDesignerScreen`)**
    - Komplett überarbeiteter Einstellungsbildschirm im modernen Glowing-Ores-Kategorienstil.
    - Layout-Presets, Leertasten-Designs, CPS-Zähler und einstellbare Tastenabstände.
    - Neue Effekte: RGB-Welle, dynamische Farbverläufe, Chroma und Anschlags-Animationen.

13. 🌫️ **NoFog Module (`NoFogModule` & `NoFogMixin`)**
    - Feingranulare Deaktivierung von Nebel in der Überwelt, im Nether, im End, in Lava, unter Wasser und im Pulverschnee.

14. 🔮 **Partikel-Customizer: Neue Typen-Verwaltung (`ParticleTypesScreen`)**
    - Sämtliche 100+ Partikel-Flags wurden aus der Hauptseite in eine eigene Unterseite mit Live-Suche ausgelagert.
    - Schnellbuttons für "Alle an" und "Alle aus" sowie Einzel-Toggles pro Partikeltyp.

15. 🌈 **Saturation Module (`SaturationModule`)**
    - Dynamische Anpassung von Farbsättigung und Kontrast für eine lebendigere Spieloptik.

16. 📦 **Shulker- & Karten-Vorschau (`ShulkerPreviewModule` & `ShulkerPreviewMixin`)**
    - Hover-Vorschau für Shulker-Boxen und Karten im Inventar.
    - Vanilla-Tooltips (Map-Lore, Shulker-Inhaltstext) werden automatisch unterdrückt, sodass nur die saubere EzClient-Kachel sichtbar ist.

17. 🎵 **Spotify HUD Overlay (`SpotifyOverlayModule`)**
    - Live-Anzeige des aktuell wiedergegebenen Musiktitels, Interpreten und Album-Covers direkt im Spiel.

18. 🦸 **Wavey Capes & 3D Cape Container (`WaveyCapesModule` & `WaveyCapeModel`)**
    - Fehlerhaftes Streifen-Rendering behoben: Capes werden als solider, korrekter 3D-Container (`10x16x1`) gerendert.
    - Sanfte Wellenphysik beim Laufen, Sprinten und Fliegen.

19. 📍 **Waypoints Manager & Xaero-Share (`WaypointScreen` & `XaeroWaypointShare`)**
    - Vollständige Wegpunkt-Verwaltung mit Beams, Distanzanzeigen, Dimension-Filterung und Teleport-Befehlen.
    - Nahtloser Im- und Export von Wegpunkten aus Xaero's Minimap.

20. 🛏️ **Bedwars Hypixel Overlay (`BedwarsModule`)**
    - Automatische Lobby-Erkennung: Overlay wird in Lobbys ausgeblendet (keine Geisteranzeigen von Eisen/Gold außerhalb des Matches).
    - 1:1 Editor-Maßstab und Begrenzung an den Bildschirmrändern.

21. 🔍 **FOV Changer & Memory Module**
    - Static-FOV-Modus sperrt und graut Modifikator-Slider automatisch aus; 0.05x-Stufen erlauben exaktes Einstellen von 1.00x.
    - Memory-Modul mit übersichtlicher Speicherauslastung und dynamischen Warnfarben (>70% Orange, >85% Rot).

---

### 🎨 Einheitliches UI- & Design-System Ingame

- 🎨 **Universal ModuleColorScreen mit HEX-Eingabe:**
  - Jedes Modul nutzt nun den gleichen modernen Farbdialog.
  - Interaktive HEX-Eingabe (`#RRGGBB` oder `#AARRGGBB`) mit direkter Tastatureingabe.
  - Automatische 2-Wege-Synchronisation mit HSV-Feld, Farbkreis-Hue, Alpha-Schieberegler und 3x3 Farb-Presets.
- 📜 **ScrollingSettingsScreen Architektur:**
  - Feste, aufgeräumte Panel-Größe (416x236) für alle Einstellungsdialoge mit weichem Scrollen und Scissor-Clipping.
- 🖱️ **Hardware EzCursor:**
  - Mauszeiger wechselt dynamisch auf einen Pointer-Cursor bei klickbaren Komponenten.
- ⌨️ **Hotkey-System Bereinigung:**
  - `[+K]`-Badges von Hub-Karten entfernt. Hotkeys werden sauber und zentral in der linken Seitenleiste der jeweiligen Moduleinstellungen konfiguriert.
- 👁️ **Keine Fake-Vorschauen:**
  - Module ohne visuelle HUD-Darstellung (wie Nameplate, Waypoints, Damage Tint, Fullbright) zeigen kein irreführendes Vorschau-Icon (`◉`) mehr an.

---

### 💻 Launcher, Backend & Client-Erkennung

- 🌐 **Startseite & Recent Servers:**
  - Server-Pinger mit Live-Status, Spieleranzahl, MOTD und 1-Klick-Direktstart für zuletzt gespielte Server.
- 🎬 **Hintergrund-Videos & Ambient-Effekte:**
  - Eigene Videos (MP4, WEBM, MOV, MKV) können als dynamischer Launcher-Hintergrund festgelegt werden.
  - Stimmungsvolle Schwebepartikel auf der Startseite.
- 🦸 **3D Cape- & Skin-Inspektion:**
  - Vollwertige `Skin3DView`-Komponente auf der Startseite, der Cape-Seite und im Vorschau-Modal für eine freie 360°-Betrachtung.
- 🎨 **Cape-Editor Overhaul:**
  - Volle Unterstützung des Standard-Minecraft-Cape-Formats (**64x32 / 2:1 HD**): Alle 6 Seiten (Vorderseite, Rückseite, Kanten) und Elytren werden 1:1 ohne Verzerrung übernommen.
  - Moduswahl zwischen *Minecraft Cape (64x32)* und *Eigenes Motiv (Zuschneiden & 3D)*.
  - Video-Trimmer und GIF-Unterstützung bleiben vollständig aktiv.
- ⚡ **Offline-sicherer "Nutzen"-Button:**
  - Capes werden lokal sofort gespeichert und aktiviert, bevor der Hintergrund-Sync zum Server ausgeführt wird.
- 📦 **Modpack-Installation:**
  - Modrinth-Modpacks können mit einem Klick installiert werden; automatischer Fortschritts-Dialog und Profilerstellung.
- 📋 **Live-Log-Service:**
  - Separates Log-Fenster mit Echtzeit-Filtern (`ALL`, `INFO`, `WARN`, `ERROR`), Textsuche und intelligentem Autoscroll.
- 🔒 **Community-Server & Presence:**
  - HMAC-Verifikation über Mojangs `hasJoined`-Endpunkt, Event-Streams (SSE) für Live-Cape-Übertragungen und Online-Status.

---

## 📦 Downloads & Installation

- **`EzClient-Setup.exe`**: Nativer Windows-Installer (Inno Setup mit LZMA2-Kompression und Authenticode-Signatur).
- **`EzClient-v2.1.0-Windows-Portable.zip`**: Portable Version (einfach entpacken und direkt `EzClient.exe` starten).
- **`EzClient-2.1.0+26.2.jar`**: Client Mod JAR für Minecraft 26.2.
- **`EzClient-2.1.0+26.1.1.jar`**: Client Mod JAR für Minecraft 26.1.1.
- **`EzClient-2.1.0+26.1.jar`**: Client Mod JAR für Minecraft 26.1.
