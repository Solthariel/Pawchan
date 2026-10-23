"""Run the metadata assertions with cp1251 for every unspecified text encoding."""
from pathlib import Path
import unittest
from unittest.mock import patch

import test_bump_version
import test_release_metadata


class WindowsTextEncodingTest(unittest.TestCase):
    def test_metadata_suite_works_without_python_utf8_mode(self):
        original_read = Path.read_text
        original_write = Path.write_text

        def windows_read(path, *args, **kwargs):
            if not args and kwargs.get('encoding') is None:
                kwargs['encoding'] = 'cp1251'
            return original_read(path, *args, **kwargs)

        def windows_write(path, data, *args, **kwargs):
            if not args and kwargs.get('encoding') is None:
                kwargs['encoding'] = 'cp1251'
            return original_write(path, data, *args, **kwargs)

        loader = unittest.TestLoader()
        suite = unittest.TestSuite([
            loader.loadTestsFromTestCase(test_bump_version.VersioningTest),
            loader.loadTestsFromTestCase(test_release_metadata.CurrentReleaseTest),
        ])
        result = unittest.TestResult()
        with patch.object(Path, 'read_text', windows_read), patch.object(Path, 'write_text', windows_write):
            suite.run(result)
        self.assertEqual(12, result.testsRun)
        self.assertEqual([], result.errors)
        self.assertEqual([], result.failures)
