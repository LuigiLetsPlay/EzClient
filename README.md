# ⚡ EzClient

<p align="center">
  <img src="ui/assets/logo.png" alt="EzClient Logo" width="128" height="128" />
</p>

<p align="center">
  <strong>The Ultimate Next-Gen Minecraft Client & High-Performance Launcher</strong><br>
  Built with PySide6 (Qt Quick / QML) · Standalone Fabric Client Mod · 100% German & English Localization
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.4%20%7C%20Fabric-24D677?style=flat-square" alt="Minecraft" />
  <img src="https://img.shields.io/badge/Python-3.11+-3776AB?style=flat-square&logo=python&logoColor=white" alt="Python" />
  <img src="https://img.shields.io/badge/UI-PySide6%20%2F%20QML-41CD52?style=flat-square&logo=qt&logoColor=white" alt="Qt" />
  <img src="https://img.shields.io/badge/License-EzClient%20License-purple?style=flat-square" alt="License" />
</p>

---

## ✨ Features

- 🚀 **Maximum FPS & Ultra-Low Latency**: Built-in Sodium, Lithium, FerriteCore, Memory Leak Fix, Krypton, ImmediatelyFast, and Entity Culling.
- ⚡ **Standalone Ingame Client (`EzClient.jar`)**:
  - Sets GLFW window title dynamically to **`EzClient`** with custom icon.
  - Automatically bypasses narrator & accessibility onboarding prompts.
  - Auto-configures optimal PvP & Sodium graphics presets.
  - Synchronizes with unified `%APPDATA%\.ezclient` client storage.
- 🎨 **Modern Dark UI (QML & PySide6)**: Fluid animations, glassmorphism, responsive sidebar navigation, and Minecraft-styled typography.
- 📦 **Modrinth Marketplace Integration**: Search, inspect, install, toggle, and update mods with one click.
- 🌍 **100% Multilingual**: Seamless, instant switching between **German (🇩🇪)** and **English (🇬🇧)**.
- 🎮 **Direct Launch & Official Launcher Integration**: Launch profiles directly into Minecraft or patch them into the official Minecraft Launcher.
- 💾 **Microsoft / Minecraft Account Management**: Seamless Xbox Live / Microsoft OAuth authentication.

---

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| **Frontend UI** | PySide6 (Qt Quick, QML, JavaScript) |
| **Backend & Controllers** | Python 3.11+ (Threading, PySide6 Signals/Slots) |
| **Ingame Client Mod** | Java 17/21 (Fabric Mod Loader) |
| **Mod Ecosystem** | Modrinth REST API v2 |
| **Packaging** | PyInstaller (Standalone Windows `.exe`) |

---

## 🚀 Quick Start (Running from Source)

### 1. Prerequisites
- **Python 3.11+**
- **Java JDK 17+** (for building the client mod)

### 2. Clone the Repository
```bash
git clone https://github.com/LuigiLetsPlay/EzClient.git
cd EzClient
```

### 3. Install Dependencies
```bash
pip install -r requirements.txt
```

### 4. Launch EzClient
```bash
python main.py
```

---

## 🔨 Building Standalone `EzClient.exe`

To build the single-file executable for Windows:

```bash
python build_exe.py
```

The resulting binary will be in `dist/EzClient.exe`.

---

## 📂 Project Structure

```
EzClient/
├── backend/                  # Python backend controllers & services
│   ├── assets/               # Built-in EzClient.jar and assets
│   ├── controllers/          # QML-exposed PySide6 controllers
│   ├── models/               # Data structures and QAbstractListModels
│   └── services/             # Minecraft launcher, direct launch, Modrinth API
├── client_mod/               # Java source code for EzClient.jar Fabric mod
├── ui/                       # Modern QML user interface
│   ├── assets/               # Branding logos and icons
│   ├── components/           # Reusable QML widgets (Buttons, Dropdowns, Cards)
│   ├── fonts/                # Authentic Minecraft fonts
│   ├── App.qml               # Main QML application entry point
│   ├── EzI18n.qml            # Complete DE/EN translation dictionaries
│   └── EzTheme.qml           # Central theme tokens and color palette
├── build_exe.py              # PyInstaller build automation script
├── main.py                   # Desktop application entry point
└── requirements.txt          # Python dependencies
```

---

## 📜 License

This project is licensed under the **EzClient License** (Personal, non-commercial use only). See the [LICENSE](LICENSE) file for details.

Contributions, feature suggestions and issue reports are welcome!
