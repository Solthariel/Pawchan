"""Fail closed when a request skips, edits or batches releases after the published baseline."""
import json
from pathlib import Path
import re

SEMVER = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")


def identity(version):
    code, name = version["code"], version["name"]
    if type(code) is not int or code < 1 or SEMVER.fullmatch(name) is None:
        raise ValueError("Invalid version identity")
    return code, name


def read_history(root):
    path = Path(root) / "update/feed/history-v1.json"
    if not path.exists():
        return []
    history = json.loads(path.read_text(encoding="utf-8"))
    if history["schema"] != 1:
        raise ValueError("Unknown published history schema")
    return sorted(history["releases"], key=lambda v: v["code"])


def validate_sequence(versions, published):
    candidates = [identity(v) for v in versions]
    previous = [identity(v) for v in published]
    if len(set(candidates)) != len(candidates) or len({v[0] for v in candidates}) != len(candidates):
        raise ValueError("Duplicate release identities")
    if len({v[1] for v in candidates}) != len(candidates):
        raise ValueError("Duplicate release names")
    if len(candidates) != len(previous) + 1 or candidates[:-1] != previous:
        raise ValueError("Exactly one new release is required; published versions cannot be changed")
    if not previous:
        if candidates != [(1, "0.1.0")]:
            raise ValueError("Missing published baseline")
        return
    code, name = candidates[-1]
    old_code, old_name = previous[-1]
    major, minor, patch = map(int, SEMVER.fullmatch(old_name).groups())
    allowed = {f"{major}.{minor}.{patch + 1}", f"{major}.{minor + 1}.0", f"{major + 1}.0.0"}
    if code != old_code + 1 or name not in allowed:
        raise ValueError("Release must use the next versionCode and next patch, minor or major")


def validate_local(root):
    root = Path(root)
    directory = root / "update/publishing"
    versions = json.loads((directory / "versions.json").read_text(encoding="utf-8"))["versions"]
    published = read_history(root)
    validate_sequence(versions, published)
    packages = json.loads((directory / "manifest/data-v1.json").read_text(encoding="utf-8"))
    client = next(v for v in packages["applications"] if v["name"] == "client")
    if [(p["version_code"], p["version_name"]) for p in client["packages"]] != [identity(v) for v in versions]:
        raise ValueError("Manifest and version history differ")
    request = json.loads((directory / "release-request.json").read_text(encoding="utf-8"))
    if (request["code"], request["version"]) != identity(versions[-1]):
        raise ValueError("Request and version history differ")
    return published
