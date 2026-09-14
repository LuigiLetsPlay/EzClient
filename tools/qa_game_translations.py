"""List untranslated declarative module setting labels and help text."""
import json
import re
from update_display_translations import ROOT, pairs

def missing():
    reviewed = pairs()
    known = set(reviewed) | set(reviewed.values())
    for lang in ("de_de", "en_us"):
        known.update(json.loads((ROOT / f"client_mod/src/main/resources/assets/ezclient/lang/{lang}.json").read_text(encoding="utf-8")).values())
    found = set()
    literal = r'"((?:\\.|[^"\\])*)"'
    for path in (ROOT / "client_mod/src/main/java/app/ezclient/gui").glob("*Module.java"):
        source = path.read_text(encoding="utf-8")
        for match in re.finditer(r'\b(?:option|flag|colorOption)\(\s*' + literal + r'\s*,\s*' + literal + r'\s*,\s*' + literal + r'\s*,\s*' + literal + r'\s*,', source):
            for value in (match[1], match[3], match[4]):
                if value not in known:
                    found.add(value)
    return sorted(found)

if __name__ == "__main__":
    print("\n".join(missing()))
