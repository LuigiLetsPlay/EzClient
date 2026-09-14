# EzClient 2.2.0

Willkommen zu **EzClient 2.2.0**! 🚀  
EzClient 2.2.0 setzt neue Maßstäbe in Sachen Stabilität, Anpassbarkeit und Internationalisierung. Dieses Release bringt ein brandneues Ingame-Account-System zum nahtlosen Wechseln des Minecraft-Accounts während des Spiels, ein umfangreiches Item-Model-Customizer-Modul mit 1.7-Animationsstilen, eine massive Lokalisierungsoffensive mit über 800 geprüften DE/EN-Übersetzungen, robuste Download- und Loader-Pipelines für Fabric & Forge sowie ein runderneuertes Cape-Studio mit pixelgenauer UV-Projektion.

---

## 🌟 Highlights & Wichtigste Neuerungen

### 👤 Ingame-Account-System (`EzAccountScreen` & `JoinMultiplayerScreenMixin`)
- **Account-Wechsel ohne Neustart:** Wechsel direkt im laufenden Spiel den Minecraft-Account – kein lästiges Schließen und Neustarten des Spiels mehr nötig.
- **Multiplayer-Schnellzugriff:** Eigener Account-Manager-Button direkt im Mehrspieler-Bildschirm (`JoinMultiplayerScreen`), um vor dem Server-Join blitzschnell das Profil oder den Account auszuwählen.
- **Sichere Sitzungsverwaltung:** Nahtlose Authentifizierung über Microsoft/Mojang-Tokens mit Session-Validierung und Statusanzeige.

### 🗡️ Item Model Customizer & 1.7-Style Animationen (`ItemModelModule` & `ItemModelScreen`)
- **Freie Item-Positionierung:** Vollständige Kontrolle über die 3D-Positionierung von gehaltenen Gegenständen in Haupt- und Nebenhand (X-, Y- und Z-Achsenversatz, Drehung und Skalierung).
- **Klassische 1.7-Animationen:** Beliebte 1.7-Style Schwertblock- und Schlaganimationen ("Blockhit") mit stufenlos konfigurierbarer Geschwindigkeit und Schwenkwinkeln.
- **Aktionsspezifische Animationen:** Einstellbare Haltungen und Animationen für Essen, Trinken, Bogen spannen und Item-Wechsel im First-Person-Modus.
- **Echtzeit-Vorschau:** Alle Änderungen werden sofort live im Spiel auf das gehaltene Item angewendet.

### 🌐 Gewaltige Lokalisierung & Internationalisierung (829+ DE/EN Sprachpaare)
- **Zweisprachig durch und durch:** 829 zusätzliche, sorgfältig geprüfte deutsche und englische Textpaare decken den Launcher und sämtliche Ingame-Oberflächen ab.
- **Modulsuche auf Deutsch & Englisch:** Die Schnellsuche im Ingame-Hub findet Module jetzt auch anhand ihrer deutschen Namen und Funktionsbeschreibungen.
- **Übersetzte Moduleinstellungen:** Sämtliche Schieberegler, Umschalter, Dropdowns, Kategorien und Hilfe-Tooltips sind vollständig lokalisiert.
- **Stabile Einstellungs-IDs:** Interne Modul- und Einstellungs-Schlüssel bleiben sprachenunabhängig stabil, sodass beim Wechseln der Sprache keine Konfigurationswerte verloren gehen.

### 🚀 Robuste Launcher-Engine & Loader-Architektur
- **Strikte Fabric-Metadaten-Auflösung:** Fabric-Loader-Versionen werden exakt auf die geforderte Minecraft-Version geprüft. Verhindert zuverlässig, dass das Spiel bei fehlerhafter Loader-Zuordnung unbemerkt im Vanilla-Modus startet.
- **Forge-Profile ohne Vorab-ID:** Forge-Profile lassen sich nun auch vorbereiten, wenn noch keine feste Loader-Kennung gespeichert war. Alte und neue Forge-Metadatenformate werden automatisch normalisiert.
- **Sauberes natives DLL-Handling:** Bibliotheken mit separaten nativen Binärdateien werden vollständig heruntergeladen und im JVM-Startpfad isoliert; keine veralteten oder kollidierenden DLLs mehr nach Bibliotheks-Updates.
- **Prüfsummen- und Größen-Validierung:** Alle Downloads und Minecraft-Assets werden strikt gegen bekannte SHA-1- und SHA-256-Prüfsummen sowie die Soll-Dateigröße geprüft. Fehlgeschlagene Downloads melden klare Fehlerursachen statt Spielabstürze zu verursachen.
- **Modpack-Pins & Performance-Profile:** Modpack-Profile behalten ihren spezifischen Profiltyp dauerhaft bei; fest verankerte Mod-Pins werden zuverlässig validiert und wiederhergestellt.

### 🦸 Cape-Studio & Medien-Overhaul
- **Pixelgenaue 6-Seiten-UV-Projektion:** Vorderseite, Rückseite, Kanten, Ober-/Unterseite und Elytren werden akkurat auf das Standard-Minecraft-Cape-Format (64x32) gemappt.
- **Kein Farbbluten (Bleed-Through-Fix):** Vorder- und Rückseite bleiben sauber getrennt, ohne unschöne Pixelüberlagerungen auf den Kanten.
- **Statische Bilder & GIF-Trimmer:** Volle Unterstützung für Base64 Data-URLs, PNG/JPG-Dateien und animierte Cape-Motive mit Vorschau.
- **Sofortiges lokales Caching:** Capes sind lokal unverzüglich aktiv und synchronisieren im Hintergrund zuverlässig über HMAC-Token und Server-Sent-Events (SSE) mit der EzClient-Community.

### 🎨 Ingame-UI & Modul-Feinschliff
- **EzSlider-Präzision:** Schieberegler speichern den Wert exakt beim Loslassen; Dezimalwerte werden nicht mehr fälschlicherweise als Ganzzahlen abgeschnitten.
- **EzHotkeyButton:** Einheitliche, reaktionsschnelle Tastenbelegungs-Buttons in allen Modul-Einstellungsmenüs.
- **Optimierte ScrollingSettingsScreen-Höhen:** Saubere Scissor-Masken, kein Überlappen mehr mit Fußzeilen oder Fensterrändern.
- **Wavey Capes 3D-Container:** Optimiertes Cape-Rendering verhindert Streifenbildung und transparente Randartefakte bei dynamischer Cape-Physik.
- **Glowing Ores:** Texturpaket unterstützt die Ressourcenpaket-Formate 84–88 für alle aktiven 26.x-Minecraft-Versionen.

### 💻 Launcher-Oberfläche & Benutzererlebnis
- **Responsive Layouts:** Menüs, Versionsauswahl und Hintergrundeinstellungen passen sich flexibel an kleine Fenstergrößen bis hinunter zu 760×560 Pixeln an.
- **Textkürzung & Typografie:** Lange Bezeichnungen auf Buttons, Schiebereglern und Tabs werden elegant per Ellipsis gekürzt, anstatt das UI-Layout zu zerstören.
- **Detaillierte Fehlerberichte:** Start- und Mod-Fehler zeigen bei Bedarf vollständige Tracebacks zur schnellen Fehlerdiagnose.
- **Sichere Profilaktionen:** Bestätigungsdialoge beim Löschen und klare Benachrichtigungen beim Profilwechsel.

---

## 🧪 Durchgeführte Qualitätssicherung (Release-QA)

Alle automatisierten und manuellen QA-Schritte wurden erfolgreich abgeschlossen:

- ✅ **116 Launcher- & Python-Tests** (inkl. 42 Subtests) ohne Fehler bestanden (`tools/run_release_tests.py`).
- ✅ **Sprachkatalog-Konsistenz:** 100% Übereinstimmung der DE/EN-Schlüssel und deklarativen Modultexte.
- ✅ **Layout-Tests:** 25 QML-Ansichten unter DE und EN in unterschiedlichen Auflösungen validiert.
- ✅ **Separate 26.x Mod-JARs:** Exakt gebaute und verifizierte JAR-Dateien für Minecraft 26.1, 26.1.1 und 26.2.
- ✅ **Paket-Integrität:** Vollständiger Abgleich aller Mod-JARs, QML- und JS-Dateien zwischen Quellcode, Launcher-Runtime und Portable-ZIP (`tools/verify_release_package.py`).

---

## 📦 Downloads & Installation

- **`EzClient-Setup.exe`**: Nativer Windows-Installer (Inno Setup) für die bequeme 1-Klick-Installation.
- **`EzClient-v2.2.0-Windows-Portable.zip`**: Portables ZIP-Archiv – einfach entpacken und sofort `EzClient.exe` starten.
- **`EzClient-2.2.0+26.2.jar`**: Fabric Client-Mod für Minecraft 26.2
- **`EzClient-2.2.0+26.1.1.jar`**: Fabric Client-Mod für Minecraft 26.1.1
- **`EzClient-2.2.0+26.1.jar`**: Fabric Client-Mod für Minecraft 26.1

