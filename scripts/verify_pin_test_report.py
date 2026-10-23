"""Require fresh, complete device XML results; a successful Gradle exit is insufficient."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET

EXPECTED = {
    'com.solthariel.pawchan.DebugPackageIsolationRuntimeTest': 2,
    'com.solthariel.pawchan.PinEditorInputRuntimeTest': 5,
}

KNOWN_TESTS = {
    **EXPECTED,
    'com.solthariel.pawchan.PinUnlockLayoutRuntimeTest': 5,
    'com.solthariel.pawchan.AppLockStoreRuntimeTest': 3,
    'com.solthariel.pawchan.SecuritySettingsLayoutRuntimeTest': 3,
}
REPORT_MODULES = {
    'com.solthariel.pawchan.DebugPackageIsolationRuntimeTest': 'app',
    'com.solthariel.pawchan.PinEditorInputRuntimeTest': 'feature/applock',
    'com.solthariel.pawchan.PinUnlockLayoutRuntimeTest': 'feature/applock',
    'com.solthariel.pawchan.AppLockStoreRuntimeTest': 'feature/applock',
    'com.solthariel.pawchan.SecuritySettingsLayoutRuntimeTest': 'app',
}


def verify(root, since, class_name=None):
    if class_name is not None and class_name not in KNOWN_TESTS:
        raise ValueError("Unknown test class: " + class_name)
    expected = {class_name: KNOWN_TESTS[class_name]} if class_name else EXPECTED
    report_roots = {name: root / module / 'build/outputs/androidTest-results/connected/debug'
                    for name, module in REPORT_MODULES.items()}
    reports = {p for directory in report_roots.values() for p in directory.rglob('TEST-*.xml')}
    seen = set()
    counts = {name: 0 for name in expected}
    for report in sorted(reports):
        if report.stat().st_mtime < since:
            continue
        tree = ET.parse(report)
        cases = tree.findall('.//testcase')
        if not any(case.get('classname') in expected for case in cases):
            continue
        for tag in ('failure', 'error', 'skipped'):
            if tree.find('.//' + tag) is not None:
                raise ValueError(f'{report.name}: contains {tag}')
        suites = tree.findall('.//testsuite')
        if tree.getroot().tag == 'testsuite':
            suites.append(tree.getroot())
        if any(int(suite.get(key, '0')) for suite in suites for key in ('failures', 'errors', 'skipped')):
            raise ValueError(f'{report.name}: nonzero failure/error/skipped count')
        for case in cases:
            name = case.get('classname')
            if name not in expected:
                continue
            if not report.is_relative_to(report_roots[name]):
                raise ValueError('Test result belongs to the wrong module: ' + name)
            identity = (name, case.get('name'))
            if not identity[1] or identity in seen:
                raise ValueError('Missing/duplicate test identity: ' + str(identity))
            seen.add(identity)
            counts[name] += 1
    if counts != expected:
        raise ValueError('Expected fresh device results ' + str(expected) + '; received ' + str(counts))
    return counts


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--since', type=int, required=True)
    parser.add_argument('--class', dest='class_name', choices=tuple(KNOWN_TESTS))
    args = parser.parse_args()
    try:
        for name, count in verify(args.root, args.since, args.class_name).items():
            print(f'{name}: {count} passed, no failures/errors/skips')
    except (OSError, ValueError, ET.ParseError) as error:
        raise SystemExit('DEVICE CHECK FAILED: ' + str(error))
