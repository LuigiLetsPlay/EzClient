# EzClient 1.8.2

EzClient 1.8.2 verbessert die Ingame-Performance, die Glasdarstellung und die Synchronisierung animierter Capes zwischen Launcher, Community-Server und Spiel.

## Highlights und Fehlerbehebungen

### Performance

- Rendering- und HUD-Abläufe wurden reduziert und zwischengespeichert, um unnötige Arbeit pro Frame zu vermeiden.
- Die interne Sichtbarkeitsoptimierung wurde überarbeitet und verwendet eigenständige EzClient-Komponenten.
- Cape-Synchronisierung und Animationsverarbeitung laufen begrenzt und überwiegend außerhalb des Render-Threads.

### Clear Glass

- Zusammenhängende Glasflächen blenden innere Kanten zuverlässiger aus.
- Der gewünschte äußere weiße Rand bleibt erhalten.

### Animierte Capes

- Animierte Capes werden jetzt ingame abgespielt und über den Community-Server geladen.
- Die Home-Vorschau im Launcher zeigt das aktive Cape animiert.
- Änderungen aus dem Launcher werden während des Spiels automatisch übernommen.
- Der Zuschnitt im Cape-Editor lässt sich wieder stabil verschieben und skalieren.
- Ein neuer Zurücksetzen-Button entfernt das EzClient-Cape und aktiviert wieder das Mojang-/Microsoft-Cape.

## Downloads

- **`EzClient-Setup.exe`**: Windows-Installer
- **`EzClient.exe`**: Standalone-Launcher
- **`EzClient-1.8.2.jar`**: normale EzClient-Mod
- **`EzClient-Lite-1.8.2.jar`**: EzClient-Lite-Mod
