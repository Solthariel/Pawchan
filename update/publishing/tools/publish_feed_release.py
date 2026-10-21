#!/usr/bin/env python3
"""Publish APK assets and the update feed; replacement is limited to the initial release."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import tempfile
import urllib.error
import urllib.request

from release_sequence import identity, read_history, validate_local, validate_sequence

ROOT = Path(__file__).resolve().parents[3]
REPO = "Solthariel/Pawchan"


def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()


def api(path):
    request = urllib.request.Request(
        f"https://api.github.com/repos/{REPO}/{path}",
        headers={"Authorization": "Bearer " + os.environ["GH_TOKEN"],
                 "Accept": "application/vnd.github+json", "User-Agent": "Pawchan-release"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def context():
    if os.environ.get("GITHUB_REPOSITORY") != REPO:
        raise SystemExit("Unexpected repository")
    versions = json.loads((ROOT / "update/publishing/versions.json").read_text())["versions"]
    latest = max(versions, key=lambda item: item["code"])
    tag = "v" + latest["name"]
    replacement = None
    ref = os.environ["GITHUB_REF"]
    if ref == "refs/heads/master":
        request = json.loads((ROOT / "update/publishing/release-request.json").read_text())
        if (request["version"], request["code"]) != (latest["name"], latest["code"]):
            raise SystemExit("Request and version metadata differ")
        replacement = request.get("replace_first_release")
        if replacement and (latest["name"], latest["code"]) != ("0.1.0", 1):
            raise SystemExit("Only the initial 0.1.0 release may be replaced")
    elif ref != "refs/tags/" + tag:
        raise SystemExit("Tag and version metadata differ")
    return latest, tag, replacement


def check(latest, tag, replacement):
    release = api("releases/tags/" + tag)
    reference = api("git/ref/tags/" + tag)
    if replacement:
        if release and release["id"] != replacement["release_id"]:
            raise SystemExit("The original release changed. Refusing to delete a different release.")
        if (not reference or reference["object"]["type"] != "commit"
                or reference["object"]["sha"] != replacement["tag_commit"]):
            raise SystemExit("The original tag changed. Refusing to move it.")
    else:
        if release:
            raise SystemExit("Release already exists. Published APKs are not overwritten.")
        if reference and reference["object"]["sha"] != os.environ["GITHUB_SHA"]:
            raise SystemExit("Tag points to another commit")
    try:
        published = validate_local(ROOT)
    except (ValueError, KeyError, OSError, StopIteration) as error:
        raise SystemExit(f"Release sequence rejected: {error}") from error
    public_latest = api("releases/latest")
    expected_tag = "v" + published[-1]["name"] if published else None
    if (public_latest or {}).get("tag_name") != expected_tag:
        raise SystemExit("Latest public release and published baseline differ; repair the feed before publishing")
    branch = api("branches/master")
    if branch is None:
        raise SystemExit("master is unavailable")
    print("Publication preflight passed:", tag)


def prepare(latest, tag):
    apk = ROOT / "dist" / f"Pawchan-{latest['name']}.apk"
    if not apk.is_file():
        raise SystemExit("Signed APK is missing")
    source = f"https://github.com/{REPO}/releases/download/{tag}/{apk.name}"
    changelog = f"https://raw.githubusercontent.com/{REPO}/master/update/feed/changelogs/{latest['code']}.txt"
    run("python3", "update/publishing/tools/update_manifest.py", str(apk), "--publish",
        "--source", source, "--changelog", changelog)
    manifest = (ROOT / "update/publishing/manifest/data-v1.json").read_bytes()
    notes = (ROOT / f"update/publishing/changelogs/ru/{latest['code']}.txt").read_bytes()
    return apk, manifest, notes


def publish_release(latest, tag, replacement, apk):
    # The workflow has already completed all build, signature and checksum checks.
    check(latest, tag, replacement)
    sha = os.environ["GITHUB_SHA"]
    if replacement:
        if api("releases/tags/" + tag) is not None:
            run("gh", "release", "delete", tag, "--repo", REPO, "--yes")
        run("gh", "api", "--method", "PATCH", f"repos/{REPO}/git/refs/tags/{tag}",
            "-f", "sha=" + sha, "-F", "force=true")
    # Draft first: upload both assets before making the new release public.
    run("gh", "release", "create", tag, str(apk), str(apk) + ".sha256",
        "--repo", REPO, "--target", sha, "--title", "Pawchan " + latest["name"],
        "--notes-file", f"update/publishing/changelogs/ru/{latest['code']}.txt", "--draft")
    run("gh", "release", "edit", tag, "--repo", REPO, "--draft=false", "--latest")


def publish_feed(latest, manifest, notes):
    # An isolated worktree keeps generated build metadata out of the feed commit.
    run("gh", "auth", "setup-git", "--hostname", "github.com")
    run("git", "fetch", "origin", "master")
    with tempfile.TemporaryDirectory(prefix="pawchan-feed-") as directory:
        worktree = Path(directory) / "tree"
        run("git", "worktree", "add", "--detach", str(worktree), "origin/master")
        try:
            write_feed(worktree, latest, manifest, notes)
        finally:
            run("git", "worktree", "remove", "--force", str(worktree))
    print("Update feed published on master")


def published_history(latest, baseline=None):
    """Append only the release just published; never infer releases from unshipped notes."""
    metadata = json.loads((ROOT / "update/publishing/versions.json").read_text())
    releases = read_history(ROOT) if baseline is None else baseline
    validate_sequence(metadata["versions"], releases)
    if identity(latest) != identity(metadata["versions"][-1]):
        raise ValueError("Published release is not the requested version")
    notes = (ROOT / f"update/publishing/changelogs/ru/{latest['code']}.txt").read_text()
    if not notes.strip():
        raise ValueError("Empty published release notes")
    history = sorted([*releases, {**latest, "notes": notes}], key=lambda v: v["code"], reverse=True)
    return (json.dumps({"schema": 1, "releases": history}, ensure_ascii=False, indent=2) + "\n").encode()


def write_feed(worktree, latest, manifest, notes):
    def git(*args):
        return subprocess.check_output(("git", *args), cwd=worktree, text=True).strip()

    history = published_history(latest, read_history(worktree))
    feed = worktree / "update/feed"
    (feed / "changelogs").mkdir(parents=True, exist_ok=True)
    (feed / "data-v1.json").write_bytes(manifest)
    (feed / "history-v1.json").write_bytes(history)
    (feed / f"changelogs/{latest['code']}.txt").write_bytes(notes)
    git("config", "user.name", "github-actions[bot]")
    git("config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com")
    git("add", "--", "update/feed/data-v1.json", "update/feed/history-v1.json",
        f"update/feed/changelogs/{latest['code']}.txt")
    if git("diff", "--cached", "--name-only"):
        git("commit", "-m", f"Publish update feed for {latest['name']}")
        # A concurrent master update causes a safe failure, never a force push.
        git("push", f"https://github.com/{REPO}.git", "HEAD:master")


def main():
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--check", action="store_true")
    group.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    latest, tag, replacement = context()
    check(latest, tag, replacement)
    if args.check:
        return
    apk, manifest, notes = prepare(latest, tag)
    publish_release(latest, tag, replacement, apk)
    publish_feed(latest, manifest, notes)
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
        summary.write(f"\nPublic release: https://github.com/{REPO}/releases/tag/{tag}\n")
        summary.write("\nAssets: APK and SHA-256 only. Update feed: update/feed on master.\n")


if __name__ == "__main__":
    main()
