"""The isolated benchmark host follows the app's no-backup resource policy."""
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / "performance/target/src/main"
ANDROID = "{http://schemas.android.com/apk/res/android}"
CREDENTIAL_DOMAINS = {"root", "file", "database", "sharedpref", "external"}
DEVICE_DOMAINS = {"device_root", "device_file", "device_database", "device_sharedpref"}


def exclusion_domains(section):
    return {node.get("domain") for node in section.findall("exclude") if node.get("path") == "."}


class PerformanceBackupRulesTests(unittest.TestCase):
    def setUp(self):
        self.app = ET.parse(MAIN / "AndroidManifest.xml").getroot().find("application")

    def resource(self, attribute):
        reference = self.app.get(ANDROID + attribute)
        self.assertIsNotNone(reference)
        self.assertTrue(reference.startswith("@xml/"))
        return ET.parse(MAIN / "res/xml" / (reference.removeprefix("@xml/") + ".xml")).getroot()

    def test_manifest_disables_backup_and_references_both_formats(self):
        self.assertEqual("false", self.app.get(ANDROID + "allowBackup"))
        self.assertEqual("full-backup-content", self.resource("fullBackupContent").tag)
        self.assertEqual("data-extraction-rules", self.resource("dataExtractionRules").tag)

    def test_legacy_rules_exclude_all_credential_domains(self):
        self.assertEqual(CREDENTIAL_DOMAINS, exclusion_domains(self.resource("fullBackupContent")))

    def test_modern_rules_exclude_cloud_and_device_transfer(self):
        rules = self.resource("dataExtractionRules")
        self.assertEqual({"cloud-backup", "device-transfer"}, {node.tag for node in rules})
        for section in rules:
            self.assertEqual(CREDENTIAL_DOMAINS | DEVICE_DOMAINS, exclusion_domains(section))
            self.assertEqual([], section.findall("include"))

    def test_matches_existing_application_backup_policy(self):
        for attribute, filename in (("fullBackupContent", "backup_rules.xml"),
                                    ("dataExtractionRules", "data_extraction_rules.xml")):
            expected = ET.parse(ROOT / "app/src/main/res/xml" / filename).getroot()
            self.assertEqual(ET.tostring(expected), ET.tostring(self.resource(attribute)))


if __name__ == "__main__":
    unittest.main()
