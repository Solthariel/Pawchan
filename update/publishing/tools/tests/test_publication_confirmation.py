"""Creation response identity, bounded reads and uncached HTTP transport."""
import io
import json
import os
import unittest
from unittest.mock import patch
from urllib.parse import parse_qs, urlsplit

from publication_fixture import PublicationFixture, assets, delivery, state


class ConfirmationTest(PublicationFixture):
    def test_creation_request_contains_exact_tag_source_title_and_utf8_notes(self):
        self.publish()
        request = next(c for c in self.calls if c[:5] == (
            'gh', 'api', '--method', 'POST', f'repos/{assets.REPO}/releases'))
        for field in ('tag_name=' + self.tag, 'target_commitish=' + self.sha,
                      'name=Pawchan 0.1.1', 'body=' + self.notes, 'draft=true'):
            self.assertIn(field, request)

    def test_invalid_creation_response_never_uploads_or_promotes(self):
        responses = ('not-json', '[]', '{}', '{"id":true}', '{"id":0}',
                     json.dumps({'id': 2, 'tag_name': 'vother', 'draft': True,
                                 'target_commitish': self.sha}),
                     json.dumps({'id': 2, 'tag_name': self.tag, 'draft': False,
                                 'target_commitish': self.sha}))
        for response in responses:
            with self.subTest(response=response):
                context, _, _ = state.inspect(self.root)
                with patch.object(assets, 'run', return_value=response), self.assertRaises(ValueError):
                    delivery.create_draft(self.root, context)
        self.assertFalse(any(c[:3] in (('gh', 'release', 'upload'), ('gh', 'release', 'edit')) for c in self.calls))

    def test_wrong_id_from_confirmation_stops_immediately(self):
        context, _, _ = state.inspect(self.root)
        self.draft()
        real_api = self.api

        def wrong_id(path):
            response = real_api(path)
            if path == 'releases/2':
                response['id'] = 3
            return response

        with patch.object(assets, 'api', side_effect=wrong_id), patch.object(delivery.time, 'sleep') as sleep:
            with self.assertRaisesRegex(ValueError, 'release ID changed'):
                delivery.confirm_draft(self.root, context, 2)
        sleep.assert_not_called()

    def test_changed_release_notes_are_not_retried_or_overwritten(self):
        context, _, _ = state.inspect(self.root)
        self.draft()
        self.releases[self.tag]['body'] = 'other notes'
        with patch.object(delivery.time, 'sleep') as sleep:
            with self.assertRaisesRegex(ValueError, 'notes differ'):
                delivery.confirm_draft(self.root, context, 2)
        sleep.assert_not_called()
        self.assertEqual('other notes', self.releases[self.tag]['body'])

    def test_changed_source_context_is_not_accepted(self):
        context, _, _ = state.inspect(self.root)
        self.draft()
        self.releases[self.tag]['target_commitish'] = 'b' * 40
        self.refs[self.tag] = {'object': {'type': 'commit', 'sha': 'b' * 40}}
        with self.assertRaisesRegex(ValueError, 'context changed'):
            delivery.confirm_draft(self.root, context, 2)


class HttpFreshnessTest(unittest.TestCase):
    def test_each_read_bypasses_cached_lists_and_preserves_pagination(self):
        requests = []

        def opened(request, timeout):
            requests.append(request)
            self.assertEqual(30, timeout)
            return io.BytesIO(b'[]')

        with patch.dict(os.environ, GH_TOKEN='test-only-token'), patch('urllib.request.urlopen', side_effect=opened):
            self.assertEqual([], assets.api('releases?per_page=100&page=2'))
            self.assertEqual([], assets.api('releases?per_page=100&page=2'))
        for request in requests:
            query = parse_qs(urlsplit(request.full_url).query)
            self.assertEqual(['100'], query['per_page'])
            self.assertEqual(['2'], query['page'])
            self.assertEqual('no-cache, no-store', request.get_header('Cache-control'))
            self.assertEqual('Bearer test-only-token', request.get_header('Authorization'))
        self.assertNotEqual(requests[0].full_url, requests[1].full_url)
