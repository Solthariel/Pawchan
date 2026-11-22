"""Configuration guards; anonymous transport is covered by Kotlin/device tests."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]


class ReleaseConfigurationTests(unittest.TestCase):
    def test_release_has_canonical_id_and_is_optimized(self):
        build = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
        release = build.split('    buildTypes {', 1)[1].split('        release {', 1)[1].split('    compileOptions {', 1)[0]
        for required in ('isDebuggable = false',
                         'isMinifyEnabled = true', 'isShrinkResources = true'):
            self.assertIn(required, release)
        self.assertIn('signingConfigs.findByName("release")', release)
        self.assertNotRegex(release, r'applicationIdSuffix\s*=')
        self.assertIn('applicationId = "com.solthariel.pawchan"', build)
        self.assertIn('applicationIdSuffix = ".debug"', build)


    def test_release_label_and_anonymous_updates_are_explicit(self):
        strings = ET.parse(ROOT / 'app/src/main/res/values/strings.xml').getroot()
        self.assertEqual('Pawchan', strings.find("string[@name='app_name']").text)
        self.assertFalse((ROOT / 'app/src/release/res/values/strings.xml').exists())
        build = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
        settings = ROOT / 'app/src/main/kotlin/com/solthariel/pawchan/settings'
        configuration = (settings / 'ReleaseUpdateConfiguration.kt').read_text(encoding='utf-8')
        self.assertNotIn('PRIVATE_UPDATE_REPOSITORY', build)
        self.assertNotIn('privateRepository =', configuration)
        self.assertIn('manifestUrl = BuildConfig.UPDATE_MANIFEST_URL', configuration)
        self.assertIn('releaseUpdateConfiguration()', (settings / 'SettingsScreen.kt').read_text(encoding='utf-8'))
        self.assertIn('https://raw.githubusercontent.com/Solthariel/Pawchan/', build)

    def test_public_links_no_longer_point_to_lunachan(self):
        settings = ROOT / 'app/src/main/kotlin/com/solthariel/pawchan/settings'
        about = (settings / 'AboutDetails.kt').read_text(encoding='utf-8')
        licenses = (settings / 'LicenseDocument.kt').read_text(encoding='utf-8')
        self.assertIn('Solthariel/Pawchan/releases/latest', about)
        self.assertNotIn('Solthariel/Lunachan', about + licenses)

    def test_native_licenses_are_bundled_in_the_installed_app(self):
        bundled = ROOT / 'app/src/main/assets/licenses'
        original = ROOT / 'third-party/libmpvkt'
        expected = {
            'libmpvkt-Apache-2.0.txt': 'LIBMPVKT-LICENSE.txt',
            'libmpvkt-NOTICE.txt': 'LIBMPVKT-NOTICE.txt',
            'android-ndk-sysroot-NOTICE.txt': 'ANDROID-NDK-SYSROOT-NOTICE.txt',
        }
        documents = (ROOT / 'app/src/main/kotlin/com/solthariel/pawchan/settings/LicenseDocument.kt').read_text(encoding='utf-8')
        for installed, source in expected.items():
            self.assertEqual((original / source).read_bytes(), (bundled / installed).read_bytes())
            self.assertIn(f'licenses/{installed}', documents)

    def test_pull_request_ci_is_read_only_and_cannot_publish(self):
        workflow = (ROOT / '.github/workflows/check-pull-request.yml').read_text(encoding='utf-8')
        self.assertIn('pull_request:', workflow)
        self.assertIn('contents: read', workflow)
        self.assertIn('./scripts/local-check.sh', workflow)
        self.assertNotIn('release_entry.py --publish', workflow)
        self.assertNotIn('PAWCHAN_KEYSTORE_BASE64', workflow)

    def test_changelog_uses_regular_feed_without_private_access(self):
        source = ROOT / 'app/src/main/kotlin/com/solthariel/pawchan/settings/ChangelogSettings.kt'
        settings = source.read_text(encoding='utf-8')
        self.assertIn('val url = historyUrl(BuildConfig.UPDATE_MANIFEST_URL)', settings)
        self.assertNotIn('PRIVATE_UPDATE_REPOSITORY', settings)
        self.assertNotIn('AndroidUpdateTokenStore', settings)

    def test_no_credential_is_a_build_config_field(self):
        build = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
        fields = re.findall(r'buildConfigField\(\s*"[^"]+",\s*"([^"]+)"', build)
        self.assertFalse(any('TOKEN' in name or 'PASSWORD' in name for name in fields))

    def test_changelog_settings_lines_fit_detekt_limit(self):
        # Regression for the Detekt MaxLineLength failure in the first release configuration patch.
        source = ROOT / 'app/src/main/kotlin/com/solthariel/pawchan/settings/ChangelogSettings.kt'
        too_long = [(number, len(line)) for number, line in enumerate(source.read_text(encoding='utf-8').splitlines(), 1)
                    if len(line) > 120]
        self.assertEqual([], too_long)

    def test_updater_has_no_private_access_implementation(self):
        source = ROOT / 'update/src/main/kotlin/com/solthariel/pawchan/update'
        retired = ('UpdateTokenStore', 'UpdateTokenCipher', 'PrivateUpdateAccess',
                   'GithubUpdateAuth', 'GithubUpdateUrls', 'GithubReleaseAsset',
                   'privateRepository', 'ACCESS_REQUIRED', 'removeSavedUpdateAccess')
        production = '\n'.join(path.read_text(encoding='utf-8') for path in source.glob('*.kt'))
        for symbol in retired:
            self.assertNotIn(symbol, production)
        self.assertNotIn('AndroidKeyStore', production)
        self.assertNotIn('private-update-access.bin', production)
        self.assertNotIn('Authorization', production)

    def test_all_device_scripts_include_update_runtime_tests(self):
        for suffix in ('ps1', 'sh'):
            script = (ROOT / ('scripts/device-smoke.' + suffix)).read_text(encoding='utf-8')
            self.assertIn(':update:connectedDebugAndroidTest', script)


if __name__ == '__main__':
    unittest.main()
