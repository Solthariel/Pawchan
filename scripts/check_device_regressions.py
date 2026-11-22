"""Verify selected Android device regressions from fresh reports, not the full smoke suite."""
from html.parser import HTMLParser
from pathlib import Path
from tempfile import NamedTemporaryFile
from zipfile import ZipFile, ZIP_DEFLATED
import argparse
import json
import os
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
# Shared by the full regression selection and --navigation-state. Each class gets its own invocation.
NAVIGATION_STATE_CASES = {
    "com.solthariel.pawchan.InstantNavigationProductionRuntimeTest": [
        "destinationChangesImmediatelyWithoutScreenTransition",
        "draftArgumentsAndBackStackSurviveRoundTrip",
        "backStackAndDraftSurviveSaveableStateRestoration",
        "firstFrameShowsOnlyDestinationColor",
        "backKeepsPreviousViewModelAndClearsRemovedEntry",
    ],
    "com.solthariel.pawchan.navigation.TabReselectionRuntimeTest": [
        "restoringThreadRetainsItsSaveableUiState",
    ],
    "com.solthariel.pawchan.navigation.ThreadsMotionRuntimeTest": [
        "childAndHubOverlapDuringForwardAndBackAndPoppedEntrySurvivesTransition",
    ],
}

# AndroidJUnitRunner class filters are scoped to the APK for their own module.
CASES = {
    "app": {
        "com.solthariel.pawchan.AboutDetailsRuntimeTest": [
            "licensesShowFontAttributionsInlineAndOpenSourceLinksDirectly"
        ],
        "com.solthariel.pawchan.AppBottomRippleRuntimeTest": [
            "allFivePressIndicationsStayCircularWhileWholeCellIsClickable"
        ],
        "com.solthariel.pawchan.AppNavigationMotionRuntimeTest": [
            "destinationChangesImmediatelyWithoutScreenTransition"
        ],
        "com.solthariel.pawchan.BoardsCategoryChipsRuntimeTest": [
            "stripUsesMatchingInsetsAndScrollsToTheLastChip"
        ],
        "com.solthariel.pawchan.BottomNavigationOutlineRuntimeTest": [
            "plainDarkPanelHasFlatTranslucentSurface",
            "plainLightPanelHasFlatTranslucentSurface"
        ],
        "com.solthariel.pawchan.CatalogNavigationWorkspaceRuntimeTest": [
            "bottomBackingDoesNotCoverTheRailAndHideMovesItSideways",
            "bottomTapReachesTheEndWithTheLastRowAboveTheButtons",
            "railAnd48DpTargetsEndAtTheWorkspaceBottom"
        ],
        "com.solthariel.pawchan.CustomColorUiTest": [
            "lavenderPrefillsRgbWithoutChangingSelectedPalette"
        ],
        **NAVIGATION_STATE_CASES,
        "com.solthariel.pawchan.MirrorAvailabilityResultsRuntimeTest": [
            "checkingOpensASeparateWindowAndClosingHidesAllResults"
        ],
        "com.solthariel.pawchan.ModalNavigationBackgroundRuntimeTest": [
            "mediaDialogDimsTheNavigationBackgroundToTenPercent",
            "normalDialogDimsTheThemesLastDrawnNavigationBackground"
        ],
        "com.solthariel.pawchan.PolishedSettingsRuntimeTest": [
            "versionAndNumberShareOneLineWithChangelogAction"
        ]
    },
    "core/ui": {
        "com.solthariel.pawchan.core.ui.ModalLayerRuntimeTest": [
            "videoBackdropReplacesPickerScrimAndClosingRestoresPicker"
        ],
        "com.solthariel.pawchan.core.ui.WorkspaceScrollbarBoundsRuntimeTest": [
            "scrollbarStartsBelowToolbarAndEndsAboveSystemNavigation"
        ]
    },
    "core/media": {
        "com.solthariel.pawchan.MpvVideoEngineRuntimeTest": [
            "detachedTextureCannotReportOutputForReplacement",
            "softwareDecoderPlaysH264AndAac",
            "softwareDecoderPlaysMpeg4AndAc3",
            "softwareDecoderPlaysVp8AndVorbis",
            "softwareDecoderSeeksTruncatedWebm",
            "softwareDecoderAcceptsGenuineBlackFrames",
            "hardwareRequestedDecoderAcceptsGenuineBlackFrames"
        ],
        "com.solthariel.pawchan.VideoChromeAutoHideRuntimeTest": [
            "controlsHideDuringPlaybackButRemainVisibleWhenPaused"
        ],
        "com.solthariel.pawchan.VideoControlLayoutRuntimeTest": [
            "narrowLayoutCanScrollToRotationWithoutAddingAnotherRow",
            "phoneLayoutIsCompactAndHasOneRow",
            "wideLayoutIsCompactAndHasOneRow"
        ],
        "com.solthariel.pawchan.VideoViewerBackdropRuntimeTest": [
            "actualViewerWindowShowsTheDimmedPageWhileVideoIsWaiting",
            "blackVideoFrameIsVisibleWhenOutputIsReady",
            "playingLandscapeFrameHasDimmedBackdropOutsideItsBounds",
            "waitingAndSurfaceResetShowOnlyNinetyPercentDimEvenWithAReservedFrame"
        ],
        "com.solthariel.pawchan.MpvSurfaceHandoffRuntimeTest": [
            "pausedSoftwarePlaybackRedrawsOnReplacementSurfaces",
            "pausedAutoPlaybackRedrawsOnReplacementSurfaces"
        ]
    },
    "feature/composer": {
        "com.solthariel.pawchan.AttachmentApplyReopenUiTest": [
            "toolbarAndSystemBackApplyOptionsWithoutDoneOrDiscardPrompt"
        ]
    }
}


class ResultRows(HTMLParser):
    """Read the class report's per-method, per-device cells, not the overall Gradle status."""

    def __init__(self):
        super().__init__()
        self.rows = {}
        self.row = None
        self.cell = None

    def handle_starttag(self, tag, attrs):
        if tag == 'tr':
            self.row = []
        elif tag == 'td' and self.row is not None:
            self.cell = (dict(attrs).get('class', '').split(), [])

    def handle_data(self, data):
        if self.cell is not None:
            self.cell[1].append(data)

    def handle_endtag(self, tag):
        if tag == 'td' and self.cell is not None:
            classes, parts = self.cell
            self.row.append((classes, ''.join(parts).strip()))
            self.cell = None
        elif tag == 'tr' and self.row is not None:
            if len(self.row) >= 2:
                self.rows.setdefault(self.row[0][1], []).extend(self.row[1:])
            self.row = None
            self.cell = None


def report_stamp(page):
    try:
        stat = page.stat()
    except FileNotFoundError:
        return None
    return stat.st_mtime_ns, stat.st_size, stat.st_ino


def inspect_report(page, cls, methods, since, before=None):
    """Only this invocation's class page can prove these methods ran on the device(s)."""
    stamp = report_stamp(page)
    if stamp is None:
        return [], ['no class report: ' + cls], None
    if stamp == before or stamp[0] < since * 1_000_000_000:
        return [], ['stale class report: ' + cls], None
    raw = page.read_bytes()
    report = ResultRows()
    try:
        report.feed(raw.decode('utf-8-sig'))
        report.close()
    except (UnicodeError, ValueError) as error:
        return [], [f'unreadable report for {cls}: {error}'], raw
    passed, issues = [], []
    for method in methods:
        cells = report.rows.get(method)
        if cells and all('success' in styles and text.split()[:1] == ['passed'] for styles, text in cells):
            passed.append(method)
        else:
            status = ', '.join(text for _, text in cells) if cells else 'not run'
            issues.append(f'{cls}#{method}: {status}')
    for method, cells in report.rows.items():
        if method not in methods and any('failures' in styles and text.split()[:1] in (['failed'], ['error'])
                                         for styles, text in cells):
            issues.append(f'{cls}#{method}: additional test in this class failed')
    return passed, issues, raw


def verify_reports(report_dir, classes, since):
    return [issue for cls, methods in classes.items()
            for issue in inspect_report(report_dir / (cls + '.html'), cls, methods, since)[1]]


def archive_class(bundle, module, page, raw):
    archive_name = module + '/' + page.name
    bundle.writestr(archive_name, raw)
    for name in ('css/base-style.css', 'css/style.css', 'js/report.js'):
        item = page.parent / name
        archived = module + '/' + name
        if item.is_file() and archived not in bundle.namelist():
            bundle.write(item, archived)
    return archive_name


def class_runs(cases):
    for module, classes in cases.items():
        for cls, methods in classes.items():
            # A comma-separated filter gave a green Gradle exit after only the *first* method.
            # For several methods in one class, run that class and inspect each required row.
            selector = cls + '#' + methods[0] if len(methods) == 1 else cls
            yield module, cls, methods, selector


def run_regressions(root, cases, run=subprocess.run):
    wrapper = root / ('gradlew.bat' if os.name == 'nt' else 'gradlew')
    output = root / 'build/reports/device-regressions'
    output.mkdir(parents=True, exist_ok=True)
    runs = list(class_runs(cases))
    expected = sum(len(methods) for _, _, methods, _ in runs)
    if not expected:
        raise ValueError('No regression tests selected')
    confirmed, all_ok, records = 0, True, []
    prefix = 'pawchan-device-regressions-' + time.strftime('%Y%m%d-%H%M%S-')
    with NamedTemporaryFile(dir=output, prefix=prefix, suffix='.zip', delete=False) as dest, \
            ZipFile(dest, 'w', compression=ZIP_DEFLATED) as bundle:
        for i, (module, cls, methods, selector) in enumerate(runs, 1):
            print(f'==== {i}/{len(runs)} {module}: {selector} ({len(methods)} required) ====', flush=True)
            task = ':' + module.replace('/', ':') + ':connectedDebugAndroidTest'
            args = [str(wrapper), task, '--rerun', '-Pandroid.testInstrumentationRunnerArguments.class=' + selector,
                    '-Pandroid.testInstrumentationRunnerArguments.timeout_msec=180000', '--warning-mode=fail', '--console=plain']
            report_dir = root / module / 'build/reports/androidTests/connected/debug'
            page = report_dir / (cls + '.html')
            before = report_stamp(page)
            since = int(time.time())
            code = run(args, cwd=root, check=False).returncode
            # The next Gradle invocation may replace the entire report for this module.
            passed, issues, raw = inspect_report(page, cls, methods, since, before)
            archived = archive_class(bundle, module, page, raw) if raw is not None else None
            if code:
                issues.append(f'Gradle exit: {code} for {selector}')
            confirmed += len(passed)
            all_ok = all_ok and not issues
            records.append(dict(module=module, selector=selector, required=list(methods), passed=passed,
                                issues=issues, gradle_exit=code, report=archived))
            print(f'  Confirmed passes: {len(passed)}/{len(methods)}', flush=True)
            for issue in issues:
                print('  ' + issue, file=sys.stderr, flush=True)
        ok = all_ok and confirmed == expected
        summary = dict(result='PASS' if ok else 'FAIL', confirmed=confirmed, required=expected, runs=records)
        bundle.writestr('summary.json', json.dumps(summary, indent=2, ensure_ascii=False).encode('utf-8'))
        archive_path = dest.name
    print(f'Class reports and summary: {archive_path}', flush=True)
    print(f'DEVICE REGRESSIONS: {summary["result"]} ({confirmed}/{expected} required tests confirmed; not a full device smoke).', flush=True)
    return 0 if ok else 1


def selected_cases(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--navigation-state', action='store_true',
                        help='Only verify navigation state restoration and first-frame/lifecycle guards.')
    args = parser.parse_args(argv)
    return {'app': NAVIGATION_STATE_CASES} if args.navigation_state else CASES


if __name__ == '__main__':
    sys.exit(run_regressions(ROOT, selected_cases()))
