"""A compile success or a stale/empty HTML page is not proof a selected device test ran."""
from pathlib import Path
from contextlib import redirect_stdout, redirect_stderr
from types import SimpleNamespace
from zipfile import ZipFile
import io
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from check_device_regressions import CASES, class_runs, run_regressions, selected_cases, verify_reports


class DeviceRegressionReportsTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.page = self.root / 'sample.PlayerTest.html'
        self.cases = {'sample.PlayerTest': ['opens']}

    def write_result(self, cells, name='opens'):
        self.page.write_text('<table><tr><td>' + name + '</td>' + cells + '</tr></table>', encoding='utf-8')

    def test_accepts_fresh_passing_row(self):
        self.write_result('<td class="success">passed (1s)</td>')
        self.assertEqual([], verify_reports(self.root, self.cases, 1))

    def test_rejects_absent_or_stale_page(self):
        self.assertTrue(verify_reports(self.root, self.cases, 1))
        self.write_result('<td class="success">passed (1s)</td>')
        os.utime(self.page, (100, 100))
        self.assertTrue(verify_reports(self.root, self.cases, 101))

    def test_rejects_failure_or_skip_even_if_other_device_passed(self):
        for failed in ('failed', 'skipped'):
            with self.subTest(failed=failed):
                self.write_result('<td class="success">passed</td><td class="failures">' + failed + '</td>')
                self.assertTrue(verify_reports(self.root, self.cases, 1))

    def test_other_method_cannot_satisfy_filter(self):
        self.write_result('<td class="success">passed (1s)</td>', name='other')
        self.assertTrue(verify_reports(self.root, self.cases, 1))


class DeviceRegressionRunnerTests(unittest.TestCase):
    prefix = '-Pandroid.testInstrumentationRunnerArguments.class='

    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)

    def pages(self, module):
        path = self.root / module / 'build/reports/androidTests/connected/debug'
        path.mkdir(parents=True, exist_ok=True)
        return path

    @staticmethod
    def write_page(file, outcomes):
        rows = [f'<tr><td>{name}</td><td class="{style}">{status} (0.2s)</td></tr>'
                for name, style, status in outcomes]
        file.write_text('<table>' + ''.join(rows) + '</table>', encoding='utf-8')

    def execute(self, cases, run):
        with redirect_stdout(io.StringIO()), redirect_stderr(io.StringIO()):
            code = run_regressions(self.root, cases, run=run)
        bundles = list((self.root / 'build/reports/device-regressions').glob('*.zip'))
        self.assertEqual(1, len(bundles))
        with ZipFile(bundles[0]) as archive:
            self.assertIsNone(archive.testzip())
            return code, json.loads(archive.read('summary.json')), set(archive.namelist())

    def test_full_manifest_is_verified_even_when_runner_only_accepts_first_selector_and_replaces_pages(self):
        calls = []

        def first_only(args, *, cwd, check):
            self.assertEqual(self.root, cwd)
            self.assertFalse(check)
            filters = [a[len(self.prefix):] for a in args if a.startswith(self.prefix)]
            self.assertEqual(1, len(filters))
            selector = filters[0]
            module = args[1].removesuffix(':connectedDebugAndroidTest').strip(':').replace(':', '/')
            calls.append((module, selector))
            cls, _, method = selector.split(',', 1)[0].partition('#')
            required = CASES[module][cls]
            pages = self.pages(module)
            # Each Gradle invocation can replace the entire module's connected report directory.
            for page in pages.glob('*.html'):
                page.unlink()
            methods = [method] if method else required + ['anotherPassingTest']
            self.write_page(pages / (cls + '.html'), [(m, 'success', 'passed') for m in methods])
            self.assertIn('--rerun', args)
            return SimpleNamespace(returncode=0)

        code, summary, names = self.execute(CASES, first_only)
        self.assertEqual((0, 'PASS', 59, 59), (code, summary['result'], summary['confirmed'], summary['required']))
        self.assertEqual([(mod, cls + '#' + ids[0] if len(ids) == 1 else cls)
                          for mod, classes in CASES.items() for cls, ids in classes.items()], calls)
        self.assertFalse(any(',' in selector for _, selector in calls))
        self.assertEqual(29, len(calls))
        self.assertIn('app/com.solthariel.pawchan.locale.LanguageDeliveryRuntimeTest.html', names)
        self.assertIn('core/media/com.solthariel.pawchan.media.video.MediaLocaleHostRuntimeTest.html', names)
        self.assertIn('app/com.solthariel.pawchan.settings.InterfaceLocalesRuntimeTest.html', names)
        self.assertIn('core/media/com.solthariel.pawchan.VideoThumbnailBadgeRuntimeTest.html', names)
        self.assertIn('feature/thread/com.solthariel.pawchan.ThreadShotPixelRuntimeTest.html', names)
        self.assertIn('app/com.solthariel.pawchan.downloads.SavedThumbnailRuntimeTest.html', names)
        self.assertIn('core/media/com.solthariel.pawchan.MpvClipClockRuntimeTest.html', names)
        self.assertEqual({mod + '/' + cls + '.html' for mod, classes in CASES.items() for cls in classes}, names - {'summary.json'})

    def test_navigation_mode_keeps_the_three_reported_failures_and_four_guards(self):
        focused = selected_cases(['--navigation-state'])
        self.assertEqual({'app'}, set(focused))
        required = {name for names in focused['app'].values() for name in names}
        self.assertEqual({
            'draftArgumentsAndBackStackSurviveRoundTrip', 'backStackAndDraftSurviveSaveableStateRestoration',
            'restoringThreadRetainsItsSaveableUiState', 'destinationChangesImmediatelyWithoutScreenTransition',
            'firstFrameShowsOnlyDestinationColor', 'backKeepsPreviousViewModelAndClearsRemovedEntry',
            'childAndHubOverlapDuringForwardAndBackAndPoppedEntrySurvivesTransition',
        }, required)
        self.assertEqual(3, len(list(class_runs(focused))))
        self.assertIs(CASES, selected_cases([]))
        for cls, methods in focused['app'].items():
            self.assertEqual(methods, CASES['app'][cls])

    def test_gradle_success_with_partial_report_is_not_a_pass(self):
        cases = {'core/media': {'p.Player': ['opens', 'seeks']}}

        def incomplete(*args, **kwargs):
            self.write_page(self.pages('core/media') / 'p.Player.html', [('opens', 'success', 'passed')])
            return SimpleNamespace(returncode=0)

        code, summary, _ = self.execute(cases, incomplete)
        self.assertEqual((1, 'FAIL', 1, 2), (code, summary['result'], summary['confirmed'], summary['required']))
        self.assertIn('p.Player#seeks: not run', summary['runs'][0]['issues'])

    def test_unchanged_report_from_the_same_second_is_not_accepted_or_archived(self):
        self.write_page(self.pages('app') / 'p.Screen.html', [('shows', 'success', 'passed')])
        code, summary, names = self.execute({'app': {'p.Screen': ['shows']}}, lambda *a, **k: SimpleNamespace(returncode=0))
        self.assertEqual((1, 0, {'summary.json'}), (code, summary['confirmed'], names))
        self.assertIn('stale class report: p.Screen', summary['runs'][0]['issues'])

    def test_nonzero_gradle_exit_still_fails_after_a_passing_method(self):
        def gradle_error(*args, **kwargs):
            self.write_page(self.pages('app') / 'p.Screen.html', [('shows', 'success', 'passed')])
            return SimpleNamespace(returncode=1)

        code, summary, _ = self.execute({'app': {'p.Screen': ['shows']}}, gradle_error)
        self.assertEqual((1, 'FAIL', 1), (code, summary['result'], summary['confirmed']))
        self.assertEqual(1, summary['runs'][0]['gradle_exit'])

    def test_an_extra_failure_in_a_selected_class_is_visible(self):
        def extra_failure(*args, **kwargs):
            self.write_page(self.pages('app') / 'p.Screen.html', [('shows', 'success', 'passed'),
                            ('opens', 'success', 'passed'), ('closes', 'failures', 'failed')])
            return SimpleNamespace(returncode=0)

        code, summary, names = self.execute({'app': {'p.Screen': ['shows', 'opens']}}, extra_failure)
        self.assertEqual((1, 'FAIL'), (code, summary['result']))
        self.assertIn('app/p.Screen.html', names)
        self.assertIn('p.Screen#closes: additional test in this class failed', summary['runs'][0]['issues'])
