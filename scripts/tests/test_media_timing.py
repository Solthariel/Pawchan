"""Collector behavior tests; mocked adb is not an Android runtime measurement."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location("media_timing", ROOT / "scripts/performance/media_timing.py")
TIMING = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(TIMING)


class MediaTimingTests(unittest.TestCase):
    def test_requires_one_authorized_device(self):
        self.assertEqual("phone", TIMING.choose_device("List of devices attached\nphone device\nother unauthorized", None))
        for text in ("", "p unauthorized", "p device\nq device"):
            with self.assertRaises(RuntimeError):
                TIMING.choose_device(text, None)
        self.assertEqual("q", TIMING.choose_device("p device\nq device", "q"))
        with self.assertRaises(RuntimeError):
            TIMING.choose_device("p device", "q")

    def test_only_numeric_probe_events_are_parsed(self):
        data = TIMING.summarize("noise\nprobe=-2 elapsed_ms=5 event=GET_HEADERS value=200\nprobe=-2 elapsed_ms=9 event=GET_LENGTH value=-1")
        self.assertEqual(2, len(data["-2"]))
        self.assertEqual(-1, data["-2"][1]["value"])
        self.assertEqual({}, TIMING.summarize("unrelated application log"))

    def test_restores_previous_level_even_when_logcat_fails(self):
        calls = []

        def adb(*args):
            calls.append(args)
            if "getprop" in args and args[-1] == "log.tag.PawchanMediaTiming":
                return "DEBUG" if len([call for call in calls if "getprop" in call and call[-1] == args[-1]]) > 1 else "INFO"
            return "test-device"

        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "capture"
            with patch.object(TIMING, "run", side_effect=adb), patch.object(TIMING.subprocess, "Popen", side_effect=OSError("failed")):
                with self.assertRaises(OSError):
                    TIMING.capture("adb", "phone", 10, output)
            self.assertTrue(any(call[-1] == "'INFO'" and "setprop" in call for call in calls))
            self.assertTrue(output.with_suffix(".zip").is_file())
            self.assertIsNone(json.loads((output / "capture.json").read_text())["restore_error"])

    def test_empty_level_restored_explicitly(self):
        calls = []
        tag_reads = 0

        def adb(*args):
            nonlocal tag_reads
            calls.append(args)
            if "getprop" in args and args[-1] == "log.tag.PawchanMediaTiming":
                tag_reads += 1
                return "" if tag_reads == 1 else "DEBUG"
            return "model"

        with tempfile.TemporaryDirectory() as directory:
            with patch.object(TIMING, "run", side_effect=adb), patch.object(TIMING.subprocess, "Popen", side_effect=OSError("failed")):
                with self.assertRaises(OSError):
                    TIMING.capture("adb", "phone", 10, Path(directory) / "capture")
        self.assertTrue(any(call[-1] == "''" and "setprop" in call for call in calls))
