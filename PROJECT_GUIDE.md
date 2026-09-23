# EzClient – Projektleitfaden

Diese Datei ist die zentrale, tool-neutrale Dokumentation für die Weiterentwicklung von EzClient. Sie ersetzt frühere KI-spezifische Anweisungsdateien. Vor Änderungen sollten zusätzlich der aktuelle Quellcode, `git status` und die Tests geprüft werden, weil sich Implementierungsdetails weiterentwickeln können.

## 1. Projektziel und aktueller Stand

EzClient besteht aus:

- einem Windows-Launcher auf Basis von Python, PySide6 und QML;
- einem direkten Minecraft-Startsystem mit Microsoft-Anmeldung;
- Profil-, Mod-, Ressourcenpaket-, Cape- und Java-Verwaltung;
- versionsabhängigen Fabric-/Legacy-Fabric-Mod-JARs;
- einem PyInstaller-Build für `EzClient.exe` und einem nativen Inno-Setup-Build für `EzClient-Setup.exe`.

Aktuelle Produktversion: `2.2.3`.

Es gibt keine Lite-Version mehr. Neue Builds, UI-Texte und Releases dürfen keine `EzClient-Lite`-Artefakte erzeugen oder voraussetzen.

## 2. Wichtige Verzeichnisse

| Pfad | Bedeutung |
|---|---|
| `main.py` | Einstiegspunkt des Launchers |
| `backend/controllers/` | PySide6-Controller und QML-Schnittstellen |
| `backend/models/` | Datenklassen und Qt-Modelle |
| `backend/services/` | Profile, Downloads, Minecraft-Start, Java, Mods und Updates |
| `backend/assets/` | Auszuliefernde versionsabhängige EzClient-JARs |
| `ui/` | QML-Oberfläche, Icons, Banner, Fonts und Komponenten |
| `client_mod/` | Stonecutter-/Gradle-Projekt für die Minecraft-Mods |
| `client_mod/versions/` | Abhängigkeiten und Metadaten je Build-Ziel |
| `client_mod/src/main/` | Vollversion für das aktuelle Minecraft-Ziel |
| `client_mod/src/main/java/app/ezclient/v1_8/` | Legacy-Fabric-Dashboard, HUD, Module und Capes |
| `client_mod/src/main/java/app/ezclient/v1_16_v1_20/` | Fabric-Adapter für 1.16.5 bis 1.21.x |
| `client_mod/src/v26_1/` | abweichende Ressourcen/Mixins für 26.1.x |
| `tests/` | Python-Regressionstests |
| `installer/` | Inno Setup Skript (`EzClient.iss`), Sprachdateien und Setup-Zertifikatsskripte |
| `tools/` | Signier- und Entwicklertools (`sign_tool.py`, Zertifikate) |
| `dist/` | erzeugte Windows-Binaries |

Benutzerdaten liegen unter Windows standardmäßig in `%APPDATA%\.ezclient`:

- `state.json`: globale Einstellungen und Profilregistrierung;
- `profiles/`: getrennte Spielprofile;
- `cache/`: Launcher-Caches;
- `assets/`: optional nachgeladene EzClient-JARs.

Die normalen Minecraft-Dateien bleiben unter `%APPDATA%\.minecraft`.

## 3. Unterstützte Minecraft- und Java-Versionen

Der vollständige Launcher-Katalog steht in `backend/services/minecraft_versions.py`. Er umfasst Minecraft 1.8 bis 26.2.

Java-Zuordnung:

| Minecraft | Java |
|---|---:|
| 1.8 bis 1.16.x | 8 |
| 1.17.x | 16 |
| 1.18 bis 1.20.4 | 17 |
| 1.20.5 bis 1.21.x | 21 |
| 26.x | 25 |

`backend/services/java_runtime.py` verwaltet Java 8, 16, 17, 21 und 25. Verwaltete Installationen liegen in `.minecraft/runtime/ezclient-jdk-<Version>`.

Fabric wird ab Minecraft 1.3 angeboten. Für 1.3 bis 1.13.2 verwendet der Erststart `meta.legacyfabric.net`; neuere Versionen verwenden die normale Fabric-Meta-API.

## 4. EzClient-Mod-Kompatibilität

Nur die Minecraft-Reihe 26.x wird aktiv als EzClient-Mod gepflegt.
26.1, 26.1.1 und 26.2 erhalten jeweils eine exakt passende JAR mit Java 25:

```text
EzClient-2.2.3+26.1.jar
EzClient-2.2.3+26.1.1.jar
EzClient-2.2.3+26.2.jar
```

Eine JAR darf niemals als Ersatz für eine andere Minecraft-Version umbenannt werden.
Ältere EzClient-Mod-Reihen sind in `Old/` archiviert und werden nicht ausgeliefert.
Der Launcher unterstützt weiterhin Vanilla, Fabric und Forge für die älteren
Minecraft-Versionen; die tatsächliche Loader-Verfügbarkeit muss vor dem Start geprüft werden.
Das goldene EzClient-Abzeichen erscheint nur für vorhandene exakte 26.x-Assets.

## 5. Produktversion ändern

Die Produktversion niemals einzeln per Suchen/Ersetzen in nur einer Datei ändern. Dafür existiert im Repository-Root:

```powershell
python update_version.py <neue_version>
```

Beispiel:

```powershell
python update_version.py 1.9.1
```

Das Skript erkennt die aktuelle Version über `APP_VERSION` und aktualisiert die aktiven Versionsangaben in Mod, Backend, Wrapper, Installer, Build-Skripten und QML.

Alternativ kann die alte Version ausdrücklich angegeben werden:

```powershell
python update_version.py 2.0.0 2.0.1
```

Danach immer kontrollieren:

```powershell
git diff
rg "2\.0\.0" backend client_mod installer minecraft_wrapper ui *.py
```

Treffer in historischen Release Notes oder alten Changelogs dürfen bestehen bleiben. Aktive Produktdateien müssen überall die neue Version verwenden.

Falls eine neue aktive Datei eine Versionsnummer enthält, muss sie in `RELATIVE_FILES` in `update_version.py` aufgenommen werden. Insbesondere muss jede neue Mod-Entry-Point-Klasse berücksichtigt werden.

## 6. EzClient-JARs bauen

Nach jeder Änderung an der Minecraft-Mod oder nach einer Produktversionsänderung aus dem Repository-Root ausführen:

```powershell
python client_mod/build_mod.py
```

Das Skript baut ausschließlich 26.1, 26.1.1 und 26.2, prüft die
Minecraft-Abhängigkeit in jeder fertigen JAR und kopiert die exakten Artefakte
nach `backend/assets/`. Legacy-Builds und Versions-Aliase gehören nicht zum Release.

### Gemeinsamer Mod-Kern

Neue Minecraft-unabhängige Logik gehört nach `client_mod/src/main/java/app/ezclient/shared/`. Die Versionspakete binden sie nur an Minecraft an und rendern das Ergebnis. Sie sollen keine zweite Zustandsmaschine mit abweichenden Standardwerten anlegen.

Derzeit zentralisiert:

- `ZoomState`: Standard-Zoom, Scroll-Grenzen, Zurücksetzen bei einem neuen Tastendruck und FOV-Faktor;
- `ClickRateTracker`: gemeinsames, speichereffizientes CPS-Zeitfenster.

Die Adapter bleiben für Mappings, Mixins, Eingabe-, Screen- und Render-APIs zuständig. Neue `26.x`-Funktionen dürfen nicht im gemeinsamen Kern landen, wenn sie dadurch einen späteren Rebuild der eingefrorenen Ziele verändern würden. Das gemeinsame Verhalten wird in `tests/test_shared_mod_state.py` regressionsgetestet.

### Neue Minecraft-Version ergänzen

Bei einer neuen Minecraft-Version sind mindestens folgende Stellen zu prüfen:

1. Version in `backend/services/minecraft_versions.py` ergänzen.
2. Richtige Java-Grenze in `required_java()` prüfen.
3. Stonecutter-Version in `client_mod/settings.gradle` ergänzen.
4. `client_mod/versions/<MC-Version>/gradle.properties` mit Minecraft-, Loader-, Mapping- und Abhängigkeitswerten erstellen.
5. Exaktes 26.x-Ziel in `client_mod/build_mod.py` ergänzen.
6. Modrinth-/CurseForge-Versionsfilter aktualisieren.
7. Versionsbanner und UI-Katalog prüfen.
8. JAR bauen und `fabric.mod.json` innerhalb der JAR kontrollieren.
9. Erststart, Fabric-Auflösung, Java-Auswahl und tatsächlichen Minecraft-Start testen.

Keine leere oder umbenannte JAR als „Kompatibilität“ ausliefern. Das Asset muss kompilieren und seine Metadaten müssen exakt zur Zielversion und Java-Version passen.

## 7. Launcher und Installer bauen

Python-Abhängigkeiten:

```powershell
python -m pip install -r requirements.txt
```

Launcher aus dem Quellcode starten:

```powershell
python main.py
```

Nur den Windows-Launcher bauen:

```powershell
python build_exe.py
```

`build_exe.py` baut zuerst automatisch alle Mod-JARs neu und bindet anschließend `ui/` sowie `backend/assets/` in `dist/EzClient/EzClient.exe` ein.

Installer bauen, nachdem `dist/EzClient/EzClient.exe` existiert:

```powershell
python build_installer.py
```

Kompletten Windows-Release bauen:

```powershell
python build_release.py
```

Erwartete Ergebnisse:

```text
dist/EzClient/EzClient.exe
dist/EzClient-Setup.exe
```

Der Upgrade-Button des Launchers ersetzt nicht eigenständig den Quellcode. Ein öffentliches Update benötigt ein korrekt gebautes und veröffentlichtes Launcher-/Installer-Artefakt. Die versionsabhängigen JARs werden beim EXE-Build aus `backend/assets` eingebettet und beim internen EzClient-Mod-Update in passende Profile kopiert.

## 8. Empfohlener Update- und Release-Ablauf

1. Arbeitsbaum mit `git status --short` prüfen und fremde Änderungen nicht überschreiben.
2. Funktion implementieren und zwischen gemeinsamem, Legacy-, Compatibility- und Vollversions-Code unterscheiden.
3. Produktversion mit `update_version.py` erhöhen.
4. Die aktiv gepflegten `26.x`-JARs mit `client_mod/build_mod.py` neu bauen; Frozen-JARs nicht anfassen.
5. Python-Tests und Syntaxprüfung ausführen.
6. Launcher aus dem Quellcode starten und die betroffenen QML-Seiten prüfen.
7. Repräsentative Minecraft-Ziele testen: mindestens Legacy, 1.21.x und 26.x.
8. `build_release.py` ausführen.
9. EXE, Installer und eingebettete Assets prüfen.
10. Professionelle Release Notes erstellen und erst dann veröffentlichen.

Versionsnummer erst erhöhen, wenn die enthaltenen Änderungen für dieses Release feststehen. Keine alten JARs unter einem neuen Namen weiterverwenden, außer bei ausdrücklich geprüften versionsunabhängigen Compatibility-Aliasen.

## 9. Tests und Validierung

Alle vorhandenen Python-Tests ohne zusätzliche Test-Abhängigkeit:

```powershell
python -m unittest discover -s tests -v
```

Syntaxprüfung:

```powershell
python -m compileall -q backend client_mod/build_mod.py main.py
```

Gradle-Build aller Mod-Ziele:

```powershell
python client_mod/build_mod.py
```

Bei JAR-Problemen `fabric.mod.json` direkt aus der JAR prüfen. Erwartet werden die aktuelle EzClient-Version, die richtige Minecraft-Abhängigkeit und die richtige Java-Abhängigkeit.

Bei Startproblemen sind besonders zu prüfen:

- `%APPDATA%\.ezclient\state.json` und das ausgewählte Profil;
- `%APPDATA%\.minecraft\versions`;
- `%APPDATA%\.minecraft\libraries`;
- `%APPDATA%\.minecraft\assets`;
- Profilordner `mods`, `resourcepacks`, `logs` und `natives`;
- `logs/` im Repository bzw. Launcher-Logausgabe;
- Fabric-/Legacy-Fabric-Metadaten und HTTP-Fehler inklusive vollständiger URL;
- tatsächlich verwendete Java-Hauptversion.

## 10. Profile und Inhalte

Profiltypen:

- `ezclient`: verwendet eine exakt zur Minecraft-Version passende EzClient-JAR;
- `raw`/Vanilla: keine EzClient-Ingame-Funktionen;
- Fabric ohne EzClient: Fabric-Mods möglich, aber keine EzClient-spezifischen Ingame-Funktionen.

Vanilla-Profile dürfen den Modrinth-/CurseForge-Bereich für Ressourcenpakete sehen, aber keine Mods oder EzClient-Capes als nutzbare Ingame-Funktion behandeln. Mod-, Ressourcenpaket- und Cape-Funktionen müssen deshalb immer nach Profiltyp getrennt werden.

Profilnamen werden eindeutig gehalten. Bei Duplikaten werden sichtbare Namen wie `Name (2)` und `Name (3)` verwendet. Neue Profile werden nach ihrer Erstellung als aktives Profil gespeichert.

Launcher-eigene Kern-Mods und vom Benutzer installierte JARs müssen getrennt verwaltet werden. Synchronisierung darf unbekannte Benutzer-JARs niemals löschen.

Die aktuelle verwaltete Mod-Auswahl wird in `backend/services/store.py` und `profile_migration.py` definiert. Sie umfasst EzClient Core, Sodium, Lithium, Iris und Entity Culling; das Performance-Profil lässt EzClient Core weg. Für ältere Minecraft-Versionen werden nur tatsächlich kompatible Veröffentlichungen installiert. Es darf keinen Fallback auf eine JAR für eine andere Minecraft-Version geben. Benutzerverwaltete Mods und festgelegte Modpack-Versionen bleiben davon getrennt.

## 11. Direct Launch, Assets und Legacy Fabric

`backend/services/game_bootstrap.py` lädt Minecraft-Bibliotheken und Assets parallel. Mehrere logische Asset-Namen können denselben Hash besitzen; deshalb werden physische Objekte dedupliziert. Downloads verwenden eindeutige temporäre Dateien und eine Sperre pro Zielpfad.

Legacy Fabric kann Native-Abhängigkeiten über ein `natives`-Mapping statt über eine normale Maven-JAR angeben. Für Windows muss beispielsweise `lwjgl-platform-...-natives-windows.jar` geladen und entpackt werden. Native-only-JARs gehören nicht auf den Java-Classpath.

Bei Änderungen am Downloader unbedingt testen:

- parallele Downloads desselben Zielpfads;
- übrig gebliebene `.part`-Dateien;
- Prüfsumme und Dateigröße;
- Legacy-Fabric-Natives;
- vollständige Asset-Indizes;
- verständliche Fortschrittsanzeige mit aktuellem und gesamtem Datenvolumen.

## 12. UI-Grundsätze

- dunkles, minimalistisches Launcher-Design mit Sidebar und kompakter Topbar;
- einheitliche Icons statt gemischter Stilrichtungen;
- Buttons und Werte innerhalb einer Seite sauber rechts ausrichten;
- Texte dürfen Buttons und Karten nicht verlassen;
- aktives Profil nur eindeutig grün markieren, ohne Layout-Verschiebung;
- Vanilla-, Fabric- und EzClient-Auswahl über korrekte Logos darstellen;
- Sterne nur bei tatsächlich vorhandener EzClient-JAR anzeigen;
- jeder Stern benötigt einen funktionierenden Tooltip „EzClient Compatible“;
- Versionsbanner müssen die Karten vollständig füllen und dürfen keine weißen Ränder zeigen;
- keine generierten Platzhalterbanner verwenden, wenn offizielles oder geeignetes Community-Material mit geklärter Nutzung verfügbar ist;
- Vanilla-Seiten dürfen keine funktionslosen EzClient-Inhalte anzeigen;
- QML-Änderungen bei verschiedenen Fenstergrößen und Skalierungsfaktoren prüfen.

Das Ingame-EzClient-GUI soll kompakt, zentriert und kleiner als das Minecraft-Fenster bleiben.

## 13. Release Notes

Release Notes müssen professionell und lesbar sein:

- echtes Markdown mit Überschriften und Listen verwenden;
- keine sichtbaren Escape-Sequenzen wie `\n` oder `\r` veröffentlichen;
- keine rohen JSON- oder Konsolen-Dumps in den Release-Text kopieren;
- Änderungen, Fehlerbehebungen, Kompatibilität und bekannte Einschränkungen klar trennen;
- nur Funktionen behaupten, die gebaut und getestet wurden.

## 14. Arbeitsregeln für zukünftige Änderungen

- Vor jeder Änderung erst relevante Dateien und bestehenden Git-Diff lesen.
- Unabhängige Benutzeränderungen niemals verwerfen.
- Keine destruktiven Git-Befehle wie `git reset --hard` verwenden.
- Quelltext gezielt ändern; generierte Build-Ordner nicht als primäre Quelle bearbeiten.
- Neue Downloads nur von offiziellen oder nachvollziehbaren Quellen beziehen.
- Versionen, Loader, Java-Anforderungen und externe APIs vor dem Festschreiben aktuell verifizieren.
- Bei Fehlern die Ursache beheben, nicht nur Fehlermeldungen ausblenden.
- Nach Änderungen Tests proportional zum Risiko ausführen und Ergebnisse dokumentieren.
- Keine Lite-Mod oder generische `EzClient.jar` wieder einführen; verbindlich sind versionierte Voll-/Kompatibilitätsassets.

## 15. Kurzcheckliste für die nächste Weiterentwicklung

```text
[ ] PROJECT_GUIDE.md und relevante Quellen gelesen
[ ] git status / vorhandene Änderungen geprüft
[ ] Zielversionen und Java-Matrix berücksichtigt
[ ] Gemeinsamer oder versionsspezifischer Mod-Code korrekt gewählt
[ ] update_version.py bei Releasewechsel ausgeführt
[ ] build_mod.py für alle aktiven 26.x-Ziele erfolgreich; exakte 26.x-Assets geprüft
[ ] alle Python-Tests erfolgreich
[ ] Launcher-UI manuell geprüft
[ ] Legacy-, 1.21.x- und 26.x-Start repräsentativ geprüft
[ ] EzClient.exe und Installer neu gebaut
[ ] Assets und JAR-Metadaten kontrolliert
[ ] saubere Release Notes erstellt
```

## Release-Prüfung und Übersetzungen

`python tools/run_release_tests.py` führt die Python-/QML-Tests mit temporärem
APPDATA aus. `python tools/qa_launcher_render.py` erzeugt tatsächliche QML-Ansichten
in DE/EN bei 760×560 und 1280×820 unter `build/release-qa/launcher`.
Der Software-Renderer prüft Layouts; WebGL-Skin-/Cape-Vorschauen benötigen zusätzlich
eine Prüfung mit GPU. Ergebnisse und Grenzen stehen in `docs/release-qa-2.2.2.md`.

Geprüfte Sprachpaare stehen in `localization/*.tsv`. Nach Änderungen
`python tools/update_display_translations.py` ausführen. Die erzeugten Kataloge
sind Teil von Launcher und Mod. Konfigurations-IDs und gespeicherte Auswahlwerte
bleiben unabhängig von der Sprache stabil.

Der optionale Minecraft-Integrationstest läuft über
`:26.2:runClientGameTest -PezclientTests -Ploomx.unobfuscated=true`.
Er öffnet eine isolierte Testwelt und Moduleinstellungen in beiden Sprachen.
Ein erfolgreicher Build ersetzt keine vollständigen Spieltests aller Loader-Versionen.
