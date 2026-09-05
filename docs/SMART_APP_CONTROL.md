# Windows 11 Smart App Control & Defender Leitfaden

Dieser Leitfaden erklärt, warum Windows 11 Smart App Control (SAC) oder SmartScreen Programme blockieren kann und wie EzClient damit umgeht.

---

## 1. Was ist Smart App Control (SAC)?

**Smart App Control** ist ein Sicherheitsfeature in Windows 11 (ab Version 22H2). Es prüft jede ausgeführte Anwendung in Echtzeit gegen Microsofts Cloud-Sicherheitsdienst:

1. **Gültige digitale Signatur:** Besitzt die Datei ein gültiges Authenticode-Zertifikat einer im Microsoft Trusted Root Program anerkannten Zertifizierungsstelle?
2. **Cloud-Reputation:** Kennt Microsoft die Datei aus Milliarden weltweiten Telemetriedaten als sicher?

Wenn eine Anwendung **weder** über ein kommerzielles, von Microsoft vorab anerkanntes Zertifikat (wie DigiCert EV für ~500€/Jahr) verfügt, **noch** Millionen weltweite Downloads verzeichnet, blockiert Windows 11 Smart App Control die Datei automatisch mit der Meldung:

> *„Smart App Control hat eine App blockiert, die möglicherweise unsicher ist. Wir haben ... blockiert, weil wir den Herausgeber nicht bestätigen konnten, um sicherzustellen, dass die Ausführung sicher ist.“*

---

## 2. Wie EzClient das Problem löst

### A. Professioneller Inno Setup Installer
Anstelle des alten, langsamen PyInstaller-Setup-Wrappers verwendet EzClient jetzt einen nativen **Inno Setup** Windows-Installer (`EzClient-Setup.exe`).
- Echter Windows-Installer (C++) mit sauberer PE-Struktur.
- Komprimiert mit moderner LZMA2-Kompression (spart über 160 MB Dateigröße).
- Standardkonforme Deinstallation (`unins000.exe`) in *Windows Apps & Features*.
- Schließt automatisch laufende Instanzen vor Aktualisierungen (`CloseApplications=force`).

### B. Digitale Authenticode-Signatur (SHA256 & RFC 3161 Timestamp)
Alle EzClient-Binaries (`EzClient.exe` und `EzClient-Setup.exe`) werden beim Build automatisch mit dem EzClient-Entwicklerzertifikat digital signiert und mit einem offiziellen DigiCert-Zeitstempel versehen.
- Windows zeigt in den Datei-Eigenschaften nun einen verifizierten Herausgeber (`Luigi / EzClient`) und gültige PE-Versionsmetadaten an.

---

## 3. Endanwender-Lösungen bei Smart App Control

### Option 1: Zertifikat mit 1 Klick vertrauen (Empfohlen für Tester & Entwickler)
Im Ordner `installer/` liegt das Skript `TrustCertificate.bat`.
1. Mache einen Rechtsklick auf `TrustCertificate.bat` -> **Ausführen**.
2. Bestätige die Sicherheitsabfrage von Windows mit **Ja**.
3. Windows stuft das Zertifikat von `Luigi / EzClient` als vertrauenswürdige Stammzertifizierungsstelle ein, sodass der Launcher ohne Warnungen ausgeführt werden kann.

### Option 2: „Zulassen“ bei heruntergeladenen Dateien (Mark of the Web entfernen)
Wenn Windows den Installer nach dem Download aus dem Browser blockiert:
1. Mache einen **Rechtsklick** auf `EzClient-Setup.exe` -> **Eigenschaften**.
2. Unten unter *Sicherheit* das Häkchen bei **„Zulassen“** (Unblock) setzen und auf **Übernehmen** klicken.
3. Alternativ per PowerShell:
   ```powershell
   Unblock-File -Path "dist\EzClient-Setup.exe"
   ```

### Option 3: Smart App Control Status prüfen
Unter Windows 11 kann der Modus von Smart App Control in den Windows-Einstellungen eingesehen werden:
- **Einstellungen** -> **Datenschutz & Sicherheit** -> **Windows-Sicherheit** -> **App- & Browsersteuerung** -> **Smart App Control-Einstellungen**.
- Befindet sich Windows im Modus *„Ein“*, blockiert Microsoft prinzipiell jede Open-Source-Software ohne teures kommerzielles EV-Zertifikat.
- Entwickler und Power-User können SAC auf *„Auswertung“* oder *„Aus“* stellen, um eigene Builds ausführen zu können.
