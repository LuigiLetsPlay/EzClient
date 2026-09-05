# EzClient 2.0.0

Willkommen zu **EzClient 2.0.0** – dem bisher größten Meilenstein von EzClient! 🚀  
Diese Version bringt native Unterstützung für die moderne Minecraft 26.x-Reihe, ein komplett überarbeitetes Dark-Design, eine vereinte Modrinth- und CurseForge-Bibliothek mit Modpack-Profilerstellung, fehlerfreies Clear Glass ohne Pixellücken, animierte Capes und einen integrierten Schutz vor Windows Smart App Control.

---

## 🚀 Highlights & Neuerungen

### ⚡ Minecraft 26.x Active Engine & Ultra-Performance
- **Volle 26.x Unterstützung:** Speziell angepasste und optimierte Fabric Mod-JARs für Minecraft **`26.1`**, **`26.1.1`** und **`26.2`**.
- **Nativer Schnellstart:** Direkter Spielstart über die interne Java-Engine mit optimierten JVM-Flags und Memory-Pooling für maximale FPS und minimale Render-Latenzen.
- **Breite Versionskompatibilität:** Der Launcher unterstützt weiterhin alle Minecraft-Versionen von `1.8.9` bis `26.x` für **Vanilla**, **Fabric** und **Forge**.

### 🪟 Clear Glass (Connected Textures)
- **Sofortiger Chunk-Reload:** Beim Aktivieren oder Deaktivieren des Clear Glass Moduls im Ingame-Menü werden die Chunks sofort in Echtzeit neu geladen (kein manuelles `F3 + A` mehr nötig).
- **Lückenlose Außenränder:** Dynamische Kantenerkennung schließt vertikale Glasblöcke nahtlos ab – keine fehlenden Randpixel oder Risse mehr beim Bauen von Glastürmen und Glaswänden.

### 📦 Integrierte Mod- & Modpack-Bibliothek
- **Modrinth & CurseForge vereint:** Riesiger Katalog an Mods, Shadern und Resource Packs direkt durchsuchbar.
- **1-Klick-Modpack-Profilerstellung:** Die Installation von Modpacks erstellt jetzt vollautomatisch ein neues, eigenständiges Profil mit der passenden Minecraft-Version und allen benötigten Mods.
- **Smarte Versionsfilterung:** Bei Modpacks wird standardmäßig jede Version angezeigt (kein einschränkender Filter), da ohnehin ein eigenes Profil erzeugt wird.
- **Infinite Scrolling & Ladeanimation:** Der statische "Mehr"-Button am Fensterboden wurde durch flüssiges, automatisches Nachladen mit dezenter pulsierender Ladeanzeige ersetzt.
- **Intelligente Shader-Erkennung:** Beim Auswählen von Shadern prüft EzClient automatisch auf kompatible Shader-Loader (z. B. Iris) und bietet die Installation direkt an.

### 🖱️ Mod-Bibliothek UI & Mauszeiger
- **Normaler System-Mauszeiger:** Der störende Hand-/Drag-Mauszeiger über Suchergebnissen wurde durch den standardmäßigen Windows-Pfeilzeiger ersetzt.
- **Direkt klickbarer Installieren-Button:** Der 1-Klick-Installationsbutton liegt nun sauber im Vordergrund und führt die Installation direkt aus, ohne versehentlich die Mod-Detailansicht zu öffnen.

### 🎨 Modernes Dark UI & Profile Creation Hub
- **Neues Design & Navigation:** Klare NavigationRail, responsive Animationen und Minecraft-typische Typografie.
- **Vielseitige Profilerstellung:** Schnelleinstieg per Wizard, benutzerdefinierte Profile, Modpack-Suche oder 1-Klick-Import vorhandener Profile aus dem NoRiskClient samt Mods und Einstellungen.

### 🎭 Animierte Capes & Cape-Editor
- **Live-Wiedergabe:** Volle Unterstützung für animierte Capes (GIF/PNG) im Launcher und im Spiel über den Community-Server.
- **Integrierter Editor:** Capes direkt im Launcher pixelgenau anpassen, skalieren und zuschneiden.
- **Interaktive 3D-Vorschau:** Eigene Skins und Capes vor dem Start in 3D aus allen Winkeln betrachten.

### 🛡️ Ingame-Module & HUDs (Taste RSHIFT)
- **Große Modulauswahl:** ArmorStatus, Keystrokes, Coordinates, CPS- & FPS-Counter, Ping, DayCounter, Fullbright, MotionBlur, TimeWeather, AutoGG, Hitboxen, ItemPhysics, DamageTint, Reach, ComboCounter, BossBar u.v.m.
- **Flexibler HUD-Editor:** Mit der rechten Shift-Taste (`RSHIFT`) lassen sich alle Elemente direkt im Spiel frei anordnen und stylen.

### 🩺 Crash Doctor & Diagnose
- **Intelligente Fehlerbehebung:** Bei Startproblemen oder inkompatiblen Mods schlägt der Crash Doctor die Lösung sofort vor.
- **Live-Logs-Fenster:** Farbkodierte Konsolenausgabe zur einfachen Diagnose.

### 🪟 Nativer Windows Installer & Standalone Launcher
- **Inno Setup Engine:** Schnelle, native C++ Installation mit moderner LZMA2-Kompression, automatischen Verknüpfungen und sauberem Deinstallations-Eintrag in den Windows Apps & Features.
- **Authenticode Signatur:** Setup und Launcher sind mit vollständigen Windows PE-Versions- und Signatur-Metadaten ausgestattet.

---

## 📦 Downloads & Installation
- **`EzClient-Setup.exe`**: Komfortabler Windows-Installer (Desktop- & Startmenü-Verknüpfungen)
- **`EzClient.exe`**: Standalone Executable (sofort startbar)
- **`EzClient-2.0.0+26.1.jar`**: Client Mod JAR für Minecraft 26.1
- **`EzClient-2.0.0+26.1.1.jar`**: Client Mod JAR für Minecraft 26.1.1
- **`EzClient-2.0.0+26.2.jar`**: Client Mod JAR für Minecraft 26.2
