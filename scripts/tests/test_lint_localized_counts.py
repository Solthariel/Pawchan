"""Regression tests for the production Android Lint findings, independent of SDK installation."""
from collections import Counter
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
# Android's CLDR cardinal classes for integers, including million forms in Romance languages.
FORMS = {
    'values': {'one', 'few', 'many', 'other'}, 'values-en': {'one', 'other'},
    'values-de': {'one', 'other'}, 'values-pt-rBR': {'one', 'many', 'other'},
    'values-es': {'one', 'many', 'other'}, 'values-fr': {'one', 'many', 'other'},
    'values-it': {'one', 'many', 'other'}, 'values-pl': {'one', 'few', 'many', 'other'},
    'values-fi': {'one', 'other'}, 'values-ja': {'other'},
}
NUMERIC = {
    'app': {'ui_replies_6b38': 'sd', **dict.fromkeys((
        'ui_total_pages', 'ui_show_auto_hidden', 'ui_triggers', 'ui_thread_no',
        'platform_new_replies_to_your_posts_s', 'ui_expand_remaining'), 'd')},
    'core/ui': {'ui_from_to': 'dd'}, 'feature/applock': {'ui_digits_entered_of': 'dd'},
    'feature/composer': dict.fromkeys(('ui_filled', 'ui_color', 'ui_seconds_remaining'), 'd'),
    'feature/thread': dict.fromkeys(('ui_selected', 'ui_report_on_posts', 'ui_sent',
                                     'ui_post_hidden_tap_to_show', 'ui_hidden_comments', 'ui_post_text_sp'), 'd'),
}
QUANTITIES = {
    'app': dict.fromkeys(('number_posts_per_hour', 'watcher_notification_posts', 'board_comment_limit'), 'd'),
    'feature/applock': {'ui_retry_in_sec': 'd'}, 'feature/thread': {'ui_collapse_after_lines': 'd'},
    'feature/composer': {'composer_limits': 'sdd'},
}


def resources(module, qualifier):
    path = ROOT / module / 'src/main/res' / qualifier
    return {e.get('name'): e for f in path.glob('*.xml') for e in ET.parse(f).getroot() if e.tag in ('string', 'plurals')}


def placeholders(value):
    return Counter((int(i), t) for i, t in re.findall(r'%(\d+)\$([a-zA-Z])', value))


def signature(types):
    return Counter(enumerate(types, 1))


class LintLocalizedCountsTest(unittest.TestCase):
    def test_title_is_a_brand_in_both_build_types(self):
        for variant, expected in [('main', 'Pawchan'), ('debug', 'Pawchan Debug')]:
            el = ET.parse(ROOT / f'app/src/{variant}/res/values/strings.xml').find("string[@name='app_name']")
            self.assertIsNotNone(el)
            self.assertEqual((expected, 'false'), (el.text, el.get('translatable')))

    def test_numerical_call_sites_have_matching_native_format_types_in_all_locales(self):
        for module, ids in NUMERIC.items():
            for locale in FORMS:
                values = resources(module, locale)
                for name, types in ids.items():
                    with self.subTest(module=module, locale=locale, name=name):
                        self.assertEqual('string', values[name].tag)
                        self.assertEqual(signature(types), placeholders(values[name].text))

    def test_plural_categories_and_argument_types_in_all_locales(self):
        for module, ids in QUANTITIES.items():
            for locale, expected in FORMS.items():
                values = resources(module, locale)
                for name, types in ids.items():
                    with self.subTest(module=module, locale=locale, name=name):
                        group = values[name]
                        self.assertEqual('plurals', group.tag)
                        self.assertEqual(Counter(expected), Counter(el.get('quantity') for el in group))
                        for el in group:
                            self.assertEqual(signature(types), placeholders(el.text))

    def test_english_singular_does_not_display_plural_nouns(self):
        for mod, key, singular, plural in [
            ('app', 'board_comment_limit', 'character', 'characters'),
            ('feature/applock', 'ui_retry_in_sec', 'second', 'seconds'),
            ('feature/thread', 'ui_collapse_after_lines', 'line', 'lines'),
            ('feature/composer', 'composer_limits', 'file', 'files'),
        ]:
            group = resources(mod, 'values-en')[key]
            self.assertRegex(group.find("item[@quantity='one']").text, rf'\b{singular}\b')
            self.assertRegex(group.find("item[@quantity='other']").text, rf'\b{plural}\b')
