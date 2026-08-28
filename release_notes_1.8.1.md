# EzClient 1.8.1

EzClient 1.8.1 ist ein Wartungs- und Stabilitäts-Update für Launcher, Mod-Downloader, Profil-Migration und den Community-Server.

## 🚀 Highlights & Fehlerbehebungen

### 🧹 Bereinigung von Legacy-Abhängigkeiten
- **Automatische Mod-Filterung**: Veraltete Bibliotheken und Companion-Config-Mods (Yet Another Config Lib / YACL, Cloth Config, Cull Less Leaves etc.) werden beim Erstellen oder Synchronisieren von EzClient-Profilen nicht mehr heruntergeladen.
- **Bereinigte Altlasten**: Bestehende Profile entfernen beim Start automatisch verwaiste JAR-Dateien dieser Bibliotheken aus dem `mods/`-Ordner.
- **Aktualisierte Migrationsstufe**: Die Profil-Migration wurde auf Version `1801` angehoben.

### 🌐 Community Server & Systemd Service
- **Fix für Status 226/NAMESPACE**: Die Systemd-Service-Konfiguration (`ezclient-community.service` und `setup.sh`) wurde für Virtualisierungen, LXC-Container und VPS optimiert, indem fehleranfällige Mount-Namespace-Restriktionen behoben wurden.
- **Robustes Setup**: Umgebungsvariablen-Dateien werden nun fehlertolerant eingebunden und Datenverzeichnisse bei Bedarf automatisch initialisiert.

### 📦 Client & Core Updates
- Version auf **1.8.1** für alle Komponenten angehoben (Client Mod, Lite Mod, Launcher, Installer, Backend und UI).

## 📦 Downloads & Installation
- **`EzClient-Setup.exe`**: Komfortabler Windows-Installer (automatische Verknüpfungen & Updates)
- **`EzClient.exe`**: Standalone Executable (ohne Installation sofort startbar)
- **`EzClient-1.8.1.jar`**: Core Client Mod JAR
- **`EzClient-Lite-1.8.1.jar`**: Versionsübergreifender Lite Mod JAR
