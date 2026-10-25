"""Offline tests for comparison isolation and raw benchmark report validation."""
import importlib.util
import json
import math
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'performance'

def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / (name + '.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

prepare = load('prepare_comparison')
verify = load('verify_benchmark')

class ComparisonTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.project = self.root / 'project'
        status = self.project / prepare.STATUS
        status.parent.mkdir(parents=True)
        status.write_text('prefix\n' + prepare.AFTER + '\nsuffix\n', encoding='utf-8')

    def test_only_alignment_diff_and_original_unchanged(self):
        before = prepare.inventory(self.project)
        pair = prepare.prepare(self.project, self.root / 'pair')
        self.assertEqual([prepare.STATUS], pair['differences'])
        self.assertEqual(before, prepare.inventory(self.project))
        self.assertIn(prepare.BEFORE, (self.root / 'pair/baseline' / prepare.STATUS).read_text())

    def test_reuse_refused(self):
        output = self.root / 'pair'
        prepare.prepare(self.project, output)
        snapshot = prepare.inventory(output)
        with self.assertRaises(FileExistsError):
            prepare.prepare(self.project, output)
        self.assertEqual(snapshot, prepare.inventory(output))

    def test_nested_destination_refused(self):
        with self.assertRaises(ValueError):
            prepare.prepare(self.project, self.project / 'work')

    def test_unknown_alignment_refused(self):
        (self.project / prepare.STATUS).write_text('unknown')
        with self.assertRaises(ValueError):
            prepare.prepare(self.project, self.root / 'pair')

    def test_generated_and_secret_files_excluded(self):
        (self.project / 'build').mkdir()
        (self.project / 'build/stale.json').write_text('old')
        (self.project / 'release.jks').write_text('private')
        pair = prepare.prepare(self.project, self.root / 'pair')
        self.assertNotIn('build/stale.json', pair['candidate'])
        self.assertNotIn('release.jks', pair['candidate'])

class ReportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.path = self.root / 'probe-benchmarkData.json'
        self.data = {'context': {'build': {'model': 'fixture'}}, 'benchmarks': [{
            'className': verify.CLASS, 'name': verify.METHOD, 'repeatIterations': 5,
            'metrics': {'frameCount': {'runs': [3] * 5}},
            'sampledMetrics': {name: {'runs': [[1., 2., 3.]] * 5} for name in verify.METRICS},
            'profilerOutputs': [{'type': 'PerfettoTrace', 'filename': f'iteration{i}.perfetto-trace'} for i in range(5)]}]}
        for i in range(5):
            (self.root / f'iteration{i}.perfetto-trace').write_bytes(b'fixture-not-a-real-trace')
        (self.root / 'TEST-fixture.xml').write_text(
            f'<testsuite><testcase classname="{verify.CLASS}" name="{verify.METHOD}"/></testsuite>')
        self.save()

    def save(self):
        self.path.write_text(json.dumps(self.data), encoding='utf-8')

    def rejects(self):
        self.save()
        with self.assertRaises(ValueError):
            verify.validate(self.root)

    def test_valid_fixture_still_not_performance_pass(self):
        result = verify.validate(self.root)
        self.assertFalse(result['performancePass'])
        self.assertEqual(15, result['metrics']['frameDurationCpuMs']['pooled']['frames'])

    def test_missing_iteration(self):
        self.data['benchmarks'][0]['sampledMetrics']['frameOverrunMs']['runs'].pop()
        self.rejects()

    def test_nan_rejected(self):
        self.data['benchmarks'][0]['sampledMetrics']['frameDurationCpuMs']['runs'][0] = [math.nan]
        self.rejects()

    def test_boolean_rejected(self):
        self.data['benchmarks'][0]['sampledMetrics']['frameDurationCpuMs']['runs'][0] = [True]
        self.rejects()

    def test_metric_counts_must_match(self):
        self.data['benchmarks'][0]['sampledMetrics']['frameOverrunMs']['runs'][0] = [1.]
        self.rejects()

    def test_missing_trace(self):
        (self.root / 'iteration0.perfetto-trace').unlink()
        with self.assertRaises(ValueError):
            verify.validate(self.root)

    def test_duplicate_report_rejected(self):
        (self.root / 'other-benchmarkData.json').write_bytes(self.path.read_bytes())
        with self.assertRaises(ValueError):
            verify.validate(self.root)

    def test_wrong_identity(self):
        self.data['benchmarks'][0]['name'] = 'startup'
        self.rejects()

    def test_skipped_test_rejected(self):
        (self.root / 'TEST-fixture.xml').write_text('<testsuite><testcase><skipped/></testcase></testsuite>')
        with self.assertRaises(ValueError):
            verify.validate(self.root)

    def test_frame_count_must_match(self):
        self.data['benchmarks'][0]['metrics']['frameCount']['runs'][0] = 10
        self.rejects()

    def test_suite_error_counter_rejected(self):
        (self.root / 'TEST-fixture.xml').write_text(
            f'<testsuite errors="1"><testcase classname="{verify.CLASS}" name="{verify.METHOD}"/></testsuite>')
        with self.assertRaises(ValueError):
            verify.validate(self.root)

    def test_linear_percentile(self):
        self.assertEqual(2.5, verify.percentile([1, 2, 3, 4], 50))

if __name__ == '__main__':
    unittest.main()
