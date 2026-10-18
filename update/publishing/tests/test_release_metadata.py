import json
from pathlib import Path
import unittest


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


if __name__ == "__main__":
    unittest.main()
