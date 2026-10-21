import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_sequence import validate_local, validate_sequence


def version(code, name):
    return {"code": code, "name": name, "date": "21.10.2026", "notes": "Published"}


class SequenceTests(unittest.TestCase):
    def setUp(self):
        self.published = [version(19, "0.1.18"), version(20, "0.1.19")]

    def test_one_patch_minor_or_major_is_allowed(self):
        for name in ("0.1.20", "0.2.0", "1.0.0"):
            with self.subTest(name=name):
                validate_sequence(self.published + [version(21, name)], self.published)

    def test_two_new_versions_cannot_be_published_as_one(self):
        with self.assertRaises(ValueError):
            validate_sequence(self.published + [version(21, "0.1.20"), version(22, "0.1.21")], self.published)

    def test_number_or_code_cannot_skip(self):
        for code, name in ((21, "0.1.21"), (22, "0.1.20"), (21, "0.3.0"), (21, "2.0.0")):
            with self.subTest(code=code, name=name), self.assertRaises(ValueError):
                validate_sequence(self.published + [version(code, name)], self.published)

    def test_published_baseline_cannot_be_removed_or_renamed(self):
        for prefix in (self.published[1:], [version(19, "0.1.17"), self.published[1]]):
            with self.assertRaises(ValueError):
                validate_sequence(prefix + [version(21, "0.1.20")], self.published)

    def test_historical_skip_is_kept_but_not_repeated(self):
        old = [version(9, "0.1.8"), version(11, "0.1.10")]
        validate_sequence(old + [version(12, "0.1.11")], old)
        with self.assertRaises(ValueError):
            validate_sequence(old + [version(13, "0.1.12")], old)

    def test_duplicate_names_codes_or_invalid_types_are_rejected(self):
        for code, name in ((21, "0.1.19"), (20, "0.1.20"), (True, "0.1.20"), (21, "0.1.020")):
            with self.subTest(code=code, name=name), self.assertRaises(ValueError):
                validate_sequence(self.published + [version(code, name)], self.published)

    def test_missing_baseline_cannot_restart_history(self):
        with self.assertRaises(ValueError):
            validate_sequence([version(21, "0.1.20")], [])
        validate_sequence([version(1, "0.1.0")], [])

    def test_request_and_manifest_must_match_the_single_pending_version(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            publishing = root / "update/publishing"
            (publishing / "manifest").mkdir(parents=True)
            (root / "update/feed").mkdir()
            versions = self.published + [version(21, "0.1.20")]
            (publishing / "versions.json").write_text(json.dumps({"versions": versions}))
            (root / "update/feed/history-v1.json").write_text(json.dumps({"schema": 1, "releases": self.published}))
            request = publishing / "release-request.json"
            manifest = publishing / "manifest/data-v1.json"
            request.write_text(json.dumps({"code": 21, "version": "0.1.20"}))
            packages = [{"version_code": v["code"], "version_name": v["name"]} for v in versions]
            manifest.write_text(json.dumps({"applications": [{"name": "client", "packages": packages}]}))
            self.assertEqual(self.published, validate_local(root))
            request.write_text(json.dumps({"code": 22, "version": "0.1.21"}))
            with self.assertRaises(ValueError):
                validate_local(root)
            request.write_text(json.dumps({"code": 21, "version": "0.1.20"}))
            packages[-1]["version_name"] = "0.1.21"
            manifest.write_text(json.dumps({"applications": [{"name": "client", "packages": packages}]}))
            with self.assertRaises(ValueError):
                validate_local(root)


if __name__ == "__main__":
    unittest.main()
