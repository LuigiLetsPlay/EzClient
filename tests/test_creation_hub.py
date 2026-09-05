import os
import sys
import unittest
import threading

os.environ["QT_QPA_PLATFORM"] = "offscreen"
from PySide6.QtCore import QUrl
from PySide6.QtGui import QGuiApplication
from PySide6.QtQml import QQmlApplicationEngine


class CreationHubTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.app = QGuiApplication.instance() or QGuiApplication(sys.argv)
        cls.engine = QQmlApplicationEngine()
        cls.engine.load(QUrl.fromLocalFile("ui/App.qml"))
        assert cls.engine.rootObjects(), "Failed to load App.qml"
        cls.root = cls.engine.rootObjects()[0]

    def test_creation_hub_modal_exists_and_opens(self):
        # Verify openCreationHub method on root window
        self.root.openCreationHub()
        modal = self.root.findChild(object, "globalCreationHubModal")
        if modal is None:
            # Look up via QML child hierarchy
            for child in self.root.children():
                if "CreationHubModal" in child.__class__.__name__ or hasattr(child, "creationHubView"):
                    modal = child
                    break
        self.assertIsNotNone(modal, "CreationHubModal must exist in App.qml")
        self.assertTrue(modal.property("isOpen"))
        self.assertEqual(modal.property("creationHubView"), "choices")

        # Test close
        modal.close()
        self.assertFalse(modal.property("isOpen"))

    def test_creation_hub_norisk_view(self):
        self.root.openCreationHubView("norisk")
        modal = self.root.findChild(object, "globalCreationHubModal")
        self.assertIsNotNone(modal)
        self.assertTrue(modal.property("isOpen"))
        self.assertEqual(modal.property("creationHubView"), "norisk")
        modal.close()

    def test_navigation_rail_has_create_signal(self):
        rail = self.root.findChild(object, "navRail")
        if rail is None:
            for child in self.root.children():
                if "NavigationRail" in child.__class__.__name__ or hasattr(child, "createProfileClicked"):
                    rail = child
                    break
        self.assertIsNotNone(rail, "NavigationRail must be present in App.qml")
        # Ensure createProfileClicked signal exists
        self.assertTrue(hasattr(rail, "createProfileClicked"))

    def test_global_profile_icon_picker_modal_exists_and_opens(self):
        self.assertTrue(hasattr(self.root, "openProfileIconPicker"))
        self.root.openProfileIconPicker("test_id", "norisk", "Test Profile")
        modal = self.root.findChild(object, "globalProfileIconPickerModal")
        if modal is None:
            for child in self.root.children():
                if "ProfileIconPickerModal" in child.__class__.__name__ or hasattr(child, "profileId"):
                    modal = child
                    break
        self.assertIsNotNone(modal, "ProfileIconPickerModal must exist in App.qml")
        self.assertTrue(modal.property("isOpen"))
        self.assertEqual(modal.property("profileId"), "test_id")
        self.assertEqual(modal.property("selectedIcon"), "norisk")
        modal.close()
        self.assertFalse(modal.property("isOpen"))

    def test_official_norisk_icon_asset(self):
        from pathlib import Path
        png_path = Path("ui/icons/client-norisk.png")
        self.assertTrue(png_path.is_file(), "Official NoRisk PNG asset must exist")
        data = png_path.read_bytes()
        self.assertTrue(data.startswith(b"\x89PNG\r\n\x1a\n"), "Must be a valid PNG")
        self.assertGreaterEqual(len(data), 14000, "Must be authentic high-res logo")

        svg_path = Path("ui/icons/client-norisk.svg")
        self.assertTrue(svg_path.is_file(), "client-norisk.svg must exist")
        self.assertIn("image/png;base64,", svg_path.read_text(encoding="utf-8"))

    def test_profile_controller_icon_management(self):
        import tempfile
        import shutil
        from pathlib import Path
        from backend.services.store import ProfileStore
        from backend.models.profile_model import ProfileModel
        from backend.models.mod_model import ModModel
        from backend.controllers.profile_controller import ProfileController

        store = ProfileStore()
        prof_model = ProfileModel()
        mod_model = ModModel()
        controller = ProfileController(store, prof_model, mod_model)

        # Create profile with preset icon
        pid = controller.createProfile("Test Warrior", "26.2", "Fabric", "ezclient", "shield")
        try:
            self.assertEqual(controller.activeIcon, "shield")
            self.assertEqual(controller.inspectedIcon, "shield")

            # Change icon to another preset
            controller.setProfileIcon(pid, "tnt")
            self.assertEqual(controller.activeIcon, "tnt")

            # Change icon using custom file
            temp_dir = tempfile.mkdtemp(prefix="ezclient_icon_test_")
            try:
                fake_png = Path(temp_dir) / "custom.png"
                fake_png.write_bytes(b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDRfake")
                controller.setProfileIcon(pid, str(fake_png))

                p = store.get_by_id(pid)
                dest_png = p.path / "icon.png"
                self.assertTrue(dest_png.is_file())
                self.assertEqual(dest_png.read_bytes(), fake_png.read_bytes())
                self.assertEqual(controller.activeIcon, f"file:///{dest_png.as_posix()}")

                # Verify store persistence across reload
                new_store = ProfileStore()
                loaded = new_store.get_by_id(pid)
                self.assertEqual(loaded.icon, f"file:///{dest_png.as_posix()}")
            finally:
                shutil.rmtree(temp_dir, ignore_errors=True)
        finally:
            controller.deleteProfile(pid)

    def test_delete_profile_modal_exists_and_opens(self):
        self.root.confirmDeleteProfile("test-del-id", "My Test Profile", None)
        modal = self.root.findChild(object, "globalDeleteProfileModal")
        if modal is None:
            for child in self.root.children():
                if "DeleteProfileModal" in child.__class__.__name__ or hasattr(child, "confirmDelete"):
                    modal = child
                    break
        self.assertIsNotNone(modal, "DeleteProfileModal must exist in App.qml")
        self.assertTrue(modal.property("isOpen"))
        self.assertEqual(modal.property("profileId"), "test-del-id")
        self.assertEqual(modal.property("profileName"), "My Test Profile")

        modal.close()
        self.assertFalse(modal.property("isOpen"))

    def test_robust_deletion_cleans_folder_and_frees_name(self):
        from backend.services.store import ProfileStore, PROFILES_DIR
        store = ProfileStore()
        p1 = store.create_profile("Delete Robust Test", "26.2", "Fabric", "raw")
        p1_folder = p1.path
        self.assertTrue(p1_folder.exists())
        (p1_folder / "test_file.txt").write_text("hello", encoding="utf-8")

        # Delete profile
        deleted = store.delete_profile(p1.id)
        self.assertTrue(deleted)
        self.assertFalse(p1_folder.exists(), "Profile folder must be completely deleted from disk")

        # Recreate profile with exact same name - should NOT have (2) suffix!
        p2 = store.create_profile("Delete Robust Test", "26.2", "Fabric", "raw")
        try:
            self.assertEqual(p2.name, "Delete Robust Test", "Recreated profile must retain base name without unwanted number suffix")
        finally:
            store.delete_profile(p2.id)

    def test_deleting_all_profiles_restarts_onboarding_at_welcome(self):
        onboarding = self.root.findChild(object, "onboardingPage")
        if onboarding is None:
            for child in self.root.children():
                if "OnboardingPage" in child.__class__.__name__ or hasattr(child, "allMinecraftVersions"):
                    onboarding = child
                    break
        self.assertIsNotNone(onboarding, "onboardingPage must exist in App.qml")

        # Simulate onboarding having finished earlier
        onboarding.setProperty("step", "downloading")
        onboarding.setProperty("downloadProgress", 1.0)
        onboarding.setProperty("newName", "Previous Finished")

        # Now simulate all profiles deleted -> needsOnboarding becomes true
        self.root.setProperty("needsOnboarding", False)
        self.root.setProperty("needsOnboarding", True)

        self.assertEqual(onboarding.property("step"), "welcome", "Onboarding must restart at welcome screen")
        self.assertEqual(onboarding.property("downloadProgress"), 0.0)
        self.assertEqual(onboarding.property("newName"), "")

    def test_live_logs_window_shows_immediately_on_launch_preparation(self):
        # 1. Verify openLiveLogs method exists on root window and opens globalLiveLogsWindow
        self.assertTrue(hasattr(self.root, "openLiveLogs"))
        logs_window = self.root.findChild(object, "globalLiveLogsWindow")
        self.assertIsNotNone(logs_window, "LiveLogsWindow must exist in App.qml")

        self.root.openLiveLogs()
        self.assertTrue(logs_window.property("visible"), "openLiveLogs() must show the LiveLogsWindow")
        logs_window.hide()
        self.assertFalse(logs_window.property("visible"))

        # 2. Test that launching starts instance early and emits non-error preparation signal
        from backend.services.store import ProfileStore
        from backend.models.profile_model import ProfileModel
        from backend.models.mod_model import ModModel
        from backend.controllers.profile_controller import ProfileController

        store = ProfileStore()
        prof_model = ProfileModel()
        mod_model = ModModel()
        controller = ProfileController(store, prof_model, mod_model)

        pid = controller.createProfile("Logs Test Profile", "26.2", "Fabric", "ezclient")
        try:
            emitted_statuses = []
            controller.launchStatusChanged.connect(lambda msg, is_err: emitted_statuses.append((msg, is_err)))

            worker_done = threading.Event()

            def mock_direct(prof, cb, log_file):
                cb("Mock launching")
                worker_done.set()
                return None

            import backend.controllers.profile_controller as pc_mod
            orig_sync = pc_mod.sync_profile_mods
            orig_direct = pc_mod.launch_minecraft_direct
            try:
                pc_mod.sync_profile_mods = lambda prof, status_callback=None: status_callback("Syncing test...") if status_callback else None
                pc_mod.launch_minecraft_direct = mock_direct

                controller.launchActiveProfile()
                self.assertIsNotNone(controller.liveLogService.selectedInstanceId)
                log_text = controller.liveLogService.getAllLogsText()
                self.assertIn("Starte Vorbereitung", log_text)
                self.assertTrue(any(status[0] == "Starte Vorbereitung…" and status[1] is False for status in emitted_statuses))
                worker_done.wait(timeout=2.0)
            finally:
                pc_mod.sync_profile_mods = orig_sync
                pc_mod.launch_minecraft_direct = orig_direct
        finally:
            if controller.liveLogService.selectedInstanceId:
                controller.liveLogService.detach_process(controller.liveLogService.selectedInstanceId)
            controller.deleteProfile(pid)


if __name__ == "__main__":
    unittest.main()



