import json
import os
from pathlib import Path
import tempfile
from unittest.mock import patch
import zipfile

from publication_fixture import PublicationFixture, assets, state


class GuardTest(PublicationFixture):
    def test_nested_source_archive_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / 'source.zip'
            with zipfile.ZipFile(path, 'w') as z: z.writestr('Pawchan/gradlew.bat', 'nested')
            with self.assertRaises(ValueError): assets.verify_zip(path)

    def test_ide_cache_keys_and_unsafe_paths_are_rejected(self):
        for name in ('.idea/workspace.xml', 'app/build/output.apk', 'secret.jks', '../outside', 'local.properties'):
            with self.subTest(name=name), tempfile.TemporaryDirectory() as d:
                path = Path(d) / 'source.zip'
                with zipfile.ZipFile(path, 'w') as z:
                    for good in ('gradlew.bat', 'settings.gradle.kts', 'app/build.gradle.kts'):
                        z.writestr(good, 'source')
                    z.writestr(name, 'forbidden')
                with self.assertRaises(ValueError): assets.verify_zip(path)

    def test_wrong_apk_sidecar_is_rejected(self):
        (self.root / 'dist' / assets.names('0.1.1')[1]).write_text('wrong')
        with self.assertRaises(ValueError): self.publish()
        self.assertEqual({}, self.releases)

    def test_wrong_source_sidecar_is_rejected(self):
        (self.root / 'dist' / assets.names('0.1.1')[3]).write_text('wrong')
        with self.assertRaises(ValueError): self.publish()

    def test_request_version_disagreement_is_rejected(self):
        self.metadata('update/publishing/release-request.json', {'code': 3, 'version': '0.1.2'})
        with self.assertRaises(ValueError): state.inspect(self.root)

    def test_legacy_replacement_flag_cannot_publish_pending_version(self):
        self.metadata('update/publishing/release-request.json', {'code': 2, 'version': '0.1.1', 'replace_first_release': {'id': 1}})
        with self.assertRaises(ValueError): state.inspect(self.root)

    def test_request_with_two_pending_versions_is_rejected(self):
        versions = {'versions': [self.old, {'code': 2, 'name': '0.1.1'}, {'code': 3, 'name': '0.1.2'}]}
        self.metadata('update/publishing/versions.json', versions)
        self.metadata('update/publishing/release-request.json', {'code': 3, 'version': '0.1.2'})
        with self.assertRaises(ValueError): state.inspect(self.root)

    def test_legacy_public_apk_only_format_remains_read_only(self):
        self.complete()
        for name in assets.names('0.1.1')[2:]: self.blobs[self.tag].pop(name)
        self.refresh()
        context, mode, release = state.inspect(self.root)
        with tempfile.TemporaryDirectory() as d:
            self.assertTrue(state.verify_public(self.root, context, release, Path(d)))
        self.assertEqual('verify', mode)

    def test_new_releases_require_named_source_archive(self):
        self.complete()
        for name in assets.names('0.1.1')[2:]: self.blobs[self.tag].pop(name)
        self.refresh(); context, _, release = state.inspect(self.root)
        with patch.object(assets, 'LEGACY_LAST_CODE', 0), tempfile.TemporaryDirectory() as d:
            with self.assertRaises(ValueError): state.verify_public(self.root, context, release, Path(d))
