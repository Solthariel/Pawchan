import importlib.util
import subprocess
import sys
import unittest
from pathlib import Path

PATH = Path(__file__).resolve().parents[1] / "check_release_signer.py"
SPEC = importlib.util.spec_from_file_location("check_release_signer", PATH)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ReleaseSignerTest(unittest.TestCase):
    def test_official_key_matches_apk_including_case_and_duplicate_schemes(self):
        official = MODULE.RELEASE_SIGNER_SHA256
        self.assertTrue(MODULE.compatible_signer(official.upper(), f"{official}\n{official.upper()}"))

    def test_another_key_even_if_apk_matches_is_rejected(self):
        other = "a" * 64
        self.assertFalse(MODULE.compatible_signer(other, other))

    def test_missing_wrong_or_additional_signers_are_rejected(self):
        official = MODULE.RELEASE_SIGNER_SHA256
        for actual in ("", "a" * 64, official + "\n" + "a" * 64, "not-a-digest"):
            with self.subTest(actual=actual):
                self.assertFalse(MODULE.compatible_signer(official, actual))
        self.assertFalse(MODULE.compatible_signer("", official))

    def test_command_fails_closed_for_wrong_signer(self):
        args = [sys.executable, str(PATH), "--configured", "a" * 64, "--apk-signers", "a" * 64]
        result = subprocess.run(args, capture_output=True, text=True, check=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("publication stopped", result.stdout)

    def test_publish_workflow_runs_check_before_publish(self):
        workflow = (PATH.parents[3] / ".github/workflows/publish-release.yml").read_text(encoding="utf-8")
        check = workflow.index("python3 update/publishing/tools/check_release_signer.py")
        self.assertGreater(check, workflow.index("apksigner\" verify"))
        self.assertLess(check, workflow.index("release_entry.py --publish"))
