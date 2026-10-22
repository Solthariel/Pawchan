"""Idempotent feed commit after verified publication, with bounded fast-forward retries."""
import copy
import json
import os
from pathlib import Path
import subprocess
import tempfile

import publication_assets as assets
import publication_state as state
from release_sequence import identity, read_history, validate_local


def contents(tree, context, digest, length):
    local = state.read_context(tree)
    if local['latest'] != context['latest'] or local['notes'] != context['notes']:
        raise ValueError('Master now requests a different release; refusing stale feed update')
    reference = assets.api('git/ref/tags/' + context['tag'])
    state.same_source(tree, reference['object']['sha'], assets.run('git', 'rev-parse', 'HEAD', cwd=tree))
    data = json.loads((tree / 'update/feed/data-v1.json').read_text(encoding='utf-8'))
    latest = context['latest']
    history = read_history(tree)
    if local['complete']:
        package = state.package_for(data, latest['code'])
        if (package['sha256sum'], package['length']) != (digest, length):
            raise ValueError('An already published feed entry has different APK bytes')
        if history[-1].get('notes') != context['notes']:
            raise ValueError('Published changelog differs')
        return None
    validate_local(tree)
    manifest = json.loads((tree / 'update/publishing/manifest/data-v1.json').read_text(encoding='utf-8'))
    package = copy.deepcopy(state.package_for(manifest, latest['code']))
    package.update(length=length, sha256sum=digest,
                   source=f"https://github.com/{assets.REPO}/releases/download/{context['tag']}/{assets.names(latest['name'])[0]}",
                   changelog=f"https://raw.githubusercontent.com/{assets.REPO}/master/update/feed/changelogs/{latest['code']}.txt")
    client = next(x for x in data['applications'] if x['name'] == 'client')
    if any(x['version_code'] == latest['code'] for x in client['packages']):
        raise ValueError('Partial/conflicting public feed entry; explicit recovery required')
    client['packages'].append(package)
    releases = sorted([*history, {**latest, 'notes': context['notes']}], key=lambda v: v['code'], reverse=True)
    return data, {'schema': 1, 'releases': releases}


def write(tree, context, values):
    data, history = values
    directory = tree / 'update/feed'
    (directory / 'changelogs').mkdir(exist_ok=True)
    for name, value in [('data-v1.json', data), ('history-v1.json', history)]:
        (directory / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (directory / f"changelogs/{context['latest']['code']}.txt").write_text(context['notes'], encoding='utf-8')
    def git(*args):
        return assets.run('git', *args, cwd=tree)
    git('config', 'user.name', 'github-actions[bot]')
    git('config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com')
    git('add', '--', 'update/feed/data-v1.json', 'update/feed/history-v1.json',
        f"update/feed/changelogs/{context['latest']['code']}.txt")
    git('commit', '-m', 'Publish update feed for ' + context['latest']['name'])
    git('push', f'https://github.com/{assets.REPO}.git', 'HEAD:master')


def attempt(root, context, digest, length):
    assets.run('git', 'fetch', 'origin', 'master', cwd=root)
    with tempfile.TemporaryDirectory(prefix='pawchan-feed-') as directory:
        tree = Path(directory) / 'tree'
        assets.run('git', 'worktree', 'add', '--detach', str(tree), 'origin/master', cwd=root)
        try:
            values = contents(tree, context, digest, length)
            if values is not None:
                write(tree, context, values)
        finally:
            assets.run('git', 'worktree', 'remove', '--force', str(tree), cwd=root)


def publish(root, context, digest, length):
    assets.run('gh', 'auth', 'setup-git', '--hostname', 'github.com', cwd=root)
    for number in range(3):
        try:
            attempt(root, context, digest, length)
            return
        except subprocess.CalledProcessError:
            # Includes a push whose response was lost. The next attempt reads remote state first.
            if number == 2:
                raise
