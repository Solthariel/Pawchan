"""Regression coverage for versioned Android plugin registration at the root."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ANDROID_ALIASES = ("application", "library", "test")


def root_registration_errors(root_script):
    return [name for name in ANDROID_ALIASES
            if not re.search(r"alias\(libs\.plugins\.android\." + name + r"\)\s+apply\s+false", root_script)]


class AndroidPluginRegistrationTests(unittest.TestCase):
    def test_all_versioned_android_ids_are_registered_at_root(self):
        self.assertEqual([], root_registration_errors((ROOT / "build.gradle.kts").read_text(encoding="utf-8-sig")))

    def test_previous_configuration_is_rejected(self):
        old = (ROOT / "build.gradle.kts").read_text(encoding="utf-8-sig").replace(
            "    alias(libs.plugins.android.test) apply false\n", "")
        self.assertEqual(["test"], root_registration_errors(old))

    def test_macrobenchmark_keeps_catalog_plugin_and_agp_version(self):
        catalog = (ROOT / "gradle/libs.versions.toml").read_text(encoding="utf-8-sig")
        module = (ROOT / "performance/macrobenchmark/build.gradle.kts").read_text(encoding="utf-8-sig")
        self.assertIn('android-test = { id = "com.android.test", version.ref = "agp" }', catalog)
        self.assertIn("alias(libs.plugins.android.test)", module)


if __name__ == "__main__":
    unittest.main()
