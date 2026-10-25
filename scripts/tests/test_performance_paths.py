"""Regression tests for legacy Windows/adb artifact paths."""
import importlib.util
import ntpath
import unittest
from pathlib import Path

FILE = Path(__file__).resolve().parents[1] / 'performance/performance_paths.py'
SPEC = importlib.util.spec_from_file_location('performance_paths', FILE)
paths = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(paths)
ROOT = r'C:\Users\Eva\pwc'
PROJECT = r'C:\Users\Eva\Downloads\Pawchan-0.1.37\Pawchan-publish-0.1.38-native-l41c5ygp'
OUTPUT = r'C:\Users\Eva\AppData\Local\Temp\Pawchan-performance-preflight-0123456789abcdef0123456789abcdef\benchmark'
TOKEN = '0123456789'


def planned(root=ROOT, output=OUTPUT, model='SM-A057F', android='15'):
    return paths.plan(root, PROJECT, output, model, android, TOKEN)


class PerformancePathsTests(unittest.TestCase):
    def test_short_workspace_fits_actual_phone_export(self):
        result = planned()
        self.assertLessEqual(result['longestPredictedUnits'], 240)
        self.assertEqual(ntpath.join(ROOT, TOKEN), result['workspace'])
        self.assertEqual(4, len(result['expectedArtifactPaths']))

    def test_previous_long_workspace_is_rejected(self):
        old = r'C:\Users\Eva\AppData\Local\Temp\Pawchan-perf-work-f250c0f125cd45d7a9dad984a5d382e1'
        with self.assertRaisesRegex(ValueError, 'budget'):
            planned(old)

    def test_long_output_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'budget'):
            planned(output='C:\\' + 'very-long-directory\\' * 15)

    def test_nested_workspace_is_refused(self):
        with self.assertRaisesRegex(ValueError, 'outside'):
            planned(root=PROJECT + r'\work')

    def test_relative_workspace_is_refused(self):
        with self.assertRaisesRegex(ValueError, 'Absolute'):
            planned(root='relative')

    def test_invalid_device_label_is_refused(self):
        with self.assertRaisesRegex(ValueError, 'device'):
            planned(model='bad/name')

    def test_utf16_units_are_counted(self):
        self.assertEqual(2, paths.units('\U0001F43E'))

    def test_different_drive_workspace_is_supported(self):
        self.assertTrue(planned(root=r'D:\pw')['workspace'].startswith('D:'))

    def test_token_cannot_escape_workspace(self):
        with self.assertRaisesRegex(ValueError, 'token'):
            paths.plan(ROOT, PROJECT, OUTPUT, 'SM-A057F', '15', '../badpath')


if __name__ == '__main__':
    unittest.main()
