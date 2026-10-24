import subprocess
from pathlib import Path
import tempfile

from publication_fixture import PublicationFixture, assets, delivery, state


class DeliveryTest(PublicationFixture):
    def test_new_publication_uses_verified_draft_and_never_deletes(self):
        release = self.publish()
        self.assertFalse(release['draft'])
        self.assertEqual(set(assets.names('0.1.1')), set(self.blobs[self.tag]))
        self.assertFalse(any('delete' in c or '--clobber' in c for c in self.calls))

    def test_partial_draft_upload_resumes_without_overwrite(self):
        self.partial_upload = True
        with self.assertRaises(subprocess.CalledProcessError): self.publish()
        saved = dict(self.blobs[self.tag])
        self.publish()
        for n, b in saved.items(): self.assertEqual(b, self.blobs[self.tag][n])
        self.assertEqual(1, sum(c[:5] == ('gh', 'api', '--method', 'POST', f'repos/{assets.REPO}/releases') for c in self.calls))

    def test_different_existing_draft_bytes_are_rejected(self):
        self.draft(assets.names('0.1.1')[:1])
        self.blobs[self.tag][assets.names('0.1.1')[0]] = b'different'
        with self.assertRaises(ValueError): self.publish()
        self.assertTrue(self.releases[self.tag]['draft'])

    def test_unknown_draft_asset_is_not_removed(self):
        self.draft(); self.blobs[self.tag]['unknown.txt'] = b'keep'; self.refresh()
        with self.assertRaises(ValueError): self.publish()
        self.assertIn('unknown.txt', self.blobs[self.tag])

    def test_corrupt_download_never_promotes_draft(self):
        self.corrupt_download = True
        with self.assertRaises(ValueError): self.publish()
        self.assertTrue(self.releases[self.tag]['draft'])

    def test_timed_out_successful_promotion_is_recognized(self):
        self.timeout_promote = True
        self.assertFalse(self.publish()['draft'])
        self.assertEqual(1, sum(c[:3] == ('gh', 'release', 'edit') for c in self.calls))

    def test_already_public_is_not_rebuilt_or_reuploaded(self):
        first = self.publish(); self.calls.clear()
        again = self.publish()
        self.assertEqual(first['id'], again['id'])
        self.assertFalse(any(c[:2] == ('gh', 'release') for c in self.calls))

    def test_uploaded_apk_is_verified_against_local_bytes(self):
        self.publish()
        context, mode, release = state.inspect(self.root)
        self.assertEqual('recover', mode)
        with tempfile.TemporaryDirectory() as d:
            self.assertEqual(assets.verify_files(self.root / 'dist', '0.1.1'),
                             state.verify_public(self.root, context, release, Path(d)))
