import os
import sys
import unittest
from pathlib import Path

os.environ["QT_QPA_PLATFORM"] = "offscreen"
from PySide6.QtWidgets import QApplication
from PySide6.QtQml import QQmlApplicationEngine
from PySide6.QtCore import QUrl

from backend.services.store import ProfileStore
from backend.models.profile_model import ProfileModel
from backend.models.mod_model import ModModel
from backend.controllers.profile_controller import ProfileController
from backend.controllers.account_controller import AccountController


class OnboardingSelectionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not QApplication.instance():
            cls.app = QApplication(sys.argv)
        else:
            cls.app = QApplication.instance()

    def setUp(self):
        self.engine = QQmlApplicationEngine()
        self.qml_dir = Path(__file__).resolve().parent.parent / "ui"
        self.engine.addImportPath(str(self.qml_dir))

        self.store = ProfileStore()
        self.profile_model = ProfileModel()
        self.mod_model = ModModel()
        self.profile_controller = ProfileController(self.store, self.profile_model, self.mod_model)
        self.account_controller = AccountController()

        self.engine.rootContext().setContextProperty("profileController", self.profile_controller)
        self.engine.rootContext().setContextProperty("accountController", self.account_controller)

    def test_ezdropdown_is_ezclient_supported(self):
        self.engine.load(QUrl.fromLocalFile(str(self.qml_dir / "components" / "EzDropDown.qml")))
        self.assertTrue(len(self.engine.rootObjects()) > 0)
        dd = self.engine.rootObjects()[-1]
        self.assertTrue(dd.isEzClientSupported("26.2"))
        self.assertTrue(dd.isEzClientSupported("26.1.1"))
        self.assertTrue(dd.isEzClientSupported("26.1"))
        self.assertFalse(dd.isEzClientSupported("1.15.2"))

    def test_versions_page_defaults_to_ezclient(self):
        self.engine.load(QUrl.fromLocalFile(str(self.qml_dir / "VersionsPage.qml")))
        self.assertTrue(len(self.engine.rootObjects()) > 0)
        vp = self.engine.rootObjects()[-1]
        self.assertEqual(vp.property("selectedLoader"), "EzClient")

    def test_onboarding_page_defaults_and_version_switching(self):
        self.engine.load(QUrl.fromLocalFile(str(self.qml_dir / "OnboardingPage.qml")))
        self.assertTrue(len(self.engine.rootObjects()) > 0)
        page = self.engine.rootObjects()[-1]

        # Initial default must be EzClient
        self.assertEqual(page.property("newVersion"), "26.2")
        self.assertEqual(page.property("newLoader"), "Fabric")
        self.assertEqual(page.property("selectedPreset"), "ezclient")

        # Find versionPicker dropdown
        dropdowns = []
        def collect_dropdowns(item):
            if "EzDropDown" in item.metaObject().className():
                dropdowns.append(item)
            for child in item.children():
                collect_dropdowns(child)

        collect_dropdowns(page)
        self.assertGreaterEqual(len(dropdowns), 1)
        picker = dropdowns[0]
        self.assertTrue(picker.property("formatEzClientSupported"))

        # Find index for an unsupported release, e.g. 1.15.2
        choices = page.allMinecraftVersions().toVariant()
        if "1.15.2" in choices:
            idx_115 = choices.index("1.15.2")
            picker.setProperty("currentIndex", idx_115)
            picker.choiceChanged.emit()
            self.assertEqual(page.property("selectedPreset"), "performance")
            self.assertEqual(page.property("newLoader"), "Fabric")

            # Switch back to 26.2 (index 0)
            picker.setProperty("currentIndex", 0)
            picker.choiceChanged.emit()
            self.assertEqual(page.property("selectedPreset"), "ezclient")
            self.assertEqual(page.property("newLoader"), "Fabric")

    def test_onboarding_reset_restores_welcome_and_clears_state(self):
        self.engine.load(QUrl.fromLocalFile(str(self.qml_dir / "OnboardingPage.qml")))
        self.assertTrue(len(self.engine.rootObjects()) > 0)
        page = self.engine.rootObjects()[-1]

        # Simulate a completed download step
        page.setProperty("step", "downloading")
        page.setProperty("downloadProgress", 1.0)
        page.setProperty("newName", "Test Completed Profile")
        page.setProperty("downloadStatus", "Profil erfolgreich eingerichtet & optimiert!")

        self.assertEqual(page.property("step"), "downloading")
        self.assertEqual(page.property("downloadProgress"), 1.0)

        # Call reset
        page.reset()

        self.assertEqual(page.property("step"), "welcome", "Onboarding must reset to welcome step")
        self.assertEqual(page.property("downloadProgress"), 0.0, "Progress must be reset to 0.0")
        self.assertEqual(page.property("newName"), "", "Profile name must be cleared")
        self.assertEqual(page.property("setupFailed"), False)
        self.assertEqual(page.property("selectedPreset"), "ezclient")


if __name__ == "__main__":
    unittest.main()

