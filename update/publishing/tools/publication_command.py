"""One local push-based release command; no GitHub CLI, tokens or history rewriting."""
import argparse
from datetime import date
import json
import os
from pathlib import Path
import subprocess

from bump_version import next_version, prepare_release
from release_notes_format import explicit_sections, validate_notes

ROOT = Path(__file__).resolve().parents[3]
NAME = 'Solthariel'
EMAIL = '339961350+Solthariel@users.noreply.github.com'


def environment():
    value = os.environ.copy()
    value.update(GIT_AUTHOR_NAME=NAME, GIT_COMMITTER_NAME=NAME,
                 GIT_AUTHOR_EMAIL=EMAIL, GIT_COMMITTER_EMAIL=EMAIL)
    value.pop('GIT_AUTHOR_DATE', None)
    value.pop('GIT_COMMITTER_DATE', None)
    return value


def git(*args):
    return subprocess.check_output(('git', *args), cwd=ROOT, text=True, env=environment()).strip()


def preflight(notes):
    if git('branch', '--show-current') != 'master':
        raise ValueError('Release requires the master branch')
    url = git('remote', 'get-url', 'origin').removesuffix('.git').rstrip('/')
    if url not in ('https://github.com/Solthariel/Pawchan', 'git@github.com:Solthariel/Pawchan'):
        raise ValueError('Unexpected origin; refusing push')
    if git('status', '--porcelain', '--untracked-files=no'):
        raise ValueError('Commit tracked changes before requesting a release')
    others = git('ls-files', '--others', '--exclude-standard', '-z').split('\0')
    if any(n and (ROOT / n).resolve() != notes for n in others):
        raise ValueError('Untracked source/files exist; review and commit them first')
    git('fetch', 'origin', 'master')
    subprocess.run(('git', 'merge-base', '--is-ancestor', 'origin/master', 'HEAD'),
                   cwd=ROOT, check=True, env=environment())
    git('config', '--local', 'user.name', NAME)
    git('config', '--local', 'user.email', EMAIL)


def plan(change, notes, retry):
    metadata = ROOT / 'update/publishing/versions.json'
    latest = json.loads(metadata.read_text(encoding='utf-8'))['versions'][-1]
    if retry:
        return latest['name'], latest['code'], 'Retry existing publication; never replace a public APK'
    text = validate_notes(explicit_sections(notes.read_text(encoding='utf-8').strip()))
    print(text)
    return next_version(latest['name'], change), latest['code'] + 1, 'Prepare a NEW release'


def prepare(change, notes, retry):
    request = ROOT / 'update/publishing/release-request.json'
    if retry:
        data = json.loads(request.read_text(encoding='utf-8'))
        # Old one-time flags are not authorities in the general publication pipeline.
        data = {'version': data['version'], 'code': data['code'], 'revision': data['revision'] + 1}
        request.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
        return data['version'], [request.relative_to(ROOT).as_posix()]
    name, code = prepare_release(ROOT, change, notes.read_text(encoding='utf-8'), date.today())
    return name, ['update/publishing/versions.json', 'update/publishing/manifest/data-v1.json',
                  'update/publishing/release-request.json', f'update/publishing/changelogs/ru/{code}.txt',
                  'app/src/main/assets/settings-changelog.txt']


def commit_and_push(name, paths, retry):
    git('add', '--', *paths)
    if set(git('diff', '--cached', '--name-only').splitlines()) != set(paths):
        raise ValueError('Unexpected staged files; no push performed')
    subprocess.run(('git', 'diff', '--cached', '--check'), cwd=ROOT, env=environment(), check=True)
    title = ('Retry publication ' if retry else 'Prepare release ') + name
    git('commit', '-m', title)
    expected = f'{NAME} <{EMAIL}>'
    if git('show', '-s', '--format=%an <%ae>') != expected or git('show', '-s', '--format=%cn <%ce>') != expected:
        raise ValueError('Unexpected author/committer; no push performed')
    subprocess.run(('git', 'push', 'origin', 'HEAD:master'), cwd=ROOT, env=environment(), check=True)
    print('Request pushed. Wait for Publish signed release; publication is not yet confirmed.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument('--change', choices=('patch', 'minor', 'major'))
    modes.add_argument('--retry', action='store_true')
    parser.add_argument('--notes', type=Path)
    args = parser.parse_args()
    if args.change and not args.notes:
        parser.error('--notes is required for a new release')
    notes = args.notes.resolve() if args.notes else None
    preflight(notes)
    name, code, action = plan(args.change, notes, args.retry)
    print(f'{action}\nVersion: {name}; code: {code}\nAuthor: {NAME} <{EMAIL}>\nBranch: master')
    if input('Type YES to create the commit and push: ').strip() != 'YES':
        print('Cancelled; release files not changed.')
        return
    name, paths = prepare(args.change, notes, args.retry)
    commit_and_push(name, paths, args.retry)


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        raise SystemExit('STOP: ' + str(error))
