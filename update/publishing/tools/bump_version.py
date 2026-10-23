#!/usr/bin/env python3
"""Create the next SemVer release and its sequential Android versionCode."""

import argparse
from datetime import date
import json
from pathlib import Path
import re

from release_sequence import identity, read_history
from release_notes_format import explicit_sections, validate_notes

SEMVER = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")
MAX_VERSION_CODE = 2_100_000_000


def next_version(name, change):
    match = SEMVER.fullmatch(name)
    if match is None:
        raise ValueError("Version must use MAJOR.MINOR.PATCH without leading zeroes")
    major, minor, patch = map(int, match.groups())
    if change == "major":
        return f"{major + 1}.0.0"
    if change == "minor":
        return f"{major}.{minor + 1}.0"
    if change == "patch":
        return f"{major}.{minor}.{patch + 1}"
    raise ValueError("Change must be major, minor or patch")


def prepare_release(project, change, notes, release_date):
    publishing = project / "update" / "publishing"
    versions_path = publishing / "versions.json"
    manifest_path = publishing / "manifest" / "data-v1.json"
    metadata = json.loads(versions_path.read_text(encoding="utf-8"))
    versions = metadata["versions"]
    if not versions or any(type(v["code"]) is not int for v in versions):
        raise ValueError("Release history must contain integer version codes")
    codes = [v["code"] for v in versions]
    if codes != sorted(set(codes)) or codes[0] < 1:
        raise ValueError("Version codes must be positive, unique and increasing")
    for version in versions:
        if SEMVER.fullmatch(version["name"]) is None:
            raise ValueError("Release history contains an invalid SemVer name")
    published = read_history(project)
    if [identity(v) for v in versions] != [identity(v) for v in published]:
        raise ValueError("Publish the pending release before preparing another version")
    previous = versions[-1]
    name = next_version(previous["name"], change)
    code = previous["code"] + 1
    if code > MAX_VERSION_CODE:
        raise ValueError("Android versionCode limit reached")
    notes = validate_notes(explicit_sections(notes.strip()))
    changelog = publishing / "changelogs" / "ru" / f"{code}.txt"
    if changelog.exists():
        raise ValueError(f"Changelog already exists: {changelog.name}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    client = next(item for item in manifest["applications"] if item["name"] == "client")
    previous_package = max(client["packages"], key=lambda item: item["version_code"])
    if (previous_package["version_code"], previous_package["version_name"]) != (previous["code"], previous["name"]):
        raise ValueError("Release history and update manifest disagree")
    package = dict(previous_package)
    package.update(
        title=f"Pawchan {name}", version_name=name, version_code=code,
        length=0, source=None, changelog=None, sha256sum=None,
    )
    client["packages"].append(package)
    versions.append({"code": code, "name": name, "date": release_date.strftime("%d.%m.%Y")})
    text = f"Версия {name}\n\n{notes.strip()}\n"
    # All validation precedes writes; never change an older release's notes or package.
    changelog.write_text(text, encoding="utf-8", newline="\n")
    (project / "app/src/main/assets/settings-changelog.txt").write_text(text, encoding="utf-8", newline="\n")
    versions_path.write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    (publishing / "release-request.json").write_text(
        json.dumps({"version": name, "code": code, "revision": 1}, indent=2) + "\n", encoding="utf-8", newline="\n")
    return name, code


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("change", choices=("major", "minor", "patch"))
    parser.add_argument("--notes", required=True, type=Path, help="UTF-8 file with actual release notes")
    parser.add_argument("--date", type=date.fromisoformat, default=date.today(), help="YYYY-MM-DD")
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[3]
    try:
        name, code = prepare_release(project, args.change, args.notes.read_text(encoding="utf-8"), args.date)
    except (ValueError, OSError, KeyError, StopIteration) as error:
        parser.exit(1, f"Cannot prepare release: {error}\n")
    print(f"Prepared Pawchan {name}, release #{code}. Build and verify before publishing.")


if __name__ == "__main__":
    main()
