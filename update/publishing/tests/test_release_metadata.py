import json
from pathlib import Path
import unittest
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from release_notes_format import validate_notes


class CurrentReleaseTest(unittest.TestCase):
    def test_current_notes_are_bundled_without_drift(self):
        root = Path(__file__).resolve().parents[3]
        publishing = root / "update/publishing"
        versions = json.loads((publishing / "versions.json").read_text(encoding="utf-8"))["versions"]
        current = max(versions, key=lambda version: version["code"])
        notes = (publishing / f"changelogs/ru/{current['code']}.txt").read_text(encoding="utf-8")
        bundled = (root / "app/src/main/assets/settings-changelog.txt").read_text(encoding="utf-8")
        self.assertTrue(notes.startswith(f"Версия {current['name']}\n"))
        self.assertEqual(notes, bundled)

    def test_current_manifest_matches_registry(self):
        publishing = Path(__file__).resolve().parents[1]
        versions = json.loads((publishing / "versions.json").read_text(encoding="utf-8"))["versions"]
        current = max(versions, key=lambda version: version["code"])
        manifest = json.loads((publishing / "manifest/data-v1.json").read_text(encoding="utf-8"))
        client = next(app for app in manifest["applications"] if app["name"] == "client")
        package = max(client["packages"], key=lambda item: item["version_code"])
        self.assertEqual(current["code"], package["version_code"])
        self.assertEqual(current["name"], package["version_name"])

    def test_all_registered_release_notes_have_explicit_headings(self):
        directory = Path(__file__).resolve().parents[1]
        versions = json.loads((directory / "versions.json").read_text())["versions"]
        for version in versions:
            with self.subTest(version=version["name"]):
                text = (directory / f"changelogs/ru/{version['code']}.txt").read_text()
                self.assertEqual(text, validate_notes(text))

    def test_corrected_0120_notes_match_the_published_history_and_text_feed(self):
        root = Path(__file__).resolve().parents[3]
        notes = (root / "update/publishing/changelogs/ru/21.txt").read_text()
        history = json.loads((root / "update/feed/history-v1.json").read_text())["releases"]
        published = next(v for v in history if v["code"] == 21 and v["name"] == "0.1.20")
        self.assertEqual(notes, published["notes"])
        self.assertEqual(notes, (root / "update/feed/changelogs/21.txt").read_text())
        self.assertIn("\n\nИзменения\n\n", notes)


if __name__ == "__main__":
    unittest.main()
