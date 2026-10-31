"""Static restoration guards, not a substitute for the Android runtime tests."""
from pathlib import Path
import hashlib
import unittest

ROOT = Path(__file__).resolve().parents[2]


class SettingsRowRestorationTests(unittest.TestCase):
    def test_row_matches_the_supplied_0148_implementation(self):
        source = (ROOT / 'app/src/main/kotlin/com/solthariel/pawchan/settings/SettingsComponents.kt').read_text()
        start = source.index('@Composable\ninternal fun SettingsRow(')
        end = source.index('@Composable\nprivate fun ChoiceRow(', start)
        actual = hashlib.sha256(source[start:end].encode()).hexdigest()
        self.assertEqual('e31b5d660840da1e6c207f8c5e9b3852b440397d349210f54841f8fe81d83103', actual)

    def test_original_0148_layout_assertions_are_preserved(self):
        path = ROOT / 'app/src/androidTest/kotlin/com/solthariel/pawchan/SettingsLayoutRuntimeTest.kt'
        normalized = path.read_text(encoding='utf-8-sig').replace('\r\n', '\n').encode()
        self.assertEqual('862afd170d94c5bac15fab64b86afcb2a44694c697104559fdd3aea309177744', hashlib.sha256(normalized).hexdigest())


if __name__ == '__main__':
    unittest.main()
