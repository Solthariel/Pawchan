"""In-memory GitHub service with real file bytes/checksums and a real on-disk metadata tree."""
import copy
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import publication_assets as assets
import publication_delivery as delivery
import publication_state as state
import publication_feed as feed


class PublicationFixture(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.old = {'name': '0.1.0', 'code': 1, 'date': '01.01.2026', 'notes': 'Old notes'}
        self.latest = {'name': '0.1.1', 'code': 2, 'date': '02.01.2026'}
        self.notes = 'Версия 0.1.1\n\nИсправления\n- Проверка\n'
        self.tag = 'v0.1.1'
        self.sha = 'a' * 40
        self.blobs = {}
        self.releases = {}
        self.refs = {}
        self.public = {'id': 1, 'tag_name': 'v0.1.0'}
        self.calls = []
        self.timeout_promote = False
        self.partial_upload = False
        self.corrupt_download = False
        self.changed_paths = ''
        self.metadata('update/publishing/versions.json', {'versions': [self.old, self.latest]})
        self.metadata('update/feed/history-v1.json', {'schema': 1, 'releases': [self.old]})
        self.metadata('update/publishing/release-request.json', {'version': '0.1.1', 'code': 2, 'revision': 1})
        package = {'version_code': 2, 'version_name': '0.1.1', 'source': None, 'changelog': None, 'length': 0, 'sha256sum': None}
        self.metadata('update/publishing/manifest/data-v1.json', {'applications': [{'name': 'client', 'packages': [
            {'version_code': 1, 'version_name': '0.1.0'}, package]}]})
        self.metadata('update/feed/data-v1.json', {'applications': [{'name': 'client', 'packages': [
            {'version_code': 1, 'version_name': '0.1.0', 'source': 'https://old.invalid', 'sha256sum': 'old', 'length': 3}]}]})
        p = self.root / 'update/publishing/changelogs/ru/2.txt'
        p.parent.mkdir(parents=True)
        p.write_text(self.notes, encoding='utf-8')
        self.create_files()
        self.patches = [patch.object(assets, 'ROOT', self.root), patch.object(assets, 'api', self.api),
                        patch.object(assets, 'run', self.gh_run), patch.dict(os.environ, {
                            'GITHUB_REPOSITORY': assets.REPO, 'GITHUB_ACTOR': 'Solthariel', 'GITHUB_REF': 'refs/heads/master', 'GITHUB_SHA': self.sha})]
        for value in self.patches: value.start()

    def tearDown(self):
        for value in reversed(self.patches): value.stop()
        self.temp.cleanup()

    def metadata(self, name, value):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value), encoding='utf-8')

    def create_files(self):
        d = self.root / 'dist'; d.mkdir()
        apk, apk_sha, source, source_sha = assets.historical_names('0.1.1')
        (d / apk).write_bytes(b'real-test-apk-bytes')
        with zipfile.ZipFile(d / source, 'w') as z:
            for name in ('gradlew.bat', 'settings.gradle.kts', 'app/build.gradle.kts'):
                z.writestr(name, 'source')
        for name, sidecar in ((apk, apk_sha), (source, source_sha)):
            (d / sidecar).write_text(assets.checksum(d / name) + '  ' + name + '\n')

    def draft(self, subset=()):
        files = {n: (self.root / 'dist' / n).read_bytes() for n in subset}
        self.blobs[self.tag] = files
        self.releases[self.tag] = {'id': 2, 'tag_name': self.tag, 'draft': True, 'target_commitish': self.sha,
                                   'name': 'Pawchan 0.1.1', 'body': self.notes, 'assets': []}
        self.refresh()

    def refresh(self):
        self.releases[self.tag]['assets'] = [{'name': n, 'id': i + 10, 'size': len(b)}
                                             for i, (n, b) in enumerate(self.blobs[self.tag].items())]

    def api(self, path):
        if path == 'branches/master': return {'commit': {'sha': self.sha}}
        if path == 'releases/latest': return copy.deepcopy(self.public)
        if path.startswith('releases/tags/'):
            release = self.releases.get(path.split('/')[-1])
            return copy.deepcopy(release) if release and not release['draft'] else None
        if path.startswith('releases?per_page=100&page='):
            page = int(path.split('page=')[-1])
            return copy.deepcopy(list(self.releases.values())[(page - 1) * 100:page * 100])
        if path.startswith('releases/') and path.split('/')[-1].isdigit():
            return copy.deepcopy(next((r for r in self.releases.values() if r['id'] == int(path.split('/')[-1])), None))
        if path.startswith('git/ref/tags/'): return copy.deepcopy(self.refs.get(path.split('/')[-1]))
        raise AssertionError(path)

    def gh_run(self, *args, cwd=None):
        self.calls.append(args)
        if args[:2] == ('git', 'rev-parse'):
            return self.sha if args[2] == 'HEAD' else args[2].removesuffix('^{commit}')
        if args[:2] == ('git', 'diff'): return self.changed_paths
        if args[:3] == ('git', 'show', '-s'):
            return ('Solthariel <339961350+Solthariel@users.noreply.github.com>\n'
                    'Solthariel <339961350+Solthariel@users.noreply.github.com>')
        if args[:3] == ('gh', 'auth', 'setup-git'): return ''
        if args[:5] == ('gh', 'api', '--method', 'POST', f'repos/{assets.REPO}/git/refs'):
            self.refs[self.tag] = {'object': {'type': 'commit', 'sha': self.sha}}
            return ''
        if args[:5] == ('gh', 'api', '--method', 'POST', f'repos/{assets.REPO}/releases'):
            self.draft()
            self.releases[self.tag]['target_commitish'] = next(a.split('=', 1)[1] for a in args if a.startswith('target_commitish='))
            return json.dumps(self.releases[self.tag])
        if args[:3] == ('gh', 'release', 'download'):
            return self.download(args)
        if args[:3] == ('gh', 'release', 'upload'):
            for value in args[4:args.index('--repo')]:
                p = Path(value); self.blobs[self.tag][p.name] = p.read_bytes()
                self.refresh()
                if self.partial_upload:
                    self.partial_upload = False
                    raise subprocess.CalledProcessError(1, args)
            return ''
        if args[:3] == ('gh', 'release', 'edit'):
            self.releases[self.tag]['draft'] = False
            self.refs[self.tag] = {'object': {'type': 'commit', 'sha': self.releases[self.tag]['target_commitish']}}
            self.public = self.releases[self.tag]
            if self.timeout_promote:
                self.timeout_promote = False
                raise subprocess.CalledProcessError(1, args)
            return ''
        raise AssertionError(args)

    def publish(self):
        context, mode, release = state.inspect(self.root)
        return delivery.publish(self.root, context)

    def complete(self):
        self.publish()
        context, _, _ = state.inspect(self.root)
        digest, length = assets.verify_files(self.root / 'dist', '0.1.1')
        data, history = feed.contents(self.root, context, digest, length)
        self.metadata('update/feed/data-v1.json', data)
        self.metadata('update/feed/history-v1.json', history)
        path = self.root / 'update/feed/changelogs/2.txt'
        path.parent.mkdir(exist_ok=True)
        path.write_text(self.notes, encoding='utf-8')

    def download(self, args):
        directory = Path(args[args.index('--dir') + 1])
        for name, data in self.blobs[args[3]].items():
            (directory / name).write_bytes(b'corrupt' if self.corrupt_download else data)
        return ''
