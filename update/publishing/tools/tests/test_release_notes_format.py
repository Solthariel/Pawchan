import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import Mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_notes_format import explicit_sections, validate_notes
from release_notes_corrections import correct_published_notes


class NotesFormatTest(unittest.TestCase):
    def test_unheaded_notes_receive_the_same_default_as_the_phone(self):
        old = "Версия 0.1.20\n\n• Один\n• Два\n"
        self.assertEqual("Версия 0.1.20\n\nИзменения\n\n• Один\n• Два\n", explicit_sections(old))

    def test_explicit_sections_and_user_text_are_preserved(self):
        text = "Версия 0.1.21\n\nНовое\n\n• Один\n\nИсправления\n• Два\n"
        self.assertEqual(text, validate_notes(explicit_sections(text)))

    def test_publication_rejects_missing_heading_even_though_the_phone_has_a_fallback(self):
        with self.assertRaisesRegex(ValueError, "explicit section"):
            validate_notes("Версия 0.1.20\n\n• Пункт")

    def test_empty_notes_or_heading_without_entries_are_rejected(self):
        for text in ["", "  ", "Версия 0.1.21", "Изменения", "Изменения\n• "]:
            with self.subTest(text=text), self.assertRaises(ValueError):
                validate_notes(text)

    def test_preparation_is_idempotent_and_handles_all_supported_bullets(self):
        for marker in ["•", "*", "-"]:
            text = explicit_sections(f"{marker} Пункт")
            self.assertEqual(text, explicit_sections(text))
            self.assertEqual(text, validate_notes(text))


class PublishedNotesCorrectionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        directory = self.root / "update/publishing"
        (directory / "changelogs/ru").mkdir(parents=True)
        self.before = "Версия 0.1.20\n\n• Первый\n• Второй\n"
        self.after = explicit_sections(self.before)
        (directory / "changelogs/ru/21.txt").write_text(self.after)
        (directory / "notes-corrections.json").write_text(json.dumps({"corrections": [{
            "publish_with": "0.1.21", "version": "0.1.20", "code": 21,
            "expected_body_sha256": hashlib.sha256(self.before.strip().encode()).hexdigest(),
        }]}))
        self.latest = {"name": "0.1.21", "code": 22}
        self.release = {"tag_name": "v0.1.20", "draft": False, "body": self.before}
        self.api = Mock(side_effect=lambda _: self.release)
        self.run = Mock()

    def tearDown(self):
        self.temp.cleanup()

    def repair(self, apply=True):
        correct_published_notes(self.root, self.latest, self.api, self.run, apply=apply)

    def test_preflight_is_read_only(self):
        self.repair(apply=False)
        self.run.assert_not_called()
        self.api.assert_called_once_with("releases/tags/v0.1.20")

    def test_only_the_release_body_is_edited_without_touching_tags_assets_or_latest(self):
        self.repair()
        self.run.assert_called_once_with(
            "gh", "release", "edit", "v0.1.20", "--repo", "Solthariel/Pawchan", "--notes-file",
            "update/publishing/changelogs/ru/21.txt")

    def test_already_corrected_body_is_a_noop_even_with_different_line_endings(self):
        self.release["body"] = self.after.replace("\n", "\r\n")
        self.repair()
        self.run.assert_not_called()

    def test_unknown_live_edits_are_never_overwritten(self):
        self.release["body"] += "• Чужое изменение\n"
        with self.assertRaises(ValueError):
            self.repair()
        self.run.assert_not_called()

    def test_canonical_file_cannot_change_anything_beyond_the_heading(self):
        path = self.root / "update/publishing/changelogs/ru/21.txt"
        path.write_text(self.after.replace("Первый", "Подмена"))
        with self.assertRaises(ValueError):
            self.repair()
        self.run.assert_not_called()

    def test_absent_draft_or_wrong_release_is_rejected(self):
        for release in [None, {**self.release, "draft": True}, {**self.release, "tag_name": "v0.1.19"}]:
            self.release = release
            with self.assertRaises(ValueError):
                self.repair()
        self.run.assert_not_called()

    def test_the_one_time_correction_does_not_run_for_future_versions(self):
        self.latest["name"] = "0.1.22"
        self.repair()
        self.api.assert_not_called()
        self.run.assert_not_called()
