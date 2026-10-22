import json
import os
import sys
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import publication_command as command


class CommandTest(unittest.TestCase):
    def test_both_identities_are_explicit_and_dates_are_not_inherited(self):
        with patch.dict(os.environ, {'GIT_AUTHOR_NAME': 'wrong', 'GIT_COMMITTER_EMAIL': 'wrong', 'GIT_AUTHOR_DATE': 'old'}):
            env = command.environment()
        self.assertEqual(command.NAME, env['GIT_AUTHOR_NAME'])
        self.assertEqual(command.NAME, env['GIT_COMMITTER_NAME'])
        self.assertEqual(command.EMAIL, env['GIT_AUTHOR_EMAIL'])
        self.assertEqual(command.EMAIL, env['GIT_COMMITTER_EMAIL'])
        self.assertNotIn('GIT_AUTHOR_DATE', env)

    def test_cancel_does_not_prepare_commit_or_push(self):
        with patch.object(sys, 'argv', ['command', '--retry']), patch.object(command, 'preflight'), \
                patch.object(command, 'plan', return_value=('0.1.29', 30, 'retry')), \
                patch('builtins.input', return_value='NO'), patch.object(command, 'prepare') as prepare, \
                patch.object(command, 'commit_and_push') as push:
            command.main()
        prepare.assert_not_called(); push.assert_not_called()

    def test_retry_changes_only_request_revision_not_version(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d); p = root / 'update/publishing/release-request.json'; p.parent.mkdir(parents=True)
            p.write_text(json.dumps({'version': '0.1.29', 'code': 30, 'revision': 5, 'republish_029': {}}))
            with patch.object(command, 'ROOT', root): name, files = command.prepare(None, None, True)
            self.assertEqual('0.1.29', name)
            self.assertEqual(['update/publishing/release-request.json'], files)
            self.assertEqual({'version': '0.1.29', 'code': 30, 'revision': 6}, json.loads(p.read_text()))

    def test_unexpected_staged_paths_stop_before_commit(self):
        with patch.object(command, 'git', side_effect=['', 'unexpected.kt']), patch.object(command.subprocess, 'run') as run:
            with self.assertRaises(ValueError): command.commit_and_push('0.1.30', ['expected'], False)
            run.assert_not_called()
