"""Protocol isolation and benchmark-source contracts, not Android execution tests."""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('diagnostic_verify', ROOT / 'scripts/performance/verify_benchmark.py')
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)


class DiagnosticProtocolTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        data = {'context': {}, 'benchmarks': [{
            'className': VERIFY.CLASS, 'name': VERIFY.METHOD, 'repeatIterations': 5,
            'metrics': {'frameCount': {'runs': [2] * 5}},
            'sampledMetrics': {key: {'runs': [[1, 2]] * 5} for key in VERIFY.METRICS},
            'profilerOutputs': [{'type': 'PerfettoTrace', 'filename': f't{i}.perfetto-trace'} for i in range(5)],
        }]}
        (self.root / 'test-benchmarkData.json').write_text(json.dumps(data))
        for i in range(5):
            (self.root / f't{i}.perfetto-trace').write_bytes(b'mock-not-real-trace')
        (self.root / 'TEST-result.xml').write_text(f'<testsuite><testcase classname="{VERIFY.CLASS}" name="{VERIFY.METHOD}"/></testsuite>')

    def mode(self, **values):
        (self.root / 'measurement-mode.json').write_text(json.dumps(values))

    def test_original_without_mode_remains_supported(self):
        self.assertFalse(VERIFY.validate(self.root)['diagnostics'])

    def test_diagnostic_requires_declaration(self):
        with self.assertRaises(ValueError):
            VERIFY.validate(self.root, diagnostic=True)

    def test_original_rejects_diagnostic_results(self):
        self.mode(diagnostics=True, protocol='PAW_DIAGNOSTIC_V1')
        with self.assertRaises(ValueError):
            VERIFY.validate(self.root)

    def test_diagnostic_is_explicit_and_never_pass(self):
        self.mode(diagnostics=True, protocol='PAW_DIAGNOSTIC_V1')
        result = VERIFY.validate(self.root, diagnostic=True)
        self.assertTrue(result['diagnostics'])
        self.assertFalse(result['performancePass'])
        self.assertEqual('DIAGNOSTIC_RESULTS_VALID_REVIEW_REQUIRED', result['status'])

    def test_wrong_protocol_is_rejected(self):
        self.mode(diagnostics=True, protocol='ORIGINAL')
        with self.assertRaises(ValueError):
            VERIFY.validate(self.root, diagnostic=True)

    def test_false_mode_is_not_diagnostic(self):
        self.mode(diagnostics=False, protocol='PAW_DIAGNOSTIC_V1')
        with self.assertRaises(ValueError):
            VERIFY.validate(self.root, diagnostic=True)

    def test_missing_trace_is_still_fatal(self):
        self.mode(diagnostics=True, protocol='PAW_DIAGNOSTIC_V1')
        (self.root / 't3.perfetto-trace').unlink()
        with self.assertRaises(ValueError):
            VERIFY.validate(self.root, diagnostic=True)

    def test_failed_ui_is_still_fatal(self):
        self.mode(diagnostics=True, protocol='PAW_DIAGNOSTIC_V1')
        (self.root / 'TEST-result.xml').write_text('<testsuite failures="1"><testcase><failure>bad state</failure></testcase></testsuite>')
        with self.assertRaises(ValueError):
            VERIFY.validate(self.root, diagnostic=True)


class DiagnosticSourceContracts(unittest.TestCase):
    def test_target_observers_are_disabled_by_default(self):
        source = (ROOT / 'feature/composer/src/benchmark/kotlin/com/solthariel/pawchan/composer/ComposerPerformanceActivity.kt').read_text()
        self.assertIn('getBooleanExtra("paw_diag", false)', source)
        self.assertNotIn('src/main', source)

    def test_trace_scopes_and_scroll_are_balanced(self):
        source = (ROOT / 'feature/composer/src/benchmark/kotlin/com/solthariel/pawchan/composer/ComposerPerformanceDiagnostics.kt').read_text()
        self.assertIn('finally {\n        Trace.endSection()', source)
        self.assertIn('if (!enabled) return this', source)
        self.assertIn('if (!enabled) return', source)
        for marker in ['paw.scrollY', 'paw.scroll', 'paw.measure.', 'paw.place.', 'paw.draw.']:
            self.assertIn(marker, source)

    def test_modifier_factory_is_receiver_preserving_extension(self):
        source = (ROOT / 'feature/composer/src/benchmark/kotlin/com/solthariel/pawchan/composer/ComposerPerformanceDiagnostics.kt').read_text()
        self.assertIn('internal fun Modifier.diagnosticLayout(', source)
        self.assertIn('if (!enabled) return this', source)
        self.assertIn('return this\n        .layout', source)
        self.assertNotIn('return Modifier', source)

    def test_fixture_chains_diagnostics_after_scroll(self):
        source = (ROOT / 'feature/composer/src/benchmark/kotlin/com/solthariel/pawchan/composer/ComposerPerformanceActivity.kt').read_text()
        self.assertIn('Modifier.verticalScroll(scroll).diagnosticLayout(diagnostics, expanded, research)', source)
        self.assertNotIn('.then(diagnosticLayout(', source)

    def test_driver_separates_protocol_and_validation(self):
        source = (ROOT / 'scripts/performance/Measure-ComposerPerformance.ps1').read_text()
        self.assertIn('[switch]$Diagnostics', source)
        self.assertIn('if ($Diagnostics) {', source)
        self.assertIn('pawDiagnostics=true', source)
        self.assertIn("@('--diagnostic')", source)
        self.assertIn('measurement-mode.json', source)

    def test_no_retry_and_original_test_identity(self):
        source = (ROOT / 'performance/macrobenchmark/src/main/kotlin/com/solthariel/pawchan/performance/ComposerLayoutBenchmark.kt').read_text()
        self.assertIn('fun warmErrorLayout()', source)
        self.assertIn('getOrThrow()', source)
        self.assertIn('pawdiag-failure.png', source)
        self.assertIn('pawdiag-failure-ui.xml', source)
        self.assertIn('additionalTestOutputDir', source)
        self.assertEqual(1, source.count('benchmark.measureRepeated('))
        self.assertIn('MEASURED_ITERATIONS = 5', source)
        self.assertIn('MEASURED_CYCLES = 10', source)


if __name__ == '__main__':
    unittest.main()
