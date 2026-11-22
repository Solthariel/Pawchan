import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import publication_command as command


class RetryTargetConsistencyTest(unittest.TestCase):
    def fixture(self, root, requested):
        folder = root / 'update/publishing'
        folder.mkdir(parents=True)
        (folder / 'versions.json').write_text(json.dumps({'versions': [{'name': '0.1.52', 'code': 53}]}))
        path = folder / 'release-request.json'
        path.write_text(json.dumps({'version': requested, 'code': 53 if requested == '0.1.52' else 52, 'revision': 1}))
        return path

    def test_mismatch_stops_before_confirmation_and_does_not_mutate_request(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            request = self.fixture(root, '0.1.51')
            before = request.read_bytes()
            with patch.object(command, 'ROOT', root), patch.object(command, 'preflight'), \
                    patch.object(sys, 'argv', ['cmd', '--retry']), patch('builtins.input') as ask, \
                    patch.object(command, 'commit_and_push') as push:
                with self.assertRaisesRegex(ValueError, 'differ'):
                    command.main()
            ask.assert_not_called()
            push.assert_not_called()
            self.assertEqual(request.read_bytes(), before)

    def test_matching_request_prepares_the_displayed_version(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            request = self.fixture(root, '0.1.52')
            with patch.object(command, 'ROOT', root):
                name, code, _ = command.plan(None, None, True)
                actual, _ = command.prepare(None, None, True)
            self.assertEqual(name, actual)
            self.assertEqual((actual, code, 2), tuple(json.loads(request.read_text())[k] for k in ('version', 'code', 'revision')))

    def test_changed_target_is_rejected_before_any_write(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            request = self.fixture(root, '0.1.51')
            before = request.read_bytes()
            with patch.object(command, 'ROOT', root), self.assertRaisesRegex(ValueError, 'changed'):
                command.prepare(None, None, True, expected=('0.1.52', 53))
            self.assertEqual(before, request.read_bytes())
