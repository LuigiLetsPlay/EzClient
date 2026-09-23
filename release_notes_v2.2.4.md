# EzClient 2.2.4

EzClient 2.2.4 vereinheitlicht Starter- und Profil-Flow, behebt die sichtbare Log-Trennung, speichert Account-Skins als echte Texturen und startet den Launcher nach stillen Updates wieder automatisch. Außerdem enthält das Release die Profil-, Cape-, HUD-, Erweiterungs- und Importverbesserungen aus dem aktuellen Entwicklungsstand.

## Highlights & Fehlerbehebungen

- **Starter- und Profil-Flow synchron:** Profile werden vollständig in EzClient vorbereitet und Minecraft wird direkt mit der in EzClient verwalteten Microsoft-Sitzung gestartet. Der offizielle Minecraft Launcher wird nicht als Start-Fallback verwendet.
- **Saubere Logs pro Spielstart:** Das sichtbare Logmodell wird beim Beginn jeder neuen Instanz explizit geleert. Frühere Sitzungen bleiben separat auswählbar, laufen aber nicht mehr optisch in den neuen Start hinein.
- **Serverliste aktualisiert sich gezielt:** Die aufgeklappte Serverliste auf Home wird alle 30 Sekunden aktualisiert – nur während Home sichtbar und die Liste geöffnet ist.
- **Automatischer Relaunch nach Updates:** Der stille Update-Modus startet die frisch installierte EzClient-Version nach Abschluss des Setups wieder.
- **Gespeicherte Skins bleiben korrekt:** Beim Speichern des aktuellen Account-Skins wird die vollständige PNG-Textur als unveränderliche Bibliothekskopie abgelegt. Benutzerdefinierte Namen wie `Main` werden nicht mehr als fremde Minecraft-Spielernamen aufgelöst; Slim/Classic bleibt erhalten.
- **Nativer Direktstart & Spielzeit:** Der Startpfad verwendet ausschließlich den EzClient-Direktstart und zeichnet den letzten erfolgreichen Spielstart im Profil auf.
- **Fenster-Lebenszyklus:** Launcher und Minecraft-Logs können unabhängig geschlossen werden; EzClient beendet sich erst, wenn beide Fenster geschlossen sind, sofern Tray-Modus deaktiviert ist.
- **Capes und Namens-Badges:** Cape-Aktualisierungen werden zuverlässig nachgeholt und mehrere Client-Badges reservieren dynamisch Platz.
- **Erweiterungen:** Lokal vorhandene JARs erscheinen in der Erweiterungsansicht. Für Fabric-Profile auf 26.x kann EzClient nach einer Entfernung direkt wieder hinzugefügt werden.
- **Profilimport:** Profile aus NoRiskClient, Modrinth, CurseForge, Prism Launcher und MultiMC können mitsamt Profilinhalten importiert werden.
- **Minecraft-orientiertes Design:** HUD-Radien sind auf maximal einen Pixel reduziert; Profilicons nutzen EzClient-/Client-Logos sowie Vanilla-Blöcke und -Items; Cape-Karten zeigen die Vorderseite.

## Downloads

- **`EzClient-Setup.exe`** – empfohlener, signierter Windows-Installer und Updater.
- **`EzClient-v2.2.4-Windows-Portable.zip`** – signierte portable Windows-Version.
- **`EzClient-2.2.4+26.2.jar`** – exakte EzClient Fabric-Mod für Minecraft 26.2.
- **`EzClient-2.2.4+26.1.1.jar`** – exakte EzClient Fabric-Mod für Minecraft 26.1.1.
- **`EzClient-2.2.4+26.1.jar`** – exakte EzClient Fabric-Mod für Minecraft 26.1.

## SHA-256

- `EzClient-Setup.exe`: `a3c8469ef8590c09502ffb0be6c94a382a11b896a80350c22816739c2594ffe0`
- `EzClient-v2.2.4-Windows-Portable.zip`: `44df90b8f85bc943e0ce258e8547cd64c2bee13b6a86153c37684b1291ad238e`
- `EzClient-2.2.4+26.2.jar`: `75cd58b6f93db2f87c898ab24bdc0aeb7f22e9e554cfc5b342f755884baaba27`
- `EzClient-2.2.4+26.1.1.jar`: `b61ae2967d2924a46c30d513213fc103850c40ec10215a3a0be8f5f3b50036c5`
- `EzClient-2.2.4+26.1.jar`: `507d796098f94290998c26b9642609c57053833ec1e0408c6be3bc3a1b0a8310`
