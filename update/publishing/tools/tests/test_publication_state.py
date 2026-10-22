import json
import os
from pathlib import Path
import tempfile
from unittest.mock import patch

from publication_fixture import PublicationFixture, assets, state


class StateTest(PublicationFixture):
    def test_missing_release_selects_build(self):
        self.assertEqual('build', state.inspect(self.root)[1])

    def test_public_release_without_feed_selects_recovery(self):
        self.publish()
        self.assertEqual('recover', state.inspect(self.root)[1])

    def test_complete_release_selects_verified_noop(self):
        self.complete()
        self.assertEqual('verify', state.inspect(self.root)[1])

    def test_missing_published_release_does_not_trigger_rebuild(self):
        self.complete(); self.releases.clear()
        with self.assertRaises(ValueError): state.inspect(self.root)

    def test_application_changes_require_new_version(self):
        self.publish(); self.changed_paths = 'app/src/main/kotlin/Changed.kt'
        with self.assertRaises(ValueError): state.inspect(self.root)

    def test_request_revision_only_can_recover(self):
        self.publish(); self.changed_paths = 'update/publishing/release-request.json'
        self.assertEqual('recover', state.inspect(self.root)[1])

    def test_wrong_repository_is_rejected(self):
        with patch.dict(os.environ, {'GITHUB_REPOSITORY': 'someone/else'}):
            with self.assertRaises(ValueError): state.inspect(self.root)

    def test_newer_release_blocks_stale_recovery(self):
        self.publish(); self.public = {'id': 99, 'tag_name': 'v0.1.2'}
        with self.assertRaises(ValueError): state.inspect(self.root)

    def test_public_apk_hash_cannot_be_silently_changed(self):
        self.complete()
        p = self.root / 'update/feed/data-v1.json'; data = json.loads(p.read_text())
        state.package_for(data, 2)['sha256sum'] = 'wrong'; p.write_text(json.dumps(data))
        context, _, release = state.inspect(self.root)
        with tempfile.TemporaryDirectory() as d, self.assertRaises(ValueError):
            state.verify_public(self.root, context, release, Path(d))

    def test_another_commit_draft_is_rejected_when_code_changed(self):
        self.draft(); self.changed_paths = 'feature/thread/Changed.kt'
        with self.assertRaises(ValueError): state.inspect(self.root)
