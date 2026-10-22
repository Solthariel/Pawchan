import zipfile

from republish_029_api_fixture import RepublishFixture, r


class RepublishRecoveryTest(RepublishFixture):
    def test_concurrent_master_update_keeps_original_public(self):
        r.prepare(self.directory); self.advance_after_upload = True
        with self.assertRaises(ValueError): r.publish(self.directory)
        self.assertEqual([], self.deletions)

    def test_failed_promotion_restores_original_bytes(self):
        r.prepare(self.directory); self.promote_fail = True
        with self.assertRaises(OSError): r.publish(self.directory)
        self.assertFalse(self.releases[r.TAG]['draft'])
        self.assertEqual(self.bytes, self.blobs[r.TAG][r.APK])
        self.assertEqual(self.original['body'], self.releases[r.TAG]['body'])

    def test_timed_out_success_does_not_delete_the_new_release(self):
        r.prepare(self.directory); self.timeout_after_promote = True; r.publish(self.directory)
        self.assertEqual(4, len(self.releases[r.TAG]['assets']))
        self.assertEqual([r.RELEASE_ID], self.deletions)

    def test_immutable_original_is_not_replaced(self):
        self.releases[r.TAG]['immutable'] = True
        with self.assertRaises(ValueError): r.prepare(self.directory)
        self.assertEqual([], self.deletions)

    def test_nested_source_zip_is_rejected(self):
        path = self.root / 'bad.zip'
        with zipfile.ZipFile(path, 'w') as archive:
            archive.writestr('Pawchan-0.1.29/gradlew.bat', 'nested')
        with self.assertRaises(ValueError): r.verify_zip(path)

    def test_delete_timeout_restores_original_without_losing_apk(self):
        r.prepare(self.directory); self.delete_timeout = True
        with self.assertRaises(OSError): r.publish(self.directory)
        self.assertFalse(self.releases[r.TAG]['draft'])
        self.assertEqual(self.bytes, self.blobs[r.TAG][r.APK])

    def test_local_ide_files_are_not_allowed_in_source_archive(self):
        path = self.root / 'local-ide.zip'
        with zipfile.ZipFile(path, 'w') as archive:
            for name in ('gradlew.bat', 'settings.gradle.kts', 'app/build.gradle.kts', '.idea/workspace.xml'):
                archive.writestr(name, 'local')
        with self.assertRaises(ValueError): r.verify_zip(path)
