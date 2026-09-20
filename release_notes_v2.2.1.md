# EzClient 2.2.1

EzClient 2.2.1 verbessert vor allem die Anpassbarkeit und Stabilität der Ingame-Module. Der Launcher bleibt vollständig kompatibel mit Vanilla, Fabric und Forge von Minecraft 1.8.9 bis 26.x; die aktiv gepflegten EzClient-Mod-Builds werden weiterhin separat und exakt für 26.1, 26.1.1 und 26.2 ausgeliefert.

## Highlights

- **Neues Crosshair-Einstellungszentrum:** Presets, Paint-Editor, Zielregeln, Farbmodi, einzelne Elemente und Hotkey werden zentral konfiguriert und dauerhaft gespeichert.
- **Überarbeiteter HUD-Editor:** Module lassen sich zuverlässiger positionieren, skalieren und an Bildschirmkanten verankern. Editor-Größen und Anker bleiben über Auflösungswechsel hinweg erhalten.
- **Glowing Ores 2.0:** Native, pixelbasierte Erz-Konturen und leuchtende Erzpixel ersetzen den alten Scan-Renderer. Verbundene Adern, Emissive-Darstellung, dynamisches Umgebungslicht und optionale Farbtönung sind direkt konfigurierbar.
- **Ausgebaute Moduleinstellungen:** Mehr Kontrolle für Scoreboard, Ping-Anzeige mit Server-Icon, Spotify-Overlay, FOV, Zoom, Fullbright, Motion Blur, TNT Timer, Block Overlay, Hitboxen, Damage Tint und Item-Modelle.
- **Verbesserte Item-Model-Konfiguration:** Präzisere Vorschau und übersichtlichere Einstellungen für Handposition, Skalierung, Rotation und Animationsstile.
- **Cape-Studio-Feinschliff:** Verbesserte Bedienung und Darstellung im Editor.
- **Erweiterte Lokalisierung:** 130 neue deutsche und englische UI-Texte für die neuen Moduloptionen.

## Launcher, Profile und Mods

- Profile behalten ihre getrennten, versionsgenauen Mod-Zustände und benutzerverwalteten JARs.
- EzClient Core wird nur für die aktiv unterstützten 26.x-Ziele eingebunden; es gibt keinen Fallback auf eine JAR einer anderen Minecraft-Version.
- Vanilla-, Fabric- und Forge-Profile bleiben über den vollständigen Launcher-Katalog von 1.8.9 bis 26.x verfügbar.
- Die eingebetteten Mod-Artefakte wurden für 26.1, 26.1.1 und 26.2 frisch gebaut und in Launcher, Installer und Portable-Paket abgeglichen.

## Qualitätssicherung

- Python-Regressionstests für Profile, Mod-Verwaltung, Launcher-Aktionen, Downloads und Minecraft-Startpfade ausgeführt.
- Python-Syntaxprüfung ohne Fehler.
- Alle drei 26.x-Gradle-Builds erfolgreich; `fabric.mod.json` enthält jeweils Version 2.2.1 und die exakte Minecraft-Abhängigkeit.
- Paketvergleich bestätigt bytegleiche JAR-, QML- und JavaScript-Dateien zwischen Quellcode, Launcher-Runtime und Portable-ZIP.
- Laufender Minecraft-26.2-Realtest bestätigt die erfolgreiche Initialisierung von EzClient Core 2.2.1 ohne EzClient-Mixin- oder Startfehler.
- Launcher und Installer wurden mit Authenticode signiert und mit DigiCert-Zeitstempel versehen.

## Downloads

- **`EzClient-Setup.exe`** – nativer Windows-Installer für die empfohlene Installation und Updates.
- **`EzClient-v2.2.1-Windows-Portable.zip`** – portable Windows-Version zum Entpacken und direkten Starten.
- **`EzClient-2.2.1+26.2.jar`** – EzClient Fabric-Mod für Minecraft 26.2.
- **`EzClient-2.2.1+26.1.1.jar`** – EzClient Fabric-Mod für Minecraft 26.1.1.
- **`EzClient-2.2.1+26.1.jar`** – EzClient Fabric-Mod für Minecraft 26.1.

## SHA-256

- `EzClient-Setup.exe`: `6181aed6b890d54b8ec5a3d86781cd9cce967c8b89adc98cd3a97008b1dc3b8c`
- `EzClient-v2.2.1-Windows-Portable.zip`: `6a39bf2d0096ef8f1fb8425dfc2eeab34cc01dca253400980e283a9b9a270dee`
- `EzClient-2.2.1+26.2.jar`: `47d6c8bcf56145bf6406f08ca595c0cec305c0128a53c8bc2f33f6db9b372b26`
- `EzClient-2.2.1+26.1.1.jar`: `38e8001d94b7fec703c7604d83f793c356b87191e5b40a1e53b3613a189e6520`
- `EzClient-2.2.1+26.1.jar`: `5b9c7c9af2dfd0e92758081074aa07563066ee1ae508aa31a2f16312a17b417b`
