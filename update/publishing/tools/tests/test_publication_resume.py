"""Real local Git repositories; never connect to GitHub or rewrite history."""
from contextlib import redirect_stdout
from datetime import date
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import publication_command as command
import publication_resume as resume
from bump_version import prepare_release


class ResumeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / 'project'
        self.root.mkdir()
        self.remote = self.base / 'origin.git'
        subprocess.run(['git', 'init', '--bare', '--initial-branch=master', str(self.remote)],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.root_patch = patch.object(command, 'ROOT', self.root)
        self.root_patch.start(); self.addCleanup(self.root_patch.stop)
        self.git('init', '--initial-branch=master')
        self.git('config', 'core.autocrlf', 'false')
        self.git('config', 'user.name', command.NAME)
        self.git('config', 'user.email', command.EMAIL)
        self.git('remote', 'add', 'origin', str(self.remote))
        self.write('update/publishing/versions.json', {'versions': [{'code': 1, 'name': '0.1.0', 'date': '01.01.2026'}]})
        self.write('update/feed/history-v1.json', {'schema': 1, 'releases': [
            {'code': 1, 'name': '0.1.0', 'date': '01.01.2026', 'notes': 'old'}]})
        self.write('update/publishing/manifest/data-v1.json', {'applications': [{'name': 'client', 'packages': [
            {'version_code': 1, 'version_name': '0.1.0', 'length': 1, 'source': 'old', 'min_sdk': 30}]}]})
        self.write('update/publishing/release-request.json', {'version': '0.1.0', 'code': 1, 'revision': 1})
        (self.root / 'update/publishing/changelogs/ru').mkdir(parents=True)
        (self.root / 'update/publishing/changelogs/ru/1.txt').write_text('Old notes\n', encoding='utf8')
        (self.root / 'app/src/main/assets').mkdir(parents=True)
        (self.root / 'app/src/main/assets/settings-changelog.txt').write_text('old\n', encoding='utf8')
        (self.root / 'app.kt').write_text('original\n', encoding='utf8')
        self.git('add', '--all'); self.git('commit', '-m', 'Baseline')
        self.git('push', '-u', 'origin', 'master')
        self.original = self.git('rev-parse', 'HEAD')
        self.paths = resume.release_paths(2)

    def git(self, *args):
        return command.git(*args)

    def write(self, name, value):
        p = self.root / name; p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(value) + '\n', encoding='utf8')

    def prepared(self, crlf=False):
        prepare_release(self.root, 'patch', 'Исправления\n\n- Проверка\n', date(2026, 1, 2))
        if crlf:
            for name in self.paths:
                p = self.root / name; p.write_bytes(p.read_bytes().replace(b'\n', b'\r\n'))
        return self

    def snapshot(self):
        return {n: (self.root / n).read_bytes() for n in self.paths if (self.root / n).exists()}

    def inspect(self):
        return resume.inspect(self.root, self.git)

    def main(self, answer='YES'):
        with patch.object(sys, 'argv', ['command', '--resume']), \
                patch.object(command, 'preflight'), patch('builtins.input', return_value=answer), \
                redirect_stdout(io.StringIO()):
            command.main()

    def remote_head(self):
        return subprocess.check_output(['git', '--git-dir', str(self.remote), 'rev-parse', 'master'], text=True).strip()

    def test_resume_unstaged_crlf_and_partial_index_commits_only_same_five(self):
        self.prepared(crlf=True)
        self.git('add', '--', self.paths[0])
        plan = self.inspect()
        self.assertEqual('prepared', plan['mode'])
        self.main()
        self.assertEqual('', self.git('status', '--porcelain'))
        self.assertEqual('2', self.git('rev-list', '--count', 'HEAD'))
        self.assertEqual(self.git('rev-parse', 'HEAD'), self.remote_head())
        self.assertEqual(set(self.paths), set(self.git('diff', 'HEAD^', 'HEAD', '--name-only').splitlines()))
        request = json.loads((self.root / self.paths[2]).read_text())
        self.assertEqual({'version': '0.1.1', 'code': 2, 'revision': 1}, request)
        for data in self.snapshot().values(): self.assertNotIn(b'\r', data)
        self.assertEqual('original\n', (self.root / 'app.kt').read_text())
        self.assertEqual(command.NAME, self.git('show', '-s', '--format=%an'))
        self.assertEqual(command.EMAIL, self.git('show', '-s', '--format=%ce'))

    def test_cancel_leaves_crlf_index_and_remote_untouched(self):
        self.prepared(crlf=True)
        self.git('add', '--', self.paths[0])
        before = self.snapshot(); index = self.git('diff', '--cached')
        self.main('NO')
        self.assertEqual(before, self.snapshot())
        self.assertEqual(index, self.git('diff', '--cached'))
        self.assertEqual(self.original, self.remote_head())
        self.assertEqual(self.original, self.git('rev-parse', 'HEAD'))

    def test_already_committed_request_pushes_without_another_commit(self):
        self.prepared(); self.git('add', '--', *self.paths); self.git('commit', '-m', 'Prepare release 0.1.1')
        head = self.git('rev-parse', 'HEAD')
        self.assertEqual('committed', self.inspect()['mode'])
        self.main()
        self.assertEqual(head, self.git('rev-parse', 'HEAD'))
        self.assertEqual(head, self.remote_head())

    def test_lost_push_response_produces_no_duplicate_commit_or_push(self):
        self.prepared(); self.git('add', '--', *self.paths); self.git('commit', '-m', 'Prepare release 0.1.1')
        self.git('push', 'origin', 'master'); head = self.remote_head()
        self.assertEqual('noop', self.inspect()['mode'])
        with patch.object(command, 'commit_and_push') as push: self.main()
        push.assert_not_called()
        self.assertEqual(head, self.git('rev-parse', 'HEAD'))

    def test_completed_release_is_noop(self):
        self.assertEqual('noop', self.inspect()['mode'])
        self.assertEqual(self.original, self.remote_head())

    def test_dirty_source_untracked_and_staged_are_all_refused(self):
        for where in ('tracked', 'untracked', 'staged'):
            with self.subTest(where=where):
                self.prepared()
                p = self.root / ('extra.txt' if where == 'untracked' else 'app.kt')
                p.write_text('local change\n')
                if where == 'staged': self.git('add', '--', 'app.kt')
                before = self.snapshot()
                with self.assertRaisesRegex(ValueError, 'Unrelated'): self.inspect()
                self.assertEqual(before, self.snapshot())
                self.assertEqual(self.original, self.remote_head())
                self.git('reset', '--hard', 'HEAD'); self.git('clean', '-fd')

    def test_altered_old_manifest_bytes_are_not_normalized_or_committed(self):
        self.prepared()
        name = self.paths[1]; data = json.loads((self.root / name).read_text())
        data['applications'][0]['packages'][0]['source'] = 'tampered'
        self.write(name, data); before = self.snapshot()
        with self.assertRaisesRegex(ValueError, 'differs from standard'): self.inspect()
        self.assertEqual(before, self.snapshot())
        self.assertEqual(self.original, self.remote_head())

    def test_missing_changelog_and_missing_assets_refuse(self):
        self.prepared()
        for name in self.paths[3:]:
            p = self.root / name; data = p.read_bytes(); p.unlink()
            with self.assertRaises((OSError, ValueError)): self.inspect()
            p.write_bytes(data)
        self.assertEqual(self.original, self.remote_head())

    def test_new_plan_never_increments_an_existing_pending_release(self):
        self.prepared(); before = self.snapshot()
        notes = self.base / 'notes.txt'; notes.write_text('- Next fix\n')
        with self.assertRaisesRegex(ValueError, '-Resume'):
            command.plan('patch', notes, False)
        self.assertEqual(before, self.snapshot())

    def test_missing_git_in_nested_zip_refuses_before_any_git_command(self):
        child = self.root / 'nested/ZIP'; child.mkdir(parents=True)
        with patch.object(command, 'ROOT', child), patch.object(command, 'git') as git:
            with self.assertRaisesRegex(ValueError, 'No .git'): command.repository()
            git.assert_not_called()

    def test_wrong_actual_root_refuses_before_branch_or_fetch(self):
        with patch.object(command, 'git', return_value=str(self.root.parent)) as git:
            with self.assertRaisesRegex(ValueError, 'root differs'): command.repository()
            self.assertEqual(1, git.call_count)

    def test_unexpected_origin_refuses_before_fetch_and_local_config_change(self):
        with self.assertRaisesRegex(ValueError, 'Unexpected origin'): command.repository()
        self.assertEqual(str(self.remote), self.git('remote', 'get-url', 'origin'))

    def test_git_directory_environment_cannot_redirect_operations(self):
        with patch.dict(os.environ, {'GIT_DIR': '/wrong', 'GIT_WORK_TREE': '/wrong',
                                   'GIT_INDEX_FILE': '/wrong', 'GIT_COMMON_DIR': '/wrong'}):
            self.assertEqual(self.root.resolve(), Path(self.git('rev-parse', '--show-toplevel')).resolve())

    def test_behind_branch_is_refused_without_changing_pending_metadata(self):
        self.prepared(); before = self.snapshot()
        # Create a real remote-only descendant without editing the selected worktree.
        other = self.base / 'other'
        subprocess.run(['git', 'clone', str(self.remote), str(other)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        (other / 'other.txt').write_text('new remote source\n')
        for args in [('add', 'other.txt'), ('commit', '-m', 'Remote source'), ('push', 'origin', 'master')]:
            subprocess.run(['git', *args], cwd=other, env=command.environment(), check=True,
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.git('fetch', 'origin', 'master')
        with self.assertRaisesRegex(ValueError, 'behind/diverged'): self.inspect()
        self.assertEqual(before, self.snapshot())
        self.assertEqual(self.original, self.git('rev-parse', 'HEAD'))

    def test_resume_does_not_push_a_wrong_identity_existing_commit(self):
        self.prepared(); self.git('add', '--', *self.paths)
        env = command.environment(); env.update(GIT_AUTHOR_NAME='Other', GIT_AUTHOR_EMAIL='other@example.test')
        subprocess.run(['git', 'commit', '-m', 'Prepared'], cwd=self.root, env=env, check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        with self.assertRaisesRegex(ValueError, 'author/committer'): self.main()
        self.assertEqual(self.original, self.remote_head())

    def test_windows_text_defaults_cannot_insert_crlf_in_five_new_files(self):
        original = Path.open

        def windows_open(path, mode='r', *args, **kwargs):
            if any(flag in mode for flag in ('w', 'a', 'x')) and 'b' not in mode:
                if kwargs.get('newline') is None:
                    kwargs['newline'] = '\r\n'
            return original(path, mode, *args, **kwargs)

        with patch.object(Path, 'open', windows_open):
            self.prepared()
        for name, data in self.snapshot().items():
            with self.subTest(path=name): self.assertNotIn(b'\r', data)

    def test_retry_request_uses_lf_with_windows_text_defaults(self):
        self.prepared()
        original = Path.open

        def windows_open(path, mode='r', *args, **kwargs):
            if 'w' in mode and 'b' not in mode and kwargs.get('newline') is None:
                kwargs['newline'] = '\r\n'
            return original(path, mode, *args, **kwargs)

        with patch.object(Path, 'open', windows_open):
            name, paths = command.prepare(None, None, True)
        self.assertEqual('0.1.1', name)
        self.assertEqual([self.paths[2]], paths)
        self.assertNotIn(b'\r', (self.root / self.paths[2]).read_bytes())
        self.assertEqual(2, json.loads((self.root / self.paths[2]).read_text())['revision'])

    def test_changed_metadata_during_confirmation_stops_before_commit_or_push(self):
        self.prepared()

        def changed(_):
            (self.root / self.paths[4]).write_text('tampered after plan\n')
            return 'YES'

        with patch.object(sys, 'argv', ['command', '--resume']), patch.object(command, 'preflight'), \
                patch('builtins.input', side_effect=changed), redirect_stdout(io.StringIO()):
            with self.assertRaisesRegex(ValueError, 'differs from standard'): command.main()
        self.assertEqual(self.original, self.remote_head())
        self.assertEqual(self.original, self.git('rev-parse', 'HEAD'))

    def test_git_utf8_history_is_independent_of_windows_cp1251_default(self):
        original = subprocess.check_output
        value = 'Старые изменения: проверка истории и кириллицы'
        self.write('utf8-history.json', {'notes': value})
        # ensure_ascii=False is needed so the committed blob really contains UTF-8.
        p = self.root / 'utf8-history.json'
        p.write_text(json.dumps({'notes': value}, ensure_ascii=False), encoding='utf8')
        self.git('add', '--', 'utf8-history.json')
        self.git('commit', '-m', 'UTF8 fixture')

        def windows_output(*args, **kwargs):
            if kwargs.get('text') and kwargs.get('encoding') is None:
                kwargs['encoding'] = 'cp1251'
            return original(*args, **kwargs)

        with patch.object(subprocess, 'check_output', windows_output):
            actual = json.loads(self.git('show', 'HEAD:utf8-history.json'))
        self.assertEqual(value, actual['notes'])
