"""Fail-closed state machine: build, recover the feed, or report already published."""
import json
import os

import publication_assets as assets
from release_sequence import identity, read_history, validate_local
from release_notes_format import current_notes


def read_context(root):
    if os.environ.get('GITHUB_REPOSITORY') != assets.REPO:
        raise ValueError('Unexpected repository')
    versions = json.loads((root / 'update/publishing/versions.json').read_text(encoding='utf-8'))['versions']
    latest = versions[-1]
    tag = 'v' + latest['name']
    if os.environ.get('GITHUB_REF') != 'refs/heads/master':
        raise ValueError('Unexpected branch/tag')
    request = json.loads((root / 'update/publishing/release-request.json').read_text(encoding='utf-8'))
    if (request['code'], request['version']) != identity(latest):
        raise ValueError('Request and version metadata differ')
    history = read_history(root)
    complete = [identity(v) for v in history] == [identity(v) for v in versions]
    if not complete:
        validate_local(root)
        if request.get('republish_029') or request.get('replace_first_release'):
            raise ValueError('Legacy replacement requests cannot authorize a new publication')
    elif not history:
        raise ValueError('Empty release history')
    current_notes(root, latest)
    return {'latest': latest, 'tag': tag, 'complete': complete, 'notes': current_notes(root, latest)}


def same_source(root, commit, head=None):
    if not commit or assets.run('git', 'rev-parse', commit + '^{commit}', cwd=root) != commit:
        raise ValueError('Release must reference an exact commit')
    paths = assets.run('git', 'diff', '--name-only', commit, head or os.environ['GITHUB_SHA'], cwd=root).splitlines()
    allowed = ('update/feed/', 'update/publishing/tools/')
    exact = {'.gitignore', 'update/publishing/notes-corrections.json', 'update/publishing/release-request.json', '.github/workflows/publish-release.yml',
             'docs/RELEASE_PROCESS.md', 'docs/CHANGELOG_HISTORY.md', 'update/publishing/README.md',
             'scripts/Publish-Release.ps1', 'scripts/Retry-Release.ps1'}
    if any(not (p in exact or p.startswith(allowed)) for p in paths):
        raise ValueError('Application/release source differs from the tag; prepare a new version')


def inspect(root):
    context = read_context(root)
    release = assets.api('releases/tags/' + context['tag'])
    reference = assets.api('git/ref/tags/' + context['tag'])
    context['source_commit'] = reference['object']['sha'] if reference else os.environ['GITHUB_SHA']
    if not release:
        check_missing(root, context, reference)
        return context, 'build', None
    if release.get('immutable') and release['draft']:
        raise ValueError('Unexpected immutable draft')
    if release['tag_name'] != context['tag'] or release['name'] != 'Pawchan ' + context['latest']['name']:
        raise ValueError('Unexpected release identity')
    if (release.get('body') or '').replace('\r\n', '\n').strip() != context['notes'].strip():
        raise ValueError('Release notes differ; refusing to overwrite')
    if release['draft']:
        check_draft(root, context, release, reference)
        return context, 'build', release
    if not reference or reference['object']['type'] != 'commit':
        raise ValueError('Public release has no direct commit tag')
    same_source(root, reference['object']['sha'])
    public = assets.api('releases/latest')
    if not public or public['id'] != release['id']:
        raise ValueError('A newer public release exists; refusing stale recovery')
    return context, 'verify' if context['complete'] else 'recover', release


def verify_public(root, context, release, directory):
    expected = set(assets.names(context['latest']['name']))
    actual = {a['name'] for a in release['assets']}
    legacy = context['complete'] and context['latest']['code'] <= assets.LEGACY_LAST_CODE and actual == set(assets.names(context['latest']['name'])[:2])
    if not legacy and actual != expected or len(actual) != len(release['assets']):
        raise ValueError('Public release assets are incomplete or unexpected')
    assets.download(release, directory)
    verifier = assets.verify_legacy if legacy else assets.verify_files
    digest, length = verifier(directory, context['latest']['name'])
    # A different APK for an already-published entry is NEVER silently accepted.
    if context['complete']:
        history = read_history(root)
        changelog = root / f"update/feed/changelogs/{context['latest']['code']}.txt"
        if history[-1].get('notes') != context['notes'] or changelog.read_text(encoding='utf-8') != context['notes']:
            raise ValueError('Published changelog/history differs; explicit review required')
        manifest = json.loads((root / 'update/feed/data-v1.json').read_text(encoding='utf-8'))
        package = package_for(manifest, context['latest']['code'])
        expected_source = f"https://github.com/{assets.REPO}/releases/download/{context['tag']}/{assets.names(context['latest']['name'])[0]}"
        expected_notes = f"https://raw.githubusercontent.com/{assets.REPO}/master/update/feed/changelogs/{context['latest']['code']}.txt"
        if (package['version_name'], package['sha256sum'], package['length'], package['source'], package['changelog']) != (
                context['latest']['name'], digest, length, expected_source, expected_notes):
            raise ValueError('Published feed and APK differ; explicit reviewed recovery is required')
    return digest, length


def package_for(manifest, code):
    client = next(item for item in manifest['applications'] if item['name'] == 'client')
    return next(item for item in client['packages'] if item['version_code'] == code)


def check_missing(root, context, reference):
    if context['complete']:
        raise ValueError('History says published but release is missing; no automatic deletion/rebuild')
    if reference:
        if reference['object']['type'] != 'commit':
            raise ValueError('Release tag must point directly to a commit')
        same_source(root, reference['object']['sha'])
    public = assets.api('releases/latest')
    history = read_history(root)
    expected = 'v' + history[-1]['name'] if history else None
    if (public or {}).get('tag_name') != expected:
        raise ValueError('Latest public release and published baseline differ')


def check_draft(root, context, release, reference):
    if context['complete']:
        raise ValueError('History says published but release is a draft')
    same_source(root, release['target_commitish'])
    if not reference and release['target_commitish'] != os.environ['GITHUB_SHA']:
        raise ValueError('Old draft has no durable tag; explicit review required before promotion')
    public = assets.api('releases/latest')
    history = read_history(root)
    expected = 'v' + history[-1]['name'] if history else None
    if (public or {}).get('tag_name') != expected:
        raise ValueError('A newer release exists; draft recovery stopped')
    if reference and reference['object']['sha'] != release['target_commitish']:
        raise ValueError('Draft tag belongs to another source commit')
