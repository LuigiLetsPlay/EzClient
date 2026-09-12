# EzClient 2.0.1

Willkommen zu **EzClient 2.0.1**! 🚀  
Dieses Update bringt wichtige Verbesserungen für den Ingame HUD-Editor, brandneue individuelle Modul-Icons für alle erweiterten Client-Module, Fehlerbehebungen im Cape-Editor für animierte GIFs sowie die neue produktionsreife Windows-Ordner-Architektur (`--onedir`) mit extrem schnellen Startzeiten.

---

## 🚀 Highlights & Neuerungen

### 🖥️ Ingame HUD-Editor: Perfektes Zentrieren & Volle Freiheit
- 🚫 **Keine Geister-Module mehr:** Deaktivierte ("AUS") Module werden im HUD-Editor nicht mehr als transparente Boxen mit roten "AUS"-Badges angezeigt und können nicht mehr versehentlich angeklickt oder verschoben werden. Es sind nur noch die tatsächlich aktiven Module sichtbar und editierbar.
- 🎯 **Mitten-Snapping (X- & Y-Achse):** Module rasten beim Verschieben nun präzise an der horizontalen (`width / 2`) und vertikalen (`height / 2`) Mittelachse des Bildschirms ein. Helle Cyan-Führungslinien zeigen die Achsen visuell an – für ein perfekt symmetrisches und aufgeräumtes HUD!
- 📐 **Platzierung im gesamten Minecraft-Fenster:** Die künstliche Schranke am unteren Bildschirmrand wurde vollständig entfernt. Module können ab sofort millimetergenau bis an die unterste Kante platziert werden (z. B. direkt neben die Hotbar oder den XP-Balken).

### 🎨 Brandneue Modul-Icons
- **Vollständige Icon-Palette:** Alle Module ab dem *Hitbox Visualizer* verfügen nun über eigene, handgefertigte 64x64 RGBA-Icons im modernen EzClient Mint- & Smaragd-Look:
  - 📦 **Hitbox Visualizer** (`hitbox.png`): 3D-Hitbox-Wireframe mit Blickvektor & Augenhöhe
  - 💎 **Item Physics** (`item_physics.png`): Rotierendes 3D-Item mit Bodenreflexion und Flugbahnbogen
  - ☀️ **Time & Weather** (`time_weather.png`): Sonne mit Strahlen und Regenwolke
  - ✨ **Particle Customizer** (`particle.png`): Magische Glitzersterne und Crit-Partikel
  - 🧊 **Block Overlay** (`block_overlay.png`): 3D-Blockauswahl mit Eck-Brackets und Konturen
  - 👑 **Boss Bar** (`boss_bar.png`): Boss-Lebensbalken mit Krone und Segmentkerben
  - 🛏️ **Bedwars** (`bedwars.png`): Minecraft-Bett mit Kissen, Decke und Bettrahmen
  - 🏷️ **Nameplate / Levelhead** (`nameplate.png`): Namensschild-Badge mit Spieler-Avatar
  - 📍 **Waypoints** (`waypoints.png`): Karten-Pin mit Zielring und Bodenimpuls
  - 🔊 **Sound Enhancer** (`sound.png`): Lautsprecher mit Schallwellen
  - ⚡ **Memory / RAM** (`memory.png`): RAM-Riegel mit Speicherchips und Kontakt-Pins
  - ⏰ **Clock** (`clock.png`): Saubere Einbindung des Uhr-Icons

### 🎭 Cape-Editor: Motiv & Grid-Fix für GIFs
- 🎬 **Funktionierendes 10:16 Cape-Motiv für GIFs:** Beim Laden von animierten GIFs und Videodateien wird das Cape-Motiv-Zuschneidefenster sofort im korrekten 10:16-Cape-Seitenverhältnis initialisiert.
- 📏 **Pixel-Grid Repaint-Fix:** Das Raster (Pixel-Grid) passt sich dynamisch an Größenänderungen an und bleibt bei animierten Medien dauerhaft gestochen scharf sichtbar.
- ⚡ **Cache-Busting:** Kein Verharren auf veralteten Zwischenspeichern bei erneutem Laden von Animationen.

### ⚡ Produktions-Ordner-Architektur (`--onedir`)
- **Sofortstart unter 1 Sekunde:** Durch die Umstellung von monolithischer Einzel-EXE auf eine native Ordner-Architektur entfällt das lästige temporäre Entpacken von 250 MB nach `%TEMP%`.
- **Maximale Antivirus-Verträglichkeit:** Windows SmartScreen und Antivirenprogramme stufen den Launcher als reguläres Programm ein.
- **Sauberer Inno Setup Installer & Portable ZIP:** Das Setup installiert EzClient nach `AppData\Local\Programs\EzClient\`, während die portable Version ohne Installation überallhin mitgenommen werden kann.

---

## 📦 Downloads & Installation
- **`EzClient-Setup.exe`**: Nativer Windows-Installer (Desktop- & Startmenü-Verknüpfungen)
- **`EzClient-v2.0.1-Windows-Portable.zip`**: Portable Version (entpacken und direkt `EzClient.exe` starten)
- **`EzClient-2.0.1+26.1.jar`**: Client Mod JAR für Minecraft 26.1
- **`EzClient-2.0.1+26.1.1.jar`**: Client Mod JAR für Minecraft 26.1.1
- **`EzClient-2.0.1+26.2.jar`**: Client Mod JAR für Minecraft 26.2
