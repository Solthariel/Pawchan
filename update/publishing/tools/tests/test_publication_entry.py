import argparse
import os
from pathlib import Path
import tempfile
from unittest.mock import patch

from publication_fixture import PublicationFixture, assets, state
import release_entry as entry


class EntryTest(PublicationFixture):
    def test_public_release_recovers_feed_without_build_or_upload(self):
        self.publish(); self.calls.clear()
        with patch.object(entry.feed, 'publish') as publish:
            entry.perform(argparse.Namespace(check=False, assets=False), {'phase': 'preflight'})
            publish.assert_called_once()
        self.assertFalse(any(c[:5] == ('gh', 'api', '--method', 'POST', f'repos/{assets.REPO}/releases') for c in self.calls))
        self.assertFalse(any(c[:3] in (('gh', 'release', 'create'), ('gh', 'release', 'upload')) for c in self.calls))

    def test_complete_publication_reports_noop_and_does_not_push_feed(self):
        self.complete()
        with patch.object(entry.feed, 'publish') as publish:
            entry.perform(argparse.Namespace(check=False, assets=False), {'phase': 'preflight'})
            publish.assert_not_called()

    def test_preflight_outputs_recovery_mode(self):
        self.publish()
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'output'
            with patch.dict(os.environ, {'GITHUB_OUTPUT': str(p)}):
                entry.perform(argparse.Namespace(check=True, assets=False), {'phase': 'preflight'})
            self.assertEqual('mode=recover\n', p.read_text())

    def test_completed_preflight_outputs_done_mode(self):
        self.complete()
        with patch.object(entry, 'output') as output:
            entry.perform(argparse.Namespace(check=True, assets=False), {'phase': 'preflight'})
            output.assert_called_once_with('done')

    def test_other_actor_cannot_publish(self):
        with patch.dict(os.environ, {'GITHUB_ACTOR': 'other'}):
            with self.assertRaises(ValueError): entry.verify_origin()

    def test_wrong_commit_identity_cannot_publish(self):
        with patch.object(assets, 'run', return_value='Wrong <wrong@example.invalid>'):
            with self.assertRaises(ValueError): entry.verify_origin()
