import hashlib
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from update_display_translations import pairs
from qa_game_translations import missing


def test_generated_catalogs_match_reviewed_source():
    folder = ROOT / "client_mod/src/main/resources/assets/ezclient/lang"
    de = json.loads((folder / "de_de.json").read_text(encoding="utf-8"))
    en = json.loads((folder / "en_us.json").read_text(encoding="utf-8"))
    assert de.keys() == en.keys()
    for source, translated in pairs().items():
        key = "ezclient.ui." + hashlib.sha1(source.encode("utf-8")).hexdigest()[:16]
        assert de[key] == source
        assert en[key] == translated
    js = (ROOT / "ui/DisplayText.js").read_text(encoding="utf-8")
    payload = js.split("var translations = ", 1)[1].split(";\nvar reverse", 1)[0]
    assert json.loads(payload) == pairs()


def test_all_declarative_module_help_and_labels_have_translations():
    # Brand names and terms identical in both languages are deliberately not translated.
    unchanged = {"Amazon Music", "Animation", "Apple Music", "Chat", "Cider", "Deezer", "Filter", "Format",
                 "Gold", "Items", "Lapis", "Overlay", "Performance", "Redstone", "Shulker", "SoundCloud",
                 "Spotify", "System", "Tidal", "Timer", "Wind", "YouTube"}
    assert set(missing()) - unchanged == set()
