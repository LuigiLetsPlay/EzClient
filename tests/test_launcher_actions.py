from types import SimpleNamespace
from unittest.mock import Mock

from backend.controllers.profile_controller import ProfileController
from backend.models.types import ProfileData


def test_install_handler_exposes_both_qml_signatures():
    meta = ProfileController.staticMetaObject
    for count in (7, 8):
        signature = "installMod(" + ",".join(["QString"] * count) + ")"
        assert meta.indexOfSlot(signature) >= 0


def test_select_profile_updates_backend_and_both_views():
    target = ProfileData(id="target", name="Target", minecraft_version="26.2")
    store = SimpleNamespace(get_by_id=lambda ident: target if ident == "target" else None,
                            settings={}, save=Mock())
    controller = SimpleNamespace(_store=store, _active_profile=None, _inspected_profile=None,
                                 _sync_models=Mock(), inspectedProfileChanged=Mock())
    ProfileController.selectProfile(controller, "target")
    assert controller._active_profile is target
    assert controller._inspected_profile is target
    assert store.settings["last_profile"] == "target"
    controller._sync_models.assert_called_once()
    controller.inspectedProfileChanged.emit.assert_called_once()


def test_delete_pack_removes_inspected_file_and_notifies(tmp_path, monkeypatch):
    profile = ProfileData(id="pack", name="Pack", minecraft_version="26.2")
    monkeypatch.setattr(ProfileData, "path", property(lambda self: tmp_path))
    directory = tmp_path / "resourcepacks"
    directory.mkdir()
    target = directory / "test.zip.disabled"
    target.write_bytes(b"pack")
    controller = SimpleNamespace(_inspected_profile=profile, _active_profile=None,
                                 _store=SimpleNamespace(save=Mock()), _sync_models=Mock(),
                                 inspectedProfileChanged=Mock(), _report_exception=Mock())
    ProfileController.deletePack(controller, "resourcepacks", target.name)
    assert not target.exists()
    controller.inspectedProfileChanged.emit.assert_called_once()
    controller._report_exception.assert_not_called()
    outside = tmp_path / "outside.zip"
    outside.write_bytes(b"keep")
    ProfileController.deletePack(controller, "resourcepacks", "../outside.zip")
    assert outside.exists()


def test_crash_report_contains_complete_traceback():
    controller = SimpleNamespace(analyzeCrash=Mock(), gameCrashed=Mock())
    try:
        raise RuntimeError("import failure")
    except RuntimeError:
        ProfileController._report_exception(controller, "Import")
    log = controller.analyzeCrash.call_args.args[0]
    assert "Traceback (most recent call last)" in log
    assert "test_crash_report_contains_complete_traceback" in log
    assert "RuntimeError: import failure" in log
    assert controller.gameCrashed.emit.call_args.args == ("Import", "RuntimeError: import failure", log)
