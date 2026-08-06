# EzClient — Minecraft Launcher

Ein einfacher, moderner Minecraft Launcher mit Microsoft-Login und automatischen Updates.

## Features

- 🎮 **Microsoft-Login** — sicher über Device-Code-Flow
- 🔄 **Automatische Updates** — prüft auf neue Versionen beim Start
- 📦 **Easy Installation** — `EzClientSetup.exe` downloaded und installiert den Client
- 🎯 **Vanilla Minecraft** — volle Kompatibilität mit aktuellen Versionen
- 🖥️ **Modernes UI** — dunkles tkinter-Design

## Installation

1. Lade `EzClientSetup.exe` aus dem [neuesten Release](https://github.com/LuigiLetsPlay/EzClient/releases/latest) herunter
2. Führe die `.exe` aus — der Launcher wird in `%LOCALAPPDATA%\EzClient\` installiert
3. Eine Desktop-Verknüpfung wird automatisch erstellt

## Verwendung

1. Starte `EzClient.exe`
2. Melde dich mit deinem Microsoft-Konto an (Device-Code-Flow)
3. Wähle eine Minecraft-Version aus der Liste
4. Klick „Spielen" — der Client wird installiert und startet automatisch
5. Dein Spieler-Name wird automatisch erkannt

## Entwicklung

### Requirements

- Python 3.12+ (mit tkinter)
- `minecraft-launcher-lib>=8.0`
- `requests>=2.28.0`

### Local Setup

```bash
git clone https://github.com/LuigiLetsPlay/EzClient.git
cd EzClient
pip install -r requirements.txt
python -m launcher.main
```

### Building

```bash
pip install pyinstaller
pyinstaller --onefile --noconsole --name EzClient launcher/main.py
pyinstaller --onefile --noconsole --name EzClientSetup setup/setup_main.py
```

Binaries erscheinen in `dist/`.

## Releases & Auto-Build

Pushe einen Tag mit dem Format `v*` (z.B. `v1.1.0`):

```bash
git tag v1.1.0
git push origin v1.1.0
```

GitHub Actions baut beide Exes automatisch und uploaded sie ins Release.

## Architecture

```
launcher/
  ├─ main.py       # tkinter-GUI (3 Screens: Login, Version, Install)
  ├─ auth.py       # Microsoft Device-Code-Flow
  ├─ game.py       # minecraft-launcher-lib Wrapper
  ├─ updater.py    # GitHub-Release-Check
  └─ version.py    # Konstanten

setup/
  └─ setup_main.py # Downloader für EzClient.exe
```

## Lizenz

MIT

## Support

Melde Bugs oder Feature-Requests unter [Issues](https://github.com/LuigiLetsPlay/EzClient/issues).
