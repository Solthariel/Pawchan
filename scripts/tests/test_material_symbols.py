"""Resource/provenance guards, not a substitute for Android rendering tests."""
import hashlib
import json
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
ANDROID = "{http://schemas.android.com/apk/res/android}"
MANIFEST = ROOT / "docs/licenses/material-symbols-manifest.json"


class MaterialSymbolsResourceTests(unittest.TestCase):
    def setUp(self):
        self.manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        self.resources = self.manifest["resources"]

    def test_manifest_uses_one_fixed_official_revision(self):
        revision = self.manifest["commit"]
        self.assertRegex(revision, r"^[a-f0-9]{40}$")
        prefix = f"https://raw.githubusercontent.com/google/material-design-icons/{revision}/symbols/web/"
        for entry in self.resources:
            self.assertTrue(entry["source"].startswith(prefix), entry["source"])
            self.assertIn("/materialsymbolsrounded/", entry["source"])
            self.assertRegex(entry["sourceSha256"], r"^[a-f0-9]{64}$")

    def test_every_bundled_vector_matches_its_manifest(self):
        paths = [entry["resource"] for entry in self.resources]
        self.assertEqual(len(paths), len(set(paths)))
        for entry in self.resources:
            data = (ROOT / entry["resource"]).read_bytes()
            self.assertEqual(entry["vectorSha256"], hashlib.sha256(data).hexdigest(), entry["resource"])

    def test_vectors_have_fixed_size_and_tintable_visible_geometry(self):
        for entry in self.resources:
            vector = ET.parse(ROOT / entry["resource"]).getroot()
            self.assertEqual("vector", vector.tag)
            self.assertEqual("24dp", vector.get(ANDROID + "width"))
            self.assertEqual("24dp", vector.get(ANDROID + "height"))
            self.assertIn(vector.get(ANDROID + "viewportWidth"), {"24", "960"})
            paths = list(vector.iter("path"))
            self.assertTrue(paths)
            for path in paths:
                self.assertEqual("#FFFFFFFF", path.get(ANDROID + "fillColor"))
                self.assertTrue(path.get(ANDROID + "pathData"))

    def test_standard_action_lookup_has_no_missing_or_extra_kinds(self):
        core = ROOT / "core/ui/src/main/kotlin/com/solthariel/pawchan/core/ui"
        enums = (core / "UiIcons.kt").read_text(encoding="utf-8").split("enum class UiIconKind", 1)[1]
        kinds = set(re.findall(r"^    ([A-Z_]+),", enums, re.M))
        pairs = re.findall(r"UiIconKind\.([A-Z_]+) to R\.drawable\.(\w+)",
                           (core / "RoundedIconResources.kt").read_text(encoding="utf-8"))
        self.assertEqual(kinds, {kind for kind, _ in pairs})
        self.assertEqual(len(kinds), len(pairs))
        for _, name in pairs:
            self.assertTrue((ROOT / "core/ui/src/main/res/drawable" / f"{name}.xml").is_file())

    def test_gallery_and_ratings_have_semantically_distinct_glyphs(self):
        entries = {Path(e["resource"]).stem: e for e in self.resources
                   if e["resource"].startswith("core/ui/")}
        self.assertEqual("photo_library", entries["ic_rounded_photo_library"]["symbol"])
        self.assertNotEqual(entries["ic_rounded_photo_library"]["vectorSha256"],
                            entries["ic_rounded_open_in_full"]["vectorSha256"])
        for direction in ("up", "down"):
            unfilled = entries[f"ic_rounded_thumb_{direction}"]
            filled = entries[f"ic_rounded_thumb_{direction}_fill"]
            self.assertEqual(0, unfilled["fill"])
            self.assertEqual(1, filled["fill"])
            self.assertNotEqual(unfilled["vectorSha256"], filled["vectorSha256"])

    def test_selected_bookmarks_and_stars_are_official_filled_exports(self):
        entries = {Path(e["resource"]).stem: e for e in self.resources if e["resource"].startswith("core/ui/")}
        for name in ("star", "bookmark"):
            self.assertEqual(1, entries[f"ic_rounded_{name}_fill"]["fill"])
            self.assertEqual(0, entries[f"ic_rounded_{name}"]["fill"])
            self.assertNotEqual(entries[f"ic_rounded_{name}_fill"]["vectorSha256"],
                                entries[f"ic_rounded_{name}"]["vectorSha256"])

    def test_disclosure_submenu_and_destination_are_distinct_symbols(self):
        source = (ROOT / "core/ui/src/main/kotlin/com/solthariel/pawchan/core/ui/UiNavigationIndicators.kt").read_text(encoding="utf-8")
        for symbol in ("arrow_drop_down", "arrow_drop_up", "arrow_right", "chevron_right", "arrow_back"):
            self.assertIn(f"R.drawable.ic_rounded_{symbol}", source)
        action = (ROOT / "core/ui/src/main/kotlin/com/solthariel/pawchan/core/ui/ActionMenuItem.kt").read_text(encoding="utf-8")
        self.assertIn("MenuNavigation.FORWARD) UiSubmenuIndicator()", action)

    def test_settings_keep_media_and_have_distinct_posting_and_storage_icons(self):
        source = (ROOT / "app/src/main/kotlin/com/solthariel/pawchan/settings/SettingsModel.kt").read_text(encoding="utf-8")
        for item, symbol in (("MEDIA", "IMAGE"), ("POSTING", "SEND"), ("STORAGE", "STORAGE"), ("THREADS", "FORUM")):
            self.assertIn(f"{item}(UiIconKind.{symbol},", source)

    def test_custom_post_marker_canvas_is_removed(self):
        core = ROOT / "core/ui/src/main/kotlin/com/solthariel/pawchan/core/ui"
        self.assertFalse((core / "UiPostingIcons.kt").exists())
        self.assertIn("uiIconPainter(kind)", (core / "UiIcons.kt").read_text(encoding="utf-8"))

    def test_composer_uses_add_photo_without_changing_picker_protocol(self):
        composer = [e for e in self.resources if e["resource"].endswith("ic_composer_attach.xml")]
        self.assertEqual(1, len(composer))
        self.assertEqual("add_photo_alternate", composer[0]["symbol"])


    def test_license_is_distributed_with_the_application(self):
        license_text = (ROOT / "app/src/main/assets/licenses/material-symbols.txt").read_text(encoding="utf-8")
        self.assertIn("Apache License", license_text)
        self.assertIn("Version 2.0", license_text)
        self.assertIn("google/material-design-icons", license_text)


if __name__ == "__main__":
    unittest.main()
