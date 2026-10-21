"""One explicit section format for APK assets, the feed and GitHub release bodies."""
import re

VERSION_HEADER = re.compile(r"^(?:Версия|Version)\s+\d+[.]\d+[.]\d+", re.IGNORECASE)
BULLETS = ("* ", "- ", "• ")


def explicit_sections(source):
    lines = source.splitlines(keepends=True)
    for index, raw in enumerate(lines):
        line = raw.strip().lstrip("\ufeff").lstrip("# ")
        if not line or VERSION_HEADER.match(line):
            continue
        if line.startswith(BULLETS):
            lines.insert(index, "Изменения\n\n")
        break
    return "".join(lines)


def validate_notes(source):
    if not source.strip() or not any(line.strip().startswith(BULLETS) and line.strip()[2:].strip()
                                    for line in source.splitlines()):
        raise ValueError("Release notes require nonempty list entries")
    if explicit_sections(source) != source:
        raise ValueError("Release notes require an explicit section heading before the first list")
    return source


def current_notes(root, latest):
    source = (root / f"update/publishing/changelogs/ru/{latest['code']}.txt").read_text(encoding="utf-8")
    return validate_notes(source)
