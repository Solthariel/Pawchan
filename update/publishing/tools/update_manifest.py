#!/usr/bin/env python3
"""Prepare or publish update metadata from the final signed Pawchan APK."""

import argparse
import hashlib
import json
from pathlib import Path
from urllib.parse import urlparse


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--publish", action="store_true")
    parser.add_argument("--source")
    parser.add_argument("--changelog")
    return parser.parse_args()


def require_https(value, label):
    parsed = urlparse(value)
    if parsed.scheme != "https" or not parsed.netloc:
        raise SystemExit(f"{label} must be an absolute HTTPS URL")
    return value


def publication_urls(args):
    supplied = args.source is not None or args.changelog is not None
    if supplied and not args.publish:
        raise SystemExit("--source and --changelog are accepted only with --publish")
    if not args.publish:
        return None, None
    if not args.source or not args.changelog:
        raise SystemExit("--publish requires both --source and --changelog")
    return (
        require_https(args.source, "source"),
        require_https(args.changelog, "changelog"),
    )


def validate_existing_publication(package, same_release, apk_length, sha256):
    source = package.get("source")
    changelog = package.get("changelog")
    if (source is None) != (changelog is None):
        raise SystemExit(
            "Existing source and changelog must either both be null or both be set"
        )
    if source is None:
        return
    require_https(source, "existing source")
    require_https(changelog, "existing changelog")
    if not same_release:
        return
    stored_sha256 = package.get("sha256sum")
    stored_length = package.get("length")
    if not stored_sha256 or not isinstance(stored_length, int) or stored_length <= 0:
        raise SystemExit("Published release has incomplete integrity metadata")
    if stored_sha256 != sha256 or stored_length != apk_length:
        raise SystemExit(
            "Published release bytes are immutable; increase versionCode "
            "before packaging a different APK"
        )


def main():
    args = parse_args()
    source, changelog = publication_urls(args)
    publishing = Path(__file__).resolve().parents[1]
    versions_path = publishing / "versions.json"
    manifest_path = publishing / "manifest" / "data-v1.json"

    versions = json.loads(versions_path.read_text(encoding="utf-8"))["versions"]
    latest = max(versions, key=lambda version: version["code"])
    apk_bytes = args.apk.read_bytes()
    sha256 = hashlib.sha256(apk_bytes).hexdigest()

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    client = next(item for item in manifest["applications"] if item["name"] == "client")
    package = max(client["packages"], key=lambda item: item["version_code"])
    same_release = (
        package["version_code"] == latest["code"]
        and package["version_name"] == latest["name"]
    )
    validate_existing_publication(package, same_release, len(apk_bytes), sha256)
    package["version_name"] = latest["name"]
    package["version_code"] = latest["code"]
    package["length"] = len(apk_bytes)
    package["sha256sum"] = sha256
    if args.publish:
        package["source"] = source
        package["changelog"] = changelog
    elif not same_release:
        package["source"] = None
        package["changelog"] = None

    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    sidecar = args.apk.with_name(args.apk.name + ".sha256")
    sidecar.write_text(f"{sha256}  {args.apk.name}\n", encoding="ascii")
    state = "PUBLISHED" if args.publish else "PREPARED"
    print(f"RELEASE METADATA: {state}")
    print(f"APK: {args.apk}")
    print(f"SHA-256: {sha256}")
    print(f"Manifest: {manifest_path}")


if __name__ == "__main__":
    main()
