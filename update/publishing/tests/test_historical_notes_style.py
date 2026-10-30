"""Editorial consistency, without changing package identities or claiming runtime tests."""
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'update/publishing/tools'))
from release_notes_format import validate_notes, explicit_sections


class HistoricalNotesStyleTest(unittest.TestCase):
    def test_every_registered_release_and_existing_feed_copy_match(self):
        versions = json.loads((ROOT / 'update/publishing/versions.json').read_text(encoding='utf-8'))['versions']
        history = json.loads((ROOT / 'update/feed/history-v1.json').read_text(encoding='utf-8'))['releases']
        by_code = {row['code']: row for row in history}
        for version in versions:
            with self.subTest(version=version['name']):
                text = (ROOT / f"update/publishing/changelogs/ru/{version['code']}.txt").read_text(encoding='utf-8')
                validate_notes(text)
                self.assertEqual(text, explicit_sections(text))
                self.assertTrue(text.startswith(f"Версия {version['name']}\n\n"))
                if version['code'] in by_code:
                    self.assertEqual(text, by_code[version['code']]['notes'])
                feed = ROOT / f"update/feed/changelogs/{version['code']}.txt"
                if feed.exists():
                    self.assertEqual(text, feed.read_text(encoding='utf-8'))

    def test_latest_notes_keep_first_stage_and_performance_disclaimer(self):
        text = (ROOT / 'update/publishing/changelogs/ru/49.txt').read_text(encoding='utf-8')
        # Historical claims must not turn into a claim that a later UI patch shipped.
        self.assertIn('Первый этап', text)
        self.assertIn('ускорение загрузки этим выпуском не заявляется', text)
        tests = text.split('\n\nТесты\n\n')[1]
        self.assertIn('Добавлены проверки', tests)
