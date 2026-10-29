"""Execute the native OS entry point against a fake Gradle, without running Android."""
import json
from pathlib import Path
import os
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
MODULES = ('app', 'data:database', 'core:ui', 'core:media',
           'feature:composer', 'feature:thread', 'feature:applock')


class DeviceSmokeCommandTest(unittest.TestCase):
    def invoke(self, exit_code):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'scripts').mkdir()
            windows = os.name == 'nt'
            script = 'device-smoke.ps1' if windows else 'device-smoke.sh'
            shutil.copyfile(SCRIPTS / script, root / 'scripts' / script)
            gradle = root / ('gradlew.bat' if windows else 'gradlew')
            capture = root / 'capture.py'
            capture.write_text('import json, sys\nfrom pathlib import Path\n'
                               'Path(__file__).with_name("arguments.json").write_text(json.dumps(sys.argv[1:]))\n'
                               'raise SystemExit(' + str(exit_code) + ')\n')
            content = (f'@echo off\n"{sys.executable}" -B "%~dp0capture.py" %*\nexit /b %errorlevel%\n'
                       if windows else f'#!/bin/sh\nexec "{sys.executable}" -B "$(dirname "$0")/capture.py" "$@"\n')
            gradle.write_text(content)
            gradle.chmod(0o755)
            if windows:
                shell = shutil.which('powershell.exe') or shutil.which('pwsh.exe')
                self.assertIsNotNone(shell, 'PowerShell is required to check the Windows entry point')
                command = [shell, '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(root / 'scripts' / script)]
            else:
                command = ['sh', str(root / 'scripts' / script)]
            result = subprocess.run(command, capture_output=True, text=True, timeout=10)
            return result, json.loads((root / 'arguments.json').read_text())

    def test_all_modules_run_with_a_real_per_test_timeout_and_no_test_filter(self):
        result, args = self.invoke(0)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('DEVICE SMOKE: PASS', result.stdout)
        self.assertEqual([a for a in args if a.startswith(':')],
                         [f':{module}:connectedDebugAndroidTest' for module in MODULES])
        self.assertIn('-Pandroid.testInstrumentationRunnerArguments.timeout_msec=180000', args)
        self.assertIn('--warning-mode=fail', args)
        self.assertFalse(any('.class=' in a or '.log=' in a for a in args))

    def test_failed_gradle_never_prints_pass(self):
        result, _ = self.invoke(7)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('DEVICE SMOKE: PASS', result.stdout)
