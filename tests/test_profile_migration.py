import tempfile
import unittest
import zipfile
from pathlib import Path

import backend.models.types as model_types
import backend.services.store as store_module
from backend.models.types import ModData, ProfileData
from backend.services.profile_migration import CORE_IDS, MIGRATION_VERSION, ProfileMigrationService
from backend.services.mod_downloader import sync_profile_mods


def write_fabric_jar(path: Path, mod_id: str, name: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    manifest = f'{{"schemaVersion":1,"id":"{mod_id}","version":"1.0","name":"{name}"}}'
    with zipfile.ZipFile(path, "w") as archive:
        archive.writestr("fabric.mod.json", manifest)


class FakeStore:
    def __init__(self, profiles: list[ProfileData]):
        self.profiles = profiles
        self.settings = {}


class ProfileMigrationTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.old_profiles_dir = model_types.PROFILES_DIR
        self.old_store_profiles_dir = store_module.PROFILES_DIR
        self.old_state_path = store_module.STATE_PATH
        model_types.PROFILES_DIR = Path(self.temp.name) / "profiles"
        store_module.PROFILES_DIR = model_types.PROFILES_DIR
        store_module.STATE_PATH = Path(self.temp.name) / "state.json"

    def tearDown(self) -> None:
        model_types.PROFILES_DIR = self.old_profiles_dir
        store_module.PROFILES_DIR = self.old_store_profiles_dir
        store_module.STATE_PATH = self.old_state_path
        try:
            self.temp.cleanup()
        except Exception:
            pass

    def test_removes_only_launcher_owned_legacy_jar_and_repairs_core(self) -> None:
        profile = ProfileData(
            id="managed", name="Managed", minecraft_version="26.2",
            profile_type="ezclient",
            integrated_mods=["ezclient", "entityculling"],
            managed_core_mods=["ezclient", "entityculling"],
            mods=[
                ModData("entityculling", "entityculling", "Entity Culling", "old", "1", "entityculling.jar", recommended=True),
                ModData("custom", "custom", "Custom Mod", "1", "1", "custom.jar"),
            ],
        )
        write_fabric_jar(profile.mods_path / "entityculling.jar", "entityculling", "Entity Culling")
        write_fabric_jar(profile.mods_path / "custom.jar", "custom", "Custom Mod")

        store = FakeStore([profile])
        report = ProfileMigrationService(store).run_if_needed()

        self.assertTrue(report.changed)
        self.assertFalse((profile.mods_path / "entityculling.jar").exists())
        self.assertTrue((profile.mods_path / "custom.jar").exists())
        self.assertEqual(list(CORE_IDS), profile.managed_core_mods)
        self.assertEqual(set(CORE_IDS), {mod.slug for mod in profile.mods if mod.slug in CORE_IDS})
        self.assertIn("custom.jar", profile.user_mods)
        self.assertEqual(MIGRATION_VERSION, store.settings["profile_migration_version"])

    def test_removes_legacy_dependency_libraries_in_ezclient_profile(self) -> None:
        profile = ProfileData(
            id="managed", name="Managed", minecraft_version="26.2",
            profile_type="ezclient",
            integrated_mods=["ezclient"],
            managed_core_mods=["ezclient"],
            mods=[
                ModData("ezclient", "ezclient", "EzClient", "1.8.0", "1.8.0", "EzClient.jar", essential=True),
            ],
        )
        write_fabric_jar(profile.mods_path / "yet_another_config_lib_v3-3.9.6+26.2-fabric.jar", "yet_another_config_lib_v3", "Yet Another Config Lib")
        write_fabric_jar(profile.mods_path / "cloth-config-fabric-17.0.144.jar", "cloth-config", "Cloth Config")
        write_fabric_jar(profile.mods_path / "EzClient.jar", "ezclient", "EzClient")

        store = FakeStore([profile])
        report = ProfileMigrationService(store).run_if_needed()

        self.assertTrue(report.changed)
        self.assertFalse((profile.mods_path / "yet_another_config_lib_v3-3.9.6+26.2-fabric.jar").exists())
        self.assertFalse((profile.mods_path / "cloth-config-fabric-17.0.144.jar").exists())
        self.assertTrue((profile.mods_path / "EzClient.jar").exists())

    def test_preserves_manually_installed_legacy_mod_without_ownership(self) -> None:
        profile = ProfileData(
            id="manual", name="Manual", minecraft_version="26.2", profile_type="raw",
            mods=[ModData("entityculling", "entityculling", "Entity Culling", "1", "1", "entityculling.jar")],
            user_mods=["entityculling"],
        )
        write_fabric_jar(profile.mods_path / "entityculling.jar", "entityculling", "Entity Culling")

        ProfileMigrationService(FakeStore([profile])).run_if_needed()

        self.assertTrue((profile.mods_path / "entityculling.jar").exists())
        self.assertEqual(["entityculling"], [mod.slug for mod in profile.mods])

    def test_raw_profile_loses_launcher_injected_core(self) -> None:
        profile = ProfileData(
            id="raw", name="Raw", minecraft_version="26.2", profile_type="raw",
            mods=[ModData("ezclient", "ezclient", "EzClient", "old", "1", "EzClient.jar", essential=True)],
        )
        write_fabric_jar(profile.mods_path / "EzClient.jar", "ezclient", "EzClient")

        ProfileMigrationService(FakeStore([profile])).run_if_needed()

        self.assertFalse((profile.mods_path / "EzClient.jar").exists())
        self.assertEqual([], profile.mods)
        self.assertEqual([], profile.managed_core_mods)

    def test_profile_creation_separates_core_and_opt_in_mods(self) -> None:
        store = object.__new__(store_module.ProfileStore)
        store.settings = {"last_profile": ""}
        store.profiles = []

        raw = store.create_profile("Raw Test", "26.2", preset="raw")
        managed = store.create_profile(
            "Managed Test", "26.2", preset="ezclient",
            selected_optional_mods=["essential"],
        )

        self.assertEqual([], raw.mods)
        self.assertEqual("raw", raw.profile_type)
        self.assertEqual(set(CORE_IDS), set(managed.managed_core_mods))
        self.assertEqual({"essential"}, set(managed.user_mods))
        self.assertEqual(set(CORE_IDS) | {"essential"}, {mod.slug for mod in managed.mods})
        self.assertTrue((managed.path / "profile.json").is_file())

    def test_forge_profile_is_always_raw_without_ezclient_mods(self) -> None:
        store = object.__new__(store_module.ProfileStore)
        store.settings = {"last_profile": ""}
        store.profiles = []

        profile = store.create_profile("Forge Test", "1.20.1", loader="Forge", preset="ezclient")

        self.assertEqual("Forge", profile.loader)
        self.assertEqual("raw", profile.profile_type)
        self.assertEqual([], profile.mods)
        self.assertEqual([], profile.managed_core_mods)

    def test_unsupported_ezclient_target_falls_back_to_working_performance_profile(self) -> None:
        store = object.__new__(store_module.ProfileStore)
        store.settings = {"last_profile": ""}
        store.profiles = []

        profile = store.create_profile("Modern", "1.21.11", loader="Fabric", preset="ezclient")

        self.assertEqual("performance", profile.profile_type)
        self.assertNotIn("ezclient", profile.managed_core_mods)
        self.assertTrue({"sodium", "lithium", "iris"}.issubset(set(profile.managed_core_mods)))

    def test_duplicate_profile_names_match_unique_folder_names(self) -> None:
        store = object.__new__(store_module.ProfileStore)
        store.settings = {"last_profile": ""}
        store.profiles = []

        first = store.create_profile("Mein Profil", "26.2", preset="raw")
        second = store.create_profile("Mein Profil", "26.2", preset="raw")
        third = store.create_profile("Mein Profil", "26.2", preset="raw")

        self.assertEqual(("Mein Profil", "Mein Profil (2)", "Mein Profil (3)"),
                         (first.name, second.name, third.name))
        self.assertEqual((first.name, second.name, third.name),
                         (first.path.name, second.path.name, third.path.name))

    def test_profile_creation_never_reuses_a_stale_deleted_profile_directory(self) -> None:
        store = object.__new__(store_module.ProfileStore)
        store.settings = {"last_profile": ""}
        store.profiles = []
        stale = store_module.PROFILES_DIR / "Reused"
        stale.mkdir(parents=True, exist_ok=True)
        (stale / "mods").mkdir(exist_ok=True)
        (stale / "mods" / "old-incompatible.jar").write_bytes(b"old")

        profile = store.create_profile("Reused", "1.21.11", loader="Fabric", preset="performance")

        self.assertEqual("Reused (2)", profile.name)
        self.assertFalse((profile.mods_path / "old-incompatible.jar").exists())

    def test_legacy_profile_does_not_install_ezclient_core_or_modern_renderers(self) -> None:
        store = object.__new__(store_module.ProfileStore)
        store.settings = {"last_profile": ""}
        store.profiles = []

        profile = store.create_profile("Legacy", "1.8.9", loader="Fabric", preset="ezclient")

        self.assertNotIn("ezclient", [mod.slug for mod in profile.mods])
        self.assertNotIn("sodium", profile.managed_core_mods)
        self.assertNotIn("iris", profile.managed_core_mods)

    def test_sync_never_deletes_unknown_user_jar(self) -> None:
        profile = ProfileData(
            id="safe-sync", name="Safe Sync", minecraft_version="26.2", profile_type="raw",
            mods=[ModData("known", "known", "Known", "1", "1.0", "known.jar")],
            user_mods=["known"],
        )
        profile.mods_path.mkdir(parents=True, exist_ok=True)
        (profile.mods_path / "known.jar").write_bytes(b"k" * 2048)
        (profile.mods_path / "manually-added.jar").write_bytes(b"u" * 2048)

        sync_profile_mods(profile)

        self.assertTrue((profile.mods_path / "known.jar").exists())
        self.assertTrue((profile.mods_path / "manually-added.jar").exists())


if __name__ == "__main__":
    unittest.main()
