"""Resume compatible drafts; upload missing files only; promote only verified assets."""
import json
import os
import subprocess
import tempfile
import time
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
        release = create_draft(root, context)
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


def create_draft(root, context):
    # Use the REST creation response, not a tag/list lookup that can omit a new draft.
    response = assets.run('gh', 'api', '--method', 'POST', f'repos/{assets.REPO}/releases',
                          '-f', 'tag_name=' + context['tag'],
                          '-f', 'target_commitish=' + context['source_commit'],
                          '-f', 'name=Pawchan ' + context['latest']['name'],
                          '-f', 'body=' + context['notes'], '-F', 'draft=true')
    created = json.loads(response)
    if not isinstance(created, dict) or type(created.get('id')) is not int or created['id'] <= 0:
        raise ValueError('Draft creation returned no valid ID; inspect GitHub before retrying')
    if (created.get('tag_name'), created.get('draft'), created.get('target_commitish')) != (
            context['tag'], True, context['source_commit']):
        raise ValueError('Draft creation response identity differs; no upload or promotion')
    return confirm_draft(root, context, created['id'])


def confirm_draft(root, context, release_id):
    for attempt in range(4):
        checked, mode, release = state.inspect(root, release_id=release_id)
        if checked != context:
            raise ValueError('Created draft context changed; no upload or promotion')
        if release is not None:
            if mode != 'build' or not release['draft']:
                raise ValueError('Created release is not the expected draft; no upload or promotion')
            return release
        if attempt < 3:
            time.sleep(2)
    raise ValueError(f'Created draft ID {release_id} is not readable after 4 checks; '
                     'retry the existing publication without deleting it')
