"""Refresh only the existing 0.1.29 APK integrity entry; never append duplicate history."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

from republish_029_assets import APK, APK_HASH, APK_SIZE, REPO, TAG
from release_sequence import identity, read_history


def client_package(manifest):
    client = next(item for item in manifest['applications'] if item['name'] == 'client')
    return next(item for item in client['packages'] if item['version_code'] == 30)


def check_metadata(root):
    versions = json.loads((root / 'update/publishing/versions.json').read_text())['versions']
    history = read_history(root)
    if [identity(v) for v in versions] != [identity(v) for v in history] or identity(versions[-1]) != (30, '0.1.29'):
        raise ValueError('Rebuild must preserve the complete already-published version history')
    manifest = json.loads((root / 'update/feed/data-v1.json').read_text())
    package = client_package(manifest)
    expected = (30, '0.1.29', APK_HASH, APK_SIZE, f'https://github.com/{REPO}/releases/download/{TAG}/{APK}')
    actual = tuple(package.get(k) for k in ('version_code', 'version_name', 'sha256sum', 'length', 'source'))
    if actual != expected:
        raise ValueError('Published APK feed differs from the approved baseline')
    return manifest


def updated_manifest(root, data):
    apk = root / 'dist' / APK
    client_package(data).update(length=apk.stat().st_size, sha256sum=hashlib.sha256(apk.read_bytes()).hexdigest())
    return (json.dumps(data, ensure_ascii=False, indent=2) + '\n').encode('utf-8')


def publish_feed(root):
    data = updated_manifest(root, check_metadata(root))
    def git(*args):
        return subprocess.check_output(('git', *args), cwd=root, text=True).strip()
    # A normal fast-forward push only. Never amend/rewrite history or reset the user's checkout.
    subprocess.run(('gh', 'auth', 'setup-git', '--hostname', 'github.com'), cwd=root, check=True)
    git('fetch', 'origin', 'master')
    if git('rev-parse', 'origin/master') != os.environ['GITHUB_SHA']:
        raise ValueError('Master advanced after publication; feed update needs manual recovery')
    with tempfile.TemporaryDirectory(prefix='pawchan-029-feed-') as directory:
        tree = Path(directory) / 'tree'
        git('worktree', 'add', '--detach', str(tree), 'origin/master')
        try:
            write_feed(tree, data)
        finally:
            git('worktree', 'remove', '--force', str(tree))


def write_feed(tree, data):
    def git(*args):
        return subprocess.check_output(('git', *args), cwd=tree, text=True).strip()
    path = tree / 'update/feed/data-v1.json'
    path.write_bytes(data)
    git('config', 'user.name', 'github-actions[bot]')
    git('config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com')
    git('add', '--', 'update/feed/data-v1.json')
    if git('diff', '--cached', '--name-only'):
        git('commit', '-m', 'Refresh APK integrity for republished 0.1.29')
        git('push', f'https://github.com/{REPO}.git', 'HEAD:master')
