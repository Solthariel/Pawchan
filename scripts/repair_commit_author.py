"""One-off, guarded author repair for the published 0.1.29 commit. APK bytes are untouched."""
from pathlib import Path
import argparse
import json
import os
import re
import subprocess
import urllib.request

REPO = 'Solthariel/Pawchan'
BAD = 'a5d8b3ba656259344028a731d0792f5bb8861ec6'
TAG = 'refs/tags/v0.1.29'
TREE = '5b1d0cf6b95763f608dbc63c20957896d22ab353'
RELEASE = 420811273
NAME = 'Solthariel'
EMAIL = '339961350+Solthariel@users.noreply.github.com'
ROOT = Path.cwd()


def git(*args, data=None, env=None):
    result = subprocess.run(['git', '-C', str(ROOT), *args], input=data,
                            stdout=subprocess.PIPE, check=True, env=env)
    return result.stdout


def text(*args):
    return git(*args).decode('utf-8').strip()


def api(path, payload=None):
    headers = {'Accept': 'application/vnd.github+json', 'User-Agent': 'Pawchan-author-repair'}
    token = os.environ.get('GH_TOKEN')
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request('https://api.github.com/repos/' + REPO + '/' + path,
                                    headers=headers, method='PATCH' if payload is not None else 'GET',
                                    data=json.dumps(payload).encode() if payload is not None else None)
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def author_environment(headers, fix):
    environment = os.environ.copy()
    for role in ('author', 'committer'):
        match = re.fullmatch(r'(.*) <([^<>]*)> (\d+) ([+-]\d{4})', headers[role])
        if match is None:
            raise RuntimeError('Unexpected identity format')
        name, email, timestamp, zone = match.groups()
        environment['GIT_' + role.upper() + '_NAME'] = NAME if fix else name
        environment['GIT_' + role.upper() + '_EMAIL'] = EMAIL if fix else email
        environment['GIT_' + role.upper() + '_DATE'] = '@' + timestamp + ' ' + zone
    return environment


def rewrite(commit, parents, fix):
    raw = git('cat-file', 'commit', commit)
    head, message = raw.split(b'\n\n', 1)
    headers = {}
    for line in head.decode('utf-8').splitlines():
        if line.startswith(('tree ', 'author ', 'committer ')):
            key, value = line.split(' ', 1)
            headers[key] = value
    args = ['-c', 'commit.gpgsign=false', 'commit-tree', headers['tree']]
    for parent in parents:
        args += ['-p', parent]
    new = git(*args, data=message, env=author_environment(headers, fix)).decode().strip()
    if text('rev-parse', new + '^{tree}') != headers['tree']:
        raise RuntimeError('Source tree changed unexpectedly')
    return new


def identities(commit):
    return text('show', '-s', '--format=%an%n%ae%n%cn%n%ce', commit).splitlines()


def assets(release):
    return sorted((x['id'], x['name'], x['size'], x.get('digest')) for x in release['assets'])


def check_release(release):
    if release['id'] != RELEASE or release['tag_name'] != 'v0.1.29' or release['draft']:
        raise RuntimeError('The expected public release changed; refusing mutation')
    if release.get('immutable'):
        raise RuntimeError('This release is immutable; tag repair is not permitted')


def plan(directory):
    directory.mkdir(parents=True, exist_ok=True)
    git('fetch', 'origin', 'master', '--tags')
    master = text('rev-parse', 'refs/remotes/origin/master')
    tag = text('rev-parse', TAG)
    release = api('releases/tags/v0.1.29')
    check_release(release)
    if text('cat-file', '-t', TAG) != 'commit' or text('rev-parse', tag + '^{tree}') != TREE:
        raise RuntimeError('Unexpected release tag; nothing will be rewritten')
    git('bundle', 'create', str(directory / 'before-repair.bundle'), 'refs/remotes/origin/master', TAG)
    mapping = {}
    if tag == BAD:
        base = text('rev-parse', BAD + '^')
        commits = text('rev-list', '--reverse', '--topo-order', base + '..' + master).splitlines()
        if not commits or commits[0] != BAD:
            raise RuntimeError('The target commit is not in the expected ancestry')
        previous = base
        for commit in commits:
            parents = text('rev-list', '--parents', '-n', '1', commit).split()[1:]
            if parents != [previous]:
                raise RuntimeError('Nonlinear history requires manual review; nothing pushed')
            if commit == BAD and identities(commit) != ['Vaelithra', 'vaelithra@users.noreply.github.com'] * 2:
                raise RuntimeError('Original target author differs from the allowlisted identity')
            mapping[commit] = rewrite(commit, [mapping.get(previous, previous)], commit == BAD)
            previous = commit
        new_master, new_tag = mapping[master], mapping[BAD]
    else:
        if identities(tag) != [NAME, EMAIL, NAME, EMAIL]:
            raise RuntimeError('Unexpected tag author after repair')
        new_master, new_tag = master, tag
    git('diff', '--exit-code', master, new_master)
    if identities(new_tag) != [NAME, EMAIL, NAME, EMAIL]:
        raise RuntimeError('The author repair failed verification')
    for ref in (text('for-each-ref', '--format=%(refname)', '--contains=' + BAD).splitlines() if mapping else []):
        if ref not in ('refs/heads/master', 'refs/remotes/origin/master', 'refs/remotes/origin/HEAD', TAG):
            raise RuntimeError('Another ref still contains the old commit: ' + ref)
    result = {'old_master': master, 'old_tag': tag, 'new_master': new_master,
              'new_tag': new_tag, 'mapping': mapping, 'release_before': release}
    (directory / 'plan.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print('Prepared author-only repair. All commit source trees remain identical.')
    print('New release commit: ' + new_tag)
    return result


def apply(directory):
    result = json.loads((directory / 'plan.json').read_text(encoding='utf-8'))
    release = api('releases/tags/v0.1.29')
    check_release(release)
    if assets(release) != assets(result['release_before']):
        raise RuntimeError('Release assets changed; refusing mutation')
    git('diff', '--exit-code', result['old_master'], result['new_master'])
    if result['old_master'] != result['new_master'] or result['old_tag'] != result['new_tag']:
        git('push', '--atomic',
            '--force-with-lease=refs/heads/master:' + result['old_master'],
            '--force-with-lease=' + TAG + ':' + result['old_tag'],
            'origin', result['new_master'] + ':refs/heads/master', result['new_tag'] + ':' + TAG)
    if text('rev-parse', TAG) != result['new_tag']:
        git('update-ref', TAG, result['new_tag'], result['old_tag'])
    # Existing assets/body/name remain untouched. Patch only the stored release target metadata.
    if release['target_commitish'] != result['new_tag']:
        api('releases/' + str(RELEASE), {'target_commitish': result['new_tag']})
    after = api('releases/tags/v0.1.29')
    check_release(after)
    if assets(after) != assets(release) or after['target_commitish'] != result['new_tag']:
        raise RuntimeError('Post-repair release verification failed; backup and plan are retained')
    refs = text('ls-remote', 'origin', 'refs/heads/master', TAG)
    if result['new_master'] + '\trefs/heads/master' not in refs or result['new_tag'] + '\t' + TAG not in refs:
        raise RuntimeError('Remote refs do not match the verified plan')
    print('Master and v0.1.29 now use the corrected author. APK assets were not changed.')
    print('Old SHA may remain reachable through old workflow runs, caches, forks or direct links.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=['plan', 'apply'])
    parser.add_argument('directory', type=Path)
    args = parser.parse_args()
    if os.environ.get('GITHUB_REPOSITORY') != REPO:
        raise SystemExit('This one-off repair is restricted to ' + REPO)
    try:
        plan(args.directory) if args.operation == 'plan' else apply(args.directory)
    except (RuntimeError, OSError, ValueError, KeyError, subprocess.CalledProcessError) as error:
        raise SystemExit('STOP: ' + str(error)) from error
