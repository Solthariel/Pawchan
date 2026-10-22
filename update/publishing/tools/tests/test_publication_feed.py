import json
from pathlib import Path
import subprocess
from unittest.mock import patch

from publication_fixture import PublicationFixture, assets, feed, state


class FeedTest(PublicationFixture):
    def test_pending_feed_appends_only_requested_version(self):
        self.publish(); context = state.read_context(self.root)
        digest, length = assets.verify_files(self.root / 'dist', '0.1.1')
        data, history = feed.contents(self.root, context, digest, length)
        self.assertEqual([2, 1], [v['code'] for v in history['releases']])
        self.assertEqual(self.old, history['releases'][1])
        self.assertEqual(digest, state.package_for(data, 2)['sha256sum'])
        self.assertEqual('https://old.invalid', state.package_for(data, 1)['source'])

    def test_feed_already_committed_is_a_noop(self):
        self.complete(); context = state.read_context(self.root)
        digest, length = assets.verify_files(self.root / 'dist', '0.1.1')
        self.assertIsNone(feed.contents(self.root, context, digest, length))

    def test_feed_recovery_does_not_create_or_upload_release(self):
        self.publish(); self.calls.clear(); context = state.read_context(self.root)
        digest, length = assets.verify_files(self.root / 'dist', '0.1.1')
        with patch.object(feed, 'attempt') as attempt:
            feed.publish(self.root, context, digest, length)
            attempt.assert_called_once()
        self.assertFalse(any(c[:2] == ('gh', 'release') for c in self.calls))

    def test_transient_feed_push_failure_is_retried(self):
        context = state.read_context(self.root)
        with patch.object(feed, 'attempt', side_effect=[subprocess.CalledProcessError(1, 'push'), None]) as attempt:
            feed.publish(self.root, context, 'hash', 3)
            self.assertEqual(2, attempt.call_count)

    def test_permanent_feed_push_failure_is_not_reported_as_success(self):
        context = state.read_context(self.root)
        with patch.object(feed, 'attempt', side_effect=subprocess.CalledProcessError(1, 'push')) as attempt:
            with self.assertRaises(subprocess.CalledProcessError): feed.publish(self.root, context, 'hash', 3)
            self.assertEqual(3, attempt.call_count)

    def test_semantic_conflict_is_not_retried_or_overwritten(self):
        context = state.read_context(self.root)
        with patch.object(feed, 'attempt', side_effect=ValueError('conflict')) as attempt:
            with self.assertRaises(ValueError): feed.publish(self.root, context, 'hash', 3)
            self.assertEqual(1, attempt.call_count)

    def test_changed_completed_history_notes_are_rejected(self):
        self.complete(); context = state.read_context(self.root)
        p = self.root / 'update/feed/history-v1.json'; data = json.loads(p.read_text())
        data['releases'][0]['notes'] = 'changed'; p.write_text(json.dumps(data))
        digest, length = assets.verify_files(self.root / 'dist', '0.1.1')
        with self.assertRaises(ValueError): feed.contents(self.root, context, digest, length)

    def test_wrong_existing_feed_hash_is_not_replaced(self):
        self.complete(); context = state.read_context(self.root)
        with self.assertRaises(ValueError): feed.contents(self.root, context, 'different', 2)
