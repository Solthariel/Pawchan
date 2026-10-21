"""Allowlisted, content-checked cosmetic fixes: never move tags or touch release assets."""
import hashlib
import json

from release_notes_format import current_notes, explicit_sections


def body_key(source):
    return source.replace("\r\n", "\n").strip()


def correct_published_notes(root, latest, api, run, apply=False):
    plan = root / "update/publishing/notes-corrections.json"
    if not plan.exists():
        return
    for entry in json.loads(plan.read_text(encoding="utf-8"))["corrections"]:
        if entry["publish_with"] != latest["name"]:
            continue
        target = {"name": entry["version"], "code": entry["code"]}
        tag = "v" + target["name"]
        desired = current_notes(root, target)
        release = api("releases/tags/" + tag)
        if release is None or release.get("tag_name") != tag or release.get("draft"):
            raise ValueError("Cannot repair an absent, draft or mismatched release")
        existing = body_key(release.get("body") or "")
        if existing == body_key(desired):
            continue
        digest = hashlib.sha256(existing.encode()).hexdigest()
        if digest != entry["expected_body_sha256"] or body_key(explicit_sections(existing)) != body_key(desired):
            raise ValueError("Published notes changed; refusing anything beyond the approved heading insertion")
        if apply:
            run("gh", "release", "edit", tag, "--repo", "Solthariel/Pawchan", "--notes-file",
                f"update/publishing/changelogs/ru/{target['code']}.txt")
