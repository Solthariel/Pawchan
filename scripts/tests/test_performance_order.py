"""Source contract checks; actual PowerShell orchestration is integration-tested separately."""
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "performance" / "Measure-ComposerPerformance.ps1"


class PerformanceOrderTests(unittest.TestCase):
    def setUp(self):
        self.source = SCRIPT.read_text(encoding="utf-8-sig")

    def test_default_order_is_backward_compatible(self):
        self.assertIn("[string]$MeasurementOrder = 'BaselineFirst'", self.source)

    def test_only_two_named_orders_are_allowed(self):
        self.assertIn("[ValidateSet('BaselineFirst', 'CandidateFirst')]", self.source)

    def test_reverse_order_and_summary_share_one_plan(self):
        self.assertIn("if ($MeasurementOrder -eq 'CandidateFirst') { @('candidate', 'baseline') } else { @('baseline', 'candidate') }", self.source)
        self.assertIn("order = @($measurementVariants); measurementOrder = $MeasurementOrder", self.source)
        self.assertIn("foreach ($variant in $measurementVariants) {\n        if ($NonInteractive)", self.source)

    def test_normal_runs_remain_interactive(self):
        self.assertIn("if ($NonInteractive -and -not $Research)", self.source)
        self.assertIn('Read-Host "Ready for ${variant}?', self.source)

    def test_both_builds_still_precede_measurement(self):
        build = self.source.index("foreach ($variant in @('baseline', 'candidate'))")
        assembly = self.source.index(":performance:target:assembleBenchmark", build)
        measure = self.source.index("foreach ($variant in $measurementVariants)")
        self.assertLess(assembly, measure)
        self.assertEqual(self.source.count("connectedBenchmarkAndroidTest"), 1)

    def test_missing_outputs_remain_fatal_before_next_variant(self):
        verify = self.source.index("'verify_benchmark.py'")
        fail = self.source.index('throw "Missing/invalid raw metrics or traces: $variant"', verify)
        complete = self.source.index("$summary.status = 'COLLECTED_REVIEW_REQUIRED'")
        self.assertLess(fail, complete)
        self.assertIn("performancePass = $false", self.source)
        self.assertNotIn("connectedDebugAndroidTest", self.source)


if __name__ == "__main__":
    unittest.main()
