import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from backend.services.crash_doctor import CrashDoctorService
from backend.models.types import ModData, ProfileData


class TestCrashDoctor(unittest.TestCase):
    def setUp(self):
        self.doctor = CrashDoctorService()

    def test_fabric_sodium_iris_incompatible_log(self):
        raw_log = """Incompatible mods found!
net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!
A potential solution has been determined, this may resolve your problem:
	 - Replace mod 'Sodium' (sodium) 0.8.14+mc1.21.11 with any 0.8.x version that is compatible with:
		 - iris 1.10.7+mc1.21.11
More details:
	 - Mod 'Sodium' (sodium) 0.8.14+mc1.21.11 is incompatible with version 1.10.7 or earlier of mod 'Iris' (iris), yet a conflicting version is present: 1.10.7+mc1.21.11!
	at net.fabricmc.loader.impl.FormattedException.ofLocalized(FormattedException.java:51)
	at net.fabricmc.loader.impl.FabricLoaderImpl.load(FabricLoaderImpl.java:202)
	at net.fabricmc.loader.impl.launch.knot.Knot.init(Knot.java:142)
	at net.fabricmc.loader.impl.launch.knot.Knot.launch(Knot.java:66)
	at net.fabricmc.loader.impl.launch.knot.KnotClient.main(KnotClient.java:23)"""

        diagnosis = self.doctor.analyze(raw_log, "Prozess wurde mit Fehlercode 1 beendet.")
        self.assertTrue(diagnosis.has_solution)
        self.assertTrue(diagnosis.can_auto_fix)
        self.assertEqual(diagnosis.action_type, "FIX_MOD_INCOMPATIBILITY")
        self.assertIn("sodium", diagnosis.action_data.get("primary_mod", "").lower())
        self.assertIn("iris", diagnosis.action_data.get("conflicting_mod", "").lower())
        self.assertIn("Sodium", diagnosis.problem_title)
        self.assertIn("Iris", diagnosis.problem_title)

    def test_missing_dependency_log(self):
        raw_log = """net.fabricmc.loader.impl.discovery.ModResolutionException: Mod 'YetAnotherConfigLib' (yet_another_config_lib_v3) requires mod 'Cloth Config' (cloth-config), which is missing!"""
        diagnosis = self.doctor.analyze(raw_log)
        self.assertTrue(diagnosis.has_solution)
        self.assertEqual(diagnosis.action_type, "INSTALL_DEPENDENCY")
        self.assertEqual(diagnosis.action_data.get("dep_id"), "cloth-config")

    def test_missing_dependency_format2(self):
        raw_log = """Could not find required mod: zoomify requires {fabric-api @ [*]}"""
        diagnosis = self.doctor.analyze(raw_log)
        self.assertTrue(diagnosis.has_solution)
        self.assertEqual(diagnosis.action_type, "INSTALL_DEPENDENCY")
        self.assertEqual(diagnosis.action_data.get("dep_id"), "fabric-api")

    def test_out_of_memory_log(self):
        raw_log = """Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	at java.base/java.util.Arrays.copyOf(Arrays.java:3512)"""
        diagnosis = self.doctor.analyze(raw_log)
        self.assertTrue(diagnosis.has_solution)
        self.assertEqual(diagnosis.action_type, "INCREASE_RAM")
        self.assertTrue(diagnosis.can_auto_fix)

    def test_duplicate_mods_log(self):
        raw_log = """Duplicate mod IDs: duplicate mod 'sodium' found in files 'sodium-0.5.8.jar' and 'sodium-0.6.0.jar'"""
        diagnosis = self.doctor.analyze(raw_log)
        self.assertTrue(diagnosis.has_solution)
        self.assertEqual(diagnosis.action_type, "REMOVE_DUPLICATES")
        self.assertEqual(diagnosis.action_data.get("mod_id"), "sodium")


    def test_fabric_timestamped_log(self):
        raw_log = """[17:25:25] [main/INFO]: Loading Minecraft 1.21.11 with Fabric Loader 0.19.3
[17:25:26] [main/WARN]: Mod resolution failed
[17:25:26] [main/INFO]: Immediate reason: [HARD_DEP iris 1.10.7+mc1.21.11 {depends sodium @ [0.8.x]}, NEG_HARD_DEP sodium 0.8.14+mc1.21.11 {breaks iris @ [<=1.10.7]}, ROOT_FORCELOAD_SINGLE iris 1.10.7+mc1.21.11]
[17:25:26] [main/INFO]: Reason: [NEG_HARD_DEP sodium 0.8.14+mc1.21.11 {breaks iris @ [<=1.10.7]}]
[17:25:26] [main/INFO]: Fix: add [], remove [], replace [[sodium 0.8.14+mc1.21.11] -> add:sodium 0.8- ([[0.8-,0.9-)])]
[17:25:26] [main/ERROR]: Incompatible mods found!
net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!
A potential solution has been determined, this may resolve your problem:
	 - Replace mod 'Sodium' (sodium) 0.8.14+mc1.21.11 with any 0.8.x version that is compatible with:
		 - iris 1.10.7+mc1.21.11
More details:
	 - Mod 'Sodium' (sodium) 0.8.14+mc1.21.11 is incompatible with version 1.10.7 or earlier of mod 'Iris' (iris), yet a conflicting version is present: 1.10.7+mc1.21.11!
	at net.fabricmc.loader.impl.FormattedException.ofLocalized(FormattedException.java:51)
	at net.fabricmc.loader.impl.FabricLoaderImpl.load(FabricLoaderImpl.java:202)
	at net.fabricmc.loader.impl.launch.knot.Knot.init(Knot.java:142)
	at net.fabricmc.loader.impl.launch.knot.Knot.launch(Knot.java:66)
	at net.fabricmc.loader.impl.launch.knot.KnotClient.main(KnotClient.java:23)"""

        diagnosis = self.doctor.analyze(raw_log)
        self.assertTrue(diagnosis.has_solution)
        self.assertEqual(diagnosis.action_type, "FIX_MOD_INCOMPATIBILITY")
        self.assertEqual(diagnosis.action_data.get("primary_mod").lower(), "sodium")
        self.assertEqual(diagnosis.action_data.get("conflicting_mod").lower(), "iris")

    def test_wrong_minecraft_and_java_version_uses_the_reported_mod(self):
        raw_log = """Incompatible mods found!
 - Replace mod 'Simple Voice Chat' (voicechat) 2.6.22+26.2 with any version that is compatible with:
     - minecraft 1.21.11
     - java 21
More details:
 - Mod 'Simple Voice Chat' (voicechat) 2.6.22+26.2 requires any 26.2.x version of 'Minecraft' (minecraft), but only the wrong version is present: 1.21.11!
 - Mod 'Simple Voice Chat' (voicechat) 2.6.22+26.2 requires version 25 or later of 'OpenJDK 64-Bit Server VM' (java), but only the wrong version is present: 21!"""
        profile = ProfileData(id="test", name="Test", minecraft_version="1.21.11", loader="Fabric")
        self.doctor._modrinth.get_project_versions = lambda *args, **kwargs: [{"version_number": "2.6.9"}]

        diagnosis = self.doctor.analyze(raw_log, profile=profile)

        self.assertTrue(diagnosis.has_solution)
        self.assertTrue(diagnosis.can_auto_fix)
        self.assertEqual(diagnosis.action_type, "FIX_MOD_INCOMPATIBILITY")
        self.assertEqual(diagnosis.action_data.get("primary_mod"), "voicechat")
        self.assertEqual(diagnosis.action_data.get("conflicting_mod"), "")

    def test_wrong_version_reports_no_solution_when_no_compatible_build_exists(self):
        raw_log = """Mod 'Simple Voice Chat' (voicechat) 2.6.22+26.2 requires any 26.2.x version of 'Minecraft' (minecraft), but only the wrong version is present: 1.21.11!"""
        profile = ProfileData(id="test", name="Test", minecraft_version="1.21.11", loader="Fabric")
        self.doctor._modrinth.get_project_versions = lambda *args, **kwargs: []
        self.doctor._curseforge.get_project_versions = lambda *args, **kwargs: []

        diagnosis = self.doctor.analyze(raw_log, profile=profile)

        self.assertFalse(diagnosis.has_solution)
        self.assertFalse(diagnosis.can_auto_fix)
        self.assertEqual(diagnosis.action_type, "NONE")

    def test_incompatible_ezclient_mixin_is_really_removed(self):
        log = "Mixin apply for mod ezclient failed ezclient.v1_16_v1_20.mixins.json:AbstractClientPlayerMixin"
        with tempfile.TemporaryDirectory() as directory, patch("backend.models.types.PROFILES_DIR", Path(directory)):
            profile = ProfileData(
                id="broken", name="Broken", minecraft_version="1.21.11", loader="Fabric",
                profile_type="ezclient", managed_core_mods=["ezclient"], integrated_mods=["ezclient"],
                mods=[ModData("ezclient", "ezclient", "EzClient Core", "v", "2.0.0", "EzClient-2.0.0+1.21.11.jar")],
            )
            profile.mods_path.mkdir(parents=True)
            (profile.mods_path / profile.mods[0].filename).write_bytes(b"broken")
            diagnosis = self.doctor.analyze(log, profile=profile)
            self.assertEqual("REMOVE_INCOMPATIBLE_EZCLIENT", diagnosis.action_type)
            success, _ = self.doctor.apply_fix(profile, diagnosis)
            self.assertTrue(success)
            self.assertEqual("raw", profile.profile_type)
            self.assertEqual([], profile.mods)
            self.assertFalse(any(profile.mods_path.glob("*EzClient*.jar")))


if __name__ == "__main__":
    unittest.main()
