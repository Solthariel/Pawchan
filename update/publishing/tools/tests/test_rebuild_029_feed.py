import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import rebuild_029_feed as feed
import release_entry as entry


class RebuildFeedTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.version = {'code': 30, 'name': '0.1.29'}
        self.data = {'applications': [{'name': 'client', 'packages': [{
            'version_code': 30, 'version_name': '0.1.29', 'sha256sum': feed.APK_HASH,
            'length': feed.APK_SIZE, 'source': f'https://github.com/{feed.REPO}/releases/download/{feed.TAG}/{feed.APK}',
            'changelog': 'https://example.invalid/30.txt'}]}]}
        for name, value in [('update/publishing/versions.json', {'versions': [self.version]}),
                            ('update/feed/history-v1.json', {'schema': 1, 'releases': [self.version]}),
                            ('update/feed/data-v1.json', self.data)]:
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(value))
        (self.root / 'dist').mkdir()
        (self.root / 'dist' / feed.APK).write_bytes(b'rebuilt-apk')

    def tearDown(self):
        self.temp.cleanup()

    def test_exact_approved_feed_and_history_pass(self):
        self.assertEqual(self.data, feed.check_metadata(self.root))

    def test_changed_public_hash_is_rejected(self):
        self.data['applications'][0]['packages'][0]['sha256sum'] = 'unexpected'
        (self.root / 'update/feed/data-v1.json').write_text(json.dumps(self.data))
        with self.assertRaises(ValueError): feed.check_metadata(self.root)

    def test_missing_published_version_is_rejected(self):
        (self.root / 'update/feed/history-v1.json').write_text('{"schema": 1, "releases": []}')
        with self.assertRaises(ValueError): feed.check_metadata(self.root)

    def test_new_version_cannot_use_rebuild_exception(self):
        self.version.update(code=31, name='0.1.30')
        for path in ('update/publishing/versions.json', 'update/feed/history-v1.json'):
            key = 'versions' if 'versions' in path else 'releases'
            (self.root / path).write_text(json.dumps({'schema': 1, key: [self.version]}))
        with self.assertRaises(ValueError): feed.check_metadata(self.root)

    def test_only_hash_and_length_change_and_history_is_not_duplicated(self):
        history = (self.root / 'update/feed/history-v1.json').read_bytes()
        before = copy.deepcopy(self.data)
        result = json.loads(feed.updated_manifest(self.root, copy.deepcopy(self.data)))
        expected = before['applications'][0]['packages'][0]
        expected.update(length=11, sha256sum=hashlib.sha256(b'rebuilt-apk').hexdigest())
        self.assertEqual(before, result)
        self.assertEqual(history, (self.root / 'update/feed/history-v1.json').read_bytes())

    def test_normal_publication_uses_unchanged_publisher(self):
        with patch.object(entry.replacement, 'requested', return_value=False), \
                patch.object(entry.normal, 'main') as normal, patch.object(sys, 'argv', ['release_entry.py', '--check']):
            entry.main()
            normal.assert_called_once_with()
