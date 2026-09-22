# EzClient 2.2.2

EzClient 2.2.2 behebt ein Startproblem beim Aufruf des offiziellen Launchers sowie ein Darstellungsproblem in der Erweiterungs-Übersicht. Der Launcher bleibt vollständig kompatibel mit Vanilla, Fabric und Forge von Minecraft 1.8.9 bis 26.x; die aktiv gepflegten EzClient-Mod-Builds werden weiterhin separat und exakt für 26.1, 26.1.1 und 26.2 ausgeliefert.

## Highlights & Fehlerbehebungen

- **Start-Korrektur (Offizieller Launcher-Pfad):** Behebt einen `TypeError` (`patch_launcher_profile() takes 1 positional argument but 2 were given`), der beim Ausführen des Fallbacks auf den offiziellen Launcher auf frischen Installationen auftreten konnte.
- **Erweiterungen-Sichtbarkeit korrigiert:** Die vorinstallierten Performance-Mods werden in der Erweiterungsliste nun standardmäßig angezeigt (Filter *Integrierte ausblenden* ist standardmäßig deaktiviert). Der Mod-Zähler im Header und die Liste stimmen nun sofort nach Erstinstallation und Profilerstellung überein.
- **Glowing Ores 2.0 Lokalisierung:** Alle neu hinzugekommenen Anzeige- und Einstellungstexte für Glowing Ores 2.0 (u. a. Konturen, Erzader-Verbindungen, Emissive-Leuchten und Farbdefinitionen) wurden vollständig in deutscher und englischer Sprache integriert.
- **Versionssprung 2.2.2:** Sämtliche Komponenten, Netzwerk-Clients und Mod-Metadaten wurden auf Version 2.2.2 angehoben.

## Launcher, Profile und Mods

- Profile behalten ihre getrennten, versionsgenauen Mod-Zustände und benutzerverwalteten JARs.
- EzClient Core wird nur für die aktiv unterstützten 26.x-Ziele eingebunden; es gibt keinen Fallback auf eine JAR einer anderen Minecraft-Version.
- Vanilla-, Fabric- und Forge-Profile bleiben über den vollständigen Launcher-Katalog von 1.8.9 bis 26.x verfügbar.
- Die eingebetteten Mod-Artefakte wurden für 26.1, 26.1.1 und 26.2 frisch gebaut und in Launcher, Installer und Portable-Paket abgeglichen.

## Downloads

- **`EzClient-Setup.exe`** – nativer Windows-Installer für die empfohlene Installation und Updates.
- **`EzClient-v2.2.2-Windows-Portable.zip`** – portable Windows-Version zum Entpacken und direkten Starten.
- **`EzClient-2.2.2+26.2.jar`** – EzClient Fabric-Mod für Minecraft 26.2.
- **`EzClient-2.2.2+26.1.1.jar`** – EzClient Fabric-Mod für Minecraft 26.1.1.
- **`EzClient-2.2.2+26.1.jar`** – EzClient Fabric-Mod für Minecraft 26.1.

## SHA-256

- `EzClient-Setup.exe`: `e9a4fb62a22a696509d7b5c2dbc9c5ad8fe78bb69ec8a72a2416ac6432d2b95b`
- `EzClient-v2.2.2-Windows-Portable.zip`: `fa25c1a6350b1946f6dda5a1b2b16590081125621d53b1cedf374d6310d5a8aa`
- `EzClient-2.2.2+26.2.jar`: `fce4e4dcf34f09efd9b4df5a23cce2c6e9714f7587fed8bd1f3c7b4226eac68c`
- `EzClient-2.2.2+26.1.1.jar`: `a84007436f81c9653596c00b9814ff06f277d39477adc41b482c0fc28cf9a2c1`
- `EzClient-2.2.2+26.1.jar`: `fc4ccdb8e4e3fffa05a8e8dab3a58fceab27a6dae0a685e5b03af1e4d9e24cb8`
