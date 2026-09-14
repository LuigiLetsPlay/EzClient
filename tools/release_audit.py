"""Read-only inventories used for the 2.2.0 release review."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
STRING = r'"((?:\\.|[^"\\])*)"'


def inventory():
    result = {"qml": {}, "java": {}}
    for path in (ROOT / "ui").rglob("*.qml"):
        if path.name == "EzI18n.qml":
            continue
        for line in path.read_text(encoding="utf-8").splitlines():
            if not re.search(r'\b(text|title|placeholderText|label|description)\s*:', line):
                continue
            if "EzI18n.t(" in line or "currentLanguage" in line:
                continue
            for text in re.findall(STRING, line.split("//")[0]):
                if re.search(r'[A-Za-zÄÖÜäöüß]{3}', text) and not re.search(r'^(#|qrc:|https?:|.*\.(png|svg|jpg|qml)$)', text):
                    result["qml"].setdefault(text, []).append(str(path.relative_to(ROOT)))
    for path in (ROOT / "client_mod/src/main/java/app/ezclient/gui").glob("*.java"):
        for line in path.read_text(encoding="utf-8").splitlines():
            if not any(token in line for token in ("Component.literal(", ".text(", "option(", "flag(", "colorOption(", "return \"", "String[]")):
                continue
            for text in re.findall(STRING, line):
                if re.search(r'[A-Za-zÄÖÜäöüß]{3}', text):
                    result["java"].setdefault(text, []).append(str(path.relative_to(ROOT)))
    return result


if __name__ == "__main__":
    output = ROOT / "build/release-qa"
    output.mkdir(parents=True, exist_ok=True)
    result = inventory()
    (output / "text-inventory.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    for group, entries in result.items():
        (output / f"{group}-strings.txt").write_text("\n".join(sorted(entries)), encoding="utf-8")
        print(group, len(entries), "candidate display strings")
