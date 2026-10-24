from pathlib import Path
import tempfile

from publication_fixture import PublicationFixture, assets, state


class ApkOnlyPublicationTest(PublicationFixture):
    def test_new_release_uploads_only_apk_even_if_old_local_files_exist(self):
        self.publish()
        self.assertEqual({'Pawchan-0.1.1.apk'}, set(self.blobs[self.tag]))
        uploads = [c for c in self.calls if c[:3] == ('gh', 'release', 'upload')]
        self.assertEqual(1, len(uploads))
        self.assertEqual([str(self.root / 'dist/Pawchan-0.1.1.apk')], list(uploads[0][4:uploads[0].index('--repo')]))

    def test_public_apk_has_no_sidecar_but_feed_hash_is_still_enforced(self):
        self.complete()
        self.blobs[self.tag]['Pawchan-0.1.1.apk'] = b'changed public apk'
        self.refresh()
        context, _, release = state.inspect(self.root)
        with tempfile.TemporaryDirectory() as d, self.assertRaisesRegex(ValueError, 'feed and APK differ'):
            state.verify_public(self.root, context, release, Path(d))

    def test_public_release_missing_apk_is_rejected(self):
        self.complete()
        self.blobs[self.tag].clear()
        self.refresh()
        context, _, release = state.inspect(self.root)
        with tempfile.TemporaryDirectory() as d, self.assertRaises(ValueError):
            state.verify_public(self.root, context, release, Path(d))

    def test_historical_apk_and_sidecar_still_verify(self):
        self.complete()
        name = assets.historical_names('0.1.1')[1]
        self.blobs[self.tag][name] = (self.root / 'dist' / name).read_bytes()
        self.refresh()
        context, _, release = state.inspect(self.root)
        with tempfile.TemporaryDirectory() as d:
            state.verify_public(self.root, context, release, Path(d))

    def test_historical_bad_source_checksum_is_rejected(self):
        self.complete()
        for name in assets.historical_names('0.1.1')[1:]:
            self.blobs[self.tag][name] = (self.root / 'dist' / name).read_bytes()
        self.blobs[self.tag][assets.historical_names('0.1.1')[3]] = b'wrong'
        self.refresh()
        context, _, release = state.inspect(self.root)
        with tempfile.TemporaryDirectory() as d, self.assertRaises(ValueError):
            state.verify_public(self.root, context, release, Path(d))

    def test_old_source_assets_in_new_draft_are_not_uploaded_or_deleted(self):
        self.draft()
        name = assets.historical_names('0.1.1')[2]
        self.blobs[self.tag][name] = b'old source'
        self.refresh()
        with self.assertRaises(ValueError):
            self.publish()
        self.assertEqual(b'old source', self.blobs[self.tag][name])
        self.assertFalse(any('delete' in c or '--clobber' in c for c in self.calls))
