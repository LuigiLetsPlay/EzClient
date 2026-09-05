import tempfile
import unittest
from pathlib import Path

from backend.services.live_log_service import LiveLogService


class FakeProcess:
    def __init__(self, pid):
        self.pid = pid
        self.terminated = False

    def terminate(self):
        self.terminated = True

    def kill(self):
        self.terminated = True


class LiveLogServiceTests(unittest.TestCase):
    def test_instances_keep_separate_processes_and_logs(self):
        service = LiveLogService()
        with tempfile.TemporaryDirectory() as tmp:
            first = FakeProcess(101)
            second = FakeProcess(202)
            first_id = service.attach_process(first, Path(tmp) / "first.log", "PVP", "Legacy 1.8.9", "Luigi")
            second_id = service.attach_process(second, Path(tmp) / "second.log", "PVP", "Fabric 26.2", "Alex")
            service._process_log_line(first_id, "[12:00:00] [INFO]: first")
            service._process_log_line(second_id, "[12:00:01] [WARN]: second")

            self.assertEqual(service.runningCount, 2)
            self.assertEqual(service.selectedInstanceId, second_id)
            self.assertIn("second", service.getAllLogsText())
            self.assertNotIn("first", service.getAllLogsText())

            self.assertTrue(service.selectInstance(first_id))
            self.assertIn("first", service.getAllLogsText())
            service.stopInstance(first_id)
            self.assertTrue(first.terminated)
            self.assertTrue(service.was_intentionally_stopped(first_id))
            self.assertEqual(service.runningCount, 1)

    def test_bootstrap_messages_are_isolated_before_process_attach(self):
        service = LiveLogService()
        with tempfile.TemporaryDirectory() as tmp:
            first_id = service.begin_instance(Path(tmp) / "one.log", "One", "Fabric", "A", tmp)
            service.append_system_message("only one", instance_id=first_id)
            second_id = service.begin_instance(Path(tmp) / "two.log", "Two", "Fabric", "B", tmp)
            service.append_system_message("only two", instance_id=second_id)
            service.attach_process(FakeProcess(303), Path(tmp) / "one.log", "One", "Fabric", "A", tmp, first_id)
            service.attach_process(FakeProcess(404), Path(tmp) / "two.log", "Two", "Fabric", "B", tmp, second_id)
            service.selectInstance(first_id)
            self.assertIn("only one", service.getAllLogsText())
            self.assertNotIn("only two", service.getAllLogsText())


if __name__ == "__main__":
    unittest.main()
