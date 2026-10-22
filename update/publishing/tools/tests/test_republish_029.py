import json

from republish_029_api_fixture import RepublishFixture, r


class RepublishGuardTest(RepublishFixture):
    def test_normal_request_uses_normal_publication_route(self):
        p = self.root / 'update/publishing/release-request.json'
        data = json.loads(p.read_text()); data.pop('republish_029'); p.write_text(json.dumps(data))
        self.assertFalse(r.requested(self.root))

    def test_unapproved_release_cannot_select_republication(self):
        p = self.root / 'update/publishing/release-request.json'
        data = json.loads(p.read_text()); data['version'] = '0.1.30'; p.write_text(json.dumps(data))
        with self.assertRaises(ValueError): r.requested(self.root)

    def test_prepare_is_read_only_and_flat(self):
        r.prepare(self.directory)
        self.assertEqual([], self.deletions)
        self.assertEqual(self.original, self.releases[r.TAG])
        r.verify_zip(self.directory / r.SOURCE)
        self.assertTrue((self.directory / 'backup/release.json').exists())

    def test_success_publishes_rebuilt_apk_notes_and_corrected_tag(self):
        r.prepare(self.directory); r.publish(self.directory)
        release = self.releases[r.TAG]
        self.assertFalse(release['draft'])
        self.assertEqual([r.RELEASE_ID], self.deletions)
        self.assertEqual(self.rebuilt, self.blobs[r.TAG][r.APK])
        self.assertNotEqual(self.sha, self.blobs[r.TAG][r.APK + '.sha256'])
        self.assertEqual(self.original['body'], release['body'])
        self.assertEqual(r.COMMIT, release['target_commitish'])
        self.assertEqual(4, len(release['assets']))

    def test_changed_original_release_stops_before_removal(self):
        r.prepare(self.directory); self.releases[r.TAG]['id'] += 1
        with self.assertRaises(ValueError): r.publish(self.directory)
        self.assertEqual([], self.deletions)

    def test_changed_asset_set_stops_before_removal(self):
        r.prepare(self.directory); self.releases[r.TAG]['assets'][0]['size'] += 1
        with self.assertRaises(ValueError): r.publish(self.directory)
        self.assertEqual([], self.deletions)

    def test_corrupt_staging_upload_keeps_original_public(self):
        r.prepare(self.directory); self.corrupt_staging = True
        with self.assertRaises(ValueError): r.publish(self.directory)
        self.assertEqual([], self.deletions)
        self.assertEqual(self.original, self.releases[r.TAG])
