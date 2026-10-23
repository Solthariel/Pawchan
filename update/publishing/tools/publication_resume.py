"""Resume only reproducible NEW release metadata; never re-bump or rewrite a commit."""
from datetime import datetime
import json
from pathlib import Path
import tempfile

from bump_version import prepare_release, next_version
from release_sequence import identity, read_history, validate_local


def release_paths(code):
    return ['update/publishing/versions.json', 'update/publishing/manifest/data-v1.json',
            'update/publishing/release-request.json', f'update/publishing/changelogs/ru/{code}.txt',
            'app/src/main/assets/settings-changelog.txt']


def changed_paths(git):
    tracked = git('diff', 'HEAD', '--name-only', '--no-renames').splitlines()
    staged = git('diff', '--cached', '--name-only', '--no-renames').splitlines()
    others = git('ls-files', '--others', '--exclude-standard', '-z').split('\0')
    return set(tracked + staged + [n for n in others if n])


def reconstruct(root, git, base, latest, paths):
    """Require byte-equivalence (apart from CRLF) to the standard generator output."""
    with tempfile.TemporaryDirectory(prefix='pawchan-resume-check-') as folder:
        check = Path(folder)
        inputs = paths[:3] + ['update/feed/history-v1.json']
        for name in inputs:
            p = check / name
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(git('show', f'{base}:{name}') + '\n', encoding='utf-8', newline='\n')
        (check / 'update/publishing/changelogs/ru').mkdir(parents=True)
        (check / 'app/src/main/assets').mkdir(parents=True)
        previous = json.loads((check / paths[0]).read_text(encoding='utf-8'))['versions'][-1]
        changes = [c for c in ('patch', 'minor', 'major') if next_version(previous['name'], c) == latest['name']]
        if len(changes) != 1:
            raise ValueError('Cannot reproduce a single next release from the base commit')
        text = (root / paths[3]).read_text(encoding='utf-8')
        header = f"Версия {latest['name']}\n\n"
        if not text.startswith(header):
            raise ValueError('Unexpected pending changelog header')
        day = datetime.strptime(latest['date'], '%d.%m.%Y').date()
        prepare_release(check, changes[0], text[len(header):], day)
        for name in paths:
            actual = (root / name).read_bytes().replace(b'\r\n', b'\n')
            if actual != (check / name).read_bytes():
                raise ValueError('Pending metadata differs from standard preparation: ' + name)


def inspect(root, git):
    root = Path(root)
    versions = json.loads((root / 'update/publishing/versions.json').read_text(encoding='utf-8'))['versions']
    latest = versions[-1]
    paths = release_paths(latest['code'])
    changed = changed_paths(git)
    published = read_history(root)
    if not changed and [identity(v) for v in versions] == [identity(v) for v in published]:
        return {'mode': 'noop', 'action': 'No pending new release in this checkout. No commit or push performed.'}
    validate_local(root)
    if changed and not changed <= set(paths):
        raise ValueError('Unrelated source/staged/untracked changes; resume refuses to touch them')
    if changed:
        reconstruct(root, git, 'HEAD', latest, paths)
        # Exactly these five files must comprise the pending release; no arbitrary staging.
        if changed != set(paths):
            raise ValueError('Incomplete pending new release; inspect the files, do not bump again')
        if git('merge-base', 'origin/master', 'HEAD') != git('rev-parse', 'origin/master'):
            raise ValueError('Local branch is behind/diverged; no resume commit or push')
        mode = 'prepared'
    else:
        committed = set(git('diff-tree', '--no-commit-id', '--name-only', '--no-renames', '-r', 'HEAD').splitlines())
        if committed != set(paths):
            raise ValueError('HEAD is not a standalone standard release metadata commit')
        reconstruct(root, git, 'HEAD^', latest, paths)
        common = git('merge-base', 'origin/master', 'HEAD')
        if common == git('rev-parse', 'HEAD'):
            return {'mode': 'noop', 'action': 'Request already exists in origin/master. No duplicate commit/push; inspect Publish signed release.'}
        if common != git('rev-parse', 'origin/master'):
            raise ValueError('Branches diverged; no push performed')
        mode = 'committed'
    return {'mode': mode, 'name': latest['name'], 'code': latest['code'], 'paths': paths,
            'action': 'Resume the SAME prepared release; no version/code/revision increment'}


def normalize(root, paths):
    for name in paths:
        path = Path(root) / name
        path.write_bytes(path.read_bytes().replace(b'\r\n', b'\n'))
