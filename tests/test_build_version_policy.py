import unittest
from unittest.mock import patch

from client_mod.build_mod import CURRENT_TARGETS, FROZEN_EZCLIENT_VERSION, FROZEN_TARGETS, artifact_version, build_targets


class BuildVersionPolicyTests(unittest.TestCase):
    def test_normal_build_selects_only_current_26x_targets(self):
        self.assertEqual(CURRENT_TARGETS, build_targets())
        self.assertEqual(("26.1", "26.1.1", "26.2"), CURRENT_TARGETS)

    def test_legacy_frozen_targets_are_retired(self):
        self.assertEqual((), FROZEN_TARGETS)
        self.assertEqual(CURRENT_TARGETS, build_targets(include_frozen=True))
        self.assertEqual((), build_targets(frozen_only=True))

    def test_26x_targets_follow_current_product_version(self):
        with patch("client_mod.build_mod.project_version", return_value="9.9.9"):
            for target in ("26.1", "26.1.1", "26.2"):
                self.assertEqual("9.9.9", artifact_version(target))


if __name__ == "__main__":
    unittest.main()
