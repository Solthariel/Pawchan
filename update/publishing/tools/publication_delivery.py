"""Resume compatible drafts; upload missing files only; promote only verified assets."""
import os
import subprocess
import tempfile
from pathlib import Path

import publication_assets as assets
import publication_state as state


def publish(root, context):
    directory = root / 'dist'
    assets.verify_files(directory, context['latest']['name'])
    current, mode, release = state.inspect(root)
    if mode != 'build':
        return release
    if current != context:
        raise ValueError('Publication context changed')
    branch = assets.api('branches/master')
    if not branch or branch['commit']['sha'] != os.environ['GITHUB_SHA']:
        raise ValueError('Master advanced; refusing stale publication')
    if release is None:
        if assets.api('git/ref/tags/' + context['tag']) is None:
            assets.run('gh', 'api', '--method', 'POST', f'repos/{assets.REPO}/git/refs',
                       '-f', 'ref=refs/tags/' + context['tag'], '-f', 'sha=' + os.environ['GITHUB_SHA'])
        assets.run('gh', 'release', 'create', context['tag'], '--repo', assets.REPO,
                   '--verify-tag', '--target', context['source_commit'], '--title', 'Pawchan ' + context['latest']['name'],
                   '--notes-file', f"update/publishing/changelogs/ru/{context['latest']['code']}.txt", '--draft')
        checked, mode, release = state.inspect(root)
        if checked != context or mode != 'build' or release is None or not release['draft']:
            raise ValueError('Created draft was not confirmed; inspect GitHub and retry without deleting it')
    upload_missing(root, context, release)
    try:
        assets.run('gh', 'release', 'edit', context['tag'], '--repo', assets.REPO, '--draft=false', '--latest')
    except (OSError, subprocess.CalledProcessError):
        # A timeout may occur after the server promoted the verified draft.
        if (assets.api('releases/' + str(release['id'])) or {}).get('draft', True):
            raise
    result = assets.api('releases/' + str(release['id']))
    if not result or result['draft'] or result['id'] != release['id'] or result['tag_name'] != context['tag']:
        raise ValueError('Public release was not confirmed')
    return result


def upload_missing(root, context, release):
    expected = set(assets.names(context['latest']['name']))
    existing = {a['name'] for a in release['assets']}
    if not existing <= expected or len(existing) != len(release['assets']):
        raise ValueError('Unexpected draft assets; no automatic cleanup/overwrite')
    with tempfile.TemporaryDirectory(prefix='pawchan-draft-') as value:
        check = Path(value)
        assets.download(release, check)
        assets.compare_files(root / 'dist', check, existing)
    missing = sorted(expected - existing)
    if missing:
        assets.run('gh', 'release', 'upload', context['tag'], *[str(root / 'dist' / n) for n in missing], '--repo', assets.REPO)
    updated = assets.api('releases/' + str(release['id']))
    if not updated or updated['id'] != release['id'] or not updated['draft']:
        raise ValueError('Draft changed during upload')
    if {a['name'] for a in updated['assets']} != expected:
        raise ValueError('Draft upload is incomplete; rerun can resume it')
    with tempfile.TemporaryDirectory(prefix='pawchan-upload-check-') as value:
        check = assets.download(updated, Path(value))
        assets.verify_files(check, context['latest']['name'])
        assets.compare_files(root / 'dist', check, expected)
