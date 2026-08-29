# EzClient 1.8.2

EzClient 1.8.2 verbessert die Ingame-Performance, die Glasdarstellung und die Synchronisierung animierter Capes zwischen Launcher, Community-Server und Spiel.

## 🚀 Highlights & Fehlerbehebungen

### ⚡ Performance & Rendering
- **Optimierte Render- & HUD-Abläufe**: Reduzierte Frame-Belastung und Caching zur Vermeidung unnötiger Berechnungen pro Frame.
- **Überarbeitete Sichtbarkeitsoptimierung**: Interne Occlusion- und Visibility-Engine setzt auf eigenständige, entkoppelte EzClient-Komponenten.
- **Entlasteter Render-Thread**: Cape-Synchronisierung und Animationsverarbeitung laufen begrenzt und überwiegend außerhalb des Render-Threads.

### 🪟 Clear Glass (Connected Textures)
- **Zuverlässige Glasflächen**: Nahtlose Ausblendung innerer Kanten bei zusammenhängenden Glasblöcken.
- **Präzise Außenränder**: Der charakteristische weiße Außenrand bleibt sauber erhalten.

### 🎭 Animierte Capes & Community Sync
- **Ingame-Wiedergabe**: Animierte Capes werden live im Spiel abgespielt und über den Community-Server geladen.
- **Launcher-Vorschau**: Die Home-Ansicht im Launcher zeigt aktive animierte Capes in Echtzeit.
- **Live-Übernahme**: Cape-Änderungen im Launcher werden während des Spiels automatisch übernommen.
- **Editor-Stabilität**: Der Zuschnitt im Cape-Editor lässt sich wieder zuverlässig und stabil verschieben und skalieren.
- **Reset-Funktion**: Neuer Zurücksetzen-Button entfernt das EzClient-Cape und stellt das Mojang-/Microsoft-Cape wieder her.

## 📦 Downloads & Installation
- **`EzClient-Setup.exe`**: Komfortabler Windows-Installer (automatische Verknüpfungen & Updates)
- **`EzClient.exe`**: Standalone Executable (ohne Installation sofort startbar)
- **`EzClient-1.8.2.jar`**: Core Client Mod JAR
- **`EzClient-Lite-1.8.2.jar`**: Versionsübergreifender Lite Mod JAR
