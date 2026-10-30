"""Canonical text shared by APK assets, the update feed and GitHub releases."""
import re

VERSION_HEADER = re.compile(r"^(?:Версия|Version)\s+\d+[.]\d+[.]\d+$", re.IGNORECASE)
BULLETS = ("* ", "- ", "• ")
SECTION_ORDER = ("Важно", "Изменения", "Исправления", "Тесты")
ALIASES = {name: name for name in SECTION_ORDER}
ALIASES.update(dict.fromkeys(("Первый выпуск", "Улучшения", "Новое", "Обновлено", "Внутренние изменения"), "Изменения"))
ALIASES["Исправлено"] = "Исправления"
ALIASES.update(dict.fromkeys(("Проверки", "Безопасность тестирования", "Проверки и публикация"), "Тесты"))


def explicit_sections(source):
    """Normalize structure, never infer facts or reclassify individual entries."""
    version = None
    current = None
    groups = {name: [] for name in SECTION_ORDER}
    pending_heading = None
    for raw in source.lstrip("\ufeff").splitlines():
        line = raw.strip()
        if not line:
            continue
        heading = line.lstrip("# ")
        if VERSION_HEADER.fullmatch(heading):
            if version is not None or current is not None:
                raise ValueError("Release notes require one version header at the beginning")
            version = "Версия " + heading.split()[-1]
        elif line.startswith(BULLETS):
            text = line[2:].strip()
            if not text:
                raise ValueError("Release notes require nonempty list entries")
            current = current or "Изменения"
            groups[current].append(text)
            pending_heading = None
        elif heading in ALIASES:
            if pending_heading:
                raise ValueError("Release notes contain an empty section: " + pending_heading)
            current = ALIASES[heading]
            pending_heading = current
        else:
            raise ValueError("Unknown release notes heading or non-list text: " + line)
    if pending_heading or not any(groups.values()):
        raise ValueError("Release notes require nonempty list entries in every section")
    sections = ([version] if version else [])
    for name in SECTION_ORDER:
        if groups[name]:
            sections.append(name + "\n\n" + "\n".join("- " + text for text in groups[name]))
    return "\n\n".join(sections) + "\n"


def validate_notes(source):
    canonical = explicit_sections(source)
    if canonical.strip() != source.strip():
        raise ValueError("Release notes require canonical headings, bullets and an explicit section before the first list")
    return source


def current_notes(root, latest):
    source = (root / f"update/publishing/changelogs/ru/{latest['code']}.txt").read_text(encoding="utf-8")
    return validate_notes(source)
