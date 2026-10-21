import importlib.util
import sys
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

SPEC = importlib.util.spec_from_file_location(
    "release", Path(__file__).resolve().parents[1] / "publish_feed_release.py")
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        p = self.root / "update/publishing"
        p.mkdir(parents=True)
        self.latest = {"name": "0.1.0", "code": 1}
        (p / "changelogs/ru").mkdir(parents=True)
        (p / "changelogs/ru/1.txt").write_text("Исправления\n• Запись")
        (p / "versions.json").write_text(json.dumps({"versions": [self.latest]}))
        self.replace = {"release_id": 10, "tag_commit": "old"}
        self.request = {"version": "0.1.0", "code": 1,
                        "replace_first_release": self.replace}
        self.save_request()
        (p / "manifest").mkdir()
        (p / "manifest/data-v1.json").write_text(json.dumps({"applications": [
            {"name": "client", "packages": [{"version_code": 1, "version_name": "0.1.0"}]}]}))
        self.root_patch = patch.object(release, "ROOT", self.root)
        self.root_patch.start()
        self.env = patch.dict(os.environ, {"GITHUB_REPOSITORY": "Solthariel/Pawchan",
                              "GITHUB_REF": "refs/heads/master", "GITHUB_SHA": "new"})
        self.env.start()
        self.remote = {"releases/tags/v0.1.0": {"id": 10},
                       "git/ref/tags/v0.1.0": {"object": {"type": "commit", "sha": "old"}},
                       "branches/master": {"name": "master"}}

    def tearDown(self):
        self.env.stop()
        self.root_patch.stop()
        self.temp.cleanup()

    def save_request(self):
        (self.root / "update/publishing/release-request.json").write_text(json.dumps(self.request))

    def test_history_appends_only_the_successfully_published_release(self):
        directory = self.root / "update/publishing"
        (directory / "changelogs/ru").mkdir(parents=True, exist_ok=True)
        (directory / "changelogs/ru/1.txt").write_text("Исправления\n• Запись")
        result = json.loads(release.published_history(self.latest))
        self.assertEqual(result["schema"], 1)
        self.assertEqual([v["code"] for v in result["releases"]], [1])
        self.assertIn("Запись", result["releases"][0]["notes"])

    def test_history_preserves_published_notes_instead_of_reimporting_old_drafts(self):
        directory = self.root / "update/publishing"
        latest = {"code": 2, "name": "0.1.1"}
        (directory / "versions.json").write_text(json.dumps({"versions": [self.latest, latest]}))
        (directory / "changelogs/ru").mkdir(parents=True, exist_ok=True)
        (directory / "changelogs/ru/1.txt").write_text("edited old draft")
        (directory / "changelogs/ru/2.txt").write_text("Новое\n• new published notes")
        baseline = [{**self.latest, "notes": "original published notes"}]
        result = json.loads(release.published_history(latest, baseline))["releases"]
        self.assertEqual([2, 1], [v["code"] for v in result])
        self.assertEqual("Новое\n• new published notes", result[0]["notes"])
        self.assertEqual("original published notes", result[1]["notes"])

    def test_unpublished_intermediate_version_prevents_history_generation(self):
        directory = self.root / "update/publishing"
        (directory / "versions.json").write_text(json.dumps({"versions": [
            self.latest, {"code": 2, "name": "0.1.1"}]}))
        with self.assertRaises(ValueError):
            release.published_history({"code": 2, "name": "0.1.1"})

    def test_missing_published_notes_prevents_history_generation(self):
        (self.root / "update/publishing/changelogs/ru/1.txt").unlink()
        with self.assertRaises(FileNotFoundError):
            release.published_history(self.latest)

    def test_context_initial_replacement(self):
        self.assertEqual(release.context(), (self.latest, "v0.1.0", self.replace))

    def test_unknown_repository_rejected(self):
        with patch.dict(os.environ, {"GITHUB_REPOSITORY": "some/fork"}):
            with self.assertRaises(SystemExit):
                release.context()

    def test_request_version_mismatch_rejected(self):
        self.request["version"] = "0.2.0"
        self.save_request()
        with self.assertRaises(SystemExit):
            release.context()

    def test_future_release_replacement_rejected(self):
        self.latest.update(name="0.1.1", code=2)
        p = self.root / "update/publishing/versions.json"
        p.write_text(json.dumps({"versions": [self.latest]}))
        self.request.update(version="0.1.1", code=2)
        self.save_request()
        with self.assertRaises(SystemExit):
            release.context()

    def test_tag_event_never_authorizes_replacement(self):
        with patch.dict(os.environ, {"GITHUB_REF": "refs/tags/v0.1.0"}):
            self.assertIsNone(release.context()[2])

    def test_wrong_tag_rejected(self):
        with patch.dict(os.environ, {"GITHUB_REF": "refs/tags/v9.9.9"}):
            with self.assertRaises(SystemExit):
                release.context()

    def test_exact_original_release_passes(self):
        with patch.object(release, "api", side_effect=self.remote.get):
            release.check(self.latest, "v0.1.0", self.replace)

    def test_different_release_id_rejected(self):
        self.remote["releases/tags/v0.1.0"] = {"id": 11}
        with patch.object(release, "api", side_effect=self.remote.get):
            with self.assertRaises(SystemExit):
                release.check(self.latest, "v0.1.0", self.replace)

    def test_changed_tag_rejected(self):
        self.remote["git/ref/tags/v0.1.0"]["object"]["sha"] = "different"
        with patch.object(release, "api", side_effect=self.remote.get):
            with self.assertRaises(SystemExit):
                release.check(self.latest, "v0.1.0", self.replace)

    def test_existing_release_not_replaced_by_default(self):
        with patch.object(release, "api", side_effect=self.remote.get):
            with self.assertRaises(SystemExit):
                release.check(self.latest, "v0.1.0", None)

    def test_new_release_allowed(self):
        self.remote["releases/tags/v0.1.0"] = None
        self.remote["git/ref/tags/v0.1.0"] = None
        with patch.object(release, "api", side_effect=self.remote.get):
            release.check(self.latest, "v0.1.0", None)

    def test_release_has_only_apk_and_checksum(self):
        commands = []
        with patch.object(release, "api", side_effect=self.remote.get), patch.object(
                release, "run", side_effect=lambda *a: commands.append(a) or ""):
            release.publish_release(self.latest, "v0.1.0", self.replace, Path("dist/Pawchan-0.1.0.apk"))
        create = next(c for c in commands if c[:3] == ("gh", "release", "create"))
        self.assertEqual(create[4:6], ("dist/Pawchan-0.1.0.apk", "dist/Pawchan-0.1.0.apk.sha256"))
        self.assertNotIn("dist/data-v1.json", create)
        self.assertIn("--draft", create)
        self.assertEqual(commands[-1][:3], ("gh", "release", "edit"))
        self.assertIn("--draft=false", commands[-1])

    def test_failed_preflight_cannot_delete_release(self):
        self.remote["releases/tags/v0.1.0"] = {"id": 99}
        with patch.object(release, "api", side_effect=self.remote.get), patch.object(release, "run") as run:
            with self.assertRaises(SystemExit):
                release.publish_release(self.latest, "v0.1.0", self.replace, Path("app.apk"))
            run.assert_not_called()

    def test_missing_original_release_allows_new_publication(self):
        self.remote["releases/tags/v0.1.0"] = None
        commands = []
        with patch.object(release, "api", side_effect=self.remote.get), patch.object(
                release, "run", side_effect=lambda *a: commands.append(a) or ""):
            release.publish_release(self.latest, "v0.1.0", self.replace, Path("dist/app.apk"))
        self.assertFalse(any(c[:3] == ("gh", "release", "delete") for c in commands))
        self.assertTrue(any(c[:3] == ("gh", "release", "create") for c in commands))

    def test_missing_release_with_changed_tag_is_rejected(self):
        self.remote["releases/tags/v0.1.0"] = None
        self.remote["git/ref/tags/v0.1.0"]["object"]["sha"] = "another"
        with patch.object(release, "api", side_effect=self.remote.get):
            with self.assertRaises(SystemExit):
                release.check(self.latest, "v0.1.0", self.replace)

    def test_public_release_ahead_of_feed_blocks_publication(self):
        self.remote["releases/tags/v0.1.0"] = None
        self.remote["git/ref/tags/v0.1.0"] = None
        self.remote["releases/latest"] = {"tag_name": "v0.1.1"}
        with patch.object(release, "api", side_effect=self.remote.get), patch.object(release, "run") as run:
            with self.assertRaises(SystemExit):
                release.publish_release(self.latest, "v0.1.0", None, Path("app.apk"))
            run.assert_not_called()

    def test_feed_write_rejects_concurrent_publication_before_touching_files(self):
        worktree = self.root / "worktree"
        feed = worktree / "update/feed"
        feed.mkdir(parents=True)
        original = json.dumps({"schema": 1, "releases": [self.latest]})
        (feed / "history-v1.json").write_text(original)
        with patch.object(release, "run"):
            with self.assertRaises(ValueError):
                release.write_feed(worktree, self.latest, b"manifest", b"notes")
        self.assertEqual(original, (feed / "history-v1.json").read_text())
        self.assertFalse((feed / "data-v1.json").exists())

    def test_missing_apk_stops_before_preparation(self):
        with patch.object(release, "run") as run:
            with self.assertRaises(SystemExit):
                release.prepare(self.latest, "v0.1.0")
            run.assert_not_called()


if __name__ == "__main__":
    unittest.main()
