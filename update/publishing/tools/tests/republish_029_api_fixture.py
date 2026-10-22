import copy
import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import republish_029 as r
import republish_029_assets as a


class RepublishFixture(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.directory = self.root / 'operation'
        self.bytes = b'verified-original-apk'
        self.digest = hashlib.sha256(self.bytes).hexdigest()
        self.sha = (self.digest + '  ' + r.APK + '\n').encode()
        self.rebuilt = b'new-verified-signed-apk'
        dist = self.root / 'dist'
        dist.mkdir()
        (dist / r.APK).write_bytes(self.rebuilt)
        (dist / (r.APK + '.sha256')).write_text(hashlib.sha256(self.rebuilt).hexdigest() + '  ' + r.APK + '\n')
        self.original = {'id': r.RELEASE_ID, 'tag_name': r.TAG, 'draft': False, 'immutable': False,
                         'name': 'Pawchan 0.1.29', 'body': 'Original release notes', 'target_commitish': r.COMMIT,
                         'assets': [{'id': 1, 'name': r.APK, 'size': len(self.bytes)},
                                    {'id': 2, 'name': r.APK + '.sha256', 'size': len(self.sha)}]}
        self.releases = {r.TAG: copy.deepcopy(self.original)}
        self.blobs = {r.TAG: {r.APK: self.bytes, r.APK + '.sha256': self.sha}}
        self.latest = r.TAG
        self.head = 'request-commit'
        self.deletions = []
        self.delete_timeout = False
        self.promote_fail = False
        self.timeout_after_promote = False
        self.corrupt_staging = False
        self.advance_after_upload = False
        self.counter = 100
        request = {'version': '0.1.29', 'code': 30, 'revision': 5, 'republish_029': {
            'release_id': r.RELEASE_ID, 'tag_commit': r.COMMIT, 'apk_sha256': r.APK_HASH}}
        target = self.root / 'update/publishing/release-request.json'
        target.parent.mkdir(parents=True)
        target.write_text(json.dumps(request), encoding='utf-8')
        self.patches = [patch.object(r, 'ROOT', self.root), patch.object(r, 'api', self.api),
                        patch.object(r, 'run', self.fake_run), patch.object(a, 'APK_HASH', self.digest),
                        patch.object(a, 'APK_SIZE', len(self.bytes)), patch.dict(os.environ, {
                            'GITHUB_REPOSITORY': r.REPO, 'GITHUB_REF': 'refs/heads/master',
                            'GITHUB_SHA': self.head, 'GITHUB_RUN_ID': '123',
                            'GITHUB_STEP_SUMMARY': str(self.root / 'summary.md')})]
        for value in self.patches:
            value.start()

    def tearDown(self):
        for value in reversed(self.patches):
            value.stop()
        self.temp.cleanup()

    def api(self, path, method='GET', payload=None):
        if path.startswith('releases/tags/'):
            return copy.deepcopy(self.releases.get(path[len('releases/tags/'):]))
        if path == 'releases/latest':
            return copy.deepcopy(self.releases.get(self.latest))
        if path == 'branches/master':
            return {'commit': {'sha': self.head}}
        if path == 'git/ref/tags/' + r.TAG:
            return {'object': {'sha': r.COMMIT, 'type': 'commit', 'url': 'test'}}
        if path.startswith('git/ref/tags/'):
            return None
        if path.startswith('releases/'):
            identity = int(path.split('/')[-1])
            key = next((tag for tag, release in self.releases.items() if release['id'] == identity), None)
            if key is None:
                raise ValueError('unexpected release id')
            if method == 'DELETE':
                # Replacement is forbidden until the complete candidate was uploaded and verified.
                assert any(x.startswith('pawchan-029-staging-') for x in self.blobs)
                self.deletions.append(identity)
                del self.releases[key]
                if self.delete_timeout:
                    self.delete_timeout = False
                    raise OSError('delete response timed out')
                return None
            if method == 'PATCH':
                if self.promote_fail:
                    raise OSError('promotion failed')
                release = self.releases.pop(key)
                release.update(payload)
                self.releases[release['tag_name']] = release
                self.blobs[release['tag_name']] = self.blobs.pop(key)
                self.latest = release['tag_name']
                if self.timeout_after_promote:
                    self.timeout_after_promote = False
                    raise OSError('response timed out after successful update')
                return copy.deepcopy(release)
        raise AssertionError((path, method))

    def fake_run(self, *args):
        if args[:2] == ('git', 'archive'):
            path = Path(next(x.split('=', 1)[1] for x in args if x.startswith('--output=')))
            with zipfile.ZipFile(path, 'w') as archive:
                for name in ('gradlew.bat', 'settings.gradle.kts', 'app/build.gradle.kts'):
                    archive.writestr(name, 'original source')
            return ''
        assert args[:2] == ('gh', 'release')
        action, tag = args[2:4]
        if action == 'download':
            directory = Path(args[args.index('--dir') + 1])
            for name, value in self.blobs[tag].items():
                if self.corrupt_staging and tag != r.TAG and name == r.APK:
                    value = b'corrupt'
                (directory / name).write_bytes(value)
            return ''
        if action == 'create':
            paths = [Path(x) for x in args[4:args.index('--repo')]]
            self.counter += 1
            self.blobs[tag] = {p.name: p.read_bytes() for p in paths}
            self.releases[tag] = {'id': self.counter, 'tag_name': tag, 'draft': True,
                                  'name': args[args.index('--title') + 1], 'body': 'Original release notes',
                                  'target_commitish': r.COMMIT, 'assets': [
                                      {'id': i + 20, 'name': p.name, 'size': p.stat().st_size}
                                      for i, p in enumerate(paths)]}
            if self.advance_after_upload:
                self.head = 'another-commit'
            return ''
        if action == 'edit':
            self.releases[tag]['draft'] = False
            self.latest = tag
            return ''
        raise AssertionError(args)
