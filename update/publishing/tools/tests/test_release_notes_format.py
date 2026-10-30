from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from release_notes_format import explicit_sections, validate_notes


class NotesFormatTest(unittest.TestCase):
    def test_unheaded_notes_receive_the_same_default_as_the_phone(self):
        old = "Версия 0.1.20\n\n• Один\n• Два\n"
        self.assertEqual("Версия 0.1.20\n\nИзменения\n\n- Один\n- Два\n", explicit_sections(old))

    def test_explicit_sections_and_user_text_are_preserved(self):
        text = "Версия 0.1.21\n\nНовое\n\n• Один\n\nИсправления\n• Два\n"
        self.assertEqual("Версия 0.1.21\n\nИзменения\n\n- Один\n\nИсправления\n\n- Два\n", validate_notes(explicit_sections(text)))

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


class CanonicalNotesTest(unittest.TestCase):
    def test_aliases_order_and_duplicate_groups(self):
        raw = "Исправлено\n* Баг.\nНовое\n• Функция.\nВажно\n- Сброс.\nОбновлено\n- Интерфейс.\nПроверки\n- Не запускались."
        text = explicit_sections(raw)
        self.assertEqual("Важно\n\n- Сброс.\n\nИзменения\n\n- Функция.\n- Интерфейс.\n\nИсправления\n\n- Баг.\n\nТесты\n\n- Не запускались.\n", text)
        self.assertEqual(text, validate_notes(text))

    def test_unknown_text_and_empty_sections_are_not_silently_lost(self):
        for raw in ("Внезапно\n- Текст", "Изменения\nИсправления\n- Баг", "Изменения\n- ", "- Текст\nПроза"):
            with self.subTest(raw=raw), self.assertRaises(ValueError):
                explicit_sections(raw)

    def test_version_only_at_start(self):
        for raw in ("- Текст\nВерсия 0.1.0", "Версия 0.1.0\nВерсия 0.1.1\n- Текст"):
            with self.subTest(raw=raw), self.assertRaises(ValueError):
                explicit_sections(raw)

    def test_bom_crlf_and_markdown_headings(self):
        self.assertEqual("Версия 0.1.0\n\nТесты\n\n- Только добавлены.\n", explicit_sections("\ufeffVersion 0.1.0\r\n## Проверки\r\n* Только добавлены.\r\n"))

    def test_validation_rejects_noncanonical_but_accepts_missing_final_newline(self):
        raw = "Изменения\n\n- Не обещает ускорения."
        self.assertEqual(raw, validate_notes(raw))
        for old in ("Новое\n\n- Текст", "Изменения\n- Текст", "Изменения\n\n• Текст"):
            with self.subTest(old=old), self.assertRaises(ValueError):
                validate_notes(old)
