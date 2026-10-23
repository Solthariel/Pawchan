"""Regression coverage for false-positive/stale device test reports."""
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('report', Path(__file__).resolve().parents[1] / 'verify_pin_test_report.py')
REPORT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REPORT)


class PinReportTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.reports = self.root / 'app/build/outputs/androidTest-results/connected/debug'
        self.reports.mkdir(parents=True)

    def write_report(self, counts=None, issue='', timestamp=200):
        counts = counts or REPORT.EXPECTED
        cases = ''.join(f'<testcase classname="{name}" name="case{i}">{issue}</testcase>'
                        for name, count in counts.items() for i in range(count))
        path = self.reports / 'TEST-device.xml'
        path.write_text('<testsuite>' + cases + '</testsuite>', encoding='utf-8')
        os.utime(path, (timestamp, timestamp))
        return path

    def test_seven_complete_tests_pass(self):
        self.write_report()
        self.assertEqual(REPORT.EXPECTED, REPORT.verify(self.root, 100))

    def test_no_xml_refuses_even_if_gradle_succeeded(self):
        with self.assertRaises(ValueError):
            REPORT.verify(self.root, 100)

    def test_stale_success_refuses(self):
        self.write_report(timestamp=99)
        with self.assertRaises(ValueError):
            REPORT.verify(self.root, 100)

    def test_missing_isolation_tests_refuses(self):
        self.write_report({'com.solthariel.pawchan.PinEditorInputRuntimeTest': 5})
        with self.assertRaises(ValueError):
            REPORT.verify(self.root, 100)

    def test_failure_error_and_skip_refuse(self):
        for tag in ('failure', 'error', 'skipped'):
            with self.subTest(tag=tag):
                self.write_report(issue=f'<{tag}/>')
                with self.assertRaises(ValueError):
                    REPORT.verify(self.root, 100)

    def test_duplicate_results_refuse(self):
        path = self.write_report()
        second = self.reports / 'TEST-other-device.xml'
        second.write_bytes(path.read_bytes())
        os.utime(second, (200, 200))
        with self.assertRaises(ValueError):
            REPORT.verify(self.root, 100)

    def test_suite_error_without_case_failure_refuses(self):
        path = self.write_report()
        path.write_text(path.read_text().replace('<testsuite>', '<testsuite errors="1">'))
        os.utime(path, (200, 200))
        with self.assertRaises(ValueError):
            REPORT.verify(self.root, 100)

    def test_separate_runs_with_same_output_filename_both_verify(self):
        for name, count in REPORT.EXPECTED.items():
            with self.subTest(class_name=name):
                self.write_report({name: count})
                self.assertEqual({name: count}, REPORT.verify(self.root, 100, name))
                with self.assertRaises(ValueError):
                    REPORT.verify(self.root, 100)

    def test_wrong_class_cannot_satisfy_selected_run(self):
        self.write_report({'com.solthariel.pawchan.DebugPackageIsolationRuntimeTest': 2})
        with self.assertRaises(ValueError):
            REPORT.verify(self.root, 100, 'com.solthariel.pawchan.PinEditorInputRuntimeTest')

    def test_selected_run_still_rejects_stale_or_failed_results(self):
        name = 'com.solthariel.pawchan.PinEditorInputRuntimeTest'
        self.write_report({name: 5}, timestamp=99)
        with self.assertRaises(ValueError):
            REPORT.verify(self.root, 100, name)
        self.write_report({name: 5}, issue='<failure/>')
        with self.assertRaises(ValueError):
            REPORT.verify(self.root, 100, name)


if __name__ == '__main__':
    unittest.main()
