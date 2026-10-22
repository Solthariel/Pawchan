"""Allowlisted one-time 0.1.29 republication after a full signed rebuild, with a flat source ZIP."""
import hashlib
import json
import os

from republish_029_assets import (
    APK, APK_HASH, COMMIT, RELEASE_ID, REPO, ROOT, SOURCE, TAG,
    api, asset_identity, run, verify_apk, verify_zip,
    verify_candidate,
)


def requested(root=None):
    root = ROOT if root is None else root
    request = json.loads((root / 'update/publishing/release-request.json').read_text(encoding='utf-8'))
    value = request.get('republish_029')
    if value is None:
        return False
    if request['version'] != '0.1.29' or request['code'] != 30 or value != {
            'release_id': RELEASE_ID, 'tag_commit': COMMIT, 'apk_sha256': APK_HASH}:
        raise ValueError('Unexpected one-time republication request')
    return True


def verify_context():
    if not requested() or os.environ.get('GITHUB_REPOSITORY') != REPO or os.environ.get('GITHUB_REF') != 'refs/heads/master':
        raise ValueError('This operation is restricted to the approved 0.1.29 request on master')


def original_release():
    release = api('releases/tags/' + TAG)
    if not release or release['id'] != RELEASE_ID or release['draft'] or release.get('immutable'):
        raise ValueError('The approved original release changed; nothing will be removed')
    if release['tag_name'] != TAG or set(x['name'] for x in release['assets']) != {APK, APK + '.sha256'}:
        raise ValueError('The original release asset set changed')
    tag = api('git/ref/tags/' + TAG)
    if not tag or tag['object']['sha'] != COMMIT or tag['object']['type'] != 'commit':
        raise ValueError('The corrected original tag changed')
    latest = api('releases/latest')
    if not latest or latest['id'] != RELEASE_ID:
        raise ValueError('A newer public release exists; refusing replacement')
    return release


def prepare(directory):
    verify_context()
    release = original_release()
    if (api('branches/master') or {}).get('commit', {}).get('sha') != os.environ['GITHUB_SHA']:
        raise ValueError('Master advanced; nothing will be removed')
    directory.mkdir(parents=True, exist_ok=True)
    backup = directory / 'backup'
    backup.mkdir()
    run('gh', 'release', 'download', TAG, '--repo', REPO, '--dir', str(backup))
    verify_apk(backup)
    (backup / 'release.json').write_text(json.dumps(release, indent=2), encoding='utf-8')
    (backup / 'notes.md').write_text(release.get('body') or '', encoding='utf-8')
    # Application source is unchanged; preserve the corrected release tag.
    run('git', 'archive', '--format=zip', '--output=' + str(directory / SOURCE), COMMIT)
    verify_zip(directory / SOURCE)
    digest = hashlib.sha256((directory / SOURCE).read_bytes()).hexdigest()
    (directory / (SOURCE + '.sha256')).write_text(digest + '  ' + SOURCE + '\n', encoding='ascii')
    print('Original APK verified and backed up; flat source ZIP prepared. No release mutated.')


def create_draft(stage, directory, original):
    files = [ROOT / 'dist' / APK, ROOT / 'dist' / (APK + '.sha256'),
             directory / SOURCE, directory / (SOURCE + '.sha256')]
    run('gh', 'release', 'create', stage, *map(str, files), '--repo', REPO, '--target', COMMIT,
        '--title', original['name'], '--notes-file', str(directory / 'backup/notes.md'), '--draft')
    candidate = api('releases/tags/' + stage)
    expected = {p.name for p in files}
    if not candidate or not candidate['draft'] or candidate['id'] == RELEASE_ID or candidate['tag_name'] != stage:
        raise ValueError('Unexpected staged draft release')
    if {a['name'] for a in candidate['assets']} != expected:
        raise ValueError('Staged assets are incomplete')
    check = directory / 'staged-verification'
    check.mkdir()
    run('gh', 'release', 'download', stage, '--repo', REPO, '--dir', str(check))
    verify_candidate(check, ROOT / 'dist')
    verify_zip(check / SOURCE)
    for path in files:
        if (check / path.name).read_bytes() != path.read_bytes():
            raise ValueError('Uploaded asset differs: ' + path.name)
    return candidate


def verify_public(candidate_id):
    release = api('releases/tags/' + TAG)
    if not release or release['id'] != candidate_id or release['draft'] or release['tag_name'] != TAG:
        raise ValueError('The new public release is not verified')
    if {x['name'] for x in release['assets']} != {APK, APK + '.sha256', SOURCE, SOURCE + '.sha256'}:
        raise ValueError('Published asset list differs')
    tag = api('git/ref/tags/' + TAG)
    if not tag or tag['object']['sha'] != COMMIT:
        raise ValueError('Release tag changed unexpectedly')
    return release


def restore_original(directory, original):
    # Never delete another release or overwrite a concurrent actor's publication.
    if api('releases/tags/' + TAG) is not None:
        return False
    run('gh', 'release', 'create', TAG, str(directory / 'backup' / APK),
        str(directory / 'backup' / (APK + '.sha256')), '--repo', REPO, '--target', COMMIT,
        '--title', original['name'], '--notes-file', str(directory / 'backup/notes.md'), '--draft')
    run('gh', 'release', 'edit', TAG, '--repo', REPO, '--draft=false', '--latest')
    print('Original APK release restored from verified backup; its release ID has changed.')
    return True


def publish(directory):
    verify_context()
    original = json.loads((directory / 'backup/release.json').read_text(encoding='utf-8'))
    verify_apk(directory / 'backup')
    verify_zip(directory / SOURCE)
    if asset_identity(original_release()) != asset_identity(original):
        raise ValueError('Original assets changed after backup')
    run_id = os.environ['GITHUB_RUN_ID']
    if not run_id.isdigit():
        raise ValueError('Invalid workflow run ID')
    attempt = os.environ.get('GITHUB_RUN_ATTEMPT', '1')
    if not attempt.isdigit():
        raise ValueError('Invalid workflow attempt')
    stage = 'pawchan-029-staging-' + run_id + '-' + attempt
    candidate = create_draft(stage, directory, original)
    current = original_release()
    if asset_identity(current) != asset_identity(original):
        raise ValueError('Original release changed while uploading; original left untouched')
    if (api('branches/master') or {}).get('commit', {}).get('sha') != os.environ['GITHUB_SHA']:
        raise ValueError('Master advanced; original left untouched')
    try:
        api('releases/' + str(RELEASE_ID), method='DELETE')
        api('releases/' + str(candidate['id']), method='PATCH', payload={
            'tag_name': TAG, 'target_commitish': COMMIT, 'draft': False, 'make_latest': 'true'})
        verify_public(candidate['id'])
    except (OSError, ValueError, KeyError) as failure:
        # A timed-out successful PATCH must not be undone or mistaken for a missing release.
        try:
            verify_public(candidate['id'])
        except (OSError, ValueError, KeyError):
            restore_original(directory, original)
            raise failure
    # A staging tag, if the server created one for the draft, is not part of release history.
    temporary = api('git/ref/tags/' + stage)
    if temporary and temporary['object']['sha'] == COMMIT:
        api('git/refs/tags/' + stage, method='DELETE')
    print('Pawchan 0.1.29 republished after full build; corrected original tag preserved.')
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as summary:
        summary.write('\n## Pawchan 0.1.29 republished\n'
                      '- APK rebuilt, signed and verified by the existing release pipeline.\n'
                      '- Download Pawchan-0.1.29-source.zip for a flat project root.\n'
                      '- GitHub-generated Source code archives still include their automatic root folder.\n'
                      '- Version remains 0.1.29 / code 30; installed copies will not auto-update to the same code.\n')
