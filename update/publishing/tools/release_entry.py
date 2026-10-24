"""Single entry for normal publication, draft resumption, feed recovery and verified no-op."""
import argparse
import os
import subprocess
from pathlib import Path
import tempfile

import publication_assets as assets
import publication_delivery as delivery
import publication_feed as feed
import publication_state as state


def output(mode):
    path = os.environ.get('GITHUB_OUTPUT')
    if path:
        with open(path, 'a', encoding='utf-8') as stream:
            stream.write('mode=' + mode + '\n')
    print('Publication mode:', mode)


def verify(root, context, release):
    with tempfile.TemporaryDirectory(prefix='pawchan-public-') as directory:
        return state.verify_public(root, context, release, Path(directory))


def summary(context, mode):
    text = (f"\n## Pawchan {context['latest']['name']}\n"
            f"- Mode: {mode}.\n- Release assets and checksums verified.\n"
            '- Update feed confirmed; no duplicate history entry.\n'
            f"- Release: https://github.com/{assets.REPO}/releases/tag/{context['tag']}\n"
            '- Existing published APKs are never automatically overwritten.\n')
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as stream:
            stream.write(text)
    print(text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    choice = parser.add_mutually_exclusive_group(required=True)
    choice.add_argument('--check', action='store_true')
    choice.add_argument('--assets', action='store_true')
    choice.add_argument('--publish', action='store_true')
    args = parser.parse_args()
    progress = {'phase': 'preflight'}
    try:
        perform(args, progress)
    except (OSError, ValueError, KeyError, StopIteration, subprocess.CalledProcessError) as error:
        message = f"Publication stopped at {progress['phase']}: {error}"
        print(message)
        if os.environ.get('GITHUB_STEP_SUMMARY'):
            with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as stream:
                stream.write('\n## Publication stopped\n' + message + '\n'
                             'No public APK was overwritten. Inspect the log; use Retry-Release for recovery, not deletion.\n')
        raise SystemExit(1) from error


def perform(args, progress):
    verify_origin()
    context, mode, release = state.inspect(assets.ROOT)
    if args.assets:
        if mode != 'build':
            raise ValueError('Assets may be prepared only for a pending build')
        assets.verify_build(assets.ROOT / 'dist', context['latest']['name'])
        return
    if mode != 'build':
        progress['phase'] = 'public artifact verification'
        digest, length = verify(assets.ROOT, context, release)
    if args.check:
        output('done' if mode == 'verify' else mode)
        return
    if mode == 'build':
        progress['phase'] = 'draft upload and promotion'
        release = delivery.publish(assets.ROOT, context)
        digest, length = verify(assets.ROOT, context, release)
    if mode != 'verify':
        progress['phase'] = 'update feed recovery'
        feed.publish(assets.ROOT, context, digest, length)
    summary(context, 'already published' if mode == 'verify' else mode)


def verify_origin():
    if os.environ.get('GITHUB_ACTOR') != 'Solthariel':
        raise ValueError('Publication must be pushed using the Solthariel GitHub account')
    expected = 'Solthariel <339961350+Solthariel@users.noreply.github.com>'
    identities = assets.run('git', 'show', '-s', '--format=%an <%ae>%n%cn <%ce>', os.environ['GITHUB_SHA'], cwd=assets.ROOT)
    if identities.splitlines() != [expected, expected]:
        raise ValueError('Release commit author/committer differs; use Publish-Release or Retry-Release')


if __name__ == '__main__':
    main()
