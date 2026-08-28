# EzClient 1.8.0

EzClient 1.8.0 ist ein umfangreiches Client-, Performance- und Launcher-Release. Der Schwerpunkt liegt auf einer vollständigeren HUD-Konfiguration, frühem Entity-Culling, einem abgesicherten Cape-Dienst und einer klaren Trennung zwischen verwalteten und eigenen Profil-Mods.

## 🚀 Highlights

- Neues, nativ integriertes Entity- und BlockEntity-Occlusion-Culling vor der Render-State-Extraktion.
- Stark erweiterter HUD-Editor mit konsistenten Design-, Farb-, Skalierungs- und Layoutoptionen.
- Vollständig überarbeitetes Crosshair mit Zielklassen, dynamischem Feedback und individuellen Zielregeln.
- Neuer mehrstufiger Profil-Wizard für EzClient- und Raw-/Vanilla-Profile.
- Gehärteter Cape-Upload-Service mit Minecraft-Session-Prüfung, Besitzer-Tokens und animierten Capes.
- Einheitliche Versionsanhebung für Launcher, Installer, Client Core und Lite Core auf 1.8.0.

## ✨ Neue Features & Modul-Anpassungen

### Crosshair

- Formen: Classic Cross, Custom Cross, Dot, Circle, T-Shape und Chevron.
- Einstellbare Größe, vertikale Größe, Gap, Linienstärke, Punktgröße, Deckkraft und Outline.
- Eigene Farben und Skalierungsfaktoren für Spieler, feindliche Mobs, neutrale Mobs und sonstige Entitäten.
- Separate Block-Zielfarbe und auswählbare Target-Modi.
- Dynamisches Spreizen bei Bewegung, Sprüngen und nicht vollständig abgelaufenem Angriffs-Cooldown.
- Automatisches Ausblenden in Third-Person, im F3-Debug-Screen und beim Spannen eines Bogens.
- Die Farbauswahl bearbeitet direkt die aktuell ausgewählte Zielregel.

### HUD und Utility

- Gemeinsame HUD-Basis für Drag-and-Drop-Positionierung und Skalierung.
- Solid-, Wave- und Rainbow-Farbmodi mit Geschwindigkeit und Sättigung.
- Konfigurierbare Hintergründe, Rahmen, Rahmenbreite, Eckenradius, Textschatten und Custom-Font-Modus.
- Erweiterte Optionen für FPS, CPS, Ping, Koordinaten, Keystrokes, Armor Status, Potion Effects, Day Counter und Toggle Sprint/Sneak.
- Überarbeitete Modul-Icons, Live-Vorschau und Reset-Workflow.
- Ungültige Hotkey-Werte werden verworfen, bevor sie GLFW erreichen.

## ⚡ Performance & Engine

- Asynchroner Occlusion-Worker mit lock-frei veröffentlichten Visibility-Snapshots.
- Frühes Frustum- und Occlusion-Culling verhindert Render-State-Aufbau und Draw-Submission für verdeckte Entities und BlockEntities.
- Chunk-/Section-Occluder werden inkrementell aktualisiert; große Entities und Render-Ausnahmen werden gesondert behandelt.
- `CompactStateTable` speichert statische Property-Kombinationen und Neighbor-Transitions in flachen Arrays mit Byte-IDs.
- `CanonicalPool` stellt schwaches, thread-sicheres Interning für unveränderliche Modell- und Quad-Daten bereit.
- Reduzierter Draw-Call-Druck durch Abbruch vor Model-, Matrix- und Vertex-Verarbeitung.
- Sodiums Chunk-Shader- und VAO-Pipeline bleibt unangetastet.

## 🛠️ Bugfixes & Backend

- Neuer `server.py` mit `/api/capes`, `/upload_cape`, `/get_cape/<uuid>` und Health-Endpunkt.
- UUIDs mit und ohne Bindestriche werden kanonisch verarbeitet; ungültige Spielerdaten liefern nachvollziehbare 400-Antworten.
- Erst-Uploads werden gegen die Minecraft-Session geprüft; Folgeänderungen können sichere Besitzer-Tokens verwenden.
- PNG-Uploads werden auf Signatur, CRC, Abmessungen, Pixelformat und Größe geprüft.
- GIF-, MP4- und WebM-Quellen nutzen begrenzte Frame-, Laufzeit-, FPS- und Dateigrößenlimits.
- Cape-Dateien und Metadaten werden atomar geschrieben.
- Crosshair-Auto-Hide unterdrückt jetzt auch das Vanilla-Fadenkreuz korrekt.
- V-/Hotkey-Crash-Pfad durch validierte GLFW-Keybereiche abgesichert.
- Client-Konfiguration wird UTF-8-kodiert und per atomarem Replace gespeichert.
- Doppelte QML-ID in den RAM-Presets entfernt.

## 📦 Launcher & Profile-Management

- Neuer Wizard mit EzClient- und Raw-/Vanilla-Profiltypen.
- EzClient-Profile verwenden den verwalteten Core-Stack aus EzClient, Sodium, Lithium und Iris.
- Optionale Mods wie Simple Voice Chat und Essential sind vollständig Opt-in.
- Parallele Mod-Downloads mit isolierter Fehlerbehandlung und anschließendem Dependency-Abgleich.
- `profile.json` trennt `managed_core_mods` und `user_mods`.
- Versionsgebundene Startmigration entfernt nur nachweislich vom Launcher verwaltete Alt-Mods.
- Manuell installierte Mods und unbekannte JARs bleiben unangetastet.
- Fehlgeschlagene Dateisystemmigrationen werden nicht als abgeschlossen markiert und beim nächsten Start erneut versucht.

## ✅ Qualitätssicherung

- Sauberer Fabric-Loom-Build für EzClient Core.
- Build des versionsübergreifenden EzClient Lite Core.
- Python-Syntax- und Import-Prüfungen für Launcher, Backend und Cape-Server.
- Automatisierte Tests für Profil-Migration, User-Mod-Schutz, PNG-/GIF-/MP4-Uploads, UUID-Validierung, Authentifizierung und Besitzer-Tokens.
- Vollständiger QML-Syntaxcheck ohne Fehler.

## Kompatibilität

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 oder neuer
- Java 25 oder neuer für den vollständigen Core
- Sodium, Lithium und Iris werden vom Launcher als verwalteter Core-Stack installiert
