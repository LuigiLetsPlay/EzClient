"""Apply reviewed translations only to QML presentation bindings, never saved IDs."""
import json
import re
from pathlib import Path
from update_display_translations import ROOT, pairs

STRINGS = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'')
BINDING = re.compile(r'\b(?:text|title|placeholderText|label|description)\s*:\s*')


def expression_end(source, start):
    stack = []
    i = start
    while i < len(source):
        c = source[i]
        if c in ('"', "'"):
            match = STRINGS.match(source, i)
            if match:
                i = match.end()
                continue
        if c in "([{":
            stack.append(c)
        elif c in ")]}":
            if not stack:
                return i
            stack.pop()
        elif not stack and c in ";\n":
            return i
        i += 1
    return i


def localize_qml(source, known):
    replacements = {}
    for binding in BINDING.finditer(source):
        end = expression_end(source, binding.end())
        for literal in STRINGS.finditer(source, binding.end(), end):
            raw = literal.group()
            try:
                value = json.loads(raw) if raw.startswith('"') else raw[1:-1]
            except ValueError:
                continue
            if value not in known:
                continue
            before = source[max(binding.end(), literal.start()-100):literal.start()]
            after = source[literal.end():min(end, literal.end()+24)]
            # Existing localization calls, comparisons and technical keys are not display literals.
            if re.search(r'EzI18n\.(?:text|t)\([^)]*$', before):
                continue
            if re.search(r'(?:[=!]=+|\bcase)\s*$', before) or re.match(r'\s*[=!]=+', after):
                continue
            replacements[literal.start()] = (literal.end(), f"EzI18n.text({raw})")
    for start, (end, replacement) in sorted(replacements.items(), reverse=True):
        source = source[:start] + replacement + source[end:]
    return source, len(replacements)


if __name__ == "__main__":
    reviewed = pairs()
    known = set(reviewed) | set(reviewed.values())
    total = 0
    for path in (ROOT / "ui").rglob("*.qml"):
        if path.name == "EzI18n.qml":
            continue
        source = path.read_text(encoding="utf-8")
        updated, count = localize_qml(source, known)
        if count:
            path.write_text(updated, encoding="utf-8")
            print(path.relative_to(ROOT), count)
            total += count
    print("Localized", total, "QML display literals")
    total = 0
    # Only literal arguments in presentation calls. Option IDs and persisted choices stay intact.
    for path in (ROOT / "client_mod/src/main/java/app/ezclient/gui").glob("*.java"):
        source = path.read_text(encoding="utf-8")
        original = source
        for name in ("hotkeyLabel", "sbHkText", "hkText"):
            source = source.replace(f"Component.literal({name})", f"Component.literal(app.ezclient.util.EzI18n.text({name}))")
        replacements = {}
        for call in re.finditer(r'(?:Component\.literal|\b(?:g|graphics)\.(?:text|centeredText))\(', source):
            end = expression_end(source, call.end())
            for literal in STRINGS.finditer(source, call.end(), end):
                if not literal.group().startswith('"'):
                    continue
                try:
                    value = json.loads(literal.group())
                except ValueError:
                    continue
                before = source[max(call.end(), literal.start()-100):literal.start()]
                if value not in known or re.search(r'EzI18n\.(?:text|get|comp)\w*\([^)]*$', before):
                    continue
                if re.search(r'(?:\.equals\(|[=!]=+)\s*$', before):
                    continue
                replacements[literal.start()] = (literal.end(), 'app.ezclient.util.EzI18n.text(' + literal.group() + ')')
        for start, (end, replacement) in sorted(replacements.items(), reverse=True):
            source = source[:start] + replacement + source[end:]
        if source != original:
            path.write_text(source, encoding="utf-8")
            total += len(replacements)
            print(path.name, len(replacements))
    print("Localized", total, "Java display literals")
