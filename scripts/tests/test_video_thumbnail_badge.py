"""Wiring and vector guards. Native pixels, focus and gestures are tested on Android."""
from pathlib import Path
import json
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / 'core/ui/src/main'
MEDIA = ROOT / 'core/media/src/main/kotlin/com/solthariel/pawchan/media'
APP = ROOT / 'app/src/main/kotlin/com/solthariel/pawchan'
COMPOSER = ROOT / 'feature/composer/src/main/kotlin/com/solthariel/pawchan/composer'


class VideoThumbnailBadgeTests(unittest.TestCase):
    def test_video_tiles_share_one_badge_without_changing_multiple_attachment_action(self):
        paths = [MEDIA / 'PostAttachments.kt', MEDIA / 'MediaGallery.kt', APP / 'catalog/CatalogAttachmentPicker.kt',
                 APP / 'downloads/DownloadThumbnail.kt', APP / 'threads/ThreadListCard.kt', COMPOSER / 'ComposerAttachmentGroup.kt']
        for path in paths:
            src = path.read_text(encoding='utf-8')
            self.assertEqual(1, src.count('VideoThumbnailBadge('), str(path))
            self.assertNotIn('R.string.play_video', src)
        listing = (APP / 'threads/ThreadListCard.kt').read_text(encoding='utf-8')
        self.assertRegex(listing, r'(?s)if \(attachments.size > 1\) \{\s*ThreadListMediaBadge\(attachments.size\)\s*\} else if '
                                  r'\(preview.viewer == AttachmentViewer.VIDEO\) \{\s*VideoThumbnailBadge\(\)')
        self.assertIn('if (video && image != null) VideoThumbnailBadge(compact = true)', (APP / 'downloads/DownloadThumbnail.kt').read_text(encoding='utf-8'))

    def test_uses_neutral_scrim_without_dimming_glyph_or_adding_an_input_target(self):
        src = (MEDIA / 'VideoThumbnailBadge.kt').read_text(encoding='utf-8')
        self.assertIn('VIDEO_THUMBNAIL_SCRIM_ALPHA = 0.60f', src)
        self.assertIn('Color.Black.copy(alpha = VIDEO_THUMBNAIL_SCRIM_ALPHA)', src)
        self.assertIn('CircleShape', src)
        self.assertIn('UiIconKind.VIDEO_PREVIEW_PLAY, Color.White', src)
        self.assertNotRegex(src, r'\b(IconButton|clickable|combinedClickable|pointerInput|focusable|indication|Role)\b')
        self.assertNotIn('.alpha(', src)
        self.assertIn('if (compact) 24.dp else 32.dp', src)

    def test_preview_uses_official_fill_without_changing_transport_play_icon(self):
        rows = json.loads((ROOT / 'docs/licenses/material-symbols-manifest.json').read_text(encoding='utf-8'))['resources']
        icons = {r['fill']: r for r in rows if r['symbol'] == 'play_arrow'}
        self.assertEqual({0, 1}, set(icons))
        self.assertNotEqual(icons[0]['vectorSha256'], icons[1]['vectorSha256'])
        mapping = (UI / 'kotlin/com/solthariel/pawchan/core/ui/RoundedIconResources.kt').read_text(encoding='utf-8')
        self.assertIn('UiIconKind.PLAY to R.drawable.ic_rounded_play_arrow,', mapping)
        self.assertIn('UiIconKind.VIDEO_PREVIEW_PLAY to R.drawable.ic_rounded_play_arrow_fill,', mapping)
        self.assertNotIn('VideoThumbnailBadge', (MEDIA / 'video/VideoControlRows.kt').read_text(encoding='utf-8'))

    def test_unicode_triangle_is_not_shipped_as_a_string_in_any_locale(self):
        for module in ('app', 'feature/composer'):
            for file in (ROOT / module / 'src/main/res').glob('values*/strings.xml'):
                self.assertIsNone(ET.parse(file).getroot().find("string[@name='play_video']"), str(file))
