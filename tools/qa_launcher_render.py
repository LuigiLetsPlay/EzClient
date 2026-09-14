"""Render actual launcher pages in DE/EN using disposable data and software Qt."""
import json
import os
import sys
import tempfile
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
sys.path.insert(0, str(ROOT))
os.environ["APPDATA"] = tempfile.mkdtemp(prefix="ezclient-render-qa-")
os.environ["QT_QPA_PLATFORM"] = "offscreen"
os.environ["QT_QUICK_BACKEND"] = "software"
os.environ["QT_QUICK_CONTROLS_STYLE"] = "Basic"
from PySide6.QtCore import QUrl, QEventLoop, QTimer, qInstallMessageHandler
from PySide6.QtWidgets import QApplication
from PySide6.QtQml import QQmlApplicationEngine
from PySide6.QtQuick import QQuickWindow
from backend.services.store import ProfileStore
from backend.models.profile_model import ProfileModel
from backend.models.mod_model import ModModel
from backend.controllers.profile_controller import ProfileController
from backend.controllers.account_controller import AccountController

def settle():
    loop = QEventLoop()
    QTimer.singleShot(450, loop.quit)
    loop.exec()

def main():
    out = ROOT / "build/release-qa/launcher"
    out.mkdir(parents=True, exist_ok=True)
    messages = []
    qInstallMessageHandler(lambda kind, context, message: messages.append(message))
    app = QApplication([])
    store = ProfileStore()
    store.create_profile("Survival – langer Profilname zum Layouttest", "26.2", "Fabric", "raw")
    model, mods = ProfileModel(), ModModel()
    controller = ProfileController(store, model, mods)
    account = AccountController()
    engine = QQmlApplicationEngine()
    engine.rootContext().setContextProperty("profileController", controller)
    engine.rootContext().setContextProperty("accountController", account)
    engine.rootContext().setContextProperty("modrinthController", None)
    engine.rootContext().setContextProperty("updateController", None)
    engine.load(QUrl.fromLocalFile(str(ROOT / "ui/App.qml")))
    if not engine.rootObjects():
        raise RuntimeError("QML failed to load: " + "\n".join(messages))
    window = engine.rootObjects()[0]
    window.show()
    captures = []
    for language in ("de", "en"):
        controller.setLanguage(language)
        for width, height in ((760, 560), (1280, 820)):
            window.resize(width, height)
            for route in ("home", "profiles", "versions", "installed_mods", "settings", "cape"):
                window.navigateTo(route)
                settle()
                filename = f"{language}-{width}-{route}.png"
                if not window.grabWindow().save(str(out / filename)):
                    raise RuntimeError("Screenshot failed: " + filename)
                captures.append(filename)
    window.openCreationHub()
    settle()
    window.grabWindow().save(str(out / "en-creation-hub.png"))
    (out / "report.json").write_text(json.dumps({"captures": captures, "qml_messages": sorted(set(messages))}, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"Rendered {len(captures) + 1} actual QML views; warnings recorded in {out / 'report.json'}")
    window.hide()

if __name__ == "__main__":
    main()
