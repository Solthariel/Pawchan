"""Configuration guards; runtime authentication is covered by Kotlin/device tests."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]


class PrivateDevConfigurationTests(unittest.TestCase):
    def test_release_is_isolated_and_optimized(self):
        build = (ROOT / 'app/build.gradle.kts').read_text()
        release = build.split('    buildTypes {', 1)[1].split('        release {', 1)[1].split('    compileOptions {', 1)[0]
        for required in ('applicationIdSuffix = ".dev"', 'isDebuggable = false',
                         'isMinifyEnabled = true', 'isShrinkResources = true'):
            self.assertIn(required, release)
        self.assertIn('signingConfigs.findByName("release")', release)

    def test_release_label_and_private_repo_are_explicit(self):
        strings = ET.parse(ROOT / 'app/src/release/res/values/strings.xml').getroot()
        self.assertEqual('Pawchan Dev', strings.find("string[@name='app_name']").text)
        build = (ROOT / 'app/build.gradle.kts').read_text()
        self.assertIn('PRIVATE_UPDATE_REPOSITORY', build)
        self.assertIn('Solthariel/Pawchan', build)

    def test_no_credential_is_a_build_config_field(self):
        build = (ROOT / 'app/build.gradle.kts').read_text()
        fields = re.findall(r'buildConfigField\(\s*"[^"]+",\s*"([^"]+)"', build)
        self.assertFalse(any('TOKEN' in name or 'PASSWORD' in name for name in fields))

    def test_changelog_settings_lines_fit_detekt_limit(self):
        # Regression for the Detekt MaxLineLength failure in the first Dev patch.
        source = ROOT / 'app/src/main/kotlin/com/solthariel/pawchan/settings/ChangelogSettings.kt'
        too_long = [(number, len(line)) for number, line in enumerate(source.read_text().splitlines(), 1)
                    if len(line) > 120]
        self.assertEqual([], too_long)

    def test_private_token_file_is_in_no_backup_storage(self):
        store = (ROOT / 'update/src/main/kotlin/com/solthariel/pawchan/update/AndroidUpdateTokenStore.kt').read_text()
        self.assertIn('app.noBackupFilesDir', store)
        self.assertIn('"AndroidKeyStore"', store)
        self.assertNotIn('getSharedPreferences(', store)

    def test_all_device_scripts_include_update_runtime_tests(self):
        for suffix in ('ps1', 'sh'):
            script = (ROOT / ('scripts/device-smoke.' + suffix)).read_text()
            self.assertIn(':update:connectedDebugAndroidTest', script)


if __name__ == '__main__':
    unittest.main()
