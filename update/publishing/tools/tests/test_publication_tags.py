import os
from unittest.mock import patch

from publication_fixture import PublicationFixture, assets, state


class TagTest(PublicationFixture):
    def test_tag_is_persisted_at_current_commit_before_draft(self):
        self.publish()
        create = next(i for i, c in enumerate(self.calls) if c[:5] == ('gh', 'api', '--method', 'POST', f'repos/{assets.REPO}/releases'))
        tag = next(i for i, c in enumerate(self.calls) if c[:4] == ('gh', 'api', '--method', 'POST'))
        self.assertLess(tag, create)
        self.assertEqual(self.sha, self.refs[self.tag]['object']['sha'])
        self.assertIn('tag_name=' + self.tag, self.calls[create])
        self.assertIn('target_commitish=' + self.sha, self.calls[create])

    def test_persisted_tag_can_resume_after_request_revision_commit(self):
        old = 'b' * 40
        self.refs[self.tag] = {'object': {'sha': old, 'type': 'commit'}}
        self.changed_paths = 'update/publishing/release-request.json'
        context, mode, _ = state.inspect(self.root)
        self.assertEqual(old, context['source_commit'])
        self.assertEqual('build', mode)

    def test_persisted_tag_with_different_app_code_is_rejected(self):
        self.refs[self.tag] = {'object': {'sha': 'b' * 40, 'type': 'commit'}}
        self.changed_paths = 'app/src/main/Changed.kt'
        with self.assertRaises(ValueError): state.inspect(self.root)

    def test_old_draft_without_tag_does_not_try_creating_old_commit_tag(self):
        self.draft(); self.releases[self.tag]['target_commitish'] = 'b' * 40
        self.changed_paths = 'update/publishing/release-request.json'
        with self.assertRaises(ValueError): state.inspect(self.root)
        self.assertFalse(any(c[:4] == ('gh', 'api', '--method', 'POST') for c in self.calls))
