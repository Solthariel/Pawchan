"""Existing publisher by default; narrowly approved 0.1.29 rebuild otherwise."""
import argparse
import json
from pathlib import Path

import publish_feed_release as normal
import republish_029 as replacement
from rebuild_029_feed import check_metadata, client_package, publish_feed


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    choice = parser.add_mutually_exclusive_group(required=True)
    choice.add_argument('--check', action='store_true')
    choice.add_argument('--publish', action='store_true')
    args = parser.parse_args()
    if not replacement.requested():
        normal.main()
        return
    directory = replacement.ROOT / 'dist/republish-029'
    replacement.verify_context()
    if args.check:
        replacement.original_release()
        check_metadata(replacement.ROOT)
        replacement.prepare(directory)
        for name in ('data-v1.json', 'history-v1.json'):
            (directory / 'backup' / ('feed-' + name)).write_bytes(
                (replacement.ROOT / 'update/feed' / name).read_bytes())
        # Only a local build input. The general immutable-release guard stays unchanged.
        path = replacement.ROOT / 'update/publishing/manifest/data-v1.json'
        manifest = json.loads(path.read_text(encoding='utf-8'))
        package = client_package(manifest)
        package.update(source=None, changelog=None)
        path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    else:
        replacement.verify_candidate(replacement.ROOT / 'dist', replacement.ROOT / 'dist')
        replacement.publish(directory)
        publish_feed(replacement.ROOT)


if __name__ == '__main__':
    main()
