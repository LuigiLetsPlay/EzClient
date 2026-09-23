# EzClient 2.2.3

EzClient 2.2.3 stellt den nativen EzClient-Direktstart und Microsoft-Login als Standard sicher, verhindert jegliches automatisches Aufrufen des offiziellen Launchers, beseitigt aufblitzende CMD-Konsolenfenster beim Server-Pingen und optimiert die MOTD- und Spielerlisten-Erkennung für moderne Netzwerke.

## Highlights & Neuerungen in v2.2.3

- **Reiner Direktstart ohne offiziellen Minecraft Launcher:** EzClient lädt alle erforderlichen Bibliotheken, Assets und Fabric-Dateien eigenständig über die integrierte Download-Engine herunter (`ensure_game_ready`). Ein fehlerhafter automatischer Start des offiziellen Mojang Launchers auf frischen Systemen ist nun vollständig ausgeschlossen.
- **Nativer EzClient Microsoft-Login Dialog:** Wenn ein Spieler noch nicht angemeldet ist oder eine Sitzung abgelaufen ist, fordert EzClient den Spieler direkt über die Oberfläche auf (*„ANMELDEN & SPIELEN“*). Ein Klick öffnet das native EzClient-Anmeldefenster (`MicrosoftLoginDialog`).
- **Keine aufblitzenden CMD-Konsolenfenster mehr:** Der integrierte Server-Pinger führt DNS-SRV-Auflösungen über `nslookup` nun geräuschlos ohne jegliche aufpoppende Windows-Konsolenfenster (`CREATE_NO_WINDOW`, `SW_HIDE`) aus.
- **Optimiertes MOTD- & Spieler-Parsing:** Der Server-Pinger unterstützt nun rekursiv verschachtelte Minecraft-JSON-Chat-Komponenten (`extra`, `translate`, `with`), sodass farbige Server-Beschreibungen und Spielernamen auf Netzwerken (BungeeCord, Velocity, Paper) zuverlässig und ohne Zeichenmüll dargestellt werden.
- **Virtuelles Server-Hosting & Handshake-Fix:** Im Handshake-Paket wird nun die vom Spieler eingegebene Hostadresse übergeben, wodurch auch Server hinter Proxies oder Cloudflare Spectrum sofort korrekt antworten.
- **Versionssprung 2.2.3:** Alle Metadaten, Mod-JARs und Installer-Konfigurationen wurden auf Version 2.2.3 synchronisiert.

## Downloads

- **`EzClient-Setup.exe`** – nativer Windows-Installer für die empfohlene Installation und Updates.
- **`EzClient-v2.2.3-Windows-Portable.zip`** – portable Windows-Version zum Entpacken und direkten Starten.
- **`EzClient-2.2.3+26.2.jar`** – EzClient Fabric-Mod für Minecraft 26.2.
- **`EzClient-2.2.3+26.1.1.jar`** – EzClient Fabric-Mod für Minecraft 26.1.1.
- **`EzClient-2.2.3+26.1.jar`** – EzClient Fabric-Mod für Minecraft 26.1.

## SHA-256

- `EzClient-Setup.exe`: `4a9d3f923d9d24dedafe0adf9fdc41aa4fbb224349f8520e4158ba4c8d8af898`
- `EzClient-v2.2.3-Windows-Portable.zip`: `0670e5105f65ebecf788d97796c7a1d56fc0d9790181906d2c1b873331dc11e7`
- `EzClient-2.2.3+26.2.jar`: `69507906f477274157dfe21397b849c21060c8a98ab7bd0175ec547d19613049`
- `EzClient-2.2.3+26.1.1.jar`: `a6c39cfd3e285fcd1873d527ccb1f724cdc529115f7b166650c621277ba0b372`
- `EzClient-2.2.3+26.1.jar`: `3c977e394cc31a7c6f32062ca9ba121bc68b9cf05a76e1754d0b88867d9a6bdb`
