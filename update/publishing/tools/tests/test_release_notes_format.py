from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_notes_format import explicit_sections, validate_notes


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
