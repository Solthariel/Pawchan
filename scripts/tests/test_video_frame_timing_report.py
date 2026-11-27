from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from report_video_frame_timing import CASES, TRIALS, results


def measurements(run="current"):
    return [
        f"logcat: PAW_FRAME run={run} case={case} trial={trial} first_ms=123 from_ms=100 to_ms=3880 "
        "decoder=sw decoder_drops=NA output_drops=2 model=test-device api=35"
        for case in CASES for trial in TRIALS
    ]


class VideoTimingReportTests(unittest.TestCase):
    def test_only_current_measurements_are_used_without_inventing_drops(self):
        rows = results(measurements("stale") + measurements(), "current")
        self.assertEqual(10, len(rows))
        self.assertEqual(["NA"] * 10, [r["decoder_drops"] for r in rows])
        self.assertEqual(["2"] * 10, [r["output_drops"] for r in rows])

    def test_incomplete_or_duplicate_set_is_not_a_pass(self):
        rows = measurements()
        for bad in ([], rows[:-1], rows + rows[:1], [s for s in rows if "case=fhd60" in s]):
            with self.subTest(size=len(bad)), self.assertRaises(ValueError):
                results(bad, "current")

    def test_mixed_device_or_invalid_window_or_counter_cannot_be_reported(self):
        rows = measurements()
        for before, after in (("test-device", "other-device"), ("3880", "100"), ("output_drops=2", "output_drops=-1")):
            with self.subTest(before=before), self.assertRaises(ValueError):
                results([rows[0].replace(before, after)] + rows[1:], "current")
