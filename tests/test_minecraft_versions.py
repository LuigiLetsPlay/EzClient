import unittest

from backend.models.types import APP_VERSION
from backend.services.minecraft_versions import FROZEN_EZCLIENT_VERSION, asset_name, catalog, required_java
from backend.services.store import ezclient_asset_name


class MinecraftVersionCatalogTests(unittest.TestCase):
    def test_java_runtime_boundaries(self):
        self.assertEqual(8, required_java("1.8.9"))
        self.assertEqual(16, required_java("1.17.1"))
        self.assertEqual(17, required_java("1.18"))
        self.assertEqual(17, required_java("1.20.4"))
        self.assertEqual(21, required_java("1.20.5"))
        self.assertEqual(21, required_java("1.21.11"))
        self.assertEqual(25, required_java("26.1"))
        self.assertEqual(25, required_java("26.2"))

    def test_versioned_asset_name(self):
        self.assertEqual(f"EzClient-{APP_VERSION}+26.2.jar", asset_name("26.2"))

    def test_26x_never_falls_back_to_a_different_jar(self):
        self.assertEqual(f"EzClient-{APP_VERSION}+26.1.jar", ezclient_asset_name("26.1"))
        self.assertEqual(f"EzClient-{APP_VERSION}+26.1.1.jar", ezclient_asset_name("26.1.1"))
        self.assertEqual(f"EzClient-{APP_VERSION}+26.2.jar", ezclient_asset_name("26.2"))

    def test_non_26x_builds_are_not_marked_as_ezclient(self):
        families = catalog(lambda _: True)
        for family in families:
            for release in family["releases"]:
                if release["version"].startswith("26."):
                    self.assertTrue(release["hasEzClient"])
                    self.assertFalse(release["isFrozen"])
                    self.assertEqual("current", release["supportStatus"])
                else:
                    self.assertFalse(release["hasEzClient"])
                    self.assertFalse(release["isFrozen"])
                    self.assertEqual("none", release["supportStatus"])

    def test_catalog_marks_only_existing_assets(self):
        families = catalog(lambda filename: filename.endswith("+26.2.jar"))
        releases = [release for family in families for release in family["releases"]]
        self.assertTrue(next(item for item in releases if item["version"] == "26.2")["hasEzClient"])
        self.assertFalse(next(item for item in releases if item["version"] == "26.1")["hasEzClient"])
        self.assertFalse(next(item for item in releases if item["version"] == "1.21.11")["hasEzClient"])

    def test_legacy_fabric_is_offered_for_classic_versions(self):
        releases = {
            release["version"]: release
            for family in catalog(lambda _: False)
            for release in family["releases"]
        }
        self.assertTrue(releases["1.8.9"]["hasFabric"])
        self.assertTrue(releases["1.12.2"]["hasFabric"])
        self.assertTrue(releases["1.8.8"]["hasFabric"])
        self.assertTrue(releases["1.10.2"]["hasFabric"])


if __name__ == "__main__":
    unittest.main()
