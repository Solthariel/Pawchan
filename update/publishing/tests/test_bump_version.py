import importlib.util
import sys
import json
from datetime import date
from pathlib import Path
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))

SPEC = importlib.util.spec_from_file_location("bump_version", Path(__file__).parents[1] / "tools/bump_version.py")
bump = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(bump)


class VersioningTest(unittest.TestCase):
    def test_semver_increments_reset_lower_components(self):
        for change, expected in [("patch", "2.3.5"), ("minor", "2.4.0"), ("major", "3.0.0")]:
            with self.subTest(change=change):
                self.assertEqual(expected, bump.next_version("2.3.4", change))

    def test_strict_format(self):
        for name in ("01.0.0", "1.01.0", "1.0.01", "1.0", "1.0.0-mvp", "-1.0.0", "1.0.0\n"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                bump.next_version(name, "patch")
        self.assertEqual("0.1.1", bump.next_version("0.1.0", "patch"))

    def fixture(self, root):
        publishing = root / "update/publishing"
        (publishing / "manifest").mkdir(parents=True)
        (publishing / "changelogs/ru").mkdir(parents=True)
        (root / "app/src/main/assets").mkdir(parents=True)
        (publishing / "versions.json").write_text(json.dumps({"versions": [{"code": 1, "name": "0.1.0", "date": "01.10.2026"}]}))
        package = {"version_code": 1, "version_name": "0.1.0", "min_sdk": 30, "length": 123,
                   "source": "https://example.test/old.apk", "changelog": "https://example.test/1.txt", "sha256sum": "original"}
        (publishing / "manifest/data-v1.json").write_text(json.dumps({"applications": [{"name": "client", "packages": [package]}]}))
        feed = root / "update/feed"
        feed.mkdir(parents=True)
        (feed / "history-v1.json").write_text(json.dumps({"schema": 1, "releases": [
            {"code": 1, "name": "0.1.0", "date": "01.10.2026", "notes": "old"}]}))
        return publishing, package

    def test_new_release_keeps_published_package_and_uses_next_ordinal(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            publishing, old_package = self.fixture(root)
            self.assertEqual(("0.2.0", 2), bump.prepare_release(root, "minor", "* Новая функция", date(2026, 10, 18)))
            history = json.loads((publishing / "versions.json").read_text())["versions"]
            self.assertEqual({"code": 2, "name": "0.2.0", "date": "18.10.2026"}, history[-1])
            packages = json.loads((publishing / "manifest/data-v1.json").read_text())["applications"][0]["packages"]
            self.assertEqual(old_package, packages[0])
            for field in ("source", "changelog", "sha256sum"):
                self.assertIsNone(packages[1][field])
            self.assertEqual(0, packages[1]["length"])
            self.assertEqual((publishing / "changelogs/ru/2.txt").read_text(), (root / "app/src/main/assets/settings-changelog.txt").read_text())

    def test_heading_is_written_explicitly_for_both_phone_assets_and_github_notes(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            publishing, _ = self.fixture(root)
            bump.prepare_release(root, "patch", "• Один\n• Два", date.today())
            text = (publishing / "changelogs/ru/2.txt").read_text()
            self.assertEqual("Версия 0.1.1\n\nИзменения\n\n• Один\n• Два\n", text)
            self.assertEqual(text, (root / "app/src/main/assets/settings-changelog.txt").read_text())

    def test_unstructured_notes_cannot_create_an_empty_history_entry(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            publishing, _ = self.fixture(root)
            before = (publishing / "versions.json").read_bytes()
            with self.assertRaises(ValueError):
                bump.prepare_release(root, "patch", "Исправления без пунктов", date.today())
            self.assertEqual(before, (publishing / "versions.json").read_bytes())
            self.assertFalse((publishing / "changelogs/ru/2.txt").exists())

    def test_cannot_prepare_two_versions_without_publication(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            publishing, _ = self.fixture(root)
            bump.prepare_release(root, "patch", "* Fix", date.today())
            request = json.loads((publishing / "release-request.json").read_text())
            self.assertEqual({"version": "0.1.1", "code": 2, "revision": 1}, request)
            before = (publishing / "versions.json").read_bytes()
            with self.assertRaises(ValueError):
                bump.prepare_release(root, "patch", "* Another fix", date.today())
            self.assertEqual(before, (publishing / "versions.json").read_bytes())
            self.assertFalse((publishing / "changelogs/ru/3.txt").exists())

    def test_empty_notes_do_not_change_files(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            publishing, _ = self.fixture(root)
            before = (publishing / "versions.json").read_bytes()
            with self.assertRaises(ValueError):
                bump.prepare_release(root, "patch", "  ", date.today())
            self.assertEqual(before, (publishing / "versions.json").read_bytes())
            self.assertFalse((publishing / "changelogs/ru/2.txt").exists())

    def test_disagreement_does_not_create_a_release(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            publishing, _ = self.fixture(root)
            manifest_path = publishing / "manifest/data-v1.json"
            manifest = json.loads(manifest_path.read_text())
            manifest["applications"][0]["packages"][0]["version_code"] = 7
            manifest_path.write_text(json.dumps(manifest))
            with self.assertRaises(ValueError):
                bump.prepare_release(root, "patch", "* Fix", date.today())
            self.assertFalse((publishing / "changelogs/ru/2.txt").exists())


if __name__ == "__main__":
    unittest.main()
