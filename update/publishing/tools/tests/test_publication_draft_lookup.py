"""Realistic draft visibility and fail-closed recovery regressions."""
import copy
import io
import unittest
from unittest.mock import patch

from publication_fixture import PublicationFixture, assets, delivery, state


class DraftLookupTest(PublicationFixture):
    def test_draft_hidden_from_tag_endpoint_is_found_in_authenticated_list(self):
        self.draft()
        self.assertIsNone(assets.api('releases/tags/' + self.tag))
        context, mode, release = state.inspect(self.root)
        self.assertEqual(('build', 2), (mode, release['id']))
        self.assertEqual(self.tag, context['tag'])

    def test_new_draft_hidden_by_tag_lookup_is_uploaded_and_promoted(self):
        self.assertFalse(self.publish()['draft'])
        self.assertEqual(1, sum(c[:3] == ('gh', 'release', 'create') for c in self.calls))

    def test_existing_hidden_draft_is_resumed_without_duplicate_creation(self):
        self.draft()
        self.refs[self.tag] = {'object': {'type': 'commit', 'sha': self.sha}}
        self.assertFalse(self.publish()['draft'])
        self.assertFalse(any(c[:3] == ('gh', 'release', 'create') for c in self.calls))

    def test_lookup_checks_later_pages(self):
        self.draft()
        draft = copy.deepcopy(self.releases[self.tag])
        real_api = self.api

        def paginated(path):
            if path == 'releases?per_page=100&page=1':
                return [{'id': 100 + i, 'tag_name': f'old-{i}'} for i in range(100)]
            if path == 'releases?per_page=100&page=2':
                return [draft]
            return real_api(path)

        with patch.object(assets, 'api', side_effect=paginated):
            self.assertEqual(2, assets.find_release(self.tag)['id'])

    def test_duplicate_drafts_stop_without_creation_upload_or_promotion(self):
        self.draft()
        real_api = self.api

        def duplicate(path):
            if path == 'releases?per_page=100&page=1':
                draft = copy.deepcopy(self.releases[self.tag])
                return [draft, dict(draft, id=3)]
            return real_api(path)

        with patch.object(assets, 'api', side_effect=duplicate), self.assertRaisesRegex(ValueError, 'Multiple releases'):
            self.publish()
        self.assertFalse(any(c[:2] == ('gh', 'release') for c in self.calls))

    def test_unavailable_release_list_does_not_authorize_creation(self):
        real_api = self.api
        with patch.object(assets, 'api', side_effect=lambda path: None if path.startswith('releases?') else real_api(path)):
            with self.assertRaisesRegex(ValueError, 'release list'):
                self.publish()
        self.assertFalse(any(c[:3] == ('gh', 'release', 'create') for c in self.calls))

    def test_created_draft_not_yet_visible_fails_cleanly_without_upload(self):
        real_api = self.api
        with patch.object(assets, 'api', side_effect=lambda path: [] if path.startswith('releases?') else real_api(path)):
            with self.assertRaisesRegex(ValueError, 'Created draft was not confirmed'):
                self.publish()
        self.assertTrue(self.releases[self.tag]['draft'])
        self.assertFalse(any(c[:3] in (('gh', 'release', 'upload'), ('gh', 'release', 'edit')) for c in self.calls))
        self.assertFalse(self.publish()['draft'])
        self.assertEqual(1, sum(c[:3] == ('gh', 'release', 'create') for c in self.calls))

    def test_draft_disappearing_after_upload_does_not_get_promoted(self):
        real_api = self.api
        with patch.object(assets, 'api', side_effect=lambda path: None if path == 'releases/2' else real_api(path)):
            with self.assertRaisesRegex(ValueError, 'Draft changed during upload'):
                self.publish()
        self.assertTrue(self.releases[self.tag]['draft'])
        self.assertFalse(any(c[:3] == ('gh', 'release', 'edit') for c in self.calls))

    def test_incomplete_upload_cannot_be_promoted(self):
        self.draft()
        context, _, release = state.inspect(self.root)
        real_api = self.api
        with patch.object(assets, 'run', side_effect=lambda *args, **kwargs: '' if args[:3] == ('gh', 'release', 'upload') else self.gh_run(*args, **kwargs)):
            with self.assertRaisesRegex(ValueError, 'incomplete'):
                delivery.upload_missing(self.root, context, release)
        self.assertTrue(real_api('releases/2')['draft'])


class FixtureEncodingTest(unittest.TestCase):
    def exercise_legacy_locale(self, complete):
        original = io.open

        def legacy_open(file, mode='r', buffering=-1, encoding=None, errors=None, newline=None):
            if 'b' not in mode and encoding in (None, 'locale'):
                encoding = 'cp1251'
            if 'b' not in mode and any(flag in mode for flag in ('w', 'a', 'x')) and newline is None:
                newline = '\r\n'
            return original(file, mode, buffering, encoding, errors, newline)

        fixture = PublicationFixture()
        with patch('io.open', side_effect=legacy_open), patch(
                'io.text_encoding', side_effect=lambda encoding=None, *args: 'cp1251' if encoding in (None, 'locale') else encoding):
            fixture.setUp()
            try:
                if complete:
                    fixture.complete()
                    name = 'update/feed/changelogs/2.txt'
                else:
                    name = 'update/publishing/changelogs/ru/2.txt'
                self.assertEqual(fixture.notes, (fixture.root / name).read_text(encoding='utf-8'))
                state.inspect(fixture.root)
            finally:
                fixture.tearDown()

    def test_request_notes_remain_utf8_under_legacy_windows_locale(self):
        self.exercise_legacy_locale(False)

    def test_published_notes_remain_utf8_under_legacy_windows_locale(self):
        self.exercise_legacy_locale(True)
