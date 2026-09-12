import unittest
import tempfile
from pathlib import Path
from backend.services.server_nbt import get_recent_servers, clean_minecraft_formatting
from backend.services.store import ProfileStore
from backend.models.profile_model import ProfileModel
from backend.models.mod_model import ModModel
from backend.controllers.profile_controller import ProfileController


class TestRecentServers(unittest.TestCase):
    def test_strip_mc_formatting(self):
        self.assertEqual(clean_minecraft_formatting("§aHypixel §lNetwork"), "Hypixel Network")
        self.assertEqual(clean_minecraft_formatting("§c§lEzClient §rServer"), "EzClient Server")
        self.assertEqual(clean_minecraft_formatting("Normal Server"), "Normal Server")

    def test_get_recent_servers_missing_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            servers = get_recent_servers(Path(tmp))
            self.assertEqual(servers, [])

    def test_profile_controller_recent_servers_settings(self):
        store = ProfileStore()
        orig_show = store.settings.get("show_recent_servers_home", True)
        orig_hidden = store.settings.get("recent_servers_home_hidden", False)
        try:
            p_model = ProfileModel()
            m_model = ModModel()
            ctrl = ProfileController(store, p_model, m_model)

            ctrl.setShowRecentServersHome(True)
            self.assertTrue(ctrl.showRecentServersHome)
            ctrl.setShowRecentServersHome(False)
            self.assertFalse(ctrl.showRecentServersHome)

            ctrl.setRecentServersHomeHidden(False)
            self.assertFalse(ctrl.recentServersHomeHidden)
            ctrl.setRecentServersHomeHidden(True)
            self.assertTrue(ctrl.recentServersHomeHidden)
        finally:
            ctrl.setShowRecentServersHome(orig_show)
            ctrl.setRecentServersHomeHidden(orig_hidden)

    def test_quickplay_multiplayer_argument_decision(self):
        from backend.services.minecraft_versions import version_tuple
        # 26.x and 1.20+ must use --quickPlayMultiplayer
        self.assertGreaterEqual(version_tuple("26.2"), (1, 20))
        self.assertGreaterEqual(version_tuple("26.1"), (1, 20))
        self.assertGreaterEqual(version_tuple("1.21.1"), (1, 20))
        self.assertGreaterEqual(version_tuple("1.20.1"), (1, 20))
        # Legacy versions must use legacy --server
        self.assertLess(version_tuple("1.19.4"), (1, 20))
        self.assertLess(version_tuple("1.16.5"), (1, 20))
        self.assertLess(version_tuple("1.8.9"), (1, 20))


    def test_ad_or_dummy_filter(self):
        from backend.services.server_nbt import is_ad_or_dummy_server
        self.assertTrue(is_ad_or_dummy_server("advert.norisk.space"))
        self.assertTrue(is_ad_or_dummy_server("heroisland.net"))
        self.assertTrue(is_ad_or_dummy_server("localhost"))
        self.assertTrue(is_ad_or_dummy_server("127.0.0.1"))
        self.assertTrue(is_ad_or_dummy_server("some.server.net", name="ad"))
        self.assertTrue(is_ad_or_dummy_server("some.server.net", name="Minecraft Server"))
        self.assertTrue(is_ad_or_dummy_server("some.server.net", name="NoRisk.Host"))
        self.assertFalse(is_ad_or_dummy_server("cracky2.blueface.dev"))
        self.assertFalse(is_ad_or_dummy_server("play.hypixel.net", name="Hypixel Network"))

    def test_get_actually_played_servers_from_logs(self):
        from backend.services.server_nbt import get_actually_played_servers
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp)
            logs_dir = p / "logs"
            logs_dir.mkdir(parents=True)
            log_file = logs_dir / "latest.log"
            log_file.write_text(
                "[12:00:00] [INFO]: Starting Minecraft...\n"
                "[12:01:00] [INFO]: Connecting to advert.norisk.space, 25565\n"
                "[12:02:00] [INFO]: Connecting to test1.net, 25565\n"
                "[12:03:00] [INFO]: Connecting to test2.net, 25570\n"
            )
            played = get_actually_played_servers(p, limit=3)
            # Should be test2.net:25570 (most recent), test1.net, and advert.norisk.space should be filtered out!
            self.assertEqual(len(played), 2)
            self.assertEqual(played[0]["ip"], "test2.net:25570")
            self.assertEqual(played[1]["ip"], "test1.net")

    def test_custom_and_suggested_home_servers(self):
        store = ProfileStore()
        p_model = ProfileModel()
        m_model = ModModel()
        ctrl = ProfileController(store, p_model, m_model)

        orig_custom = list(store.settings.get("custom_home_servers", []))
        orig_hidden = list(store.settings.get("hidden_home_servers", []))
        orig_show_sug = store.settings.get("show_suggested_servers_home", True)

        try:
            # Clear state
            store.settings["custom_home_servers"] = []
            store.settings["hidden_home_servers"] = []
            store.settings["show_suggested_servers_home"] = True

            # Add multiple custom servers and test reordering
            ctrl.addCustomHomeServer("Server 1", "srv1.net")
            ctrl.addCustomHomeServer("Server 2", "srv2.net")
            ctrl.addCustomHomeServer("Server 3", "srv3.net")

            servers = ctrl.getHomeServers()
            self.assertEqual([s["ip"] for s in servers if s["is_custom"]], ["srv1.net", "srv2.net", "srv3.net"])
            self.assertTrue(servers[0]["is_first_custom"])
            self.assertFalse(servers[0]["is_last_custom"])
            self.assertTrue(servers[2]["is_last_custom"])

            # Move srv3 up by 1
            ok = ctrl.moveCustomHomeServer("srv3.net", -1)
            self.assertTrue(ok)
            servers = ctrl.getHomeServers()
            self.assertEqual([s["ip"] for s in servers if s["is_custom"]], ["srv1.net", "srv3.net", "srv2.net"])

            # Move srv1 down by 1
            ok = ctrl.moveCustomHomeServer("srv1.net", 1)
            self.assertTrue(ok)
            servers = ctrl.getHomeServers()
            self.assertEqual([s["ip"] for s in servers if s["is_custom"]], ["srv3.net", "srv1.net", "srv2.net"])

            # Cannot move first server up
            self.assertFalse(ctrl.moveCustomHomeServer("srv3.net", -1))
            # Cannot move last server down
            self.assertFalse(ctrl.moveCustomHomeServer("srv2.net", 1))

            # Test deduplication: if mock profile has servers.dat with "srv1.net"
            import tempfile
            from unittest.mock import patch, MagicMock
            mock_prof = MagicMock()
            mock_prof.path = Path(tempfile.gettempdir())
            mock_prof.id = "mock_prof_id"
            ctrl._active_profile = mock_prof

            fake_recent = [
                {"name": "Dupe Srv1", "ip": "srv1.net", "icon": ""},
                {"name": "Recent A", "ip": "recentA.net", "icon": ""},
                {"name": "Recent B", "ip": "recentB.net", "icon": ""},
                {"name": "Recent C", "ip": "recentC.net", "icon": ""},
                {"name": "Recent D", "ip": "recentD.net", "icon": ""},
            ]
            with patch("backend.services.server_nbt.get_actually_played_servers", return_value=fake_recent):
                servers = ctrl.getHomeServers()
                custom = [s for s in servers if s["is_custom"]]
                suggested = [s for s in servers if not s["is_custom"]]

                # Custom servers are at top
                self.assertEqual([s["ip"] for s in custom], ["srv3.net", "srv1.net", "srv2.net"])
                # Suggested servers: srv1.net must be skipped because it is already custom!
                # Max 3 suggested servers: recentA, recentB, recentC
                self.assertEqual(len(suggested), 3)
                self.assertEqual([s["ip"] for s in suggested], ["recentA.net", "recentB.net", "recentC.net"])

                # Hide recentA
                ctrl.removeHomeServer("recentA.net", is_custom=False)
                servers = ctrl.getHomeServers()
                suggested = [s for s in servers if not s["is_custom"]]
                self.assertEqual([s["ip"] for s in suggested], ["recentB.net", "recentC.net", "recentD.net"])

                # Disable suggestions completely
                ctrl.setShowSuggestedServersHome(False)
                self.assertFalse(ctrl.showSuggestedServersHome)
                servers = ctrl.getHomeServers()
                self.assertEqual(len([s for s in servers if not s["is_custom"]]), 0)
                self.assertEqual(len(servers), 3)

                # Check hasHiddenSuggestedServers
                self.assertTrue(ctrl.hasHiddenSuggestedServers)
                # Reset suggested servers
                ctrl.resetSuggestedHomeServers()
                self.assertFalse(ctrl.hasHiddenSuggestedServers)
                self.assertTrue(ctrl.showSuggestedServersHome)
        finally:
            store.settings["custom_home_servers"] = orig_custom
            store.settings["hidden_home_servers"] = orig_hidden
            store.settings["show_suggested_servers_home"] = orig_show_sug
            store.save()

    def test_inspected_can_install_ezclient(self):
        from backend.models.types import ProfileData
        store = ProfileStore()
        p_model = ProfileModel()
        m_model = ModModel()
        ctrl = ProfileController(store, p_model, m_model)

        # Profile with fabric 26.2 and vanilla type -> eligible
        p1 = ProfileData(id="p1", name="Prof1", minecraft_version="26.2", loader="Fabric", profile_type="custom")
        ctrl._inspected_profile = p1
        self.assertTrue(ctrl.inspectedCanInstallEzClient)

        # Profile already ezclient -> not eligible
        p2 = ProfileData(id="p2", name="Prof2", minecraft_version="26.2", loader="Fabric", profile_type="ezclient")
        ctrl._inspected_profile = p2
        self.assertFalse(ctrl.inspectedCanInstallEzClient)

        # Vanilla loader -> not eligible
        p3 = ProfileData(id="p3", name="Prof3", minecraft_version="26.2", loader="Vanilla", profile_type="custom")
        ctrl._inspected_profile = p3
        self.assertFalse(ctrl.inspectedCanInstallEzClient)

        # Legacy 1.20.1 -> not active 26.x ezclient version
        p4 = ProfileData(id="p4", name="Prof4", minecraft_version="1.20.1", loader="Fabric", profile_type="custom")
        ctrl._inspected_profile = p4
        self.assertFalse(ctrl.inspectedCanInstallEzClient)


if __name__ == "__main__":
    unittest.main()
