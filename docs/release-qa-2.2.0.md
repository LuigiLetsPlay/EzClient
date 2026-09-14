# EzClient 2.2.0 – Release-Prüfung

Stand: 13. September 2026. Lokale Prüfung und lokale Builds; nichts veröffentlicht.

## Korrigierte Probleme

- Fabric-Metadaten werden nur für die angeforderte Minecraft-Version verwendet. Importierte, festgelegte Loader-Versionen werden exakt aufgelöst; ein fehlender Fabric-Loader startet nicht unbemerkt Vanilla.
- Forge-Profile ohne vorher gespeicherte Loader-Version lassen sich vorbereiten. Alte und neue Forge-Versionskennungen werden normalisiert, installierte Metadaten nach Basisversion und gewünschtem Loader geprüft.
- Bibliotheken mit Java-Artefakt und separaten nativen Bibliotheken werden vollständig heruntergeladen und im Startpfad korrekt behandelt. Native DLLs bleiben bei einem Austausch ihrer Quellbibliothek nicht veraltet.
- Downloads prüfen vorhandene Dateien mit bekannter Prüfsumme sowie die erwartete Dateigröße. Fehlgeschlagene Minecraft-Asset-Downloads werden als Fehler weitergegeben.
- Performance-Profile behalten ihren Profiltyp beim Laden. Importierte Modpack-Pins werden exakt wiederhergestellt und anhand verfügbarer Prüfsummen validiert.
- Fehlende QML-Signatur für die Modinstallation, Benachrichtigung beim Profilwechsel und sichere Paket-Löschaktion ergänzt. Fehlerberichte können vollständige Tracebacks enthalten.
- 829 zusätzliche geprüfte DE/EN-Textpaare für Launcher und Ingame-Oberflächen. Moduleinstellungen übersetzen Beschriftungen, Kategorien, Hilfetexte und sichtbare Auswahlwerte; gespeicherte IDs bleiben stabil. Suche berücksichtigt lokalisierte Modulnamen und Beschreibungen.
- Versionsauswahl und Hintergrund-Einstellungen passen bei kleinen Fenstern. Lange Button-, Slider- und Kategorie-Texte werden begrenzt. Item-Model-Menü erhält übersetzte Resttexte und ausreichend Abstand vor der Fußzeile.
- Numerische Einstellungen werden nicht mehr wegen ihres Wertebereichs pauschal als Ganzzahlen angezeigt. Slider speichern beim Abschließen der Änderung.
- Erz-Texturpaket deklariert die Ressourcenformate der aktiven Minecraft-Reihe (84–88).
- Aktive Produktmetadaten auf 2.2.0 vereinheitlicht; Windows-Build bettet ausschließlich die drei aktuellen, exakt zugeordneten 26.x-JARs ein. Historische Versionsangaben bleiben historisch.

## Durchgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Python-/QML-Tests in temporärem APPDATA | 116 Tests bestanden, 42 Untertests bestanden |
| Sprachkataloge | DE/EN-Schlüssel identisch; erzeugte Kataloge stimmen mit geprüften Sprachpaaren überein |
| Deklarative Moduloptionen | Beschriftungen und Hilfetexte auf Übersetzungsabdeckung geprüft; identische Fach- und Markennamen ausgenommen |
| Launcher-Layouts | 25 tatsächliche QML-Ansichten; DE/EN, 760×560 und 1280×820 |
| Mod-Builds | Separate 2.2.0-JARs für 26.1, 26.1.1 und 26.2 erfolgreich gebaut |
| Render-Hooks | Gegen tatsächliches Minecraft-Bytecode aller drei gepflegten Versionen geprüft |
| Minecraft 26.2 | Echte Testwelt und Render-Prüfung erfolgreich; Feature-Einstellungsseiten in DE/EN geöffnet, Item-Model-Einstellungen gespeichert und wieder geladen |
| Windows-Pakete | Launcher, portables ZIP und Installer gebaut; Launcher und Installer mit vorhandener Authenticode-Konfiguration signiert |
| Paketinhalt | Aktuelle Mod-JARs sowie QML-/JS-Dateien stimmen zwischen Quellen, Launcher-Verzeichnis und portablem ZIP überein; SHA-256-Prüfsummen gespeichert |
| Server-Detektor | Maven-Paket 2.2.0 erfolgreich gebaut |
| Website-Metadaten | Version 2.2.0; Produktionsbuild erfolgreich; Lint ohne Fehler, mit bestehenden ungenutzten Importen |

Die Minecraft-Prüfung umfasst unter anderem Wetter, Trefferpartikel, Bossleiste,
Scoreboard-Overlay, Soundhinweise, Wegpunkte, F1/F3, Drittperson sowie Item-Darstellung.
Sie ist ein Funktionstest, kein vollständiger Spieltest jedes einzelnen Einstellungswerts.

## Artefakte und Nachweise

- `backend/assets/EzClient-2.2.0+26.1.jar`
- `backend/assets/EzClient-2.2.0+26.1.1.jar`
- `backend/assets/EzClient-2.2.0+26.2.jar`
- `client_detector_plugin/target/ezclient-client-detector-2.2.0.jar`
- `dist/EzClient/EzClient.exe` (Dateiversion 2.2.0.0)
- `dist/EzClient-v2.2.0-Windows-Portable.zip`
- `dist/EzClient-Setup.exe` (Produktversion 2.2.0.0)
- Launcher-Bilder und QML-Meldungen: `build/release-qa/launcher/`
- Minecraft-Bilder: `client_mod/versions/26.2/build/run/clientGameTest/screenshots/`
- Paket-Prüfsummen nach Abschluss: `build/release-qa/artifacts.json`
- Abschließender Quellen-/Paketvergleich und SHA-256: `build/release-qa/package-checksums.json`
- Vollständiger abschließender Testlauf: `build/release-qa/python-tests.log`
- Abschließender Minecraft-Lauf: `build/release-qa/minecraft-final.log`
- Installer-Build: `build/release-qa/installer-build.log`

## Grenzen der Freigabe

Nicht jede historische Minecraft-Version wurde tatsächlich mit jedem verfügbaren
Vanilla-/Fabric-/Forge-Loader gestartet. Die Startlogik und repräsentative Metadatenfälle
sind geprüft; eine vollständige Online-Startmatrix ist damit nicht belegt.

Microsoft-Neuanmeldung, Käufe/Besitzprüfung, Multiplayer-Server und ein frischer
Windows-Installations-/Upgrade-Durchlauf wurden nicht end-to-end getestet.
Der QML-Software-Renderer prüft die Oberfläche, aber keine WebGL-Cape-/Skin-Vorschauen.
Diese Vorschauen melden in der isolierten Offscreen-Prüfung erwartungsgemäß fehlendes WebGL.

Die Übersetzungsprüfung deckt die deklarativen Moduloptionen und die ergänzten
Oberflächentexte ab. Sie beweist keine vollständige Übersetzung sämtlicher dynamischer
Fehlerausgaben, Drittanbieter-Inhalte oder aller spezialisierten Vorschautexte.
Eine Fehlerfreiheit aller Module und aller Versionskombinationen wird nicht zugesichert.

## Wiederholen

```text
python tools/run_release_tests.py
python tools/qa_launcher_render.py
python client_mod/build_mod.py
cd client_mod
gradlew.bat :26.2:runClientGameTest -PezclientTests -Ploomx.unobfuscated=true --offline
```

Die Tests verwenden temporäre Launcher-Daten und eine isolierte Minecraft-Testwelt.
Bei QML-Änderungen die erzeugten Screenshots ebenfalls ansehen; ein bestandener Test
allein erkennt keine schlecht lesbare Anordnung.
