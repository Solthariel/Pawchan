"""Declarative manifest/adapter consistency; Kotlin tests exercise URL behavior."""
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ANDROID = "{http://schemas.android.com/apk/res/android}"
EXPECTED_HOSTS = {"2ch.hk", "2ch.su", "2ch.life", "2ch.org"}
POLICY = "app/src/main/kotlin/com/solthariel/pawchan/infrastructure/sites/DvachLinkHosts.kt"


def declared_link_hosts(text):
    match = re.search(r"\bDVACH_LINK_HOSTS\s*=\s*setOf\(([^)]*)\)", text)
    if match is None:
        raise ValueError("Missing explicit web-link alias set")
    return re.findall(r'"([^"\n]+)"', match.group(1))


def manifest_web_filters(text):
    root = ET.fromstring(text)
    filters = []
    for activity in root.findall("./application/activity"):
        if activity.get(ANDROID + "name") not in (".MainActivity", "com.solthariel.pawchan.MainActivity"):
            continue
        for entry in activity.findall("intent-filter"):
            actions = {node.get(ANDROID + "name") for node in entry.findall("action")}
            if "android.intent.action.VIEW" in actions:
                filters.append(entry)
    return filters


class IncomingLinkManifestTests(unittest.TestCase):
    def setUp(self):
        self.manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8-sig")
        self.hosts = declared_link_hosts((ROOT / POLICY).read_text(encoding="utf-8-sig"))

    def assert_manifest_contract(self, text):
        filters = manifest_web_filters(text)
        self.assertEqual(1, len(filters), "Expected one explicit VIEW filter")
        entry = filters[0]
        categories = {node.get(ANDROID + "name") for node in entry.findall("category")}
        self.assertEqual({"android.intent.category.DEFAULT", "android.intent.category.BROWSABLE"}, categories)
        data = entry.findall("data")
        hosts = [node.get(ANDROID + "host") for node in data if node.get(ANDROID + "host") is not None]
        schemes = {node.get(ANDROID + "scheme") for node in data if node.get(ANDROID + "scheme") is not None}
        self.assertEqual(len(hosts), len(set(hosts)), "Duplicate manifest host")
        self.assertEqual(EXPECTED_HOSTS, set(hosts))
        self.assertEqual(set(self.hosts), set(hosts), "Manifest and Kotlin link aliases diverged")
        self.assertEqual({"http", "https"}, schemes)

    def test_checked_in_manifest_matches_the_explicit_alias_policy(self):
        self.assertEqual(EXPECTED_HOSTS, set(self.hosts))
        self.assertEqual(len(self.hosts), len(set(self.hosts)))
        self.assert_manifest_contract(self.manifest)

    def test_previous_manifest_without_historical_alias_is_detected(self):
        old = self.manifest.replace('                <data android:host="2ch.hk" />\n', "")
        with self.assertRaises(AssertionError):
            self.assert_manifest_contract(old)

    def test_extra_or_wildcard_hosts_are_detected(self):
        for host in ("2ch.pm", "*.2ch.hk", "evil.example"):
            changed = self.manifest.replace('android:host="2ch.hk"', f'android:host="{host}"')
            with self.subTest(host=host), self.assertRaises(AssertionError):
                self.assert_manifest_contract(changed)

    def test_duplicate_hosts_are_detected(self):
        declaration = '                <data android:host="2ch.hk" />\n'
        with self.assertRaises(AssertionError):
            self.assert_manifest_contract(self.manifest.replace(declaration, declaration * 2))

    def test_missing_https_or_browsable_category_is_detected(self):
        for declaration in ('<data android:scheme="https" />',
                            '<category android:name="android.intent.category.BROWSABLE" />'):
            with self.subTest(declaration=declaration), self.assertRaises(AssertionError):
                self.assert_manifest_contract(self.manifest.replace(declaration, ""))


if __name__ == "__main__":
    unittest.main()
