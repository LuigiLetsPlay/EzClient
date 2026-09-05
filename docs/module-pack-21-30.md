# Module 21–30 und Performance-Korrekturen

Die Implementierung gilt für EzClient auf Minecraft **26.1, 26.1.1 und 26.2**. Die zehn neuen Module sind standardmäßig deaktiviert und über Rechts-Shift → Modul → Einstellungen erreichbar. Einstellungen werden in der bestehenden `config/ezclient.json` gespeichert. Weltmodule erscheinen nicht als leere Kacheln im HUD-Editor.

## Module

| Modul | Anbindung und Verhalten |
| --- | --- |
| Hitbox Visualizer | Spieler-/Mob-/Projektil-/Item-Filter, RGBA, 1–3 px, Chroma, Augenhöhe, Blickvektor, Füllung; optional nur bei F3+B. Tiefentest, maximal 128 nahe Entities. |
| Item Physics | Renderzustand speichert Bodenlage und Flugrotation; konfigurierbare Drehgeschwindigkeit. Bei Überschreiten der Item-Grenze wieder Vanilla; Zählung alle zehn Ticks. Keine Änderung an Item-Bewegung oder Serverzustand. |
| Time Weather Changer | Serverzeit, statisch, dynamisch, Tag/Nacht/Sonnenuntergang; clientseitige Umgebungsattribute statt Manipulation der Serveruhr. Server/Klar/Regen/Gewitter, Niederschlags- und Blitzfilter. |
| Particle Customizer | 0–5× Crit-/Sharpness-Partikel, zusätzliche Treffer-Emitter, Rauch-/Explosions-/eigene Potion-Filter, RGBA-Tint. Zusätzliche Partikel sind gegen rekursive Vervielfachung und übermäßige Erzeugung begrenzt. |
| Block Overlay | Tatsächliche Selektionsform des Blocks, Outline/Fill/beides, 1–5 px, RGBA und Chroma, Füllungsdeckkraft, Vanilla/versteckte Risse/farbiger Abbau-Overlay. |
| Boss Bar Customizer | Position, Skalierung, Namensfilter, Ausblenden, Vanilla-/Minimal-/Textstil, Prozent/HP/ohne Wert, Custom-/Rainbow-Farbe. Exakte HP benötigen eine bekannte, manuell eingetragene maximale HP-Zahl; das Bossbar-Protokoll übermittelt nur relativen Fortschritt. Ohne diese Angabe steht ausdrücklich „unavailable“. |
| Bedwars Hypixel Overlay | Liest Generator-Upgrades und Team-Betten aus dem bereits empfangenen Bedwars-/Skywars-Scoreboard; Inventarzähler für Eisen/Gold; Teamfarben; konfigurierbare Karten-Bauhöhe. Keine externen Statistikanfragen und keine geratenen Generator-Zeitpunkte. |
| Nameplate Levelhead | Eigener Name in F5, HP/Herzen, Hintergrund/Schatten, lokaler Clan-/Level-Präfix, Freundesfarbe über Namensliste, TTF. Namen bleiben tiefengeprüft. |
| Waypoints Minimap Light | GUI zum Erstellen/Bearbeiten/Löschen von Name, XYZ, ARGB-Farbe und Icon. Punkte sind pro Server/Welt und Dimension getrennt. Lichtmarkierung, schwebende Beschriftung, Richtung/Entfernung, temporärer letzter Todespunkt und Integration in die vorhandene Coordinates-Compass-Bar. Kein Entity-Radar oder Scan ungeladener Chunks. |
| Sound Subtitles Enhancer | Tatsächlich abgespielte, hörbare Untertitel mit Richtungspfeilen, Whitelist/Highlights, Reglern für Regen/Schritte/frei gewählten Sound. Ersetzt bei Aktivierung die Vanilla-Untertitelanzeige. |

HUD-Stil bietet X/Y, 0,5–2,0×, Minecraft oder eingebettetes Noto Sans (OFL), Schatten, ARGB-Farben, Hintergrunddeckkraft, tatsächliche Rundungen, Rand und getrenntes Rainbow für Text/Rand mit Geschwindigkeit/Sättigung. Auch die bisherigen HUD-Einstellungen haben einen Zugang zur erweiterten Stilseite.

F1 blendet die Zusatzanzeigen und Weltmarkierungen aus; Items behalten ihre physische Darstellung. F3 blendet die zusätzlichen HUD-Panels aus, während Weltinformationen und F3+B weiterhin gezielt nutzbar bleiben. Die Weltgeometrie wird pro Frame aus interpolierten Positionen in Minecrafts Gizmo-Sammlung extrahiert. 26.1/26.1.1 und 26.2 verwenden dafür ihre jeweiligen Extraktions-Hooks.

## Gefundene Lastquellen

- **Cape-Anfragen:** Wiederholte priorisierte Anfragen konnten zusätzliche Einträge für dieselbe UUID erzeugen. Die alte Prioritätswarteschlange hatte keine harte Gesamtgrenze; ihre Anfangskapazität war kein Limit. Neue Queue: höchstens 64 ausstehende/einlaufende Jobs, Deduplizierung bis zum Ende der Anfrage und korrektes Leeren beim Disconnect. Ein Worker, zusätzliche Pausen zwischen Spielerabfragen, maximal 2048 Erkennungsergebnisse.
- **Cape-Ressourcen:** Animationsblätter wurden bei Wechseln nicht zuverlässig freigegeben, alte Spieler weiter animiert und statische Texturen nicht freigegeben. Nun besitzt der TextureManager die GPU-Texturen; Freigaben und Animationswechsel erfolgen auf dem Client-Thread. Höchstens 33 residente Capes, acht gleichzeitig aktualisierte nahe Animationen. Entfernte Capes werden verworfen; verspätete Installationen für nicht mehr relevante Spieler werden geschlossen.
- **Downloads/Decoder:** Antworten werden bereits während des Empfangs begrenzt, Bildabmessungen vor der NativeImage-Dekodierung validiert. Externe GIFs verwenden höchstens 32 Frames; lokale Sheets erhalten Byte-/Pixel-/Geometriegrenzen. Dekodierte GIF-Zwischenbilder werden auch bei Fehlern geschlossen.
- **Skin-Polling:** Statt Dateilesen und JSON-Parsing alle 350 ms auf dem Spiel-Thread erfolgt die Abfrage alle zwei Sekunden auf einem einzelnen Worker. Unveränderte Skin-Wrappers werden wiederverwendet.
- **FPS-Vorgabe:** Vorhandene Profile wurden mit `maxFps:260` und `enableVsync:false` gefunden. Neue Profile bekommen 120 FPS als Vorgabe; bestehende Profilwerte werden vom Launcher nicht überschrieben. In bestehenden Profilen kann ein niedrigeres FPS-Limit zusätzlich CPU/GPU-Spielraum schaffen.
- **Erststart:** Config-Speichern während der noch unvollständigen Modul-Konstruktion verursachte eine abgefangene NullPointerException. Speichern beginnt jetzt erst nach vollständiger Registry-Initialisierung.
- **Windows-Dateisperren:** Kurzzeitig gesperrte Konfigurationsdateien werden bis zu dreimal verzögert erneut atomar gespeichert. Dabei schläft der Render-Thread nicht; neuere Einstellungen verdrängen veraltete Wiederholungen.

Das sind konkrete Codebefunde, keine abgeschlossene Messung des ursprünglichen Multiplayer-Problems. Beim ersten Systemcheck lief Minecraft nicht; rund 8 GB von 16 GB RAM waren frei. Ob hohe Server-Latenz durch lokale Überlastung, die Verbindung oder den Server entsteht, muss in einer repräsentativen Spielsession getrennt gemessen werden. Cape-Daten laufen über HTTP zu Cosmetic-Diensten, nicht durch den Minecraft-Spielserver.

## Prüfung und reproduzierbare Befehle

Abschlussprüfung am 5. September 2026: fünf Unittests bestanden, zusätzlicher HTTP-Grenztest bestanden, alle drei Release-JARs erfolgreich gebaut. Der echte 26.2-Client-Gametest ist auch nach den letzten Korrekturen erfolgreich durchgelaufen; sieben Screenshots dokumentieren Rendering und Einstellungsseiten. Für 26.1 und 26.1.1 wurden Build und Mixin-Zielsignaturen geprüft, kein eigener Spieltest durchgeführt.

```powershell
python -m unittest tests.test_cosmetic_work_queue tests.test_module_pack_targets tests.test_build_version_policy
python client_mod/build_mod.py
```

Der Queue-Test führt den echten Java-Queue-Code mit jeweils 100.000 wiederholten Anfragen aus und prüft Inflight-Deduplizierung, Begrenzung, Disconnect und Prioritätsreihenfolge. Er prüft außerdem den tatsächlichen HTTP-Subscriber: Antworten genau am Limit werden angenommen, über mehrere Datenblöcke überschrittene Limits brechen den Empfang ab. Die Zielprüfung vergleicht Mixin-Selektoren mit dem tatsächlichen Bytecode aller drei gepflegten Minecraft-Versionen.

Zusätzlich ist ein isolierter Fabric-Client-Gametest vorhanden:

```powershell
cd client_mod
.\gradlew.bat '-Ploomx.unobfuscated=true' '-PezclientTests' ':26.2:runClientGameTest'
```

Er erstellt eine temporäre Einzelspielerwelt, aktiviert die zehn Module, prüft Konfigurations-Roundtrip/ungültige Werte, generiert Entities und Bossbar, prüft Scoreboard, Partikelfilter, Wetter, Sound-Highlights und Todespunkt und erstellt Screenshots von Welt, Nameplate, F1/F3 sowie Einstellungen. Der Testmod wird nur bei explizitem `-PezclientTests` eingebunden und kommt nicht in Release-JARs. Test-AppData ist vom Benutzerprofil getrennt.

Screenshots und Logs liegen unter `client_mod/versions/26.2/build/run/clientGameTest/`. Ein Test mit Drittanbieter-Rendering-Mods und ein Vorher/Nachher-Ping-Benchmark auf einem öffentlichen Server sind damit nicht ersetzt.

Die Build-Ausgabe enthält **je eine eigene JAR** in `backend/assets/EzClient-2.0.0+26.1.jar`, `...+26.1.1.jar` und `...+26.2.jar`. Sie sind nicht untereinander austauschbar.
